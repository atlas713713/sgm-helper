package com.local.sgmhelper;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/** The same captain-location and portrait-state checks used by Wudang. */
final class TeamFollowerVision {
    private static final String SELF_CAPTAIN_ASSET = "team_captain_self.webp";
    private static final String TEAMMATE_CAPTAIN_ASSET = "team_captain_teammate.webp";
    private static final int REFERENCE_WIDTH = 1280;
    private static final int REFERENCE_HEIGHT = 720;
    private static final double TEMPLATE_SCORE = 0.85;
    private static final double SAME_MAP_BRIGHT_RATIO = 0.20;

    static final class Inspection {
        final boolean selfCaptain;
        final int captainSlot;
        final double templateScore;
        final double brightRatio;

        Inspection(boolean selfCaptain, int captainSlot,
                double templateScore, double brightRatio) {
            this.selfCaptain = selfCaptain;
            this.captainSlot = captainSlot;
            this.templateScore = templateScore;
            this.brightRatio = brightRatio;
        }

        boolean hasTeammateCaptain() {
            return captainSlot >= 0;
        }

        boolean captainIsOnSameMap() {
            return isSameMap(brightRatio);
        }
    }

    private static final class Match {
        final double score;
        final double centerX;

        Match(double score, double centerX) {
            this.score = score;
            this.centerX = centerX;
        }
    }

    private final AssetManager assets;
    private Mat selfCaptainTemplate;
    private Mat teammateCaptainTemplate;

    TeamFollowerVision(AssetManager assets) {
        this.assets = assets;
        if (!OpenCVLoader.initLocal()) {
            throw new IllegalStateException("OpenCV AAR 初始化失败");
        }
    }

    Inspection inspect(Bitmap screenshot) throws IOException {
        ensureTemplatesLoaded();
        Mat color = new Mat();
        Mat gray = new Mat();
        try {
            Utils.bitmapToMat(screenshot, color);
            Imgproc.cvtColor(color, gray, Imgproc.COLOR_RGBA2GRAY);
            double scaleX = screenshot.getWidth() / (double) REFERENCE_WIDTH;
            double scaleY = screenshot.getHeight() / (double) REFERENCE_HEIGHT;

            Match self = match(gray, selfCaptainTemplate,
                    4, 4, 40, 40, scaleX, scaleY);
            if (self.score >= TEMPLATE_SCORE) {
                return new Inspection(true, -1, self.score, 1.0);
            }

            Match teammate = match(gray, teammateCaptainTemplate,
                    16, 112, 260, 145, scaleX, scaleY);
            if (teammate.score < TEMPLATE_SCORE) {
                return new Inspection(false, -1, teammate.score, 1.0);
            }
            int slot = captainSlotForFlagCenter(teammate.centerX);
            if (slot < 0) {
                return new Inspection(false, -1, teammate.score, 1.0);
            }
            return new Inspection(false, slot, teammate.score,
                    captainBrightRatio(screenshot, slot));
        } finally {
            color.release();
            gray.release();
        }
    }

    private void ensureTemplatesLoaded() throws IOException {
        if (selfCaptainTemplate != null && teammateCaptainTemplate != null) {
            return;
        }
        selfCaptainTemplate = loadGrayTemplate(SELF_CAPTAIN_ASSET);
        teammateCaptainTemplate = loadGrayTemplate(TEAMMATE_CAPTAIN_ASSET);
    }

    private Mat loadGrayTemplate(String assetName) throws IOException {
        return WudangTemplateMatcher.loadGrayTemplate(assets, assetName);
    }

    private static Match match(Mat source, Mat originalTemplate,
            int left, int top, int right, int bottom,
            double scaleX, double scaleY) {
        WudangTemplateMatcher.GrayMatch match = WudangTemplateMatcher.matchGray(
                source, originalTemplate, left, top, right, bottom, scaleX, scaleY);
        return new Match(match.score, match.centerX);
    }

    private static double captainBrightRatio(Bitmap screenshot, int slot) {
        int avatarX = avatarX(slot);
        int left = scale(avatarX, screenshot.getWidth(), REFERENCE_WIDTH);
        int right = scale(avatarX + 46, screenshot.getWidth(), REFERENCE_WIDTH);
        int top = scale(141, screenshot.getHeight(), REFERENCE_HEIGHT);
        int bottom = scale(181, screenshot.getHeight(), REFERENCE_HEIGHT);
        int bright = 0;
        int total = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                int pixel = screenshot.getPixel(x, y);
                // Wudang converts the bitmap with OpenCV COLOR_BGR2GRAY, then thresholds at 70.
                int gray = (29 * Color.red(pixel)
                        + 150 * Color.green(pixel)
                        + 77 * Color.blue(pixel) + 128) >> 8;
                if (gray > 70) {
                    bright++;
                }
                total++;
            }
        }
        return total == 0 ? 0.0 : bright / (double) total;
    }

    static int captainSlotForFlagCenter(double flagCenterX) {
        for (int slot = 0; slot < 5; slot++) {
            int flagLeft = avatarX(slot) - 10;
            if (flagCenterX >= flagLeft && flagCenterX <= flagLeft + 25) {
                return slot;
            }
        }
        return -1;
    }

    static boolean isSameMap(double brightRatio) {
        return brightRatio >= SAME_MAP_BRIGHT_RATIO;
    }

    private static int avatarX(int slot) {
        return 26 + 51 * slot + slot / 2;
    }

    private static int scale(int coordinate, int actual, int reference) {
        return clamp((int) Math.round(coordinate * actual / (double) reference), 0, actual);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
