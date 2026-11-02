package com.campus.studyroom.auth.controller;

import com.campus.studyroom.auth.common.Result;
import com.campus.studyroom.auth.dto.LoginRequest;
import com.campus.studyroom.auth.dto.LoginResponse;
import com.campus.studyroom.auth.dto.RegisterRequest;
import com.campus.studyroom.auth.dto.UserInfoResponse;
import com.campus.studyroom.auth.service.AuthService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户端鉴权接口（契约 4.1.1 ~ 4.1.3）。
 *
 * @author 成员1
 */
@CrossOrigin
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 4.1.1 学生用户注册 */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        Map<String, Object> data = new HashMap<>(2);
        data.put("userId", userId);
        return Result.ok("注册成功", data);
    }

    /** 4.1.2 用户登录，签发 JWT */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok("登录成功", authService.login(request));
    }

    /** 4.1.3 获取当前登录用户信息 */
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.ok(authService.currentUser());
    }
}
