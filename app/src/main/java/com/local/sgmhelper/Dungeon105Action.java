package com.local.sgmhelper;

import java.util.List;

/** 105 级：399 地图，从左向右。保护目标“张辽”只从攻击目标里排除，不读血条。 */
final class Dungeon105Action extends BaseDungeonAction {
    private static final int PART0_END_X = 369;
    private static final int PART1_BEGIN_X = 370;
    private static final int PART1_END_X = 390;
    private static final List<String> PRIORITY = List.of("谷利");

    @Override int level() { return 105; }
    @Override String dungeonName() { return "逍遥津之战"; }
    @Override String entryNpcName() { return "乐进"; }
    @Override String exitNpcName() { return "乐进"; }
    @Override String campName() { return "合肥城中"; }
    @Override int campNpcX() { return 216; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {380, 27}; }
    @Override int dungeonMapMaxX() { return 399; }
    @Override String[] entryNpcButtons() { return new String[] {"随时可以出击", "进入"}; }
    @Override String[] exitNpcButtons() { return new String[] {"收下奖赏"}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x <= PART0_END_X) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    stepRight(x, 0, PART0_END_X), 27, 0, PRIORITY, "张辽");
        }
        if (x < PART1_END_X) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    stepRight(x, PART1_BEGIN_X, PART1_END_X), 27, 0, PRIORITY, "张辽");
        }
        return DungeonBattleAutomation.RouteDecision.exit(380, 27);
    }
}
