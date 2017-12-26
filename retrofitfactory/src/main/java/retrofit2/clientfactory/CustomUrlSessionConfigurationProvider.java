package retrofit2.clientfactory;

import retrofit2.urlsession.UrlSessionConfigurationProvider;

/**
 * Created by kgalligan on 12/18/17.
 */

public class CustomUrlSessionConfigurationProvider implements UrlSessionConfigurationProvider
{
    
    private final CallClientFactory.UrlSessionBuilder sessionBuilder;

    public CustomUrlSessionConfigurationProvider( CallClientFactory.UrlSessionBuilder sessionBuilder)
    {
        this.sessionBuilder = sessionBuilder;
    }

    @Override
    public native Object createUrlSession()/*-[
        NSURLSessionConfiguration *sessionConfiguration = [NSURLSessionConfiguration defaultSessionConfiguration];

        if(self->sessionBuilder_->timeoutIntervalForRequest_ != -1)
            sessionConfiguration.timeoutIntervalForRequest = self->sessionBuilder_->timeoutIntervalForRequest_;

        if(self->sessionBuilder_->timeoutIntervalForResource_ != -1)
            sessionConfiguration.timeoutIntervalForResource = self->sessionBuilder_->timeoutIntervalForResource_;

        sessionConfiguration.allowsCellularAccess = self->sessionBuilder_->allowsCellularAccess_;

        return [NSURLSession sessionWithConfiguration:sessionConfiguration
        delegate:(id<NSURLSessionDataDelegate>)self
        delegateQueue:nil];
    ]-*/;
}
