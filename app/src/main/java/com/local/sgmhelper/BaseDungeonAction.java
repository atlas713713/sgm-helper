package com.local.sgmhelper;

import java.util.concurrent.ThreadLocalRandom;

/** Shared dungeon mechanics. Each level keeps its own route and dialogue in a subclass. */
abstract class BaseDungeonAction {
    /** 武当 80 级以上副本 toLeft/toRight 的固定步长。 */
    private static final int HIGH_LEVEL_STEP = 30;

    abstract int level();

    /** 副本名；返回 null 表示不 OCR 校验宫殿引路 NPC 的标题。 */
    abstract String dungeonName();

    /** 入口 NPC 名；返回 null 表示改用 {@link #entryNpcButtons()} 按文字点行。 */
    abstract String entryNpcName();

    /** 出口 NPC 名；返回 null 表示改用 {@link #exitNpcButtons()} 按文字点行。 */
    abstract String exitNpcName();

    abstract String campName();

    abstract int campNpcX();

    abstract int[] entryNpcRows();

    abstract int[] exitNpcRows();

    abstract DungeonBattleAutomation.RouteDecision decide(int x, int y);

    /** 武当的 exitLocation：离场时寻路的固定坐标，和推进方向无关。 */
    abstract int[] exitPoint();

    /**
     * 武当自己就是按按钮文字找行的（`npcButtonEnter` 等）。80 级以上的副本名和
     * NPC 名来自服务端配置，APK 里没有，所以这些副本只能按文字点，返回 null 表示
     * 沿用 10–75 已经验证过的固定行号。
     */
    String[] entryNpcButtons() {
        return null;
    }

    String[] exitNpcButtons() {
        return null;
    }

    int entryNpcX() {
        return 100;
    }

    int entryNpcY() {
        return 16;
    }

    /** 副本地图宽度：武当的 `mapType`。 */
    int dungeonMapMaxX() {
        return 599;
    }

    /** 副本内交互 NPC 的名字；返回 null 表示按按钮文字点行。 */
    String interactionNpcName() {
        if (level() >= 80) {
            return null;
        }
        return level() == 65 ? "刘备" : "孙坚";
    }

    int interactionNpcRow() {
        return level() == 65 ? 3 : 4;
    }

    final boolean isAtEntryNpc(String coordinateText) {
        return DungeonBattleAutomation.isAtCoordinate(
                coordinateText, entryNpcX(), entryNpcY());
    }

    static BaseDungeonAction forLevel(int level) {
        switch (level) {
            case 10:
                return new Dungeon10Action();
            case 20:
                return new Dungeon20Action();
            case 30:
                return new Dungeon30Action();
            case 40:
                return new Dungeon40Action();
            case 50:
                return new Dungeon50Action();
            case 60:
                return new Dungeon60Action();
            case 65:
                return new Dungeon65Action();
            case 70:
                return new Dungeon70Action();
            case 75:
                return new Dungeon75Action();
            case 80:
                return new Dungeon80Action();
            case 90:
                return new Dungeon90Action();
            case 105:
                return new Dungeon105Action();
            case 120:
                return new Dungeon120Action();
            case 135:
                return new Dungeon135Action();
            case 150:
                return new Dungeon150Action();
            case 165:
                return new Dungeon165Action();
            default:
                throw new IllegalArgumentException("Unsupported dungeon level " + level);
        }
    }

    protected static int scanLeft(int currentX, int gateX) {
        return Math.max(gateX, currentX - randomGuideStep());
    }

    protected static int scanRight(int currentX, int gateX) {
        return Math.min(gateX, currentX + randomGuideStep());
    }

    /**
     * 80 级以上副本的 toLeft/toRight：固定 30 格一步、y=27，夹到
     * {@code min(begin,end)-5} / {@code max(begin,end)+5}。
     */
    protected static int stepLeft(int currentX, int beginX, int endX) {
        int next = currentX - HIGH_LEVEL_STEP;
        int limit = Math.min(beginX, endX);
        return next < limit ? limit - 5 : next;
    }

    protected static int stepRight(int currentX, int beginX, int endX) {
        int next = currentX + HIGH_LEVEL_STEP;
        int limit = Math.max(beginX, endX);
        return next > limit ? limit + 5 : next;
    }

    protected static boolean isNear(int x, int y, int targetX, int targetY) {
        return Math.abs(x - targetX) <= 3 && Math.abs(y - targetY) <= 3;
    }

    private static int randomGuideStep() {
        return ThreadLocalRandom.current().nextInt(40, 50);
    }
}
