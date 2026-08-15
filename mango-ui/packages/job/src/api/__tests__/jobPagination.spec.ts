import { beforeEach, describe, expect, it, vi } from 'vitest';

const request = vi.hoisted(() => ({
  del: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

vi.mock('@mango/common/utils/request', () => request);

import { jobApi } from '../job';

describe('job pagination API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    request.get.mockResolvedValue({
      list: [],
      total: 37,
      page: 2,
      size: 25,
      pages: 2,
    });
  });

  it('maps every list query to the backend page and size contract', async () => {
    await jobApi.pageDefinitions({ pageNum: 2, pageSize: 25 });
    await jobApi.pageInstances({ pageNum: 2, pageSize: 25 });
    await jobApi.pageLogs({ pageNum: 2, pageSize: 25 });
    await jobApi.pageWorkers({ pageNum: 2, pageSize: 25 });
    await jobApi.pageAlarmRules({ pageNum: 2, pageSize: 25 });

    const endpoints = [
      '/job/definitions/page',
      '/job/instances/page',
      '/job/logs/page',
      '/job/workers/page',
      '/job/alarm-rules/page',
    ];
    endpoints.forEach((endpoint, index) => {
      expect(request.get).toHaveBeenNthCalledWith(index + 1, endpoint, {
        params: { page: 2, size: 25 },
      });
    });
  });

  it('normalizes backend page metadata to the public frontend result', async () => {
    const result = await jobApi.pageDefinitions({ pageNum: 1, pageSize: 10 });

    expect(result).toEqual({
      list: [],
      total: 37,
      pageNum: 2,
      pageSize: 25,
    });
  });

  it('removes empty filters without dropping pagination', async () => {
    await jobApi.pageDefinitions({
      pageNum: 3,
      pageSize: 10,
      keyword: 'guarantee',
      status: '',
    });

    expect(request.get).toHaveBeenCalledWith('/job/definitions/page', {
      params: {
        keyword: 'guarantee',
        page: 3,
        size: 10,
      },
    });
  });

  it('falls back to the requested pagination for an empty backend result', async () => {
    request.get.mockResolvedValueOnce({});

    const result = await jobApi.pageDefinitions({ pageNum: 4, pageSize: 50 });

    expect(result).toEqual({
      list: [],
      total: 0,
      pageNum: 4,
      pageSize: 50,
    });
  });

  it('propagates pagination request failures to the page error handler', async () => {
    const error = new Error('network unavailable');
    request.get.mockRejectedValueOnce(error);

    await expect(jobApi.pageDefinitions({ pageNum: 1, pageSize: 10 })).rejects.toBe(error);
  });
});
