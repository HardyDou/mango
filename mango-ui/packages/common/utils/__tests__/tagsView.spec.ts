import { describe, expect, it } from 'vitest';
import { createHomeTag, isHomeTag, normalizeTagsViewRoutes } from '../tagsView';

describe('tagsView', () => {
  it('keeps home tag as the first affix tag', () => {
    const tags = normalizeTagsViewRoutes([
      { path: '/system', name: 'System', meta: { title: '系统管理' } },
      { path: '/home', name: 'CustomHome', meta: { title: '首页' } },
      { path: '/file', name: 'File', meta: { title: '文件中心' } },
    ] as any);

    expect(tags.map(tag => tag.path)).toEqual(['/home', '/system', '/file']);
    expect(tags[0].meta?.isAffix).toBe(true);
  });

  it('creates home tag when tags do not include it', () => {
    const tags = normalizeTagsViewRoutes([
      { path: '/system', name: 'System', meta: { title: '系统管理' } },
    ] as any);

    expect(tags[0]).toMatchObject(createHomeTag());
    expect(isHomeTag(tags[0])).toBe(true);
  });

  it('deduplicates tags by path', () => {
    const tags = normalizeTagsViewRoutes([
      { path: '/file', name: 'FileA', meta: { title: '文件中心' } },
      { path: '/file', name: 'FileB', meta: { title: '文件中心重复' } },
    ] as any);

    expect(tags.map(tag => tag.path)).toEqual(['/home', '/file']);
    expect(tags[1].name).toBe('FileA');
  });
});
