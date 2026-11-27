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
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像地址（默认 /img/defaults/avatar-N.jpg 或上传后 /api/avatar/文件名）',
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

-- ---------------------------- 迁移 3：新增 avatar 头像字段 ----------------------------
SET @has_avatar := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_user' AND COLUMN_NAME = 'avatar'
);
SET @sql3 := IF(@has_avatar = 0,
  'ALTER TABLE `boonie_user` ADD COLUMN `avatar` varchar(255) DEFAULT NULL COMMENT ''头像地址（默认 /img/defaults/avatar-N.jpg 或上传后 /api/avatar/文件名）'' AFTER `permission`',
  'SELECT 1');
PREPARE _st3 FROM @sql3; EXECUTE _st3; DEALLOCATE PREPARE _st3;

-- 回填：历史用户 avatar 为空时，按 id 轮转分配 3 个默认头像（幂等，已有头像的不动）
UPDATE `boonie_user`
SET `avatar` = CONCAT('/img/defaults/avatar-', (MOD(`id` - 1, 3) + 1), '.jpg')
WHERE `avatar` IS NULL OR `avatar` = '';

-- ---------------------------- 迁移 4：评价头像字段扩容 ----------------------------
-- 原 avatar 为 varchar(16)，只够存 emoji；用户真实头像需存图片 URL，故扩到 255
SET @review_avatar_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_review' AND COLUMN_NAME = 'avatar'
);
SET @sql4 := IF(@review_avatar_len IS NOT NULL AND @review_avatar_len < 255,
  'ALTER TABLE `boonie_review` MODIFY COLUMN `avatar` varchar(255) NOT NULL DEFAULT ''🐻'' COMMENT ''评价人头像：varchar(255) 兼容 emoji 与图片 URL''',
  'SELECT 1');
PREPARE _st4 FROM @sql4; EXECUTE _st4; DEALLOCATE PREPARE _st4;

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

-- ---------------------------- 迁移：游乐项目补风景图 img ----------------------------
SET @has_attr_img := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_attraction' AND COLUMN_NAME = 'img'
);
SET @sql_img := IF(@has_attr_img = 0,
  'ALTER TABLE `boonie_attraction` ADD COLUMN `img` varchar(128) DEFAULT NULL COMMENT ''项目风景图路径（前端 /img/… 静态资源）'' AFTER `emoji`',
  'SELECT 1');
PREPARE _st_img FROM @sql_img; EXECUTE _st_img; DEALLOCATE PREPARE _st_img;

-- 老数据按项目名回填风景图（幂等：已有图的不动；新项目缺图由接口层按 id 兜底）
UPDATE `boonie_attraction` SET `img` = '/img/coaster.gif'   WHERE (`img` IS NULL OR `img` = '') AND `name` = '树洞过山车';
UPDATE `boonie_attraction` SET `img` = '/img/ferris.jpeg'   WHERE (`img` IS NULL OR `img` = '') AND `name` = '星空摩天轮';
UPDATE `boonie_attraction` SET `img` = '/img/train.jpeg'    WHERE (`img` IS NULL OR `img` = '') AND `name` = '森林小火车';
UPDATE `boonie_attraction` SET `img` = '/img/rafting.jpeg'  WHERE (`img` IS NULL OR `img` = '') AND `name` = '蜂蜜漂流';
UPDATE `boonie_attraction` SET `img` = '/img/maze.jpeg'     WHERE (`img` IS NULL OR `img` = '') AND `name` = '光头强迷宫';
UPDATE `boonie_attraction` SET `img` = '/img/waterfall.jpg' WHERE (`img` IS NULL OR `img` = '') AND `name` = '激流勇士';

-- ---------------------------- 游客评价表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attraction_id` bigint NOT NULL COMMENT '所属项目',
  `reviewer_name` varchar(64) NOT NULL COMMENT '评价人昵称',
  `avatar` varchar(255) NOT NULL DEFAULT '🐻' COMMENT '评价人头像：varchar(255) 兼容 emoji 与图片 URL',
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

-- =============================================================
-- 迁移 5：门票表扩容（对齐前端票库：票种编码 / 原价 / 优惠标签 / 排序）
-- =============================================================
SET @has_ticket_code := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_ticket' AND COLUMN_NAME = 'type_code'
);
SET @sql5 := IF(@has_ticket_code = 0,
  'ALTER TABLE `boonie_ticket`
     ADD COLUMN `type_code` varchar(32) DEFAULT NULL COMMENT ''票种编码：adult/child/senior/family/night'' AFTER `id`,
     ADD COLUMN `original_price` decimal(10,2) NOT NULL DEFAULT 0 COMMENT ''原价（用于划线优惠）'' AFTER `price`,
     ADD COLUMN `discount_label` varchar(16) DEFAULT NULL COMMENT ''优惠标签，如 5 折 / 套票'' AFTER `original_price`,
     ADD COLUMN `tag_label` varchar(16) DEFAULT NULL COMMENT ''角标文案：标准/儿童优惠/热门/夜场'' AFTER `discount_label`,
     ADD COLUMN `desc_text` varchar(255) DEFAULT NULL COMMENT ''一句话说明（订票流程用）'' AFTER `tag_label`,
     ADD COLUMN `color` varchar(16) DEFAULT ''#2f7d4f'' COMMENT ''主题色'' AFTER `desc_text`,
     ADD COLUMN `sort_no` int NOT NULL DEFAULT 0 COMMENT ''首页排序'' AFTER `color`,
     ADD COLUMN `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT ''是否上架：1 上架 0 下架'' AFTER `sort_no`',
  'SELECT 1');
PREPARE _st5 FROM @sql5; EXECUTE _st5; DEALLOCATE PREPARE _st5;

-- 票种编码/原价缺失的老数据补齐（幂等）
UPDATE `boonie_ticket` SET `original_price` = `price` WHERE `original_price` = 0 OR `original_price` IS NULL;
UPDATE `boonie_ticket` SET `sort_no` = `id` WHERE `sort_no` = 0;
UPDATE `boonie_ticket` SET `type_code` = CONCAT('ticket_', `id`) WHERE `type_code` IS NULL OR `type_code` = '';

-- ---------------------------- 门票订单表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_ticket_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单号 BK+时间+随机',
  `user_id` bigint DEFAULT NULL COMMENT '下单用户（游客为空）',
  `user_name` varchar(64) DEFAULT NULL COMMENT '下单用户名',
  `ticket_id` bigint NOT NULL COMMENT '票种 id',
  `ticket_code` varchar(32) DEFAULT NULL COMMENT '票种编码',
  `ticket_name` varchar(64) NOT NULL COMMENT '票种名称快照',
  `emoji` varchar(16) DEFAULT '🎫' COMMENT '票种图标',
  `unit_price` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '成交单价',
  `original_price` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '原价单价',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_amount` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '应付总额',
  `saved_amount` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '已优惠金额',
  `visit_date` date NOT NULL COMMENT '游玩日期',
  `buyer_name` varchar(64) NOT NULL COMMENT '取票人姓名',
  `buyer_phone` varchar(20) NOT NULL COMMENT '取票人手机号',
  `status` varchar(16) NOT NULL DEFAULT '待使用' COMMENT '待使用/已取消/已使用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `member_id` bigint DEFAULT NULL COMMENT '会员 id（下单手机号对应的会员档案）',
  `gross_amount` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '积分抵扣前金额',
  `points_used` int NOT NULL DEFAULT 0 COMMENT '使用的积分',
  `points_discount` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '积分抵扣金额',
  `points_earned` int NOT NULL DEFAULT 0 COMMENT '本单获得的积分',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_order_no`(`order_no`),
  INDEX `idx_order_user`(`user_id`),
  INDEX `idx_order_visit`(`visit_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 会员表（下单手机号自动建档） ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT '绑定账号 boonie_user.id（账号买票积分归该账号）',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号（游客会员标识；账号会员取最近下单手机号）',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(16) DEFAULT '🐻' COMMENT '头像 emoji',
  `total_points` int NOT NULL DEFAULT 0 COMMENT '当前可用积分',
  `cum_points` int NOT NULL DEFAULT 0 COMMENT '累计获得积分（决定等级）',
  `level_name` varchar(16) NOT NULL DEFAULT '普通会员' COMMENT '普通会员/白银会员/黄金会员',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_member_phone`(`phone`),
  UNIQUE INDEX `uk_member_user`(`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 迁移：会员绑定登录账号 ----------------------------
SET @has_member_user := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_member' AND COLUMN_NAME = 'user_id'
);
SET @sql_member_user := IF(@has_member_user = 0,
  'ALTER TABLE `boonie_member` ADD COLUMN `user_id` bigint DEFAULT NULL COMMENT ''绑定账号 boonie_user.id（账号买票积分归该账号）'' AFTER `id`, ADD UNIQUE INDEX `uk_member_user`(`user_id`), MODIFY COLUMN `phone` varchar(20) NULL COMMENT ''手机号（游客会员标识）''',
  'SELECT 1');
PREPARE _st_mu FROM @sql_member_user; EXECUTE _st_mu; DEALLOCATE PREPARE _st_mu;

-- ---------------------------- 积分明细表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_point_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '会员 id',
  `log_type` varchar(16) NOT NULL COMMENT '获取 / 抵扣 / 退款 / 扣回',
  `points` int NOT NULL COMMENT '积分变动（获取为正、抵扣为负）',
  `balance` int NOT NULL DEFAULT 0 COMMENT '变动后余额',
  `order_no` varchar(32) DEFAULT NULL COMMENT '关联订单号',
  `remark` varchar(128) DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_log_member`(`member_id`),
  INDEX `idx_log_order`(`order_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 小游戏成绩表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_game_score` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT '用户 id（后台手工补录的成绩为 NULL）',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名快照',
  `game_id` varchar(32) NOT NULL COMMENT '游戏编码：honey/mole/match',
  `game_name` varchar(64) DEFAULT NULL COMMENT '游戏名称',
  `best_score` int NOT NULL DEFAULT 0 COMMENT '最佳成绩（match 为最少步数，越小越好）',
  `plays` int NOT NULL DEFAULT 0 COMMENT '游玩局数',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_game`(`user_id`, `game_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 迁移：游戏成绩表允许后台补录（user_id 可空） ----------------------------
SET @has_game_uid_nn := (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_game_score' AND COLUMN_NAME = 'user_id'
);
SET @sql_game_uid := IF(@has_game_uid_nn = 'NO',
  'ALTER TABLE `boonie_game_score` MODIFY COLUMN `user_id` bigint DEFAULT NULL COMMENT ''用户 id（后台手工补录的成绩为 NULL）''',
  'SELECT 1');
PREPARE _st_gu FROM @sql_game_uid; EXECUTE _st_gu; DEALLOCATE PREPARE _st_gu;

-- ---------------------------- 种子：5 种门票（含儿童/老人优惠，幂等 upsert） ----------------------------
INSERT INTO `boonie_ticket`
  (`id`, `type_code`, `emoji`, `name`, `price`, `original_price`, `discount_label`, `tag_label`, `desc_text`, `color`, `sort_no`, `hot`, `features`, `enabled`)
VALUES
(1, 'adult', '🧑', '成人票', 199.00, 199.00, NULL, '标准',
 '1.5m 以上游客全天畅玩', '#e4572e', 1, 0, '全天畅玩所有项目\n含 1 场剧场演出', 1),
(2, 'child', '🧒', '儿童票', 99.00, 199.00, '5 折', '儿童优惠',
 '1.0m–1.5m 儿童 · 原价 ¥199 立减 100', '#ffb703', 2, 0, '1.0m–1.5m 儿童适用\n全天畅玩所有项目\n含 1 场剧场演出', 1),
(3, 'senior', '👴', '老人票', 99.00, 199.00, '5 折', '长者优惠',
 '65 周岁及以上长者 · 原价 ¥199 立减 100', '#7c5cbf', 3, 0, '65 周岁及以上长者适用\n全天畅玩所有项目\n含 1 场剧场演出', 1),
(4, 'family', '👨‍👩‍👧', '亲子套票', 399.00, 497.00, '套票', '热门',
 '2 大 1 小 全天畅玩 · 比单买省 ¥98', '#2f7d4f', 4, 1, '2 大 1 小全天畅玩\n含全部演出\n赠蜂蜜爆米花一份', 1),
(5, 'night', '🌙', '夜场票', 99.00, 99.00, NULL, '夜场',
 '17:00 后入园 · 含星光巡游', '#4caf6d', 5, 0, '17:00 后入园\n含星光巡游观赏位', 1)
ON DUPLICATE KEY UPDATE
  `type_code` = VALUES(`type_code`),
  `emoji` = VALUES(`emoji`),
  `name` = VALUES(`name`),
  `price` = VALUES(`price`),
  `original_price` = VALUES(`original_price`),
  `discount_label` = VALUES(`discount_label`),
  `tag_label` = VALUES(`tag_label`),
  `desc_text` = VALUES(`desc_text`),
  `color` = VALUES(`color`),
  `sort_no` = VALUES(`sort_no`),
  `hot` = VALUES(`hot`),
  `features` = VALUES(`features`),
  `enabled` = VALUES(`enabled`);

-- 票种编码兜底唯一化（老库若存在重复编码，按 id 修正；新库无副作用）
UPDATE `boonie_ticket` SET `type_code` = CONCAT('ticket_', `id`)
 WHERE `type_code` IS NULL OR `type_code` = '';

-- =============================================================
-- 后台扩展：轮播图 / 公告 / 评价审核状态
-- =============================================================

-- ---------------------------- 首页轮播图表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_banner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(64) NOT NULL COMMENT '轮播标题',
  `subtitle` varchar(200) DEFAULT NULL COMMENT '副标题/描述（前台 desc）',
  `tag` varchar(32) DEFAULT NULL COMMENT '角标文案，如 🎡 招牌项目',
  `img` varchar(255) DEFAULT NULL COMMENT '图片地址（/img/... 或 /api/avatar/...）',
  `link` varchar(64) DEFAULT NULL COMMENT '跳转板块：游乐项目/栖息地/演出/门票/风景',
  `color` varchar(16) DEFAULT '#2f7d4f' COMMENT '主题色',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '展示顺序，越小越靠前',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否上架：1 上架 0 下架',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 顶部公告表 ----------------------------
CREATE TABLE IF NOT EXISTS `boonie_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(255) NOT NULL COMMENT '公告内容（前台滚动播报）',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '展示顺序，越小越靠前',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否上架：1 上架 0 下架',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------- 评价审核状态迁移 ----------------------------
-- pending=待审核 / approved=已通过 / rejected=已驳回；存量数据默认已通过，不影响历史展示
SET @has_review_status := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'boonie_review' AND COLUMN_NAME = 'status'
);
SET @sql_review_status := IF(@has_review_status = 0,
  'ALTER TABLE `boonie_review` ADD COLUMN `status` varchar(16) NOT NULL DEFAULT ''approved'' COMMENT ''审核状态：pending/approved/rejected'' AFTER `content`',
  'SELECT 1');
PREPARE _st_rs FROM @sql_review_status; EXECUTE _st_rs; DEALLOCATE PREPARE _st_rs;

-- ---------------------------- 种子：轮播图（对齐首页原有 4 张，INSERT IGNORE 幂等） ----------------------------
INSERT IGNORE INTO `boonie_banner`
  (`id`, `title`, `subtitle`, `tag`, `img`, `link`, `color`, `sort_no`, `enabled`) VALUES
(1, '森林摩天轮', '360° 俯瞰整片狗熊岭林海，黄昏时最浪漫。', '🎡 招牌项目', '/img/ferris.jpeg', '游乐项目', '#7c5cbf', 1, 1),
(2, '林海过山车', '穿梭树冠间的极速轨道，熊二都直呼过瘾！', '🎢 尖叫首选', '/img/coaster.gif', '游乐项目', '#e4572e', 2, 1),
(3, '激流勇进漂流', '顺溪而下穿越原始森林，全程水花四溅。', '🛶 清凉一夏', '/img/rafting.jpeg', '游乐项目', '#2a9d8f', 3, 1),
(4, '小熊观光火车', '慢悠悠绕园一圈，熊大熊二沿途陪你打招呼。', '🚂 全家同游', '/img/train.jpeg', '游乐项目', '#4caf6d', 4, 1);

-- ---------------------------- 种子：顶部公告（对齐首页原有播报，INSERT IGNORE 幂等） ----------------------------
INSERT IGNORE INTO `boonie_notice` (`id`, `content`, `sort_no`, `enabled`) VALUES
(1, '🕘 开园时间：09:00 - 21:00（20:30 停止入园）', 1, 1),
(2, '☀️ 今日天气：晴 12~22°C，紫外线偏强，请注意防晒', 2, 1),
(3, '📌 临时通知：「树洞过山车」11月23日例行检修，暂停开放一天，敬请谅解', 3, 1);

-- =============================================================
-- 分类字典表（借鉴 java_travel 的 b_classification）
-- 用途：项目列表页「刺激/亲子/观光」筛选 + 后台项目编辑「标签」下拉
-- 统一由本表维护，前台读库，后台可增删改（enabled 控制是否展示）
-- =============================================================
CREATE TABLE IF NOT EXISTS `boonie_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL COMMENT '分类名称（如 刺激/亲子/观光）',
  `emoji` varchar(16) DEFAULT NULL COMMENT '分类图标（可选）',
  `color` varchar(16) DEFAULT NULL COMMENT '分类主题色（可选）',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '展示顺序，越小越靠前',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1 启用 0 停用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_category_name`(`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 种子分类（对齐原前端写死的 5 个标签 + 3 个筛选类，INSERT IGNORE 幂等）
INSERT IGNORE INTO `boonie_category` (`id`, `name`, `emoji`, `color`, `sort_no`, `enabled`) VALUES
(1, '全部', '🎡', '#2f7d4f', 0, 1),
(2, '刺激', '🎢', '#e4572e', 1, 1),
(3, '亲子', '👨‍👩‍👧', '#ffb703', 2, 1),
(4, '观光', '📷', '#7c5cbf', 3, 1),
(5, '浪漫', '💕', '#e4572e', 4, 1),
(6, '清凉', '🛶', '#2a9d8f', 5, 1),
(7, '益智', '🧩', '#4caf6d', 6, 1);

-- =============================================================
-- 访问统计表（借鉴 java_travel 的 b_op_log / VisitData）
-- 用途：记录每次前台访问，看板据此计算真实 PV/UV
-- =============================================================
CREATE TABLE IF NOT EXISTS `boonie_visit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ip` varchar(64) NOT NULL COMMENT '访问者 IP',
  `visit_date` date NOT NULL COMMENT '访问日期（便于按天聚合）',
  `visit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  `page` varchar(128) DEFAULT NULL COMMENT '访问路径（如 /、/attractions）',
  `user_agent` varchar(255) DEFAULT NULL COMMENT '浏览器标识（UV 辅助判断）',
  PRIMARY KEY (`id`),
  INDEX `idx_visit_date`(`visit_date`),
  INDEX `idx_visit_ip`(`ip`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'db-init.sql 执行完成（用户/栖息地/演出/门票/订单/成绩/轮播/公告/分类/访问统计 已就绪）' AS `result`;
