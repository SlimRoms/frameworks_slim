package com.slim.settings.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.UserHandle;

import slim.utils.ShortcutPickerHelper;
import slim.utils.ShortcutPickerHelper.OnPickListener;

public class ShortcutPickerActivity extends Activity implements OnPickListener {

    private ShortcutPickerHelper mPicker;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPicker = new ShortcutPickerHelper(this, this);
        mPicker.pickShortcut(0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap bmp, boolean isApplication) {
        Intent intent = new Intent(ShortcutPickerHelper.ACTION_SHORTCUT_PICKED);
        intent.putExtra("action", action);
        intent.putExtra("description", description);
        intent.putExtra("bitmap", bmp);
        sendBroadcastAsUser(intent, UserHandle.CURRENT);
        finish();
    }
}
