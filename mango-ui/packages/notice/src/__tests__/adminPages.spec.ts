import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

const adminPagesMock = vi.hoisted(() => ({
  registerModulePages: vi.fn(),
}));
vi.mock('@mango/admin-pages/core', () => adminPagesMock);

import { registerMangoNoticeAdminPages } from '../admin-pages';

describe('notice admin pages', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('注册隐藏兼容入口', () => {
    const registration = registerMangoNoticeAdminPages();

    expect(adminPagesMock.registerModulePages).toHaveBeenCalledTimes(1);
    expect(adminPagesMock.registerModulePages).toHaveBeenCalledWith(
      expect.objectContaining({
        moduleCode: 'mango-notice',
        routes: expect.arrayContaining([
          expect.objectContaining({
            path: '/notice/site-message',
            component: 'notice/site-message/index',
            visible: 0,
          }),
          expect.objectContaining({
            path: '/notice/setting',
            component: 'notice/setting/index',
            visible: 0,
          }),
        ]),
        pages: expect.objectContaining({
          'notice/inbound/index': expect.any(Function),
        }),
      }),
    );
    expect(registration.profileSections.map((section) => section.key)).toEqual([
      'notice-site-message',
      'notice-announcement-user',
      'notice-receive-setting',
    ]);
    expect(registration.profileSections.every((section) => section.group === '消息中心')).toBe(true);
  });

  it('企业微信接收密钥使用专用字段并从普通配置中剥离', () => {
    const source = readFileSync(resolve(packageRoot, 'src/views/channel/index.vue'), 'utf-8');

    expect(source).toContain('v-model="channelConfig.callbackToken"');
    expect(source).toContain('v-model="channelConfig.encodingAesKey"');
    expect(source).toContain("{ key: 'callbackToken', label: '回调 Token' }");
    expect(source).toContain("{ key: 'encodingAesKey', label: 'EncodingAESKey' }");
    expect(source).toMatch(/'callbacktoken'[\s\S]*'encodingaeskey'[\s\S]*'callbackencodingaeskey'/);
    expect(source).toContain('splitSensitiveConfig(resolvedConfigJson)');
    expect(source).toContain('secretValues: Object.entries');
  });

  it('渠道页面显式展示并提交收发用途', () => {
    const source = readFileSync(resolve(packageRoot, 'src/views/channel/index.vue'), 'utf-8');

    expect(source).toContain('data-field="notice.channel.capability-mode"');
    expect(source).toContain("{ label: '仅发送', value: 'SEND' }");
    expect(source).toContain("{ label: '仅接收', value: 'RECEIVE' }");
    expect(source).toContain("{ label: '收发一体', value: 'BOTH' }");
    expect(source).toContain('capabilityMode: form.capabilityMode');
    expect(source).toContain('data-surface="notice.channel.send-config"');
    expect(source).toContain('data-surface="notice.channel.receive-config"');
    expect(source).toContain('v-model="channelConfig.inboundProtocol"');
    expect(source).toContain('v-model="channelConfig.inboundPassword"');
  });
});
