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

/**
 * This class defines build information about the SDK.
 */
public final class BuildInfo {

  /**
   * Build version string.
   */
  final static String SDK_VERSION = "1.1.0";

  /**
   * Retrieves the Magnet SDK version string.
   * @return The Magnet SDK version string. For example, "1.0.0".
   */
  public static String getSdkVersion() {
    return SDK_VERSION;
  }
}
