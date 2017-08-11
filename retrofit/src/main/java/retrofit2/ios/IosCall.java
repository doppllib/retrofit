package retrofit2.ios;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.*;
import okhttp3.internal.NamedRunnable;
import okhttp3.internal.platform.Platform;


import static okhttp3.internal.platform.Platform.INFO;

/**
 * Created by kgalligan on 12/20/16.
 */

public class IosCall implements okhttp3.Call
{
    private final IosCallFactory                client;

    // Guarded by this.
    private boolean executed;

    private final IosRetryAndFollowUpInterceptor retryAndFollowUpInterceptor;

    /** The application's original request unadulterated by redirects or auth headers. */
    Request originalRequest;

    protected   IosCall(IosCallFactory client, Request originalRequest) {
        this.client = client;
        this.originalRequest = originalRequest;
        this.retryAndFollowUpInterceptor = new IosRetryAndFollowUpInterceptor(client);
    }

    @Override public Request request() {
        return originalRequest;
    }

    @Override public okhttp3.Response execute() throws IOException
    {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
        try {
            client.dispatcher().executed(this);

            Response result = getResponseWithInterceptorChain();
            if (result == null) throw new IOException("Canceled");
            return result;
        } finally {
            client.dispatcher().finished(this);
        }
    }

    @Override public void enqueue(okhttp3.Callback responseCallback) {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
        client.dispatcher().enqueue(new IosCall.AsyncCall(responseCallback));
    }

    @Override public void cancel() {
        retryAndFollowUpInterceptor.cancel();
    }

    @Override public synchronized boolean isExecuted() {
        return executed;
    }

    @Override public boolean isCanceled() {
        return retryAndFollowUpInterceptor.isCanceled();
    }

    final class AsyncCall extends NamedRunnable
    {
        private final okhttp3.Callback responseCallback;

        private AsyncCall(okhttp3.Callback responseCallback) {
            super("OkHttp %s", redactedUrl().toString());
            this.responseCallback = responseCallback;
        }

        String host() {
            return originalRequest.url().host();
        }

        Request request() {
            return originalRequest;
        }

        IosCall get() {
            return IosCall.this;
        }

        @Override protected void execute() {
            boolean signalledCallback = false;
            try {
                Response response = getResponseWithInterceptorChain();
                if (retryAndFollowUpInterceptor.isCanceled()) {
                    signalledCallback = true;
                    responseCallback.onFailure(IosCall.this, new IOException("Canceled"));
                } else {
                    signalledCallback = true;
                    responseCallback.onResponse(IosCall.this, response);
                }
            } catch (IOException e) {
                if (signalledCallback) {
                    // Do not signal the callback twice!
                    Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
                } else {
                    responseCallback.onFailure(IosCall.this, e);
                }
            } catch(Throwable throwable){
                Platform.get().log(INFO, "Unknown exception " + toLoggableString(), throwable);
                responseCallback.onFailure(IosCall.this, new IOException(throwable));
            }
            finally {
                client.dispatcher().finished(this);
            }
        }
    }

    /**
     * Returns a string that describes this call. Doesn't include a full URL as that might contain
     * sensitive information.
     */
    private String toLoggableString() {
        return "call to " + redactedUrl();
    }

    HttpUrl redactedUrl() {
        return originalRequest.url().resolve("/...");
    }

    private Response getResponseWithInterceptorChain() throws IOException {
        // Build a full stack of interceptors.
        try
        {
            List<Interceptor> interceptors = new ArrayList<>();
            interceptors.addAll(client.interceptors());
            interceptors.add(retryAndFollowUpInterceptor);
            interceptors.add(new BridgeInterceptor(client.cookieJar()));
            interceptors.add(new CacheInterceptor(client.internalCache()));
            interceptors.add(new ConnectInterceptor(client));
            interceptors.addAll(client.networkInterceptors());
            interceptors.add(new CallServerInterceptor(false));

            Interceptor.Chain chain = new RealInterceptorChain(
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
    }
}
