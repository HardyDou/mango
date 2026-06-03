import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

describe('admin shell router', () => {
  it('redirects root path to home so shell does not render 404 for /', () => {
    const routerSource = readFileSync(resolve(__dirname, '../router.ts'), 'utf-8');

    expect(routerSource).toContain("path: '/'");
    expect(routerSource).toContain("redirect: '/home'");
  });

  it('installs auth guard inside the router factory used by createMangoAdminApp', () => {
    const routerSource = readFileSync(resolve(__dirname, '../router.ts'), 'utf-8');
    const factoryBody = routerSource.slice(
      routerSource.indexOf('export function createMangoAdminRouter()'),
      routerSource.indexOf('export const router = createMangoAdminRouter()'),
    );

    expect(factoryBody).toContain('router.beforeEach');
    expect(factoryBody).toContain("return '/login'");
  });
});
