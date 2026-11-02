package com.campus.studyroom.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 分配角色入参（契约 4.1.4）。
 *
 * @author 成员1
 */
@Data
public class RoleAssignRequest {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;
}
