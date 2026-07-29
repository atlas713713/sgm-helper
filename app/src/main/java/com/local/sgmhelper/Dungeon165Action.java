package com.local.sgmhelper;

import java.util.List;

/**
 * 165 级：399 地图，守中央的“北平雕像”。武当在中央点 (210,29) 附近清波次，
 * 左右备用点是 (195,27) 和 (222,27)，最后打大 BOSS“东明圣王”。
 * 保护目标“北平雕像”只从攻击目标里排除，不读血条。
 */
final class Dungeon165Action extends BaseDungeonAction {
    private static final int CENTER_X = 210;
    private static final int CENTER_Y = 29;
    private static final int LEFT_X = 195;
    private static final int RIGHT_X = 222;
    private static final List<String> PRIORITY = List.of(
            "山上王", "故国川王", "新大王", "次大王", "太祖王",
            "慕本王", "闵中王", "大武神王", "琉璃明王", "东明圣王", "高句丽兵营");

    @Override int level() { return 165; }
    @Override String dungeonName() { return "高句丽入侵"; }
    @Override String entryNpcName() { return "司马懿"; }
    @Override String exitNpcName() { return "司马懿"; }
    @Override String campName() { return "北平城门"; }
    @Override int campNpcX() { return 279; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {210, 36}; }
    @Override int dungeonMapMaxX() { return 399; }
    @Override String[] entryNpcButtons() { return new String[] {"随时可以出击", "进入"}; }
    @Override String[] exitNpcButtons() { return new String[] {"收下奖赏"}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        if (x < LEFT_X) {
            // 偏左时先回左备用点，再回中央。
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    LEFT_X, 27, 0, PRIORITY, "北平雕像");
        }
        if (x > RIGHT_X) {
            return DungeonBattleAutomation.RouteDecision.searchNamed(
                    RIGHT_X, 27, 0, PRIORITY, "北平雕像");
        }
        // 守中央：清完所有波次和大 BOSS 后才离场。
        return DungeonBattleAutomation.RouteDecision.searchThenExit(
                CENTER_X, CENTER_Y, PRIORITY, "北平雕像");
    }
}
