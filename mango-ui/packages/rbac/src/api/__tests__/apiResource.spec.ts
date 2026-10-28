import { beforeEach, describe, expect, it, vi } from 'vitest';
import { get, post, put } from '@mango/common/utils/request';
import { apiResourceApi } from '../apiResource';

vi.mock('@mango/common/utils/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
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

  it('sends manual takeover and keeps the permission code across mode switches', async () => {
    vi.mocked(post).mockResolvedValue(undefined);

    await apiResourceApi.saveManual({
      moduleName: 'mango-guarantee',
      httpMethod: 'POST',
      pathPattern: '/guarantee/open/apply',
      accessMode: 'PERMISSION',
      permissionCode: 'guarantee:apply:create',
      description: '上游回调',
    });
    await apiResourceApi.saveManual({
      moduleName: 'mango-guarantee',
      httpMethod: 'POST',
      pathPattern: '/guarantee/open/apply',
      accessMode: 'PUBLIC',
      permissionCode: 'ignored:code',
    });

    expect(post).toHaveBeenNthCalledWith(1, '/authorization/api-resources/manual', {
      moduleName: 'mango-guarantee',
      httpMethod: 'POST',
      pathPattern: '/guarantee/open/apply',
      accessMode: 'PERMISSION',
      permissionCode: 'guarantee:apply:create',
      description: '上游回调',
    });
    expect(post).toHaveBeenNthCalledWith(2, '/authorization/api-resources/manual', {
      moduleName: 'mango-guarantee',
      httpMethod: 'POST',
      pathPattern: '/guarantee/open/apply',
      accessMode: 'PUBLIC',
      permissionCode: 'ignored:code',
      description: undefined,
    });
  });

  it('releases manual takeover with route identity as query parameters', async () => {
    vi.mocked(put).mockResolvedValue(undefined);

    await apiResourceApi.releaseManual({
      moduleName: 'mango-guarantee',
      httpMethod: 'POST',
      pathPattern: '/guarantee/open/apply',
    });

    expect(put).toHaveBeenCalledWith('/authorization/api-resources/manual/release', undefined, {
      params: {
        moduleName: 'mango-guarantee',
        httpMethod: 'POST',
        pathPattern: '/guarantee/open/apply',
      },
    });
  });
});
