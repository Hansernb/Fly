/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : boonie_auth

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 14/10/2026 09:10:14
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for boonie_attraction
-- ----------------------------
DROP TABLE IF EXISTS `boonie_attraction`;
CREATE TABLE `boonie_attraction`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
  `emoji` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目图标 emoji',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目具体内容（简介/玩法）',
  `tag` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签：刺激/浪漫/亲子/清凉/益智',
  `wait_minutes` int NOT NULL DEFAULT 0 COMMENT '预计排队分钟',
  `color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主题色',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of boonie_attraction
-- ----------------------------
INSERT INTO `boonie_attraction` VALUES (1, '树洞过山车', '🎢', '穿梭在千年古树之间，俯冲、旋转，尖叫声响彻狗熊岭！全程 880 米，最高时速 78km/h，适合追求刺激的勇敢者。', '刺激', 25, '#e4572e', '2026-10-14 08:29:02');
INSERT INTO `boonie_attraction` VALUES (2, '星空摩天轮', '🎡', '升至森林之巅，俯瞰整片狗熊岭的日落与星空。每舱可坐 6 人，缓缓转动 12 分钟一圈，浪漫满分。', '浪漫', 15, '#7c5cbf', '2026-10-14 08:29:02');
INSERT INTO `boonie_attraction` VALUES (3, '森林小火车', '🚂', '呜——哐当哐当，跟着吉吉国王环游五大主题区。全程 1.6 公里，沿途有动物演员挥手互动，亲子首选。', '亲子', 10, '#2f7d4f', '2026-10-14 08:29:02');
INSERT INTO `boonie_attraction` VALUES (4, '蜂蜜漂流', '🍯', '乘上蜂蜜罐小船，顺着金色溪流来一场甜蜜漂流。全长 420 米，中途有喷泉惊喜，清凉解暑。', '清凉', 30, '#ffb703', '2026-10-14 08:29:02');
INSERT INTO `boonie_attraction` VALUES (5, '光头强迷宫', '🏰', '在光头强的小木屋迷宫里寻宝，小心别被发现哦！共 3 层木质迷宫，藏着 12 个惊喜彩蛋。', '益智', 8, '#a9743c', '2026-10-14 08:29:02');
INSERT INTO `boonie_attraction` VALUES (6, '激流勇士', '💦', '从瀑布顶端一跃而下，夏日限定的透心凉体验。落差 18 米，尖叫与水花齐飞，记得带雨衣。', '刺激', 40, '#2a9d8f', '2026-10-14 08:29:02');

-- ----------------------------
-- Table structure for boonie_review
-- ----------------------------
DROP TABLE IF EXISTS `boonie_review`;
CREATE TABLE `boonie_review`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attraction_id` bigint NOT NULL COMMENT '所属项目',
  `reviewer_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评价人昵称',
  `avatar` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '?' COMMENT '头像 emoji',
  `rating` decimal(2, 1) NOT NULL COMMENT '评分 0-5（一位小数）',
  `review_date` date NOT NULL COMMENT '评价日期',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评价内容',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_attraction`(`attraction_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of boonie_review
-- ----------------------------
INSERT INTO `boonie_review` VALUES (1, 1, '熊大粉丝', '🐻', 5.0, '2026-10-08', '太刺激了！熊大熊二就在旁边加油，孩子们玩得不想走。', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (2, 1, '森林旅人', '🦊', 4.0, '2026-10-05', '排队略久但值回票价，建议上午来避开高峰。', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (3, 2, '仰望星空的猫', '🐱', 4.0, '2026-10-07', '傍晚坐上去看日落特别美，就是风有点大。', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (4, 2, '情侣小白', '💑', 5.0, '2026-10-02', '求婚成功！摩天轮顶端的那一刻太浪漫了。', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (5, 3, '亲子妈妈', '🐰', 5.0, '2026-10-03', '超适合带娃，吉吉国王还会挥手，女儿玩得超开心！', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (6, 3, '吉吉粉丝', '🐵', 4.0, '2026-09-28', '环游一圈能看遍全园，性价比很高。', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (7, 4, '甜筒熊', '🐻', 4.0, '2026-10-06', '夏天玩太爽了，就是排队有点久。', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (8, 4, '漂流达人', '🌊', 5.0, '2026-09-30', '蜂蜜罐小船超可爱，拍照出片率很高！', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (9, 5, '迷宫王', '🧩', 3.0, '2026-10-01', '有点绕，小朋友偶尔会急，建议大人陪同。', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (10, 5, '寻宝少年', '🗺️', 4.0, '2026-09-26', '找到彩蛋那一刻超有成就感！', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (11, 6, '浪里白条', '🐬', 5.0, '2026-10-04', '落差超带感，下来全身湿透但超爽！', '2026-10-14 08:29:02');
INSERT INTO `boonie_review` VALUES (12, 6, '夏日控', '☀️', 4.0, '2026-09-29', '夏天必玩项目，排队久但值得。', '2026-10-14 08:29:02');

-- ----------------------------
-- Table structure for boonie_user
-- ----------------------------
DROP TABLE IF EXISTS `boonie_user`;
CREATE TABLE `boonie_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名/显示名',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录邮箱',
  `password_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 哈希',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of boonie_user
-- ----------------------------
INSERT INTO `boonie_user` VALUES (1, '熊大', 'admin@boonie.com', '$2a$10$36kGP0rvuYEqIe54pVeUI.PItza59hAUz6Kjt5sm/Yu/BXtKGINEK', '2026-10-14 08:29:02');
INSERT INTO `boonie_user` VALUES (2, '熊二', 'bear2@boonie.com', '$2a$10$ShdkbIotdapU4ejt0FRAsuagmKzLDe423T.swGLX/qFmlBPG.PPMe', '2026-10-14 08:29:02');
INSERT INTO `boonie_user` VALUES (3, 'aaacc', '123@qq.com', '$2a$10$czqL4ujaBQk7C8Ykh8iGg.5KTnF5X.Xsl2U3zo8Pja.BPKAv3gilu', '2026-10-14 08:29:17');

SET FOREIGN_KEY_CHECKS = 1;
