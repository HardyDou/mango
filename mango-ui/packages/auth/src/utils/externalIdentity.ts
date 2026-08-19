import type { ExternalIdentityBinding } from '../api/identity';

export function externalIdentityLabel(binding?: ExternalIdentityBinding) {
  const displayName = binding?.displayName?.trim();
  if (displayName) return displayName;
  return `${providerLabel(binding?.provider)}资料未同步`;
}

export function externalIdentityHint(binding?: ExternalIdentityBinding) {
  const externalUserId = binding?.externalUserId?.trim();
  if (!externalUserId) return '';
  const suffix = externalUserId.replace(/\*/gu, '').slice(-4);
  return suffix ? `${providerLabel(binding?.provider)}账号尾号 ${suffix}` : '';
}

export function externalIdentityNeedsSync(binding?: ExternalIdentityBinding) {
  return !binding?.displayName?.trim();
}

export function externalIdentitySyncMessage(binding?: ExternalIdentityBinding) {
  if (binding?.provider === 'WECOM') return '请点击右侧同步按钮获取企业微信资料';
  if (binding?.provider === 'DINGTALK') return '请重新绑定或联系管理员同步钉钉账号资料';
  return '请重新绑定或联系管理员同步第三方账号资料';
}

function providerLabel(provider?: ExternalIdentityBinding['provider']) {
  if (provider === 'WECOM') return '企业微信';
  if (provider === 'DINGTALK') return '钉钉';
  return '第三方账号';
}
