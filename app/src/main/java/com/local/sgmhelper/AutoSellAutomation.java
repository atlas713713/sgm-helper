package com.local.sgmhelper;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AutoSellAutomation {
    private static final int ARRIVAL_ATTEMPTS = 90;
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
        host.recognizeBackpackCapacity(firstValue -> {
            int[] first = parseCapacity(firstValue);
            if (first == null) {
                DiagnosticLog.warn("AUTO_SELL",
                        "Backpack OCR was unreadable: " + firstValue);
                result.accept(false);
                return;
            }
            host.postDelayed(() -> host.recognizeBackpackCapacity(secondValue -> {
                int[] second = parseCapacity(secondValue);
                boolean stable = second != null
                        && first[0] == second[0] && first[1] == second[1];
                DiagnosticLog.info("AUTO_SELL", stable
                        ? "backpack=" + first[0] + "/" + first[1]
                                + " threshold=" + minimumFreeSlots
                        : "backpack OCR was not stable");
                result.accept(stable && first[1] - first[0] < minimumFreeSlots);
            }), 1_000);
        });
    }

    static int[] parseCapacity(String value) {
        Matcher matcher = CAPACITY.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            return null;
        }
        int used = Integer.parseInt(matcher.group(1));
        int total = Integer.parseInt(matcher.group(2));
        return total > 0 && used <= total ? new int[] {used, total} : null;
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
        host.tap(1248, 147,
                () -> host.tap(1130, 500,
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
