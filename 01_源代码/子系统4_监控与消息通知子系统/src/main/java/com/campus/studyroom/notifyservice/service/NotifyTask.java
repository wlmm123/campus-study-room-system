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

    // 每 30 秒触发一次 (读取 application.yml 里的 notify.scan.interval)
    @Scheduled(fixedRateString = "${notify.scan.interval}")
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
            log.info("【定时任务】调用订单超时处理成功，返回: {}", response.getBody());

            // 记录日志到数据库
            NotifyTaskLog taskLog = new NotifyTaskLog();
            taskLog.setExecuteTime(LocalDateTime.now());
            taskLog.setHandleCount(0); // 为了不报错先写0，后续可解析 response.getBody() 拿到真实数据
            taskLog.setCostMs(cost);
            taskLog.setTriggerType("SCHEDULE");
            taskLogMapper.insert(taskLog);

        } catch (Exception e) {
            // 容错机制：8083 挂了也不影响 8084 本身
            log.error("【定时任务】调用订单服务失败（容错机制捕获），等待下一轮: {}", e.getMessage());
        }
    }
}