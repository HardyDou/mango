import type { HomePageVO } from '@mango/home';

export function sortableHomePages(pages: HomePageVO[]): HomePageVO[] {
  return pages.filter(page => Boolean(page.id && !page.builtIn && !page.readOnly));
}

export function moveSortableHomePage(
  pages: HomePageVO[],
  currentPageId: string,
  offset: -1 | 1,
): HomePageVO[] {
  const sortedPages = sortableHomePages(pages);
  const currentIndex = sortedPages.findIndex(page => String(page.id) === currentPageId);
  const targetIndex = currentIndex + offset;
  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= sortedPages.length) {
    return sortedPages;
  }
  const [moving] = sortedPages.splice(currentIndex, 1);
  sortedPages.splice(targetIndex, 0, moving);
  return sortedPages;
}
