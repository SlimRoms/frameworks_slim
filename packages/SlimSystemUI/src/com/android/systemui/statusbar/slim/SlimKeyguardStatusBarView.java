/*
* Copyright (C) 2016 SlimRoms Project
* Copyright (C) 2013-14 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.systemui.statusbar.slim;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.AutoReinflateContainer.InflateListener;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.KeyguardStatusBarView;
import com.android.systemui.statusbar.policy.BatteryController;

public class SlimKeyguardStatusBarView extends KeyguardStatusBarView {

    public SlimKeyguardStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View batteryLevel = findViewById(R.id.battery_level);
        ((ViewGroup) batteryLevel.getParent()).removeView(batteryLevel);
    }

    @Override
    public void setBatteryController(final BatteryController controller) {
        super.setBatteryController(controller);
        AutoReinflateContainer batteryContainer = (AutoReinflateContainer)
                findViewById(R.id.slim_reinflate_battery_container);
        if (batteryContainer != null) {
            batteryContainer.addInflateListener(new InflateListener() {
                @Override
                public void onInflated(View v) {
                    ((SlimBatteryContainer) v.findViewById(R.id.slim_battery_container))
                            .setBatteryController(controller);
                }
            });
        }
    }
}
