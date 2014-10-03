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
package com.magnet.android.mms.utils;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * A helper class to register the Handler callback in a thread-safe manner.
 * Make sure to call {@link #start()} to start the thread.
 */
public class MobileHandlerThread extends HandlerThread {
//  private final static String TAG = "MobileHandlerThread";
  private Handler mHandler;
  private Handler.Callback mCallback;
  
  /**
   * Constructor with a thread name and callback for Handler.
   * @param name A thread name.
   * @param callback Implementation of {@link Handler#handleMessage(android.os.Message)}
   */
  public MobileHandlerThread( String name, Handler.Callback callback ) {
    super(name);
    mCallback = callback;
  }
  
  /**
   * Constructor with a thread name, priority and callback for Handler.
   * @param name A thread name
   * @param priority A thread priority
   * @param callback Implementation of {@link Handler#handleMessage(android.os.Message)}
   */
  public MobileHandlerThread( String name, int priority, 
                            Handler.Callback callback ) {
    super(name, priority);
    mCallback = callback;
  }
  
  /**
   * Get the Handler.  The Handler is only available after {@link #onLooperPrepared()}
   * is invoked within {@link #run()}.
   * @return
   */
  public Handler getHandler() {
    if (mHandler == null) {
      synchronized(mCallback) {
        if (mHandler == null) {
          try {
            mCallback.wait();
          } catch (InterruptedException e) {
            // No-op
          }
        }
      }
    }
    return mHandler;
  }
  
  /*
   * Create a Handler after the looper is preapared.
   */
  protected void onLooperPrepared() {
    mHandler = new Handler(mCallback);
    synchronized(mCallback) {
      // Notify anyone waiting for the availability of the Handler.
      mCallback.notifyAll();
    }
  }
}
