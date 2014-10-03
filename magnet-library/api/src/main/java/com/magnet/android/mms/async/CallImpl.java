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

import com.magnet.android.mms.MagnetMobileClient;
import com.magnet.android.mms.async.AsyncService.CallRequest;
import com.magnet.android.mms.async.AsyncService.CallResult;
import com.magnet.android.mms.async.StateChangedListener.ProgressData;
import com.magnet.android.mms.connection.ConnectionConfigManager;
import com.magnet.android.mms.connection.ConnectionConfigManager.ConnectionConfig;
import com.magnet.android.mms.connection.ConnectionService;
import com.magnet.android.mms.connection.ConnectionService.Request;
import com.magnet.android.mms.connection.ConnectionService.Response;
import com.magnet.android.mms.connection.ConnectionService.Response.Status;
import com.magnet.android.mms.exception.HttpCallException;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.request.ParserFactory;
import com.magnet.android.mms.request.ResponseParser;
import com.magnet.android.mms.exception.MarshallingException;
import com.magnet.android.mms.utils.FileUtil.InProgressFileOp.ProgressListener;
import com.magnet.android.mms.utils.Util;
import com.magnet.android.mms.utils.logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This object represents an asynchronous invocation to a controller.  An
 * instance of the CallImpl is typically returned by a method call from any
 * generated controller implementation.
 * <p/>
 * Calling {@link #dispose()} is optional upon completion. Once {@link #get()} is invoked, the call
 * result is automatically disposed. However, without {@link #get()} invocation or if exception occured
 * from {@link #get()}, the caller should invoke {@link #dispose(boolean)} to free the call result
 * explicitly.
 *
 * @param <T> The result type.
 */
public class CallImpl<T> implements Call<T>, Runnable, ProgressListener {
  private final static String TAG = "CallImpl";
  private final static String DETAIL_MSG = " failed; detail is in the cause.";

  private final static String ID_PREFIX_DEFAULT = "U";

  Context mContext;     // application context
  boolean mCancelled;
  CallRequest mRequest; // null if the CallImpl is done
  CallResult mResult;   // null if QUEUED, FAILED, CANCELLED, TIMEDOUT
  Throwable mStack;
  ProgressData mProgress; // only valid when EXECUTING
  int mLastMarker;        // only use when EXECUTING; last marker of [0..10]

  CallImpl(Context context, CallRequest request) {
    mStack = new Throwable();
    mStack.fillInStackTrace();
    mProgress = new ProgressData();
    mContext = context.getApplicationContext();
    if (((mRequest = request) != null) && (mRequest.correlationId == null)) {
      // Generate a correlation ID (aka call ID or request ID.)
      mRequest.correlationId = genId(ID_PREFIX_DEFAULT);
    }
  }

  /**
   * @hide
   * Get the unique ID for this invocation.
   *
   * @return
   */
  public String getId() {
    return (mRequest == null) ? null : mRequest.correlationId;
  }

  /**
   * @hide
   * Get the unique token being specified in this invocation.  The token is
   * used to avoid duplicated invocations.
   *
   * @return
   */
  public String getToken() {
    return (mRequest == null || mRequest.options == null) ?
        null : mRequest.options.mToken;
  }

  /**
   * Get the state of this invocation.  Reliable call states are
   * {@link State#QUEUED}, {@link State#EXECUTING}, {@link State#SUCCESS},
   * {@link State#FAILED}, {@link State#TIMEDOUT} or {@link State#CANCELLED}.
   * Unreliable call states are {@link State#EXECUTING}, {@link State#SUCCESS},
   * or {@link State#FAILED}.  The {@link State#INIT} is the initial transient
   * state.
   *
   * @return One of the State values, or null if the call has been disposed.
   */
  public State getState() {
    return (mRequest == null) ? null : mRequest.state;
  }

  /**
   * Resend the request if it has not been disposed and it was failed or timed
   * out.
   *
   * @return true if it has been resubmitted.
   */
  public boolean resend() {
    if (mRequest == null || mRequest.correlationId == null) {
      return false;
    }
    return AsyncManager.getInstance(mContext).resend(this);
  }

  /**
   * Dispose this CallImpl without clearing its result from cache.  The call must
   * be in SUCCESS, FAILED or TIMEDOUT state.  All resources used by this
   * CallImpl will be released.  To dispose a queued or executing call, it must
   * be cancelled first.  This method is same as dispose(false).
   *
   * @return true for success
   */
  public boolean dispose() {
    return dispose(false);
  }

  /**
   * Dispose the Call and optionally clearing its result from cache.  The call
   * must be in SUCCESS, FAILED or TIMEDOUT state.  All resources used by this
   * CallImpl will be released.  The clearResult allows the caller to remove its
   * result from the cache immediately; it is useful to any infrequent calls
   * with large result data. But caution should be taken since it may delete
   * the cached result for other concurrent similar calls.
   *
   * @param clearResult true to remove its result if exists.
   * @return true
   */
  public boolean dispose(boolean clearResult) {
    if (mRequest == null || mRequest.correlationId == null) {
      Log.e(TAG, "dispose(" + clearResult + ") failed: already disposed.");
      return false;
    }
    // GC and finalize are not reliable enough to remove tmp file in payload.
    if (mRequest.payload != null && mRequest.payload.isDeleteOnSent()) {
      mRequest.payload.deleteFile();
    }
    // Remove the cached result if exists.
    if (clearResult && mResult != null && mResult.requestHash != null) {
      AsyncPersister.getInstance(mContext).removeCacheByRequestHash(
          mResult.requestHash);
    }

    mRequest = null;
    mResult = null;
    return true;
  }

  /**
   * Cancel a queued or executing call.  If the call has been disposed,
   * completed, cancelled, or unable to cancel, it will return false.  Upon
   * successful completion, this call object will be disposed too.
   *
   * @param mayInterruptIfRunning
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (mRequest == null || mRequest.correlationId == null) {
      return false;
    }
    boolean cancelled = AsyncManager.getInstance(mContext).cancel(
        mRequest.correlationId, mayInterruptIfRunning);
    if (cancelled) {
      mCancelled = true;
      mRequest = null;
      mResult = null;
    }
    return cancelled;
  }

  /**
   * Returns true if this task was cancelled before it completed normally.  If
   * the call has been disposed, it will return true too.
   *
   * @return true if this task was cancelled before it completed
   */
  @Override
  public boolean isCancelled() {
    return mCancelled || mRequest == null || mRequest.state == State.CANCELLED;
  }

  /**
   * Returns true if this task completed. Completion may be due to normal
   * termination, an exception, or cancellation -- in all of these cases, this
   * method will return true.  If the call has been disposed, it will return
   * true too.
   *
   * @return
   */
  @Override
  public boolean isDone() {
    return mCancelled || (mRequest == null) ||
        (mRequest.state == State.FAILED) || (mRequest.state == State.SUCCESS) ||
        (mRequest.state == State.CANCELLED);
  }

  /**
   * Get the cause of the failure without using {@link #get()} or
   * {@link #get(long, TimeUnit)}.  If the CallImpl has been disposed, it will
   * return null.
   *
   * @return null if no error or the CallImpl has been disposed, or the cause.
   * @see com.magnet.android.mms.exception.HttpCallException
   * @see com.magnet.android.mms.exception.MarshallingException
   */
  public Throwable getCause() {
    return (mRequest == null) ? null : mRequest.cause;
  }

  /**
   * Waits if necessary for the request to complete, and then retrieves its
   * result.  Be careful when calling this method in the main thread; it will
   * block the main thread. Once the result is returned successfully, the result is automatically
   * disposed and subsequent call to this method will fail with MobileRuntimeException.
   *
   * @return The type-safe result.
   * @throws HttpCallException       If error code received for HTTP request and response.
   * @throws com.magnet.android.mms.exception.MarshallingException If the result failed to convert to the type-safe result.
   * @throws CancellationException   if the computation was cancelled
   * @throws ExecutionException      if the computation threw an exception
   * @throws InterruptedException    if the current thread was interrupted while waiting
   */
  @Override
  public T get() throws InterruptedException, ExecutionException {
    try {
      return get(0L, TimeUnit.DAYS);
    } catch (TimeoutException e) {
      // Never happen here.
      return null;
    }
  }

  /**
   * @hide
   * Waits if necessary for at most the given time for the request to
   * complete, and then retrieves its result, if available.  Be careful when
   * calling this method in the main thread; it will block the main thread.
   *
   * @param timeout The maximum time to wait, or 0 for no time out.
   * @param unit    The time unit of the timeout argument
   * @return The type-safe result.
   * @throws HttpCallException       If error code received for HTTP request
   * @throws com.magnet.android.mms.exception.MarshallingException If the result failed to convert to the type-safe result.
   * @throws CancellationException   if the request was cancelled
   * @throws ExecutionException      if the request execution threw an exception
   * @throws InterruptedException    if the current thread was interrupted while waiting
   * @throws TimeoutException        if the wait timed out
   */
  @Override
  public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    synchronized (this) {
      if (isPending()) {
        Log.d(TAG, "get(timeout=" + timeout + "); wait()...");
        long tod = System.currentTimeMillis();
        timeout = unit.toMillis(timeout);
        do {
          this.wait(timeout);
          if (timeout > 0) {
            timeout -= System.currentTimeMillis() - tod;
            if (timeout <= 0) {
              throw new TimeoutException();
            }
          }
          // If it is queued, continue to wait.
        } while (mRequest != null && mRequest.state == State.QUEUED);
      }
      if (mRequest == null) {  // already disposed
          throw new MobileRuntimeException("result already gotten and no longer available");
      }
      if (mRequest != null && mRequest.state == State.SUCCESS) {
        if (mResult == null) {
          mResult = AsyncPersister.getInstance(mContext).getCacheByRequestHash(
              mRequest.computeHash());
        }
      } else if (isCancelled()) {
        throw new CancellationException();
      } else if (mRequest.cause != null) {  // State.FAILED
        if (mRequest.cause instanceof ExecutionException) {
          throw (ExecutionException) mRequest.cause;
        } else if (mRequest.cause instanceof HttpCallException) {
          throw (HttpCallException) mRequest.cause;
        } else if (mRequest.cause instanceof RuntimeException) {
          throw (RuntimeException) mRequest.cause;
        } else {
          throw new MobileRuntimeException((Exception) mRequest.cause);
        }
      }
    }

    // Check the result is available and unmarshall the result.
    if (mResult == null || mResult.resultTime == null) {
      throw new ExecutionException("Cannot parse a null response", null);
    }
    InputStream ins = null;
    boolean parseError = false;
    try {
      ins = mResult.getResultInputStream();
      ParserFactory pf = new ParserFactory(mResult.resultClz, mResult.rtnCmpTypes);
      ResponseParser parser = pf.createInstance(mResult.contentType, mResult.encodingType);
      T result = null;
      try {
        result = (T) parser.parseResponse(ins);
        return result;
      } catch (MarshallingException e) {
        ins.close();
        e.setErrorContent(Util.inputStreamToString(mResult.getResultInputStream()));
        ins = null;
      }
    } catch (IOException ie) {
        throw new ExecutionException("Unexpected IO exception while processing result", ie);
    } finally {
      if (ins != null) {
        try {
          ins.close();
        } catch (IOException e) {
          Log.w(TAG, "ignoring unexpected IO exception closing result input", e);
        }
        dispose(true);
      }
    }
    throw new MobileRuntimeException("Unexpected exception while processing result");
  }

  @Override
  public String toString() {
    return "[" + super.toString() + ", ctx=" + mContext + ", rqt=" + mRequest + ", res=" + mResult + "]";
  }

  private static String genId(String prefix) {
    return UUID.randomUUID().toString() + (prefix);
  }

  // check if a call is in one of the pending states.
  boolean isPending() {
    return mRequest != null && (mRequest.state == State.INIT ||
        mRequest.state == State.QUEUED || mRequest.state == State.EXECUTING);
  }

  Context getContext() {
    return mContext;
  }

  // Update the call state and the cause only if the old state is different from
  // the new state. Return the old state.
  State setState(State newState, Throwable cause) {
    Log.d(TAG, "setState() state=" + newState + ", cause=" + cause);
    State oldState = mRequest.state;

    mRequest.cause = cause;
    mRequest.state = newState;
    return oldState;
  }

  boolean notifyStateChanged(Options options) {
    if (options == null)
      return false;

    if (options instanceof AsyncCallOptions) {
      return notifyStateChanged((AsyncCallOptions) options);
    }
    return false;
  }

  private boolean notifyStateChanged(AsyncCallOptions options) {
    if (options == null || options.mStateListener == null) {
      return false;
    }
    options.mStateListener.onStateChanged(CallImpl.this);
    return true;
  }


  boolean setStateAndNotify(State state, Throwable cause, Options options) {
    boolean ok = false;
    synchronized (this) {
      if (this.setState(state, cause) != state) {
        this.notifyStateChanged(options);
        ok = true;
      }
      this.notify();
    }
    return ok;
  }

  // Remove the file used in the payload when the call is success.  If the call
  // failed, we have to keep the file until the Call is GC'ed in case the caller
  // wants to resend.
  private void cleanupFileInPayload() {
    if (mRequest.payload != null && mRequest.payload.isDeleteOnSent()) {
      mRequest.payload.deleteFile();
    }
  }

  /**
   * @hide
   * The main execution of the call.
   */
  @Override
  public void run() {
    Log.d(TAG, "@@@ run() call=" + super.toString());

    Context context = this.getContext();
    Options options = this.mRequest.options;  // never null

    // This synchronized block prevents concurrent requests with same result
    // hash overwriting the result from each other. WON-9066
    String resultHash = mRequest.computeHash().intern();
    synchronized (resultHash) {

      InputStream payloadInput = null;
      Response response = null;
      Throwable cause;
      try {
        ConnectionConfigManager ccMgr = MagnetMobileClient.getInstance(
            mContext).getManager(ConnectionConfigManager.class, mContext);
        ConnectionConfig cc = ccMgr.getConnectionConfig(mRequest.envelope.getEndPoint());
        if (cc == null) {
          throw new IOException("No such endpoint in connection_configs.xml: " +
              mRequest.envelope.getEndPoint());
        }
        ConnectionService connectSvc = cc.getConnectionService();

        // Add the headers populated when the call was invoked.
        Request request = connectSvc.createRequest();
        request.setPath(mRequest.path);
        mRequest.envelope.populateRequest(request);
        request.addHeaders(mRequest.headers);

        request.setContentType(mRequest.contentType);
        int payloadSize = 0;
        if (mRequest.payload != null) {
          payloadSize = mRequest.payload.getSize();
          payloadInput = mRequest.payload.getAsRawInputStream();

          if (payloadInput != null) {
            request.setPayload(payloadInput);
          }
        }

        AsyncPersister persister = AsyncPersister.getInstance(mContext);

        synchronized (this) {
          // Don't use setStateAndNotify() here; it will cause CallImpl.get() return
          // prematurely when CallImpl.wait().
          setProgress(false, payloadSize);
          report(0);
        }
        response = request.execute();
        Status status = response.getStatus();

        Log.d(TAG, "Response received: ctype=" + response.getContentType() +
            ", status=" + status);

        switch (status) {
          case SUCCESS:
            setProgress(true, getIntHeader(response, "Content-Length", -1));
            mResult = persister.addCache(mRequest, response.getContentType(),
                response.getContentTransferEncoding(), response.getPayload(), this);
            if (mResult != null) {
              cleanupFileInPayload();
              setStateAndNotify(State.SUCCESS, null, options);
            } else {
              cause = new ExecutionException(stripQuery(mRequest.path) +
                  ": unable to cache the result; check logcat for details.", null);
              fillStack(cause);
              setStateAndNotify(State.FAILED, cause, options);
            }
            break;
          case ERROR:
            mResult = null;
            cause = new HttpCallException(Util.inputStreamToString(
                response.getPayload()), null, response.getResponseCode());
            fillStack(cause);
            setStateAndNotify(State.FAILED, cause, options);
            break;
        }
      } catch (Throwable e) {
        Log.e(TAG, "run() failed", e);
        mResult = null;
        cause = new ExecutionException(stripQuery(mRequest.path) + DETAIL_MSG, e);
        setStateAndNotify(State.FAILED, cause, options);
      } finally {
        if (payloadInput != null) {
          try {
            payloadInput.close();
          } catch (IOException e) {
            // Ignored.
          }
        }
        if (response != null) {
          response.release();
        }
      }
    }
  }

  /**
   * @hide
   * Update the EXECUTING state and do a callback with I/O statistics.  If the
   * total length is known, the callback will happen every 10% completion or 8KB
   * whichever is higher.  Otherwise, the callback will happen at every I/O.
   */
  @Override
  public void report(int count) {
    mProgress.byteCount = count;
    int tenthMarker = (mProgress.totalCount <= 0) ?
        0 : (mProgress.byteCount * 10 / mProgress.totalCount);
    if ((mProgress.totalCount < 0) || (tenthMarker > mLastMarker)) {
      Log.d(TAG, "@@@ EXECUTING report=" + mProgress + ", marker=" + tenthMarker);
      setState(State.EXECUTING, null);
      notifyStateChanged(mRequest.options);
      mLastMarker = tenthMarker;
    }
  }

  private void setProgress(boolean rcv, int total) {
    mProgress.isReceived = rcv;
    mProgress.totalCount = total;
    mProgress.byteCount = 0;
    mLastMarker = -1;
  }

  private int getIntHeader(Response response, String name, int defValue) {
    List<String> list = response.getHeaders().get(name);
    if (list == null || list.size() == 0) {
      return defValue;
    }
    return Integer.parseInt(list.get(0));
  }

  private void fillStack(Throwable cause) {
    if (mStack != null) {
      cause.setStackTrace(mStack.getStackTrace());
    } else {
      cause.fillInStackTrace();
    }
  }

  // Strip the query string from the URL path.
  private static String stripQuery(String path) {
    int index = path.indexOf('?');
    if (index < 0)
      return path;
    return path.substring(0, index);
  }

}