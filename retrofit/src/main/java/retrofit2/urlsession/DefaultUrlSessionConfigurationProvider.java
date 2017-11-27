package retrofit2.urlsession;
/**
 * Created by kgalligan on 8/19/17.
 */

public class DefaultUrlSessionConfigurationProvider implements UrlSessionConfigurationProvider
{
    @Override
    public native Object createUrlSession()/*-[
        NSURLSessionConfiguration *sessionConfiguration = [NSURLSessionConfiguration defaultSessionConfiguration];

//        NSOperationQueue *queue = [[NSOperationQueue alloc] init];
//        queue.maxConcurrentOperationCount = 5;

        return [NSURLSession sessionWithConfiguration:sessionConfiguration
                                        delegate:(id<NSURLSessionDataDelegate>)self
                                   delegateQueue:nil];
    ]-*/;
}
