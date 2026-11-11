package com.campus.studyroom.seat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.studyroom.seat.entity.SeatInfo;
import com.campus.studyroom.seat.entity.SeatOccupancy;
import com.campus.studyroom.seat.mapper.SeatInfoMapper;
import com.campus.studyroom.seat.mapper.SeatOccupancyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SeatReleaseService {

    @Autowired
    private SeatInfoMapper seatInfoMapper;
    @Autowired
    private SeatOccupancyMapper seatOccupancyMapper;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> releaseSeat(Long seatId, Long orderId, String action) {
        // 1. action 校验
        if (!"CANCEL".equals(action) && !"TIMEOUT".equals(action) && !"FINISH".equals(action)) {
            throw new IllegalArgumentException("action参数不合法");
        }

        // 2. 查记录
        LambdaQueryWrapper<SeatOccupancy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeatOccupancy::getOrderId, orderId);
        SeatOccupancy occupancy = seatOccupancyMapper.selectOne(wrapper);

        Map<String, Object> result = new HashMap<>();

        // 幂等：不存在
        if (occupancy == null) {
            result.put("occupancyId", null);
            result.put("seatId", seatId);
            result.put("message", "无待释放占用记录");
            return result;
        }

        // 校验 seatId 是否一致
        if (!occupancy.getSeatId().equals(seatId)) {
            throw new RuntimeException("1003:当前订单状态不允许此操作");
        }

        // 幂等：已释放
        if (occupancy.getReleaseStatus() == 1) {
            result.put("occupancyId", occupancy.getId());
            result.put("seatId", seatId);
            result.put("message", "座位已释放");
            return result;
        }

        // 3. 置释放状态
        occupancy.setReleaseStatus(1);
        occupancy.setReleaseTime(LocalDateTime.now());
        occupancy.setAction(action);
        seatOccupancyMapper.updateById(occupancy);

        // 4. 三档回落
        SeatInfo seat = seatInfoMapper.selectById(seatId);
        String currentStatus = seat.getStatus();

        // 第一档：维护/停用 -> 保持
        if ("维护".equals(currentStatus) || "停用".equals(currentStatus)) {
            result.put("seatStatus", currentStatus);
            result.put("occupancyId", occupancy.getId());
            result.put("seatId", seatId);
            return result;
        }

        // 查是否有其他覆盖当前时刻的未释放记录
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SeatOccupancy> coverWrapper = new LambdaQueryWrapper<>();
        coverWrapper.eq(SeatOccupancy::getSeatId, seatId)
                .eq(SeatOccupancy::getReleaseStatus, 0)
                .le(SeatOccupancy::getStartTime, now)
                .gt(SeatOccupancy::getEndTime, now);
        SeatOccupancy covering = seatOccupancyMapper.selectOne(coverWrapper);

        if (covering != null) {
            // 第二档：有其他覆盖记录
            if (covering.getCheckinFlag() == 1) {
                // 人已到，保持原状态不变
                result.put("seatStatus", currentStatus);
            } else {
                // 人未到，置已预约
                seat.setStatus("已预约");
                seatInfoMapper.updateById(seat);
                result.put("seatStatus", "已预约");
            }
            result.put("coveringOrderId", covering.getOrderId());
        } else {
            // 第三档：没有覆盖，回可用
            seat.setStatus("可用");
            seatInfoMapper.updateById(seat);
            result.put("seatStatus", "可用");
        }

        result.put("occupancyId", occupancy.getId());
        result.put("seatId", seatId);
        return result;
    }
}