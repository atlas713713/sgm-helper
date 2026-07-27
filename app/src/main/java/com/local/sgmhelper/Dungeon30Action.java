package com.local.sgmhelper;

import java.util.List;

final class Dungeon30Action extends BaseDungeonAction {
    @Override int level() { return 30; }
    @Override String dungeonName() { return "三公战华雄"; }
    @Override String entryNpcName() { return "黄巾太平道长|乱军太平道长"; }
    @Override String exitNpcName() { return "黄巾太平道长|乱军太平道长"; }
    @Override String campName() { return "黄巾营寨"; }
    @Override int campNpcX() { return 58; }
    @Override int[] entryNpcRows() { return new int[] {2, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x >= 451) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 458), 4, 570, false, 30);
        }
        if (x >= 301) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanLeft(x, 304), 27, 420, false, 30);
        }
        if (x >= 151) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    scanLeft(x, 174), 4, 0, List.of("董军阵旗"), null);
        }
        if (x >= 33) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    scanLeft(x, 32), 27, 100, List.of("董军阵旗"), null);
        }
        return DungeonBattleAutomation.RouteDecision.exit(24, 27);
    }
}
