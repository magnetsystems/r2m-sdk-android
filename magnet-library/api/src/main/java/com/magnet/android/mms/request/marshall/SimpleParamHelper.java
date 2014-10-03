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

import com.magnet.android.core.MagnetRestRequestType;
import com.magnet.android.mms.exception.MarshallingException;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.request.ByteArrayHelper;
import com.magnet.android.mms.utils.logger.Log;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;


/**
 * Helper class to getting marshalled string values for native Java primitive and wrapper class objects
 */
public class SimpleParamHelper {
  
  private static final String TAG = SimpleParamHelper.class.getSimpleName();
  private static Map<Class<?>, Class<?>> pTypeMap = new HashMap<Class<?>, Class<?>>();
  private static Map<Class<?>, MagnetRestRequestType.ParamType> wpTypeMap = new HashMap<Class<?>, MagnetRestRequestType.ParamType>();
  public static SimpleDateFormat sDateTimeFormat;

  static {
    TimeZone utc = TimeZone.getTimeZone("UTC");
    sDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    sDateTimeFormat.setTimeZone(utc);
  }

  static {
    pTypeMap.put(int.class, Integer.class);
    pTypeMap.put(long.class, Long.class);
    pTypeMap.put(short.class, Short.class);
    pTypeMap.put(double.class, Double.class);
    pTypeMap.put(float.class, Float.class);
    pTypeMap.put(boolean.class, Boolean.class);
    pTypeMap.put(char.class, Character.class);
    pTypeMap.put(byte.class, Byte.class);

    // wrapper for primitive types
    // marshalled names come from (./core/core/common/attr/src/.../AttributeType.java)

    wpTypeMap.put(String.class,  MagnetRestRequestType.ParamType.STRING);
    wpTypeMap.put(Integer.class, MagnetRestRequestType.ParamType.INTEGER);
    wpTypeMap.put(Long.class, MagnetRestRequestType.ParamType.LONG);
    wpTypeMap.put(Short.class, MagnetRestRequestType.ParamType.SHORT);
    wpTypeMap.put(Float.class, MagnetRestRequestType.ParamType.FLOAT);
    wpTypeMap.put(Double.class, MagnetRestRequestType.ParamType.DOUBLE);
    wpTypeMap.put(Boolean.class, MagnetRestRequestType.ParamType.BOOLEAN);
    wpTypeMap.put(Character.class, MagnetRestRequestType.ParamType.CHAR);
    wpTypeMap.put(Byte.class, MagnetRestRequestType.ParamType.BYTE);
    wpTypeMap.put(Enum.class, MagnetRestRequestType.ParamType.ENUM);
    wpTypeMap.put(Date.class, MagnetRestRequestType.ParamType.DATE);
    wpTypeMap.put(BigDecimal.class, MagnetRestRequestType.ParamType.BIG_DECIMAL);
    wpTypeMap.put(BigInteger.class, MagnetRestRequestType.ParamType.BIG_INTEGER);
    wpTypeMap.put(URI.class, MagnetRestRequestType.ParamType.URI);
//    wpTypeMap.put(Collection.class, MagnetRestType.Type.LIST);

  }

  public static boolean isPrimitiveWrapperType(Class<?> type) {
    return wpTypeMap.containsKey(type);
  }

  public static boolean isMarshalledAsPrimitiveType(Class<?> type) {
    return wpTypeMap.containsKey(type) || pTypeMap.containsKey(type) || 
           Enum.class.isAssignableFrom(type);
  }

  public static boolean isCollection(Object obj) {
    return ( List.class.isInstance(obj) ||
             Set.class.isInstance(obj) ||
             Collection.class.isInstance(obj) );
  }
  public static boolean isCollectionClass(Class<?> clazz) {
    if (clazz == null) {
      return false;
    }
    return ( List.class.isAssignableFrom(clazz) ||
             Collection.class.isAssignableFrom(clazz) ||
             Set.class.isAssignableFrom(clazz));
  }
  public static Object formatEmptySimpleObject(Class<?> clazz) {
    if (String.class.equals(clazz)) {
      return new String("");
    }
    if (SimpleParamHelper.isCollection(clazz)) {
      return new ArrayList();
    }
    return null;
  }
  public static String getPrimitiveParamValueAsString(Object value) {
    if (value==null) {
      return null;
    }
    if(value instanceof String) {
        return (String) value;
    }
    if (value instanceof BigDecimal) {
      return ((BigDecimal)value).toString();
    }
    if (value instanceof BigInteger) {
      return ((BigInteger)value).toString();
    }
    if (value instanceof byte[]) {
      return ByteArrayHelper.toBase64((byte[]) value);
    }
    if (value.getClass().isArray() && Byte.class.equals(value.getClass().getComponentType())) {
      return ByteArrayHelper.toBase64(ByteArrayHelper.toByteArrayFromByteWrapperArray((Byte[]) value));
    }
    if (value.getClass().isArray()) {  // byte[]  or Byte[] is the only that gets handled here
      throw new MobileRuntimeException("unexpected array type getting primitive string value");
    }
    if (value instanceof Date) {
      return sDateTimeFormat.format((Date)value);
    }

    if (isMarshalledAsPrimitiveType(value.getClass())) {
      return String.valueOf(value);
    }
    // default - unexpected
    Log.w(TAG, "value is not primitive type; default returning toString");
    return value.toString();
  }
  private static Number getPrimitiveWrapperNumberFromString(String strval, Class<?> numberType) throws MarshallingException {
    if (strval == null || strval.length() == 0 ) {
      return null;
    }
    if (numberType.equals(Integer.class) || numberType.equals(int.class)) {
      Integer intval = Integer.valueOf(strval);
      return intval;
    } else if (numberType.equals(Short.class) || numberType.equals(short.class)) {
      Short shortval = Short.valueOf(strval);
      return shortval;
    } else if (numberType.equals(Long.class) || numberType.equals(long.class)) {
      Long longval = Long.valueOf(strval);
      return longval;
    } else if (numberType.equals(Double.class) || numberType.equals(double.class)) {
      Double dval = Double.valueOf(strval);
      return dval;
    } else if (numberType.equals(Float.class) || numberType.equals(float.class)) {
      Float fval = Float.valueOf(strval);
      return fval;
    }  else if (numberType.equals(Byte.class) || numberType.equals(byte.class)) {
      Byte bval = Byte.valueOf(strval);
      return bval;
    } else if (BigDecimal.class.equals(numberType)) {
      BigDecimal bd = new BigDecimal(strval);
      return bd;
    } else if (BigInteger.class.equals(numberType)) {
      BigInteger bi = new BigInteger(strval);
      return bi;
    }
    throw new MarshallingException("Unsupported type parsing number error from string:" + strval + "; class:" + numberType.getName());
  }

  public static Object getPrimitiveAnyFromString(String strVal, Class<?> primitiveType) throws MarshallingException {
    if (byte[].class.equals(primitiveType)) {
      // create byte array buffer
      byte[] result = ByteArrayHelper.fromBase64(strVal);
      return result;
    }
    if (SimpleParamHelper.isMarshalledAsPrimitiveType(primitiveType)) {
      if ((primitiveType.isPrimitive() && 
          !boolean.class.equals(primitiveType) && !char.class.equals(primitiveType)) 
          || Number.class.isAssignableFrom(primitiveType)) {
        return  SimpleParamHelper.getPrimitiveWrapperNumberFromString(strVal, primitiveType);
      } else {
        return SimpleParamHelper.getPrimitiveObjectFromString(strVal, primitiveType);
      }
    } else {
      throw new MarshallingException("type is not primitive when calling getPrimitiveAny");
    }
  }
  private static Object getPrimitiveObjectFromString(String strval, Class<?> primitiveType) throws MarshallingException{
    if (strval == null || (strval.length() == 0 && !String.class.equals(primitiveType))) {
      return null;
    }

    if (primitiveType.equals(String.class)) {
      return strval;
    } else if (primitiveType.equals(Character.class) || char.class.equals(primitiveType)) {
      return strval.charAt(0);  // single character
    } else if (Enum.class.isAssignableFrom(primitiveType)) {
      Enum enumval = Enum.valueOf((Class<? extends Enum>)primitiveType, strval);
      return enumval;
    } else if (Boolean.class.equals(primitiveType) || boolean.class.equals(primitiveType)) {
      Boolean boolValue = Boolean.valueOf(strval);
      return boolValue;
    } else if (Date.class.equals(primitiveType)) {
      try {
        Date date = sDateTimeFormat.parse(strval);
        return date;
      } catch (ParseException e) {
        throw new MobileRuntimeException("Date parsing error from string:" + strval, e);
      }
    } else if (URI.class.equals(primitiveType)) {
      URI uri;
      try {
        uri = new URI(strval);
        return uri;
      } catch (URISyntaxException e) {
        throw new MobileRuntimeException("uri parsing error from string:" + strval, e);
      }
    }
    throw new MarshallingException("Unsupported type parsing error from string:" + strval + "; class:" + primitiveType.getName());
  }

  public static boolean isPrimitiveTypeName(String fname) {
    return MagnetRestRequestType.ParamType.getTypeForName(fname) != null;
  }
}
