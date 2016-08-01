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
 */

package org.slim.service;

import android.content.Context;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class SlimSystemServer {

    private static final String TAG = "SlimSystemServer";

    private Context mContext;

    public SlimSystemServer(Context context) {
        mContext = context;
    }

    /**
     * Invoked via reflection by the SystemServer
     */
    private void run() {
        // Start services.
        try {
            startServices();
        } catch (Throwable ex) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting slim system services", ex);
            throw ex;
        }
    }

    private void startServices() {
        final Context context = mContext;
        final SystemServiceManager ssm = LocalServices.getService(SystemServiceManager.class);
        String[] externalServices = context.getResources().getStringArray(
                org.slim.framework.internal.R.array.config_externalSlimServices);

        for (String service : externalServices) {
            try {
                Slog.i(TAG, "Attempting to start service " + service);
                SlimSystemService slimSystemService =  getServiceFor(service);
                Slog.i(TAG, "Starting service " + service);
                ssm.startService(slimSystemService.getClass());
            } catch (Throwable e) {
                reportWtf("starting " + service , e);
            }
        }
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    private SlimSystemService getServiceFor(String className) {
        final Class<SlimSystemService> serviceClass;
        try {
            serviceClass = (Class<SlimSystemService>)Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Failed to create service " + className
                    + ": service class not found", ex);
        }

        return getServiceFromClass(serviceClass);
    }

    private <T extends SlimSystemService> T getServiceFromClass(Class<T> serviceClass) {
        final T service;
        try {
            Constructor<T> constructor = serviceClass.getConstructor(Context.class);
            service = constructor.newInstance(mContext);
        } catch (InstantiationException ex) {
            throw new RuntimeException("Failed to create service " + serviceClass
                    + ": service could not be instantiated", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Failed to create service " + serviceClass
                    + ": service must have a public constructor with a Context argument", ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Failed to create service " + serviceClass
                    + ": service must have a public constructor with a Context argument", ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Failed to create service " + serviceClass
                    + ": service constructor threw an exception", ex);
        }
        return service;
    }
}
