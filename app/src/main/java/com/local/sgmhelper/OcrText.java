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

    static OcrText offset(OcrText text, int dx, int dy) {
        List<OcrLine> lines = new ArrayList<>();
        for (TextBlock block : text.getTextBlocks()) {
            for (Line line : block.getLines()) {
                Rect bounds = line.getBoundingBox();
                Rect shifted = null;
                if (bounds != null) {
                    shifted = new Rect();
                    shifted.left = bounds.left + dx;
                    shifted.top = bounds.top + dy;
                    shifted.right = bounds.right + dx;
                    shifted.bottom = bounds.bottom + dy;
                }
                lines.add(new OcrLine(line.getText(), shifted, 0f));
            }
        }
        return new OcrText(lines);
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
