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

import com.magnet.android.mms.MagnetMobileClient;
import com.magnet.android.mms.connection.ConnectionConfigManager;
import com.magnet.android.mms.connection.ConnectionConfigManager.ConnectionConfig;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.exception.SchemaException;

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;

/**
 * Use this class to obtain instances of controller classes for making method calls to a specific Magnet backend.
 */
public abstract class ControllerFactory<T> {

  /** (Internal Use Only) The controller Java interface associated with this factory instance. */ 
  public final Class<T> controllerClass;
  private WeakReference<MagnetMobileClient> magnetClientRef;
  /** Internal Use Only  */ 
  protected RequestSchema schema;

  // subclass must implement this
  protected void initSchemaMaps() {
    throw new MobileRuntimeException("no schema implemented for controller");
  }

  /**
   * Internal Use Only: Constructs a new instance of the controller factory.
   */
  public ControllerFactory(Class<T> controller, RequestSchema schema, MagnetMobileClient magnetClient) {
    if (magnetClient == null  || controller == null  || schema == null) {
      throw new IllegalArgumentException("Controller factory fails with null parameter");
    }
    controllerClass = controller;
    this.schema = schema;
    this.magnetClientRef = new WeakReference<MagnetMobileClient>(magnetClient);
  }

  /**
   * Internal Use Only
   */
  public synchronized RequestSchema getSchema() {
    return schema;
  }

  /**
   * Construct a new proxy controller instance for making controller method calls in asynchronous mode.
   * @param connName Name of the connection defining the endpoint.
   * @return A new proxy instance of the Controller class implementing the controller interface. See {@link #getControllerClass()} for more information.
   * @throws SchemaException if the Controller class fails to load. Possible reasons include missing dependent classes in the classpath.
   */
  @SuppressWarnings("unchecked")
  public T obtainInstance(String connName) throws SchemaException {
    if (connName == null || connName.isEmpty()) {
      throw new IllegalArgumentException("Controller instantiation fails with null or empty connName");
    }
    if (magnetClientRef.get() == null) {
      throw new  IllegalStateException("Controller instantiation fails with null MagnetMobileClient.");
    }
    ConnectionConfigManager cm = magnetClientRef.get().getConnectionConfigManager();
    if (cm.getConnectionConfig(connName) == null) {
      throw new IllegalArgumentException("Controller instantiation fails with non-existing connection named:" + connName);
    }
    try {
      Class.forName(controllerClass.getName());
    } catch (ClassNotFoundException e) {
      throw new SchemaException("controller class not found in class path", e);
    }
    ControllerProxy proxy;
    proxy = new ControllerProxy(this, magnetClientRef.get(), false, connName);
    return ((T) Proxy.newProxyInstance(
        magnetClientRef.get().getAppContext().getClassLoader(),
        new Class[]{getControllerClass()}, proxy));
  }

    @SuppressWarnings("unchecked")
    public T obtainInstance() throws SchemaException {

        if (magnetClientRef.get() == null) {
            throw new  IllegalStateException("Controller instantiation fails with null MagnetMobileClient.");
        }
        ConnectionConfigManager cm = magnetClientRef.get().getConnectionConfigManager();
            ConnectionConfig connConfig = cm.getDefaultGenericRestConfig();

        try {
            Class.forName(controllerClass.getName());
        } catch (ClassNotFoundException e) {
            throw new SchemaException("controller class not found in class path", e);
        }
        ControllerProxy proxy;
        proxy = new ControllerProxy(this, magnetClientRef.get(), false, connConfig.getName());
        return ((T) Proxy.newProxyInstance(
                magnetClientRef.get().getAppContext().getClassLoader(),
                new Class[]{getControllerClass()}, proxy));
    }

    /**
   * Internal Use Only: Construct a new proxy controller instance for making controller method calls in synchronous mode.
   * @param conn Connection defining the endpoint.
   * @return A new proxy instance of the Controller class implementing the controller interface. See {@link #getControllerClass()} for more information.
   * @throws SchemaException if the Controller class fails to load. Possible reasons include missing dependent classes in the classpath.
   */
  @SuppressWarnings("unchecked")
  public T obtainSyncInstance(ConnectionConfig conn) throws SchemaException {
    if (conn == null) {
      throw new IllegalArgumentException("Controller instantiation fails with null connection");
    }
    if (magnetClientRef.get() == null) {
      throw new  IllegalStateException("Controller instantiation fails with null MagnetMobileClient.");
    }
    try {
      Class.forName(controllerClass.getName());
    } catch (ClassNotFoundException e) {
      throw new SchemaException("controller class not found in class path", e);
    }
    // a separate instance is needed per connection
    ControllerProxy proxy;
    proxy = new ControllerProxy(this, magnetClientRef.get(), true, conn);
    return ((T) Proxy.newProxyInstance(
        magnetClientRef.get().getAppContext().getClassLoader(),
        new Class[]{getControllerClass()}, proxy));
  }
  /**
   * Retrieve the controller Java interface associated with this factory instance.
   * @return The controller Java interface associated with this factory instance.
   */
  public Class<T> getControllerClass() {
    return controllerClass;
  }
}


