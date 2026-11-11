package com.campus.studyroom.seat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("room_info")
public class RoomInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roomName;
    private String location;
    private Integer status; // 1开放 0关闭
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}