package com.local.sgmhelper;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runs the dungeon itself. This is separate from {@link DungeonSweepAutomation}. */
final class DungeonBattleAutomation {
    private static final Pattern INTEGER = Pattern.compile("\\d+");
    private static final int CAMP_MAP_MAX_X = 199;
    private static final int PALACE_MAP_MAX_X = 399;
    private static final int DUNGEON_MAP_MAX_X = 599;
    private static final int NPC_SHORTCUT_X = 1208;
    private static final int NPC_SHORTCUT_Y = 410;
    // Wudang's NPCDialog bounds at 1280x720. Only OCR the title so the progress bar
    // and option text can never be mistaken for an open NPC dialog.
    private static final int NPC_TITLE_LEFT = 120;
    private static final int NPC_TITLE_TOP = 195;
    private static final int NPC_TITLE_RIGHT = 390;
    private static final int NPC_TITLE_BOTTOM = 245;
    private static final int NPC_OPTION_X = 251;
    private static final int NPC_OPTION_4_X = 176;
    private static final int[] NPC_OPTION_Y = {0, 450, 512, 574, 636};
    private static final int GUIDE_UP_X = 974;
    private static final int GUIDE_UP_Y = 31;
    private static final int[] GUIDE_UP_ENEMY_CENTERS = {151, 206, 261, 316, 371};
    private static final int[] GUIDE_DOWN_ENEMY_CENTERS = {294, 349, 404};
    private static final int GUIDE_ROW_LEFT = 950;
    private static final int GUIDE_ROW_RIGHT = 1235;
    private static final int GUIDE_STEP_MIN = 40;
    private static final int GUIDE_STEP_MAX_EXCLUSIVE = 50;
    private static final int ROUTE_MAX_CHECKS = 30;
    private static final int ROUTE_RETRY_INTERVAL = 5;
    private static final int ROUTE_TOLERANCE = 3;
    private static final long MAP_WAIT_MS = 5_000;
    private static final long FIGHT_CHECK_MS = 4_000;

    private final AutomationHost host;
    private List<Integer> selectedLevels = Collections.emptyList();
    private int currentIndex;
    private int lastX = -1;
    private int samePositionCount;
    private int routeChecks;

    DungeonBattleAutomation(AutomationHost host) {
        this.host = host;
    }

    void start(List<Integer> levels) {
        if (host.isAutomationRunning()) {
            host.showProgress("已有任务正在运行");
            return;
        }
        if (levels.isEmpty()) {
            host.showProgress("请先选择需要实战的副本");
            return;
        }
        for (int level : levels) {
            if (level != 10 && level != 20 && level != 30 && level != 40 && level != 50
                    && level != 60 && level != 65 && level != 70 && level != 75) {
                host.showProgress("实战副本：目前支持 10–75 级，暂不执行 "
                        + level + " 级");
                return;
            }
        }
        selectedLevels = new ArrayList<>(levels);
        host.startPrimaryAutomation(
                AutomationHost.PrimaryTask.DUNGEON, "实战副本：打开游戏", this::begin);
    }

    private void begin() {
        currentIndex = 0;
        returnHomeForDungeon();
    }

    private void returnHomeForDungeon() {
        host.showProgress(currentLevel() + "级副本：先回城");
        host.closeAutoPathPanel(() -> host.returnHome(() -> host.postDelayed(
                () -> host.waitForMapReady(20, () -> readHomeCity(10)), MAP_WAIT_MS)));
    }

    private void readHomeCity(int remainingAttempts) {
        host.showProgress(currentLevel() + "级副本：识别回城地点");
        host.recognizeMapName(mapName -> {
            String normalized = mapName == null ? "" : mapName.replaceAll("\\s", "");
            if (!normalized.isEmpty()) {
                gotoPalaceGuide(normalized);
            } else if (remainingAttempts > 1) {
                host.postDelayed(() -> readHomeCity(remainingAttempts - 1), 1_000);
            } else {
                host.failAutomation(currentLevel() + "级副本：回城后未识别到城市");
            }
        });
    }

    private void gotoPalaceGuide(String cityName) {
        host.showProgress(currentLevel() + "级副本：前往驻地引路员");
        routeChecks = 0;
        tapPalaceGuideRoute(cityName, 2);
    }

    private void tapPalaceGuideRoute(String cityName, int remainingTaps) {
        int x = palaceGuideX(cityName);
        int y = palaceGuideY(cityName);
        host.tapMapCoordinate(x, y, PALACE_MAP_MAX_X, () -> {
            if (remainingTaps > 1) {
                host.postDelayed(() -> tapPalaceGuideRoute(cityName, remainingTaps - 1), 500);
            } else {
                host.postDelayed(() -> waitForPalaceGuide(cityName), 1_000);
            }
        });
    }

    private void waitForPalaceGuide(String cityName) {
        int x = palaceGuideX(cityName);
        int y = palaceGuideY(cityName);
        host.recognizeMapCoordinate(value -> {
            if (isAtCoordinate(value, x, y)) {
                openPalaceGuide();
                return;
            }
            routeChecks++;
            if (routeChecks >= ROUTE_MAX_CHECKS) {
                host.failAutomation(currentLevel() + "级副本：未能到达驻地引路员");
            } else if (routeChecks % ROUTE_RETRY_INTERVAL == 0) {
                tapPalaceGuideRoute(cityName, 2);
            } else {
                host.postDelayed(() -> waitForPalaceGuide(cityName), 1_000);
            }
        });
    }

    private void openPalaceGuide() {
        host.showProgress(currentLevel() + "级副本：进入副本宫殿（一般）");
        host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y,
                () -> clickNpcOption("驻地引路员", 1,
                        () -> host.postDelayed(() -> waitForPalace(20), 3_000), 10));
    }

    private void waitForPalace(int remainingAttempts) {
        host.recognizeMapName(mapName -> {
            String normalized = mapName == null ? "" : mapName.replaceAll("\\s", "");
            if (normalized.contains("副本宫殿") && normalized.contains("一般")) {
                gotoCampNpc();
            } else if (remainingAttempts > 1) {
                host.postDelayed(() -> waitForPalace(remainingAttempts - 1), 1_000);
            } else {
                host.failAutomation(currentLevel() + "级副本：未进入副本宫殿（一般）");
            }
        });
    }

    private void gotoCampNpc() {
        host.showProgress(currentLevel() + "级副本：前往" + campName() + "入口");
        routeChecks = 0;
        tapCampNpcRoute(2);
    }

    private void tapCampNpcRoute(int remainingTaps) {
        host.tapMapCoordinate(campNpcX(currentLevel()), 16, PALACE_MAP_MAX_X, () -> {
            if (remainingTaps > 1) {
                host.postDelayed(() -> tapCampNpcRoute(remainingTaps - 1), 500);
            } else {
                host.postDelayed(this::waitForCampNpc, 1_000);
            }
        });
    }

    private void waitForCampNpc() {
        int targetX = campNpcX(currentLevel());
        host.recognizeMapCoordinate(value -> {
            if (isAtCoordinate(value, targetX, 16)) {
                openCampNpc();
                return;
            }
            routeChecks++;
            if (routeChecks >= ROUTE_MAX_CHECKS) {
                host.failAutomation(currentLevel() + "级副本：未能到达" + campName() + "入口");
            } else if (routeChecks % ROUTE_RETRY_INTERVAL == 0) {
                tapCampNpcRoute(2);
            } else {
                host.postDelayed(this::waitForCampNpc, 1_000);
            }
        });
    }

    private void openCampNpc() {
        host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y,
                () -> clickNpcSequence(dungeonName(), new int[] {4, 3},
                        () -> host.postDelayed(() -> waitForCamp(20), 3_000)));
    }

    private void waitForCamp(int remainingAttempts) {
        host.recognizeMapName(mapName -> {
            String normalized = mapName == null ? "" : mapName.replaceAll("\\s", "");
            if (normalized.contains(campName())) {
                gotoEntryNpc();
            } else if (remainingAttempts > 1) {
                host.postDelayed(() -> waitForCamp(remainingAttempts - 1), 1_000);
            } else {
                host.failAutomation(currentLevel() + "级副本：未进入" + campName());
            }
        });
    }

    private void gotoEntryNpc() {
        host.showProgress(currentLevel() + "级副本：前往入口 NPC");
        routeChecks = 0;
        tapEntryNpcRoute(2);
    }

    private void tapEntryNpcRoute(int remainingTaps) {
        host.tapMapCoordinate(entryNpcX(currentLevel()), entryNpcY(currentLevel()),
                CAMP_MAP_MAX_X, () -> {
                    if (remainingTaps > 1) {
                        host.postDelayed(() -> tapEntryNpcRoute(remainingTaps - 1), 500);
                    } else {
                        host.postDelayed(this::waitForEntryNpc, 1_000);
                    }
                });
    }

    private void waitForEntryNpc() {
        host.recognizeMapCoordinate(value -> {
            if (isAtEntryNpc(currentLevel(), value)) {
                openEntryNpc();
                return;
            }
            routeChecks++;
            if (routeChecks >= ROUTE_MAX_CHECKS) {
                host.failAutomation(currentLevel() + "级副本：未能寻路到入口 NPC");
            } else if (routeChecks % ROUTE_RETRY_INTERVAL == 0) {
                host.showProgress(currentLevel() + "级副本：重新前往入口 NPC");
                tapEntryNpcRoute(2);
            } else {
                host.postDelayed(this::waitForEntryNpc, 1_000);
            }
        });
    }

    private void openEntryNpc() {
        host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y,
                () -> clickNpcSequence(
                        entryNpcName(), entryNpcRows(currentLevel()), this::waitForDungeon));
    }

    private void clickNpcSequence(
            String expectedTitle, int[] rows, Runnable next) {
        clickNpcSequence(expectedTitle, rows, 0, next);
    }

    private void clickNpcSequence(
            String expectedTitle, int[] rows, int index, Runnable next) {
        clickNpcOption(expectedTitle, rows[index], () -> {
            if (index + 1 == rows.length) {
                next.run();
            } else {
                host.postDelayed(
                        () -> clickNpcSequence(expectedTitle, rows, index + 1, next),
                        1_000);
            }
        }, 8);
    }

    private void clickNpcOption(
            String expectedTitle, int row, Runnable next, int remainingAttempts) {
        host.recognizeTextRegion(
                NPC_TITLE_LEFT, NPC_TITLE_TOP, NPC_TITLE_RIGHT, NPC_TITLE_BOTTOM,
                text -> {
                    String recognized = text == null ? "" : text.getText();
                    DiagnosticLog.info("Dungeon", "NPC title OCR: " + recognized);
                    if (matchesNpcDialogTitle(recognized, expectedTitle)) {
                        host.tap(npcOptionX(row), npcOptionY(row), next);
                    } else if (remainingAttempts > 1) {
                        host.postDelayed(() -> clickNpcOption(
                                expectedTitle, row, next, remainingAttempts - 1), 500);
                    } else {
                        host.failAutomation(currentLevel() + "级副本：未识别到“"
                                + expectedTitle + "”对话框");
                    }
                });
    }

    private void waitForDungeon() {
        host.showProgress(currentLevel() + "级副本：等待副本地图");
        host.postDelayed(() -> host.waitForMapReady(20, this::openGuide), MAP_WAIT_MS);
    }

    private void openGuide() {
        host.openAutoPathPanel(() -> selectGuideSide(() -> {
            lastX = -1;
            samePositionCount = 0;
            inspectPosition();
        }));
    }

    private void selectGuideSide(Runnable next) {
        if (usesGuideUp(currentLevel())) {
            host.tap(GUIDE_UP_X, GUIDE_UP_Y, next);
        } else {
            next.run();
        }
    }

    private void inspectPosition() {
        host.showProgress(currentLevel() + "级副本：读取当前位置");
        host.recognizeMapCoordinate(value -> {
            int[] coordinate = BossAutomation.parseMapCoordinate(value);
            if (coordinate == null || coordinate[0] > dungeonMapMaxX()) {
                host.postDelayed(this::inspectPosition, 1_000);
                return;
            }
            int x = coordinate[0];
            int y = coordinate[1];
            samePositionCount = x == lastX ? samePositionCount + 1 : 0;
            lastX = x;
            execute(decide(currentLevel(), x, y));
        });
    }

    private void execute(RouteDecision decision) {
        if (decision.exit) {
            gotoExit();
        } else if (decision.shortcutClicks > 0) {
            host.showProgress(currentLevel() + "级副本：" + decision.interactionText);
            tapNpcShortcut(decision.shortcutClicks);
        } else if (decision.interactionText != null) {
            host.showProgress(currentLevel() + "级副本：" + decision.interactionText);
            Runnable click = () -> clickNpcOption(
                    interactionNpcName(), interactionNpcRow(),
                    () -> host.postDelayed(this::inspectPosition, 2_000), 8);
            if (decision.openNpc) {
                host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y, click);
            } else {
                click.run();
            }
        } else if (decision.searchEnemies) {
            findEnemy(decision);
        } else {
            move(decision);
        }
    }

    private void tapNpcShortcut(int remainingClicks) {
        host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y, () -> {
            if (remainingClicks > 1) {
                host.postDelayed(() -> tapNpcShortcut(remainingClicks - 1), 1_000);
            } else {
                host.postDelayed(this::inspectPosition, 2_000);
            }
        });
    }

    private void findEnemy(RouteDecision decision) {
        host.showProgress(currentLevel() + "级副本：搜索 " + decision.enemyDescription());
        host.recognizeDungeonText(lines -> {
            EnemyRow enemy = selectEnemyRow(
                    readEnemyRows(lines, usesGuideUp(currentLevel())), decision.enemyLevels,
                    decision.priorityEnemyNames, decision.protectName,
                    decision.acceptAnyEnemy);
            if (enemy == null) {
                if (decision.exitWhenNoEnemy) {
                    gotoExit();
                } else {
                    move(decision);
                }
                return;
            }
            host.showProgress(currentLevel() + "级副本：攻击 " + enemy.label);
            host.tap(enemy.bounds.centerX(), enemy.bounds.centerY(),
                    () -> host.ensureAutoAttackEnabled(
                            () -> host.postDelayed(this::inspectPosition, FIGHT_CHECK_MS)));
        });
    }

    private void move(RouteDecision decision) {
        if (decision.waypoints.length > 0) {
            moveWaypoints(decision.waypoints, 0);
            return;
        }
        if (samePositionCount >= 3 && decision.unstuckX > 0) {
            samePositionCount = 0;
            host.showProgress(currentLevel() + "级副本：脱离门点 " + lastX);
            host.tapMapCoordinate(decision.unstuckX, 25, dungeonMapMaxX(),
                    () -> host.postDelayed(this::inspectPosition, 2_000));
            return;
        }
        host.showProgress(currentLevel() + "级副本：前往 "
                + decision.targetX + "," + decision.targetY);
        host.tapMapCoordinate(decision.targetX, decision.targetY, dungeonMapMaxX(),
                () -> host.postDelayed(this::inspectPosition, 4_000));
    }

    private void moveWaypoints(int[][] points, int index) {
        int[] point = points[index];
        host.showProgress(currentLevel() + "级副本：前往 "
                + point[0] + "," + point[1]);
        host.tapMapCoordinate(point[0], point[1], dungeonMapMaxX(), () -> {
            if (index + 1 < points.length) {
                host.postDelayed(() -> moveWaypoints(points, index + 1), 2_000);
            } else {
                host.postDelayed(this::inspectPosition, 4_000);
            }
        });
    }

    private void gotoExit() {
        RouteDecision exit = decide(currentLevel(), 0);
        host.showProgress(currentLevel() + "级副本：前往出口 "
                + exit.targetX + "," + exit.targetY);
        host.tapMapCoordinate(exit.targetX, exit.targetY, dungeonMapMaxX(),
                () -> host.postDelayed(() -> host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y,
                        this::claimReward), MAP_WAIT_MS));
    }

    private void claimReward() {
        host.showProgress(currentLevel() + "级副本：离开并领取奖励");
        clickNpcSequence(
                exitNpcName(), exitNpcRows(currentLevel()), this::finishCurrent);
    }

    private void finishCurrent() {
        currentIndex++;
        if (currentIndex < selectedLevels.size()) {
            host.postDelayed(this::returnHomeForDungeon, MAP_WAIT_MS);
        } else {
            host.completeAutomation();
        }
    }

    static RouteDecision decide10(int x) {
        if (x >= 504) {
            return RouteDecision.search(scanLeft(x, 505), 27, 550, false, 7, 10);
        }
        if (x >= 386) {
            return RouteDecision.search(scanLeft(x, 387), 27, 450, false, 7, 10);
        }
        if (x >= 168) {
            return RouteDecision.search(scanLeft(x, 170), 27, 0, false, 10);
        }
        if (x >= 39) {
            return RouteDecision.search(scanLeft(x, 38), 27, 110, false, 10);
        }
        return RouteDecision.exit(25, 27);
    }

    static RouteDecision decide20(int x) {
        if (x >= 478) {
            return RouteDecision.search(scanLeft(x, 480), 27, 550, false, 18, 20);
        }
        if (x >= 348) {
            return RouteDecision.move(350, 27, 0);
        }
        if (x >= 206) {
            return RouteDecision.search(scanLeft(x, 211), 27, 280, true, 20);
        }
        if (x >= 91) {
            return RouteDecision.search(scanLeft(x, 91), 27, 150, false, 20);
        }
        if (x > 31) {
            return RouteDecision.search(scanLeft(x, 31), 27, 80, true, 20);
        }
        return RouteDecision.exit(22, 27);
    }

    static RouteDecision decide(int level, int x) {
        return decide(level, x, 25);
    }

    static RouteDecision decide(int level, int x, int y) {
        if (level == 10) {
            return decide10(x);
        }
        if (level == 20) {
            return decide20(x);
        }
        if (level == 30) {
            return decide30(x);
        }
        if (level == 40) {
            return decide40(x);
        }
        if (level == 50) {
            return decide50(x, y);
        }
        if (level == 60) {
            return decide60(x, y);
        }
        if (level == 65) {
            return decide65(x, y);
        }
        if (level == 70) {
            return decide70(x);
        }
        if (level == 75) {
            return decide75(x);
        }
        throw new IllegalArgumentException("Unsupported dungeon level " + level);
    }

    static RouteDecision decide30(int x) {
        if (x >= 451) {
            return RouteDecision.search(scanLeft(x, 458), 4, 570, false, 30);
        }
        if (x >= 301) {
            return RouteDecision.search(scanLeft(x, 304), 27, 420, false, 30);
        }
        if (x >= 151) {
            return RouteDecision.searchNamed(scanLeft(x, 174), 4, 0,
                    List.of("董军阵旗"), null);
        }
        if (x >= 33) {
            return RouteDecision.searchNamed(scanLeft(x, 32), 27, 100,
                    List.of("董军阵旗"), null);
        }
        return RouteDecision.exit(24, 27);
    }

    static RouteDecision decide40(int x) {
        if (x >= 32) {
            return RouteDecision.search(scanLeft(x, 32), 25, 0, true);
        }
        return RouteDecision.exit(25, 25);
    }

    static RouteDecision decide50(int x) {
        return decide50(x, 25);
    }

    static RouteDecision decide50(int x, int y) {
        if (x <= 49) {
            return RouteDecision.moveRoute(
                    new int[][] {{46, 6}, {46, 45}, {60, 45}});
        }
        if (x <= 110) {
            return RouteDecision.search(scanRight(x, 112), 25, 0, true);
        }
        if (x <= 194) {
            return RouteDecision.search(scanRight(x, 205), 25, 0, true);
        }
        if (x <= 287) {
            return RouteDecision.search(scanRight(x, 291), 25, 0, true);
        }
        if (x <= 308) {
            return RouteDecision.interact("交给我吧");
        }
        if (x <= 388) {
            return RouteDecision.search(scanRight(x, 399), 25, 0, true);
        }
        if (x <= 484) {
            return RouteDecision.search(scanRight(x, 495), 25, 0, true);
        }
        if (x <= 581) {
            return RouteDecision.search(scanRight(x, 583), 25, 0, true);
        }
        return RouteDecision.exit(570, 25);
    }

    private static int scanLeft(int currentX, int gateX) {
        return Math.max(gateX, currentX - randomGuideStep());
    }

    private static int scanRight(int currentX, int gateX) {
        return Math.min(gateX, currentX + randomGuideStep());
    }

    static RouteDecision decide60(int x) {
        return decide60(x, 25);
    }

    static RouteDecision decide60(int x, int y) {
        if (x <= 98) {
            return RouteDecision.searchRoute(new int[][] {{71, 10}, {71, 4}});
        }
        if (x >= 505 && x <= 528) {
            return RouteDecision.shortcut(1, "离开第一战斗房");
        }
        if (x >= 105 && x <= 198) {
            return RouteDecision.searchRoute(new int[][] {{70, 40}, {70, 46}});
        }
        if (x >= 531 && x <= 553) {
            return RouteDecision.shortcut(1, "离开第二战斗房");
        }
        if (x >= 205 && x <= 298) {
            return RouteDecision.searchRoute(new int[][] {{90, 27}, {94, 27}});
        }
        if (x >= 305 && x <= 398) {
            return RouteDecision.searchRoute(new int[][] {{380, 27}, {394, 27}});
        }
        if (x >= 556 && x <= 581) {
            if (isNear(x, y, 570, 27)) {
                return RouteDecision.shortcut(2, "通过中央传送点");
            }
            return RouteDecision.move(570, 27, 0);
        }
        if (x >= 405 && x <= 479) {
            return RouteDecision.search(486, 25, 0, true);
        }
        return RouteDecision.exit(486, 27);
    }

    static RouteDecision decide65(int x) {
        return decide65(x, 25);
    }

    static RouteDecision decide65(int x, int y) {
        if (x <= 58) {
            if (x >= 30 && x <= 40) {
                return RouteDecision.interactNpc("出击");
            }
            return RouteDecision.move(34, 22, 0);
        }
        if (x >= 79 && x <= 230) {
            return RouteDecision.search(245, 27, 0, true, 64);
        }
        if (x <= 250) {
            return RouteDecision.move(252, 27, 0);
        }
        if (x <= 437) {
            return RouteDecision.searchRoute(
                    new int[][] {{409, 30}, {410, 10}, {410, 4}}, 64);
        }
        if (x <= 537) {
            return RouteDecision.search(538, 27, 0, true, 64);
        }
        return RouteDecision.exit(560, 27);
    }

    private static int randomGuideStep() {
        return ThreadLocalRandom.current().nextInt(
                GUIDE_STEP_MIN, GUIDE_STEP_MAX_EXCLUSIVE);
    }

    private static boolean isNear(int x, int y, int targetX, int targetY) {
        return Math.abs(x - targetX) <= ROUTE_TOLERANCE
                && Math.abs(y - targetY) <= ROUTE_TOLERANCE;
    }

    static RouteDecision decide70(int x) {
        if (x < 120) {
            return RouteDecision.searchNamed(115, 27, 0,
                    List.of("长安城驻军队长", "长安白虎大门"), "马车");
        }
        if (x <= 568) {
            return RouteDecision.searchThenExit(560, 27,
                    List.of("长安城驻军队长", "长安白虎大门"), "马车");
        }
        return RouteDecision.exit(560, 27);
    }

    static RouteDecision decide75(int x) {
        List<String> priority = List.of("吕虔", "于禁", "李典", "夏侯渊");
        if (x < 65) {
            return RouteDecision.searchNamed(73, 27, 0, priority, null);
        }
        if (x <= 78) {
            return RouteDecision.move(73, 27, 0);
        }
        if (x < 130) {
            return RouteDecision.searchNamed(130, 27, 0, priority, null);
        }
        if (x < 180) {
            return RouteDecision.searchNamed(187, 27, 0, priority, null);
        }
        return RouteDecision.exit(187, 27);
    }

    private int currentLevel() {
        return selectedLevels.get(currentIndex);
    }

    private String dungeonName() {
        switch (currentLevel()) {
            case 10:
                return "桃园结义";
            case 20:
                return "枭雄崛起";
            case 30:
                return "三公战华雄";
            case 40:
                return "十常侍之乱";
            case 50:
                return "孙坚与玉玺";
            case 60:
                return "界桥之战";
            case 65:
                return "三英战吕布";
            case 70:
                return "二狼劫献帝";
            case 75:
                return "濮阳之战";
            default:
                throw new IllegalArgumentException("Unsupported dungeon level " + currentLevel());
        }
    }

    private String entryNpcName() {
        switch (currentLevel()) {
            case 10:
                return "邹靖";
            case 20:
                return "曹嵩";
            case 30:
                return "黄巾太平道长|乱军太平道长";
            case 40:
                return "长乐宫官吏";
            case 50:
                return "历战老兵";
            case 60:
            case 65:
                return "说书人";
            case 70:
                return "毛玠";
            case 75:
                return "吕布";
            default:
                throw new IllegalArgumentException("Unsupported dungeon level " + currentLevel());
        }
    }

    private String exitNpcName() {
        switch (currentLevel()) {
            case 10:
                return "义勇军刘玄德";
            case 20:
                return "骑都尉曹操";
            case 30:
                return "黄巾太平道长|乱军太平道长";
            case 40:
                return "袁绍";
            case 50:
                return "孙坚";
            case 60:
                return "公孙瓒";
            case 65:
                return "刘备";
            case 70:
                return "曹操";
            case 75:
                return "陈宫";
            default:
                throw new IllegalArgumentException("Unsupported dungeon level " + currentLevel());
        }
    }

    private String interactionNpcName() {
        return currentLevel() == 65 ? "刘备" : "孙坚";
    }

    private int interactionNpcRow() {
        return currentLevel() == 65 ? 3 : 4;
    }

    private String campName() {
        if (currentLevel() == 20) {
            return "曹公府";
        }
        if (currentLevel() == 30) {
            return "乱军营寨";
        }
        if (currentLevel() == 40) {
            return "长乐宫";
        }
        if (currentLevel() == 50) {
            return "追忆之地";
        }
        if (currentLevel() == 60) {
            return "盘河桥西";
        }
        if (currentLevel() == 65) {
            return "虎牢关近郊";
        }
        if (currentLevel() == 70) {
            return "安邑郊外";
        }
        return currentLevel() == 75 ? "濮阳城外" : "幽州边境";
    }

    static int entryNpcX(int level) {
        return level == 60 ? 102 : 100;
    }

    static int entryNpcY(int level) {
        return level == 70 ? 17 : 16;
    }

    static int palaceGuideX(String cityName) {
        return isCapitalCity(cityName) ? 200 : 210;
    }

    static int palaceGuideY(String cityName) {
        return isCapitalCity(cityName) ? 31 : 33;
    }

    private static boolean isCapitalCity(String cityName) {
        String value = cityName == null ? "" : cityName;
        return value.contains("洛阳") || value.contains("成都") || value.contains("建业");
    }

    static int campNpcX(int level) {
        switch (level) {
            case 10:
                return 26;
            case 20:
                return 42;
            case 30:
                return 58;
            case 40:
                return 73;
            case 50:
                return 89;
            case 60:
                return 105;
            case 65:
                return 121;
            case 70:
                return 136;
            case 75:
                return 152;
            default:
                throw new IllegalArgumentException("Unsupported dungeon level " + level);
        }
    }

    static boolean isAtEntryNpc(int level, String coordinateText) {
        return isAtCoordinate(coordinateText, entryNpcX(level), entryNpcY(level));
    }

    static boolean isAtCoordinate(String coordinateText, int targetX, int targetY) {
        int[] coordinate = BossAutomation.parseMapCoordinate(coordinateText);
        return coordinate != null
                && Math.abs(coordinate[0] - targetX) <= ROUTE_TOLERANCE
                && Math.abs(coordinate[1] - targetY) <= ROUTE_TOLERANCE;
    }

    static int npcOptionY(int row) {
        if (row < 1 || row >= NPC_OPTION_Y.length) {
            throw new IllegalArgumentException("Unsupported NPC option row " + row);
        }
        return NPC_OPTION_Y[row];
    }

    static int npcOptionX(int row) {
        npcOptionY(row);
        return row == 4 ? NPC_OPTION_4_X : NPC_OPTION_X;
    }

    static int[] entryNpcRows(int level) {
        switch (level) {
            case 10:
                return new int[] {3, 3};
            case 20:
                return new int[] {4, 4, 3};
            case 30:
                return new int[] {2, 3, 3};
            case 40:
                return new int[] {3, 3};
            case 50:
            case 60:
            case 70:
            case 75:
                return new int[] {4, 3, 3};
            case 65:
                return new int[] {3, 3, 3};
            default:
                throw new IllegalArgumentException("Unsupported dungeon level " + level);
        }
    }

    static int[] exitNpcRows(int level) {
        switch (level) {
            case 10:
            case 20:
            case 30:
            case 40:
            case 50:
            case 60:
            case 65:
                return new int[] {4, 4};
            case 70:
            case 75:
                return new int[] {4, 4, 4};
            default:
                throw new IllegalArgumentException("Unsupported dungeon level " + level);
        }
    }

    static boolean matchesNpcDialogTitle(String recognized, String expectedTitles) {
        String actual = normalizeDialogText(recognized);
        for (String expected : expectedTitles.split("\\|")) {
            String target = normalizeDialogText(expected);
            if (!target.isEmpty() && actual.contains(target)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeDialogText(String value) {
        return value == null ? "" : value.replaceAll("[\\s·:：()（）]", "");
    }

    private int dungeonMapMaxX() {
        return currentLevel() == 75 ? 199 : DUNGEON_MAP_MAX_X;
    }

    static EnemyRow selectEnemyRow(
            List<EnemyRow> rows, List<Integer> levels, boolean acceptAnyEnemy) {
        return selectEnemyRow(rows, levels, Collections.emptyList(), null, acceptAnyEnemy);
    }

    static EnemyRow selectEnemyRow(List<EnemyRow> rows, List<Integer> levels,
            List<String> priorityNames, String protectName, boolean acceptAnyEnemy) {
        for (String priorityName : priorityNames) {
            for (EnemyRow row : rows) {
                if (row.label.contains(priorityName)) {
                    return row;
                }
            }
        }
        for (EnemyRow row : rows) {
            if (protectName != null && row.label.contains(protectName)) {
                continue;
            }
            if (row.level != null && levels.contains(row.level)) {
                return row;
            }
        }
        if (acceptAnyEnemy || !priorityNames.isEmpty()) {
            for (EnemyRow row : rows) {
                if (protectName == null || !row.label.contains(protectName)) {
                    return row;
                }
            }
        }
        return null;
    }

    static boolean usesGuideUp(int level) {
        return level <= 30 || level == 65 || level >= 70;
    }

    static List<EnemyRow> readEnemyRows(List<OcrLine> lines, boolean guideUp) {
        List<EnemyRow> rows = new ArrayList<>();
        int[] centers = guideUp ? GUIDE_UP_ENEMY_CENTERS : GUIDE_DOWN_ENEMY_CENTERS;
        for (int centerY : centers) {
            StringBuilder label = new StringBuilder();
            Integer level = null;
            for (OcrLine line : lines) {
                Rect bounds = line.bounds;
                if (bounds == null || bounds.left < GUIDE_ROW_LEFT
                        || bounds.right > GUIDE_ROW_RIGHT
                        || rectCenterY(bounds) < centerY - 20
                        || rectCenterY(bounds) > centerY + 22) {
                    continue;
                }
                if (label.length() > 0) {
                    label.append(' ');
                }
                label.append(line.text);
                if (bounds.left < 1015) {
                    Integer parsed = firstInteger(line.text);
                    if (parsed != null) {
                        level = parsed;
                    }
                }
            }
            if (label.length() > 0) {
                rows.add(new EnemyRow(level, label.toString(),
                        new Rect(960, centerY - 20, 1233, centerY + 20)));
            }
        }
        return rows;
    }

    private static int rectCenterY(Rect bounds) {
        return bounds.top + (bounds.bottom - bounds.top) / 2;
    }

    private static Integer firstInteger(String value) {
        Matcher matcher = INTEGER.matcher(value);
        return matcher.find() ? Integer.valueOf(matcher.group()) : null;
    }

    static final class EnemyRow {
        final Integer level;
        final String label;
        final Rect bounds;

        EnemyRow(Integer level, String label, Rect bounds) {
            this.level = level;
            this.label = label;
            this.bounds = bounds;
        }
    }

    static final class RouteDecision {
        final int targetX;
        final int targetY;
        final int unstuckX;
        final boolean searchEnemies;
        final boolean acceptAnyEnemy;
        final boolean exit;
        final String interactionText;
        final boolean openNpc;
        final List<Integer> enemyLevels;
        final List<String> priorityEnemyNames;
        final String protectName;
        final boolean exitWhenNoEnemy;
        final int shortcutClicks;
        final int[][] waypoints;

        private RouteDecision(int targetX, int targetY, int unstuckX,
                boolean searchEnemies, boolean acceptAnyEnemy, boolean exit,
                String interactionText, boolean openNpc, List<Integer> enemyLevels,
                List<String> priorityEnemyNames, String protectName, boolean exitWhenNoEnemy,
                int shortcutClicks, int[][] waypoints) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.unstuckX = unstuckX;
            this.searchEnemies = searchEnemies;
            this.acceptAnyEnemy = acceptAnyEnemy;
            this.exit = exit;
            this.interactionText = interactionText;
            this.openNpc = openNpc;
            this.enemyLevels = enemyLevels;
            this.priorityEnemyNames = priorityEnemyNames;
            this.protectName = protectName;
            this.exitWhenNoEnemy = exitWhenNoEnemy;
            this.shortcutClicks = shortcutClicks;
            this.waypoints = waypoints;
        }

        static RouteDecision search(
                int x, int y, int unstuckX, boolean acceptAnyEnemy, Integer... levels) {
            return new RouteDecision(x, y, unstuckX, true, acceptAnyEnemy, false, null, false,
                    List.of(levels), Collections.emptyList(), null, false, 0, new int[0][]);
        }

        static RouteDecision searchRoute(int[][] points, Integer... levels) {
            int[] first = points[0];
            return new RouteDecision(first[0], first[1], 0, true, true, false, null, false,
                    List.of(levels), Collections.emptyList(), null, false, 0, points);
        }

        static RouteDecision searchNamed(int x, int y, int unstuckX,
                List<String> priorityNames, String protectName) {
            return new RouteDecision(x, y, unstuckX, true, false, false, null, false,
                    Collections.emptyList(), priorityNames, protectName, false, 0,
                    new int[0][]);
        }

        static RouteDecision searchThenExit(int x, int y,
                List<String> priorityNames, String protectName) {
            return new RouteDecision(x, y, 0, true, false, false, null, false,
                    Collections.emptyList(), priorityNames, protectName, true, 0,
                    new int[0][]);
        }

        static RouteDecision move(int x, int y, int unstuckX) {
            return new RouteDecision(x, y, unstuckX, false, false, false, null, false,
                    Collections.emptyList(), Collections.emptyList(), null, false, 0,
                    new int[0][]);
        }

        static RouteDecision moveRoute(int[][] points) {
            int[] first = points[0];
            return new RouteDecision(first[0], first[1], 0, false, false, false, null, false,
                    Collections.emptyList(), Collections.emptyList(), null, false, 0, points);
        }

        static RouteDecision interact(String text) {
            return new RouteDecision(0, 0, 0, false, false, false, text, false,
                    Collections.emptyList(), Collections.emptyList(), null, false, 0,
                    new int[0][]);
        }

        static RouteDecision interactNpc(String text) {
            return new RouteDecision(0, 0, 0, false, false, false, text, true,
                    Collections.emptyList(), Collections.emptyList(), null, false, 0,
                    new int[0][]);
        }

        static RouteDecision shortcut(int clicks, String text) {
            return new RouteDecision(0, 0, 0, false, false, false, text, false,
                    Collections.emptyList(), Collections.emptyList(), null, false, clicks,
                    new int[0][]);
        }

        static RouteDecision exit(int x, int y) {
            return new RouteDecision(x, y, 0, false, false, true, null, false,
                    Collections.emptyList(), Collections.emptyList(), null, false, 0,
                    new int[0][]);
        }

        String enemyDescription() {
            if (!priorityEnemyNames.isEmpty()) {
                return priorityEnemyNames.toString();
            }
            return acceptAnyEnemy ? "副本BOSS/普通敌人" : enemyLevels + "级敌人";
        }
    }
}
