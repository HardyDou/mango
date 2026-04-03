/**
 * MSW Browser Entry - 浏览器环境 Mock
 *
 * 使用方式：
 * 1. 开发环境自动启用（在 main.ts 中根据 VITE_USE_MOCK 控制）
 * 2. 可通过 window.__ENABLE_MOCK__ 手动开启
 */

import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);

// 启动 MSW 服务
export async function startMockWorker() {
  // 确认真实 API 请求不被拦截（只在开发环境使用）
  if (import.meta.env.DEV || window.__ENABLE_MOCK__) {
    await worker.start({
      // 安静模式，不在控制台输出请求拦截信息
      quiet: true,
      // 允许在生产环境通过 window.__ENABLE_MOCK__ 开启
      onUnhandledRequest: 'bypass',
    });
    console.log('[MSW] Mock Service Worker started');
  }
}

export { handlers };
