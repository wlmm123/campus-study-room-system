package com.campus.studyroom.notifyservice.entity;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotifyTaskLog {
    private Long id;
    private LocalDateTime executeTime;
    private Integer handleCount;
    private Integer signInTimeoutReleased;
    private Integer tempLeaveTimeoutReleased;
    private Integer expiredCompleted;
    private Integer violationCreated;
    private Integer seatReleaseFailed;
    private Integer messagePushTriggered;
    private Long costMs;
    private String triggerType;
}