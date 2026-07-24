import { describe, expect, it } from 'vitest';
import { resolveNoticeTargetPath } from './targets';

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
});
