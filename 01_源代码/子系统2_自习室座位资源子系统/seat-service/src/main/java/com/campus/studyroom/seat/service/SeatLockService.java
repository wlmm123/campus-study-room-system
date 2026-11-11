package com.campus.studyroom.seat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.studyroom.seat.entity.RoomInfo;
import com.campus.studyroom.seat.entity.SeatInfo;
import com.campus.studyroom.seat.entity.SeatOccupancy;
import com.campus.studyroom.seat.mapper.RoomInfoMapper;
import com.campus.studyroom.seat.mapper.SeatInfoMapper;
import com.campus.studyroom.seat.mapper.SeatOccupancyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SeatLockService {

    @Autowired
    private SeatInfoMapper seatInfoMapper;
    @Autowired
    private RoomInfoMapper roomInfoMapper;
    @Autowired
    private SeatOccupancyMapper seatOccupancyMapper;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> lockSeat(Long seatId, Long orderId, LocalDateTime startTime, LocalDateTime endTime) {

        // 1. 幂等校验：看这个订单是不是已经锁过座了
        LambdaQueryWrapper<SeatOccupancy> idemWrapper = new LambdaQueryWrapper<>();
        idemWrapper.eq(SeatOccupancy::getOrderId, orderId);
        SeatOccupancy exist = seatOccupancyMapper.selectOne(idemWrapper);
        if (exist != null) {
            if (exist.getReleaseStatus() == 0 && exist.getSeatId().equals(seatId)) {
                Map<String, Object> res = new HashMap<>();
                res.put("occupancyId", exist.getId());
                res.put("seatId", seatId);
                res.put("orderId", orderId);
                return res; // 幂等返回
            } else {
                throw new RuntimeException("1003:当前订单状态不允许此操作");
            }
        }

        // 2. 前置校验：座位存不存在？自习室关没关？
        SeatInfo seat = seatInfoMapper.selectById(seatId);
        if (seat == null || seat.getIsDeleted() == 1) {
            throw new RuntimeException("1006:座位不存在");
        }
        RoomInfo room = roomInfoMapper.selectById(seat.getRoomId());
        if (room == null || room.getStatus() == 0) {
            throw new RuntimeException("1006:自习室已关闭");
        }

        // 3. 核心：加行级锁 FOR UPDATE (防止并发超卖)
        seat = seatInfoMapper.selectByIdForUpdate(seatId);

        if ("维护".equals(seat.getStatus()) || "停用".equals(seat.getStatus())) {
            throw new RuntimeException("1006:座位处于维护或停用");
        }

        // 4. 冲突校验：时间有没有重叠？
        LambdaQueryWrapper<SeatOccupancy> conflictWrapper = new LambdaQueryWrapper<>();
        conflictWrapper.eq(SeatOccupancy::getSeatId, seatId)
                .eq(SeatOccupancy::getReleaseStatus, 0)
                .lt(SeatOccupancy::getStartTime, endTime) // 开始时间 < 传入的结束时间
                .gt(SeatOccupancy::getEndTime, startTime); // 结束时间 > 传入的开始时间
        Long count = seatOccupancyMapper.selectCount(conflictWrapper);
        if (count > 0) {
            throw new RuntimeException("1001:座位已被预约");
        }

        // 5. 插入占用记录
        SeatOccupancy occupancy = new SeatOccupancy();
        occupancy.setSeatId(seatId);
        occupancy.setOrderId(orderId);
        occupancy.setStartTime(startTime);
        occupancy.setEndTime(endTime);
        occupancy.setReleaseStatus(0);
        occupancy.setCheckinFlag(0);
        occupancy.setIsDeleted(0);
        seatOccupancyMapper.insert(occupancy);

        // 6. 条件式置已预约
        LocalDateTime now = LocalDateTime.now();
        if (!startTime.isAfter(now) && endTime.isAfter(now)) {
            if ("可用".equals(seat.getStatus())) {
                seat.setStatus("已预约");
                seatInfoMapper.updateById(seat);
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("occupancyId", occupancy.getId());
        res.put("seatId", seatId);
        res.put("orderId", orderId);
        return res;
    }
}