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

  it('注册旧站内信路径为指向当前页面的隐藏兼容入口', () => {
    registerMangoNoticeAdminPages();

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
  });
});
