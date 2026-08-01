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
            checkExactAndBlankMatches();
            result.putString("result", "ok");
            finish(0, result);
        } catch (Throwable error) {
            result.putString("result", error.toString());
            finish(1, result);
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

    private static Bitmap decode(AssetManager assets, String path) throws Exception {
        try (InputStream stream = assets.open(path)) {
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            require(bitmap != null, "无法解码 " + path);
            return bitmap;
        }
    }

    private static WudangTemplateMatcher.Match awaitMatch(
            WudangTemplateMatcher matcher, Bitmap screenshot) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<WudangTemplateMatcher.Template, WudangTemplateMatcher.Match>> result =
                new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        matcher.matchAsync(screenshot,
                new WudangTemplateMatcher.Template[] {
                        WudangTemplateMatcher.Template.MAP_TAB
                }, 400, 535, 860, 635,
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
        return result.get().get(WudangTemplateMatcher.Template.MAP_TAB);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
