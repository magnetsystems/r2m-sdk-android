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

import com.magnet.android.mms.async.Call;
import com.magnet.android.mms.exception.SchemaException;
import com.magneteng.restbyexamples.google.controller.api.GoogleGeoCode;
import com.magneteng.restbyexamples.google.controller.api.GoogleGeoCodeFactory;
import com.magneteng.restbyexamples.google.model.beans.GeoCodeResult;
import com.magneteng.restbyexamples.google.model.beans.Result;

import java.util.List;
import java.util.concurrent.ExecutionException;


public class GoogleGeoCodeTest extends InstrumentationTestCase {
  private MagnetMobileClient magnetClient;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    magnetClient = MagnetMobileClient.getInstance(this.getInstrumentation().getTargetContext());
  }

  @SmallTest
  public void testGetGeoCode() throws SchemaException {
    final String E_ADDRESS = "1600 Amphitheatre Parkway, Mountain View, CA 94043, USA";
    final Double E_LAT = 37.4222953d;
    final Double E_LNG = -122.0840671d;
    final String E_LOCATION_TYPE = "ROOFTOP";
    final String E_STATUS = "OK";
    final String E_TYPE = "street_address";

    // create the controller factory
    GoogleGeoCodeFactory restFactory = new GoogleGeoCodeFactory(magnetClient);
    GoogleGeoCode restController = restFactory.obtainInstance();

    // Call the controller
    Call<GeoCodeResult> g = restController.getGeoCode(E_ADDRESS, "true", null);
    GeoCodeResult resultGeoCode;
    try {
      resultGeoCode = g.get();
      // Assert the results
      List<Result> results = resultGeoCode.getResults();
      assertEquals(1, results.size());


      Result result = results.get(0);

      assertEquals(E_LAT, result.getGeometry().getLocation().getLat());
      assertEquals(E_LNG, result.getGeometry().getLocation().getLng());
      assertEquals(E_LOCATION_TYPE, result.getGeometry().getLocation_type());
      assertEquals(E_ADDRESS, result.getFormatted_address());

      List<String> types = result.getTypes();
      assertEquals(1, types.size());
      assertEquals(E_TYPE, types.get(0));

      assertEquals(E_STATUS, resultGeoCode.getStatus());
    } catch (ExecutionException ee) {
      ee.getCause().getMessage();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
