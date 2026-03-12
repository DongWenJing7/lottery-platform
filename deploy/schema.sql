-- MySQL dump 10.13  Distrib 8.0.44, for macos26.0 (arm64)
--
-- Host: localhost    Database: lottery
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `agent`
--

DROP TABLE IF EXISTS `agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `invite_code` varchar(8) DEFAULT NULL COMMENT '随机邀请码',
  `level` tinyint DEFAULT '0' COMMENT '0无 1V1 2V2 3V3',
  `team_count` int DEFAULT '0' COMMENT '团队人数',
  `team_recharge` int DEFAULT '0' COMMENT '团队充值代币总量',
  `commission_rate` decimal(4,2) DEFAULT '0.00' COMMENT '佣金比例',
  `total_commission` decimal(10,2) DEFAULT '0.00' COMMENT '累计佣金',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `invite_code` (`invite_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代理信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `delivery_order`
--

DROP TABLE IF EXISTS `delivery_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `user_id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `receiver_name` varchar(50) DEFAULT NULL,
  `receiver_phone` varchar(20) DEFAULT NULL,
  `receiver_address` varchar(200) DEFAULT NULL,
  `status` enum('pending','shipped','done') DEFAULT 'pending',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `shipped_at` datetime DEFAULT NULL,
  `done_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发货订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `draw_record`
--

DROP TABLE IF EXISTS `draw_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `draw_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `prize_id` bigint NOT NULL,
  `is_jackpot` tinyint DEFAULT '0',
  `consecutive_count` int DEFAULT '0' COMMENT '当前连续未中大奖次数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=138 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抽奖记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lottery_config`
--

DROP TABLE IF EXISTS `lottery_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lottery_config` (
  `id` int NOT NULL DEFAULT '1',
  `cost_tokens` int DEFAULT '10' COMMENT '每次抽奖消耗代币',
  `jackpot_threshold` int DEFAULT '50' COMMENT '保底次数',
  `jackpot_prize_ids` varchar(500) DEFAULT NULL COMMENT '大奖池奖品ID，逗号分隔',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抽奖配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `market`
--

DROP TABLE IF EXISTS `market`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `market` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `seller_id` bigint NOT NULL,
  `price` int NOT NULL,
  `status` enum('on','sold','off') DEFAULT 'on',
  `buyer_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `sold_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='转售市场表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `prize`
--

DROP TABLE IF EXISTS `prize`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prize` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `type` varchar(10) NOT NULL DEFAULT 'item' COMMENT '奖品类型: item=实物, token=代币',
  `image` varchar(255) DEFAULT NULL,
  `value` decimal(10,2) DEFAULT NULL COMMENT '市场价值（展示用）',
  `probability` int DEFAULT '100' COMMENT '概率权重',
  `is_jackpot` tinyint DEFAULT '0' COMMENT '是否大奖',
  `recycle_tokens` int DEFAULT '0' COMMENT '回收可得代币',
  `token_reward` int DEFAULT '0' COMMENT '代币奖励数量(type=token时生效)',
  `stock` int DEFAULT '-1' COMMENT '-1不限库存',
  `status` tinyint DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recharge_order`
--

DROP TABLE IF EXISTS `recharge_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recharge_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `user_id` bigint NOT NULL,
  `tokens` int NOT NULL COMMENT '充值代币数',
  `amount` decimal(10,2) NOT NULL COMMENT '实际金额',
  `remark` varchar(100) DEFAULT NULL COMMENT '付款人备注',
  `status` enum('pending','done','rejected') DEFAULT 'pending',
  `reject_reason` varchar(200) DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL COMMENT '操作管理员ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='充值订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `token_log`
--

DROP TABLE IF EXISTS `token_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `token_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `amount` int NOT NULL COMMENT '正数增加 负数减少',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `type` enum('recharge','draw','recycle','sell','buy','manual','refund','draw_reward') NOT NULL,
  `ref_id` bigint DEFAULT NULL COMMENT '关联订单/记录ID',
  `remark` varchar(200) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代币流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT 'BCrypt密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `address` varchar(300) DEFAULT NULL COMMENT '默认收货地址',
  `role` enum('player','agent','admin') DEFAULT 'player',
  `balance` int DEFAULT '0' COMMENT '代币余额',
  `agent_id` bigint DEFAULT NULL COMMENT '所属代理user_id',
  `status` tinyint DEFAULT '1' COMMENT '1正常 0封禁',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `warehouse`
--

DROP TABLE IF EXISTS `warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `prize_id` bigint NOT NULL,
  `status` enum('held','selling','recycled','delivered') DEFAULT 'held',
  `sell_price` int DEFAULT NULL COMMENT '转售价格（代币）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=124 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户仓库表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-10 17:05:22

-- 初始数据：管理员账号 admin/admin123，抽奖配置
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `balance`, `status`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 'admin', 0, 1);

INSERT INTO `lottery_config` (`id`, `cost_tokens`, `jackpot_threshold`) VALUES (1, 10, 50);
