package com.local.sgmhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GearHandleEventTest {
    @Test
    public void coalescesRequestsAndRunsOnce() {
        GearHandleEvent event = new GearHandleEvent();

        assertTrue(event.request());
        assertFalse(event.request());
        assertEquals(GearHandleEvent.State.PENDING, event.state());

        assertTrue(event.begin());
        assertFalse(event.begin());
        assertEquals(GearHandleEvent.State.HANDLING, event.state());

        event.complete();
        assertEquals(GearHandleEvent.State.IDLE, event.state());
    }

    @Test
    public void clearAllowsRecoveryToRequestAgain() {
        GearHandleEvent event = new GearHandleEvent();
        event.request();
        event.begin();

        event.clear();

        assertEquals(GearHandleEvent.State.IDLE, event.state());
        assertTrue(event.request());
    }

    @Test
    public void cancelledHandlingIsDeferredUntilPrimaryResumes() {
        GearHandleEvent event = new GearHandleEvent();
        event.request();
        event.begin();

        event.defer();

        assertEquals(GearHandleEvent.State.PENDING, event.state());
        assertTrue(event.begin());
    }
}
