package com.campus.studyroom.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.studyroom.order.entity.Violation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 违约记录表 Mapper 接口
 * 对应数据库表：violation
 * 依据《软件设计规格说明书》3.2.3
 */
@Mapper
public interface ViolationMapper extends BaseMapper<Violation> {
}