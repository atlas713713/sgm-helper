package com.local.sgmhelper;

import java.util.List;

/**
 * 75 级精英副本（武当 id 2075）。`Dungeon75Action` 一次都没读过副本类型，所以副本里的
 * 分段、路点、门点、BOSS 名单和出口坐标与一般副本完全相同；差别只有三处：
 * 驻地名是“精英濮阳城内”不是“濮阳城外”、宫殿入口在 (73,20) 而不是 (152,16)、
 * 以及 `DungeonAction` 传进来的次选等级表。
 */
final class Dungeon75EliteAction extends Dungeon75Action {
    /** `DungeonAction` 给 2075 传的 `enemyLevels`，和一般副本的 [1,74,75] 不是一张表。 */
    private static final List<Integer> ELITE_ENEMY_LEVELS = List.of(1, 75, 79, 80);

    @Override int dungeonType() { return 2; }
    @Override String dungeonName() { return "精英濮阳之战|濮阳之战"; }
    @Override String campName() { return "精英濮阳城内"; }
    @Override int campNpcX() { return 73; }
    @Override int campNpcY() { return 20; }

    @Override
    List<Integer> enemyLevels() {
        return ELITE_ENEMY_LEVELS;
    }
}
