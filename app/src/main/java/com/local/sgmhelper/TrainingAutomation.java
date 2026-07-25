package com.local.sgmhelper;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

final class TrainingAutomation {
    private static final int PULL_ROUTE_RETRY_COUNT = 90;
    private static final long PULL_ROUTE_CHECK_MS = 1_000;

    private final AutomationHost host;
    private final WildernessNavigator wildernessNavigator;
    private int wildernessZone;
    private String wildernessMonster;
    private List<PullPoint> pullRoute = List.of();
    private int pullRouteIndex;

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
        if (preferences.getBoolean(
                HelperAccessibilityService.PREF_TRAINING_PULL_FOR_OTHERS, false)) {
            try {
                pullRoute = parsePullRoute(preferences.getString(
                        HelperAccessibilityService.PREF_TRAINING_PULL_ROUTE, ""));
            } catch (IllegalArgumentException error) {
                host.failAutomation("为别人拉怪：路线格式错误");
                return;
            }
            if (pullRoute.isEmpty()) {
                host.failAutomation("为别人拉怪：请先设置拉怪路线");
                return;
            }
        } else {
            pullRoute = List.of();
        }
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
                startTrainingAtLocation();
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
        host.openAutoPathPanel(
                () -> host.tap(1015, 165,
                        () -> wildernessNavigator.navigateToMonster(
                                wildernessMonster,
                                this::startTrainingAtLocation)));
    }

    private void startTrainingAtLocation() {
        if (pullRoute.isEmpty()) {
            host.ensureAutoAttackEnabled(this::enterTraining);
            return;
        }
        host.ensureAutoAttackDisabled(this::startPullRoute);
    }

    private void startPullRoute() {
        pullRouteIndex = 0;
        long nextMilitaryAt = WorshipAlarmReceiver.scheduleMilitary(host.context());
        host.enterActiveTraining(nextMilitaryAt);
        moveToPullPoint();
    }

    private void moveToPullPoint() {
        if (!host.isAutomationRunning()) {
            return;
        }
        PullPoint point = pullRoute.get(pullRouteIndex);
        host.showProgress("为别人拉怪：前往 " + point.x + "," + point.y);
        host.ensureAutoAttackDisabled(() -> host.openAutoPathPanel(
                () -> host.tapMapCoordinate(point.x, point.y,
                        () -> waitForPullPoint(point, PULL_ROUTE_RETRY_COUNT))));
    }

    private void waitForPullPoint(PullPoint point, int remainingAttempts) {
        host.recognizeMapCoordinate(value -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (pullPointReached(value, point)) {
                host.closeAutoPathPanel(() -> stayAtPullPoint(point));
            } else if (remainingAttempts > 1) {
                host.postDelayed(
                        () -> waitForPullPoint(point, remainingAttempts - 1),
                        PULL_ROUTE_CHECK_MS);
            } else {
                host.failAutomation("为别人拉怪：前往 "
                        + point.x + "," + point.y + " 超时");
            }
        });
    }

    private void stayAtPullPoint(PullPoint point) {
        host.showProgress("为别人拉怪：到达 " + point.x + "," + point.y
                + (point.attack ? "，攻击" : "，不攻击")
                + "并停留 " + point.durationMillis + " 毫秒");
        Runnable wait = () -> host.postDelayed(this::advancePullPoint, point.durationMillis);
        if (point.attack) {
            host.ensureAutoAttackEnabled(wait);
        } else {
            wait.run();
        }
    }

    private void advancePullPoint() {
        pullRouteIndex = (pullRouteIndex + 1) % pullRoute.size();
        moveToPullPoint();
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

    static List<PullPoint> parsePullRoute(String value) {
        List<PullPoint> route = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return route;
        }
        for (String row : value.replace('，', ',').split("[;\\n]+")) {
            if (row.trim().isEmpty()) {
                continue;
            }
            String[] fields = row.trim().split("\\s*,\\s*");
            if (fields.length < 3 || fields.length > 4) {
                throw new IllegalArgumentException("每行必须是 X,Y,停留毫秒,是否攻击(1/0)");
            }
            try {
                int x = Integer.parseInt(fields[0]);
                int y = Integer.parseInt(fields[1]);
                long durationMillis = Long.parseLong(fields[2]);
                boolean attack = fields.length == 3 || "1".equals(fields[3]);
                if (x < 0 || x > AutomationHost.MAP_GAME_MAX_X
                        || y < 0 || y > AutomationHost.MAP_GAME_MAX_Y) {
                    throw new IllegalArgumentException("坐标范围：X 0–600，Y 0–50");
                }
                if (durationMillis < 100 || durationMillis > 60_000) {
                    throw new IllegalArgumentException("停留时间范围：100–60000 毫秒");
                }
                if (fields.length == 4
                        && !"0".equals(fields[3]) && !"1".equals(fields[3])) {
                    throw new IllegalArgumentException("是否攻击只能填写 1 或 0");
                }
                route.add(new PullPoint(x, y, durationMillis, attack));
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("路线只能填写数字", error);
            }
        }
        return route;
    }

    static boolean pullPointReached(String coordinate, PullPoint point) {
        int[] current = BossAutomation.parseMapCoordinate(coordinate);
        return current != null
                && Math.abs(current[0] - point.x) <= 2
                && Math.abs(current[1] - point.y) <= 2;
    }

    static final class PullPoint {
        final int x;
        final int y;
        final long durationMillis;
        final boolean attack;

        PullPoint(int x, int y, long durationMillis, boolean attack) {
            this.x = x;
            this.y = y;
            this.durationMillis = durationMillis;
            this.attack = attack;
        }
    }

}
