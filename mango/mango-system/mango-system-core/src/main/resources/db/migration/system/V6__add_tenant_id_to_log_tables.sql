-- =============================================
-- V6: Add tenant_id to log tables for multi-tenancy
-- Date: 2026-04-04
-- =============================================

-- Add tenant_id to sys_login_log
ALTER TABLE `sys_login_log` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER `id`;

-- Add tenant_id to sys_operation_log
ALTER TABLE `sys_operation_log` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER `id`;
