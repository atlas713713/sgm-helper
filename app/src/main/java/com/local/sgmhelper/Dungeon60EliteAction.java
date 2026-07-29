package com.local.sgmhelper;

/**
 * 60 级精英副本（武当 `dungeon_pro_list_cn` 的 id 2060）。副本内的房间传送路线和一般
 * 副本一模一样，只有入场信息不同。
 */
final class Dungeon60EliteAction extends Dungeon60Action {
    @Override int dungeonType() { return 2; }
    @Override String dungeonName() { return "精英界桥之战|界桥之战"; }
    @Override String campName() { return "精英盘河桥西"; }
    @Override int campNpcX() { return 26; }
    @Override int campNpcY() { return 20; }
}
