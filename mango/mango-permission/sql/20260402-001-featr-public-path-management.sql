-- Public path configuration table for dynamic access control
-- Supports: anonymous access, login-required, permission-required

CREATE TABLE IF NOT EXISTS sys_public_path (
    id          BIGINT NOT NULL COMMENT '主键ID',
    path        VARCHAR(255) NOT NULL COMMENT '路径（支持通配符如 /public/**）',
    path_type   TINYINT     NOT NULL DEFAULT 1 COMMENT '类型：1=匿名访问，2=登录即可，3=需要权限',
    description VARCHAR(255)          COMMENT '描述',
    priority    INT          NOT NULL DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    creator     VARCHAR(64)           COMMENT '创建人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)           COMMENT '更新人',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    INDEX idx_path (path),
    INDEX idx_type_status (path_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公共访问路径配置';

-- Insert default public paths
INSERT INTO sys_public_path (path, path_type, description, priority, status) VALUES
-- Anonymous access
('/auth/login', 1, '用户登录', 100, 1),
('/auth/captcha', 1, '获取验证码', 100, 1),
('/captcha/**', 1, '验证码图片', 100, 1),
('/kaptcha/**', 1, '验证码Kaptcha', 100, 1),
('/public/**', 1, '公开接口', 90, 1),
('/actuator/health', 1, '健康检查', 100, 1),
('/swagger-ui/**', 1, 'Swagger文档', 100, 1),
('/v3/api-docs/**', 1, 'OpenAPI文档', 100, 1),
('/swagger-resources/**', 1, 'Swagger资源', 100, 1),

-- Login required (but no specific permission)
('/auth/refresh', 2, '刷新Token', 100, 1),
('/auth/logout', 2, '退出登录', 100, 1);
