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
package com.magnet.android.mms.exception;

import java.util.concurrent.ExecutionException;

/**
 * This class is a wrapper for HTTP errors for controller request executions.
 * In controller request where HTTP failure occurs, an instance of this exception can be obtained
 * via callback {@link com.magnet.android.mms.async.StateChangedListener}. The same exception is
 * thrown via {@link com.magnet.android.mms.async.Call#get()} method call.
 * Cause of the exception can be due to:
 * <ul>
 * <li>Connection problems such as network issues or authentication failure</li>
 * <li>Incorrect request URL</li>
 * <li>Error return from server due to incorrect parameters</li>
 * </ul>
 */
public class HttpCallException extends ExecutionException {
  private final int responseCode;

    /**
     * Constructs a new {@code HttpCallException} with the given detail message, the original
     * exception, and the response code.
     * @param message the detail message for this {@code HttpCallException}
     * @param original the original {@link java.lang.Exception}
     * @param code the HTTP response code
     */
  public HttpCallException(String message, Exception original, int code) {
    super(message, original);
    responseCode = code;
  }

  /**
   * Get the HTTP response code from the request.
   * @return
   */
  public int getResponseCode() {
    return responseCode;
  }

  /**
   * Get the HTTP response body from the request.
   * @return
   */
  public String getResponse() {
    return getMessage();
  }
}
