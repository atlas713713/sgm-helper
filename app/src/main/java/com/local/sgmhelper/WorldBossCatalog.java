package com.local.sgmhelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class WorldBossCatalog {
    private static final int DEFAULT_LEFT = 260;
    private static final int DEFAULT_RIGHT = 340;

    static final List<MapEntry> MAPS = Collections.unmodifiableList(Arrays.asList(
            map("房陵", boss(30, "孟达", 500, 590)),
            map("大兴山", boss(30, "程远志")),
            map("天柱山", boss(30, "陈兰")),
            map("地下洞窟二层", boss(40, "扒手")),
            map("碧水地穴二层", boss(40, "史前冰蚕")),
            map("先秦地监二层", boss(40, "夜叉酋长")),
            map("潼关", boss(50, "张让")),
            map("葫芦谷口", boss(50, "刘表")),
            map("白马", boss(50, "公孙瓒")),
            map("乱军山寨", boss(55, "开山将军")),
            map("铁门峡二层", boss(58, "流焰将军")),
            map("乱军山寨三层", boss(60, "雷霆将军")),
            map("鄱阳湖", boss(65, "袁术")),
            map("官渡", boss(68, "袁绍")),
            map("葭萌古道三层", boss(70, "董卓")),
            map("青城山", boss(72, "徐庶")),
            map("壶关", boss(72, "典韦")),
            map("濮阳", boss(72, "郭嘉")),
            map("麦城", boss(72, "太史慈")),
            map("箕谷", boss(73, "马腾")),
            map("下邳", boss(73, "貂蝉")),
            map("兖州", boss(74, "张辽")),
            map("峨嵋山云海",
                    boss(75, "玄武", 400, 500), boss(75, "白虎", 400, 500),
                    boss(95, "青龙", 100, 200), boss(95, "朱雀", 100, 200)),
            map("定军山", boss(81, "庞统"), boss(84, "黄忠")),
            map("乌巢", boss(81, "荀彧"), boss(84, "张合")),
            map("卢陵", boss(81, "小乔"), boss(84, "大乔")),
            map("地下洞窟四层", boss(87, "古代捕熊蟒")),
            map("碧水地穴四层", boss(87, "古代碧水巨蚕")),
            map("先秦地监四层", boss(87, "古代地狱犬")),
            map("朝阳洞二层", boss(87, "古代吸血巨蛾")),
            map("石亭", boss(94, "甘宁"), boss(99, "周瑜")),
            map("街亭", boss(94, "姜维"), boss(99, "马超")),
            map("子午谷", boss(94, "程昱"), boss(99, "许褚")),
            map("云中", boss(109, "呼厨泉"), boss(114, "蹋顿")),
            map("沙口", boss(109, "祝融夫人"), boss(114, "孟获")),
            map("陈仓", boss(126, "法正"), boss(129, "魏延")),
            map("邪马台东境", boss(144, "卑弥呼")),
            map("夷北", boss(144, "举父")),
            map("罗马", boss(159, "佩蒂奈克斯"), boss(159, "康茂德")),
            map("拜占庭", boss(165, "奈哲尔")),
            map("上潘诺尼亚", boss(165, "塞维鲁斯")),
            map("辽东", boss(174, "徐晃"), boss(174, "曹植")),
            map("吴兴", boss(179, "鲁肃"), boss(179, "孙仁")),
            map("乱军祭坛一层", boss(189, "人公张梁")),
            map("乱军祭坛二层", boss(189, "地公张宝")),
            map("玉门关", boss(194, "头曼"), boss(194, "冒顿")),
            map("越巂", boss(200, "饕餮"), boss(200, "穷奇")),
            map("楼兰宫殿", boss(209, "混沌"), boss(209, "梼杌")),
            map("马图拉", boss(219, "帕尔瓦蒂"), boss(219, "迦尼萨")),
            map("华氏城", boss(224, "梵天"), boss(224, "毗湿奴")),
            map("郿坞宫殿", boss(234, "魔化董卓"), boss(234, "魔化貂蝉")),
            map("广陵城内", boss(239, "夏侯渊"), boss(239, "贾诩")),
            map("武都大寨", boss(249, "马谡"), boss(249, "黄月英")),
            map("夷陵大帐", boss(254, "周泰"), boss(254, "孙权")),
            map("斯巴达城", boss(264, "阿瑞斯"), boss(264, "阿提米斯")),
            map("高天原", boss(279, "琼琼杵尊"), boss(279, "大国主"))));

    private WorldBossCatalog() {
    }

    static MapEntry findMap(String text) {
        String normalized = normalize(text);
        MapEntry best = null;
        for (MapEntry map : MAPS) {
            if (normalized.contains(normalize(map.name))
                    && (best == null || map.name.length() > best.name.length())) {
                best = map;
            }
        }
        return best;
    }

    static MapEntry findMapByName(String name) {
        for (MapEntry map : MAPS) {
            if (map.name.equals(name)) {
                return map;
            }
        }
        return null;
    }

    static List<String> mapNames() {
        List<String> names = new ArrayList<>();
        for (MapEntry map : MAPS) {
            names.add(map.name + " " + map.bossInfo());
        }
        return names;
    }

    static MapEntry findMapByDisplayName(String displayName) {
        for (MapEntry map : MAPS) {
            if (displayName != null && displayName.startsWith(map.name + " ")) {
                return map;
            }
        }
        return null;
    }

    private static MapEntry map(String name, BossEntry... bosses) {
        return new MapEntry(name, Arrays.asList(bosses));
    }

    private static BossEntry boss(int level, String name) {
        return boss(level, name, DEFAULT_LEFT, DEFAULT_RIGHT);
    }

    private static BossEntry boss(int level, String name, int left, int right) {
        return new BossEntry(level, name, left, right);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^\\p{IsHan}A-Za-z0-9]", "");
    }

    static final class MapEntry {
        final String name;
        final List<BossEntry> bosses;

        private MapEntry(String name, List<BossEntry> bosses) {
            this.name = name;
            this.bosses = Collections.unmodifiableList(bosses);
        }

        String bossInfo() {
            List<String> values = new ArrayList<>();
            for (BossEntry boss : bosses) {
                values.add(boss.displayName());
            }
            return "[" + String.join(", ", values) + "]";
        }

        List<String> bossNames() {
            List<String> names = new ArrayList<>();
            for (BossEntry boss : bosses) {
                names.add(boss.displayName());
            }
            return names;
        }

        BossEntry findBoss(String value) {
            for (BossEntry boss : bosses) {
                if (boss.name.equals(value) || boss.displayName().equals(value)) {
                    return boss;
                }
            }
            return null;
        }
    }

    static final class BossEntry {
        final int level;
        final String name;
        final int searchLeft;
        final int searchRight;

        private BossEntry(int level, String name, int searchLeft, int searchRight) {
            this.level = level;
            this.name = name;
            this.searchLeft = searchLeft;
            this.searchRight = searchRight;
        }

        String displayName() {
            return level + " " + name;
        }

        boolean isVisible(String text) {
            return normalize(text).contains(normalize(displayName()))
                    || normalize(text).contains(normalize(name));
        }
    }
}
