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

/**
 * The interface that all Magnet Android "Managers" must implement.
 * 
 */
public interface ManagerInterface {

  /**
   * Instantiate a new instance of the Manager
   * @param context Android context
   * @param baseMobile MagnetMobile instance associated with the manager
   * @return Instance of ManagerInterface
   */
  void initInstance(Context context, MagnetMobileClient baseMobile);

  /**
   * Returns the current context for the Manager
   * @return Android context
   */
  Context getContext();

  /**
   * Returns parent MagnetMobileClient associated with the Manager
   * @return the MagnetMobileClient
   */
  MagnetMobileClient getBaseMobileClient();
  
}
