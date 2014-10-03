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

import com.magnet.android.mms.async.AsyncService.CallRequest;
import com.magnet.android.mms.utils.logger.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

/**
 * @hide
 * It is an internal class.
 */
public class AsyncQueueManager {
  private final static String TAG = "AsyncQueueManager";
  private static int sQueueId = 100;
  private Context mContext;
  private ExecutorService mExecutor;
  private HashMap<String, AsyncQueue> mQueues = new HashMap<String, AsyncQueue>();
  
  public AsyncQueueManager(Context context, ExecutorService executor) {
    mContext = context.getApplicationContext();
    mExecutor = executor;
  }
  
  public Collection<AsyncQueue> getQueues() {
    return mQueues.values();
  }
  
  public void clearAllQueues() {
    mQueues.clear();
  }
  
  private synchronized static String genAsynQueueName() {
    return "AsyncQueue-"+(++sQueueId);
  }
  
  public AsyncQueue getQueue(String name) {
    return mQueues.get(name);
  }
  
  /**
   * Enqueue a request.  If no name is specified, a new queue with fail-fast is created.
   * @param name A queue name.
   * @param request
   * @return
   */
  public CallImpl<?> enqueue(String name, CallRequest request) {
    if (name == null) {
      ((AsyncCallOptions) request.options).mQueueName = name = genAsynQueueName();
    }
    
    // Create a queue first if it does not exist.
    AsyncQueue queue = mQueues.get(name);
    if (queue == null) {
      queue = new AsyncQueue(name, this);
      synchronized(mQueues) {
        mQueues.put(name, queue);
      }
    }
    
    // Append the request to the queue and use a worker thread to process all
    // pending requests in this queue.
    CallImpl<?> call = new CallImpl(mContext, request);
    queue.add(call);
    mExecutor.execute(queue);

    return call;
  }
  
  public boolean requeue(CallImpl<?> call) {
    String name = ((AsyncCallOptions) call.mRequest.options).mQueueName;
    AsyncQueue queue = mQueues.get(name);
    if (queue == null) {
      Log.w(TAG, "recreate a queue '"+name+"' for requeue Async CallImpl");
      queue = new AsyncQueue(name, this);
      synchronized(mQueues) {
        mQueues.put(name, queue);
      }
    }
    
    // Append the request to the queue.
    try {
      call.mRequest.requestTime = System.currentTimeMillis();
      queue.add(call);
      mExecutor.execute(queue);
      return true;
    } catch (Throwable e) {
      Log.e(TAG, "Unable to requeue Async CallImpl", e);
      return false;
    }
  }
  
  /**
   * Remove the queue.  Only remove the queue if it is empty.
   * @param queueName
   * @return
   */
  public boolean removeQueue(String queueName) {
    synchronized(mQueues) {
      return (mQueues.remove(queueName) != null);
    }
  }
  
  /**
   * Remove a request based on a filter.  The removed object becomes obsolete.
   * If the queue becomes empty, it will be removed as well.  If the removed
   * object has a QUEUED state and has a constraint, the constraint will be
   * released.
   * @param callId
   * @return
   */
  public boolean removeRequest(String callId) {
    for (AsyncQueue queue : mQueues.values()) {
      Iterator<Runnable> iterator = queue.iterator();
      while (iterator.hasNext()) {
        CallImpl<?> call = (CallImpl<?>) iterator.next();
        if (callId.equals(call.mRequest.correlationId)) {
          boolean removed = queue.remove(call);
          call.mRequest = null;
          call.mRequest = null;
          // TODO: need to be synchronized when queuing is supported.
          // Remove the queue if it is empty.
          if (queue.isEmpty())
            removeQueue(queue.getQueueName());
          return removed;
        }
      }
    }
    return false;
  }
  
  public CallImpl<?> getRequestById(String callId) {
    for (AsyncQueue queue : mQueues.values()) {
      Iterator<Runnable> iterator = queue.iterator();
      while (iterator.hasNext()) {
        CallImpl<?> call = (CallImpl<?>) iterator.next();
        if (callId.equals(call.mRequest.correlationId)) {
          return call;
        }
      }
    }
    return null;
  }

  public boolean isEmpty() {
    for (AsyncQueue queue : mQueues.values()) {
      if (!queue.isEmpty()) {
        return false;
      }
    }
    return true;
  }
  
  Context getContext() {
    return mContext;
  }
}
