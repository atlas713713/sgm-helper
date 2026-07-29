package com.local.sgmhelper;

final class Dungeon10Action extends BaseDungeonAction {
    @Override int level() { return 10; }
    @Override String dungeonName() { return "桃园结义"; }
    @Override String entryNpcName() { return "邹靖"; }
    @Override String exitNpcName() { return "义勇军刘玄德"; }
    @Override String campName() { return "幽州边境"; }
    @Override int campNpcX() { return 26; }
    @Override int[] entryNpcRows() { return new int[] {3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {25, 27}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        boolean atGate = (x >= 504 && x <= 550) || (x >= 168 && x <= 310);
        return decideRoute(x, nextWudangGateAttempt(x, atGate));
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 0);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int gateAttempt) {
        if (x >= 504) {
            // 武当 part1Enter=(505,27)，门点重试依次为中心、+2、-2、中心。
            int[] gate = wudangGate(505, 27, gateAttempt);
            return DungeonBattleAutomation.RouteDecision.search(
                    x <= 550 ? gate[0] : scanLeft(x, 499),
                    x <= 550 ? gate[1] : 27, x <= 550 ? 0 : 550, false, 7, 10);
        }
        if (x >= 386) {
            // part1（500..386）：同样打 7 或 10 级敌人；低于 397 先走门点 387。
            return DungeonBattleAutomation.RouteDecision.search(
                    x < 397 ? 387 : scanLeft(x, 381), 27, 450, false, 7, 10);
        }
        if (x >= 168) {
            // 武当 part3Enter=(170,27)，同样不能无限点击入口中心。
            int[] gate = wudangGate(170, 27, gateAttempt);
            return DungeonBattleAutomation.RouteDecision.move(
                    x > 310 ? 290 : gate[0], x > 310 ? 27 : gate[1], 0);
        }
        if (x >= 39) {
            // part3（159..39）：先找副本 BOSS，再打 10 级阵旗，一路推到出口段。
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 13), 27, 110, true, 10);
        }
        return DungeonBattleAutomation.RouteDecision.exit(25, 27);
    }
}
