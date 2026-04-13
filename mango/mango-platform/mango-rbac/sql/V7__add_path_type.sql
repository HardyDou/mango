-- Add path_type column to sys_public_path for internal API support
-- 1=anonymous, 2=login, 3=permission, 4=internal

ALTER TABLE sys_public_path ADD COLUMN IF NOT EXISTS path_type INT DEFAULT 1 COMMENT '1=公开, 2=需登录, 3=权限, 4=内部专用';

-- Initialize existing records: public paths are type=1 (anonymous)
UPDATE sys_public_path SET path_type = 1 WHERE path_type IS NULL OR path_type = 0;
