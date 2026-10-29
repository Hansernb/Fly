-- =============================================================
-- 狗熊岭森林游乐园 —— 数据库初始化 / 迁移脚本（可重复执行，幂等）
-- 用法：mysql -uroot -p123456 < db-init.sql
-- 说明：
--   1. 建库建表（已存在则跳过）
--   2. 迁移：boonie_user 新增 permission 权限字段（1=默认，0=管理员）
--   3. 迁移：password_hash(BCrypt 哈希) -> password(明文存储，按需求不加密)
--   4. 种子用户重置为明文密码 bear123456，熊大为管理员(0)
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `boonie_auth`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `boonie_auth`;

-- ---------------------------- 用户表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '用户名/显示名',
  `email` varchar(128) NOT NULL COMMENT '登录邮箱',
  `password` varchar(128) NOT NULL COMMENT '登录密码（明文，按需求不加密）',
  `permission` int NOT NULL DEFAULT 1 COMMENT '权限：1=默认权限(注册用户)，0=管理员权限',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `email`(`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 迁移 1：新增 permission 字段 ----------------------------
SET @db := DATABASE();
SET @has_perm := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_user' AND COLUMN_NAME = 'permission'
);
SET @sql := IF(@has_perm = 0,
  'ALTER TABLE `boonie_user` ADD COLUMN `permission` int NOT NULL DEFAULT 1 COMMENT ''权限：1=默认权限(注册用户)，0=管理员权限'' AFTER `email`',
  'SELECT 1');
PREPARE _st FROM @sql; EXECUTE _st; DEALLOCATE PREPARE _st;

-- ---------------------------- 迁移 2：password_hash -> password（改明文） ----------------------------
SET @has_pwd := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_user' AND COLUMN_NAME = 'password'
);
SET @has_hash := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_user' AND COLUMN_NAME = 'password_hash'
);
SET @sql2 := IF(@has_pwd = 0 AND @has_hash = 1,
  'ALTER TABLE `boonie_user` CHANGE COLUMN `password_hash` `password` varchar(128) NOT NULL COMMENT ''登录密码（明文，按需求不加密）''',
  'SELECT 1');
PREPARE _st2 FROM @sql2; EXECUTE _st2; DEALLOCATE PREPARE _st2;

-- ---------------------------- 游乐项目表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_attraction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '项目名称',
  `emoji` varchar(16) NOT NULL COMMENT '项目图标 emoji',
  `description` text NOT NULL COMMENT '项目具体内容（简介/玩法）',
  `tag` varchar(16) NOT NULL COMMENT '标签：刺激/浪漫/亲子/清凉/益智',
  `wait_minutes` int NOT NULL DEFAULT 0 COMMENT '预计排队分钟',
  `color` varchar(16) NOT NULL COMMENT '主题色',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 游客评价表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attraction_id` bigint NOT NULL COMMENT '所属项目',
  `reviewer_name` varchar(64) NOT NULL COMMENT '评价人昵称',
  `avatar` varchar(16) NOT NULL DEFAULT '🐻' COMMENT '头像 emoji',
  `rating` decimal(2, 1) NOT NULL COMMENT '评分 0-5（一位小数）',
  `review_date` date NOT NULL COMMENT '评价日期',
  `content` text NOT NULL COMMENT '评价内容',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_attraction`(`attraction_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 种子/修复：明文密码 + 管理员权限 ----------------------------
-- 旧库里存的是 BCrypt 哈希，无法还原明文，这里把已知种子账号重置为明文 bear123456
UPDATE `boonie_user`
   SET `password` = 'bear123456'
 WHERE `email` IN ('admin@boonie.com', 'bear2@boonie.com');

-- 熊大 = 管理员(0)；其余用户保持/设为默认权限(1)
UPDATE `boonie_user` SET `permission` = 0 WHERE `email` = 'admin@boonie.com';
UPDATE `boonie_user` SET `permission` = 1 WHERE `email` <> 'admin@boonie.com' AND `permission` IS NULL;

-- 补齐空权限（老数据迁移后应为 NULL）
UPDATE `boonie_user` SET `permission` = 1 WHERE `permission` IS NULL;

-- 用户名唯一索引：登录已由邮箱改为用户名（按 name 匹配），加唯一键避免重名导致登录歧义（幂等）
SET @uk_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'boonie_user' AND INDEX_NAME = 'uk_user_name'
);
SET @ddl := IF(@uk_exists = 0,
  'ALTER TABLE `boonie_user` ADD UNIQUE KEY `uk_user_name`(`name`)',
  'DO 0');
PREPARE _stmt FROM @ddl;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- ---------------------------- 森林栖息地 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_habitat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emoji` varchar(16) NOT NULL DEFAULT '🦉' COMMENT '图标 emoji',
  `img` varchar(255) DEFAULT NULL COMMENT '配图地址',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `description` text COMMENT '简介',
  `residents` varchar(128) DEFAULT NULL COMMENT '居民',
  `mood` varchar(128) DEFAULT NULL COMMENT '当前状态',
  `color` varchar(16) DEFAULT '#2f7d4f' COMMENT '主题色',
  `rating` decimal(2, 1) NOT NULL DEFAULT 5.0 COMMENT '评分 0-5',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 森林演出 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_show` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `show_time` varchar(16) NOT NULL COMMENT '演出时间，如 10:30',
  `emoji` varchar(16) NOT NULL DEFAULT '🎪' COMMENT '图标 emoji',
  `name` varchar(64) NOT NULL COMMENT '演出名称',
  `place` varchar(128) DEFAULT NULL COMMENT '地点说明',
  `tag` varchar(64) DEFAULT NULL COMMENT '时长标签',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 门票 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emoji` varchar(16) NOT NULL DEFAULT '🎫' COMMENT '图标 emoji',
  `name` varchar(64) NOT NULL COMMENT '票种名称',
  `price` decimal(10, 2) NOT NULL DEFAULT 0 COMMENT '价格（元）',
  `hot` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否热门推荐',
  `features` text COMMENT '权益说明，每行一条（\n 分隔）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 后端项目登记表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_project` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '项目名称',
  `type` varchar(32) NOT NULL DEFAULT '未知' COMMENT '项目类型：Spring Boot / Vue / Node 等',
  `dir_path` varchar(128) DEFAULT NULL COMMENT '项目目录（相对工作区根目录）',
  `port` int DEFAULT NULL COMMENT '主端口',
  `health_url` varchar(255) DEFAULT NULL COMMENT '健康检查地址',
  `description` text COMMENT '备注说明',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 种子：登记本工作区下的两个核心项目（INSERT IGNORE 幂等）
INSERT IGNORE INTO `boonie_project` (`id`, `name`, `type`, `dir_path`, `port`, `health_url`, `description`) VALUES
(1,'森林游乐园后端','Spring Boot','springBootTest',8080,'http://localhost:8080/api/health','主后端服务：登录注册、游乐项目、游客评价、用户权限与后台管理接口（MySQL 持久化）。'),
(2,'森林登录注册前端','Vue 3 + Vite','boonie-auth-pro',5173,'http://127.0.0.1:5173/','前端 SPA：登录注册页、游乐园主页与后台管理系统（开发服务器）。');

-- ---------------------------- 种子数据：把首页原有静态数据搬进库（INSERT IGNORE 幂等） ----------------------------
INSERT IGNORE INTO `boonie_habitat` (`id`, `emoji`, `img`, `name`, `description`, `residents`, `mood`, `color`, `rating`) VALUES
(1,'🦉','/img/owl.jpg','猫头鹰树屋','百年老橡树上的圆圆树洞，夜幕降临后这里最热闹。','猫头鹰一家 · 夜间巡逻员','😴 白天打盹中','#7c5cbf',4.9),
(2,'🐿️','/img/squirrel.jpg','松鼠松果谷','满地松果的山谷，小松鼠们正忙着囤积冬天的口粮。','蹦蹦和它的表兄弟们','🌰 囤粮大作战','#a9743c',4.7),
(3,'🦌','/img/deer.jpg','小鹿溪谷','清澈溪流穿谷而过，清晨常能遇见来喝水的小鹿。','鹿妈妈与两只小鹿','💧 溪边饮水','#2a9d8f',4.8),
(4,'🐇','/img/rabbit.jpg','兔子草甸','开满野花的柔软草甸，是兔子们赛跑的天然运动场。','萝卜头田径队','🏃 晨练进行中','#4caf6d',4.6),
(5,'🐝','/img/bee.jpeg','蜜蜂花园','蜂蜜美食街的秘密源头，万花丛中嗡嗡作响。','熊二的蜂蜜供应商','🍯 采蜜忙碌中','#ffb703',4.9),
(6,'🦔','/img/hedgehog.jpeg','刺猬灌木丛','浆果灌木深处的小窝，缩成球的小刺猬最可爱。','团团 & 圆圆','🍓 浆果品鉴会','#e4572e',4.7);

INSERT IGNORE INTO `boonie_show` (`id`, `show_time`, `emoji`, `name`, `place`, `tag`) VALUES
(1,'10:30','🎭','熊出没剧场','中央大舞台 · 熊大熊二爆笑登场','约 40 分钟'),
(2,'14:00','🦜','百鸟森林音乐会','鸟语林剧场 · 涂涂领衔主唱','约 30 分钟'),
(3,'19:30','🎆','森林星光巡游','主干道 · 花车 + 烟花压轴大秀','约 50 分钟');

INSERT IGNORE INTO `boonie_ticket` (`id`, `emoji`, `name`, `price`, `hot`, `features`) VALUES
(1,'🎫','成人票',199.00,0,'全天畅玩所有项目\n含 1 场剧场演出'),
(2,'👨‍👩‍👧','亲子套票',399.00,1,'2 大 1 小全天畅玩\n含全部演出\n赠蜂蜜爆米花一份'),
(3,'🌙','夜场票',99.00,0,'17:00 后入园\n含星光巡游观赏位');

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'db-init.sql 执行完成（permission 字段 + 明文密码 + 栖息地/演出/门票 已就绪）' AS `result`;
