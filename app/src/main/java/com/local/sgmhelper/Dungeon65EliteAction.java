package com.local.sgmhelper;

/**
 * 65 级精英副本（武当 id 2065）。路线和一般副本相同，但武当把 `priorityEnemyLevels`
 * 从 64 换成 68，所以每个战斗分段优先打 68 级目标。
 */
final class Dungeon65EliteAction extends Dungeon65Action {
    @Override int dungeonType() { return 2; }
    @Override String dungeonName() { return "精英三英战吕布|三英战吕布"; }
    @Override String campName() { return "精英虎牢关近郊"; }
    @Override int campNpcX() { return 41; }
    @Override int campNpcY() { return 20; }
    @Override int priorityEnemyLevel() { return 68; }
}
