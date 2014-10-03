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

import com.magnet.android.mms.controller.RequestSchema.JMeta;
import com.magnet.android.mms.controller.RequestSchema.JMethod;

import java.util.List;

/**
 * An abstract class which populates RequestSchema for making controller calls
 */
public abstract class AbstractControllerSchemaFactory {

  protected RequestSchema schema;

  /**
   * Method to be implement in sub-class to populate the RequestSchema
   */
  abstract protected void initSchemaMaps();

  /**
   * Return the schema defined for the controller interface
   * @return instance of RequestSchema
   */
  public RequestSchema getSchema() {
    if(null == schema) {
      initSchemaMaps();
    }
    return schema;
  }

  protected JMethod addMethod(String name, String path, String verb, Class<?> returnType, Class<?> returnComponentType, List<String> consumes, List<String> produces) {
 
    // construct JMethod
    // returnType: return type of the method
    // returnComponentType: if returnType is generic or an array/collection, the actual type
    JMethod method = new JMethod(returnType, returnComponentType);

    // Construct JMeta and set meta info
    // Java name of the method (originalName)
    // Rest method (verb)
    // path (path)
    JMeta jMeta = new JMeta(name, path, verb);
    if(null != consumes && !consumes.isEmpty()) {
      jMeta.setConsumes(consumes);
    }
    if(null != produces && !produces.isEmpty()) {
      jMeta.setProduces(produces);
    }
    method.setMetaInfo(jMeta);
    getSchema().putMethod(name, method);
    return method;
  }
}
