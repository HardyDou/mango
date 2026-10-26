import { beforeEach, describe, expect, it, vi } from 'vitest';
import { get } from '@mango/common/utils/request';
import { apiResourceApi } from '../apiResource';

vi.mock('@mango/common/utils/request', () => ({
  get: vi.fn(),
}));

describe('api resource API contract', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps page and filter fields to the backend query contract', async () => {
    vi.mocked(get).mockResolvedValue({
      list: [
        {
          id: '123',
          moduleName: 'mango-infra-doc',
          domain: 'root',
          source: 'FIXED_DECLARATION',
          httpMethod: 'GET',
          pathPattern: '/favicon.ico',
          accessMode: 'PUBLIC',
          status: 1,
        },
      ],
      total: '1',
      page: '1',
      size: '10',
    });

    const result = await apiResourceApi.page({
      pageNum: 1,
      pageSize: 10,
      moduleName: 'mango-infra-doc',
      domain: 'root',
      source: 'FIXED_DECLARATION',
      accessMode: 'PUBLIC',
      status: 1,
      keyword: 'favicon',
    });

    expect(get).toHaveBeenCalledWith('/authorization/api-resources/page', {
      params: {
        page: 1,
        size: 10,
        moduleName: 'mango-infra-doc',
        domain: 'root',
        source: 'FIXED_DECLARATION',
        accessMode: 'PUBLIC',
        status: 1,
        keyword: 'favicon',
      },
    });
    expect(result).toMatchObject({ total: 1, pageNum: 1, pageSize: 10 });
    expect(result.list[0]).toMatchObject({
      source: 'FIXED_DECLARATION',
      domain: 'root',
      pathPattern: '/favicon.ico',
    });
  });

  it('omits empty optional filters', async () => {
    vi.mocked(get).mockResolvedValue({ list: [], total: 0, page: 1, size: 10 });

    await apiResourceApi.page({
      pageNum: 1,
      pageSize: 10,
      moduleName: '',
      domain: '',
      keyword: '',
    });

    expect(get).toHaveBeenCalledWith('/authorization/api-resources/page', {
      params: {
        page: 1,
        size: 10,
        moduleName: undefined,
        domain: undefined,
        source: undefined,
        accessMode: undefined,
        status: undefined,
        keyword: undefined,
      },
    });
  });
});
