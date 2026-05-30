import { describe, expect, it } from 'vitest';
import { defineComponent } from 'vue';
import {
  configureMangoAdminShell,
  getMangoAdminShellOptions,
} from '../config';
import {
  getLastShellRuntimeConfigDiagnostics,
  getLastShellRuntimeDecision,
} from '../runtime/runtimeHost';
import type { RuntimeDecision } from '../runtime/runtimeHost';

describe('@mango/admin-shell options contract', () => {
  it('merges shell options without dropping nested extension points', () => {
    const loginBrand = defineComponent({ name: 'LoginBrand' });
    const loginFooter = defineComponent({ name: 'LoginFooter' });
    const customShell = defineComponent({ name: 'CustomShell' });

    configureMangoAdminShell({
      title: 'Tenant Console',
      apiBaseUrl: '/tenant-api',
      components: {
        shell: customShell,
      },
      login: {
        brand: {
          title: 'Tenant Console',
        },
        slots: {
          brand: loginBrand,
        },
        defaults: {
          appCode: 'tenant-admin',
        },
      },
      theme: {
        primary: '#1677ff',
        tokens: {
          '--tenant-nav-height': '52px',
        },
      },
    });

    configureMangoAdminShell({
      login: {
        brand: {
          subtitle: 'Operations',
        },
        slots: {
          footer: loginFooter,
        },
      },
      theme: {
        tokens: {
          '--tenant-content-gap': '12px',
        },
      },
    });

    const options = getMangoAdminShellOptions();
    expect(options.title).toBe('Tenant Console');
    expect(options.apiBaseUrl).toBe('/tenant-api');
    expect(options.components?.shell).toBe(customShell);
    expect(options.login?.brand?.title).toBe('Tenant Console');
    expect(options.login?.brand?.subtitle).toBe('Operations');
    expect(options.login?.slots?.brand).toBe(loginBrand);
    expect(options.login?.slots?.footer).toBe(loginFooter);
    expect(options.login?.defaults?.appCode).toBe('tenant-admin');
    expect(options.theme?.primary).toBe('#1677ff');
    expect(options.theme?.tokens?.['--tenant-nav-height']).toBe('52px');
    expect(options.theme?.tokens?.['--tenant-content-gap']).toBe('12px');
  });

  it('accepts menu loader and runtime debug options as stable public options', async () => {
    configureMangoAdminShell({
      runtimeDebug: true,
      menu: {
        appCode: 'ops-admin',
        loader: async ({ appCode }) => [
          {
            menuId: 'ops-home',
            appCode,
            moduleCode: 'ops',
            parentId: 0,
            menuType: 2,
            menuName: 'Ops Home',
            menuCode: 'ops:home',
            path: '/ops/home',
            component: 'ops/home',
            sort: 1,
            status: 1,
            visible: 1,
          },
        ],
      },
      modules: {
        ops: {
          mode: 'micro',
          runtimeCode: 'ops-remote',
          entry: 'https://ops.example.com/',
        },
      },
    });

    const options = getMangoAdminShellOptions();
    const menus = await options.menu?.loader?.({ appCode: options.menu.appCode || '' });
    expect(options.runtimeDebug).toBe(true);
    expect(menus?.[0].appCode).toBe('ops-admin');
    expect(options.modules?.ops.runtimeCode).toBe('ops-remote');
  });

  it('exposes runtime diagnostics through stable getters', () => {
    const decision: RuntimeDecision | undefined = getLastShellRuntimeDecision();
    const diagnostics = getLastShellRuntimeConfigDiagnostics();

    expect(decision).toBeUndefined();
    expect(diagnostics).toEqual([]);
  });
});
