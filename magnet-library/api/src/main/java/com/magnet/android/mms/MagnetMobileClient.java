/*
 * Copyright (c) 2014 Magnet Systems, Inc.
 * All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.magnet.android.mms;

import java.util.HashMap;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import com.magnet.android.mms.async.AsyncManager;
import com.magnet.android.mms.connection.ConnectionConfigManager;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.utils.logger.Log;

/**
 * MagnetMobileClient is the starting class for initializing the Magnet SDK. It provides a single entry point for initialization
 * of global resources and the Magnet mobile engine.
 * <p>
 * The following sample shows how to use this class and get a controller instance for making requests:
 * <pre>
 *
 * try {
 *      MagnetMobileClient magnetClient = MagnetMobileClient.getInstance(context.getApplicationContext());
 *
 *      // invoke the controller using the generated controller class
 *      GoogleDistanceMatrixFactory restFactory = new GoogleDistanceMatrixFactory(magnetClient);
 *      GoogleDistanceMatrix restController  = restFactory.obtainInstance();
 *
 *      Call<GoogleDistanceMatrixResult> g =  restController.GoogleDistanceMatrix("-52.93", "55.930", null);
 *      GoogleDistanceMatrixResult resultDistance = g.get();
 *
 *      resultDistance.getOrigin_addresses();
 *      resultDistance.getDestination_addresses();
 *      resultDistance.getStatus();
 * } catch (SchemaException se) {
 *      // please look at {@link com.magnet.android.mms.exception.SchemaException} for the detail
 * } catch (InterruptedException ie) {
 *      // please look at {@code InterruptedException} for the detail
 * } catch (ExecutionException ee) {
 *      // please look at the message inside the exception for the detail
 * }
 * </pre>
 *
 */
public class MagnetMobileClient {
  private static final String TAG = MagnetMobileClient.class.getSimpleName();
  private static MagnetMobileClient sGlobalInstance;

  private Context mApplicationContext;
  private HashMap<Class<? extends ManagerInterface>, ManagerInterface> mManagers =
      new HashMap<Class<? extends ManagerInterface>, ManagerInterface>();

  private AsyncManager asyncManager;

  private static final String[] MANDATORY_PERMISSIONS = {
    android.Manifest.permission.INTERNET,
    android.Manifest.permission.READ_PHONE_STATE,
  };

  /**
   * Retrieves the singleton MagnetMobileClient instance for the specified application
   * context. Only one instance of MagnetMobileClient is associated with a
   * single application context.
   *
   * @param context
   *          The single global application context that is valid for the lifetime
   *          of the Android process. This is required for the MobileServer
   *          library so it can access Android Context related APIs, such
   *          as looking up resources, shared preferences, Android services,
   *          start activities, and so on.
   * @return A global instance of MagnetMobileClient.
   * @throws IllegalArgumentException if the application context is null.
   */
  public synchronized static MagnetMobileClient getInstance(Context context) {
    if (context == null) {
      throw new IllegalArgumentException("context must not be null");
    }
    if (sGlobalInstance == null) {
      // use passed in context or the applicationContext if it exists;
      // for InstrumentationTestCase, applicationContext is null
      Context appContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
      sGlobalInstance = new MagnetMobileClient(appContext);
    }
    return sGlobalInstance;
  }

  /**
   * Call this method when you no longer need to use this MagnetMobileClient. This method cleans up the global instance of MagnetMobileClient.
   * A new global instance will be created the next time {@link #getInstance(Context)} is invoked.
   *
   */
  public synchronized void finish() {
    if (asyncManager != null) {
      asyncManager = null;
    }
    // clean up the managers?
    // clean up the controllers?
    sGlobalInstance = null;
  }
  private void initCoreServices() {
    // bind to local services
    if (mApplicationContext instanceof Application) {
      asyncManager = AsyncManager.getInstance(mApplicationContext);
    }
  }

  private void checkPermissions() {
    StringBuilder denied = new StringBuilder();
    for (String permission : MANDATORY_PERMISSIONS) {
      int res = mApplicationContext.checkCallingOrSelfPermission(permission);
      if (res != PackageManager.PERMISSION_GRANTED) {
        if (denied.length() != 0) {
          denied.append(", ");
        }
        denied.append(permission);
      }
    }
    if (denied.length() > 0) {
      Log.e(TAG, "Application "+mApplicationContext.getPackageName()+
          " is missing the required permission(s): "+denied.toString());
      throw new SecurityException("Missing permissions: "+
          denied.toString()+".  Are they specified in AndroidManifest.xml?");
    }
  }

  /**
   * Retrieves the application context associated with this MagnetMobileClient.
   * @return The application context associated with this MagnetMobileClient.
   */
  public Context getAppContext() {
    return mApplicationContext;
  }

  /**
   * Constructor with an application context.
   *
   * @param context
   *          The application context.
   */
  protected MagnetMobileClient(Context context) {
    mApplicationContext = context;
    checkPermissions();
    initCoreServices();
  }

  /**
   * @hide
   * Retrieves an instance of the specified manager associated with the context.
   *
   * @param managerClass
   *          A manager class, such as the ConnectionConfigManager.class.
   * @param context
   *          The context of the current Android component to be associated with the
   *          Manager object, such as an Activity, Service, or ContentProvider.
   *          Having a specific <i>live</i> context can be useful in cases where a
   *          context is required, such as popping up a dialog, starting
   *          an activity with a result code, or binding to an Android service.
   * @return An instance of the specified manager.
   * @throws MobileRuntimeException if a failure occurred when constructing a manager instance.
   */
  public synchronized <T extends ManagerInterface> T getManager(
      Class<? extends ManagerInterface> managerClass, Context context) {
    if (mManagers.containsKey(managerClass)) {
      return (T) mManagers.get(managerClass);
    } else {
      T result;
      try {
        result = (T) managerClass.newInstance();
        result.initInstance(context, this);
        mManagers.put(managerClass, result);
        return result;
      } catch (InstantiationException e) {
        Log.w(TAG, "unexpected getManager failure. incorrect Magnet library?");
        throw new MobileRuntimeException(e);
      } catch (IllegalAccessException e) {
        Log.w(TAG, "unexpected getManager failure. incorrect Magnet library?");
        throw new MobileRuntimeException(e);
      }
    }
  }

  /**
   * @hide
   * Convenience method to retrieve the application's ConnectionConfigManager
   *
   * @return The singleton ConnectionConfigManager.
   */
  public ConnectionConfigManager getConnectionConfigManager() {
    return getManager(ConnectionConfigManager.class, mApplicationContext);
  }
}
