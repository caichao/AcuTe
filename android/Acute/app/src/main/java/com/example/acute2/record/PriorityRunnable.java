package com.example.acute2.record;

/**
 * Created by cc on 2016/10/13.
 */

import android.os.Process;

/**
 * Runnable that set thread priority before execution. See {@link Process} for list of available priority levels.
 */

abstract class PriorityRunnable implements Runnable {
    private int threadPriority;

    public PriorityRunnable() {
        threadPriority = Process.THREAD_PRIORITY_BACKGROUND;
    }

    public PriorityRunnable(@ProcessPriority int threadPriority) {
        this.threadPriority = threadPriority;
    }

    @Override
    public void run() {
        Process.setThreadPriority(threadPriority);
        runImpl();
    }

    protected abstract void runImpl();
}