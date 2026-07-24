package com.local.sgmhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class HelperAccessibilityServiceTest {
    @Test
    public void newestDiagnosticLinesStayFirstAndCapped() {
        ArrayDeque<String> lines = new ArrayDeque<>();
        lines.add("older\n");
        DiagnosticLog.addLatestLines(lines, "newest\ndetail\n", 2);
        assertEquals(Arrays.asList("newest\n", "detail\n"),
                Arrays.asList(lines.toArray(new String[0])));
    }

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
        assertEquals("sgmhelper-地球瘦子-e8bc951d645bea2c.log",
                DiagnosticLog.logFileName("e8bc951d645bea2c"));
        assertEquals("sgmhelper-地球-82b65b00b0a740bd.log",
                DiagnosticLog.logFileName("82b65b00b0a740bd"));
        assertEquals("sgmhelper-米饭-81efa78434210a97.log",
                DiagnosticLog.logFileName("81efa78434210a97"));
        assertEquals("sgmhelper-地球瘦子-5fe0495ccad09f13.log",
                DiagnosticLog.logFileName("5fe0495ccad09f13"));
        assertEquals("sgmhelper-栗威-0944ab5bedb6535a.log",
                DiagnosticLog.logFileName("0944ab5bedb6535a"));
        assertEquals("sgmhelper-米饭-60377771f3d25b63.log",
                DiagnosticLog.logFileName("60377771f3d25b63"));
        assertEquals("sgmhelper-未知模拟器-unsafe_id.log",
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
    public void mapsEveryMilitaryWildernessTaskToWudangZoneRange() {
        assertArrayEquals(new int[] {1, 3},
                TaskAutomation.militaryWildernessZoneRange("一"));
        assertArrayEquals(new int[] {1, 3},
                TaskAutomation.militaryWildernessZoneRange("六"));
        assertArrayEquals(new int[] {4, 6},
                TaskAutomation.militaryWildernessZoneRange("七"));
        assertArrayEquals(new int[] {7, 9},
                TaskAutomation.militaryWildernessZoneRange("十"));
        assertArrayEquals(new int[] {10, 12},
                TaskAutomation.militaryWildernessZoneRange("十二"));
        assertArrayEquals(new int[] {13, 15},
                TaskAutomation.militaryWildernessZoneRange("十四"));
        assertArrayEquals(new int[] {16, 18},
                TaskAutomation.militaryWildernessZoneRange("十六"));
        assertArrayEquals(new int[] {19, 21},
                TaskAutomation.militaryWildernessZoneRange("十八"));
        assertArrayEquals(new int[] {22, 24},
                TaskAutomation.militaryWildernessZoneRange("十九"));
        assertArrayEquals(new int[] {22, 24},
                TaskAutomation.militaryWildernessZoneRange("19"));
        assertNull(TaskAutomation.militaryWildernessZoneRange("二十"));
    }

    @Test
    public void militaryWildernessKeepsPreferredZoneOnlyInsideTaskRange() {
        assertEquals(11, TaskAutomation.militaryWildernessZone("十二", 11));
        int fallback = TaskAutomation.militaryWildernessZone("十九", 11);
        assertTrue(fallback >= 22 && fallback <= 24);
        assertEquals(0, TaskAutomation.militaryWildernessZone("二十", 1));
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
    public void fallsBackOnlyToTheSelectedCompletedWildernessQuest() {
        assertTrue(TaskAutomation.isSelectedQuestComplete(
                "巡狩军团荒野",
                Arrays.asList("巡狩军团荒野（五）", "任务目标"), true));
        assertFalse(TaskAutomation.isSelectedQuestComplete(
                "巡狩军团荒野",
                Arrays.asList("勇讨军团天将（五）"), true));
        assertFalse(TaskAutomation.isSelectedQuestComplete(
                "巡狩军团荒野",
                Arrays.asList("巡狩军团荒野（五）"), false));
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
        assertEquals("补充军团物资", TaskAutomation.firstOngoingCollectionQuest(
                Arrays.asList("补充军团物资（五）", "巡狩军团荒野（五）"),
                true, false));
        assertNull(TaskAutomation.firstOngoingCollectionQuest(
                Arrays.asList("补充军团物资（五）", "巡狩军团荒野（五）"),
                false, false));
    }

    @Test
    public void schedulesMilitaryOnlyWhenAQuestIsSelected() {
        assertTrue(WorshipAlarmReceiver.shouldScheduleMilitary(true, true, false));
        assertTrue(WorshipAlarmReceiver.shouldScheduleMilitary(true, false, true));
        assertFalse(WorshipAlarmReceiver.shouldScheduleMilitary(true, false, false));
        assertFalse(WorshipAlarmReceiver.shouldScheduleMilitary(false, true, true));
    }

    @Test
    public void checksSupplyMoreOftenThanWilderness() {
        assertEquals(3 * 60 * 1_000,
                TaskAutomation.progressCheckDelayMillis("补充军团物资"));
        assertEquals(5 * 60 * 1_000,
                TaskAutomation.progressCheckDelayMillis("巡狩军团荒野"));
    }

    @Test
    public void checksTheBackpackBeforeTrainingAndBoss() {
        assertTrue(HelperAccessibilityService.shouldCheckInventoryBeforePrimary(
                true, AutomationHost.PrimaryTask.TRAINING));
        assertTrue(HelperAccessibilityService.shouldCheckInventoryBeforePrimary(
                true, AutomationHost.PrimaryTask.BOSS));
        assertFalse(HelperAccessibilityService.shouldCheckInventoryBeforePrimary(
                true, AutomationHost.PrimaryTask.DUNGEON));
        assertFalse(HelperAccessibilityService.shouldCheckInventoryBeforePrimary(
                false, AutomationHost.PrimaryTask.TRAINING));
    }

    @Test
    public void acceptsChangingBackpackCountsWithTheSameCapacityDecision() {
        assertEquals(50, AutoSellAutomation.parseCapacity("So/81")[0]);
        assertArrayEquals(new int[] {77, 81},
                AutoSellAutomation.parseCapacity("7Ty81"));
        assertArrayEquals(new int[] {77, 81},
                AutoSellAutomation.parseCapacity("7T81"));
        assertArrayEquals(new int[] {129, 150},
                AutoSellAutomation.parseCapacity("129y150"));
        assertArrayEquals(new int[] {45, 81},
                AutoSellAutomation.parseCapacity("4581"));
        assertArrayEquals(new int[] {79, 81},
                AutoSellAutomation.parseCapacity("7981"));
        assertArrayEquals(new int[] {128, 150},
                AutoSellAutomation.parseCapacity("128150"));
        assertEquals(Boolean.FALSE, AutoSellAutomation.consistentNearlyFull(
                new int[] {48, 69}, new int[] {49, 69}, 5));
        assertEquals(Boolean.TRUE, AutoSellAutomation.consistentNearlyFull(
                new int[] {65, 69}, new int[] {66, 69}, 5));
        assertNull(AutoSellAutomation.consistentNearlyFull(
                new int[] {64, 69}, new int[] {65, 69}, 5));
    }

    @Test
    public void recognizesQuickSaleOnlyInsideItsFixedButtonCrop() {
        assertTrue(HelperAccessibilityService.matchesYuanbaoQuickSell(
                "快速\n贩卖装备"));
        assertTrue(HelperAccessibilityService.matchesYuanbaoQuickSell(
                "快速 贩卖"));
        assertFalse(HelperAccessibilityService.matchesYuanbaoQuickSell(
                "批次贩卖"));
        assertFalse(HelperAccessibilityService.matchesYuanbaoQuickSell(null));
    }

    @Test
    public void recognizesTheFixedMapButtonAfterUpscaledOcr() {
        assertTrue(HelperAccessibilityService.isMapButtonText("地 图"));
        assertTrue(HelperAccessibilityService.isMapButtonText("地图"));
        assertFalse(HelperAccessibilityService.isMapButtonText("商城"));
    }

    @Test
    public void confirmsBossDefeatOnlyAfterThreeConsecutiveMisses() {
        assertFalse(BossAutomation.isBossDefeatConfirmed(1));
        assertFalse(BossAutomation.isBossDefeatConfirmed(2));
        assertTrue(BossAutomation.isBossDefeatConfirmed(3));
    }

    @Test
    public void scalesCoordinatesForTheShortDungeonCampMap() {
        assertEquals(642, AutomationHost.mapScreenX(100, 199));
        assertEquals(842, AutomationHost.mapScreenX(199, 199));
    }

    @Test
    public void dungeonEntranceRouteUsesWudangCoordinatesAndArrivalCheck() {
        assertEquals(200, DungeonBattleAutomation.palaceGuideX("洛阳"));
        assertEquals(31, DungeonBattleAutomation.palaceGuideY("建业"));
        assertEquals(210, DungeonBattleAutomation.palaceGuideX("北平"));
        assertEquals(33, DungeonBattleAutomation.palaceGuideY("襄阳"));
        assertEquals(26, DungeonBattleAutomation.campNpcX(10));
        assertEquals(105, DungeonBattleAutomation.campNpcX(60));
        assertEquals(152, DungeonBattleAutomation.campNpcX(75));
        assertEquals(100, DungeonBattleAutomation.entryNpcX(10));
        assertEquals(102, DungeonBattleAutomation.entryNpcX(60));
        assertEquals(16, DungeonBattleAutomation.entryNpcY(65));
        assertEquals(17, DungeonBattleAutomation.entryNpcY(70));
        assertTrue(DungeonBattleAutomation.isAtEntryNpc(10, "100,16"));
        assertTrue(DungeonBattleAutomation.isAtEntryNpc(60, "坐标 100，18"));
        assertTrue(DungeonBattleAutomation.isAtEntryNpc(70, "103.17"));
        assertFalse(DungeonBattleAutomation.isAtEntryNpc(75, "90,16"));
        assertFalse(DungeonBattleAutomation.isAtEntryNpc(40, "识别失败"));
        assertTrue(DungeonBattleAutomation.isAtCoordinate("208,34", 210, 33));
    }

    @Test
    public void reproducesTheFiveLevel10DungeonSections() {
        DungeonBattleAutomation.RouteDecision part0 = DungeonBattleAutomation.decide10(599);
        assertTrue(part0.searchEnemies);
        assertEquals(Arrays.asList(7, 10), part0.enemyLevels);
        assertEquals(500, part0.targetX);

        DungeonBattleAutomation.RouteDecision part1 = DungeonBattleAutomation.decide10(450);
        assertTrue(part1.searchEnemies);
        assertEquals(382, part1.targetX);

        DungeonBattleAutomation.RouteDecision part2 = DungeonBattleAutomation.decide10(300);
        assertFalse(part2.searchEnemies);
        assertEquals(159, part2.targetX);

        DungeonBattleAutomation.RouteDecision part3 = DungeonBattleAutomation.decide10(100);
        assertTrue(part3.searchEnemies);
        assertTrue(part3.acceptAnyEnemy);
        assertEquals(Arrays.asList(10), part3.enemyLevels);

        assertTrue(DungeonBattleAutomation.decide10(25).exit);
    }

    @Test
    public void level10DungeonSelectsOnlyConfiguredEnemyRows() {
        DungeonBattleAutomation.EnemyRow level18 = new DungeonBattleAutomation.EnemyRow(
                18, "18级 路人", new android.graphics.Rect(0, 0, 1, 1));
        DungeonBattleAutomation.EnemyRow level10 = new DungeonBattleAutomation.EnemyRow(
                10, "10级 黄巾兵", new android.graphics.Rect(0, 0, 1, 1));
        assertEquals(level10, DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(level18, level10), Arrays.asList(7, 10), false));
        assertEquals(level18, DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(level18, level10), Arrays.asList(10), true));
        assertNull(DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(level18), Arrays.asList(7, 10), false));
    }

    @Test
    public void reproducesTheSixLevel20DungeonSections() {
        DungeonBattleAutomation.RouteDecision part0 = DungeonBattleAutomation.decide20(599);
        assertEquals(Arrays.asList(18, 20), part0.enemyLevels);
        assertEquals(474, part0.targetX);

        DungeonBattleAutomation.RouteDecision part1 = DungeonBattleAutomation.decide20(400);
        assertFalse(part1.searchEnemies);
        assertEquals(338, part1.targetX);

        DungeonBattleAutomation.RouteDecision part2 = DungeonBattleAutomation.decide20(300);
        assertTrue(part2.acceptAnyEnemy);
        assertEquals(Arrays.asList(20), part2.enemyLevels);

        DungeonBattleAutomation.RouteDecision part3 = DungeonBattleAutomation.decide20(150);
        assertFalse(part3.acceptAnyEnemy);
        assertEquals(Arrays.asList(20), part3.enemyLevels);

        DungeonBattleAutomation.RouteDecision part4 = DungeonBattleAutomation.decide20(60);
        assertTrue(part4.acceptAnyEnemy);
        assertEquals(26, part4.targetX);

        DungeonBattleAutomation.RouteDecision part5 = DungeonBattleAutomation.decide20(20);
        assertTrue(part5.exit);
        assertEquals(22, part5.targetX);
    }

    @Test
    public void reproducesTheFiveLevel30DungeonSectionsAndAlternatingDoorY() {
        DungeonBattleAutomation.RouteDecision part0 = DungeonBattleAutomation.decide30(599);
        assertEquals(Arrays.asList(30), part0.enemyLevels);
        assertEquals(448, part0.targetX);
        assertEquals(4, part0.targetY);

        DungeonBattleAutomation.RouteDecision part1 = DungeonBattleAutomation.decide30(400);
        assertEquals(297, part1.targetX);
        assertEquals(27, part1.targetY);

        DungeonBattleAutomation.RouteDecision part2 = DungeonBattleAutomation.decide30(200);
        assertTrue(part2.acceptAnyEnemy);
        assertTrue(part2.enemyLevels.isEmpty());
        assertEquals(4, part2.targetY);

        DungeonBattleAutomation.RouteDecision part3 = DungeonBattleAutomation.decide30(100);
        assertEquals(Arrays.asList(30), part3.enemyLevels);
        assertEquals(32, part3.targetX);

        DungeonBattleAutomation.RouteDecision part4 = DungeonBattleAutomation.decide30(24);
        assertTrue(part4.exit);
        assertEquals(24, part4.targetX);
    }

    @Test
    public void reproducesTheLevel40BossSweepWaypoints() {
        int[] positions = {570, 500, 400, 320, 220, 150, 80, 40};
        int[] targets = {520, 420, 350, 275, 180, 110, 50, 25};
        for (int index = 0; index < positions.length; index++) {
            DungeonBattleAutomation.RouteDecision decision =
                    DungeonBattleAutomation.decide40(positions[index]);
            assertTrue(decision.searchEnemies);
            assertTrue(decision.acceptAnyEnemy);
            assertEquals(targets[index], decision.targetX);
            assertEquals(25, decision.targetY);
        }
        assertTrue(DungeonBattleAutomation.decide40(25).exit);
    }

    @Test
    public void reproducesTheLevel50RightwardBossRouteAndCenterDialog() {
        assertEquals(60, DungeonBattleAutomation.decide50(25).targetX);
        assertEquals(45, DungeonBattleAutomation.decide50(25).targetY);
        assertTrue(DungeonBattleAutomation.decide50(80).searchEnemies);
        assertEquals(205, DungeonBattleAutomation.decide50(150).targetX);
        assertEquals("交给我吧", DungeonBattleAutomation.decide50(300).interactionText);
        assertEquals(399, DungeonBattleAutomation.decide50(350).targetX);
        assertEquals(495, DungeonBattleAutomation.decide50(450).targetX);
        assertEquals(583, DungeonBattleAutomation.decide50(550).targetX);
        assertTrue(DungeonBattleAutomation.decide50(590).exit);
        assertEquals(570, DungeonBattleAutomation.decide50(590).targetX);
    }

    @Test
    public void reproducesTheLevel60PortalRoomRoute() {
        assertEquals(71, DungeonBattleAutomation.decide60(50).targetX);
        assertEquals(4, DungeonBattleAutomation.decide60(50).targetY);
        assertEquals(114, DungeonBattleAutomation.decide60(515).targetX);
        assertEquals(46, DungeonBattleAutomation.decide60(515).targetY);
        assertEquals(70, DungeonBattleAutomation.decide60(150).targetX);
        assertEquals(214, DungeonBattleAutomation.decide60(540).targetX);
        assertEquals(94, DungeonBattleAutomation.decide60(250).targetX);
        assertEquals(394, DungeonBattleAutomation.decide60(350).targetX);
        assertEquals(405, DungeonBattleAutomation.decide60(570).targetX);
        assertEquals(486, DungeonBattleAutomation.decide60(450).targetX);
        assertTrue(DungeonBattleAutomation.decide60(490).exit);
    }

    @Test
    public void reproducesTheLevel65GateAndLuBuRoute() {
        assertEquals(34, DungeonBattleAutomation.decide65(20).targetX);
        assertEquals("出击", DungeonBattleAutomation.decide65(34).interactionText);
        assertTrue(DungeonBattleAutomation.decide65(34).openNpc);
        assertEquals(245, DungeonBattleAutomation.decide65(150).targetX);
        assertEquals(Arrays.asList(64), DungeonBattleAutomation.decide65(150).enemyLevels);
        assertEquals(252, DungeonBattleAutomation.decide65(245).targetX);
        assertEquals(410, DungeonBattleAutomation.decide65(350).targetX);
        assertEquals(4, DungeonBattleAutomation.decide65(350).targetY);
        assertEquals(538, DungeonBattleAutomation.decide65(500).targetX);
        assertTrue(DungeonBattleAutomation.decide65(550).exit);
    }

    @Test
    public void reproducesTheLevel70EscortRouteAndNeverTargetsTheCarriage() {
        DungeonBattleAutomation.RouteDecision part0 = DungeonBattleAutomation.decide70(80);
        assertEquals(115, part0.targetX);
        assertEquals(Arrays.asList("长安城驻军队长", "长安白虎大门"),
                part0.priorityEnemyNames);
        assertEquals("马车", part0.protectName);
        assertEquals(560, DungeonBattleAutomation.decide70(300).targetX);
        assertTrue(DungeonBattleAutomation.decide70(570).exit);

        DungeonBattleAutomation.EnemyRow carriage = new DungeonBattleAutomation.EnemyRow(
                70, "70级 马车", new android.graphics.Rect(0, 0, 1, 1));
        DungeonBattleAutomation.EnemyRow guard = new DungeonBattleAutomation.EnemyRow(
                70, "长安城驻军队长", new android.graphics.Rect(0, 0, 1, 1));
        assertEquals(guard, DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(carriage, guard), List.of(),
                part0.priorityEnemyNames, part0.protectName, false));
    }

    @Test
    public void reproducesTheLevel75ShortMapGatesAndBossPriority() {
        DungeonBattleAutomation.RouteDecision part0 = DungeonBattleAutomation.decide75(50);
        assertEquals(73, part0.targetX);
        assertEquals(Arrays.asList("吕虔", "于禁", "李典", "夏侯渊"),
                part0.priorityEnemyNames);
        assertEquals(73, DungeonBattleAutomation.decide75(70).targetX);
        assertEquals(130, DungeonBattleAutomation.decide75(100).targetX);
        assertEquals(187, DungeonBattleAutomation.decide75(150).targetX);
        assertTrue(DungeonBattleAutomation.decide75(187).exit);

        DungeonBattleAutomation.EnemyRow xiahou = new DungeonBattleAutomation.EnemyRow(
                75, "夏侯渊", new android.graphics.Rect(0, 0, 1, 1));
        DungeonBattleAutomation.EnemyRow lvdian = new DungeonBattleAutomation.EnemyRow(
                75, "李典", new android.graphics.Rect(0, 0, 1, 1));
        assertEquals(lvdian, DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(xiahou, lvdian), List.of(),
                part0.priorityEnemyNames, null, false));
    }

    @Test
    public void matchesTheStableSupplyTaskSuffixAfterOcrSubstitution() {
        assertTrue(HelperAccessibilityService.matchesTextFragments(
                Arrays.asList("一补充至团物资E門"), "团物资", false));
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
    public void returnsToTheCityBeforeResumingSupplyCollection() {
        assertTrue(TaskAutomation.isWildernessLocation(
                Arrays.asList("荒野营地")));
        assertTrue(TaskAutomation.isWildernessLocation(
                Arrays.asList("荒野修炼2区")));
        assertFalse(TaskAutomation.isWildernessLocation(
                Arrays.asList("军团主城")));
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
        assertEquals(3 * 60 * 60 * 1_000L + 1_000,
                WorshipAlarmReceiver.nextRollingMilitaryAt(1_000));
        assertEquals(2_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 2_000, 5_000, 10_000));
        assertEquals(5_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 900, 5_000, 10_000));
        assertEquals(61_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 900, 500_000, 1_000_000));
        assertEquals(10_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 0, 0, 10_000));
    }

    @Test
    public void manualStopBlocksScheduledAutomationUntilTheUserStartsAgain() {
        assertFalse(WorshipAlarmReceiver.shouldStartScheduledAutomation(true));
        assertTrue(WorshipAlarmReceiver.shouldStartScheduledAutomation(false));
    }

    @Test
    public void deferredScheduledAutomationExpiresAfterFifteenMinutes() {
        long pendingAt = 1_000;
        assertTrue(WorshipAlarmReceiver.isPendingFresh(pendingAt, pendingAt));
        assertTrue(WorshipAlarmReceiver.isPendingFresh(
                pendingAt + 15 * 60 * 1_000L, pendingAt));
        assertFalse(WorshipAlarmReceiver.isPendingFresh(
                pendingAt + 15 * 60 * 1_000L + 1, pendingAt));
        assertFalse(WorshipAlarmReceiver.isPendingFresh(pendingAt - 1, pendingAt));
        assertFalse(WorshipAlarmReceiver.isPendingFresh(pendingAt, 0));
    }

    @Test
    public void assignsMilitaryCooldownOnlyToTheSelectedQuest() {
        assertTrue(TaskAutomation.isMilitaryQuestDetail(
                "补充军团物资", "补充 军团物资 冷却时间2小时"));
        assertFalse(TaskAutomation.isMilitaryQuestDetail(
                "补充军团物资", "巡狩军团荒野 冷却时间3小时"));
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
    public void retriesPrimaryAutomationOnlyThreeTimes() {
        assertTrue(HelperAccessibilityService.shouldRetryAutomation(1));
        assertTrue(HelperAccessibilityService.shouldRetryAutomation(3));
        assertFalse(HelperAccessibilityService.shouldRetryAutomation(4));
        assertFalse(HelperAccessibilityService.shouldRetryAutomation(100));
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
    public void showsWildernessFieldsOnlyForWildernessTraining() {
        assertTrue(HelperAccessibilityService.shouldShowWildernessOptions("荒野"));
        assertFalse(HelperAccessibilityService.shouldShowWildernessOptions("野境"));
        assertFalse(HelperAccessibilityService.shouldShowWildernessOptions("标记点"));
    }

    @Test
    public void mapsWildernessZonesToMonstersAndFixedDialogueRows() {
        assertEquals(Arrays.asList("60 食人花", "70 金甲龙", "75 圣武士"),
                TrainingAutomation.monstersForZone(1));
        assertEquals(Arrays.asList("80 魔斗士", "90 海妖", "100 螳螂巨妖"),
                TrainingAutomation.monstersForZone(6));
        assertEquals(Arrays.asList("115 九尾狐", "130 石狮精"),
                TrainingAutomation.monstersForZone(7));
        assertEquals(Arrays.asList("145 人面鸟", "160 战鬼"),
                TrainingAutomation.monstersForZone(12));
        assertEquals(Arrays.asList("175 式神童子", "190 剑齿虎"),
                TrainingAutomation.monstersForZone(15));
        assertEquals("金甲龙", TrainingAutomation.monsterName("70 金甲龙"));
        assertEquals(0, WildernessNavigator.page(3));
        assertEquals(1, WildernessNavigator.page(4));
        assertEquals(4, WildernessNavigator.page(15));
        assertEquals(7, WildernessNavigator.page(24));
        assertEquals(450, WildernessNavigator.zoneRowY(1));
        assertEquals(512, WildernessNavigator.zoneRowY(2));
        assertEquals(574, WildernessNavigator.zoneRowY(15));
        assertTrue(WildernessNavigator.isSelectedMapName(
                "荒野修炼15区", 15));
        assertTrue(WildernessNavigator.isSelectedMapName(
                "荒野修练 十五 区", 15));
        assertFalse(WildernessNavigator.isSelectedMapName(
                "荒野修炼14区", 15));
        assertTrue(WildernessNavigator.isSelectedMapName(
                "荒野修练 二十四 区", 24));
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
    public void mergesAdjacentScreenTextFragments() {
        assertTrue(HelperAccessibilityService.matchesTextFragments(
                Arrays.asList("补充军团", "物资(五)"), "补充军团物资", false));
        assertFalse(HelperAccessibilityService.matchesTextFragments(
                Arrays.asList("巡狩军团", "荒野(五)"), "补充军团物资", false));
        assertTrue(HelperAccessibilityService.matchesTextFragments(
                Arrays.asList("想"), "想", true));
        assertFalse(HelperAccessibilityService.matchesTextFragments(
                Arrays.asList("不想"), "想", true));
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
    public void recognizesCampArrivalText() {
        assertTrue(SoldierRevivalAutomation.hasCampArrival(
                Arrays.asList("军营", "士兵 卸下")));
        assertFalse(SoldierRevivalAutomation.hasCampArrival(
                Arrays.asList("工坊", "客栈")));
    }

    @Test
    public void recognizesTheWelfareWindowException() {
        assertTrue(ScreenGuard.isWelfareWindow(
                Arrays.asList("福利", "群英商店", "在线奖励")));
        assertFalse(ScreenGuard.isWelfareWindow(
                Arrays.asList("福利", "菜单")));
    }

    @Test
    public void classifiesOnlyKnownHudBlockers() {
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("指挥命令", "发布对象", "作战内容")));
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("军团", "公告", "主页", "成员")));
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("信件", "全部领取", "全部删除", "附件")));
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("副本快速通关奖励", "剩余29",
                        "全部领取", "全部删除")));
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("副本快速通关奖励", "利余29",
                        "全部领取", "全都刪除", "13/100")));
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("能力资料", "装备资料", "传家宝")));
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("能カ资料 装备资料", "传家宝")));
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("任务", "进行中", "可承接")));
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("敌人", "寻路", "炼造房", "客栈")));
        assertEquals(ScreenGuard.Blocker.GAME_WINDOW,
                ScreenGuard.blockerFor(Arrays.asList("名称", "职业", "所在地",
                        "点击此处增加队伍成员")));
        assertEquals(ScreenGuard.Blocker.DEFEATED,
                ScreenGuard.blockerFor(Arrays.asList("角色被击倒，自动功能已停止。", "确定")));
        assertEquals(ScreenGuard.Blocker.DEFEATED,
                ScreenGuard.blockerFor(Arrays.asList("地球胖子想要帮你复活，接受吗？", "确定")));
        assertEquals(ScreenGuard.Blocker.DISCONNECTED,
                ScreenGuard.blockerFor(Arrays.asList("与地图服务器失去联机。是否立刻尝试重连？", "确定")));
        assertEquals(ScreenGuard.Blocker.DUPLICATE_LOGIN,
                ScreenGuard.blockerFor(Arrays.asList("相同账号已在其他设备上登录", "确定", "开始游戏")));
        assertEquals(ScreenGuard.Blocker.ANTI_CHEAT,
                ScreenGuard.blockerFor(Arrays.asList("反外挂验证", "请点击下方图片，旋转至正确方向")));
        assertEquals(ScreenGuard.Blocker.NONE,
                ScreenGuard.blockerFor(Arrays.asList("商城", "福利", "竞技场", "菜单")));
        assertEquals(ScreenGuard.Blocker.NONE,
                ScreenGuard.blockerFor(Arrays.asList("信件", "军团任务", "你获得布匹")));
        assertEquals(ScreenGuard.Blocker.NONE,
                ScreenGuard.blockerFor(Arrays.asList("经验分配", "均分", "离开", "管理")));
    }

    @Test
    public void recognizesEveryLoginScreenBeforeTheCoveredStartButton() {
        assertEquals(LoginAutomation.Screen.ANNOUNCEMENT,
                LoginAutomation.screenFor(Arrays.asList("最新公告", "今日内不再弹出")));
        assertEquals(LoginAutomation.Screen.QUICK_LOGIN,
                LoginAutomation.screenFor(Arrays.asList("快捷登录", "账号登录/注册", "开始游戏")));
        assertEquals(LoginAutomation.Screen.ACCOUNT_LOGIN,
                LoginAutomation.screenFor(Arrays.asList("账号登录", "账号", "密码", "开始游戏")));
        assertEquals(LoginAutomation.Screen.ACCOUNT_LOGIN,
                LoginAutomation.screenFor(Arrays.asList("帐号登录", "帐号", "密码", "开始游戏")));
        assertEquals(LoginAutomation.Screen.START,
                LoginAutomation.screenFor(Arrays.asList("开始游戏", "S4-白虎")));
        assertEquals(LoginAutomation.Screen.WELFARE,
                LoginAutomation.screenFor(Arrays.asList("福利", "在线奖励", "累积在线")));
        assertEquals(LoginAutomation.Screen.UNCLAIMED_REWARDS,
                LoginAutomation.screenFor(Arrays.asList("尚未领取的奖励", "关闭界面", "前往领取")));
        assertEquals(LoginAutomation.Screen.REWARD_RECOVERY,
                LoginAutomation.screenFor(Arrays.asList("奖励找回", "商城", "福利", "竞技场", "菜单")));
        assertEquals(LoginAutomation.Screen.REWARD_RECOVERY,
                LoginAutomation.screenFor(Arrays.asList("重复任务", "副本奖励", "领取80%", "铜钱找回")));
        assertEquals(LoginAutomation.Screen.LOGGED_IN,
                LoginAutomation.screenFor(Arrays.asList("商城", "福利", "竞技场", "菜单")));
        assertEquals(LoginAutomation.Screen.LOGGED_IN,
                LoginAutomation.screenFor(Arrays.asList("对话", "地图", "南城地", "克标场")));
        assertEquals(LoginAutomation.Screen.UNKNOWN,
                LoginAutomation.screenFor(Arrays.asList("正在连接服务器")));
        assertEquals(LoginAutomation.Screen.UNKNOWN,
                LoginAutomation.screenFor(Arrays.asList(LoginAutomation.START_PROGRESS)));
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
        assertEquals(Arrays.asList(310, 340, 290, 260),
                BossAutomation.buildRoute(260, 260, 340));
    }

    @Test
    public void selectsAnExactWorldBossInsideTheConfiguredMap() {
        WorldBossCatalog.MapEntry map = WorldBossCatalog.findMap("葫芦谷口\n100,24");
        assertEquals("葫芦谷口", map.name);
        WorldBossCatalog.BossEntry target = map.findBoss("50 刘表");
        assertEquals("刘表", target.name);
        assertTrue(target.isVisible("50 刘表"));
        assertFalse(target.isVisible("50 刻表"));
        assertEquals(map, WorldBossCatalog.findMapByDisplayName(
                "葫芦谷口 [50 刘表]"));

        WorldBossCatalog.MapEntry cloud = WorldBossCatalog.findMap("峨嵋山云海");
        WorldBossCatalog.BossEntry dragon = cloud.findBoss("95 青龙");
        assertEquals(100, dragon.searchLeft);
        assertEquals(200, dragon.searchRight);
    }

    @Test
    public void recognizesBossMapCoordinatesAndRedNames() {
        assertEquals(Integer.valueOf(567), BossAutomation.parseMapX("567,50"));
        assertEquals(Integer.valueOf(590), BossAutomation.parseMapX("590.24"));
        assertEquals(Integer.valueOf(590), BossAutomation.parseMapX("590 24"));
        assertEquals(Integer.valueOf(590), BossAutomation.parseMapX("590：24"));
        assertEquals(Integer.valueOf(4), BossAutomation.parseMapX("坐标 4，7"));
        assertNull(BossAutomation.parseMapX("601,25"));
        assertTrue(Arrays.equals(new int[] {300, 25},
                BossAutomation.parseMapCoordinate("300,25")));
        assertEquals(Integer.valueOf(4), BossAutomation.parseChannel("第 4 分流"));
        assertEquals(Integer.valueOf(8), BossAutomation.parseChannel("临渊道·分流8"));
        assertEquals(Integer.valueOf(1), BossAutomation.parseChannel("分流\n1"));
        assertNull(BossAutomation.parseChannel("第9分流"));
        assertEquals(Integer.valueOf(3), BossAutomation.parseLeaderChannel("3"));
        assertEquals(Integer.valueOf(7), BossAutomation.parseLeaderChannel("分流7"));
        assertNull(BossAutomation.parseLeaderChannel("63/72"));
        assertTrue(BossAutomation.isMenuAutoCandidate("自 动", 1150, 320));
        assertFalse(BossAutomation.isMenuAutoCandidate("自动", 530, 320));
        assertFalse(BossAutomation.isMenuAutoCandidate("打开自动设置", 1170, 320));
        assertTrue(BossAutomation.isPartyManageDialogText(
                "名称  职业  等级  所在地  分流  操作"));
        assertTrue(BossAutomation.isPartyManageDialogText(
                "点击此处增加队伍成员"));
        assertFalse(BossAutomation.isPartyManageDialogText(
                "经验分配 均分 离开 管理"));
        assertEquals(Integer.valueOf(8),
                HelperAccessibilityService.parseHudChannelContext("国分流8"));
        assertEquals(Integer.valueOf(8),
                HelperAccessibilityService.parseHudChannelContext(
                        "S4白虎(4、7国分流8"));
        assertEquals(Integer.valueOf(2),
                HelperAccessibilityService.parseHudChannelContext("分流 2"));
        assertNull(HelperAccessibilityService.parseHudChannelContext("9"));
        assertEquals(4, ChannelSwitcher.nextChannel(3));
        assertEquals(1, ChannelSwitcher.nextChannel(8));
        assertTrue(BossAutomation.usesLeaderChannelAtRouteEnd(true));
        assertFalse(BossAutomation.usesLeaderChannelAtRouteEnd(false));
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
        assertEquals(10, BossAutomation.toOriginalStart(30, 3));
        assertEquals(31, BossAutomation.toOriginalEnd(91, 3));
        assertTrue(BossAutomation.isEnemyListCandidate(290, 720));
        assertTrue(BossAutomation.isEnemyListCandidate(410, 720));
        assertFalse(BossAutomation.isEnemyListCandidate(465, 720));
        assertFalse(BossAutomation.isEnemyListCandidate(90, 720));
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
    public void findsTheTemplateAfterAnInMemoryRotation() {
        byte[] upright = {
                0, 10, 40,
                20, 80, 30,
                90, 60, 50
        };
        byte[] counterClockwise = upright;
        for (int i = 0; i < 3; i++) {
            counterClockwise = AntiCheatTemplateMatcher.rotateClockwise(
                    counterClockwise, 3);
        }
        AntiCheatTemplateMatcher.OrientationMatch match =
                AntiCheatTemplateMatcher.bestOrientation(
                        counterClockwise, new byte[][] {upright}, 3);
        assertEquals(1, match.orientation);
        assertEquals(1.0, match.score, 0.000001);
        assertEquals(1.0,
                AntiCheatTemplateMatcher.correlation(upright, upright), 0.000001);
    }

    @Test
    public void ignoresTheProgressOverlayWhenCheckingForAntiCheat() {
        assertFalse(AntiCheatVerification.isChallengeLabel(
                "【BOSS】检查反外挂验证", 640, 20));
        assertTrue(AntiCheatVerification.isChallengeLabel(
                "反外挂验证", 640, 220));
        assertTrue(AntiCheatVerification.isChallengeLabel(
                "请旋转至正确方向", 640, 260));
        assertTrue(AntiCheatVerification.isChallengeText(
                "反外挂验证\n请点击下方图片，旋转至正确方向"));
        assertFalse(AntiCheatVerification.isChallengeText(
                "【BOSS】野王：打开自动设置"));
    }

    @Test
    public void restoresEveryPrimaryTaskAfterServiceRestart() {
        assertEquals(AutomationHost.PrimaryTask.TRAINING,
                HelperAccessibilityService.parsePrimaryTask("TRAINING"));
        assertEquals(AutomationHost.PrimaryTask.BOSS,
                HelperAccessibilityService.parsePrimaryTask("BOSS"));
        assertEquals(AutomationHost.PrimaryTask.DUNGEON,
                HelperAccessibilityService.parsePrimaryTask("DUNGEON"));
        assertEquals(AutomationHost.PrimaryTask.TRAINING,
                HelperAccessibilityService.parsePrimaryTask("invalid"));
    }

    @Test
    public void scansForBossesThroughoutTheMovementWindow() {
        assertEquals(250, BossAutomation.moveScanDelayMillis(1_000, 5_000));
        assertEquals(100, BossAutomation.moveScanDelayMillis(4_900, 5_000));
        assertEquals(0, BossAutomation.moveScanDelayMillis(5_000, 5_000));
    }

    @Test
    public void followsWudangCaptainSlotAndPortraitThresholds() {
        assertEquals(0, TeamFollowerVision.captainSlotForFlagCenter(28));
        assertEquals(1, TeamFollowerVision.captainSlotForFlagCenter(80));
        assertEquals(2, TeamFollowerVision.captainSlotForFlagCenter(132));
        assertEquals(-1, TeamFollowerVision.captainSlotForFlagCenter(300));
        assertFalse(TeamFollowerVision.isSameMap(0.199));
        assertTrue(TeamFollowerVision.isSameMap(0.20));
        assertTrue(TeamFollowerVision.isSameMap(0.75));
    }

    @Test
    public void usesTheConfiguredHeavenfallDurationAndMapCenter() {
        assertEquals(300_000, HeavenfallAutomation.durationMillis(5));
        assertTrue(HeavenfallAutomation.expired(300_000, 300_000));
        assertFalse(HeavenfallAutomation.expired(299_999, 300_000));
        assertTrue(HeavenfallAutomation.centerReached("300,25"));
        assertTrue(HeavenfallAutomation.centerReached("280,25"));
        assertFalse(HeavenfallAutomation.centerReached("279,25"));
        assertTrue(HeavenfallAutomation.centerReached("300,30"));
        assertFalse(HeavenfallAutomation.centerReached("300,31"));
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
