package com.local.sgmhelper;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class AntiCheatVerification {
    static final int UNKNOWN_ROTATION = -1;

    private static final int[] TILE_X = {530, 640, 750};
    private static final int TILE_Y = 315;
    private static final int TILE_SIZE = 74;
    private static final int VERIFY_X = 640;
    private static final int VERIFY_Y = 430;
    private static final int TAP_INTERVAL_MS = 250;
    private static final int MAX_ROUNDS = 2;
    private static final long ROUND_BUDGET_MS = 25_000;

    /** Whether one tap turns a tile clockwise; corrected the first time a challenge is solved. */
    private static boolean tapTurnsClockwise = true;

    private final AutomationHost host;

    AntiCheatVerification(AutomationHost host) {
        this.host = host;
    }

    private static final class Attempt {
        final int round;
        final long deadlineAt;
        final boolean clockwise;
        final Runnable next;
        boolean turned;
        boolean allTilesRecognized;

        Attempt(int round, long deadlineAt, boolean clockwise, Runnable next) {
            this.round = round;
            this.deadlineAt = deadlineAt;
            this.clockwise = clockwise;
            this.next = next;
        }
    }

    void checkThen(Runnable next) {
        host.showProgress("检查反外挂验证");
        host.recognizeText(text -> {
            if (hasChallenge(text)) {
                solve(new Attempt(1, System.currentTimeMillis() + ROUND_BUDGET_MS,
                        tapTurnsClockwise, next));
            } else {
                next.run();
            }
        });
    }

    private void solve(Attempt attempt) {
        if (attempt.round > MAX_ROUNDS || System.currentTimeMillis() > attempt.deadlineAt) {
            giveUp(attempt);
            return;
        }
        host.showProgress("反外挂：识别头像方向（第 " + attempt.round + " 轮）");
        host.captureScreenshot(screenshot -> {
            if (screenshot == null) {
                host.postDelayed(() -> solve(attempt), 500);
                return;
            }
            List<Bitmap> tiles = new ArrayList<>();
            for (int index = 0; index < TILE_X.length; index++) {
                tiles.add(cropTile(screenshot, index));
            }
            screenshot.recycle();
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setMinFaceSize(0.05f)
                    .build();
            FaceDetector detector = FaceDetection.getClient(options);
            findRotations(detector, tiles, new int[tiles.size()], 0, rotations -> {
                detector.close();
                turnTiles(tiles, rotations, attempt);
            });
        });
    }

    /** Finds, for every tile, the clockwise rotation that makes its portrait stand upright. */
    private void findRotations(FaceDetector detector, List<Bitmap> tiles, int[] rotations,
            int tile, Consumer<int[]> result) {
        if (tile == tiles.size()) {
            result.accept(rotations);
            return;
        }
        scoreRotation(detector, tiles.get(tile), 0, UNKNOWN_ROTATION, 0, rotation -> {
            rotations[tile] = rotation;
            findRotations(detector, tiles, rotations, tile + 1, result);
        });
    }

    /** Scores one rotation of a tile and keeps the orientation showing the largest face. */
    private void scoreRotation(FaceDetector detector, Bitmap tile, int step,
            int bestRotation, int bestArea, Consumer<Integer> result) {
        if (step == 4) {
            result.accept(bestRotation);
            return;
        }
        Bitmap rotated = rotate(tile, step * 90);
        detector.process(InputImage.fromBitmap(rotated, 0))
                .addOnFailureListener(error -> DiagnosticLog.warn("ANTI_CHEAT",
                        "Face detection failed: " + error.getMessage()))
                .addOnCompleteListener(done -> {
                    int area = done.isSuccessful() ? largestFaceArea(done.getResult()) : 0;
                    if (rotated != tile) {
                        rotated.recycle();
                    }
                    if (area > bestArea) {
                        scoreRotation(detector, tile, step + 1, step * 90, area, result);
                    } else {
                        scoreRotation(detector, tile, step + 1, bestRotation, bestArea, result);
                    }
                });
    }

    private static int largestFaceArea(List<Face> faces) {
        int largest = 0;
        if (faces == null) {
            return largest;
        }
        for (Face face : faces) {
            Rect box = face.getBoundingBox();
            largest = Math.max(largest, box.width() * box.height());
        }
        return largest;
    }

    private void turnTiles(List<Bitmap> tiles, int[] rotations, Attempt attempt) {
        StringBuilder plan = new StringBuilder();
        int totalTaps = 0;
        attempt.allTilesRecognized = true;
        for (int index = 0; index < rotations.length; index++) {
            int taps = tapCount(rotations[index], attempt.clockwise);
            totalTaps += taps;
            if (rotations[index] == UNKNOWN_ROTATION) {
                attempt.allTilesRecognized = false;
                DiagnosticLog.saveScreenshot(tiles.get(index),
                        "anticheat-unrecognized-tile" + (index + 1));
            }
            plan.append(index == 0 ? "" : " ")
                    .append("tile").append(index + 1).append('=')
                    .append(rotations[index] == UNKNOWN_ROTATION
                            ? "?" : String.valueOf(rotations[index]))
                    .append("/taps").append(taps);
        }
        for (Bitmap tile : tiles) {
            tile.recycle();
        }
        attempt.turned = totalTaps > 0;
        DiagnosticLog.info("ANTI_CHEAT", "round=" + attempt.round
                + " clockwise=" + attempt.clockwise + " " + plan);
        turnTile(rotations, 0, attempt, () -> submit(attempt));
    }

    private void turnTile(int[] rotations, int tile, Attempt attempt, Runnable done) {
        if (tile == rotations.length) {
            done.run();
            return;
        }
        int taps = tapCount(rotations[tile], attempt.clockwise);
        if (taps > 0) {
            host.showProgress("反外挂：旋转头像 " + (tile + 1) + " ×" + taps);
        }
        tapTile(tile, taps, () -> turnTile(rotations, tile + 1, attempt, done));
    }

    private void tapTile(int tile, int remaining, Runnable done) {
        if (remaining <= 0) {
            done.run();
            return;
        }
        host.tapFast(TILE_X[tile], TILE_Y, () -> host.postDelayed(
                () -> tapTile(tile, remaining - 1, done), TAP_INTERVAL_MS));
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

    private static Bitmap rotate(Bitmap tile, int degrees) {
        if (degrees == 0) {
            return tile;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(tile, 0, 0, tile.getWidth(), tile.getHeight(), matrix, true);
    }

    private void submit(Attempt attempt) {
        host.showProgress("反外挂：提交验证");
        host.tapFast(VERIFY_X, VERIFY_Y,
                () -> host.postDelayed(() -> confirmSolved(attempt, 0), 1_000));
    }

    private void confirmSolved(Attempt attempt, int clearChecks) {
        host.recognizeText(text -> {
            if (hasChallenge(text)) {
                // Only a round that recognised every tile and actually turned one says anything
                // about the turning direction; otherwise keep the current guess.
                boolean flip = attempt.turned && attempt.allTilesRecognized;
                DiagnosticLog.info("ANTI_CHEAT", "round=" + attempt.round
                        + " did not clear the challenge; flipDirection=" + flip);
                solve(new Attempt(attempt.round + 1, attempt.deadlineAt,
                        flip != attempt.clockwise, attempt.next));
            } else if (clearChecks == 0) {
                host.postDelayed(() -> confirmSolved(attempt, 1), 500);
            } else {
                if (attempt.turned && attempt.allTilesRecognized) {
                    tapTurnsClockwise = attempt.clockwise;
                }
                DiagnosticLog.info("ANTI_CHEAT", "solved in round=" + attempt.round
                        + " clockwise=" + attempt.clockwise);
                attempt.next.run();
            }
        });
    }

    private void giveUp(Attempt attempt) {
        DiagnosticLog.warn("ANTI_CHEAT",
                "Giving up after " + (attempt.round - 1) + " rounds; challenge still open");
        host.captureScreenshot(screenshot -> {
            if (screenshot != null) {
                DiagnosticLog.saveScreenshot(screenshot, "anticheat-unsolved");
                screenshot.recycle();
            }
            host.failAutomation("反外挂验证未通过");
        });
    }

    /** Taps needed to turn a tile clockwise by {@code rotation} degrees. */
    static int tapCount(int rotation, boolean clockwise) {
        if (rotation == UNKNOWN_ROTATION) {
            return 0;
        }
        int steps = Math.floorMod(rotation / 90, 4);
        return clockwise ? steps : Math.floorMod(-steps, 4);
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
