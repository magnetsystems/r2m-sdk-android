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
package com.magnet.android.mms.settings;

public interface MagnetSystemSettings {

  /**
   * Preferences name where Magnet preferences are stored. Use it in call to {@link android.content.Context#getSharedPreferences(String, int)}
   */
  public static final String MAGNET_SYSTEM_SHARED_PREFERENCE_FILE_NAME = "magnet.sys";

  /**
   * Enable encryption in local cache when using AsyncManager
   */
  public static final String PREFS_KEY_DEVICE_ID = "DEVICE_ID";

 
}
