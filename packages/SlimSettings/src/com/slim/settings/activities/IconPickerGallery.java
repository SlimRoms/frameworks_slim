/**
 * Copyright (C) 2016 The DirtyUnicorns Project
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
import android.provider.MediaStore;
import android.widget.Toast;

import com.slim.settings.R;

import java.io.IOException;

import slim.utils.ImageHelper;

/**
 * So we can capture image selection in DUSystemReceiver
 */
public class IconPickerGallery extends Activity {
    public static String TAG = IconPickerGallery.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(intent, 69);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == 69) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Bitmap b = null;
                try {
                    b = ImageHelper.getBitmapFromUri(this, data.getData());
                } catch (Exception e) {}
                if (b != null) {
                    Uri uri = ImageHelper.addBitmapToStorage(b);
                    if (uri != null) {
                        Intent resultIntent = new Intent(ImageHelper.ACTION_IMAGE_PICKED);
                        resultIntent.putExtra("result", Activity.RESULT_OK);
                        resultIntent.putExtra("uri", uri.toString());
                        sendBroadcastAsUser(resultIntent, UserHandle.CURRENT);
                        finish();
                    }
                }
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
