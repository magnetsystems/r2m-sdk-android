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

import com.magnet.android.mms.utils.logger.Log;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @hide
 */
@SuppressWarnings("serial")
public class AsyncQueue extends LinkedBlockingQueue<Runnable> implements Runnable {
  private final static String TAG = "AsyncQueue";
  private String mQueueName;
  private AsyncQueueManager mQueueMgr;
  
  public AsyncQueue(String queueName, AsyncQueueManager queueMgr) {
    super(5);
    mQueueName = queueName;
    mQueueMgr = queueMgr;
  }
  
  public String getQueueName() {
    return mQueueName;
  }
  
  /**
   * Process all requests in this queue in a worker thread.  At the end, if
   * the queue becomes empty, remove the queue.  But during the run, a CallImpl
   * may sleep and resend itself.
   */
  public void run() {
    CallImpl<?> call;
    while ((call = (CallImpl<?>) this.poll()) != null) {
      Log.d(TAG, "Process an async call on queue="+mQueueName);
      call.run();
    }
    // TODO: need to be synchronized when queuing is supported.
    if (this.isEmpty()) {
      mQueueMgr.removeQueue(mQueueName);
    }
    AsyncManager.getInstance(mQueueMgr.getContext()).checkAndHandleNoPendingRqts();
    Log.d(TAG, "AsyncQueue.run() returned");
  }
  
  // Insert a call back to the head of the queue.
  public void insert(CallImpl<?> call) {
    ArrayList<Runnable> calls = new ArrayList<Runnable>();
    calls.add(call);
    this.drainTo(calls);
    this.addAll(calls);
  }
  
  public CallImpl<?> getCallByToken(String token) {
    if (token != null) {
      for (Runnable runnable : this) {
        CallImpl<?> call = (CallImpl<?>) runnable;
        if (call.mRequest == null) {
          continue;
        }
        AsyncCallOptions options = (AsyncCallOptions) call.mRequest.options;
        if (options != null && token.equals(options.mToken)) {
          return call;
        }
      }
    }
    return null;
  }
  
}