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

import com.magnet.android.mms.exception.MarshallingException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;


/**
 * Implements the primary switch logic .
 */
public abstract class AbstractJsonWriterAdapter implements JsonWriterAdapter {

  public void writeField(String name, Object firstAttributeValue) throws MarshallingException {
    writeFieldName(name);
    writeFieldValue(firstAttributeValue);
  }

  public void writeFieldValue(Object attributeValue) throws MarshallingException {

    if (attributeValue==null) {
      writeNullValue();
      return;
    }
    Class<?> attrClazz = attributeValue.getClass();

    // TODO: Consider optimizing this to a lookup table on class name
    if (attrClazz.isAssignableFrom(String.class)) {
      writeValue((String)attributeValue);
      return;
    }
    if (attrClazz.isAssignableFrom(Integer.class)) {
      writeValue(((Integer) attributeValue).intValue());
      return;
    }
    if (attrClazz.isAssignableFrom(Long.class)) {
      writeValue(((Long) attributeValue).longValue());
      return;
    }
    if (attrClazz.isAssignableFrom(Short.class)) {
      writeValue(((Short) attributeValue).shortValue());
      return;
    }
    if (attrClazz.isAssignableFrom(Float.class)) {
      writeValue(((Float) attributeValue).floatValue());
      return;
    }
    if (attrClazz.isAssignableFrom(Double.class)) {
      writeValue(((Double) attributeValue).doubleValue());
      return;
    }
    if (attrClazz.isAssignableFrom(Boolean.class)) {
      writeValue(((Boolean) attributeValue).booleanValue());
      return;
    }
    if (attrClazz.isAssignableFrom(Date.class)) {
      Date toWrite = (Date) attributeValue;
      String stringToWrite = SimpleParamHelper.sDateTimeFormat.format(toWrite);
      writeValue(stringToWrite);
      return;
    }
    if (attrClazz.isArray()
        && attrClazz.getComponentType().isAssignableFrom(Byte.TYPE)) {
      writeValue((byte[]) attributeValue);
      return;
    }
    if (attrClazz.isAssignableFrom(BigDecimal.class)) {
      writeValue((BigDecimal) attributeValue);
      return;
    }
    if (attrClazz.isAssignableFrom(BigInteger.class)) {
      writeValue((BigInteger) attributeValue);
      return;
    }
    if (attrClazz.isAssignableFrom(Byte.class)) {
      writeValue(((Byte) attributeValue).byteValue());
      return;
    }
    writeValue(attributeValue.toString());
  }
}
