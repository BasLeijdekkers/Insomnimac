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
public class InsomnimacStartupActivity implements StartupActivity.Background {

    private static volatile Runnable command = null;

    /**
     * Register once application wide after first project is opened.
     * @param project  unused
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Override
    public synchronized void runActivity(@NotNull Project project) {
        if (command != null) {
            return;
        }
        if (!SystemInfo.isMac) {
            Logger.getInstance("#insomnimac").warn("Insomnimac does not work on " + SystemInfoRt.OS_NAME);
            command = () -> {}; // dummy
        }
        final ScheduledExecutorService executorService = AppExecutorUtil.getAppScheduledExecutorService();
        final int delay = Registry.intValue("insomnimac.poll.interval.seconds");
        executorService.scheduleWithFixedDelay(command = new SleepBlocker(), delay, delay, TimeUnit.SECONDS);
    }

}
