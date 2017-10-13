/*
 * Copyright (C) 2017 The SlimRoms Project
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
 */
package com.android.systemui.statusbar.slim;

import android.content.Context;

import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;

import com.android.systemui.plugins.qs.QSTile;

import com.android.systemui.qs.tiles.AmbientDisplayTile;
import com.android.systemui.qs.tiles.CaffeineTile;
import com.android.systemui.qs.tiles.ImeTile;
import com.android.systemui.qs.tiles.NfcTile;
import com.android.systemui.qs.tiles.ScreenshotTile;
import com.android.systemui.qs.tiles.SyncTile;
import com.android.systemui.qs.tiles.UsbTetherTile;

public class SlimQSTileHost extends QSTileHost {

    public SlimQSTileHost(Context context, StatusBar statusBar,
            StatusBarIconController iconController) {
        super(context, statusBar, iconController);
    }


    @Override
    public QSTile createTile(String tileSpec) {
        // handle additional tiles here
        switch(tileSpec) {
            case "ambient_display":
                return new AmbientDisplayTile(this);
            case "caffeine":
                return new CaffeineTile(this);
            case "ime":
                return new ImeTile(this);
            case "nfc":
                return new NfcTile(this);
            case "screenshot":
                return new ScreenshotTile(this);
            case "sync":
                return new SyncTile(this);
            case "usb_tether":
                return new UsbTetherTile(this);
            default:
                return super.createTile(tileSpec);
        }
    }
}
