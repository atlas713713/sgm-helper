package com.local.sgmhelper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

public final class WorshipAlarmReceiver extends BroadcastReceiver {
    private static final long MILITARY_INTERVAL_MS = 3 * 60 * 60 * 1_000L;
    private static final String ACTION_WORSHIP = "com.local.sgmhelper.WORSHIP";
    private static final String ACTION_LEGION_REWARD = "com.local.sgmhelper.LEGION_REWARD";
    private static final String ACTION_MILITARY = "com.local.sgmhelper.MILITARY";
    private static final String ACTION_WELFARE = "com.local.sgmhelper.WELFARE";
    private static final String ACTION_HEAVENFALL = "com.local.sgmhelper.HEAVENFALL";
    private static final String ACTION_DUNGEON = "com.local.sgmhelper.DUNGEON";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!isActionEnabled(context, action)) {
            return;
        }
        if (ACTION_LEGION_REWARD.equals(action)) {
            scheduleLegionReward(context);
        } else if (ACTION_WELFARE.equals(action)) {
            scheduleWelfare(context);
        } else if (ACTION_MILITARY.equals(action)) {
            scheduleMilitary(context);
        } else if (ACTION_HEAVENFALL.equals(action)) {
            scheduleHeavenfall(context);
        } else if (ACTION_DUNGEON.equals(action)) {
            scheduleDungeon(context);
        } else {
            scheduleWorship(context);
        }
        HelperAccessibilityService service = HelperAccessibilityService.getInstance();
        if (service != null) {
            if (ACTION_LEGION_REWARD.equals(action)) {
                service.startScheduledLegionReward();
            } else if (ACTION_WELFARE.equals(action)) {
                service.startScheduledWelfare();
            } else if (ACTION_MILITARY.equals(action)) {
                service.startScheduledMilitary();
            } else if (ACTION_HEAVENFALL.equals(action)) {
                service.startScheduledHeavenfall();
            } else if (ACTION_DUNGEON.equals(action)) {
                service.startScheduledDungeon();
            } else {
                service.startScheduledWorship();
            }
        } else {
            DiagnosticLog.error("SCHEDULE",
                    "task skipped because the accessibility service is unavailable: " + action);
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
        scheduleWelfare(context);
        scheduleHeavenfall(context);
        scheduleDungeon(context);
        return scheduleMilitary(context);
    }

    static void scheduleWorship(Context context) {
        schedule(context, ACTION_WORSHIP, 0,
                HelperAccessibilityService.PREF_WORSHIP_ENABLED, true,
                HelperAccessibilityService.PREF_HOUR,
                HelperAccessibilityService.PREF_MINUTE, 10, 30);
    }

    static void scheduleLegionReward(Context context) {
        schedule(context, ACTION_LEGION_REWARD, 1,
                HelperAccessibilityService.PREF_LEGION_REWARD_ENABLED, true,
                HelperAccessibilityService.PREF_LEGION_HOUR,
                HelperAccessibilityService.PREF_LEGION_MINUTE, 10, 10);
    }

    static void scheduleWelfare(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        int firstHour = preferences.getInt(
                HelperAccessibilityService.PREF_WELFARE_HOUR, 12);
        int firstMinute = preferences.getInt(
                HelperAccessibilityService.PREF_WELFARE_MINUTE, 5);
        scheduleAt(context, ACTION_WELFARE, 3,
                HelperAccessibilityService.PREF_WELFARE_ENABLED, true,
                firstHour, firstMinute);
        int second = welfareSecondMinuteOfDay(firstHour, firstMinute);
        scheduleAt(context, ACTION_WELFARE, 4,
                HelperAccessibilityService.PREF_WELFARE_ENABLED, true,
                second / 60, second % 60);
    }

    static void scheduleHeavenfall(Context context) {
        schedule(context, ACTION_HEAVENFALL, 5,
                HelperAccessibilityService.PREF_HEAVENFALL_ENABLED, true,
                HelperAccessibilityService.PREF_HEAVENFALL_HOUR,
                HelperAccessibilityService.PREF_HEAVENFALL_MINUTE, 22, 0);
    }

    static void scheduleDungeon(Context context) {
        schedule(context, ACTION_DUNGEON, 6,
                HelperAccessibilityService.PREF_DUNGEON_ENABLED, true,
                HelperAccessibilityService.PREF_DUNGEON_HOUR,
                HelperAccessibilityService.PREF_DUNGEON_MINUTE, 14, 0);
    }

    static int welfareSecondMinuteOfDay(int firstHour, int firstMinute) {
        return (firstHour * 60 + firstMinute + 6 * 60) % (24 * 60);
    }

    static long scheduleMilitary(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        if (!preferences.getBoolean(
                HelperAccessibilityService.PREF_MILITARY_ENABLED, true)) {
            cancelAlarm(context, ACTION_MILITARY, 2);
            return 0;
        }
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
        long now = System.currentTimeMillis();
        if (!hasConfiguredMilitaryTime(
                preferences.contains(HelperAccessibilityService.PREF_MILITARY_HOUR),
                preferences.contains(HelperAccessibilityService.PREF_MILITARY_MINUTE))) {
            return nextRollingMilitaryAt(now);
        }
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY,
                preferences.getInt(HelperAccessibilityService.PREF_MILITARY_HOUR, 0));
        next.set(Calendar.MINUTE,
                preferences.getInt(HelperAccessibilityService.PREF_MILITARY_MINUTE, 0));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        while (next.getTimeInMillis() <= now) {
            next.add(Calendar.HOUR_OF_DAY, 6);
        }
        return next.getTimeInMillis();
    }

    static long nextRollingMilitaryAt(long now) {
        return now + MILITARY_INTERVAL_MS;
    }

    static boolean hasConfiguredMilitaryTime(boolean hasHour, boolean hasMinute) {
        return hasHour && hasMinute;
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
        return scheduleMilitaryRetry(context, questName,
                System.currentTimeMillis() + (cooldownMinutes + 1L) * 60_000L);
    }

    static long scheduleMilitaryQuestCheck(
            Context context, String questName, long delayMillis) {
        return scheduleMilitaryRetry(
                context, questName, System.currentTimeMillis() + delayMillis);
    }

    private static long scheduleMilitaryRetry(
            Context context, String questName, long retryAt) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
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
            String enabledKey, boolean defaultEnabled,
            String hourKey, String minuteKey, int defaultHour, int defaultMinute) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        scheduleAt(context, action, requestCode, enabledKey, defaultEnabled,
                preferences.getInt(hourKey, defaultHour),
                preferences.getInt(minuteKey, defaultMinute));
    }

    private static void scheduleAt(Context context, String action, int requestCode,
            String enabledKey, boolean defaultEnabled, int hour, int minute) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        if (!preferences.getBoolean(enabledKey, defaultEnabled)) {
            cancelAlarm(context, action, requestCode);
            return;
        }
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        setAlarm(context, action, requestCode, next.getTimeInMillis());
    }

    private static boolean isActionEnabled(Context context, String action) {
        SharedPreferences preferences = context.getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        if (ACTION_LEGION_REWARD.equals(action)) {
            return preferences.getBoolean(
                    HelperAccessibilityService.PREF_LEGION_REWARD_ENABLED, true);
        }
        if (ACTION_MILITARY.equals(action)) {
            return preferences.getBoolean(
                    HelperAccessibilityService.PREF_MILITARY_ENABLED, true);
        }
        if (ACTION_WELFARE.equals(action)) {
            return preferences.getBoolean(
                    HelperAccessibilityService.PREF_WELFARE_ENABLED, true);
        }
        if (ACTION_HEAVENFALL.equals(action)) {
            return preferences.getBoolean(
                    HelperAccessibilityService.PREF_HEAVENFALL_ENABLED, true);
        }
        if (ACTION_DUNGEON.equals(action)) {
            return preferences.getBoolean(
                    HelperAccessibilityService.PREF_DUNGEON_ENABLED, true);
        }
        return preferences.getBoolean(
                HelperAccessibilityService.PREF_WORSHIP_ENABLED, true);
    }

    private static void cancelAlarm(Context context, String action, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                new Intent(context, WorshipAlarmReceiver.class).setAction(action),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
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
