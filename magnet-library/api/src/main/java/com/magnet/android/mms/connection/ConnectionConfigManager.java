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

package com.magnet.android.mms.connection;

import android.content.Context;
import android.net.Uri;

import com.magnet.android.mms.AbstractManager;
import com.magnet.android.mms.MagnetMobileClient;
import com.magnet.android.mms.connection.ConnectionConfigManager.ConnectionConfig.ConfigType;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.utils.logger.Log;

import java.util.HashMap;

/**
 * This manager maintains the backend configurations for this
 * application.
 */
public final class ConnectionConfigManager extends AbstractManager {
  private static final String TAG = ConnectionConfigManager.class.getSimpleName();
  private HashMap<String, ConnectionConfig> mConnectionConfigs = null;
  public static final String DEFAULT_GENERIC_REST_NAME = "_default_generic";

  private ConnectionConfig defaultRestConfig;

  public ConnectionService getDefaultGenericService() {
    // return the generic REST connection service
    return defaultRestConfig.getConnectionService();
  }
  public ConnectionConfig getDefaultGenericRestConfig() {
    return defaultRestConfig;
  }

  /**
   * Represents a connection endpoint.  Each endpoint has an arbitrary
   * name, a URI, and an authentication/authorization handler.
   */
  public static final class ConnectionConfig {
    public enum ConfigType {
    	/**
       * REST connection configuration.
       */
      GENERIC_REST
    };

    private String mName;
    private Uri mUri;
    private ConfigType mConfigType;
    private ConnectionConfigManager mConnectionConfigManager;
    private ConnectionService mConnectionService;


    ConnectionConfig(ConnectionConfigManager connectionConfigManager, String name, ConfigType configType) {
      mConnectionConfigManager = connectionConfigManager;
      mName = name;
      mConfigType = configType;
    }
    ConnectionConfig(ConnectionConfigManager connectionConfigManager,
        String name, Uri uri, ConfigType configType) {
      mConnectionConfigManager = connectionConfigManager;
      mName = name;
      mUri = uri;
      mConfigType = configType;
    }

    @SuppressWarnings("unchecked")
    ConnectionConfig(ConnectionConfigManager connectionConfigManager,
        String name, String uriString, String typeName) throws ClassNotFoundException {

        this(connectionConfigManager, name, uriString != null ? Uri.parse(uriString) : null, ConfigType.valueOf(typeName));
    }

    /**
     * Retrieves the name associated with this connection configuration.
     * This name is the same one used to reference this connection
     * when performing endpoint-specific lookups.
     *
     * @return The name associated with this connection configuration.
     */
    public String getName() {
      return mName;
    }

    /**
     * Retrieves the URI associated with this connection configuration.
     *
     * @return The URI associated with this connection configuration.
     */
    public Uri getUri() {
      return mUri;
    }

    /**
     * Retrieves the configuration type for this connection.
     *
     * @return The configuration type for this connection.
     */
    public ConfigType getConfigType() {
      return mConfigType;
    }


    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return false;
      }
      if (this == other) {
        return true;
      }
      if (!(other instanceof ConnectionConfig)) {
        return false;
      }
      ConnectionConfig otherConfig = (ConnectionConfig) other;

      boolean uriSame = (otherConfig.getUri() == null && mUri == null) || otherConfig.getUri().equals(mUri);
      return otherConfig.getName().equals(mName) &&
             uriSame &&
          otherConfig.getConfigType().equals(mConfigType);
    }

    /**
     * Retrieves the connection service for this connection configuration.
     * @return The connection service for this connection configuration.
     */
    public synchronized ConnectionService getConnectionService() {
      if (mConnectionService == null) {
        try {
          switch (mConfigType) {
           case GENERIC_REST:
            mConnectionService =
               new GenericRestConnectionService(mConnectionConfigManager.getContext());
           break;
          }
        } catch (Exception ex) {
          Log.e(TAG, "getConnectionService(): Unable to create connection service.", ex);
          throw new MobileRuntimeException("Unable to create ConnectionService instance.", ex);
        }
      }
      return mConnectionService;
    }
  }

  /**
   * This method is for testing only and will simulate
   * restarting the app.
   */
  void resetInstance() {
    initInstance(getContext(), getBaseMobileClient());
  }

  /**
   * (Internal Use Only)
   */
  public void initInstance(Context context, MagnetMobileClient baseMobile) {
    Log.v(TAG, "initInstance(): start");
    super.initInstance(context, baseMobile);
    mConnectionConfigs = new HashMap<String, ConnectionConfig>();
    // create the default rest connection config
    defaultRestConfig = new ConnectionConfig(this, DEFAULT_GENERIC_REST_NAME, ConfigType.GENERIC_REST);
    mConnectionConfigs.put(DEFAULT_GENERIC_REST_NAME, defaultRestConfig);
  }

  /**
   * Retrieves the connection configuration for the specified name.
   *
   * @param name The name of the configuration.
   * @return The connection configuration for the specified name, or null if there is no configuration for the specified name.
   */
  public ConnectionConfig getConnectionConfig(String name) {
    synchronized (mConnectionConfigs) {
      return mConnectionConfigs.get(name);
    }
  }

}
