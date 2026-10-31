-- ==========================================================
-- 校园自习室分布式预约管理系统 - 单机全量归档备份脚本
-- 警告：仅供单机归档备份，双节点环境禁止执行此脚本！
-- 双节点请分别使用 init_nodeA.sql 和 init_nodeB.sql。
-- ==========================================================

-- 模块1：身份权限子系统
-- ----------------------------------------------------------
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

INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_code`) VALUES (1, '学生', 'STUDENT'), (2, '管理员', 'ADMIN');
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`) VALUES 
(1, '20230001', '$2a$10$hEks75ip5cNhEizuQAOmqe2KHYxZxNG52j/MAzvjOsZgc7EAwN3W.', '张三', 1),
(2, 'admin',    '$2a$10$XQq1c0p/NEo.TTOXp1mKlObqTr3C44oZ9QN/1P2FX6oFTt3/isege', '系统管理员', 1);
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1), (2, 2);

-- 模块2：座位资源子系统
-- ----------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `room_seat_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `room_seat_db`;

CREATE TABLE IF NOT EXISTS `room_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_name` VARCHAR(50) NOT NULL COMMENT '自习室名称',
  `location` VARCHAR(100) COMMENT '位置描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1开放, 0关闭',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室信息表';

CREATE TABLE IF NOT EXISTS `seat_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_id` BIGINT NOT NULL COMMENT '归属自习室ID (逻辑外键)',
  `seat_number` VARCHAR(20) NOT NULL COMMENT '座位号',
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
  `release_status` TINYINT DEFAULT 0 COMMENT '逻辑释放状态: 0未释放, 1已释放',
  `release_time` DATETIME DEFAULT NULL COMMENT '释放时间',
  `action` VARCHAR(20) DEFAULT NULL COMMENT '释放原因(CANCEL/TIMEOUT/FINISH)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_seat_start_time` (`seat_id`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位时段记录表';

INSERT IGNORE INTO `room_info` (`id`, `room_name`, `location`, `status`) VALUES (1, '第一自习室', '图书馆一楼', 1), (2, '第二自习室', '图书馆二楼', 1);
INSERT IGNORE INTO `seat_info` (`id`, `room_id`, `seat_number`, `status`) VALUES (1, 1, 'A-01', '可用'), (2, 1, 'A-02', '可用'), (3, 1, 'A-03', '可用'), (4, 2, 'B-01', '可用');

-- 模块3：订单流转子系统
-- ----------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `order_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `order_db`;

CREATE TABLE IF NOT EXISTS `order_info` (
  `id` bigint NOT NULL COMMENT '主键（预生成）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `seat_id` bigint NOT NULL COMMENT '座位ID',
  `status` varchar(20) NOT NULL COMMENT '状态',
  `start_time` datetime NOT NULL COMMENT '预约开始时间',
  `end_time` datetime NOT NULL COMMENT '预约结束时间',
  `temp_leave_start_time` datetime DEFAULT NULL COMMENT '暂离开始时间',
  `finish_time` datetime DEFAULT NULL COMMENT '订单进入终态时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_seat_id` (`seat_id`),
  KEY `idx_status_end_time` (`status`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约订单表';

CREATE TABLE IF NOT EXISTS `violation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` varchar(50) NOT NULL COMMENT '违约类型',
  `violation_time` datetime NOT NULL COMMENT '业务违约发生时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `reason` varchar(255) DEFAULT NULL COMMENT '违约原因描述',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='违约记录表';

CREATE TABLE IF NOT EXISTS `order_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `from_status` varchar(20) DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(20) NOT NULL COMMENT '新状态',
  `operator_type` varchar(20) NOT NULL COMMENT '操作来源',
  `transition_time` datetime NOT NULL COMMENT '业务流转时间',
  `reason` varchar(255) DEFAULT NULL COMMENT '变更原因',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单状态流转日志表';

CREATE TABLE IF NOT EXISTS `biz_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(255) NOT NULL COMMENT '配置值',
  `description` varchar(255) DEFAULT NULL COMMENT '配置项中文说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务配置表';

INSERT IGNORE INTO `biz_config` (`config_key`, `config_value`, `description`) VALUES 
('sign_in_deadline_minutes', '15', '签到截止时长（分钟）'), ('temp_leave_limit_minutes', '120', '暂离保留上限（分钟）'), ('duration_options', '1,2,3,4,5,6,7,8', '时长档位'), ('scan_interval_seconds', '60', '定时扫描周期'), ('max_duration_hours', '8', '最大时长'), ('duration_unit_minutes', '60', '档位单位');

-- 模块4：监控通知子系统
-- ----------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `notify_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `notify_db`;

CREATE TABLE IF NOT EXISTS `notify_message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '接收用户ID',
  `order_id`    BIGINT       NULL     DEFAULT NULL   COMMENT '关联订单ID',
  `event_type`  VARCHAR(64)  NOT NULL                COMMENT '事件类型',
  `title`       VARCHAR(100) NOT NULL                COMMENT '消息标题',
  `content`     VARCHAR(500) NOT NULL                COMMENT '消息内容',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'UNREAD' COMMENT '消息状态',
  `biz_time`    DATETIME     NOT NULL                COMMENT '业务发生时间',
  `dedup_key`   VARCHAR(128) NOT NULL                COMMENT '幂等键',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `read_time`   DATETIME     NULL     DEFAULT NULL   COMMENT '已读时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dedup_key` (`dedup_key`),
  KEY `idx_user_status_time` (`user_id`, `status`, `create_time`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知消息主体表';

CREATE TABLE IF NOT EXISTS `notify_push_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '推送日志ID',
  `message_id`  BIGINT       NOT NULL                COMMENT '消息ID',
  `push_status` VARCHAR(16)  NOT NULL                COMMENT '推送状态',
  `retry_count` INT          NOT NULL DEFAULT 0      COMMENT '重试次数',
  `error_msg`   VARCHAR(500) NULL     DEFAULT NULL   COMMENT '失败原因',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_push_status_time` (`push_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息推送日志表';

CREATE TABLE IF NOT EXISTS `notify_task_log` (
  `id`                        BIGINT   NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `execute_time`              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `handle_count`              INT      NOT NULL DEFAULT 0 COMMENT '总处理数',
  `sign_in_timeout_released`  INT      NOT NULL DEFAULT 0 COMMENT '超时未签到释放数',
  `temp_leave_timeout_released` INT    NOT NULL DEFAULT 0 COMMENT '暂离超时释放数',
  `expired_completed`         INT      NOT NULL DEFAULT 0 COMMENT '到期完成数',
  `violation_created`         INT      NOT NULL DEFAULT 0 COMMENT '生成违约记录数',
  `seat_release_failed`       INT      NOT NULL DEFAULT 0 COMMENT '座位释放失败数',
  `message_push_triggered`    INT      NOT NULL DEFAULT 0 COMMENT '触发消息推送数',
  `cost_ms`                   BIGINT   NOT NULL DEFAULT 0 COMMENT '执行耗时（毫秒）',
  `trigger_type`              VARCHAR(16) NOT NULL DEFAULT 'SCHEDULE' COMMENT '触发方式',
  PRIMARY KEY (`id`),
  KEY `idx_execute_time` (`execute_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定时任务执行日志表';