package com.campus.studyroom.auth.dto;

import lombok.Data;

/**
 * 批量查询用户角色时的行结果。
 *
 * @author 成员1
 */
@Data
public class UserRoleRow {

    private Long userId;

    private String roleCode;
}
