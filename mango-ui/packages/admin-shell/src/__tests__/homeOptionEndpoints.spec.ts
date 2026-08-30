import { readFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const request = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

vi.mock('@mango/common/utils/request', () => request);

import { homeOptionApi } from '@mango/home';

const packageRoot = resolve(import.meta.dirname, '..', '..');

describe('Home 用户候选窄接口', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('uses distinct Home permissions endpoints for both management pages', async () => {
    request.get.mockResolvedValue([]);

    await homeOptionApi.listPageUsers({ size: 200 });
    await homeOptionApi.listVisibleUsers({ keyword: 'admin', size: 50 });

    expect(request.get).toHaveBeenNthCalledWith(1, '/home/options/page-users', {
      params: { size: 200 },
    });
    expect(request.get).toHaveBeenNthCalledWith(2, '/home/options/visible-users', {
      params: { keyword: 'admin', size: 50 },
    });
  });

  it('does not couple Home pages to the Identity management endpoint', () => {
    const source = ['list/index.vue', 'user/index.vue']
      .map(file => readFileSync(join(packageRoot, 'src/views/home', file), 'utf8'))
      .join('\n');

    expect(source).not.toContain('/identity/users/page');
    expect(source).not.toMatch(/\buserApi\b/);
    expect(source).toContain('homeOptionApi.listPageUsers');
    expect(source).toContain('homeOptionApi.listVisibleUsers');
  });
});
