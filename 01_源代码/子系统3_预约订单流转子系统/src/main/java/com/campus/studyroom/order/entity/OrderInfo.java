package com.campus.studyroom.order.entity;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("order_info")
public class OrderInfo {
    /** 主键（应用层预生成全局唯一 orderId） */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 逻辑外键：用户ID */
    private Long userId;
    /** 逻辑外键：座位ID */
    private Long seatId;
    /** 状态（待签到,已签到,已取消,超时释放,暂离超时释放,已完成） */
    private String status;
    /** 预约开始时间 */
    private LocalDateTime startTime;
    /** 预约结束时间 */
    private LocalDateTime endTime;
    /** 暂离开始时间 */
    private LocalDateTime tempLeaveStartTime;
    /** 订单进入终态时间；仅终态有值，进行中为NULL */
    private LocalDateTime finishTime;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /** 逻辑删除: 0未删, 1已删 */
    @TableLogic
    private Integer isDeleted;
}
