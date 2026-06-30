import { beforeEach, describe, expect, it } from 'vitest';
import { resetPersistedTagsView } from '../runtime/tagsViewSession';

function installLocalStorageMock() {
  const store = new Map<string, string>();
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    value: {
      clear: () => store.clear(),
      getItem: (key: string) => store.get(key) ?? null,
      setItem: (key: string, value: string) => store.set(key, value),
    },
  });
}

describe('tagsViewSession', () => {
  beforeEach(() => {
    installLocalStorageMock();
    localStorage.clear();
  });

  it('resets persisted opened tags to home when entering login flow', () => {
    localStorage.setItem(
      'mango-tags-view-routes',
      JSON.stringify({
        tagsViewRoutes: [
          { path: '/home', name: 'Home' },
          { path: '/system/menu', name: 'Menu' },
        ],
        isTagsViewCurrenFull: true,
        favoriteRoutes: [{ path: '/system/menu', name: 'Menu' }],
      })
    );

    resetPersistedTagsView();

    const persisted = JSON.parse(localStorage.getItem('mango-tags-view-routes') || '{}');
    expect(persisted.tagsViewRoutes.map((tag: { path: string }) => tag.path)).toEqual(['/home']);
    expect(persisted.tagsViewRoutes[0].meta.isAffix).toBe(true);
    expect(persisted.isTagsViewCurrenFull).toBe(false);
    expect(persisted.favoriteRoutes).toEqual([{ path: '/system/menu', name: 'Menu' }]);
  });
});
