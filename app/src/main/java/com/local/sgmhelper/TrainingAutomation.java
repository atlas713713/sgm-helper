package com.local.sgmhelper;

final class TrainingAutomation {
    private final AutomationHost host;

    TrainingAutomation(AutomationHost host) {
        this.host = host;
    }

    void start() {
        host.showProgress("自动练级：停止自动攻击后使用第一个标记卷");
        host.useFirstMarker(() -> {
            host.showProgress("自动练级：等待地图加载");
            host.postDelayed(() -> {
                host.showProgress("自动练级：等待自动攻击按钮");
                host.ensureAutoAttackEnabled(() -> {
                    long nextMilitaryAt = WorshipAlarmReceiver.scheduleMilitary(host.context());
                    host.enterTraining(nextMilitaryAt);
                });
            }, 5_000);
        });
    }
}
