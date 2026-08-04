package com.local.sgmhelper;

import android.content.Context;
import android.content.SharedPreferences;


import java.util.ArrayList;
import java.util.List;

final class LoginAutomation {
    static final String PREF_ACCOUNT = "login_account";
    static final String PREF_PASSWORD = "login_password";
    static final String START_PROGRESS = "Login: tapping start";

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
        detectLoginMode(next, attempts);
    }

    /**
     * The original Wudang implementation identifies the two login-mode labels
     * in AsiaLoginPage.switchLogin before doing any wider screen analysis.
     * Keep OCR as the fallback for announcements, start-game and post-login
     * dialogs, but avoid Paddle OCR for the common login-mode transition.
     */
    private void detectLoginMode(Runnable next, int attempts) {
        host.matchTemplates(new WudangTemplateMatcher.Template[] {
                        WudangTemplateMatcher.Template.LOGIN_QUICK,
                        WudangTemplateMatcher.Template.LOGIN_ACCOUNT
                },
                WudangTemplateMatcher.LOGIN_SWITCH_LEFT,
                WudangTemplateMatcher.LOGIN_SWITCH_TOP,
                WudangTemplateMatcher.LOGIN_SWITCH_LEFT
                        + WudangTemplateMatcher.LOGIN_SWITCH_WIDTH,
                WudangTemplateMatcher.LOGIN_SWITCH_TOP
                        + WudangTemplateMatcher.LOGIN_SWITCH_HEIGHT,
                matches -> {
                    WudangTemplateMatcher.Match quick = matches.get(
                            WudangTemplateMatcher.Template.LOGIN_QUICK);
                    WudangTemplateMatcher.Match account = matches.get(
                            WudangTemplateMatcher.Template.LOGIN_ACCOUNT);
                    if (quick != null && quick.found()) {
                        openAccountLogin(next);
                    } else if (account != null && account.found()) {
                        enterCredentials(next);
                    } else {
                        inspectScreenWithOcr(next, attempts);
                    }
                }, error -> inspectScreenWithOcr(next, attempts));
    }

    private void inspectScreenWithOcr(Runnable next, int attempts) {
        host.recognizeText(text -> {
            List<String> values = lines(text);
            if (ScreenGuard.blockerFor(values) != ScreenGuard.Blocker.NONE) {
                host.ensureGameHudVisible(next);
                return;
            }
            Screen screen = screenFor(values);
            switch (screen) {
                case ANNOUNCEMENT -> closeAnnouncement(next);
                case QUICK_LOGIN -> openAccountLogin(next);
                case ACCOUNT_LOGIN -> enterCredentials(next);
                case START -> startGame(next);
                case WELFARE -> closeWelfare(next);
                case UNCLAIMED_REWARDS -> closeUnclaimedRewards(next);
                case REWARD_RECOVERY -> closeRewardRecovery(next);
                case LOGGED_IN -> host.ensureGameHudVisible(next);
                case UNKNOWN -> {
                    if (attempts == SCREEN_ATTEMPTS) {
                        DiagnosticLog.warn("LOGIN", "unknown screen OCR lines=" + values);
                    }
                    retry(next, attempts);
                }
            }
        });
    }

    private void closeAnnouncement(Runnable next) {
        host.showProgress("Login: closing announcement");
        host.tap(861, 75,
                () -> host.tap(1063, 60,
                        () -> inspectScreen(next, SCREEN_ATTEMPTS)));
    }

    private void startGame(Runnable next) {
        host.showProgress(START_PROGRESS);
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
        host.closeWelfareWindow(next);
    }

    private void closeRewardRecovery(Runnable next) {
        host.showProgress("Login: closing reward recovery");
        host.ensureGameHudVisible(() -> inspectScreen(next, SCREEN_ATTEMPTS));
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
            if (raw.contains("准备游戏画面")) {
                continue;
            }
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
        if (all.contains("奖励找回")
                || (all.contains("重复任务") && all.contains("副本奖励"))
                || all.contains("铜钱找回") || all.contains("元宝找回")) {
            return Screen.REWARD_RECOVERY;
        }
        if (all.contains("账号登录/注册") || all.contains("快捷登录")) {
            return Screen.QUICK_LOGIN;
        }
        if ((all.contains("账号登录") || all.contains("帐号登录"))
                && all.contains("密码")) {
            return Screen.ACCOUNT_LOGIN;
        }
        if (all.contains("开始游戏")) {
            return Screen.START;
        }
        if (all.contains("对话") && all.contains("地图")) {
            return Screen.LOGGED_IN;
        }
        int hudLabels = 0;
        for (String label : List.of("商城", "福利", "竞技场", "菜单")) {
            if (all.contains(label)) {
                hudLabels++;
            }
        }
        return hudLabels >= 2 ? Screen.LOGGED_IN : Screen.UNKNOWN;
    }

    private static List<String> lines(OcrText text) {
        List<String> result = new ArrayList<>();
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
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
        REWARD_RECOVERY,
        LOGGED_IN,
        UNKNOWN
    }
}
