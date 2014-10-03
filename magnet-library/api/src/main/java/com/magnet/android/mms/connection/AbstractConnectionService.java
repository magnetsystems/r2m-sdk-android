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

package com.magnet.android.mms.connection;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * (Internal Use Only) Abstract class that must be used for extended ConnectionService implementations.
 */
abstract class AbstractConnectionService implements ConnectionService {
  private Context mContext = null;

  protected AbstractConnectionService(Context context)
      throws InstantiationException, IllegalAccessException {
    mContext = context;
  }

  protected Context getContext() {
    return mContext;
  }

  abstract static class AbstractRequest implements Request {
    private Request.Method mMethod = Method.GET; //default to GET
    private String mPath;
    private HashMap<String, String> mHeaders;
    private InputStream mPayloadInputStream;
    private String mContentType;
    private String mContentTransferEncoding;
    private String mPayloadString;
    private byte[] mPayloadBytes;
    private int mPayloadBytesOffset = -1;
    private int mPayloadBytesLength = -1;

    protected AbstractRequest() {
      mHeaders = new HashMap<String, String>();
    }

    public void setMethod(Request.Method method) {
      mMethod = method;
    }

    protected Request.Method getMethod() {
      return mMethod;
    }

    public String getPath() {
      return mPath;
    }

    public void setPath(String path) {
      mPath = path;
    }

    public void setHeader(String headerName, String headerValue) {
      synchronized (mHeaders) {
        mHeaders.put(headerName, headerValue);
      }
    }

    public void addHeaders(HashMap<String, String> headers) {
      if (headers == null || headers.size() == 0) {
        return;
      }
      synchronized (mHeaders) {
        for (Entry<String, String> entry:headers.entrySet()) {
          mHeaders.put(entry.getKey(), entry.getValue());
        }
      }
    }

    public void setHeaders(HashMap<String, String> headers) {
      synchronized (mHeaders) {
        mHeaders.clear();
        addHeaders(headers);
      }
    }

    public void removeHeader(String name) {
      synchronized (mHeaders) {
        mHeaders.remove(name);
      }
    }

    protected String getContentType() {
      return mContentType;
    }

    public void setContentType(String contentType) {
      mContentType = contentType;
    }

    protected String getContentTransferEncoding() {
      return mContentTransferEncoding;
    }

    public void setContentTransferEncoding(String transferEncoding) {
      mContentTransferEncoding = transferEncoding;
    }

    private void resetPayload() {
      mPayloadString = null;
      mPayloadInputStream = null;
      mPayloadBytes = null;
      mPayloadBytesOffset = -1;
      mPayloadBytesLength = -1;
    }

    public void setPayload(InputStream payload) {
      resetPayload();
      mPayloadInputStream = payload;
    }

    public void setPayload(String payload) {
      resetPayload();
      mPayloadString = payload;
    }

    public void setPayload(byte[] bytes, int offset, int length) {
      resetPayload();
      mPayloadBytes = bytes;
      mPayloadBytesOffset = offset;
      mPayloadBytesLength = length;
    }

    protected HashMap<String, String> getHeaders() {
      return mHeaders;
    }

    protected InputStream getPayloadInputStream() {
      return mPayloadInputStream;
    }

    protected String getPayloadString() {
      return mPayloadString;
    }

    protected byte[] getPayloadBytes() {
      return mPayloadBytes;
    }

    protected int getPayloadBytesOffset() {
      return mPayloadBytesOffset;
    }

    protected int getPayloadBytesLength() {
      return mPayloadBytesLength;
    }

    abstract public Response execute() throws IOException;
  }

  abstract static class AbstractResponse implements Response {
    private Status mStatus;
    private Map<String, List<String>> mHeaders;
    private String mContentType;
    private String mContentTransferEncoding;
    private InputStream mPayload;
    private int mResponseCode;

    protected AbstractResponse() {
      this(new HashMap<String, List<String>>());
    }

    protected AbstractResponse(Map<String, List<String>> headers) {
      mHeaders = headers;
    }

    public Status getStatus() {
      return mStatus;
    }

    void setStatus(Status status) {
      mStatus = status;
    }

    public int getResponseCode() {
      return mResponseCode;
    }

    void setResponseCode(int responseCode) {
      mResponseCode = responseCode;
    }

    public Map<String, List<String>> getHeaders() {
      return mHeaders;
    }

    public String getContentType() {
      return mContentType;
    }

    public void setContentType(String contentType) {
      mContentType = contentType;
    }

    public String getContentTransferEncoding() {
      return mContentTransferEncoding;
    }

    public void setContentTransferEncoding(String transferEncoding) {
      mContentTransferEncoding = transferEncoding;
    }

    public InputStream getPayload() {
      return mPayload;
    }

    void setPayload(InputStream payload) {
      mPayload = payload;
    }

    abstract public void release();
  }
}
