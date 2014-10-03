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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.magnet.android.mms.connection.ConnectionConfigManager;

public class MobileClientTest extends AndroidTestCase {

  @SmallTest
  public void testNewInstance() {

    MagnetMobileClient client = MagnetMobileClient.getInstance(this.getContext());
    assertNotNull(client);
  }

  @SmallTest
  public void testDefaultConnectionConfig() {
    ConnectionConfigManager cm = MagnetMobileClient.getInstance(this.getContext()).getConnectionConfigManager();
    assertNotNull(cm.getDefaultGenericRestConfig());
  }
}
