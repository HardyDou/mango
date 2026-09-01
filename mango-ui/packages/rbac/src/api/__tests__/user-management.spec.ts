import { beforeEach, describe, expect, it, vi } from 'vitest';
import { get, post } from '@mango/common/utils/request';
import { orgApi } from '../org';
import { roleApi } from '../role';
import { userApi } from '../user';

vi.mock('@mango/common/utils/request', () => ({
  del: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

describe('user management API contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps organization scope and candidate exclusion to identity paging parameters', async () => {
    vi.mocked(get).mockResolvedValue({ records: [], total: 0, current: 1, size: 50 });

    await userApi.page({
      pageNum: 1,
      pageSize: 50,
      keyword: 'candidate@example.com',
      orgIds: ['100', '101'],
      excludeOrgId: '101',
    });

    expect(get).toHaveBeenCalledWith('/identity/users/page', {
      params: expect.objectContaining({
        page: 1,
        size: 50,
        keyword: 'candidate@example.com',
        orgIds: '100,101',
        excludeOrgId: '101',
      }),
    });
  });

  it('uses the organization composite command when creating a member account', async () => {
    vi.mocked(post).mockResolvedValue('1001');
    const command = {
      orgId: '101',
      username: 'new-user',
      nickname: 'New User',
      primaryFlag: true,
      leaderFlag: false,
    };

    await orgApi.createMemberAccount(command);

    expect(post).toHaveBeenCalledWith('/org/member-accounts', command);
  });

  it('loads organization scope and direct roles through bounded batch endpoints', async () => {
    vi.mocked(get).mockResolvedValue(['100', '101']);
    vi.mocked(post).mockResolvedValue([]);

    await orgApi.memberScope('100');
    await roleApi.getSubjectRolesBatch(['2001', '2002']);

    expect(get).toHaveBeenCalledWith('/org/member-scope', { params: { orgId: '100' } });
    expect(post).toHaveBeenCalledWith('/authorization/roles/subjects/batch', {
      subjectIds: ['2001', '2002'],
    });
  });
});
