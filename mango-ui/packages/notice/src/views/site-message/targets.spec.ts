import { describe, expect, it } from 'vitest';
import { isSafeNoticePath, normalizeNoticeQuery, noticeFallbackTargetKey, resolveNoticeTargetPath } from './targets';

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
