import { beforeEach, describe, expect, it, vi } from 'vitest';
import { get } from '@mango/common/utils/request';
import { loginLogApi } from '../log';

vi.mock('@mango/common/utils/request', () => ({
  del: vi.fn(),
  get: vi.fn(),
}));

describe('login log statistics contract', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('normalizes backend numeric strings before exposing statistics to Element Plus', async () => {
    vi.mocked(get).mockResolvedValue({
      totalCount: '30',
      successCount: '25',
      failCount: '5',
      todayCount: '3',
      weekCount: '12',
      monthCount: null,
    });

    const result = await loginLogApi.statistics();

    expect(result).toEqual({
      totalCount: 30,
      successCount: 25,
      failCount: 5,
      todayCount: 3,
      weekCount: 12,
      monthCount: undefined,
    });
  });
});
