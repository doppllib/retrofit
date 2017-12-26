# Doppl Retrofit Factory

Retrofit handles the REST part of networking, but doesn't try to wrap some underlying
network details, and instead requires that the developer provide a custom configured
OkHttpClient instance.

This makes sense, but on iOS the Doppl implementation uses URLSession rather than OkHttp

Retrofit for Doppl relies on using UrlSession as the underlying networking
library, rather than OKHttp, around which Retrofit 2 in Java has been primarily
designed. As a result, any functionality for Retrofit 2, beyond "basic" functionality,
is handled by creating an instance of OKHttpClient.

## Why URLSession?

OKHttp does a lot of stuff under the hood. It is not simply a wrapper around
the underlying networking code. As such, trying to adapt that to J2objc presents
a number of issues. Besides raw performance, J2objc does not provide SSL
Sockets out of the box. This would mean implementing SSL Sockets, and trying
to make sure all of OKHttp was supported, including HTTP 2, SPDY, etc. While
certainly possible, URLSession provides a first-class implementation of these
things for iOS. Although Retrofit 2 definitely leans toward OkHttp as the
networking layer implementation, it is sufficiently abstracted to allow an alternate
implementation.

## How to use

Retrofit and OkHttp have pretty good defaults, so for many general cases, you don't need
to do anything to use Retrofit on iOS. The functionality is generally the same. However,
on Android you'll often need to provide your own instance of OkHttpClient to Retrofit,
which allows you to step in and manage network details directly.

On iOS we are using the native networking provided by URLSession and associated classes.
This provides much of the functionality of OkHttp, although the details of how features
are implemented differs.

For some of the more common operations, the Doppl version has a helper class. This will
let you set timeouts and modify headers. For more complex features (Interceptors, etc), you'll
need to provide your own URLSessionConfiguration and manage URLSession task creation directly.

### okhttp3.Call.Factory

Retrofit needs an implementation of [okhttp3.Call.Factory][Call.Factory] to create [okhttp3.Call][Call]
instances. The Retrofit.Builder class has a method to set the factory instance
[retrofit2.Retrofit.Builder.callFactory][callFactory]. On Android you would provide [OkHttpClient][web-OkHttpClient],
and on iOS [retrofit2.urlsession.UrlSessionCallFactory][gh-UrlSessionCallFactory].

> You'll probably be more familiar with [retrofit2.Retrofit.Builder.client][https://github.com/square/retrofit/blob/903e149a4f211a0fb7e01ed4fdf8c48a8b6c7891/retrofit/src/main/java/retrofit2/Retrofit.java#L431], which
> takes an OkHttpClient parameter. This is just a convenience method that calls into
> [retrofit2.Retrofit.Builder.callFactory][callFactory]

To use the helper class, create an instance of retrofit2.clientfactory.CallClientFactory, then override methods and
implement as desired.

```java
@Provides
@Singleton
Call.Factory providesDroidconRetrofitCallFactory()
{
    CallClientFactory callClientFactory = new CallClientFactory()
    {
        /**
         * Override initAndroid if you want to manipulate the OkHttpClient.Builder
         */
        @Override
        protected void initAndroid(OkHttpClient.Builder builder)
        {
            builder.connectTimeout(10, TimeUnit.SECONDS);
        }

        /**
         * Override initIos if you want to manipulate the UrlSessionBuilder. This class
         * has a few common iOS settings.
         */
        @Override
        protected void initIos(UrlSessionBuilder builder)
        {
            builder.setTimeoutIntervalForRequest(15);
        }

        /**
         * Generally for modifying headers. This may be confusing, as this is called in an interceptor on Android
         * but before the request on iOS. The default implementation is a passthrough, but you can modify headers
         * as shown here.
         */
        @Override
        protected Request modifyRequest(Request request)
        {
            Request.Builder builder = request.newBuilder();
            builder.addHeader("HELLO", "HITHERE!!!");
            return builder.build();
        }
    };

    //This will create the expected implementation for the platform you're currently running on.
    Call.Factory factory = callClientFactory.createFactory();
}
```

Create a CallClientFactory instance and override methods that you would like to manipulate. You can interact with the OkHttpClient.Builder
by overriding initAndroid. In initIos you can apply changes to UrlSessionBuilder, although we currently only have a limited set of
fields implemented. See [URLSessionConfiguration][urlsessionconfiguration] for more info on these fields

* timeoutIntervalForRequest - The timeout interval to use when waiting for additional data.
* timeoutIntervalForResource - The maximum amount of time that a resource request should be allowed to take.
* allowsCellularAccess - A Boolean value that determines whether connections should be made over a cellular network.

Depending on which platform you are on, Android or iOS, CallClientFactory will create the appropriate Builder and call the
associated method.

You can also override 'modifyRequest'. This primarily exists to allow you to modify headers for the request. With OkHttpClient,
this is implemented with an Interceptor. On iOS, this is called a little earlier in the chain. For general header
manipulation, this should be sufficient, but we currently don't have a facility as flexible as the Interceptors, so
you will likely run into scenarios that need custom implementations as needs become more complex. This is out of scope for this doc,
but see retrofit2.clientfactory.CustomUrlSessionConfigurationProvider for an example of a custom URLSession creation, and
retrofit2.urlsession.UrlSessionCall for the iOS call handling.

## Usage

Add gradle dependencies for both retrofit and retrofitfactory, which has the helper classes.

```gradle
compile "com.squareup.retrofit2:retrofit:2.3.0"
compile "co.doppl.com.squareup.retrofit2.urlsession:retrofitfactory:2.3.0"

doppl "co.doppl.com.squareup.retrofit2.urlsession:retrofit:2.3.0.10"
doppl "co.doppl.com.squareup.retrofit2.urlsession:retrofitfactory:2.3.0.10"
```

Create the CallClientFactory like above, and set it as follows:

```java

CallClientFactory callClientFactory = new CallClientFactory()
    {
    //Implement init methods
    };

Retrofit.Builder builder = new Retrofit.Builder()
    .baseUrl(baseUrl)
    .callFactory(callClientFactory.createFactory());

Retrofit retrofit = builder.build();
```

[urlsessionconfiguration]: https://developer.apple.com/documentation/foundation/urlsessionconfiguration
[Call]: https://square.github.io/okhttp/3.x/okhttp/okhttp3/Call.html
[Call.Factory]: https://square.github.io/okhttp/3.x/okhttp/okhttp3/Call.Factory.html
[client]: https://github.com/square/retrofit/blob/903e149a4f211a0fb7e01ed4fdf8c48a8b6c7891/retrofit/src/main/java/retrofit2/Retrofit.java#L431
[callFactory]: https://github.com/square/retrofit/blob/903e149a4f211a0fb7e01ed4fdf8c48a8b6c7891/retrofit/src/main/java/retrofit2/Retrofit.java#L440
[web-OkHttpClient]: https://square.github.io/okhttp/3.x/okhttp/okhttp3/OkHttpClient.html
[gh-UrlSessionCallFactory]: https://github.com/doppllib/retrofit/blob/doppl-2.3.0/retrofit/src/main/java/retrofit2/urlsession/UrlSessionCallFactory.java