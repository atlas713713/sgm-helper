package com.local.sgmhelper;

import android.graphics.Rect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
    public void restartsGameOnlyWhileThePrimaryTaskIsIdling() {
        assertTrue(HelperAccessibilityService.isGameRestartWindow(
                true, 0, false, false, false));
        // 正在跑定时军务/副本这类插入任务，主线任务不是当前任务。
        assertFalse(HelperAccessibilityService.isGameRestartWindow(
                false, 0, false, false, false));
        // 插入任务已经排上队，等它们跑完再说。
        assertFalse(HelperAccessibilityService.isGameRestartWindow(
                true, 1, false, false, false));
        assertFalse(HelperAccessibilityService.isGameRestartWindow(
                true, 0, true, false, false));
        assertFalse(HelperAccessibilityService.isGameRestartWindow(
                true, 0, false, true, false));
        assertFalse(HelperAccessibilityService.isGameRestartWindow(
                true, 0, false, false, true));
    }

    @Test
    public void fallsBackToTheDefaultRestartIntervalWhenUnknown() {
        assertEquals(30, HelperAccessibilityService.normalizeGameRestartInterval(30));
        assertEquals(360, HelperAccessibilityService.normalizeGameRestartInterval(360));
        assertEquals(HelperAccessibilityService.DEFAULT_GAME_RESTART_INTERVAL_MINUTES,
                HelperAccessibilityService.normalizeGameRestartInterval(7));
        assertEquals(HelperAccessibilityService.DEFAULT_GAME_RESTART_INTERVAL_MINUTES,
                HelperAccessibilityService.normalizeGameRestartInterval(0));
        assertTrue(HelperAccessibilityService.GAME_RESTART_INTERVAL_MINUTES.contains(
                HelperAccessibilityService.DEFAULT_GAME_RESTART_INTERVAL_MINUTES));
    }

    @Test
    public void usesWudangDungeonGearHandlingIntervals() {
        assertEquals(1, DungeonBattleAutomation.normalizeDungeonGearHandleCount(0));
        assertEquals(1, DungeonBattleAutomation.normalizeDungeonGearHandleCount(-2));
        assertEquals(1, DungeonBattleAutomation.normalizeDungeonGearHandleCount(1));
        assertEquals(20, DungeonBattleAutomation.normalizeDungeonGearHandleCount(20));
        assertEquals(1, DungeonBattleAutomation.normalizeDungeonGearHandleCount(21));

        assertTrue(DungeonBattleAutomation.shouldHandleDungeonGear(1, 1));
        assertFalse(DungeonBattleAutomation.shouldHandleDungeonGear(2, 3));
        assertTrue(DungeonBattleAutomation.shouldHandleDungeonGear(3, 3));
        assertTrue(DungeonBattleAutomation.shouldHandleDungeonGear(4, 3));
    }

    @Test
    public void usesWudangDungeonCampMerchantCoordinates() {
        assertArrayEquals(new int[] {107, 26}, BaseDungeonAction.gearMerchantPoint(30, 1));
        assertArrayEquals(new int[] {109, 26}, BaseDungeonAction.gearMerchantPoint(60, 1));
        assertArrayEquals(new int[] {106, 24}, BaseDungeonAction.gearMerchantPoint(75, 1));
        assertArrayEquals(new int[] {115, 21}, BaseDungeonAction.gearMerchantPoint(75, 2));
        assertArrayEquals(new int[] {110, 21}, BaseDungeonAction.gearMerchantPoint(90, 1));
    }

    @Test
    public void recognizesWudangDungeonMerchantTitleOnly() {
        assertTrue(AutoSellAutomation.isDungeonMerchantTitle("行脚商队"));
        assertTrue(AutoSellAutomation.isDungeonMerchantTitle("行脚 商"));
        assertFalse(AutoSellAutomation.isDungeonMerchantTitle("客栈"));
        assertFalse(AutoSellAutomation.isDungeonMerchantTitle(null));
    }

    @Test
    public void repeatsEachSelectedDungeonUsingItsWudangCount() {
        assertEquals(3, DungeonBattleAutomation.normalizeDungeonRunCount(0));
        assertEquals(3, DungeonBattleAutomation.normalizeDungeonRunCount(-1));
        assertEquals(1, DungeonBattleAutomation.normalizeDungeonRunCount(1));
        assertEquals(3, DungeonBattleAutomation.normalizeDungeonRunCount(3));
        assertEquals(3, DungeonBattleAutomation.normalizeDungeonRunCount(4));

        assertTrue(DungeonBattleAutomation.shouldRepeatDungeon(1, 3));
        assertTrue(DungeonBattleAutomation.shouldRepeatDungeon(2, 3));
        assertFalse(DungeonBattleAutomation.shouldRepeatDungeon(3, 3));
        assertFalse(DungeonBattleAutomation.shouldRepeatDungeon(20, 1));
    }

    @Test
    public void triesBothSuFlavoursWhenForceStoppingTheGame() {
        List<String[]> commands = GameRestartAutomation.forceStopCommands(
                GameRestartAutomation.GAME_PACKAGE);
        assertEquals(2, commands.size());
        assertArrayEquals(
                new String[] {"su", "-c", "am force-stop hk.phx.khm.cs"}, commands.get(0));
        assertArrayEquals(
                new String[] {"su", "0", "am", "force-stop", "hk.phx.khm.cs"},
                commands.get(1));
    }

    @Test
    public void recognizesSystemForceStopButtonsWithoutOcr() {
        assertTrue(HelperAccessibilityService.isForceStopLabel("FORCE STOP"));
        assertTrue(HelperAccessibilityService.isForceStopLabel("强行停止"));
        assertTrue(HelperAccessibilityService.isForceStopLabel("强制停止"));
        assertFalse(HelperAccessibilityService.isForceStopLabel("UNINSTALL"));
        assertTrue(HelperAccessibilityService.isConfirmLabel("OK"));
        assertTrue(HelperAccessibilityService.isConfirmLabel("确定"));
        assertFalse(HelperAccessibilityService.isConfirmLabel("CANCEL"));
    }

    @Test
    public void requiresTheAutoAttackButtonSignalBeforeTapping() {
        assertTrue(HelperAccessibilityService.isAutoAttackButtonVisible(4, 44, 31));
        assertTrue(HelperAccessibilityService.isAutoAttackButtonVisible(28, 3, 12));
        // The same ROI when the in-game menu covers the control has no button
        // highlight, so it must not be treated as a tappable attack control.
        assertFalse(HelperAccessibilityService.isAutoAttackButtonVisible(2, 0, 0));
        assertFalse(HelperAccessibilityService.isAutoAttackButtonVisible(20, 0, 4));
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
    public void labelsDungeonReturnHomeAsDungeonEvenWhenScheduledAsInterrupt() {
        assertEquals("副本", HelperAccessibilityService.returnHomeProgressPrefix(
                AutomationHost.PrimaryTask.DUNGEON, "primary:DUNGEON"));
        assertEquals("副本", HelperAccessibilityService.returnHomeProgressPrefix(
                AutomationHost.PrimaryTask.TRAINING, "定时一般副本"));
        assertEquals("自动军务", HelperAccessibilityService.returnHomeProgressPrefix(
                AutomationHost.PrimaryTask.TRAINING, "自动军务"));
    }

    @Test
    public void queuesMilitaryWhileSoldierRevivalIsRunning() {
        assertTrue(HelperAccessibilityService.shouldQueueMilitaryForSoldierRevival(true));
        assertFalse(HelperAccessibilityService.shouldQueueMilitaryForSoldierRevival(false));
    }

    @Test
    public void revivesSoldiersBeforeDungeonInventoryCheck() {
        List<String> calls = new ArrayList<>();
        AtomicReference<Runnable> afterRevival = new AtomicReference<>();
        AutomationHost host = (AutomationHost) Proxy.newProxyInstance(
                AutomationHost.class.getClassLoader(),
                new Class<?>[] {AutomationHost.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("reviveSoldiersBeforeDungeon")) {
                        calls.add("revive");
                        afterRevival.set((Runnable) args[0]);
                    } else if (method.getName().equals("checkInventoryBeforePrimary")) {
                        calls.add("inventory");
                    }
                    return null;
                });

        new DungeonBattleAutomation(host).begin();

        assertEquals(List.of("revive"), calls);
        afterRevival.get().run();
        assertEquals(List.of("revive", "inventory"), calls);
    }

    @Test
    public void inventoryMonitoringAllowsMilitaryButNotOtherInterrupts() {
        AutomationTaskManager.Run primary = automationRun(
                "primary:TRAINING", AutomationTaskManager.Kind.PRIMARY);
        AutomationTaskManager.Run military = automationRun(
                "自动军务", AutomationTaskManager.Kind.INTERRUPT);
        AutomationTaskManager.Run welfare = automationRun(
                "领取福利", AutomationTaskManager.Kind.HIGH_PRIORITY_INTERRUPT);

        assertTrue(HelperAccessibilityService.shouldMonitorInventoryForRun(
                primary, AutomationHost.PrimaryTask.TRAINING));
        assertTrue(HelperAccessibilityService.shouldMonitorInventoryForRun(
                military, AutomationHost.PrimaryTask.TRAINING));
        assertFalse(HelperAccessibilityService.shouldMonitorInventoryForRun(
                welfare, AutomationHost.PrimaryTask.TRAINING));
    }

    private static AutomationTaskManager.Run automationRun(
            String key, AutomationTaskManager.Kind kind) {
        return new AutomationTaskManager.Run(1,
                new AutomationTaskManager.Request(key, key, kind, () -> { }));
    }

    @Test
    public void reusesOnlyRecentHudConfirmation() {
        assertFalse(HelperAccessibilityService.isRecentHudConfirmation(0, 1_000));
        assertTrue(HelperAccessibilityService.isRecentHudConfirmation(1_000, 4_000));
        assertFalse(HelperAccessibilityService.isRecentHudConfirmation(1_000, 4_001));
        assertFalse(HelperAccessibilityService.isRecentHudConfirmation(5_000, 4_000));
    }

    @Test
    public void closingWorldBossMapContinuesWithoutHudCheck() {
        List<String> calls = new ArrayList<>();
        AtomicReference<Runnable> afterBack = new AtomicReference<>();
        AtomicInteger completed = new AtomicInteger();
        AutomationHost host = (AutomationHost) Proxy.newProxyInstance(
                AutomationHost.class.getClassLoader(),
                new Class<?>[] {AutomationHost.class},
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    if ("pressBack".equals(method.getName())) {
                        afterBack.set((Runnable) args[0]);
                    }
                    return null;
                });

        new BossAutomation(host).closeWorldBossMap(completed::incrementAndGet);

        assertEquals(Arrays.asList("showProgress", "pressBack"), calls);
        afterBack.get().run();
        assertEquals(1, completed.get());
        assertFalse(calls.contains("ensureGameHudVisible"));
    }

    @Test
    public void scansForRedBossBeforeMoving() {
        List<String> calls = new ArrayList<>();
        AtomicReference<java.util.function.Consumer<BossAutomation.BossTarget>> scanResult =
                new AtomicReference<>();
        AtomicInteger moves = new AtomicInteger();
        AutomationHost host = (AutomationHost) Proxy.newProxyInstance(
                AutomationHost.class.getClassLoader(),
                new Class<?>[] {AutomationHost.class},
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    if ("recognizeRedBoss".equals(method.getName())) {
                        @SuppressWarnings("unchecked")
                        java.util.function.Consumer<BossAutomation.BossTarget> callback =
                                (java.util.function.Consumer<BossAutomation.BossTarget>) args[0];
                        scanResult.set(callback);
                    }
                    return null;
                });

        new BossAutomation(host).scanBeforeMove(moves::incrementAndGet);

        assertEquals(Arrays.asList("showProgress", "recognizeRedBoss"), calls);
        assertEquals(0, moves.get());
        scanResult.get().accept(null);
        assertEquals(1, moves.get());
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
    public void revivesSoldiersBeforeTrainingEvenWhenMilitaryIsEnabled() {
        // 以前这里是二选一：军务默认开着，复活那条分支永远走不到。
        assertEquals(List.of(HelperAccessibilityService.TrainingStartStep.REVIVE,
                        HelperAccessibilityService.TrainingStartStep.MILITARY,
                        HelperAccessibilityService.TrainingStartStep.TRAINING),
                HelperAccessibilityService.trainingStartSteps(true, true));
        assertEquals(List.of(HelperAccessibilityService.TrainingStartStep.REVIVE,
                        HelperAccessibilityService.TrainingStartStep.TRAINING),
                HelperAccessibilityService.trainingStartSteps(false, true));
        assertEquals(List.of(HelperAccessibilityService.TrainingStartStep.MILITARY,
                        HelperAccessibilityService.TrainingStartStep.TRAINING),
                HelperAccessibilityService.trainingStartSteps(true, false));
        assertEquals(List.of(HelperAccessibilityService.TrainingStartStep.TRAINING),
                HelperAccessibilityService.trainingStartSteps(false, false));
        // 军务的三个开关默认都开着，所以默认配置一定包含复活。
        assertTrue(WorshipAlarmReceiver.shouldScheduleMilitary(true, true, true));
        assertTrue(HelperAccessibilityService.DEFAULT_SOLDIER_REVIVAL_ENABLED);
        assertTrue(HelperAccessibilityService.trainingStartSteps(
                        WorshipAlarmReceiver.shouldScheduleMilitary(true, true, true),
                        HelperAccessibilityService.DEFAULT_SOLDIER_REVIVAL_ENABLED)
                .contains(HelperAccessibilityService.TrainingStartStep.REVIVE));
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
        assertTrue(TaskAutomation.isRequiredItemDetailPosition(750, 300));
        assertFalse(TaskAutomation.isRequiredItemDetailPosition(1100, 220));
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
    public void waitsForAutoPathButtonBeforeWaitingForMilitaryDialogue() {
        List<String> calls = new ArrayList<>();
        AtomicReference<Runnable> autoPathFound = new AtomicReference<>();
        AtomicReference<Runnable> autoPathMissing = new AtomicReference<>();
        AtomicReference<Runnable> quickArrivalClicked = new AtomicReference<>();
        AtomicReference<Runnable> autoPathClicked = new AtomicReference<>();
        AutomationHost host = (AutomationHost) Proxy.newProxyInstance(
                AutomationHost.class.getClassLoader(),
                new Class<?>[] {AutomationHost.class},
                (proxy, method, args) -> {
                    if ("showProgress".equals(method.getName())) {
                        calls.add("progress:" + args[0]);
                    } else if ("waitForTextRegion".equals(method.getName())) {
                        calls.add("waitForTextRegion:" + args[0] + ":"
                                + args[1] + "," + args[2] + "-" + args[3] + "," + args[4]);
                        autoPathFound.set((Runnable) args[6]);
                        autoPathMissing.set((Runnable) args[7]);
                    } else if ("clickTextRegion".equals(method.getName())) {
                        calls.add("clickTextRegion:" + args[0]);
                    } else if ("failAutomation".equals(method.getName())) {
                        calls.add("failAutomation:" + args[0]);
                    } else if ("clickQuickArrival".equals(method.getName())) {
                        calls.add("clickQuickArrival");
                        quickArrivalClicked.set((Runnable) args[0]);
                    } else if ("tap".equals(method.getName())) {
                        calls.add("tap:" + args[0] + "," + args[1]);
                        autoPathClicked.set((Runnable) args[2]);
                    }
                    return null;
                });

        new TaskAutomation(host).finishMilitaryArrival("补充军团物资", () -> {});
        assertEquals(Arrays.asList(
                "progress:自动军务：检测是否出现自动寻路",
                "waitForTextRegion:自动寻路:510,445-770,505"), calls);

        autoPathMissing.get().run();
        assertEquals("clickQuickArrival", calls.get(3));
        assertFalse(calls.contains("clickTextRegion:离开"));

        quickArrivalClicked.get().run();
        autoPathFound.get().run();
        assertEquals("tap:642,473", calls.get(7));
        assertFalse(calls.contains("clickTextRegion:离开"));

        autoPathClicked.get().run();
        assertEquals("clickTextRegion:离开", calls.get(9));
    }

    @Test
    public void completedSupplyQuestUsesModalAutoPathInsteadOfHud() throws Exception {
        List<String> calls = new ArrayList<>();
        AtomicReference<Runnable> delayed = new AtomicReference<>();
        AtomicReference<Runnable> quickArrivalClicked = new AtomicReference<>();
        AutomationHost host = (AutomationHost) Proxy.newProxyInstance(
                AutomationHost.class.getClassLoader(),
                new Class<?>[] {AutomationHost.class},
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    if ("postDelayed".equals(method.getName())) {
                        delayed.set((Runnable) args[0]);
                    } else if ("clickQuickArrival".equals(method.getName())) {
                        quickArrivalClicked.set((Runnable) args[0]);
                    }
                    return null;
                });
        TaskAutomation automation = new TaskAutomation(host);
        java.lang.reflect.Method finish = TaskAutomation.class.getDeclaredMethod(
                "finishOngoingMilitaryQuest", String.class, Runnable.class);
        finish.setAccessible(true);

        finish.invoke(automation, "补充军团物资", (Runnable) () -> {});
        delayed.get().run();
        quickArrivalClicked.get().run();

        assertTrue(calls.contains("waitForTextRegion"));
        assertFalse(calls.contains("openAutoPathPanel"));
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
        assertEquals(Integer.valueOf(120),
                TaskAutomation.extractCooldownMinutes("冷却时间:2小时30分钟"));
        assertEquals(Integer.valueOf(31),
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
    public void checksTheBackpackBeforeEveryLongRunningPrimaryTask() {
        assertTrue(HelperAccessibilityService.shouldCheckInventoryBeforePrimary(
                true, AutomationHost.PrimaryTask.TRAINING));
        assertTrue(HelperAccessibilityService.shouldCheckInventoryBeforePrimary(
                true, AutomationHost.PrimaryTask.BOSS));
        assertTrue(HelperAccessibilityService.shouldCheckInventoryBeforePrimary(
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
        assertEquals(641, AutomationHost.mapScreenX(100, 199));
        assertEquals(847, AutomationHost.mapScreenX(199, 199));
    }

    @Test
    public void soldierRevivalAndAutoSellDefaultToChecked() {
        assertTrue(HelperAccessibilityService.DEFAULT_SOLDIER_REVIVAL_ENABLED);
        assertTrue(HelperAccessibilityService.DEFAULT_AUTO_SELL_ENABLED);
    }

    @Test
    public void createsOneWudangStyleActionClassPerDungeon() {
        assertTrue(BaseDungeonAction.forLevel(10) instanceof Dungeon10Action);
        assertTrue(BaseDungeonAction.forLevel(20) instanceof Dungeon20Action);
        assertTrue(BaseDungeonAction.forLevel(30) instanceof Dungeon30Action);
        assertTrue(BaseDungeonAction.forLevel(40) instanceof Dungeon40Action);
        assertTrue(BaseDungeonAction.forLevel(50) instanceof Dungeon50Action);
        assertTrue(BaseDungeonAction.forLevel(60) instanceof Dungeon60Action);
        assertTrue(BaseDungeonAction.forLevel(65) instanceof Dungeon65Action);
        assertTrue(BaseDungeonAction.forLevel(70) instanceof Dungeon70Action);
        assertTrue(BaseDungeonAction.forLevel(75) instanceof Dungeon75Action);
        assertTrue(BaseDungeonAction.forLevel(80) instanceof Dungeon80Action);
        assertTrue(BaseDungeonAction.forLevel(90) instanceof Dungeon90Action);
        assertTrue(BaseDungeonAction.forLevel(105) instanceof Dungeon105Action);
        assertTrue(BaseDungeonAction.forLevel(120) instanceof Dungeon120Action);
        assertTrue(BaseDungeonAction.forLevel(135) instanceof Dungeon135Action);
        assertTrue(BaseDungeonAction.forLevel(150) instanceof Dungeon150Action);
        assertTrue(BaseDungeonAction.forLevel(165) instanceof Dungeon165Action);
        assertEquals("黄巾营寨", BaseDungeonAction.forLevel(30).campName());
    }

    @Test
    public void highLevelDungeonsUseWudangPalaceEntriesAndMapWidths() {
        // DungeonUtil.h 的“副本宫殿(一般)”入口 x，y 固定 16。
        int[][] palace = {{80, 169}, {90, 184}, {105, 216}, {120, 232},
                {135, 248}, {150, 263}, {165, 279}};
        for (int[] entry : palace) {
            assertEquals(entry[1], BaseDungeonAction.forLevel(entry[0]).campNpcX());
            assertEquals(100, BaseDungeonAction.forLevel(entry[0]).entryNpcX());
            assertEquals(16, BaseDungeonAction.forLevel(entry[0]).entryNpcY());
        }
        assertEquals(599, BaseDungeonAction.forLevel(10).dungeonMapMaxX());
        assertEquals(199, BaseDungeonAction.forLevel(75).dungeonMapMaxX());
        assertEquals(399, BaseDungeonAction.forLevel(80).dungeonMapMaxX());
        assertEquals(399, BaseDungeonAction.forLevel(120).dungeonMapMaxX());
        assertEquals(199, BaseDungeonAction.forLevel(150).dungeonMapMaxX());
        assertEquals(399, BaseDungeonAction.forLevel(165).dungeonMapMaxX());
    }

    @Test
    public void highLevelDungeonsClickDialogButtonsByTextInsteadOfFixedRows() {
        // 80 级以上按武当的按钮文字找行，但 NPC 名字照样要 OCR 校验。
        BaseDungeonAction level80 = BaseDungeonAction.forLevel(80);
        assertArrayEquals(new String[] {"随时可以出击", "进入"}, level80.entryNpcButtons());
        assertArrayEquals(new String[] {"收下奖赏"}, level80.exitNpcButtons());
        assertArrayEquals(new String[] {"下一步", "收下战利品"},
                BaseDungeonAction.forLevel(90).exitNpcButtons());
        // 10–75 继续用已经验证过的固定行号。
        assertNull(BaseDungeonAction.forLevel(10).entryNpcButtons());
        assertEquals("邹靖", BaseDungeonAction.forLevel(10).entryNpcName());
    }

    @Test
    public void everyDungeonHasTheWudangNameAndNpcsFromTheApkConfig() {
        // res/values/arrays.xml 的 dungeon_list_cn：80 级以上的副本名并不是读不到的，
        // 所以这些副本也能在点固定行/按钮前先 OCR 校验对话框标题。
        String[][] config = {
            {"80", "火烧博望坡", "博望营寨", "李典", "李典"},
            {"90", "血战长坂坡", "当阳营寨", "诸葛亮", "诸葛亮"},
            {"105", "逍遥津之战", "合肥城中", "乐进", "乐进"},
            {"120", "汉中之战", "汉中大营", "黄权", "黄权"},
            {"135", "麦城之战", "荆州东部", "陆逊", "陆逊"},
            {"150", "罗马竞技场", "场外广场", "克利安德", "克利安德"},
            {"165", "高句丽入侵", "北平城门", "司马懿", "司马懿"},
        };
        for (String[] entry : config) {
            BaseDungeonAction dungeon =
                    BaseDungeonAction.forLevel(Integer.parseInt(entry[0]));
            assertEquals(entry[1], dungeon.dungeonName());
            assertEquals(entry[2], dungeon.campName());
            assertEquals(entry[3], dungeon.entryNpcName());
            assertEquals(entry[4], dungeon.exitNpcName());
        }
        for (int level : DungeonBattleAutomation.LEVELS) {
            assertNotNull("缺少副本名：" + level,
                    BaseDungeonAction.forLevel(level).dungeonName());
            assertNotNull("缺少入口 NPC：" + level,
                    BaseDungeonAction.forLevel(level).entryNpcName());
            assertNotNull("缺少出口 NPC：" + level,
                    BaseDungeonAction.forLevel(level).exitNpcName());
        }
    }

    @Test
    public void dungeonLevelOptionsOnlyUseWudangGeneralDungeonLevels() {
        // res/values/arrays.xml 的 dungeon_list（一般副本）等级，没有 85 级。
        List<Integer> wudang = Arrays.asList(
                10, 20, 30, 40, 50, 60, 65, 70, 75, 80, 90, 105, 120, 135, 150, 165, 180,
                195, 210, 225, 240, 255, 270);
        for (int level : DungeonSweepAutomation.LEVELS) {
            assertTrue("扫荡副本没有这个等级：" + level, wudang.contains(level));
        }
        for (int level : DungeonBattleAutomation.LEVELS) {
            assertTrue("一般副本没有这个等级：" + level, wudang.contains(level));
        }
    }

    @Test
    public void findsTheNpcDialogRowThatCarriesTheWudangButtonText() {
        OcrText dialog = new OcrText(Arrays.asList(
                new OcrLine("再稍做准备", testRect(140, 432, 300, 468), 0.9f),
                new OcrLine("随时可以出击", testRect(140, 556, 320, 592), 0.9f),
                new OcrLine("交给我吧", testRect(140, 618, 240, 654), 0.9f)));
        assertEquals(1, DungeonBattleAutomation.npcRowForButton(dialog, "再稍做准备"));
        assertEquals(3, DungeonBattleAutomation.npcRowForButton(dialog, "随时可以出击"));
        assertEquals(4, DungeonBattleAutomation.npcRowForButton(dialog, "交给我吧"));
        assertEquals(0, DungeonBattleAutomation.npcRowForButton(dialog, "收下奖赏"));
        assertEquals(0, DungeonBattleAutomation.npcRowForButton(null, "进入"));
    }

    @Test
    public void reproducesTheWudangHighLevelRoutes() {
        // 80 级：拾取点 → 下路 → 右推 30 格一步。
        assertEquals(20, Dungeon80Action.decideRoute(10).targetX);
        assertEquals("下路", Dungeon80Action.decideRoute(20).interactionText);
        assertEquals(130, Dungeon80Action.decideRoute(100).targetX);
        assertEquals(27, Dungeon80Action.decideRoute(100).targetY);
        assertEquals("夏侯", Dungeon80Action.decideRoute(100).protectName);
        assertTrue(Dungeon80Action.decideRoute(399).exit);

        // 90/105：右推到各自的 part1，再离场。
        assertEquals(330, Dungeon90Action.decideRoute(300).targetX);
        assertEquals("赵云", Dungeon90Action.decideRoute(300).protectName);
        assertTrue(Dungeon90Action.decideRoute(390).exit);
        assertEquals(330, Dungeon105Action.decideRoute(300).targetX);
        assertEquals("张辽", Dungeon105Action.decideRoute(300).protectName);
        assertTrue(Dungeon105Action.decideRoute(390).exit);

        // 120：固定路点 350 → 320 → 270 → 220 → 175 → 135 → 100 → 80 → 60。
        Dungeon120Action level120 = new Dungeon120Action();
        assertEquals(350, level120.decideRoute(380).targetX);
        int[][] band330 = level120.decideRoute(340).waypoints;
        assertArrayEquals(new int[] {350, 25}, band330[0]);
        assertArrayEquals(new int[] {320, 25}, band330[1]);
        assertEquals(320, level120.decideRoute(340).targetX);
        assertEquals(270, level120.decideRoute(300).targetX);
        assertEquals(220, level120.decideRoute(240).targetX);
        assertEquals(175, level120.decideRoute(210).targetX);
        assertEquals(135, level120.decideRoute(150).targetX);
        assertEquals(100, level120.decideRoute(120).targetX);
        assertEquals(60, level120.decideRoute(75).targetX);
        assertTrue(level120.decideRoute(20).exit);
        assertArrayEquals(new int[] {55, 23}, level120.exitPoint());

        // 135：从右向左 30 格一步；到段尾会被夹到 min(begin,end)-5。
        assertEquals(350, Dungeon135Action.decideRoute(380).targetX);
        assertEquals(325, Dungeon135Action.decideRoute(350).targetX);
        assertEquals("吕蒙", Dungeon135Action.decideRoute(350).protectName);
        assertTrue(Dungeon135Action.decideRoute(10).exit);

        // 150：199 地图，右推；part0 到段尾夹到 max(begin,end)+5。
        assertEquals(55, Dungeon150Action.decideRoute(30).targetX);
        assertEquals(130, Dungeon150Action.decideRoute(100).targetX);
        assertTrue(Dungeon150Action.decideRoute(190).exit);

        // 165：守中央，偏离时回左右备用点。
        assertEquals(195, Dungeon165Action.decideRoute(150).targetX);
        assertEquals(222, Dungeon165Action.decideRoute(300).targetX);
        assertEquals(210, Dungeon165Action.decideRoute(210).targetX);
        assertEquals(29, Dungeon165Action.decideRoute(210).targetY);
        assertTrue(Dungeon165Action.decideRoute(210).exitWhenNoEnemy);
        assertArrayEquals(new int[] {210, 36}, BaseDungeonAction.forLevel(165).exitPoint());
    }

    @Test
    public void dungeonEntranceRouteUsesWudangCoordinatesAndArrivalCheck() {
        assertEquals(200, DungeonBattleAutomation.palaceGuideX("洛阳"));
        assertEquals(31, DungeonBattleAutomation.palaceGuideY("建业"));
        assertEquals(210, DungeonBattleAutomation.palaceGuideX("北平"));
        assertEquals(33, DungeonBattleAutomation.palaceGuideY("襄阳"));
        assertEquals(26, BaseDungeonAction.forLevel(10).campNpcX());
        assertEquals(105, BaseDungeonAction.forLevel(60).campNpcX());
        assertEquals(152, BaseDungeonAction.forLevel(75).campNpcX());
        assertEquals(100, BaseDungeonAction.forLevel(10).entryNpcX());
        assertEquals(102, BaseDungeonAction.forLevel(60).entryNpcX());
        assertEquals(16, BaseDungeonAction.forLevel(65).entryNpcY());
        assertEquals(17, BaseDungeonAction.forLevel(70).entryNpcY());
        assertTrue(BaseDungeonAction.forLevel(10).isAtEntryNpc("100,16"));
        assertTrue(BaseDungeonAction.forLevel(60).isAtEntryNpc("坐标 100，18"));
        assertTrue(BaseDungeonAction.forLevel(70).isAtEntryNpc("103,17"));
        assertFalse(BaseDungeonAction.forLevel(75).isAtEntryNpc("90,16"));
        assertFalse(BaseDungeonAction.forLevel(40).isAtEntryNpc("识别失败"));
        assertTrue(DungeonBattleAutomation.isAtCoordinate("208,34", 210, 33));
    }

    @Test
    public void dungeonNpcDialogUsesFixedWudangTitleAndOptionRows() {
        assertTrue(DungeonBattleAutomation.matchesNpcDialogTitle(
                "驻地 引路员", "驻地引路员"));
        assertTrue(DungeonBattleAutomation.matchesNpcDialogTitle(
                "乱军太平道长", "黄巾太平道长|乱军太平道长"));
        assertFalse(DungeonBattleAutomation.matchesNpcDialogTitle(
                "副本进度 3/10", "驻地引路员"));
        assertEquals(450, DungeonBattleAutomation.npcOptionY(1));
        assertEquals(512, DungeonBattleAutomation.npcOptionY(2));
        assertEquals(574, DungeonBattleAutomation.npcOptionY(3));
        assertEquals(636, DungeonBattleAutomation.npcOptionY(4));
        assertEquals(251, DungeonBattleAutomation.npcOptionX(3));
        assertEquals(176, DungeonBattleAutomation.npcOptionX(4));
    }

    @Test
    public void recognizesSuccessfulDungeonEntryFromTheMiniMapLikeWudang() {
        assertTrue(DungeonBattleAutomation.isCurrentDungeonMap(
                "十常侍之乱", "十常侍之乱"));
        assertTrue(DungeonBattleAutomation.isCurrentDungeonMap(
                "三英战吕布", "精英三英战吕布|三英战吕布"));
        assertFalse(DungeonBattleAutomation.isCurrentDungeonMap(
                "副本宫殿（一般）", "十常侍之乱"));
        assertFalse(DungeonBattleAutomation.isCurrentDungeonMap("", "十常侍之乱"));
    }

    @Test
    public void recognizesWudangStyleNoDungeonCountDialog() {
        assertTrue(DungeonBattleAutomation.isNoCountDialog(
                "您已经没有此副本的次数：无法获得所有怪物的掉落，无法领取通关奖励"));
        assertTrue(DungeonBattleAutomation.isNoCountDialog("没有副本次数"));
        assertFalse(DungeonBattleAutomation.isNoCountDialog("孙坚与玉玺 进入 离开"));
    }

    @Test
    public void dungeonNpcDialogsFollowEveryWudangClickSequence() {
        assertArrayEquals(new int[] {3, 3},
                BaseDungeonAction.forLevel(10).entryNpcRows());
        assertArrayEquals(new int[] {4, 4, 3},
                BaseDungeonAction.forLevel(20).entryNpcRows());
        assertArrayEquals(new int[] {2, 3, 3},
                BaseDungeonAction.forLevel(30).entryNpcRows());
        assertArrayEquals(new int[] {3, 3},
                BaseDungeonAction.forLevel(40).entryNpcRows());
        assertArrayEquals(new int[] {4, 3, 3},
                BaseDungeonAction.forLevel(50).entryNpcRows());
        assertArrayEquals(new int[] {4, 3, 3},
                BaseDungeonAction.forLevel(60).entryNpcRows());
        assertArrayEquals(new int[] {3, 3, 3},
                BaseDungeonAction.forLevel(65).entryNpcRows());
        assertArrayEquals(new int[] {4, 3, 3},
                BaseDungeonAction.forLevel(70).entryNpcRows());
        assertArrayEquals(new int[] {4, 3, 3},
                BaseDungeonAction.forLevel(75).entryNpcRows());

        assertArrayEquals(new int[] {4, 4},
                BaseDungeonAction.forLevel(10).exitNpcRows());
        assertArrayEquals(new int[] {4, 4},
                BaseDungeonAction.forLevel(20).exitNpcRows());
        assertArrayEquals(new int[] {4, 4},
                BaseDungeonAction.forLevel(30).exitNpcRows());
        assertArrayEquals(new int[] {4, 4},
                BaseDungeonAction.forLevel(40).exitNpcRows());
        assertArrayEquals(new int[] {4, 4},
                BaseDungeonAction.forLevel(50).exitNpcRows());
        assertArrayEquals(new int[] {4, 4},
                BaseDungeonAction.forLevel(60).exitNpcRows());
        assertArrayEquals(new int[] {4, 4},
                BaseDungeonAction.forLevel(65).exitNpcRows());
        assertArrayEquals(new int[] {4, 4, 4},
                BaseDungeonAction.forLevel(70).exitNpcRows());
        assertArrayEquals(new int[] {4, 4, 4},
                BaseDungeonAction.forLevel(75).exitNpcRows());
    }

    @Test
    public void reproducesTheFiveLevel10DungeonSections() {
        DungeonBattleAutomation.RouteDecision part0 = Dungeon10Action.decideRoute(599);
        assertTrue(part0.searchEnemies);
        assertEquals(Arrays.asList(7, 10), part0.enemyLevels);
        assertTrue(part0.targetX >= 550 && part0.targetX <= 559);
        assertEquals(505, Dungeon10Action.decideRoute(510).targetX);

        DungeonBattleAutomation.RouteDecision part1 = Dungeon10Action.decideRoute(450);
        assertTrue(part1.searchEnemies);
        assertEquals(Arrays.asList(7, 10), part1.enemyLevels);
        assertTrue(part1.targetX >= 401 && part1.targetX <= 410);
        assertEquals(387, Dungeon10Action.decideRoute(390).targetX);

        // part2 在武当里完全不打怪，只走 290 和 170 两个点。
        DungeonBattleAutomation.RouteDecision part2 = Dungeon10Action.decideRoute(350);
        assertFalse(part2.searchEnemies);
        assertEquals(290, part2.targetX);
        assertEquals(170, Dungeon10Action.decideRoute(300).targetX);
        assertFalse(Dungeon10Action.decideRoute(300).searchEnemies);

        DungeonBattleAutomation.RouteDecision part3 = Dungeon10Action.decideRoute(100);
        assertTrue(part3.searchEnemies);
        assertTrue(part3.acceptAnyEnemy);
        assertEquals(Arrays.asList(10), part3.enemyLevels);
        assertTrue(part3.targetX >= 51 && part3.targetX <= 60);

        assertTrue(Dungeon10Action.decideRoute(25).exit);
    }

    @Test
    public void dungeonEnemyOcrAnchorsOnTheGuideHeaderNotFixedRows() {
        // 坐标取自 1280x720 真机截图：表头 210..245，第一行 260..297。
        OcrLine header = new OcrLine("全部怪物", testRect(1000, 210, 1100, 245), 0.9f);
        OcrLine level = new OcrLine("27", testRect(965, 262, 995, 295), 0.9f);
        OcrLine name = new OcrLine("凉州力士", testRect(1010, 262, 1140, 295), 0.9f);
        OcrLine second = new OcrLine("30 凉州副将", testRect(965, 317, 1140, 350), 0.9f);
        // 面板外的数字：背包 76/91 在 y≈137，技能栏在 y≈560，副本进度在左上角。
        OcrLine bag = new OcrLine("76/91", testRect(1100, 122, 1160, 152), 0.9f);
        OcrLine skill = new OcrLine("10", testRect(1000, 550, 1030, 580), 0.9f);
        OcrLine progress = new OcrLine("副本进度10·42", testRect(780, 12, 1100, 42), 0.9f);

        List<DungeonBattleAutomation.EnemyRow> rows = DungeonBattleAutomation.readEnemyRows(
                Arrays.asList(progress, bag, header, name, level, second, skill));
        assertEquals(2, rows.size());
        assertEquals(Integer.valueOf(27), rows.get(0).level);
        assertTrue(rows.get(0).label.contains("凉州力士"));
        assertEquals(Integer.valueOf(30), rows.get(1).level);
        // 点击点必须落在这一行上。
        int tapY = (rows.get(0).bounds.top + rows.get(0).bounds.bottom) / 2;
        assertTrue(tapY >= 262 && tapY <= 295);

        // OCR 常把“剩余时间”和“敌人/寻路”并成一行，它在表头上方，不能当成敌人行。
        OcrLine chrome = new OcrLine(
                "利全时问 08·54 敌人 寻路", testRect(966, 136, 1160, 166), 0.9f);
        assertTrue(DungeonBattleAutomation.readEnemyRows(
                Arrays.asList(chrome, header)).isEmpty());

        // 没有表头就说明面板没开，不要瞎猜坐标。
        assertTrue(DungeonBattleAutomation.readEnemyRows(
                Arrays.asList(progress, bag, skill)).isEmpty());
    }

    private static android.graphics.Rect testRect(int left, int top, int right, int bottom) {
        android.graphics.Rect bounds = new android.graphics.Rect();
        bounds.left = left;
        bounds.top = top;
        bounds.right = right;
        bounds.bottom = bottom;
        return bounds;
    }

    @Test
    public void level10DungeonSelectsOnlyConfiguredEnemyRows() {
        DungeonBattleAutomation.EnemyRow level18 = new DungeonBattleAutomation.EnemyRow(
                18, "18级 路人", new android.graphics.Rect(0, 0, 1, 1));
        DungeonBattleAutomation.EnemyRow level10 = new DungeonBattleAutomation.EnemyRow(
                10, "10级 黄巾兵", new android.graphics.Rect(0, 0, 1, 1));
        assertEquals(level10, DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(level18, level10), Arrays.asList(7, 10), false));
        assertEquals(level10, DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(level18, level10), Arrays.asList(10), true));
        assertNull(DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(level18), Arrays.asList(7, 10), false));
    }

    @Test
    public void reproducesTheSixLevel20DungeonSections() {
        DungeonBattleAutomation.RouteDecision part0 = Dungeon20Action.decideRoute(599);
        assertEquals(Arrays.asList(18, 20), part0.enemyLevels);
        assertTrue(part0.targetX >= 550 && part0.targetX <= 559);
        assertEquals(480, Dungeon20Action.decideRoute(485).targetX);

        DungeonBattleAutomation.RouteDecision part1 = Dungeon20Action.decideRoute(400);
        assertFalse(part1.searchEnemies);
        assertEquals(350, part1.targetX);

        DungeonBattleAutomation.RouteDecision part2 = Dungeon20Action.decideRoute(300);
        assertTrue(part2.acceptAnyEnemy);
        assertEquals(Arrays.asList(20), part2.enemyLevels);
        assertTrue(part2.targetX >= 251 && part2.targetX <= 260);
        assertEquals(211, Dungeon20Action.decideRoute(215).targetX);

        DungeonBattleAutomation.RouteDecision part3 = Dungeon20Action.decideRoute(150);
        assertFalse(part3.acceptAnyEnemy);
        assertEquals(Arrays.asList(20), part3.enemyLevels);
        assertTrue(part3.targetX >= 101 && part3.targetX <= 110);
        assertEquals(170, part3.unstuckX);

        DungeonBattleAutomation.RouteDecision part4 = Dungeon20Action.decideRoute(60);
        assertTrue(part4.acceptAnyEnemy);
        assertTrue(part4.targetX >= 13 && part4.targetX <= 20);
        assertEquals(70, part4.unstuckX);

        DungeonBattleAutomation.RouteDecision part5 = Dungeon20Action.decideRoute(20);
        assertTrue(part5.exit);
        assertEquals(22, part5.targetX);
        assertTrue(Dungeon20Action.decideRoute(31).exit);
    }

    @Test
    public void retriesEveryLevel20And30GateWithTheWudangOffsets() {
        int[][] level20Gates = {{485, 480, 27}, {400, 350, 27}, {215, 211, 27}};
        for (int[] gate : level20Gates) {
            Dungeon20Action dungeon = new Dungeon20Action();
            assertGateOffsets(gate, call -> dungeon.decide(gate[0], 27));
        }

        int[][] level30Gates = {{470, 458, 4}, {320, 304, 27}, {180, 174, 4}};
        for (int[] gate : level30Gates) {
            Dungeon30Action dungeon = new Dungeon30Action();
            assertGateOffsets(gate, call -> dungeon.decideRoute(gate[0]));
        }
    }

    private static void assertGateOffsets(
            int[] gate, java.util.function.IntFunction<DungeonBattleAutomation.RouteDecision> next) {
        int[][] offsets = {{0, 0}, {2, 2}, {-2, -2}, {0, 0}};
        for (int index = 0; index < offsets.length; index++) {
            DungeonBattleAutomation.RouteDecision decision = next.apply(index);
            assertEquals(gate[1] + offsets[index][0], decision.targetX);
            assertEquals(gate[2] + offsets[index][1], decision.targetY);
        }
    }

    @Test
    public void reproducesTheFiveLevel30DungeonSectionsAndAlternatingDoorY() {
        Dungeon30Action dungeon = new Dungeon30Action();

        // part0：570 → 500 → (458,4)，第一次进入 510..579 时先补一个 570 路点。
        // 武当这两个路点都在 y=25，只有门点 (458,4) 例外。
        DungeonBattleAutomation.RouteDecision part0 = dungeon.decideRoute(599);
        assertTrue(part0.isBossOnly());
        assertTrue(part0.enemyLevels.isEmpty());
        assertEquals(570, part0.targetX);
        assertEquals(25, part0.targetY);
        int[][] band570 = dungeon.decideRoute(570).waypoints;
        assertEquals(2, band570.length);
        assertArrayEquals(new int[] {570, 25}, band570[0]);
        assertArrayEquals(new int[] {500, 25}, band570[1]);
        assertTrue(dungeon.decideRoute(570).isBossOnly());
        assertEquals(500, dungeon.decideRoute(540).targetX);
        assertEquals(25, dungeon.decideRoute(540).targetY);
        assertEquals(0, dungeon.decideRoute(540).waypoints.length);
        DungeonBattleAutomation.RouteDecision part0Door = dungeon.decideRoute(470);
        assertTrue(part0Door.isBossOnly());
        assertEquals(458, part0Door.targetX);
        assertEquals(4, part0Door.targetY);

        // part1：410 → 340 → (304,27)。
        assertTrue(dungeon.decideRoute(440).isBossOnly());
        assertEquals(410, dungeon.decideRoute(440).targetX);
        int[][] band420 = dungeon.decideRoute(400).waypoints;
        assertEquals(2, band420.length);
        assertArrayEquals(new int[] {410, 25}, band420[0]);
        assertArrayEquals(new int[] {340, 25}, band420[1]);
        assertEquals(340, dungeon.decideRoute(360).targetX);
        DungeonBattleAutomation.RouteDecision part1Door = dungeon.decideRoute(320);
        assertTrue(part1Door.isBossOnly());
        assertEquals(304, part1Door.targetX);
        assertEquals(27, part1Door.targetY);

        // part2：只找副本 BOSS，235 → 195 → (174,4)。
        DungeonBattleAutomation.RouteDecision part2 = dungeon.decideRoute(200);
        assertTrue(part2.isBossOnly());
        assertTrue(part2.enemyLevels.isEmpty());
        assertEquals(174, part2.targetX);
        assertEquals(4, part2.targetY);
        assertEquals(235, dungeon.decideRoute(260).targetX);
        assertEquals(195, dungeon.decideRoute(220).targetX);

        // part3：85 → 45 → 出口点 (24,27)。武当这一段先打“董军阵旗”，再找红名 BOSS。
        DungeonBattleAutomation.RouteDecision part3 = dungeon.decideRoute(100);
        assertFalse(part3.isBossOnly());
        assertTrue(part3.bossFallback);
        assertFalse(part3.acceptAnyEnemy);
        assertEquals(Arrays.asList("董军阵旗"), part3.priorityEnemyNames);
        assertEquals(85, part3.targetX);
        assertEquals(45, dungeon.decideRoute(60).targetX);
        assertEquals(24, dungeon.decideRoute(40).targetX);

        DungeonBattleAutomation.RouteDecision part4 = dungeon.decideRoute(24);
        assertTrue(part4.exit);
        assertEquals(24, part4.targetX);
    }

    @Test
    public void reproducesTheLevel40BossSweepWaypoints() {
        // 武当的固定阶梯：520/420/350/275/180/110/50/25。
        int[] positions = {570, 500, 400, 320, 220, 150, 80, 40};
        int[] targets = {520, 420, 350, 275, 180, 110, 50, 25};
        for (int index = 0; index < positions.length; index++) {
            DungeonBattleAutomation.RouteDecision decision =
                    Dungeon40Action.decideRoute(positions[index]);
            assertTrue(decision.searchEnemies);
            assertFalse(decision.acceptAnyEnemy);
            assertEquals(targets[index], decision.targetX);
            assertEquals(25, decision.targetY);
        }
        assertTrue(Dungeon40Action.decideRoute(32).searchEnemies);
        DungeonBattleAutomation.RouteDecision boundary =
                Dungeon40Action.decideRoute(31);
        assertTrue(boundary.exit);
        assertEquals(25, boundary.targetX);
        assertEquals(25, boundary.targetY);
        assertArrayEquals(new int[] {25, 25},
                BaseDungeonAction.forLevel(40).exitPoint());
    }

    @Test
    public void reproducesTheLevel50RightwardBossRouteAndCenterDialog() {
        // part0 是三选一，不是一次走完三个点：x<42 走 (46,6)，到位后按 y 接着走。
        assertEquals(46, Dungeon50Action.decideRoute(25, 25).targetX);
        assertEquals(6, Dungeon50Action.decideRoute(25, 25).targetY);
        assertEquals(46, Dungeon50Action.decideRoute(46, 6).targetX);
        assertEquals(45, Dungeon50Action.decideRoute(46, 6).targetY);
        assertEquals(60, Dungeon50Action.decideRoute(46, 45).targetX);
        assertEquals(45, Dungeon50Action.decideRoute(46, 45).targetY);
        // 武当 toRight 是固定 +30、y=27，不是随机 40–49。
        assertTrue(Dungeon50Action.decideRoute(80).searchEnemies);
        assertFalse(Dungeon50Action.decideRoute(80).acceptAnyEnemy);
        assertEquals(110, Dungeon50Action.decideRoute(80).targetX);
        assertEquals(27, Dungeon50Action.decideRoute(80).targetY);
        // 脱困：越过分段终点才退回 endX-10，没到终点则再往前顶 20 格，落点也在 y=27。
        assertEquals(130, Dungeon50Action.decideRoute(80).unstuckX);
        assertEquals(27, Dungeon50Action.decideRoute(80).unstuckY);
        assertEquals(130, Dungeon50Action.decideRoute(100).targetX);
        assertEquals(100, Dungeon50Action.decideRoute(100).unstuckX);
        assertEquals(180, Dungeon50Action.decideRoute(150).targetX);
        assertEquals(200, Dungeon50Action.decideRoute(150).unstuckX);
        assertEquals(224, Dungeon50Action.decideRoute(194).targetX);
        assertEquals(184, Dungeon50Action.decideRoute(194).unstuckX);
        assertEquals(317, Dungeon50Action.decideRoute(287).targetX);
        assertEquals(277, Dungeon50Action.decideRoute(287).unstuckX);
        assertEquals("交给我吧", Dungeon50Action.decideRoute(300).interactionText);
        assertTrue(Dungeon50Action.decideRoute(300).openNpc);
        assertNull(BaseDungeonAction.forLevel(50).interactionNpcName());
        assertEquals("孙坚", BaseDungeonAction.forLevel(50).exitNpcName());
        assertEquals(380, Dungeon50Action.decideRoute(350).targetX);
        assertEquals(400, Dungeon50Action.decideRoute(350).unstuckX);
        assertEquals(480, Dungeon50Action.decideRoute(450).targetX);
        assertEquals(500, Dungeon50Action.decideRoute(450).unstuckX);
        assertEquals(575, Dungeon50Action.decideRoute(550).targetX);
        assertEquals(595, Dungeon50Action.decideRoute(550).unstuckX);
        assertTrue(Dungeon50Action.decideRoute(590).exit);
        assertArrayEquals(new int[] {570, 25},
                BaseDungeonAction.forLevel(50).exitPoint());
    }

    @Test
    public void keepsWaitingWhileActiveDungeonBossNameIsVisible() {
        assertTrue(DungeonBattleAutomation.containsBossName("52 贾诩", "贾诩"));
        assertTrue(DungeonBattleAutomation.containsBossName("战斗中：50宋果", "50宋果"));
        assertFalse(DungeonBattleAutomation.containsBossName("自动 寻路", "贾诩"));
    }

    @Test
    public void reproducesTheLevel60PortalRoomRoute() {
        Dungeon60Action dungeon = new Dungeon60Action();

        // partStep 0：走廊指向第一战斗房门点。武当走廊里不搜怪，只走门点。
        assertFalse(dungeon.decideRoute(50, 25).searchEnemies);
        assertEquals(71, dungeon.decideRoute(50, 25).targetX);
        assertEquals(10, dungeon.decideRoute(50, 25).targetY);
        assertArrayEquals(new int[] {71, 4}, dungeon.decideRoute(50, 25).waypoints[1]);

        // 第一战斗房只打田丰，用 toRight 逐格扫（随机 20–29 格），扫到房尾才传送。
        DungeonBattleAutomation.RouteDecision room1 = dungeon.decideRoute(150, 25);
        assertEquals(Arrays.asList("田丰"), room1.priorityEnemyNames);
        assertFalse(room1.acceptAnyEnemy);
        assertEquals(0, room1.waypoints.length);
        assertTrue(room1.targetX >= 170 && room1.targetX <= 179);
        DungeonBattleAutomation.RouteDecision room1Exit = dungeon.decideRoute(190, 25);
        assertEquals(114, room1Exit.targetX);
        assertArrayEquals(new int[] {114, 46}, room1Exit.waypoints[1]);

        // 传送区点一次快捷键并推进 partStep，走廊随之指向第二战斗房。
        assertEquals(1, dungeon.decideRoute(515, 25).shortcutClicks);
        assertEquals(70, dungeon.decideRoute(50, 25).targetX);
        assertArrayEquals(new int[] {70, 46}, dungeon.decideRoute(50, 25).waypoints[1]);

        // 第二战斗房打文丑，扫完走 (214,10) → (214,4)。
        assertEquals(Arrays.asList("文丑"), dungeon.decideRoute(250, 25).priorityEnemyNames);
        DungeonBattleAutomation.RouteDecision room2Exit = dungeon.decideRoute(290, 25);
        assertEquals(214, room2Exit.targetX);
        assertArrayEquals(new int[] {214, 4}, room2Exit.waypoints[1]);

        assertEquals(1, dungeon.decideRoute(540, 25).shortcutClicks);
        assertEquals(90, dungeon.decideRoute(50, 25).targetX);
        assertArrayEquals(new int[] {94, 27}, dungeon.decideRoute(50, 25).waypoints[1]);

        // 第三战斗房打颜良。
        assertEquals(Arrays.asList("颜良"), dungeon.decideRoute(350, 25).priorityEnemyNames);
        DungeonBattleAutomation.RouteDecision room3Exit = dungeon.decideRoute(390, 25);
        assertEquals(380, room3Exit.targetX);
        assertArrayEquals(new int[] {394, 27}, room3Exit.waypoints[1]);

        assertEquals(570, dungeon.decideRoute(560, 25).targetX);
        assertEquals(2, dungeon.decideRoute(570, 27).shortcutClicks);
        // 最终 BOSS 段是红名，扫到段尾才去出口点。
        assertTrue(dungeon.decideRoute(450).isBossOnly());
        assertEquals(486, dungeon.decideRoute(470).targetX);
        assertTrue(dungeon.decideRoute(490).exit);
        assertArrayEquals(new int[] {486, 27},
                BaseDungeonAction.forLevel(60).exitPoint());
    }

    @Test
    public void reproducesTheLevel65GateAndLuBuRoute() {
        assertEquals(34, Dungeon65Action.decideRoute(20).targetX);
        assertEquals("出击", Dungeon65Action.decideRoute(34).interactionText);
        assertTrue(Dungeon65Action.decideRoute(34).openNpc);
        assertEquals(245, Dungeon65Action.decideRoute(150).targetX);
        // 三层：优先 64 级 BOSS → 精英才有的名字 → 次选等级 [1,63]，全落空就继续推进。
        assertEquals(Arrays.asList(64),
                Dungeon65Action.decideRoute(150).priorityEnemyLevels);
        assertEquals(Arrays.asList(1, 63), Dungeon65Action.decideRoute(150).enemyLevels);
        assertFalse(Dungeon65Action.decideRoute(150).acceptAnyEnemy);
        // 231..250 武当照样搜怪，再用 toRight 顶进下一段，不是纯移动。
        assertTrue(Dungeon65Action.decideRoute(245).searchEnemies);
        assertEquals(275, Dungeon65Action.decideRoute(245).targetX);
        // 252..349 逐格扫；310..380 这段走廊的 y 是 37。
        assertEquals(330, Dungeon65Action.decideRoute(300).targetX);
        assertEquals(27, Dungeon65Action.decideRoute(300).targetY);
        assertEquals(350, Dungeon65Action.decideRoute(320).targetX);
        assertEquals(37, Dungeon65Action.decideRoute(320).targetY);
        assertEquals(409, Dungeon65Action.decideRoute(350, 25).targetX);
        assertEquals(30, Dungeon65Action.decideRoute(350, 25).targetY);
        int[][] gate = Dungeon65Action.decideRoute(350, 25).waypoints;
        assertArrayEquals(new int[] {410, 10}, gate[1]);
        assertArrayEquals(new int[] {410, 4}, gate[2]);
        // 440..537 武当找的是红名副本 BOSS，不按等级；扫到段尾才走 538。
        assertTrue(Dungeon65Action.decideRoute(500).isBossOnly());
        assertEquals(530, Dungeon65Action.decideRoute(500).targetX);
        assertEquals(538, Dungeon65Action.decideRoute(520).targetX);
        assertTrue(Dungeon65Action.decideRoute(550).exit);
    }

    @Test
    public void marksTheWudangFakeBossZoneAtTheMidpointOfTheSpawnRange() {
        // 房陵 孟达：区间 500-590，中点 545，假王位 535-555。
        WorldBossCatalog.BossEntry mengDa =
                WorldBossCatalog.findMapByName("房陵").findBoss("孟达");
        assertTrue(mengDa.isInFakeBossZone(545));
        assertTrue(mengDa.isInFakeBossZone(535));
        assertTrue(mengDa.isInFakeBossZone(555));
        assertFalse(mengDa.isInFakeBossZone(534));
        assertFalse(mengDa.isInFakeBossZone(556));
        assertFalse(mengDa.isInFakeBossZone(500));
        assertFalse(mengDa.isInFakeBossZone(590));

        // 默认区间 260-340，中点 300，假王位 290-310。
        WorldBossCatalog.BossEntry chengYuanZhi =
                WorldBossCatalog.findMapByName("大兴山").findBoss("程远志");
        assertTrue(chengYuanZhi.isInFakeBossZone(300));
        assertTrue(chengYuanZhi.isInFakeBossZone(290));
        assertTrue(chengYuanZhi.isInFakeBossZone(310));
        assertFalse(chengYuanZhi.isInFakeBossZone(289));
        assertFalse(chengYuanZhi.isInFakeBossZone(311));
    }

    @Test
    public void reproducesTheWudangEliteDungeonEntries() {
        // dungeon_pro_list_cn 的 id 2060/2065/2070/2075 + DungeonUtil.h 的 type 2 宫殿入口。
        int[][] palace = {{60, 26}, {65, 41}, {70, 57}, {75, 73}};
        for (int[] entry : palace) {
            BaseDungeonAction elite = BaseDungeonAction.forElite(entry[0]);
            assertEquals(2, elite.dungeonType());
            assertEquals(entry[1], elite.campNpcX());
            // 精英副本的宫殿入口 y 是 20，一般副本是 16。
            assertEquals(20, elite.campNpcY());
            assertEquals(16, BaseDungeonAction.forLevel(entry[0]).campNpcY());
            assertEquals(1, BaseDungeonAction.forLevel(entry[0]).dungeonType());
        }
        assertEquals("精英盘河桥西", BaseDungeonAction.forElite(60).campName());
        assertEquals("精英虎牢关近郊", BaseDungeonAction.forElite(65).campName());
        assertEquals("精英安邑郊外", BaseDungeonAction.forElite(70).campName());
        // 75 级精英的驻地是“濮阳城内”，不是一般副本的“濮阳城外”。
        assertEquals("精英濮阳城内", BaseDungeonAction.forElite(75).campName());
        assertEquals("濮阳城外", BaseDungeonAction.forLevel(75).campName());

        // 入口/出口 NPC、驻地内 NPC 坐标、地图宽度都沿用一般副本。
        assertEquals("说书人", BaseDungeonAction.forElite(60).entryNpcName());
        assertEquals("公孙瓒", BaseDungeonAction.forElite(60).exitNpcName());
        assertEquals(102, BaseDungeonAction.forElite(60).entryNpcX());
        assertEquals(17, BaseDungeonAction.forElite(70).entryNpcY());
        assertEquals(199, BaseDungeonAction.forElite(75).dungeonMapMaxX());
        assertArrayEquals(new int[] {4, 4, 4}, BaseDungeonAction.forElite(75).exitNpcRows());
    }

    @Test
    public void eliteDungeonsReuseTheirGeneralRoutesExceptTheLevel65EnemyLevel() {
        // 武当 Dungeon60/70/75Action 不看副本类型，路线完全相同。
        assertArrayEquals(new int[] {486, 27}, BaseDungeonAction.forElite(60).exitPoint());
        assertEquals(115, BaseDungeonAction.forElite(70).decide(80, 27).targetX);
        assertEquals(73, BaseDungeonAction.forElite(75).decide(62, 27).targetX);
        assertTrue(BaseDungeonAction.forElite(75).decide(187, 27).exit);

        // Dungeon65Action：priorityEnemyLevels 一般 64、精英 68；次选目标也不是同一套。
        assertEquals(Arrays.asList(64),
                BaseDungeonAction.forLevel(65).decide(150, 27).priorityEnemyLevels);
        assertEquals(Arrays.asList(1, 63),
                BaseDungeonAction.forLevel(65).decide(150, 27).enemyLevels);
        assertEquals(Arrays.asList(68),
                BaseDungeonAction.forElite(65).decide(150, 27).priorityEnemyLevels);
        assertEquals(Arrays.asList(1),
                BaseDungeonAction.forElite(65).decide(150, 27).enemyLevels);
        assertEquals(Arrays.asList("虎牢关副将"),
                BaseDungeonAction.forElite(65).decide(150, 27).priorityEnemyNames);
        assertEquals(Arrays.asList(68),
                BaseDungeonAction.forElite(65).decide(350, 25).priorityEnemyLevels);
        assertEquals(245, BaseDungeonAction.forElite(65).decide(150, 27).targetX);

        // 精英 75 的次选等级表和一般副本不是同一张。
        assertEquals(Arrays.asList(1, 74, 75),
                BaseDungeonAction.forLevel(75).decide(100, 27).enemyLevels);
        assertEquals(Arrays.asList(1, 75, 79, 80),
                BaseDungeonAction.forElite(75).decide(100, 27).enemyLevels);
        assertEquals(Arrays.asList("吕虔", "于禁", "李典", "夏侯渊"),
                BaseDungeonAction.forElite(75).decide(100, 27).priorityEnemyNames);
        // 行脚商队：一般 75 在 (106,24)，精英 75 在 (115,21)。
        assertArrayEquals(new int[] {106, 24}, BaseDungeonAction.gearMerchantPoint(75, 1));
        assertArrayEquals(new int[] {115, 21}, BaseDungeonAction.gearMerchantPoint(75, 2));
        assertArrayEquals(new int[] {110, 26}, BaseDungeonAction.gearMerchantPoint(65, 1));
        assertArrayEquals(new int[] {107, 26}, BaseDungeonAction.gearMerchantPoint(65, 2));
    }

    @Test
    public void rejectsLevelsThatHaveNoEliteDungeon() {
        assertThrows(IllegalArgumentException.class, () -> BaseDungeonAction.forElite(10));
        assertThrows(IllegalArgumentException.class, () -> BaseDungeonAction.forElite(80));
        assertArrayEquals(new int[] {60, 65, 70, 75}, DungeonBattleAutomation.ELITE_LEVELS);
    }

    @Test
    public void reproducesTheLevel70EscortRouteAndNeverTargetsTheCarriage() {
        DungeonBattleAutomation.RouteDecision part0 = Dungeon70Action.decideRoute(80);
        assertEquals(115, part0.targetX);
        assertEquals(27, part0.targetY);
        assertEquals(Arrays.asList("长安城驻军队长", "长安白虎大门"),
                part0.priorityEnemyNames);
        assertEquals("马车", part0.protectName);

        // 护送是一段一段推的：40 → 90 → 115 → 230 → 330 → 450 → 540 → 560，
        // 中途站都在 y=25。一次寻路冲到 560 会把马车甩在后面。
        int[] positions = {10, 50, 80, 150, 300, 400, 500, 545};
        int[] targets = {40, 90, 115, 230, 330, 450, 540, 560};
        int[] targetYs = {25, 25, 27, 25, 25, 25, 25, 27};
        for (int index = 0; index < positions.length; index++) {
            DungeonBattleAutomation.RouteDecision leg =
                    Dungeon70Action.decideRoute(positions[index]);
            assertEquals(targets[index], leg.targetX);
            assertEquals(targetYs[index], leg.targetY);
            assertEquals("马车", leg.protectName);
        }
        // 偏离走廊时先把 y 拉回 25。
        assertEquals(300, Dungeon70Action.decideRoute(300, 45).targetX);
        assertEquals(25, Dungeon70Action.decideRoute(300, 45).targetY);
        assertTrue(Dungeon70Action.decideRoute(570).exit);

        DungeonBattleAutomation.EnemyRow carriage = new DungeonBattleAutomation.EnemyRow(
                70, "70级 马车", new android.graphics.Rect(0, 0, 1, 1));
        DungeonBattleAutomation.EnemyRow guard = new DungeonBattleAutomation.EnemyRow(
                70, "长安城驻军队长", new android.graphics.Rect(0, 0, 1, 1));
        assertEquals(guard, DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(carriage, guard), List.of(),
                part0.priorityEnemyNames, List.of(), part0.protectName, false));
        // 名单落空时武当不会退而打杂兵——护送途中乱打会脱离马车。
        DungeonBattleAutomation.EnemyRow mob = new DungeonBattleAutomation.EnemyRow(
                70, "70级 长安守军", new android.graphics.Rect(0, 0, 1, 1));
        assertFalse(part0.acceptAnyEnemy);
        assertNull(DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(carriage, mob), List.of(),
                part0.priorityEnemyNames, List.of(), part0.protectName, false));
    }

    @Test
    public void reproducesTheLevel75ShortMapGatesAndBossPriority() {
        // part0 武当按等级打杂兵，BOSS 名单只在 part1（x≥79）生效；x<61 用 toRight 顶进。
        DungeonBattleAutomation.RouteDecision part0 = Dungeon75Action.decideRoute(50);
        assertTrue(part0.priorityEnemyNames.isEmpty());
        assertFalse(part0.acceptAnyEnemy);
        assertEquals(Arrays.asList(1, 74, 75), part0.enemyLevels);
        assertEquals(80, part0.targetX);
        assertEquals(73, Dungeon75Action.decideRoute(62).targetX);
        // 67..78 只有 y 偏出走廊才走门点，否则继续往前顶。
        assertEquals(97, Dungeon75Action.decideRoute(67).targetX);
        assertEquals(73, Dungeon75Action.decideRoute(67, 40).targetX);

        // part1：(130,25) → (170,25) → 离场，边界是 120 和 160。
        DungeonBattleAutomation.RouteDecision part1 = Dungeon75Action.decideRoute(100);
        assertEquals(Arrays.asList("吕虔", "于禁", "李典", "夏侯渊"),
                part1.priorityEnemyNames);
        assertEquals(130, part1.targetX);
        assertEquals(25, part1.targetY);
        assertEquals(170, Dungeon75Action.decideRoute(150).targetX);
        assertEquals(25, Dungeon75Action.decideRoute(150).targetY);
        assertTrue(Dungeon75Action.decideRoute(160).exit);
        assertTrue(Dungeon75Action.decideRoute(187).exit);

        DungeonBattleAutomation.EnemyRow xiahou = new DungeonBattleAutomation.EnemyRow(
                75, "夏侯渊", new android.graphics.Rect(0, 0, 1, 1));
        DungeonBattleAutomation.EnemyRow lvdian = new DungeonBattleAutomation.EnemyRow(
                75, "李典", new android.graphics.Rect(0, 0, 1, 1));
        assertEquals(lvdian, DungeonBattleAutomation.selectEnemyRow(
                Arrays.asList(xiahou, lvdian), List.of(),
                part1.priorityEnemyNames, List.of(), null, false));
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
    public void acceptsAnyLoadedWildernessTrainingZone() {
        assertTrue(TaskAutomation.isWildernessTrainingMapName("荒野修炼1区"));
        assertTrue(TaskAutomation.isWildernessTrainingMapName("荒野修练 六 区"));
        assertFalse(TaskAutomation.isWildernessTrainingMapName("荒野营地"));
    }

    @Test
    public void picksTheGoldenChariotOnlyOnTheIronGateSecondFloor() {
        assertTrue(TrainingAutomation.isChariotMap("铁门峡二层"));
        // 地图名 OCR 常带空格，或者把周围的字一起认进来。
        assertTrue(TrainingAutomation.isChariotMap(" 铁门峡 二层 "));
        assertTrue(TrainingAutomation.isChariotMap("当前地图：铁门峡二层"));
        assertFalse(TrainingAutomation.isChariotMap("铁门峡一层"));
        assertFalse(TrainingAutomation.isChariotMap("荒野修炼三区"));
        assertFalse(TrainingAutomation.isChariotMap(""));
        assertFalse(TrainingAutomation.isChariotMap(null));

        // 落地瞬间地图名还没画出来，OCR 读回空串，这时要重试而不是当成别的图。
        assertTrue(TrainingAutomation.isBlankMapName(null));
        assertTrue(TrainingAutomation.isBlankMapName(""));
        assertTrue(TrainingAutomation.isBlankMapName("  "));
        assertFalse(TrainingAutomation.isBlankMapName("铁门峡二层"));
        assertFalse(TrainingAutomation.isBlankMapName("荒野修炼三区"));
    }

    @Test
    public void picksTheChariotRowOutOfTheGuideEnemyList() {
        // 坐标取自铁门峡二层截图：表头 140..170，三行敌人 180..300。
        OcrLine header = new OcrLine("全部怪物", testRect(1000, 140, 1120, 170), 0.9f);
        OcrLine chariotLevel = new OcrLine("35", testRect(965, 182, 995, 212), 0.9f);
        OcrLine chariotName = new OcrLine("黄巾刀车", testRect(1005, 182, 1120, 212), 0.9f);
        OcrLine lancer = new OcrLine("38 黄巾枪骑兵", testRect(965, 220, 1140, 250), 0.9f);
        OcrLine strongman = new OcrLine("39 黄巾力士", testRect(965, 258, 1140, 288), 0.9f);
        // 战斗飘字会盖到面板上，不能因此认错行。
        OcrLine damage = new OcrLine("200", testRect(1190, 190, 1245, 220), 0.9f);

        List<DungeonBattleAutomation.EnemyRow> rows = DungeonBattleAutomation.readEnemyRows(
                Arrays.asList(header, chariotLevel, chariotName, damage, lancer, strongman));
        android.graphics.Rect bounds = TrainingAutomation.findChariotBounds(rows);
        assertNotNull(bounds);
        int tapY = (bounds.top + bounds.bottom) / 2;
        assertTrue("点击点必须落在黄巾刀车那一行", tapY >= 182 && tapY <= 212);

        // 刀车没刷出来时不能误点别的怪。
        assertNull(TrainingAutomation.findChariotBounds(
                DungeonBattleAutomation.readEnemyRows(
                        Arrays.asList(header, lancer, strongman))));
        assertNull(TrainingAutomation.findChariotBounds(List.of()));

        // “巾/金”同音又形近，OCR 读成哪个都得认出来。
        OcrLine goldName = new OcrLine("35 黄金刀车", testRect(965, 182, 1120, 212), 0.9f);
        assertNotNull(TrainingAutomation.findChariotBounds(
                DungeonBattleAutomation.readEnemyRows(
                        Arrays.asList(header, goldName, lancer, strongman))));
    }

    @Test
    public void parsesAndLoopsThroughPullForOthersRoutePoints() {
        List<TrainingAutomation.PullPoint> route =
                TrainingAutomation.parsePullRoute(
                        "100,20,1000,1\n300，25，500，0");
        assertEquals(2, route.size());
        assertEquals(100, route.get(0).x);
        assertEquals(20, route.get(0).y);
        assertEquals(1_000, route.get(0).durationMillis);
        assertTrue(route.get(0).attack);
        assertFalse(route.get(1).attack);
        assertTrue(TrainingAutomation.pullPointReached("坐标 102,18", route.get(0)));
        assertFalse(TrainingAutomation.pullPointReached("坐标 103,20", route.get(0)));
    }

    @Test
    public void rejectsInvalidPullForOthersRoutePoints() {
        try {
            TrainingAutomation.parsePullRoute("601,20,1000,1");
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("out-of-range pull route must be rejected");
    }

    @Test
    public void givesValidCustomWildernessCoordinatePriority() {
        TrainingAutomation.CustomCoordinate coordinate =
                TrainingAutomation.parseCustomCoordinate("123", "27");
        assertEquals(123, coordinate.x);
        assertEquals(27, coordinate.y);
        assertTrue(TrainingAutomation.coordinateReached("坐标 125,25", coordinate.x, coordinate.y));
        assertFalse(TrainingAutomation.coordinateReached("坐标 126,25", coordinate.x, coordinate.y));
    }

    @Test
    public void rejectsInvalidCustomWildernessCoordinate() {
        assertThrows(IllegalArgumentException.class,
                () -> TrainingAutomation.parseCustomCoordinate("0", "27"));
        assertThrows(IllegalArgumentException.class,
                () -> TrainingAutomation.parseCustomCoordinate("123", "50"));
        assertThrows(IllegalArgumentException.class,
                () -> TrainingAutomation.parseCustomCoordinate("abc", "27"));
    }

    @Test
    public void schedulesTheEarliestIndependentMilitaryTask() {
        assertTrue(WorshipAlarmReceiver.hasConfiguredMilitaryTime(true, true));
        assertFalse(WorshipAlarmReceiver.hasConfiguredMilitaryTime(true, false));
        assertFalse(WorshipAlarmReceiver.hasConfiguredMilitaryTime(false, true));
        assertEquals(2 * 60 * 60 * 1_000L + 1_000,
                WorshipAlarmReceiver.nextRollingMilitaryAt(1_000));
        assertEquals(2_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 2_000, 5_000, 10_000));
        assertEquals(5_000, WorshipAlarmReceiver.nextMilitaryAt(
                1_000, 5_000, 0, 2_000));
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
    public void persistsScheduledTaskQueueInOrderWithoutDuplicates() {
        List<String> actions = Arrays.asList(
                "com.local.sgmhelper.WORSHIP",
                "com.local.sgmhelper.MILITARY",
                "com.local.sgmhelper.WORSHIP");
        String encoded = WorshipAlarmReceiver.encodePendingActions(actions);

        assertEquals(actions.subList(0, 2), WorshipAlarmReceiver.decodePendingActions(encoded));
        assertEquals("com.local.sgmhelper.WORSHIP",
                WorshipAlarmReceiver.scheduledActionForTaskKey("膜拜"));
        assertEquals("com.local.sgmhelper.MILITARY",
                WorshipAlarmReceiver.scheduledActionForTaskKey("自动军务"));
        assertNull(WorshipAlarmReceiver.scheduledActionForTaskKey("primary:TRAINING"));
    }

    @Test
    public void assignsMilitaryCooldownOnlyToTheSelectedQuest() {
        assertTrue(TaskAutomation.isMilitaryQuestDetail(
                "补充军团物资", "补充 军团物资 冷却时间2小时"));
        assertFalse(TaskAutomation.isMilitaryQuestDetail(
                "补充军团物资", "巡狩军团荒野 冷却时间3小时"));
    }

    @Test
    public void parsesMilitaryCooldownLikeWudang() {
        assertEquals(Integer.valueOf(120),
                TaskAutomation.extractCooldownMinutes("冷却时间2小时"));
        assertEquals(Integer.valueOf(31),
                TaskAutomation.extractCooldownMinutes("冷却时间30分"));
        assertEquals(Integer.valueOf(120),
                TaskAutomation.extractCooldownMinutes("冷却时间2小时30分"));
        assertNull(TaskAutomation.extractCooldownMinutes("冷却时间45秒"));
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
    public void restartsGameAfterScreenGuardFailureWithABound() {
        assertTrue(HelperAccessibilityService.shouldRestartAfterScreenGuard(1));
        assertTrue(HelperAccessibilityService.shouldRestartAfterScreenGuard(3));
        assertFalse(HelperAccessibilityService.shouldRestartAfterScreenGuard(0));
        assertFalse(HelperAccessibilityService.shouldRestartAfterScreenGuard(4));
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
        // 名单取自武当 arrays.xml 的 wild 表：1–3 区连低级怪一起有六种，
        // 100 级是“螳蝎妖”、175 级是“式神童”，之前这里写的是错名。
        assertEquals(Arrays.asList("30 蛇魔女", "40 火妖", "50 土霸王",
                        "60 食人花", "70 金甲龙", "75 圣武士"),
                TrainingAutomation.monstersForZone(1));
        assertEquals(Arrays.asList("80 魔斗士", "90 海妖", "100 螳蝎妖"),
                TrainingAutomation.monstersForZone(6));
        assertEquals(Arrays.asList("115 九尾狐", "130 石狮精"),
                TrainingAutomation.monstersForZone(7));
        assertEquals(Arrays.asList("145 人面鸟", "160 战鬼"),
                TrainingAutomation.monstersForZone(12));
        assertEquals(Arrays.asList("175 式神童", "190 剑齿虎"),
                TrainingAutomation.monstersForZone(15));
        assertEquals(Arrays.asList("265 史前猿", "280 死魂刀兵"),
                TrainingAutomation.monstersForZone(24));
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
        assertTrue(WildernessNavigator.isWildernessCampMapName("对话 荒野营地 地图"));
        assertFalse(WildernessNavigator.isWildernessCampMapName("荒野修炼十五区"));
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
                Arrays.asList("击败敌人", "敌人数量 3")));
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
    public void templateThresholdAndScaledBoundsAreRecordedWithoutOcrText() {
        WudangTemplateMatcher.Match match = templateMatch(
                WudangTemplateMatcher.Template.MAP_TAB, 0.95);
        assertEquals(WudangTemplateMatcher.DEFAULT_THRESHOLD, 0.85, 0.000001);
        assertTrue(match.found());
        assertEquals("地图", match.template.label);
    }

    @Test
    public void yuanbaoRecyclePageRequiresTheTitleTemplate() {
        Map<WudangTemplateMatcher.Template, WudangTemplateMatcher.Match> matches =
                new EnumMap<>(WudangTemplateMatcher.Template.class);
        assertFalse(AutoSellAutomation.isYuanbaoRecycleOpen(matches));

        matches.put(WudangTemplateMatcher.Template.YUANBAO_RECYCLE,
                templateMatch(WudangTemplateMatcher.Template.YUANBAO_RECYCLE, 0.95));
        assertTrue(AutoSellAutomation.isYuanbaoRecycleOpen(matches));

        matches.put(WudangTemplateMatcher.Template.YUANBAO_RECYCLE,
                templateMatch(WudangTemplateMatcher.Template.YUANBAO_RECYCLE, 0.80));
        assertFalse(AutoSellAutomation.isYuanbaoRecycleOpen(matches));
    }

    private static WudangTemplateMatcher.Match templateMatch(
            WudangTemplateMatcher.Template template, double score) {
        return new WudangTemplateMatcher.Match(template, template.assets[0], score,
                new Rect(620, 570, 660, 600), 4);
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
        assertEquals(LoginAutomation.Screen.LOGGED_IN,
                LoginAutomation.screenFor(Arrays.asList(
                        "【BOSS】准备游戏画面：关闭奖励找回",
                        "商城", "福利", "竞技场", "菜单")));
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
    public void recognizesInnFromItsFixedWindowTitle() {
        assertTrue(AutoSellAutomation.isInnScreenText("客栈"));
        assertTrue(AutoSellAutomation.isInnScreenText("客 栈"));
        assertFalse(AutoSellAutomation.isInnScreenText("女老板"));
        assertFalse(AutoSellAutomation.isInnScreenText("汉中"));
    }

    @Test
    public void convertsGameMapCoordinatesToSafeScreenTaps() {
        assertEquals(640, AutomationHost.mapScreenX(300));
        assertEquals(651, AutomationHost.mapScreenY(25));
        assertEquals(691, AutomationHost.mapScreenY(49));
        assertEquals(433, AutomationHost.mapScreenX(0));
        assertEquals(610, AutomationHost.mapScreenY(0));
        assertEquals(847, AutomationHost.mapScreenX(599));
        assertEquals(464, AutomationHost.mapScreenX(46, 599));
        assertEquals(684, AutomationHost.mapScreenY(45));
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
        assertEquals(Integer.valueOf(567), BossAutomation.parseMapX("567,49"));
        assertNull(BossAutomation.parseMapX("590.24"));
        assertNull(BossAutomation.parseMapX("590 24"));
        assertNull(BossAutomation.parseMapX("590：24"));
        assertEquals(Integer.valueOf(4), BossAutomation.parseMapX("坐标 4，7"));
        assertTrue(Arrays.equals(new int[] {349, 26},
                BossAutomation.parseMapCoordinate("29 349,26")));
        assertNull(BossAutomation.parseMapX("600,25"));
        assertTrue(Arrays.equals(new int[] {300, 25},
                BossAutomation.parseMapCoordinate("300,25")));
        assertEquals(Integer.valueOf(4), BossAutomation.parseChannel("第 4 分流"));
        assertEquals(Integer.valueOf(8), BossAutomation.parseChannel("临渊道·分流8"));
        assertEquals(Integer.valueOf(1), BossAutomation.parseChannel("分流\n1"));
        assertNull(BossAutomation.parseChannel("第9分流"));
        assertEquals(Integer.valueOf(3), BossAutomation.parseLeaderChannel("3"));
        assertEquals(Integer.valueOf(7), BossAutomation.parseLeaderChannel("分流7"));
        assertNull(BossAutomation.parseLeaderChannel("63/72"));
        assertTrue(BossAutomation.isStableLeaderChannel(4, 4));
        assertFalse(BossAutomation.isStableLeaderChannel(4, 5));
        assertFalse(BossAutomation.isStableLeaderChannel(4, null));
        assertTrue(BossAutomation.isMenuAutoCandidate("自 动", 1150, 320));
        assertTrue(BossAutomation.isMenuAutoCandidate("自动", 1210, 130));
        assertFalse(BossAutomation.isMenuAutoCandidate("自动", 530, 320));
        assertFalse(BossAutomation.isMenuAutoCandidate("打开自动设置", 1170, 320));
        assertTrue(BossAutomation.matchesBossName("73 貂蝉", "貂蝉"));
        assertTrue(BossAutomation.matchesBossName("貂 蝉", "貂蝉"));
        assertFalse(BossAutomation.matchesBossName("71 李肃", "貂蝉"));
        assertFalse(BossAutomation.matchesBossName("刻表", "刘表"));
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
        assertEquals(16, HelperAccessibilityService.DEFAULT_HEAVENFALL_HOUR);
        assertEquals(0, HelperAccessibilityService.DEFAULT_HEAVENFALL_MINUTE);
        assertEquals(10,
                HelperAccessibilityService.DEFAULT_HEAVENFALL_DURATION_MINUTES);
        assertEquals(1, HelperAccessibilityService.DEFAULT_HEAVENFALL_ZONE);
        assertEquals(300_000, HeavenfallAutomation.durationMillis(5));
        assertTrue(HeavenfallAutomation.expired(300_000, 300_000));
        assertFalse(HeavenfallAutomation.expired(299_999, 300_000));
        assertTrue(HeavenfallAutomation.centerReached("300,25"));
        assertTrue(HeavenfallAutomation.centerReached("280,25"));
        assertFalse(HeavenfallAutomation.centerReached("279,25"));
        assertTrue(HeavenfallAutomation.centerReached("300,30"));
        assertFalse(HeavenfallAutomation.centerReached("300,31"));
        assertTrue(HeavenfallAutomation.isTargetChannel(1, 1));
        assertFalse(HeavenfallAutomation.isTargetChannel(2, 1));
        assertFalse(HeavenfallAutomation.isTargetChannel(null, 1));
    }

    @Test
    public void displaysTheActualWorldMapOcrValueInRecoveryErrors() {
        assertEquals("汉中", BossAutomation.displayOcrValue(" 汉中\n"));
        assertEquals("空", BossAutomation.displayOcrValue("  "));
        assertEquals("空", BossAutomation.displayOcrValue(null));
    }

    @Test
    public void skipsTheWorldBossMarkerOnlyOnTheConfiguredMap() {
        WorldBossCatalog.MapEntry target = WorldBossCatalog.findMapByName("潼关");
        assertTrue(BossAutomation.isAlreadyOnWorldMap("当前地图：潼 关", target));
        assertFalse(BossAutomation.isAlreadyOnWorldMap("当前地图：白马", target));
        assertFalse(BossAutomation.isAlreadyOnWorldMap("", target));
        assertFalse(BossAutomation.isAlreadyOnWorldMap("潼关", null));
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
    public void recognizesWelfarePageByBothKnownTitlesOrCategoryList() {
        assertTrue(WelfareAutomation.isWelfarePage(new OcrText(Arrays.asList(
                new OcrLine("群英助手", new Rect(0, 0, 100, 30), 1f)))));
        assertTrue(WelfareAutomation.isWelfarePage(new OcrText(Arrays.asList(
                new OcrLine("在线奖励", new Rect(0, 0, 100, 30), 1f),
                new OcrLine("每日挑战", new Rect(0, 40, 100, 70), 1f)))));
        assertFalse(WelfareAutomation.isWelfarePage(new OcrText(Arrays.asList(
                new OcrLine("商城 福利 竞技场 菜单", new Rect(0, 0, 200, 30), 1f)))));
    }

    @Test
    public void recognizesAnOpenDailySignInBeforeScrollingCategories() {
        assertTrue(WelfareAutomation.isDailySignInPage(new OcrText(Arrays.asList(
                new OcrLine("今日签到奖励!!", new Rect(300, 100, 700, 160), 1f)))));
        assertTrue(WelfareAutomation.isDailySignInPage(new OcrText(Arrays.asList(
                new OcrLine("每 日 签 到", new Rect(500, 180, 700, 220), 1f)))));
        assertFalse(WelfareAutomation.isDailySignInPage(new OcrText(Arrays.asList(
                new OcrLine("每日完成奖励", new Rect(500, 180, 800, 220), 1f)))));
    }

    @Test
    public void legionRewardOnlyStartsWhenNoTaskIsRunning() {
        assertTrue(RewardAutomation.shouldStartLegionReward(false));
        assertFalse(RewardAutomation.shouldStartLegionReward(true));
    }

    @Test
    public void extractsDungeonLevelWithoutMatchingOtherNumbers() {
        assertEquals(10, DungeonSweepAutomation.parseLevel("等级：10 人数：1~6"));
        assertEquals(180, DungeonSweepAutomation.parseLevel("等级: 180"));
        assertEquals(-1, DungeonSweepAutomation.parseLevel("每日次数：3/3"));
    }
}
