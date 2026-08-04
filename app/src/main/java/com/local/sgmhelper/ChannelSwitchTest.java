package com.local.sgmhelper;

final class ChannelSwitchTest {
    private static final long DIALOG_WAIT_MS = 1_000;
    private static final long FIRST_CHECK_WAIT_MS = 1_500;
    private static final long CHECK_INTERVAL_MS = 600;
    private static final int CHECKS_PER_CHANNEL = 10;

    private final AutomationHost host;
    private final ChannelSwitcher channelSwitcher;
    private final AntiCheatVerification antiCheatVerification;
    private int targetChannel;

    ChannelSwitchTest(AutomationHost host) {
        this.host = host;
        channelSwitcher = new ChannelSwitcher(host);
        antiCheatVerification = new AntiCheatVerification(host);
    }

    void start() {
        if (host.isAutomationRunning()) {
            host.showProgress("已有任务正在运行，请先终止");
            return;
        }
        targetChannel = 1;
        host.startIdleAutomation("换线测试：打开游戏", this::switchChannel);
    }

    private void switchChannel() {
        if (!host.isAutomationRunning()) {
            return;
        }
        int channel = targetChannel;
        targetChannel = ChannelSwitcher.nextChannel(targetChannel);
        host.showProgress("换线测试：切换到第 " + channel + " 分流");
        host.openChannelDropdown(
                () -> host.postDelayed(
                        () -> waitForChannelDialog(channel),
                        DIALOG_WAIT_MS));
    }

    private void waitForChannelDialog(int channel) {
        host.waitTemplateOrText(
                WudangTemplateMatcher.Template.LINE_INFO,
                "分流信息", true,
                WudangTemplateMatcher.LINE_INFO_LEFT,
                WudangTemplateMatcher.LINE_INFO_TOP,
                WudangTemplateMatcher.LINE_INFO_LEFT
                        + WudangTemplateMatcher.LINE_INFO_WIDTH,
                WudangTemplateMatcher.LINE_INFO_TOP
                        + WudangTemplateMatcher.LINE_INFO_HEIGHT,
                2, 1,
                () -> channelSwitcher.switchOpenTo(channel,
                        () -> host.postDelayed(
                                () -> checkChallenge(channel, CHECKS_PER_CHANNEL),
                                FIRST_CHECK_WAIT_MS)),
                () -> host.failAutomation("换线测试：分流窗口未打开"));
    }

    private void checkChallenge(int channel, int remainingChecks) {
        if (!host.isAutomationRunning()) {
            return;
        }
        host.showProgress("换线测试：第 " + channel + " 分流检查验证窗口");
        antiCheatVerification.captureChallengeIfVisible(visible -> {
            if (visible) {
                DiagnosticLog.info("CHANNEL_TEST",
                        "anti-cheat screenshot captured after switching to channel " + channel);
                host.completeAutomation();
            } else if (remainingChecks > 1) {
                host.postDelayed(
                        () -> checkChallenge(channel, remainingChecks - 1),
                        CHECK_INTERVAL_MS);
            } else {
                host.postDelayed(this::switchChannel, CHECK_INTERVAL_MS);
            }
        });
    }
}
