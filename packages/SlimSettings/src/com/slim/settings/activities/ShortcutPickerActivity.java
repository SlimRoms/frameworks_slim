package com.slim.settings.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.UserHandle;

import slim.utils.ShortcutPickerHelper;

public class ShortcutPickerActivity extends Activity implements
        ShortcutPickerHelper.OnPickListener {

    ShortcutPickerHelper mPicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPicker = new ShortcutPickerHelper(this, this);
        mPicker.pickShortcut(0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPicker.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, Bitmap bmp, boolean isApplication) {
        Intent intent = new Intent(ShortcutPickerHelper.ACTION_SHORTCUT_PICKED);
        intent.putExtra(ShortcutPickerHelper.EXTRA_ACTION, uri);
        intent.putExtra(ShortcutPickerHelper.EXTRA_DESCRIPTION, friendlyName);
        sendBroadcastAsUser(intent, UserHandle.CURRENT);
        finish();
    }
}
