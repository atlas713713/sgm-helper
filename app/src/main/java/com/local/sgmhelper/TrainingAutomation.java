package com.local.sgmhelper;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

final class TrainingAutomation {
    private static final int PULL_ROUTE_RETRY_COUNT = 90;
    private static final long PULL_ROUTE_CHECK_MS = 1_000;
    /** 这张图的怪不会自动打，得在引路面板里手动选目标。 */
    private static final String CHARIOT_MAP = "铁门峡二层";
    private static final String CHARIOT_ENEMY = "黄金刀车";
    private static final int CHARIOT_RETRY_COUNT = 10;
    /** 引路面板的“敌人”页签，和荒野选怪用的是同一个按钮。 */
    private static final int GUIDE_ENEMY_TAB_X = 1015;
    private static final int GUIDE_ENEMY_TAB_Y = 165;

    private final AutomationHost host;
    private final WildernessNavigator wildernessNavigator;
    private int wildernessZone;
    private String wildernessMonster;
    private CustomCoordinate customTrainingCoordinate;
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
        customTrainingCoordinate = null;
        if (HelperAccessibilityService.TRAINING_LOCATION_WILDERNESS.equals(location)) {
            wildernessZone = preferences.getInt(
                    HelperAccessibilityService.PREF_TRAINING_WILDERNESS_ZONE, 1);
            List<String> allowedMonsters = monstersForZone(wildernessZone);
            String selectedMonster = preferences.getString(
                    HelperAccessibilityService.PREF_TRAINING_MONSTER,
                    allowedMonsters.get(0));
            wildernessMonster = monsterName(allowedMonsters.contains(selectedMonster)
                    ? selectedMonster : allowedMonsters.get(0));
            customTrainingCoordinate = customCoordinateFromPreferences(preferences);
            if (preferences.getBoolean(
                    HelperAccessibilityService.PREF_TRAINING_CUSTOM_COORDINATE_ENABLED,
                    false) && customTrainingCoordinate == null) {
                host.showProgress("自动练级：自定义坐标无效，使用目标怪物区域");
            }
            startWilderness();
            return;
        }
        startMarker();
    }

    private void startMarker() {
        host.showProgress("自动练级：停止自动攻击后使用第一个标记卷");
        host.useFirstMarker(() -> {
            host.showProgress("自动练级：等待地图加载");
            host.postDelayed(this::checkMarkerMap, 5_000);
        });
    }

    /** 标记点落地后先看是哪张图：铁门峡二层要自己在引路列表里点黄金刀车。 */
    private void checkMarkerMap() {
        host.showProgress("自动练级：识别标记点地图");
        host.recognizeMapName(mapName -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            DiagnosticLog.info("TRAINING", "marker map=" + mapName);
            if (!isChariotMap(mapName)) {
                host.showProgress("自动练级：等待自动攻击按钮");
                startTrainingAtLocation();
                return;
            }
            host.showProgress("自动练级：" + CHARIOT_MAP + "，改打" + CHARIOT_ENEMY);
            selectChariot();
        });
    }

    private void selectChariot() {
        host.openAutoPathPanel(() -> host.tap(GUIDE_ENEMY_TAB_X, GUIDE_ENEMY_TAB_Y,
                () -> host.clickRightText(CHARIOT_ENEMY, () -> {
                    host.showProgress("自动练级：已选中" + CHARIOT_ENEMY);
                    startTrainingAtLocation();
                }, CHARIOT_RETRY_COUNT, () -> {
                    // 刀车没刷出来时照常练级，不把整条主线判死。
                    DiagnosticLog.warn("TRAINING", "chariot not in the guide list");
                    host.showProgress("自动练级：未找到" + CHARIOT_ENEMY + "，按普通练级继续");
                    host.closeAutoPathPanel(this::startTrainingAtLocation);
                })));
    }

    /** 地图名 OCR 偶尔会带空格或多认一两个字，包含即可。 */
    static boolean isChariotMap(String mapName) {
        return mapName != null && mapName.replaceAll("\\s", "").contains(CHARIOT_MAP);
    }

    private void startWilderness() {
        host.ensureAutoAttackDisabled(
                () -> wildernessNavigator.navigateToZone(
                        wildernessZone, this::selectMonster));
    }

    private void selectMonster() {
        if (customTrainingCoordinate != null) {
            host.showProgress("自动练级：前往自定义坐标 "
                    + customTrainingCoordinate.x + "," + customTrainingCoordinate.y);
            host.openAutoPathPanel(() -> wildernessNavigator.navigateToCoordinate(
                    customTrainingCoordinate.x, customTrainingCoordinate.y,
                    this::startTrainingAtLocation));
            return;
        }
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
        if (host.handlePendingGear(this::resumeAfterGearHandle)) {
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

    void resumeAfterGearHandle() {
        start();
    }

    /** 荒野每三个区共用一套怪，数据取自武当 {@code arrays.xml} 的 {@code wild} 表。 */
    static List<String> monstersForZone(int zone) {
        List<WildernessCatalog.Enemy> enemies = WildernessCatalog.enemies(zone);
        if (enemies.isEmpty()) {
            throw new IllegalArgumentException(
                    "zone must be between 1 and " + WildernessCatalog.MAX_ZONE);
        }
        List<String> values = new ArrayList<>();
        for (WildernessCatalog.Enemy enemy : enemies) {
            values.add(enemy.level + " " + enemy.name);
        }
        return values;
    }

    static String defaultMonsterForZone(int zone) {
        return monstersForZone(zone).get(0);
    }

    static String monsterName(String value) {
        int separator = value.indexOf(' ');
        return separator < 0 ? value : value.substring(separator + 1);
    }

    /**
     * 武当的荒野练级逻辑：勾选自定义坐标且 X/Y 都有效时，优先使用该坐标；
     * 否则回退到目标怪物区域。
     */
    static CustomCoordinate customCoordinateFromPreferences(SharedPreferences preferences) {
        if (!preferences.getBoolean(
                HelperAccessibilityService.PREF_TRAINING_CUSTOM_COORDINATE_ENABLED, false)) {
            return null;
        }
        int x = preferences.getInt(
                HelperAccessibilityService.PREF_TRAINING_CUSTOM_COORDINATE_X, 0);
        int y = preferences.getInt(
                HelperAccessibilityService.PREF_TRAINING_CUSTOM_COORDINATE_Y, 0);
        if (x <= 0 || y <= 0 || x > AutomationHost.MAP_GAME_MAX_X
                || y > AutomationHost.MAP_GAME_MAX_Y) {
            return null;
        }
        return new CustomCoordinate(x, y);
    }

    static CustomCoordinate parseCustomCoordinate(String xValue, String yValue) {
        if (xValue == null || yValue == null
                || xValue.trim().isEmpty() || yValue.trim().isEmpty()) {
            throw new IllegalArgumentException("X/Y 坐标不能为空");
        }
        try {
            return parseCustomCoordinate(Integer.parseInt(xValue.trim()),
                    Integer.parseInt(yValue.trim()));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("坐标只能填写数字", error);
        }
    }

    static CustomCoordinate parseCustomCoordinate(int x, int y) {
        if (x <= 0 || x > AutomationHost.MAP_GAME_MAX_X
                || y <= 0 || y > AutomationHost.MAP_GAME_MAX_Y) {
            throw new IllegalArgumentException("坐标范围：X 1–599，Y 1–49");
        }
        return new CustomCoordinate(x, y);
    }

    static boolean coordinateReached(String coordinate, int targetX, int targetY) {
        int[] current = BossAutomation.parseMapCoordinate(coordinate);
        return current != null
                && Math.abs(current[0] - targetX) <= 2
                && Math.abs(current[1] - targetY) <= 2;
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
                    throw new IllegalArgumentException("坐标范围：X 0–599，Y 0–49");
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
        return coordinateReached(coordinate, point.x, point.y);
    }

    static final class CustomCoordinate {
        final int x;
        final int y;

        CustomCoordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }
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
