package com.local.sgmhelper;

import java.util.List;

/**
 * 65 级精英副本（武当 id 2065）。路线和一般副本相同，但武当把 `priorityEnemyLevels`
 * 从 64 换成 68；`DungeonAction` 传的次选目标也不同：一般是 `enemyLevels=[1,63]`，
 * 精英是 `enemyLevels=[1]` 加 `enemyNames=["虎牢关副将"]`。
 */
final class Dungeon65EliteAction extends Dungeon65Action {
    @Override int dungeonType() { return 2; }
    @Override String dungeonName() { return "精英三英战吕布|三英战吕布"; }
    @Override String campName() { return "精英虎牢关近郊"; }
    @Override int campNpcX() { return 41; }
    @Override int campNpcY() { return 20; }
    @Override int priorityEnemyLevel() { return 68; }
    @Override List<Integer> enemyLevels() { return List.of(1); }
    @Override List<String> enemyNames() { return List.of("虎牢关副将"); }
}
