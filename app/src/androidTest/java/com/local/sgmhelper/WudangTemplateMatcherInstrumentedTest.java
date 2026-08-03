package com.local.sgmhelper;

import android.app.Instrumentation;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Dependency-free device checks for the real APK assets and OpenCV matcher. */
public final class WudangTemplateMatcherInstrumentedTest extends Instrumentation {
    @Override
    public void onStart() {
        super.onStart();
        Bundle result = new Bundle();
        try {
            checkSelectedAssetsDecode();
            checkAllFixedCatalogMatches();
            checkExactAndBlankMatches();
            checkNativeColorYuanbaoRecycleMatch();
            checkNativeColorSoldierMatch();
            checkNativeColorShopMatch();
            checkNativeColorMilitaryCampMatch();
            checkNativeColorSkillMatch();
            checkNativeColorLegionMatch();
            checkNativeColorAutoPathMatch();
            checkNativeColorLegionMenuMatch();
            result.putString("result", "ok");
            finish(0, result);
        } catch (Throwable error) {
            result.putString("result", error.toString());
            finish(1, result);
        }
    }

    private void checkNativeColorYuanbaoRecycleMatch() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        Bitmap template = decode(assets, WudangTemplateMatcher.Template.YUANBAO_RECYCLE.assets[0]);
        WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
        try {
            require(template.getWidth() == 131 && template.getHeight() == 33,
                    "t1 尺寸不是 131x33");
            Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            new Canvas(exact).drawBitmap(template,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_LEFT + 35,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_TOP + 3,
                    null);
            WudangTemplateMatcher.Match exactMatch = awaitMatch(matcher, exact,
                    WudangTemplateMatcher.Template.YUANBAO_RECYCLE,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_LEFT,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_TOP,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_LEFT
                            + WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_WIDTH,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_TOP
                            + WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_HEIGHT);
            require(exactMatch.found() && exactMatch.score > 0.95,
                    "t1 彩色原尺寸匹配失败: " + exactMatch.score);

            Bitmap blank = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            WudangTemplateMatcher.Match blankMatch = awaitMatch(matcher, blank,
                    WudangTemplateMatcher.Template.YUANBAO_RECYCLE,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_LEFT,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_TOP,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_LEFT
                            + WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_WIDTH,
                    WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_TOP
                            + WudangTemplateMatcher.YUANBAO_RECYCLE_TITLE_HEIGHT);
            require(Double.isNaN(blankMatch.score)
                            || blankMatch.score <= WudangTemplateMatcher.DEFAULT_THRESHOLD,
                    "t1 空白截图误命中: " + blankMatch.score);
        } finally {
            template.recycle();
            matcher.close();
        }
    }

    private void checkNativeColorSoldierMatch() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        Bitmap template = decode(assets, WudangTemplateMatcher.Template.SOLDIER.assets[0]);
        WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
        try {
            require(template.getWidth() == 174 && template.getHeight() == 34,
                    "t2 尺寸不是 174x34");
            Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            new Canvas(exact).drawBitmap(template,
                    WudangTemplateMatcher.SOLDIER_TITLE_LEFT + 18,
                    WudangTemplateMatcher.SOLDIER_TITLE_TOP + 1,
                    null);
            WudangTemplateMatcher.Match exactMatch = awaitMatch(matcher, exact,
                    WudangTemplateMatcher.Template.SOLDIER,
                    WudangTemplateMatcher.SOLDIER_TITLE_LEFT,
                    WudangTemplateMatcher.SOLDIER_TITLE_TOP,
                    WudangTemplateMatcher.SOLDIER_TITLE_LEFT
                            + WudangTemplateMatcher.SOLDIER_TITLE_WIDTH,
                    WudangTemplateMatcher.SOLDIER_TITLE_TOP
                            + WudangTemplateMatcher.SOLDIER_TITLE_HEIGHT);
            require(exactMatch.found() && exactMatch.score > 0.95,
                    "t2 彩色原尺寸匹配失败: " + exactMatch.score);

            Bitmap blank = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            WudangTemplateMatcher.Match blankMatch = awaitMatch(matcher, blank,
                    WudangTemplateMatcher.Template.SOLDIER,
                    WudangTemplateMatcher.SOLDIER_TITLE_LEFT,
                    WudangTemplateMatcher.SOLDIER_TITLE_TOP,
                    WudangTemplateMatcher.SOLDIER_TITLE_LEFT
                            + WudangTemplateMatcher.SOLDIER_TITLE_WIDTH,
                    WudangTemplateMatcher.SOLDIER_TITLE_TOP
                            + WudangTemplateMatcher.SOLDIER_TITLE_HEIGHT);
            require(Double.isNaN(blankMatch.score)
                            || blankMatch.score <= WudangTemplateMatcher.DEFAULT_THRESHOLD,
                    "t2 空白截图误命中: " + blankMatch.score);
        } finally {
            template.recycle();
            matcher.close();
        }
    }

    private void checkNativeColorShopMatch() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        Bitmap template = decode(assets, WudangTemplateMatcher.Template.SHOP.assets[0]);
        WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
        try {
            require(template.getWidth() == 64 && template.getHeight() == 33,
                    "t3 尺寸不是 64x33");
            Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            new Canvas(exact).drawBitmap(template,
                    WudangTemplateMatcher.SHOP_TITLE_LEFT + 28,
                    WudangTemplateMatcher.SHOP_TITLE_TOP + 2,
                    null);
            WudangTemplateMatcher.Match exactMatch = awaitMatch(matcher, exact,
                    WudangTemplateMatcher.Template.SHOP,
                    WudangTemplateMatcher.SHOP_TITLE_LEFT,
                    WudangTemplateMatcher.SHOP_TITLE_TOP,
                    WudangTemplateMatcher.SHOP_TITLE_LEFT
                            + WudangTemplateMatcher.SHOP_TITLE_WIDTH,
                    WudangTemplateMatcher.SHOP_TITLE_TOP
                            + WudangTemplateMatcher.SHOP_TITLE_HEIGHT);
            require(exactMatch.found() && exactMatch.score > 0.95,
                    "t3 彩色原尺寸匹配失败: " + exactMatch.score);

            Bitmap blank = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            WudangTemplateMatcher.Match blankMatch = awaitMatch(matcher, blank,
                    WudangTemplateMatcher.Template.SHOP,
                    WudangTemplateMatcher.SHOP_TITLE_LEFT,
                    WudangTemplateMatcher.SHOP_TITLE_TOP,
                    WudangTemplateMatcher.SHOP_TITLE_LEFT
                            + WudangTemplateMatcher.SHOP_TITLE_WIDTH,
                    WudangTemplateMatcher.SHOP_TITLE_TOP
                            + WudangTemplateMatcher.SHOP_TITLE_HEIGHT);
            require(Double.isNaN(blankMatch.score)
                            || blankMatch.score <= WudangTemplateMatcher.DEFAULT_THRESHOLD,
                    "t3 空白截图误命中: " + blankMatch.score);
        } finally {
            template.recycle();
            matcher.close();
        }
    }

    private void checkNativeColorMilitaryCampMatch() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        Bitmap template = decode(assets,
                WudangTemplateMatcher.Template.MILITARY_CAMP.assets[0]);
        WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
        try {
            require(template.getWidth() == 63 && template.getHeight() == 32,
                    "t4 尺寸不是 63x32");
            Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            new Canvas(exact).drawBitmap(template,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_LEFT + 28,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_TOP + 4,
                    null);
            WudangTemplateMatcher.Match exactMatch = awaitMatch(matcher, exact,
                    WudangTemplateMatcher.Template.MILITARY_CAMP,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_LEFT,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_TOP,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_LEFT
                            + WudangTemplateMatcher.MILITARY_CAMP_TITLE_WIDTH,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_TOP
                            + WudangTemplateMatcher.MILITARY_CAMP_TITLE_HEIGHT);
            require(exactMatch.found() && exactMatch.score > 0.95,
                    "t4 彩色原尺寸匹配失败: " + exactMatch.score);

            Bitmap blank = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            WudangTemplateMatcher.Match blankMatch = awaitMatch(matcher, blank,
                    WudangTemplateMatcher.Template.MILITARY_CAMP,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_LEFT,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_TOP,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_LEFT
                            + WudangTemplateMatcher.MILITARY_CAMP_TITLE_WIDTH,
                    WudangTemplateMatcher.MILITARY_CAMP_TITLE_TOP
                            + WudangTemplateMatcher.MILITARY_CAMP_TITLE_HEIGHT);
            require(Double.isNaN(blankMatch.score)
                            || blankMatch.score <= WudangTemplateMatcher.DEFAULT_THRESHOLD,
                    "t4 空白截图误命中: " + blankMatch.score);
        } finally {
            template.recycle();
            matcher.close();
        }
    }

    private void checkNativeColorSkillMatch() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        Bitmap template = decode(assets, WudangTemplateMatcher.Template.SKILL.assets[0]);
        WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
        try {
            require(template.getWidth() == 66 && template.getHeight() == 33,
                    "t5 尺寸不是 66x33");
            Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            new Canvas(exact).drawBitmap(template,
                    WudangTemplateMatcher.SKILL_TITLE_LEFT + 47,
                    WudangTemplateMatcher.SKILL_TITLE_TOP,
                    null);
            WudangTemplateMatcher.Match exactMatch = awaitMatch(matcher, exact,
                    WudangTemplateMatcher.Template.SKILL,
                    WudangTemplateMatcher.SKILL_TITLE_LEFT,
                    WudangTemplateMatcher.SKILL_TITLE_TOP,
                    WudangTemplateMatcher.SKILL_TITLE_LEFT
                            + WudangTemplateMatcher.SKILL_TITLE_WIDTH,
                    WudangTemplateMatcher.SKILL_TITLE_TOP
                            + WudangTemplateMatcher.SKILL_TITLE_HEIGHT);
            require(exactMatch.found() && exactMatch.score > 0.95,
                    "t5 彩色原尺寸匹配失败: " + exactMatch.score);

            Bitmap blank = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            WudangTemplateMatcher.Match blankMatch = awaitMatch(matcher, blank,
                    WudangTemplateMatcher.Template.SKILL,
                    WudangTemplateMatcher.SKILL_TITLE_LEFT,
                    WudangTemplateMatcher.SKILL_TITLE_TOP,
                    WudangTemplateMatcher.SKILL_TITLE_LEFT
                            + WudangTemplateMatcher.SKILL_TITLE_WIDTH,
                    WudangTemplateMatcher.SKILL_TITLE_TOP
                            + WudangTemplateMatcher.SKILL_TITLE_HEIGHT);
            require(Double.isNaN(blankMatch.score)
                            || blankMatch.score <= WudangTemplateMatcher.DEFAULT_THRESHOLD,
                    "t5 空白截图误命中: " + blankMatch.score);
        } finally {
            template.recycle();
            matcher.close();
        }
    }

    private void checkSelectedAssetsDecode() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        for (WudangTemplateMatcher.Template template : WudangTemplateMatcher.Template.values()) {
            for (String asset : template.assets) {
                try (InputStream stream = assets.open(asset)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    require(bitmap != null, "无法解码 " + asset);
                    require(bitmap.getWidth() > 0 && bitmap.getHeight() > 0,
                            "模板尺寸为空 " + asset);
                    bitmap.recycle();
                }
            }
        }
    }

    private void checkAllFixedCatalogMatches() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        for (WudangTemplateMatcher.Template template
                : WudangTemplateMatcher.Template.values()) {
            WudangTemplateMatcher.FixedRegion region = template.fixedRegion;
            if (region == null) {
                continue;
            }
            Bitmap source = decode(assets, template.assets[0]);
            WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
            try {
                Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
                new Canvas(exact).drawBitmap(source,
                        region.left + (region.width - source.getWidth()) / 2,
                        region.top + (region.height - source.getHeight()) / 2,
                        null);
                WudangTemplateMatcher.Match match = awaitMatch(matcher, exact, template,
                        region.left, region.top,
                        region.left + region.width, region.top + region.height);
                require(match.found() && match.score > 0.95,
                        template.label + " 固定 ROI 彩色匹配失败: " + match.score);
                exact.recycle();
            } finally {
                source.recycle();
                matcher.close();
            }
        }
    }

    private void checkExactAndBlankMatches() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        Bitmap template = decode(assets, WudangTemplateMatcher.Template.MAP_TAB.assets[0]);
        WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
        try {
            Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            new Canvas(exact).drawBitmap(template, 620, 570,
                    new Paint(Paint.ANTI_ALIAS_FLAG));
            WudangTemplateMatcher.Match exactMatch = awaitMatch(matcher, exact);
            require(exactMatch.score > 0.95 && exactMatch.found(),
                    "精确模板分数不足: " + exactMatch.score);

            Bitmap blank = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            WudangTemplateMatcher.Match blankMatch = awaitMatch(matcher, blank);
            require(Double.isNaN(blankMatch.score)
                            || blankMatch.score < WudangTemplateMatcher.DEFAULT_THRESHOLD,
                    "空白截图误命中: " + blankMatch.score);
        } finally {
            template.recycle();
            matcher.close();
        }
    }

    private void checkNativeColorLegionMatch() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        Bitmap template = decode(assets, WudangTemplateMatcher.Template.LEGION.assets[0]);
        WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
        try {
            require(template.getWidth() == 64 && template.getHeight() == 31,
                    "t8 尺寸不是 64x31");
            Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            new Canvas(exact).drawBitmap(template,
                    WudangTemplateMatcher.LEGION_TITLE_LEFT + 46,
                    WudangTemplateMatcher.LEGION_TITLE_TOP + 2,
                    null);
            WudangTemplateMatcher.Match exactMatch = awaitMatch(matcher, exact,
                    WudangTemplateMatcher.Template.LEGION,
                    WudangTemplateMatcher.LEGION_TITLE_LEFT,
                    WudangTemplateMatcher.LEGION_TITLE_TOP,
                    WudangTemplateMatcher.LEGION_TITLE_LEFT
                            + WudangTemplateMatcher.LEGION_TITLE_WIDTH,
                    WudangTemplateMatcher.LEGION_TITLE_TOP
                            + WudangTemplateMatcher.LEGION_TITLE_HEIGHT);
            require(exactMatch.found() && exactMatch.score > 0.95,
                    "t8 彩色原尺寸匹配失败: " + exactMatch.score);

            Bitmap blank = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            WudangTemplateMatcher.Match blankMatch = awaitMatch(matcher, blank,
                    WudangTemplateMatcher.Template.LEGION,
                    WudangTemplateMatcher.LEGION_TITLE_LEFT,
                    WudangTemplateMatcher.LEGION_TITLE_TOP,
                    WudangTemplateMatcher.LEGION_TITLE_LEFT
                            + WudangTemplateMatcher.LEGION_TITLE_WIDTH,
                    WudangTemplateMatcher.LEGION_TITLE_TOP
                            + WudangTemplateMatcher.LEGION_TITLE_HEIGHT);
            require(Double.isNaN(blankMatch.score)
                            || blankMatch.score <= WudangTemplateMatcher.DEFAULT_THRESHOLD,
                    "t8 空白截图误命中: " + blankMatch.score);
        } finally {
            template.recycle();
            matcher.close();
        }
    }

    private void checkNativeColorAutoPathMatch() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        Bitmap template = decode(assets, WudangTemplateMatcher.Template.AUTO_PATH.assets[0]);
        WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
        try {
            require(template.getWidth() == 25 && template.getHeight() == 25,
                    "g04 尺寸不是 25x25");
            Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            new Canvas(exact).drawBitmap(template,
                    WudangTemplateMatcher.AUTO_PATH_LEFT + 4,
                    WudangTemplateMatcher.AUTO_PATH_TOP + 4,
                    null);
            WudangTemplateMatcher.Match exactMatch = awaitMatch(matcher, exact,
                    WudangTemplateMatcher.Template.AUTO_PATH,
                    WudangTemplateMatcher.AUTO_PATH_LEFT,
                    WudangTemplateMatcher.AUTO_PATH_TOP,
                    WudangTemplateMatcher.AUTO_PATH_LEFT
                            + WudangTemplateMatcher.AUTO_PATH_WIDTH,
                    WudangTemplateMatcher.AUTO_PATH_TOP
                            + WudangTemplateMatcher.AUTO_PATH_HEIGHT);
            require(exactMatch.found() && exactMatch.score > 0.95,
                    "g04 彩色原尺寸匹配失败: " + exactMatch.score);

            Bitmap blank = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            WudangTemplateMatcher.Match blankMatch = awaitMatch(matcher, blank,
                    WudangTemplateMatcher.Template.AUTO_PATH,
                    WudangTemplateMatcher.AUTO_PATH_LEFT,
                    WudangTemplateMatcher.AUTO_PATH_TOP,
                    WudangTemplateMatcher.AUTO_PATH_LEFT
                            + WudangTemplateMatcher.AUTO_PATH_WIDTH,
                    WudangTemplateMatcher.AUTO_PATH_TOP
                            + WudangTemplateMatcher.AUTO_PATH_HEIGHT);
            require(Double.isNaN(blankMatch.score)
                            || blankMatch.score <= WudangTemplateMatcher.DEFAULT_THRESHOLD,
                    "g04 空白截图误命中: " + blankMatch.score);
        } finally {
            template.recycle();
            matcher.close();
        }
    }

    private void checkNativeColorLegionMenuMatch() throws Exception {
        AssetManager assets = getTargetContext().getAssets();
        Bitmap template = decode(assets, WudangTemplateMatcher.Template.LEGION_MENU.assets[0]);
        WudangTemplateMatcher matcher = new WudangTemplateMatcher(assets);
        try {
            require(template.getWidth() == 37 && template.getHeight() == 20,
                    "mjt 尺寸不是 37x20");
            Bitmap exact = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            new Canvas(exact).drawBitmap(template,
                    WudangTemplateMatcher.LEGION_MENU_LEFT + 28,
                    WudangTemplateMatcher.LEGION_MENU_TOP + 31,
                    null);
            WudangTemplateMatcher.Match exactMatch = awaitMatch(matcher, exact,
                    WudangTemplateMatcher.Template.LEGION_MENU,
                    WudangTemplateMatcher.LEGION_MENU_LEFT,
                    WudangTemplateMatcher.LEGION_MENU_TOP,
                    WudangTemplateMatcher.LEGION_MENU_LEFT
                            + WudangTemplateMatcher.LEGION_MENU_WIDTH,
                    WudangTemplateMatcher.LEGION_MENU_TOP
                            + WudangTemplateMatcher.LEGION_MENU_HEIGHT);
            require(exactMatch.found() && exactMatch.score > 0.95,
                    "mjt 彩色原尺寸匹配失败: " + exactMatch.score);

            Bitmap blank = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            WudangTemplateMatcher.Match blankMatch = awaitMatch(matcher, blank,
                    WudangTemplateMatcher.Template.LEGION_MENU,
                    WudangTemplateMatcher.LEGION_MENU_LEFT,
                    WudangTemplateMatcher.LEGION_MENU_TOP,
                    WudangTemplateMatcher.LEGION_MENU_LEFT
                            + WudangTemplateMatcher.LEGION_MENU_WIDTH,
                    WudangTemplateMatcher.LEGION_MENU_TOP
                            + WudangTemplateMatcher.LEGION_MENU_HEIGHT);
            require(Double.isNaN(blankMatch.score)
                            || blankMatch.score <= WudangTemplateMatcher.DEFAULT_THRESHOLD,
                    "mjt 空白截图误命中: " + blankMatch.score);
        } finally {
            template.recycle();
            matcher.close();
        }
    }

    private static Bitmap decode(AssetManager assets, String path) throws Exception {
        try (InputStream stream = assets.open(path)) {
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            require(bitmap != null, "无法解码 " + path);
            return bitmap;
        }
    }

    private static WudangTemplateMatcher.Match awaitMatch(
            WudangTemplateMatcher matcher, Bitmap screenshot) throws Exception {
        return awaitMatch(matcher, screenshot,
                WudangTemplateMatcher.Template.MAP_TAB,
                400, 535, 860, 635);
    }

    private static WudangTemplateMatcher.Match awaitMatch(
            WudangTemplateMatcher matcher, Bitmap screenshot,
            WudangTemplateMatcher.Template template,
            int left, int top, int right, int bottom) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<WudangTemplateMatcher.Template, WudangTemplateMatcher.Match>> result =
                new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        matcher.matchAsync(screenshot,
                new WudangTemplateMatcher.Template[] {
                        template
                }, left, top, right, bottom,
                matches -> {
                    result.set(matches);
                    latch.countDown();
                }, failure -> {
                    error.set(failure);
                    latch.countDown();
                });
        require(latch.await(10, TimeUnit.SECONDS), "模板匹配超时");
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        return result.get().get(template);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
