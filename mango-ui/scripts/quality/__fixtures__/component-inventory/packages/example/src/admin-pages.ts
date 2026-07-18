export function registerFixturePages() {
  const pages = import.meta.glob('./pages/**/*.vue');
  return {
    pages,
    asyncCard: () => import('./components/AsyncCard.vue')
  };
}
