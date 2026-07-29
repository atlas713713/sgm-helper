package com.local.sgmhelper;

final class Dungeon20Action extends BaseDungeonAction {
    @Override int level() { return 20; }
    @Override String dungeonName() { return "枭雄崛起"; }
    @Override String entryNpcName() { return "曹嵩"; }
    @Override String exitNpcName() { return "骑都尉曹操"; }
    @Override String campName() { return "曹公府"; }
    @Override int campNpcX() { return 42; }
    @Override int[] entryNpcRows() { return new int[] {4, 4, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {22, 27}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        boolean atGate = (x >= 478 && x < 491)
                || (x >= 348 && x < 478)
                || (x >= 206 && x < 221);
        return decideRoute(x, nextWudangGateAttempt(x, atGate));
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 0);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int gateAttempt) {
        if (x >= 478) {
            // part0（599..478）：打 18 或 20 级敌人；低于 491 先走 part1 门点 480。
            int[] gate = wudangGate(480, 27, gateAttempt);
            return DungeonBattleAutomation.RouteDecision.search(
                    x < 491 ? gate[0] : scanLeft(x, 473),
                    x < 491 ? gate[1] : 27, 550, false, 18, 20);
        }
        if (x >= 348) {
            // part1（474..348）：武当这一段不搜怪，直接走门点 350。
            int[] gate = wudangGate(350, 27, gateAttempt);
            return DungeonBattleAutomation.RouteDecision.move(gate[0], gate[1], 0);
        }
        if (x >= 206) {
            // part2（338..206）：先找副本 BOSS，再打 20 级阵旗；低于 221 走门点 211。
            int[] gate = wudangGate(211, 27, gateAttempt);
            return DungeonBattleAutomation.RouteDecision.search(
                    x < 221 ? gate[0] : scanLeft(x, 201),
                    x < 221 ? gate[1] : 27, 280, true, 20);
        }
        if (x >= 91) {
            // part3（199..91）：只打 20 级阵旗，不找 BOSS。
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 26), 27, 170, false, 20);
        }
        if (x > 31) {
            // part4（91..31）：本段 BOSS 加 20 级阵旗。
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 13), 27, 70, true, 20);
        }
        return DungeonBattleAutomation.RouteDecision.exit(22, 27);
    }
}
