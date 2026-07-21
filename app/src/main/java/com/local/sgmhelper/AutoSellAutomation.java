package com.local.sgmhelper;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AutoSellAutomation {
    private static final int ARRIVAL_ATTEMPTS = 90;
    private static final int PREFLIGHT_CAPACITY_ATTEMPTS = 3;
    private static final Pattern CAPACITY = Pattern.compile(
            "(\\d{1,3})\\s*[/／]\\s*(\\d{1,3})");

    private final AutomationHost host;

    AutoSellAutomation(AutomationHost host) {
        this.host = host;
    }

    void start(Runnable next) {
        host.startAutomation("Auto sell: opening game", () -> returnToTown(next));
    }

    void checkNearlyFull(int minimumFreeSlots, Consumer<Boolean> result) {
        readNearlyFull(minimumFreeSlots,
                nearlyFull -> result.accept(Boolean.TRUE.equals(nearlyFull)));
    }

    void checkNearlyFullBeforeStart(int minimumFreeSlots, Consumer<Boolean> result) {
        checkNearlyFullBeforeStart(minimumFreeSlots, PREFLIGHT_CAPACITY_ATTEMPTS, result);
    }

    private void checkNearlyFullBeforeStart(
            int minimumFreeSlots, int attemptsRemaining, Consumer<Boolean> result) {
        readNearlyFull(minimumFreeSlots, nearlyFull -> {
            if (nearlyFull != null) {
                result.accept(nearlyFull);
                return;
            }
            if (attemptsRemaining > 1) {
                DiagnosticLog.warn("AUTO_SELL",
                        "Backpack preflight OCR failed; retrying ("
                                + (PREFLIGHT_CAPACITY_ATTEMPTS - attemptsRemaining + 2)
                                + "/" + PREFLIGHT_CAPACITY_ATTEMPTS + ")");
                host.postDelayed(() -> checkNearlyFullBeforeStart(
                        minimumFreeSlots, attemptsRemaining - 1, result), 1_000);
                return;
            }
            host.showProgress("启动前暂未识别到背包容量，5秒后重试");
            host.postDelayed(() -> checkNearlyFullBeforeStart(
                    minimumFreeSlots, PREFLIGHT_CAPACITY_ATTEMPTS, result), 5_000);
        });
    }

    private void readNearlyFull(int minimumFreeSlots, Consumer<Boolean> result) {
        host.recognizeBackpackCapacity(firstValue -> {
            int[] first = parseCapacity(firstValue);
            if (first == null) {
                DiagnosticLog.warn("AUTO_SELL",
                        "Backpack OCR was unreadable: " + firstValue);
                result.accept(null);
                return;
            }
            host.postDelayed(() -> host.recognizeBackpackCapacity(secondValue -> {
                int[] second = parseCapacity(secondValue);
                Boolean nearlyFull = consistentNearlyFull(
                        first, second, minimumFreeSlots);
                DiagnosticLog.info("AUTO_SELL", nearlyFull != null
                        ? "backpack=" + first[0] + "/" + first[1]
                                + " threshold=" + minimumFreeSlots
                        : "backpack OCR was not stable");
                result.accept(nearlyFull);
            }), 1_000);
        });
    }

    static int[] parseCapacity(String value) {
        String normalized = value == null ? "" : value
                .replace('S', '5').replace('s', '5')
                .replace('O', '0').replace('o', '0');
        Matcher matcher = CAPACITY.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        int used = Integer.parseInt(matcher.group(1));
        int total = Integer.parseInt(matcher.group(2));
        return total > 0 && used <= total ? new int[] {used, total} : null;
    }

    static Boolean consistentNearlyFull(
            int[] first, int[] second, int minimumFreeSlots) {
        if (first == null || second == null || first[1] != second[1]) {
            return null;
        }
        boolean firstNearlyFull = first[1] - first[0] < minimumFreeSlots;
        boolean secondNearlyFull = second[1] - second[0] < minimumFreeSlots;
        return firstNearlyFull == secondNearlyFull ? firstNearlyFull : null;
    }

    private void returnToTown(Runnable next) {
        host.showProgress("Auto sell: closing overlays");
        host.tap(640, 20, () -> host.tap(640, 20, () -> {
            host.showProgress("Auto sell: returning to town (1/2)");
            host.tap(1210, 640, () -> {
                host.showProgress("Auto sell: returning to town (2/2)");
                host.tap(1210, 640,
                        () -> host.postDelayed(() -> openAutoPath(next), 9_000));
            });
        }));
    }

    private void openAutoPath(Runnable next) {
        host.showProgress("Auto sell: opening auto path");
        host.closeAutoPathPanel(() -> host.openAutoPathPanel(
                () -> host.tap(1165, 165, () -> selectInn(next))));
    }

    private void selectInn(Runnable next) {
        host.showProgress("Auto sell: selecting Inn");
        host.clickRightText("客栈", () -> waitForInn(next), 5,
                () -> host.failAutomation("Auto sell: Inn was not found"));
    }

    private void waitForInn(Runnable next) {
        host.showProgress("Auto sell: waiting for Inn");
        host.waitForText("女老板", ARRIVAL_ATTEMPTS, () -> sell(next));
    }

    private void sell(Runnable next) {
        host.showProgress("Auto sell: opening Sell");
        host.tap(718, 105, () -> {
            host.showProgress("Auto sell: selling junk");
            host.tap(1080, 621, () -> {
                host.showProgress("Auto sell: quick-selling equipment");
                host.tap(495, 621, () -> {
                    host.showProgress("Auto sell: confirming sale");
                    host.tap(547, 592, () -> {
                        host.showProgress("Auto sell: closing window");
                        host.tap(1233, 55, () -> {
                            host.showProgress("Auto sell: completed");
                            next.run();
                        });
                    });
                });
            });
        });
    }
}
