package org.slim.framework.internal.policy;

import android.view.KeyEvent;

public interface SlimKeyHandler {
    public boolean handleKeyEvent(KeyEvent event, boolean longpress, boolean keyguardOn);
}
