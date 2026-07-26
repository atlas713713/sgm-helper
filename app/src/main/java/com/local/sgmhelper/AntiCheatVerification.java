package com.local.sgmhelper;

import android.graphics.Bitmap;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

final class AntiCheatVerification {
    private static final int[] TILE_X = {534, 642, 750};
    private static final int TILE_Y = 316;
    private static final int TILE_SIZE = 70;
    private static final int VERIFY_X = 640;
    private static final int VERIFY_Y = 430;
    private static final int CHALLENGE_LEFT = 440;
    private static final int CHALLENGE_TOP = 150;
    private static final int CHALLENGE_RIGHT = 840;
    private static final int CHALLENGE_BOTTOM = 470;
    private static final int TAP_INTERVAL_MS = 300;
    private static final int MAX_ROUNDS = 2;

    private final AutomationHost host;
    private AntiCheatTemplateMatcher templateMatcher;

    AntiCheatVerification(AutomationHost host) {
        this.host = host;
    }

    void checkThen(Runnable next) {
        host.showProgress("检查验证窗口");
        recognizeChallenge(challengeVisible -> {
            if (challengeVisible) {
                solve(next, 1);
            } else {
                next.run();
            }
        });
    }

    private void solve(Runnable next, int round) {
        host.showProgress("反外挂：截取头像");
        host.captureScreenshot(bitmap -> {
            if (bitmap == null) {
                host.postDelayed(() -> solve(next, round), 500);
                return;
            }
            Bitmap[] tiles = new Bitmap[TILE_X.length];
            for (int tile = 0; tile < TILE_X.length; tile++) {
                tiles[tile] = cropTile(bitmap, tile);
            }
            bitmap.recycle();
            host.showProgress("反外挂：模板匹配中");
            AntiCheatTemplateMatcher matcher = templateMatcher();
            long runId = host.currentRunId();
            CompletableFuture.supplyAsync(() -> {
                try {
                    return matcher.match(tiles);
                } catch (Exception error) {
                    throw new CompletionException(error);
                }
            }).whenComplete((result, error) -> {
                if (!host.isRunCurrent(runId)) {
                    recycle(tiles);
                    return;
                }
                if (error != null) {
                    saveUnrecognizedSamples(tiles);
                }
                recycle(tiles);
                host.postDelayed(() -> {
                    if (!host.isRunCurrent(runId)) {
                        return;
                    }
                    if (error != null) {
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        DiagnosticLog.warn("ANTI_CHEAT",
                                "Template matching failed: " + cause.getMessage());
                        host.failAutomation("反外挂验证模板匹配失败");
                        return;
                    }
                    DiagnosticLog.info("ANTI_CHEAT",
                            "orientations=" + Arrays.toString(result.orientations)
                                    + " scores=" + Arrays.toString(result.scores));
                    applyOrientations(result.orientations, 0, next, round);
                }, 0);
            });
        });
    }

    private AntiCheatTemplateMatcher templateMatcher() {
        if (templateMatcher == null) {
            templateMatcher = new AntiCheatTemplateMatcher(host.context().getAssets());
        }
        return templateMatcher;
    }

    private void applyOrientations(int[] orientations, int tile, Runnable next, int round) {
        if (tile == orientations.length) {
            submit(next, round);
            return;
        }
        host.showProgress("反外挂：头像 " + (tile + 1)
                + " 旋转 " + orientations[tile] + " 次");
        tapTile(tile, orientations[tile],
                () -> applyOrientations(orientations, tile + 1, next, round));
    }

    private Bitmap cropTile(Bitmap screenshot, int tile) {
        int centerX = TILE_X[tile] * screenshot.getWidth() / 1280;
        int centerY = TILE_Y * screenshot.getHeight() / 720;
        int size = TILE_SIZE * screenshot.getWidth() / 1280;
        int left = Math.max(0, centerX - size / 2);
        int top = Math.max(0, centerY - size / 2);
        int width = Math.min(size, screenshot.getWidth() - left);
        int height = Math.min(size, screenshot.getHeight() - top);
        return Bitmap.createBitmap(screenshot, left, top, width, height);
    }

    private void tapTile(int tile, int remaining, Runnable done) {
        if (remaining == 0) {
            done.run();
            return;
        }
        host.tapFast(TILE_X[tile], TILE_Y, () -> host.postDelayed(
                () -> tapTile(tile, remaining - 1, done), TAP_INTERVAL_MS));
    }

    private static void saveUnrecognizedSamples(Bitmap[] samples) {
        for (int tile = 0; tile < samples.length; tile++) {
            DiagnosticLog.saveScreenshot(samples[tile], "anticheat-tile" + (tile + 1));
        }
    }

    private static void recycle(Bitmap[] bitmaps) {
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private void submit(Runnable next, int round) {
        host.showProgress("反外挂：提交验证");
        host.tapFast(VERIFY_X, VERIFY_Y,
                () -> host.postDelayed(() -> confirmSolved(next, round, 0), 1_000));
    }

    private void confirmSolved(Runnable next, int round, int clearChecks) {
        recognizeChallenge(challengeVisible -> {
            if (challengeVisible) {
                if (round >= MAX_ROUNDS) {
                    host.failAutomation("反外挂验证未通过");
                } else {
                    solve(next, round + 1);
                }
            } else if (clearChecks == 0) {
                host.postDelayed(() -> confirmSolved(next, round, 1), 500);
            } else {
                next.run();
            }
        });
    }

    private void recognizeChallenge(Consumer<Boolean> result) {
        recognizeChallenge(false, result);
    }

    void captureChallengeIfVisible(Consumer<Boolean> result) {
        recognizeChallenge(true, result);
    }

    private void recognizeChallenge(boolean saveRawScreenshot, Consumer<Boolean> result) {
        host.captureScreenshot(screenshot -> {
            if (screenshot == null) {
                host.postDelayed(
                        () -> recognizeChallenge(saveRawScreenshot, result), 400);
                return;
            }
            Bitmap region = cropChallengeRegion(screenshot);
            Bitmap enlarged = Bitmap.createScaledBitmap(region,
                    region.getWidth() * 2, region.getHeight() * 2, true);
            region.recycle();
            host.recognizeText(enlarged, text -> {
                        boolean visible = containsChallengeText(text);
                        if (visible && saveRawScreenshot) {
                            DiagnosticLog.saveScreenshot(
                                    screenshot, "channel-test-anticheat-raw");
                        }
                        screenshot.recycle();
                        enlarged.recycle();
                        result.accept(visible);
                    }, error -> {
                        screenshot.recycle();
                        enlarged.recycle();
                        DiagnosticLog.warn("ANTI_CHEAT",
                                "Challenge ROI Paddle OCR failed: " + error.getMessage());
                        result.accept(false);
                    });
        });
    }

    private static Bitmap cropChallengeRegion(Bitmap screenshot) {
        int left = CHALLENGE_LEFT * screenshot.getWidth() / 1280;
        int top = CHALLENGE_TOP * screenshot.getHeight() / 720;
        int right = CHALLENGE_RIGHT * screenshot.getWidth() / 1280;
        int bottom = CHALLENGE_BOTTOM * screenshot.getHeight() / 720;
        return Bitmap.createBitmap(screenshot, left, top, right - left, bottom - top);
    }

    static boolean hasChallenge(OcrText text) {
        for (OcrText.TextBlock block : text.getTextBlocks()) {
            for (OcrText.Line line : block.getLines()) {
                android.graphics.Rect bounds = line.getBoundingBox();
                if (bounds != null && isChallengeLabel(
                        line.getText(), bounds.centerX(), bounds.centerY())) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean containsChallengeText(OcrText text) {
        return isChallengeText(text.getText());
    }

    static boolean isChallengeText(String value) {
        String normalized = value.replaceAll("\\s+", "");
        return normalized.contains("反外挂验证")
                || normalized.contains("旋转至正确方向");
    }

    static boolean isChallengeLabel(String value, int centerX, int centerY) {
        String normalized = value.replaceAll("\\s+", "");
        return centerX >= 350 && centerX <= 900
                && centerY >= 150 && centerY <= 360
                && (normalized.contains("反外挂验证")
                        || normalized.contains("旋转至正确方向"));
    }
}
