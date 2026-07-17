package com.local.sgmhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class HelperAccessibilityServiceTest {
    @Test
    public void prependsCurrentPrimaryTaskToProgress() {
        assertEquals("【练级】 自动军务：检查任务",
                HelperAccessibilityService.formatProgress(
                        AutomationHost.PrimaryTask.TRAINING, "自动军务：检查任务"));
        assertEquals("【BOSS】 野王：搜索坐标",
                HelperAccessibilityService.formatProgress(
                        AutomationHost.PrimaryTask.BOSS, "野王：搜索坐标"));
        assertEquals("【副本】 扫荡副本：确认奖励",
                HelperAccessibilityService.formatProgress(
                        AutomationHost.PrimaryTask.DUNGEON, "扫荡副本：确认奖励"));
    }

    @Test
    public void createsOneSafeLogFilePerSimulator() {
        assertEquals("sgmhelper-fea64ace64515149.log",
                DiagnosticLog.logFileName("fea64ace64515149"));
        assertEquals("sgmhelper-unsafe_id.log",
                DiagnosticLog.logFileName("unsafe/id"));
    }

    @Test
    public void extractsWildernessQuestNumber() {
        assertEquals("十二", TaskAutomation.extractWildernessZone(
                "巡狩军团荒野（十二）(0/2)"));
        assertEquals("四", TaskAutomation.extractWildernessZone(
                "巡狩军团荒野 ( 四 )"));
        assertNull(TaskAutomation.extractWildernessZone("补充军团物资（十二）"));
    }

    @Test
    public void recognizesAllMilitaryQuestNames() {
        assertEquals("补充军团物资", TaskAutomation.extractMilitaryQuestName(
                "补充军团物资（五）(0/2)"));
        assertEquals("巡狩军团荒野", TaskAutomation.extractMilitaryQuestName(
                "巡狩军团荒野（五）(0/2)"));
        assertEquals("勇讨军团天将", TaskAutomation.extractMilitaryQuestName(
                "勇讨军团天将（五）(0/2)"));
        assertNull(TaskAutomation.extractMilitaryQuestName("一般任务"));
    }

    @Test
    public void usesTheNpcForEachExecutableMilitaryQuest() {
        assertEquals("常务军士",
                TaskAutomation.expectedMilitaryNpc("补充军团物资"));
        assertEquals("荒野军士",
                TaskAutomation.expectedMilitaryNpc("巡狩军团荒野"));
        assertNull(TaskAutomation.expectedMilitaryNpc("勇讨军团天将"));
    }

    @Test
    public void recognizesCompletedMilitaryMaterials() {
        assertEquals(Boolean.TRUE, TaskAutomation.requiredItemComplete(
                "取得精制丝绸 (250/250) 个"));
        assertEquals(Boolean.FALSE, TaskAutomation.requiredItemComplete(
                "取得精制丝绸 (249/250) 个"));
        assertEquals(Boolean.TRUE, TaskAutomation.requiredItemComplete(
                "取得道具（300/250）个"));
        assertEquals(Boolean.TRUE, TaskAutomation.requiredItemComplete(
                "取得铜铸茶壶 250 ／ 250 个 [56尺]"));
        assertNull(TaskAutomation.requiredItemComplete("军团财富：50"));
    }

    @Test
    public void recognizesCompletedMilitaryGreenMark() {
        assertTrue(TaskAutomation.isCompletionGreen(20, 220, 35));
        assertFalse(TaskAutomation.isCompletionGreen(210, 190, 30));
        assertFalse(TaskAutomation.isCompletionGreen(30, 120, 220));
    }

    @Test
    public void explicitIncompleteProgressOverridesAnotherGreenMark() {
        assertTrue(TaskAutomation.isMilitaryQuestComplete(Boolean.TRUE, false));
        assertTrue(TaskAutomation.isMilitaryQuestComplete(null, true));
        assertFalse(TaskAutomation.isMilitaryQuestComplete(Boolean.FALSE, true));
        assertFalse(TaskAutomation.isMilitaryQuestComplete(null, false));
    }

    @Test
    public void wildernessReturnsHomeBeforeTurningIn() {
        assertTrue(TaskAutomation.requiresReturnHomeBeforeTurnIn("巡狩军团荒野"));
        assertFalse(TaskAutomation.requiresReturnHomeBeforeTurnIn("补充军团物资"));
    }

    @Test
    public void recognizesMilitaryQuestCooldown() {
        assertTrue(TaskAutomation.isCooldownText("冷却时间：2小时"));
        assertTrue(TaskAutomation.isCooldownText("冷却\n时间：09分05秒"));
        assertTrue(TaskAutomation.isCooldownText("冷卻時間：2小時"));
        assertTrue(TaskAutomation.isCooldownText("冷却 09分05秒"));
        assertFalse(TaskAutomation.isCooldownText("承接等级：61"));
        assertEquals(Integer.valueOf(120),
                TaskAutomation.extractCooldownMinutes("冷却时间：2小时"));
        assertEquals(Integer.valueOf(150),
                TaskAutomation.extractCooldownMinutes("冷却时间:2小时30分钟"));
        assertEquals(Integer.valueOf(30),
                TaskAutomation.extractCooldownMinutes("冷却时间：30分钟"));
        assertEquals(Integer.valueOf(32),
                TaskAutomation.extractCooldownMinutes("冷却时间:31分49秒"));
        assertEquals(Integer.valueOf(10),
                TaskAutomation.extractCooldownMinutes("冷却 09分05秒"));
        assertNull(TaskAutomation.extractCooldownMinutes("冷却时间"));
    }

    @Test
    public void waitsOnlyWhenBothExecutableMilitaryQuestsAreCooling() {
        assertTrue(TaskAutomation.allExecutableMilitaryQuestsCooling(true, true));
        assertFalse(TaskAutomation.allExecutableMilitaryQuestsCooling(true, false));
        assertFalse(TaskAutomation.allExecutableMilitaryQuestsCooling(false, true));
    }

    @Test
    public void checksOnlyOngoingCollectionQuestsAndPrefersWilderness() {
        assertEquals("巡狩军团荒野", TaskAutomation.firstOngoingCollectionQuest(
                Arrays.asList("补充军团物资（五）", "巡狩军团荒野（五）")));
        assertEquals("补充军团物资", TaskAutomation.firstOngoingCollectionQuest(
                Arrays.asList("勇讨军团天将（五）", "补充军团物资（五）")));
        assertNull(TaskAutomation.firstOngoingCollectionQuest(
                Arrays.asList("勇讨军团天将（五）")));
    }

    @Test
    public void checksSupplyMoreOftenThanWilderness() {
        assertEquals(3 * 60 * 1_000,
                TaskAutomation.progressCheckDelayMillis("补充军团物资"));
        assertEquals(5 * 60 * 1_000,
                TaskAutomation.progressCheckDelayMillis("巡狩军团荒野"));
    }

    @Test
    public void resumesOnlySupplyCollectionFromTheRightTaskBar() {
        assertTrue(TaskAutomation.shouldResumeSupplyCollection("补充军团物资"));
        assertFalse(TaskAutomation.shouldResumeSupplyCollection("巡狩军团荒野"));
    }

    @Test
    public void recognizesWildernessTrainingLocationBeforeCollectionCheck() {
        assertTrue(TaskAutomation.isWildernessTrainingLocation(
                Arrays.asList("荒野修炼2区")));
        assertTrue(TaskAutomation.isWildernessTrainingLocation(
                Arrays.asList("荒野修练 二 区")));
        assertFalse(TaskAutomation.isWildernessTrainingLocation(
                Arrays.asList("荒野营地", "巡狩军团荒野（五）")));
        assertTrue(TaskAutomation.shouldEnterWilderness("巡狩军团荒野", false));
        assertFalse(TaskAutomation.shouldEnterWilderness("巡狩军团荒野", true));
        assertFalse(TaskAutomation.shouldEnterWilderness("补充军团物资", false));
    }

    @Test
    public void acceptsAnyLoadedWildernessTrainingZoneAndTaskStartSignal() {
        assertTrue(TaskAutomation.isWildernessTrainingMapName("荒野修炼1区"));
        assertTrue(TaskAutomation.isWildernessTrainingMapName("荒野修练 六 区"));
        assertFalse(TaskAutomation.isWildernessTrainingMapName("荒野营地"));
        assertTrue(TaskAutomation.hasTaskExecutionSignal(
                Arrays.asList("执行任务：巡狩军团荒野（五）的自动寻路")));
        assertFalse(TaskAutomation.hasTaskExecutionSignal(
                Arrays.asList("巡狩军团荒野收集中")));
    }

    @Test
    public void schedulesTheEarliestIndependentMilitaryTask() {
        assertTrue(WorshipAlarmReceiver.hasConfiguredMilitaryTime(true, true));
        assertFalse(WorshipAlarmReceiver.hasConfiguredMilitaryTime(true, false));
        assertFalse(WorshipAlarmReceiver.hasConfiguredMilitaryTime(false, true));
        assertEquals(6 * 60 * 60 * 1_000L + 1_000,
                WorshipAlarmReceiver.nextRollingMilitaryAt(1_000));
        assertEquals(2_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 2_000, 5_000, 10_000));
        assertEquals(5_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 900, 5_000, 10_000));
        assertEquals(10_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 0, 0, 10_000));
    }

    @Test
    public void schedulesTheSecondWelfareRunSixHoursLater() {
        assertEquals(18 * 60 + 5,
                WorshipAlarmReceiver.welfareSecondMinuteOfDay(12, 5));
        assertEquals(2 * 60 + 30,
                WorshipAlarmReceiver.welfareSecondMinuteOfDay(20, 30));
    }

    @Test
    public void retriesOnlyScreenshotIntervalErrors() {
        assertTrue(HelperAccessibilityService.shouldRetryScreenshot(3, 2));
        assertFalse(HelperAccessibilityService.shouldRetryScreenshot(3, 1));
        assertFalse(HelperAccessibilityService.shouldRetryScreenshot(2, 5));
    }

    @Test
    public void usesLongerPrimaryRecoveryAfterThreeFailures() {
        assertTrue(HelperAccessibilityService.shouldRetryAutomation(1));
        assertTrue(HelperAccessibilityService.shouldRetryAutomation(3));
        assertFalse(HelperAccessibilityService.shouldRetryAutomation(4));
    }

    @Test
    public void runsMilitaryOnlyDuringTraining() {
        assertTrue(HelperAccessibilityService.shouldRunMilitary(
                AutomationHost.PrimaryTask.TRAINING));
        assertFalse(HelperAccessibilityService.shouldRunMilitary(
                AutomationHost.PrimaryTask.BOSS));
        assertFalse(HelperAccessibilityService.shouldRunMilitary(
                AutomationHost.PrimaryTask.DUNGEON));
    }

    @Test
    public void ongoingQuestClicksStayInsideTheTaskList() {
        assertTrue(HelperAccessibilityService.isHorizontalMatch(320, 0, 640));
        assertFalse(HelperAccessibilityService.isHorizontalMatch(1_050, 0, 640));
    }

    @Test
    public void mergesQuickArrivalOcrFragments() {
        assertTrue(HelperAccessibilityService.matchesQuickArrivalFragments(
                Arrays.asList("快速", "抵达")));
        assertTrue(HelperAccessibilityService.matchesQuickArrivalFragments(
                Arrays.asList("抵达")));
        assertFalse(HelperAccessibilityService.matchesQuickArrivalFragments(
                Arrays.asList("放弃任务")));
    }

    @Test
    public void recognizesAnAlreadyOpenTaskWindow() {
        assertTrue(TaskAutomation.isTaskWindowVisible(
                Arrays.asList("任务", "进行中", "可承接")));
        assertTrue(TaskAutomation.isTaskWindowVisible(
                Arrays.asList("任务", "进行中")));
        assertTrue(TaskAutomation.isTaskWindowVisible(
                Arrays.asList("任务", "可承接")));
        assertTrue(TaskAutomation.isTaskWindowVisible(
                Arrays.asList("任务计数：2/20", "任务目标")));
        assertTrue(TaskAutomation.isTaskWindowVisible(
                Arrays.asList("放弃任务", "快速抵达")));
        assertFalse(TaskAutomation.isTaskWindowVisible(
                Arrays.asList("任务目标")));
        assertFalse(TaskAutomation.isTaskWindowVisible(
                Arrays.asList("右侧任务栏", "进行中")));
    }

    @Test
    public void recognizesMergedAutoPathPanelLabels() {
        assertTrue(HelperAccessibilityService.hasAutoPathPanelLabels(
                Arrays.asList("敌人 寻路")));
        assertTrue(HelperAccessibilityService.hasAutoPathPanelLabels(
                Arrays.asList("寻路")));
        assertFalse(HelperAccessibilityService.hasAutoPathPanelLabels(
                Arrays.asList("自动寻路", "巡狩军团荒野")));
    }

    @Test
    public void recognizesTheWelfareWindowException() {
        assertTrue(ScreenGuard.isWelfareWindow(
                Arrays.asList("福利", "群英商店", "在线奖励")));
        assertFalse(ScreenGuard.isWelfareWindow(
                Arrays.asList("福利", "菜单")));
    }

    @Test
    public void recognizesEveryLoginScreenBeforeTheCoveredStartButton() {
        assertEquals(LoginAutomation.Screen.ANNOUNCEMENT,
                LoginAutomation.screenFor(Arrays.asList("最新公告", "今日内不再弹出")));
        assertEquals(LoginAutomation.Screen.QUICK_LOGIN,
                LoginAutomation.screenFor(Arrays.asList("快捷登录", "账号登录/注册", "开始游戏")));
        assertEquals(LoginAutomation.Screen.ACCOUNT_LOGIN,
                LoginAutomation.screenFor(Arrays.asList("账号登录", "账号", "密码", "开始游戏")));
        assertEquals(LoginAutomation.Screen.START,
                LoginAutomation.screenFor(Arrays.asList("开始游戏", "S4-白虎")));
        assertEquals(LoginAutomation.Screen.WELFARE,
                LoginAutomation.screenFor(Arrays.asList("福利", "在线奖励", "累积在线")));
        assertEquals(LoginAutomation.Screen.UNCLAIMED_REWARDS,
                LoginAutomation.screenFor(Arrays.asList("尚未领取的奖励", "关闭界面", "前往领取")));
        assertEquals(LoginAutomation.Screen.LOGGED_IN,
                LoginAutomation.screenFor(Arrays.asList("商城", "福利", "竞技场", "菜单")));
        assertEquals(LoginAutomation.Screen.UNKNOWN,
                LoginAutomation.screenFor(Arrays.asList("正在连接服务器")));
    }

    @Test
    public void convertsGameMapCoordinatesToSafeScreenTaps() {
        assertEquals(640, AutomationHost.mapScreenX(300));
        assertEquals(650, AutomationHost.mapScreenY(25));
        assertEquals(689, AutomationHost.mapScreenY(50));
        assertEquals(437, AutomationHost.mapScreenX(0));
        assertEquals(612, AutomationHost.mapScreenY(0));
        assertEquals(843, AutomationHost.mapScreenX(600));
    }

    @Test
    public void buildsOneBossMapRoundTripTowardTheNearestDirection() {
        assertEquals(Arrays.asList(150, 200, 250, 300, 350, 400, 450, 500,
                550, 500, 450, 400, 350, 300, 250, 200, 150, 100, 50),
                BossAutomation.buildRoute(100));
        List<Integer> fromRight = BossAutomation.buildRoute(500);
        assertEquals(Integer.valueOf(450), fromRight.get(0));
        assertEquals(Integer.valueOf(50), fromRight.get(8));
        assertEquals(Integer.valueOf(550), fromRight.get(fromRight.size() - 1));
        assertEquals(Integer.valueOf(517), BossAutomation.buildRoute(567).get(0));
    }

    @Test
    public void recognizesBossMapCoordinatesAndRedNames() {
        assertEquals(Integer.valueOf(567), BossAutomation.parseMapX("567,50"));
        assertEquals(Integer.valueOf(590), BossAutomation.parseMapX("590.24"));
        assertEquals(Integer.valueOf(590), BossAutomation.parseMapX("590 24"));
        assertEquals(Integer.valueOf(590), BossAutomation.parseMapX("590：24"));
        assertEquals(Integer.valueOf(4), BossAutomation.parseMapX("坐标 4，7"));
        assertNull(BossAutomation.parseMapX("601,25"));
        assertEquals(Integer.valueOf(4), BossAutomation.parseChannel("第 4 分流"));
        assertEquals(Integer.valueOf(8), BossAutomation.parseChannel("临渊道·分流8"));
        assertNull(BossAutomation.parseChannel("第9分流"));
        assertEquals(4, BossAutomation.nextChannel(3));
        assertEquals(1, BossAutomation.nextChannel(8));
        assertTrue(BossAutomation.isChannelSelectionGold(180, 130, 45));
        assertTrue(BossAutomation.isChannelSelectionGold(150, 120, 80));
        assertFalse(BossAutomation.isChannelSelectionGold(160, 150, 140));
        assertFalse(BossAutomation.isChannelSelectionGold(90, 70, 30));
        assertTrue(BossAutomation.isBossRed(150, 50, 40));
        assertFalse(BossAutomation.isBossRed(150, 125, 40));
        assertFalse(BossAutomation.isBossRed(150, 100, 40));
        assertFalse(BossAutomation.isBossRed(90, 20, 10));
        assertTrue(BossAutomation.hasRedTextDistribution(
                80, 10, 40, 12, 100, 20));
        assertFalse(BossAutomation.hasRedTextDistribution(
                20, 80, 4, 12, 100, 20));
        assertFalse(BossAutomation.hasRedTextDistribution(
                30, 100, 30, 10, 100, 20));
    }

    @Test
    public void usesFixedChannelRowsAfterScrolling() {
        assertEquals(205, ChannelSwitcher.selectionY(1));
        assertEquals(385, ChannelSwitcher.selectionY(3));
        assertEquals(475, ChannelSwitcher.selectionY(4));
        assertEquals(263, ChannelSwitcher.selectionY(5));
        assertEquals(351, ChannelSwitcher.selectionY(6));
        assertEquals(439, ChannelSwitcher.selectionY(7));
        assertEquals(527, ChannelSwitcher.selectionY(8));
        assertTrue(ChannelSwitcher.scrollsToTop(4));
        assertFalse(ChannelSwitcher.scrollsToTop(5));
    }

    @Test
    public void choosesTheFaceOrientationWithTheLargestDetection() {
        assertEquals(90, AntiCheatVerification.bestRotation(new int[]{0, 40, 10, 0}));
        assertEquals(0, AntiCheatVerification.bestRotation(new int[]{20, 0, 0, 0}));
        assertEquals(-1, AntiCheatVerification.bestRotation(new int[]{0, 0, 0, 0}));
    }

    @Test
    public void scansForBossesThroughoutTheMovementWindow() {
        assertEquals(250, BossAutomation.moveScanDelayMillis(1_000, 5_000));
        assertEquals(100, BossAutomation.moveScanDelayMillis(4_900, 5_000));
        assertEquals(0, BossAutomation.moveScanDelayMillis(5_000, 5_000));
    }

    @Test
    public void recognizesWelfareCategoriesAndRedDots() {
        assertEquals("在线奖励", WelfareAutomation.matchingCategoryName(" 在线 奖励 "));
        assertEquals("每日应援", WelfareAutomation.matchingCategoryName("每日应援好礼拿"));
        assertNull(WelfareAutomation.matchingCategoryName("已领取"));
        assertTrue(WelfareAutomation.isRedDotPixel(255, 20, 30));
        assertFalse(WelfareAutomation.isRedDotPixel(180, 20, 30));
        assertFalse(WelfareAutomation.isRedDotPixel(255, 150, 30));
        assertEquals(Integer.valueOf(2), DailyChallengeCategory.completionCount("（2 / 7）"));
        assertNull(DailyChallengeCategory.completionCount("每日完成奖励"));
        assertEquals(Arrays.asList(565), DailyChallengeCategory.rewardSlotsForCount(2));
        assertEquals(Arrays.asList(565, 755),
                DailyChallengeCategory.rewardSlotsForCount(4));
        assertEquals(Arrays.asList(565, 755, 945, 1135),
                DailyChallengeCategory.rewardSlotsForCount(7));
    }

    @Test
    public void extractsDungeonLevelWithoutMatchingOtherNumbers() {
        assertEquals(10, DungeonSweepAutomation.parseLevel("等级：10 人数：1~6"));
        assertEquals(180, DungeonSweepAutomation.parseLevel("等级: 180"));
        assertEquals(-1, DungeonSweepAutomation.parseLevel("每日次数：3/3"));
    }
}
