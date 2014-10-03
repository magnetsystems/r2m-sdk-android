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

import android.os.Handler;
import android.os.Looper;

/**
 * @hide
 * The options for an asynchronous (non-guaranteed delivery) call.
 * The default settings are as follows:
 * <ul>
 *    <li>One thread per call.</li>
 *    <li>The cache age is 0 (discard cached result).</li>
 *    <li>A cache over constraint policy is used (check the cache before evaluating the constraint).</li>
 *    <li>There is no implicit constraint used.</li>
 *    <li>There is no token (duplicated requests are not checked).</li>
 *    <li>No custom contextual tags are included, except for system level contextual tags.</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class AsyncCallOptions extends Options {
  String mQueueName;
  StateChangedListener mStateChangedListener;
  
  StateListener mStateListener = new StateListener() {
    @Override
    public void onStateChanged(final Call<?> call) {
      if (mStateChangedListener == null)
        return;
      // Make sure the state unchanged until the actual callback.
      final Call.State state = call.getState();
      Handler handler = new Handler(Looper.getMainLooper());
      handler.post(new Runnable() {
        public void run() {
          switch (state) {
          case EXECUTING:
            // The progress data may have been updated by the async thread, but
            // it should not matter much (I hope.)
            mStateChangedListener.onExecuting(call, ((CallImpl<?>) call).mProgress);
            break;
          case SUCCESS:
            try {
              mStateChangedListener.onSuccess(call);
            } catch (Throwable cause) {
              mStateChangedListener.onError(call, cause);
            }
            break;
          case FAILED:
            mStateChangedListener.onError(call, call.getCause());
            break;
          }
        }
      });
    }
  };

  /**
   * Default constructor.
   */
  public AsyncCallOptions() {
    super();
  }

  /**
   * Sets the name of the queue. Each queue represents one worker thread to execute the asynchronous call.  If
   * the name is not set, a default queue will be used.
   * @param queueName The name of the queue.
   * @return The asynchronous call options.
   */
  AsyncCallOptions setQueueName(String queueName) {
    mQueueName = queueName;
    return this;
  }
  
  /**
   * Set the listener for the State Changed event.
   * @param listener The listener for the State Changed event.
   * @return The asynchronous call options.
   * @see StateChangedHandler
   */
  public AsyncCallOptions setStateChangedListener(StateChangedListener listener) {
    mStateChangedListener = listener;
    return this;
  }

  /**
   * Override the default state listener, which dispatches the new state
   * to the handler.
   * @param listener The state listener
   * @return The asynchronous call options.
   */
  AsyncCallOptions setStateListener(StateListener listener) {
    mStateListener = listener;
    return this;
  }
  
  // Async call usually fails fast unless a queue is specified.
  boolean isFailFast() {
    return true;
  }
  
  public String toString() {
    return super.toString()+", qName="+mQueueName+", listener="+
            mStateChangedListener+"]";
  }
}
