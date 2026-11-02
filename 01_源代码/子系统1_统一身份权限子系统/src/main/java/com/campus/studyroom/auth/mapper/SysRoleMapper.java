package com.campus.studyroom.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.studyroom.auth.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色表 Mapper。
 *
 * @author 成员1
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询某用户绑定的全部角色编码（按角色 id 升序）。
     *
     * @param userId 用户 ID
     * @return 角色编码列表，如 ["STUDENT"]
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
