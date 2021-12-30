// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.openapi.util.registry.RegistryValueListener;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Bas Leijdekkers
 */
public class Insomniac implements StartupActivity.Background {

    static final Logger LOG = Logger.getInstance("#insomniac");
    private static volatile boolean initialized = false;

    /**
     * Register once application wide after first project is opened.
     * @param project  unused
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Override
    public synchronized void runActivity(@NotNull Project project) {
        if (initialized) {
            return;
        }
        initialized = true;
        schedule(new TaskChecker());
    }

    private static void schedule(Runnable task) {
        final ScheduledExecutorService executorService = AppExecutorUtil.getAppScheduledExecutorService();
        final int delay = Registry.intValue("insomniac.poll.interval.seconds");
        executorService.schedule(task, delay, TimeUnit.SECONDS);
    }

    private static class TaskChecker implements Runnable, Disposable {
        private static final int THRESHOLD = 3;

        private int taskDetectedCount = 0;
        private Notification notification;
        private SleepBlocker sleepBlocker = null;
        private boolean disposed = false;

        public TaskChecker() {
            initializeSleepBlocker();
            final RegistryValueListener listener = new RegistryValueListener() {
                @Override
                public void afterValueChanged(@NotNull RegistryValue value) {
                    sleepBlocker.allowSleep();
                    initializeSleepBlocker();
                }
            };
            final DisposableService disposable = DisposableService.getInstance();
            Registry.get("insomniac.force.generic.sleep.blocker").addListener(listener, disposable);
            Disposer.register(DisposableService.getInstance(), this);
        }

        private void initializeSleepBlocker() {
            final boolean useGenericSleepBlocker = Registry.is("insomniac.force.generic.sleep.blocker");
            if (SystemInfo.isMac && !useGenericSleepBlocker) {
                sleepBlocker = new MacCaffeinateSleepBlocker();
            }
            else if (SystemInfo.isWindows && !useGenericSleepBlocker) {
                sleepBlocker = new WindowsSleepBlocker();
            }
            else {
                LOG.warn("Using generic sleep blocker for " + SystemInfoRt.OS_NAME);
                sleepBlocker = new GenericSleepBlocker();
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            if (sleepBlocker != null) {
                sleepBlocker.allowSleep();
            }
        }

        @Override
        public void run() {
            try {
                doCheck();
            } catch (RuntimeException e) {
                LOG.error(e);
                throw e; // rethrow exception so further execution of this task will be suppressed
            } catch (Exception e) {
                LOG.error(e);
                throw new RuntimeException(e);
            }
        }

        /**
         * <table>
         *     <tr>
         *         <th>hasLongRunningTask</th>
         *         <th>taskDetectedCount</th>
         *         <th>result</th>
         *     </tr>
         *     <tr>
         *         <td>true</td>
         *         <td>&lt; threshold</td>
         *         <td>taskDetectedCount++</td>
         *     </tr>
         *     <tr>
         *         <td>true</td>
         *         <td>&gt;= threshold</td>
         *         <td>preventSleep()</td>
         *     </tr>
         *     <tr>
         *         <td>false</td>
         *         <td>-</td>
         *         <td>allowSleep(); taskDetectedCount = 0</td>
         *     </tr>
         * </table>
         */
        private void doCheck() {
            final boolean hasLongRunningTask = ProgressManager.getInstance().hasUnsafeProgressIndicator();
            if (hasLongRunningTask) {
                if (taskDetectedCount < THRESHOLD) {
                    taskDetectedCount++;
                } else {
                    sleepBlocker.preventSleep();
                    if (Registry.is("insomniac.show.notification")) {
                        showNotification();
                    }
                }
            } else {
                sleepBlocker.allowSleep();
                taskDetectedCount = 0;
                hideNotification();
            }
            if (!disposed) {
                schedule(this);
            }
        }

        private void showNotification() {
            if (notification == null) {
                notification = NotificationGroupManager.getInstance().getNotificationGroup("Insomniac")
                        .createNotification("Long running task detected, preventing sleep", NotificationType.INFORMATION);
                notification.notify(null);
            }
        }

        private void hideNotification() {
            if (notification != null) {
                notification.expire();
                notification = null;
            }
        }
    }
}
