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

package com.magnet.android.mms.request.marshall;

import com.google.gson.stream.JsonWriter;
import com.magnet.android.mms.exception.MarshallingException;
import com.magnet.android.mms.request.ByteArrayHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A GSON implementation of the JsonWriter
 */
public class GsonStreamWriter extends AbstractJsonWriterAdapter {

  private final JsonWriter jw;
  private OutputStreamWriter osw;
  private OutputStream os;

  public GsonStreamWriter(OutputStream outputStream) {
    JsonWriter writer;
    try {
      osw = new OutputStreamWriter(outputStream, "UTF-8");
      writer = new JsonWriter(osw);
    } catch (UnsupportedEncodingException e) {
      osw = new OutputStreamWriter(outputStream);
    }
    writer = new JsonWriter(osw);
    this.os = os;
    this.jw = writer;
    // TODO: Need to write test for https://magneteng.atlassian.net/browse/WON-5358 - Gson marshaller cannot write NaN because JSON spec disallows this value
    this.jw.setLenient(true);
  }

  public GsonStreamWriter(JsonWriter jsonWriter) {
    jw = jsonWriter;
  }
  public JsonWriter getJsonWriter() {
    return jw;
  }
  protected OutputStreamWriter getOutputStreamWriter() {
    return osw;
  }
  protected OutputStream getOutputStream() {
    return os;
  }
  @Override
  public void startDocument() throws MarshallingException {
    startObject();
  }

  @Override
  public void startObject() throws MarshallingException {
    try {
      jw.beginObject();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void startNamedObject(String objectName) throws MarshallingException {
    try {
      jw.name(objectName);
      jw.beginObject();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void endObject() throws MarshallingException {
    try {
      jw.endObject();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void finishDocument() throws MarshallingException {
    try {
      jw.close();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void startArray(String fieldName) throws MarshallingException {
    try {
      jw.name(fieldName);
      jw.beginArray();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void endArray() throws MarshallingException {
    try {
      jw.endArray();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeFieldName(String name) throws MarshallingException {
    try {
      jw.name(name);
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeValue(boolean value) throws MarshallingException {
    try {
      jw.value(value);
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeValue(double value) throws MarshallingException {
    try {
      if (Double.isNaN(value) || Double.isInfinite(value)) {
        // Probably a Gson bug: lenient is applicable only to Number type.
        jw.value(Double.valueOf(value));
      } else {
        jw.value(value);
      }
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeValue(long value) throws MarshallingException {
    try {
      jw.value(value);
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeValue(int value) throws MarshallingException {
    try {
      jw.value(value);
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeValue(float value) throws MarshallingException {
    try {
      if (Float.isNaN(value) || Float.isInfinite(value)) {
        // Probably a Gson bug: lenient is applicable only to Number type.
        jw.value(Float.valueOf(value));
      } else {
        jw.value(value);
      }
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeValue(BigDecimal value) throws MarshallingException {
    try {
      jw.value(value);
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }


  @Override
  public void writeValue(BigInteger value) throws MarshallingException {
    try {
      jw.value(value);
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeValue(String value) throws MarshallingException {
    try {
      jw.value(value);
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeValue(byte[] value) throws MarshallingException {
    try {
      jw.value(ByteArrayHelper.toBase64(value));
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void writeNullValue() throws MarshallingException {
    try {
      jw.nullValue();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void beginArray() throws MarshallingException {
    try {
      jw.beginArray();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }
  public void writeFromInputStream(String name, InputStream is, boolean append) throws Exception {
    // write inputstream as a String
    // first, write deferredName
    //invokeDeferredNameMethod();

    OutputStreamWriter writer = this.getOutputStreamWriter();
    // write the field name
    if (append) {  // add comma if this is supposed to come after another field
      writer.write(", \"");
    }
    writer.write(name);
    writer.write("\": ");

    writer.write("\"");
    InputStreamReader reader = new InputStreamReader(is, "UTF-8");
    char[] buf = new char[1024];  // read 1k at a time
    int count;
    while ((count = reader.read(buf)) != -1) {
      writer.write(buf, 0, count);
    }
    writer.write("\"");
    writer.flush();
    reader.close();
  }
}
