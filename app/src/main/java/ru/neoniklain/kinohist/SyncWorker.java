package ru.neoniklain.kinohist;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/** Periodic background sync of the history channel. */
public class SyncWorker extends Worker {

    private static final String PERIODIC = "history_sync";
    private static final String ONE_SHOT = "history_sync_now";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!Prefs.hasTokens(getApplicationContext())) {
            return Result.success(); // nothing to sync yet
        }
        try {
            ChannelSync.sync(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Prefs.setLastSync(getApplicationContext(), System.currentTimeMillis(),
                    "Ошибка: " + e.getMessage());
            return Result.retry();
        }
    }

    public static void schedulePeriodic(Context ctx) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req);
    }

    public static void runNow(Context ctx) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(ctx).enqueueUniqueWork(
                ONE_SHOT, ExistingWorkPolicy.REPLACE, req);
    }
}
