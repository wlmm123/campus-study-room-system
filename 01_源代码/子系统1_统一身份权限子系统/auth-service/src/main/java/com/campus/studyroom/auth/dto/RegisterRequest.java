package com.campus.studyroom.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 注册入参（契约 4.1.1）。
 *
 * @author 成员1
 */
@Data
public class RegisterRequest {

    /** 学号，作为登录账号，全局唯一 */
    @NotBlank(message = "学号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;
}
