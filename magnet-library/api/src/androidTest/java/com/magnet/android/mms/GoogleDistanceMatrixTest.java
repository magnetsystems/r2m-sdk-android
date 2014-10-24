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
import com.magneteng.restbyexamples.google.controller.api.GoogleDistanceMatrix;
import com.magneteng.restbyexamples.google.controller.api.GoogleDistanceMatrixFactory;
import com.magneteng.restbyexamples.google.model.beans.Element;
import com.magneteng.restbyexamples.google.model.beans.GoogleDistanceMatrixResult;
import com.magneteng.restbyexamples.google.model.beans.Row;

import java.util.List;


public class GoogleDistanceMatrixTest extends InstrumentationTestCase {
  private MagnetMobileClient magnetClient;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    magnetClient = MagnetMobileClient.getInstance(this.getInstrumentation().getTargetContext());
  }

  @SmallTest
  public void testGetDistanceMatrix() throws Exception {
      final String E_ORIGIN = "435 Tasso Street, Palo Alto, CA 94301, USA";
      final String E_DESTINATION = "Embarcadero North Street, San Francisco, CA, USA";
      final String E_DISTANCE = "34.4 mi";
      final String E_DURATION = "45 mins";
      final String E_STATUS = "OK";

      // create the controller factory
      GoogleDistanceMatrixFactory restFactory = new GoogleDistanceMatrixFactory(magnetClient);
      GoogleDistanceMatrix restController  = restFactory.obtainInstance();

      // Call the controller
      Call<GoogleDistanceMatrixResult> g =  restController.getDistanceMatrix(E_ORIGIN, E_DESTINATION, "false", "driving", "en", "imperial", null);
      GoogleDistanceMatrixResult resultDistance = g.get();

      // Assert the results
      List<String> originAddresses = resultDistance.getOrigin_addresses();
      List<String> destinationAddresses = resultDistance.getDestination_addresses();
      assertEquals(1, originAddresses.size());
      assertEquals(1, destinationAddresses.size());

      String aOrigin = originAddresses.get(0);
      String aDestination = destinationAddresses.get(0);
      assertEquals(E_ORIGIN, aOrigin);
      assertEquals(E_DESTINATION, aDestination);

      List<Row> rows = resultDistance.getRows();
      assertEquals(1, rows.size());

      Row row = rows.get(0);
      List<Element> elementList = row.getElements();
      assertEquals(1, elementList.size());

      Element e = elementList.get(0);
      assertNotNull(e.getDistance().getText());
      assertNotNull(e.getDuration().getText());
      assertEquals(E_STATUS, e.getStatus());

      assertEquals(E_STATUS, resultDistance.getStatus());
  }

}
