package com.campus.studyroom.seat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.studyroom.seat.common.Result;
import com.campus.studyroom.seat.entity.RoomInfo;
import com.campus.studyroom.seat.entity.SeatOccupancy;
import com.campus.studyroom.seat.mapper.SeatOccupancyMapper;
import com.campus.studyroom.seat.service.AdminRoomService;
import com.campus.studyroom.seat.service.AdminSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    @Autowired
    private AdminRoomService adminRoomService;
    @Autowired
    private AdminSeatService adminSeatService;
    @Autowired
    private SeatOccupancyMapper seatOccupancyMapper;

    // === 3.6.1 ~ 3.6.3 自习室管理 ===
    @PostMapping("/rooms")
    public Result<?> addRoom(@RequestBody RoomInfo room) {
        return Result.success(Map.of("roomId", adminRoomService.addRoom(room)));
    }

    @PutMapping("/rooms/{roomId}")
    public Result<?> updateRoom(@PathVariable Long roomId, @RequestBody RoomInfo room) {
        adminRoomService.updateRoom(roomId, room);
        return Result.success();
    }

    @DeleteMapping("/rooms/{roomId}")
    public Result<?> deleteRoom(@PathVariable Long roomId) {
        adminRoomService.deleteRoom(roomId);
        return Result.success();
    }

    // === 3.6.4 ~ 3.6.7 座位管理 ===
    @PostMapping("/rooms/{roomId}/seats")
    public Result<?> addSeat(@PathVariable Long roomId, @RequestBody Map<String, String> body) {
        return Result.success(Map.of("seatId", adminSeatService.addSeat(roomId, body.get("seatNumber"))));
    }

    @PutMapping("/seats/{seatId}")
    public Result<?> updateSeat(@PathVariable Long seatId, @RequestBody Map<String, String> body) {
        adminSeatService.updateSeat(seatId, body.get("seatNumber"));
        return Result.success();
    }

    @DeleteMapping("/seats/{seatId}")
    public Result<?> deleteSeat(@PathVariable Long seatId) {
        adminSeatService.deleteSeat(seatId);
        return Result.success();
    }

    // === 3.6.7 调整座位状态（核心修改部分） ===
    @PutMapping("/seats/{seatId}/status")
    public Result<?> changeSeatStatus(@PathVariable Long seatId, @RequestBody Map<String, String> body) {
        String targetStatus = body.get("status");

        // 只有设置维护或停用时，才进行未释放记录的校验
        if ("维护".equals(targetStatus) || "停用".equals(targetStatus)) {

            // 查该座位所有未释放的占用记录
            List<SeatOccupancy> unreleasedList = seatOccupancyMapper.selectList(
                    new LambdaQueryWrapper<SeatOccupancy>()
                            .eq(SeatOccupancy::getSeatId, seatId)
                            .eq(SeatOccupancy::getReleaseStatus, 0) // 0 表示未释放
            );

            if (unreleasedList != null && !unreleasedList.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                boolean isOccupying = false;
                List<Map<String, Object>> affectedOrders = new ArrayList<>();

                for (SeatOccupancy occ : unreleasedList) {
                    // 判断是否覆盖当前时刻
                    if (!now.isBefore(occ.getStartTime()) && now.isBefore(occ.getEndTime())) {
                        isOccupying = true;
                    }

                    // 组装受影响订单清单
                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("orderId", String.valueOf(occ.getOrderId())); // 雪花ID转String防精度丢失
                    orderMap.put("startTime", occ.getStartTime());
                    orderMap.put("endTime", occ.getEndTime());
                    affectedOrders.add(orderMap);
                }

                Map<String, Object> data = new HashMap<>();

                if (isOccupying) {
                    // 规则一：有覆盖此刻的记录（正在使用/暂离中）
                    data.put("affectedOrders", new ArrayList<>()); // 清单必须为空
                    return Result.error(1006, "座位正在使用中，无法设置维护/停用", data);
                } else {
                    // 规则二：无覆盖此刻，但有未来未释放记录
                    data.put("affectedOrders", affectedOrders); // 塞入清单
                    return Result.error(1006, "请先取消受影响的预约", data);
                }
            }
        }

        // 如果无任何冲突，正常更新座位状态
        adminSeatService.changeSeatStatus(seatId, targetStatus);
        // 注意：成功时，契约要求 data.affectedOrders 为空数组
        Map<String, Object> successData = new HashMap<>();
        successData.put("affectedOrders", new ArrayList<>());
        return Result.success(successData);
    }
}