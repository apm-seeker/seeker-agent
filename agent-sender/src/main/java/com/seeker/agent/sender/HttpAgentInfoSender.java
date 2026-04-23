package com.seeker.agent.sender;

import com.seeker.agent.core.model.AgentInfo;
import com.seeker.agent.core.sender.AgentInfoSender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP(POST /agents)로 에이전트 식별 정보를 Collector에 등록하는 구현체입니다.
 */
public class HttpAgentInfoSender implements AgentInfoSender {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final URI endpoint;
    private final HttpClient httpClient;

    public HttpAgentInfoSender(String host, int httpPort) {
        this.endpoint = URI.create("http://" + host + ":" + httpPort + "/agents");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public void register(AgentInfo info) {
        String body = toJson(info);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                System.out.println("[Seeker] 에이전트 등록 성공 (status=" + status + ", endpoint=" + endpoint + ")");
            } else {
                System.err.println("[Seeker] 에이전트 등록 실패 (status=" + status + ", body=" + response.body() + ")");
            }
        } catch (Exception e) {
            System.err.println("[Seeker] 에이전트 등록 요청 중 오류: " + e.getMessage());
        }
    }

    private String toJson(AgentInfo info) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        appendString(sb, "agentId", info.getAgentId()).append(',');
        appendString(sb, "agentName", info.getAgentName()).append(',');
        appendString(sb, "agentType", info.getAgentType()).append(',');
        appendString(sb, "agentGroup", info.getAgentGroup()).append(',');
        sb.append("\"startTime\":").append(info.getStartTime());
        sb.append('}');
        return sb.toString();
    }

    private StringBuilder appendString(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escape(value)).append('"');
        }
        return sb;
    }

    private String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
