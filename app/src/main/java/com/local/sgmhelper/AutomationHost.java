package com.local.sgmhelper;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.mlkit.vision.text.Text;

import java.util.function.Consumer;

interface AutomationHost {
    enum PrimaryTask {
        TRAINING,
        BOSS,
        DUNGEON
    }

    int MAP_GAME_MAX_X = 600;
    int MAP_GAME_MAX_Y = 50;
    int MAP_SCREEN_LEFT = 436;
    int MAP_SCREEN_TOP = 610;
    int MAP_SCREEN_WIDTH = 408;
    int MAP_SCREEN_HEIGHT = 80;

    Context context();

    boolean isAutomationRunning();

    void startAutomation(String progress, Runnable firstAction);

    void startInGameAutomation(String progress, Runnable firstAction);

    void startPrimaryAutomation(PrimaryTask task, String progress, Runnable firstAction);

    void showProgress(String value);

    void failAutomation(String message);

    void completeAutomation();

    void resumePrimaryTask();

    void enterTraining(long nextMilitaryAt);

    void checkInventoryBeforePrimary(Runnable next);

    void postDelayed(Runnable action, long delayMillis);

    String formatTime(long value);

    void tap(int x, int y, Runnable next);

    void tapFast(int x, int y, Runnable next);

    void setTextAt(int x, int y, String value, Runnable next);

    default void tapMapCoordinate(int mapX, int mapY, Runnable next) {
        tap(mapScreenX(mapX), mapScreenY(mapY), next);
    }

    default void tapMapCoordinateFast(int mapX, int mapY, Runnable next) {
        tapFast(mapScreenX(mapX), mapScreenY(mapY), next);
    }

    static int mapScreenX(int mapX) {
        if (mapX < 0 || mapX > MAP_GAME_MAX_X) {
            throw new IllegalArgumentException("mapX must be between 0 and 600");
        }
        int safeX = Math.max(1, Math.min(mapX, MAP_GAME_MAX_X - 1));
        int screenX = MAP_SCREEN_LEFT
                + (int) Math.ceil(safeX * MAP_SCREEN_WIDTH / (double) MAP_GAME_MAX_X);
        return Math.min(screenX, MAP_SCREEN_LEFT + MAP_SCREEN_WIDTH - 1);
    }

    static int mapScreenY(int mapY) {
        if (mapY < 0 || mapY > MAP_GAME_MAX_Y) {
            throw new IllegalArgumentException("mapY must be between 0 and 50");
        }
        int safeY = Math.max(1, Math.min(mapY, MAP_GAME_MAX_Y - 1));
        int screenY = MAP_SCREEN_TOP
                + (int) Math.ceil(safeY * MAP_SCREEN_HEIGHT / (double) MAP_GAME_MAX_Y);
        return Math.min(screenY, MAP_SCREEN_TOP + MAP_SCREEN_HEIGHT - 1);
    }

    void swipe(int startX, int startY, int endX, int endY, Runnable next);

    void swipe(int startX, int startY, int endX, int endY,
            long durationMillis, Runnable next);

    void recognizeText(Consumer<Text> result);

    void recognizeRedBoss(Consumer<BossAutomation.BossTarget> result);

    void recognizeHudChannel(Bitmap screenshot, Consumer<Integer> result);

    void recognizeLeaderChannel(Consumer<Integer> result);

    void recognizeMapCoordinate(Consumer<String> result);

    void recognizeBackpackCapacity(Consumer<String> result);

    void recognizeYuanbaoQuickSell(Consumer<Boolean> result);

    void captureScreenshot(Consumer<Bitmap> result);

    void clickText(String expected, boolean exact, Runnable next, int attempts);

    void clickText(String expected, boolean exact, Runnable next,
            int attempts, Runnable ifMissing);

    void clickLeftText(String expected, Runnable next, int attempts, Runnable ifMissing);

    void clickRightText(String expected, Runnable next, int attempts, Runnable ifMissing);

    void clickRightTextFast(String expected, Runnable next, int attempts, Runnable ifMissing);

    void clickQuickArrival(Runnable next);

    void waitForText(String expected, int attempts, Runnable next);

    void waitForMapReady(int attempts, Runnable next);

    default void closeWelfareWindow(Runnable next) {
        showProgress("关闭福利：关闭福利界面");
        tap(1225, 38, () -> {
            showProgress("关闭福利：检查尚未领取的奖励");
            clickText("关闭界面", true, next, 5, next);
        });
    }

    void claimWelfare(Runnable next);

    void closeAutoPathPanel(Runnable next);

    void openAutoPathPanel(Runnable next);

    void ensureGameHudVisible(Runnable next);

    void ensureAutoAttackDisabled(Runnable next);

    default void useFirstMarker(Runnable next) {
        ensureAutoAttackDisabled(() -> tap(50, 625, next));
    }

    void ensureAutoAttackEnabled(Runnable next);

    void returnHome(Runnable next);
}
