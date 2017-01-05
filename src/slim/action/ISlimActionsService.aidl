/**
 * Copyright (c) 2016-2017, The SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package slim.action;

import android.content.Intent;
import android.os.Bundle;

import org.slim.framework.internal.statusbar.ISlimStatusBar;

/** @hide */
interface ISlimActionsService {

    void registerSlimStatusBar(ISlimStatusBar bar);

    void showCustomIntentAfterKeyguard(inout Intent intent);
    void toggleScreenshot();
    void toggleLastApp();
    void toggleKillApp();

    void toggleGlobalMenu();

    void startAssist(in Bundle bundle);
    void toggleSplitScreen();
    void toggleRecentApps();
    void preloadRecentApps();
    void cancelPreloadRecentApps();
}
