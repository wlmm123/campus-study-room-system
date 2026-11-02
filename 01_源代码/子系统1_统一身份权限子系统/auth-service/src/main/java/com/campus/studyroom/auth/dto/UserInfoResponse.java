package com.campus.studyroom.auth.dto;

import lombok.Data;

/**
 * 当前用户信息出参（契约 4.1.3）。
 *
 * @author 成员1
 */
@Data
public class UserInfoResponse {

    private Long userId;

    private String username;

    private String realName;

    private String role;
}
