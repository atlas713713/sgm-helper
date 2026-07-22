package com.local.sgmhelper;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class AntiCheatTemplateMatcher {
    private static final String TEMPLATE_ASSET = "anti_cheat_templates.zip";
    private static final int IMAGE_SIZE = 16;
    private static final double EARLY_MATCH_SCORE = 0.95;

    static final class Result {
        final int[] orientations;
        final double[] scores;

        Result(int[] orientations, double[] scores) {
            this.orientations = orientations;
            this.scores = scores;
        }
    }

    static final class OrientationMatch {
        final int orientation;
        final double score;

        OrientationMatch(int orientation, double score) {
            this.orientation = orientation;
            this.score = score;
        }
    }

    private final AssetManager assets;
    private volatile Mat[] templates;

    AntiCheatTemplateMatcher(AssetManager assets) {
        this.assets = assets;
        if (!OpenCVLoader.initLocal()) {
            throw new IllegalStateException("OpenCV AAR 初始化失败");
        }
    }

    Result match(Bitmap[] tiles) throws IOException {
        Mat[] loadedTemplates = templates();
        int[] orientations = new int[tiles.length];
        double[] scores = new double[tiles.length];
        for (int tile = 0; tile < tiles.length; tile++) {
            Mat source = sourceGray16(tiles[tile]);
            try {
                OrientationMatch match = bestOrientation(source, loadedTemplates);
                orientations[tile] = match.orientation;
                scores[tile] = match.score;
            } finally {
                source.release();
            }
        }
        return new Result(orientations, scores);
    }

    private Mat[] templates() throws IOException {
        Mat[] cached = templates;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (templates == null) {
                templates = loadTemplates();
            }
            return templates;
        }
    }

    private Mat[] loadTemplates() throws IOException {
        List<Mat> loaded = new ArrayList<>(4_000);
        byte[] buffer = new byte[4_096];
        try (ZipInputStream zip = new ZipInputStream(assets.open(TEMPLATE_ASSET))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(
                        entry.getSize() > 0 ? (int) entry.getSize() : 4_096);
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                }
                Mat encoded = new MatOfByte(bytes.toByteArray());
                Mat gray = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_GRAYSCALE);
                encoded.release();
                if (!gray.empty()) {
                    Mat resized = new Mat();
                    Imgproc.resize(gray, resized, new Size(IMAGE_SIZE, IMAGE_SIZE));
                    resized.convertTo(resized, CvType.CV_32F);
                    loaded.add(resized);
                }
                gray.release();
                zip.closeEntry();
            }
        }
        if (loaded.isEmpty()) {
            throw new IOException("反外挂头像模板库为空");
        }
        DiagnosticLog.info("ANTI_CHEAT", "Loaded templates=" + loaded.size());
        return loaded.toArray(new Mat[0]);
    }

    private static Mat sourceGray16(Bitmap source) throws IOException {
        Bitmap rgb565 = source.copy(Bitmap.Config.RGB_565, false);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        if (!rgb565.compress(Bitmap.CompressFormat.PNG, 100, png)) {
            rgb565.recycle();
            throw new IOException("反外挂裁图编码失败");
        }
        rgb565.recycle();

        Mat encoded = new MatOfByte(png.toByteArray());
        Mat gray = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_GRAYSCALE);
        encoded.release();
        if (gray.empty()) {
            gray.release();
            throw new IOException("反外挂裁图灰度解码失败");
        }
        Mat resized = new Mat();
        Imgproc.resize(gray, resized, new Size(IMAGE_SIZE, IMAGE_SIZE));
        gray.release();
        resized.convertTo(resized, CvType.CV_32F);
        return resized;
    }

    private static OrientationMatch bestOrientation(Mat source, Mat[] templates) {
        int bestOrientation = 0;
        double bestScore = -1.0;
        for (int orientation = 0; orientation < 4; orientation++) {
            Mat candidate = rotated(source, orientation);
            double score = bestTemplateScore(candidate, templates);
            if (score > bestScore) {
                bestScore = score;
                bestOrientation = orientation;
            }
            if (candidate != source) {
                candidate.release();
            }
        }
        return new OrientationMatch(bestOrientation, bestScore);
    }

    private static Mat rotated(Mat source, int orientation) {
        if (orientation == 0) {
            return source;
        }
        Mat rotated = new Mat();
        int rotateCode = orientation == 1
                ? Core.ROTATE_90_CLOCKWISE
                : orientation == 2
                ? Core.ROTATE_180
                : Core.ROTATE_90_COUNTERCLOCKWISE;
        Core.rotate(source, rotated, rotateCode);
        return rotated;
    }

    private static double bestTemplateScore(Mat source, Mat[] templates) {
        double best = -1.0;
        for (Mat template : templates) {
            double score = Imgproc.compareHist(source, template, Imgproc.HISTCMP_CORREL);
            if (score >= EARLY_MATCH_SCORE) {
                return score;
            }
            best = Math.max(best, score);
        }
        return best;
    }

    static OrientationMatch bestOrientation(byte[] source, byte[][] templates, int size) {
        int bestOrientation = 0;
        double bestScore = -1.0;
        byte[] rotated = source;
        for (int orientation = 0; orientation < 4; orientation++) {
            double score = bestTemplateScore(rotated, templates);
            if (score > bestScore) {
                bestScore = score;
                bestOrientation = orientation;
            }
            rotated = rotateClockwise(rotated, size);
        }
        return new OrientationMatch(bestOrientation, bestScore);
    }

    private static double bestTemplateScore(byte[] source, byte[][] templates) {
        double best = -1.0;
        for (byte[] template : templates) {
            double score = correlation(source, template);
            if (score >= EARLY_MATCH_SCORE) {
                return score;
            }
            best = Math.max(best, score);
        }
        return best;
    }

    static byte[] rotateClockwise(byte[] source, int size) {
        byte[] rotated = new byte[source.length];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                rotated[y * size + x] = source[(size - 1 - x) * size + y];
            }
        }
        return rotated;
    }

    static double correlation(byte[] first, byte[] second) {
        if (first.length != second.length || first.length == 0) {
            return -1.0;
        }
        double firstMean = 0.0;
        double secondMean = 0.0;
        for (int i = 0; i < first.length; i++) {
            firstMean += first[i] & 0xff;
            secondMean += second[i] & 0xff;
        }
        firstMean /= first.length;
        secondMean /= second.length;

        double numerator = 0.0;
        double firstVariance = 0.0;
        double secondVariance = 0.0;
        for (int i = 0; i < first.length; i++) {
            double firstDelta = (first[i] & 0xff) - firstMean;
            double secondDelta = (second[i] & 0xff) - secondMean;
            numerator += firstDelta * secondDelta;
            firstVariance += firstDelta * firstDelta;
            secondVariance += secondDelta * secondDelta;
        }
        double denominator = Math.sqrt(firstVariance * secondVariance);
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }
}
