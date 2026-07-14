package com.local.sgmhelper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

public final class WorshipAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "SgmHelper";
    private static final String ACTION_WORSHIP = "com.local.sgmhelper.WORSHIP";
    private static final String ACTION_LEGION_REWARD = "com.local.sgmhelper.LEGION_REWARD";
    private static final String ACTION_MILITARY = "com.local.sgmhelper.MILITARY";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_LEGION_REWARD.equals(action)) {
            scheduleLegionReward(context);
        } else if (ACTION_MILITARY.equals(action)) {
            scheduleMilitary(context);
        } else {
            scheduleWorship(context);
        }
        HelperAccessibilityService service = HelperAccessibilityService.getInstance();
        if (service != null) {
            if (ACTION_LEGION_REWARD.equals(action)) {
                service.startScheduledLegionReward();
            } else if (ACTION_MILITARY.equals(action)) {
                service.startScheduledMilitary();
            } else {
                service.startScheduledWorship();
            }
        } else {
            Log.e(TAG, "Scheduled task skipped because the accessibility service is unavailable");
        }
    }

    static long scheduleAll(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent legacy = PendingIntent.getBroadcast(context, 0,
                new Intent(context, WorshipAlarmReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null && legacy != null) {
            alarmManager.cancel(legacy);
            legacy.cancel();
        }
        scheduleWorship(context);
        scheduleLegionReward(context);
        return scheduleMilitary(context);
    }

    static void scheduleWorship(Context context) {
        schedule(context, ACTION_WORSHIP, 0,
                HelperAccessibilityService.PREF_HOUR,
                HelperAccessibilityService.PREF_MINUTE, 10, 0);
    }

    static void scheduleLegionReward(Context context) {
        schedule(context, ACTION_LEGION_REWARD, 1,
                HelperAccessibilityService.PREF_LEGION_HOUR,
                HelperAccessibilityService.PREF_LEGION_MINUTE, 10, 10);
    }

    static long scheduleMilitary(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        preferences.edit().remove(HelperAccessibilityService.PREF_MILITARY_RETRY_AT).apply();
        long supplyRetryAt = preferences.getLong(
                HelperAccessibilityService.PREF_SUPPLY_RETRY_AT, 0);
        long wildernessRetryAt = preferences.getLong(
                HelperAccessibilityService.PREF_WILDERNESS_RETRY_AT, 0);
        long regularAt = nextRegularMilitaryAt(preferences);
        long nextMilitaryAt = nextMilitaryAt(
                now, supplyRetryAt, wildernessRetryAt, regularAt);
        setAlarm(context, ACTION_MILITARY, 2, nextMilitaryAt);
        return nextMilitaryAt;
    }

    private static long nextRegularMilitaryAt(SharedPreferences preferences) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY,
                preferences.getInt(HelperAccessibilityService.PREF_MILITARY_HOUR, 10));
        next.set(Calendar.MINUTE,
                preferences.getInt(HelperAccessibilityService.PREF_MILITARY_MINUTE, 30));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        while (!next.after(Calendar.getInstance())) {
            next.add(Calendar.HOUR_OF_DAY, 6);
        }
        return next.getTimeInMillis();
    }

    static long nextMilitaryAt(
            long now, long supplyRetryAt, long wildernessRetryAt, long regularAt) {
        long next = regularAt;
        if (supplyRetryAt > now) {
            next = Math.min(next, supplyRetryAt);
        }
        if (wildernessRetryAt > now) {
            next = Math.min(next, wildernessRetryAt);
        }
        return next;
    }

    static long scheduleMilitaryAfterCooldown(
            Context context, String questName, int cooldownMinutes) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long retryAt = now + (cooldownMinutes + 1L) * 60_000L;
        String retryKey = militaryRetryKey(questName);
        if (retryKey == null) {
            return scheduleMilitary(context);
        }
        preferences.edit()
                .putLong(retryKey, retryAt)
                .apply();
        scheduleMilitary(context);
        return retryAt;
    }

    static long markMilitaryQuestHandled(Context context, String questName) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        String retryKey = militaryRetryKey(questName);
        if (retryKey == null) {
            return scheduleMilitary(context);
        }
        preferences.edit()
                .putLong(retryKey, nextRegularMilitaryAt(preferences))
                .apply();
        return scheduleMilitary(context);
    }

    private static String militaryRetryKey(String questName) {
        if ("补充军团物资".equals(questName)) {
            return HelperAccessibilityService.PREF_SUPPLY_RETRY_AT;
        }
        if ("巡狩军团荒野".equals(questName)) {
            return HelperAccessibilityService.PREF_WILDERNESS_RETRY_AT;
        }
        return null;
    }

    private static void schedule(Context context, String action, int requestCode,
            String hourKey, String minuteKey, int defaultHour, int defaultMinute) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, preferences.getInt(hourKey, defaultHour));
        next.set(Calendar.MINUTE, preferences.getInt(minuteKey, defaultMinute));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        setAlarm(context, action, requestCode, next.getTimeInMillis());
    }

    private static void setAlarm(Context context, String action, int requestCode, long triggerAt) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                new Intent(context, WorshipAlarmReceiver.class).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent);
    }
}
