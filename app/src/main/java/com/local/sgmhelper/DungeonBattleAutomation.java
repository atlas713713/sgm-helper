package com.local.sgmhelper;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runs the dungeon itself. This is separate from {@link DungeonSweepAutomation}. */
final class DungeonBattleAutomation {
    /** Levels with a route implemented; {@link BaseDungeonAction#forLevel} rejects the rest. */
    static final int[] LEVELS =
            {10, 20, 30, 40, 50, 60, 65, 70, 75, 80, 90, 105, 120, 135, 150, 165};

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
    // Wudang's NPCDialog item1..item4 boxes: x 133..369, 40 tall, 62 apart.
    private static final int NPC_ROW_LEFT = 133;
    private static final int NPC_ROW_RIGHT = 369;
    private static final int NPC_ROW_TOP = 430;
    private static final int NPC_ROW_BOTTOM = 656;
    private static final int NPC_ROW_TOLERANCE = 26;
    private static final int GUIDE_ENEMY_X = 1025;
    private static final int GUIDE_ENEMY_Y = 160;
    private static final int[] GUIDE_UP_ENEMY_CENTERS = {151, 206, 261, 316, 371};
    private static final int[] GUIDE_DOWN_ENEMY_CENTERS = {294, 349, 404};
    private static final int GUIDE_ROW_LEFT = 950;
    private static final int GUIDE_ROW_RIGHT = 1235;
    private static final int ROUTE_MAX_CHECKS = 30;
    private static final int ROUTE_RETRY_INTERVAL = 5;
    private static final int ROUTE_TOLERANCE = 3;
    private static final long MAP_WAIT_MS = 5_000;
    private static final long FIGHT_CHECK_MS = 4_000;

    private final AutomationHost host;
    private List<BaseDungeonAction> selectedDungeons = Collections.emptyList();
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
        List<BaseDungeonAction> dungeons = new ArrayList<>();
        for (int level : levels) {
            try {
                dungeons.add(BaseDungeonAction.forLevel(level));
            } catch (IllegalArgumentException unsupported) {
                host.showProgress("实战副本：目前支持 10–165 级，暂不执行 "
                        + level + " 级");
                return;
            }
        }
        selectedDungeons = dungeons;
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
        host.showProgress(currentLevel() + "级副本：前往"
                + currentDungeon().campName() + "入口");
        routeChecks = 0;
        tapCampNpcRoute(2);
    }

    private void tapCampNpcRoute(int remainingTaps) {
        host.tapMapCoordinate(currentDungeon().campNpcX(), 16, PALACE_MAP_MAX_X, () -> {
            if (remainingTaps > 1) {
                host.postDelayed(() -> tapCampNpcRoute(remainingTaps - 1), 500);
            } else {
                host.postDelayed(this::waitForCampNpc, 1_000);
            }
        });
    }

    private void waitForCampNpc() {
        int targetX = currentDungeon().campNpcX();
        host.recognizeMapCoordinate(value -> {
            if (isAtCoordinate(value, targetX, 16)) {
                openCampNpc();
                return;
            }
            routeChecks++;
            if (routeChecks >= ROUTE_MAX_CHECKS) {
                host.failAutomation(currentLevel() + "级副本：未能到达"
                        + currentDungeon().campName() + "入口");
            } else if (routeChecks % ROUTE_RETRY_INTERVAL == 0) {
                tapCampNpcRoute(2);
            } else {
                host.postDelayed(this::waitForCampNpc, 1_000);
            }
        });
    }

    private void openCampNpc() {
        Runnable next = () -> host.postDelayed(
                () -> host.waitForMapReady(20, this::gotoEntryNpc), MAP_WAIT_MS);
        host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y, () -> {
            String title = currentDungeon().dungeonName();
            int[] rows = {4, 3};
            if (title == null) {
                // 80 级以上的副本名来自服务端配置，APK 里没有，只能直接按固定行走。
                clickNpcRows(rows, 0, next);
            } else {
                clickNpcSequence(title, rows, next);
            }
        });
    }

    private void gotoEntryNpc() {
        host.showProgress(currentLevel() + "级副本：前往入口 NPC");
        routeChecks = 0;
        tapEntryNpcRoute(2);
    }

    private void tapEntryNpcRoute(int remainingTaps) {
        host.tapMapCoordinate(currentDungeon().entryNpcX(), currentDungeon().entryNpcY(),
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
            if (currentDungeon().isAtEntryNpc(value)) {
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
        host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y, () -> {
            String[] buttons = currentDungeon().entryNpcButtons();
            if (buttons != null) {
                clickNpcButtons(buttons, 0, this::waitForDungeon);
            } else {
                clickNpcSequence(currentDungeon().entryNpcName(),
                        currentDungeon().entryNpcRows(), this::waitForDungeon);
            }
        });
    }

    private void clickNpcSequence(
            String expectedTitle, int[] rows, Runnable next) {
        clickNpcOption(expectedTitle, rows[0], () -> {
            if (rows.length == 1) {
                next.run();
            } else {
                host.postDelayed(() -> clickNpcRows(rows, 1, next), 1_000);
            }
        }, 8);
    }

    private void clickNpcRows(int[] rows, int index, Runnable next) {
        host.tap(npcOptionX(rows[index]), npcOptionY(rows[index]), () -> {
            if (index + 1 == rows.length) {
                next.run();
            } else {
                host.postDelayed(() -> clickNpcRows(rows, index + 1, next), 1_000);
            }
        });
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
        host.tap(GUIDE_ENEMY_X, GUIDE_ENEMY_Y, next);
    }

    private void inspectPosition() {
        host.showProgress(currentLevel() + "级副本：读取当前位置");
        host.recognizeMapCoordinate(value -> {
            int[] coordinate = BossAutomation.parseMapCoordinate(value);
            if (coordinate == null || coordinate[0] > currentDungeon().dungeonMapMaxX()) {
                DiagnosticLog.warn("Dungeon",
                        "Invalid map coordinate Paddle OCR: " + value);
                host.postDelayed(this::inspectPosition, 1_000);
                return;
            }
            int x = coordinate[0];
            int y = coordinate[1];
            samePositionCount = x == lastX ? samePositionCount + 1 : 0;
            lastX = x;
            execute(currentDungeon().decide(x, y));
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
            String npcName = currentDungeon().interactionNpcName();
            Runnable afterClick = () -> host.postDelayed(this::inspectPosition, 2_000);
            Runnable click = npcName == null
                    ? () -> clickNpcButton(decision.interactionText, 8, afterClick)
                    : () -> clickNpcOption(npcName,
                            currentDungeon().interactionNpcRow(), afterClick, 8);
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
        if (currentLevel() == 40 || currentLevel() == 50) {
            findRedBoss(decision);
            return;
        }
        host.showProgress(currentLevel() + "级副本：搜索 " + decision.enemyDescription());
        host.recognizeDungeonText(lines -> {
            EnemyRow enemy = selectEnemyRow(
                    readEnemyRows(lines, currentDungeon().usesGuideUp()), decision.enemyLevels,
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

    private void findRedBoss(RouteDecision decision) {
        host.showProgress(currentLevel() + "级副本：搜索红名 BOSS");
        host.recognizeRedBoss(boss -> {
            if (boss == null) {
                move(decision);
                return;
            }
            host.showProgress(currentLevel() + "级副本：攻击 " + boss.name);
            host.tap(boss.bounds.centerX(), boss.bounds.centerY(),
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
            host.tapMapCoordinate(decision.unstuckX, 25, currentDungeon().dungeonMapMaxX(),
                    () -> host.postDelayed(this::inspectPosition, 2_000));
            return;
        }
        host.showProgress(currentLevel() + "级副本：前往 "
                + decision.targetX + "," + decision.targetY);
        host.tapMapCoordinate(decision.targetX, decision.targetY,
                currentDungeon().dungeonMapMaxX(),
                () -> host.postDelayed(this::inspectPosition, 4_000));
    }

    private void moveWaypoints(int[][] points, int index) {
        int[] point = points[index];
        host.showProgress(currentLevel() + "级副本：前往 "
                + point[0] + "," + point[1]);
        host.tapMapCoordinate(point[0], point[1], currentDungeon().dungeonMapMaxX(), () -> {
            if (index + 1 < points.length) {
                host.postDelayed(() -> moveWaypoints(points, index + 1), 2_000);
            } else {
                host.postDelayed(this::inspectPosition, 4_000);
            }
        });
    }

    private void gotoExit() {
        int[] exit = currentDungeon().exitPoint();
        host.showProgress(currentLevel() + "级副本：前往出口 "
                + exit[0] + "," + exit[1]);
        host.tapMapCoordinate(exit[0], exit[1], currentDungeon().dungeonMapMaxX(),
                () -> host.postDelayed(() -> host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y,
                        this::claimReward), MAP_WAIT_MS));
    }

    private void claimReward() {
        host.showProgress(currentLevel() + "级副本：离开并领取奖励");
        String[] buttons = currentDungeon().exitNpcButtons();
        if (buttons != null) {
            clickNpcButtons(buttons, 0, this::finishCurrent);
        } else {
            clickNpcSequence(currentDungeon().exitNpcName(),
                    currentDungeon().exitNpcRows(), this::finishCurrent);
        }
    }

    /** 复刻武当按按钮文字点行：OCR 四个选项框，命中哪一行就点哪一行。 */
    private void clickNpcButtons(String[] buttons, int index, Runnable next) {
        clickNpcButton(buttons[index], 10, () -> {
            if (index + 1 == buttons.length) {
                next.run();
            } else {
                host.postDelayed(() -> clickNpcButtons(buttons, index + 1, next), 1_000);
            }
        });
    }

    private void clickNpcButton(String button, int remainingAttempts, Runnable next) {
        host.recognizeTextRegion(NPC_ROW_LEFT, NPC_ROW_TOP, NPC_ROW_RIGHT, NPC_ROW_BOTTOM,
                text -> {
                    int row = npcRowForButton(text, button);
                    DiagnosticLog.info("Dungeon", "NPC button \"" + button + "\" row " + row
                            + " from " + (text == null ? "" : text.getText()));
                    if (row > 0) {
                        host.tap(npcOptionX(row), npcOptionY(row), next);
                    } else if (remainingAttempts > 1) {
                        host.postDelayed(
                                () -> clickNpcButton(button, remainingAttempts - 1, next), 500);
                    } else {
                        host.failAutomation(currentLevel() + "级副本：未找到“" + button + "”按钮");
                    }
                });
    }

    private void finishCurrent() {
        currentIndex++;
        if (currentIndex < selectedDungeons.size()) {
            host.postDelayed(this::returnHomeForDungeon, MAP_WAIT_MS);
        } else {
            host.completeAutomation();
        }
    }

    private BaseDungeonAction currentDungeon() {
        return selectedDungeons.get(currentIndex);
    }

    private int currentLevel() {
        return currentDungeon().level();
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

    /**
     * 在 NPCDialog 的四个选项框里找按钮文字，返回 1..4；找不到返回 0。
     * 用 OCR 行的中心 y 去对最近的固定行中心，避免依赖 OCR 的分行方式。
     */
    static int npcRowForButton(OcrText text, String button) {
        String target = normalizeDialogText(button);
        if (text == null || target.isEmpty()) {
            return 0;
        }
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                if (!normalizeDialogText(line.getText()).contains(target)) {
                    continue;
                }
                Rect bounds = line.getBoundingBox();
                if (bounds == null) {
                    continue;
                }
                int centerY = rectCenterY(bounds);
                for (int row = 1; row < NPC_OPTION_Y.length; row++) {
                    if (Math.abs(centerY - NPC_OPTION_Y[row]) <= NPC_ROW_TOLERANCE) {
                        return row;
                    }
                }
            }
        }
        return 0;
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

        static RouteDecision searchNamed(int x, int y, int unstuckX,
                List<String> priorityNames, String protectName, Integer... levels) {
            return new RouteDecision(x, y, unstuckX, true, false, false, null, false,
                    List.of(levels), priorityNames, protectName, false, 0, new int[0][]);
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
