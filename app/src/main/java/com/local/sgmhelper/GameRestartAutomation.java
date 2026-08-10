package com.local.sgmhelper;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 定时重启游戏：把游戏进程杀干净，再交回主线任务重新拉起并登录。
 *
 * <p>只做“杀进程”这一段。杀完直接结束本次插入任务，任务管理器会走 {@code onResumePrimary}
 * 把主线任务重跑一遍，而主线任务本来就是从“打开游戏 → 登录”开始的，不用在这里重复一套。
 *
 * <p>先走 su 快速路径；模拟器没有可用 su 时，回退到系统应用详情页的无障碍按钮。
 */
final class GameRestartAutomation {
    static final String GAME_PACKAGE = "hk.phx.khm.cs";
    /** 杀完等一会儿再让主线任务拉起游戏，避免进程还没退干净就重开。 */
    private static final long AFTER_STOP_DELAY_MS = 5_000;
    /** su 卡住时不能一直挂着插入任务，超时就当失败处理，让主线任务照常恢复。 */
    private static final long FORCE_STOP_TIMEOUT_MS = 20_000;

    private final AutomationHost host;

    GameRestartAutomation(AutomationHost host) {
        this.host = host;
    }

    void run() {
        host.showProgress("定时重启：关闭游戏进程");
        long runId = host.currentRunId();
        forceStopAsync(stopped -> {
            if (!host.isRunCurrent(runId)) {
                return;
            }
            if (stopped) {
                finish(runId, true);
                return;
            }
            host.showProgress("定时重启：无 root，改用系统强行停止");
            host.forceStopPackageViaSettings(GAME_PACKAGE,
                    settingsStopped -> finish(runId, settingsStopped));
        });
    }

    private void finish(long runId, boolean stopped) {
        if (!host.isRunCurrent(runId)) {
            return;
        }
        if (!stopped) {
            host.failAutomation("定时重启：无法通过 root 或系统设置关闭游戏");
            return;
        }
        host.showProgress("定时重启：游戏已关闭，等待重新登录");
        host.postDelayed(host::resumePrimaryTask, AFTER_STOP_DELAY_MS);
    }

    private void forceStopAsync(Consumer<Boolean> result) {
        Handler main = new Handler(Looper.getMainLooper());
        AtomicBoolean answered = new AtomicBoolean();
        main.postDelayed(() -> {
            if (answered.compareAndSet(false, true)) {
                DiagnosticLog.warn("RESTART", "force-stop timed out");
                result.accept(false);
            }
        }, FORCE_STOP_TIMEOUT_MS);
        Thread worker = new Thread(() -> {
            boolean stopped = forceStopGame();
            main.post(() -> {
                if (answered.compareAndSet(false, true)) {
                    result.accept(stopped);
                }
            });
        }, "sgm-game-restart");
        worker.setDaemon(true);
        worker.start();
    }

    /** 阻塞执行，只能在后台线程调用。 */
    private static boolean forceStopGame() {
        for (String[] command : forceStopCommands(GAME_PACKAGE)) {
            if (runCommand(command)) {
                DiagnosticLog.info("RESTART", "force-stopped " + GAME_PACKAGE
                        + " via " + String.join(" ", command));
                return true;
            }
        }
        DiagnosticLog.error("RESTART", "unable to force-stop " + GAME_PACKAGE);
        return false;
    }

    /** 两种 su 写法轮着试：Magisk 认 {@code -c}，部分模拟器自带的 su 只认 {@code 0}。 */
    static List<String[]> forceStopCommands(String packageName) {
        String stop = "am force-stop " + packageName;
        return List.of(
                new String[] {"su", "-c", stop},
                new String[] {"su", "0", "am", "force-stop", packageName});
    }

    private static boolean runCommand(String[] command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            return process.waitFor() == 0;
        } catch (IOException error) {
            DiagnosticLog.warn("RESTART", "command failed: " + String.join(" ", command)
                    + " · " + error);
            return false;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
