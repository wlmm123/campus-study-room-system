package com.campus.studyroom.auth.controller;

import com.campus.studyroom.auth.common.PageResult;
import com.campus.studyroom.auth.common.Result;
import com.campus.studyroom.auth.dto.RoleAssignRequest;
import com.campus.studyroom.auth.dto.RoleItem;
import com.campus.studyroom.auth.dto.UserItem;
import com.campus.studyroom.auth.service.AuthService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

/**
 * 管理员用户与角色管理接口（契约 4.1.4 ~ 4.1.6）。
 *
 * <p>/api/admin/** 由 AdminInterceptor 统一校验 ADMIN 角色，非管理员返回 403。
 *
 * @author 成员1
 */
@CrossOrigin
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    /** 4.1.4 管理员分配用户角色 */
    @PutMapping("/users/{userId}/role")
    public Result<Object> assignRole(@PathVariable("userId") Long userId,
                                     @Valid @RequestBody RoleAssignRequest request) {
        authService.assignRole(userId, request.getRoleId());
        return Result.ok("角色分配成功", Collections.emptyMap());
    }

    /** 4.1.5 管理员分页查询用户列表 */
    @GetMapping("/users")
    public Result<PageResult<UserItem>> pageUsers(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "username", required = false) String username) {
        return Result.ok(authService.pageUsers(pageNum, pageSize, username));
    }

    /** 4.1.6 管理员查询角色列表 */
    @GetMapping("/roles")
    public Result<List<RoleItem>> listRoles() {
        return Result.ok("查询成功", authService.listRoles());
    }
}
