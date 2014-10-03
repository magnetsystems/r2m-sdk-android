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
 * This class defines schema exceptions that can be thrown from controller initialization.
 * A {@code SchemaException} may occur due to missing dependent classes or general Java class loading failures.
 *
 */
public class SchemaException extends MobileException {

  /**
   * Constructs a new {@code SchemaException}.
   */
  public SchemaException() {
    super();
  }

  /**
   * Constructs a new {@code SchemaException} with the original exception.
   * @param cause the original {@link java.lang.Exception}.
   */
  public SchemaException(Exception cause) {
    super(cause);
  }

    /**
     * Constructs a new {@code SchemaException} with the given detail message and the original exception.
     * @param message the detail message for this {@code SchemaException}
     * @param cause the original {@link java.lang.Exception}
     */
  public SchemaException(String message, Exception cause) {
    super(message, cause);
  }

    /**
     * Constructs a new {@code SchemaException} with the given detail message.
     * @param message the detail message for this {@code SchemaException}
     */
  public SchemaException(String message) {
    super(message);
  }

}
