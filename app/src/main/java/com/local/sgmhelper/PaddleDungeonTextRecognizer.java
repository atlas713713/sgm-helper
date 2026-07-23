package com.local.sgmhelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;

import com.paddle.ocr.EngineConfig;
import com.paddle.ocr.PaddleOCRConfig;
import com.paddle.ocr.engine.OCREngine;
import com.paddle.ocr.model.OCRResult;
import com.paddle.ocr.util.OpenCVUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class PaddleDungeonTextRecognizer {
    private static final String DET_MODEL = "models/det/inference.onnx";
    private static final String REC_MODEL = "models/rec/inference.onnx";
    private static final String REC_CONFIG = "models/rec/inference.yml";

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private OCREngine engine;

    PaddleDungeonTextRecognizer(Context context) {
        this.context = context.getApplicationContext();
    }

    void recognize(Bitmap bitmap, Consumer<List<OcrLine>> success,
            Consumer<Throwable> failure) {
        executor.execute(() -> {
            try {
                List<OcrLine> lines = new ArrayList<>();
                for (OCRResult result : engine().run(bitmap).getResults()) {
                    lines.add(new OcrLine(
                            result.getText(),
                            bounds(result),
                            result.getConfidence()));
                }
                success.accept(lines);
            } catch (Throwable error) {
                failure.accept(error);
            }
        });
    }

    void close() {
        executor.execute(() -> {
            if (engine != null) {
                engine.release();
                engine = null;
            }
        });
        executor.shutdown();
    }

    private OCREngine engine() {
        if (engine == null) {
            if (!OpenCVUtils.INSTANCE.init(context)) {
                throw new IllegalStateException("OpenCV initialization failed");
            }
            PaddleOCRConfig config = new PaddleOCRConfig(
                    "BGR", 64, "min", 4000,
                    0.3f, 0.6f, 1.5f, 3000,
                    false, "fast", "quad", 0.35f, 1);
            engine = new OCREngine(
                    context, config, new EngineConfig(4),
                    DET_MODEL, REC_MODEL, REC_CONFIG);
        }
        return engine;
    }

    private static Rect bounds(OCRResult result) {
        float left = Float.MAX_VALUE;
        float top = Float.MAX_VALUE;
        float right = -Float.MAX_VALUE;
        float bottom = -Float.MAX_VALUE;
        for (PointF point : result.getBox().getPoints()) {
            left = Math.min(left, point.x);
            top = Math.min(top, point.y);
            right = Math.max(right, point.x);
            bottom = Math.max(bottom, point.y);
        }
        return new Rect(Math.round(left), Math.round(top),
                Math.round(right), Math.round(bottom));
    }
}
