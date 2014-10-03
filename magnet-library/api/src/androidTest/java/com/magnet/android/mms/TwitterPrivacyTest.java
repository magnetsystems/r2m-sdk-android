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


import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.magnet.android.mms.async.Call;
import com.magnet.android.mms.exception.HttpCallException;
import com.twitter.controller.api.TwitterPrivacy;
import com.twitter.controller.api.TwitterPrivacyFactory;
import com.twitter.model.beans.PrivacyResult;


public class TwitterPrivacyTest extends InstrumentationTestCase {
  private MagnetMobileClient magnetClient;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    magnetClient = MagnetMobileClient.getInstance(this.getInstrumentation().getTargetContext());
  }

  @SmallTest
  public void testTwitterPrivacyBadRequest() throws Exception {
    // create the controller factory
    TwitterPrivacyFactory restFactory = new TwitterPrivacyFactory(magnetClient);
    TwitterPrivacy restController = restFactory.obtainInstance();

    // Call the controller
    Call<PrivacyResult> g = restController.getPrivacy(null);
    try {
      PrivacyResult result = g.get();
      fail("call should get exception due to no auth");
    } catch (HttpCallException e) {
      // expect exception cuz no auth token
      int statusCode = e.getResponseCode();
      assertTrue(statusCode/100 == 4);
      assertTrue(e.getResponse().length() > 0);
      Log.d("testTwitterPrivacy", "http response=" + e.getResponse());
    }
  }

}
