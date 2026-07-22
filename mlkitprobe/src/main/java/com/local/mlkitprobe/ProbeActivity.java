package com.local.mlkitprobe;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class ProbeActivity extends Activity {
    private static final String TAG = "TEMPLATE_PROBE";
    private static final String SCREENSHOT_NAME = "earth_channel_test_raw.png";
    private static final int[][] PORTRAIT_RECTS = {
            {502, 281, 65, 70},
            {604, 281, 65, 70},
            {704, 281, 65, 70}
    };
    private static final int SEARCH_X = 12;
    private static final int SEARCH_Y = 6;
    private static final int[] SEARCH_WIDTHS = {65, 70};

    private final Bitmap[] portraits = new Bitmap[PORTRAIT_RECTS.length];
    private final int[] rotations = new int[PORTRAIT_RECTS.length];
    private final ImageView[] images = new ImageView[PORTRAIT_RECTS.length];
    private final ImageView[] templateViews = new ImageView[PORTRAIT_RECTS.length];
    private final Bitmap[] matchedTemplates = new Bitmap[PORTRAIT_RECTS.length];
    private final TextView[] results = new TextView[PORTRAIT_RECTS.length];
    private Bitmap screenshot;
    private TemplateMatcher matcher;
    private Button detectButton;
    private TextView selfCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            matcher = new TemplateMatcher(getAssets());
            screenshot = decodeScreenshot();
            for (int i = 0; i < PORTRAIT_RECTS.length; i++) {
                int[] rect = PORTRAIT_RECTS[i];
                portraits[i] = Bitmap.createBitmap(screenshot,
                        rect[0], rect[1], rect[2], rect[3]);
            }
            setContentView(buildUi());
        } catch (Exception error) {
            TextView failure = new TextView(this);
            failure.setText("测试初始化失败：" + error.getMessage());
            setContentView(failure);
        }
    }

    private ScrollView buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(24, 16, 24, 16);

        TextView title = new TextView(this);
        title.setText("Wudang 头像模板识别测试");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView help = new TextView(this);
        help.setText("OpenCV " + matcher.openCvVersion()
                + " 地球原始截图：1280×720按Wudang国战分支裁65×70\n"
                + "灰度化→16×16→4方向各扫3958模板");
        help.setTextSize(15);
        help.setGravity(Gravity.CENTER);
        help.setPadding(0, 8, 0, 12);
        root.addView(help, matchWrap());

        selfCheck = new TextView(this);
        selfCheck.setText("模板自校验：尚未运行");
        selfCheck.setTextSize(14);
        selfCheck.setGravity(Gravity.CENTER);
        root.addView(selfCheck, matchWrap());

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        for (int i = 0; i < PORTRAIT_RECTS.length; i++) {
            row.addView(buildCard(i), new LinearLayout.LayoutParams(300, -2));
        }
        root.addView(row, matchWrap());

        detectButton = new Button(this);
        detectButton.setText("运行正确流程");
        detectButton.setTextSize(18);
        detectButton.setOnClickListener(view -> detectAll());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(420, 72);
        buttonParams.topMargin = 16;
        root.addView(detectButton, buttonParams);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private LinearLayout buildCard(int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(8, 0, 8, 0);

        TextView label = new TextView(this);
        label.setText("游戏头像 " + (index + 1));
        label.setTextSize(17);
        label.setGravity(Gravity.CENTER);
        card.addView(label, matchWrap());

        ImageView image = new ImageView(this);
        images[index] = image;
        image.setImageBitmap(portraits[index]);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setOnClickListener(view -> {
            rotations[index] = (rotations[index] + 90) % 360;
            image.setRotation(rotations[index]);
            results[index].setText("当前显示：" + rotations[index] + "°\n尚未识别");
        });
        card.addView(image, new LinearLayout.LayoutParams(220, 220));

        TextView templateLabel = new TextView(this);
        templateLabel.setText("最高分原始模板");
        templateLabel.setGravity(Gravity.CENTER);
        card.addView(templateLabel, matchWrap());

        ImageView templateView = new ImageView(this);
        templateViews[index] = templateView;
        templateView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(templateView, new LinearLayout.LayoutParams(150, 150));

        TextView result = new TextView(this);
        results[index] = result;
        result.setText("当前显示：0°\n尚未识别");
        result.setTextSize(14);
        result.setGravity(Gravity.CENTER);
        card.addView(result, matchWrap());
        return card;
    }

    private void detectAll() {
        detectButton.setEnabled(false);
        detectButton.setText("加载模板并识别中…");
        int[] requestedRotations = rotations.clone();
        new Thread(() -> runDetection(requestedRotations)).start();
    }

    private void runDetection(int[] requestedRotations) {
        long started = System.currentTimeMillis();
        try {
            String[] descriptions = new String[portraits.length];
            Bitmap[] bestImages = new Bitmap[portraits.length];
            Bitmap[] bestCrops = new Bitmap[portraits.length];
            int templateCount = matcher.templateCount();
            Bitmap calibration = matcher.calibrationBitmap();
            String calibrationResult = runCalibration(calibration);
            calibration.recycle();
            for (int i = 0; i < portraits.length; i++) {
                int[] rect = PORTRAIT_RECTS[i];
                TemplateMatcher.Match match = null;
                int bestX = rect[0];
                int bestY = rect[1];
                int bestWidth = rect[2];
                for (int width : SEARCH_WIDTHS) {
                    for (int dy = -SEARCH_Y; dy <= SEARCH_Y; dy++) {
                        for (int dx = -SEARCH_X; dx <= SEARCH_X; dx++) {
                            Bitmap crop = Bitmap.createBitmap(screenshot,
                                    rect[0] + dx, rect[1] + dy, width, rect[3]);
                            Bitmap current = rotatedCopy(crop, requestedRotations[i]);
                            TemplateMatcher.Match candidate = matcher.match(current);
                            current.recycle();
                            crop.recycle();
                            if (match == null || candidate.score > match.score) {
                                match = candidate;
                                bestX = rect[0] + dx;
                                bestY = rect[1] + dy;
                                bestWidth = width;
                            }
                        }
                    }
                }
                bestCrops[i] = Bitmap.createBitmap(screenshot,
                        bestX, bestY, bestWidth, rect[3]);
                bestImages[i] = matcher.templateBitmap(match);
                String decision = match.recognized
                        ? "命中：预测再点 " + match.orientation + " 次"
                        : "未命中（<0.95）：不输出点击方向";
                String inputMode = match.exactInput
                        ? "Wudang国战原始65×70裁图"
                        : "非65×70输入";
                descriptions[i] = String.format(Locale.US,
                        "当前：%d°  %s\n%s\n"
                                + "最佳裁剪=(%d,%d) %dx%d\n"
                                + "再转 0°=%.4f  90°=%.4f\n"
                                + "再转180°=%.4f 270°=%.4f\n最高分=%.4f\n模板=%s",
                        requestedRotations[i], decision, inputMode,
                        bestX, bestY, bestWidth, rect[3],
                        match.orientationScores[0], match.orientationScores[1],
                        match.orientationScores[2], match.orientationScores[3], match.score,
                        match.templateName);
                Log.i(TAG, "tile=" + (i + 1) + " rotation=" + requestedRotations[i]
                        + " taps=" + match.orientation
                        + " template=" + match.templateName
                        + " crop=(" + bestX + "," + bestY + ","
                        + bestWidth + "x" + rect[3] + ")"
                        + " scores=" + java.util.Arrays.toString(match.orientationScores));
            }
            long elapsed = System.currentTimeMillis() - started;
            runOnUiThread(() -> {
                selfCheck.setText(calibrationResult);
                for (int i = 0; i < descriptions.length; i++) {
                    if (matchedTemplates[i] != null) {
                        matchedTemplates[i].recycle();
                    }
                    matchedTemplates[i] = bestImages[i];
                    templateViews[i].setImageBitmap(bestImages[i]);
                    portraits[i].recycle();
                    portraits[i] = bestCrops[i];
                    images[i].setImageBitmap(bestCrops[i]);
                    results[i].setText(descriptions[i]);
                }
                detectButton.setEnabled(true);
                detectButton.setText("重新识别（" + templateCount + " 模板 / "
                        + elapsed + "ms）");
            });
        } catch (Exception error) {
            Log.e(TAG, "template detection failed", error);
            runOnUiThread(() -> {
                detectButton.setEnabled(true);
                detectButton.setText("识别失败：" + error.getMessage());
            });
        }
    }

    private String runCalibration(Bitmap upright) throws IOException {
        StringBuilder text = new StringBuilder("模板四方向自校验：");
        boolean passed = true;
        for (int turn = 0; turn < 4; turn++) {
            Bitmap current = rotatedCopy(upright, turn * 90);
            TemplateMatcher.Match match = matcher.match(current);
            current.recycle();
            boolean itemPassed = match.recognized
                    && match.orientation == TemplateMatcher.expectedCorrection(turn);
            passed &= itemPassed;
            text.append(String.format(Locale.US, "\n%d°→%d次 %.4f %s",
                    turn * 90, match.orientation, match.score, itemPassed ? "PASS" : "FAIL"));
        }
        return (passed ? "PASS\n" : "FAIL\n") + text;
    }

    private Bitmap decodeScreenshot() throws IOException {
        File file = new File(getExternalFilesDir(null), SCREENSHOT_NAME);
        try (InputStream stream = new FileInputStream(file)) {
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            if (bitmap == null) {
                throw new IOException("无法读取测试截图");
            }
            if (bitmap.getWidth() != 1280 || bitmap.getHeight() != 720) {
                bitmap.recycle();
                throw new IOException("测试截图必须是1280×720原始截图");
            }
            return bitmap;
        }
    }

    private static Bitmap rotatedCopy(Bitmap source, int degrees) {
        if (degrees == 0) {
            return source.copy(Bitmap.Config.ARGB_8888, false);
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0,
                source.getWidth(), source.getHeight(), matrix, true);
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (matcher != null) {
            matcher.close();
        }
        if (screenshot != null && !screenshot.isRecycled()) {
            screenshot.recycle();
        }
        for (Bitmap portrait : portraits) {
            if (portrait != null && !portrait.isRecycled()) {
                portrait.recycle();
            }
        }
        for (Bitmap template : matchedTemplates) {
            if (template != null && !template.isRecycled()) {
                template.recycle();
            }
        }
    }
}
