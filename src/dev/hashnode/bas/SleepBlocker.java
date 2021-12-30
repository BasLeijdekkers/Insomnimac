// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.openapi.diagnostic.Logger;

/**
 * @author Bas Leijdekkers
 */
abstract class SleepBlocker {
    protected static final Logger LOG = Logger.getInstance("#insomniac");

    public abstract void preventSleep();
    public void allowSleep() {}
}
