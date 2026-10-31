-- ==========================================================
-- 校园自习室分布式预约管理系统 - 节点B 初始化脚本
-- 包含数据库：order_db（订单）、notify_db（通知）
-- 执行节点：节点B (Ubuntu虚拟机)
-- 注意：已去除 DROP TABLE 语句，全部采用 CREATE TABLE IF NOT EXISTS，防止重复执行误清空数据。
-- ==========================================================

SET NAMES utf8mb4;

-- 模块3：订单流转子系统 (负责人：成员3)
-- ==========================================================
CREATE DATABASE IF NOT EXISTS `order_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `order_db`;

CREATE TABLE IF NOT EXISTS `order_info` (
  `id` bigint NOT NULL COMMENT '主键（应用层预生成全局唯一 orderId）',
  `user_id` bigint NOT NULL COMMENT '逻辑外键：用户ID',
  `seat_id` bigint NOT NULL COMMENT '逻辑外键：座位ID',
  `status` varchar(20) NOT NULL COMMENT '状态（待签到,已签到,已取消,超时释放,暂离超时释放,已完成）',
  `start_time` datetime NOT NULL COMMENT '预约开始时间',
  `end_time` datetime NOT NULL COMMENT '预约结束时间',
  `temp_leave_start_time` datetime DEFAULT NULL COMMENT '暂离开始时间',
  `finish_time` datetime DEFAULT NULL COMMENT '订单进入终态时间；仅终态有值，进行中为NULL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删, 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_seat_id` (`seat_id`),
  KEY `idx_status_end_time` (`status`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约订单表';

CREATE TABLE IF NOT EXISTS `violation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '逻辑外键：订单ID',
  `user_id` bigint NOT NULL COMMENT '逻辑外键：用户ID',
  `type` varchar(50) NOT NULL COMMENT '违约类型（超时未签到, 暂离超时）',
  `violation_time` datetime NOT NULL COMMENT '业务违约发生时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '违约记录入库落库时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删, 1已删',
  `reason` varchar(255) DEFAULT NULL COMMENT '违约原因描述',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='违约记录表';

CREATE TABLE IF NOT EXISTS `order_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '逻辑外键：订单ID',
  `from_status` varchar(20) DEFAULT NULL COMMENT '原状态；订单首次创建初始流转记录时为NULL',
  `to_status` varchar(20) NOT NULL COMMENT '新状态',
  `operator_type` varchar(20) NOT NULL COMMENT '操作来源（STUDENT=学生手动操作, SYSTEM=定时任务系统触发）',
  `transition_time` datetime NOT NULL COMMENT '业务流转时间（即日志创建时间）',
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
('sign_in_deadline_minutes', '15', '签到截止时长（分钟）'),
('temp_leave_limit_minutes', '120', '暂离保留上限（分钟）'),
('duration_options', '1,2,3,4,5,6,7,8', '可选时长档位（小时），逗号分隔字符串，应用层解析为数组返回'),
('scan_interval_seconds', '60', '定时扫描周期（秒）'),
('max_duration_hours', '8', '单次预约最大时长（小时）'),
('duration_unit_minutes', '60', '档位单位（分钟）');


-- 模块4：监控通知子系统 (负责人：成员4)
-- ==========================================================
CREATE DATABASE IF NOT EXISTS `notify_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `notify_db`;

CREATE TABLE IF NOT EXISTS `notify_message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '接收用户ID（逻辑关联 auth_db 用户，不建外键）',
  `order_id`    BIGINT       NULL     DEFAULT NULL   COMMENT '关联订单ID（逻辑关联 order_db，系统消息可为空）',
  `event_type`  VARCHAR(64)  NOT NULL                COMMENT '事件类型',
  `title`       VARCHAR(100) NOT NULL                COMMENT '消息标题',
  `content`     VARCHAR(500) NOT NULL                COMMENT '消息内容',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'UNREAD' COMMENT '消息状态：UNREAD/READ',
  `biz_time`    DATETIME     NOT NULL                COMMENT '业务发生时间',
  `dedup_key`   VARCHAR(128) NOT NULL                COMMENT '幂等键，建议 eventType:orderId',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `read_time`   DATETIME     NULL     DEFAULT NULL   COMMENT '已读时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dedup_key` (`dedup_key`),
  KEY `idx_user_status_time` (`user_id`, `status`, `create_time`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知消息主体表';

CREATE TABLE IF NOT EXISTS `notify_push_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '推送日志ID',
  `message_id`  BIGINT       NOT NULL                COMMENT '消息ID（逻辑关联 notify_message.id，不建外键）',
  `push_status` VARCHAR(16)  NOT NULL                COMMENT '推送状态：SUCCESS/FAIL',
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
  `trigger_type`              VARCHAR(16) NOT NULL DEFAULT 'SCHEDULE' COMMENT '触发方式：SCHEDULE定时/MANUAL手动补偿',
  PRIMARY KEY (`id`),
  KEY `idx_execute_time` (`execute_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定时任务执行日志表';