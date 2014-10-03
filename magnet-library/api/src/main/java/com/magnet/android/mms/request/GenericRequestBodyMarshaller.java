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

import com.google.gson.Gson;
import com.magnet.android.core.GenericRestConstants;
import com.magnet.android.mms.controller.RequestSchema;
import com.magnet.android.mms.controller.RequestSchema.JParam;
import com.magnet.android.mms.exception.MobileException;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.request.marshall.GsonStreamWriter;
import com.magnet.android.mms.exception.MarshallingException;
import com.magnet.android.mms.request.marshall.SimpleParamHelper;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;


public class GenericRequestBodyMarshaller implements RequestBodyMarshaller {

  private static final String TAG = GenericRequestBodyMarshaller.class.getSimpleName();
  private Gson requestGson;


  public GenericRequestBodyMarshaller() {
    initGsonAdapters();
  }

  public void initGsonAdapters() {
    // .serializeNulls();
    requestGson = MagnetGsonFactory.createMagnetGson();
  }

  @Override
  public String serializeRequest(RequestSchema.JMethod schema, JParam[] params, Object[] args, OutputStream requestOs) throws MobileException {
    if (params.length == 1 && args[0] instanceof String) {
      // pass through the parameter as is
      try {
        requestOs.write(((String) args[0]).getBytes());
        return GenericRestConstants.CONTENT_TYPE_TEXT_PLAIN;
      } catch (IOException e) {
        throw new MobileException(e);
      }
    } else {
      writeJsonBody(schema, params, args, requestOs);
      return GenericRestConstants.CONTENT_TYPE_JSON;
    }
  }

  /**
   * Create a JSONObject for request payload
   * @param schema Original controller method schema
   * @param params List of method parameter definitions for the args
   * @param args List of method args
   * @param requestOs
   * @return
   * @throws com.magnet.android.mms.exception.MarshallingException
   * @throws com.magnet.android.mms.exception.MobileRuntimeException - JSON exception
   */
  public void writeJsonBody(final RequestSchema.JMethod schema, final JParam[] params,
      final Object[] args, OutputStream requestOs) throws MarshallingException {

    GsonStreamWriter gw;

    gw = new GsonStreamWriter(requestOs);

    if (params.length > 1) {
      gw.startObject();
    }
    for (int idx=0; idx<params.length; idx++) {
      JParam param = params[idx];

      if (!param.optional && args[idx] == null) {
        throw new MobileRuntimeException("required parameter ["+ param.name + "] is null!");
      }

      if (args[idx] == null) {
        gw.writeNullValue();
      }

      Object argObj = args[idx];

      // primitive or primitive wrapper types with arrays
      Class<?> actualClz = param.getActualTypeAsClass();

      if (params.length > 1 && SimpleParamHelper.isMarshalledAsPrimitiveType(actualClz)) {
        String valueToWrite = SimpleParamHelper.getPrimitiveParamValueAsString(argObj);
        gw.writeFieldName(param.name);
        gw.writeValue(valueToWrite);
        continue;
      }

      // write from POJO
      if (params.length > 1) {
        // write the name of the parameter
        gw.startNamedObject(param.name);
      }
      if (SimpleParamHelper.isCollection(argObj)) {
        writeJsonFromPojoCollection(gw, argObj, actualClz, null);
      } else {
        writeJsonFromPojo(gw, argObj, actualClz);
      }
    }

    if (params.length > 1) {
      gw.endObject();
    }
    gw.finishDocument();
  }

  protected void writeJsonFromPojo(GsonStreamWriter gw,  Object obj, Type objType) throws MarshallingException {
    requestGson.toJson(obj, objType, gw.getJsonWriter());
  }

  protected void writeJsonFromPojoCollection(GsonStreamWriter gw, Object collObj, Class<?> bclass, String fieldName) throws MarshallingException {
    if (fieldName != null) {
      gw.writeFieldName(fieldName);
    }
    if (collObj == null) {  // write null
      gw.writeNullValue();
      return;
    }
    Iterable iterObj = Iterable.class.cast(collObj);
    final Iterator iter = iterObj.iterator();

    gw.beginArray();
    while (iter.hasNext()) {
      Object item = iter.next();
      if (item != null) {
        writeJsonFromPojo(gw, item, bclass);
      } else {
        gw.writeNullValue();
      }
    }
    gw.endArray();
  }
  protected String toJsonFromPojo(Object obj) throws JSONException, MarshallingException, IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    GsonStreamWriter gw = new GsonStreamWriter(os);
    writeJsonFromPojo(gw, obj, obj.getClass());
    gw.getJsonWriter().close();
    os.close();
    return new String(os.toByteArray());
  }

  protected String toJsonFromPojoArray(Collection<?> obj, Class<?> objectClass) throws IOException, MarshallingException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    GsonStreamWriter gw = new GsonStreamWriter(os);
    writeJsonFromPojoCollection(gw, obj, objectClass, null);
    gw.getJsonWriter().close();
    os.close();
    return new String(os.toByteArray());
  }
}
