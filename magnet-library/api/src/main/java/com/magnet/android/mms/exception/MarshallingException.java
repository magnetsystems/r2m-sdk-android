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

/**
 * This class is a wrapper for parsing errors from converting controller responses to type-safe objects.
 * In controller request where parsing failure occurs, an instance of this exception can be obtained via callback {@link com.magnet.android.mms.async.StateChangedListener}. The same exception is thrown via {@link com.magnet.android.mms.async.Call#get()} method call.
 * Cause of the exception can be due to:
 * <ul>
 * <li>Mismatch object type between expected type versus actual response</li>
 * <li>Invalid JSON format</li>
 * </ul>
 */
public class MarshallingException extends MobileException {

  private static final long serialVersionUID = 624599124193950100L;
  private String errorContent;

  /**
   * Constructs a new {@code MarshallingException}.
   */
  public MarshallingException() {
    super();
  }

  /**
   * Constructs a new {@code MarshallingException} with the original exception.
   * @param e the original exception.
   */
  public MarshallingException(Exception e) {
    super(e);
  }

  /**
   * Constructs a new {@code MarshallingException} with the message.
   * @param message the message
   */
  public MarshallingException(String message) {
    super(message);
  }

  /**
   * @hide
   * @param content
   */
  public void setErrorContent(String content) {
    errorContent = content;
  }

  /**
   * Return the content from HTTP response that failed parsing.
   * @return
   */
  public String getErrorContent() {
    return errorContent;
  }

}
