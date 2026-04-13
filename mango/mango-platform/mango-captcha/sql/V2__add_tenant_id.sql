-- =============================================
-- V2: Add tenant_id to captcha tables for multi-tenancy
-- Date: 2026-04-04
-- =============================================

ALTER TABLE `captcha_code` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER `id`;
