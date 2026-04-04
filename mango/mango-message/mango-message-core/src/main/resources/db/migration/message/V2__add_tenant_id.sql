-- =============================================
-- V2: Add tenant_id to sys_message for multi-tenancy
-- Date: 2026-04-04
-- =============================================

ALTER TABLE `sys_message` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER `id`;
