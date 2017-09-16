package retrofit2.urlsession;

/**
 * Should implement a native method to create an NSURLSession instance.
 *
 * Created by kgalligan on 8/19/17.
 */

public interface UrlSessionConfigurationProvider
{
    Object createUrlSession();
}
