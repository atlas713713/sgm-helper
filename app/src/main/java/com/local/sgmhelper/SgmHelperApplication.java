package com.local.sgmhelper;

import android.app.Application;

public final class SgmHelperApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DiagnosticLog.initialize(this);
        WorshipAlarmReceiver.scheduleAll(this);
        Thread.UncaughtExceptionHandler previous =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            DiagnosticLog.error("CRASH", "uncaught on " + thread.getName(), error);
            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
        });
    }
}
