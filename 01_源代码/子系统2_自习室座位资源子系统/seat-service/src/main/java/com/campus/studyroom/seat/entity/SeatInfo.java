
package com.campus.studyroom.seat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seat_info")
public class SeatInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String seatNumber;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}