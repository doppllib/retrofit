Retrofit
========

Type-safe HTTP client for Android and Java by Square, Inc.

For more information please see [the website][1].

# Doppl Version

The most important thing to keep in mind is that OkHttp is *not* supported in
J2objc. This is primarily because SSL stream support isn't implemented in iOS.
Retrofit defines an interface that lets you implement networking with something
other than OKHttp, which is what we've done.

iOS networking is implemented with [URLSession][3]. This will be frustrating if you do complex stuff with OkHttp.
However, from an iOS development perspective, URLSession kind of makes more sense. Rather than trying
to mimic what's happening with OkHttp, using URLSession will allow advanced and direct control
of networking. On the down side, you'll need to implement that stuff more than once.

We will be adding a common factory comonent that will allow you to generate the platform-specific
networking class, but for now you'll need to create the

See retrofit2.urlsession.UrlSessionConfigurationProvider and retrofit2.urlsession.DefaultUrlSessionConfigurationProvider
for an example of how to create URLSession instances

| Module        | Status        | Tests  | Memory  |
| ------------- |:-------------:| :------:|:--------:|
| Retrofit      | 👍            | 👍     | Not Done |
| adapters/rxjava        | Unsupported    | N/A    | N/A      |
| adapters/rxjava2       | 👍             | 🤢     | Not Done |
| adapters/guava         | 👍             | 👍     | Not Done |
| adapters/java8         | Unsupported    | N/A    | N/A      |
| converters/gson        | 👍       | 👍     | Not Done |
| converters/guava       | 👍       | 👍     | Not Done |
| converters/jackson     | Unsupported    | N/A    | N/A      |
| converters/java8       | 👍       | 👍     | Not Done |
| converters/moshi       | 👍       | 👍     | Not Done |
| converters/protobuf    | Unsupported    | N/A    | N/A      |
| converters/scalars     | 👍       | 👍     | Not Done |
| converters/simplexml   | Unsupported    | N/A    | N/A      |
| converters/wire        | Unsupported    | N/A    | N/A      |



Download
--------

Download [the latest JAR][2] or grab via Maven:
```xml
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>retrofit</artifactId>
  <version>2.3.0</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup.retrofit2:retrofit:2.3.0'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

Retrofit requires at minimum Java 7 or Android 2.3.



License
=======

    Copyright 2013 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


 [1]: http://square.github.io/retrofit/
 [2]: https://search.maven.org/remote_content?g=com.squareup.retrofit2&a=retrofit&v=LATEST
 [3]: https://developer.apple.com/documentation/foundation/urlsession
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
