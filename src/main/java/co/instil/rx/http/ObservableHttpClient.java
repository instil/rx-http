/*
 * Copyright 2015 Instil.
 */

package co.instil.rx.http;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.apache.http.ObservableHttp;
import rx.functions.Func1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * A HTTP client which utilises RX and the Apache Async HTTP libraries to execute asynchronous
 * HTTP requests with responses being returned via observables.
 */
public class ObservableHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(ObservableHttpClient.class);

    private final RequestConfig defaultRequestConfig;
    private final CloseableHttpAsyncClient asyncHttpClient;
    private final AuthCache authCache = new BasicAuthCache();

    public ObservableHttpClient(int connectTimeout, int socketTimeout, int connectionPoolSize) {
        this(null, null, connectTimeout, socketTimeout, connectionPoolSize);
    }

    /**
     * Create an observable HTTP client and start threads used for request/response handling.
     */
    public ObservableHttpClient(String username,
                                String password,
                                int connectTimeout,
                                int socketTimeout,
                                int connectionPoolSize) {

        defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
        asyncHttpClient = HttpAsyncClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig)
                .setDefaultCredentialsProvider(credentialsProvider(username, password))
                .setMaxConnPerRoute(connectionPoolSize)
                .setMaxConnTotal(connectionPoolSize)
                .build();
        asyncHttpClient.start();
    }

    /**
     * Stop any threads used for request/response handling and release pooled connections.
     */
    public void stop() {
        try {
            asyncHttpClient.close();
        } catch (IOException e) {
            logger.error("Failed to stop http client", e);
        }
    }

    private CredentialsProvider credentialsProvider(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return credentialsProvider;
    }

    /**
     * Enable preemptive HTTP basic authentication for a supplied host. This will cause the Authorization
     * header to be sent with every request to the server to avoid challenge/response authentication.
     */
    public void enablePreemptiveBasicAuth(String hostname) {
        if (hostname != null) {
            logger.debug("Enabling preemptive basic authentication for {}", hostname);
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(new HttpHost(hostname), basicAuth);
        }
    }

    /**
     * Enable preemptive HTTP digest authentication for a supplied host. This will cause the Authorization
     * header to be sent with every request to the server to avoid challenge/response authentication.
     */
    public void enablePreemptiveDigestAuth(String hostname, String realm, String nonce) {
        if (hostname != null) {
            logger.debug("Enabling preemptive digest authentication for {}", hostname);
            DigestScheme digestAuth = new DigestScheme();
            digestAuth.overrideParamter("realm", realm);
            digestAuth.overrideParamter("nonce", nonce);
            authCache.put(new HttpHost(hostname), digestAuth);
        }
    }

    /**
     * A convenience method to execute a simple HTTP GET. For greater flexibility
     * use {@link #executeHttpRequest(org.apache.http.client.methods.HttpUriRequest)}.
     */
    public Observable<ObservableHttpResponse> httpGet(String url) {
        RequestBuilder request = RequestBuilder.get(url);
        return executeHttpRequest(request.build());
    }

    /**
     * A convenience method to execute a simple HTTP POST. For greater flexibility
     * use {@link #executeHttpRequest(org.apache.http.client.methods.HttpUriRequest)}.
     */
    public Observable<ObservableHttpResponse> httpPost(String url, String body) throws UnsupportedEncodingException {
        RequestBuilder request = RequestBuilder.post(url).setEntity(new StringEntity(body));
        return executeHttpRequest(request.build());
    }

    /**
     * A convenience method to execute a simple HTTP PUT. For greater flexibility
     * use {@link #executeHttpRequest(org.apache.http.client.methods.HttpUriRequest)}.
     */
    public Observable<ObservableHttpResponse> httpPut(String url, String body) throws UnsupportedEncodingException {
        RequestBuilder request = RequestBuilder.put(url).setEntity(new StringEntity(body));
        return executeHttpRequest(request.build());
    }

    /**
     * A convenience method to execute a simple HTTP DELETE. For greater flexibility
     * use {@link #executeHttpRequest(org.apache.http.client.methods.HttpUriRequest)}.
     */
    public Observable<ObservableHttpResponse> httpDelete(String url) throws UnsupportedEncodingException {
        RequestBuilder request = RequestBuilder.delete(url);
        return executeHttpRequest(request.build());
    }

    /**
     * Execute a HTTP request asynchronously with the response being returned wrapped in a observable. Use a
     * {@link org.apache.http.client.methods.RequestBuilder} to construct the request to be executed.
     *
     * <code>
     * RequestBuilder request = RequestBuilder.get("http://instil.co").addHeader("Header", "Value");
     * executeHttpRequest(request.build());
     * </code>
     */
    public Observable<ObservableHttpResponse> executeHttpRequest(HttpUriRequest request) {
        logger.debug("Executing async {}", request);
        HttpClientContext clientContext = HttpClientContext.create();
        clientContext.setAuthCache(authCache);
        ObservableHttp observableHttp = ObservableHttp.createRequest(HttpAsyncMethods.create(request), asyncHttpClient, clientContext);
        return observableHttp.toObservable().map(new Func1<rx.apache.http.ObservableHttpResponse, ObservableHttpResponse>() {
            @Override
            public ObservableHttpResponse call(rx.apache.http.ObservableHttpResponse observableHttpResponse) {
                return new ObservableHttpResponse(observableHttpResponse);
            }
        });
    }

}
