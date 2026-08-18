package com.local.sgmhelper;

import java.util.List;

class Dungeon65Action extends BaseDungeonAction {
    /** 武当 `priorityEnemyLevels`：一般副本 64 级，精英副本 68 级。 */
    static final int PRIORITY_ENEMY_LEVEL = 64;

    /** `DungeonAction` 给 1065 传的次选 `enemyLevels`；精英是 [1] 加一个名字，见子类。 */
    static final List<Integer> ENEMY_LEVELS = List.of(1, 63);

    @Override int level() { return 65; }
    @Override String dungeonName() { return "三英战吕布"; }
    @Override String entryNpcName() { return "说书人"; }
    @Override String exitNpcName() { return "刘备"; }
    @Override String campName() { return "虎牢关近郊"; }
    @Override int campNpcX() { return 121; }
    @Override int[] entryNpcRows() { return new int[] {3, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4}; }
    @Override int[] exitPoint() { return new int[] {560, 27}; }

    int priorityEnemyLevel() {
        return PRIORITY_ENEMY_LEVEL;
    }

    List<Integer> enemyLevels() {
        return ENEMY_LEVELS;
    }

    /** 武当只给精英副本配了这个次选名字。 */
    List<String> enemyNames() {
        return List.of();
    }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x, y, priorityEnemyLevel(), enemyLevels(), enemyNames());
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 25);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int y) {
        return decideRoute(x, y, PRIORITY_ENEMY_LEVEL, ENEMY_LEVELS, List.of());
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int y,
            int priorityLevel, List<Integer> levels, List<String> names) {
        if (x <= 58) {
            if (x >= 30 && x <= 40) {
                return DungeonBattleAutomation.RouteDecision.interactNpc("出击");
            }
            return DungeonBattleAutomation.RouteDecision.move(34, 22, 0);
        }
        if (x >= 79 && x <= 230) {
            return DungeonBattleAutomation.RouteDecision.searchByLevel(
                    245, 27, priorityLevel, levels, names);
        }
        if (x <= 250) {
            // 武当这一段照样先搜怪，再用 toRight 顶进下一段，不是纯移动。
            return DungeonBattleAutomation.RouteDecision.searchByLevel(
                    x + 30, 27, priorityLevel, levels, names);
        }
        if (x <= 349) {
            // 252..349 武当用 toRight 逐格扫；310..380 这段走廊的 y 是 37，不是 27。
            return DungeonBattleAutomation.RouteDecision.searchByLevel(
                    x + 30, x >= 310 ? 37 : 27, priorityLevel, levels, names);
        }
        if (x <= 437) {
            return DungeonBattleAutomation.RouteDecision.searchByLevelRoute(
                    new int[][] {{409, 30}, {410, 10}, {410, 4}}, priorityLevel, levels, names);
        }
        if (x <= 537) {
            // 武当这一段用的是 FindDungeonBossAction（红名副本 BOSS），不按等级找。
            int next = sweepRight(x, 537, 30);
            return next < 0
                    ? DungeonBattleAutomation.RouteDecision.searchBoss(538, 27, 0)
                    : DungeonBattleAutomation.RouteDecision.searchBoss(next, 27, 0);
        }
        return DungeonBattleAutomation.RouteDecision.exit(560, 27);
    }
}
