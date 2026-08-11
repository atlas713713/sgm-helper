package com.local.sgmhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class AutomationTaskManagerTest {
    @Test
    public void serializesInterruptsAndResumesPrimaryOnce() {
        List<String> events = new ArrayList<>();
        AutomationTaskManager manager = new AutomationTaskManager(
                new AutomationTaskManager.Listener() {
                    @Override
                    public void onStart(AutomationTaskManager.Run run) {
                        events.add("start:" + run.request.key);
                    }

                    @Override
                    public void onCancel(AutomationTaskManager.Run run) {
                        events.add("cancel:" + run.request.key);
                    }

                    @Override
                    public void onResumePrimary() {
                        events.add("resume");
                    }
                });
        assertTrue(manager.submit(request("primary", AutomationTaskManager.Kind.PRIMARY)));
        long primaryRun = manager.current().id;
        assertTrue(manager.submit(request("welfare", AutomationTaskManager.Kind.INTERRUPT)));
        long welfareRun = manager.current().id;
        assertFalse(manager.isCurrent(primaryRun));
        assertTrue(manager.submit(request("reward", AutomationTaskManager.Kind.INTERRUPT)));
        assertFalse(manager.submit(request("reward", AutomationTaskManager.Kind.INTERRUPT)));
        assertEquals(1, manager.pendingCount());

        assertTrue(manager.finish(welfareRun));
        long rewardRun = manager.current().id;
        assertEquals("reward", manager.current().request.key);
        assertTrue(manager.finish(rewardRun));

        assertEquals(List.of(
                "start:primary",
                "cancel:primary",
                "start:welfare",
                "start:reward",
                "resume"), events);
    }

    @Test
    public void stopInvalidatesCurrentAndClearsQueue() {
        List<String> cancelled = new ArrayList<>();
        AutomationTaskManager manager = new AutomationTaskManager(
                new AutomationTaskManager.Listener() {
                    @Override
                    public void onStart(AutomationTaskManager.Run run) {
                    }

                    @Override
                    public void onCancel(AutomationTaskManager.Run run) {
                        cancelled.add(run.request.key);
                    }

                    @Override
                    public void onResumePrimary() {
                    }
                });
        manager.submit(request("primary", AutomationTaskManager.Kind.PRIMARY));
        manager.submit(request("welfare", AutomationTaskManager.Kind.INTERRUPT));
        long welfareRun = manager.current().id;
        manager.submit(request("reward", AutomationTaskManager.Kind.INTERRUPT));

        manager.stopAll();

        assertFalse(manager.hasCurrent());
        assertFalse(manager.isCurrent(welfareRun));
        assertEquals(0, manager.pendingCount());
        assertEquals(List.of("primary", "welfare"), cancelled);
    }

    @Test
    public void idleOnlyAndPrimaryCannotOverlapCurrentTask() {
        AutomationTaskManager manager = new AutomationTaskManager(
                new AutomationTaskManager.Listener() {
                    @Override
                    public void onStart(AutomationTaskManager.Run run) {
                    }

                    @Override
                    public void onCancel(AutomationTaskManager.Run run) {
                    }

                    @Override
                    public void onResumePrimary() {
                    }
                });
        manager.submit(request("primary", AutomationTaskManager.Kind.PRIMARY));

        assertFalse(manager.submit(request("tool", AutomationTaskManager.Kind.IDLE_ONLY)));
        assertFalse(manager.submit(request("other-primary", AutomationTaskManager.Kind.PRIMARY)));
        assertEquals("primary", manager.current().request.key);
    }

    @Test
    public void restartPermanentlyInvalidatesOldRun() {
        AutomationTaskManager manager = new AutomationTaskManager(
                new AutomationTaskManager.Listener() {
                    @Override
                    public void onStart(AutomationTaskManager.Run run) {
                    }

                    @Override
                    public void onCancel(AutomationTaskManager.Run run) {
                    }

                    @Override
                    public void onResumePrimary() {
                    }
                });
        manager.submit(request("primary", AutomationTaskManager.Kind.PRIMARY));
        long oldRun = manager.current().id;

        assertTrue(manager.restart(oldRun,
                request("primary", AutomationTaskManager.Kind.PRIMARY)));

        assertFalse(manager.isCurrent(oldRun));
        assertTrue(manager.current().id > oldRun);
    }

    @Test
    public void failedInterruptContinuesQueueBeforeResumingPrimary() {
        List<String> events = new ArrayList<>();
        AutomationTaskManager manager = recordingManager(events);
        manager.submit(request("primary", AutomationTaskManager.Kind.PRIMARY));
        manager.submit(request("welfare", AutomationTaskManager.Kind.INTERRUPT));
        long welfareRun = manager.current().id;
        manager.submit(request("reward", AutomationTaskManager.Kind.INTERRUPT));

        assertTrue(manager.abort(welfareRun));
        assertEquals("reward", manager.current().request.key);
        assertTrue(manager.finish(manager.current().id));

        assertEquals(List.of(
                "start:primary",
                "cancel:primary",
                "start:welfare",
                "cancel:welfare",
                "start:reward",
                "resume"), events);
    }

    @Test
    public void scheduledDungeonRunsBattleThenSweepBeforeResumingPrimary() {
        List<String> events = new ArrayList<>();
        AutomationTaskManager manager = recordingManager(events);
        manager.submit(request("primary", AutomationTaskManager.Kind.PRIMARY));

        // 定时“自动副本”提交的两段。键必须不同，否则第二段会被当成重复任务合并掉。
        String battle = HelperAccessibilityService.taskKey("定时一般副本：打开游戏");
        String sweep = HelperAccessibilityService.taskKey("定时副本：打开游戏");
        assertNotEquals(battle, sweep);
        assertTrue(manager.submit(request(battle, AutomationTaskManager.Kind.INTERRUPT)));
        assertTrue(manager.submit(request(sweep, AutomationTaskManager.Kind.INTERRUPT)));

        assertTrue(manager.finish(manager.current().id));
        assertEquals(sweep, manager.current().request.key);
        assertTrue(manager.finish(manager.current().id));

        assertEquals(List.of(
                "start:primary",
                "cancel:primary",
                "start:" + battle,
                "start:" + sweep,
                "resume"), events);
    }

    @Test
    public void idleInterruptResumesPrimaryOnce() {
        List<String> events = new ArrayList<>();
        AutomationTaskManager manager = recordingManager(events);

        assertTrue(manager.submit(request("welfare", AutomationTaskManager.Kind.INTERRUPT)));
        assertTrue(manager.finish(manager.current().id));

        assertEquals(List.of("start:welfare", "resume"), events);
    }

    @Test
    public void highPriorityInterruptPreemptsAndThenRestartsRegularInterrupt() {
        List<String> events = new ArrayList<>();
        AutomationTaskManager manager = recordingManager(events);
        manager.submit(request("primary", AutomationTaskManager.Kind.PRIMARY));
        manager.submit(request("welfare", AutomationTaskManager.Kind.INTERRUPT));
        manager.submit(request(
                "reward", AutomationTaskManager.Kind.INTERRUPT));

        assertTrue(manager.submit(request(
                "heavenfall", AutomationTaskManager.Kind.HIGH_PRIORITY_INTERRUPT)));
        assertEquals("heavenfall", manager.current().request.key);
        assertEquals(2, manager.pendingCount());

        assertTrue(manager.finish(manager.current().id));
        assertEquals("welfare", manager.current().request.key);
        assertTrue(manager.finish(manager.current().id));
        assertEquals("reward", manager.current().request.key);
        assertTrue(manager.finish(manager.current().id));

        assertEquals(List.of(
                "start:primary",
                "cancel:primary",
                "start:welfare",
                "cancel:welfare",
                "start:heavenfall",
                "start:welfare",
                "start:reward",
                "resume"), events);
    }

    @Test
    public void militaryInventorySaleReturnsToMilitaryBeforeOtherQueuedTasks() {
        AutomationTaskManager manager = recordingManager(new ArrayList<>());
        manager.submit(request("primary", AutomationTaskManager.Kind.PRIMARY));
        manager.submit(request("自动军务", AutomationTaskManager.Kind.INTERRUPT));
        manager.submit(request("定时副本", AutomationTaskManager.Kind.INTERRUPT));

        assertTrue(manager.submit(request(
                "自动出售", AutomationTaskManager.Kind.HIGH_PRIORITY_INTERRUPT)));
        assertEquals("自动出售", manager.current().request.key);

        assertTrue(manager.finish(manager.current().id));
        assertEquals("自动军务", manager.current().request.key);
        assertTrue(manager.finish(manager.current().id));
        assertEquals("定时副本", manager.current().request.key);
    }

    @Test
    public void welfareHighPriorityRunsBeforeMilitary() {
        AutomationTaskManager manager = recordingManager(new ArrayList<>());
        manager.submit(request("primary", AutomationTaskManager.Kind.PRIMARY));
        manager.submit(request("领取福利", AutomationTaskManager.Kind.HIGH_PRIORITY_INTERRUPT));
        manager.submit(request("自动军务", AutomationTaskManager.Kind.INTERRUPT));

        assertEquals("领取福利", manager.current().request.key);
        assertEquals(1, manager.pendingCount());
        assertTrue(manager.finish(manager.current().id));
        assertEquals("自动军务", manager.current().request.key);
    }

    private static AutomationTaskManager recordingManager(List<String> events) {
        return new AutomationTaskManager(new AutomationTaskManager.Listener() {
            @Override
            public void onStart(AutomationTaskManager.Run run) {
                events.add("start:" + run.request.key);
            }

            @Override
            public void onCancel(AutomationTaskManager.Run run) {
                events.add("cancel:" + run.request.key);
            }

            @Override
            public void onResumePrimary() {
                events.add("resume");
            }
        });
    }

    private static AutomationTaskManager.Request request(
            String key, AutomationTaskManager.Kind kind) {
        return new AutomationTaskManager.Request(key, key, kind, () -> {
        });
    }
}
