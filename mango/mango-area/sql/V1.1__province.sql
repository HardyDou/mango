-- ----------------------------
-- mango-area 区划数据初始化
-- 数据来源: Administrative-divisions-of-China-2.7.0
-- 包含: 省/市/区/街道/村庄 5级数据
-- ----------------------------

DROP TABLE IF EXISTS `sys_area`;
CREATE TABLE `sys_area` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `pid` bigint NOT NULL DEFAULT 0 COMMENT '父级ID (0为根节点)',
  `name` varchar(100) NOT NULL COMMENT '地区名称',
  `letter` varchar(50) DEFAULT '' COMMENT '地区字母(用于拼音排序)',
  `adcode` bigint NOT NULL COMMENT '地区编码',
  `location` varchar(100) DEFAULT '' COMMENT '经纬度',
  `area_sort` int DEFAULT 0 COMMENT '排序值',
  `area_status` char(1) DEFAULT '1' COMMENT '状态 (0-未生效, 1-生效)',
  `area_type` char(1) DEFAULT '1' COMMENT '地区类型 (0-国家, 1-省/直辖市, 2-城市, 3-区县, 4-街道, 5-村庄)',
  `hot` char(1) DEFAULT '0' COMMENT '是否热门 (0-否, 1-是)',
  `city_code` varchar(20) DEFAULT '' COMMENT '城市编码',
  `tenant_id` bigint DEFAULT 1 COMMENT '租户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志 (0-正常, 1-删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adcode` (`adcode`),
  KEY `idx_pid` (`pid`),
  KEY `idx_area_type` (`area_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政区划表';

-- ----------------------------
-- 区划数据 (省/市/区/街道/村庄)
-- ----------------------------

-- ----------------------------
-- 省/直辖市 数据 (area_type = 1)
-- ----------------------------
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '北京市', 110000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '天津市', 120000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '河北省', 130000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '山西省', 140000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '内蒙古自治区', 150000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '辽宁省', 210000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '吉林省', 220000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '黑龙江省', 230000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '上海市', 310000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '江苏省', 320000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '浙江省', 330000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '安徽省', 340000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '福建省', 350000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '江西省', 360000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '山东省', 370000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '河南省', 410000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '湖北省', 420000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '湖南省', 430000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '广东省', 440000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '广西壮族自治区', 450000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '海南省', 460000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '重庆市', 500000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '四川省', 510000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '贵州省', 520000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '云南省', 530000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '西藏自治区', 540000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '陕西省', 610000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '甘肃省', 620000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '青海省', 630000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '宁夏回族自治区', 640000, '1', 0, '1', '0');
INSERT INTO `sys_area` (`id`, `pid`, `name`, `adcode`, `area_type`, `area_sort`, `area_status`, `hot`) VALUES (NULL, 1, '新疆维吾尔自治区', 650000, '1', 0, '1', '0');
