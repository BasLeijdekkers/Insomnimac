// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Bas Leijdekkers
 */
public class InsomniacStartupActivity implements StartupActivity.Background {

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
        final boolean useGenericSleepBlocker = Registry.is("insomniac.force.generic.sleep.blocker");
        final SleepBlocker sleepBlocker;
        if (SystemInfo.isMac && !useGenericSleepBlocker) {
            sleepBlocker = new MacSleepBlocker();
        }
        else if (SystemInfo.isWindows && !useGenericSleepBlocker) {
            sleepBlocker = new WindowsSleepBlocker();
        }
        else {
            Logger.getInstance("#insomniac").warn("Using generic sleep blocker for " + SystemInfoRt.OS_NAME);
            sleepBlocker = new GenericSleepBlocker();
        }
        final ScheduledExecutorService executorService = AppExecutorUtil.getAppScheduledExecutorService();
        final int delay = Registry.intValue("insomniac.poll.interval.seconds");
        executorService.scheduleWithFixedDelay(sleepBlocker, delay, delay, TimeUnit.SECONDS);
    }
}
