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
        return decideRoute(x, y);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 25);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int y) {
        if (x >= 32) {
            // 武当先把 y 拉回 25 再继续推进，走廊之外的 y 会让阶梯路点寻不到路。
            if (y < 20 || y > 30) {
                return DungeonBattleAutomation.RouteDecision.move(x, 25, 0);
            }
            // part0（32..599）：只找红名副本 BOSS，清空后按固定阶梯路点往左走，
            // 武当这里没有随机推进。卡住时武当连走两次 (x+100,25) 往回退。
            return DungeonBattleAutomation.RouteDecision.search(
                    part0TargetX(x), 25, Math.min(x + 100, 599), false);
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
