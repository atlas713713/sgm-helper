package com.local.sgmhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.graphics.Rect;

import java.util.Collections;

import org.junit.Test;

public final class OcrTextTest {
    @Test
    public void preservesPaddleLineTextAndBounds() {
        Rect bounds = new Rect(10, 20, 110, 50);
        OcrText text = new OcrText(Collections.singletonList(
                new OcrLine("元宝回收", bounds, 0.9f)));

        OcrText.Line line = text.getTextBlocks().get(0).getLines().get(0);
        assertEquals("元宝回收", text.getText());
        assertSame(bounds, line.getBoundingBox());
        assertEquals("元宝回收", line.getElements().get(0).getText());
        assertSame(bounds, line.getElements().get(0).getBoundingBox());
    }
}
