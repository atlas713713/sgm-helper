package com.local.mlkitprobe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TemplateMatcherTest {
    @Test
    public void mapsDisplayedRotationToRequiredClicks() {
        assertEquals(0, TemplateMatcher.expectedCorrection(0));
        assertEquals(3, TemplateMatcher.expectedCorrection(1));
        assertEquals(2, TemplateMatcher.expectedCorrection(2));
        assertEquals(1, TemplateMatcher.expectedCorrection(3));
    }

    @Test
    public void requiresWudangRecognitionThreshold() {
        assertTrue(TemplateMatcher.isRecognized(0.95));
        assertFalse(TemplateMatcher.isRecognized(0.949999));
    }
}
