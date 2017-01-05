/*
* Copyright (C) 2014-2017 SlimRoms Project
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

package slim.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import java.util.ArrayList;

import slim.action.ActionConfig;
import slim.action.ActionConstants;

public class ConfigSplitHelper {

    private static final String SLIM_METADATA_NAME = "slim";

    public static ArrayList<ActionConfig> getActionConfigValues(Context context, String config,
                String values, String entries, boolean isShortcut) {
        // init vars to fill with them later the config values
        int counter = 0;
        ArrayList<ActionConfig> actionConfigList = new ArrayList<ActionConfig>();
        ActionConfig actionConfig = null;

        PackageManager pm = context.getPackageManager();
        Resources slimResources = context.getResources();

        // Split out the config to work with and add to the list
        for (String configValue : config.split("\\" + ActionConstants.ACTION_DELIMITER)) {
            counter++;
            if (counter == 1) {
                actionConfig = new ActionConfig(configValue,
                            AppHelper.getProperSummary(context, pm, slimResources,
                            configValue, values, entries), null, null, null, null, null);
            }
            if (counter == 2) {
                if (isShortcut) {
                    actionConfig.setIcon(configValue);
                    actionConfigList.add(actionConfig);
                    //reset counter due that shortcut iteration of one action is finished
                    counter = 0;
                } else {
                    actionConfig.setLongpressAction(configValue);
                    actionConfig.setLongpressActionDescription(
                            AppHelper.getProperSummary(context, pm, slimResources,
                            configValue, values, entries));
                }
            }
            if (counter == 3) {
                actionConfig.setDoubleTapAction(configValue);
                actionConfig.setDoubleTapActionDescription(
                        AppHelper.getProperSummary(context, pm, slimResources,
                        configValue, values, entries));
            }
            if (counter == 4) {
                actionConfig.setIcon(configValue);
                actionConfigList.add(actionConfig);
                //reset counter due that iteration of full config action is finished
                counter = 0;
            }
        }

        return actionConfigList;
    }

    public static String setActionConfig(
            ArrayList<ActionConfig> actionConfigs, boolean isShortcut) {
        String finalConfig = "";
        ActionConfig actionConfig;

        for (int i = 0; i < actionConfigs.size(); i++) {
            if (i != 0) {
                finalConfig += ActionConstants.ACTION_DELIMITER;
            }
            actionConfig = actionConfigs.get(i);
            finalConfig += actionConfig.getClickAction() + ActionConstants.ACTION_DELIMITER;
            if (!isShortcut) {
                finalConfig += actionConfig.getLongpressAction()
                    + ActionConstants.ACTION_DELIMITER;
                finalConfig += actionConfig.getDoubleTapAction()
                    + ActionConstants.ACTION_DELIMITER;
            }
            finalConfig += actionConfig.getIcon();
        }

        return finalConfig;
    }

}
