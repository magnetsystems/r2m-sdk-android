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

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This interface represents an asynchronous invocation to a controller.  An
 * instance of the Call is typically returned by a Controller method call. Calling {@link #dispose()} or
 * {@link #dispose(boolean)} is optional, but these methods allow developers to
 * delete the cached response immediately.
 *
 * @param <T> The result type.
 */
public interface Call<T> extends Future<T> {  
  /**
   * @hide
   * Intent to be broadcast when there are no more pending calls.
   */
  public static final String ACTION_NO_ASYNC_PENDING_REQUESTS =
      "com.magnet.android.action.NO_ASYNC_PENDING_REQUESTS";
  /**
   * @hide
   * Extra for the call ID (String).
   */
  public static final String EXTRA_CALL_ID = "com.magnet.async.callId";
  /**
   * @hide
   * Extra for the call state (Serializable of Call.State).
   */
  public static final String EXTRA_CALL_STATE = "com.magnet.async.callState";
  /**
   * @hide
   * Extra for the additional data (Serializable object).
   */
  public static final String EXTRA_EXTRAS = "com.magnet.async.extras";
  /**
   * @hide
   * Extra for the progress data (Serializable of StateChangedListener.ProgressData).
   */
  public static final String EXTRA_PROGRESS_DATA = "com.magnet.async.progress";
  /**
   * Call state.
   */
  public enum State {
    /**
     * Initial state.
     */
    INIT,
    /**
     * Call is queued; constraint is not met.
     */
    QUEUED,
    /**
     * Call is being executed and is waiting for a response.
     */
    EXECUTING,
    /**
     * Call is timed out by remaining in the queue for too long.
     */
    TIMEDOUT,
    /**
     * Call is executed and has succeeded.
     */
    SUCCESS,
    /**
     * Call cannot be executed and has encountered a failure or server-side error.
     */
    FAILED,
    /**
     * Call has been cancelled (transitional state).
     */
    CANCELLED,
  }

    /**
     * @hide
     */
  public enum When {
   /**
     * The tag is added during the call send.
     */
    SEND,
    /**
     * The tag is added during the call submit.
     */
    SUBMIT,
  }

  /**
   * Retrieve the unique ID for this invocation.
   * @return The unique ID for this invocation.
   */
  public String getId();

  /**
   * @hide
   * Retrieve the token specified in this call.  The token is used to avoid 
   * duplicated invocations from the client.
   * @return The token specified in this call, or null.
   */
  public String getToken();

  /**
   * Retrieve the state of this invocation.  The {@link State#INIT} is the initial transient
   * state.
   * @return One of the State values, or null if the call has been disposed.
   */
  public State getState();


  /**
   * Resend this request if its state is FAILED or TIMEDOUT.  This operation is
   * invalid if this Call instance has been disposed.
   * @return <code>true</code> if it is resubmitted; <code>false</code> if this call has an invalid state.
   */
  public boolean resend();

  /**
   * Dispose this Call without clearing its result from cache.  The call must
   * be in a SUCCESS, FAILED or TIMEDOUT state.  All resources used by this 
   * Call will be released.  To dispose a queued or executing call, it must
   * be cancelled first.  This method is equivalent to {@link #dispose(boolean)}
   * with false.
   * @return <code>true</code> for success; <code>false</code> if in an illegal state.
   */
  public boolean dispose();
  
  /**
   * Dispose this Call and optionally clearing its result from cache.  The call
   * must be in SUCCESS, FAILED or TIMEDOUT state.  All resources used by this 
   * Call will be released.  The clearResult allows the caller to remove its 
   * result from the cache immediately; it is useful to any infrequent calls
   * with large result data. But caution should be taken since it may delete
   * the cached result for other concurrent similar calls.
   * @param clearResult true to remove its result if exists.
   * @return <code>true</code> for success; <code>false</code> if in an illegal state.
   */
  public boolean dispose(boolean clearResult);
  
  /**
   * Cancel a queued or executing call.  If the call has been disposed,
   * completed, cancelled, or is unable to cancel, it will return <code>false</code>.  Upon 
   * successful completion, this call object will be disposed.  There is no
   * guarantee that the call can actually be cancelled; the system makes 
   * its best effort.
   * @param mayInterruptIfRunning Set to <code>true</code> to permit interruption if the call is running; set to <code>false</code> otherwise.
   * @return <code>true</code> if cancellation is submitted; <code>false</code> if unable to cancel.
   */
  public boolean cancel(boolean mayInterruptIfRunning);

  /**
   * Indicates whether this task was cancelled before it completed normally.  If
   * the call has been disposed, it will return <code>true</code>.
   * @return <code>true</code> if this task was cancelled before it completed; <code>false</code> otherwise.
   */
  public boolean isCancelled();

  /**
   * Indicates whether this task completed. Completion may be due to normal
   * termination, an exception, or cancellation; in all of these cases, this
   * method will return <code>true</code>.  If the call has been disposed, it will return
   * <code>true</code>.
   * @return <code>true</code> if this task completed or the call has been disposed; <code>false</code> otherwise.
   */
  public boolean isDone();

  /**
   * Retrieve the cause of the failure without using {@link #get()} or 
   * {@link #get(long, TimeUnit)}.  If the Call has been disposed, it will
   * return null.
   * @return The cause of the failure, or null if there was no error or the Call has been disposed.
   */
//    * @see com.magnet.android.mms.exception.CloudRuntimeException // for 1k90d
  public Throwable getCause();

  /**
   * Waits, if necessary, for the computation to complete, and retrieves the
   * result.
   * @return The computed result.
   * @throws ExecutionException  if the computation threw an exception.
   * @throws InterruptedException  if the current thread was interrupted while waiting.
   */
//    * @exception CloudRuntimeException Server threw an exception. // for 1k90d
  public T get() throws InterruptedException, ExecutionException;

  /**
   * Waits, if necessary, for at most the specified timeout period for the computation to
   * complete, and then retrieves its result, if available.
   * @param timeout The maximum time to wait.
   * @param unit The time unit of the timeout argument.
   * @return The computed result.
   * @throws ExecutionException  if the computation threw an exception.
   * @throws InterruptedException  if the current thread was interrupted while waiting.
   * @throws TimeoutException  if the wait timed out.
   */
//    * @exception CloudRuntimeException Server threw an exception. // for 1k90d
  public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException;
}