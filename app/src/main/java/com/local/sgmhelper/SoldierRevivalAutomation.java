package com.local.sgmhelper;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;

final class SoldierRevivalAutomation {
    private static final int CAMP_SCROLL_ATTEMPTS = 4;
    private static final int ARRIVAL_ATTEMPTS = 30;

    private final AutomationHost host;

    SoldierRevivalAutomation(AutomationHost host) {
        this.host = host;
    }

    void start() {
        if (host.isAutomationRunning()) {
            host.showProgress("已有任务正在运行");
            return;
        }
        host.startAutomation("复活士兵：打开游戏", () -> run(host::completeAutomation));
    }

    void run(Runnable next) {
        host.ensureGameHudVisible(() -> openCampSearch(true, next));
    }

    private void openCampSearch(boolean mayReturnHome, Runnable next) {
        host.showProgress("复活士兵：打开右侧自动寻路");
        host.closeAutoPathPanel(() -> host.openAutoPathPanel(
                () -> findCamp(CAMP_SCROLL_ATTEMPTS, mayReturnHome, next)));
    }

    private void findCamp(int remainingScrolls, boolean mayReturnHome, Runnable next) {
        host.showProgress("复活士兵：查找军营");
        host.clickRightText("军营", () -> waitForCamp(ARRIVAL_ATTEMPTS, next), 2, () -> {
            if (remainingScrolls > 0) {
                host.swipe(1120, 450, 1120, 230,
                        () -> findCamp(remainingScrolls - 1, mayReturnHome, next));
            } else if (mayReturnHome) {
                host.showProgress("复活士兵：未找到军营，先回城");
                host.closeAutoPathPanel(() -> host.returnHome(
                        () -> host.postDelayed(() -> openCampSearch(false, next), 5_000)));
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
                host.postDelayed(() -> waitForCamp(remainingAttempts - 1, next), 1_000);
            } else {
                skip("等待到达军营超时", next);
            }
        });
    }

    private void revive(Runnable next) {
        host.showProgress("复活士兵：打开复活页");
        host.tap(573, 107, () -> host.clickText("复活全部", false,
                () -> confirmIfNeeded(next), 10,
                () -> close("未找到复活全部士兵", next)));
    }

    private void confirmIfNeeded(Runnable next) {
        host.showProgress("复活士兵：检查确认框");
        host.clickText("确认", true, () -> close("已复活全部士兵", next), 3,
                () -> host.clickText("确定", true,
                        () -> close("已复活全部士兵", next), 1,
                        () -> close("未出现确认框", next)));
    }

    private void skip(String reason, Runnable next) {
        host.closeAutoPathPanel(() -> {
            host.showProgress("复活士兵：" + reason + "，继续后续流程");
            next.run();
        });
    }

    private void close(String result, Runnable next) {
        host.showProgress("复活士兵：" + result + "，关闭军营");
        host.tap(1235, 54, next);
    }

    private static List<String> screenLines(Text text) {
        List<String> values = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
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
