package retrofit2.urlsession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;
import okhttp3.internal.Version;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;
import okio.Okio;
import okio.Source;

/*-[
#include "java/lang/Double.h"
]-*/

/**
 * Created by kgalligan on 8/12/17.
 */

public class UrlSessionCall implements okhttp3.Call
{
    private final Request originalRequest;
    private final UrlSessionCallFactory client;
    private List<HeaderEntry> headers = new ArrayList<HeaderEntry>();
    // Guarded by this.
    private boolean executed;

    private final Object getResponseLock = new Object();
    private int responseCode;
    private Response response;
    private CountDownLatch latch = new CountDownLatch(1);

    public UrlSessionCall(UrlSessionCallFactory client, Request originalRequest)
    {
        this.originalRequest = originalRequest;
        this.client = client;
    }

    public void constructResponse(int responseCode, byte[] body) throws IOException {

        Protocol protocol = Protocol.HTTP_1_1;

        Headers.Builder builder = new Headers.Builder();
        for(HeaderEntry header : headers)
        {
            builder.add(header.getKey(), header.getValue());
        }
        Headers headers = builder.build();

        Response.Builder responseBuilder = new Response.Builder()
                .request(originalRequest)
                .protocol(protocol)
                .code(responseCode)
                //TODO: Figure out how to parse message
                .message(findStandardResponseMessageForCode(responseCode))
                .headers(headers);

        Buffer buffer = new Buffer();
        buffer.write(body);

        RealResponseBody responseBody = new RealResponseBody(headers,
                Okio.buffer((Source) buffer));

        responseBuilder.body(responseBody);

        response = responseBuilder.build();

        latch.countDown();
    }

    @Override
    public Request request()
    {
        return originalRequest;
    }

    public Request prepRequestHeaders(Request userRequest) throws IOException
    {
        Request.Builder requestBuilder = userRequest.newBuilder();

        RequestBody body = userRequest.body();
        if (body != null) {
            MediaType contentType = body.contentType();
            if (contentType != null) {
                requestBuilder.header("Content-Type", contentType.toString());
            }

            long contentLength = body.contentLength();
            if (contentLength != -1) {
                requestBuilder.header("Content-Length", Long.toString(contentLength));
                requestBuilder.removeHeader("Transfer-Encoding");
            } else {
                requestBuilder.header("Transfer-Encoding", "chunked");
                requestBuilder.removeHeader("Content-Length");
            }
        }

        if (userRequest.header("User-Agent") == null) {
            requestBuilder.header("User-Agent", "doppl-"+ Version.userAgent());
        }

        return requestBuilder.build();
    }

    @Override
    public Response execute() throws IOException
    {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
        try {
//            client.dispatcher().executed(this);

            Request request = prepRequestHeaders(originalRequest);
            Headers headers = request.headers();

            Iterator<String> iterator = headers.names().iterator();
            while(iterator.hasNext())
            {
                String key = iterator.next();
                addHeader(key, headers.get(key));
            }

            byte[] body = null;

            if(request.body() != null)
            {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                buffer.flush();

                body = buffer.readByteArray();
                String bodyString = new String(body);
                System.out.println("BODY_STRING: |"+ bodyString +"|");
            }

            makeRequest(request.url().url().toExternalForm(), request.method(),
                    body);

            try
            {
                latch.await();
            }
            catch(InterruptedException e)
            {
                throw new RuntimeException("What?", e);
            }

            if (response == null) throw new IOException("Canceled");
            return response;
        } finally {
//            client.dispatcher().finished(this);
        }
    }

    /*private Response getResponseWithInterceptorChain() throws IOException {
        // Build a full stack of interceptors.
        try
        {
            List<retrofit2.ios.Interceptor> interceptors = new ArrayList<>();
//            interceptors.addAll(client.interceptors());
//            interceptors.add(retryAndFollowUpInterceptor);
            interceptors.add(new BridgeInterceptor(client.cookieJar()));
//            interceptors.add(new CacheInterceptor(client.internalCache()));
//            interceptors.add(new ConnectInterceptor(client));
            interceptors.addAll(client.networkInterceptors());
//            interceptors.add(new CallServerInterceptor(false));

            retrofit2.ios.Interceptor.Chain chain = new RealInterceptorChain(
                    interceptors, null, 0, originalRequest);
            return chain.proceed(originalRequest);
        }
        catch(Throwable e)
        {
            if(e instanceof IOException)
                throw  (IOException)e;

            e.printStackTrace();

            if(e instanceof RuntimeException)
                throw (RuntimeException)e;

            throw new RuntimeException(e);
        }
    }*/

    private native void makeRequest(String urlString, String method, byte[] body)throws IOException /*-[
    NSMutableURLRequest *request =
          [NSMutableURLRequest requestWithURL:[NSURL URLWithString:urlString]];
      request.cachePolicy = NSURLRequestUseProtocolCachePolicy;
      int readTimeout = 10000;
      request.timeoutInterval = readTimeout > 0 ? (readTimeout / 1000.0) : JavaLangDouble_MAX_VALUE;
      int n = [self->headers_ size];
      for (int i = 0; i < n; i++) {
        Retrofit2UrlsessionUrlSessionCall_HeaderEntry *entry = [self->headers_ getWithInt:i];
        if (entry->key_) {
          [request setValue:[entry getValue] forHTTPHeaderField:entry->key_];
        }
      }

      if (body != nil) {
          request.HTTPBody = [body toNSData];
      }

      request.HTTPMethod = method;

      NSURLSessionConfiguration *sessionConfiguration =
          [NSURLSessionConfiguration defaultSessionConfiguration];
      NSURLSession *session =
          [NSURLSession sessionWithConfiguration:sessionConfiguration
                                        delegate:(id<NSURLSessionDataDelegate>)self
                                   delegateQueue:nil];
      NSURLSessionTask *task = [session dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable urlResponse, NSError * _Nullable error) {

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

            if (error!=nil)
            {
                //completionBlock(nil,error,0);

            }
            else
            {
                [self constructResponseWithInt:responseCode withByteArray:[IOSByteArray arrayWithNSData:data]];
            }

//            [task suspend];

        }];
      [task resume];
//      JreStrongAssign(&self->nativeDataTask_, task);
//      [session finishTasksAndInvalidate];

      ]-*/
    ;

    private void addHeader(String k, String v) {
        headers.add(new HeaderEntry(k, v));
    }

    @Override
    public void enqueue(Callback responseCallback)
    {
        throw new UnsupportedOperationException("enqueue not implemented yet");
    }

    @Override
    public void cancel()
    {

    }

    @Override
    public boolean isExecuted()
    {
        return false;
    }

    @Override
    public boolean isCanceled()
    {
        return false;
    }

    private static class HeaderEntry implements Map.Entry<String, String> {
        private final String key;
        private final String value;

        HeaderEntry(String k, String v) {
            this.key = k;
            this.value = v;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String object) {
            throw new AssertionError("mutable method called on immutable class");
        }
    }

    private native String findStandardResponseMessageForCode(int code)/*-[
        return [NSHTTPURLResponse localizedStringForStatusCode:code];
    ]-*/;


}
