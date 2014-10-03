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
package com.magnet.android.mms.async;

import android.content.Context;

/**
 * A default handler with a context used as a callback from an asynchronous call. The caller
 * must override {@link #onError(Call, Throwable)}, {@link #onExecuting(Call, StateChangedListener.ProgressData)}
 * and {@link #onSuccess(Call)}.
 */
public abstract class StateChangedHandler implements StateChangedListener {
  private Context mContext;
  
  /**
   * Default constructor with a context. 
   * @param context The application context.
   */
  public StateChangedHandler(Context context) {
    mContext = context;
  }
  
  /**
   * Retrieve the application context.
   * @return The application context.
   */
  final public Context getContext() {
    return mContext;
  }
  
  /**
   * Callback invoked when the call is being executed.
   * @param call The asynchronous call.
   * @param data The progression data.
   */
  public void onExecuting(Call<?> call, StateChangedListener.ProgressData data) { }

  /**
   * Callback invoked when the call is executed successfully.
   * @param call The asynchronous call.
   * @throws Throwable
   */
  public void onSuccess(Call<?> call) throws Throwable { }
  
  /**
   * Callback when the call cannot be executed or encounters a failure.
   * Typically the call state is FAILED.  The caller must invoke {@link Call#get()}
   * within a <code>try-catch</code> block to determine the cause.
   * @param call The asynchronous call.
   * @param cause The cause of the failure.
   */
  public void onError(Call<?> call, Throwable cause) { }
}
