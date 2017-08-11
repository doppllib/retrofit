package retrofit2.ios;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by kgalligan on 12/21/16.
 */

public class RealInterceptorChain implements Interceptor.Chain {
    private final List<Interceptor> interceptors;
    private final HttpURLConnection httpURLConnection;
    private final int               index;
    private final Request           request;
    private       int               calls;

    public RealInterceptorChain(List<Interceptor> interceptors, HttpURLConnection httpURLConnection, int index, Request request) {
        this.interceptors = interceptors;
        this.httpURLConnection = httpURLConnection;
        this.index = index;
        this.request = request;
    }

    @Override public HttpURLConnection connection() {
        return httpURLConnection;
    }

    @Override public Request request() {
        return request;
    }

    @Override public Response proceed(Request request) throws IOException
    {
        return proceed(request, httpURLConnection);
    }

    public Response proceed(Request request, HttpURLConnection httpURLConnection) throws IOException {
        if (index >= interceptors.size()) throw new AssertionError();

        calls++;

        /*// If we already have a stream, confirm that the incoming request will use it.
        if (this.httpStream != null && !sameConnection(request.url())) {
            throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
                    + " must retain the same host and port");
        }

        // If we already have a stream, confirm that this is the only call to chain.proceed().
        if (this.httpStream != null && calls > 1) {
            throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
                    + " must call proceed() exactly once");
        }*/

        // Call the next interceptor in the chain.
        RealInterceptorChain next = new RealInterceptorChain(
                interceptors, httpURLConnection, index + 1, request);
        Interceptor interceptor = interceptors.get(index);
        Response response = interceptor.intercept(next);

        /*// Confirm that the next interceptor made its required call to chain.proceed().
        if (httpStream != null && index + 1 < interceptors.size() && next.calls != 1) {
            throw new IllegalStateException("network interceptor " + interceptor
                    + " must call proceed() exactly once");
        }*/

        // Confirm that the intercepted response isn't null.
        if (response == null) {
            throw new NullPointerException("interceptor " + interceptor + " returned null");
        }

        return response;
    }

    /*private boolean sameConnection(HttpUrl url) {
        return url.host().equals(connection.route().address().url().host())
                && url.port() == connection.route().address().url().port();
    }*/
}
