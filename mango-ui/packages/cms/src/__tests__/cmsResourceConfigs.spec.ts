import { describe, expect, it, vi } from 'vitest';
import type { CmsApi, CmsPageQuery, CmsRequestOptions } from '../api/cms';
import { createCmsResourceConfigs } from '../views/configs';

describe('CMS resource request lifecycle', () => {
  it('keeps publish creation in the content center and exposes publish metadata', () => {
    const { publishConfig, contentConfig } = createCmsResourceConfigs({} as CmsApi);
    const topScope = publishConfig.editor?.fields.find((field) => field.prop === 'topScope');
    const recommendationType = publishConfig.editor?.fields.find((field) => field.prop === 'recommendationType');

    expect(publishConfig.canCreate).toBe(false);
    expect(contentConfig.canPublish).toBe(true);
    expect(publishConfig.title).toBe('发布记录');
    expect(topScope).toMatchObject({ type: 'select', optionKey: 'topScope' });
    expect(recommendationType).toMatchObject({ type: 'select', optionKey: 'recommendationType' });
    expect(publishConfig.editor?.fields.find((field) => field.prop === 'categoryId')).toMatchObject({ span: 24 });
    expect(
      publishConfig.normalizePayload?.({ contentId: '11', categoryId: '22' }, { contentId: '11', categoryId: '22' }),
    ).toMatchObject({
      contentIds: ['11'],
      categoryIds: ['22'],
      topScope: 'NONE',
      recommendationType: 'NONE',
    });
  });

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
