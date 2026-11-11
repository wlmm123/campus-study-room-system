package com.campus.studyroom.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.studyroom.order.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态流转日志表 Mapper 接口
 * 对应数据库表：order_status_log
 * 依据《软件设计规格说明书》3.2.3
 */
@Mapper
public interface OrderStatusLogMapper extends BaseMapper<OrderStatusLog> {
}