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
import android.net.Uri;

import com.magnet.android.core.GenericRestConstants;
import com.magnet.android.mms.connection.ConnectionService.Response.Status;
import com.magnet.android.mms.settings.MagnetDefaultSettings;
import com.magnet.android.mms.utils.logger.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;


public class GenericRestConnectionService extends AbstractConnectionService {
  private static final String TAG = GenericRestConnectionService.class.getSimpleName().substring(0,23);
  private static final int BUFFER_SIZE = 1024;
  private static final Map<String, String> HEADER_MAP;

  static {
    HashMap<String, String> headerMap = new HashMap<String, String>();
    HEADER_MAP = Collections.unmodifiableMap(headerMap);
  }

  protected GenericRestConnectionService(Context context) throws InstantiationException, IllegalAccessException {
    super(context);
  }


  public Request createRequest() {
    return new GenericRequest();
  }

  public final class GenericRequest extends AbstractRequest {
    public Response execute() throws IOException {
      try {
        Context context = getContext();

        //Setup the path
        //use path instead of uri, as it will b null
        Uri uriWithPath = Uri.parse(getPath());
        URL url = new URL(uriWithPath.toString());

        Log.d(TAG, " Generic URL -: " + url.toString());

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connection instanceof HttpsURLConnection) {
          HttpsURLConnection sslConnection = (HttpsURLConnection) connection;
          sslConnection.setHostnameVerifier(SslManager.getInstance(context).getHostnameVerifier());
        }
        MagnetDefaultSettings settings = MagnetDefaultSettings.getInstance(context);
        connection.setConnectTimeout(settings.getHttpConnectTimeoutMillis());
        connection.setReadTimeout(settings.getHttpReadTimeoutMillis());//don't block indefinitely
        connection.setDoInput(true);

        //Set the method
        Method methodObj = getMethod();
        String method = methodObj.name();
        Log.d(TAG, "GenericRequest.execute(): request method=" + method);
        connection.setRequestMethod(method);

        String requestContentType = getContentType();
        String requestContentTransferEncoding = getContentTransferEncoding();
        if (requestContentType != null) {
          connection.setRequestProperty(GenericRestConstants.Header.CONTENT_TYPE, requestContentType);
        }
        if (requestContentTransferEncoding != null) {
          connection.setRequestProperty(GenericRestConstants.Header.CONTENT_TRANSFER_ENCODING, requestContentTransferEncoding);
        }

        //Add headers
        HashMap<String, String> headers = getHeaders();
        for (Entry<String, String> entry:headers.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          String mappedKey = HEADER_MAP.get(key);
          if (mappedKey != null) {
            //this is a mapped header
            key = mappedKey;
          }
          connection.setRequestProperty(key, value);
        }

         //use path instaed of uri, as it will b null
        Uri uri = Uri.parse(getPath());//config.getUri();
        URI bigUri = URI.create(uri.toString());

        boolean isDetailedLogging = true;//Log.isLoggable(Log.VERBOSE);
        if (isDetailedLogging) {
          Map<String, List<String>> requestHeaders = connection.getRequestProperties();
          StringBuffer sb = new StringBuffer("GenericRequest.execute():\n    HTTP Request Headers: \n");
          for (Entry<String, List<String>> entry:requestHeaders.entrySet()) {
            sb.append("    ").append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
          }
          Log.v(TAG, sb.toString());
        }

        //Setup payload if necessary
        if (getPayloadString() != null ||
            getPayloadBytes() != null ||
            getPayloadInputStream() != null) {
          connection.setDoOutput(true);
          OutputStream out = null;
          try {
            int bytesSent = 0;
            String payloadBufferForDebug = null;
            if (getPayloadString() != null) {
              Log.d(TAG, "GenericRequest.execute(): sending the request string payload");
              byte[] payloadBytes = getPayloadString().getBytes();
              bytesSent = payloadBytes.length;
              connection.setRequestProperty("Content-Length", String.valueOf(bytesSent));
              // commenting out setFixecLengthStreamingMode() - causes not RetryableInputStream exception
              //connection.setFixedLengthStreamingMode(bytesSent);
              out = new BufferedOutputStream(connection.getOutputStream());
              out.write(payloadBytes);
              if (isDetailedLogging) {
                payloadBufferForDebug = getPayloadString();
              }
            } else if (getPayloadInputStream() != null) {
              Log.d(TAG, "GenericRequest.execute(): sending the request InputStream payload");
              // commenting out setChunkedStreamingMode() - causes not RetryableInputStream exception
              //connection.setChunkedStreamingMode(0);
              out = new BufferedOutputStream(connection.getOutputStream());
              InputStream bis = new BufferedInputStream(getPayloadInputStream());
              StringBuilder payloadStringBuilder = null;
              try {
                byte[] buffer = new byte[BUFFER_SIZE];
                if (isDetailedLogging) {
                  payloadStringBuilder = new StringBuilder();
                }
                int count = 0;
                while ((count = bis.read(buffer)) >= 0) {
                  out.write(buffer, 0, count);
                  bytesSent = bytesSent + count;
                  if (payloadStringBuilder != null) {
                    payloadStringBuilder.append(new String(buffer, 0, count));
                  }
                }
              } finally {
                bis.close();
              }
              if (payloadStringBuilder != null) {
                payloadBufferForDebug = payloadStringBuilder.toString();
              }
            } else if (getPayloadBytes() != null) {
              Log.d(TAG, "GenericRequest.execute(): sending the request byte[] payload");
              byte[] payloadBytes = getPayloadBytes();
              bytesSent = getPayloadBytesLength();
              // commenting out setFixecLengthStreamingMode() - causes not RetryableInputStream exception
//              connection.setFixedLengthStreamingMode(bytesSent);
              connection.setRequestProperty("Content-Length", String.valueOf(bytesSent));
              out = new BufferedOutputStream(connection.getOutputStream());
              out.write(payloadBytes, getPayloadBytesOffset(), bytesSent);
              if (isDetailedLogging) {
                payloadBufferForDebug = new String(payloadBytes, getPayloadBytesOffset(), bytesSent);
              }
            }
            if (payloadBufferForDebug != null) {
              Log.v(TAG, "GenericRequest.execute():\n    HTTP Request Payload: \n" +
                  "    " + payloadBufferForDebug);
            }
            Log.d(TAG, "GenericRequest.execute(): request bytes sent=" + bytesSent);
          } finally {
            if (out != null) {
              out.flush();
              out.close();
            }
          }
        }

        //REQUEST IS ISSUED, TIME FOR RESPONSE
        //figure out the Response status
        int responseCode = connection.getResponseCode();
        Log.d(TAG, "GenericRequest.execute(): response code = " + responseCode);
        int httpCodeRange = responseCode / 100;
        Response.Status status = Status.ERROR;
        if (httpCodeRange == 2) {
          status = Status.SUCCESS;
        }

        //get the headers
        Map<String, List<String>> responseHeaders = connection.getHeaderFields();

        //if debugging
        if (isDetailedLogging) {
          StringBuffer sb = new StringBuffer("GenericRequest.execute():\n    HTTP Response Headers: \n");
          for (Entry<String, List<String>> entry:responseHeaders.entrySet()) {
            sb.append("    ").append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
          }
          Log.v(TAG, sb.toString());
        }

        //build response
        GenericResponse response = new GenericResponse(responseHeaders, connection);
        response.setResponseCode(responseCode);
        response.setStatus(status);
        List<String> contentTypeHeader = responseHeaders.get(GenericRestConstants.Header.CONTENT_TYPE);
        if (contentTypeHeader != null) {
          //taking the first value
          response.setContentType(contentTypeHeader.get(0));
        }
        List<String> transferEncodingHeader = responseHeaders.get(GenericRestConstants.Header.CONTENT_TRANSFER_ENCODING);
        if (transferEncodingHeader != null) {
          //taking the first value
          response.setContentTransferEncoding(transferEncodingHeader.get(0));
        }
        InputStream input = null;
        try {
          input = new BufferedInputStream(connection.getInputStream());
        } catch (Exception ex) {
          input = new BufferedInputStream(connection.getErrorStream());
        }
        response.setPayload(input);

        return response;
      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }
  }

  public final class GenericResponse extends AbstractResponse {
    private HttpURLConnection mConnection;

    GenericResponse(Map<String, List<String>> headers, HttpURLConnection connection) {
      super(headers);
      mConnection = connection;
      setContentType("text/plain");
    }

    public void setResponseCode(int responseCode){
        super.setResponseCode(responseCode);
    }

    public void release() {
      InputStream is = getPayload();
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
      mConnection.disconnect();
    }
  }
}
