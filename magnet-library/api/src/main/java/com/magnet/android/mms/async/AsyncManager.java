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
import android.content.Intent;

import com.magnet.android.mms.async.AsyncService.Envelope;
import com.magnet.android.mms.async.AsyncService.Payload;

import java.lang.reflect.Type;

/**
 * @hide
 * A facade to the AsyncService.  It is mainly used by Controllers and 
 * CallManager.  This class can be merged into CallManager.
 */
public class AsyncManager {
  private static AsyncManager sInstance;
  
  private Context mContext;
  private AsyncService mSvc;
  
  public static AsyncManager getInstance(Context context) {
    if (sInstance == null) {
      synchronized(AsyncManager.class) {
        if (sInstance == null) {
          sInstance = new AsyncManager(context);
        }
      }
    }
    return sInstance;
  }
  
  public static void stopInstance(Context context) {
    synchronized(AsyncManager.class) {
      if (sInstance != null) {
        sInstance.mSvc.onDestroy();
        sInstance = null;
      }
    }
  }
  
  private AsyncManager(Context context) {
    mContext = context.getApplicationContext();
    mSvc = AsyncService.getInstance(mContext);
  }
  
  /**
   * Invoke an asynchronous call.  If the envelope is for REST GET,
   * the path must contain the query string and it must be URL encoded.  The
   * file used in the <code>payload</code> will be automatically deleted.
   * @param envelope The envelope to address the invocation.
   * @param path The path of the controller method.
   * @param contentType Content type of payload (e.g. "application/json")
   * @param payload A marshalled data for POST, or null for GET.
   * @param options Async options or null.
   * @param resultClz
   * @param returnComponentTypes
   * @return
   */  
  public <T> Call<T> invoke(Envelope envelope, String path, String contentType,
            Payload payload, AsyncCallOptions options, Class<T> resultClz,
            Type[] returnComponentTypes) {
    return mSvc.invoke(envelope, path, contentType, payload, options, 
                        resultClz, returnComponentTypes);
  }

  /**
   * Cancel a call if possible.
   * @param callId
   * @param mayInterruptIfRunning
   * @return
   */
  public boolean cancel(String callId, boolean mayInterruptIfRunning) {
    return mSvc.cancel(callId, mayInterruptIfRunning);
  }
  
  /**
   * Resend a failed or timed out call.
   * @param call
   * @return true for success, false for failure.
   */
  public boolean resend(CallImpl<?> call) {
    return mSvc.resend(call);
  }
  
  /**
   * Dispose the call after it is completed, cancelled, timed out, or failed.
   * @param callId
   * @return
   */
  public boolean dispose(String callId) {
    return mSvc.dispose(callId);
  }

  void checkAndHandleNoPendingRqts() {
    mSvc.checkAndHandleNoPendingRqts();
  }
}
