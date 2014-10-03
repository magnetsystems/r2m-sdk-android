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
package com.magnet.android.mms.request.marshall;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.magnet.android.mms.exception.MarshallingException;

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A GSON based implementation of the JsonReader.
 */
public class GsonStreamReader implements JsonReaderAdapter {

  private final JsonReader jr;

  public GsonStreamReader(InputStream inputStream) {
    InputStreamReader reader = new InputStreamReader(inputStream);
    jr = new JsonReader(reader);
    jr.setLenient(true);
  }

  /**
   * @param jsonReader existing JsonReader - stream is pointing to existing position
   */
  public GsonStreamReader(JsonReader jsonReader) {
    jr = jsonReader;
  }

  public JsonReader getReader() {
    return jr;
  }

  @Override
  public void consumeStart() throws MarshallingException {
    try {
      jr.beginObject();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void consumeEnd() throws MarshallingException {
    try {
      jr.endObject();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void consumeNull() throws MarshallingException {
    try {
      jr.nextNull();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void consumeStartArray() throws MarshallingException {
    try {
      jr.beginArray();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void consumeEndArray() throws MarshallingException {
    try {
      jr.endArray();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public boolean isDocumentFinished() throws MarshallingException {
    try {
      return !jr.hasNext();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public void finishDocument() throws MarshallingException {
    try {
      jr.close();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public int getTokenType() throws MarshallingException {
    int result = TOKEN_TYPE_FIELD_VALUE;
    JsonToken tokenType;
    try {
      tokenType = jr.peek();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
    if (tokenType == JsonToken.BEGIN_OBJECT) {
      result = TOKEN_TYPE_START_OBJECT;
    } else if (tokenType == JsonToken.BEGIN_ARRAY) {
      result = TOKEN_TYPE_START_ARRAY;
    } else if (tokenType == JsonToken.END_OBJECT) {
      result = TOKEN_TYPE_END_OBJECT;
    } else if (tokenType == JsonToken.END_ARRAY) {
      result = TOKEN_TYPE_END_ARRAY;
    } else if (tokenType == JsonToken.NAME) {
      result = TOKEN_TYPE_FIELD_NAME;
    } else if (tokenType == JsonToken.NULL) {
      result = TOKEN_TYPE_NULL;
    } else if (tokenType == JsonToken.END_DOCUMENT) {
      result = TOKEN_TYPE_END_DOCUMENT;
    }
    return result;
  }

  @Override
  public String getAttributeName() throws MarshallingException {
    try {
      return jr.nextName();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  @Override
  public String getAttributeContent(Class<?> type) throws MarshallingException {
    String result;
    JsonToken tokenType = getJsonToken();
    if (tokenType==JsonToken.NULL) {
      try {
        jr.nextNull();
        return null;
      } catch (Exception e) {
        throw new MarshallingException(e);
      }
    }
    try {
      if(type == String.class) {
        result = jr.nextString();
      }
      else if (type == Boolean.class || type == boolean.class) {
        if (tokenType==JsonToken.BOOLEAN) {
          result = String.valueOf(jr.nextBoolean());
          return result;
        }
        if (tokenType==JsonToken.NUMBER) {
          result = String.valueOf(jr.nextInt());
          return result;
        }
        result = jr.nextString();
      } else if(type == int.class) {
        result = String.valueOf(jr.nextInt());
      } else if(type == long.class) {
        result = String.valueOf(jr.nextLong());
      } else if(type == double.class) {
        result = String.valueOf(jr.nextDouble());
      } else {
        result = jr.nextString();
      }
    } catch (Exception e) {
      throw new MarshallingException(e);
    }

    return result;
  }


  @Override
  public String getAttributeContent() throws MarshallingException {
    try {
      JsonToken tokenType = getJsonToken();

      switch (tokenType) {

        case BOOLEAN:
          return String.valueOf(jr.nextBoolean());

        case NULL:
          jr.nextNull();
          return null;

        case NUMBER:
          return jr.nextString();

        case STRING:
          return jr.nextString();

        case NAME:
        case BEGIN_ARRAY:
        case END_ARRAY:
        case BEGIN_OBJECT:
        case END_DOCUMENT:
        case END_OBJECT:
        default:
          throw new MarshallingException("Unexpected token type" + tokenType);
      }
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

  private JsonToken getJsonToken() throws MarshallingException {
    JsonToken tokenType;
    try {
      tokenType = jr.peek();
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
    return tokenType;
  }

}
