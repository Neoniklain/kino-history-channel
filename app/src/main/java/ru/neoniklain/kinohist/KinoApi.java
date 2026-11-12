package ru.neoniklain.kinohist;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal kino.pub API client: OAuth2 device flow + watch history.
 * Docs: https://kinoapi.com (auth), GET {host}/v1/history.
 */
public final class KinoApi {
    private static final String TAG = "KinoApi";

    public static class ApiException extends Exception {
        public ApiException(String message) { super(message); }
        public ApiException(String message, Throwable cause) { super(message, cause); }
    }

    public static class AuthorizationPendingException extends ApiException {
        public AuthorizationPendingException() { super("authorization_pending"); }
    }

    private final Context context;

    public KinoApi(Context context) {
        this.context = context.getApplicationContext();
    }

    private String base() {
        return "https://" + Prefs.apiHost(context) + "/";
    }

    private static String readFully(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private JSONObject postForm(String url, String body) throws ApiException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(data.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
            }
            int code = conn.getResponseCode();
            String text = readFully(code >= 200 && code < 300
                    ? conn.getInputStream() : conn.getErrorStream());
            JSONObject obj = new JSONObject(text);
            if (code >= 200 && code < 300) return obj;
            String err = obj.optString("error", "http_" + code);
            if ("authorization_pending".equals(err)) throw new AuthorizationPendingException();
            throw new ApiException(err + ": " + obj.optString("error_description", ""));
        } catch (JSONException e) {
            throw new ApiException("bad json", e);
        } catch (IOException e) {
            throw new ApiException("network: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private JSONObject getJson(String url, String bearer) throws ApiException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
            int code = conn.getResponseCode();
            String text = readFully(code >= 200 && code < 300
                    ? conn.getInputStream() : conn.getErrorStream());
            if (code == 401) throw new ApiException("unauthorized");
            if (code != 200) throw new ApiException("http_" + code + ": " + text);
            return new JSONObject(text);
        } catch (JSONException e) {
            throw new ApiException("bad json", e);
        } catch (IOException e) {
            throw new ApiException("network: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Step 1 of device flow. */
    public JSONObject requestDeviceCode() throws ApiException {
        String body = "grant_type=device_code"
                + "&client_id=" + enc(Prefs.clientId(context))
                + "&client_secret=" + enc(Prefs.clientSecret(context));
        return postForm(base() + "oauth2/device", body);
    }

    /** Step 2: poll. Throws AuthorizationPendingException until the user activates. */
    public JSONObject pollDeviceToken(String deviceCode) throws ApiException {
        String body = "grant_type=device_token"
                + "&client_id=" + enc(Prefs.clientId(context))
                + "&client_secret=" + enc(Prefs.clientSecret(context))
                + "&code=" + enc(deviceCode);
        return postForm(base() + "oauth2/device", body);
    }

    public void refreshTokens() throws ApiException {
        String body = "grant_type=refresh_token"
                + "&client_id=" + enc(Prefs.clientId(context))
                + "&client_secret=" + enc(Prefs.clientSecret(context))
                + "&refresh_token=" + enc(Prefs.refreshToken(context));
        JSONObject obj = postForm(base() + "oauth2/token", body);
        try {
            Prefs.saveTokens(context,
                    obj.getString("access_token"),
                    obj.optString("refresh_token", Prefs.refreshToken(context)),
                    obj.optLong("expires_in", 3600));
        } catch (JSONException e) {
            throw new ApiException("bad token response", e);
        }
    }

    /** Valid (possibly refreshed) access token. */
    public String accessToken() throws ApiException {
        if (Prefs.tokenExpired(context)) {
            if (!Prefs.hasTokens(context)) throw new ApiException("not authorized");
            refreshTokens();
        }
        return Prefs.accessToken(context);
    }

    /** Newest-first watch history entries. */
    public JSONArray fetchHistory(int perPage) throws ApiException {
        JSONObject obj = getJson(base() + "v1/history?page=1&perpage=" + perPage, accessToken());
        JSONArray history = obj.optJSONArray("history");
        if (history == null) throw new ApiException("no history field");
        return history;
    }

    /** Profile: {username, subscription:{days}}. */
    public JSONObject fetchUser() throws ApiException {
        JSONObject obj = getJson(base() + "v1/user", accessToken());
        JSONObject user = obj.optJSONObject("user");
        if (user == null) throw new ApiException("no user field");
        return user;
    }

    private static String enc(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return s;
        }
    }

    static void log(String msg) {
        Log.i(TAG, msg);
    }
}
