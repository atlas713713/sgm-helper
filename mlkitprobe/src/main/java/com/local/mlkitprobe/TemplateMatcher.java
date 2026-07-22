package com.local.mlkitprobe;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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

final class TemplateMatcher {
    private static final String TEMPLATE_ASSET = "anti_cheat_templates.zip";
    private static final int IMAGE_SIZE = 16;
    private static final int WUDANG_WIDTH = 70;
    private static final int WUDANG_ALT_WIDTH = 65;
    private static final int WUDANG_HEIGHT = 70;
    private static final double MATCH_SCORE = 0.95;

    static final class Match {
        final int orientation;
        final double score;
        final double[] orientationScores;
        final boolean recognized;
        final boolean exactInput;
        final int templateIndex;
        final String templateName;

        Match(int orientation, double score, double[] orientationScores, boolean exactInput,
                int templateIndex, String templateName) {
            this.orientation = orientation;
            this.score = score;
            this.orientationScores = orientationScores;
            this.recognized = isRecognized(score);
            this.exactInput = exactInput;
            this.templateIndex = templateIndex;
            this.templateName = templateName;
        }
    }

    private final AssetManager assets;
    private volatile Mat[] templates;
    private volatile byte[][] templateImages;
    private volatile String[] templateNames;
    private volatile Bitmap calibrationBitmap;

    TemplateMatcher(AssetManager assets) {
        this.assets = assets;
        if (!OpenCVLoader.initLocal()) {
            throw new IllegalStateException("OpenCV AAR 初始化失败");
        }
    }

    String openCvVersion() {
        return Core.VERSION;
    }

    Match match(Bitmap bitmap) throws IOException {
        boolean exactInput = bitmap.getHeight() == WUDANG_HEIGHT
                && (bitmap.getWidth() == WUDANG_WIDTH
                || bitmap.getWidth() == WUDANG_ALT_WIDTH);
        Mat source = sourceGray16(bitmap);
        try {
            return bestOrientation(source, templates(), templateNames, exactInput);
        } finally {
            source.release();
        }
    }

    int templateCount() throws IOException {
        return templates().length;
    }

    Bitmap calibrationBitmap() throws IOException {
        templates();
        Bitmap bitmap = calibrationBitmap;
        if (bitmap == null) {
            throw new IOException("没有可用的标准模板");
        }
        return bitmap.copy(Bitmap.Config.ARGB_8888, false);
    }

    Bitmap templateBitmap(Match match) throws IOException {
        templates();
        byte[] data = templateImages[match.templateIndex];
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap == null) {
            throw new IOException("无法读取最高分模板 " + match.templateName);
        }
        return bitmap;
    }

    void close() {
        Mat[] loaded = templates;
        if (loaded != null) {
            for (Mat template : loaded) {
                template.release();
            }
            templates = null;
        }
        templateImages = null;
        templateNames = null;
        Bitmap bitmap = calibrationBitmap;
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        calibrationBitmap = null;
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
        List<byte[]> images = new ArrayList<>(4_000);
        List<String> names = new ArrayList<>(4_000);
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
                byte[] data = bytes.toByteArray();
                if (calibrationBitmap == null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bitmap != null) {
                        calibrationBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                        bitmap.recycle();
                    }
                }

                Mat encoded = new MatOfByte(data);
                Mat gray = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_GRAYSCALE);
                encoded.release();
                if (!gray.empty()) {
                    Mat resized = new Mat();
                    Imgproc.resize(gray, resized, new Size(IMAGE_SIZE, IMAGE_SIZE));
                    resized.convertTo(resized, CvType.CV_32F);
                    loaded.add(resized);
                    images.add(data);
                    names.add(entry.getName());
                }
                gray.release();
                zip.closeEntry();
            }
        }
        if (loaded.isEmpty()) {
            throw new IOException("头像模板库为空");
        }
        templateImages = images.toArray(new byte[0][]);
        templateNames = names.toArray(new String[0]);
        return loaded.toArray(new Mat[0]);
    }

    private static Mat sourceGray16(Bitmap source) throws IOException {
        Bitmap rgb565 = source.copy(Bitmap.Config.RGB_565, false);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        if (!rgb565.compress(Bitmap.CompressFormat.PNG, 100, png)) {
            rgb565.recycle();
            throw new IOException("灰度图 PNG 转换失败");
        }
        rgb565.recycle();

        Mat encoded = new MatOfByte(png.toByteArray());
        Mat decoded = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_GRAYSCALE);
        encoded.release();
        if (decoded.empty()) {
            decoded.release();
            throw new IOException("OpenCV 灰度图重读失败");
        }

        Mat resized = new Mat();
        Imgproc.resize(decoded, resized, new Size(IMAGE_SIZE, IMAGE_SIZE));
        decoded.release();
        resized.convertTo(resized, CvType.CV_32F);
        return resized;
    }

    private static Match bestOrientation(Mat source, Mat[] templates, String[] templateNames,
            boolean exactInput) {
        int bestOrientation = 0;
        int bestTemplateIndex = -1;
        double bestScore = -1.0;
        double[] orientationScores = new double[4];
        for (int orientation = 0; orientation < 4; orientation++) {
            Mat candidate = rotated(source, orientation);
            int[] templateIndex = {-1};
            double score = bestTemplateScore(candidate, templates, templateIndex);
            orientationScores[orientation] = score;
            if (score > bestScore) {
                bestScore = score;
                bestOrientation = orientation;
                bestTemplateIndex = templateIndex[0];
            }
            if (candidate != source) {
                candidate.release();
            }
        }
        return new Match(bestOrientation, bestScore, orientationScores, exactInput,
                bestTemplateIndex, templateNames[bestTemplateIndex]);
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

    private static double bestTemplateScore(Mat source, Mat[] templates, int[] bestTemplateIndex) {
        double best = -1.0;
        for (int i = 0; i < templates.length; i++) {
            double score = Imgproc.compareHist(source, templates[i], Imgproc.HISTCMP_CORREL);
            if (score > best) {
                best = score;
                bestTemplateIndex[0] = i;
            }
        }
        return best;
    }

    static boolean isRecognized(double score) {
        return score >= MATCH_SCORE;
    }

    static int expectedCorrection(int displayedQuarterTurns) {
        return (4 - displayedQuarterTurns % 4) % 4;
    }
}
