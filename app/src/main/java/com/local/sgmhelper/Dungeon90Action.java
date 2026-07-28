package com.local.sgmhelper;

import java.util.List;

/**
 * 90 级：399 地图，从左向右。保护目标“赵云”只从攻击目标里排除，不读血条。
 * 离场是“下一步 → 收下战利品”两步。
 */
final class Dungeon90Action extends BaseDungeonAction {
    private static final int PART0_END_X = 374;
    private static final int PART1_BEGIN_X = 375;
    private static final int PART1_END_X = 390;
    private static final List<String> PRIORITY = List.of(
            "张南", "张顗", "焦触", "钟缙", "钟绅");

    @Override int level() { return 90; }
    @Override String dungeonName() { return null; }
    @Override String entryNpcName() { return null; }
    @Override String exitNpcName() { return null; }
    @Override String campName() { return "90级副本驻地"; }
    @Override int campNpcX() { return 184; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4, 4}; }
    @Override int[] exitPoint() { return new int[] {380, 27}; }
    @Override int dungeonMapMaxX() { return 399; }
    @Override String[] entryNpcButtons() { return new String[] {"随时可以出击", "进入"}; }
    @Override String[] exitNpcButtons() { return new String[] {"下一步", "收下战利品"}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x <= PART0_END_X) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    stepRight(x, 0, PART0_END_X), 27, 0, PRIORITY, "赵云");
        }
        if (x < PART1_END_X) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    stepRight(x, PART1_BEGIN_X, PART1_END_X), 27, 0, PRIORITY, "赵云");
        }
        return DungeonBattleAutomation.RouteDecision.exit(380, 27);
    }
}
