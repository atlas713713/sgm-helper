package com.local.sgmhelper;

final class Dungeon50Action extends BaseDungeonAction {
    @Override int level() { return 50; }
    @Override String dungeonName() { return "孙坚与玉玺"; }
    @Override String entryNpcName() { return "历战老兵"; }
    @Override String exitNpcName() { return "孙坚"; }
    @Override String campName() { return "追忆之地"; }
    @Override int campNpcX() { return 89; }
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
        if (x <= 49) {
            return DungeonBattleAutomation.RouteDecision.moveRoute(
                    new int[][] {{46, 6}, {46, 45}, {60, 45}});
        }
        if (x <= 110) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanRight(x, 112), 25, 0, true);
        }
        if (x <= 194) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanRight(x, 205), 25, 0, true);
        }
        if (x <= 287) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanRight(x, 291), 25, 0, true);
        }
        if (x <= 308) {
            return DungeonBattleAutomation.RouteDecision.interact("交给我吧");
        }
        if (x <= 388) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanRight(x, 399), 25, 0, true);
        }
        if (x <= 484) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanRight(x, 495), 25, 0, true);
        }
        if (x <= 581) {
            return DungeonBattleAutomation.RouteDecision.search(
                    scanRight(x, 583), 25, 0, true);
        }
        return DungeonBattleAutomation.RouteDecision.exit(570, 25);
    }
}
