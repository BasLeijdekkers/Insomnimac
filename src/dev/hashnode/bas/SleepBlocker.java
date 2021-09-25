// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.registry.Registry;

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
        final boolean hasLongRunningTask = ProgressManager.getInstance().hasUnsafeProgressIndicator();
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
}
