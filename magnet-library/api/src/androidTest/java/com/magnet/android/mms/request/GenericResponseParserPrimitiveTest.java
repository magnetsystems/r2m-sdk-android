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


import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.magnet.android.core.GenericRestConstants;
import com.magnet.android.mms.exception.MobileException;
import com.magnet.android.mms.exception.MarshallingException;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class GenericResponseParserPrimitiveTest extends AndroidTestCase {
  static final Logger logger =
          Logger.getLogger(GenericResponseParserPrimitiveTest.class.getSimpleName());


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
  public void testEmptyArrayResponse() throws MobileException {
    GenericResponseParser<List<String>> parser = new GenericResponseParser<List<String>>(String.class);
    Object result = parser.parseResponse("[]".getBytes());
    assertTrue(((List) result).size() == 0);
  }

  @SmallTest
  public void testNullResponse() throws MobileException {
    GenericResponseParser<String> parser = new GenericResponseParser<String>(String.class);
    Object result = parser.parseResponse((byte[]) null);
    assertNull(result);
  }

  @SmallTest
  public void testStringResponse() throws MobileException, IOException {
    GenericResponseParser<String> parser = new GenericResponseParser<String>(String.class);
    String response = "\"Whatever is the deal\"";

    String result = (String) parser.parseResponse(response.getBytes());
    assertEquals("Whatever is the deal", result);

    result = (String) parser.parseResponse((byte[]) null);
    assertEquals(null, result);

    result = (String) parser.parseResponse("\"\"".getBytes());
    assertEquals("", result);

    GenericResponseParser<Character> cparser = new GenericResponseParser<Character>(char.class);
    char cresponse = 'p';

    try {
      Character cresult = (Character) cparser.parseResponse(String.valueOf(cresponse).getBytes("UTF-8"));
      assertEquals(Character.valueOf(cresponse), cresult);
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @SmallTest
  public void testPlainTextResponse() throws MobileException, IOException {
    PlainTextResponseParser<String> parser = new PlainTextResponseParser<String>(String.class, null, GenericRestConstants.CONTENT_TYPE_TEXT_PLAIN, null);
    String response = "Whatever is the deal";

    String result = (String) parser.parseResponse(response.getBytes());
    assertEquals("Whatever is the deal", result);
  }
  @SmallTest
  public void testGenericPrimitiveResponse() throws MobileException, IOException {
    GenericResponseParser<String> parser = new GenericResponseParser<String>(String.class, null, GenericRestConstants.CONTENT_TYPE_TEXT_PLAIN, null);
    String response = "\"Whatever is the deal\"";

    String result = (String) parser.parseResponse(response.getBytes());
    assertEquals("Whatever is the deal", result);

    result = (String) parser.parseResponse((byte[]) null);
    assertEquals(null, result);

    GenericResponseParser<Character> cparser = new GenericResponseParser<Character>(char.class,  null, GenericRestConstants.CONTENT_TYPE_TEXT_PLAIN, null);
    char cresponse = 'p';

    try {
      Character cresult = (Character) cparser.parseResponse(String.valueOf(cresponse).getBytes("UTF-8"));
      assertEquals(Character.valueOf(cresponse), cresult);
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    GenericResponseParser<Integer> nparser = new GenericResponseParser<Integer>(Integer.class, null, GenericRestConstants.CONTENT_TYPE_TEXT_PLAIN, null);
    String strResponse = Integer.toString(Integer.MIN_VALUE);

    Integer nresult = (Integer) nparser.parseResponse(strResponse.getBytes());
    assertEquals(Integer.MIN_VALUE, nresult.intValue());
  }

  @SmallTest
  public void testStringListResponse() throws MobileException, JSONException {
    GenericResponseParser<String> parser = new GenericResponseParser<String>(List.class, new Type[]{String.class}, null, null);

    String[] values = {"What is the deal?", "Purple blue, and pink", "\"Okay it's time\""};
    JSONArray strArray = new JSONArray();
    int idx = 0;
    for (String value : values) {
      strArray.put(idx++, value);
    }
    String response = strArray.toString();

    Object result = parser.parseResponse(response.getBytes());
    assertTrue(List.class.isInstance(result));
    List<String> array = List.class.cast(result);
    assertEquals(array.size(), values.length);
    assertEquals(array.get(0), values[0]);
    assertEquals(array.get(1), values[1]);
    assertEquals(array.get(2), values[2]);

  }

  @SmallTest
  public void testNumberResponse() throws MobileException, JSONException {
    GenericResponseParser<Integer> parser = new GenericResponseParser<Integer>(Integer.class);
    String response = Integer.toString(Integer.MIN_VALUE);

    Integer result = (Integer) parser.parseResponse(response.toString().getBytes());
    assertEquals(Integer.MIN_VALUE, result.intValue());

    Object nullresult = parser.parseResponse((byte[]) null);
    assertEquals(null, nullresult);

    long longvalue = new Random().nextLong();
    GenericResponseParser<Long> lparser = new GenericResponseParser<Long>(Long.class);
    long longresult = (Long) lparser.parseResponse(Long.toString(longvalue).getBytes());
    assertEquals(longvalue, longresult);

    short shortvalue = Short.MAX_VALUE;
    GenericResponseParser<Short> sparser = new GenericResponseParser<Short>(short.class);
    short shortresult = (Short) sparser.parseResponse(Short.toString(shortvalue).getBytes());
    assertEquals(shortvalue, shortresult);

    float floatvalue = new Random().nextFloat();
    GenericResponseParser<Float> fparser = new GenericResponseParser<Float>(Float.class);
    float floatresult = (Float) fparser.parseResponse(Float.toString(floatvalue).getBytes());
    assertEquals(String.valueOf(floatvalue), String.valueOf(floatresult));
    // use string as the value of the float

    double doublevalue = new Random().nextDouble();
    GenericResponseParser<Double> dparser = new GenericResponseParser<Double>(double.class);
    Double doubleresult = (Double) dparser.parseResponse(Double.toString(doublevalue).getBytes());
    assertEquals(String.valueOf(doublevalue), String.valueOf(doubleresult));

    byte bytevalue = Byte.MIN_VALUE + 10;
    GenericResponseParser<Byte> bparser = new GenericResponseParser<Byte>(Byte.class);
    Byte byteresult = (Byte) bparser.parseResponse(Byte.toString(bytevalue).getBytes());
    assertEquals(bytevalue, byteresult.byteValue());

  }

  @SmallTest
  public void testJavaLangObjResponse() throws MobileException, URISyntaxException, JSONException {

    Boolean boolValue = true;
    GenericResponseParser<Boolean> barser = new GenericResponseParser<Boolean>(Boolean.class);
    Boolean bresult = (Boolean) barser.parseResponse(boolValue.toString().getBytes());
    assertEquals(boolValue, bresult);
  }


  @SmallTest
  public void testMalformatResponse() {
    // overflow tests or value loss
    long longvalue = Long.MAX_VALUE;
    GenericResponseParser<Short> lparser = new GenericResponseParser<Short>(short.class);
    try {
      short longresult = (Short) lparser.parseResponse(String.valueOf(longvalue).getBytes());
      fail("should get exception for overflow short value");
    } catch (MarshallingException e) {
      assertTrue(true);
    }

  }
}
