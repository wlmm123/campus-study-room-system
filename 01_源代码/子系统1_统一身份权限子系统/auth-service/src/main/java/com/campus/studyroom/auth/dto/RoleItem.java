package com.campus.studyroom.auth.dto;

import lombok.Data;

/**
 * 角色下拉项（契约 4.1.6）。
 *
 * @author 成员1
 */
@Data
public class RoleItem {

    private Long roleId;

    private String roleName;

    private String roleCode;
}
