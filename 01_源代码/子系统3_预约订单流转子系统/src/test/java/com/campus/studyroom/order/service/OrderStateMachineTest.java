package com.campus.studyroom.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.studyroom.order.common.Result;
import com.campus.studyroom.order.entity.OrderInfo;
import com.campus.studyroom.order.mapper.OrderInfoMapper;
import com.campus.studyroom.order.remote.NotifyClient;
import com.campus.studyroom.order.remote.SeatClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

// 【修复】去掉了 properties = "spring.profiles.active=local"
@SpringBootTest
@Transactional
public class OrderStateMachineTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @MockBean
    private SeatClient seatClient;

    @MockBean
    private NotifyClient notifyClient;

    @BeforeEach
    public void setUp() {
        orderInfoMapper.delete(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getUserId, 1L));
        String successJson = "{\"code\":200, \"message\":\"ok\"}";
        Mockito.when(seatClient.updateStatus(Mockito.anyLong(), Mockito.any())).thenReturn(successJson);
        Mockito.when(seatClient.releaseSeat(Mockito.anyLong(), Mockito.any())).thenReturn(successJson);
        Mockito.when(notifyClient.pushMessage(Mockito.any())).thenReturn(successJson);
    }

    /**
     * 测试 4.2.2~4.2.6：正向状态机迁移（签到 -> 暂离 -> 主动离开）
     */
    @Test
    public void testFullStateFlowSuccess() {
        // 由于配置的签到截止时间较短，开始时间设为当前时间的 10 秒前，确保在窗口内。
        LocalDateTime testStartTime = LocalDateTime.now().minusSeconds(10);
        LocalDateTime testEndTime = LocalDateTime.now().plusMinutes(10);

        OrderInfo order = new OrderInfo();
        order.setId(88888L);
        order.setUserId(1L);
        order.setSeatId(1L);
        order.setStatus("待签到");
        order.setStartTime(testStartTime);
        order.setEndTime(testEndTime);
        orderInfoMapper.insert(order);

        // 1. 签到
        Result signInRes = orderService.signIn(1L, 88888L);
        assertEquals(200, signInRes.getCode(), "签到应成功");

        // 2. 暂离
        Result tempLeaveRes = orderService.tempLeave(1L, 88888L);
        assertEquals(200, tempLeaveRes.getCode(), "暂离应成功");

        // 3. 主动离开
        Result leaveRes = orderService.leave(1L, 88888L);
        assertEquals(200, leaveRes.getCode(), "主动离开应成功");
        
        // 4. 验证最终数据库状态
        assertEquals("已完成", orderInfoMapper.selectById(88888L).getStatus(), "订单状态应变为已完成");
    }

    /**
     * 测试 4.2.3(2)：非待签到状态不能取消订单（1003）
     */
    @Test
    public void testCancelWrongState1003() {
        OrderInfo order = new OrderInfo();
        order.setId(88888L);
        order.setUserId(1L);
        order.setSeatId(1L);
        order.setStatus("已签到");
        order.setStartTime(LocalDateTime.now());
        order.setEndTime(LocalDateTime.now().plusMinutes(2));
        orderInfoMapper.insert(order);

        Result result = orderService.cancel(1L, 88888L, false);
        assertEquals(1003, result.getCode(), "非待签到状态取消必须返回 1003");
    }
}