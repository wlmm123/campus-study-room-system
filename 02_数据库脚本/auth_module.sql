-- ==========================================================
-- 校园自习室分布式预约管理系统 - 身份权限子系统独立脚本
-- 负责人：成员1
-- 版本：V1.2（按成员2审核意见修订）
-- 说明：遵循全局数据模型规范，主键统一 BIGINT 自增，包含公共字段，无物理外键，无跨库依赖。
-- ==========================================================

-- 0. 创建数据库并使用（防止合并时找不到库；不用 DROP 防止误删别人数据）
CREATE DATABASE IF NOT EXISTS `auth_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `auth_db`;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号（学号），全局唯一',
  `password` VARCHAR(100) NOT NULL COMMENT '加密密码（BCrypt）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `status` TINYINT DEFAULT 1 COMMENT '账号状态: 1正常, 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0未删, 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 2. 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- 3. 用户角色关联表
-- 分配角色逻辑说明：统一执行“物理删除该用户旧关联 + 插入新关联”，并在同一事务内完成；
-- 此表保留 is_deleted 字段仅为符合全局规范，实际分配角色流程不使用逻辑删除，读表时也无需加 is_deleted 过滤。
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（分配角色流程不使用）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 4. 初始化角色数据
INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_code`) VALUES 
(1, '学生', 'STUDENT'),
(2, '管理员', 'ADMIN');

-- 5. 初始化账号数据
-- 以下密文已通过在线 BCrypt 工具生成并反向验证匹配，明文均为：Password123!
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`) VALUES 
(1, '20230001', '$2a$10$hEks75ip5cNhEizuQAOmqe2KHYxZxNG52j/MAzvjOsZgc7EAwN3W.', '张三', 1),
(2, 'admin',    '$2a$10$XQq1c0p/NEo.TTOXp1mKlObqTr3C44oZ9QN/1P2FX6oFTt3/isege', '系统管理员', 1);

-- 6. 绑定角色
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES 
(1, 1),
(2, 2);