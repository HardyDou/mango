import { beforeEach, describe, expect, it, vi } from 'vitest';

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
        ]),
      }),
    );
    expect(registration.profileSections.map((section) => section.key)).toEqual([
      'notice-site-message',
      'notice-announcement-user',
      'notice-receive-setting',
    ]);
    expect(registration.profileSections.every((section) => section.group === '消息中心')).toBe(true);
  });
});
