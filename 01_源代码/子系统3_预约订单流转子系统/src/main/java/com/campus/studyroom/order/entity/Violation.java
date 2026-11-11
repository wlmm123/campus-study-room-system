package com.campus.studyroom.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("violation")
public class Violation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑外键：订单ID */
    private Long orderId;

    /** 逻辑外键：用户ID */
    private Long userId;

    /** 违约类型（超时未签到, 暂离超时） */
    private String type;

    /** 业务违约发生时间 */
    private LocalDateTime violationTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    /** 违约原因描述 */
    private String reason;
}