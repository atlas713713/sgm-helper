package com.local.sgmhelper;

final class ChannelSwitcher {
    private static final int SCROLLS = 4;
    private static final int FIRST_ROW_Y = 205;
    private static final int ROW_GAP_Y = 90;
    private static final int BOTTOM_FIRST_ROW_Y = 175;
    private static final int BOTTOM_ROW_GAP_Y = 88;

    private final AutomationHost host;

    ChannelSwitcher(AutomationHost host) {
        this.host = host;
    }

    void switchOpenTo(int channel, Runnable next) {
        if (channel < 1 || channel > 8) {
            throw new IllegalArgumentException("channel must be between 1 and 8");
        }
        if (scrollsToTop(channel)) {
            scrollToTop(channel, SCROLLS, next);
        } else {
            scrollToBottom(channel, SCROLLS, next);
        }
    }

    private void scrollToTop(int channel, int remaining, Runnable next) {
        if (remaining > 0) {
            host.swipe(640, 300, 640, 440,
                    () -> scrollToTop(channel, remaining - 1, next));
            return;
        }
        select(channel, next);
    }

    private void scrollToBottom(int channel, int remaining, Runnable next) {
        if (remaining > 0) {
            host.swipe(640, 440, 640, 300,
                    () -> scrollToBottom(channel, remaining - 1, next));
            return;
        }
        select(channel, next);
    }

    private void select(int channel, Runnable next) {
        host.tap(640, selectionY(channel),
                () -> host.tap(640, 605, () -> confirmSwitch(next)));
    }

    private void confirmSwitch(Runnable next) {
        host.clickText("确定", true, next, 5,
                () -> host.tap(530, 395, next));
    }

    static boolean scrollsToTop(int channel) {
        return channel <= 4;
    }

    static int selectionY(int channel) {
        if (channel <= 4) {
            return FIRST_ROW_Y + (channel - 1) * ROW_GAP_Y;
        }
        return BOTTOM_FIRST_ROW_Y + (channel - 4) * BOTTOM_ROW_GAP_Y;
    }

    static int nextChannel(int current) {
        if (current < 1 || current > 8) {
            throw new IllegalArgumentException("current channel must be between 1 and 8");
        }
        return current == 8 ? 1 : current + 1;
    }
}
