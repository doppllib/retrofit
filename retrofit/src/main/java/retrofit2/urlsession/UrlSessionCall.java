package retrofit2.urlsession;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Version;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;
import okio.Okio;
import okio.Source;

/*-[
#include "java/lang/Double.h"
#include "com/google/j2objc/net/NSErrorException.h"
#include "java/lang/Double.h"
#include "java/net/ConnectException.h"
#include "java/net/MalformedURLException.h"
#include "java/net/UnknownHostException.h"
#include "java/net/SocketTimeoutException.h"
]-*/

/**
 * Created by kgalligan on 8/12/17.
 */

public class UrlSessionCall implements okhttp3.Call
{
    private final Request             originalRequest;
    private final List<HeaderEntry> headers = new ArrayList<HeaderEntry>();

    private volatile boolean executed;
    private volatile boolean canceled;

    private final AtomicReference<Response>    response = new AtomicReference<>();
    private final AtomicReference<IOException> responseError = new AtomicReference<>();
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<okhttp3.Callback> responseCallbackRef = new AtomicReference<>();

    private final Object urlSessionReference;
    private Object urlSessionTask;

    //Keep as fields to make variable retention easier
    private String makeRequestUrl;
    private String makeRequestMethod;
    private byte[] makeRequestBody;

    public UrlSessionCall(Object urlSessionReference, Request originalRequest)
    {
        this.urlSessionReference = urlSessionReference;
        this.originalRequest = originalRequest;
    }

    @Override
    public Request request()
    {
        return originalRequest;
    }

    @Override
    public Response execute() throws IOException
    {
        synchronized(this)
        {
            if(executed)
            {
                throw new IllegalStateException("Already Executed");
            }
            executed = true;
        }

        runRequestCall();

        try
        {
            latch.await();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException("What?", e);
        }

        if(responseError.get() != null)
        {
            throw responseError.get();
        }

        if(response.get() == null)
        {
            throw new IOException("Canceled");
        }

        return response.get();
    }

    private void runRequestCall() throws IOException
    {
        if (canceled) {
            throw new IOException("Canceled");
        }
        Request request = prepRequestHeaders(originalRequest);
        Headers headers = request.headers();

        for(String key : headers.names())
        {
            addHeader(key, headers.get(key));
        }

        makeRequestBody = null;

        if(request.body() != null)
        {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            buffer.flush();

            makeRequestBody = buffer.readByteArray();
        }

        makeRequestUrl = request.url().url().toExternalForm();
        makeRequestMethod = request.method();
        makeRequest();
    }

    private Request prepRequestHeaders(Request userRequest) throws IOException
    {
        Request.Builder requestBuilder = userRequest.newBuilder();

        RequestBody body = userRequest.body();
        if(body != null)
        {
            MediaType contentType = body.contentType();
            if(contentType != null)
            {
                requestBuilder.header("Content-Type", contentType.toString());
            }

            long contentLength = body.contentLength();
            if(contentLength != - 1)
            {
                requestBuilder.header("Content-Length", Long.toString(contentLength));
                requestBuilder.removeHeader("Transfer-Encoding");
            }
            else
            {
                requestBuilder.header("Transfer-Encoding", "chunked");
                requestBuilder.removeHeader("Content-Length");
            }
        }

        if(userRequest.header("User-Agent") == null)
        {
            requestBuilder.header("User-Agent", "doppl-" + Version.userAgent());
        }

        return requestBuilder.build();
    }

    private synchronized void updateUrlSessionTask(Object task)
    {
        urlSessionTask = task;
    }



    private native void makeRequest() throws IOException /*-[
    NSMutableURLRequest *request =
          [NSMutableURLRequest requestWithURL:[NSURL URLWithString:self->makeRequestUrl_]];
      request.cachePolicy = NSURLRequestUseProtocolCachePolicy;

      int n = [self->headers_ size];
      for (int i = 0; i < n; i++) {
        Retrofit2UrlsessionUrlSessionCall_HeaderEntry *entry = [self->headers_ getWithInt:i];
        if (entry->key_) {
          [request setValue:[entry getValue] forHTTPHeaderField:entry->key_];
        }
      }

      if (self->makeRequestBody_ != nil) {
          request.HTTPBody = [self->makeRequestBody_ toNSData];
      }

      request.HTTPMethod = self->makeRequestMethod_;

      NSURLSessionTask *task = [self->urlSessionReference_ dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable urlResponse,
      NSError * _Nullable error) {

            [self updateUrlSessionTaskWithId:nil];

            if (error) {
                JavaIoIOException *responseException = nil;
                  NSString *url = @"";//self->makeRequestUrl_;  // Use original URL in any error text.
                  if ([[error domain] isEqualToString:@"NSURLErrorDomain"]) {
                    NSInteger theCode = [error code];
        switch (theCode) {
                      case NSURLErrorBadURL:
                        responseException = create_JavaNetMalformedURLException_initWithNSString_(url);
                        break;
                      case NSURLErrorCannotConnectToHost:
                        responseException =
                            create_JavaNetConnectException_initWithNSString_([error localizedDescription]);
                        break;
                      case NSURLErrorSecureConnectionFailed:
                        responseException = [Retrofit2UrlsessionUrlSessionCall secureConnectionExceptionWithNSString:[error localizedDescription]];
                        break;
                      case NSURLErrorCannotFindHost:
                        responseException = create_JavaNetUnknownHostException_initWithNSString_(url);
                        break;
                      case NSURLErrorTimedOut:
                        responseException = create_JavaNetSocketTimeoutException_initWithNSString_(url);
                        break;
                      case NSURLErrorCancelled:
                        responseException = create_JavaNetSocketTimeoutException_initWithNSString_(@"Canceled");
                        break;
                      case NSURLErrorNetworkConnectionLost:
                        responseException = create_JavaNetSocketTimeoutException_initWithNSString_(@"unexpected end of stream");
                        break;
                    }
                  }
                  if (!responseException) {
                    responseException = create_JavaIoIOException_initWithNSString_([error localizedDescription]);
                  }
//                      ComGoogleJ2objcNetNSErrorException *cause =
//                          create_ComGoogleJ2objcNetNSErrorException_initWithId_(error);
//                      [responseException initWithJavaLangThrowable:cause];
                  [self sendErrorWithJavaIoIOException:responseException];
            }
            else {

                if (urlResponse && ![urlResponse isKindOfClass:[NSHTTPURLResponse class]]) {
                  @throw AUTORELEASE(([[JavaLangAssertionError alloc]
                                       initWithId:[NSString stringWithFormat:@"Unknown class %@",
                                           NSStringFromClass([urlResponse class])]]));
                }
                NSHTTPURLResponse *response = (NSHTTPURLResponse *) urlResponse;
                int responseCode = (int) response.statusCode;

                // Clear request headers to make room for the response headers.
                [self->headers_ clear];

                // Copy remaining response headers.
                [response.allHeaderFields enumerateKeysAndObjectsUsingBlock:
                    ^(id key, id value, BOOL *stop) {
                  [self addHeaderWithNSString:key withNSString:value];
                }];

                    [self constructResponseWithInt:responseCode withByteArray:[IOSByteArray arrayWithNSData:data]];
            }

        }];

        [self updateUrlSessionTaskWithId:task];

        [task resume];

      ]-*/;

    /**
     * Build exception for ssl issues
     * @param description
     * @return
     */
    private static IOException secureConnectionException(String description)
    {
        try
        {
            Class<?> sslExceptionClass = Class.forName("javax.net.ssl.SSLException");
            Constructor<?> constructor = sslExceptionClass.getConstructor(String.class);
            return (IOException) constructor.newInstance(description);
        }
        catch(ClassNotFoundException e)
        {
            return new IOException(description);
        }
        catch(Exception e)
        {
            throw new AssertionError("unexpected exception", e);
        }
    }

    public void sendError(IOException ex)
    {
        Callback responseCallback = responseCallbackRef.get();
        if(responseCallback != null)
        {
            responseCallback.onFailure(this, ex);
        }
        else
        {
            responseError.set(ex);
            latch.countDown();
        }
    }

    public void constructResponse(int responseCode, byte[] body) throws IOException
    {
        Protocol protocol = Protocol.HTTP_1_1;

        Headers.Builder builder = new Headers.Builder();
        for(HeaderEntry header : headers)
        {
            builder.add(header.getKey(), header.getValue());
        }
        Headers headers = builder.build();

        Response.Builder responseBuilder = new Response.Builder().request(originalRequest)
                .protocol(protocol)
                .code(responseCode)
                //TODO: Figure out how to parse message
                .message(findStandardResponseMessageForCode(responseCode))
                .headers(headers);

        Buffer buffer = new Buffer();
        buffer.write(body);

        RealResponseBody responseBody = new RealResponseBody(headers, Okio.buffer((Source) buffer));

        responseBuilder.body(responseBody);

        Callback responseCallback = responseCallbackRef.get();
        if(responseCallback != null)
        {
            responseCallback.onResponse(this, responseBuilder.build());
        }
        else
        {
            response.set(responseBuilder.build());
            latch.countDown();
        }
    }

    private void addHeader(String k, String v)
    {
        headers.add(new HeaderEntry(k, v));
    }

    @Override
    public void enqueue(Callback responseCallback)
    {
        this.responseCallbackRef.set(responseCallback);

        try
        {
            runRequestCall();
        }
        catch(IOException e)
        {
            responseCallback.onFailure(this, e);
        }
        catch(Throwable e)
        {
            responseCallback.onFailure(this, new IOException(e));
        }
    }

    @Override
    public synchronized void cancel()
    {
        canceled = true;
        if(urlSessionTask != null)
        {
            taskCancel(urlSessionTask);
        }
    }

    private native void taskCancel(Object task)/*-[
        [((NSURLSessionDataTask *)task) cancel];
        [self updateUrlSessionTaskWithId:nil];
    ]-*/;

    @Override
    public boolean isExecuted()
    {
        return executed;
    }

    @Override
    public boolean isCanceled()
    {
        return canceled;
    }

    private static class HeaderEntry implements Map.Entry<String, String>
    {
        private final String key;
        private final String value;

        HeaderEntry(String k, String v)
        {
            this.key = k;
            this.value = v;
        }

        @Override
        public String getKey()
        {
            return key;
        }

        @Override
        public String getValue()
        {
            return value;
        }

        @Override
        public String setValue(String object)
        {
            throw new AssertionError("mutable method called on immutable class");
        }
    }

    private native String findStandardResponseMessageForCode(int code)/*-[
        return [NSHTTPURLResponse localizedStringForStatusCode:code];
    ]-*/;
}
