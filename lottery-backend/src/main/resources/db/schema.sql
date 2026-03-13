CREATE DATABASE IF NOT EXISTS lottery DEFAULT CHARSET=utf8mb4;
USE lottery;

-- 用户表
CREATE TABLE `user` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `username` varchar(50) UNIQUE NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT 'BCrypt密码',
  `nickname` varchar(50) COMMENT '昵称',
  `avatar` varchar(255) COMMENT '头像URL',
  `address` varchar(300) COMMENT '默认收货地址',
  `role` enum('player','agent','admin') DEFAULT 'player',
  `balance` int DEFAULT 0 COMMENT '代币余额',
  `agent_id` bigint COMMENT '所属代理user_id',
  `status` tinyint DEFAULT 1 COMMENT '1正常 0封禁',
  `created_at` datetime DEFAULT NOW(),
  `updated_at` datetime DEFAULT NOW() ON UPDATE NOW()
) COMMENT '用户表';

-- 代理信息表
CREATE TABLE `agent` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint UNIQUE NOT NULL,
  `invite_code` varchar(8) UNIQUE COMMENT '随机邀请码',
  `level` tinyint DEFAULT 0 COMMENT '0无 1V1 2V2 3V3',
  `team_count` int DEFAULT 0 COMMENT '团队人数',
  `team_recharge` int DEFAULT 0 COMMENT '团队充值代币总量',
  `commission_rate` decimal(4,2) DEFAULT 0.00 COMMENT '佣金比例',
  `total_commission` decimal(10,2) DEFAULT 0.00 COMMENT '累计佣金',
  `updated_at` datetime DEFAULT NOW() ON UPDATE NOW()
) COMMENT '代理信息表';

-- 充值订单表
CREATE TABLE `recharge_order` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_no` varchar(32) UNIQUE NOT NULL,
  `user_id` bigint NOT NULL,
  `tokens` int NOT NULL COMMENT '充值代币数',
  `amount` decimal(10,2) NOT NULL COMMENT '实际金额',
  `remark` varchar(100) COMMENT '付款人备注',
  `status` enum('pending','done','rejected') DEFAULT 'pending',
  `reject_reason` varchar(200),
  `operator_id` bigint COMMENT '操作管理员ID',
  `created_at` datetime DEFAULT NOW(),
  `updated_at` datetime DEFAULT NOW() ON UPDATE NOW()
) COMMENT '充值订单表';

-- 代币流水表
CREATE TABLE `token_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `amount` int NOT NULL COMMENT '正数增加 负数减少',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `type` enum('recharge','draw','recycle','sell','buy','manual','refund','draw_reward') NOT NULL,
  `ref_id` bigint COMMENT '关联订单/记录ID',
  `remark` varchar(200),
  `created_at` datetime DEFAULT NOW()
) COMMENT '代币流水表';

-- 奖品表
CREATE TABLE `prize` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `type` varchar(10) NOT NULL DEFAULT 'item' COMMENT '奖品类型: item=实物, token=代币',
  `image` varchar(255),
  `value` decimal(10,2) COMMENT '市场价值（展示用）',
  `probability` decimal(5,2) DEFAULT 0.00 COMMENT '概率百分比，如0.90表示0.9%',
  `is_jackpot` tinyint DEFAULT 0 COMMENT '是否大奖',
  `recycle_tokens` int DEFAULT 0 COMMENT '回收可得代币',
  `token_reward` int DEFAULT 0 COMMENT '代币奖励数量(type=token时生效)',
  `stock` int DEFAULT -1 COMMENT '-1不限库存',
  `status` tinyint DEFAULT 1,
  `created_at` datetime DEFAULT NOW()
) COMMENT '奖品表';

-- 抽奖配置表
CREATE TABLE `lottery_config` (
  `id` int PRIMARY KEY DEFAULT 1,
  `cost_tokens` int DEFAULT 10 COMMENT '每次抽奖消耗代币',
  `jackpot_threshold` int DEFAULT 50 COMMENT '保底次数',
  `jackpot_prize_ids` varchar(500) COMMENT '大奖池奖品ID，逗号分隔'
) COMMENT '抽奖配置';

INSERT INTO `lottery_config` VALUES (1, 10, 50, '');

-- 抽奖记录表
CREATE TABLE `draw_record` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `prize_id` bigint NOT NULL,
  `is_jackpot` tinyint DEFAULT 0,
  `consecutive_count` int DEFAULT 0 COMMENT '当前连续未中大奖次数',
  `created_at` datetime DEFAULT NOW()
) COMMENT '抽奖记录表';

-- 用户仓库表
CREATE TABLE `warehouse` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `prize_id` bigint NOT NULL,
  `status` enum('held','selling','recycled','delivered') DEFAULT 'held',
  `sell_price` int COMMENT '转售价格（代币）',
  `created_at` datetime DEFAULT NOW()
) COMMENT '用户仓库表';

-- 转售市场表
CREATE TABLE `market` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `seller_id` bigint NOT NULL,
  `price` int NOT NULL,
  `status` enum('on','sold','off') DEFAULT 'on',
  `buyer_id` bigint,
  `created_at` datetime DEFAULT NOW(),
  `sold_at` datetime
) COMMENT '转售市场表';

-- 发货订单表
CREATE TABLE `delivery_order` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_no` varchar(32) UNIQUE NOT NULL,
  `user_id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `receiver_name` varchar(50),
  `receiver_phone` varchar(20),
  `receiver_address` varchar(200),
  `status` enum('pending','shipped','done') DEFAULT 'pending',
  `created_at` datetime DEFAULT NOW(),
  `shipped_at` datetime,
  `done_at` datetime
) COMMENT '发货订单表';

-- 聊天消息表
CREATE TABLE `chat_message` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `sender_id` bigint NOT NULL,
  `receiver_id` bigint NOT NULL,
  `content` text NOT NULL,
  `is_read` tinyint DEFAULT 0,
  `is_market` tinyint DEFAULT 0 COMMENT '是否市场私聊发起的消息',
  `created_at` datetime DEFAULT NOW()
) COMMENT '聊天消息表';

-- 好友关系表
CREATE TABLE `friendship` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '发起申请方',
  `friend_id` bigint NOT NULL COMMENT '接收方',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待确认 1已同意 2已拒绝',
  `created_at` datetime DEFAULT NOW(),
  `updated_at` datetime DEFAULT NOW() ON UPDATE NOW(),
  INDEX idx_user (user_id, status),
  INDEX idx_friend (friend_id, status),
  UNIQUE KEY uk_pair (user_id, friend_id)
) COMMENT '好友关系表';

-- 单边会话删除记录表
CREATE TABLE `chat_conversation_delete` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `target_id` bigint NOT NULL,
  `deleted_at` datetime NOT NULL DEFAULT NOW(),
  UNIQUE KEY uk_pair (user_id, target_id)
) COMMENT '单边会话删除记录';

-- 默认管理员账号（密码: admin123，BCrypt加密后替换）
INSERT INTO `user` (username, password, nickname, role)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 'admin');
