package retrofit2.urlsession;
import okhttp3.Call;
import okhttp3.Request;

/**
 * Created by kgalligan on 8/12/17.
 */

public class UrlSessionCallFactory implements okhttp3.Call.Factory
{
    @Override
    public Call newCall(Request request)
    {
        return new UrlSessionCall(request);
    }
}
