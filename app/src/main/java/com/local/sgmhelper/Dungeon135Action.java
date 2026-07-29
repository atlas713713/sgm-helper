package com.local.sgmhelper;

import java.util.List;

/** 135 级：399 地图，从右向左。保护目标“吕蒙”只从攻击目标里排除，不读血条。 */
final class Dungeon135Action extends BaseDungeonAction {
    private static final int PART0_BEGIN_X = 399;
    private static final int PART0_END_X = 330;
    private static final int PART1_BEGIN_X = 329;
    private static final int PART1_END_X = 10;
    private static final List<String> PRIORITY = List.of(
            "傅士仁", "糜芳", "周仓", "刘备精锐骑兵");

    @Override int level() { return 135; }
    @Override String dungeonName() { return "麦城之战"; }
    @Override String entryNpcName() { return "陆逊"; }
    @Override String exitNpcName() { return "陆逊"; }
    @Override String campName() { return "荆州东部"; }
    @Override int campNpcX() { return 248; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {25, 26}; }
    @Override int dungeonMapMaxX() { return 399; }
    @Override String[] entryNpcButtons() { return new String[] {"随时可以出击", "进入"}; }
    @Override String[] exitNpcButtons() { return new String[] {"收下奖赏"}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x >= PART0_END_X) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    stepLeft(x, PART0_BEGIN_X, PART0_END_X), 27, 0, PRIORITY, "吕蒙");
        }
        if (x > PART1_END_X) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    stepLeft(x, PART1_BEGIN_X, PART1_END_X), 27, 0, PRIORITY, "吕蒙");
        }
        return DungeonBattleAutomation.RouteDecision.exit(25, 26);
    }
}
