package com.local.sgmhelper;

import java.util.List;

final class ScreenGuard {
    enum Blocker {
        NONE,
        GAME_WINDOW,
        DEFEATED,
        DISCONNECTED,
        DUPLICATE_LOGIN,
        ANTI_CHEAT
    }

    private ScreenGuard() {
    }

    static Blocker blockerFor(List<String> values) {
        StringBuilder combined = new StringBuilder();
        for (String raw : values) {
            combined.append(raw.replaceAll("\\s+", ""));
        }
        String all = combined.toString();

        if (all.contains("相同账号已在其他设备上登录")) {
            return Blocker.DUPLICATE_LOGIN;
        }
        if (all.contains("与地图服务器失去联机")) {
            return Blocker.DISCONNECTED;
        }
        if (all.contains("角色被击倒") || all.contains("自动功能已停止")
                || (all.contains("想要帮你复活") && all.contains("接受吗"))) {
            return Blocker.DEFEATED;
        }
        if (AntiCheatVerification.isChallengeText(all)) {
            return Blocker.ANTI_CHEAT;
        }
        if (hasGameWindow(values, all)) {
            return Blocker.GAME_WINDOW;
        }
        return Blocker.NONE;
    }

    private static boolean hasGameWindow(List<String> values, String all) {
        boolean deleteAllMail = all.contains("全部删除")
                || all.contains("全都删除")
                || all.contains("全部刪除")
                || all.contains("全都刪除");
        boolean mail = (all.contains("信件")
                && (all.contains("全部领取") || deleteAllMail
                        || all.contains("附件")))
                || (all.contains("全部领取") && deleteAllMail);
        boolean profile = (all.contains("能力资料") || all.contains("能カ资料"))
                && all.contains("装备资料");
        boolean legion = all.contains("指挥命令")
                || (all.contains("军团") && all.contains("公告")
                        && (all.contains("主页") || all.contains("成员")
                                || all.contains("列表") || all.contains("军务")));
        boolean party = BossAutomation.isPartyManageDialogText(all);
        return mail || profile || legion || party
                || TaskAutomation.isTaskWindowVisible(values)
                || hasAutoPathPanel(values);
    }

    static boolean isWelfareWindow(List<String> values) {
        boolean title = false;
        boolean category = false;
        for (String raw : values) {
            String value = raw.replaceAll("\\s+", "");
            title |= value.equals("福利");
            category |= value.contains("群英商店") || value.contains("在线奖励")
                    || value.contains("每日签到") || value.contains("每日挑战");
        }
        return title && category;
    }

    static boolean hasAutoPathPanel(List<String> values) {
        for (String raw : values) {
            String value = raw.replaceAll("\\s+", "");
            if (value.contains("敌人") || value.equals("寻路")) {
                return true;
            }
        }
        return false;
    }
}
