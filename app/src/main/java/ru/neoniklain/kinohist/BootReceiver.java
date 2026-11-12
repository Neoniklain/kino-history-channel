package ru.neoniklain.kinohist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-schedules the periodic sync after reboot (WorkManager usually handles this itself). */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SyncWorker.schedulePeriodic(context);
            if (Prefs.hasTokens(context)) {
                SyncWorker.runNow(context);
            }
        }
    }
}
