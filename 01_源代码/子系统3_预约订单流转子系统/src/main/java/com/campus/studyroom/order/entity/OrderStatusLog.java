package com.campus.studyroom.order.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_status_log")
public class OrderStatusLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 逻辑外键：订单ID */
    private Long orderId;
    /** 原状态；订单首次创建初始流转记录时为NULL */
    private String fromStatus;
    /** 新状态 */
    private String toStatus;
    /** 操作来源（STUDENT=学生本人操作, ADMIN=管理员操作, SYSTEM=定时任务系统触发） */
    private String operatorType;
    /** 业务流转时间（即日志创建时间） */
    private LocalDateTime transitionTime;
    /** 变更原因 */
    private String reason;
}
