-- 1. 创建并切换数据库
CREATE DATABASE IF NOT EXISTS notify_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE notify_db;

-- 2. 消息主体表
DROP TABLE IF EXISTS `notify_message`;
CREATE TABLE `notify_message` (
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

-- 3. 消息推送日志表（日志表暂不加 is_deleted，但已有 update_time）
DROP TABLE IF EXISTS `notify_push_log`;
CREATE TABLE `notify_push_log` (
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

-- 4. 定时任务执行日志表
DROP TABLE IF EXISTS `notify_task_log`;
CREATE TABLE `notify_task_log` (
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