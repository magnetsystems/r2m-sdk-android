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
package com.magnet.android.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes the properties of one of the attributes of a property
 * It consists of the name and class
 * information for the property.  It is sub classed
 * such that each attribute provides access to type specific
 * properties.
 * 
 */
public interface MagnetRestRequestType {

  /**
   * Describes the legal set of recognized types for
   * use in Node definitions as well as parameters to controllers.
   * All of the primitive types are prefaced with "_" with the exception
   * of the "magnet-type".  Complex types are either "_node" or are 
   * the name of an "attribute schema that can be resolved through
   * the SchemaManager service.
   */
  enum ParamType {
    STRING("_string"),
    ENUM("_enum"),
    BOOLEAN("_boolean"),
    BYTE("_byte"),
    CHAR("_char"),
    SHORT("_short"),
    INTEGER("_integer"),
    LONG("_long"),
    FLOAT("_float"),
    DOUBLE("_double"),
    BIG_DECIMAL("_big_decimal"),
    BIG_INTEGER("_big_integer"),
    DATE("_date"),
    URI("_uri"),
    MAGNET_URI("magnet-uri"),
    LIST("_list"),
    DATA("_data"),
    BYTES("_bytes"),
    REFERENCE("_reference"),
    MAGNET_NODE("_node"),
    MAGNET_PROJECTION("_projection"),
    OBJECT("_object"),
    MAGNET_BEAN("_bean");

    private final String marshalledName;

    ParamType(String marshalledName) {
      this.marshalledName = marshalledName;
    }

    public String getMarshalledName() {
      return marshalledName;
    }

    public static final ParamType getTypeForName(String name) {
      return LAZY.mapping.get(name);
    }

    private static final class LAZY {
      static final Map<String,ParamType> mapping = new HashMap<String,ParamType>();
      static {
        for (ParamType t: values()) {
          mapping.put(t.getMarshalledName(), t);
        }
      }
    }
  }

  enum ParamStyle {
    TEMPLATE,
    FORM,
    QUERY,
    PLAIN,
    HEADER, MATRIX
  }

}
