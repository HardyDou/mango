import { beforeEach, describe, expect, it, vi } from 'vitest';
import { get } from '@mango/common/utils/request';
import { loginLogApi, operationLogApi } from '../log';

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

  it('sends complete-day bounds for login statistics', async () => {
    vi.mocked(get).mockResolvedValue({});

    await loginLogApi.statistics({
      startTime: '2026-08-27',
      endTime: '2026-08-27',
    });

    expect(get).toHaveBeenCalledWith('/system/log/login/statistics', {
      params: {
        startTime: '2026-08-27 00:00:00',
        endTime: '2026-08-27 23:59:59',
      },
    });
  });

  it('sends complete-day bounds for operation log pages', async () => {
    vi.mocked(get).mockResolvedValue({ list: [], total: 0 });

    await operationLogApi.list({
      pageNum: 2,
      pageSize: 20,
      startTime: '2026-08-27',
      endTime: '2026-08-27',
    });

    expect(get).toHaveBeenCalledWith('/system/log/operation/list', {
      params: {
        page: 2,
        size: 20,
        startTime: '2026-08-27 00:00:00',
        endTime: '2026-08-27 23:59:59',
      },
    });
  });
});
