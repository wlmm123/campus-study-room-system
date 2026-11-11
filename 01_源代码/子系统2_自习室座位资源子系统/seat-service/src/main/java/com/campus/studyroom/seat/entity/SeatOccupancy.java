package com.campus.studyroom.seat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seat_occupancy")
public class SeatOccupancy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long seatId;
    private Long orderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer releaseStatus;
    private Integer checkinFlag;
    private LocalDateTime releaseTime;
    private String action;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
