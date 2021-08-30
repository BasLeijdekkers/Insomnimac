// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ConcurrentLongObjectMap;

/**
 * @author Bas Leijdekkers
 */
abstract class SleepBlocker implements Runnable {
    protected static final Logger LOG = Logger.getInstance("#insomniac");

    private boolean shouldPreventSleep = false;

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

    private void preventSleep() {
        final boolean hasLongRunningTask = isProgressBarActive();
        if (shouldPreventSleep) {
            shouldPreventSleep = hasLongRunningTask;
            handleSleep(shouldPreventSleep);
        } else if (hasLongRunningTask) {
            shouldPreventSleep = true;
        }
    }

    public abstract void handleSleep(boolean keepAwake);

    /**
     * Dirty hack to detect if a progress bar is currently visible.
     */
    private static boolean isProgressBarActive() {
        try {
            final ConcurrentLongObjectMap<?> currentIndicators =
                    ReflectionUtil.getStaticFieldValue(CoreProgressManager.class, ConcurrentLongObjectMap.class,
                                                       "currentIndicators");
            if (currentIndicators == null) {
                throw new IllegalStateException();
            }
            return !currentIndicators.isEmpty();
        } catch (RuntimeException e) {
            LOG.warn("Can't detect long running tasks, sleep will not be prevented", e);
            return false;
        }
    }
}
