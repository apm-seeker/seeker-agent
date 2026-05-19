package com.seeker.scenario.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Apache HttpClient 4.x wrapper.
 * seeker-agent 의 httpclient-plugin 이 이 클래스를 통과하는 요청에 W3C traceparent 헤더를 자동 주입.
 */
@Service
public class HttpService {

    private static final int CONNECT_TIMEOUT_MS = 1_000;
    private static final int SOCKET_TIMEOUT_MS = 2_000;

    public String get(String url, String scenario) {
        HttpGet req = new HttpGet(url);
        return execute(req, scenario);
    }

    public String post(String url, String jsonBody, String scenario) {
        HttpPost req = new HttpPost(url);
        req.setHeader("Content-Type", "application/json");
        if (jsonBody != null) {
            req.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }
        return execute(req, scenario);
    }

    private String execute(HttpUriRequest req, String scenario) {
        if (scenario != null) {
            req.setHeader("X-Scenario", scenario);
        }
        RequestConfig cfg = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .build();
        if (req instanceof HttpRequestBase base) {
            base.setConfig(cfg);
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpResponse response = client.execute(req);
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() == null
                    ? ""
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (status >= 400) {
                throw new DownstreamException(status, body);
            }
            return body;
        } catch (DownstreamException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpServiceException(e);
        }
    }

    public static class HttpServiceException extends RuntimeException {
        public HttpServiceException(Throwable cause) {
            super(cause);
        }
    }

    public static class DownstreamException extends RuntimeException {
        public final int status;
        public final String body;
        public DownstreamException(int status, String body) {
            super("HTTP " + status + " " + body);
            this.status = status;
            this.body = body;
        }
    }
}
