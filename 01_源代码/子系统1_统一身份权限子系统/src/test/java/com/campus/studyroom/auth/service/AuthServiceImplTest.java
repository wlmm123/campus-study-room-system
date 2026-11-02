package com.campus.studyroom.auth.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.campus.studyroom.auth.common.BizCode;
import com.campus.studyroom.auth.common.BusinessException;
import com.campus.studyroom.auth.config.JwtProperties;
import com.campus.studyroom.auth.dto.LoginRequest;
import com.campus.studyroom.auth.dto.LoginResponse;
import com.campus.studyroom.auth.dto.RegisterRequest;
import com.campus.studyroom.auth.entity.SysRole;
import com.campus.studyroom.auth.entity.SysUser;
import com.campus.studyroom.auth.mapper.SysRoleMapper;
import com.campus.studyroom.auth.mapper.SysUserMapper;
import com.campus.studyroom.auth.mapper.SysUserRoleMapper;
import com.campus.studyroom.auth.security.JwtUtil;
import com.campus.studyroom.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * 身份权限业务分支单元测试（登录、注册、异常口径）。
 *
 * @author 成员1
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    private JwtProperties jwtProperties;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setExpireSeconds(7200L);
        authService = new AuthServiceImpl(userMapper, roleMapper, userRoleMapper,
                passwordEncoder, jwtUtil, jwtProperties);
    }

    private SysUser student() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("20230001");
        user.setPassword("$2a$10$encoded");
        user.setRealName("张三");
        user.setStatus(1);
        return user;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("20230001");
        request.setPassword("Password123!");
        return request;
    }

    @Test
    @DisplayName("登录成功：签发 Token 并返回主角色 STUDENT、expiresIn=7200")
    void loginShouldReturnTokenAndRole() {
        when(userMapper.selectOne(any())).thenReturn(student());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(roleMapper.selectRoleCodesByUserId(1L)).thenReturn(Collections.singletonList("STUDENT"));
        when(jwtUtil.generateToken(anyLong(), any())).thenReturn("mock-token");

        LoginResponse response = authService.login(loginRequest());

        assertEquals("mock-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(7200L, response.getExpiresIn());
        assertEquals("STUDENT", response.getRole());
    }

    @Test
    @DisplayName("密码错误：业务码 2001")
    void loginShouldFailWith2001WhenPasswordWrong() {
        when(userMapper.selectOne(any())).thenReturn(student());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, () -> authService.login(loginRequest()));
        assertEquals(BizCode.ACCOUNT_OR_PASSWORD_ERROR, e.getCode());
    }

    @Test
    @DisplayName("账号不存在：与密码错误同码 2001，避免泄露账号是否存在")
    void loginShouldFailWith2001WhenUserAbsent() {
        when(userMapper.selectOne(any())).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class, () -> authService.login(loginRequest()));
        assertEquals(BizCode.ACCOUNT_OR_PASSWORD_ERROR, e.getCode());
    }

    @Test
    @DisplayName("账号被禁用：业务码 2002")
    void loginShouldFailWith2002WhenDisabled() {
        SysUser user = student();
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);

        BusinessException e = assertThrows(BusinessException.class, () -> authService.login(loginRequest()));
        assertEquals(BizCode.ACCOUNT_DISABLED, e.getCode());
    }

    @Test
    @DisplayName("学号重复注册：业务码 2003")
    void registerShouldFailWith2003WhenUsernameExists() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("20230001");
        request.setPassword("Password123!");
        request.setRealName("张三");

        BusinessException e = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals(BizCode.USERNAME_EXISTS, e.getCode());
    }

    @Test
    @DisplayName("注册成功：默认绑定 STUDENT 角色并返回新用户 ID")
    void registerShouldBindStudentRole() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        SysRole studentRole = new SysRole();
        studentRole.setId(1L);
        studentRole.setRoleCode("STUDENT");
        when(roleMapper.selectOne(any())).thenReturn(studentRole);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("20239999");
        request.setPassword("Password123!");
        request.setRealName("李四");

        Long userId = authService.register(request);

        assertNotNull(userId);
        assertEquals(10L, userId);
    }
}
