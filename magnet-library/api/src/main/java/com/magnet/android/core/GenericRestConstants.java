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

public interface GenericRestConstants {

  public static final class Header {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String ACCEPT = "Accept";
    public static final String USER_AGENT = "User-Agent";
    public static final String LOCATION = "Location";

    public static final String COOKIE = "Cookie";
    public static final String SESSION_ID = "jsessionid";
  }

  public final static String ACCEPT_ALL = "*/*";
  public final static String CONTENT_TYPE_JSON = "application/json";
  public final static String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
  public final static String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

  public final static String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";

  public final static String MIME_ENCODING_NONE = "none";
  public final static String MIME_ENCODING_BASE64 = "base64";



}
