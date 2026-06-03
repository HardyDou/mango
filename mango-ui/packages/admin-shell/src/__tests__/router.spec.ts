import { afterEach, describe, expect, it } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

let token: string | null = null;

function createTestRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'Login', component: {} },
      { path: '/', name: 'Shell', redirect: '/home', meta: { isHide: true } },
      { path: '/:pathMatch(.*)*', name: 'ShellMenu', component: {}, meta: { isHide: true } },
    ],
  });

  router.beforeEach((to) => {
    if (to.path === '/login') {
      return true;
    }
    if (!token) {
      return '/login';
    }
    if (to.path === '/') {
      return '/home';
    }
    return true;
  });

  return router;
}

describe('admin shell router', () => {
  afterEach(() => {
    token = null;
  });

  it('redirects an authenticated root visit to home so shell does not render 404 for /', async () => {
    const router = createTestRouter();
    token = 'token';

    await router.push('/');

    expect(router.currentRoute.value.path).toBe('/home');
  });

  it('redirects an unauthenticated root visit to login without initializing shell pages', async () => {
    const router = createTestRouter();

    await router.push('/');

    expect(router.currentRoute.value.path).toBe('/login');
  });
});
