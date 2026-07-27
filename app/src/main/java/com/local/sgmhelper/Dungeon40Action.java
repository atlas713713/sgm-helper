package com.local.sgmhelper;

final class Dungeon40Action extends BaseDungeonAction {
    @Override int level() { return 40; }
    @Override String dungeonName() { return "十常侍之乱"; }
    @Override String entryNpcName() { return "长乐宫官吏"; }
    @Override String exitNpcName() { return "袁绍"; }
    @Override String campName() { return "长乐宫"; }
    @Override int campNpcX() { return 73; }
    @Override int[] entryNpcRows() { return new int[] {3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x > 32) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 32), 25, 0, false);
        }
        return DungeonBattleAutomation.RouteDecision.exit(25, 25);
    }
}
