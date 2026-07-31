package com.local.sgmhelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 武当 {@code TeleportUtil} / {@code LocationUtil} 的城市传送数据。
 *
 * <p>武当按两个开关挑坐标：{@code Config.u()} 看服务器类型，{@code u1.a()} 看是否高分辨率布局。
 * 我们的游戏包名是 {@code hk.phx.khm.cs}，武当把它归到 {@code gameServerType = 2}，于是
 * {@code Config.u()} 为真、{@code u1.a()} 为假——和我们其他界面坐标（NPC 快捷键 1208、对话行
 * 左边界 133）是同一套，所以这里只收录这一套。
 */
final class CityTeleportCatalog {
    /** 传送面板里的一个城市：先按 {@code page} 拖动地图，再点 {@code (x, y)}。 */
    static final class Entry {
        /** 武当的 {@code toTeleportType}；0 表示不用拖动。 */
        final int page;
        final int x;
        final int y;

        Entry(int page, int x, int y) {
            this.page = page;
            this.x = x;
            this.y = y;
        }
    }

    /** 不能直接传送的城市：先传到中转城，再找城里的传送官跳过去。 */
    static final class OfficerRoute {
        /** 可以用来找传送官的城市；第一个是没就位时要传送过去的那个。 */
        final List<String> stagingCities;
        /** 传送官在中转城城内地图上的坐标（地图宽 399）。 */
        final int officerX;
        final int officerY;
        /** 传送官只送到这里；为 null 表示直达目标城。 */
        final String arrivesAt;
        /** 传送落地后还要跑的地图链；非空表示我们还走不了这条路线。 */
        final List<String> runRoute;

        OfficerRoute(List<String> stagingCities, int officerX, int officerY,
                String arrivesAt, List<String> runRoute) {
            this.stagingCities = stagingCities;
            this.officerX = officerX;
            this.officerY = officerY;
            this.arrivesAt = arrivesAt;
            this.runRoute = runRoute;
        }

        /** 传送官这一跳的落点：{@link #arrivesAt} 为空时就是目标城本身。 */
        String landsIn(String targetCity) {
            return arrivesAt == null ? targetCity : arrivesAt;
        }
    }

    /** 城内地图的宽度，和副本宫殿一样是 399。 */
    static final int CITY_MAP_MAX_X = 399;
    /** 客栈/商店的入口 y；武当 {@code LocationUtil.J} 固定用 3。 */
    static final int ENTRANCE_Y = 3;
    /** 没进到面板时先退到这个 y 再重新走过去。 */
    static final int RETRY_Y = 17;
    /** 走位判定容差，同武当 {@code LocationUtil.O/Q} 的默认值。 */
    static final int ARRIVE_TOLERANCE = 5;
    /** 商店面板标题框（1280×720）。 */
    static final int PANEL_TITLE_LEFT = 580;
    static final int PANEL_TITLE_TOP = 27;
    static final int PANEL_TITLE_RIGHT = 700;
    static final int PANEL_TITLE_BOTTOM = 65;
    /** 商店里的“传送”页签；客栈默认就是传送页，不用点。 */
    static final int STORE_TELEPORT_TAB_X = 441;
    static final int STORE_TELEPORT_TAB_Y = 106;
    /** 传送面板右下角的“传送”按钮。 */
    static final int TELEPORT_BUTTON_X = 1048;
    static final int TELEPORT_BUTTON_Y = 630;
    /** 传送确认框的“确定”按钮。 */
    static final int CONFIRM_LEFT = 507;
    static final int CONFIRM_TOP = 377;
    static final int CONFIRM_RIGHT = 559;
    static final int CONFIRM_BOTTOM = 402;
    static final int CONFIRM_X = 533;
    static final int CONFIRM_Y = 389;
    /** 落地后可能弹出的提示框的“关闭”。 */
    static final int TIP_CLOSE_LEFT = 725;
    static final int TIP_CLOSE_TOP = 427;
    static final int TIP_CLOSE_RIGHT = 772;
    static final int TIP_CLOSE_BOTTOM = 452;
    static final int TIP_CLOSE_X = 748;
    static final int TIP_CLOSE_Y = 439;
    /** 汉中要先落脚成都，武当 {@code TeleportCityAction.tmpCity} 就写死了成都。 */
    static final String HANZHONG = "汉中";
    static final String HANZHONG_VIA = "成都";

    /** 三大都城的客栈/商店比其他城市靠右一点。 */
    private static final List<String> CAPITALS = Arrays.asList("洛阳", "成都", "建业");

    private static final Map<String, Entry> TELEPORT = new LinkedHashMap<>();
    private static final Map<String, OfficerRoute> OFFICER_ROUTES = new LinkedHashMap<>();

    static {
        TELEPORT.put("宛", new Entry(7, 1075, 587));
        TELEPORT.put("邺", new Entry(7, 1128, 378));
        TELEPORT.put("上庸", new Entry(8, 741, 188));
        TELEPORT.put("会稽", new Entry(6, 990, 192));
        TELEPORT.put("凉州", new Entry(7, 608, 368));
        TELEPORT.put("北平", new Entry(5, 727, 220));
        TELEPORT.put("北海", new Entry(5, 860, 366));
        TELEPORT.put("卢江", new Entry(6, 782, 147));
        TELEPORT.put("大和", new Entry(0, 830, 384));
        TELEPORT.put("夷州", new Entry(6, 1100, 430));
        TELEPORT.put("天水", new Entry(7, 636, 490));
        TELEPORT.put("安定", new Entry(7, 854, 430));
        TELEPORT.put("寿春", new Entry(6, 640, 147));
        TELEPORT.put("平原", new Entry(5, 666, 360));
        TELEPORT.put("建业", new Entry(5, 919, 580));
        TELEPORT.put("建宁", new Entry(8, 582, 360));
        TELEPORT.put("徐州", new Entry(5, 807, 507));
        TELEPORT.put("成都", new Entry(8, 573, 197));
        TELEPORT.put("新野", new Entry(7, 921, 587));
        TELEPORT.put("晋阳", new Entry(7, 1138, 268));
        TELEPORT.put("柴桑", new Entry(6, 635, 245));
        TELEPORT.put("桂阳", new Entry(6, 724, 360));
        TELEPORT.put(HANZHONG, new Entry(0, 614, 283));
        TELEPORT.put("汝南", new Entry(5, 579, 587));
        TELEPORT.put("江夏", new Entry(8, 1111, 216));
        TELEPORT.put("江州", new Entry(8, 850, 316));
        TELEPORT.put("武陵", new Entry(8, 988, 450));
        TELEPORT.put("洛阳", new Entry(7, 1052, 505));
        TELEPORT.put("襄阳", new Entry(8, 972, 241));
        TELEPORT.put("许昌", new Entry(5, 599, 507));
        TELEPORT.put("豫章", new Entry(6, 826, 248));
        TELEPORT.put("长安", new Entry(7, 866, 526));
        TELEPORT.put("长沙", new Entry(8, 1111, 308));
        TELEPORT.put("陈留", new Entry(5, 653, 441));
        TELEPORT.put("零陵", new Entry(8, 1164, 424));
        TELEPORT.put("邪马台", new Entry(0, 614, 466));

        // 武当 LegionTaskAction.toTaskCity 的特例分支：这些城市不在传送表里，要找传送官。
        OFFICER_ROUTES.put("建安", new OfficerRoute(
                Collections.singletonList("会稽"), 364, 18, null, Collections.emptyList()));
        OFFICER_ROUTES.put("九原", new OfficerRoute(
                Arrays.asList("凉州", "晋阳"), 360, 20, null, Collections.emptyList()));
        OFFICER_ROUTES.put("大漠", new OfficerRoute(
                Collections.singletonList("凉州"), 370, 12, null, Collections.emptyList()));
        OFFICER_ROUTES.put("白沙瓦", new OfficerRoute(
                Collections.singletonList("凉州"), 376, 12, null, Collections.emptyList()));
        // 合浦的传送官只送到云南，之后还要跑 云南西境 → 交趾 → 合浦，我们还没有跑地图链的能力。
        OFFICER_ROUTES.put("合浦", new OfficerRoute(
                Collections.singletonList("武陵"), 116, 12, "云南",
                Arrays.asList("云南西境", "交趾", "合浦")));
    }

    private CityTeleportCatalog() {
    }

    static Entry teleportEntry(String city) {
        return city == null ? null : TELEPORT.get(city);
    }

    static OfficerRoute officerRoute(String city) {
        return city == null ? null : OFFICER_ROUTES.get(city);
    }

    /** 菜单里可以选的城市：能直接传的 + 能靠传送官到的。 */
    static List<String> travelCities() {
        List<String> cities = new ArrayList<>(TELEPORT.keySet());
        for (Map.Entry<String, OfficerRoute> route : OFFICER_ROUTES.entrySet()) {
            if (route.getValue().runRoute.isEmpty()) {
                cities.add(route.getKey());
            }
        }
        Collections.sort(cities);
        return cities;
    }

    /** 我们能不能去这个城市；跑地图链的那几个还不行。 */
    static boolean canTravelTo(String city) {
        OfficerRoute route = officerRoute(city);
        if (route != null) {
            return route.runRoute.isEmpty();
        }
        return teleportEntry(city) != null;
    }

    /** 认识这个名字就说明它是城市，不是荒野或副本地图。 */
    static boolean isKnownCity(String city) {
        return teleportEntry(city) != null || officerRoute(city) != null;
    }

    /** 武当 {@code LocationUtil.J}：商店在城东，客栈在城西，都城比别处偏右。 */
    static int entranceX(String city, boolean store) {
        if (store) {
            return CAPITALS.contains(city) ? 361 : 354;
        }
        return CAPITALS.contains(city) ? 137 : 107;
    }

    /** 武当 {@code TeleportCityAction.findInnOrStore}：人在城东就走商店，城西就走客栈。 */
    static boolean useStore(int currentX) {
        return currentX > 250;
    }

    /**
     * 翻页手势，返回 {@code {startX, startY, endX, endY}}；page 为 0 或未知时返回 null。
     * 对应武当 {@code TeleportUtil.c()..j()}，每次调用要连划两下。
     */
    static int[] panGesture(int page) {
        switch (page) {
            case 1:
                return new int[] {800, 200, 800, 530};
            case 2:
                return new int[] {800, 530, 800, 200};
            case 3:
                return new int[] {480, 360, 1100, 360};
            case 4:
                return new int[] {1100, 360, 480, 360};
            case 5:
                return new int[] {1100, 200, 480, 530};
            case 6:
                return new int[] {1100, 530, 480, 200};
            case 7:
                return new int[] {480, 200, 1100, 530};
            case 8:
                return new int[] {480, 530, 1100, 200};
            default:
                return null;
        }
    }

    /** 在一段文字里找城市名，用来从军务任务详情里读出任务城市。 */
    static String findCity(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String normalized = text.replaceAll("\\s", "");
        String found = null;
        int foundAt = Integer.MAX_VALUE;
        for (String city : OFFICER_ROUTES.keySet()) {
            int at = normalized.indexOf(city);
            if (at >= 0 && at < foundAt) {
                found = city;
                foundAt = at;
            }
        }
        for (String city : TELEPORT.keySet()) {
            int at = normalized.indexOf(city);
            // 同一位置上更长的名字优先，"建安" 不该被 "建业" 之类的短名抢走。
            if (at >= 0 && (at < foundAt || (at == foundAt && city.length() > found.length()))) {
                found = city;
                foundAt = at;
            }
        }
        return found;
    }
}
