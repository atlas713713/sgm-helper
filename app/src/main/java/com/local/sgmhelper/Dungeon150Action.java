package com.local.sgmhelper;

/** 150 级：199 地图，从左向右，中央集合点 (95,25)。 */
final class Dungeon150Action extends BaseDungeonAction {
    private static final int PART0_BEGIN_X = 1;
    private static final int PART0_END_X = 50;
    private static final int PART1_BEGIN_X = 51;
    private static final int PART1_END_X = 190;

    @Override int level() { return 150; }
    @Override String dungeonName() { return "罗马竞技场"; }
    @Override String entryNpcName() { return "克利安德"; }
    @Override String exitNpcName() { return "克利安德"; }
    @Override String campName() { return "场外广场"; }
    @Override int campNpcX() { return 263; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {172, 25}; }
    @Override int dungeonMapMaxX() { return 199; }
    @Override String[] entryNpcButtons() { return new String[] {"随时可以出击", "进入"}; }
    @Override String[] exitNpcButtons() { return new String[] {"收下奖赏"}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x <= PART0_END_X) {
            // part0：先打本段 BOSS，再往中央集合点推进。
            return DungeonBattleAutomation.RouteDecision.search(
                    stepRight(x, PART0_BEGIN_X, PART0_END_X), 27, 0, true);
        }
        if (x < PART1_END_X) {
            return DungeonBattleAutomation.RouteDecision.search(
                    stepRight(x, PART1_BEGIN_X, PART1_END_X), 27, 0, true);
        }
        return DungeonBattleAutomation.RouteDecision.exit(172, 25);
    }
}
