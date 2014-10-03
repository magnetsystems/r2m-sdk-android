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


import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import com.magnet.android.core.GenericRestConstants;
import com.magnet.android.mms.connection.ConnectionConfigManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.magnet.android.mms.controller.RequestPrimitiveTest;
import com.magnet.android.mms.exception.MobileException;
import com.magnet.android.mms.request.marshall.SimpleParamHelper;

public class ResponsePrimitiveTest extends AndroidTestCase {
  static final Logger logger = 
      Logger.getLogger(RequestPrimitiveTest.class.getSimpleName());

  @SmallTest
  public void testVoidResponse() throws MobileException {
    GenericResponseParser parser = new GenericResponseParser(Void.class);
    Object result = parser.parseResponse("Whatever".getBytes());
    assertNull(result);
  }

  @SmallTest
  public void testPrimitiveVoidResponse() throws MobileException {
    GenericResponseParser parser = new GenericResponseParser(void.class);
    Object result = parser.parseResponse("Whatever".getBytes());
    assertNull(result);
  }

  @SmallTest
  public void testEmptyResponse() throws MobileException {
    GenericResponseParser<List<String>> parser = new GenericResponseParser<List<String>>(String.class);
    List<String> result = parser.parseResponse("[]".getBytes());
    assertTrue(result.size() == 0);
  }

  @SmallTest
  public void testNullResponse() throws MobileException {
    GenericResponseParser<String> parser = new GenericResponseParser<String>(String.class);
    String result = parser.parseResponse("".getBytes());
    assertEquals(0, result.length());
  }

  @SmallTest
  public void testStringListResponse() throws MobileException, JSONException {
    GenericResponseParser<List<String>> parser = new GenericResponseParser<List<String>>(String.class);

    String[] values = { "What is the deal?", "Purple blue, and pink", "\"Okay it's time\"" };
    JSONArray strArray = new JSONArray();
    int idx=0;
    for (String value: values) {
      strArray.put(idx++, value);
    }

    Object result = parser.parseResponse(strArray.toString().getBytes());
    assertTrue(List.class.isInstance(result));
    List<String> array = List.class.cast(result);
    assertEquals(array.size(), values.length);
    assertEquals(array.get(0), values[0]);
    assertEquals(array.get(1), values[1]);
    assertEquals(array.get(2), values[2]);

  }

  @SmallTest
  public void testGenericVoidResponse() throws MobileException {
    ParserFactory pf = new ParserFactory(Void.class);
    ResponseParser parser = pf.createInstance(GenericRestConstants.CONTENT_TYPE_JSON, GenericRestConstants.MIME_ENCODING_NONE);

    Object result = parser.parseResponse("Whatever".getBytes());
    assertNull(result);
  }


  @SmallTest
  public void testGenericStringResponse() throws MobileException {
    ParserFactory pf = new ParserFactory(String.class);
    ResponseParser parser = pf.createInstance(GenericRestConstants.CONTENT_TYPE_JSON, GenericRestConstants.MIME_ENCODING_NONE);

    String result = (String) parser.parseResponse("Whatever".getBytes());
    assertEquals("Whatever", result);
  }

  @SmallTest
  public void testGenericUrlResponse() throws MobileException {
    ParserFactory pf = new ParserFactory(URL.class);
    ResponseParser parser = pf.createInstance(GenericRestConstants.CONTENT_TYPE_JSON, GenericRestConstants.MIME_ENCODING_NONE);

    String strUrl = "http://www.wellsfargo.com:9090";
    String parseStr = "\"" + strUrl + "\"";
    URL result = (URL) parser.parseResponse(parseStr.getBytes());
    assertEquals(strUrl, result.toString());
  }
}
