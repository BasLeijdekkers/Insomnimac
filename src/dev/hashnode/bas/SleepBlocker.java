// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ConcurrentLongObjectMap;

/**
 * @author Bas Leijdekkers
 */
abstract class SleepBlocker implements Runnable {
    protected static final Logger LOG = Logger.getInstance("#insomniac");

    private static final int threshold = 3;

    private int longRunningTaskDetected = 0;
    private Notification notification;

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
        if (hasLongRunningTask && longRunningTaskDetected < threshold) {
            longRunningTaskDetected++;
        }
        if (longRunningTaskDetected >= threshold) {
            if (!hasLongRunningTask) {
                longRunningTaskDetected = 0;
            }
            showHideNotification(longRunningTaskDetected != 0);
            handleSleep(longRunningTaskDetected != 0);
        }
    }

    private void showHideNotification(boolean show) {
        if (!Registry.is("insomniac.show.notification")) {
            return;
        }
        if (show) {
            if (notification != null) {
                return;
            }
            notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("Insomniac")
                    .createNotification("Long running task detected, preventing sleep", NotificationType.INFORMATION);
            notification.notify(null);
        }
        else if (notification != null) {
            notification.expire();
            notification = null;
        }
    }

    public abstract void handleSleep(boolean keepAwake);

    /**
     * Dirty hack to detect if a progress bar is currently visible.
     */
    private static boolean isProgressBarActive() {
        try {
            final ConcurrentLongObjectMap<ProgressIndicator> currentIndicators =
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
