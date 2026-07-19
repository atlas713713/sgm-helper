package com.local.sgmhelper;

import android.graphics.Bitmap;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;

import java.util.List;

final class AntiCheatVerification {
    enum TileDecision {
        NEXT_TILE,
        CLICK,
        RECHECK_CHALLENGE
    }

    private static final int[] TILE_X = {530, 640, 750};
    private static final int TILE_Y = 315;
    private static final int TILE_SIZE = 74;
    private static final int VERIFY_X = 640;
    private static final int VERIFY_Y = 430;

    private final AutomationHost host;

    AntiCheatVerification(AutomationHost host) {
        this.host = host;
    }

    void checkThen(Runnable next) {
        host.showProgress("检查反外挂验证");
        host.recognizeText(text -> {
            if (hasChallenge(text)) {
                solve(next);
            } else {
                next.run();
            }
        });
    }

    private void solve(Runnable next) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(0.05f)
                .build();
        orientTile(FaceDetection.getClient(options), 0, 0, next);
    }

    private void orientTile(FaceDetector detector, int tile, int clicks, Runnable next) {
        if (tile == TILE_X.length) {
            detector.close();
            submit(next);
            return;
        }
        host.showProgress("反外挂：检查头像 " + (tile + 1) + "/" + TILE_X.length);
        host.captureScreenshot(bitmap -> {
            if (bitmap == null) {
                host.postDelayed(() -> orientTile(detector, tile, clicks, next), 500);
                return;
            }
            Bitmap face = cropTile(bitmap, tile);
            bitmap.recycle();
            detectFace(detector, face, upright -> {
                DiagnosticLog.info("ANTI_CHEAT", "Tile=" + (tile + 1)
                        + " clicks=" + clicks + " upright=" + upright);
                TileDecision decision = decideTile(upright, clicks);
                if (decision == TileDecision.NEXT_TILE) {
                    orientTile(detector, tile + 1, 0, next);
                    return;
                }
                if (decision == TileDecision.RECHECK_CHALLENGE) {
                    detector.close();
                    DiagnosticLog.info("ANTI_CHEAT",
                            "Tile stayed unrecognized after one cycle; rechecking challenge");
                    host.postDelayed(() -> checkThen(next), 500);
                    return;
                }
                host.showProgress("反外挂：旋转头像 " + (tile + 1));
                host.tapFast(TILE_X[tile], TILE_Y,
                        () -> host.postDelayed(
                                () -> orientTile(detector, tile, clicks + 1, next), 300));
            });
        });
    }

    private Bitmap cropTile(Bitmap screenshot, int tile) {
        int centerX = TILE_X[tile] * screenshot.getWidth() / 1280;
        int centerY = TILE_Y * screenshot.getHeight() / 720;
        int size = TILE_SIZE * screenshot.getWidth() / 1280;
        int left = Math.max(0, centerX - size / 2);
        int top = Math.max(0, centerY - size / 2);
        int width = Math.min(size, screenshot.getWidth() - left);
        int height = Math.min(size, screenshot.getHeight() - top);
        Bitmap crop = Bitmap.createBitmap(screenshot, left, top, width, height);
        Bitmap scaled = Bitmap.createScaledBitmap(crop, width * 4, height * 4, true);
        crop.recycle();
        return scaled;
    }

    private void detectFace(FaceDetector detector, Bitmap face,
            java.util.function.Consumer<Boolean> result) {
        Task<List<Face>> task = detector.process(InputImage.fromBitmap(face, 0));
        task.addOnFailureListener(error -> DiagnosticLog.warn(
                        "ANTI_CHEAT", "Face detection failed: " + error.getMessage()))
                .addOnCompleteListener(done -> {
                    boolean upright = done.isSuccessful() && !done.getResult().isEmpty();
                    face.recycle();
                    result.accept(upright);
                });
    }

    private void submit(Runnable next) {
        host.showProgress("反外挂：提交验证");
        host.tapFast(VERIFY_X, VERIFY_Y,
                () -> host.postDelayed(() -> confirmSolved(next, 0), 1_000));
    }

    private void confirmSolved(Runnable next, int clearChecks) {
        host.recognizeText(text -> {
            if (hasChallenge(text)) {
                solve(next);
            } else if (clearChecks == 0) {
                host.postDelayed(() -> confirmSolved(next, 1), 500);
            } else {
                next.run();
            }
        });
    }

    static TileDecision decideTile(boolean upright, int clicks) {
        if (upright) {
            return TileDecision.NEXT_TILE;
        }
        return clicks < 3 ? TileDecision.CLICK : TileDecision.RECHECK_CHALLENGE;
    }

    static boolean hasChallenge(Text text) {
        for (Text.TextBlock block : text.getTextBlocks()) {
            String value = block.getText().replaceAll("\\s+", "");
            if (value.contains("反外挂验证") || value.contains("旋转至正确方向")) {
                return true;
            }
        }
        return false;
    }
}
