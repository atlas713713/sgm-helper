package com.local.sgmhelper;

/**
 * 75 级精英副本（武当 id 2075）。路线和 BOSS 优先顺序与一般副本相同；注意驻地名不是
 * 一般副本的“濮阳城外”，武当配置的是“精英濮阳城内”。
 */
final class Dungeon75EliteAction extends Dungeon75Action {
    @Override int dungeonType() { return 2; }
    @Override String dungeonName() { return "精英濮阳之战|濮阳之战"; }
    @Override String campName() { return "精英濮阳城内"; }
    @Override int campNpcX() { return 73; }
    @Override int campNpcY() { return 20; }
}
