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

import java.io.Serializable;


/**
 * Listener for the change of state of a controller call.
 */
public interface StateChangedListener {
  
  /**
   * This class contains the I/O statistics when the Controller call is being
   * executed.
   */
  public static class ProgressData implements Serializable {
    private static final long serialVersionUID = 4710502395375620625L;
    /**
     * True for received data; false  for sent data.
     */
    public boolean isReceived;
    /**
     * Accumulative number of bytes received or sent.
     */
    public int byteCount;
    /**
     * Total number of bytes to be received or sent.  -1 means unknown.
     */
    public int totalCount;
    
    /**
     * The String representation of I/O data in progress.
     */
    @Override
    public String toString() {
      return "[dir="+(isReceived?"IN":"OUT")+", stat="+byteCount+"/"+totalCount+"]";
    }
  }

  /**
   * Specify the callback to be invoked when the call is executed.  This
   * callback will be invoked when I/O happens.
   * @param call The call object.
   * @param data An updated I/O statistics.
   */
  public void onExecuting(Call<?> call, ProgressData data);

  /**
   * Specify the callback to be invoked when the call is executed successfully.  
   * The caller must call
   * {@link Call#get()} or {@link Call#get(long, java.util.concurrent.TimeUnit)}
   * to retrieve the result. There is no need to handle any exceptions.
   * @param call The callback to be invoked when the call is executed successfully
   * @throws Throwable if an unexpected exception occurs.
   */
  public void onSuccess(Call<?> call) throws Throwable;
  
  /**
   * Specify the callback to be invoked when the call cannot be executed or encounters a failure.
   * Typically the call state is <code>FAILED</code>.
   * @param call The callback to be invoked when the call cannot be executed or encounters a failure.
   * @param cause The cause of the failure.
   * @see com.magnet.android.mms.exception.HttpCallException
   * @see com.magnet.android.mms.exception.MarshallingException
   */
  public void onError(Call<?> call, Throwable cause);
}
