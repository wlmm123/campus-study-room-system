package com.campus.studyroom.notifyservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotifyMessage {
    private Long id;
    private Long userId;
    private Long orderId;
    private String eventType;
    private String title;
    private String content;
    private String status; // UNREAD, READ
    private LocalDateTime bizTime;
    private String dedupKey;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime readTime;
    private Integer isDeleted;
}