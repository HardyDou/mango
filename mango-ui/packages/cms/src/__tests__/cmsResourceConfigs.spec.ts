import { describe, expect, it, vi } from 'vitest';
import type { CmsApi, CmsPageQuery, CmsRequestOptions } from '../api/cms';
import { createCmsResourceConfigs } from '../views/configs';

describe('CMS resource request lifecycle', () => {
  it('forwards the active AbortSignal from every resource page configuration', async () => {
    const calls: Array<{ method: string; options?: CmsRequestOptions }> = [];
    const cmsApi = new Proxy(
      {},
      {
        get(_target, property) {
          return vi.fn(async (...args: unknown[]) => {
            calls.push({ method: String(property), options: args.at(-1) as CmsRequestOptions });
            return String(property).startsWith('tree') ? [] : { list: [], total: 0, pageNum: 1, pageSize: 10 };
          });
        },
      },
    ) as CmsApi;
    const controller = new AbortController();
    const query: CmsPageQuery = { pageNum: 1, pageSize: 10 };

    await Promise.all(
      Object.values(createCmsResourceConfigs(cmsApi)).map((config) =>
        config.page(query, { signal: controller.signal }),
      ),
    );

    expect(calls).toHaveLength(9);
    expect(calls.map((call) => call.method).sort()).toEqual([
      'pageAdDeliveries',
      'pageAdvertisements',
      'pageContentTags',
      'pageContents',
      'pageNavigations',
      'pagePublishes',
      'pageSites',
      'treeContentCategories',
      'treeSiteCategories',
    ]);
    expect(calls.every((call) => call.options?.signal === controller.signal)).toBe(true);
  });
});
