package com.local.sgmhelper;

import java.util.List;

/**
 * 120 级：399 地图，从右向左，整条路线是固定路点。part0 的 330..360 段和 part1 的
 * 90..110 段，武当第一次进入时会先补一个上一段的路点（flag），之后才走下一个点。
 * 保护目标“黄忠/赵云”只从攻击目标里排除，不读血条。
 */
final class Dungeon120Action extends BaseDungeonAction {
    private static final List<String> ENEMY_NAMES = List.of(
            "曹操军先锋队长", "魏国军旗", "曹操军游击队长", "曹操军近卫队长");

    private boolean firstBand330 = true;
    private boolean firstBand90 = true;

    @Override int level() { return 120; }
    @Override String dungeonName() { return "汉中之战"; }
    @Override String entryNpcName() { return "黄权"; }
    @Override String exitNpcName() { return "黄权"; }
    @Override String campName() { return "汉中大营"; }
    @Override int campNpcX() { return 232; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {55, 23}; }
    @Override int dungeonMapMaxX() { return 399; }
    @Override String[] entryNpcButtons() { return new String[] {"随时可以出击", "进入"}; }
    @Override String[] exitNpcButtons() { return new String[] {"收下奖赏"}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x >= 200) {
            return part0(x);
        }
        if (x >= 30) {
            return part1(x);
        }
        return DungeonBattleAutomation.RouteDecision.exit(55, 23);
    }

    /** part0（399..200）：350 → 320 → 270 → 220 → 175。 */
    private DungeonBattleAutomation.RouteDecision part0(int x) {
        if (x >= 360) {
            return attack(350, 25);
        }
        if (x >= 330) {
            if (firstBand330) {
                firstBand330 = false;
                return attackRoute(new int[][] {{350, 25}, {320, 25}});
            }
            return attack(320, 25);
        }
        if (x >= 280) {
            return attack(270, 25);
        }
        if (x >= 230) {
            return attack(220, 25);
        }
        return attack(175, 25);
    }

    /** part1（199..30）：175 → 135 → 100 → 80 → 60，之后打最终 BOSS。 */
    private DungeonBattleAutomation.RouteDecision part1(int x) {
        if (x >= 185) {
            return attack(175, 25);
        }
        if (x >= 145) {
            return attack(135, 25);
        }
        if (x >= 110) {
            return attack(100, 25);
        }
        if (x >= 90) {
            if (firstBand90) {
                firstBand90 = false;
                return attackRoute(new int[][] {{100, 25}, {80, 25}});
            }
            return attack(80, 25);
        }
        if (x >= 70) {
            return attack(60, 25);
        }
        // 最后一段只打副本 BOSS，没有目标就去出口。
        return DungeonBattleAutomation.RouteDecision.searchThenExit(
                55, 23, ENEMY_NAMES, "黄忠");
    }

    private static DungeonBattleAutomation.RouteDecision attack(int x, int y) {
        return DungeonBattleAutomation.RouteDecision.searchNamed(
                x, y, 0, ENEMY_NAMES, "黄忠", 119, 124);
    }

    private static DungeonBattleAutomation.RouteDecision attackRoute(int[][] points) {
        return DungeonBattleAutomation.RouteDecision.searchRoute(points, 119, 124);
    }
}
