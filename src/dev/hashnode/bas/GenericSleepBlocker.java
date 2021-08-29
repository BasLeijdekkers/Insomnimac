// Copyright 2020-2021 Bas Leijdekkers Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.hashnode.bas;

import java.awt.*;

/**
 * @author Bas Leijdekkers
 */
class GenericSleepBlocker extends SleepBlocker {

    @Override
    public void handleSleep(boolean keepAwake) {
        if (keepAwake) {
            LOG.info("Wiggling mouse to prevent sleep");
            try {
                final PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                final Robot robot = new Robot(pointerInfo.getDevice());
                final Point location = pointerInfo.getLocation();
                final int delta = (location.x == 0) ? -1 : 1;
                robot.mouseMove(location.x + delta, location.y);
                robot.mouseMove(location.x - delta, location.y);
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
