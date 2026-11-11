package com.campus.studyroom.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.studyroom.order.entity.BizConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务配置表 Mapper 接口
 * 对应数据库表：biz_config
 * 依据《软件设计规格说明书》3.2.3
 */
@Mapper
public interface BizConfigMapper extends BaseMapper<BizConfig> {
}