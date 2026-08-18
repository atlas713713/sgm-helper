package com.local.sgmhelper;

import java.util.List;

class Dungeon75Action extends BaseDungeonAction {
    /**
     * 武当 `FindDungeonBossAction(priorityEnemyNames)` 的顺序，只在 part1（x≥79）生效；
     * part0 用的是 `FindEnemyAction(enemyLevels)`，这几个 BOSS 本来也不在开场那一段。
     */
    private static final List<String> PRIORITY = List.of("吕虔", "于禁", "李典", "夏侯渊");

    /** `DungeonAction` 给 1075 传的 `enemyLevels`。精英是另一张表，见子类。 */
    static final List<Integer> ENEMY_LEVELS = List.of(1, 74, 75);

    @Override int level() { return 75; }
    @Override String dungeonName() { return "濮阳之战"; }
    @Override String entryNpcName() { return "吕布"; }
    @Override String exitNpcName() { return "陈宫"; }
    @Override String campName() { return "濮阳城外"; }
    @Override int campNpcX() { return 152; }
    @Override int[] entryNpcRows() { return new int[] {4, 3, 3}; }
    @Override int[] exitNpcRows() { return new int[] {4, 4, 4}; }
    @Override int[] exitPoint() { return new int[] {187, 27}; }
    @Override int dungeonMapMaxX() { return 199; }

    List<Integer> enemyLevels() {
        return ENEMY_LEVELS;
    }

    @Override
    DungeonBattleAutomation.RouteDecision decide(int x, int y) {
        return decideRoute(x, y, enemyLevels());
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x) {
        return decideRoute(x, 27);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(int x, int y) {
        return decideRoute(x, y, ENEMY_LEVELS);
    }

    static DungeonBattleAutomation.RouteDecision decideRoute(
            int x, int y, List<Integer> levels) {
        if (x <= 78) {
            // part0：武当只按等级打杂兵，不认 BOSS 名单。落在 61..78 的门区才走 (73,27)，
            // 其余用 toRight 逐格往前顶。
            if (x >= 61 && (x < 67 || y < 20 || y > 35)) {
                return byLevel(73, 27, levels);
            }
            return byLevel(x + 30, 27, levels);
        }
        // part1：先按名字找 BOSS，找不到退到等级表；路线是 (130,25) → (170,25) → 离场。
        if (x < 120) {
            return byName(130, 25, levels);
        }
        if (x < 160) {
            return byName(170, 25, levels);
        }
        return DungeonBattleAutomation.RouteDecision.exit(187, 27);
    }

    private static DungeonBattleAutomation.RouteDecision byLevel(
            int x, int y, List<Integer> levels) {
        return DungeonBattleAutomation.RouteDecision.searchTargetBoss(
                x, y, 0, List.of(), levels.toArray(new Integer[0]));
    }

    private static DungeonBattleAutomation.RouteDecision byName(
            int x, int y, List<Integer> levels) {
        return DungeonBattleAutomation.RouteDecision.searchTargetBoss(
                x, y, 0, PRIORITY, levels.toArray(new Integer[0]));
    }
}
