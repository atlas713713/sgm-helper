package com.local.sgmhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public final class CityTeleportCatalogTest {
    @Test
    public void keepsTheWudangTeleportPointsForTheServerWeRunOn() {
        // 武当 TeleportUtil.b()：Config.u() 为真、u1.a() 为假的那一套（hk.phx.khm.cs）。
        assertPoint("会稽", 6, 990, 192);
        assertPoint("凉州", 7, 608, 368);
        assertPoint("晋阳", 7, 1138, 268);
        assertPoint("武陵", 8, 988, 450);
        assertPoint("洛阳", 7, 1052, 505);
        assertPoint("成都", 8, 573, 197);
        assertPoint("许昌", 5, 599, 507);
        assertPoint("零陵", 8, 1164, 424);
        assertPoint("汉中", 0, 614, 283);
    }

    @Test
    public void everyOfficerRouteStartsFromACityWeCanTeleportTo() {
        for (String city : new String[] {"建安", "九原", "大漠", "白沙瓦", "合浦"}) {
            CityTeleportCatalog.OfficerRoute route = CityTeleportCatalog.officerRoute(city);
            assertNotNull(city + " 应该有传送官路线", route);
            assertFalse(city + " 的中转城不能为空", route.stagingCities.isEmpty());
            for (String staging : route.stagingCities) {
                assertNotNull(city + " 的中转城 " + staging + " 必须能直接传送",
                        CityTeleportCatalog.teleportEntry(staging));
            }
        }
    }

    @Test
    public void keepsTheWudangOfficerCoordinates() {
        CityTeleportCatalog.OfficerRoute jianan = CityTeleportCatalog.officerRoute("建安");
        assertEquals(List.of("会稽"), jianan.stagingCities);
        assertEquals(364, jianan.officerX);
        assertEquals(18, jianan.officerY);
        assertEquals("建安", jianan.landsIn("建安"));

        // 武当在九原分支里认凉州和晋阳两个起点，只有都不在时才传去凉州。
        CityTeleportCatalog.OfficerRoute jiuyuan = CityTeleportCatalog.officerRoute("九原");
        assertEquals(List.of("凉州", "晋阳"), jiuyuan.stagingCities);
        assertEquals(360, jiuyuan.officerX);
        assertEquals(20, jiuyuan.officerY);
    }

    @Test
    public void refusesTheRoutesThatStillNeedToWalkThroughMaps() {
        assertTrue(CityTeleportCatalog.canTravelTo("建安"));
        assertTrue(CityTeleportCatalog.canTravelTo("九原"));
        assertTrue(CityTeleportCatalog.canTravelTo("洛阳"));
        // 合浦的传送官只到云南，之后还要跑 云南西境 → 交趾 → 合浦。
        assertFalse(CityTeleportCatalog.canTravelTo("合浦"));
        assertEquals("云南", CityTeleportCatalog.officerRoute("合浦").landsIn("合浦"));
        assertFalse(CityTeleportCatalog.canTravelTo("渔阳"));
        assertFalse(CityTeleportCatalog.canTravelTo(null));
        assertFalse(CityTeleportCatalog.travelCities().contains("合浦"));
        assertTrue(CityTeleportCatalog.travelCities().contains("九原"));
    }

    @Test
    public void putsTheInnAndStoreWhereWudangPutsThem() {
        // 武当 LocationUtil.J：三大都城的客栈/商店比别的城偏右。
        assertEquals(354, CityTeleportCatalog.entranceX("会稽", true));
        assertEquals(107, CityTeleportCatalog.entranceX("会稽", false));
        assertEquals(361, CityTeleportCatalog.entranceX("洛阳", true));
        assertEquals(137, CityTeleportCatalog.entranceX("成都", false));
        assertEquals(361, CityTeleportCatalog.entranceX("建业", true));

        assertTrue(CityTeleportCatalog.useStore(251));
        assertFalse(CityTeleportCatalog.useStore(250));
    }

    @Test
    public void hasAPanGestureForEveryPageThatCitiesUse() {
        for (int page = 1; page <= 8; page++) {
            assertNotNull("page " + page + " 缺少翻页手势",
                    CityTeleportCatalog.panGesture(page));
            assertEquals(4, CityTeleportCatalog.panGesture(page).length);
        }
        assertNull(CityTeleportCatalog.panGesture(0));
        assertNull(CityTeleportCatalog.panGesture(9));
    }

    @Test
    public void readsTheTaskCityOutOfTheQuestDetail() {
        assertEquals("建安", CityTeleportCatalog.findCity("勇讨军团天将 任务地点：建安 次数：1/1"));
        assertEquals("九原", CityTeleportCatalog.findCity("任务地点:九原"));
        // 建业和建安只差一个字，别读串了。
        assertEquals("建业", CityTeleportCatalog.findCity("任务地点：建业"));
        assertNull(CityTeleportCatalog.findCity("补充军团物资 冷却中 30分钟"));
        assertNull(CityTeleportCatalog.findCity(null));
    }

    @Test
    public void tellsTheInnPanelApartFromTheStorePanel() {
        assertTrue(CityTravelAutomation.matchesPanelTitle("商 店", true));
        assertFalse(CityTravelAutomation.matchesPanelTitle("客栈", true));
        assertTrue(CityTravelAutomation.matchesPanelTitle("客栈", false));
        assertFalse(CityTravelAutomation.matchesPanelTitle("", false));
        assertEquals("会稽", CityTravelAutomation.normalize(" 会 稽 "));
    }

    private static void assertPoint(String city, int page, int x, int y) {
        CityTeleportCatalog.Entry entry = CityTeleportCatalog.teleportEntry(city);
        assertNotNull(city, entry);
        assertEquals(city + " page", page, entry.page);
        assertEquals(city + " x", x, entry.x);
        assertEquals(city + " y", y, entry.y);
    }
}
