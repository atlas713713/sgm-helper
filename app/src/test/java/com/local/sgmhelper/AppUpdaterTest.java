package com.local.sgmhelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AppUpdaterTest {
    @Test
    public void comparesReleaseVersionsNumerically() {
        assertTrue(AppUpdater.isNewerVersion("v0.1.235", "0.1.234"));
        assertTrue(AppUpdater.isNewerVersion("v0.2.0", "0.1.999"));
        assertFalse(AppUpdater.isNewerVersion("v0.1.234", "0.1.234"));
        assertFalse(AppUpdater.isNewerVersion("v0.1.233", "0.1.234"));
    }
}
