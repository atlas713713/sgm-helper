package com.local.sgmhelper;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runs the dungeon itself. This is separate from {@link DungeonSweepAutomation}. */
final class DungeonBattleAutomation {
    private static final Pattern INTEGER = Pattern.compile("\\d+");
    private static final int MAX_LEVEL_SCROLLS = 12;
    private static final int CAMP_MAP_MAX_X = 199;
    private static final int DUNGEON_MAP_MAX_X = 599;
    private static final int NPC_SHORTCUT_X = 1208;
    private static final int NPC_SHORTCUT_Y = 410;
    private static final int GUIDE_UP_X = 974;
    private static final int GUIDE_UP_Y = 31;
    private static final long MAP_WAIT_MS = 5_000;
    private static final long FIGHT_CHECK_MS = 4_000;

    private final AutomationHost host;
    private List<Integer> selectedLevels = Collections.emptyList();
    private int currentIndex;
    private int lastX = -1;
    private int samePositionCount;

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
        openDungeonMenu();
    }

    private void openDungeonMenu() {
        host.showProgress("实战副本：打开副本列表");
        host.tap(1215, 58, () -> swipeGameMenuToTop(3));
    }

    private void swipeGameMenuToTop(int remaining) {
        if (remaining == 0) {
            host.tap(900, 362,
                    () -> host.tap(1105, 96, () -> findCurrentLevel(MAX_LEVEL_SCROLLS)));
            return;
        }
        host.swipe(1120, 250, 1120, 350,
                () -> swipeGameMenuToTop(remaining - 1));
    }

    private void findCurrentLevel(int remainingScrolls) {
        int level = selectedLevels.get(currentIndex);
        host.showProgress("实战副本：查找 " + level + " 级");
        host.recognizeDungeonText(lines -> {
            Rect bounds = DungeonSweepAutomation.findLevelBounds(lines, level);
            if (bounds != null) {
                host.tap(bounds.centerX(), bounds.centerY(), this::enterCamp);
            } else if (remainingScrolls > 0) {
                host.swipe(200, 500, 200, 250, 800,
                        () -> findCurrentLevel(remainingScrolls - 1));
            } else {
                host.failAutomation("实战副本：未找到 " + level + " 级副本");
            }
        });
    }

    private void enterCamp() {
        host.showProgress(currentLevel() + "级副本：传送至" + campName());
        // Wudang DungeonDialog.enterCampPoint at 1280x720.
        host.tap(176, 618, () -> host.postDelayed(
                () -> host.waitForMapReady(20, this::gotoEntryNpc), MAP_WAIT_MS));
    }

    private void gotoEntryNpc() {
        host.showProgress(currentLevel() + "级副本：前往入口 NPC");
        int entryX = currentLevel() == 60 ? 102 : 100;
        int entryY = currentLevel() == 70 ? 17 : 16;
        host.tapMapCoordinate(entryX, entryY, CAMP_MAP_MAX_X,
                () -> host.postDelayed(this::openEntryNpc, MAP_WAIT_MS));
    }

    private void openEntryNpc() {
        host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y, () -> {
            if (currentLevel() == 20) {
                host.clickDungeonText("详情告知", true,
                        () -> host.clickDungeonText("下一步", true, this::waitForDungeon, 8,
                                () -> failMissing("下一步")),
                        8, () -> failMissing("详情告知"));
            } else if (currentLevel() == 30) {
                host.clickDungeonText("黄天", true,
                        () -> host.clickDungeonText("进入", true, this::waitForDungeon, 8,
                                () -> failMissing("进入")),
                        8, () -> failMissing("黄天"));
            } else if (currentLevel() == 40) {
                host.clickDungeonText("闯入", false, this::waitForDungeon, 8,
                        () -> failMissing("闯入"));
            } else if (currentLevel() == 50) {
                host.clickDungeonText("下一步", true,
                        () -> host.clickDungeonText("入场", true, this::waitForDungeon, 5,
                                () -> host.clickDungeonText("进入", true, this::waitForDungeon, 5,
                                        () -> failMissing("入场/进入"))),
                        8, () -> failMissing("下一步"));
            } else if (currentLevel() == 60 || currentLevel() == 65) {
                host.clickDungeonText("开始神游", true,
                        () -> host.clickDungeonText("进入", true, this::waitForDungeon, 8,
                                () -> failMissing("进入")),
                        8, () -> failMissing("开始神游"));
            } else if (currentLevel() >= 70) {
                host.clickDungeonText("下一步", true,
                        () -> host.clickDungeonText("出发", true, this::waitForDungeon, 5,
                                () -> host.clickDungeonText("进入", true, this::waitForDungeon, 5,
                                        () -> failMissing("出发/进入"))),
                        8, () -> failMissing("下一步"));
            } else {
                host.clickDungeonText("进入", true, this::waitForDungeon, 8,
                        () -> failMissing("进入"));
            }
        });
    }

    private void failMissing(String text) {
        host.failAutomation(currentLevel() + "级副本：未找到“" + text + "”选项");
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
        if (currentLevel() <= 30 || currentLevel() == 65 || currentLevel() >= 70) {
            host.tap(GUIDE_UP_X, GUIDE_UP_Y, next);
        } else {
            next.run();
        }
    }

    private void inspectPosition() {
        host.showProgress(currentLevel() + "级副本：读取当前位置");
        host.recognizeMapCoordinate(value -> {
            Integer x = BossAutomation.parseMapX(value);
            if (x == null || x > dungeonMapMaxX()) {
                host.postDelayed(this::inspectPosition, 1_000);
                return;
            }
            samePositionCount = x == lastX ? samePositionCount + 1 : 0;
            lastX = x;
            execute(decide(currentLevel(), x));
        });
    }

    private void execute(RouteDecision decision) {
        if (decision.exit) {
            gotoExit();
        } else if (decision.interactionText != null) {
            host.showProgress(currentLevel() + "级副本：" + decision.interactionText);
            Runnable click = () -> host.clickDungeonText(decision.interactionText, true,
                    () -> host.postDelayed(this::inspectPosition, 2_000), 8,
                    () -> failMissing(decision.interactionText));
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

    private void findEnemy(RouteDecision decision) {
        host.showProgress(currentLevel() + "级副本：搜索 " + decision.enemyDescription());
        host.recognizeDungeonText(lines -> {
            EnemyRow enemy = selectEnemyRow(readEnemyRows(lines), decision.enemyLevels,
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

    private void gotoExit() {
        RouteDecision exit = decide(currentLevel(), 0);
        host.showProgress(currentLevel() + "级副本：前往出口 "
                + exit.targetX + "," + exit.targetY);
        host.tapMapCoordinate(exit.targetX, exit.targetY, dungeonMapMaxX(),
                () -> host.postDelayed(() -> host.tap(NPC_SHORTCUT_X, NPC_SHORTCUT_Y,
                        this::claimReward), MAP_WAIT_MS));
    }

    private void claimReward() {
        if (currentLevel() == 30) {
            host.showProgress("30级副本：与黄巾太平道长对话离开");
            host.clickDungeonText("黄天", true,
                    () -> host.clickDungeonText("进入", true, this::finishCurrent, 8,
                            () -> failMissing("进入")),
                    8, () -> failMissing("黄天"));
            return;
        }
        if (currentLevel() == 50) {
            host.showProgress("50级副本：与孙坚对话离开");
            host.clickDungeonText("离开", true, this::finishCurrent, 10,
                    () -> failMissing("离开"));
            return;
        }
        if (currentLevel() == 60) {
            host.showProgress("60级副本：答谢公孙瓒");
            host.clickDungeonText("答谢公孙瓒", true, this::finishCurrent, 10,
                    () -> failMissing("答谢公孙瓒"));
            return;
        }
        if (currentLevel() == 65) {
            host.showProgress("65级副本：收下战利品");
            host.clickDungeonText("收下战利品", true, this::finishCurrent, 10,
                    () -> failMissing("收下战利品"));
            return;
        }
        if (currentLevel() >= 70) {
            host.showProgress(currentLevel() + "级副本：收下战利品");
            host.clickDungeonText("下一步", true,
                    () -> host.clickDungeonText("收下战利品", true, this::finishCurrent, 10,
                            () -> failMissing("收下战利品")),
                    8, () -> host.clickDungeonText("收下战利品", true,
                            this::finishCurrent, 10, () -> failMissing("下一步/收下战利品")));
            return;
        }
        String reward;
        if (currentLevel() == 20) {
            reward = "领取奖赏";
        } else if (currentLevel() == 40) {
            reward = "领取奖励";
        } else {
            reward = "收下奖赏";
        }
        host.showProgress(currentLevel() + "级副本：" + reward);
        host.clickDungeonText(reward, true, this::finishCurrent, 10,
                () -> failMissing(reward));
    }

    private void finishCurrent() {
        currentIndex++;
        if (currentIndex < selectedLevels.size()) {
            host.postDelayed(this::openDungeonMenu, MAP_WAIT_MS);
        } else {
            host.completeAutomation();
        }
    }

    static RouteDecision decide10(int x) {
        if (x >= 504) {
            return RouteDecision.search(500, 27, 550, false, 7, 10);
        }
        if (x >= 386) {
            return RouteDecision.search(382, 27, 450, false, 7, 10);
        }
        if (x >= 168) {
            return RouteDecision.move(159, 27, 0);
        }
        if (x >= 39) {
            return RouteDecision.search(38, 27, 110, true, 10);
        }
        return RouteDecision.exit(25, 27);
    }

    static RouteDecision decide20(int x) {
        if (x >= 478) {
            return RouteDecision.search(474, 27, 550, false, 18, 20);
        }
        if (x >= 348) {
            return RouteDecision.move(338, 27, 0);
        }
        if (x >= 206) {
            return RouteDecision.search(199, 27, 280, true, 20);
        }
        if (x >= 91) {
            return RouteDecision.search(86, 27, 150, false, 20);
        }
        if (x >= 31) {
            return RouteDecision.search(26, 27, 80, true, 20);
        }
        if (x >= 18) {
            return RouteDecision.exit(22, 27);
        }
        return RouteDecision.exit(22, 27);
    }

    static RouteDecision decide(int level, int x) {
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
            return decide50(x);
        }
        if (level == 60) {
            return decide60(x);
        }
        if (level == 65) {
            return decide65(x);
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
            return RouteDecision.search(448, 4, 570, true, 30);
        }
        if (x >= 301) {
            return RouteDecision.search(297, 27, 420, true, 30);
        }
        if (x >= 151) {
            return RouteDecision.search(148, 4, 0, true);
        }
        if (x >= 33) {
            return RouteDecision.search(32, 27, 100, true, 30);
        }
        return RouteDecision.exit(24, 27);
    }

    static RouteDecision decide40(int x) {
        if (x >= 530) {
            return RouteDecision.search(520, 25, 0, true);
        }
        if (x >= 430) {
            return RouteDecision.search(420, 25, 0, true);
        }
        if (x >= 360) {
            return RouteDecision.search(350, 25, 0, true);
        }
        if (x >= 285) {
            return RouteDecision.search(275, 25, 0, true);
        }
        if (x >= 190) {
            return RouteDecision.search(180, 25, 0, true);
        }
        if (x >= 120) {
            return RouteDecision.search(110, 25, 0, true);
        }
        if (x >= 60) {
            return RouteDecision.search(50, 25, 0, true);
        }
        if (x >= 32) {
            return RouteDecision.search(25, 25, 0, true);
        }
        return RouteDecision.exit(25, 25);
    }

    static RouteDecision decide50(int x) {
        if (x <= 49) {
            return RouteDecision.move(60, 45, 0);
        }
        if (x <= 110) {
            return RouteDecision.search(112, 25, 0, true);
        }
        if (x <= 194) {
            return RouteDecision.search(205, 25, 0, true);
        }
        if (x <= 287) {
            return RouteDecision.search(291, 25, 0, true);
        }
        if (x <= 308) {
            return RouteDecision.interact("交给我吧");
        }
        if (x <= 388) {
            return RouteDecision.search(399, 25, 0, true);
        }
        if (x <= 484) {
            return RouteDecision.search(495, 25, 0, true);
        }
        if (x <= 581) {
            return RouteDecision.search(583, 25, 0, true);
        }
        return RouteDecision.exit(570, 25);
    }

    static RouteDecision decide60(int x) {
        if (x <= 98) {
            return RouteDecision.search(71, 4, 0, true);
        }
        if (x >= 505 && x <= 528) {
            return RouteDecision.search(114, 46, 0, true);
        }
        if (x >= 105 && x <= 198) {
            return RouteDecision.search(70, 46, 0, true);
        }
        if (x >= 531 && x <= 553) {
            return RouteDecision.search(214, 4, 0, true);
        }
        if (x >= 205 && x <= 298) {
            return RouteDecision.search(94, 27, 0, true);
        }
        if (x >= 305 && x <= 398) {
            return RouteDecision.search(394, 27, 0, true);
        }
        if (x >= 556 && x <= 581) {
            return RouteDecision.search(405, 27, 0, true);
        }
        if (x >= 405 && x <= 479) {
            return RouteDecision.search(486, 25, 0, true);
        }
        return RouteDecision.exit(486, 27);
    }

    static RouteDecision decide65(int x) {
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
            return RouteDecision.search(410, 4, 0, true, 64);
        }
        if (x <= 537) {
            return RouteDecision.search(538, 27, 0, true, 64);
        }
        return RouteDecision.exit(560, 27);
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

    private String campName() {
        if (currentLevel() == 20) {
            return "曹公府";
        }
        if (currentLevel() == 30) {
            return "黄巾营寨";
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
        if (acceptAnyEnemy && protectName == null && priorityNames.isEmpty()
                && !rows.isEmpty()) {
            return rows.get(0);
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

    static List<EnemyRow> readEnemyRows(List<OcrLine> lines) {
        List<EnemyRow> rows = new ArrayList<>();
        int[] centers = {151, 206, 261, 316, 371};
        for (int centerY : centers) {
            StringBuilder label = new StringBuilder();
            Integer level = null;
            for (OcrLine line : lines) {
                Rect bounds = line.bounds;
                if (bounds == null || bounds.right < 950 || bounds.centerY() < centerY - 22
                        || bounds.centerY() > centerY + 22) {
                    continue;
                }
                if (label.length() > 0) {
                    label.append(' ');
                }
                label.append(line.text);
                if (bounds.left < 1030) {
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

        private RouteDecision(int targetX, int targetY, int unstuckX,
                boolean searchEnemies, boolean acceptAnyEnemy, boolean exit,
                String interactionText, boolean openNpc, List<Integer> enemyLevels,
                List<String> priorityEnemyNames, String protectName, boolean exitWhenNoEnemy) {
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
        }

        static RouteDecision search(
                int x, int y, int unstuckX, boolean acceptAnyEnemy, Integer... levels) {
            return new RouteDecision(x, y, unstuckX, true, acceptAnyEnemy, false, null, false,
                    List.of(levels), Collections.emptyList(), null, false);
        }

        static RouteDecision searchNamed(int x, int y, int unstuckX,
                List<String> priorityNames, String protectName) {
            return new RouteDecision(x, y, unstuckX, true, false, false, null, false,
                    Collections.emptyList(), priorityNames, protectName, false);
        }

        static RouteDecision searchThenExit(int x, int y,
                List<String> priorityNames, String protectName) {
            return new RouteDecision(x, y, 0, true, false, false, null, false,
                    Collections.emptyList(), priorityNames, protectName, true);
        }

        static RouteDecision move(int x, int y, int unstuckX) {
            return new RouteDecision(x, y, unstuckX, false, false, false, null, false,
                    Collections.emptyList(), Collections.emptyList(), null, false);
        }

        static RouteDecision interact(String text) {
            return new RouteDecision(0, 0, 0, false, false, false, text, false,
                    Collections.emptyList(), Collections.emptyList(), null, false);
        }

        static RouteDecision interactNpc(String text) {
            return new RouteDecision(0, 0, 0, false, false, false, text, true,
                    Collections.emptyList(), Collections.emptyList(), null, false);
        }

        static RouteDecision exit(int x, int y) {
            return new RouteDecision(x, y, 0, false, false, true, null, false,
                    Collections.emptyList(), Collections.emptyList(), null, false);
        }

        String enemyDescription() {
            if (!priorityEnemyNames.isEmpty()) {
                return priorityEnemyNames.toString();
            }
            return acceptAnyEnemy ? "副本BOSS/普通敌人" : enemyLevels + "级敌人";
        }
    }
}
