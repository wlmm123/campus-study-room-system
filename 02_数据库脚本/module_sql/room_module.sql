-- ==========================================================
-- 校园自习室分布式预约管理系统 - 座位资源子系统独立脚本
-- 负责人：成员2
-- 版本：V1.0（已通过表结构审核）
-- 说明：遵循全局数据模型规范，主键统一 BIGINT 自增，包含公共字段，无物理外键。
-- ==========================================================

CREATE DATABASE IF NOT EXISTS `room_seat_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `room_seat_db`;

-- 1. 自习室表
CREATE TABLE `room_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_name` VARCHAR(50) NOT NULL COMMENT '自习室名称',
  `location` VARCHAR(100) COMMENT '位置描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1开放, 0关闭',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除: 0未删, 1已删',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室信息表';

-- 2. 座位表 (仅维护座位基础信息和当前粗状态)
CREATE TABLE `seat_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_id` BIGINT NOT NULL COMMENT '归属自习室ID (逻辑外键)',
  `seat_number` VARCHAR(20) NOT NULL COMMENT '座位号(如: A-01)',
  `status` VARCHAR(20) NOT NULL DEFAULT '可用' COMMENT '状态: 可用, 已预约, 已占用, 暂离中, 维护, 停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位信息表';

-- 3. 座位时段占用记录表 (核心防并发超卖表)
CREATE TABLE `seat_occupancy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `seat_id` BIGINT NOT NULL COMMENT '座位ID (逻辑外键)',
  `order_id` BIGINT NOT NULL COMMENT '订单ID (由订单子系统传递，逻辑外键)',
  `start_time` DATETIME NOT NULL COMMENT '占用开始时间',
  `end_time` DATETIME NOT NULL COMMENT '占用结束时间',
  `status` TINYINT DEFAULT 1 COMMENT '占用状态: 1锁定中, 0已释放',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  -- 用来快速查冲突
  KEY `idx_seat_time` (`seat_id`, `start_time`, `end_time`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位时段占用记录表';

-- 4. 初始测试数据
INSERT INTO `room_info` (`id`, `room_name`, `location`, `status`) VALUES 
(1, '第一自习室', '图书馆一楼', 1),
(2, '第二自习室', '图书馆二楼', 1);

INSERT INTO `seat_info` (`id`, `room_id`, `seat_number`, `status`) VALUES 
(1, 1, 'A-01', '可用'),
(2, 1, 'A-02', '可用'),
(3, 1, 'A-03', '可用'),
(4, 2, 'B-01', '可用');