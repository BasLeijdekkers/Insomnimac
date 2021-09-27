// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.jna.JnaLoader;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;

/**
 * @author Bas Leijdekkers
 */
class WindowsSleepBlocker extends SleepBlocker {

    @Override
    public void handleSleep(boolean keepAwake) {
        if (!JnaLoader.isLoaded()) {
            LOG.warn("JNA not loaded, can't prevent sleep");
            return;
        }
        if (keepAwake) {
            LOG.info("Resetting idle timer to prevent sleep");
            // see https://docs.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-setthreadexecutionstate
            Kernel32.INSTANCE.SetThreadExecutionState(WinBase.ES_SYSTEM_REQUIRED);
        }
    }
}
