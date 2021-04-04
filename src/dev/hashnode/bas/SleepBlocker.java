// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ConcurrentLongObjectMap;

/**
 * @author Bas Leijdekkers
 */
class SleepBlocker implements Runnable {
    private static final Logger LOG = Logger.getInstance("#insomnimac");

    private boolean progressRunning = false;
    private Process process = null;

    @Override
    public void run() {
        try {
            preventSleep();
        } catch (RuntimeException e) {
            LOG.error(e);
            throw e; // rethrow exception so further execution of this task will be suppressed
        } catch (Exception e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    private void preventSleep() throws ExecutionException {
        if (progressRunning) {
            final boolean stillRunning = isProgressBarActive();
            if (process != null) {
                if (!stillRunning) {
                    LOG.info("task finished, stopping caffeinate");
                    process.destroy();
                    try {
                        process.waitFor();
                    } catch (InterruptedException ignore) {
                    }
                    process = null;
                    progressRunning = false;
                }
            } else {
                if (!stillRunning) {
                    progressRunning = false;
                } else {
                    LOG.info("long running task detected, starting caffeinate");
                    final GeneralCommandLine commandLine = new GeneralCommandLine("/usr/bin/caffeinate");
                    commandLine.addParameter("-w " + OSProcessUtil.getApplicationPid());
                    process = commandLine.createProcess();
                }
            }
        } else if (isProgressBarActive()) {
            progressRunning = true;
        }
    }

    /**
     * Dirty hack to detect if a progress bar is currently visible.
     */
    private static boolean isProgressBarActive() {
        final ConcurrentLongObjectMap<?> currentIndicators =
                ReflectionUtil.getStaticFieldValue(CoreProgressManager.class, ConcurrentLongObjectMap.class,
                                                   "currentIndicators");
        return currentIndicators != null && !currentIndicators.isEmpty();
    }
}
