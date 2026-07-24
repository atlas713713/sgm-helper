package com.local.sgmhelper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WelfareAutomation {
    private static final int MAX_PAGES = 4;
    private static final int TOP_SWIPES = 4;
    private final AutomationHost host;
    private final List<WelfareCategory> categories = List.of(
            new HeroesStoreCategory(),
            new OnlineRewardCategory(),
            new DailySignInCategory(),
            new DailyChallengeCategory(),
            new WeeklyChallengeCategory(),
            new HeroesGloryCategory(),
            new DailySupportCategory());
    private final Set<String> processed = new HashSet<>();
    private Runnable completion;

    WelfareAutomation(AutomationHost host) {
        this.host = host;
    }

    void start() {
        completion = host::resumePrimaryTask;
        host.startAutomation("领取福利：打开游戏", this::begin);
    }

    void startInGame(Runnable next) {
        completion = next;
        begin();
    }

    private void begin() {
        processed.clear();
        host.tap(945, 60,
                () -> host.postDelayed(() -> scrollToTop(TOP_SWIPES), 2_000));
    }

    private void scrollToTop(int remaining) {
        if (remaining == 0) {
            host.showProgress("领取福利：确认群英商店");
            host.waitForText("群英商店", 5, () -> scanPage(0));
            return;
        }
        host.showProgress("领取福利：返回类目顶部");
        host.swipe(150, 180, 150, 630, () -> scrollToTop(remaining - 1));
    }

    private void scanPage(int page) {
        if (page >= MAX_PAGES) {
            host.failAutomation("领取福利：滑动后仍未找到每日应援");
            return;
        }
        host.showProgress("领取福利：检查有红点的类目");
        host.recognizeText(text -> {
            List<VisibleCategory> visible = visibleCategories(text);
            boolean lastPage = visible.stream()
                    .anyMatch(item -> "每日应援".equals(item.category.name()));
            host.captureScreenshot(bitmap -> {
                if (bitmap == null) {
                    host.failAutomation("领取福利：无法截屏检测红点");
                    return;
                }
                List<VisibleCategory> pending = new ArrayList<>();
                try {
                    for (VisibleCategory item : visible) {
                        if (!processed.contains(item.category.name())
                                && hasRedDot(bitmap, item.bounds)) {
                            pending.add(item);
                        }
                    }
                } finally {
                    bitmap.recycle();
                }
                processCategories(pending, 0, () -> {
                    if (lastPage) {
                        finish();
                    } else {
                        host.showProgress("领取福利：查看下一页类目");
                        host.swipe(150, 620, 150, 180, () -> scanPage(page + 1));
                    }
                });
            });
        });
    }

    private void processCategories(List<VisibleCategory> pending, int index, Runnable next) {
        if (index >= pending.size()) {
            next.run();
            return;
        }
        VisibleCategory item = pending.get(index);
        processed.add(item.category.name());
        host.showProgress("领取福利：" + item.category.name());
        host.tap(item.bounds.centerX(), item.bounds.centerY(),
                () -> item.category.claim(host,
                        () -> processCategories(pending, index + 1, next)));
    }

    private List<VisibleCategory> visibleCategories(OcrText text) {
        List<VisibleCategory> result = new ArrayList<>();
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                WelfareCategory category = categoryForText(line.getText());
                if (bounds != null && bounds.centerX() < 280 && category != null) {
                    result.add(new VisibleCategory(category, bounds));
                }
            }
        }
        return result;
    }

    private WelfareCategory categoryForText(String value) {
        String name = matchingCategoryName(value);
        for (WelfareCategory category : categories) {
            if (category.name().equals(name)) {
                return category;
            }
        }
        return null;
    }

    private void finish() {
        Runnable resume = () -> {
            host.showProgress("领取福利完成，10秒后继续之前任务");
            host.postDelayed(completion, 10_000);
        };
        host.closeWelfareWindow(resume);
    }

    static String matchingCategoryName(String value) {
        String normalized = normalize(value);
        for (String name : List.of("群英商店", "在线奖励", "每日签到", "每日挑战",
                "每周挑战", "群英荣耀无限", "每日应援")) {
            if (normalized.contains(name)) {
                return name;
            }
        }
        return null;
    }

    static boolean isRedDotPixel(int red, int green, int blue) {
        return red >= 210 && green <= 90 && blue <= 90;
    }

    private static boolean hasRedDot(Bitmap bitmap, Rect labelBounds) {
        int left = 242 * bitmap.getWidth() / 1280;
        int right = 264 * bitmap.getWidth() / 1280;
        int top = Math.max(0, labelBounds.centerY() - 90 * bitmap.getHeight() / 720);
        int bottom = Math.max(top, labelBounds.centerY() - 55 * bitmap.getHeight() / 720);
        int redPixels = 0;
        for (int y = top; y < Math.min(bottom, bitmap.getHeight()); y++) {
            for (int x = left; x < Math.min(right, bitmap.getWidth()); x++) {
                int color = bitmap.getPixel(x, y);
                if (isRedDotPixel(Color.red(color), Color.green(color), Color.blue(color))) {
                    redPixels++;
                }
            }
        }
        return redPixels >= 25;
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", "");
    }

    private record VisibleCategory(WelfareCategory category, Rect bounds) {
    }
}

abstract class WelfareCategory {
    private static final int MAX_CLAIMS = 12;

    abstract String name();

    void claim(AutomationHost host, Runnable next) {
        claimVisible(host, MAX_CLAIMS, next);
    }

    final void claimVisible(AutomationHost host, Runnable next) {
        claimVisible(host, MAX_CLAIMS, next);
    }

    private void claimVisible(AutomationHost host, int remaining, Runnable next) {
        if (remaining == 0) {
            next.run();
            return;
        }
        host.clickText("领取", true,
                () -> claimVisible(host, remaining - 1, next), 2, next);
    }
}

final class HeroesStoreCategory extends WelfareCategory {
    @Override String name() { return "群英商店"; }
}

final class OnlineRewardCategory extends WelfareCategory {
    @Override String name() { return "在线奖励"; }

    @Override
    void claim(AutomationHost host, Runnable next) {
        host.showProgress("领取福利：在线奖励向左滑动");
        host.swipe(1080, 450, 420, 450,
                () -> claimPage(host,
                        () -> {
                            host.showProgress("领取福利：在线奖励向右滑动");
                            host.swipe(420, 450, 1080, 450,
                                    () -> claimPage(host, next));
                        }));
    }

    private void claimPage(AutomationHost host, Runnable next) {
        claimVisible(host, () -> tapRewardSlots(host, 0, next));
    }

    private void tapRewardSlots(AutomationHost host, int index, Runnable next) {
        int[] slots = {388, 570, 750, 932, 1114};
        if (index >= slots.length) {
            next.run();
            return;
        }
        host.tap(slots[index], 600,
                () -> tapRewardSlots(host, index + 1, next));
    }
}

final class DailySignInCategory extends WelfareCategory {
    @Override String name() { return "每日签到"; }
}

final class DailyChallengeCategory extends WelfareCategory {
    private static final Pattern COMPLETION_COUNT = Pattern.compile("(\\d+)[/／]7");

    @Override String name() { return "每日挑战"; }

    @Override
    void claim(AutomationHost host, Runnable next) {
        claimVisible(host, () -> readCompletionCount(host, next, 5));
    }

    private void readCompletionCount(AutomationHost host, Runnable next, int remainingAttempts) {
        host.showProgress("领取福利：读取每日完成奖励次数");
        host.recognizeText(text -> {
            StringBuilder value = new StringBuilder();
            for (OcrText.TextBlock block : text.getTextBlocks()) {
                value.append(block.getText()).append(' ');
            }
            Integer count = completionCount(value.toString());
            if (count != null) {
                host.showProgress("领取福利：每日挑战已完成" + count + "次");
                tapCompletionRewards(host, rewardSlotsForCount(count), 0, next);
            } else if (remainingAttempts > 1) {
                host.postDelayed(
                        () -> readCompletionCount(host, next, remainingAttempts - 1),
                        1_000);
            } else {
                host.failAutomation("领取福利：无法识别每日完成奖励 x/7");
            }
        });
    }

    private void tapCompletionRewards(
            AutomationHost host, List<Integer> slots, int index, Runnable next) {
        if (index >= slots.size()) {
            next.run();
            return;
        }
        host.tap(slots.get(index), 550,
                () -> tapCompletionRewards(host, slots, index + 1, next));
    }

    static Integer completionCount(String value) {
        Matcher matcher = COMPLETION_COUNT.matcher(value.replaceAll("\\s+", ""));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    static List<Integer> rewardSlotsForCount(int count) {
        List<Integer> result = new ArrayList<>();
        int[] thresholds = {1, 3, 5, 7};
        int[] slots = {565, 755, 945, 1135};
        for (int index = 0; index < thresholds.length; index++) {
            if (count >= thresholds[index]) {
                result.add(slots[index]);
            }
        }
        return result;
    }
}

final class WeeklyChallengeCategory extends WelfareCategory {
    @Override String name() { return "每周挑战"; }
}

final class HeroesGloryCategory extends WelfareCategory {
    @Override String name() { return "群英荣耀无限"; }
}

final class DailySupportCategory extends WelfareCategory {
    @Override String name() { return "每日应援"; }

    @Override
    void claim(AutomationHost host, Runnable next) {
        host.showProgress("领取福利：每日应援 OCR 查找立即领取");
        host.clickText("立即领取", true, next, 5, next);
    }
}
