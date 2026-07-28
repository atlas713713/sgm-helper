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
    @Override int[] exitPoint() { return new int[] {25, 25}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x >= 32) {
            // part0（32..599）：只找红名副本 BOSS，清空后按固定阶梯路点往左走，
            // 武当这里没有随机推进。
            return DungeonBattleAutomation.RouteDecision.search(
                    part0TargetX(x), 25, 0, false);
        }
        // part1（13..31）：直接走出口点。
        return DungeonBattleAutomation.RouteDecision.exit(25, 25);
    }

    private static int part0TargetX(int x) {
        if (x >= 530) {
            return 520;
        }
        if (x >= 430) {
            return 420;
        }
        if (x >= 360) {
            return 350;
        }
        if (x >= 285) {
            return 275;
        }
        if (x >= 190) {
            return 180;
        }
        if (x >= 120) {
            return 110;
        }
        if (x >= 60) {
            return 50;
        }
        return 25;
    }
}
