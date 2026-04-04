CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`          BIGINT(20) NOT NULL COMMENT '主键' PRIMARY KEY,
    `config_key`  VARCHAR(100) NOT NULL COMMENT '配置键' UNIQUE,
    `config_value` TEXT NOT NULL COMMENT '配置值',
    `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `type`        VARCHAR(20) NOT NULL COMMENT '类型: SYSTEM/BUSINESS/SECURITY/FEATURE',
    `sort`        INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_config_key` (`config_key`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `config_name`, `type`, `sort`, `status`) VALUES
(1, 'sys.index.skinName', 'skin-blue', '皮肤名称', 'SYSTEM', 1, 1),
(2, 'sys.account.captchaEnabled', 'true', '验证码开关', 'SECURITY', 1, 1),
(3, 'sys.account.registerEnabled', 'false', '注册开关', 'SECURITY', 2, 1),
(4, 'sys.login.lockCount', '5', '登录锁定次数', 'SECURITY', 3, 1);
