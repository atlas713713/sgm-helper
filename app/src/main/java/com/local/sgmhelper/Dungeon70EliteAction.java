package com.local.sgmhelper;

/**
 * 70 级精英副本（武当 id 2070）。武当的 `Dungeon70Action2` 不看副本类型，护送路线、
 * 保护目标“马车”和优先目标都和一般副本相同。
 */
final class Dungeon70EliteAction extends Dungeon70Action {
    @Override int dungeonType() { return 2; }
    @Override String dungeonName() { return "精英二狼劫献帝|二狼劫献帝"; }
    @Override String campName() { return "精英安邑郊外"; }
    @Override int campNpcX() { return 57; }
    @Override int campNpcY() { return 20; }
}
