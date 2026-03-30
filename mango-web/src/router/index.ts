import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router';
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Session } from '@/utils/storage';
import { useUserInfo } from '@/stores/userInfo';
import { usePreferencesStore } from '@/stores/preferences';
import { useRoutesList } from '@/stores/routesList';
import { staticRoutes } from './route';
import { initBackEndControlRoutes } from './backEnd';
import { getFrontEndRoutes } from './frontEnd';

const router = createRouter({
  // 使用 hash 模式
  history: createWebHashHistory(),
  routes: staticRoutes,
  // 路由切换时滚动到顶部
  scrollBehavior: () => ({ left: 0, top: 0 }),
});

// 使用 reactive ref + promise lock 防止竞态条件
const isRoutesInitialized = ref(false);
let initPromise: Promise<void> | null = null;

/**
 * 路由守卫：初始化路由
 */
async function initRoutes(): Promise<void> {
  // 如果已经在初始化中，返回现有 promise 等待完成
  if (initPromise) {
    return initPromise;
  }

  // 如果已经初始化完成，直接返回
  if (isRoutesInitialized.value) {
    return;
  }

  initPromise = doInitRoutes().finally(() => {
    isRoutesInitialized.value = true;
    initPromise = null;
  });

  return initPromise;
}

async function doInitRoutes(): Promise<void> {
  const storesPreferences = usePreferencesStore();
  const storesRoutesList = useRoutesList();
  const isRequestRoutes = storesPreferences.isRequestRoutes;

  console.log('[Router] doInitRoutes called, isRequestRoutes:', isRequestRoutes);
  console.log('[Router] Current routes:', router.getRoutes().map(r => ({ path: r.path, name: r.name, children: r.children?.length })));

  if (isRequestRoutes) {
    // 后端路由模式
    await initBackEndControlRoutes();
    console.log('[Router] After backEnd init:', router.getRoutes().map(r => ({ path: r.path, name: r.name, children: r.children?.length })));
  } else {
    // 前端路由模式 - 静态路由已在 staticRoutes 中，直接使用
    // 填充 routesList store 以供菜单使用
    const staticRoute = staticRoutes.find((r) => r.path === '/');
    if (staticRoute && staticRoute.children) {
      storesRoutesList.setRoutesList(staticRoute.children);
    }
  }
}

/**
 * 全局前置守卫
 */
router.beforeEach(async (to, from, next) => {
  const storesUserInfo = useUserInfo();
  const title = (to.meta.title as string) || '';

  // 设置页面标题
  document.title = title ? `${title} - Mango Admin` : 'Mango Admin';

  // 白名单路由直接放行
  const whiteList = ['/login', '/404', '/401'];
  if (whiteList.includes(to.path)) {
    next();
    return;
  }

  // 检查 Token
  const token = Session.getToken();
  if (!token) {
    next('/login');
    return;
  }

  // 已登录，初始化用户信息
  try {
    const userInfo = Session.get('userInfo');
    if (userInfo) {
      storesUserInfo.setUserInfos(userInfo);

      // 确保路由已初始化（使用 promise lock 防止竞态条件）
      await initRoutes();

      next();
    } else {
      throw new Error('用户信息不存在');
    }
  } catch (error) {
    console.error('路由守卫失败:', error);
    Session.clearToken();
    ElMessage.error('登录已过期，请重新登录');
    next('/login');
  }
});

/**
 * 全局后置守卫
 */
router.afterEach((to) => {
  // 路由切换后的处理
});

export { router };
export default router;
