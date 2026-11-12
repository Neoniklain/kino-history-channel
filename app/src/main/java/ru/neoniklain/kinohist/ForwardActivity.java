package ru.neoniklain.kinohist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Projectivy launches channel item clicks inside the channel owner's package.
 * This invisible trampoline catches those clicks and forwards the https link
 * to the kino.pub app.
 */
public class ForwardActivity extends Activity {

    public static final String KINOPUB_PACKAGE = "com.kinopub";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent() != null ? getIntent().getData() : null;
        if (data != null) {
            Intent out = new Intent(Intent.ACTION_VIEW, data);
            out.setPackage(KINOPUB_PACKAGE);
            try {
                startActivity(out);
            } catch (Exception ignored) {
                // kino.pub app not installed or link unsupported: do nothing
            }
        }
        finish();
    }
}
