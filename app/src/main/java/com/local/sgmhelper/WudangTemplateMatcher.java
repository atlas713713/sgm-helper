package com.local.sgmhelper;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Small, cached matcher for fixed Wudang UI assets. */
final class WudangTemplateMatcher {
    static final int REFERENCE_WIDTH = 1280;
    static final int REFERENCE_HEIGHT = 720;
    static final double DEFAULT_THRESHOLD = 0.85;

    enum Template {
        MAP_TAB("地图", "data/m.wd"),
        DIALOG_TAB("对话", "data/n.wd"),
        AUTO_PATH("自动寻路", "data/g04.wd"),
        YUANBAO_RECYCLE("元宝回收", "data/t1.wd"),
        SHOP("商店", "data/t3.wd", "data/t3_a.wd"),
        SKILL("技能", "data/t5.wd", "data/t5_a.wd"),
        INN("客栈", "data/t6.wd", "data/t6_a.wd"),
        LEGION("军团", "data/t8.wd", "data/t8_a.wd"),
        PALACE("宫殿", "data/t10.wd", "data/t10_a.wd"),
        DUNGEON("副本", "data/t11.wd", "data/t11_a.wd"),
        AUTO_FUNCTION("自动功能", "data/t14.wd", "data/t14_a.wd"),
        REWARD_RECOVERY("奖励找回", "data/t15.wd", "data/t15_a.wd"),
        ONE_CLICK_CLAIM("一键领取", "data/bf2.wd", "data/bf2_a.wd"),
        CLAIM("领取", "data/bf3.wd", "data/bf3_a.wd",
                "data/bf4.wd", "data/bf4_a.wd",
                "data/bf5.wd", "data/bf5_a.wd"),
        GO_CLAIM("前往领取", "data/bf4.wd", "data/bf4_a.wd");

        final String label;
        final String[] assets;

        Template(String label, String... assets) {
            this.label = label;
            this.assets = assets;
        }
    }

    static final class Match {
        final Template template;
        final String asset;
        final double score;
        final Rect bounds;
        final long elapsedMs;

        Match(Template template, String asset, double score, Rect bounds, long elapsedMs) {
            this.template = template;
            this.asset = asset;
            this.score = score;
            this.bounds = bounds;
            this.elapsedMs = elapsedMs;
        }

        boolean found() {
            return bounds != null && score >= DEFAULT_THRESHOLD;
        }

        int centerX() {
            return bounds == null ? -1 : bounds.centerX();
        }

        int centerY() {
            return bounds == null ? -1 : bounds.centerY();
        }
    }

    static final class GrayMatch {
        final double score;
        final double centerX;

        GrayMatch(double score, double centerX) {
            this.score = score;
            this.centerX = centerX;
        }
    }

    private final AssetManager assets;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, Mat> cache = new HashMap<>();
    private volatile boolean closed;

    WudangTemplateMatcher(AssetManager assets) {
        this.assets = assets;
        if (!OpenCVLoader.initLocal()) {
            throw new IllegalStateException("OpenCV AAR 初始化失败");
        }
    }

    void matchAsync(Bitmap screenshot, Template[] templates,
            int left, int top, int right, int bottom,
            Consumer<Map<Template, Match>> success, Consumer<Throwable> failure) {
        if (closed) {
            recycle(screenshot);
            failure.accept(new IllegalStateException("模板匹配器已关闭"));
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    EnumMap<Template, Match> matches = match(
                            screenshot, templates, left, top, right, bottom);
                    if (!closed) {
                        success.accept(matches);
                    }
                } catch (Throwable error) {
                    if (!closed) {
                        failure.accept(error);
                    }
                } finally {
                    if (screenshot != null && !screenshot.isRecycled()) {
                        screenshot.recycle();
                    }
                }
            });
        } catch (RejectedExecutionException error) {
            recycle(screenshot);
            failure.accept(error);
        }
    }

    private EnumMap<Template, Match> match(Bitmap screenshot, Template[] templates,
            int left, int top, int right, int bottom) throws IOException {
        EnumMap<Template, Match> result = new EnumMap<>(Template.class);
        long started = System.nanoTime();
        Mat color = new Mat();
        Mat gray = new Mat();
        try {
            if (screenshot == null || screenshot.isRecycled()
                    || screenshot.getWidth() <= 0 || screenshot.getHeight() <= 0) {
                throw new IOException("模板匹配截图为空");
            }
            Utils.bitmapToMat(screenshot, color);
            Imgproc.cvtColor(color, gray, Imgproc.COLOR_RGBA2GRAY);
            for (Template template : templates) {
                result.put(template, matchOne(gray, screenshot, template,
                        left, top, right, bottom, started));
            }
            return result;
        } finally {
            gray.release();
            color.release();
        }
    }

    private Match matchOne(Mat source, Bitmap screenshot, Template template,
            int left, int top, int right, int bottom, long started) throws IOException {
        double scaleX = screenshot.getWidth() / (double) REFERENCE_WIDTH;
        double scaleY = screenshot.getHeight() / (double) REFERENCE_HEIGHT;
        int scaledLeft = clamp((int) Math.round(left * scaleX), 0, source.cols());
        int scaledTop = clamp((int) Math.round(top * scaleY), 0, source.rows());
        int scaledRight = clamp((int) Math.round(right * scaleX), scaledLeft, source.cols());
        int scaledBottom = clamp((int) Math.round(bottom * scaleY), scaledTop, source.rows());
        if (scaledRight <= scaledLeft || scaledBottom <= scaledTop) {
            return new Match(template, "", -1.0, null, elapsedMs(started));
        }
        Mat search = source.submat(new org.opencv.core.Rect(
                scaledLeft, scaledTop, scaledRight - scaledLeft, scaledBottom - scaledTop));
        double bestScore = -1.0;
        Rect bestBounds = null;
        String bestAsset = "";
        try {
            for (String asset : template.assets) {
                Mat original = template(asset);
                Mat resized = new Mat();
                Mat match = new Mat();
                try {
                    Imgproc.resize(original, resized, new Size(
                            Math.max(1, Math.round(original.cols() * scaleX)),
                            Math.max(1, Math.round(original.rows() * scaleY))));
                    if (search.cols() < resized.cols() || search.rows() < resized.rows()) {
                        continue;
                    }
                    Imgproc.matchTemplate(search, resized, match, Imgproc.TM_CCOEFF_NORMED);
                    Core.MinMaxLocResult best = Core.minMaxLoc(match);
                    if (best.maxVal > bestScore) {
                        bestScore = best.maxVal;
                        int matchLeft = scaledLeft + (int) Math.round(best.maxLoc.x);
                        int matchTop = scaledTop + (int) Math.round(best.maxLoc.y);
                        bestBounds = new Rect(
                                matchLeft,
                                matchTop,
                                matchLeft + resized.cols(),
                                matchTop + resized.rows());
                        bestAsset = asset;
                    }
                } finally {
                    match.release();
                    resized.release();
                }
            }
            return new Match(template, bestAsset, bestScore, bestBounds, elapsedMs(started));
        } finally {
            search.release();
        }
    }

    private Mat template(String asset) throws IOException {
        synchronized (cache) {
            Mat cached = cache.get(asset);
            if (cached != null) {
                return cached;
            }
            Bitmap bitmap;
            try (InputStream stream = assets.open(asset)) {
                bitmap = BitmapFactory.decodeStream(stream);
            }
            if (bitmap == null) {
                throw new IOException("无法读取 Wudang 模板: " + asset);
            }
            Mat color = new Mat();
            Mat gray = new Mat();
            try {
                Utils.bitmapToMat(bitmap, color);
                Imgproc.cvtColor(color, gray, Imgproc.COLOR_RGBA2GRAY);
                cache.put(asset, gray);
                return gray;
            } finally {
                bitmap.recycle();
                color.release();
            }
        }
    }

    static Mat loadGrayTemplate(AssetManager assets, String asset) throws IOException {
        Bitmap bitmap;
        try (InputStream stream = assets.open(asset)) {
            bitmap = BitmapFactory.decodeStream(stream);
        }
        if (bitmap == null) {
            throw new IOException("无法读取 Wudang 模板: " + asset);
        }
        Mat color = new Mat();
        Mat gray = new Mat();
        try {
            Utils.bitmapToMat(bitmap, color);
            Imgproc.cvtColor(color, gray, Imgproc.COLOR_RGBA2GRAY);
            return gray;
        } finally {
            bitmap.recycle();
            color.release();
        }
    }

    static GrayMatch matchGray(Mat source, Mat originalTemplate,
            int left, int top, int right, int bottom,
            double scaleX, double scaleY) {
        int scaledLeft = clamp((int) Math.round(left * scaleX), 0, source.cols());
        int scaledTop = clamp((int) Math.round(top * scaleY), 0, source.rows());
        int scaledRight = clamp((int) Math.round(right * scaleX), scaledLeft, source.cols());
        int scaledBottom = clamp((int) Math.round(bottom * scaleY), scaledTop, source.rows());
        Mat search = source.submat(new org.opencv.core.Rect(
                scaledLeft, scaledTop, scaledRight - scaledLeft,
                scaledBottom - scaledTop));
        Mat template = new Mat();
        Mat result = new Mat();
        try {
            Imgproc.resize(originalTemplate, template, new Size(
                    Math.max(1, Math.round(originalTemplate.cols() * scaleX)),
                    Math.max(1, Math.round(originalTemplate.rows() * scaleY))));
            if (search.cols() < template.cols() || search.rows() < template.rows()) {
                return new GrayMatch(-1.0, -1.0);
            }
            Imgproc.matchTemplate(search, template, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult best = Core.minMaxLoc(result);
            double centerX = (scaledLeft + best.maxLoc.x + template.cols() / 2.0) / scaleX;
            return new GrayMatch(best.maxVal, centerX);
        } finally {
            search.release();
            template.release();
            result.release();
        }
    }

    void close() {
        closed = true;
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        synchronized (cache) {
            for (Mat value : cache.values()) {
                value.release();
            }
            cache.clear();
        }
    }

    private static long elapsedMs(long started) {
        return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }

    private static void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
