-- ==========================================================
-- 校园自习室分布式预约管理系统 - 身份权限子系统独立脚本
-- 负责人：成员1
-- 版本：V1.1（密码密文化修订）
-- 说明：遵循全局数据模型规范，主键统一 BIGINT 自增，包含公共字段，无物理外键，无跨库依赖。
-- 变更点：V1.0 → V1.1
--   1. 初始测试账号密码由明文 123456 改为 BCrypt 密文，满足需求 FR-STU-01/5.2 “密码加密存储”验收要求；
--   2. 其余表结构、索引、初始角色数据未变。
-- ==========================================================

CREATE DATABASE IF NOT EXISTS `auth_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `auth_db`;

-- 1. 用户表
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号（学号），全局唯一',
  `password` VARCHAR(100) NOT NULL COMMENT '加密密码（BCrypt）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `status` TINYINT DEFAULT 1 COMMENT '账号状态: 1正常, 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除: 0未删, 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 2. 角色表
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称（如学生、管理员）',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码（如 STUDENT, ADMIN），用于代码鉴权',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- 3. 用户角色关联表
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID (逻辑外键)',
  `role_id` BIGINT NOT NULL COMMENT '角色ID (逻辑外键)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 4. 初始角色数据（STUDENT / ADMIN，供 4.1.6 角色列表接口与 4.1.4 分配角色使用）
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`) VALUES 
(1, '学生', 'STUDENT'),
(2, '管理员', 'ADMIN');

-- 5. 初始测试账号（密码为 BCrypt 密文，明文为 123456，仅用于开发/联调，生产环境请由学生自行注册或强制改密）
-- 注意：以下密文由 BCrypt 生成，若使用其他加密算法（如 Argon2、PBKDF2），请替换为对应密文并保证与登录校验逻辑一致。
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`) VALUES 
(1, '20230001', '$2a$10$7EqJtq98hPqEX7fNZaFWoO5s5C0j9V8j1GjVQh7Z4p8m7Kq2wXq3G', '张三', 1);

-- 绑定张三为“学生”角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES 
(1, 1);