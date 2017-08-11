package retrofit2.ios;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.RouteException;
import okhttp3.internal.http.UnrepeatableRequestBody;


import static okhttp3.internal.Util.closeQuietly;

/**
 * Created by kgalligan on 12/20/16.
 */

public class IosRetryAndFollowUpInterceptor implements Interceptor
{
    /**
     * How many redirects and auth challenges should we attempt? Chrome follows 21 redirects; Firefox,
     * curl, and wget follow 20; Safari follows 16; and HTTP/1.0 recommends 5.
     */
    private static final int MAX_FOLLOW_UPS = 20;

    private final    IosCallFactory     client;
    private volatile boolean          canceled;
    private HttpURLConnection connection;

    public IosRetryAndFollowUpInterceptor(IosCallFactory client) {
        this.client = client;
    }

    /**
     * Immediately closes the socket connection if it's currently held. Use this to interrupt an
     * in-flight request from any thread. It's the caller's responsibility to close the request body
     * and response body streams; otherwise resources may be leaked.
     *
     * <p>This method is safe to be called concurrently, but provides limited guarantees. If a
     * transport layer connection has been established (such as a HTTP/2 stream) that is terminated.
     * Otherwise if a socket connection is being established, that is terminated.
     */
    public void cancel() {
        canceled = true;
        HttpURLConnection streamAllocation = this.connection;
        if (streamAllocation != null) streamAllocation.disconnect();
    }

    public boolean isCanceled() {
        return canceled;
    }

    public IosCallFactory client() {
        return client;
    }

    @Override public Response intercept(Chain chain) throws IOException
    {
        Request request = chain.request();

        int followUpCount = 0;
        Response priorResponse = null;
        while (true) {
            if (canceled) {
//                connection.disconnect();
                throw new IOException("Canceled");
            }

            Response response = null;
            boolean releaseConnection = true;
            try {
                response = ((RealInterceptorChain) chain).proceed(request, connection);
                releaseConnection = false;
            } catch (RouteException e) {
                // The attempt to connect via a route failed. The request will not have been sent.
                if (!recover(e.getLastConnectException(), true, request)) throw e.getLastConnectException();
                releaseConnection = false;
                continue;
            } catch (IOException e) {
                // An attempt to communicate with a server failed. The request may have been sent.
                if (!recover(e, false, request)) throw e;
                releaseConnection = false;
                continue;
            } finally {
                // We're throwing an unchecked exception. Release any resources.
                if (releaseConnection) {
//                    streamAllocation.streamFailed(null);
//                    connection.disconnect();
                }
            }

            // Attach the prior response if it exists. Such responses never have a body.
            if (priorResponse != null) {
                response = response.newBuilder()
                        .priorResponse(priorResponse.newBuilder()
                                .body(null)
                                .build())
                        .build();
            }

            Request followUp = null;//followUpRequest(response);

            if (followUp == null) {
//                connection.disconnect();
                return response;
            }

            closeQuietly(response.body());

            if (++followUpCount > MAX_FOLLOW_UPS) {
//                connection.disconnect();
                throw new ProtocolException("Too many follow-up requests: " + followUpCount);
            }

            if (followUp.body() instanceof UnrepeatableRequestBody) {
                throw new HttpRetryException("Cannot retry streamed HTTP body", response.code());
            }

            if (!sameConnection(response, followUp.url())) {
//                connection.disconnect();
//                connection = (HttpURLConnection)followUp.url().url().openConnection();
            }
            //TODO: Figure out if there's an equivalent
            /* else if (streamAllocation.stream() != null) {
                throw new IllegalStateException("Closing the body of " + response
                        + " didn't close its backing stream. Bad interceptor?");
            }*/

            request = followUp;
            priorResponse = response;
        }
    }

    //TODO: figure out equivalent
    /**
     * Report and attempt to recover from a failure to communicate with a server. Returns true if
     * {@code e} is recoverable, or false if the failure is permanent. Requests with a body can only
     * be recovered if the body is buffered.
     */
    private boolean recover(IOException e, boolean routeException, Request userRequest) {
//        streamAllocation.streamFailed(e);

        // The application layer has forbidden retries.
        if (!client.retryOnConnectionFailure()) return false;

        // We can't send the request body again.
        if (!routeException && userRequest.body() instanceof UnrepeatableRequestBody) return false;

        // This exception is fatal.
        if (!isRecoverable(e, routeException)) return false;

        // No more routes to attempt.
        //TODO: figure out equivalent
//        if (!streamAllocation.hasMoreRoutes()) return false;

        // For failure recovery, use the same route selector with a new connection.
        return false;
    }

    //TODO: We need to figure out appropriate error handling here
    private boolean isRecoverable(IOException e, boolean routeException) {
        // If there was a protocol problem, don't recover.
        if (e instanceof ProtocolException) {
            return false;
        }

        // If there was an interruption don't recover, but if there was a timeout connecting to a route
        // we should try the next route (if there is one).
        if (e instanceof InterruptedIOException) {
            return e instanceof SocketTimeoutException && routeException;
        }

        // Look for known client-side or negotiation errors that are unlikely to be fixed by trying
        // again with a different route.
        if (e instanceof SSLHandshakeException) {
            // If the problem was a CertificateException from the X509TrustManager,
            // do not retry.
            if (e.getCause() instanceof CertificateException) {
                return false;
            }
        }
        if (e instanceof SSLPeerUnverifiedException) {
            // e.g. a certificate pinning error.
            return false;
        }

        // An example of one we might want to retry with a different route is a problem connecting to a
        // proxy and would manifest as a standard IOException. Unless it is one we know we should not
        // retry, we return true and try a new route.
        return true;
    }

    //TODO
    /**
     * Figures out the HTTP request to make in response to receiving {@code userResponse}. This will
     * either add authentication headers, follow redirects or handle a client request timeout. If a
     * follow-up is either unnecessary or not applicable, this returns null.
     */
    /*private Request followUpRequest(Response userResponse) throws IOException {
        if (userResponse == null) throw new IllegalStateException();
        Connection connection = streamAllocation.connection();
        Route route = connection != null
                ? connection.route()
                : null;
        int responseCode = userResponse.code();

        final String method = userResponse.request().method();
        switch (responseCode) {
            case HTTP_PROXY_AUTH:
                Proxy selectedProxy = route != null
                        ? route.proxy()
                        : client.proxy();
                if (selectedProxy.type() != Proxy.Type.HTTP) {
                    throw new ProtocolException("Received HTTP_PROXY_AUTH (407) code while not using proxy");
                }
                return client.proxyAuthenticator().authenticate(route, userResponse);

            case HTTP_UNAUTHORIZED:
                return client.authenticator().authenticate(route, userResponse);

            case HTTP_PERM_REDIRECT:
            case HTTP_TEMP_REDIRECT:
                // "If the 307 or 308 status code is received in response to a request other than GET
                // or HEAD, the user agent MUST NOT automatically redirect the request"
                if (!method.equals("GET") && !method.equals("HEAD")) {
                    return null;
                }
                // fall-through
            case HTTP_MULT_CHOICE:
            case HTTP_MOVED_PERM:
            case HTTP_MOVED_TEMP:
            case HTTP_SEE_OTHER:
                // Does the client allow redirects?
                if (!client.followRedirects()) return null;

                String location = userResponse.header("Location");
                if (location == null) return null;
                HttpUrl url = userResponse.request().url().resolve(location);

                // Don't follow redirects to unsupported protocols.
                if (url == null) return null;

                // If configured, don't follow redirects between SSL and non-SSL.
                boolean sameScheme = url.scheme().equals(userResponse.request().url().scheme());
                if (!sameScheme && !client.followSslRedirects()) return null;

                // Redirects don't include a request body.
                Request.Builder requestBuilder = userResponse.request().newBuilder();
                if (HttpMethod.permitsRequestBody(method)) {
                    if (HttpMethod.redirectsToGet(method)) {
                        requestBuilder.method("GET", null);
                    } else {
                        requestBuilder.method(method, null);
                    }
                    requestBuilder.removeHeader("Transfer-Encoding");
                    requestBuilder.removeHeader("Content-Length");
                    requestBuilder.removeHeader("Content-Type");
                }

                // When redirecting across hosts, drop all authentication headers. This
                // is potentially annoying to the application layer since they have no
                // way to retain them.
                if (!sameConnection(userResponse, url)) {
                    requestBuilder.removeHeader("Authorization");
                }

                return requestBuilder.url(url).build();

            case HTTP_CLIENT_TIMEOUT:
                // 408's are rare in practice, but some servers like HAProxy use this response code. The
                // spec says that we may repeat the request without modifications. Modern browsers also
                // repeat the request (even non-idempotent ones.)
                if (userResponse.request().body() instanceof UnrepeatableRequestBody) {
                    return null;
                }

                return userResponse.request();

            default:
                return null;
        }
    }*/

    /**
     * Returns true if an HTTP request for {@code followUp} can reuse the connection used by this
     * engine.
     */
    private boolean sameConnection(Response response, HttpUrl followUp) {
        HttpUrl url = response.request().url();
        return url.host().equals(followUp.host())
                && url.port() == followUp.port()
                && url.scheme().equals(followUp.scheme());
    }
}