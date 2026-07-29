package com.local.sgmhelper;

import java.util.List;

class Dungeon70Action extends BaseDungeonAction {
    @Override int level() { return 70; }
    @Override String dungeonName() { return "二狼劫献帝"; }
    @Override String entryNpcName() { return "毛玠"; }
    @Override String exitNpcName() { return "曹操"; }
    @Override String campName() { return "安邑郊外"; }
    @Override int campNpcX() { return 136; }
    @Override int entryNpcY() { return 17; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4, 4}; }
    @Override int[] exitPoint() { return new int[] {560, 27}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        List<String> priority = List.of("长安城驻军队长", "长安白虎大门");
        if (x < 120) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    115, 27, 0, priority, "马车");
        }
        if (x <= 568) {
            return DungeonBattleAutomation.RouteDecision.searchThenExit(
                    560, 27, priority, "马车");
        }
        return DungeonBattleAutomation.RouteDecision.exit(560, 27);
    }
}
