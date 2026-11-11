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
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

// 【修复】去掉了 properties = "spring.profiles.active=local"
@SpringBootTest
@Transactional
public class OrderServiceBusinessTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    // 使用 @MockBean 模拟远程服务，防止测试时因为没启动 8082/8084 导致报错
    @MockBean
    private SeatClient seatClient;

    @MockBean
    private NotifyClient notifyClient;

    @BeforeEach
    public void setUp() {
        orderInfoMapper.delete(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getUserId, 1L));
        // 打桩：模拟锁座成功
        String successJson = "{\"code\":200, \"message\":\"ok\"}";
        Mockito.when(seatClient.lockSeat(Mockito.anyLong(), Mockito.any())).thenReturn(successJson);
        Mockito.when(seatClient.updateStatus(Mockito.anyLong(), Mockito.any())).thenReturn(successJson);
        Mockito.when(seatClient.releaseSeat(Mockito.anyLong(), Mockito.any())).thenReturn(successJson);
        Mockito.when(notifyClient.pushMessage(Mockito.any())).thenReturn(successJson);
    }

    /**
     * 测试 4.2.1：正向预约下单成功
     */
    @Test
    public void testCreateOrderSuccess() {
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest();
        request.setSeatId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        request.setDurationHours(2);

        Result result = orderService.createOrder(1L, request);
        assertEquals(200, result.getCode(), "正向预约应该成功");
        
        // 验证数据库里多了一条待签到的订单
        OrderInfo order = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getUserId, 1L));
        assertNotNull(order);
        assertEquals("待签到", order.getStatus());
    }

    /**
     * 测试 4.2.1(4)：同一学生时段不重叠（1008）
     */
    @Test
    public void testCreateOrderOverlap1008() {
        // 模拟已有预约
        OrderInfo existingOrder = new OrderInfo();
        existingOrder.setId(99999L);
        existingOrder.setUserId(1L);
        existingOrder.setSeatId(1L);
        existingOrder.setStatus("待签到");
        existingOrder.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        existingOrder.setEndTime(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0));
        orderInfoMapper.insert(existingOrder);

        // 发起冲突预约
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest();
        request.setSeatId(2L);
        request.setStartTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        request.setDurationHours(2);

        Result result = orderService.createOrder(1L, request);
        assertEquals(1008, result.getCode(), "时段重叠必须返回 1008");
    }

    /**
     * 测试 4.2.1(1)：预约参数不在可选范围（1002）
     */
    @Test
    public void testCreateOrderInvalidParam1002() {
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest();
        request.setSeatId(1L);
        request.setDurationHours(99);
        request.setStartTime("2026-12-01 10:00:00");

        Result result = orderService.createOrder(1L, request);
        assertEquals(1002, result.getCode(), "非法档位必须返回 1002");
    }

    /**
     * 测试 4.2.12：定时任务扫描，超时未签到释放
     */
    @Test
    public void testBatchHandleTimeout() {
        // 构造一条已超时的待签到订单（开始时间是 1 小时前，铁定超时）
        OrderInfo order = new OrderInfo();
        order.setId(55555L);
        order.setUserId(1L);
        order.setSeatId(1L);
        order.setStatus("待签到");
        order.setStartTime(LocalDateTime.now().minusHours(1));
        order.setEndTime(LocalDateTime.now().plusHours(1));
        orderInfoMapper.insert(order);

        // 触发扫描
        Result result = orderService.batchHandleTimeout();
        assertNotNull(result);

        // 重新查一下状态，断言必须变为 超时释放
        OrderInfo updatedOrder = orderInfoMapper.selectById(55555L);
        assertEquals("超时释放", updatedOrder.getStatus(), "超时订单应被扫描释放");
    }
}