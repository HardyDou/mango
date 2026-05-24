import type { RouteRecordRaw } from 'vue-router';
import { generateStaticRoutes } from '@/config/menuConfig';

/**
 * 静态路由配置
 * @description 路由配置从 menuConfig 加载，后端授权菜单由登录后动态路由加载
 * @see src/config/menuConfig.ts - 菜单配置入口
 */

// 生成路由配置
export const staticRoutes: RouteRecordRaw[] = generateStaticRoutes();
