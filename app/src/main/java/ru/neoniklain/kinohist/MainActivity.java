package ru.neoniklain.kinohist;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * One-time setup screen: enter API credentials from the kino.pub profile,
 * run the device pairing flow, trigger manual sync.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean polling;

    private TextView tvStatus, tvCode;
    private Button btnPair, btnSync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        SyncWorker.schedulePeriodic(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        int pad = dp(24);
        int dp16 = dp(16);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(0x12, 0x12, 0x12));
        scroll.addView(root);

        TextView title = tv("История kino.pub", 28, true);
        root.addView(title);

        root.addView(tvStatus = tv("", 16, false));

        btnPair = btn("1. Получить код привязки", v -> startPairing());
        root.addView(btnPair);

        tvCode = tv("", 22, true);
        tvCode.setTextColor(Color.rgb(0xFF, 0xD5, 0x4F));
        root.addView(tvCode);

        btnSync = btn("2. Синхронизировать сейчас", v -> {
            SyncWorker.runNow(this);
            toast("Синхронизация запущена");
        });
        root.addView(btnSync);

        TextView hint = tv(
                "После привязки строка «История kino.pub» появится на главном "
                        + "экране Projectivy (Настройки → Изменить каналы, если не видна). "
                        + "Обновление — автоматически, раз в 12 часов.",
                14, false);
        hint.setPadding(0, dp16, 0, 0);
        root.addView(hint);

        setContentView(scroll);
    }

    private void startPairing() {
        setButtons(false);
        tvCode.setText("Запрос кода…");
        new Thread(() -> {
            try {
                KinoApi api = new KinoApi(this);
                JSONObject dev = api.requestDeviceCode();
                final String deviceCode = dev.getString("code");
                final String userCode = dev.getString("user_code");
                final String verifyUrl = dev.getString("verification_uri");
                final long expiresIn = dev.optLong("expires_in", 600);
                final long interval = dev.optLong("interval", 5);

                runOnUiThread(() -> tvCode.setText(
                        "Откройте на телефоне/компьютере:\n" + verifyUrl
                                + "\n\nи введите код:\n\n        " + userCode));

                long deadline = System.currentTimeMillis() + expiresIn * 1000L;
                polling = true;
                while (System.currentTimeMillis() < deadline && polling) {
                    Thread.sleep(interval * 1000L);
                    try {
                        JSONObject tokens = api.pollDeviceToken(deviceCode);
                        Prefs.saveTokens(this,
                                tokens.getString("access_token"),
                                tokens.getString("refresh_token"),
                                tokens.optLong("expires_in", 3600));
                        runOnUiThread(() -> {
                            tvCode.setText("Устройство привязано ✔");
                            toast("Привязано!");
                            SyncWorker.runNow(this);
                            refreshStatus();
                        });
                        return;
                    } catch (KinoApi.AuthorizationPendingException pending) {
                        // keep polling
                    }
                }
                runOnUiThread(() -> tvCode.setText("Код истёк, попробуйте снова"));
            } catch (Exception e) {
                Log.e(TAG, "pairing failed", e);
                runOnUiThread(() -> tvCode.setText("Ошибка: " + e.getMessage()));
            } finally {
                polling = false;
                runOnUiThread(() -> setButtons(true));
            }
        }, "pairing").start();
    }

    private void refreshStatus() {
        String auth = Prefs.hasTokens(this)
                ? "Авторизация: ✔ (refresh-токен есть)"
                : "Авторизация: не привязано";
        String sync;
        long last = Prefs.lastSync(this);
        if (last > 0) {
            String time = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    .format(new Date(last));
            sync = "Последняя синхронизация: " + time + "\n" + Prefs.lastStatus(this);
        } else {
            sync = "Синхронизация ещё не выполнялась";
        }
        tvStatus.setText(auth + "\n" + sync + "\n");
    }

    private void setButtons(boolean enabled) {
        btnPair.setEnabled(enabled);
        btnSync.setEnabled(enabled);
    }

    private TextView tv(String text, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(Color.WHITE);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setPadding(0, dp(10), 0, dp(4));
        return v;
    }

    private Button btn(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        b.setLayoutParams(lp);
        return b;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        polling = false;
        super.onDestroy();
    }
}
