package com.local.sgmhelper;

import java.util.ArrayDeque;

final class AutomationTaskManager {
    enum Kind {
        PRIMARY,
        INTERRUPT,
        IDLE_ONLY
    }

    interface Listener {
        void onStart(Run run);

        void onCancel(Run run);

        void onResumePrimary();
    }

    static final class Request {
        final String key;
        final String progress;
        final Kind kind;
        final Runnable start;
        final boolean preserveRecovery;

        Request(String key, String progress, Kind kind, Runnable start) {
            this(key, progress, kind, start, false);
        }

        Request(String key, String progress, Kind kind,
                Runnable start, boolean preserveRecovery) {
            this.key = key;
            this.progress = progress;
            this.kind = kind;
            this.start = start;
            this.preserveRecovery = preserveRecovery;
        }
    }

    static final class Run {
        final long id;
        final Request request;

        Run(long id, Request request) {
            this.id = id;
            this.request = request;
        }
    }

    private final Listener listener;
    private final ArrayDeque<Request> pending = new ArrayDeque<>();
    private long nextRunId;
    private Run current;
    private boolean resumePrimary;

    AutomationTaskManager(Listener listener) {
        this.listener = listener;
    }

    boolean submit(Request request) {
        if (contains(request.key)) {
            return false;
        }
        if (current == null) {
            resumePrimary = request.kind == Kind.INTERRUPT;
            start(request);
            return true;
        }
        if (request.kind != Kind.INTERRUPT) {
            return false;
        }
        if (current.request.kind == Kind.INTERRUPT) {
            pending.addLast(request);
            return true;
        }
        resumePrimary = true;
        cancelCurrent();
        start(request);
        return true;
    }

    boolean finish(long runId) {
        if (!isCurrent(runId)) {
            return false;
        }
        current = null;
        dispatchNext();
        return true;
    }

    boolean abort(long runId) {
        if (!isCurrent(runId)) {
            return false;
        }
        cancelCurrent();
        dispatchNext();
        return true;
    }

    boolean restart(long runId, Request request) {
        if (!isCurrent(runId)) {
            return false;
        }
        cancelCurrent();
        start(request);
        return true;
    }

    void stopAll() {
        pending.clear();
        resumePrimary = false;
        cancelCurrent();
    }

    boolean isCurrent(long runId) {
        return current != null && current.id == runId;
    }

    boolean hasCurrent() {
        return current != null;
    }

    Run current() {
        return current;
    }

    int pendingCount() {
        return pending.size();
    }

    private boolean contains(String key) {
        if (current != null && current.request.key.equals(key)) {
            return true;
        }
        for (Request request : pending) {
            if (request.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private void cancelCurrent() {
        Run cancelled = current;
        current = null;
        if (cancelled != null) {
            listener.onCancel(cancelled);
        }
    }

    private void start(Request request) {
        current = new Run(++nextRunId, request);
        listener.onStart(current);
    }

    private void dispatchNext() {
        Request next = pending.pollFirst();
        if (next != null) {
            start(next);
        } else if (resumePrimary) {
            resumePrimary = false;
            listener.onResumePrimary();
        }
    }
}
