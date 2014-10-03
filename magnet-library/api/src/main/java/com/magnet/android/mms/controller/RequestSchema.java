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

import com.magnet.android.core.MagnetRestRequestType;
import com.magnet.android.mms.connection.ConnectionService;
import com.magnet.android.mms.connection.ConnectionService.Request.Method;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RequestSchema {

  public final static String DEFAULT_CONTENT_TYPE = "application/json";
  public final static String MULTIPART_RELATED_CONTENT_TYPE = "multipart/related";

  public static boolean isMultiPart(Collection<String> types) {
    if (types == null) {
      return false;
    }
    for (String type: types) {
      if (type.equals(MULTIPART_RELATED_CONTENT_TYPE)) {
        return true;
      }
    }
    return false;
  }
  // describes the schema for how to construct a controller request over the wire from Java class
  public static class JMethod {
    JMeta metaInfo;
    List<JParam> params = new ArrayList<JParam>();
    private final Class<?> returnType;      // return type of the method. if collection, the type of the element
    private final Type[] returnComponentTypes;  // return component or templated types if returnType is array or generic class

    public JMethod() {
      returnType = Void.class;
      returnComponentTypes = null;
    }
    /**
     * Constructs a new JMethod object
     * @param returnType return type of the method. Use this if returnType is not a generic or collection
     */
    public JMethod(Class<?> returnType) {
      this.returnType = returnType;
      this.returnComponentTypes = null;
    }
    /**
     * Constructs a new JMethod object
     * @param returnType return type of the method
     * @param returnComponentType if returnType is generic or array/collection, the type of the actual objects
     */
    public JMethod(Class<?> returnType, Class<?> returnComponentType) {
      this.returnType = returnType;
      if (returnComponentType != null) {
        this.returnComponentTypes = new Type[1];
        this.returnComponentTypes[0] = returnComponentType;
      } else {
        this.returnComponentTypes = null;
      }
    }
    public JMethod setBaseUrl(String baseUrl) {
      this.metaInfo.baseUrl = baseUrl;
      return this;
    }
    public boolean equals(Object method) {
      boolean result;
      JMethod m = (JMethod) method; 
      result = this.metaInfo.methodName.equals(m.metaInfo.methodName);
      if (result) {
        result = this.metaInfo.restApiPath.equals(m.metaInfo.restApiPath);
      }
      if (result) {
        result = this.metaInfo.restMethod.compareTo(m.metaInfo.restMethod) == 0;
      }
      if (result) {
        result = this.params.size() == m.params.size();
      }
      if (result) {
        // compare each parameter and see if it's the same
        for (int i=0; result && i<this.params.size() ; i++) {
          JParam param = this.params.get(i);
          JParam compParam = m.params.get(i);
          result = param.equals(compParam);
        }
      }
      return result;
    }
    public Class<?> getReturnType()  {
      return returnType;
    }
    public JMethod setMetaInfo(JMeta meta) {
      this.metaInfo = meta;
      return this;
    }

    public JParam addParam(String name, String style, Class<?> paramClass, Class<?> componentClass, String typeName, boolean optional) {
      JParam param = new JParam(name, style, paramClass, componentClass, typeName, optional);
      this.params.add(param);
      return param;
    }
    public List<JParam> getParams() {
      return Collections.unmodifiableList(params);
    }
    public JMeta getMetaInfo() {
      return metaInfo;
    }
    public Type[] getReturnComponentTypes() {
      return returnComponentTypes;
    }
    /**
     * Return the actual class that the method returns. 
     * For generic or array/collection, returns the class of the component objects.
     * @return
     */
    public Class<?> getActualReturnTypeAsClass() {
      if (returnComponentTypes == null) {
        return returnType;
      } else {
        return Class.class.cast(returnComponentTypes[0]);
      }
    }
  }

  public static class JMeta {
    public final ConnectionService.Request.Method restMethod;    // "POST","GET"
    public final String restApiPath;   // "/<rest-api-path>"
    private String methodName;  // Java method name
    private Collection<String> produces;    // "produces" : [ "multipart/related" ]
    private Collection<String> consumes;    // "consumes" : [ "multipart/related" ]
    private String baseUrl;   // base URL, typically the host URL to the 3rd party Rest service

    public JMeta(String methodName, String path, String restMethod) {
      this.methodName = methodName;
      this.restApiPath = path;
      this.restMethod = Method.valueOf(restMethod);

    }
    public void setMethodName(String methodName) {
      this.methodName = methodName;
    }
    public String getMethodName() {
      return methodName;
    }
    public void setProduces(List<String> produces) {
      this.produces = produces;
    }
    public Collection<String> getProduces() {
      return produces;
    }
    public void setConsumes(List<String> consumes) {
      this.consumes = consumes;
    }
    public Collection<String> getConsumes() {
      return consumes;
    }
    public void setBaseUrl(String path) {
      baseUrl = path;
    }
    public String getBaseUrl() {
      return baseUrl;
    }
  }

  /**
   * PLAIN style:
   * Payload for primitive parameter is constructed based:
   * <name>: value
   *
   * Entity aClass: "magnet-aClass" is added to Each JSON rep of the entity
   *
   * "magnet-class": "<typeName>"
   * "<prop-name>": <prop-value>
   *
   * QUERY style (primitive class only):
   * value is added to the URL
   * /<rest-api-path>/<cmeta.name>?<cparam.name>=value
   *
   * FORM style - form-urlencoded
   * <cparam.nama>=value&<cparam.nama>=value
   *
   * TEMPLATE style:
   *
   */
  public static class JParam {

    public final String name;   // "name"  (the label used in constructing label in json)
    public final MagnetRestRequestType.ParamStyle style;  // "style" (QUERY,PLAIN,FORM,TEMPLATE)
    private final Class<?> paramClz;  // Java class of the parameter;
    private final Type[] actualClz;   // if paramClz is generic

    public final boolean optional;   // true,false
    private String typeName;         // magnet-type

    public JParam(String name, String style, Class<?> paramClz, Class<?> componentClz, String typeName, boolean optional) {
      this.name = name;
      this.style = MagnetRestRequestType.ParamStyle.valueOf(style);
      this.optional = optional;
      this.typeName = typeName;
      if (componentClz != null) {
        this.actualClz = new Type[1];
        this.actualClz[0] = componentClz;
      } else {
        this.actualClz = null;
      }
      this.paramClz = paramClz;
    }
    public boolean equals(Object param) {
      JParam p = (JParam) param;
      boolean result = this.getParamType().equals(p.getParamType());
      if (result) {
        result = this.getActualTypeAsClass().equals(p.getActualTypeAsClass());
      }
      return result;
    }
    /*
     *  return true if the instance can be considered the same type as the parameter spec
     */
    public boolean isInstanceOf(Object actualObj) {
      boolean result;
      Class<?> actualType = actualObj.getClass();
      // since there is no way to get the actual type of for generics based on object instance,
      // compare the type only
      result = this.getParamType().equals(actualType);
      return result;
    }
    public void setTypeName() {
      this.typeName = typeName;
    }
    public String getTypeName() {
      return typeName;
    }
    public Class<?> getParamType() {
      return paramClz;
    }
    public Type[] getActualTypes() {
      return actualClz;
    }
    /**
     * Return the actual type of the parameter.
     * If paramType is generic, returns the templated type from the first item in the type list
     * @return
     */
    public Class<?> getActualTypeAsClass() {
      if (actualClz == null) {
        return paramClz;
      } else {
        return Class.class.cast(actualClz[0]);
      }
    }
  }

  private Map<String, List<JMethod>> overloadMethodMap;

  private final Map<String, JMethod> methods = new HashMap<String, JMethod>();
  private String apiRootPath;

  public RequestSchema() {
  }
  /**
   * TODO Construct a JMethod suitable for Java proxy construction
   * @param schema Schema of the class in JSON representation
   * @return instance of JMethod
   * @throws java.lang.RuntimeException if schema is invalid
   */
  public static RequestSchema fromJson(JSONObject schema) {
    return new RequestSchema();
  }
  /**
   * TODO Create JSON representation of the schema
   * @return schema in JSON
   */
  public static JSONObject toJson() {
    return new JSONObject();
  }

  // root path to the controller API; could be useful for diff. versions
  // e.g. v1/postHello, v2/postHello, etc
  public void setRootPath(String path) {
    this.apiRootPath = path;
  }

  public String getRootPath() {
    return apiRootPath;
  }

  public void putMethod(String methodName, JMethod method) {
    JMethod existingMethod = methods.get(methodName);
    if (existingMethod == null || existingMethod != null && existingMethod.equals(method)) {
      methods.put(methodName, method);
      return;
    }
    // add to list of existing methods
    if (overloadMethodMap == null) {
      overloadMethodMap = new HashMap<String, List<JMethod>>();
    }
    List<JMethod> methodList = overloadMethodMap.get(methodName);
    if (methodList == null) {
      methodList = new ArrayList<JMethod>();
      if (existingMethod != null) {
        methodList.add(existingMethod);
        // remove it from normal map
        methods.remove(methodName);
      }
      overloadMethodMap.put(methodName, methodList);
    }
    // add it to the method list
    methodList.add(method);
  }


  public JMethod getMethod(String methodName) {
    return methods.get(methodName);
  }

  // find match for method based on methodName and parameters for overloaded methods
  public JMethod getMethod(String methodName, Object[] args) {
    JMethod method = methods.get(methodName);
    if (method != null) {
      return method;
    }
    List<JMethod> overloadMethods = overloadMethodMap.get(methodName);
    if (overloadMethods != null) {
      for (JMethod m: overloadMethods) {
        // compare each parameter
        if ((args == null || args.length == 0) && m.params.size() == 0) {
          return m;
        }
        if (m.params.size() != args.length) {
          continue;
        }
        int i = 0;
        boolean same = false;
        for (JParam param: m.params) {
          // see if it's the same type
          same = param.isInstanceOf(args[i++]);
          if (!same) {
            continue;
          }
        }
        if (same) {
          return m;
        }
      }
      // should have found match in the method list
      return null;
    }
    return null;
  }
  public Map<String, JMethod> getMethodMap() {
    return Collections.unmodifiableMap(methods);
  }
  // use it when code is obfuscated to rebind method names to method meta map
  public void bindMethodName(String plainName, String encodedName) {
    JMethod method = methods.get(plainName);
    if (method != null) {
      methods.remove(plainName);
      methods.put(encodedName, method);
    }
  }
  // use it when code is obfuscated to rebind method names to method meta map
  public void bindMethodNameMap(HashMap<String, String> encNameMap) {
    for (String key : encNameMap.keySet()) {
      bindMethodName(key, encNameMap.get(key));
    }
  }
}
