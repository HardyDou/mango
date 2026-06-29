import { createRouter, createWebHashHistory } from 'vue-router';
import { ref } from 'vue';
import { mangoMessage, Session } from '@mango/common';
import { useUserInfo } from '@/stores/userInfo';
import { usePreferencesStore } from '@/stores/preferences';
import { useRoutesList } from '@/stores/routesList';
import { staticRoutes } from './route';
import { initBackEndControlRoutes } from './backEnd';
import { menuLoader } from '@/config/menuLoader';

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
// 标记当前导航是否已经等待过路由初始化
let isNavigatingAfterInit = false;
let initializedContextKey = '';

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

  initPromise = doInitRoutes()
    .then(() => {
      isRoutesInitialized.value = true;
    })
    .finally(() => {
      initPromise = null;
    });

  return initPromise;
}

async function doInitRoutes(): Promise<void> {
  const storesPreferences = usePreferencesStore();
  const storesRoutesList = useRoutesList();
  // Mock 模式下强制启用后端路由模式，以加载完整的菜单（前端配置 + 后端配置）
  const useMock = import.meta.env.VITE_USE_MOCK === 'true';
  const isRequestRoutes = useMock || storesPreferences.isRequestRoutes;

  if (import.meta.env.DEV) console.log('[Router] doInitRoutes called, isRequestRoutes:', isRequestRoutes, '(useMock:', useMock, ')');
  if (import.meta.env.DEV) console.log('[Router] Current routes:', router.getRoutes().map(r => ({ path: r.path, name: r.name, children: r.children?.length })));

  if (isRequestRoutes) {
    // 后端路由模式
    await initBackEndControlRoutes();
    if (import.meta.env.DEV) console.log('[Router] After backEnd init:', router.getRoutes().map(r => ({ path: r.path, name: r.name, children: r.children?.length })));
  } else {
    // 前端路由模式 - 静态路由已在 staticRoutes 中，直接使用
    // 填充 routesList store 以供菜单使用
    const staticRoute = staticRoutes.find((r) => r.path === '/');
    if (staticRoute && staticRoute.children) {
      storesRoutesList.setRoutesList(staticRoute.children);
    }
  }
}

function routeContextKey(userInfo: any): string {
  return [
    userInfo?.userId ?? '',
    userInfo?.tenantId ?? '',
    userInfo?.appCode ?? '',
    userInfo?.realm ?? '',
    userInfo?.actorType ?? '',
    userInfo?.partyType ?? '',
    userInfo?.partyId ?? '',
  ].join('|');
}

function resetRoutesForContextChange(nextContextKey: string): void {
  if (!initializedContextKey || initializedContextKey === nextContextKey) {
    return;
  }
  isRoutesInitialized.value = false;
  initPromise = null;
  isNavigatingAfterInit = false;
  initializedContextKey = '';
  menuLoader.resetBackendCache();
  useRoutesList().resetRoutesList();
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
  const whiteList = ['/login', '/404', '/401', '/payment/gateway-result'];
  if (whiteList.includes(to.path)) {
    next();
    return;
  }

  // 检查 Token
  const token = Session.getToken();
  if (!token) {
    next({
      path: '/login',
      query: { redirect: to.fullPath },
    });
    return;
  }

  // 已登录，初始化用户信息
  try {
    const userInfo = Session.get('userInfo');
    if (userInfo) {
      storesUserInfo.setUserInfos(userInfo);
      const currentContextKey = routeContextKey(storesUserInfo.userInfos);
      resetRoutesForContextChange(currentContextKey);

      // 确保路由已初始化（使用 promise lock 防止竞态条件）
      if (!isRoutesInitialized.value && !isNavigatingAfterInit) {
        isNavigatingAfterInit = true;
        await initRoutes();
        initializedContextKey = currentContextKey;
        isNavigatingAfterInit = false;
        // 重新触发当前导航，让路由有机会匹配新加载的动态路由
        next(to.fullPath);
        return;
      }

      next();
    } else {
      throw new Error('用户信息不存在');
    }
  } catch (error) {
    console.error('路由守卫失败:', error);
    isNavigatingAfterInit = false;
    Session.clearSession();
    mangoMessage.error(error instanceof Error ? error.message : '登录已过期，请重新登录');
    next({
      path: '/login',
      query: { redirect: to.fullPath },
    });
  }
});

/**
 * 全局后置守卫
 */
router.afterEach(() => {
  // 路由切换后的处理
});

export { router };
export default router;
