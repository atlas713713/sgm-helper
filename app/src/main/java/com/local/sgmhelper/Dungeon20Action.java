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

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x >= 478) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 480), 27, 550, false, 18, 20);
        }
        if (x >= 348) {
            return DungeonBattleAutomation.RouteDecision.move(350, 27, 0);
        }
        if (x >= 206) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 211), 27, 280, true, 20);
        }
        if (x >= 91) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 91), 27, 150, false, 20);
        }
        if (x > 31) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 31), 27, 80, true, 20);
        }
        return DungeonBattleAutomation.RouteDecision.exit(22, 27);
    }
}
