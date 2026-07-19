package com.local.sgmhelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

final class HeavenfallAutomation {
    private static final int MAP_CENTER_X = 300;
    private static final int MAP_CENTER_Y = 25;
    private static final int CENTER_X_TOLERANCE = 20;
    private static final int CENTER_Y_TOLERANCE = 5;
    private static final int CENTER_WAIT_ATTEMPTS = 60;
    private static final long CENTER_CHECK_INTERVAL_MS = 1_000;
    private static final long BOSS_SCAN_INTERVAL_MS = 3_000;
    private static final long RESUME_DELAY_MS = 10_000;

    private final AutomationHost host;
    private final WildernessNavigator wildernessNavigator;
    private long deadline;
    private boolean finishing;

    HeavenfallAutomation(AutomationHost host) {
        this.host = host;
        wildernessNavigator = new WildernessNavigator(host, "天降");
    }

    void start() {
        SharedPreferences preferences = host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        int durationMinutes = preferences.getInt(
                HelperAccessibilityService.PREF_HEAVENFALL_DURATION_MINUTES, 5);
        int zone = preferences.getInt(
                HelperAccessibilityService.PREF_HEAVENFALL_ZONE, 1);
        host.startAutomation("天降：打开游戏", () -> begin(zone, durationMinutes));
    }

    private void begin(int zone, int durationMinutes) {
        finishing = false;
        deadline = System.currentTimeMillis() + durationMillis(durationMinutes);
        host.showProgress("天降：停止自动攻击");
        host.ensureAutoAttackDisabled(
                () -> wildernessNavigator.navigateToZone(zone, this::goToCenter));
    }

    private void goToCenter() {
        if (finishIfExpired()) {
            return;
        }
        host.showProgress("天降：前往地图中间 300,25");
        host.tapMapCoordinate(MAP_CENTER_X, MAP_CENTER_Y,
                () -> waitForCenter(CENTER_WAIT_ATTEMPTS));
    }

    private void waitForCenter(int remainingAttempts) {
        if (finishIfExpired()) {
            return;
        }
        host.recognizeMapCoordinate(value -> {
            if (centerReached(value)) {
                host.closeAutoPathPanel(this::openEnemyPanel);
            } else if (remainingAttempts > 1) {
                host.postDelayed(
                        () -> waitForCenter(remainingAttempts - 1), CENTER_CHECK_INTERVAL_MS);
            } else {
                host.failAutomation("天降：前往地图中间超时");
            }
        });
    }

    private void openEnemyPanel() {
        if (finishIfExpired()) {
            return;
        }
        host.showProgress("天降：打开自动寻敌");
        host.tap(1130, 500, () -> host.tap(1015, 165, this::scanForBoss));
    }

    private void scanForBoss() {
        if (finishIfExpired()) {
            return;
        }
        findRedBoss(target -> {
            if (target == null) {
                host.showProgress("天降：未发现红色 BOSS，返回地图中间");
                host.postDelayed(this::goToCenter, BOSS_SCAN_INTERVAL_MS);
                return;
            }
            host.showProgress("天降：攻击 " + target.name);
            host.tap(target.bounds.centerX(), target.bounds.centerY(),
                    () -> host.postDelayed(
                            () -> waitForBossToDisappear(target.name), BOSS_SCAN_INTERVAL_MS));
        });
    }

    private void waitForBossToDisappear(String bossName) {
        if (finishIfExpired()) {
            return;
        }
        findRedBoss(target -> {
            if (target == null) {
                host.showProgress("天降：" + bossName + " 已消失，停止攻击并返回中间");
                host.ensureAutoAttackDisabled(this::goToCenter);
            } else {
                host.showProgress("天降：正在攻击 " + bossName);
                host.postDelayed(
                        () -> waitForBossToDisappear(bossName), BOSS_SCAN_INTERVAL_MS);
            }
        });
    }

    private void findRedBoss(java.util.function.Consumer<BossAutomation.BossTarget> result) {
        host.recognizeText(text -> host.captureScreenshot(bitmap -> {
            try {
                result.accept(bitmap == null ? null : BossAutomation.findRedBoss(text, bitmap));
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        }));
    }

    private boolean finishIfExpired() {
        if (finishing) {
            return true;
        }
        if (!expired(System.currentTimeMillis(), deadline)) {
            return false;
        }
        finishing = true;
        host.showProgress("天降：时间结束，回城");
        host.returnHome(() -> {
            host.showProgress("天降：已结束，10秒后恢复主要任务");
            host.postDelayed(host::resumePrimaryTask, RESUME_DELAY_MS);
        });
        return true;
    }

    static long durationMillis(int minutes) {
        return Math.max(1, minutes) * 60_000L;
    }

    static boolean expired(long now, long deadline) {
        return now >= deadline;
    }

    static boolean centerReached(String mapCoordinate) {
        int[] coordinate = BossAutomation.parseMapCoordinate(mapCoordinate);
        return coordinate != null
                && Math.abs(coordinate[0] - MAP_CENTER_X) <= CENTER_X_TOLERANCE
                && Math.abs(coordinate[1] - MAP_CENTER_Y) <= CENTER_Y_TOLERANCE;
    }
}
