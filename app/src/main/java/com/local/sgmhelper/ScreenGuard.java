package com.local.sgmhelper;

import java.util.List;

final class ScreenGuard {
    private ScreenGuard() {
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
