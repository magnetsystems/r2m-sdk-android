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
package com.magnet.android.mms.utils.logger;

import com.magnet.android.mms.BuildConfig;

import java.util.HashMap;

/**
 * Wrapper class for Android logs. This class outputs log information 
 * using the existing {@link android.util.Log} class, prepended
 * with the <code>"MagnetMobile"</code> tag.
 */
public final class Log {
  /** Suppress log level. */
  public final static int SUPPRESS = 1;
  /** Verbose log level. */
  public final static int VERBOSE = 2;
  /** Debug log level. */
  public final static int DEBUG = 3;
  /** Information log level. */
  public final static int INFO = 4;
  /** Warning log level. */
  public final static int WARN = 5;
  /** Error log level. */
  public final static int ERROR = 6;
//  public final static int ASSERT = 7;
//  private final static String[] sLevels = { "", "SUPPRESS", "VERBOSE", 
//                                "DEBUG", "INFO", "WARN", "ERROR", "ASSERT" };
  private final static String sGlobalTag = "MagnetMobile";
  private static int sGlobalLogLevel = BuildConfig.DEBUG ? DEBUG : INFO;
  private static HashMap<String, Integer> sLogTags = new HashMap<String, Integer>();
  
  /**
   * Set the global log level in this process.  The global log level is
   * used if a tag-based log level is not set.
   * @param level The global log level.
   */
  public static void setLoggable(int level) {
    if (level < SUPPRESS || level > ERROR) {
      throw new IllegalArgumentException("Invalid log level: "+level);
    }
    sGlobalLogLevel = level;
  }
  
  /**
   * Determine whether the global log level is at least at the specified level.
   * @param level The global log level.
   * @return <code>true</code> if the global log level is at least at the specified level; <code>false</code> otherwise.
   */
  public static boolean isLoggable(int level) {
    return (sGlobalLogLevel > SUPPRESS) && (level >= sGlobalLogLevel);
  }
  
  /**
   * Set the log level for the specified tag in this process.
   * @param tag The log tag.
   * @param level The log level: {@link #SUPPRESS} , {@link #VERBOSE} , {@link #DEBUG} , 
   *            {@link #INFO} , {@link #WARN} , {@link #ERROR}
   * @return <code>true</code> if the log level was successfully set; otherwise an exception is thrown.
   * @exception IllegalArgumentException Invalid log level.
   */
  public static boolean setLoggable(String tag, int level) {
    if (level < SUPPRESS || level > ERROR) {
      throw new IllegalArgumentException("Invalid log level: "+level);
    }
    sLogTags.put(tag, level);
    return true;
  }
  
  /**
   * Determine whether the log level for the tag is at least at the specified level.  If no
   * level is set for the specified tag, it will compare against {@link #sGlobalLogLevel}.
   * A log level with {@link #SUPPRESS} always returns <code>false</code>.   
   * Use this method or {@link #isLoggable(int)} to guard against 
   * {@link #v(String, String)} and {@link #d(String, String)}.
   * @param tag The log tag.
   * @param level The log level: {@link #VERBOSE}, {@link #DEBUG}, {@link #INFO}, 
   *    {@link #WARN}, {@link #ERROR}
   * @return <code>true</code> if the log level for the tag is at least at the specified level; <code>false</code> otherwise.
   */
  public static boolean isLoggable(String tag, int level) {
    Integer tagLevel = sLogTags.get(tag);
    if (tagLevel == null) {
      return sGlobalLogLevel > SUPPRESS && level >= sGlobalLogLevel;
    } else {
      return tagLevel > SUPPRESS && level >= tagLevel;
    }
  }
  
  /**
   * Sends a verbose log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static int v(String tag, String msg) {
    if (!isLoggable(tag, VERBOSE))
      return 0;
    return android.util.Log.v(tag, getNewMsg(sGlobalTag, msg));
  }

  /**
   * Sends a verbose log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static int v(String tag, String msg, Throwable tr) {
    if (!isLoggable(tag, VERBOSE))
      return 0;
    return android.util.Log.v(tag, getNewMsg(sGlobalTag, msg), tr);
  }
  
  /**
   * Sends a debug log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static int d(String tag, String msg) {
    if (!isLoggable(tag, DEBUG))
      return 0;
    return android.util.Log.d(tag, getNewMsg(sGlobalTag, msg));
  }
  
  /**
   * Sends a debug log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static int d(String tag, String msg, Throwable tr) {
    if (!isLoggable(tag, DEBUG))
      return 0;
    return android.util.Log.d(tag, getNewMsg(sGlobalTag, msg), tr);
  }
  
  /**
   * Sends an information log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static int i(String tag, String msg) {
    return android.util.Log.i(tag, getNewMsg(sGlobalTag, msg));
  }
  
  /**
   * Sends an information log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static int i(String tag, String msg, Throwable tr) {
    return android.util.Log.i(tag, getNewMsg(sGlobalTag, msg), tr);
  }

  /**
   * Sends a warning log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static int w(String tag, String msg) {
    return android.util.Log.w(tag, getNewMsg(sGlobalTag, msg));
  }
  
  /**
   * Sends a warning log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static int w(String tag, String msg, Throwable tr) {
    return android.util.Log.w(tag, getNewMsg(sGlobalTag, msg), tr);
  }

  /**
   * Sends a warning log message.
   * @param tag The log tag.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static int w(String tag, Throwable tr) {
    return android.util.Log.w(tag, getNewMsg(sGlobalTag, null), tr);
  }

  /**
   * Sends an error log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static int e(String tag, String msg) {
    return android.util.Log.e(tag, getNewMsg(sGlobalTag, msg));
  }
  
  /**
   * Sends an error log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static int e(String tag, String msg, Throwable tr) {
    return android.util.Log.e(tag, getNewMsg(sGlobalTag, msg), tr);
  }

  /**
   * Sends a "What a Terrible Failure" log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static int wtf(String tag, String msg) {
    return android.util.Log.wtf(tag, getNewMsg(sGlobalTag, msg));
  }

  /**
   * Sends a "What a Terrible Failure" log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @param tr The log exception.
   * @return The number of bytes written.
   */
  public static int wtf(String tag, String msg, Throwable tr) {
    return android.util.Log.wtf(tag, getNewMsg(sGlobalTag, msg), tr);
  }

  /**
   * Retrieves the stack trace.
   * @param tr The log exception.
   * @return A string containing the stack trace.
   */
  public static String getStackTraceString(Throwable tr) {
    return android.util.Log.getStackTraceString(tr);
  }

  /**
   * Sends a low-level log message.
   * @param priority The priority of the log message.
   * @param tag The log tag.
   * @param msg The log message.
   * @return The number of bytes written.
   */
  public static int println(int priority, String tag, String msg) {
    return android.util.Log.println(priority, tag, getNewMsg(sGlobalTag, msg));
  }
  
  private static String getNewMsg(String tag, String msg) {
    return ((tag == null ? "" : tag) + "[" + Thread.currentThread().getName() + "]" + (msg == null || msg.length() == 0 ? "" : ": " + msg));
  }
}
