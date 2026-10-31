-- ==========================================================
-- 校园自习室分布式预约管理系统 - 节点A 初始化脚本
-- 包含数据库：auth_db（身份）、room_seat_db（座位）
-- 执行节点：节点A (宿主机)
-- 注意：已去除 DROP TABLE 语句，全部采用 CREATE TABLE IF NOT EXISTS，防止重复执行误清空数据。
-- ==========================================================

SET NAMES utf8mb4;

-- 模块1：身份权限子系统 (负责人：成员1)
-- ==========================================================
CREATE DATABASE IF NOT EXISTS `auth_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `auth_db`;

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

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_code`) VALUES 
(1, '学生', 'STUDENT'),
(2, '管理员', 'ADMIN');

-- 密码密文对应明文均为：Password123!
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`) VALUES 
(1, '20230001', '$2a$10$hEks75ip5cNhEizuQAOmqe2KHYxZxNG52j/MAzvjOsZgc7EAwN3W.', '张三', 1),
(2, 'admin',    '$2a$10$XQq1c0p/NEo.TTOXp1mKlObqTr3C44oZ9QN/1P2FX6oFTt3/isege', '系统管理员', 1);

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES 
(1, 1),
(2, 2);


-- 模块2：座位资源子系统 (负责人：成员2)
-- ==========================================================
CREATE DATABASE IF NOT EXISTS `room_seat_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `room_seat_db`;

CREATE TABLE IF NOT EXISTS `room_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_name` VARCHAR(50) NOT NULL COMMENT '自习室名称',
  `location` VARCHAR(100) COMMENT '位置描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1开放, 0关闭',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0未删, 1已删',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室信息表';

CREATE TABLE IF NOT EXISTS `seat_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_id` BIGINT NOT NULL COMMENT '归属自习室ID (逻辑外键)',
  `seat_number` VARCHAR(20) NOT NULL COMMENT '座位号(如: A-01)',
  `status` VARCHAR(20) NOT NULL DEFAULT '可用' COMMENT '状态: 可用, 已预约, 已占用, 暂离中, 维护, 停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_seat` (`room_id`, `seat_number`),
  KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位信息表';

CREATE TABLE IF NOT EXISTS `seat_occupancy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `seat_id` BIGINT NOT NULL COMMENT '座位ID (逻辑外键)',
  `order_id` BIGINT NOT NULL COMMENT '订单ID (逻辑外键，一单一座)',
  `start_time` DATETIME NOT NULL COMMENT '占用开始时间',
  `end_time` DATETIME NOT NULL COMMENT '占用结束时间',
  `release_status` TINYINT DEFAULT 0 COMMENT '逻辑释放状态: 0未释放(锁定中), 1已释放',
  `release_time` DATETIME DEFAULT NULL COMMENT '释放时间',
  `action` VARCHAR(20) DEFAULT NULL COMMENT '释放原因(CANCEL/TIMEOUT/FINISH)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_seat_start_time` (`seat_id`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位时段记录表';

INSERT IGNORE INTO `room_info` (`id`, `room_name`, `location`, `status`) VALUES 
(1, '第一自习室', '图书馆一楼', 1),
(2, '第二自习室', '图书馆二楼', 1);

INSERT IGNORE INTO `seat_info` (`id`, `room_id`, `seat_number`, `status`) VALUES 
(1, 1, 'A-01', '可用'),
(2, 1, 'A-02', '可用'),
(3, 1, 'A-03', '可用'),
(4, 2, 'B-01', '可用');