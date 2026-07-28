package com.local.sgmhelper;

final class Dungeon65Action extends BaseDungeonAction {
    @Override int level() { return 65; }
    @Override String dungeonName() { return "三英战吕布"; }
    @Override String entryNpcName() { return "说书人"; }
    @Override String exitNpcName() { return "刘备"; }
    @Override String campName() { return "虎牢关近郊"; }
    @Override int campNpcX() { return 121; }
    @Override int[] entryNpcRows() { return new int[] {3, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {560, 27}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x, y);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 25);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int y) {
        if (x <= 58) {
            if (x >= 30 && x <= 40) {
                return DungeonBattleAutomation.RouteDecision.interactNpc("出击");
            }
            return DungeonBattleAutomation.RouteDecision.move(34, 22, 0);
        }
        if (x >= 79 && x <= 230) {
            return DungeonBattleAutomation.RouteDecision.search(245, 27, 0, true, 64);
        }
        if (x <= 250) {
            return DungeonBattleAutomation.RouteDecision.move(252, 27, 0);
        }
        if (x <= 437) {
            return DungeonBattleAutomation.RouteDecision.searchRoute(
                    new int[][] {{409, 30}, {410, 10}, {410, 4}}, 64);
        }
        if (x <= 537) {
            return DungeonBattleAutomation.RouteDecision.search(538, 27, 0, true, 64);
        }
        return DungeonBattleAutomation.RouteDecision.exit(560, 27);
    }
}
