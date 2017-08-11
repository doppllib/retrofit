/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.ios;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpMethod;
import okhttp3.internal.http.RealResponseBody;
import okhttp3.internal.http.StatusLine;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;


import static okhttp3.internal.http.StatusLine.HTTP_CONTINUE;

/** This is the last interceptor in the chain. It makes a network call to the server. */
public final class CallServerInterceptor implements Interceptor
{
  private final boolean forWebSocket;

  public CallServerInterceptor(boolean forWebSocket) {
    this.forWebSocket = forWebSocket;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    HttpURLConnection connection = ((RealInterceptorChain) chain).connection();
//    StreamAllocation streamAllocation = ((RealInterceptorChain) chain).streamAllocation();
    Request request = chain.request();

    long sentRequestMillis = System.currentTimeMillis();
//    httpStream.writeRequestHeaders(request);

    connection.setRequestMethod(request.method());

    Headers headers = request.headers();
    for (int i = 0, size = headers.size(); i < size; i++) {
      connection.addRequestProperty(headers.name(i), headers.value(i));
    }

    if (HttpMethod.permitsRequestBody(request.method()) && request.body() != null) {
      connection.setDoOutput(true);
      OutputStream outputStream = connection.getOutputStream();
      Sink requestBodyOut = Okio.sink(outputStream);
      BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);
      request.body().writeTo(bufferedRequestBody);
      bufferedRequestBody.close();
      outputStream.close();
    }

    Response response = readResponseHeaders(connection)
        .request(request)
        .sentRequestAtMillis(sentRequestMillis)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build();

    if (!forWebSocket || response.code() != 101) {
      response = response.newBuilder()
          .body(openResponseBody(response, connection))
          .build();
    }

    if ("close".equalsIgnoreCase(response.request().header("Connection"))
        || "close".equalsIgnoreCase(response.header("Connection"))) {
      connection.disconnect();
    }

    int code = response.code();
    if ((code == 204 || code == 205) && response.body().contentLength() > 0) {
      throw new ProtocolException(
          "HTTP " + code + " had non-zero Content-Length: " + response.body().contentLength());
    }

    return response;
  }

  public ResponseBody openResponseBody(Response response, HttpURLConnection connection) throws IOException
  {
    InputStream inputStream;
    if(response.code() >= 400)
    {
      inputStream = connection.getErrorStream();
    }
    else
    {
      inputStream = connection.getInputStream();
    }
    return new RealResponseBody(response.headers(), Okio.buffer(Okio.source(inputStream)));
  }

  /** Parses bytes of a response header from an HTTP transport. */
  public Response.Builder readResponseHeaders(HttpURLConnection connection) throws IOException {
    try {
      while (true) {

        int responseCode = connection.getResponseCode();

        Protocol protocol = null;

        try
        {
          StatusLine statusLine = StatusLine.parse(connection.getHeaderField(0));
          protocol = statusLine.protocol;
        }
        catch(IOException e)
        {
          //Protocol fail
        }

        if(protocol == null)
          protocol = Protocol.HTTP_1_1;

        Response.Builder responseBuilder = new Response.Builder()
                .protocol(protocol)
                .code(responseCode)
                .message(connection.getResponseMessage())
                .headers(readHeaders(connection));

        if (responseCode != HTTP_CONTINUE) {
          return responseBuilder;
        }
      }
    } catch (EOFException e) {
      // Provide more context if the server ends the stream before sending a response.
      IOException exception = new IOException("unexpected end of stream on " + connection);
      exception.initCause(e);
      throw exception;
    }
  }

  private Headers readHeaders(HttpURLConnection connection)
  {
    Headers.Builder headers = new Headers.Builder();

    for (Map.Entry<String, List<String>> field : connection.getHeaderFields().entrySet()) {
      String name = field.getKey();
      if(name != null)
      {
        for(String value : field.getValue())
        {
          headers.add(name, value);
        }
      }
    }
    return headers.build();
  }
}
