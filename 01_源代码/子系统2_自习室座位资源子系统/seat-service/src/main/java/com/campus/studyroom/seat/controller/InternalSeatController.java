package com.campus.studyroom.seat.controller;

import com.campus.studyroom.seat.common.Result;
import com.campus.studyroom.seat.service.SeatReleaseService;
import com.campus.studyroom.seat.service.SeatStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/seats")
@CrossOrigin
public class InternalSeatController {

    @Autowired
    private SeatStatusService seatStatusService;

    @Autowired
    private SeatReleaseService seatReleaseService;

    // 3.5 状态同步
    @PostMapping("/{seatId}/status")
    public Result<?> updateStatus(@PathVariable Long seatId, @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        Long orderId = Long.valueOf(body.get("orderId").toString());
        seatStatusService.updateSeatStatus(seatId, orderId, status);
        return Result.success("状态同步成功", null);
    }

    // 3.4 释放座位
    @PostMapping("/{seatId}/release")
    public Result<?> releaseSeat(@PathVariable Long seatId, @RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        String action = (String) body.get("action");
        // 调用 Service
        Map<String, Object> data = seatReleaseService.releaseSeat(seatId, orderId, action);
        String msg = data.containsKey("message") ? (String) data.get("message") : "释放成功";
        return Result.success(msg, data);
    }
}