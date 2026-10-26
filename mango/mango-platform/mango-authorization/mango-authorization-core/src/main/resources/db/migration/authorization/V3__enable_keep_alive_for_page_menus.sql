-- 页面菜单默认开启路由缓存：切换页签保留搜索条件、分页和表单状态。
ALTER TABLE authorization_menu
  MODIFY COLUMN keep_alive tinyint NOT NULL DEFAULT '1' COMMENT '路由缓存: 0-不缓存, 1-缓存';

UPDATE authorization_menu
SET keep_alive = 1
WHERE menu_type = 2
  AND embedded = 0
  AND del_flag = 0;
