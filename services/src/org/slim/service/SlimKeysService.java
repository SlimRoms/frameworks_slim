package org.slim.service;

import android.content.Context;
import android.os.Handler;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.util.Log;
import android.text.TextUtils;

import com.android.server.LocalServices;

import org.slim.service.policy.AlertSliderObserver;
import org.slim.service.policy.HardwareKeyHandler;
import org.slim.service.policy.KeyHandler;

import org.slim.framework.internal.policy.SlimKeyHandler;

import org.slim.framework.internal.R;

public class SlimKeysService extends SlimSystemService implements SlimKeyHandler {
    private static final String TAG = SlimKeysService.class.getSimpleName();

    private AlertSliderObserver mAlertSliderObserver;
    private HardwareKeyHandler mHardwareKeyHandler;
    private KeyHandler mKeyHandler;

    private boolean mHasAlertSlider = false;

    private Context mContext;

    public SlimKeysService(Context context) {
        super(context);
        LocalServices.addService(SlimKeyHandler.class, this);
        mContext = context;

        mHasAlertSlider = !TextUtils.isEmpty(
                context.getResources().getString(R.string.alert_slider_state_path))
                && !TextUtils.isEmpty(context.getResources().getString(
                    R.string.alert_slider_uevent_match_path));

        if (mHasAlertSlider) {
            mAlertSliderObserver = new AlertSliderObserver(context);
            mAlertSliderObserver.startObserving(R.string.alert_slider_uevent_match_path);
        }

        mHardwareKeyHandler = new HardwareKeyHandler(context, new Handler());
        mKeyHandler = new KeyHandler(context);
    }

    public void onStart() {
    }


    public boolean handleKeyEvent(KeyEvent event, boolean longpress, boolean keyguardOn) {
        Log.d(TAG, "key - " + KeyEvent.keyCodeToString(event.getKeyCode()));
        Log.d(TAG, "scancode - " + event.getScanCode());

        final int keyCode = event.getKeyCode();
        final int repeatCount = event.getRepeatCount();
        final int flags = event.getFlags();
        final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        final boolean canceled = event.isCanceled();
        final boolean virtualKey = event.getDeviceId() == KeyCharacterMap.VIRTUAL_KEYBOARD;

        if (!keyguardOn && !virtualKey) {
            if (mHardwareKeyHandler.handleKeyEvent(keyCode, repeatCount,
                    down, canceled, longpress, keyguardOn)) {
                return true;
            }
        }
        if (mKeyHandler.handleKeyEvent(event)) {
            return true;
        }

        return false;
    }
}
