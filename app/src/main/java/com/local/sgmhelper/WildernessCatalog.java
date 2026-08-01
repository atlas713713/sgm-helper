package com.local.sgmhelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 武当 {@code arrays.xml} 的 {@code wild} 表和 {@code w0.c()} 的军团任务映射。
 *
 * <p>荒野修炼 1–24 区每三个区共用一套怪物；军团任务的“阶”决定去哪一段区、打哪一级怪。
 * 例如军团任务十 → 7–9 区、130 级石狮精。
 */
final class WildernessCatalog {
    static final int MAX_ZONE = 24;
    /** 武当的怪物坐标 y 固定是 30（typeX 599 的荒野地图）。 */
    static final int ENEMY_Y = 30;

    /** 一种荒野怪：等级、名字，以及它出现的 x 区间（可能不止一段）。 */
    static final class Enemy {
        final int level;
        final String name;
        /** 每段是 {@code {startX, endX}}，坐标空间是 typeX 599。 */
        final int[][] xRanges;

        Enemy(int level, String name, int[][] xRanges) {
            this.level = level;
            this.name = name;
            this.xRanges = xRanges;
        }

        /** 武当 {@code GameUtil.o}：先随机挑一段，再在段内随机取 x。 */
        int randomX() {
            int[] range = xRanges[xRanges.length == 1
                    ? 0 : ThreadLocalRandom.current().nextInt(xRanges.length)];
            return range[0] >= range[1]
                    ? range[0] : ThreadLocalRandom.current().nextInt(range[0], range[1]);
        }

        boolean containsX(int x) {
            for (int[] range : xRanges) {
                if (x >= range[0] && x <= range[1]) {
                    return true;
                }
            }
            return false;
        }
    }

    /** 军团任务的一阶：去 {@code zoneLow..zoneHigh} 区，打 {@code enemyLevel} 级怪。 */
    static final class Tier {
        final int zoneLow;
        final int zoneHigh;
        final int enemyLevel;

        Tier(int zoneLow, int zoneHigh, int enemyLevel) {
            this.zoneLow = zoneLow;
            this.zoneHigh = zoneHigh;
            this.enemyLevel = enemyLevel;
        }
    }

    private static final List<List<Enemy>> ZONE_GROUPS = List.of(
            // 1–3 区
            List.of(new Enemy(30, "蛇魔女", new int[][] {{30, 90}}),
                    new Enemy(40, "火妖", new int[][] {{110, 180}}),
                    new Enemy(50, "土霸王", new int[][] {{210, 240}}),
                    new Enemy(60, "食人花", new int[][] {{370, 400}}),
                    new Enemy(70, "金甲龙", new int[][] {{440, 500}}),
                    new Enemy(75, "圣武士", new int[][] {{520, 570}})),
            // 4–6 区
            List.of(new Enemy(80, "魔斗士", new int[][] {{30, 165}}),
                    new Enemy(90, "海妖", new int[][] {{200, 230}, {370, 415}}),
                    new Enemy(100, "螳蝎妖", new int[][] {{460, 570}})),
            // 7–9 区
            List.of(new Enemy(115, "九尾狐", new int[][] {{30, 230}}),
                    new Enemy(130, "石狮精", new int[][] {{370, 570}})),
            // 10–12 区
            List.of(new Enemy(145, "人面鸟", new int[][] {{30, 230}}),
                    new Enemy(160, "战鬼", new int[][] {{370, 570}})),
            // 13–15 区
            List.of(new Enemy(175, "式神童", new int[][] {{30, 230}}),
                    new Enemy(190, "剑齿虎", new int[][] {{370, 570}})),
            // 16–18 区
            List.of(new Enemy(205, "犬神", new int[][] {{30, 230}}),
                    new Enemy(220, "木人兵", new int[][] {{370, 570}})),
            // 19–21 区
            List.of(new Enemy(235, "曼陀猪", new int[][] {{30, 230}}),
                    new Enemy(250, "黑暗鬼", new int[][] {{370, 570}})),
            // 22–24 区
            List.of(new Enemy(265, "史前猿", new int[][] {{30, 230}}),
                    new Enemy(280, "死魂刀兵", new int[][] {{370, 570}})));

    private WildernessCatalog() {
    }

    /** 这个区里能打的怪；区号超出 1–24 返回空表。 */
    static List<Enemy> enemies(int zone) {
        if (zone < 1 || zone > MAX_ZONE) {
            return Collections.emptyList();
        }
        return ZONE_GROUPS.get((zone - 1) / 3);
    }

    static Enemy enemy(int zone, int level) {
        for (Enemy enemy : enemies(zone)) {
            if (enemy.level == level) {
                return enemy;
            }
        }
        return null;
    }

    /** 按名字找怪，用来支持直接指定怪物的流程；也认早期版本里写错的两个名字。 */
    static Enemy enemyByName(String name) {
        String normalized = normalizeEnemyName(name);
        for (List<Enemy> group : ZONE_GROUPS) {
            for (Enemy enemy : group) {
                if (enemy.name.equals(normalized)) {
                    return enemy;
                }
            }
        }
        return null;
    }

    /** 旧设置里存的是“螳螂巨妖”“式神童子”，武当的表里是“螳蝎妖”“式神童”。 */
    static String normalizeEnemyName(String name) {
        if (name == null) {
            return "";
        }
        String value = name.trim();
        if ("螳螂巨妖".equals(value)) {
            return "螳蝎妖";
        }
        if ("式神童子".equals(value)) {
            return "式神童";
        }
        return value;
    }

    /** 所有怪的名字，按区从低到高。 */
    static List<String> enemyNames() {
        List<String> names = new ArrayList<>();
        for (List<Enemy> group : ZONE_GROUPS) {
            for (Enemy enemy : group) {
                names.add(enemy.name);
            }
        }
        return names;
    }

    /**
     * 军团任务的阶 → 区段和目标怪等级，对应武当 {@code w0.c()}。
     * 阶写作“十”“十九”，也接受阿拉伯数字。
     */
    static Tier tier(String taskLevel) {
        Integer index = parseTier(taskLevel);
        if (index == null) {
            return null;
        }
        switch (index) {
            case 1:
                return new Tier(1, 3, 30);
            case 2:
                return new Tier(1, 3, 40);
            case 3:
                return new Tier(1, 3, 50);
            case 4:
                return new Tier(1, 3, 60);
            case 5:
                return new Tier(1, 3, 70);
            case 6:
                return new Tier(1, 3, 75);
            case 7:
                return new Tier(4, 6, 80);
            case 8:
                return new Tier(4, 6, 100);
            case 9:
                return new Tier(7, 9, 115);
            case 10:
                return new Tier(7, 9, 130);
            case 11:
                return new Tier(10, 12, 145);
            case 12:
                return new Tier(10, 12, 160);
            case 13:
                return new Tier(13, 15, 175);
            case 14:
                return new Tier(13, 15, 190);
            case 15:
                return new Tier(16, 18, 205);
            case 16:
                return new Tier(16, 18, 220);
            case 17:
                return new Tier(19, 21, 235);
            case 18:
                return new Tier(19, 21, 250);
            case 19:
                return new Tier(22, 24, 265);
            default:
                return null;
        }
    }

    /** 武当只会在已配置的练级区不在区段里时才随机换一个。 */
    static int zoneForTier(String taskLevel, int preferredZone) {
        Tier tier = tier(taskLevel);
        if (tier == null) {
            return 0;
        }
        if (preferredZone >= tier.zoneLow && preferredZone <= tier.zoneHigh) {
            return preferredZone;
        }
        return ThreadLocalRandom.current().nextInt(tier.zoneLow, tier.zoneHigh + 1);
    }

    /** 这一阶该打的怪；阶或区不认识时返回 null。 */
    static Enemy enemyForTier(String taskLevel, int zone) {
        Tier tier = tier(taskLevel);
        return tier == null ? null : enemy(zone, tier.enemyLevel);
    }

    static Integer parseTier(String taskLevel) {
        if (taskLevel == null) {
            return null;
        }
        String value = taskLevel.replaceAll("[\\s阶（）()]", "");
        if (value.isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 1 && parsed <= 19 ? parsed : null;
        } catch (NumberFormatException notArabic) {
            return parseChineseTier(value);
        }
    }

    private static Integer parseChineseTier(String value) {
        int ten = value.indexOf('十');
        if (ten < 0) {
            int digit = chineseDigit(value);
            return digit > 0 && value.length() == 1 ? digit : null;
        }
        if (value.length() == 1) {
            return 10;
        }
        if (ten == 0 && value.length() == 2) {
            int digit = chineseDigit(value.substring(1));
            return digit > 0 ? 10 + digit : null;
        }
        return null;
    }

    private static int chineseDigit(String value) {
        int at = "一二三四五六七八九".indexOf(value);
        return at < 0 ? 0 : at + 1;
    }
}
