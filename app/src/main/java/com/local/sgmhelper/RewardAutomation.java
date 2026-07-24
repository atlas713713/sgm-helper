package com.local.sgmhelper;

import android.graphics.Bitmap;
import android.graphics.Color;

final class RewardAutomation {
    private final AutomationHost host;

    RewardAutomation(AutomationHost host) {
        this.host = host;
    }

    void startWorship() {
        host.startAutomation("膜拜：打开游戏",
                () -> host.tap(1215, 58, this::openRanking));
    }

    void startLegionReward() {
        host.startAutomation("军团奖励：打开游戏",
                () -> host.tap(1215, 58, this::openLegionReward));
    }

    private void openRanking() {
        host.showProgress("膜拜：查找排行榜");
        host.captureScreenshot(bitmap -> {
            try {
                if (bitmap != null && hasRankingIcon(bitmap)) {
                    host.tap(1190, 230, this::openGloryHall);
                } else {
                    host.swipe(1120, 250, 1120, 350,
                            () -> host.tap(1190, 330, this::openGloryHall));
                }
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        });
    }

    private void openGloryHall() {
        host.showProgress("膜拜：进入荣耀厅");
        host.tap(852, 358,
                () -> {
                    host.showProgress("膜拜：大丰收城");
                    host.tap(165, 196,
                            () -> host.tap(636, 623,
                                    () -> {
                                        host.showProgress("膜拜：国战榜");
                                        host.tap(167, 351,
                                                () -> host.tap(636, 623,
                                                        () -> host.tap(1233, 55,
                                                                this::resumePrimaryTask)));
                                    }));
                });
    }

    private void openLegionReward() {
        host.showProgress("军团奖励：领取俸禄");
        host.tap(994, 245,
                () -> host.tap(409, 101,
                        () -> host.tap(978, 178,
                                () -> host.tap(531, 406,
                                        () -> host.tap(950, 612,
                                                this::makeGeneralContributions)))));
    }

    private void makeGeneralContributions() {
        host.showProgress("军团奖励：进行贡献");
        host.tap(380, 569,
                () -> host.tap(380, 569,
                        () -> host.tap(380, 569,
                                () -> host.tap(380, 569,
                                        () -> host.tap(380, 569,
                                                () -> host.tap(380, 569,
                                                        () -> host.tap(1106, 49,
                                                                () -> host.tap(1106, 49,
                                                                        this::resumePrimaryTask))))))));
    }

    private void resumePrimaryTask() {
        host.showProgress("定时奖励已完成，10秒后恢复主要任务");
        host.postDelayed(host::resumePrimaryTask, 10_000);
    }

    static boolean hasRankingIcon(Bitmap bitmap) {
        int left = 1162 * bitmap.getWidth() / 1280;
        int right = 1234 * bitmap.getWidth() / 1280;
        int top = 188 * bitmap.getHeight() / 720;
        int bottom = 266 * bitmap.getHeight() / 720;
        int bright = 0;
        int red = 0;
        int total = 0;

        for (int y = top; y < bottom; y += 2) {
            for (int x = left; x < right; x += 2) {
                int color = bitmap.getPixel(x, y);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                if (r > 180 && g > 160 && b > 120) {
                    bright++;
                }
                if (r > 140 && r > g * 1.45f && r > b * 1.25f) {
                    red++;
                }
                total++;
            }
        }
        return bright * 100 >= total * 12 && red * 100 >= total * 7;
    }
}
