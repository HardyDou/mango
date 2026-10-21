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

    expect(calls).toHaveLength(10);
    expect(calls.map((call) => call.method).sort()).toEqual([
      'pageAdDeliveries',
      'pageAdvertisements',
      'pageBanners',
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

describe('CMS public site delivery gaps', () => {
  const calls: Record<string, number> = {};
  const stubs = new Map<string, ReturnType<typeof vi.fn>>();
  const cmsApi = new Proxy(
    {},
    {
      get(_target, property) {
        const method = String(property);
        if (!stubs.has(method)) {
          stubs.set(
            method,
            vi.fn(async () => {
              calls[method] = (calls[method] ?? 0) + 1;
              return method.startsWith('tree') ? [] : { list: [], total: 0 };
            }),
          );
        }
        return stubs.get(method);
      },
    },
  ) as CmsApi;
  const configs = createCmsResourceConfigs(cmsApi);

  it('exposes a banner management page bound to the banner APIs', async () => {
    const banner = configs.bannerConfig;

    expect(banner.key).toBe('banners');
    expect(banner.columns.map((column) => column.prop)).toEqual(
      expect.arrayContaining(['title', 'siteId', 'mediaType', 'status', 'sort']),
    );
    expect(banner.editor?.fields.map((field) => field.prop)).toEqual(
      expect.arrayContaining(['siteId', 'position', 'title', 'mediaFileId', 'startTime', 'endTime', 'sort']),
    );

    const mediaField = banner.editor?.fields.find((field) => field.prop === 'mediaFileId');
    expect(mediaField?.type).toBe('upload');
    expect(mediaField?.required).toBe(true);

    await banner.page({ pageNum: 1, pageSize: 10 });
    await banner.create({ title: 'banner' });
    await banner.update({ id: 1 });
    await banner.updateStatus?.(1, 'ENABLED');
    await banner.remove?.(1);

    expect(calls.pageBanners).toBe(1);
    expect(calls.createBanner).toBe(1);
    expect(calls.updateBanner).toBe(1);
    expect(calls.updateBannerStatus).toBe(1);
    expect(calls.deleteBanner).toBe(1);
  });

  it('exposes the content code in the editor and the detail view', () => {
    expect(configs.contentConfig.editor?.fields.map((field) => field.prop)).toContain('code');
    expect(
      configs.contentConfig.detail?.groups.flatMap((group) => group.fields.map((field) => field.prop)),
    ).toContain('code');
  });
});
