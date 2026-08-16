import { beforeEach, describe, expect, it, vi } from 'vitest';
import { get } from '@mango/common/utils/request';
import { postApi } from '../post';

vi.mock('@mango/common/utils/request', () => ({
  del: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

describe('post API pagination contract', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('normalizes backend numeric strings before exposing page data to Element Plus', async () => {
    vi.mocked(get).mockResolvedValue({
      records: [{ id: '2083696618563768322', postName: '测试岗位', postCode: 'TEST' }],
      total: '21',
      current: '2',
      size: '10',
    });

    const result = await postApi.page({ pageNum: 2, pageSize: 10 });

    expect(get).toHaveBeenCalledWith('/post/page', { params: { page: 2, size: 10 } });
    expect(result).toMatchObject({ total: 21, pageNum: 2, pageSize: 10 });
    expect(result.list).toHaveLength(1);
  });
});
