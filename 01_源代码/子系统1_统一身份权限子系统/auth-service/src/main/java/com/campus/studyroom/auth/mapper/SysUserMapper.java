package com.campus.studyroom.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.studyroom.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。
 *
 * @author 成员1
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
