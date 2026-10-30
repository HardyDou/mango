import { afterEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { registerMangoNoticeNavigationProvider, resetMangoNoticeNavigationProvider } from '@mango/admin-pages/notice';
import {
  isSafeNoticePath,
  normalizeNoticeQuery,
  noticeFallbackTargetKey,
  resolveNoticeTargetLocation,
  resolveNoticeTargetPath,
} from './targets';

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:pathMatch(.*)*', name: 'ShellMenu', component: {} }],
});

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
