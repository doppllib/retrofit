package retrofit2.clientfactory;

import okhttp3.Call;
import okhttp3.Request;
import retrofit2.urlsession.*;

/**
 * Created by kgalligan on 12/18/17.
 */

public class CustomUrlSessionCallFactory extends UrlSessionCallFactory
{
    
    private final CallClientFactory clientFactory;

    public CustomUrlSessionCallFactory( CallClientFactory clientFactory,  CallClientFactory.UrlSessionBuilder sessionBuilder)
    {
        super(new CustomUrlSessionConfigurationProvider(sessionBuilder));
        this.clientFactory = clientFactory;
    }

    @Override
    public Call newCall(Request request)
    {
        return new UrlSessionCall(urlSessionReference, clientFactory.modifyRequest(request));
    }
}
