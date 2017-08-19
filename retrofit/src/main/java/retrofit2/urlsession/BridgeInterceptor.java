package retrofit2.urlsession;
import java.io.IOException;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Version;

import static okhttp3.internal.Util.hostHeader;

/**
 * Created by kgalligan on 8/19/17.
 */

public class BridgeInterceptor implements retrofit2.ios.Interceptor
{
    @Override public Response intercept(Chain chain) throws IOException
    {
        Request userRequest = chain.request();
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

        if (userRequest.header("Host") == null) {
            requestBuilder.header("Host", hostHeader(userRequest.url(), false));
        }

        if (userRequest.header("Connection") == null) {
            requestBuilder.header("Connection", "Keep-Alive");
        }

        if (userRequest.header("Accept-Encoding") == null) {
            requestBuilder.header("Accept-Encoding", "gzip");
        }

        if (userRequest.header("User-Agent") == null) {
            requestBuilder.header("User-Agent", Version.userAgent());
        }

        Response networkResponse = chain.proceed(requestBuilder.build());

        Response.Builder responseBuilder = networkResponse.newBuilder()
                .request(userRequest);

        return responseBuilder.build();
    }
}
