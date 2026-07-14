package com.local.sgmhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public final class HelperAccessibilityServiceTest {
    @Test
    public void extractsWildernessQuestNumber() {
        assertEquals("十二", HelperAccessibilityService.extractWildernessZone(
                "巡狩军团荒野（十二）(0/2)"));
        assertEquals("四", HelperAccessibilityService.extractWildernessZone(
                "巡狩军团荒野 ( 四 )"));
        assertNull(HelperAccessibilityService.extractWildernessZone("补充军团物资（十二）"));
    }

    @Test
    public void recognizesAllMilitaryQuestNames() {
        assertEquals("补充军团物资", HelperAccessibilityService.extractMilitaryQuestName(
                "补充军团物资（五）(0/2)"));
        assertEquals("巡狩军团荒野", HelperAccessibilityService.extractMilitaryQuestName(
                "巡狩军团荒野（五）(0/2)"));
        assertEquals("勇讨军团天将", HelperAccessibilityService.extractMilitaryQuestName(
                "勇讨军团天将（五）(0/2)"));
        assertNull(HelperAccessibilityService.extractMilitaryQuestName("一般任务"));
    }

    @Test
    public void usesTheNpcForEachExecutableMilitaryQuest() {
        assertEquals("常务军士",
                HelperAccessibilityService.expectedMilitaryNpc("补充军团物资"));
        assertEquals("荒野军士",
                HelperAccessibilityService.expectedMilitaryNpc("巡狩军团荒野"));
        assertNull(HelperAccessibilityService.expectedMilitaryNpc("勇讨军团天将"));
    }

    @Test
    public void recognizesCompletedMilitaryMaterials() {
        assertEquals(Boolean.TRUE, HelperAccessibilityService.requiredItemComplete(
                "取得精制丝绸 (250/250) 个"));
        assertEquals(Boolean.FALSE, HelperAccessibilityService.requiredItemComplete(
                "取得精制丝绸 (249/250) 个"));
        assertEquals(Boolean.TRUE, HelperAccessibilityService.requiredItemComplete(
                "取得道具（300/250）个"));
        assertEquals(Boolean.TRUE, HelperAccessibilityService.requiredItemComplete(
                "取得铜铸茶壶 250 ／ 250 个 [56尺]"));
        assertNull(HelperAccessibilityService.requiredItemComplete("军团财富：50"));
    }

    @Test
    public void recognizesCompletedMilitaryGreenMark() {
        assertTrue(HelperAccessibilityService.isCompletionGreen(20, 220, 35));
        assertFalse(HelperAccessibilityService.isCompletionGreen(210, 190, 30));
        assertFalse(HelperAccessibilityService.isCompletionGreen(30, 120, 220));
    }

    @Test
    public void explicitIncompleteProgressOverridesAnotherGreenMark() {
        assertTrue(HelperAccessibilityService.isMilitaryQuestComplete(Boolean.TRUE, false));
        assertTrue(HelperAccessibilityService.isMilitaryQuestComplete(null, true));
        assertFalse(HelperAccessibilityService.isMilitaryQuestComplete(Boolean.FALSE, true));
        assertFalse(HelperAccessibilityService.isMilitaryQuestComplete(null, false));
    }

    @Test
    public void recognizesMilitaryQuestCooldown() {
        assertTrue(HelperAccessibilityService.isCooldownText("冷却时间：2小时"));
        assertFalse(HelperAccessibilityService.isCooldownText("承接等级：61"));
        assertTrue(HelperAccessibilityService.isAvailableMilitaryQuest("承接等级：61"));
        assertFalse(HelperAccessibilityService.isAvailableMilitaryQuest("冷却时间：2小时"));
        assertEquals(Integer.valueOf(120),
                HelperAccessibilityService.extractCooldownMinutes("冷却时间：2小时"));
        assertEquals(Integer.valueOf(150),
                HelperAccessibilityService.extractCooldownMinutes("冷却时间:2小时30分钟"));
        assertEquals(Integer.valueOf(30),
                HelperAccessibilityService.extractCooldownMinutes("冷却时间：30分钟"));
        assertEquals(Integer.valueOf(32),
                HelperAccessibilityService.extractCooldownMinutes("冷却时间:31分49秒"));
        assertNull(HelperAccessibilityService.extractCooldownMinutes("冷却时间"));
    }

    @Test
    public void waitsOnlyWhenBothExecutableMilitaryQuestsAreCooling() {
        assertTrue(HelperAccessibilityService.allExecutableMilitaryQuestsCooling(true, true));
        assertFalse(HelperAccessibilityService.allExecutableMilitaryQuestsCooling(true, false));
        assertFalse(HelperAccessibilityService.allExecutableMilitaryQuestsCooling(false, true));
    }

    @Test
    public void checksMilitaryOnlyWhenSavedTimeIsMissingOrDue() {
        assertTrue(HelperAccessibilityService.shouldCheckMilitaryNow(1_000, 0, 5_000));
        assertTrue(HelperAccessibilityService.shouldCheckMilitaryNow(1_000, 5_000, 1_000));
        assertFalse(HelperAccessibilityService.shouldCheckMilitaryNow(1_000, 2_000, 5_000));
    }

    @Test
    public void schedulesTheEarliestIndependentMilitaryTask() {
        assertEquals(2_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 2_000, 5_000, 10_000));
        assertEquals(5_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 900, 5_000, 10_000));
        assertEquals(10_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 0, 0, 10_000));
    }

    @Test
    public void retriesOnlyScreenshotIntervalErrors() {
        assertTrue(HelperAccessibilityService.shouldRetryScreenshot(3, 2));
        assertFalse(HelperAccessibilityService.shouldRetryScreenshot(3, 1));
        assertFalse(HelperAccessibilityService.shouldRetryScreenshot(2, 5));
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
        assertTrue(HelperAccessibilityService.isTaskWindowVisible(
                Arrays.asList("任务", "进行中", "可承接")));
        assertFalse(HelperAccessibilityService.isTaskWindowVisible(
                Arrays.asList("右侧任务栏", "进行中")));
    }
}
