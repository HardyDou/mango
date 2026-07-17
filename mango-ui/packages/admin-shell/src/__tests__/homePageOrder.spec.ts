import { describe, expect, it } from 'vitest';
import type { HomePageVO } from '@mango/home';
import { moveSortableHomePage, sortableHomePages } from '../views/home/pageOrder';

function homePage(id: string, overrides: Partial<HomePageVO> = {}): HomePageVO {
  return {
    id,
    name: id,
    layoutJson: '{}',
    sort: 10,
    enabled: true,
    defaultPage: false,
    builtIn: false,
    ...overrides,
  };
}

describe('首页排序', () => {
  it('只排序个人首页，不向后端提交内置页和授权页 ID', () => {
    const pages = [
      homePage('system', { builtIn: true }),
      homePage('personal-a'),
      homePage('template-a', { readOnly: true, sourceType: 'PERSONAL_AUTH' }),
      homePage('personal-b'),
    ];

    expect(sortableHomePages(pages).map(page => page.id)).toEqual(['personal-a', 'personal-b']);
    expect(moveSortableHomePage(pages, 'personal-b', -1).map(page => page.id))
      .toEqual(['personal-b', 'personal-a']);
  });

  it('授权页不能触发个人首页排序', () => {
    const pages = [homePage('personal-a'), homePage('template-a', { readOnly: true })];

    expect(moveSortableHomePage(pages, 'template-a', -1).map(page => page.id))
      .toEqual(['personal-a']);
  });
});
