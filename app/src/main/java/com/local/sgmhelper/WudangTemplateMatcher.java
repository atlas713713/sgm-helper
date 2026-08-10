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
    static final int LEGION_TITLE_LEFT = 570;
    static final int LEGION_TITLE_TOP = 27;
    static final int LEGION_TITLE_WIDTH = 150;
    static final int LEGION_TITLE_HEIGHT = 34;
    static final int LINE_INFO_LEFT = 590;
    static final int LINE_INFO_TOP = 67;
    static final int LINE_INFO_WIDTH = 100;
    static final int LINE_INFO_HEIGHT = 23;
    // RecycleDialog.titleItem from Wudang ScreenAdapterHelper (screenSize == 1).
    static final int YUANBAO_RECYCLE_TITLE_LEFT = 540;
    static final int YUANBAO_RECYCLE_TITLE_TOP = 27;
    static final int YUANBAO_RECYCLE_TITLE_WIDTH = 200;
    static final int YUANBAO_RECYCLE_TITLE_HEIGHT = 38;
    // Soldier.titleItem from Wudang Soldier (screenSize != 1).
    static final int SOLDIER_TITLE_LEFT = 540;
    static final int SOLDIER_TITLE_TOP = 32;
    static final int SOLDIER_TITLE_WIDTH = 200;
    static final int SOLDIER_TITLE_HEIGHT = 35;
    // Store.titleItem from Wudang Store (both layout branches use this box).
    static final int SHOP_TITLE_LEFT = 580;
    static final int SHOP_TITLE_TOP = 27;
    static final int SHOP_TITLE_WIDTH = 120;
    static final int SHOP_TITLE_HEIGHT = 38;
    // MilitaryCamp.titleItem from Wudang MilitaryCamp (screenSize != 1).
    static final int MILITARY_CAMP_TITLE_LEFT = 580;
    static final int MILITARY_CAMP_TITLE_TOP = 25;
    static final int MILITARY_CAMP_TITLE_WIDTH = 120;
    static final int MILITARY_CAMP_TITLE_HEIGHT = 40;
    // SkillDialog.titleItem from Wudang SkillDialog (screenSize != 1).
    static final int SKILL_TITLE_LEFT = 560;
    static final int SKILL_TITLE_TOP = 30;
    static final int SKILL_TITLE_WIDTH = 160;
    static final int SKILL_TITLE_HEIGHT = 33;
    // WildVipDialog.titleItem from Wudang (5745/gameServerType=2, non-Asia).
    static final int WILD_VIP_TITLE_LEFT = 580;
    static final int WILD_VIP_TITLE_TOP = 28;
    static final int WILD_VIP_TITLE_WIDTH = 120;
    static final int WILD_VIP_TITLE_HEIGHT = 38;
    // MallPage.Czbj.itemLqBtnArea from Wudang (5745/gameServerType=2, non-Asia).
    // p.wd is the monthly-card/mall "领取" glyph, not the generic welfare button.
    static final int MONTH_CARD_CLAIM_LEFT = 320;
    static final int MONTH_CARD_CLAIM_TOP = 592;
    static final int MONTH_CARD_CLAIM_WIDTH = 880;
    static final int MONTH_CARD_CLAIM_HEIGHT = 45;
    static final int AUTO_PATH_LEFT = 1114;
    static final int AUTO_PATH_TOP = 479;
    static final int AUTO_PATH_WIDTH = 32;
    static final int AUTO_PATH_HEIGHT = 32;
    static final int LEGION_MENU_LEFT = 944;
    static final int LEGION_MENU_TOP = 194;
    static final int LEGION_MENU_WIDTH = 98;
    static final int LEGION_MENU_HEIGHT = 93;
    // Line.lineSpinnerAreaItem from Wudang Line (5745/gameServerType=2).
    static final int LINE_SPINNER_LEFT = 1249;
    static final int LINE_SPINNER_TOP = 698;
    static final int LINE_SPINNER_WIDTH = 14;
    static final int LINE_SPINNER_HEIGHT = 13;
    // StartPage web-view close hit areas from the original Wudang implementation.
    static final int WEB_CLOSE_CN_LEFT = 1140;
    static final int WEB_CLOSE_CN_TOP = 92;
    static final int WEB_CLOSE_CN_WIDTH = 40;
    static final int WEB_CLOSE_CN_HEIGHT = 40;
    static final int WEB_CLOSE_ASIA1_LEFT = 1048;
    static final int WEB_CLOSE_ASIA1_TOP = 45;
    static final int WEB_CLOSE_ASIA1_WIDTH = 30;
    static final int WEB_CLOSE_ASIA1_HEIGHT = 30;
    static final int WEB_CLOSE_ASIA2_LEFT = 970;
    static final int WEB_CLOSE_ASIA2_TOP = 60;
    static final int WEB_CLOSE_ASIA2_WIDTH = 40;
    static final int WEB_CLOSE_ASIA2_HEIGHT = 40;
    static final int WEB_CLOSE_ASIA3_LEFT = 1017;
    static final int WEB_CLOSE_ASIA3_TOP = 51;
    static final int WEB_CLOSE_ASIA3_WIDTH = 34;
    static final int WEB_CLOSE_ASIA3_HEIGHT = 34;
    // AsiaLoginPage.switchLogin from the original Wudang 1280x720 layout.
    // The l1/l2 glyphs are smaller than this ItemView; matching searches the
    // original-size glyph inside this exact, fixed color ROI.
    static final int LOGIN_SWITCH_LEFT = 550;
    static final int LOGIN_SWITCH_TOP = 482;
    static final int LOGIN_SWITCH_WIDTH = 180;
    static final int LOGIN_SWITCH_HEIGHT = 50;

    static final class FixedRegion {
        final int left;
        final int top;
        final int width;
        final int height;

        FixedRegion(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }

    enum Template {
        MAP_TAB("地图", "data/m.wd"),
        DIALOG_TAB("对话", "data/n.wd"),
        AUTO_PATH("自动寻路", new FixedRegion(AUTO_PATH_LEFT, AUTO_PATH_TOP,
                AUTO_PATH_WIDTH, AUTO_PATH_HEIGHT), "data/g04.wd"),
        YUANBAO_RECYCLE("元宝回收", new FixedRegion(YUANBAO_RECYCLE_TITLE_LEFT,
                YUANBAO_RECYCLE_TITLE_TOP, YUANBAO_RECYCLE_TITLE_WIDTH,
                YUANBAO_RECYCLE_TITLE_HEIGHT), "data/t1.wd"),
        SOLDIER("士兵和阵形", new FixedRegion(SOLDIER_TITLE_LEFT, SOLDIER_TITLE_TOP,
                SOLDIER_TITLE_WIDTH, SOLDIER_TITLE_HEIGHT), "data/t2.wd", "data/t2_a.wd"),
        SHOP("商店", new FixedRegion(SHOP_TITLE_LEFT, SHOP_TITLE_TOP,
                SHOP_TITLE_WIDTH, SHOP_TITLE_HEIGHT), "data/t3.wd", "data/t3_a.wd"),
        MILITARY_CAMP("军营", new FixedRegion(MILITARY_CAMP_TITLE_LEFT,
                MILITARY_CAMP_TITLE_TOP, MILITARY_CAMP_TITLE_WIDTH,
                MILITARY_CAMP_TITLE_HEIGHT), "data/t4.wd", "data/t4_a.wd",
                // 5755 uses the same 1280x720 ROI but a different title-bar
                // raster; keep the original assets and add the real-device
                // positive sample instead of lowering the shared threshold.
                "data/t4_5755.wd"),
        SKILL("技能", new FixedRegion(SKILL_TITLE_LEFT, SKILL_TITLE_TOP,
                SKILL_TITLE_WIDTH, SKILL_TITLE_HEIGHT), "data/t5.wd", "data/t5_a.wd"),
        INN("客栈", new FixedRegion(580, 28, 120, 35), "data/t6.wd", "data/t6_a.wd"),
        LINE_INFO("分流信息", new FixedRegion(LINE_INFO_LEFT, LINE_INFO_TOP,
                LINE_INFO_WIDTH, LINE_INFO_HEIGHT), "data/t7.wd", "data/t7_a.wd"),
        LEGION("军团", new FixedRegion(LEGION_TITLE_LEFT, LEGION_TITLE_TOP,
                LEGION_TITLE_WIDTH, LEGION_TITLE_HEIGHT), "data/t8.wd"),
        WILDERNESS_TRAINING("荒野修炼", "data/wilderness_training.wd"),
        FRIEND("好友", new FixedRegion(601, 19, 120, 33), "data/t9.wd", "data/t9_a.wd"),
        LEGION_MENU("军团入口", new FixedRegion(LEGION_MENU_LEFT, LEGION_MENU_TOP,
                LEGION_MENU_WIDTH, LEGION_MENU_HEIGHT), "data/mjt.wd"),
        LINE_SPINNER("线路下拉", new FixedRegion(LINE_SPINNER_LEFT, LINE_SPINNER_TOP,
                LINE_SPINNER_WIDTH, LINE_SPINNER_HEIGHT), "data/ls.wd"),
        WEB_VIEW_CLOSE_CN("网页关闭", new FixedRegion(WEB_CLOSE_CN_LEFT, WEB_CLOSE_CN_TOP,
                WEB_CLOSE_CN_WIDTH, WEB_CLOSE_CN_HEIGHT), "data/gg1.wd"),
        WEB_VIEW_CLOSE_ASIA1("网页关闭样式1", new FixedRegion(WEB_CLOSE_ASIA1_LEFT,
                WEB_CLOSE_ASIA1_TOP, WEB_CLOSE_ASIA1_WIDTH, WEB_CLOSE_ASIA1_HEIGHT),
                "data/gg2.wd"),
        WEB_VIEW_CLOSE_ASIA2("网页关闭样式2", new FixedRegion(WEB_CLOSE_ASIA2_LEFT,
                WEB_CLOSE_ASIA2_TOP, WEB_CLOSE_ASIA2_WIDTH, WEB_CLOSE_ASIA2_HEIGHT),
                "data/gg3.wd"),
        WEB_VIEW_CLOSE_ASIA3("网页关闭样式3", new FixedRegion(WEB_CLOSE_ASIA3_LEFT,
                WEB_CLOSE_ASIA3_TOP, WEB_CLOSE_ASIA3_WIDTH, WEB_CLOSE_ASIA3_HEIGHT),
                "data/gg4.wd"),
        LOGIN_QUICK("快捷登录", new FixedRegion(LOGIN_SWITCH_LEFT, LOGIN_SWITCH_TOP,
                LOGIN_SWITCH_WIDTH, LOGIN_SWITCH_HEIGHT), "data/l1.wd"),
        LOGIN_ACCOUNT("账号登录", new FixedRegion(LOGIN_SWITCH_LEFT, LOGIN_SWITCH_TOP,
                LOGIN_SWITCH_WIDTH, LOGIN_SWITCH_HEIGHT), "data/l2.wd"),
        PALACE("宫殿", new FixedRegion(590, 30, 100, 33), "data/t10.wd", "data/t10_a.wd"),
        DUNGEON("副本", new FixedRegion(600, 27, 80, 38), "data/t11.wd", "data/t11_a.wd"),
        WILD_VIP("秘境", new FixedRegion(WILD_VIP_TITLE_LEFT, WILD_VIP_TITLE_TOP,
                WILD_VIP_TITLE_WIDTH, WILD_VIP_TITLE_HEIGHT), "data/v2.wd", "data/v2_a.wd"),
        REFINE("炼造", new FixedRegion(600, 28, 80, 36), "data/t12.wd", "data/t12_a.wd"),
        WAR_SOUL("武魂擂台", new FixedRegion(570, 16, 140, 34), "data/t13.wd", "data/t13_a.wd"),
        AUTO_FUNCTION("自动功能", new FixedRegion(570, 6, 710, 38), "data/t14.wd", "data/t14_a.wd"),
        REWARD_RECOVERY("奖励找回", new FixedRegion(540, 27, 200, 36), "data/t15.wd", "data/t15_a.wd"),
        BANK("钱庄", new FixedRegion(580, 16, 120, 40), "data/t16.wd", "data/t16_a.wd"),
        LOOT("战利品", new FixedRegion(600, 114, 80, 20), "data/t17.wd", "data/t17_a.wd"),
        WAR("战役", new FixedRegion(560, 27, 160, 40), "data/t18.wd", "data/t18_a.wd"),
        // LegionDialog.challengeDialogTitleArea, non-Asia branch (5745/gameServerType=2).
        JIANG_YUAN("名将挑战", new FixedRegion(570, 25, 160, 37), "data/t19.wd", "data/t19_a.wd"),
        HISTORIC_WAR("历史战场", new FixedRegion(560, 27, 160, 40), "data/t20.wd", "data/t20_a.wd"),
        QIANLI("千里单骑", new FixedRegion(560, 26, 160, 40), "data/t21.wd", "data/t21_a.wd"),
        MONTH_CARD_CLAIM("月卡领取", new FixedRegion(MONTH_CARD_CLAIM_LEFT,
                MONTH_CARD_CLAIM_TOP, MONTH_CARD_CLAIM_WIDTH, MONTH_CARD_CLAIM_HEIGHT),
                "data/p.wd", "data/p_a.wd"),
        ONE_CLICK_CLAIM("一键领取", "data/bf2.wd", "data/bf2_a.wd"),
        CLAIM("领取", "data/bf3.wd", "data/bf3_a.wd",
                "data/bf4.wd", "data/bf4_a.wd",
                "data/bf5.wd", "data/bf5_a.wd"),
        GO_CLAIM("前往领取", "data/bf4.wd", "data/bf4_a.wd");

        final String label;
        final String[] assets;
        final FixedRegion fixedRegion;

        Template(String label, String... assets) {
            this.label = label;
            this.fixedRegion = null;
            this.assets = assets;
        }

        Template(String label, FixedRegion fixedRegion, String... assets) {
            this.label = label;
            this.fixedRegion = fixedRegion;
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
            return bounds != null
                    && (template.fixedRegion != null
                            ? score > DEFAULT_THRESHOLD
                            : score >= DEFAULT_THRESHOLD);
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
    private final Map<String, Mat> colorCache = new HashMap<>();
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
                Match match;
                if (template.fixedRegion != null) {
                    FixedRegion region = template.fixedRegion;
                    match = matchFixedColorTemplate(color, screenshot, template,
                            left, top, right, bottom, started,
                            region.left, region.top, region.width, region.height);
                } else {
                    match = matchOne(gray, screenshot, template,
                            left, top, right, bottom, started);
                }
                result.put(template, match);
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

    private Match matchFixedColorTemplate(Mat source, Bitmap screenshot, Template template,
            int left, int top, int right, int bottom, long started,
            int fixedLeft, int fixedTop, int fixedWidth, int fixedHeight) throws IOException {
        if (screenshot.getWidth() != REFERENCE_WIDTH
                || screenshot.getHeight() != REFERENCE_HEIGHT
                || left != fixedLeft
                || top != fixedTop
                || right != fixedLeft + fixedWidth
                || bottom != fixedTop + fixedHeight) {
            return new Match(template, "", -1.0, null, elapsedMs(started));
        }
        Mat search = source.submat(new org.opencv.core.Rect(
                fixedLeft, fixedTop, fixedWidth, fixedHeight));
        double bestScore = -1.0;
        Rect bestBounds = null;
        String bestAsset = "";
        try {
            for (String asset : template.assets) {
                Mat original = colorTemplate(asset);
                Mat match = new Mat();
                try {
                    if (search.cols() < original.cols() || search.rows() < original.rows()) {
                        continue;
                    }
                    Imgproc.matchTemplate(search, original, match, Imgproc.TM_CCOEFF_NORMED);
                    Core.MinMaxLocResult best = Core.minMaxLoc(match);
                    if (best.maxVal > bestScore) {
                        bestScore = best.maxVal;
                        int matchLeft = fixedLeft + (int) Math.round(best.maxLoc.x);
                        int matchTop = fixedTop + (int) Math.round(best.maxLoc.y);
                        bestBounds = new Rect(
                                matchLeft,
                                matchTop,
                                matchLeft + original.cols(),
                                matchTop + original.rows());
                        bestAsset = asset;
                    }
                } finally {
                    match.release();
                }
            }
            return new Match(template, bestAsset, bestScore,
                    bestBounds, elapsedMs(started));
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

    private Mat colorTemplate(String asset) throws IOException {
        synchronized (colorCache) {
            Mat cached = colorCache.get(asset);
            if (cached != null) {
                return cached;
            }
            Bitmap bitmap;
            try (InputStream stream = assets.open(asset)) {
                bitmap = BitmapFactory.decodeStream(stream);
            }
            if (bitmap == null) {
                throw new IOException("无法读取 Wudang 彩色模板: " + asset);
            }
            Mat color = new Mat();
            try {
                Utils.bitmapToMat(bitmap, color);
                colorCache.put(asset, color);
                return color;
            } finally {
                bitmap.recycle();
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
        synchronized (colorCache) {
            for (Mat value : colorCache.values()) {
                value.release();
            }
            colorCache.clear();
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
