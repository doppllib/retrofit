package retrofit2.urlsession;
import okhttp3.Call;
import okhttp3.Request;

/**
 * Created by kgalligan on 8/12/17.
 */

public class UrlSessionCallFactory implements okhttp3.Call.Factory
{
    private final Object urlSessionReference;


    int connectTimeout;
    int readTimeout;
    int writeTimeout;

    public UrlSessionCallFactory()
    {
        this(new DefaultUrlSessionConfigurationProvider());
    }

    public UrlSessionCallFactory(UrlSessionConfigurationProvider urlSessionProvider)
    {
        this.urlSessionReference = urlSessionProvider.createUrlSession();
    }

    @Override
    public Call newCall(Request request)
    {
        return new UrlSessionCall(urlSessionReference, request);
    }
}
