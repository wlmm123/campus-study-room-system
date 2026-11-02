package com.campus.studyroom.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录入参（契约 4.1.2）。
 *
 * @author 成员1
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
