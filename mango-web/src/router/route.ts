import type { RouteRecordRaw } from 'vue-router';
import { generateStaticRoutes } from '@/config/menuConfig';

/**
 * 静态路由配置
 * @description 路由配置从 menuConfig.ts 加载，支持前端路由模式
 * @see src/config/menuConfig.ts - 菜单配置入口
 */

// 生成路由配置
export const staticRoutes: RouteRecordRaw[] = generateStaticRoutes();
