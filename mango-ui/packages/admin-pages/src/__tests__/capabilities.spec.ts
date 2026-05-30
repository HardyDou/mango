import { describe, expect, it, vi } from 'vitest';
import {
  getPageLoader,
  normalizeComponentPath,
  registerCapabilities,
  registerCapabilityPages,
  resolveMangoCapabilityDependencies,
  resolvePageModuleCode,
  toPageRegistry,
  type MangoCapabilityManifest,
} from '../core';

vi.mock('@mango/workflow-business-example', () => ({
  registerWorkflowBusinessExampleComponents: vi.fn(),
  WorkflowBusinessFormView: { name: 'WorkflowBusinessFormView' },
}));

describe('admin page capabilities', () => {
  it('normalizes backend component paths to capability page keys', () => {
    expect(normalizeComponentPath('@/views/system/menu/index.vue')).toBe('system/menu/index');
    expect(normalizeComponentPath('/views/notice/site/messages/index.vue')).toBe('notice/site/messages/index');
    expect(normalizeComponentPath('src/views/file/files/index.vue')).toBe('file/files/index');
  });

  it('registers capability pages and resolves module ownership', async () => {
    const manifest: MangoCapabilityManifest = {
      moduleCode: 'mango-test-capability',
      packageName: '@mango/test-capability',
      capabilityCode: 'test',
      capabilityName: '测试能力',
      pages: [
        {
          component: 'test/page/index',
          loader: () => Promise.resolve({ name: 'TestPage' }),
          menuCode: 'test:page',
          permissions: ['test:page:view'],
        },
      ],
    };

    registerCapabilityPages(manifest);

    await expect(getPageLoader('mango-test-capability', '@/views/test/page/index.vue')?.()).resolves.toEqual({
      name: 'TestPage',
    });
    expect(resolvePageModuleCode('test/page/index')).toBe('mango-test-capability');
    expect(toPageRegistry(manifest).pages['test/page/index']).toBe(manifest.pages[0].loader);
  });

  it('registers selected capabilities without requiring default bundles', async () => {
    registerCapabilities([
      {
        moduleCode: 'mango-selected-one',
        packageName: '@mango/selected-one',
        capabilityCode: 'selected-one',
        capabilityName: '选择能力一',
        pages: [{ component: 'selected/one/index', loader: () => Promise.resolve('one') }],
      },
      {
        moduleCode: 'mango-selected-two',
        packageName: '@mango/selected-two',
        capabilityCode: 'selected-two',
        capabilityName: '选择能力二',
        pages: [{ component: 'selected/two/index', loader: () => Promise.resolve('two') }],
      },
    ]);

    await expect(getPageLoader('mango-selected-one', 'selected/one/index')?.()).resolves.toBe('one');
    await expect(getPageLoader('mango-selected-two', 'selected/two/index')?.()).resolves.toBe('two');
  });

  it('keeps page registrations in a runtime singleton shared by duplicate package instances', async () => {
    const registryKey = Symbol.for('mango.admin-pages.registry');
    const duplicateModuleState = (globalThis as typeof globalThis & {
      [registryKey]: {
        pageLoaders: Map<string, () => Promise<unknown>>;
        moduleByPage: Map<string, string>;
      };
    })[registryKey];

    duplicateModuleState.pageLoaders.set('business:letter/index', () => Promise.resolve('from-duplicate-instance'));
    duplicateModuleState.moduleByPage.set('letter/index', 'business');

    await expect(getPageLoader('business', 'letter/index')?.()).resolves.toBe('from-duplicate-instance');
    expect(resolvePageModuleCode('letter/index')).toBe('business');
  });

  it('exposes Mango built-in capabilities as composable manifests', async () => {
    const { mangoDefaultCapabilities } = await import('../defaults');
    const summary = mangoDefaultCapabilities.map(capability => ({
      moduleCode: capability.moduleCode,
      packageName: capability.packageName,
      pageCount: capability.pages.length,
    }));

    expect(summary).toEqual(
      expect.arrayContaining([
        { moduleCode: 'mango-authorization', packageName: '@mango/auth-admin', pageCount: 3 },
        { moduleCode: 'mango-authorization', packageName: '@mango/rbac-admin', pageCount: 8 },
        { moduleCode: 'mango-system', packageName: '@mango/system-admin', pageCount: 8 },
        { moduleCode: 'mango-file', packageName: '@mango/file-admin', pageCount: 3 },
        { moduleCode: 'mango-workflow', packageName: '@mango/workflow-admin', pageCount: 12 },
        { moduleCode: 'mango-notice', packageName: '@mango/notice-admin', pageCount: 11 },
        { moduleCode: 'mango-template', packageName: '@mango/template-admin', pageCount: 5 },
        { moduleCode: 'mango-numgen', packageName: '@mango/numgen-admin', pageCount: 2 },
        { moduleCode: 'mango-calendar', packageName: '@mango/calendar-admin', pageCount: 1 },
      ]),
    );
  });

  it('auto-installs required catalog capabilities before selected capabilities', () => {
    const auth = createCapability('auth');
    const rbac = createCapability('rbac', ['auth']);
    const system = createCapability('system', ['auth', 'rbac']);

    const resolution = resolveMangoCapabilityDependencies({
      preset: 'custom',
      selected: [system],
      catalog: [auth, rbac, system],
      autoInstallRequired: true,
    });

    expect(resolution.diagnostics).toEqual([]);
    expect(resolution.capabilities.map(capability => capability.capabilityCode)).toEqual([
      'auth',
      'rbac',
      'system',
    ]);
    expect(resolution.report.autoInstalledCodes).toEqual(['auth', 'rbac']);
    expect(resolution.report.nodes.find(node => node.capabilityCode === 'auth')?.requiredBy).toEqual(['system', 'rbac']);
  });

  it('fails closed when required capabilities are unavailable', () => {
    const business = createCapability('business-order', ['missing-foundation']);
    const resolution = resolveMangoCapabilityDependencies({
      preset: 'custom',
      selected: [business],
      catalog: [],
      autoInstallRequired: true,
    });

    expect(resolution.diagnostics).toContain('business-order requires missing capability missing-foundation');
    expect(resolution.report.diagnostics).toEqual(resolution.diagnostics);
  });

  it('reports conflicting capabilities', () => {
    const alpha = createCapability('alpha', [], ['beta']);
    const beta = createCapability('beta');
    const resolution = resolveMangoCapabilityDependencies({
      preset: 'custom',
      selected: [alpha, beta],
      catalog: [],
      autoInstallRequired: true,
    });

    expect(resolution.diagnostics).toContain('alpha conflicts with capability beta');
  });

  it('reports circular dependencies', () => {
    const alpha = createCapability('alpha', ['beta']);
    const beta = createCapability('beta', ['gamma']);
    const gamma = createCapability('gamma', ['alpha']);

    const resolution = resolveMangoCapabilityDependencies({
      preset: 'custom',
      selected: [alpha],
      catalog: [alpha, beta, gamma],
      autoInstallRequired: true,
    });

    expect(resolution.diagnostics).toContain('circular capability dependency detected: alpha -> beta -> gamma -> alpha');
  });
});

function createCapability(
  capabilityCode: string,
  requires: string[] = [],
  conflicts: string[] = [],
): MangoCapabilityManifest {
  return {
    moduleCode: `mango-${capabilityCode}`,
    packageName: `@mango/${capabilityCode}`,
    capabilityCode,
    capabilityName: capabilityCode,
    requires,
    optional: [],
    conflicts,
    backend: {
      moduleCode: `mango-${capabilityCode}`,
      menuSource: 'backend',
      requiredApis: [],
    },
    pages: [],
    menus: [],
    permissions: [],
    styles: [],
    runtime: {
      modes: ['local'],
      defaultMode: 'local',
    },
    e2e: {
      smoke: [],
      screenshots: [],
      dataChecks: [],
    },
  };
}
