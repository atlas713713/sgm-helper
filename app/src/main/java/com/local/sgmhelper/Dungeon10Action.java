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

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x >= 504) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 505), 27, 550, false, 7, 10);
        }
        if (x >= 386) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 387), 27, 450, false, 7, 10);
        }
        if (x >= 168) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 170), 27, 0, false, 10);
        }
        if (x >= 39) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 38), 27, 110, false, 10);
        }
        return DungeonBattleAutomation.RouteDecision.exit(25, 27);
    }
}
