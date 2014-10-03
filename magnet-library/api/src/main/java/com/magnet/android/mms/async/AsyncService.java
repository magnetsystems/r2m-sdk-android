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
import android.os.Handler;
import android.os.Message;

import com.magnet.android.mms.async.Call.State;
import com.magnet.android.mms.connection.ConnectionService.Request;
import com.magnet.android.mms.connection.ConnectionService.Request.Method;
import com.magnet.android.mms.settings.MagnetDefaultSettings;
import com.magnet.android.mms.utils.FileUtil;
import com.magnet.android.mms.utils.FileUtil.Mode;
import com.magnet.android.mms.utils.MobileHandlerThread;
import com.magnet.android.mms.utils.Util;
import com.magnet.android.mms.utils.logger.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @hide
 * Use a thread pool to serve each call invocation.  Each invocation consists
 * of an envelope, controller method URL path, content type of the payload,
 * a payload stored in a file, and the result data type.
 * 
 * This class is implemented as a singleton instead of Android Service because
 * MagnetMobileClient set up is synchronous and it cannot bind to this service 
 * asynchronously.
 */
public class AsyncService implements Handler.Callback {
  final static String ASYNC_THREAD_NAME = "AsyncThread-";

  private final static String TAG = "AsyncService";
  private final static int MSG_NO_PENDING_REQUESTS = 1;
  
  private static AsyncService sInstance;
  private static AtomicBoolean sInited = new AtomicBoolean(false);
  private static boolean ENCRYPT_REQUEST;

  private Context mContext;
  private ExecutorService mExecutor;
  private AsyncQueueManager mAsyncQueueMgr;

  MobileHandlerThread getHandlerThread() {
    return mHandlerThread;
  }

  private MobileHandlerThread mHandlerThread;
  private ThreadFactory mThreadFactory = new ThreadFactory() {
    private int mId;
    public Thread newThread(Runnable runnable) {
      return new Thread(runnable, ASYNC_THREAD_NAME+(++mId));
    }
  };

  /**
   * @hide
   * This internal class is used by Controller to encapsulate the payload
   * stored in an external file or a byte array.
   */
  public static class Payload implements Serializable {
    private static final long serialVersionUID = 3015820179688035735L;
    private static final Payload EMPTY_PAYLOAD = new Payload((byte[]) null);
    private transient boolean mDeleteOnSent;
    private transient int mSize = -1;
    private String mPath;
    private byte[] mData;
    
    protected void finalize() throws Throwable {
      if (mDeleteOnSent && mPath != null) {
        (new File(mPath)).delete();
      }
    }
    
    /**
     * The payload content stored in a file path.
     * @param path null, or a readable file path.
     * @see #setDeleteOnSent(boolean)
     */
    public Payload(String path) {
      if ((mPath = path) == null) {
        mData = FileUtil.EMPTY_BYTES;
        mSize = 0;
      } else {
        mSize = (int) (new File(path)).length();
      }
    }
    
    /**
     * The payload content stored in a File.
     * @param file null, or a readable File.
     * @see #setDeleteOnSent(boolean)
     */
    public Payload(File file) {
      if (file == null) {
        mData = FileUtil.EMPTY_BYTES;
        mSize = 0;
      } else {
        mPath = file.getPath();
        mSize = (int) file.length();
      }
    }
    
    /**
     * The payload content stored in a byte array.
     * @param data null, or a byte array.
     */
    public Payload(byte[] data) {
      if ((mData = data) == null) {
        mData = FileUtil.EMPTY_BYTES;
        mSize = 0;
      } else {
        mSize = mData.length;
      }
    }
  
    /**
     * Check if payload is back by a file.
     * @return
     */
    public boolean isFile() {
      return mPath != null;
    }
    
    /**
     * Mark the file for deleteOnSent.  This flag has multiple implications: it
     * is for unreliable async call, the file is not encrypted, and the file
     * will be deleted when the call is success or it is GC'ed.
     * @param deleteOnSent
     */
    public void setDeleteOnSent(boolean deleteOnSent) {
      mDeleteOnSent = deleteOnSent;
    }
    
    /**
     * Check if the file is marked for deleteOnSent.  This flag has multiple 
     * implications: it is for unreliable async call only, the file is not 
     * encrypted, and the file will be deleted when the call is sent.
     * @return
     */
    public boolean isDeleteOnSent() {
      return mDeleteOnSent;
    }
    
    /**
     * Get the backing file for the payload.
     * @return A File or null if not back by a file.
     */
    public File getFile() {
      return (mPath == null) ? null : new File(mPath) ;
    }
    
    /**
     * Delete the backing file used by the payload.
     */
    public void deleteFile() {
      if (mPath != null) {
        (new File(mPath)).delete();
        mPath = null;
      }
    }
   
    /**
     * Get the payload as byte array.  If the payload may be back by a file if
     * its content length exceeds a threshold.
     * is no longer used.
     * @return
     */
    public byte[] getAsData() {
      if (mData != null) {
        return mData;
      } else if (mPath != null) {
        InputStream ins = null;
        try {
          ins = new FileInputStream(mPath);
          if (ENCRYPT_REQUEST) {
            ins = FileUtil.decrypt(ins);
          } 
          return Util.inputStreamToByteArray(ins);
        } catch (Throwable e) {
          Log.e(TAG, "getAsData() failed", e);
          return null;
        } finally {
          if (ins != null) {
            try {
              ins.close();
            } catch (IOException e) {
              // Ignored.
            }
          }
        }
      } else {
        return null;
      }
    }
    /**
     * Get the payload as byte array without any decryption. If the payload may be back by a file if
     * its content length exceeds a threshold.
     * is no longer used.
     * @return
     */
    public byte[] getAsRawData() {
      if (mData != null) {
        return mData;
      } else if (mPath != null) {
        InputStream ins = null;
        try {
          ins = new FileInputStream(mPath); 
          return Util.inputStreamToByteArray(ins);
        } catch (Throwable e) {
          Log.e(TAG, "getAsRawData() failed", e);
          return null;
        } finally {
          if (ins != null) {
            try {
              ins.close();
            } catch (IOException e) {
              // Ignored.
            }
          }
        }
      } else {
        return null;
      }
    }
    /**
     * Get the payload as input stream.  It is mainly used by HTTP request.
     * @return
     */
    public InputStream getAsInputStream() {
      if (mPath != null) {
        try {
          InputStream ins = new FileInputStream(mPath);
          if (ENCRYPT_REQUEST) {
            ins = FileUtil.decrypt(ins);
          }
          return ins;
        } catch (IOException e) {
          return null;
        }
      } else if (mData != null && mData.length > 0) {
        return new ByteArrayInputStream(mData);
      } else {
        return null;
      }
    }

    /**
     * Get the payload as input stream without decrypting.  It is mainly used for testing HTTP request.
     * @return
     */
    public InputStream getAsRawInputStream() {
      if (mPath != null) {
        try {
          InputStream ins = new FileInputStream(mPath);
          return ins;
        } catch (IOException e) {
          return null;
        }
      } else if (mData != null && mData.length > 0) {
        return new ByteArrayInputStream(mData);
      } else {
        return null;
      }
    }
    
    public int getSize() {
      if (mSize >= 0) {
        return mSize;
      }
      // The size is unknown after deserialization, so get it again.
      if (mPath != null) {
        return mSize = (int) (new File(mPath)).length();
      } else if (mData != null) {
        return mSize = mData.length;
      } else {
        return mSize = 0;
      }
    }
    
    @Override
    public String toString() {
      return "[mPath="+mPath+", mData="+mData+", mSize="+mSize+"]";
    }
  }
  
  /**
   * @hide
   * A REST envelope containing connection information and protocol specific 
   * information (e.g. REST methods, encoding type.)
   */
  public static class Envelope implements Serializable {
    private static final long serialVersionUID = -324835634720514013L;
    
    private String mEndPoint;
    private Method mRestMethod;
    private String mEncodingType;
    private LinkedHashMap<String, String> mHeaders;
    
    /**
     * Constructor for REST connection information.
     * @param endPoint The name of the connection configuration.
     * @param restMethod One of the REST methods for the controller invocation.
     * @param encodingType null, binary, base64
     */
    public Envelope(String endPoint, Method restMethod, String encodingType) {
      this.mEndPoint = endPoint;
      this.mRestMethod = restMethod;
      this.mEncodingType = encodingType;
    }
    
    /**
     * Constructor for REST connection information with JAX-RES headers.
     * @param endPoint The name of the connection configuration.
     * @param restMethod One of the REST methods for the controller invocation.
     * @param encodingType null, binary, base64
     * @param headers null or JAX-RES headers
     */
    public Envelope(String endPoint, Method restMethod, String encodingType, 
                     LinkedHashMap<String, String> headers) {
      this.mEndPoint = endPoint;
      this.mRestMethod = restMethod;
      this.mEncodingType = encodingType;
      this.mHeaders = headers;
    }
    
    public String getEndPoint() {
      return mEndPoint;
    }
    
    public LinkedHashMap<String, String> getHeaders() {
      return mHeaders;
    }
    
    public void populateRequest(Request request) {
      request.setMethod(mRestMethod);
      if (mEncodingType != null) {
        request.setContentTransferEncoding(mEncodingType);
      }
      if (mHeaders != null) {
        request.addHeaders(mHeaders);
      }
    }
    
    // Convert the envelope into an array with extra elements at the beginning.
    // The array is used to compute a hash code.
    public String[] toArray(int extra) {
      int i = extra;
      int hdrLen = (mHeaders == null) ? 0 : (mHeaders.size() * 2);
      String[] params = new String[2+extra+hdrLen];
      params[i++] = mEndPoint;
      params[i++] = mRestMethod.toString();
      if (mHeaders != null) {
        for (Map.Entry<String, String> entry : mHeaders.entrySet()) {
          params[i++] = entry.getKey();
          params[i++] = entry.getValue();
        }
      }
      return params;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this)
        return true;
      if (o == null)
        return false;
      try {
        Envelope env = (Envelope) o;
        return mEndPoint.equals(env.mEndPoint) && 
                mRestMethod.equals(env.mRestMethod) &&
                (mEncodingType != null && mEncodingType.equals(env.mEncodingType) &&
                (mHeaders != null && mHeaders.equals(env.mHeaders)));
      } catch (Throwable e) {
        return false;
      }
    }
    
    @Override
    public String toString() {
      return "[endpt="+mEndPoint+", method="+mRestMethod+", encode="+
              mEncodingType+", hdr="+mHeaders+"]";
    }
  }

  
  /**
   * @hide
   * An internal class for the request of invocation.
   */
  public static class CallRequest {
    long reqId;
    long requestTime;
    Envelope envelope;
    String path;                      // controller method path
    State state;
    String correlationId;
    HashMap<String, String> headers;  // contextual headers set during call
    String contentType;
    Payload payload;
    Options options;
    Class<?> resultClz;
    Type[] rtnCmpTypes;
    Throwable cause;
    String hash;
    boolean isEncrypted;
    boolean needAck;

    CallRequest() {
    }
    
    CallRequest(Envelope envelop, String path, HashMap<String, String> headers,
                String contentType, Payload payload, Options options, 
                Class<?> resultClz, Type[] returnComponentTypes,
                boolean needAck) {
      this.envelope = envelop;
      this.path = path;
      this.headers = headers;
      this.contentType = contentType;
      this.payload = payload;
      this.options = options;
      this.resultClz = resultClz;
      this.rtnCmpTypes = returnComponentTypes;
      this.requestTime = System.currentTimeMillis();
      this.needAck = needAck;
      this.isEncrypted = ENCRYPT_REQUEST;
      
      this.state = State.INIT;
    }
    
    // Privatize payload external file to an internal file.  The external file
    // will be immediately deleted.  The internal file will be encrypted, and it
    // will be deleted when a call is disposed.
    boolean privatizePayload(Context context) {        
      if (payload == null || payload.mPath == null) {
        return false;
      }
      AsyncPersister persister = AsyncPersister.getInstance(context);
      if (persister.isAsyncDataFile(payload.mPath)) {
        return false;
      }
      File inf = new File(payload.mPath);
      File outf = persister.createAsyncDataTempFile();
      hash = FileUtil.copy(inf, outf, new FileUtil.DigestFileParamsOp(
          envelope.mEndPoint, envelope.mRestMethod.toString(), path),
          isEncrypted ? Mode.ENCRYPT : Mode.NONE);
      if (hash == null) {
        return false;
      }
      File dataFile = persister.getAsyncDataFile(hash);
      if (outf.renameTo(dataFile)) {
        payload.mPath = dataFile.getAbsolutePath();
        inf.delete();
        return true;
      }
      return false;
    }
    
    // Convert payload external file into memory.  It is used by
    // async call.  Since there is no explicit dispose, we cannot delete the 
    // file, so we have to rely on the GC to free up the memory.
    boolean convertPayload() {
      if (payload == null || payload.mPath == null) {
        return false;
      }
      File file = new File(payload.mPath);
      payload.mData = FileUtil.fileToByteArray(file);
      payload.mPath = null;
      file.delete();
      return true;
    }
    
    // Compute a unique hash based on the payload, the envelope and the path of
    // the controller method.
    String computeHash() {
      if (hash == null) {
        String[] envParams = envelope.toArray(1);
        envParams[0] = path;
        if (payload.isFile())
          hash = FileUtil.digest(payload.getFile(), envParams);
        else
          hash = FileUtil.digest(payload.getAsData(), envParams);
      }
      return hash;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == this)
        return true;
      if (o == null)
        return false;
      try {
        CallRequest r = (CallRequest) o;
        if (!envelope.equals(r.envelope))
          return false;
        if (!path.equals(r.path))
          return false;
        // TODO: do we need to compare the headers and tags?
        if (!computeHash().equals(r.computeHash()))
          return false;
        return true;
      } catch (Throwable e) {
        return false;
      }
    }
    
    @Override
    public String toString() {
      return "[reqId="+reqId+", rqtTime="+requestTime+", env="+envelope+
          ", path="+path+", state="+state+", cid="+correlationId+
          ", hdrs="+headers+", ctype="+contentType+", payload="+payload+
          ", clz="+resultClz.getName()+", rtnCmpTypes="+rtnCmpTypes+
          ", cause="+cause+", hash="+hash+", opts="+options+
          ", needAck="+needAck+", encrypted="+isEncrypted+"]";
    }
  }
  
  /**
   * @hide
   * An internal class for the result of an invocation.  This object may be
   * shared by multiple CallImpl objects.
   */
  public static class CallResult {
    long resId;
    boolean isEncrypted;
    String correlationId;
    String requestHash;
    Date resultTime;
    Class<?> resultClz;
    Type[] rtnCmpTypes;
    String contentType;
    String encodingType;
    
    private Context mContext;
    
    CallResult(Context context) {
      mContext = context;
    }
    
    /**
     * Get the raw result size.  If the raw result is encrypted, the size is the
     * cipher text size.
     * @return 0 if there is no result, > 0 for the raw result size.
     */
    public long getResultSize() {
      File inf = AsyncPersister.getInstance(mContext).getResultAsFile(requestHash);
      return inf.length();
    }
    
    /**
     * Get the result as InputStream.  Any encrypted result will be returned as
     * a decrypted InputStream.
     * @return
     * @throws IOException
     */
    public InputStream getResultInputStream() throws IOException {
      File inf = AsyncPersister.getInstance(mContext).getResultAsFile(requestHash);
      if (!isEncrypted) {
        return new FileInputStream(inf);
      } else {
        return FileUtil.decrypt(new FileInputStream(inf));
      }
    }
    
    /**
     * Get the result as byte array.  Any encrypted result will be decrypted
     * first.  Note, this method is not memory efficient.
     * @return A byte array.
     * @throws IOException
     */
    public byte[] getResult() throws IOException {
      int n;
      int offset = 0;
      int fileLen = (int) getResultSize();
      byte[] buffer = new byte[fileLen];
      InputStream ins = getResultInputStream();
      // The fileLen should be >= decrypted data length.  The offset will hold
      // the actual decrypted data length.
      while ((n = ins.read(buffer, offset, fileLen)) > 0) {
        offset += n;
        fileLen -= n;
      }
      Log.d(TAG, "getResult() file size="+buffer.length+", decrypt size="+offset);
      if (fileLen == 0) {
        return buffer;
      } else {
        // Transfer to a smaller (but exact) buffer.
        byte[] result = new byte[offset];
        System.arraycopy(buffer, 0, result, 0, offset);
        return result;
      }
    }
    
    @Override
    public String toString() {
      return "[id="+resId+", cid="+correlationId+
          ", encrypted="+isEncrypted+", hash="+requestHash+
          ", resultTime="+resultTime+", clz="+resultClz.getName()+
          ", rtnCmpType="+rtnCmpTypes+", ctype="+contentType+
          ", encType="+encodingType+"]";
    }
  }
    
  @Override
  public boolean handleMessage(Message msg) {
    Log.d(TAG, "handleMessage: what="+msg.what);
    switch (msg.what) {
    case MSG_NO_PENDING_REQUESTS:
      if (mAsyncQueueMgr.isEmpty()) {
        Intent intent = new Intent(Call.ACTION_NO_ASYNC_PENDING_REQUESTS);
        intent.setPackage(mContext.getPackageName());
        mContext.sendBroadcast(intent);
      } else {
        Log.d(TAG, "Queues are not empty, no clean up yet");
      }
      break;
    }
    return true;
  }
  
  public static AsyncService getInstance(Context context) {
    if (!sInited.get()) {
      synchronized(sInited) {
        if (!sInited.get()) {
          sInstance = new AsyncService(context);
          sInstance.onCreate();
          sInited.set(true);
        }
      }
    }
    return sInstance;
  }
  
  public static void stopInstance(Context context) {
    synchronized(sInited) {
      if (sInited.get()) {
        sInstance.onDestroy();
        sInstance = null;
        sInited.set(false);
      }
    }
  }
  
  private AsyncService(Context context) {
    mContext = context.getApplicationContext();
    ENCRYPT_REQUEST = MagnetDefaultSettings.getInstance(mContext)
        .getCacheEncryptionEnabled();
  }
  
  public void onCreate() {
    Log.d(TAG, "onCreate()");
//    super.onCreate();
    
    // Initialize the cipher.
    if (MagnetDefaultSettings.getInstance(mContext).getCacheEncryptionEnabled()) {
      FileUtil.initCipher(mContext);
    }
    
    mExecutor = Executors.newCachedThreadPool(mThreadFactory);
    mAsyncQueueMgr = new AsyncQueueManager(mContext, mExecutor);
    mHandlerThread = new MobileHandlerThread("AsyncCleanupThread", this);
    mHandlerThread.start();
  }
  
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");
    
    if (mAsyncQueueMgr != null) {
      mAsyncQueueMgr.clearAllQueues();
      mAsyncQueueMgr = null;
    }
    
    if (mHandlerThread != null) {
      mHandlerThread.getLooper().quit();
      mHandlerThread = null;
    }
    
    AsyncPersister.stopInstance(mContext);
//    super.onDestroy();
  }
  
  /**
   * Process all queued requests.  It is triggered by the constraint observers
   * (e.g. NetworkStateRecceiver, LocationReceiver).  This method is put in
   * the synchronized block because every constraint observer may invoke this
   * method, but we don't want them processing the same queue simultaneously.
   */
  public synchronized void run() {
    Collection<AsyncQueue> alist = mAsyncQueueMgr.getQueues();
    Log.d(TAG, "Processing "+alist.size()+" async queues");
    for (AsyncQueue queue : alist) {
      mExecutor.execute(queue);
    }
  }
  
  public void clearCache() {
    int rows = AsyncPersister.getInstance(mContext).clearCache();
    Log.d(TAG, "clearCache() deleted "+rows+" entries");
  }

    /**
   * Cancel the call.
   * @param callId
   * @param mayInterruptIfRunning
   * @return
   */
  public boolean cancel(String callId, boolean mayInterruptIfRunning) {
    boolean cancelled = false;

    cancelled = mAsyncQueueMgr.removeRequest(callId);

    checkAndHandleNoPendingRqts();
    
    return cancelled;
  }

  /**
   * Dispose the call by its ID if it is in SUCCESS, FAILED, or TIMEDOUT state.
   * @param callId
   * @return
   */
  public boolean dispose(String callId) {
    boolean disposed;

    // For unreliable call, the disposal always fails because it is removed
    // from the queue immediately.  So we always return true even if it is
    // not in a "done" state.
    mAsyncQueueMgr.removeRequest(callId);
    disposed = true;

    checkAndHandleNoPendingRqts();
    
    return disposed;
  }
  
  /**
   * If no more pending requests, broadcast an intent so the constraint
   * observers can disable any underlying receivers.
   */
  public void checkAndHandleNoPendingRqts() {
    Handler handler = mHandlerThread.getHandler();
    synchronized(handler) {
      handler.removeMessages(MSG_NO_PENDING_REQUESTS);
      handler.sendEmptyMessageDelayed(MSG_NO_PENDING_REQUESTS, 60000L);
    }
  }
  
  /**
   * Resend the call only if it failed or timed out.
   * @param call
   * @return false for unable to resend; otherwise, true;
   */
  public boolean resend(CallImpl<?> call) {
    String callId = call.getId();
    // Just a safety check that this call is not on a queue.
    if (mAsyncQueueMgr.getRequestById(callId) != null ||
        call.mRequest == null ||
        call.mRequest.state != State.FAILED &&
        call.mRequest.state != State.TIMEDOUT) {
      return false;
    }
    return mAsyncQueueMgr.requeue(call);
  }

  /**
   * Unreliable controller invocation.  The file used in <code>payload</code>
   * is automatically deleted.
   * @param envelope Envelope containing endpoint connection information.
   * @param path The URL path for the controller method.
   * @param contentType The content type of the payload.
   * @param payload The marshalled payload, or null for GET method.
   * @param options An async unreliable options, or null.
   * @param resultClz The class of the result.
   * @return An instance of CallImpl.
   */
  @SuppressWarnings("unchecked")
  public <T> Call<T> invoke(Envelope envelope, String path, String contentType,
                             Payload payload, AsyncCallOptions options, 
                             Class<T> resultClz, Type[] returnComponentTypes) {
    if (envelope == null || path == null || resultClz == null ||
        (payload != null && contentType == null)) {
      throw new NullPointerException(
          "Envelope, path, content-type or result class type is null in CallImpl request");
    }
    
    if (payload == null) {
      payload = Payload.EMPTY_PAYLOAD;
    } else {
      payload.setDeleteOnSent(true);
    }
    if (options == null) {
      options = new AsyncCallOptions();
    }
    
    HashMap<String, String> headers = new HashMap<String, String>();
    CallRequest request = new CallRequest(envelope, path, headers, contentType,
                                    payload, options, resultClz, 
                                    returnComponentTypes, false);
    // Convert the payload external file to memory.
    //request.convertPayload();
    
    return (Call<T>) mAsyncQueueMgr.enqueue(options.mQueueName, request);
  }
}
