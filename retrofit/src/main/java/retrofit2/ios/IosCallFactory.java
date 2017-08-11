package retrofit2.ios;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.Request;
import okhttp3.internal.cache.InternalCache;

/**
 * Created by kgalligan on 12/20/16.
 */

public class IosCallFactory implements okhttp3.Call.Factory
{
    final IosDispatcher dispatcher = new IosDispatcher();

    int connectTimeout = 4_000;
    int readTimeout = 4_000;

    final List<Interceptor> interceptors;
    final List<Interceptor> networkInterceptors;

    CookieJar cookieJar = CookieJar.NO_COOKIES;
    Cache         cache;
    InternalCache internalCache;
    boolean retryOnConnectionFailure = true;

    public IosCallFactory()
    {
        this(new ArrayList<Interceptor>(), new ArrayList<Interceptor>());
    }

    public IosCallFactory(List<Interceptor> interceptors, List<Interceptor> networkInterceptors)
    {
        this.interceptors = interceptors;
        this.networkInterceptors = networkInterceptors;
    }

    public void addInterceptor(Interceptor interceptor)
    {
        interceptors.add(interceptor);
    }

    List<Interceptor> interceptors()
    {
        return interceptors;
    }

    public void addNetworkInterceptor(Interceptor interceptor)
    {
        networkInterceptors.add(interceptor);
    }

    List<Interceptor> networkInterceptors()
    {
        return networkInterceptors;
    }

    public CookieJar cookieJar(){
        return cookieJar;
    }

    public Cache cashe()
    {
        return cache;
    }
    public InternalCache internalCache()
    {
        return internalCache;
    }

    @Override
    public Call newCall(Request request)
    {
        return new IosCall(this, request);
    }

    public static final class Builder
    {
        Dispatcher dispatcher;
    }

    public IosDispatcher dispatcher(){
        return dispatcher;
    }

    public boolean retryOnConnectionFailure(){
        return retryOnConnectionFailure;
    }

    public void setRetryOnConnectionFailure(boolean retryOnConnectionFailure)
    {
        this.retryOnConnectionFailure = retryOnConnectionFailure;
    }

    public void setConnectTimeout(int connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(int readTimeout)
    {
        this.readTimeout = readTimeout;
    }
}
