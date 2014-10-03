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
package com.magnet.android.mms.settings;

import android.content.Context;

import com.magnet.android.mms.connection.SslManager;
import com.magnet.android.mms.exception.MobileRuntimeException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class loads factory settings from "res/xml/magnet_app_default.xml". If this file is not present, it uses reasonable defaults.
 */
public class MagnetDefaultSettings {

  private static final Logger logger = Logger.getLogger(MagnetDefaultSettings.class.getSimpleName());
  static {
    java.util.logging.Logger.getLogger("").setLevel(null);
  }

  // XML should be placed in "res/xml/magnet_app_default.xml"
  static final String DEFAULT_XML_FILENAME = "magnet_app_default";

  /**
   * Cache related settings.
   */
  public static final String TAG_CACHE = "cache";
  /**
   * @hide
   * Enables encryption in the data cache.<br/>
   * Valid values are "true" (default), "false".
   */
  public static final String ATTRIB_CACHE_ENCRYPTION_ENABLE = "enable_encryption";

  /** Internal Use Only */
  public static final String TAG_HTTP = "http";
  /** Internal Use Only */
  public static final String ATTRIB_CONNECT_TIMEOUT = "connectTimeoutMillis";
  /** Internal Use Only */
  public static final String ATTRIB_READ_TIMEOUT = "readTimeoutMillis";
  /** Internal Use Only */
  public static final String ATTRIB_SSL_HOSTNAME_VERIFIER = "sslHostnameVerifier";

  private static volatile MagnetDefaultSettings sDefaultSettings;
  private static final Object globalLock = new Object();

  private final HashMap<String, Properties> mParsedSettingsMap = new HashMap<String, Properties>();

  private XmlPullParser mXmlParser;

  private MagnetDefaultSettings() {
    initMaps();
  }

  private void loadFromXmlResource(Context context, int resId) {
    try {
      logger.log(Level.INFO, "loading defaults from resource id=" + resId);
      mXmlParser = context.getResources().getXml(resId);
      if (mXmlParser != null) {
        // parse defaults
        parse(mXmlParser, mParsedSettingsMap);
      }
     } catch (Exception e) {
      logger.log(Level.SEVERE, "error loading magnet default settings file from resId=" + resId, e);
      throw new MobileRuntimeException("fatal error parsing resource id=" + resId, e);
     }
  }

  private void initMaps() {
    mParsedSettingsMap.put(TAG_CACHE, new Properties());
    mParsedSettingsMap.put(TAG_HTTP, new Properties());
  }
  /**
   * Initializes the global instance of MagnetDefaultSettings.
   * Loads the factory default settings from "res/xml/magnet_app_default.xml".
   * @param context Android application context.
   * @return A global instance of MagnetDefaultSettings, or null if the factory settings file does not exist.
   */
  public static synchronized MagnetDefaultSettings getInstance(Context context) {
    synchronized (globalLock) {
      if (sDefaultSettings == null) {
        sDefaultSettings = new MagnetDefaultSettings();
        int resId = context.getResources().getIdentifier(DEFAULT_XML_FILENAME, "xml", context.getPackageName());
        if (resId > 0) {
          sDefaultSettings.loadFromXmlResource(context, resId);
        } else {
          logger.log(Level.WARNING, "no default settings file found");
        }
      }
    }
    return sDefaultSettings;
  }

  /**
   * Initializes the global instance of MagnetDefaultSettings from the user-specified resource.
   * Loads the default settings from the XML resource file (res/xml/&lt;res-id&gt;.xml).
   * @param context Android application context.
   * @param resId The resource ID of the XML file. For example, R.xml.my_app_default.
   * @return A global instance of MagnetDefaultSettings, or null if the resource file does not exist.
   */
  public static synchronized MagnetDefaultSettings getInstanceFromResource(Context context, int resId) {
    if (resId > 0) {
      synchronized (globalLock) {
        sDefaultSettings = new MagnetDefaultSettings();
        sDefaultSettings.loadFromXmlResource(context, resId);
      }
      return sDefaultSettings;
    } else {
      return null;
    }
  }

  /**
   * Set the global instance to null to force reload of settings on next call to {@link #getInstance(Context)}
   */
  public void release() {
    // set global instance to null to force reload on next getInstance()
    synchronized (globalLock) {
      sDefaultSettings = null;
    }
  }
  /**
   * Initializes the global instance of MagnetDefaultSettings using default values, without a settings file.
   * Loads values using internal hard-coded values.
   *
   * @return A global instance of MagnetDefaultSettings.
   */
  public static synchronized MagnetDefaultSettings getInstanceDefault() {
    sDefaultSettings = new MagnetDefaultSettings();
    return sDefaultSettings;
  }
  /**
   * @hide
   * Indicates whether cache encryption is enabled.
   * @return <code>false</code> if disabled by the factory default settings file; <code>true</code> otherwise.
   */
  public boolean getCacheEncryptionEnabled() {
    boolean result;
    Boolean defVal = getBooleanValue(TAG_CACHE, ATTRIB_CACHE_ENCRYPTION_ENABLE);
    result = (defVal != null) ? defVal : AndroidSettingsConstants.PREFS_KEY_DEFAULT_ENABLE_DATA_ENCRYPTION;
    return result;
  }

  /**
   * Retrieves the HTTP connect timeout in milliseconds.
   * @return The HTTP connect timeout in milliseconds.
   */
  public int getHttpConnectTimeoutMillis() {
    int result;
    Integer defVal = getIntValue(TAG_HTTP, ATTRIB_CONNECT_TIMEOUT);
    result = (defVal != null) ? defVal.intValue() : 10000;
    return result;
  }

  /**
   * Retrieves the HTTP read timeout in milliseconds.
   * @return The HTTP read timeout in milliseconds.
   */
  public int getHttpReadTimeoutMillis() {
    int result;
    Integer defVal = getIntValue(TAG_HTTP, ATTRIB_READ_TIMEOUT);
    result = (defVal != null) ? defVal.intValue() : 30000;
    return result;
  }

  /**
   * Retrieves the SSL Hostname Verifier to use.  Possible values are <code>STRICT</code>, <code>ALLOW_ALL</code>, and <code>BROWSER_COMPAT</code>.
   * @return The SSL Hostname Verifier to use.
   */
  public String getSslHostnameVerifier() {
    String result;
    String defVal = mParsedSettingsMap.get(TAG_HTTP).getProperty(ATTRIB_SSL_HOSTNAME_VERIFIER);
    result = (defVal != null) ? defVal : SslManager.HostnameVerifierEnum.BROWSER_COMPAT.name();
    return result;
  }

  /** Internal Use Only */
  public Boolean getBooleanValue(String tag, String propName) {
    Boolean result = null;
    Properties props = mParsedSettingsMap.get(tag);
    if (props != null) {
      String strVal = props.getProperty(propName);
      if (strVal != null) {
        result = Boolean.parseBoolean(strVal);
      }
    }
    return result;
  }

  /** Internal Use Only */
  public Integer getIntValue(String tag, String propName) {
    Integer result = null;
    Properties props = mParsedSettingsMap.get(tag);
    if (props != null) {
      String strVal = props.getProperty(propName);
      if (strVal != null) {
        result = Integer.parseInt(strVal);
      }
    }
    return result;
  }

  /** Internal Use Only */
  public Long getLongValue(String tag, String propName) {
    Long result = null;
    Properties props = mParsedSettingsMap.get(tag);
    if (props != null) {
      String strVal = props.getProperty(propName);
      if (strVal != null) {
        result = Long.parseLong(strVal);
      }
    }
    return result;
  }
  // generic parsing of XML with tags and attributes; no text
  static void parse(XmlPullParser parser, HashMap<String, Properties> result ) {
    int evt;
    try {
      parser.next();
      evt = parser.getEventType();
      while (evt != XmlPullParser.END_DOCUMENT) {
        if (evt == XmlPullParser.START_DOCUMENT) {
          evt = parser.nextTag();  // skip to the first tag
          continue;
        } else if (evt == XmlPullParser.START_TAG) {
          // get settings from attributes
          String tagName = parser.getName();
          Properties props;
          if ( (props=result.get(tagName)) == null) {
            props = new Properties();
            result.put(tagName, props);
          }
          int count = parser.getAttributeCount();
          while (count-- > 0) {
            props.put(parser.getAttributeName(count), parser.getAttributeValue(count));
          }
        }
        evt = parser.next();
      }
    } catch (XmlPullParserException e) {
      logger.log(Level.SEVERE, "error parsing magnet default settings file", e);
      throw new MobileRuntimeException("fatal error parsing magnet_default_settings.xml", e);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "error parsing magnet default settings file", e);
      throw new MobileRuntimeException("fatal error parsing magnet_default_settings.xml", e);
    }
  }
}
