package com.campus.studyroom.seat.controller;

import com.campus.studyroom.seat.common.Result;
import com.campus.studyroom.seat.service.SeatLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/seats")
@CrossOrigin
public class SeatLockController {

    @Autowired
    private SeatLockService seatLockService;

    @PostMapping("/{seatId}/lock")
    public Result<?> lockSeat(@PathVariable Long seatId, @RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());

        // 解析时间字符串
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startTime = LocalDateTime.parse(body.get("startTime").toString(), formatter);
        LocalDateTime endTime = LocalDateTime.parse(body.get("endTime").toString(), formatter);

        // 调用 Service
        Map<String, Object> data = seatLockService.lockSeat(seatId, orderId, startTime, endTime);
        return Result.success("锁座成功", data);
    }
}