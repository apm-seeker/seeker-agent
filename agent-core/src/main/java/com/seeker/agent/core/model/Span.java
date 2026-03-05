package com.seeker.agent.core.model;

import com.seeker.agent.core.context.TraceId;
import java.util.ArrayList;
import java.util.List;

/**
 * 하나의 서버에서 요청을 처리하는 단위를 표현하는 클래스.
 */
public class Span {

    private final TraceId traceId;
    private final long startTime;
    private int elapsedTime;
    private final String remoteAddr;
    private String agentId;
    private String applicationName;
    private String rpc;
    private String endPoint;
    private int serviceType;
    private String exceptionInfo;
    private final List<SpanEvent> spanEventList = new ArrayList<>();

    public Span(TraceId traceId, String remoteAddr, long startTime) {
        this.traceId = traceId;
        this.startTime = startTime;
        this.remoteAddr = remoteAddr;
    }

    public void finish() {
        this.elapsedTime = (int) (System.currentTimeMillis() - this.startTime);
    }

    public void finish(Throwable throwable) {
        finish();
        if (throwable != null) {
            this.exceptionInfo = throwable.toString();
        }
    }

    public void addSpanEvent(SpanEvent event) {
        this.spanEventList.add(event);
    }

    public TraceId getTraceId() {
        return traceId;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getElapsedTime() {
        return elapsedTime;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public List<SpanEvent> getSpanEventList() {
        return spanEventList;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getRpc() {
        return rpc;
    }

    public void setRpc(String rpc) {
        this.rpc = rpc;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public int getServiceType() {
        return serviceType;
    }

    public void setServiceType(int serviceType) {
        this.serviceType = serviceType;
    }

    public String getExceptionInfo() {
        return exceptionInfo;
    }

    public void setExceptionInfo(String exceptionInfo) {
        this.exceptionInfo = exceptionInfo;
    }

    @Override
    public String toString() {
        return "Span{" +
                "traceId=" + traceId +
                ", startTime=" + startTime +
                ", elapsedTime=" + elapsedTime +
                ", remoteAddr='" + remoteAddr + '\'' +
                ", spanEventCount=" + spanEventList.size() +
                '}';
    }
}
