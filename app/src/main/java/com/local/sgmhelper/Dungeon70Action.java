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

    /** 武当 `priorityEnemyNames`；`protectName` 是马车，任何分段都不能选它当目标。 */
    private static final List<String> PRIORITY =
            List.of("长安城驻军队长", "长安白虎大门");

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x, y);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 27);
    }

    /**
     * 护送是一段一段推的：武当 part0 停 (40,25)、(90,25)、(115,27)，part1 停
     * (230,25)、(330,25)、(450,25)、(540,25)，最后才到 (560,27)。一次寻路直接冲到
     * 560 会把马车甩在后面——马车按自己的速度走，人跑到终点就不再清路了。
     */
    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int y) {
        // 武当在护送途中先把 y 拉回 25，偏离走廊时阶梯路点会寻不到路。
        if (x >= 120 && x <= 550 && (y < 20 || y > 30)) {
            return DungeonBattleAutomation.RouteDecision.move(x, 25, 0);
        }
        if (x < 30) {
            return escort(40, 25);
        }
        if (x < 80) {
            return escort(90, 25);
        }
        if (x < 120) {
            return escort(115, 27);
        }
        if (x < 220) {
            return escort(230, 25);
        }
        if (x < 320) {
            return escort(330, 25);
        }
        if (x < 440) {
            return escort(450, 25);
        }
        if (x < 530) {
            return escort(540, 25);
        }
        if (x <= 550) {
            return escort(560, 27);
        }
        return DungeonBattleAutomation.RouteDecision.exit(560, 27);
    }

    /**
     * 武当这里是 `FindEnemyAction2(levels=null, names=priorityEnemyNames)`——只打这两个名字，
     * 没有等级兜底也不打任意敌人。护送途中乱打杂兵会脱离马车。
     */
    private static DungeonBattleAutomation.RouteDecision escort(int x, int y) {
        DungeonBattleAutomation.RouteDecision decision =
                DungeonBattleAutomation.RouteDecision.searchTargetBoss(x, y, 0, PRIORITY);
        decision.protectName = "马车";
        return decision;
    }
}
