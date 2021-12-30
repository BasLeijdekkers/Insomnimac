// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;

/**
 * @author Bas Leijdekkers
 */
@Service(Service.Level.APP)
public final class DisposableService implements Disposable {

    @Override
    public void dispose() {}

    public static DisposableService getInstance() {
        return ApplicationManager.getApplication().getService(DisposableService.class);
    }
}
