package com.campus.studyroom.auth.dto;

import lombok.Data;

/**
 * 登录出参（契约 4.1.2）：token / tokenType / expiresIn / role。
 *
 * @author 成员1
 */
@Data
public class LoginResponse {

    private String token;

    private String tokenType = "Bearer";

    /** 有效期，秒 */
    private long expiresIn;

    /** 主角色，多角色时按 ADMIN &gt; STUDENT */
    private String role;
}
