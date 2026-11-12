package ru.neoniklain.kinohist;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String NAME = "kinohist";

    /**
     * Public OAuth credentials of the open-source Kodi client for kino.pub
     * (quarckster/kodi.kino.pub) — the same defaults Lampa/AVReleases plugins
     * use. A personal pair can only be obtained from support@kino.pub.
     */
    private static final String DEFAULT_CLIENT_ID = "xbmc";
    private static final String DEFAULT_CLIENT_SECRET = "cgg3gtifu46urtfp2zp1nqtba0k2ezxh";
    private static final String DEFAULT_API_HOST = "api.service-kp.com";

    private static final String K_ACCESS_TOKEN = "access_token";
    private static final String K_REFRESH_TOKEN = "refresh_token";
    private static final String K_TOKEN_EXPIRES = "token_expires";
    private static final String K_LAST_SYNC = "last_sync";
    private static final String K_LAST_STATUS = "last_status";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String clientId(Context c) { return DEFAULT_CLIENT_ID; }
    public static String clientSecret(Context c) { return DEFAULT_CLIENT_SECRET; }
    public static String apiHost(Context c) { return DEFAULT_API_HOST; }

    public static String accessToken(Context c) { return sp(c).getString(K_ACCESS_TOKEN, ""); }
    public static String refreshToken(Context c) { return sp(c).getString(K_REFRESH_TOKEN, ""); }

    public static boolean hasTokens(Context c) {
        return !refreshToken(c).isEmpty();
    }

    public static void saveTokens(Context c, String access, String refresh, long expiresInSec) {
        long expiresAt = System.currentTimeMillis() + expiresInSec * 1000L;
        sp(c).edit()
                .putString(K_ACCESS_TOKEN, access)
                .putString(K_REFRESH_TOKEN, refresh)
                .putLong(K_TOKEN_EXPIRES, expiresAt)
                .apply();
    }

    public static boolean tokenExpired(Context c) {
        return System.currentTimeMillis() > sp(c).getLong(K_TOKEN_EXPIRES, 0) - 5 * 60 * 1000L;
    }

    public static void setLastSync(Context c, long when, String status) {
        sp(c).edit()
                .putLong(K_LAST_SYNC, when)
                .putString(K_LAST_STATUS, status)
                .apply();
    }

    public static long lastSync(Context c) { return sp(c).getLong(K_LAST_SYNC, 0); }
    public static String lastStatus(Context c) { return sp(c).getString(K_LAST_STATUS, ""); }

    public static void clearTokens(Context c) {
        sp(c).edit()
                .remove(K_ACCESS_TOKEN)
                .remove(K_REFRESH_TOKEN)
                .remove(K_TOKEN_EXPIRES)
                .apply();
    }
}
