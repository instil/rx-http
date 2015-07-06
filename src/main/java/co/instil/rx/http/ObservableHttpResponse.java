/*
 * Copyright 2015 Instil.
 */

package co.instil.rx.http;

import org.apache.http.HttpResponse;
import rx.Observable;

import java.nio.charset.Charset;

import static rx.observables.StringObservable.decode;
import static rx.observables.StringObservable.stringConcat;

/**
 * A layer over the top of RXApacheHttp response objects to expose
 * some convenience methods for working with asynchronous responses.
 */
public class ObservableHttpResponse {

    private final rx.apache.http.ObservableHttpResponse observableHttpResponse;

    public ObservableHttpResponse(rx.apache.http.ObservableHttpResponse observableHttpResponse) {
        this.observableHttpResponse = observableHttpResponse;
    }

    /**
     * The {@link HttpResponse} returned by the Apache client at the beginning of the response. This
     * should not be used to access the response body as it will not be immediately available.
     */
    public HttpResponse getResponse() {
        return observableHttpResponse.getResponse();
    }

    /**
     * If the response is not chunked then the observable will emit only a single byte array. If the
     * response is chunked then multiple arrays will be emitted.
     */
    public Observable<byte[]> getContent() {
        return observableHttpResponse.getContent();
    }

    /**
     * A convenience method to allow access to the completed response body whether chunked or not.
     * The observable will emit a single string containing the response body.
     */
    public Observable<String> getCompletedContent() {
        return stringConcat(decode(observableHttpResponse.getContent(), Charset.forName("UTF-8")));
    }

}
