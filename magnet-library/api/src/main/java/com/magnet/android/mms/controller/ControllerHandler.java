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

import com.magnet.android.core.GenericRestConstants;
import com.magnet.android.core.MagnetRestRequestType;
import com.magnet.android.core.MagnetRestRequestType.ParamStyle;
import com.magnet.android.mms.MagnetMobileClient;
import com.magnet.android.mms.async.AsyncCallOptions;
import com.magnet.android.mms.async.AsyncManager;
import com.magnet.android.mms.async.AsyncService.Envelope;
import com.magnet.android.mms.async.AsyncService.Payload;
import com.magnet.android.mms.async.Call;
import com.magnet.android.mms.async.StateChangedListener;
import com.magnet.android.mms.async.SyncCall;
import com.magnet.android.mms.connection.ConnectionService;
import com.magnet.android.mms.connection.ConnectionService.Request;
import com.magnet.android.mms.connection.ConnectionService.Response;
import com.magnet.android.mms.connection.ConnectionService.Response.Status;
import com.magnet.android.mms.controller.RequestSchema.JMethod;
import com.magnet.android.mms.controller.RequestSchema.JParam;
import com.magnet.android.mms.exception.HttpCallException;
import com.magnet.android.mms.exception.MobileException;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.exception.SchemaException;
import com.magnet.android.mms.request.RequestBodyFactory;
import com.magnet.android.mms.request.RequestBodyMarshaller;
import com.magnet.android.mms.request.marshall.SimpleParamHelper;
import com.magnet.android.mms.utils.Util;
import com.magnet.android.mms.utils.logger.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @hide
 */
public class ControllerHandler {

  private static final String LOG_TAG = ControllerHandler.class.getSimpleName();
  private String contentType;
  private String acceptType;
  private String encodingType = GenericRestConstants.MIME_ENCODING_NONE;
  private File requestFile;

  private long contentLength;
  private AtomicBoolean paramMapInited = new AtomicBoolean(false);

  private Map<JParam, Object> uriParamMap = new LinkedHashMap<JParam, Object>();
  private Map<JParam, Object> bodyParamMap = new LinkedHashMap<JParam, Object>();
  private Map<JParam, Object> headerParamMap = new LinkedHashMap<JParam, Object>();


  /**
   * Default handler using generic REST endpoint
   */
  ControllerHandler() {
    reset();
  }

  public void reset() {
    resetToDefaults();
  }

  private void resetToDefaults() {
    contentType = GenericRestConstants.CONTENT_TYPE_JSON;
    acceptType = GenericRestConstants.ACCEPT_ALL;
    encodingType = GenericRestConstants.MIME_ENCODING_NONE;

    paramMapInited.set(false);
    uriParamMap.clear();
    bodyParamMap.clear();
    headerParamMap.clear();

  }

  synchronized public <T> Call<T> makeCall(MagnetMobileClient magnetClient, String connName, JMethod schema,
                                           Object[] realArgs, Object callArg) {
    // reset all the internal fields
    AsyncManager async = AsyncManager.getInstance(magnetClient.getAppContext());
    if (async == null) {
      // this mean local bind is not finished yet but we can't wait on main thread?
      throw new MobileRuntimeException("local async service not available. try again later");
    }

    resetToDefaults();
    initParamMaps(schema, realArgs);

    String uriStr = buildUri(schema, realArgs);

    Payload payload = buildRequestPayload(schema, realArgs);
    Envelope envelop;
    LinkedHashMap<String, String> headers;
    if (headerParamMap.size() > 0) {
      headers = buildHeaderParams(schema, realArgs);
    } else {
      headers = new LinkedHashMap<String, String>();
    }
    headers.put(GenericRestConstants.Header.ACCEPT, acceptType);
    envelop = new Envelope(connName, schema.metaInfo.restMethod, encodingType, headers);

    Object result;
    if (callArg != null && callArg instanceof StateChangedListener) {
      AsyncCallOptions callOptions = new AsyncCallOptions();
      callOptions.setStateChangedListener((StateChangedListener) callArg);
      result = async.invoke(envelop, uriStr, contentType, payload, callOptions, schema.getReturnType(), schema.getReturnComponentTypes());
    } else {  // log warning
      Log.w(LOG_TAG, "Ignoring unrecognized option parameter in controller call.");
      result = async.invoke(envelop, uriStr, contentType, payload, (AsyncCallOptions) null, schema.getReturnType(), schema.getReturnComponentTypes());
    }
    return (Call<T>) result;
  }

  synchronized public <T> Call<T> makeSyncCall(MagnetMobileClient magnetClient, ConnectionService conn,
                                               JMethod schema, Object[] realArgs, Object callArg) {
    SyncCall<T> callResult = new SyncCall<T>(magnetClient.getAppContext());
    try {

      resetToDefaults();
      initParamMaps(schema, realArgs);

      Request request = conn.createRequest();
      request.setMethod(schema.metaInfo.restMethod);

      request.setPath(buildUri(schema, realArgs));
      Payload payload = buildRequestPayload(schema, realArgs);
      if (payload != null) {
        request.setPayload(payload.getAsRawInputStream());
      }
      request.setContentType(contentType);
      request.setContentTransferEncoding(encodingType);
      request.setHeader("Content-Length", String.valueOf(contentLength));
      if (headerParamMap.size() > 0) {
        LinkedHashMap<String, String> headers = buildHeaderParams(schema, realArgs);
        request.addHeaders(headers);
      }
      request.setHeader(GenericRestConstants.Header.ACCEPT, acceptType);
      // make the call directly for sync calls
      Response response = request.execute();
      // read the response back to a string
      byte[] responseArray = getReponseBuffer(response);
      String contentType = response.getContentType();
      String encoding = response.getContentTransferEncoding();
      if (response.getStatus() != Status.SUCCESS) {
        String responseBody = new String(responseArray);
        HttpCallException ce = new HttpCallException(responseBody, null, response.getResponseCode());
        callResult.setError(ce);
        Log.w(LOG_TAG, "Error making request. Response headers:\n" + response.getHeaders());
        Log.w(LOG_TAG, "Error making request. Response:\n" + responseBody);
      } else {
        callResult.setResponse(responseArray, schema.getReturnType(), schema.getReturnComponentTypes(),
            contentType, encoding);
        if (Log.isLoggable(Log.VERBOSE)) {
          saveToFile(new ByteArrayInputStream(responseArray), ".rsp");
        }
      }
    } catch (IOException e) {
      Log.e(LOG_TAG, "Exception making request.", e);
      callResult.setError(e);
    } finally {
      // clear out request object
      if (requestFile != null) {
        requestFile.delete();
      }
    }
    return callResult;

  }

  private byte[] getReponseBuffer(Response response) throws IOException {
    InputStream is = null;
    try {
      is = response.getPayload();
      byte[] buffer = new byte[1024];
      ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
      int count = 0;
      while ((count = is.read(buffer)) >= 0) {
        byteArray.write(buffer, 0, count);
      }
      byteArray.close();
      return byteArray.toByteArray();
    } finally {
      response.release();
      if (is != null) {
        is.close();
      }
    }
  }

  private String buildFormRequestBody(Map<JParam, Object> formParams) {
    StringBuilder queryBuilder = new StringBuilder();
    if (formParams.isEmpty()) {
      return queryBuilder.toString();
    } else {
      contentType = GenericRestConstants.CONTENT_TYPE_FORM_URLENCODED;
    }
    Iterator<Entry<JParam, Object>> iterator = formParams.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<JParam, Object> entry = iterator.next();
      Object paramValue = entry.getValue();
      JParam cp = entry.getKey();
      if (paramValue != null) {
        String value = SimpleParamHelper.getPrimitiveParamValueAsString(paramValue);
        if (queryBuilder.length() > 1) {
          queryBuilder.append('&');
        }
        queryBuilder.append(cp.name)
            .append("=").append(value);
      }
    }
    contentLength = queryBuilder.length();
    return queryBuilder.toString();
  }

  public String buildUri(JMethod schema, Object[] args) {
    // validate schema support
    if (RequestSchema.isMultiPart(schema.getMetaInfo().getProduces())) {
      throw new MobileRuntimeException("Backend produces multipart/related content type. Not supported!");
    }
    if (RequestSchema.isMultiPart(schema.getMetaInfo().getConsumes())) {
      throw new MobileRuntimeException("Backend consumes multipart/related content type. Not supported!");
    }
    if (paramMapInited.get() == false) {
      initParamMaps(schema, args);
    }

    // build up URL baseURL + apiPath for generic REST endpoint
    StringBuilder uriBuilder = new StringBuilder();
    if (schema.getMetaInfo().getBaseUrl() != null && schema.getMetaInfo().getBaseUrl().length() > 0) {
      uriBuilder.append(schema.getMetaInfo().getBaseUrl());
    }
    String restApiPath = schema.getMetaInfo().restApiPath;
    if (restApiPath != null && restApiPath.length() > 0) {
      if (!restApiPath.startsWith("/")) {
        uriBuilder.append("/");
      }
      uriBuilder.append(restApiPath);
    }

    if (args != null && args.length > 0) {
      addTemplateParams(uriBuilder, schema, args);
    }
    if (args != null && args.length > 0) {
      addMatrixParams(uriBuilder, schema, args);
    }
    if (args != null && args.length > 0) {
      addQueryParams(uriBuilder, schema, args);
    }
    String result = uriBuilder.toString().trim();
    if (Log.isLoggable(Log.DEBUG)) {
      Log.d(LOG_TAG, "URI for request: method name=" + schema.getMetaInfo().getMethodName() + ";URL=" + result);
    }
    return result;
  }

  public LinkedHashMap<String, String> buildHeaderParams(JMethod schema, Object[] args) {
    if (paramMapInited.get() == false) {
      initParamMaps(schema, args);
    }
    LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
    for (JParam param : headerParamMap.keySet()) {
      Object value = headerParamMap.get(param);
      if (value == null) {
        continue;
      }
      String strValue = SimpleParamHelper.getPrimitiveParamValueAsString(value);
      result.put(param.name, strValue);
    }
    return result;
  }

  public Payload buildRequestPayload(JMethod schema, Object[] args) {
    if (paramMapInited.get() == false) {
      resetToDefaults();
      initParamMaps(schema, args);
    }
    OutputStream os = null;
    requestFile = null;
    try {
      requestFile = File.createTempFile("reqbody", ".req");
      requestFile.deleteOnExit();
      os = new FileOutputStream(requestFile);
    } catch (IOException e1) {
      // default back to in memory array
      os = new ByteArrayOutputStream();
    }
    buildRequestBody(schema, args, os);
    if (os != null) {
      try {
        os.close();
      } catch (IOException e) {
        Log.w(LOG_TAG, "ignoring unexpected exception closing output stream for request", e);
      }
    }

    Payload payload = null;
    contentLength = 0;
    if (requestFile != null) {
      contentLength = requestFile.length();
      if (contentLength > 0) {
        payload = new Payload(requestFile.getAbsolutePath());
        if (Log.isLoggable(Log.DEBUG)) {
          Log.d(LOG_TAG, "using file for payload:" + requestFile.getAbsolutePath());
        }
      }
    } else {
      if (os instanceof ByteArrayOutputStream) {
        ByteArrayOutputStream bos = (ByteArrayOutputStream) os;
        contentLength = bos.size();
        if (contentLength > 0) {
          payload = new Payload(bos.toByteArray());
          if (Log.isLoggable(Log.VERBOSE)) {
            Log.v(LOG_TAG, "using byte buffer for payload:" + bos);
          }
        }
      }
    }
    if (contentLength == 0) {
      if (Log.isLoggable(Log.VERBOSE)) {
        Log.v(LOG_TAG, "content length = 0; no payload");
      }
    }
    return payload;
  }

  public String buildRequestBodyString(JMethod schema, Object[] args) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      buildRequestBody(schema, args, bos);
      bos.flush();
    } catch (IOException e) {
      Log.e(LOG_TAG, "unexpected IOException closing bytearray output stream", e);
    } finally {
      try {
        bos.close();
        // save to file for debugging
        if (Log.isLoggable(Log.VERBOSE)) {
          saveToFile(new ByteArrayInputStream(bos.toByteArray()), ".dat");
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "unexpected IOException closing bytearray output stream", e);
      }
    }
    return (bos.toString());
  }

  public void buildRequestBody(JMethod schema, Object[] args, OutputStream bodyOs) {
    // set correct content type
    try {
      if (!initContentTypes(schema)) {
        throw new SchemaException("only application/json is supported for consumes or produces");
      }

      // no body supported for GET and DELETE - all parameters are treated as query parameters and built from
      // buildUri
      if (ConnectionService.Request.Method.DELETE == schema.metaInfo.restMethod ||
          ConnectionService.Request.Method.GET == schema.metaInfo.restMethod) {
        contentType = null;
        return;
      }

      int totalParams = schema.params.size();
      if (args == null || args.length == 0 || totalParams == 0) {
        return;
      }

      // get form parameters
      final Map<JParam, Object> formParams = findParamMapByStyle(bodyParamMap, ParamStyle.FORM);

      if (!formParams.isEmpty()) {
        String formBody = buildFormRequestBody(formParams);
        bodyOs.write(formBody.getBytes());
        return;
      }

      final Map<JParam, Object> bodyParams = findParamMapByStyle(bodyParamMap, ParamStyle.PLAIN);

      if (bodyParams.size() == 0) {
        return;
      }

      Set<Map.Entry<JParam, Object>> entries = bodyParams.entrySet();
      // create the body from the parameters

      RequestBodyMarshaller marshaller = new RequestBodyFactory().createInstance();
      contentType = marshaller.serializeRequest(schema,
          bodyParams.keySet().toArray(new JParam[entries.size()]),
          bodyParams.values().toArray(), bodyOs);

    } catch (MobileException e) {
      throw new MobileRuntimeException(e);
    } catch (IOException e) {
      throw new MobileRuntimeException(e);
    }
  }

  private void addQueryParams(StringBuilder uriBuilder, JMethod schema, Object[] params) {
//    Map<JParam, Object> queryParams = getSpecialParams(schema, params, MagnetRestRequestType.ParamStyle.QUERY);
    Map<JParam, Object> queryParams = findParamMapByStyle(uriParamMap, MagnetRestRequestType.ParamStyle.QUERY);
    if (queryParams.size() > 0) {
      appendQueryParamsToUrl(uriBuilder, queryParams, "?", "&");
    }
    if (schema.metaInfo.restMethod.equals(ConnectionService.Request.Method.GET) ||
        schema.metaInfo.restMethod.equals(ConnectionService.Request.Method.DELETE)) {
      queryParams = findParamMapByStyle(uriParamMap, MagnetRestRequestType.ParamStyle.PLAIN);
      if (queryParams.size() > 0) {
        appendQueryParamsToUrl(uriBuilder, queryParams, "?", "&");
      }
      queryParams = findParamMapByStyle(uriParamMap, MagnetRestRequestType.ParamStyle.FORM);
      if (queryParams.size() > 0) {
        appendQueryParamsToUrl(uriBuilder, queryParams, "?", "&");
      }
    }
  }

  private void addMatrixParams(StringBuilder uriBuilder, JMethod schema, Object[] params) {
    Map<JParam, Object> queryParams = findParamMapByStyle(uriParamMap, MagnetRestRequestType.ParamStyle.MATRIX);
    if (queryParams.size() > 0) {
      appendQueryParamsToUrl(uriBuilder, queryParams, ";", ";");
    }
  }

  private void appendQueryParamsToUrl(StringBuilder uriBuilder, Map<JParam, Object> queryParams, String start, String delim) {
    StringBuilder queryBuilder;
    if (uriBuilder.toString().endsWith(start)) {
      queryBuilder = new StringBuilder();
    } else {
      queryBuilder = new StringBuilder(start);
    }
    Iterator<Entry<JParam, Object>> iterator = queryParams.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<JParam, Object> entry = iterator.next();
      Object paramValue = entry.getValue();
      JParam cp = entry.getKey();
      if (paramValue != null) {
        String value = SimpleParamHelper.getPrimitiveParamValueAsString(paramValue);
        if (queryBuilder.length() > 1) {
          queryBuilder.append(delim);
        }
        try {
          queryBuilder.append(cp.name)
              .append("=").append(URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          throw new MobileRuntimeException(e);
        }
      }
    }
    if (queryBuilder.length() > 1) {
      uriBuilder.append(queryBuilder);
    }
  }

  /**
   * Iterate the parameters looking for template parameters
   * and add them to the uri.
   */
  private void addTemplateParams(StringBuilder uriBuilder, JMethod schema, Object[] params) {
    Map<JParam, Object> templateParams = findParamMapByStyle(uriParamMap, ParamStyle.TEMPLATE);

    if (templateParams.size() > 0) {
      replaceTemplateParamsInUrl(uriBuilder, templateParams);
    }
    // clear out any {} parameters that is handled as another parameter style 
    ParamStyle[] paramStyles = ParamStyle.values();
    for (ParamStyle paramStyle : paramStyles) {
      if (paramStyle.compareTo(ParamStyle.TEMPLATE) == 0) {
        continue;
      }
      Map<JParam, Object> styleParams = findParamMapByStyle(uriParamMap, paramStyle);

      if (styleParams.size() > 0) {
        clearTemplateParamsInUrl(uriBuilder, styleParams);
      }
    }
  }

  protected void initParamMaps(JMethod schema, Object[] params) {
    if (params == null || params.length == 0) {
      if (schema.params.size() > 0) {
        throw new IllegalArgumentException("missing parameters");
      } else {
        paramMapInited.set(true);
        return;
      }
    }
    if (schema.params.size() != params.length) {
      throw new IllegalArgumentException("number of parameters don't match expected number");
    }
    try {
      for (int idx = 0; idx < schema.params.size(); idx++) {
        final JParam paramDef = schema.params.get(idx);
        final Class<?> actualClz = paramDef.getActualTypeAsClass();

        if (params[idx] == null && !paramDef.optional) {
          throw new IllegalArgumentException("required parameter is null");
        }
        // byte array is always in the body
        if (actualClz == byte[].class || actualClz == Byte[].class ||
            (actualClz == Byte.class && SimpleParamHelper.isCollection(params[idx]))) {
          bodyParamMap.put(paramDef, params[idx]);
          continue;
        }
        if (paramDef.style == ParamStyle.HEADER) {
          // only primitive types allowed for HeaderParam
          if (!SimpleParamHelper.isMarshalledAsPrimitiveType(actualClz) ||
              SimpleParamHelper.isCollectionClass(actualClz)) {
            throw new IllegalArgumentException("header parameter must be primitve type");
          }
          headerParamMap.put(paramDef, params[idx]);
          continue;
        }

        if (ConnectionService.Request.Method.GET == schema.metaInfo.restMethod ||
            ConnectionService.Request.Method.DELETE == schema.metaInfo.restMethod) {
          if (!SimpleParamHelper.isMarshalledAsPrimitiveType(actualClz) ||
              SimpleParamHelper.isCollectionClass(actualClz)) {
            throw new IllegalArgumentException("all parameters must be primitve type for GET and DELETE");
          }
          uriParamMap.put(paramDef, params[idx]);
          continue;
        }
        if (paramDef.style == ParamStyle.QUERY ||
            paramDef.style == ParamStyle.TEMPLATE ||
            paramDef.style == ParamStyle.MATRIX) {
          if (!SimpleParamHelper.isMarshalledAsPrimitiveType(actualClz) ||
              SimpleParamHelper.isCollectionClass(actualClz)) {
            throw new IllegalArgumentException("parameter must be primitve type for style:" + paramDef.style.name());
          }

          uriParamMap.put(paramDef, params[idx]);
          continue;
        }
        // default is to put it in body
        // PLAIN, FORM
        bodyParamMap.put(paramDef, params[idx]);
      }
      paramMapInited.set(true);
    } catch (Exception e) {
      // clear the map to ensure map is not half initialized due to exception
      paramMapInited.set(false);
      uriParamMap.clear();
      bodyParamMap.clear();
      headerParamMap.clear();
      throw new MobileRuntimeException(e);
    }
  }

  private boolean initContentTypes(JMethod schema) {
    // only support JSON, not what is specified in cosumes or produces
    Collection<String> consumes = schema.getMetaInfo().getConsumes();
    Collection<String> produces = schema.getMetaInfo().getProduces();

    if (consumes != null || produces != null) {
      Log.w(LOG_TAG, "ignoring consumes and produces in controller factory:p" + produces+ ";c:"+consumes);
    }
    return true;
  }

  private Map<JParam, Object> findParamMapByStyle(Map<JParam, Object> map, ParamStyle style) {
    Map<JParam, Object> resultParams = new LinkedHashMap<JParam, Object>();

    Set<JParam> paramDefs = map.keySet();
    for (JParam paramDef : paramDefs) {
      if (style.equals(paramDef.style)) {
        resultParams.put(paramDef, map.get(paramDef));
      }
    }
    return resultParams;
  }

  private void replaceTemplateParamsInUrl(StringBuilder uriBuilder, Map<JParam, Object> templateParams) {
    String uriStr = uriBuilder.toString();
    Iterator<Entry<JParam, Object>> iterator = templateParams.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<JParam, Object> entry = iterator.next();
      String replacement = SimpleParamHelper.getPrimitiveParamValueAsString(entry.getValue());
      try {
        replacement = URLEncoder.encode(replacement, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new MobileRuntimeException(e);
      }
      uriStr = uriStr.replaceAll("\\{" + entry.getKey().name + "\\}", replacement);
    }
    uriBuilder.delete(0, uriBuilder.length());
    uriBuilder.append(uriStr);
  }

  private void clearTemplateParamsInUrl(StringBuilder uriBuilder, Map<JParam, Object> params) {
    String uriStr = uriBuilder.toString();
    Iterator<Entry<JParam, Object>> iterator = params.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<JParam, Object> entry = iterator.next();
      uriStr = uriStr.replaceAll("\\{" + entry.getKey().name + "\\}", "");
    }
    uriBuilder.delete(0, uriBuilder.length());
    uriBuilder.append(uriStr);
  }

  private File saveToFile(InputStream input, String ext) {
    try {
      File file = File.createTempFile("fdata", ext);
      FileOutputStream fos = new FileOutputStream(file);
      InputStream dis = input;

      byte[] buf = new byte[1024];
      int count = 0;
      long size = 0;
      while ((count = dis.read(buf)) != -1) {
        fos.write(buf, 0, count);
        size = size + count;
      }
      fos.close();
      dis.close();
      if (Log.isLoggable(Log.DEBUG)) {
        Log.d(LOG_TAG, "saved temp data file:" + file.getAbsolutePath());
      }
      return file;
    } catch (IOException e) {
      throw new MobileRuntimeException("failed to create temp dat file", e);
    } finally {
      try {
        input.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
