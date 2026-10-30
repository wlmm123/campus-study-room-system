-- ==========================================================
-- 模块：订单流转子系统 (order_db) - 节点B
-- 编写人：成员3
-- 版本：V1.2（根据成员2评审意见修订：统一字符集、初始数据幂等、补充配置项注释）
-- 依据：E-R图、REST接口契约-v3、小组开发契约
-- ==========================================================

SET NAMES utf8mb4;

-- 创建并切换数据库
-- 【V1.2修改】排序规则 utf8mb4_0900_ai_ci → utf8mb4_general_ci（兼容MySQL5.7）
CREATE DATABASE IF NOT EXISTS `order_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `order_db`;

-- ----------------------------
-- 1. 预约订单表 (ORDER_INFO)
-- ----------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
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
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删, 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_seat_id` (`seat_id`),
  KEY `idx_status_end_time` (`status`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约订单表';

-- ----------------------------
-- 2. 违约记录表 (VIOLATION)
-- ----------------------------
DROP TABLE IF EXISTS `violation`;
CREATE TABLE `violation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '逻辑外键：订单ID',
  `user_id` bigint NOT NULL COMMENT '逻辑外键：用户ID',
  `type` varchar(50) NOT NULL COMMENT '违约类型（超时未签到, 暂离超时）',
  `violation_time` datetime NOT NULL COMMENT '业务违约发生时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '违约记录入库落库时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删, 1已删',
  `reason` varchar(255) DEFAULT NULL COMMENT '违约原因描述',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='违约记录表';

-- ----------------------------
-- 3. 订单状态流转日志表 (ORDER_STATUS_LOG)
--     说明：append-only 审计日志表，仅追加不更新不删除，
--           transition_time 即创建时间语义，故不设 update_time / is_deleted。
-- ----------------------------
DROP TABLE IF EXISTS `order_status_log`;
CREATE TABLE `order_status_log` (
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

-- ----------------------------
-- 4. 业务配置表 (BIZ_CONFIG)
-- ----------------------------
DROP TABLE IF EXISTS `biz_config`;
CREATE TABLE `biz_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) NOT NULL COMMENT '配置键（如:checkin_deadline_minutes）',
  `config_value` varchar(255) NOT NULL COMMENT '配置值',
  `description` varchar(255) DEFAULT NULL COMMENT '配置项中文说明，内部维护用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务配置表';

-- ----------------------------
-- 5. 插入默认业务配置数据
--    【V1.2修改】INSERT → INSERT IGNORE，防止脚本重复执行时 uk_config_key 冲突
--    【V1.2修改】duration_options 说明补"逗号分隔字符串，应用层解析为数组返回"
-- ----------------------------
INSERT IGNORE INTO `biz_config` (`config_key`, `config_value`, `description`) VALUES 
('checkin_deadline_minutes', '15', '签到截止时长（分钟）'),
('temp_leave_limit_minutes', '120', '暂离保留上限（分钟）'),
('duration_options', '1,2,3,4,5,6,7,8', '可选时长档位（小时），逗号分隔字符串，应用层解析为数组返回'),
('scan_interval_seconds', '60', '定时扫描周期（秒）'),
('max_duration_hours', '8', '单次预约最大时长（小时）');