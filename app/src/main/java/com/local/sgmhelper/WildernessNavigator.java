package com.local.sgmhelper;


final class WildernessNavigator {
    private static final int MAX_ZONE = 24;
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
            "十", "十一", "十二", "十三", "十四", "十五", "十六", "十七",
            "十八", "十九", "二十", "二十一", "二十二", "二十三", "二十四"
    };

    private final AutomationHost host;
    private final String progressPrefix;
    private int targetZone;
    private String targetMonster;
    private WildernessCatalog.Enemy targetEnemy;
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
        host.tapUi(1215, 58, () -> scrollMenuToTop(3));
    }

    void navigateToMonster(String monster, Runnable next) {
        WildernessCatalog.Enemy enemy = WildernessCatalog.enemyByName(monster);
        if (enemy == null) {
            host.failAutomation("荒野没有这种怪：" + monster);
            return;
        }
        targetEnemy = enemy;
        targetMonster = monster;
        completed = next;
        int targetX = enemy.randomX();
        progress("前往" + monster + "区域 " + targetX + "," + WildernessCatalog.ENEMY_Y);
        host.tapMapCoordinate(targetX, WildernessCatalog.ENEMY_Y,
                () -> waitForMonsterArea(MONSTER_ROUTE_RETRY_COUNT));
    }

    void navigateToCoordinate(int x, int y, Runnable next) {
        try {
            TrainingAutomation.parseCustomCoordinate(x, y);
        } catch (IllegalArgumentException error) {
            host.failAutomation("自定义练级坐标无效");
            return;
        }
        targetEnemy = null;
        targetMonster = null;
        completed = next;
        progress("前往自定义坐标 " + x + "," + y);
        host.tapMapCoordinate(x, y, () -> waitForCoordinate(x, y,
                MONSTER_ROUTE_RETRY_COUNT));
    }

    private void waitForMonsterArea(int remainingAttempts) {
        host.recognizeMapCoordinate(value -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            Integer currentX = BossAutomation.parseMapX(value);
            if (currentX != null && targetEnemy.containsX(currentX)) {
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

    private void waitForCoordinate(int targetX, int targetY, int remainingAttempts) {
        host.recognizeMapCoordinate(value -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (TrainingAutomation.coordinateReached(value, targetX, targetY)) {
                progress("已到达自定义坐标 " + targetX + "," + targetY);
                host.closeAutoPathPanel(completed);
            } else if (remainingAttempts > 1) {
                host.postDelayed(() -> waitForCoordinate(
                        targetX, targetY, remainingAttempts - 1), 1_000);
            } else {
                host.failAutomation("前往自定义坐标 " + targetX + "," + targetY + " 超时");
            }
        });
    }

    private void scrollMenuToTop(int remainingSwipes) {
        if (remainingSwipes == 0) {
            progress("查找军团入口");
            // Wudang searches its MenuBox crop only; never OCR the whole HUD
            // while looking for the military entry.
            host.clickTemplateOrText(WudangTemplateMatcher.Template.LEGION_MENU,
                    "军团", true,
                    846, 101, 1238, 595,
                    this::openWildernessFromLegion,
                    2, 5,
                    () -> host.tapUi(995, 270, this::openWildernessFromLegion));
            return;
        }
        host.swipeUi(1000, 180, 1000, 550,
                () -> scrollMenuToTop(remainingSwipes - 1));
    }

    private void openWildernessFromLegion() {
        progress("确认军团窗口");
        host.waitTemplateOrText(
                WudangTemplateMatcher.Template.LEGION,
                "军团", true,
                WudangTemplateMatcher.LEGION_TITLE_LEFT,
                WudangTemplateMatcher.LEGION_TITLE_TOP,
                WudangTemplateMatcher.LEGION_TITLE_LEFT
                        + WudangTemplateMatcher.LEGION_TITLE_WIDTH,
                WudangTemplateMatcher.LEGION_TITLE_TOP
                        + WudangTemplateMatcher.LEGION_TITLE_HEIGHT,
                3, 1,
                this::openWildernessFromLegionWindow,
                () -> host.failAutomation("军团窗口未打开"));
    }

    private void openWildernessFromLegionWindow() {
        progress("进入荒野营地");
        Runnable confirm = () -> host.postDelayed(
                () -> host.tapFast(640, 478,
                        () -> host.tapFast(640, 524,
                                () -> host.postDelayed(
                                        () -> waitForWildernessCamp(
                                                SCREEN_WAIT_RETRY_COUNT), 4_000))),
                300);
        // 军团长页面的按钮在左侧；只搜索右下按钮带，避开左下状态文字。
        host.clickTemplateOrTextFast(WudangTemplateMatcher.Template.WILDERNESS_TRAINING,
                "荒野修炼", true,
                520, 520, 1_050, 670,
                confirm, 1, 2,
                () -> host.tapUi(780, 613, confirm));
    }

    private void waitForWildernessCamp(int remainingAttempts) {
        progress("等待荒野营地加载");
        host.recognizeMapName(value -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (isWildernessCampMapName(value)) {
                findWildernessTeleporter();
            } else if (remainingAttempts > 1) {
                host.postDelayed(
                        () -> waitForWildernessCamp(remainingAttempts - 1),
                        1_000);
            } else {
                host.failAutomation("等待荒野营地加载超时");
            }
        });
    }

    private void findWildernessTeleporter() {
        progress("寻找荒野传送官");
        host.closeAutoPathPanel(() -> {
            progress("打开自动寻路");
            host.openAutoPathPanel(
                    () -> host.tapUi(1165, 165,
                            () -> host.tapUi(TELEPORTER_X, TELEPORTER_Y,
                                    () -> host.postDelayed(
                                            () -> clickPages(page(targetZone)), 5_000))));
        });
    }

    private void clickPages(int remainingPages) {
        if (remainingPages == 0) {
            progress("选择荒野修炼" + targetZone + "区");
            host.tapUi(DIALOG_X, zoneRowY(targetZone),
                    () -> waitForMap(MAP_LOADING_RETRY_COUNT));
            return;
        }
        progress("荒野传送列表翻到下一页");
        host.tapUi(DIALOG_X, NEXT_PAGE_Y,
                () -> clickPages(remainingPages - 1));
    }

    private void waitForMap(int remainingAttempts) {
        progress("等待荒野修炼" + targetZone + "区加载");
        host.recognizeMapName(value -> {
            if (!host.isAutomationRunning()) {
                return;
            }
            if (isSelectedMapName(value, targetZone)) {
                host.closeAutoPathPanel(completed);
                return;
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
        if (zone < 1 || zone > MAX_ZONE) {
            throw new IllegalArgumentException("zone must be between 1 and " + MAX_ZONE);
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

    static boolean isWildernessCampMapName(String value) {
        return value != null && value.replaceAll("\\s+", "").contains("荒野营地");
    }

    static boolean isWildernessMapName(String value) {
        String normalized = value.replaceAll("\\s+", "").replace("修练", "修炼");
        return normalized.contains("荒野修炼") && normalized.contains("区");
    }

    /** 怪物的 x 区间，取自武当 {@code arrays.xml} 的 {@code wild} 表。 */
    static int[] monsterXRange(String monster) {
        WildernessCatalog.Enemy enemy = WildernessCatalog.enemyByName(monster);
        if (enemy == null) {
            throw new IllegalArgumentException("unknown monster: " + monster);
        }
        return new int[] {enemy.xRanges[0][0], enemy.xRanges[enemy.xRanges.length - 1][1]};
    }
}
