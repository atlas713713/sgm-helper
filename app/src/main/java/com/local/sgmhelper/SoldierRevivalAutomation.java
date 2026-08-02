package com.local.sgmhelper;


import java.util.ArrayList;
import java.util.List;

final class SoldierRevivalAutomation {
    private static final int CAMP_SCROLL_ATTEMPTS = 4;
    private static final int ARRIVAL_ATTEMPTS = 16;
    private static final long RETURN_HOME_SETTLE_MS = 2_000L;
    private static final long ARRIVAL_POLL_DELAY_MS = 500L;

    // Wudang's MilitaryCamp uses two adjacent layouts. The two revive-all
    // centers are x=992 and x=1018; the midpoint stays inside the button in
    // both layouts. The confirmation centers are x=540 and x=533 likewise.
    private static final int REVIVE_TAB_X = 574;
    private static final int REVIVE_TAB_Y = 109;
    private static final int REVIVE_ALL_X = 1_005;
    private static final int REVIVE_ALL_Y = 588;
    private static final int CONFIRM_X = 536;
    private static final int CONFIRM_Y = 377;

    private final AutomationHost host;

    SoldierRevivalAutomation(AutomationHost host) {
        this.host = host;
    }

    void start() {
        if (host.isAutomationRunning()) {
            host.showProgress("已有任务正在运行");
            return;
        }
        host.startIdleAutomation(
                "复活士兵：打开游戏", () -> run(host::completeAutomation));
    }

    void run(Runnable next) {
        host.ensureGameHudVisible(() -> {
            host.showProgress("复活士兵：停止自动攻击后先回城");
            host.closeAutoPathPanel(() -> host.returnHome(
                    () -> host.postDelayed(() -> openCampSearch(next),
                            RETURN_HOME_SETTLE_MS)));
        });
    }

    private void openCampSearch(Runnable next) {
        host.showProgress("复活士兵：打开右侧自动寻路");
        host.closeAutoPathPanel(() -> host.openAutoPathPanel(
                () -> findCamp(CAMP_SCROLL_ATTEMPTS, next)));
    }

    private void findCamp(int remainingScrolls, Runnable next) {
        host.showProgress("复活士兵：查找军营");
        host.clickRightTextFast("军营", () -> waitForCamp(ARRIVAL_ATTEMPTS, next), 2, () -> {
            if (remainingScrolls > 0) {
                host.swipe(1120, 450, 1120, 230,
                        () -> findCamp(remainingScrolls - 1, next));
            } else {
                skip("回城后仍未找到军营", next);
            }
        });
    }

    private void waitForCamp(int remainingAttempts, Runnable next) {
        host.showProgress("复活士兵：等待到达军营");
        host.recognizeText(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (hasCampArrival(screenLines(text))) {
                revive(next);
            } else if (remainingAttempts > 1) {
                host.postDelayed(() -> waitForCamp(remainingAttempts - 1, next),
                        ARRIVAL_POLL_DELAY_MS);
            } else {
                skip("等待到达军营超时", next);
            }
        });
    }

    private void revive(Runnable next) {
        host.showProgress("复活士兵：打开复活页");
        DiagnosticLog.info("SOLDIER_REVIVAL", "action=tab_revive x="
                + REVIVE_TAB_X + " y=" + REVIVE_TAB_Y);
        host.tapUi(REVIVE_TAB_X, REVIVE_TAB_Y, () -> host.postDelayed(() -> {
            // Do not use full-screen OCR here. Chat/system text can contain
            // similar words and cause the old implementation to tap the wrong
            // location. These coordinates mirror Wudang's MilitaryCamp.
            DiagnosticLog.info("SOLDIER_REVIVAL", "action=revive_all x="
                    + REVIVE_ALL_X + " y=" + REVIVE_ALL_Y);
            host.tapFast(REVIVE_ALL_X, REVIVE_ALL_Y, () -> host.postDelayed(() -> {
                DiagnosticLog.info("SOLDIER_REVIVAL", "action=confirm x="
                        + CONFIRM_X + " y=" + CONFIRM_Y);
                host.tapFast(CONFIRM_X, CONFIRM_Y,
                        () -> host.postDelayed(
                                () -> close("已复活全部士兵", next), 1_000));
            }, 1_000));
        }, 1_000));
    }

    private void skip(String reason, Runnable next) {
        host.closeAutoPathPanel(() -> {
            host.showProgress("复活士兵：" + reason + "，继续后续流程");
            next.run();
        });
    }

    private void close(String result, Runnable next) {
        host.showProgress("复活士兵：" + result + "，关闭军营");
        host.tapUi(1235, 54, next);
    }

    private static List<String> screenLines(OcrText text) {
        List<String> values = new ArrayList<>();
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                values.add(line.getText());
            }
        }
        return values;
    }

    static boolean hasCampArrival(List<String> values) {
        for (String value : values) {
            if (value.replaceAll("\\s+", "").contains("士兵卸下")) {
                return true;
            }
        }
        return false;
    }
}
