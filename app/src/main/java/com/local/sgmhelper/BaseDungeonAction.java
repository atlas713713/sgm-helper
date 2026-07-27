package com.local.sgmhelper;

import java.util.concurrent.ThreadLocalRandom;

/** Shared dungeon mechanics. Each level keeps its own route and dialogue in a subclass. */
abstract class BaseDungeonAction {
    abstract int level();

    abstract String dungeonName();

    abstract String entryNpcName();

    abstract String exitNpcName();

    abstract String campName();

    abstract int campNpcX();

    abstract int[] entryNpcRows();

    abstract int[] exitNpcRows();

    abstract DungeonBattleAutomation.RouteDecision decide(int x, int y);

    int entryNpcX() {
        return 100;
    }

    int entryNpcY() {
        return 16;
    }

    boolean usesGuideUp() {
        return level() <= 30 || level() == 65 || level() >= 70;
    }

    int dungeonMapMaxX() {
        return level() == 75 ? 199 : 599;
    }

    String interactionNpcName() {
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

    protected static boolean isNear(int x, int y, int targetX, int targetY) {
        return Math.abs(x - targetX) <= 3 && Math.abs(y - targetY) <= 3;
    }

    private static int randomGuideStep() {
        return ThreadLocalRandom.current().nextInt(40, 50);
    }
}
