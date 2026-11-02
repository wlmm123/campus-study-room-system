package com.campus.studyroom.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色关联表 sys_user_role。
 *
 * <p>注意：分配角色采用"物理删除旧关联 + 插入新关联"，本表不使用逻辑删除，
 * 读表也不加 is_deleted 过滤（设计文档 3.2.1 说明(3)，避免撞 uk_user_role 唯一索引）。
 *
 * @author 成员1
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roleId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}
