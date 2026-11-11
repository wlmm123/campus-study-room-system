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

@Service
public class SeatStatusService {

    @Autowired
    private SeatInfoMapper seatInfoMapper;
    @Autowired
    private SeatOccupancyMapper seatOccupancyMapper;

    @Transactional(rollbackFor = Exception.class)
    public void updateSeatStatus(Long seatId, Long orderId, String status) {
        // 1. 校验 status 合法性
        if (!"已预约".equals(status) && !"已占用".equals(status) && !"暂离中".equals(status)) {
            throw new IllegalArgumentException("状态参数不合法");
        }

        // 2. 校验占用记录是否存在且未释放
        LambdaQueryWrapper<SeatOccupancy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeatOccupancy::getOrderId, orderId)
                .eq(SeatOccupancy::getReleaseStatus, 0);
        SeatOccupancy occupancy = seatOccupancyMapper.selectOne(wrapper);

        if (occupancy == null || !occupancy.getSeatId().equals(seatId)) {
            throw new RuntimeException("1003:当前订单状态不允许此操作");
        }

        // 3. 查询当前座位状态
        SeatInfo seatInfo = seatInfoMapper.selectById(seatId);
        if (seatInfo == null) {
            throw new RuntimeException("1006:座位不存在");
        }

        // 4. 防止盲目覆盖
        if ("已预约".equals(status)) {
            // 如果当前已经是“已占用”或“暂离中”，说明人已经来了，不要被定时任务强行覆盖
            if ("已占用".equals(seatInfo.getStatus()) || "暂离中".equals(seatInfo.getStatus())) {
                return; // 直接结束，不报错，也不改状态
            }
        }

        // 5. 更新座位状态
        seatInfo.setStatus(status);
        seatInfoMapper.updateById(seatInfo);

        // 6. 传已占用/暂离中时，更新 checkin_flag 为 1
        if ("已占用".equals(status) || "暂离中".equals(status)) {
            LambdaUpdateWrapper<SeatOccupancy> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SeatOccupancy::getOrderId, orderId)
                    .set(SeatOccupancy::getCheckinFlag, 1);
            seatOccupancyMapper.update(null, updateWrapper);
        }
    }
}