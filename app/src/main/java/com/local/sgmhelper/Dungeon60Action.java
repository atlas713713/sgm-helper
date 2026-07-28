package com.local.sgmhelper;

/**
 * 60 级是多房间传送，走廊坐标（x≤98）会重复出现，所以下一个房间的门点必须靠
 * 武当的 partStep 状态决定，不能只看 x：清完第一间点一次快捷键后 partStep=1，
 * 清完第二间后 partStep=2。
 */
final class Dungeon60Action extends BaseDungeonAction {
    private int partStep;

    @Override int level() { return 60; }
    @Override String dungeonName() { return "界桥之战"; }
    @Override String entryNpcName() { return "说书人"; }
    @Override String exitNpcName() { return "公孙瓒"; }
    @Override String campName() { return "盘河桥西"; }
    @Override int campNpcX() { return 105; }
    @Override int entryNpcX() { return 102; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {486, 27}; }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x, y);
    }

    DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 25);
    }

    DungeonBattleAutomation.RouteDecision decideRoute(int x, int y) {
        if (x <= 98) {
            // 走廊：按 partStep 选下一间战斗房的门点。
            if (partStep == 0) {
                return DungeonBattleAutomation.RouteDecision.searchRoute(
                        new int[][] {{71, 10}, {71, 4}});
            }
            if (partStep == 1) {
                return DungeonBattleAutomation.RouteDecision.searchRoute(
                        new int[][] {{70, 40}, {70, 46}});
            }
            return DungeonBattleAutomation.RouteDecision.searchRoute(
                    new int[][] {{90, 27}, {94, 27}});
        }
        if (x >= 505 && x <= 528) {
            partStep = 1;
            return DungeonBattleAutomation.RouteDecision.shortcut(
                    1, "离开第一战斗房");
        }
        if (x >= 531 && x <= 553) {
            partStep = 2;
            return DungeonBattleAutomation.RouteDecision.shortcut(
                    1, "离开第二战斗房");
        }
        if (x >= 105 && x <= 198) {
            // 第一战斗房：清空后从 (114,40) → (114,46) 传送出去。
            return DungeonBattleAutomation.RouteDecision.searchRoute(
                    new int[][] {{114, 40}, {114, 46}});
        }
        if (x >= 205 && x <= 298) {
            // 第二战斗房：清空后从 (214,10) → (214,4) 传送出去。
            return DungeonBattleAutomation.RouteDecision.searchRoute(
                    new int[][] {{214, 10}, {214, 4}});
        }
        if (x >= 305 && x <= 398) {
            // 第三战斗房：清空后走中央传送点前的两个点。
            return DungeonBattleAutomation.RouteDecision.searchRoute(
                    new int[][] {{380, 27}, {394, 27}});
        }
        if (x >= 556 && x <= 581) {
            if (isNear(x, y, 570, 27)) {
                return DungeonBattleAutomation.RouteDecision.shortcut(
                        2, "通过中央传送点");
            }
            return DungeonBattleAutomation.RouteDecision.move(570, 27, 0);
        }
        if (x >= 405 && x <= 479) {
            return DungeonBattleAutomation.RouteDecision.search(486, 25, 0, true);
        }
        return DungeonBattleAutomation.RouteDecision.exit(486, 27);
    }
}
