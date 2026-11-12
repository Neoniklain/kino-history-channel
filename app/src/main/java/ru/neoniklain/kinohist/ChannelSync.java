package ru.neoniklain.kinohist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;

import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.ChannelLogoUtils;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Publishes a browsable Android TV channel "История kino.pub" and fills it
 * with watch-history entries from the kino.pub API.
 */
public final class ChannelSync {
    private static final String TAG = "ChannelSync";

    public static final String CHANNEL_NAME = "История kino.pub";
    private static final int MAX_ITEMS = 30;

    /** kino.pub item deep link format (verified against com.kinopub InfoActivity filters). */
    private static final String ITEM_URL = "https://kinopub.tv/item/view/";

    private ChannelSync() {}

    public static void sync(Context ctx) throws KinoApi.ApiException {
        Context app = ctx.getApplicationContext();
        KinoApi api = new KinoApi(app);
        JSONArray history = api.fetchHistory(MAX_ITEMS);

        long channelId = ensureChannel(app);
        repopulatePrograms(app, channelId, history);
        Prefs.setLastSync(app, System.currentTimeMillis(),
                "OK, записей: " + history.length());
        Log.i(TAG, "sync ok, items=" + history.length());
    }

    private static long ensureChannel(Context ctx) {
        String[] projection = {
                TvContractCompat.Channels._ID,
                TvContractCompat.Channels.COLUMN_DISPLAY_NAME
        };
        try (android.database.Cursor cursor = ctx.getContentResolver().query(
                TvContractCompat.Channels.CONTENT_URI, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(1);
                    if (CHANNEL_NAME.equals(name)) {
                        return cursor.getLong(0);
                    }
                }
            }
        }

        Intent appLink = new Intent(ctx, MainActivity.class);
        Channel channel = new Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(CHANNEL_NAME)
                .setDescription("История просмотров kino.pub")
                .setAppLinkIntent(appLink)
                .setBrowsable(true)
                .build();
        Uri uri = ctx.getContentResolver().insert(
                TvContractCompat.Channels.CONTENT_URI, channel.toContentValues());
        long id = uri != null ? android.content.ContentUris.parseId(uri) : -1;
        if (id != -1) {
            ChannelLogoUtils.storeChannelLogo(ctx, id, makeLogo(ctx));
        }
        Log.i(TAG, "channel created, id=" + id);
        return id;
    }

    private static void repopulatePrograms(Context ctx, long channelId, JSONArray history) {
        // Wipe existing programs.
        try (android.database.Cursor cursor = ctx.getContentResolver().query(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                new String[]{TvContractCompat.PreviewPrograms._ID}, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long pid = cursor.getLong(0);
                    ctx.getContentResolver().delete(
                            TvContractCompat.buildPreviewProgramUri(pid), null, null);
                }
            }
        }

        for (int i = 0; i < history.length(); i++) {
            JSONObject entry = history.optJSONObject(i);
            if (entry == null) continue;
            JSONObject item = entry.optJSONObject("item");
            if (item == null) continue;

            int id = item.optInt("id", 0);
            if (id == 0) continue;
            String type = item.optString("type", "movie");
            String title = item.optString("title", "Без названия");
            int year = item.optInt("year", 0);
            String plot = item.optString("plot", "");
            JSONObject posters = item.optJSONObject("posters");
            String poster = posters != null ? posters.optString("medium", "") : "";
            if (poster.isEmpty() && posters != null) poster = posters.optString("small", "");

            JSONObject media = entry.optJSONObject("media");
            String subtitle = "";
            if (media != null) {
                String ep = media.optString("title", "");
                int sn = media.optInt("snumber", 0);
                int en = media.optInt("number", 0);
                if (!ep.isEmpty()) subtitle = ep;
                else if (sn > 0 || en > 0) subtitle = "S" + sn + "E" + en;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(ITEM_URL + id));

            String displayTitle = title + (year > 0 ? " (" + year + ")" : "");
            if (!subtitle.isEmpty()) displayTitle += " — " + subtitle;

            PreviewProgram.Builder builder = new PreviewProgram.Builder()
                    .setChannelId(channelId)
                    .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                    .setTitle(displayTitle)
                    .setDescription(plot)
                    .setIntent(intent)
                    .setContentId(String.valueOf(id))
                    .setInternalProviderId(type + ":" + id)
                    .setPosterArtAspectRatio(
                            TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_2_3)
                    .setWeight(history.length() - i);
            if (!poster.isEmpty()) builder.setPosterArtUri(Uri.parse(poster));

            ctx.getContentResolver().insert(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    builder.build().toContentValues());
        }
    }

    /** Simple clock glyph as the channel logo, drawn at runtime (no assets). */
    private static Bitmap makeLogo(Context ctx) {
        int size = 96;
        float d = ctx.getResources().getDisplayMetrics().density;
        int px = (int) (size * d);
        Bitmap bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.rgb(0x1B, 0x1B, 0x1B));
        c.drawCircle(px / 2f, px / 2f, px / 2f, bg);
        Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);
        fg.setColor(Color.WHITE);
        fg.setStyle(Paint.Style.STROKE);
        fg.setStrokeWidth(px * 0.07f);
        c.drawCircle(px / 2f, px / 2f, px * 0.32f, fg);
        fg.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(px / 2f, px / 2f, px / 2f, px * 0.32f, fg);
        c.drawLine(px / 2f, px / 2f, px * 0.68f, px / 2f, fg);
        return bmp;
    }
}
