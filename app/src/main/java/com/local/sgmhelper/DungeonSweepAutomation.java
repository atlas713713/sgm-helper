package com.local.sgmhelper;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DungeonSweepAutomation {
    /** 等级取自武当 {@code dungeon_list}（一般副本）：80 级是火烧博望坡，游戏里没有 85 级。 */
    static final int[] LEVELS = {
            10, 20, 30, 40, 50, 60, 65, 70, 75, 80, 90, 105, 120, 135, 150, 165, 180
    };

    private static final Pattern LEVEL_PATTERN = Pattern.compile("等[级級][:：]?\\s*(\\d+)");
    private static final int MAX_SCROLL_ATTEMPTS = 12;
    private static final long SWEEP_WAIT_MS = 10_000;

    private final AutomationHost host;
    private List<Integer> selectedLevels = new ArrayList<>();
    private int currentIndex;
    private int completedRunsForCurrentLevel;
    private int sweepRunCount = DungeonBattleAutomation.DEFAULT_DUNGEON_RUN_COUNT;
    private boolean resumePrimaryTask;

    DungeonSweepAutomation(AutomationHost host) {
        this.host = host;
    }

    void start(List<Integer> levels) {
        start(levels, false);
    }

    void startScheduled(List<Integer> levels) {
        start(levels, true);
    }

    private void start(List<Integer> levels, boolean scheduled) {
        if (!scheduled && host.isAutomationRunning()) {
            host.showProgress("已有任务正在运行");
            return;
        }
        if (levels.isEmpty()) {
            host.showProgress("请先选择需要扫荡的副本");
            return;
        }
        selectedLevels = new ArrayList<>(levels);
        resumePrimaryTask = scheduled;
        if (scheduled) {
            host.startAutomation("定时副本：打开游戏", this::begin);
        } else {
            host.startPrimaryAutomation(
                    AutomationHost.PrimaryTask.DUNGEON, "扫荡副本：打开游戏", this::begin);
        }
    }

    private void begin() {
        currentIndex = 0;
        completedRunsForCurrentLevel = 0;
        sweepRunCount = currentSweepRunCount();
        host.tap(1215, 58, this::resetGameMenu);
    }

    private void resetGameMenu() {
        host.showProgress("扫荡副本：打开副本");
        swipeGameMenuToTop(3);
    }

    private void swipeGameMenuToTop(int remaining) {
        if (remaining == 0) {
            host.clickTemplateOrText(
                    WudangTemplateMatcher.Template.DUNGEON,
                    "副本", false, 846, 101, 1238, 595,
                    () -> host.tap(1105, 96, () -> findCurrentLevel(MAX_SCROLL_ATTEMPTS)),
                    2, 5,
                    () -> host.tap(900, 362,
                            () -> host.tap(1105, 96,
                                    () -> findCurrentLevel(MAX_SCROLL_ATTEMPTS))));
            return;
        }
        host.swipe(1120, 250, 1120, 350,
                () -> swipeGameMenuToTop(remaining - 1));
    }

    private void findCurrentLevel(int remainingScrolls) {
        int level = selectedLevels.get(currentIndex);
        host.showProgress("扫荡副本：查找 " + level + " 级");
        host.recognizeDungeonText(lines -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            Rect bounds = findLevelBounds(lines, level);
            if (bounds != null) {
                host.tap(bounds.centerX(), bounds.centerY(), this::sweepCurrentLevel);
            } else if (remainingScrolls > 0) {
                host.swipe(200, 500, 200, 250, 800,
                        () -> findCurrentLevel(remainingScrolls - 1));
            } else {
                host.failAutomation("扫荡副本：未找到 " + level + " 级副本");
            }
        });
    }

    private void sweepCurrentLevel() {
        int level = selectedLevels.get(currentIndex);
        host.showProgress("扫荡副本：" + level + " 级扫荡中");
        host.tap(1118, 223,
                () -> host.postDelayed(this::confirmCurrentSweep, SWEEP_WAIT_MS));
    }

    private void confirmCurrentSweep() {
        host.showProgress("扫荡副本：确认奖励");
        host.tap(645, 700, () -> {
            completedRunsForCurrentLevel++;
            if (DungeonBattleAutomation.shouldRepeatDungeon(
                    completedRunsForCurrentLevel, sweepRunCount)) {
                host.showProgress("扫荡副本：" + selectedLevels.get(currentIndex)
                        + " 级已完成 " + completedRunsForCurrentLevel + "/"
                        + sweepRunCount + " 次，继续");
                findCurrentLevel(MAX_SCROLL_ATTEMPTS);
                return;
            }
            currentIndex++;
            if (currentIndex < selectedLevels.size()) {
                completedRunsForCurrentLevel = 0;
                sweepRunCount = currentSweepRunCount();
                findCurrentLevel(MAX_SCROLL_ATTEMPTS);
            } else {
                host.showProgress("扫荡副本：关闭副本窗口");
                host.tap(1240, 50, this::finish);
            }
        });
    }

    private void finish() {
        if (!resumePrimaryTask) {
            host.completeAutomation();
            return;
        }
        host.showProgress("定时副本已完成，10秒后恢复主要任务");
        host.postDelayed(host::resumePrimaryTask, 10_000);
    }

    static Rect findLevelBounds(List<OcrLine> lines, int expectedLevel) {
        for (OcrLine line : lines) {
            Rect bounds = line.bounds;
            if (bounds != null && bounds.centerX() < 420
                    && parseLevel(line.text) == expectedLevel) {
                return bounds;
            }
        }
        return null;
    }

    private int currentSweepRunCount() {
        return DungeonBattleAutomation.normalizeDungeonRunCount(
                host.dungeonSweepRunCount(selectedLevels.get(currentIndex)));
    }

    static int parseLevel(String value) {
        Matcher matcher = LEVEL_PATTERN.matcher(value);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
