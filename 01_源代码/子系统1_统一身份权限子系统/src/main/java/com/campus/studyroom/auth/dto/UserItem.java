package com.campus.studyroom.auth.dto;

import lombok.Data;

/**
 * 用户列表项（契约 4.1.5）。
 *
 * @author 成员1
 */
@Data
public class UserItem {

    private Long userId;

    private String username;

    private String realName;

    /** 主角色编码：ADMIN &gt; STUDENT */
    private String role;

    /** 1 正常，0 禁用 */
    private Integer status;
}
