// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import com.intellij.ui.mac.foundation.Foundation;
import com.intellij.ui.mac.foundation.ID;
import com.sun.jna.Pointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Bas Leijdekkers
 */
class MacNativeSleepBlocker extends SleepBlocker {

    private MacActivity activity;

    @Override
    public void preventSleep() {
        if (activity == null) {
            activity = new MacActivity("IntelliJ IDEA is busy performing a long running task");
        }
    }

    @Override
    public void allowSleep() {
        if (activity != null) {
            activity.end();
            activity = null;
        }
    }

    /**
     * https://developer.apple.com/documentation/foundation/nsprocessinfo/1415995-beginactivitywithoptions
     */
    private static final class MacActivity extends AtomicReference<ID> {
        /**
         * Used for activities that require the computer to not idle sleep. This is included in NSActivityUserInitiated.
         */
        public static final long IDLE_SYSTEM_SLEEP_DISABLED = 1L << 20;
        /**
         * Require the screen to stay powered on.
         */
        public static final long IDLE_DISPLAY_SLEEP_DISABLED = 1L << 40;

        /**
         * App is performing a user-requested action.
         */
        public static final long USER_INITIATED = 0xFFFFFFL | IDLE_SYSTEM_SLEEP_DISABLED;

        /**
         * App has initiated some kind of work, but not as the direct result of user request.
         */
        public static final long BACKGROUND = 0x000000FFL;

        /**
         * Used for activities that require the highest amount of timer and I/O precision available.
         * Very few applications should need to use this constant.
         */
        public static final long LATENCY_CRITICAL = 0xFF00000000L;

        private static final ID NS_PROCESS_INFO = Foundation.getObjcClass("NSProcessInfo");
        private static final Pointer PROCESS_INFO = Foundation.createSelector("processInfo");
        private static final Pointer BEGIN_ACTIVITY = Foundation.createSelector("beginActivityWithOptions:reason:");
        private static final Pointer END_ACTIVITY = Foundation.createSelector("endActivity:");
        private static final Pointer RETAIN = Foundation.createSelector("retain");
        private static final Pointer RELEASE = Foundation.createSelector("release");

        MacActivity(@NotNull Object reason) {
            this(reason, USER_INITIATED);
        }

        MacActivity(@NotNull Object reason, long flags) {
            super(begin(reason, flags));
        }

        /**
         * Ends activity, allowing macOS to trigger AppNap (idempotent).
         */
        public void end() {
            end(getAndSet(null));
        }

        private static ID getNsProcessInfo() {
            return Foundation.invoke(NS_PROCESS_INFO, PROCESS_INFO);
        }

        private static ID begin(@NotNull Object reason, long flags) {
            return Foundation.invoke(
                    Foundation.invoke(getNsProcessInfo(), BEGIN_ACTIVITY,
                                      Long.valueOf(flags), Foundation.nsString(reason.toString())), RETAIN);
        }

        private static void end(@Nullable ID activityToken) {
            if (activityToken == null) {
                return;
            }
            Foundation.invoke(getNsProcessInfo(), END_ACTIVITY, activityToken);
            Foundation.invoke(activityToken, RELEASE);
        }
    }
}
