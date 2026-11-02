package com.campus.studyroom.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.studyroom.auth.dto.UserRoleRow;
import com.campus.studyroom.auth.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联表 Mapper（批量查询用，避免用户列表 N+1 查询）。
 *
 * @author 成员1
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 批量查询多个用户的角色编码。
     *
     * @param userIds 用户 ID 列表（非空）
     * @return 每条记录为 userId + roleCode
     */
    List<UserRoleRow> selectRoleRowsByUserIds(@Param("userIds") List<Long> userIds);
}
