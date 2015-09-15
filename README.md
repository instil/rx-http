## rx-http

A very simple wrapper on top of [RxApacheHttp](https://github.com/ReactiveX/RxApacheHttp) for managing a pooled asynchronous HTTP client with some convenience methods for executing simple HTTP requests.

## Binaries

Binaries are available from the [JCenter](https://jcenter.bintray.com/) maven repository. 

Gradle example:

```
repositories {
    jcenter()
}

dependencies {
    compile "co.instil.rx:rx-http:1.2"
}
```

## Sample usage

Execute a HTTP GET and access the completed response regardless of whether chunked or unchunked.

```
ObservableHttpClient client = new ObservableHttpClient("username", "password", 1000, 5000, 20);
client.enablePreemptiveBasicAuth();
client.httpGet("http://instil.co")
.flatMap({ ObservableHttpResponse response -> return response.getCompletedContent(); })
.foreach({ String response -> println(response); });
```