import { describe, expect, it, vi } from 'vitest';

vi.mock('../widgets/quick-entry', () => ({ systemQuickEntryWidgets: [] }));
vi.mock('../widgets/user-profile', () => ({ systemUserProfileWidgets: [] }));

import { registerMangoSystemAdminPages } from '../admin-pages';

describe('system admin pages', () => {
  it('registers the personal login log profile section', () => {
    const registration = registerMangoSystemAdminPages();

    expect(registration.profileSections).toHaveLength(1);
    expect(registration.profileSections[0]).toEqual(
      expect.objectContaining({
        key: 'login-log',
        label: '登录日志',
        group: '安全设置',
      }),
    );
  });
});
