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

package com.magnet.android.mms.request;

import android.content.res.AssetManager;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.magnet.android.core.GenericRestConstants;
import com.magnet.android.mms.connection.ConnectionConfigManager;
import com.magnet.android.mms.exception.MobileException;
import com.magneteng.restbyexamples.google.model.beans.GeoCodeResult;


import java.io.*;
import java.text.ParseException;

public class DirectRestResponseTest extends InstrumentationTestCase {

  private File copyAssetFile(InputStream ais, String fname) throws IOException {
    byte[] buf = new byte[256];
    
    File cache = new File(getInstrumentation().getTargetContext().getCacheDir(), fname);
    FileOutputStream os = new FileOutputStream(cache);

    int count=0;
    while ((count=ais.read(buf)) != -1) {
      os.write(buf, 0, count);
    }
    os.close();
    return cache;
  }


  @SmallTest
  public void testGeocodeJson() throws IOException, MobileException, ParseException{
    AssetManager am = this.getInstrumentation().getContext().getAssets();
    String fileName = "response_geocode_json.txt";
    InputStream fis = am.open("direct_rest_response/" + fileName);
    File cache = copyAssetFile(fis, fileName);
    fis.close();

    ParserFactory pf = new ParserFactory(GeoCodeResult.class);
    ResponseParser parser = pf.createInstance(GenericRestConstants.CONTENT_TYPE_JSON, GenericRestConstants.MIME_ENCODING_NONE);
    Object parseValue = parser.parseResponse(new FileInputStream(cache));
    // parsed as GeoCodeResult
    assertTrue(parseValue instanceof GeoCodeResult);
    GeoCodeResult result = (GeoCodeResult)parseValue;
    assertEquals(result.getStatus(), "OK");
    assertEquals(1, result.getResults().size());
    com.magneteng.restbyexamples.google.model.beans.Result resultEntry = result.getResults().get(0);
    assertEquals(7, resultEntry.getAddress_components().size());
    assertEquals("1600 Amphitheatre Parkway, Mountain View, CA 94043, USA", resultEntry.getFormatted_address());
    com.magneteng.restbyexamples.google.model.beans.Location location = resultEntry.getGeometry().getLocation();
    assertEquals("37.4220033", location.getLat().toString());
    assertEquals("-122.0839778", location.getLng().toString());
  }
}
