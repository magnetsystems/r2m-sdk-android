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

import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * @hide
 * (Internal Use Only) Abstract class that implements basic methods of the Manager interface.
 */
public abstract class AbstractManager implements ManagerInterface {
    private MagnetMobileClient mBaseMobileClient;
    private WeakReference<Context> mContext;

    /**
     * Basic initialization of each Manager instance.
     * Override this method to implement Manager-specific initialization, but always call <code>super.initInstance()</code>.
     */
    public void initInstance(Context context, MagnetMobileClient baseMobile) {
      mContext = new WeakReference<Context>(context);
      mBaseMobileClient = baseMobile;
    }
    /**
     * Retrieves the current context for the Manager.
     * @return Android context, or null if the underlying Android context has been garbage collected.
     */
    public final Context getContext() {
      return mContext.get();
    }
    
    /**
     * Retrieves the MagnetMobile parent associated with the Manager.
     * @return the MagnetMobileClient The MagnetMobile parent associated with the Manager.
     */
    public final MagnetMobileClient getBaseMobileClient() {
      return mBaseMobileClient;
    }
}
