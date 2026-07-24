package com.local.sgmhelper;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class OcrText {
    private final List<TextBlock> blocks;
    private final String text;

    OcrText(List<OcrLine> lines) {
        List<TextBlock> values = new ArrayList<>();
        StringBuilder combined = new StringBuilder();
        for (OcrLine line : lines) {
            Line value = new Line(line.text, line.bounds);
            values.add(new TextBlock(Collections.singletonList(value)));
            if (combined.length() > 0) {
                combined.append('\n');
            }
            combined.append(line.text);
        }
        blocks = Collections.unmodifiableList(values);
        text = combined.toString();
    }

    List<TextBlock> getTextBlocks() {
        return blocks;
    }

    String getText() {
        return text;
    }

    static final class TextBlock {
        private final List<Line> lines;

        TextBlock(List<Line> lines) {
            this.lines = lines;
        }

        List<Line> getLines() {
            return lines;
        }

        String getText() {
            StringBuilder value = new StringBuilder();
            for (Line line : lines) {
                if (value.length() > 0) {
                    value.append('\n');
                }
                value.append(line.getText());
            }
            return value.toString();
        }
    }

    static final class Line {
        private final String text;
        private final Rect bounds;

        Line(String text, Rect bounds) {
            this.text = text;
            this.bounds = bounds;
        }

        String getText() {
            return text;
        }

        Rect getBoundingBox() {
            return bounds;
        }

        List<Element> getElements() {
            if (text.isEmpty() || bounds == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(new Element(text, bounds));
        }
    }

    static final class Element {
        private final String text;
        private final Rect bounds;

        Element(String text, Rect bounds) {
            this.text = text;
            this.bounds = bounds;
        }

        String getText() {
            return text;
        }

        Rect getBoundingBox() {
            return bounds;
        }
    }
}
