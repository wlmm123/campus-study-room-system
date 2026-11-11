package com.campus.studyroom.order.remote;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 座位资源子系统远程调用客户端 (8082)
 * 依据《REST接口契约》3.3 / 3.4 / 3.5
 */
@Component
public class SeatClient {

    @Value("${remote.seat-service-url}")
    private String seatServiceUrl;

    @Value("${internal.token}")
    private String internalToken;

    @Value("${remote.connect-timeout-ms:2000}")
    private int connectTimeout;

    @Value("${remote.read-timeout-ms:5000}")
    private int readTimeout;

    /**
     * 3.3 锁定座位
     * 注意：endTime 由订单侧算好传入（档位值 × duration_unit_minutes）
     */
    public String lockSeat(Long seatId, LockSeatRequest requestBody) {
        String url = seatServiceUrl + "/api/seats/" + seatId + "/lock";
        return doPost(url, JSONUtil.toJsonStr(requestBody));
    }

    /**
     * 3.4 释放座位
     * action: CANCEL / TIMEOUT / FINISH
     */
    public String releaseSeat(Long seatId, ReleaseSeatRequest requestBody) {
        String url = seatServiceUrl + "/api/internal/seats/" + seatId + "/release";
        return doPost(url, JSONUtil.toJsonStr(requestBody));
    }

    /**
     * 3.5 更新座位状态（新增，依据契约 4.2.2 / 4.2.4 / 4.2.5）
     * status 仅接收：已预约 / 已占用 / 暂离中
     */
    public String updateStatus(Long seatId, StatusUpdateRequest requestBody) {
        String url = seatServiceUrl + "/api/internal/seats/" + seatId + "/status";
        return doPost(url, JSONUtil.toJsonStr(requestBody));
    }

    /**
     * 统一的 POST 请求封装，携带内部令牌与超时时间
     */
    private String doPost(String url, String jsonBody) {
        try (HttpResponse response = HttpRequest.post(url)
                .header("X-Internal-Token", internalToken)
                .body(jsonBody)
                .setConnectionTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .execute()) {
            // 直接返回响应体，业务错误码（1001/1003/1006等）交由 Service 层统一解析处理
            return response.body();
        } catch (Exception e) {
            // 网络异常（超时、拒绝连接等），抛出由上层捕获处理为 1007
            throw new RuntimeException("调用座位子系统网络异常: " + e.getMessage());
        }
    }

    // ================= 内部请求实体类 =================

    @Data
    public static class LockSeatRequest {
        private Long orderId;
        private String startTime;
        private String endTime;
        private Integer durationHours;
    }

    @Data
    public static class ReleaseSeatRequest {
        private Long orderId;
        private String action;
    }

    // 新增：3.5 状态更新请求类
    @Data
    public static class StatusUpdateRequest {
        private String status;
        private Long orderId;
    }
}