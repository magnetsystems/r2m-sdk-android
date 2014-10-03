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
import com.magnet.android.mms.connection.ConnectionConfigManager.ConnectionConfig;
import com.magnet.android.mms.controller.RequestSchema.JMethod;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.exception.SchemaException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ControllerProxy implements InvocationHandler {

  private final boolean sync;
  private final ControllerFactory cf;
  private final ConnectionConfig conn;
  private final String connName;
  private final MagnetMobileClient magnetClient;

  public ControllerProxy(ControllerFactory cf, MagnetMobileClient magnetClient, boolean sync, ConnectionConfig conn) {
    this.sync = sync;
    this.cf = cf;
    this.conn = conn;
    this.connName = null;
    this.magnetClient = magnetClient;
  }

  public ControllerProxy(ControllerFactory cf, MagnetMobileClient magnetClient, boolean sync, String connName) {
    this.sync = sync;
    this.cf = cf;
    this.conn = null;
    this.connName = connName;
    this.magnetClient = magnetClient;
  }
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // if myself, make the call?

    if (args == null || args.length == 0) {
      throw new IllegalArgumentException("expected at least one parameter for async call options");
    }

    Object[] realParams = null;
    if (args.length > 1) {
      realParams = new Object[args.length-1];
      System.arraycopy(args, 0, realParams,  0,  args.length-1);
    }
    Object callArg = args[args.length-1];

    // create request for sending over the wire
    ControllerHandler handler = new ControllerHandler();
    JMethod methodSchema = cf.getSchema().getMethod(method.getName(), realParams);
    if (methodSchema == null) {
      throw new MobileRuntimeException(new SchemaException("Method not found:" + method.getName()) + "; may be caused by inconsistent generated controller sources");
    }
    if (sync) {
      return handler.makeSyncCall(magnetClient, conn.getConnectionService(),  methodSchema, realParams, callArg);
    } else {
      return handler.makeCall(magnetClient, connName, methodSchema, realParams, callArg);
    }
  }
}

