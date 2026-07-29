package com.local.sgmhelper;

final class GearHandleEvent {
    enum State {
        IDLE,
        PENDING,
        HANDLING
    }

    private State state = State.IDLE;

    boolean request() {
        if (state != State.IDLE) {
            return false;
        }
        state = State.PENDING;
        return true;
    }

    boolean begin() {
        if (state != State.PENDING) {
            return false;
        }
        state = State.HANDLING;
        return true;
    }

    void complete() {
        if (state == State.HANDLING) {
            state = State.IDLE;
        }
    }

    void defer() {
        if (state == State.HANDLING) {
            state = State.PENDING;
        }
    }

    void clear() {
        state = State.IDLE;
    }

    State state() {
        return state;
    }
}
