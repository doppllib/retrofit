package retrofit2.clientfactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by kgalligan on 12/18/17.
 */

public class CallClientFactory
{
    protected void initAndroid(OkHttpClient.Builder builder)
    {

    }

    protected void initIos(UrlSessionBuilder builder)
    {

    }

    protected Request modifyRequest(Request request)
    {
        return request;
    }

    public static class UrlSessionBuilder
    {
        public static final int NOTHING = - 1;
        int timeoutIntervalForRequest = NOTHING;
        int timeoutIntervalForResource = NOTHING;

        Map<String, String> additionalHeaders = new HashMap<>();
        boolean allowsCellularAccess = true;

        //Later
        /*public void addHeader(String key, String value)
        {
            additionalHeaders.put(key, value);
        }*/

        public void setTimeoutIntervalForRequest(int seconds)
        {
            timeoutIntervalForRequest = seconds;
        }

        public void setTimeoutIntervalForResource(int seconds)
        {
            timeoutIntervalForResource = seconds;
        }

        public void setAllowsCellularAccess(boolean b)
        {
            allowsCellularAccess = b;
        }
    }

    public okhttp3.Call.Factory createFactory()
    {
        if(isJ2objc())
        {
            UrlSessionBuilder sessionBuilder = new UrlSessionBuilder();
            initIos(sessionBuilder);
            return new CustomUrlSessionCallFactory(this, sessionBuilder);
        }
        else
        {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            initAndroid(builder);
            builder.addInterceptor(new Interceptor()
            {
                @Override
                public Response intercept(Chain chain) throws IOException
                {
                    Request request = chain.request();

                    return chain.proceed(modifyRequest(request));
                }
            });
            return builder.build();
        }
    }

    static boolean isJ2objc()
    {
        return System.getProperty("java.vendor").contains("J2ObjC");
    }

}
