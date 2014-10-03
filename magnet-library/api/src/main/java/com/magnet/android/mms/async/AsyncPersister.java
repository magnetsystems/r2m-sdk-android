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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.magnet.android.mms.async.AsyncPersister.AsyncDBHelper.RequestTable;
import com.magnet.android.mms.async.AsyncPersister.AsyncDBHelper.ResCacheTable;
import com.magnet.android.mms.async.AsyncService.CallRequest;
import com.magnet.android.mms.async.AsyncService.CallResult;
import com.magnet.android.mms.async.AsyncService.Envelope;
import com.magnet.android.mms.async.AsyncService.Payload;
import com.magnet.android.mms.async.Call.State;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.settings.MagnetDefaultSettings;
import com.magnet.android.mms.utils.FileUtil;
import com.magnet.android.mms.utils.FileUtil.InProgressFileOp.ProgressListener;
import com.magnet.android.mms.utils.logger.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is an internal class.  A persister for Mobile Async Request and Result
 * cache.
 */
class AsyncPersister {
  private final static String TAG = "AsyncPersister";
  
  private static boolean ENCRYPT_RESULT;
  private static AsyncPersister sInstance;
  private static AtomicBoolean sInited = new AtomicBoolean(false);
  private Context mContext;
  private AsyncDBHelper mDbHelper;
  private SQLiteDatabase mDb;
  private File mAsyncCacheDir;
  private File mAsyncDataDir;
 
  private final static String[] PROJECTION_CONSTRAINT = {
      RequestTable.ENCRYPTED,
      RequestTable.PARAMS,
      RequestTable.OPTIONS,
      RequestTable.STATE,
  };
  
  private final static String[] PROJECTION_PARAMS = {
      RequestTable.ENCRYPTED,
      RequestTable.PARAMS,
  };
  
  private final static String[] PROJECTION_ENDPOINTS = {
    RequestTable.ENVELOPE,
    RequestTable.CORRELATION_ID,
  };
  
  private final static String[] PROJECTION_REQUEST = { 
      RequestTable._ID,
      RequestTable.CAUSE,
      RequestTable.CORRELATION_ID,
      RequestTable.CREATION_TIME,
      RequestTable.ENCRYPTED,
      RequestTable.ENVELOPE,
      RequestTable.HEADERS,
      RequestTable.MIME_TYPE,
      RequestTable.OPTIONS,
      RequestTable.PARAMS,
      RequestTable.PATH,
      RequestTable.REQUEST_HASH,
      RequestTable.REQUEST_TIME,
      RequestTable.RESULT_TYPE,
      RequestTable.RTN_CMP_TYPES,
      RequestTable.STATE,
      RequestTable.NEED_ACK,
  };
  
  private final static int INDEX_REQUEST_ID = 0;
  private final static int INDEX_REQUEST_CAUSE = 1;
  private final static int INDEX_REQUEST_CORRELATION_ID = 2;
  private final static int INDEX_REQUEST_CREATION_TIME = 3;
  private final static int INDEX_REQUEST_ENCRYPTED = 4;
  private final static int INDEX_REQUEST_ENVELOPE = 5;
  private final static int INDEX_REQUEST_HEADERS = 6;
  private final static int INDEX_REQUEST_MIME_TYPE = 7;
  private final static int INDEX_REQUEST_OPTIONS = 8;
  private final static int INDEX_REQUEST_PARAMS = 9;
  private final static int INDEX_REQUEST_PATH = 10;
  private final static int INDEX_REQUEST_HASH = 11;
  private final static int INDEX_REQUEST_TIME = 12;
  private final static int INDEX_REQUEST_RESULT_TYPE = 13;
  private final static int INDEX_REQUEST_RTN_CMP_TYPES = 14;
  private final static int INDEX_REQUEST_STATE = 15;
  private final static int INDEX_REQUEST_NEED_ACK = 16;
  
  private final static String[] PROJECTION_RESULT = {
      ResCacheTable._ID, 
      ResCacheTable.ENCRYPTED,
      ResCacheTable.CORRELATION_ID,
      ResCacheTable.COMPLETION_TIME, 
      ResCacheTable.REQUEST_HASH,
      ResCacheTable.RESULT_TYPE,
      ResCacheTable.RTN_CMP_TYPES,
      ResCacheTable.MIME_TYPE,
      ResCacheTable.ENCODING_TYPE,
      };
  
  private final static int INDEX_RESULT_ID = 0;
  private final static int INDEX_RESULT_ENCRYPTED = 1;
  private final static int INDEX_RESULT_CORRELATION_ID = 2;
  private final static int INDEX_RESULT_COMPLETION_TIME = 3;
  private final static int INDEX_RESULT_REQUEST_HASH = 4;
  private final static int INDEX_RESULT_TYPE = 5;
  private final static int INDEX_RESULT_RTN_CMP_TYPES = 6;
  private final static int INDEX_RESULT_MIME_TYPE = 7;
  private final static int INDEX_RESULT_ENCODING_TYPE = 8;

  private final static HashMap<String, Class<?>> PRIMITIVE_CLASS = 
        new HashMap<String, Class<?>>();

  // Note: using full path for SQLiteOpenHelper because gradle build barfs even though
  // this class is imported. very strange.
  public class AsyncDBHelper extends android.database.sqlite.SQLiteOpenHelper {
    private final static int DB_VERSION = 1;
    private final static String DB_NAME = "com_magnet_android_mms_async.db";
    
    public class RequestTable {
      public final static String TABLE_NAME = "Request";
      
      public final static String _ID = BaseColumns._ID;
      public final static String ENCRYPTED = "encrypted"; // for intent extras and constraint param
      public final static String NEED_ACK = "needAck";
      public final static String QUEUE_NAME = "queueName";
      public final static String CREATION_TIME = "creationTime";  // queue time
      public final static String REQUEST_TIME = "requestTime";    // sent time
      public final static String EXPIRY_TIME = "expiryTime";
      public final static String CORRELATION_ID = "correlationId";
      public final static String TOKEN = "token";
      public final static String HEADERS = "ctxHeaders";        // headers set at call
      public final static String OPTIONS = "options";             // options
      public final static String STATE = "state";                 // CallImpl.State
      public final static String RESULT_TYPE = "resultType";  // class name
      public final static String RTN_CMP_TYPES = "rtnCmpTypes";   // array of Type
      public final static String REQUEST_HASH = "requestHash";    // MD5/SHA combo
      public final static String ENVELOPE = "envelope";
      public final static String PATH = "path";         // controller method path
      public final static String MIME_TYPE = "mimeType"; // payload content type (json)
      public final static String PARAMS = "params";     // payload in mem/file (json)
      public final static String CAUSE = "cause";       // exception 
    }
    
    public class ResCacheTable {
      public final static String TABLE_NAME = "ResultCache";
      
      public final static String _ID = BaseColumns._ID;   // aka RESULT_ID
      public final static String ENCRYPTED = "encrypted";   // for request and result
      public final static String CORRELATION_ID = "correlationId"; // original request
      public final static String REQUEST_HASH = "requestHash"; // MD5/SHA combo
      public final static String COMPLETION_TIME = "completionTime";  // 0=not executing, -1=queued
      public final static String RESULT_TYPE = "resultType";  // class name
      public final static String RTN_CMP_TYPES = "rtnCmpTypes"; // array of Type
      public final static String MIME_TYPE = "mimeType";      // payload mime type (json/binary
      public final static String ENCODING_TYPE = "encodingType";  // null, binary, base64
    }

    
    AsyncDBHelper(Context context) {
      super(context, DB_NAME, null, DB_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) throws SQLException {
      db.execSQL("CREATE TABLE IF NOT EXISTS " + RequestTable.TABLE_NAME + " ("
          + RequestTable._ID + " INTEGER PRIMARY KEY, "
          + RequestTable.ENCRYPTED + " INTEGER DEFAULT 0, "
          + RequestTable.NEED_ACK + " INTEGER DEFAULT 0, "
          + RequestTable.QUEUE_NAME + " TEXT, "
          + RequestTable.CREATION_TIME + " UNSIGNED INTEGER, "
          + RequestTable.REQUEST_TIME + " UNSIGNED INTEGER, "
          + RequestTable.EXPIRY_TIME + " UNSIGNED INTEGER, "
          + RequestTable.CORRELATION_ID + " TEXT UNIQUE ON CONFLICT REPLACE, "
          + RequestTable.STATE + " TEXT NOT NULL, "
          + RequestTable.TOKEN + " TEXT, "
          + RequestTable.HEADERS + " BLOB, "
          + RequestTable.OPTIONS + " BLOB, "
          + RequestTable.RESULT_TYPE + " TEXT, "  // class name
          + RequestTable.RTN_CMP_TYPES + " BLOB, "
          + RequestTable.REQUEST_HASH + " TEXT NOT NULL, "
          + RequestTable.ENVELOPE + " BLOB NOT NULL, "
          + RequestTable.PATH + " TEXT NOT NULL,"
          + RequestTable.MIME_TYPE + " TEXT NOT NULL, "
          + RequestTable.PARAMS + " BLOB, "
          + RequestTable.CAUSE + " BLOB "
          + ");" );
      
      db.execSQL("CREATE TABLE IF NOT EXISTS "+ResCacheTable.TABLE_NAME+" ("
          + ResCacheTable._ID + " INTEGER PRIMARY KEY, "
          + ResCacheTable.ENCRYPTED + " ENCRYPTED DEFAULT 0, "
          + ResCacheTable.CORRELATION_ID + " TEXT, "
          + ResCacheTable.REQUEST_HASH + " TEXT UNIQUE ON CONFLICT REPLACE, "
          + ResCacheTable.COMPLETION_TIME + " UNSIGNED INTEGER DEFAULT 0, "
          + ResCacheTable.RESULT_TYPE + " TEXT, "   // class name
          + ResCacheTable.RTN_CMP_TYPES + " BLOB, " // return component types
          + ResCacheTable.MIME_TYPE + " TEXT, "     // payload content type
          + ResCacheTable.ENCODING_TYPE + " TEXT "  // payload transfer encoding type
          //+ ResCacheTable.RESULT + " BLOB "  // file name is "cache/async/${REQUEST_HASH}.dat"
          + ");" );
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      // No upgrade yet.
    }
  }
  
  static {
    Class<?>[] classes = new Class[] { 
        void.class, boolean.class, byte.class, char.class, short.class,
        int.class, float.class, double.class, long.class };
    for (Class<?> clz : classes) {
      PRIMITIVE_CLASS.put(clz.getName(), clz);
    }
  }
  
  public static AsyncPersister getInstance(Context context) {
    if (!sInited.get()) {
      synchronized(sInited) {
        if (!sInited.get()) {
          sInstance = new AsyncPersister(context);
          sInstance.init();
          sInited.set(true);
        }
      }
    }
    return sInstance;
  }
  
  public static void stopInstance(Context context) {
    synchronized(sInited) {
      if (sInited.get()) {
        sInstance.close();
        sInstance = null;
        sInited.set(false);
      }
    }
  }
  
  private AsyncPersister(Context context) {
    mContext = context.getApplicationContext();
    ENCRYPT_RESULT = MagnetDefaultSettings.getInstance(mContext)
        .getCacheEncryptionEnabled();
  }
  
  protected void init() {
    if (mDb == null) {
      try {
        if (!createAsyncCacheDir() || !createAsyncDataDir()) {
          Log.e(TAG, "Unable to create async cache or data directory");
        } else {
          mDbHelper = new AsyncDBHelper(mContext);
          mDb = mDbHelper.getWritableDatabase();
        }
      } catch (SQLException e) {
        Log.e(TAG, "Unable to init async DB", e);
        throw new MobileRuntimeException("Unable to init async DB", e);
      }
    }
  }
  
  protected void close() {
    if (mDb != null) {
      mDb.close();
      mDb = null;
    }
    if (mDbHelper != null) {
      mDbHelper.close();
      mDbHelper = null;
    }
  }

  public CallRequest getRequestById(String callId) {
    CallRequest request = null;
    Cursor cursor = mDb.query(RequestTable.TABLE_NAME, PROJECTION_REQUEST,
        RequestTable.CORRELATION_ID+"=?", new String[] { callId },
        null, null, null);
    try {
      if (cursor.moveToNext()) {
        request = toCallRequest(cursor);
      }
    } finally {
      cursor.close();
    }
    return request;
  }

  public List<CallRequest> getPendingRequestsByQueue(String queueName) {
    ArrayList<CallRequest> list = new ArrayList<CallRequest>();
    Cursor cursor = mDb.query(RequestTable.TABLE_NAME, PROJECTION_REQUEST,
        RequestTable.QUEUE_NAME+"=? AND "+RequestTable.STATE+" IN (?,?,?)",
        new String[] { queueName, Call.State.INIT.toString(),
            Call.State.EXECUTING.toString(), Call.State.QUEUED.toString() },
            null, null, RequestTable.REQUEST_TIME);
    try {
      while (cursor.moveToNext()) {
        list.add(toCallRequest(cursor));
      }
      return list;
    } finally {
      cursor.close();
    }
  }

  public CallRequest getPendingRequestById(String callId) {
    CallRequest request = null;
    Cursor cursor = mDb.query(RequestTable.TABLE_NAME, PROJECTION_REQUEST,
        RequestTable.CORRELATION_ID+"=? AND "+RequestTable.STATE+" IN (?,?,?)",
        new String[] { callId, Call.State.INIT.toString(),
            Call.State.EXECUTING.toString(), Call.State.QUEUED.toString() },
            null, null, null);
    try {
      if (cursor.moveToNext()) {
        request = toCallRequest(cursor);
      }
      return request;
    } finally {
      cursor.close();
    }
  }

  public int getNumberPendingRequests() {
    int count = 0;
    Cursor cursor = mDb.rawQuery("SELECT COUNT(*) FROM "+RequestTable.TABLE_NAME+
        " WHERE "+RequestTable.STATE+" IN (?,?,?)", new String[] {
        State.INIT.toString(), State.QUEUED.toString(),
        State.EXECUTING.toString() });
    try {
      if (cursor.moveToNext()) {
        count = cursor.getInt(0);
      }
    } finally {
      cursor.close();
    }
    return count;
  }

  // If a custom constraint cannot be serialized, the caller will get
  // MobileRuntimeException.
  public long addRequest(String name, CallRequest request) {
    long tod = System.currentTimeMillis();
    ContentValues cv = new ContentValues();
    cv.put(RequestTable.CORRELATION_ID, request.correlationId);
    cv.put(RequestTable.CREATION_TIME, tod);
    cv.put(RequestTable.ENCRYPTED, request.isEncrypted ? 1 : 0);
    cv.put(RequestTable.ENVELOPE, FileUtil.serialize(request.envelope));
    if (request.headers == null) {
      cv.putNull(RequestTable.HEADERS);
    } else {
      cv.put(RequestTable.HEADERS, FileUtil.serialize(request.headers));
    }
    cv.put(RequestTable.MIME_TYPE, request.contentType);
    if (request.options == null) {
      cv.putNull(RequestTable.OPTIONS);
    } else {
      cv.put(RequestTable.OPTIONS, FileUtil.serialize(request.options));
    }
    byte[] params = FileUtil.serialize(request.payload);
    if (request.isEncrypted)
      params = FileUtil.encrypt(params);
    cv.put(RequestTable.PARAMS, params);
    cv.put(RequestTable.PATH, request.path);
    cv.putNull(RequestTable.QUEUE_NAME);
    cv.put(RequestTable.REQUEST_HASH, request.computeHash());
    cv.put(RequestTable.REQUEST_TIME, tod);
    cv.put(RequestTable.RESULT_TYPE, request.resultClz.getName());
    if (request.rtnCmpTypes == null) {
      cv.putNull(RequestTable.RTN_CMP_TYPES);
    } else {
      cv.put(RequestTable.RTN_CMP_TYPES,
        FileUtil.serialize(request.rtnCmpTypes));
    }
    cv.put(RequestTable.STATE, request.state.toString());
    cv.put(RequestTable.NEED_ACK, request.needAck ? 1 : 0);
    cv.putNull(RequestTable.TOKEN);
    return mDb.insert(RequestTable.TABLE_NAME, null, cv);
  }

  public boolean updateStateById(String correlationId, CallImpl.State state,
                                   Throwable cause) {
    ContentValues cv = new ContentValues();
    cv.put(RequestTable.STATE, state.toString());
    if (cause == null) {
      cv.putNull(RequestTable.CAUSE);
    } else {
      cv.put(RequestTable.CAUSE, FileUtil.serialize(cause));
    }
    return updateRequestById(correlationId, cv);
  }

  public boolean updateRequestById(String correlationId, ContentValues cv) {
    int rows = mDb.update(RequestTable.TABLE_NAME, cv,
                          RequestTable.CORRELATION_ID+"=?",
                          new String[] { correlationId });
    return (rows == 1);
  }

  private final static String[] PROJECTION_QUEUES = {
    RequestTable.QUEUE_NAME,
  };


  private Class<?> type2Class(String name) throws ClassNotFoundException {
    Class<?> clz = PRIMITIVE_CLASS.get(name);
    if (clz == null) {
      clz = Class.forName(name);
    }
    return clz;
  }
  
  private CallRequest toCallRequest(Cursor cursor) {
    try {
      CallRequest request = new CallRequest();
      request.reqId = cursor.getLong(INDEX_REQUEST_ID);
      request.isEncrypted = cursor.getInt(INDEX_REQUEST_ENCRYPTED) != 0;
      request.state = State.valueOf(cursor.getString(INDEX_REQUEST_STATE));
      request.hash = cursor.getString(INDEX_REQUEST_HASH);
      request.path = cursor.getString(INDEX_REQUEST_PATH);
      request.requestTime = cursor.getLong(INDEX_REQUEST_TIME);
      request.contentType = cursor.getString(INDEX_REQUEST_MIME_TYPE);
      request.correlationId = cursor.getString(INDEX_REQUEST_CORRELATION_ID);
      request.envelope = (Envelope) FileUtil.deserialize(
          cursor.getBlob(INDEX_REQUEST_ENVELOPE));
      request.cause = (Throwable) FileUtil.deserialize(
          cursor.getBlob(INDEX_REQUEST_CAUSE));
      request.headers = (HashMap<String, String>) FileUtil.deserialize(
          cursor.getBlob(INDEX_REQUEST_HEADERS));
      request.options = null;
      byte[] params = cursor.getBlob(INDEX_REQUEST_PARAMS);
      if (request.isEncrypted) {
        params = FileUtil.decrypt(params);
      }
      request.payload = (Payload) FileUtil.deserialize(params);
      request.resultClz = type2Class(cursor.getString(INDEX_REQUEST_RESULT_TYPE));
      request.rtnCmpTypes = (Type[]) FileUtil.deserialize(
          cursor.getBlob(INDEX_REQUEST_RTN_CMP_TYPES));
      request.needAck = cursor.getInt(INDEX_REQUEST_NEED_ACK) != 0;

      return request;
    } catch (Throwable e) {
      Log.e(TAG, "Unable to convert request to CallRequest", e);
      return null;
    }
  }
  
  private CallResult toCallResult(Cursor cursor) {
    try {
      CallResult result = new CallResult(mContext);
      result.resId = cursor.getLong(INDEX_RESULT_ID);
      result.requestHash = cursor.getString(INDEX_RESULT_REQUEST_HASH);
      long completionTime = cursor.getLong(INDEX_RESULT_COMPLETION_TIME);
      result.resultTime = (completionTime == 0) ? null : new Date(completionTime);
      result.correlationId = cursor.getString(INDEX_RESULT_CORRELATION_ID);;
      result.isEncrypted = (cursor.getInt(INDEX_RESULT_ENCRYPTED) != 0);
      result.resultClz = type2Class(cursor.getString(INDEX_RESULT_TYPE));
      result.rtnCmpTypes = (Type[]) FileUtil.deserialize(
          cursor.getBlob(INDEX_RESULT_RTN_CMP_TYPES));
      result.contentType = cursor.getString(INDEX_RESULT_MIME_TYPE);
      result.encodingType = cursor.getString(INDEX_RESULT_ENCODING_TYPE);
      return result;
    } catch (Throwable e) {
      Log.e(TAG, "Unable to convert result to CallResult", e);
      return null;
    }
  }
  
  public CallRequest getPendingRequestByToken(String token) {
    if (token == null) {
      return null;
    }

    CallRequest request = null;
    Cursor cursor = mDb.query(RequestTable.TABLE_NAME, PROJECTION_REQUEST,
        RequestTable.TOKEN+"=? AND "+RequestTable.STATE+" IN (?,?)",
        new String[] { token, CallImpl.State.EXECUTING.toString(),
                        CallImpl.State.QUEUED.toString() },
        null, null, null);
    try {
      if (cursor.moveToNext()) {
        request = toCallRequest(cursor);
      }
    } catch (Throwable e) {
      Log.e(TAG, "Unable to get pending request by token", e);
    } finally {
      cursor.close();
    }
    return request;
  }

  public boolean removePendingRequestById(String callId,
                                            boolean mayInterruptIfRunning) {
    if (mayInterruptIfRunning) {
      return mDb.delete(RequestTable.TABLE_NAME,
        RequestTable.CORRELATION_ID+"=? AND "+RequestTable.STATE+" IN (?,?,?)",
        new String[] { callId, CallImpl.State.INIT.toString(),
            CallImpl.State.EXECUTING.toString(), CallImpl.State.QUEUED.toString() }) > 0;
    } else {
      return mDb.delete(RequestTable.TABLE_NAME,
          RequestTable.CORRELATION_ID+"=? AND "+RequestTable.STATE+" IN (?,?)",
          new String[] { callId, CallImpl.State.INIT.toString(),
                          CallImpl.State.QUEUED.toString() }) > 0;
    }
  }

  public boolean removeDoneRequestById(String callId) {
    Cursor cursor = mDb.query(RequestTable.TABLE_NAME, PROJECTION_PARAMS,
        RequestTable.CORRELATION_ID+"=? AND "+RequestTable.STATE+" IN (?,?,?)",
        new String[] { callId, CallImpl.State.SUCCESS.toString(),
                        CallImpl.State.FAILED.toString(),
                        CallImpl.State.TIMEDOUT.toString() },
        null, null, null);
    deletePayloadFiles(cursor);
    return mDb.delete(RequestTable.TABLE_NAME,
        RequestTable.CORRELATION_ID+"=? AND "+RequestTable.STATE+" IN (?,?,?)",
        new String[] { callId, CallImpl.State.SUCCESS.toString(),
                        CallImpl.State.FAILED.toString(),
                        CallImpl.State.TIMEDOUT.toString() }) > 0;
  }

  private void deletePayloadFile(boolean encrypted, byte[] params) {
    if (encrypted) {
      params = FileUtil.decrypt(params);
    }
    Payload payload = (Payload) FileUtil.deserialize(params);
    if (payload != null) {
      payload.deleteFile();
    }
  }
  
  private void deletePayloadFiles(Cursor cursor) {
    try {
      while (cursor.moveToNext()) {
        boolean encrypted = (cursor.getInt(0) == 1);
        byte[] params = cursor.getBlob(1);
        deletePayloadFile(encrypted, params);
      }
    } finally {
      cursor.close();
    }
  }
  
  public boolean removeRequestById(String callId) {
    return mDb.delete(RequestTable.TABLE_NAME, RequestTable.CORRELATION_ID+"=?",
        new String[] { callId } ) > 0;
  }
  
  public CallResult getCacheByRequestHash(String requestHash) {
    CallResult result = null;
    Cursor cursor = mDb.query(ResCacheTable.TABLE_NAME, PROJECTION_RESULT, 
        ResCacheTable.REQUEST_HASH+"=?", new String[] { requestHash }, 
        null, null, null);
    try {
      if (cursor.moveToNext()) {
        result = toCallResult(cursor);
      }
    } catch (Throwable e) {
      Log.e(TAG, "Unable to match a cache with a request", e);
    } finally {
      cursor.close();
    }
    return result;
  }
  
  /**
   * Delete all rows in DB and remove all files in the cache directory.
   * @return
   */
  public int clearCache() {
    int rows = mDb.delete(ResCacheTable.TABLE_NAME, null, null);
    File cacheDir = getAsyncCacheDir();
    File[] files = cacheDir.listFiles();
    if (files != null) {
      for (File file : files) {
        file.delete();
      }
    }
    return rows;
  }

  public HashMap<String, ArrayList<String>> getSuccessCallsGroupByEndpoint() {
    HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
    Cursor cursor = mDb.query(RequestTable.TABLE_NAME, PROJECTION_ENDPOINTS,
        RequestTable.STATE+"=? AND "+RequestTable.NEED_ACK+"=1", 
        new String[] { State.SUCCESS.toString() }, null, null, null);
    try {
      ArrayList<String> list;
      while (cursor.moveToNext()) {
        Envelope envelope = (Envelope) FileUtil.deserialize(cursor.getBlob(0));
        String endPoint = envelope.getEndPoint();
        if ((list = map.get(endPoint)) == null) {
          list = new ArrayList<String>();
          map.put(endPoint, list);
        }
        list.add(cursor.getString(1));
      }
    } finally {
      cursor.close();
    }
    return map;
  }
  
  /**
   * Remove all done calls whose states are SUCCESS, FAILED or TIMEDOUT.  The
   * file in the payload will be deleted.
   * @return
   */
  public int disposeAllDoneCalls() {
    Cursor cursor = mDb.query(RequestTable.TABLE_NAME, PROJECTION_PARAMS,
        RequestTable.STATE+" IN (?,?,?,?)", new String[] {
          State.SUCCESS.toString(), State.FAILED.toString(),
          State.TIMEDOUT.toString(), State.CANCELLED.toString() },
        null, null, null);
    deletePayloadFiles(cursor);
    int rows = mDb.delete(RequestTable.TABLE_NAME, RequestTable.STATE+" IN (?,?,?,?)",
        new String[] { State.SUCCESS.toString(), State.FAILED.toString(),
                       State.TIMEDOUT.toString(), State.CANCELLED.toString() });
    Log.d(TAG, "disposeAllDoneCalls(): deletes "+rows+" rows");
    return rows;
  }

  /**
   * Cancel all pending calls whose states are INIT, QUEUED or EXECUTING.  All
   * constraints in the QUEUED calls will be released.
   * @return
   */
  public int cancelAllPendingCalls() {
    mDb.beginTransaction();
    Cursor cursor = mDb.query(RequestTable.TABLE_NAME, PROJECTION_CONSTRAINT,
        RequestTable.STATE+" IN (?,?,?)", new String[] { State.INIT.toString(),
          State.QUEUED.toString(), State.EXECUTING.toString() },
        null, null, null);
    try {
      while (cursor.moveToNext()) {
        deletePayloadFile(cursor.getInt(0)==1, cursor.getBlob(1));
        // Release the constraint for the QUEUED pending calls
        if (cursor.getString(3).equals(Call.State.QUEUED.toString())) {
          Options options = (Options) FileUtil.deserialize(cursor.getBlob(2));
        }
      }
    } finally {
      cursor.close();
    }
    int rows = mDb.delete(RequestTable.TABLE_NAME, RequestTable.STATE+" IN (?,?,?)",
              new String[] { State.INIT.toString(), State.QUEUED.toString(),
                              State.EXECUTING.toString() });
    mDb.setTransactionSuccessful();
    mDb.endTransaction();
    return rows;
  }

  /**
   * Cancel all pending calls which have INIT, QUEUED or EXECUTING states within
   * a queue.  All constraints in the QUEUED calls state will be released.
   * @param queueName
   * @return
   */
  public int cancelAllPendingCalls(String queueName) {
    mDb.beginTransaction();
    Cursor cursor = mDb.query(RequestTable.TABLE_NAME, PROJECTION_CONSTRAINT,
        RequestTable.QUEUE_NAME+"=? AND "+RequestTable.STATE+" IN (?,?,?)",
        new String[] { queueName, State.INIT.toString(),
                        State.QUEUED.toString(), State.EXECUTING.toString() },
        null, null, null);
    try {
      while (cursor.moveToNext()) {
        deletePayloadFile(cursor.getInt(0)==1, cursor.getBlob(1));
        // Release the constraint for the QUEUED pending calls
        if (cursor.getString(3).equals(Call.State.QUEUED.toString())) {
          Options options = (Options) FileUtil.deserialize(cursor.getBlob(2));
        }
      }
    } finally {
      cursor.close();
    }
    int rows = mDb.delete(RequestTable.TABLE_NAME,
        RequestTable.QUEUE_NAME+"=? AND "+RequestTable.STATE+" IN (?,?,?)",
        new String[] { queueName, State.INIT.toString(),
                        State.QUEUED.toString(), State.EXECUTING.toString() });
    mDb.setTransactionSuccessful();
    mDb.endTransaction();
    return rows;
  }


  public boolean removeCacheByRequestHash(String requestHash) {
    int rows = mDb.delete(ResCacheTable.TABLE_NAME,
        ResCacheTable.REQUEST_HASH+"=?", new String[] { requestHash });
    File cacheFile = getAsyncCacheFile(requestHash);
    cacheFile.delete();
    return (rows == 1);
  }
  
  /**
   * Add a pending cache or a completed cache result.  A pending cache means
   * that a request is being executed and waits for a response.  Typically,
   * the completion time for a pending cache is 0.
   * @param request
   * @param contentType
   * @param encodingType
   * @param payload null for pending cache, or a result payload.
   * @return
   */
  public CallResult addCache(CallRequest request, String contentType, 
                              String encodingType, InputStream payload,
                              ProgressListener listener) {
    // Save the payload (if any) to an external storage.
    if (payload != null) {
      if (!saveResult(payload, request.computeHash(), listener)) {
        return null;
      }
    }

    ContentValues cv = new ContentValues();
    cv.put(ResCacheTable.ENCRYPTED, ENCRYPT_RESULT ? 1 : 0);
    cv.put(ResCacheTable.CORRELATION_ID, request.correlationId);
    cv.put(ResCacheTable.REQUEST_HASH, request.computeHash());
    cv.put(ResCacheTable.RESULT_TYPE, request.resultClz.getName());
    if (request.rtnCmpTypes == null) {
      cv.putNull(ResCacheTable.RTN_CMP_TYPES);
    } else {
      cv.put(ResCacheTable.RTN_CMP_TYPES, FileUtil.serialize(request.rtnCmpTypes));
    }
    cv.put(ResCacheTable.MIME_TYPE, contentType);
    if (encodingType == null) {
      cv.putNull(ResCacheTable.ENCODING_TYPE);
    } else {
      cv.put(ResCacheTable.ENCODING_TYPE, encodingType);
    }
    long completionTime = (payload != null) ? System.currentTimeMillis() : 0L;
    cv.put(ResCacheTable.COMPLETION_TIME, completionTime);
    long resultId = mDb.insert(ResCacheTable.TABLE_NAME, null, cv);
    if (resultId <= 0) {
      return null;
    }

    CallResult result = new CallResult(mContext);
    result.resId = resultId;
    result.correlationId = request.correlationId;
    result.resultClz = request.resultClz;
    result.rtnCmpTypes = request.rtnCmpTypes;
    result.requestHash = request.computeHash();
    if (payload != null)
      result.resultTime = new Date(completionTime);
    result.contentType = contentType;
    result.encodingType = encodingType;
    result.isEncrypted = ENCRYPT_RESULT;
    return result;
  }

  /**
   * Get the cached result as a byte array.
   * @param requestHash
   * @return
   */
  public byte[] getResult(String requestHash) {
    File cacheFile = getAsyncCacheFile(requestHash);
    return FileUtil.fileToByteArray(cacheFile);
  }
  
  /**
   * Get the cached result as InputStream.
   * @param requestHash
   * @return
   */
  public InputStream getResultAsInputStream(String requestHash) {
    File cacheFile = getAsyncCacheFile(requestHash);
    try {
      return new FileInputStream(cacheFile);
    } catch (IOException e) {
      Log.e(TAG, "getResultAsInputStream failed requestHash="+requestHash, e);
      return null;
    }
  }
  
  /**
   * Get the cached result as a File.
   * @param requestHash
   * @return
   */
  public File getResultAsFile(String requestHash) {
    return getAsyncCacheFile(requestHash);
  }
  
  /**
   * Get the cached result size.
   * @param requestHash
   * @return
   */
  public long getResultSize(String requestHash) {
    File cacheFile = getAsyncCacheFile(requestHash);
    return cacheFile.length();
  }
  
  File createAsyncDataTempFile() {
    try {
      return File.createTempFile("tmp", ".dat", getAsyncDataDir());
    } catch (IOException e) {
      Log.e(TAG, "Unable to create temp file in async directory", e);
      return null;
    }
  }
  
  File getAsyncDataFile(String requestHash) {
    File asyncDataDir = getAsyncDataDir();
    File dataFile = new File(asyncDataDir.getAbsolutePath()+"/"+requestHash+".dat");
    return dataFile;
  }
  
  boolean isAsyncDataFile(String path) {
    return path.contains(getAsyncDataDir().getAbsolutePath());
  }
  
  private File getAsyncCacheFile(String requestHash) {
    File asyncCacheDir = getAsyncCacheDir();
    File cacheFile = new File(asyncCacheDir.getAbsolutePath()+"/"+requestHash+".dat");
    return cacheFile;
  }
  
  private File getAsyncDataDir() {
    if (mAsyncDataDir == null) {
      String path = mContext.getFilesDir().getAbsolutePath()+"/async";
      mAsyncDataDir = new File(path);
    }
    return mAsyncDataDir;
  }
  
  private File getAsyncCacheDir() {
    if (mAsyncCacheDir == null) {
      String path = mContext.getCacheDir().getAbsolutePath()+"/async";
      mAsyncCacheDir = new File(path);
    }
    return mAsyncCacheDir;
  }
  
  private boolean createAsyncDataDir() {
    File asyncDataDir = getAsyncDataDir();
    if (!asyncDataDir.exists()) {
      return asyncDataDir.mkdir();
    }
    return true;
  }
  
  private boolean createAsyncCacheDir() {
    File asyncCacheDir = getAsyncCacheDir();
    if (!asyncCacheDir.exists()) {
      return asyncCacheDir.mkdir();
    }
    return true;
  }
  
  // Save the payload in an external cache directory, not in DB.
  private boolean saveResult(InputStream payload, String requestHash, 
                               ProgressListener listener) {
    File cacheFile = getAsyncCacheFile(requestHash);
    OutputStream fos = null;
    try {
      fos = new FileOutputStream(cacheFile);
      if (ENCRYPT_RESULT) {
        fos = FileUtil.encrypt(fos);
      }
      boolean saved = FileUtil.tee(payload, new FileUtil.OutputFileOp(fos),
                        new FileUtil.InProgressFileOp(listener));
      if (!saved) {
        cacheFile.delete();
      }
      return saved;
    } catch (Throwable e) {
      cacheFile.delete();
      Log.e(TAG, "Unable to save result in file", e);
      return false;
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }
}
