import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Session } from '@/utils/storage';
import { useUserInfo } from '@/stores/userInfo';
import { useThemeConfig } from '@/stores/themeConfig';
import { staticRoutes } from './route';
import { initBackEndControlRoutes } from './backEnd';
import { getFrontEndRoutes } from './frontEnd';

// 声明 window 类型扩展
declare global {
  interface Window {
    __MANGO_ROUTER__: ReturnType<typeof createRouter>;
  }
}

const router = createRouter({
  // 使用 hash 模式
  history: createWebHashHistory(),
  routes: staticRoutes,
  // 路由切换时滚动到顶部
  scrollBehavior: () => ({ left: 0, top: 0 }),
});

// 挂载 router 实例到 window
window.__MANGO_ROUTER__ = router;

let isRoutesInitialized = false;

/**
 * 路由守卫：初始化路由
 */
async function initRoutes(): Promise<void> {
  if (isRoutesInitialized) return;

  const storesThemeConfig = useThemeConfig();
  const { isRequestRoutes } = storesThemeConfig.themeConfig;

  if (isRequestRoutes) {
    // 后端路由模式
    await initBackEndControlRoutes();
  } else {
    // 前端路由模式
    const frontEndRoutes = getFrontEndRoutes();
    frontEndRoutes.forEach((route) => {
      router.addRoute(route as RouteRecordRaw);
    });
  }

  isRoutesInitialized = true;
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
  const whiteList = ['/login', '/404', '/401', '/home'];
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

      // 确保路由已初始化
      await initRoutes();

      // 动态添加路由后需要确保路由已完全加载
      if (!isRoutesInitialized) {
        await initRoutes();
      }

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

export default router;
