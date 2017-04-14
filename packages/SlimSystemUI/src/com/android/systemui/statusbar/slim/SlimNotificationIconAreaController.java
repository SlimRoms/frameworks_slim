package com.android.systemui.statusbar.slim;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.NotificationIconAreaController;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class SlimNotificationIconAreaController extends NotificationIconAreaController {

    public SlimNotificationIconAreaController(Context context, PhoneStatusBar phoneStatusBar) {
        super(context, phoneStatusBar);
    }

    @Override
    protected View inflateIconArea(LayoutInflater inflater) {
        return inflater.inflate(R.layout.slim_notification_icon_area, null);
    }
}
