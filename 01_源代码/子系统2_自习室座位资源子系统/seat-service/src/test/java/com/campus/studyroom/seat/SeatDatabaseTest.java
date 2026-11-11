package com.campus.studyroom.seat;

import com.campus.studyroom.seat.entity.SeatInfo;
import com.campus.studyroom.seat.entity.SeatOccupancy;
import com.campus.studyroom.seat.mapper.SeatInfoMapper;
import com.campus.studyroom.seat.mapper.SeatOccupancyMapper;
import com.campus.studyroom.seat.service.SeatReleaseService;
import com.campus.studyroom.seat.service.SeatStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实数据库集成测试类
 * 目的：验证代码不仅能通过纯逻辑，还能真实地与 MySQL 数据库交互并落库。
 */
@SpringBootTest
public class SeatDatabaseTest {

    @Autowired
    private SeatInfoMapper seatInfoMapper;
    @Autowired
    private SeatOccupancyMapper seatOccupancyMapper;
    @Autowired
    private SeatStatusService seatStatusService;
    @Autowired
    private SeatReleaseService seatReleaseService;

    @Test
    public void testRealDatabaseOperations() {
        Long seatId = 1L;
        Long orderId = 88888888L;

        System.out.println("========== 开始真实数据库集成测试 ==========");

        // 1. 准备数据：确保座位1是可用状态
        SeatInfo seat = seatInfoMapper.selectById(seatId);
        if (seat == null) {
            seat = new SeatInfo();
            seat.setId(seatId);
            seat.setRoomId(1L);
            seat.setSeatNumber("A-01");
            seat.setStatus("可用");
            seatInfoMapper.insert(seat);
        } else {
            seat.setStatus("可用");
            seatInfoMapper.updateById(seat);
        }

        // 清理旧的测试数据，防止主键冲突
        seatOccupancyMapper.deleteById(1L);

        // 插入一条真实的占用记录（模拟锁座成功）
        SeatOccupancy occupancy = new SeatOccupancy();
        occupancy.setId(1L);
        occupancy.setSeatId(seatId);
        occupancy.setOrderId(orderId);
        occupancy.setStartTime(LocalDateTime.now().minusHours(1));
        occupancy.setEndTime(LocalDateTime.now().plusHours(1));
        occupancy.setReleaseStatus(0);
        occupancy.setCheckinFlag(0);
        occupancy.setIsDeleted(0);
        seatOccupancyMapper.insert(occupancy);

        // 2. 测试 【状态同步与防覆盖】（模拟用户签到操作）
        System.out.println("-> 执行状态同步（模拟用户签到，置座位为已占用）...");
        seatStatusService.updateSeatStatus(seatId, orderId, "已占用");

        // 验证真实数据库里的签到标记是否变成 1
        SeatOccupancy updatedOcc = seatOccupancyMapper.selectById(1L);
        assertEquals(1, updatedOcc.getCheckinFlag(), "数据库中的签到标记（checkin_flag）应真实变为 1");

        SeatInfo seatAfterSync = seatInfoMapper.selectById(seatId);
        assertEquals("已占用", seatAfterSync.getStatus(), "数据库中的座位状态应真实变为已占用");
        System.out.println("✅ 状态同步真实落库通过！");

        // 3. 测试 【释放座位与三档回落】（模拟用户离开释放）
        System.out.println("-> 执行释放座位（模拟用户离开，置座位为可用）...");
        seatReleaseService.releaseSeat(seatId, orderId, "FINISH");

        // 验证真实数据库里的释放状态是否变成 1
        SeatOccupancy releasedOcc = seatOccupancyMapper.selectById(1L);
        assertEquals(1, releasedOcc.getReleaseStatus(), "数据库中的释放状态（release_status）应变为 1");
        assertEquals("FINISH", releasedOcc.getAction(), "数据库中的释放动作（action）应记录为 FINISH");

        // 验证座位状态真实回落
        SeatInfo seatAfterRelease = seatInfoMapper.selectById(seatId);
        assertEquals("可用", seatAfterRelease.getStatus(), "数据库中的座位状态应真实回落为可用");

        System.out.println("✅ 释放座位与三档回落真实落库通过！");
        System.out.println("========== 真实数据库集成测试全部通过 ==========");
    }
}