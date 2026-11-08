package com.campus.studyroom.notifyservice.mapper;
import com.campus.studyroom.notifyservice.entity.NotifyTaskLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotifyTaskLogMapper {
    @Insert("INSERT INTO notify_task_log (execute_time, handle_count, sign_in_timeout_released, temp_leave_timeout_released, expired_completed, violation_created, seat_release_failed, message_push_triggered, cost_ms, trigger_type) " +
            "VALUES (#{executeTime}, #{handleCount}, #{signInTimeoutReleased}, #{tempLeaveTimeoutReleased}, #{expiredCompleted}, #{violationCreated}, #{seatReleaseFailed}, #{messagePushTriggered}, #{costMs}, #{triggerType})")
    void insert(NotifyTaskLog log);
}