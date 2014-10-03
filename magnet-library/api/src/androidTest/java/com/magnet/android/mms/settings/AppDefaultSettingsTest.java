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


import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;


public class AppDefaultSettingsTest extends InstrumentationTestCase {

  @SmallTest
  public void testDefaultValuesFromXml() {
    MagnetDefaultSettings settings = MagnetDefaultSettings.getInstance(getInstrumentation().getContext());
    assertTrue(settings != null);

    assertFalse(settings.getCacheEncryptionEnabled());
    settings.release();

  }

  @SmallTest
  public void testDefaultValuesFromBadXml() {
    
    int resId = getInstrumentation().getContext().getResources().getIdentifier("app_bad", "xml", 
        getInstrumentation().getContext().getPackageName());
    assertTrue(resId > 0);

    MagnetDefaultSettings settings = MagnetDefaultSettings.getInstanceFromResource(
        getInstrumentation().getContext(), resId);
    assertTrue(settings != null);

    // settings has bad spelling for "fals" => should return false (Boolean parses all bad strings to "false")
    assertFalse(settings.getCacheEncryptionEnabled());

    settings.release();
  }

  @SmallTest
  public void testDefaultValues() {
    MagnetDefaultSettings settings = MagnetDefaultSettings.getInstanceDefault();

    // default values when there is no settings xml file is
    // encryption = true
    // location = false
    assertTrue(settings.getCacheEncryptionEnabled());

  }
}
