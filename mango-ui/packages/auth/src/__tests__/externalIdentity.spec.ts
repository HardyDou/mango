import { describe, expect, it } from 'vitest';
import type { ExternalIdentityBinding } from '../api/identity';
import {
  externalIdentityHint,
  externalIdentityLabel,
  externalIdentityNeedsSync,
  externalIdentitySyncMessage,
} from '../utils/externalIdentity';

function binding(overrides: Partial<ExternalIdentityBinding>): ExternalIdentityBinding {
  return {
    id: 'binding-1',
    userId: 'mango-user-1',
    appCode: 'internal-admin',
    provider: 'WECOM',
    ...overrides,
  };
}

describe('externalIdentityLabel', () => {
  it('优先展示与 Mango 用户名不同的第三方显示名', () => {
    expect(externalIdentityLabel(binding({ displayName: '企业微信张三', externalUserId: '****4826' }))).toBe(
      '企业微信张三',
    );
  });

  it('第三方显示名缺失时明确提示资料未同步', () => {
    const missingProfile = binding({ externalUserId: '****4826' });

    expect(externalIdentityLabel(missingProfile)).toBe('企业微信资料未同步');
    expect(externalIdentityNeedsSync(missingProfile)).toBe(true);
    expect(externalIdentitySyncMessage(missingProfile)).toBe('请点击右侧同步按钮获取企业微信资料');
  });

  it('掩码外部标识只作为账号尾号辅助信息', () => {
    expect(externalIdentityHint(binding({ displayName: '企业微信张三', externalUserId: '****4826' }))).toBe(
      '企业微信账号尾号 4826',
    );
  });

  it('第三方平台没有可展示身份时不伪造账号标识', () => {
    expect(externalIdentityLabel(binding({}))).toBe('企业微信资料未同步');
    expect(externalIdentityHint(binding({}))).toBe('');
  });
});
