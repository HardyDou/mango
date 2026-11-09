import { afterEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { registerMangoNoticeNavigationProvider, resetMangoNoticeNavigationProvider } from '@mango/admin-pages/notice';
import {
  NOTICE_MESSAGE_CENTER_TARGET_KEY,
  isSafeNoticePath,
  normalizeNoticeQuery,
  noticeFallbackTargetKey,
  openNoticeTarget,
  resolveNoticeTargetLocation,
  resolveNoticeTargetPath,
} from './targets';

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:pathMatch(.*)*', name: 'ShellMenu', component: {} }],
});

function createRouterWithMenuPath() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/profile', name: 'profile', component: {} },
      { path: '/notice/site-message', name: 'notice-site-message', component: {} },
      { path: '/:pathMatch(.*)*', name: 'ShellMenu', component: {} },
    ],
  });
}

afterEach(() => {
  resetMangoNoticeNavigationProvider();
});

describe('notice target paths', () => {
  it('resolves account profile targets to the shell profile path', () => {
    expect(resolveNoticeTargetPath('account:profile')).toBe('/profile');
  });

  it('keeps account password targets on the shell password path', () => {
    expect(resolveNoticeTargetPath('account:password')).toBe('/password');
  });

  it('leaves unknown targets for named-route resolution', () => {
    expect(resolveNoticeTargetPath('unknown:target')).toBeUndefined();
  });

  it('delegates ShellMenu path validation to the host navigation provider', () => {
    const resolveTargetPath = vi.fn((targetKey: string) =>
      targetKey === '/guarantee/finance/payment-vouchers' ? targetKey : undefined,
    );
    registerMangoNoticeNavigationProvider({ resolveTargetPath });

    expect(
      resolveNoticeTargetLocation(router, '/guarantee/finance/payment-vouchers', {
        receiptId: 'receipt-1',
        orderNo: 'order-1',
        focus: 'payment-confirm',
      }),
    ).toEqual({
      path: '/guarantee/finance/payment-vouchers',
      query: { receiptId: 'receipt-1', orderNo: 'order-1', focus: 'payment-confirm' },
    });
    expect(resolveTargetPath).toHaveBeenCalledWith('/guarantee/finance/payment-vouchers');
  });

  it('rejects a catch-all path when the host menu provider cannot resolve it', () => {
    registerMangoNoticeNavigationProvider({ resolveTargetPath: () => undefined });

    expect(resolveNoticeTargetLocation(router, '/guarantee/unknown')).toBeUndefined();
  });

  it('消息中心目标键不依赖菜单授权即可解析', () => {
    expect(resolveNoticeTargetPath(NOTICE_MESSAGE_CENTER_TARGET_KEY)).toBe('/profile');
    expect(
      resolveNoticeTargetLocation(createRouterWithMenuPath(), NOTICE_MESSAGE_CENTER_TARGET_KEY, {
        messageId: 'message-0',
      }),
    ).toEqual({ path: '/profile', query: { tab: 'notice-site-message' } });
  });

  it('未注册的目标退到 fallbackTargetKey', async () => {
    const provider = createRouterWithMenuPath();
    const resolution = await openNoticeTarget(provider, 'workflow:task:done', {
      fallbackTargetKey: '/notice/site-message',
      messageId: 'message-1',
    });

    expect(resolution).toBe('fallback');
    expect(provider.currentRoute.value.path).toBe('/notice/site-message');
  });

  it('主目标和 fallback 都不可解析时退到消息中心而不是死路', async () => {
    const provider = createRouterWithMenuPath();
    const resolution = await openNoticeTarget(provider, 'workflow:task:done', { messageId: 'message-2' });

    expect(resolution).toBe('message-center');
    expect(provider.currentRoute.value.path).toBe('/profile');
    expect(provider.currentRoute.value.query.tab).toBe('notice-site-message');
  });

  it('已在消息中心且目标不可解析时返回 none，由调用方提示原因', async () => {
    const provider = createRouterWithMenuPath();
    await provider.push({ path: '/profile', query: { tab: 'notice-site-message' } });

    expect(await openNoticeTarget(provider, 'workflow:task:done')).toBe('none');
    expect(provider.currentRoute.value.path).toBe('/profile');
  });

  it('可解析的目标直接打开主页面', async () => {
    const provider = createRouterWithMenuPath();
    const resolution = await openNoticeTarget(provider, 'account:profile', { messageId: 'message-3' });

    expect(resolution).toBe('primary');
    expect(provider.currentRoute.value.path).toBe('/profile');
  });

  it('只接受站内绝对路径并过滤导航元数据和对象查询参数', () => {
    expect(isSafeNoticePath('/workflow/business-form')).toBe(true);
    expect(isSafeNoticePath('//example.com/path')).toBe(false);
    expect(isSafeNoticePath('/workflow/detail?redirect=https://example.com')).toBe(false);
    expect(
      normalizeNoticeQuery({
        applyId: 1,
        fallbackTargetKey: 'workflow:task:done',
        messageId: 'message-1',
        clientIp: '127.0.0.1',
        token: 'secret',
        data: { secret: true },
      }),
    ).toEqual({ applyId: '1' });
    expect(noticeFallbackTargetKey({ fallbackTargetKey: 'workflow:task:done' })).toBe('workflow:task:done');
  });
});
