// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.openapi.diagnostic.Logger;

/**
 * @author Bas Leijdekkers
 */
class MacSleepBlocker extends SleepBlocker {

    private Process process = null;

    @Override
    public void handleSleep(boolean keepAwake) {
        if (keepAwake) {
            if (process == null) {
                LOG.info("Starting Caffeinate");
                final GeneralCommandLine commandLine = new GeneralCommandLine("/usr/bin/caffeinate");
                commandLine.addParameter("-w " + OSProcessUtil.getApplicationPid());
                try {
                    process = commandLine.createProcess();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            if (process != null) {
                LOG.info("Stopping Caffeinate");
                process.destroy();
                try {
                    process.waitFor();
                } catch (InterruptedException ignore) {
                }
                process = null;
            }
        }
    }
}
