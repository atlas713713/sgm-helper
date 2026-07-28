package com.local.sgmhelper;

import java.util.List;

/**
 * 80 级：399 地图，从左向右。武当分上下两路，默认走“下路”（`enterBtnDown`）。
 * 保护目标“夏侯”只从攻击目标里排除，不读血条。
 */
final class Dungeon80Action extends BaseDungeonAction {
    private static final int PART0_END_X = 32;
    private static final int PART1_BEGIN_X = 33;
    private static final int PART1_END_X = 336;
    private static final int PART2_BEGIN_X = 337;
    private static final int PART2_END_X = 399;
    private static final List<String> DOWN_LANE = List.of(
            "孙干", "刘备军游击队长", "刘备军巡守队长");

    @Override int level() { return 80; }
    @Override String dungeonName() { return null; }
    @Override String entryNpcName() { return null; }
    @Override String exitNpcName() { return null; }
    @Override String campName() { return "80级副本驻地"; }
    @Override int campNpcX() { return 169; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {370, 27}; }
    @Override int dungeonMapMaxX() { return 399; }
    @Override String[] entryNpcButtons() { return new String[] {"随时可以出击", "进入"}; }
    @Override String[] exitNpcButtons() { return new String[] {"收下奖赏"}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x <= PART0_END_X) {
            // part0：先走到拾取点 (20,26)，到了以后按“下路”进下路。
            if (x >= 17 && x <= 23) {
                return DungeonBattleAutomation.RouteDecision.interactNpc("下路");
            }
            return DungeonBattleAutomation.RouteDecision.move(20, 26, 0);
        }
        if (x <= PART1_END_X) {
            // part1：下路推进，优先孙干，绝不攻击夏侯。
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    stepRight(x, PART1_BEGIN_X, PART1_END_X), 27, 0, DOWN_LANE, "夏侯");
        }
        if (x < PART2_END_X) {
            // part2：最终 BOSS 段，一般副本 82 级、精英 87 级。
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    stepRight(x, PART2_BEGIN_X, PART2_END_X), 27, 0,
                    DOWN_LANE, "夏侯", 82, 87);
        }
        return DungeonBattleAutomation.RouteDecision.exit(370, 27);
    }
}
