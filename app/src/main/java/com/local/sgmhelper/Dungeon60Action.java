package com.local.sgmhelper;

final class Dungeon60Action extends BaseDungeonAction {
    @Override int level() { return 60; }
    @Override String dungeonName() { return "界桥之战"; }
    @Override String entryNpcName() { return "说书人"; }
    @Override String exitNpcName() { return "公孙瓒"; }
    @Override String campName() { return "盘河桥西"; }
    @Override int campNpcX() { return 105; }
    @Override int entryNpcX() { return 102; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x, y);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 25);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int y) {
        if (x <= 98) {
            return DungeonBattleAutomation.RouteDecision.searchRoute(
                    new int[][] {{71, 10}, {71, 4}});
        }
        if (x >= 505 && x <= 528) {
            return DungeonBattleAutomation.RouteDecision.shortcut(
                    1, "离开第一战斗房");
        }
        if (x >= 105 && x <= 198) {
            return DungeonBattleAutomation.RouteDecision.searchRoute(
                    new int[][] {{70, 40}, {70, 46}});
        }
        if (x >= 531 && x <= 553) {
            return DungeonBattleAutomation.RouteDecision.shortcut(
                    1, "离开第二战斗房");
        }
        if (x >= 205 && x <= 298) {
            return DungeonBattleAutomation.RouteDecision.searchRoute(
                    new int[][] {{90, 27}, {94, 27}});
        }
        if (x >= 305 && x <= 398) {
            return DungeonBattleAutomation.RouteDecision.searchRoute(
                    new int[][] {{380, 27}, {394, 27}});
        }
        if (x >= 556 && x <= 581) {
            if (isNear(x, y, 570, 27)) {
                return DungeonBattleAutomation.RouteDecision.shortcut(
                        2, "通过中央传送点");
            }
            return DungeonBattleAutomation.RouteDecision.move(570, 27, 0);
        }
        if (x >= 405 && x <= 479) {
            return DungeonBattleAutomation.RouteDecision.search(486, 25, 0, true);
        }
        return DungeonBattleAutomation.RouteDecision.exit(486, 27);
    }
}
