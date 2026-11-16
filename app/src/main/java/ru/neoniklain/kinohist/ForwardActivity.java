package ru.neoniklain.kinohist;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

/**
 * Projectivy launches channel item clicks inside the channel owner's package.
 * This invisible trampoline catches those clicks and forwards the https link
 * to a kino.pub-family player app.
 *
 * The ecosystem has two app generations in the wild:
 *   - legacy official client: package com.kinopub, handles kinopub.tv / kino.pub links
 *   - rebranded client:       package watch.kino, handles kino.watch links
 * Try both packages and all known link hosts; first resolvable combination wins.
 */
public class ForwardActivity extends Activity {

    private static final String[] TARGET_PACKAGES = {"com.kinopub", "watch.kino"};
    private static final String[] HOST_VARIANTS = {null, "kino.pub", "kino.watch"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent() != null ? getIntent().getData() : null;
        if (data != null) {
            PackageManager pm = getPackageManager();
            Intent fallback = null;
            for (String pkg : TARGET_PACKAGES) {
                if (pm.getLaunchIntentForPackage(pkg) == null && !isResolvable(pm, data, pkg)) {
                    continue;
                }
                for (String host : HOST_VARIANTS) {
                    Uri uri = host == null ? data : data.buildUpon().authority(host).build();
                    if (isResolvable(pm, uri, pkg)) {
                        start(pm, uri, pkg);
                        finish();
                        return;
                    }
                    if (fallback == null && host == null) fallback = new Intent(Intent.ACTION_VIEW, uri).setPackage(pkg);
                }
            }
            if (fallback != null) {
                try {
                    startActivity(fallback);
                } catch (Exception ignored) {
                    // nothing handles the link at all
                }
            }
        }
        finish();
    }

    private static boolean isResolvable(PackageManager pm, Uri uri, String pkg) {
        Intent probe = new Intent(Intent.ACTION_VIEW, uri).setPackage(pkg);
        return pm.resolveActivity(probe, 0) != null;
    }

    private void start(PackageManager pm, Uri uri, String pkg) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri).setPackage(pkg));
        } catch (Exception ignored) {
            // race: package vanished between probe and start
        }
    }
}
