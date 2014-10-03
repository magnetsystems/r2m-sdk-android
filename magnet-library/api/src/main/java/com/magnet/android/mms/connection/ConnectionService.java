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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Contains the Request and Response interfaces used in communication with the backend.
 */
public interface ConnectionService {
  /**
   * The generic request object that encapsulates headers and
   * payload to be sent out to an endpoint.
   */
  public interface Request {
    /**
     * The HTTP request method.
     */
    public enum Method {
      /**
       * HTTP GET method. Retrieves information.
       */
      GET,
      /**
       * HTTP POST method. Sends new information.
       */
      POST,
      /**
       * HTTP PUT method. Updates information.
       */
      PUT,
      /**
       * HTTP DELETE method. Deletes information.
       */
      DELETE,
      /**
       * HTTP OPTIONS method. Request for options available.
       */
      OPTIONS,
      /**
       * HTTP HEAD method. Equivalent to GET but the server does not return a message-body in the response.
       */
      HEAD,
      /**
       * HTTP TRACE method. Invokes a loop back of the request message.
       */
      TRACE
    }

    ;

    /**
     * Set the path associated with the request.  If this path starts with a "/"
     * it will be interpreted as an absolute path from the host.  Otherwise, it will be
     * appended to the existing path.  For example, if the connection's base is "http://foo.com/bar":
     * <ul>
     * <li><code>setPath("abc")</code> results in "http://foo.com/bar/abc"</li>
     * <li><code>setPath("/abc")</code> results in "http://foo.com/abc"</li>
     * </ul>
     */
    public void setPath(String path);

    /**
     * Sets the method for this request.
     *
     * @param method The method for this request.
     */
    public void setMethod(Method method);

    /**
     * Adds or replaces the header name with the specified value.
     *
     * @param headerName  The header name.
     * @param headerValue The value with which to add or replace the header name.
     */
    public void setHeader(String headerName, String headerValue);

    /**
     * Adds all the headers from the specified header map.  Any
     * existing headers with the same name will be overwritten.
     *
     * @param headers A map containing the headers to be added.
     */
    public void addHeaders(HashMap<String, String> headers);

    /**
     * Removes the existing headers and adds the new headers.
     *
     * @param headers A map containing the new headers to be added.
     */
    public void setHeaders(HashMap<String, String> headers);

    /**
     * Removes a single header entry with the specified name.
     *
     * @param name The name of the header entry to be removed.
     */
    public void removeHeader(String name);

    /**
     * Set the mimetype associated with the payload.
     *
     * @param mimeType The mimetype to be associated with the payload.
     */
    public void setContentType(String mimeType);

    /**
     * Set the transfer encoding for the payload.
     *
     * @param transferEncoding The transfer encoding for the payload.
     */
    public void setContentTransferEncoding(String transferEncoding);

    /**
     * Specifies the payload of the request.  Any existing
     * payload will be replaced.
     *
     * @param payload The payload of the request.
     */
    public void setPayload(InputStream payload);

    /**
     * Specifies the payload of the request.  Any existing
     * payload will be replaced.
     *
     * @param payload The payload of the request.
     */
    public void setPayload(String payload);

    /**
     * Specifies the payload of the request.  Any existing
     * payload will be replaced.
     *
     * @param bytes  The byte array of the request payload.
     * @param offset The offset into the byte array.
     * @param length The length of the byte array.
     */
    public void setPayload(byte[] bytes, int offset, int length);

    /**
     * Executes this request
     *
     * @return The response to the request.
     */
    public Response execute() throws IOException;
  }

  /**
   * The response interface.
   */
  public interface Response {
    public enum Status {
      /**
       * Success status.
       */
      SUCCESS,
      /**
       * Error status.
       */
      ERROR,
      /**
       * Server error status.
       */
      SERVER_ERROR
    }

    /**
     * Retrieves the status code for this response.
     *
     * @return The status code for this response.
     */
    public Status getStatus();

    /**
     * Retrieves the response code for this response.
     *
     * @return The response code for this response.
     */
    public int getResponseCode();

    /**
     * Retrieves an immutable map containing the
     * headers for this response.
     *
     * @return The headers for this response.
     */
    public Map<String, List<String>> getHeaders();

    /**
     * Retrieves the payload content type as a MIME type.
     *
     * @return The payload content type.
     */
    public String getContentType();

    /**
     * Retrieves the payload content transfer encoding.
     *
     * @return The payload content transfer encoding.
     */
    public String getContentTransferEncoding();

    /**
     * Retrieves the payload for this response.
     *
     * @return The payload for this response.
     */
    public InputStream getPayload();

    /**
     * Release the resources for this response.  This must always
     * be called.  Otherwise, the underlying connection may remain open.
     */
    public void release();
  }

  /**
   * Creates an empty request to be executed for this service.
   *
   * @return An empty request to be executed for this service.
   */
  public Request createRequest();


}
