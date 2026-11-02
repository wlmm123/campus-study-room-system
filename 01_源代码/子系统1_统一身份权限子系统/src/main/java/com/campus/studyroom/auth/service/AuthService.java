package com.campus.studyroom.auth.service;

import com.campus.studyroom.auth.common.PageResult;
import com.campus.studyroom.auth.dto.LoginRequest;
import com.campus.studyroom.auth.dto.LoginResponse;
import com.campus.studyroom.auth.dto.RegisterRequest;
import com.campus.studyroom.auth.dto.RoleItem;
import com.campus.studyroom.auth.dto.UserInfoResponse;
import com.campus.studyroom.auth.dto.UserItem;

import java.util.List;

/**
 * 身份权限业务接口。
 *
 * @author 成员1
 */
public interface AuthService {

    /**
     * 学生注册：账号唯一、密码 BCrypt 加密、默认绑定 STUDENT 角色。
     *
     * @param request 注册入参
     * @return 新用户 ID
     */
    Long register(RegisterRequest request);

    /**
     * 登录并签发 JWT（Claims 含 roles 数组）。
     *
     * @param request 登录入参
     * @return token 及主角色
     */
    LoginResponse login(LoginRequest request);

    /**
     * 查询当前登录用户信息。
     *
     * @return 用户信息
     */
    UserInfoResponse currentUser();

    /**
     * 管理员分配用户角色：物理删除旧关联 + 插入新关联，同一事务。
     *
     * @param userId 被修改的用户 ID
     * @param roleId 目标角色 ID
     */
    void assignRole(Long userId, Long roleId);

    /**
     * 管理员分页查询用户列表。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @param username 学号模糊搜索，可为空
     * @return 分页结果
     */
    PageResult<UserItem> pageUsers(int pageNum, int pageSize, String username);

    /**
     * 查询全部可选角色，按 id 升序。
     *
     * @return 角色列表
     */
    List<RoleItem> listRoles();
}
