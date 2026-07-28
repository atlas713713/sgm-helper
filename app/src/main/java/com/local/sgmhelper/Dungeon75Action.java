package com.local.sgmhelper;

import java.util.List;

final class Dungeon75Action extends BaseDungeonAction {
    @Override int level() { return 75; }
    @Override String dungeonName() { return "濮阳之战"; }
    @Override String entryNpcName() { return "吕布"; }
    @Override String exitNpcName() { return "陈宫"; }
    @Override String campName() { return "濮阳城外"; }
    @Override int campNpcX() { return 152; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4, 4}; }
    @Override int[] exitPoint() { return new int[] {187, 27}; }
    @Override int dungeonMapMaxX() { return 199; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        List<String> priority = List.of("吕虔", "于禁", "李典", "夏侯渊");
        if (x < 65) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    73, 27, 0, priority, null);
        }
        if (x <= 78) {
            return DungeonBattleAutomation.RouteDecision.move(73, 27, 0);
        }
        if (x < 130) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    130, 27, 0, priority, null);
        }
        if (x < 180) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    187, 27, 0, priority, null);
        }
        return DungeonBattleAutomation.RouteDecision.exit(187, 27);
    }
}
