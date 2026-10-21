-- Banner 能力已由广告位 + 广告投放取代，且后台菜单在 2026-07-14 已移除。
-- 本迁移只清理 CMS 自有表；resource_registry 中历史 CMS_BANNER 行的清理见模块 README「升级与清理」。
DROP TABLE IF EXISTS `cms_banner`;
