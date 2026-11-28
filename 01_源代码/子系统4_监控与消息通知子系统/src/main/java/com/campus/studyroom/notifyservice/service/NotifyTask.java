package com.campus.studyroom.notifyservice.service;

import com.campus.studyroom.notifyservice.entity.NotifyTaskLog;
import com.campus.studyroom.notifyservice.mapper.NotifyTaskLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Component
public class NotifyTask {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotifyTaskLogMapper taskLogMapper;

    @Value("${remote.order-service-url}")
    private String orderServiceUrl;

    @Value("${internal.token}")
    private String internalToken;

    // 每 60 秒触发一次 (读取 application.yml 里的 notify.scan.interval)
    @Scheduled(cron = "0 */1 * * * ?")
    public void scanAndTrigger() {
        log.info("【定时任务】开始触发订单超时扫描...");
        String url = orderServiceUrl + "/api/internal/orders/batch-handle-timeout";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", internalToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        long start = System.currentTimeMillis();

        try {
            // 发起 POST 请求调用成员3的接口
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            long cost = System.currentTimeMillis() - start;
            String responseBody = response.getBody();
            log.info("【定时任务】调用订单超时处理成功，返回: {}", responseBody);

            // 使用 Jackson 解析返回的 JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode data = root.get("data");

            // 记录日志到数据库
            NotifyTaskLog taskLog = new NotifyTaskLog();
            taskLog.setExecuteTime(LocalDateTime.now());
            taskLog.setCostMs(cost);
            taskLog.setTriggerType("SCHEDULE");

            // 从返回数据中提取真实字段，如果取不到就默认为 0
            if (data != null) {
                taskLog.setHandleCount(data.has("handleCount") ? data.get("handleCount").asInt() : 0);
                taskLog.setSignInTimeoutReleased(data.has("signInTimeoutReleased") ? data.get("signInTimeoutReleased").asInt() : 0);
                taskLog.setTempLeaveTimeoutReleased(data.has("tempLeaveTimeoutReleased") ? data.get("tempLeaveTimeoutReleased").asInt() : 0);
                taskLog.setExpiredCompleted(data.has("expiredCompleted") ? data.get("expiredCompleted").asInt() : 0);
                taskLog.setViolationCreated(data.has("violationCreated") ? data.get("violationCreated").asInt() : 0);
                taskLog.setSeatReleaseFailed(data.has("seatReleaseFailed") ? data.get("seatReleaseFailed").asInt() : 0);
                taskLog.setMessagePushTriggered(data.has("messagePushTriggered") ? data.get("messagePushTriggered").asInt() : 0);
            } else {
                // 如果 data 为空，全给 0
                taskLog.setHandleCount(0);
                taskLog.setSignInTimeoutReleased(0);
                taskLog.setTempLeaveTimeoutReleased(0);
                taskLog.setExpiredCompleted(0);
                taskLog.setViolationCreated(0);
                taskLog.setSeatReleaseFailed(0);
                taskLog.setMessagePushTriggered(0);
            }

            taskLogMapper.insert(taskLog);

        } catch (Exception e) {
            // 容错机制：8083 挂了也不影响 8084 本身
            log.error("【定时任务】处理异常: {}", e.getMessage());
        }
    }
}