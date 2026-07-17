package com.local.sgmhelper;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;

final class LoginAutomation {
    static final String PREF_ACCOUNT = "login_account";
    static final String PREF_PASSWORD = "login_password";

    private static final int SCREEN_ATTEMPTS = 20;
    private final AutomationHost host;

    LoginAutomation(AutomationHost host) {
        this.host = host;
    }

    void start(Runnable next) {
        inspectScreen(next, SCREEN_ATTEMPTS);
    }

    private void inspectScreen(Runnable next, int attempts) {
        host.showProgress("登录：检查游戏画面");
        host.recognizeText(text -> {
            Screen screen = screenFor(lines(text));
            switch (screen) {
                case ANNOUNCEMENT -> closeAnnouncement(next);
                case QUICK_LOGIN -> openAccountLogin(next);
                case ACCOUNT_LOGIN -> enterCredentials(next);
                case START -> startGame(next);
                case WELFARE -> closeWelfare(next);
                case UNCLAIMED_REWARDS -> closeUnclaimedRewards(next);
                case LOGGED_IN -> next.run();
                case UNKNOWN -> retry(next, attempts);
            }
        });
    }

    private void closeAnnouncement(Runnable next) {
        host.showProgress("登录：关闭最新公告");
        host.tap(861, 75,
                () -> host.tap(1063, 60,
                        () -> inspectScreen(next, SCREEN_ATTEMPTS)));
    }

    private void startGame(Runnable next) {
        host.showProgress("登录：开始游戏");
        host.tap(625, 591,
                () -> inspectScreen(next, SCREEN_ATTEMPTS));
    }

    private void openAccountLogin(Runnable next) {
        host.showProgress("登录：选择账号登录");
        host.tap(634, 498,
                () -> inspectScreen(next, SCREEN_ATTEMPTS));
    }

    private void enterCredentials(Runnable next) {
        SharedPreferences preferences = host.context().getSharedPreferences(
                HelperAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);
        String account = preferences.getString(PREF_ACCOUNT, "").trim();
        String password = preferences.getString(PREF_PASSWORD, "");
        if (account.isEmpty() || password.isEmpty()) {
            host.failAutomation("请先在设置→登录账号中保存账号和密码");
            return;
        }

        host.showProgress("登录：输入账号密码");
        host.setTextAt(600, 253, account,
                () -> host.setTextAt(600, 327, password,
                        () -> host.tap(470, 402,
                                () -> {
                                    host.showProgress("登录：提交登录");
                                    host.tap(855, 290,
                                            () -> host.postDelayed(
                                                    () -> inspectScreen(
                                                            next, SCREEN_ATTEMPTS),
                                                    8_000));
                                })));
    }

    private void closeWelfare(Runnable next) {
        host.closeWelfareWindow(() -> inspectScreen(next, SCREEN_ATTEMPTS));
    }

    private void closeUnclaimedRewards(Runnable next) {
        host.closeWelfareWindow(() -> inspectScreen(next, SCREEN_ATTEMPTS));
    }

    private void retry(Runnable next, int attempts) {
        if (attempts > 1) {
            host.postDelayed(() -> inspectScreen(next, attempts - 1), 1_000);
        } else {
            host.failAutomation("登录超时：未识别到登录页或游戏主界面");
        }
    }

    static Screen screenFor(List<String> rawLines) {
        List<String> lines = new ArrayList<>();
        for (String raw : rawLines) {
            lines.add(raw.replaceAll("\\s+", ""));
        }
        String all = String.join("", lines);
        if (all.contains("最新公告")) {
            return Screen.ANNOUNCEMENT;
        }
        if (ScreenGuard.isWelfareWindow(lines)) {
            return Screen.WELFARE;
        }
        if (all.contains("尚未领取的奖励") || all.contains("关闭界面")) {
            return Screen.UNCLAIMED_REWARDS;
        }
        if (all.contains("账号登录/注册") || all.contains("快捷登录")) {
            return Screen.QUICK_LOGIN;
        }
        if (all.contains("账号登录") && all.contains("密码")) {
            return Screen.ACCOUNT_LOGIN;
        }
        if (all.contains("开始游戏")) {
            return Screen.START;
        }
        int hudLabels = 0;
        for (String label : List.of("商城", "福利", "竞技场", "菜单")) {
            if (all.contains(label)) {
                hudLabels++;
            }
        }
        return hudLabels >= 2 ? Screen.LOGGED_IN : Screen.UNKNOWN;
    }

    private static List<String> lines(Text text) {
        List<String> result = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                result.add(line.getText());
            }
        }
        return result;
    }

    enum Screen {
        ANNOUNCEMENT,
        START,
        QUICK_LOGIN,
        ACCOUNT_LOGIN,
        WELFARE,
        UNCLAIMED_REWARDS,
        LOGGED_IN,
        UNKNOWN
    }
}
