package com.local.sgmhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public final class WildernessCatalogTest {
    @Test
    public void mapsEveryLegionTaskTierTheWayWudangDoes() {
        // 武当 w0.c()：阶 → 区段 + 目标怪等级。
        assertTier("一", 1, 3, 30);
        assertTier("二", 1, 3, 40);
        assertTier("三", 1, 3, 50);
        assertTier("四", 1, 3, 60);
        assertTier("五", 1, 3, 70);
        assertTier("六", 1, 3, 75);
        assertTier("七", 4, 6, 80);
        assertTier("八", 4, 6, 100);
        assertTier("九", 7, 9, 115);
        assertTier("十", 7, 9, 130);
        assertTier("十一", 10, 12, 145);
        assertTier("十二", 10, 12, 160);
        assertTier("十三", 13, 15, 175);
        assertTier("十四", 13, 15, 190);
        assertTier("十五", 16, 18, 205);
        assertTier("十六", 16, 18, 220);
        assertTier("十七", 19, 21, 235);
        assertTier("十八", 19, 21, 250);
        assertTier("十九", 22, 24, 265);
        assertNull(WildernessCatalog.tier("二十"));
        assertNull(WildernessCatalog.tier(null));
    }

    @Test
    public void legionTaskTenGoesToTheStoneLionInZonesSevenToNine() {
        // 用户举的例子：军团任务十 → 7–9 区、130 级石狮精。
        WildernessCatalog.Tier tier = WildernessCatalog.tier("十");
        assertEquals(7, tier.zoneLow);
        assertEquals(9, tier.zoneHigh);
        for (int zone = 7; zone <= 9; zone++) {
            WildernessCatalog.Enemy enemy = WildernessCatalog.enemyForTier("十", zone);
            assertNotNull("zone " + zone, enemy);
            assertEquals("石狮精", enemy.name);
            assertEquals(130, enemy.level);
        }
        // 区段外的区里没有这一级怪，武当也不会去那儿。
        assertNull(WildernessCatalog.enemyForTier("十", 6));
    }

    @Test
    public void acceptsArabicAndChineseTierNumbers() {
        assertEquals(Integer.valueOf(10), WildernessCatalog.parseTier("十"));
        assertEquals(Integer.valueOf(10), WildernessCatalog.parseTier("10"));
        assertEquals(Integer.valueOf(19), WildernessCatalog.parseTier("十九"));
        assertEquals(Integer.valueOf(1), WildernessCatalog.parseTier("一"));
        assertEquals(Integer.valueOf(7), WildernessCatalog.parseTier(" 七阶 "));
        assertNull(WildernessCatalog.parseTier("二十"));
        assertNull(WildernessCatalog.parseTier("百"));
        assertNull(WildernessCatalog.parseTier(""));
    }

    @Test
    public void keepsTheConfiguredZoneWhenItAlreadyFitsTheTier() {
        assertEquals(8, WildernessCatalog.zoneForTier("十", 8));
        int picked = WildernessCatalog.zoneForTier("十", 1);
        assertTrue("随机出来的区要落在 7–9：" + picked, picked >= 7 && picked <= 9);
        assertEquals(0, WildernessCatalog.zoneForTier("二十", 1));
    }

    @Test
    public void portsEveryZoneFromTheWudangWildTable() {
        assertEnemies(1, "30 蛇魔女", "40 火妖", "50 土霸王",
                "60 食人花", "70 金甲龙", "75 圣武士");
        assertEnemies(3, "30 蛇魔女", "40 火妖", "50 土霸王",
                "60 食人花", "70 金甲龙", "75 圣武士");
        assertEnemies(4, "80 魔斗士", "90 海妖", "100 螳蝎妖");
        assertEnemies(7, "115 九尾狐", "130 石狮精");
        assertEnemies(10, "145 人面鸟", "160 战鬼");
        assertEnemies(13, "175 式神童", "190 剑齿虎");
        assertEnemies(16, "205 犬神", "220 木人兵");
        assertEnemies(19, "235 曼陀猪", "250 黑暗鬼");
        assertEnemies(24, "265 史前猿", "280 死魂刀兵");
        assertTrue(WildernessCatalog.enemies(0).isEmpty());
        assertTrue(WildernessCatalog.enemies(25).isEmpty());
    }

    @Test
    public void keepsTheWudangLocationRanges() {
        assertRange("蛇魔女", 30, 90);
        assertRange("食人花", 370, 400);
        assertRange("圣武士", 520, 570);
        assertRange("魔斗士", 30, 165);
        assertRange("石狮精", 370, 570);
        assertRange("死魂刀兵", 370, 570);
        // 海妖是唯一有两段刷新区的怪。
        WildernessCatalog.Enemy seaDemon = WildernessCatalog.enemyByName("海妖");
        assertEquals(2, seaDemon.xRanges.length);
        assertTrue(seaDemon.containsX(210));
        assertTrue(seaDemon.containsX(400));
        assertTrue(!seaDemon.containsX(300));
        for (int i = 0; i < 50; i++) {
            assertTrue(seaDemon.containsX(seaDemon.randomX()));
        }
    }

    @Test
    public void stillUnderstandsTheOldMisspelledMonsterNames() {
        // 旧设置里存的是这两个错名，升级后不能直接失败。
        assertEquals("螳蝎妖", WildernessCatalog.enemyByName("螳螂巨妖").name);
        assertEquals("式神童", WildernessCatalog.enemyByName("式神童子").name);
        assertNull(WildernessCatalog.enemyByName("不存在的怪"));
    }

    @Test
    public void offersEveryZoneToTheTrainingMenu() {
        for (int zone = 1; zone <= WildernessCatalog.MAX_ZONE; zone++) {
            List<String> monsters = TrainingAutomation.monstersForZone(zone);
            assertTrue("zone " + zone, !monsters.isEmpty());
            assertNotNull(WildernessCatalog.enemyByName(
                    TrainingAutomation.monsterName(monsters.get(0))));
        }
    }

    @Test
    public void keepsTheMilitaryZoneHelpersInSyncWithTheCatalog() {
        assertEquals(7, TaskAutomation.militaryWildernessZoneRange("十")[0]);
        assertEquals(9, TaskAutomation.militaryWildernessZoneRange("十")[1]);
        assertEquals(8, TaskAutomation.militaryWildernessZone("十", 8));
        assertNull(TaskAutomation.militaryWildernessZoneRange("二十"));
        assertEquals(0, TaskAutomation.militaryWildernessZone("二十", 1));
    }

    private static void assertTier(String tier, int low, int high, int level) {
        WildernessCatalog.Tier value = WildernessCatalog.tier(tier);
        assertNotNull(tier, value);
        assertEquals(tier + " 区段下限", low, value.zoneLow);
        assertEquals(tier + " 区段上限", high, value.zoneHigh);
        assertEquals(tier + " 目标怪等级", level, value.enemyLevel);
        // 区段里每个区都必须真的有这一级怪。
        for (int zone = low; zone <= high; zone++) {
            assertNotNull(tier + " 在 " + zone + " 区没有 " + level + " 级怪",
                    WildernessCatalog.enemy(zone, level));
        }
    }

    private static void assertEnemies(int zone, String... expected) {
        assertEquals(List.of(expected), TrainingAutomation.monstersForZone(zone));
    }

    private static void assertRange(String monster, int start, int end) {
        WildernessCatalog.Enemy enemy = WildernessCatalog.enemyByName(monster);
        assertNotNull(monster, enemy);
        assertEquals(monster + " 起点", start, enemy.xRanges[0][0]);
        assertEquals(monster + " 终点", end,
                enemy.xRanges[enemy.xRanges.length - 1][1]);
    }
}
