package ru.neoniklain.kinohist;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
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
 * Setup screen: pairing status, account info, manual sync.
 * Dark violet palette (kino.pub brand), tuned for TV: large type,
 * D-pad focus highlights.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    // palette
    private static final int BG = Color.rgb(0x12, 0x0E, 0x1A);
    private static final int SURFACE = Color.rgb(0x1E, 0x17, 0x30);
    private static final int SURFACE_EDGE = Color.rgb(0x33, 0x28, 0x4E);
    private static final int ACCENT = Color.rgb(0xA5, 0x5E, 0xFF);
    private static final int ACCENT_DARK = Color.rgb(0x7B, 0x3F, 0xF2);
    private static final int TEXT = Color.rgb(0xF4, 0xF1, 0xFA);
    private static final int TEXT_DIM = Color.rgb(0x9A, 0x92, 0xB3);
    private static final int OK = Color.rgb(0x4A, 0xDE, 0x80);
    private static final int ERR = Color.rgb(0xF8, 0x71, 0x71);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean polling;

    private TextView tvStatusDot, tvStatusLine, tvSyncLine, tvAccountLabel, tvAccountValue, tvCode;
    private LinearLayout codeCard;
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
        loadAccount();
    }

    private void buildUi() {
        int pad = dp(48);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(BG);
        scroll.addView(root);

        // header
        root.addView(header("История kino.pub",
                "История просмотров на главном экране телевизора"));

        // status card
        LinearLayout statusCard = card();
        LinearLayout statusRow = row();
        tvStatusDot = new TextView(this);
        tvStatusDot.setText("●");
        tvStatusDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvStatusDot.setPadding(0, 0, dp(14), 0);
        statusRow.addView(tvStatusDot);
        tvStatusLine = label("", 18, true, TEXT);
        statusRow.addView(tvStatusLine);
        statusCard.addView(statusRow);
        tvSyncLine = label("", 15, false, TEXT_DIM);
        tvSyncLine.setPadding(dp(30), dp(6), 0, 0);
        statusCard.addView(tvSyncLine);
        root.addView(statusCard);

        // account card
        LinearLayout accountCard = card();
        tvAccountLabel = label("Аккаунт", 14, false, TEXT_DIM);
        accountCard.addView(tvAccountLabel);
        tvAccountValue = label("…", 18, true, TEXT);
        tvAccountValue.setPadding(0, dp(6), 0, 0);
        accountCard.addView(tvAccountValue);
        root.addView(accountCard);

        // pairing code block (hidden until requested)
        tvCode = new TextView(this);
        tvCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        tvCode.setTypeface(null, Typeface.BOLD);
        tvCode.setTextColor(ACCENT);
        tvCode.setGravity(Gravity.CENTER);
        tvCode.setVisibility(View.GONE);
        LinearLayout codeCardWrap = card();
        codeCard = codeCardWrap;
        codeCardWrap.addView(tvCode);
        codeCardWrap.setVisibility(View.GONE);
        root.addView(codeCardWrap);

        // actions
        btnPair = actionButton("Привязать устройство");
        btnPair.setOnClickListener(v -> startPairing());
        root.addView(btnPair);

        btnSync = actionButton("Синхронизировать сейчас");
        btnSync.setOnClickListener(v -> {
            SyncWorker.runNow(this);
            toast("Синхронизация запущена");
            handler.postDelayed(this::refreshStatus, 4000);
        });
        root.addView(btnSync);

        TextView hint = label(
                "Строка «История kino.pub» добавляется на главный экран в "
                        + "Настройки → Изменить каналы. Обновление — автоматически, раз в 12 часов.",
                14, false, TEXT_DIM);
        hint.setPadding(0, dp(28), 0, 0);
        root.addView(hint);

        setContentView(scroll);
        btnPair.requestFocus();
    }

    // ------- UI building blocks -------

    private LinearLayout header(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(TEXT);
        box.addView(t);
        TextView s = new TextView(this);
        s.setText(subtitle);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        s.setTextColor(TEXT_DIM);
        s.setPadding(0, dp(8), 0, 0);
        box.addView(s);
        box.setPadding(0, 0, 0, dp(30));
        return box;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(SURFACE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), SURFACE_EDGE);
        c.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, dp(10));
        c.setPadding(dp(26), dp(20), dp(26), dp(20));
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private TextView label(String text, int sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        v.setTextColor(color);
        return v;
    }

    private Button actionButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setMinHeight(dp(58));
        b.setStateListAnimator(null);

        GradientDrawable idle = new GradientDrawable();
        idle.setColor(SURFACE);
        idle.setCornerRadius(dp(16));
        idle.setStroke(dp(1), SURFACE_EDGE);

        GradientDrawable focused = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{ACCENT_DARK, ACCENT});
        focused.setCornerRadius(dp(16));

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{android.R.attr.state_pressed}, focused);
        states.addState(new int[]{}, idle);
        b.setBackground(states);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    // ------- data -------

    private void refreshStatus() {
        boolean ok = Prefs.hasTokens(this);
        tvStatusDot.setTextColor(ok ? OK : ERR);
        tvStatusLine.setText(ok ? "Привязано к аккаунту" : "Не привязано");
        long last = Prefs.lastSync(this);
        if (last > 0) {
            String time = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
                    .format(new Date(last));
            String status = Prefs.lastStatus(this);
            tvSyncLine.setText("Синхронизировано: " + time + " · " + status);
        } else {
            tvSyncLine.setText("Синхронизация ещё не выполнялась");
        }
    }

    private void loadAccount() {
        if (!Prefs.hasTokens(this)) {
            tvAccountValue.setText("Привяжите устройство");
            return;
        }
        new Thread(() -> {
            try {
                JSONObject user = new KinoApi(this).fetchUser();
                String name = user.optString("username", "");
                double days = user.optJSONObject("subscription") != null
                        ? user.optJSONObject("subscription").optDouble("days", 0)
                        : 0;
                String line = name.isEmpty() ? "Аккаунт kino.pub" : name;
                if (days > 0) line += " · подписка ещё " + Math.round(days) + " дн.";
                else line += " · подписка неактивна";
                String finalLine = line;
                runOnUiThread(() -> tvAccountValue.setText(finalLine));
            } catch (Exception e) {
                Log.w(TAG, "account info failed: " + e.getMessage());
                runOnUiThread(() -> tvAccountValue.setText("не удалось загрузить"));
            }
        }, "account").start();
    }

    private void startPairing() {
        setButtons(false);
        showCode("Запрос кода…");
        new Thread(() -> {
            try {
                KinoApi api = new KinoApi(this);
                JSONObject dev = api.requestDeviceCode();
                final String deviceCode = dev.getString("code");
                final String userCode = dev.getString("user_code");
                final String verifyUrl = dev.getString("verification_uri");
                final long expiresIn = dev.optLong("expires_in", 600);
                final long interval = dev.optLong("interval", 5);

                runOnUiThread(() -> showCode(
                        "Откройте " + verifyUrl + "\nи введите код\n\n" + userCode));

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
                            showCode("Устройство привязано ✔");
                            toast("Привязано");
                            SyncWorker.runNow(this);
                            refreshStatus();
                            loadAccount();
                        });
                        return;
                    } catch (KinoApi.AuthorizationPendingException pending) {
                        // keep polling
                    }
                }
                runOnUiThread(() -> showCode("Код истёк — попробуйте снова"));
            } catch (Exception e) {
                Log.e(TAG, "pairing failed", e);
                runOnUiThread(() -> showCode("Ошибка: " + e.getMessage()));
            } finally {
                polling = false;
                runOnUiThread(() -> setButtons(true));
            }
        }, "pairing").start();
    }

    private void showCode(String text) {
        codeCard.setVisibility(View.VISIBLE);
        tvCode.setText(text);
    }

    private void setButtons(boolean enabled) {
        btnPair.setEnabled(enabled);
        btnSync.setEnabled(enabled);
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
