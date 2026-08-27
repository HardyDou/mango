import { beforeEach, describe, expect, it, vi } from 'vitest';

const request = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

vi.mock('@mango/common/utils/request', () => request);

import { workflowApi } from '../workflow';

describe('workflow business apply date range', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    request.post.mockResolvedValue({ list: [], total: 0 });
    request.get.mockResolvedValue({ list: [], total: 0 });
  });

  it('expands date-only business apply bounds for page queries', async () => {
    await workflowApi.businessAppliesPage({
      pageNum: 1,
      pageSize: 10,
      startedAtBegin: '2026-08-27',
      startedAtEnd: '2026-08-27',
    });

    expect(request.post).toHaveBeenCalledWith('/workflow/business-applies/page', {
      page: 1,
      size: 10,
      startedAtBegin: '2026-08-27 00:00:00',
      startedAtEnd: '2026-08-27 23:59:59',
    });
  });

  it('expands date-only business apply bounds for history queries', async () => {
    await workflowApi.businessApplyHistory('expense', 'EXP-1', {
      pageNum: 1,
      pageSize: 10,
      startedAtBegin: '2026-08-27',
      startedAtEnd: '2026-08-27',
    });

    expect(request.get).toHaveBeenCalledWith('/workflow/business-applies/history', {
      params: {
        page: 1,
        size: 10,
        startedAtBegin: '2026-08-27 00:00:00',
        startedAtEnd: '2026-08-27 23:59:59',
        categoryId: undefined,
        orgId: undefined,
        businessType: 'expense',
        businessKey: 'EXP-1',
      },
    });
  });
});
