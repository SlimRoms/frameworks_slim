/**
 * Copyright (C) 2016 The SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.slim.settings.activities;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.widget.Toast;

import com.slim.settings.R;

import slim.utils.ImageHelper;

/**
 * So we can capture image selection in DUSystemReceiver
 */
public class IconPackActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!IconPackHelper.pickIconPack(this)) {
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == IconPackHelper.REQUEST_PICK_ICON) {
            Bitmap b = (Bitmap) data.getParcelableExtra(IconPickerActivity.SELECTED_BITMAP_EXTRA);
            if (b != null) {
                Uri newUri = ImageHelper.addBitmapToStorage(b);
                if (newUri == null) {
                    Toast.makeText(this, getString(R.string.invalid_icon_from_uri),
                            Toast.LENGTH_SHORT)
                            .show();
                    sendCancelResultAndFinish();
                } else {
                    Intent resultIntent = new Intent();
                    resultIntent.setAction(ImageHelper.ACTION_IMAGE_PICKED);
                    resultIntent.putExtra("result", Activity.RESULT_OK);
                    resultIntent.putExtra("uri", newUri.toString());
                    sendBroadcastAsUser(resultIntent, UserHandle.CURRENT);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            } else {
                Toast.makeText(this, getString(R.string.invalid_icon_from_uri), Toast.LENGTH_SHORT)
                        .show();
                sendCancelResultAndFinish();
            }
        } else {
            sendCancelResultAndFinish();
        }
    }

    private void sendCancelResultAndFinish() {
        Intent intent = new Intent(ImageHelper.ACTION_IMAGE_PICKED);
        intent.putExtra("result", Activity.RESULT_CANCELED);
        sendBroadcastAsUser(intent, UserHandle.CURRENT);
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
