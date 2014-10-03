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

package com.magnet.android.mms.controller;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.magnet.android.mms.connection.ConnectionConfigManager;
//import magnetapi.apps.letzgo.api.PriceLevel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import com.magnet.android.core.MagnetRestRequestType.ParamStyle;
import com.magnet.android.mms.connection.ConnectionService;
import com.magnet.android.mms.controller.RequestSchema.JMeta;
import com.magnet.android.mms.controller.RequestSchema.JMethod;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.request.marshall.SimpleParamHelper;


public class RequestPrimitiveTest extends AndroidTestCase {

  private static final String GET = ConnectionService.Request.Method.GET.name();
  private static final String POST = ConnectionService.Request.Method.POST.name();

  private static final String QUERY = "QUERY";
  private static final String PLAIN = "PLAIN";
  private static final String FORM = "FORM";
  private static final String TEMPLATE = "TEMPLATE";
  

  private static final String API_METHOD_LOGIN = "/login";
  private static final String API_METHOD_1 = "/v1/hello";
  private static final String API_METHOD_2 = "/v1/postHello";
  private static final String API_METHOD_POST = "/v1/post";

  private static final String API_METHOD_TEMPLATE_PATH = "/status/{httpStatusCode}";
  private static final String API_METHOD_TEMPLATE_NAME = "/status";

  private static final String API_METHOD_TEMPLATE_PATH_MULTI = "/status/{httpStatusCode}/{msg}";

  static final Logger logger = 
      Logger.getLogger(RequestPrimitiveTest.class.getSimpleName());
  
  public enum StatusEnum {
    STARTED,
    INPROGRESS,
    ENDED
  }

  @SmallTest
  public void testVoidParams() {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("methodName1", API_METHOD_1, GET);
    method.setMetaInfo(metaInfo);
    
    String uriString = handler.buildUri(method, null);
    String bodyString = handler.buildRequestBodyString(method, null);
    assertEquals(uriString, API_METHOD_1);
    logger.log(Level.INFO, "uriString=" + uriString);

    assertTrue(TextUtils.isEmpty(bodyString));
  }
  @SmallTest
  public void testQueryParam() throws UnsupportedEncodingException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("methodName1", API_METHOD_1, GET);
    method.setMetaInfo(metaInfo);
    
    method.addParam("hello", QUERY, String.class, null, "", false);

    String paramValue = new String("What is going on?");
    String uriString = handler.buildUri(method,  new Object[] {paramValue});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {paramValue});
    String encodedValue = URLEncoder.encode(paramValue, "UTF-8");
    String expected = API_METHOD_1 + "?hello=" + encodedValue;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    assertEquals(expected, uriString);
    assertTrue(TextUtils.isEmpty(bodyString));

    // try again but not pass the parameter; expect exception for required parameter
    try {
      handler.reset();
      handler.buildUri(method,  new Object[] {});
      fail("expected exception for missing required parameter");
    } catch (Exception e) {
      assertTrue(true);
    }

    // add optional parameter
    method.addParam("p2bool", QUERY, boolean.class, null, "", true);

    Object[] values = new Object[] {new String(paramValue), true};
    handler.reset();
    uriString = handler.buildUri(method,  values);
    bodyString = handler.buildRequestBodyString(method,  values);
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    expected = API_METHOD_1 + "?hello=" + encodedValue
        + "&p2bool=true";
    assertEquals(expected, uriString);

    // try again with null optional parameter
    handler.reset();
    uriString = handler.buildUri(method,  new Object[] {paramValue, null});
    bodyString = handler.buildRequestBodyString(method,  new Object[] {paramValue, null});
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    expected = API_METHOD_1 + "?hello=" + encodedValue;
    assertEquals(expected, uriString);

  }
  @SmallTest
  public void testMatrixParam() throws UnsupportedEncodingException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("methodName1", API_METHOD_1, GET);
    method.setMetaInfo(metaInfo);
    
    method.addParam("hello", ParamStyle.MATRIX.name(), String.class, null, "", false);

    String paramValue = new String("What is going on?");
    Object[] params = new Object[] {paramValue};
    handler.reset();
    String uriString = handler.buildUri(method,  new Object[] {paramValue});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {paramValue});
    String encodedValue = URLEncoder.encode(paramValue, "UTF-8");
    String expected = API_METHOD_1 + ";hello=" + encodedValue;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    assertEquals(expected, uriString);
    assertTrue(TextUtils.isEmpty(bodyString));
  }
  @SmallTest
  public void testEmptyQueryParam() throws UnsupportedEncodingException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("methodName1", API_METHOD_1, GET);
    method.setMetaInfo(metaInfo);
    
    method.addParam("hello", QUERY, String.class, null, "", true);

    // empty string
    String paramValue = new String("");
    String uriString = handler.buildUri(method,  new Object[] {paramValue});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {paramValue});
    String expected = API_METHOD_1 + "?hello=";
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    assertEquals(expected, uriString);
    assertTrue(TextUtils.isEmpty(bodyString));

    // null string
    String strNull = null;
    handler.reset();
    uriString = handler.buildUri(method,  new Object[] {strNull});
    bodyString = handler.buildRequestBodyString(method,  new Object[] {strNull});
    logger.log(Level.INFO, "uriString=" + uriString);

    assertEquals(API_METHOD_1, uriString);
    assertTrue(TextUtils.isEmpty(bodyString));
  }

  @SmallTest
  public void testEmptyMultiParam() throws UnsupportedEncodingException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("methodName1", API_METHOD_1, GET);
    method.setMetaInfo(metaInfo);
    
    method.addParam("hello", PLAIN, String.class, null, "", false);
    method.addParam("hello2", QUERY, String.class, null, "", true);
    method.addParam("hello3", PLAIN, String.class, null, "", true);

    // empty string
    String paramValue1 = new String("");
    String paramValue2 = new String("");
    String paramValue3 = new String("");

    String uriString = handler.buildUri(method,  new Object[] {paramValue1, paramValue2, paramValue3});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {paramValue1, paramValue2, paramValue3});
    String encodedValue = URLEncoder.encode(paramValue2, "UTF-8");
    String expected = API_METHOD_1 + "?hello2=" + encodedValue;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertTrue(TextUtils.isEmpty(bodyString));

    // null strings - expect exception for required parameters
    try {
      handler.reset();
      uriString = handler.buildUri(method,  new Object[] {null, null, null});
      bodyString = handler.buildRequestBodyString(method,  new Object[] {null, null, null});
      fail("expected exception for required parameters set to null");
    } catch (MobileRuntimeException me) {
      
    }

    handler.reset();
    uriString = handler.buildUri(method,  new Object[] {"", null, null});
    bodyString = handler.buildRequestBodyString(method,  new Object[] {"", null, null});
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

//    assertEquals(expected, uriString);
//    assertEquals(bodyString.length(), 0);
  }

  @SmallTest
  public void testSingleTemplateParam() {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod(String.class);
    JMeta metaInfo = new JMeta("getStatus", API_METHOD_TEMPLATE_PATH, GET);
    method.setMetaInfo(metaInfo);

    method.addParam("httpStatusCode", TEMPLATE, int.class, null, "", false);
 
    Object[] values = new Object[] { 200 };
    String uriString = handler.buildUri(method,  values);
    String bodyString = handler.buildRequestBodyString(method, values);
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    String expected = API_METHOD_TEMPLATE_NAME + "/" + String.valueOf(200);
    assertEquals(expected, uriString);
  }

  @SmallTest
  public void testMultiTemplateParam() throws UnsupportedEncodingException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod(String.class);
    JMeta metaInfo = new JMeta("getStatus", API_METHOD_TEMPLATE_PATH_MULTI, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("httpStatusCode", TEMPLATE, int.class, null, "", false);
    method.addParam("msg", TEMPLATE, String.class, null, "", false);

    String msg = "What's up doc?";
    Object[] values = new Object[] { 200 , msg};
    String uriString = handler.buildUri(method,  values);
    String bodyString = handler.buildRequestBodyString(method, values);
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    String encodedValue = URLEncoder.encode(msg, "UTF-8");

    String expected = API_METHOD_TEMPLATE_NAME + "/" + String.valueOf(200) + "/" + encodedValue;
    assertEquals(expected, uriString);
    assertTrue(TextUtils.isEmpty(bodyString));
  }

  @SmallTest
  public void testMultiParam() throws UnsupportedEncodingException, JSONException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod(String.class);
    JMeta metaInfo = new JMeta("getStatus", API_METHOD_TEMPLATE_PATH_MULTI, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("httpStatusCode", TEMPLATE, int.class, null, "", false);
    method.addParam("msg", TEMPLATE, String.class, null, "", false);
    method.addParam("status", QUERY, StatusEnum.class, null, "", false);
    method.addParam("comments", PLAIN, String.class, null, "", false);
    method.addParam("matrix", ParamStyle.MATRIX.name(), String.class, null, "", false);
    method.addParam("header", ParamStyle.HEADER.name(), String.class, null, "", false);

    String msg = "What's up doc?";
    StatusEnum enumParam = StatusEnum.INPROGRESS;
    String comments = "the food is terric but service was TOO SLOW!!";
    String matrix = "sort up";
    String header = "vin=xyz87f";

    Object[] values = new Object[] { 200 , msg, enumParam, comments, matrix, header };

    String uriString = handler.buildUri(method,  values);
    String bodyString = handler.buildRequestBodyString(method, values);
    Map<String, String> headerMap = handler.buildHeaderParams(method, values);

    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    String encodedValue = URLEncoder.encode(msg, "UTF-8");
    String encodedValueMatrix = URLEncoder.encode(matrix, "UTF-8");

    String expected = API_METHOD_TEMPLATE_NAME + "/" + String.valueOf(200) + "/" + encodedValue
        + ";matrix=" + encodedValueMatrix
        + "?status=" + enumParam.toString();

    assertEquals(expected, uriString);
    assertEquals(comments, bodyString);  // single POST parameter

    assertEquals(headerMap.size(), 1);
    assertTrue(headerMap.containsKey("header"));
    assertEquals(headerMap.get("header"), header);
  }

  @SmallTest
  public void testMultiCoerceQueryParam() throws UnsupportedEncodingException, JSONException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod(String.class);
    JMeta metaInfo = new JMeta("getPost", API_METHOD_TEMPLATE_PATH, GET);
    method.setMetaInfo(metaInfo);

    method.addParam("httpStatusCode", TEMPLATE, int.class, null, "", false);
    method.addParam("status", QUERY, StatusEnum.class, null, "", false);
    method.addParam("groupBy", PLAIN, String.class, null, "", false);
    method.addParam("matrix", ParamStyle.MATRIX.name(), String.class, null, "", false);
    method.addParam("header", ParamStyle.HEADER.name(), String.class, null, "", false);


    StatusEnum enumParam = StatusEnum.INPROGRESS;
    String groupBy = "groupeName ascend";
    String matrix = "sort up";
    String header = "vin=xyz87f";

    Object[] values = new Object[] { 200 , enumParam, groupBy, matrix, header };

    String uriString = handler.buildUri(method,  values);
    String bodyString = handler.buildRequestBodyString(method, values);
    Map<String, String> headerMap = handler.buildHeaderParams(method, values);

    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    String encodedValue = URLEncoder.encode(groupBy, "UTF-8");
    String encodedValueMatrix = URLEncoder.encode(matrix, "UTF-8");

    String expected = API_METHOD_TEMPLATE_NAME + "/" + String.valueOf(200)
        + ";matrix=" + encodedValueMatrix
        + "?status=" + enumParam.toString()
        + "?groupBy=" + encodedValue;

    assertEquals(expected, uriString);
    assertTrue(TextUtils.isEmpty(bodyString));  // GET requests have no body

    assertEquals(headerMap.size(), 1);
    assertTrue(headerMap.containsKey("header"));
    assertEquals(headerMap.get("header"), header);
  }
  @SmallTest
  public void testLoginFormParam() {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod(String.class);
    JMeta metaInfo = new JMeta("login", API_METHOD_LOGIN, POST);
    method.setMetaInfo(metaInfo);
    
    method.addParam("authority", FORM, String.class, null, "", false);
    method.addParam("name", FORM, String.class, null, "", false);
    method.addParam("password", FORM, String.class, null, "", false);


    String[] paramValues = new String[] {"magnet", "test", "foo" };
    String uriString = handler.buildUri(method, paramValues);
    String bodyString = handler.buildRequestBodyString(method, paramValues);

    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);

    assertEquals(API_METHOD_LOGIN, uriString);
    assertEquals("authority=magnet&name=test&password=foo", bodyString);
  }

  @SmallTest
  public void testSinglePostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("postHello", API_METHOD_2, POST);
    method.setMetaInfo(metaInfo);
    
    method.addParam("param0", PLAIN, String.class, null, "", false);

    String paramValue = new String("Hello John Smith!");
    String uriString = handler.buildUri(method,  new Object[] {paramValue});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {paramValue});

    String expected = API_METHOD_2;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);
    assertEquals(paramValue, bodyString);

  }

  @SmallTest
  public void testMultPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("postHello",API_METHOD_2, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("paramStr", PLAIN, String.class, null, "", false);
    method.addParam("paramInt", PLAIN, int.class, null,"", false);
    method.addParam("paramLong", PLAIN, long.class, null,"", false);
    method.addParam("paramShort", PLAIN, short.class, null,"", false);
    method.addParam("paramFloat", PLAIN, float.class, null,"", false);
    method.addParam("paramDouble", PLAIN, double.class, null,"", false);
    method.addParam("paramBoolean", QUERY, boolean.class,null, "", false);
    method.addParam("paramChar", PLAIN, char.class, null,"", false);
    method.addParam("paramByte", PLAIN, byte.class, null,"", false);

    String paramStr = new String("Hello John Smith!");
    int paramInt = 890122;
    long paramLong = 40L;
    short paramShort = -999;
    float paramFloat = new Random().nextFloat();
    double paramDouble = Double.MAX_VALUE;
    boolean paramBoolean = false;
    char paramChar = 'Y';
    byte paramByte = 0x25;

    Object[] args = {paramStr, paramInt, paramLong, paramShort, paramFloat, paramDouble,
                     paramBoolean, paramChar, paramByte };
    String uriString = handler.buildUri(method,  args);
    String bodyString = handler.buildRequestBodyString(method,  args);

    String expected = API_METHOD_2 + "?paramBoolean=" + paramBoolean;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);
    JSONObject jsonBody = new JSONObject(bodyString);

    assertEquals(jsonBody.getString("paramStr"), paramStr);
    assertEquals(jsonBody.getInt("paramInt"), paramInt);
    assertEquals(jsonBody.getLong("paramLong"), paramLong);
    assertEquals(jsonBody.getInt("paramShort"), (int)paramShort);
    assertEquals(Float.parseFloat(jsonBody.getString("paramFloat")), paramFloat);
    assertEquals(Double.toString(jsonBody.getDouble("paramDouble")), Double.toString(paramDouble));
    assertEquals(jsonBody.getString("paramChar"), String.valueOf(paramChar));
    assertEquals(jsonBody.getString("paramByte"), String.valueOf(paramByte));

  }
  @SmallTest
  public void testHeaderParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("postHelloHttpHeader",API_METHOD_2, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("paramStr", ParamStyle.HEADER.name(), String.class, null, "", false);
    method.addParam("paramInt", ParamStyle.HEADER.name(), int.class, null,"", false);
    method.addParam("paramDouble", ParamStyle.HEADER.name(), double.class, null,"", false);
    method.addParam("paramBoolean", ParamStyle.HEADER.name(), boolean.class,null, "", false);

    String paramStr = new String("Hello John Smith!");
    int paramInt = 890122;
    long paramLong = 40L;
    double paramDouble = new Random().nextDouble();
    boolean paramBoolean = false;

    Object[] args = {paramStr, paramInt, paramDouble, paramBoolean  };
    handler.reset();
    Map<String, String> headerMap = handler.buildHeaderParams(method, args);
    String uriString = handler.buildUri(method,  args);
    String bodyString = handler.buildRequestBodyString(method,  args);

    String expected = API_METHOD_2;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);
    assertTrue(bodyString == null || bodyString.length() == 0);
    for (String paramName: headerMap.keySet()) {
      if (paramName.equals("paramStr")) {
        assertEquals(headerMap.get(paramName), paramStr);
      } else if (paramName.equals("paramInt")) {
        assertEquals(Integer.parseInt(headerMap.get(paramName)), paramInt);
      } else if (paramName.equals("paramDouble")) {
        assertEquals(Double.parseDouble(headerMap.get(paramName)), paramDouble);
      } else if (paramName.equals("paramBoolean")) {
        assertEquals(Boolean.parseBoolean(headerMap.get(paramName)), paramBoolean);
      }
    }
  }
  @SmallTest
  public void testMultPostWrapperParam() throws JSONException, URISyntaxException, UnsupportedEncodingException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("postHello", API_METHOD_2, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("paramInt", PLAIN, Integer.class, null,"", false);
    method.addParam("paramLong", PLAIN, Long.class, null,"", false);
    method.addParam("paramShort", PLAIN, Short.class,null, "", false);
    method.addParam("paramFloat", PLAIN, Float.class, null,"", false);
    method.addParam("paramDouble", PLAIN, Double.class, null,"", false);
    method.addParam("paramBoolean", PLAIN, Boolean.class, null,"", false);
    method.addParam("paramChar", PLAIN, Character.class, null,"", false);
    method.addParam("paramByte", PLAIN, Byte.class, null,"", false);
    method.addParam("paramDate", PLAIN, Date.class, null,"", false);
    method.addParam("paramEnum", PLAIN, Enum.class, null,"", false);
    method.addParam("paramUri", PLAIN, URI.class, null,"", false);
    method.addParam("redirectUri", QUERY, URI.class, null,"", false);

    Integer paramInt = 890122;
    Long paramLong = 40L;
    Short paramShort = -999;
    Float paramFloat = new Random().nextFloat();
    Double paramDouble = new Random().nextDouble();
    Boolean paramBoolean = true;
    Character paramChar = 'Y';
    Byte paramByte = 0x25;
    Date paramDate = new Date();
    StatusEnum paramEnum = StatusEnum.ENDED;
    URI paramURI = new URI("http://www.cnn.com");
    URI redirectUri = new URI("http://www.msnbc.com:8080/sandboxes");

    Object[] args = {paramInt, paramLong, paramShort, paramFloat, paramDouble,
                     paramBoolean, paramChar, paramByte,
                     paramDate, paramEnum, paramURI, redirectUri };
    String uriString = handler.buildUri(method,  args);
    String bodyString = handler.buildRequestBodyString(method,  args);
    String encodedValue = URLEncoder.encode(redirectUri.toString(), "UTF-8");
    String expected = API_METHOD_2 + "?redirectUri=" + encodedValue;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);
    JSONObject jsonBody = new JSONObject(bodyString);

    assertEquals(jsonBody.getInt("paramInt"), paramInt.intValue());
    assertEquals(jsonBody.getLong("paramLong"), paramLong.longValue());
    assertEquals(jsonBody.getInt("paramShort"), (int)paramShort);
    assertEquals(Float.parseFloat(jsonBody.getString("paramFloat")), paramFloat);
    assertEquals(Double.toString(jsonBody.getDouble("paramDouble")), Double.toString(paramDouble));
    assertEquals(jsonBody.getString("paramBoolean"), String.valueOf(paramBoolean));
    assertEquals(jsonBody.getString("paramChar"), String.valueOf(paramChar));
    assertEquals(jsonBody.getString("paramByte"), String.valueOf(paramByte));
    assertEquals(jsonBody.getString("paramDate"), SimpleParamHelper.sDateTimeFormat.format(paramDate));
    assertEquals(jsonBody.getString("paramEnum"), paramEnum.toString());
    assertEquals(jsonBody.getString("paramUri"), paramURI.toString());

  }
  @SmallTest
  public void testSingleListStringPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("postHello", API_METHOD_2, POST);
    method.setMetaInfo(metaInfo);
    
    method.addParam("param0", PLAIN, List.class, String.class,  "", false);

    String paramValue1 = new String("Hello John Smith!");
    String paramValue2 = new String("Wsup Jackson Brown?");
    String paramValue3 = new String("This is the best day ever.");
    List<String> values = new ArrayList<String>();
    values.add(paramValue1);
    values.add(paramValue2);
    values.add(paramValue3);
    
    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_2;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (String value: values) {
      assertEquals(jarray.get(idx), value);
      idx++;
    }
  }
  @SmallTest
  public void testSingleListIntegerPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postIntegers";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName,  POST);
    method.setMetaInfo(metaInfo);

    method.addParam("param0", PLAIN,  List.class, Integer.class, "", false);

    List<Integer> values = new ArrayList<Integer>();
    values.add(3456);
    values.add(1);
    values.add(-999);
    
    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (Integer value: values) {
      assertEquals(jarray.get(idx), value);
      idx++;
    }
  }

  @SmallTest
  public void testEmptySingleListIntegerParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postIntegers";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName,  POST);
    method.setMetaInfo(metaInfo);

    method.addParam("param0", PLAIN, List.class, Integer.class,"", false);

    // null value in the list
    List<Integer> values = new ArrayList<Integer>();
    values.add(3456);
    values.add(null);
    values.add(-999);
    
    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);
    JSONArray jarray = new JSONArray(bodyString);

    int idx=0;
    for (Integer value: values) {
      assertEquals(jarray.get(idx), value);
      idx++;
    }
    // empty list
    ControllerHandler handler2 = new ControllerHandler();
    Object[] params = new Object[] {new ArrayList<Integer>()};
    handler2.initParamMaps(method, params );
    bodyString = handler2.buildRequestBodyString(method, params);
    assertEquals("[]", bodyString);

  }

  @SmallTest
  public void testSingleIntArrayPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postIntegers";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("param0", PLAIN, int[].class, null, "", false);

    int[] values = {3466, 0, -889, -1};

    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (int value: values) {
      assertEquals(jarray.getInt(idx), value);
      idx++;
    }
  }

  @SmallTest
  public void testSingleBooleanArrayPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postBooleans";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("param0", PLAIN, Boolean[].class, null, "", false);

    Boolean[] values = {true, false, true};

    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);
    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (boolean value: values) {
      assertEquals(jarray.getBoolean(idx), value);
      idx++;
    }
  }
  @SmallTest
  public void testSingleListLongPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postLongs";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("param0", PLAIN, List.class, Long.class, "", false);

    List<Long> values = new ArrayList<Long>();
    values.add(3456L);
    values.add(1L);
    values.add(-999L);
    
    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (Long value: values) {
      assertEquals(jarray.getLong(idx), value.longValue());
      idx++;
    }
  }

  @SmallTest
  public void testSingleListShortPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postShorts";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("param0", PLAIN,   List.class, Short.class,"", false);

    List<Short> values = new ArrayList<Short>();
    values.add((short)3456);
    values.add((short)1);
    values.add((short)0);
    
    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (Short value: values) {
      assertEquals(jarray.getLong(idx), value.longValue());
      idx++;
    }
  }

  @SmallTest
  public void testSingleListDoublePostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postDoubles";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    // int
    method.addParam("param0", PLAIN,  List.class, Double.class, "", false);

    List<Double> values = new ArrayList<Double>();
    values.add(Math.random());
    values.add(Math.PI);

    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (Double value: values) {
      assertEquals(jarray.get(idx), value);
      idx++;
    }
  }
  @SmallTest
  public void testSingleListFloatPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postFloats";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    // int
    method.addParam("param0", PLAIN,  List.class, Float.class,"", false);

    List<Float> values = new ArrayList<Float>();
    values.add(Float.MAX_VALUE);
    values.add(Float.MIN_NORMAL);

    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (Float value: values) {
      String sf = jarray.getString(idx);
      float f = Float.parseFloat(sf);
      assertEquals(value, f);
      idx++;
    }
  }
  @SmallTest
  public void testSingleListBigDecimalPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postBigDecimals";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    // int
    method.addParam("param0", PLAIN, List.class, BigDecimal.class, "", false);

    List<BigDecimal> values = new ArrayList<BigDecimal>();
    values.add(BigDecimal.TEN);
    values.add(BigDecimal.valueOf(101.99));

    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (BigDecimal value: values) {
      assertEquals(jarray.getString(idx), value.toString());
      idx++;
    }
  }

  @SmallTest
  public void testSingleListBigIntegerPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postBigDecimals";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    // int
    method.addParam("param0", PLAIN, List.class, BigInteger.class, "", false);

    List<BigInteger> values = new ArrayList<BigInteger>();
    values.add(BigInteger.TEN);
    values.add(BigInteger.probablePrime(5, new Random()));

    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected =  API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (BigInteger value: values) {
      assertEquals(jarray.getString(idx), value.toString());
      idx++;
    }
  }
  @SmallTest
  public void testSingleListBooleanPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postBooleans";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("postBooleans",  API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    // int
    method.addParam("param0", PLAIN,  List.class, Boolean.class, "", false);
    List<Boolean> values = new ArrayList<Boolean>();
    values.add(true);

    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (Boolean value: values) {
      assertEquals(jarray.getBoolean(idx), value.booleanValue());
      idx++;
    }
  }

  @SmallTest
  public void testSingleListCharPostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postChars";
    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta("postChars", API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    // int
    method.addParam("param0", PLAIN, List.class, Character.class, "", false);

    List<Character> values = new ArrayList<Character>();
    values.add('m');
    values.add('a');
    values.add('g');
    values.add('n');
    values.add('e');
    values.add('\u00a0');

    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (Character value: values) {
      assertEquals(jarray.getString(idx), value.toString());
      idx++;
    }
  }
  
  @SmallTest
  public void testSingleListBytePostParam() throws JSONException {
    ControllerHandler handler = new ControllerHandler();
    String methodName = "postChars";

    JMethod method = new JMethod();
    JMeta metaInfo = new JMeta(methodName, API_METHOD_POST+methodName, POST);
    method.setMetaInfo(metaInfo);

    method.addParam("param0", PLAIN, List.class, Byte.class, "", false);

    List<Byte> values = new ArrayList<Byte>();
    values.add((byte) 0x03);
    values.add((byte) 0x20);

    String uriString = handler.buildUri(method,  new Object[] {values});
    String bodyString = handler.buildRequestBodyString(method,  new Object[] {values});

    String expected = API_METHOD_POST+methodName;
    logger.log(Level.INFO, "uriString=" + uriString);
    logger.log(Level.INFO, "bodyString=" + bodyString);
    assertEquals(expected, uriString);

    JSONArray jarray = new JSONArray(bodyString);
    int idx=0;
    for (Byte value: values) {
      assertEquals(jarray.getInt(idx), value.intValue());
      idx++;
    }
  }
  @SmallTest
  public void testSingleObjListPostParam() {
    
  }
}
