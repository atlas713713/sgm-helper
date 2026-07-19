package com.local.sgmhelper;

import android.graphics.Rect;

import com.google.mlkit.vision.text.Text;

import java.util.concurrent.ThreadLocalRandom;

final class WildernessNavigator {
    private static final int SCREEN_WAIT_RETRY_COUNT = 20;
    private static final int MAP_LOADING_RETRY_COUNT = 30;
    private static final int MONSTER_ROUTE_RETRY_COUNT = 90;
    private static final int TELEPORTER_X = 1095;
    private static final int TELEPORTER_Y = 228;
    private static final int DIALOG_X = 250;
    private static final int FIRST_ZONE_Y = 450;
    private static final int ZONE_GAP_Y = 62;
    private static final int NEXT_PAGE_Y = 635;
    private static final String[] CHINESE_ZONES = {
            "", "一", "二", "三", "四", "五", "六", "七", "八", "九",
            "十", "十一", "十二", "十三", "十四", "十五"
    };

    private final AutomationHost host;
    private final String progressPrefix;
    private int targetZone;
    private String targetMonster;
    private int targetMonsterMinX;
    private int targetMonsterMaxX;
    private Runnable completed;

    WildernessNavigator(AutomationHost host, String progressPrefix) {
        this.host = host;
        this.progressPrefix = progressPrefix;
    }

    void navigateToZone(int zone, Runnable next) {
        page(zone);
        targetZone = zone;
        completed = next;
        progress("前往荒野修炼" + zone + "区");
        host.tap(1215, 58, () -> scrollMenuToTop(3));
    }

    void navigateToMonster(String monster, Runnable next) {
        int[] xRange = monsterXRange(monster);
        targetMonster = monster;
        targetMonsterMinX = xRange[0];
        targetMonsterMaxX = xRange[1];
        completed = next;
        int targetX = ThreadLocalRandom.current().nextInt(
                targetMonsterMinX, targetMonsterMaxX + 1);
        int targetY = ThreadLocalRandom.current().nextInt(
                AutomationHost.MAP_GAME_MAX_Y + 1);
        progress("前往" + monster + "区域 " + targetX + "," + targetY);
        host.tapMapCoordinate(targetX, targetY,
                () -> waitForMonsterArea(MONSTER_ROUTE_RETRY_COUNT));
    }

    private void waitForMonsterArea(int remainingAttempts) {
        host.recognizeMapCoordinate(value -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            Integer currentX = BossAutomation.parseMapX(value);
            if (currentX != null
                    && currentX >= targetMonsterMinX && currentX <= targetMonsterMaxX) {
                progress("已到达" + targetMonster + "区域");
                host.closeAutoPathPanel(completed);
                return;
            }
            if (remainingAttempts > 1) {
                host.postDelayed(
                        () -> waitForMonsterArea(remainingAttempts - 1), 1_000);
            } else {
                host.failAutomation("前往" + targetMonster + "区域超时");
            }
        });
    }

    private void scrollMenuToTop(int remainingSwipes) {
        if (remainingSwipes == 0) {
            progress("查找军团入口");
            host.clickText("军团", true, this::openWildernessFromLegion,
                    5, () -> host.tap(995, 270, this::openWildernessFromLegion));
            return;
        }
        host.swipe(1000, 180, 1000, 550,
                () -> scrollMenuToTop(remainingSwipes - 1));
    }

    private void openWildernessFromLegion() {
        progress("进入荒野营地");
        host.tap(780, 613,
                () -> host.tap(640, 478,
                        () -> host.postDelayed(
                                () -> host.waitForText("荒野营地",
                                        SCREEN_WAIT_RETRY_COUNT,
                                        this::findWildernessTeleporter),
                                4_000)));
    }

    private void findWildernessTeleporter() {
        progress("寻找荒野传送官");
        host.closeAutoPathPanel(() -> host.tap(1130, 500,
                () -> host.tap(1165, 165,
                        () -> host.tap(TELEPORTER_X, TELEPORTER_Y,
                                () -> host.postDelayed(
                                        () -> clickPages(page(targetZone)), 5_000)))));
    }

    private void clickPages(int remainingPages) {
        if (remainingPages == 0) {
            progress("选择荒野修炼" + targetZone + "区");
            host.tap(DIALOG_X, zoneRowY(targetZone),
                    () -> waitForMap(MAP_LOADING_RETRY_COUNT));
            return;
        }
        progress("荒野传送列表翻到下一页");
        host.tap(DIALOG_X, NEXT_PAGE_Y,
                () -> clickPages(remainingPages - 1));
    }

    private void waitForMap(int remainingAttempts) {
        progress("等待荒野修炼" + targetZone + "区加载");
        host.recognizeText(text -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    Rect bounds = line.getBoundingBox();
                    if (bounds != null && bounds.centerX() > 300 && bounds.centerX() < 850
                            && bounds.centerY() > 400
                            && isSelectedMapName(line.getText(), targetZone)) {
                        host.closeAutoPathPanel(completed);
                        return;
                    }
                }
            }
            if (remainingAttempts > 1) {
                host.postDelayed(() -> waitForMap(remainingAttempts - 1), 1_000);
            } else {
                host.failAutomation("等待荒野修炼" + targetZone + "区加载超时");
            }
        });
    }

    private void progress(String value) {
        host.showProgress(progressPrefix + "：" + value);
    }

    static int page(int zone) {
        if (zone < 1 || zone > 15) {
            throw new IllegalArgumentException("zone must be between 1 and 15");
        }
        return (zone - 1) / 3;
    }

    static int zoneRowY(int zone) {
        page(zone);
        return FIRST_ZONE_Y + (zone - 1) % 3 * ZONE_GAP_Y;
    }

    static boolean isSelectedMapName(String value, int zone) {
        page(zone);
        String normalized = value.replaceAll("\\s+", "").replace("修练", "修炼");
        return normalized.contains("荒野修炼")
                && (normalized.contains(zone + "区")
                        || normalized.contains(CHINESE_ZONES[zone] + "区"));
    }

    static boolean isWildernessMapName(String value) {
        String normalized = value.replaceAll("\\s+", "").replace("修练", "修炼");
        return normalized.contains("荒野修炼") && normalized.contains("区");
    }

    static int[] monsterXRange(String monster) {
        return switch (monster) {
            case "食人花" -> new int[] {320, 410};
            case "金甲龙" -> new int[] {410, 510};
            case "圣武士" -> new int[] {520, 600};
            case "魔斗士" -> new int[] {10, 150};
            case "海妖" -> new int[] {180, 260};
            case "螳螂巨妖" -> new int[] {470, 600};
            case "九尾狐", "人面鸟", "式神童子" -> new int[] {0, 280};
            case "石狮精", "战鬼", "剑齿虎" -> new int[] {350, 600};
            default -> throw new IllegalArgumentException("unknown monster: " + monster);
        };
    }
}
