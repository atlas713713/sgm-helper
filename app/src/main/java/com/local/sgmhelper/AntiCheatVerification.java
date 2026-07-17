package com.local.sgmhelper;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;

import java.util.List;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

final class AntiCheatVerification {
    private static final int[] TILE_X = {530, 640, 750};
    private static final int TILE_Y = 315;
    private static final int TILE_SIZE = 74;
    private static final int[] ROTATIONS = {0, 90, 180, 270};
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
        host.showProgress("反外挂：识别头像方向");
        host.captureScreenshot(bitmap -> {
            if (bitmap == null) {
                host.postDelayed(() -> checkThen(next), 500);
                return;
            }
            Bitmap[] tiles = cropTiles(bitmap);
            bitmap.recycle();
            detectRotations(tiles, rotations -> {
                for (int rotation : rotations) {
                    if (rotation < 0) {
                        host.showProgress("反外挂：未识别人脸，重新检测");
                        host.postDelayed(() -> checkThen(next), 500);
                        return;
                    }
                }
                clickRotations(rotations, 0, next);
            });
        });
    }

    private Bitmap[] cropTiles(Bitmap screenshot) {
        Bitmap[] tiles = new Bitmap[TILE_X.length];
        for (int index = 0; index < TILE_X.length; index++) {
            int centerX = TILE_X[index] * screenshot.getWidth() / 1280;
            int centerY = TILE_Y * screenshot.getHeight() / 720;
            int size = TILE_SIZE * screenshot.getWidth() / 1280;
            int left = Math.max(0, centerX - size / 2);
            int top = Math.max(0, centerY - size / 2);
            int width = Math.min(size, screenshot.getWidth() - left);
            int height = Math.min(size, screenshot.getHeight() - top);
            Bitmap crop = Bitmap.createBitmap(screenshot, left, top, width, height);
            tiles[index] = Bitmap.createScaledBitmap(crop, width * 4, height * 4, true);
            crop.recycle();
        }
        return tiles;
    }

    private void detectRotations(Bitmap[] tiles, java.util.function.Consumer<int[]> result) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(0.05f)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);
        int[][] scores = new int[tiles.length][ROTATIONS.length];
        AtomicInteger remaining = new AtomicInteger(tiles.length * ROTATIONS.length);

        for (int tile = 0; tile < tiles.length; tile++) {
            for (int rotation = 0; rotation < ROTATIONS.length; rotation++) {
                int tileIndex = tile;
                int rotationIndex = rotation;
                Task<List<Face>> task = detector.process(
                        InputImage.fromBitmap(tiles[tile], ROTATIONS[rotation]));
                task.addOnSuccessListener(faces ->
                                scores[tileIndex][rotationIndex] = largestFace(faces))
                        .addOnFailureListener(error -> DiagnosticLog.warn(
                                "ANTI_CHEAT", "Face detection failed: " + error.getMessage()))
                        .addOnCompleteListener(done -> {
                            if (remaining.decrementAndGet() != 0) {
                                return;
                            }
                            detector.close();
                            for (Bitmap bitmap : tiles) {
                                bitmap.recycle();
                            }
                            int[] rotations = new int[tiles.length];
                            for (int index = 0; index < rotations.length; index++) {
                                rotations[index] = bestRotation(scores[index]);
                            }
                            DiagnosticLog.info("ANTI_CHEAT",
                                    "Detected rotations " + Arrays.toString(rotations));
                            result.accept(rotations);
                        });
            }
        }
    }

    private void clickRotations(int[] rotations, int tile, Runnable next) {
        if (tile == rotations.length) {
            host.showProgress("反外挂：提交验证");
            host.tapFast(VERIFY_X, VERIFY_Y,
                    () -> host.postDelayed(() -> confirmSolved(next), 1_000));
            return;
        }
        clickTile(TILE_X[tile], rotations[tile] / 90,
                () -> clickRotations(rotations, tile + 1, next));
    }

    private void clickTile(int x, int remainingClicks, Runnable next) {
        if (remainingClicks == 0) {
            next.run();
            return;
        }
        host.tapFast(x, TILE_Y,
                () -> clickTile(x, remainingClicks - 1, next));
    }

    private void confirmSolved(Runnable next) {
        host.recognizeText(text -> {
            if (hasChallenge(text)) {
                solve(next);
            } else {
                next.run();
            }
        });
    }

    private static int largestFace(List<Face> faces) {
        int largest = 0;
        for (Face face : faces) {
            Rect bounds = face.getBoundingBox();
            largest = Math.max(largest, bounds.width() * bounds.height());
        }
        return largest;
    }

    static int bestRotation(int[] scores) {
        int best = -1;
        for (int index = 0; index < scores.length; index++) {
            if (scores[index] > 0 && (best < 0 || scores[index] > scores[best])) {
                best = index;
            }
        }
        return best < 0 ? -1 : ROTATIONS[best];
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
