package com.local.sgmhelper;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

interface AutomationHost {
    enum PrimaryTask {
        TRAINING,
        BOSS,
        DUNGEON
    }

    int MAP_GAME_MAX_X = 599;
    int MAP_GAME_MAX_Y = 49;
    int MAP_SCREEN_LEFT = 433;
    int MAP_SCREEN_TOP = 610;
    int MAP_SCREEN_WIDTH = 414;
    int MAP_SCREEN_HEIGHT = 81;

    Context context();

    boolean isAutomationRunning();

    long currentRunId();

    boolean isRunCurrent(long runId);

    void startAutomation(String progress, Runnable firstAction);

    void startHighPriorityAutomation(String progress, Runnable firstAction);

    void startHighPriorityInGameAutomation(String progress, Runnable firstAction);

    void startInGameAutomation(String progress, Runnable firstAction);

    void startIdleAutomation(String progress, Runnable firstAction);

    void startPrimaryAutomation(PrimaryTask task, String progress, Runnable firstAction);

    void showProgress(String value);

    void failAutomation(String message);

    void failPrimaryAndRestartAfter(String message, long delayMillis);

    void completeAutomation();

    void resumePrimaryTask();

    void enterTraining(long nextMilitaryAt);

    void enterActiveTraining(long nextMilitaryAt);

    void checkInventoryBeforePrimary(Runnable next);

    boolean handlePendingGear(Runnable afterHandled);

    void postDelayed(Runnable action, long delayMillis);

    String formatTime(long value);

    void tap(int x, int y, Runnable next);

    void tapUi(int x, int y, Runnable next);

    void tapFast(int x, int y, Runnable next);

    void setTextAt(int x, int y, String value, Runnable next);

    default void tapMapCoordinate(int mapX, int mapY, Runnable next) {
        tap(mapScreenX(mapX), mapScreenY(mapY), next);
    }

    default void tapMapCoordinate(int mapX, int mapY, int mapMaxX, Runnable next) {
        tap(mapScreenX(mapX, mapMaxX), mapScreenY(mapY), next);
    }

    default void tapMapCoordinateFast(int mapX, int mapY, Runnable next) {
        tapFast(mapScreenX(mapX), mapScreenY(mapY), next);
    }

    static int mapScreenX(int mapX) {
        return mapScreenX(mapX, MAP_GAME_MAX_X);
    }

    static int mapScreenX(int mapX, int mapMaxX) {
        if (mapMaxX <= 1 || mapX < 0 || mapX > mapMaxX) {
            throw new IllegalArgumentException("mapX must be inside the supplied map width");
        }
        return MAP_SCREEN_LEFT + mapX * MAP_SCREEN_WIDTH / mapMaxX;
    }

    static int mapScreenY(int mapY) {
        if (mapY < 0 || mapY > MAP_GAME_MAX_Y) {
            throw new IllegalArgumentException("mapY must be between 0 and 49");
        }
        return MAP_SCREEN_TOP + mapY * MAP_SCREEN_HEIGHT / MAP_GAME_MAX_Y;
    }

    void swipe(int startX, int startY, int endX, int endY, Runnable next);

    void swipeUi(int startX, int startY, int endX, int endY, Runnable next);

    void swipe(int startX, int startY, int endX, int endY,
            long durationMillis, Runnable next);

    void recognizeText(Consumer<OcrText> result);

    void recognizeText(Bitmap bitmap, Consumer<OcrText> result,
            Consumer<Throwable> failure);

    void matchTemplates(WudangTemplateMatcher.Template[] templates,
            int left, int top, int right, int bottom,
            Consumer<Map<WudangTemplateMatcher.Template, WudangTemplateMatcher.Match>> result,
            Consumer<Throwable> failure);

    default void clickTemplateOrText(WudangTemplateMatcher.Template template,
            String fallbackText, boolean exact,
            int left, int top, int right, int bottom,
            Runnable next, int templateAttempts, int ocrAttempts,
            Runnable ifMissing) {
        if (!isAutomationRunning()) {
            return;
        }
        matchTemplates(new WudangTemplateMatcher.Template[] {template},
                left, top, right, bottom, matches -> {
                    WudangTemplateMatcher.Match match = matches.get(template);
                    if (match != null && match.found()) {
                        tap(match.centerX(), match.centerY(), next);
                    } else if (templateAttempts > 1) {
                        postDelayed(() -> clickTemplateOrText(template, fallbackText, exact,
                                left, top, right, bottom, next,
                                templateAttempts - 1, ocrAttempts, ifMissing), 500);
                    } else {
                        DiagnosticLog.info("TEMPLATE_FALLBACK",
                                "name=" + template.label + " source=ocr");
                        clickTextRegion(fallbackText, exact, left, top, right, bottom,
                                next, ocrAttempts, ifMissing);
                    }
                }, error -> {
                    if (templateAttempts > 1) {
                        postDelayed(() -> clickTemplateOrText(template, fallbackText, exact,
                                left, top, right, bottom, next,
                                templateAttempts - 1, ocrAttempts, ifMissing), 500);
                    } else {
                        DiagnosticLog.info("TEMPLATE_FALLBACK",
                                "name=" + template.label + " source=ocr error="
                                        + error.getClass().getSimpleName());
                        clickTextRegion(fallbackText, exact, left, top, right, bottom,
                                next, ocrAttempts, ifMissing);
                    }
                });
    }

    default void waitTemplateOrText(WudangTemplateMatcher.Template template,
            String fallbackText, boolean exact,
            int left, int top, int right, int bottom,
            int templateAttempts, int ocrAttempts,
            Runnable next, Runnable ifMissing) {
        if (!isAutomationRunning()) {
            return;
        }
        matchTemplates(new WudangTemplateMatcher.Template[] {template},
                left, top, right, bottom, matches -> {
                    WudangTemplateMatcher.Match match = matches.get(template);
                    if (match != null && match.found()) {
                        next.run();
                    } else if (templateAttempts > 1) {
                        postDelayed(() -> waitTemplateOrText(template, fallbackText, exact,
                                left, top, right, bottom,
                                templateAttempts - 1, ocrAttempts, next, ifMissing), 500);
                    } else {
                        DiagnosticLog.info("TEMPLATE_FALLBACK",
                                "name=" + template.label + " source=ocr");
                        waitForTextRegion(fallbackText, left, top, right, bottom,
                                ocrAttempts, next, ifMissing);
                    }
                }, error -> {
                    if (templateAttempts > 1) {
                        postDelayed(() -> waitTemplateOrText(template, fallbackText, exact,
                                left, top, right, bottom,
                                templateAttempts - 1, ocrAttempts, next, ifMissing), 500);
                    } else {
                        DiagnosticLog.info("TEMPLATE_FALLBACK",
                                "name=" + template.label + " source=ocr error="
                                        + error.getClass().getSimpleName());
                        waitForTextRegion(fallbackText, left, top, right, bottom,
                                ocrAttempts, next, ifMissing);
                    }
                });
    }

    default void recognizeTextRegion(
            int left, int top, int right, int bottom, Consumer<OcrText> result) {
        captureScreenshot(bitmap -> {
            if (bitmap == null) {
                failAutomation("Unable to capture screen for Paddle OCR");
                return;
            }
            int x = left * bitmap.getWidth() / 1280;
            int y = top * bitmap.getHeight() / 720;
            int width = (right - left) * bitmap.getWidth() / 1280;
            int height = (bottom - top) * bitmap.getHeight() / 720;
            Bitmap region = Bitmap.createBitmap(bitmap, x, y, width, height);
            bitmap.recycle();
            recognizeText(region, text -> {
                region.recycle();
                result.accept(OcrText.offset(text, x, y));
            }, error -> {
                region.recycle();
                DiagnosticLog.error("OCR", "Region Paddle OCR failed", error);
                failAutomation("Paddle OCR failed");
            });
        });
    }

    void recognizeDungeonText(Consumer<List<OcrLine>> result);

    void recognizeRedBoss(Consumer<BossAutomation.BossTarget> result);

    void recognizeHudChannel(Bitmap screenshot, Consumer<Integer> result);

    void recognizeChannelDialog(Consumer<Integer> result);

    void recognizeLeaderChannel(Consumer<Integer> result);

    void recognizeMapCoordinate(Consumer<String> result);

    void recognizeMapName(Consumer<String> result);

    void recognizeWorldBossMap(Consumer<String> result);

    void recognizeBackpackCapacity(Consumer<String> result);

    void recognizeYuanbaoQuickSell(Consumer<Boolean> result);

    void captureScreenshot(Consumer<Bitmap> result);

    void pressBack(Runnable next);

    void clickText(String expected, boolean exact, Runnable next, int attempts);

    void clickText(String expected, boolean exact, Runnable next,
            int attempts, Runnable ifMissing);

    void clickTextRegion(String expected, boolean exact,
            int left, int top, int right, int bottom,
            Runnable next, int attempts, Runnable ifMissing);

    void clickTextUi(String expected, boolean exact, Runnable next,
            int attempts, Runnable ifMissing);

    void clickCenterText(String expected, Runnable next, int attempts,
            Runnable ifMissing);

    void clickDungeonText(String expected, boolean exact, Runnable next,
            int attempts, Runnable ifMissing);

    void clickLeftText(String expected, Runnable next, int attempts, Runnable ifMissing);

    void clickRightText(String expected, Runnable next, int attempts, Runnable ifMissing);

    void clickRightTextFast(String expected, Runnable next, int attempts, Runnable ifMissing);

    void clickQuickArrival(Runnable next);

    void waitForText(String expected, int attempts, Runnable next);

    void waitForTextRegion(String expected,
            int left, int top, int right, int bottom,
            int attempts, Runnable next, Runnable ifMissing);

    void waitForMapReady(int attempts, Runnable next);

    default void closeWelfareWindow(Runnable next) {
        showProgress("关闭福利：关闭福利界面");
        tap(1225, 38, () -> {
            showProgress("关闭福利：检查尚未领取的奖励");
            clickText("关闭界面", true, () -> claimWelfare(next), 1, next);
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
