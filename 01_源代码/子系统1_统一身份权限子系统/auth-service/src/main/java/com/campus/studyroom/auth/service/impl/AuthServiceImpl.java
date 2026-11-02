package com.campus.studyroom.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.studyroom.auth.common.BizCode;
import com.campus.studyroom.auth.common.BusinessException;
import com.campus.studyroom.auth.common.PageResult;
import com.campus.studyroom.auth.config.JwtProperties;
import com.campus.studyroom.auth.dto.LoginRequest;
import com.campus.studyroom.auth.dto.LoginResponse;
import com.campus.studyroom.auth.dto.RegisterRequest;
import com.campus.studyroom.auth.dto.RoleItem;
import com.campus.studyroom.auth.dto.UserInfoResponse;
import com.campus.studyroom.auth.dto.UserItem;
import com.campus.studyroom.auth.dto.UserRoleRow;
import com.campus.studyroom.auth.entity.SysRole;
import com.campus.studyroom.auth.entity.SysUser;
import com.campus.studyroom.auth.entity.SysUserRole;
import com.campus.studyroom.auth.mapper.SysRoleMapper;
import com.campus.studyroom.auth.mapper.SysUserMapper;
import com.campus.studyroom.auth.mapper.SysUserRoleMapper;
import com.campus.studyroom.auth.security.JwtUtil;
import com.campus.studyroom.auth.security.RoleCode;
import com.campus.studyroom.auth.security.UserContext;
import com.campus.studyroom.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 身份权限业务实现。
 *
 * @author 成员1
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(SysUserMapper userMapper,
                           SysRoleMapper roleMapper,
                           SysUserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           JwtProperties jwtProperties) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (username.isEmpty()) {
            throw new BusinessException(BizCode.BAD_REQUEST, "学号不能为空");
        }

        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BusinessException(BizCode.USERNAME_EXISTS, "账号（学号）已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setStatus(1);
        userMapper.insert(user);

        // 默认绑定学生角色：角色 ID 从 sys_role 查，不硬编码 1
        SysRole studentRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, RoleCode.STUDENT));
        if (studentRole == null) {
            throw new BusinessException(BizCode.INTERNAL_ERROR, "系统角色 STUDENT 未初始化，请先执行 auth_module.sql");
        }
        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(studentRole.getId());
        userRoleMapper.insert(relation);

        log.info("注册成功: userId={}, username={}", user.getId(), username);
        return user.getId();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        // 账号不存在与密码错误返回同一错误码，避免泄露账号是否存在（契约 1.2 / 2001）
        if (user == null) {
            throw new BusinessException(BizCode.ACCOUNT_OR_PASSWORD_ERROR, "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(BizCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(BizCode.ACCOUNT_OR_PASSWORD_ERROR, "账号或密码错误");
        }

        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        if (roles == null || roles.isEmpty()) {
            log.warn("用户 {} 未绑定任何角色，按 STUDENT 兜底签发", user.getUsername());
            roles = Collections.singletonList(RoleCode.STUDENT);
        }

        String token = jwtUtil.generateToken(user.getId(), roles);
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtProperties.getExpireSeconds());
        response.setRole(JwtUtil.primaryRole(roles));
        log.info("登录成功: userId={}, roles={}", user.getId(), roles);
        return response;
    }

    @Override
    public UserInfoResponse currentUser() {
        Long userId = UserContext.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.UNAUTHORIZED, "用户不存在或已删除");
        }
        List<String> roles = roleMapper.selectRoleCodesByUserId(userId);

        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(JwtUtil.primaryRole(roles));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRole(Long userId, Long roleId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.BAD_REQUEST, "用户不存在");
        }
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(BizCode.BAD_REQUEST, "角色不存在");
        }
        // 物理删除旧关联再插入新关联（设计文档 3.2.1 说明(3)），避免 uk_user_role 冲突
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));

        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        userRoleMapper.insert(relation);
        log.info("分配角色成功: userId={}, roleCode={}", userId, role.getRoleCode());
    }

    @Override
    public PageResult<UserItem> pageUsers(int pageNum, int pageSize, String username) {
        if (pageNum < 1 || pageSize < 1) {
            throw new BusinessException(BizCode.BAD_REQUEST, "分页参数必须大于 0");
        }
        int safePageSize = Math.min(pageSize, 100);

        Page<SysUser> page = new Page<>(pageNum, safePageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.trim().isEmpty()) {
            wrapper.like(SysUser::getUsername, username.trim());
        }
        wrapper.orderByAsc(SysUser::getId);
        Page<SysUser> userPage = userMapper.selectPage(page, wrapper);

        List<SysUser> records = userPage.getRecords();
        Map<Long, List<String>> roleMap = loadRoles(records);

        List<UserItem> items = new ArrayList<>(records.size());
        for (SysUser user : records) {
            UserItem item = new UserItem();
            item.setUserId(user.getId());
            item.setUsername(user.getUsername());
            item.setRealName(user.getRealName());
            item.setStatus(user.getStatus());
            List<String> roles = roleMap.getOrDefault(user.getId(), Collections.emptyList());
            item.setRole(roles.isEmpty() ? null : JwtUtil.primaryRole(roles));
            items.add(item);
        }
        return new PageResult<>(userPage.getTotal(), pageNum, safePageSize, items);
    }

    @Override
    public List<RoleItem> listRoles() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getId));
        List<RoleItem> items = new ArrayList<>(roles.size());
        for (SysRole role : roles) {
            RoleItem item = new RoleItem();
            item.setRoleId(role.getId());
            item.setRoleName(role.getRoleName());
            item.setRoleCode(role.getRoleCode());
            items.add(item);
        }
        return items;
    }

    /** 一次查询拿到整页用户的角色，避免逐条查库 */
    private Map<Long, List<String>> loadRoles(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = new ArrayList<>(users.size());
        for (SysUser user : users) {
            ids.add(user.getId());
        }
        List<UserRoleRow> rows = userRoleMapper.selectRoleRowsByUserIds(ids);
        Map<Long, List<String>> map = new HashMap<>();
        for (UserRoleRow row : rows) {
            map.computeIfAbsent(row.getUserId(), k -> new ArrayList<>()).add(row.getRoleCode());
        }
        return map;
    }
}
