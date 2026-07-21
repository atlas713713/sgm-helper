package com.local.sgmhelper;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

final class TrainingAutomation {
    private final AutomationHost host;
    private final WildernessNavigator wildernessNavigator;
    private int wildernessZone;
    private String wildernessMonster;

    TrainingAutomation(AutomationHost host) {
        this.host = host;
        wildernessNavigator = new WildernessNavigator(host, "自动练级");
    }

    void start() {
        host.checkInventoryBeforePrimary(this::startAfterInventoryCheck);
    }

    private void startAfterInventoryCheck() {
        SharedPreferences preferences = host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        String location = preferences.getString(
                HelperAccessibilityService.PREF_TRAINING_LOCATION,
                HelperAccessibilityService.TRAINING_LOCATION_MARKER);
        if (HelperAccessibilityService.TRAINING_LOCATION_WILDERNESS.equals(location)) {
            wildernessZone = preferences.getInt(
                    HelperAccessibilityService.PREF_TRAINING_WILDERNESS_ZONE, 1);
            List<String> allowedMonsters = monstersForZone(wildernessZone);
            String selectedMonster = preferences.getString(
                    HelperAccessibilityService.PREF_TRAINING_MONSTER,
                    allowedMonsters.get(0));
            wildernessMonster = monsterName(allowedMonsters.contains(selectedMonster)
                    ? selectedMonster : allowedMonsters.get(0));
            startWilderness();
            return;
        }
        startMarker();
    }

    private void startMarker() {
        host.showProgress("自动练级：停止自动攻击后使用第一个标记卷");
        host.useFirstMarker(() -> {
            host.showProgress("自动练级：等待地图加载");
            host.postDelayed(() -> {
                host.showProgress("自动练级：等待自动攻击按钮");
                host.ensureAutoAttackEnabled(this::enterTraining);
            }, 5_000);
        });
    }

    private void startWilderness() {
        host.ensureAutoAttackDisabled(
                () -> wildernessNavigator.navigateToZone(
                        wildernessZone, this::selectMonster));
    }

    private void selectMonster() {
        host.showProgress("自动练级：选择目标怪物 " + wildernessMonster);
        host.tap(1130, 500,
                () -> host.tap(1015, 165,
                        () -> wildernessNavigator.navigateToMonster(
                                wildernessMonster,
                                () -> host.ensureAutoAttackEnabled(this::enterTraining))));
    }

    private void enterTraining() {
        long nextMilitaryAt = WorshipAlarmReceiver.scheduleMilitary(host.context());
        host.enterTraining(nextMilitaryAt);
    }

    static List<String> monstersForZone(int zone) {
        if (zone < 1 || zone > 15) {
            throw new IllegalArgumentException("zone must be between 1 and 15");
        }
        if (zone <= 3) {
            return List.of("60 食人花", "70 金甲龙", "75 圣武士");
        }
        if (zone <= 6) {
            return List.of("80 魔斗士", "90 海妖", "100 螳螂巨妖");
        }
        if (zone <= 9) {
            return List.of("115 九尾狐", "130 石狮精");
        }
        if (zone <= 12) {
            return List.of("145 人面鸟", "160 战鬼");
        }
        return List.of("175 式神童子", "190 剑齿虎");
    }

    static String defaultMonsterForZone(int zone) {
        return monstersForZone(zone).get(0);
    }

    static String monsterName(String value) {
        int separator = value.indexOf(' ');
        return separator < 0 ? value : value.substring(separator + 1);
    }

}
