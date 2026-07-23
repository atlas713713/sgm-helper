package com.local.sgmhelper;

import android.graphics.Rect;

final class OcrLine {
    final String text;
    final Rect bounds;
    final float confidence;

    OcrLine(String text, Rect bounds, float confidence) {
        this.text = text;
        this.bounds = bounds;
        this.confidence = confidence;
    }
}
