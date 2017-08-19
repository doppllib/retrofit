package retrofit2.urlsession;
import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by kgalligan on 12/21/16.
 */

public interface Interceptor
{
    Response intercept(Chain chain) throws IOException;

    interface Chain {
        Request request();

        Response proceed(Request request) throws IOException;

        HttpURLConnection connection();
    }
}
