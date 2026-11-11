package com.campus.studyroom.order.remote;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 监控通知子系统远程调用客户端 (8084)
 * 依据《REST接口契约》4.3.4
 */
@Component
public class NotifyClient {

    @Value("${remote.notify-service-url}")
    private String notifyServiceUrl;

    @Value("${internal.token}")
    private String internalToken;

    @Value("${remote.connect-timeout-ms:2000}")
    private int connectTimeout;

    @Value("${remote.read-timeout-ms:5000}")
    private int readTimeout;

    /**
     * 4.3.4 内部消息推送
     * 注意：推送失败仅记录日志，不阻塞主流程。调用方不重试。
     */
    public String pushMessage(PushRequest requestBody) {
        String url = notifyServiceUrl + "/api/internal/notify/push";
        try (HttpResponse response = HttpRequest.post(url)
                .header("X-Internal-Token", internalToken)
                .body(JSONUtil.toJsonStr(requestBody))
                .setConnectionTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .execute()) {
            return response.body();
        } catch (Exception e) {
            // 异常抛出，Service 层捕获后仅记录日志，不阻塞订单主流程
            throw new RuntimeException("调用通知子系统网络异常: " + e.getMessage());
        }
    }

    // ================= 内部请求实体类 =================

    @Data
    public static class PushRequest {
        private String eventType;
        private Long userId;
        private Long orderId;
        private String title;
        private String content;
        private String bizTime;   // 格式 yyyy-MM-dd HH:mm:ss
        private String dedupKey;  // 幂等键，如 RESERVE_SUCCESS:{orderId}
    }
}