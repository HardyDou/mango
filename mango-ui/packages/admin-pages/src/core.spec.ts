import { describe, expect, it } from 'vitest';

import { getRegisteredModulePagesSnapshot, probeRegisteredPage, registerModulePages } from './core';

describe('module page runtime diagnostics', () => {
  it('returns a deeply immutable registration snapshot with package version', () => {
    registerModulePages({
      moduleCode: 'snapshot-module',
      packageName: '@mango/snapshot-module',
      packageVersion: '1.2.3',
      pages: {
        'snapshot/index': async () => ({ name: 'SnapshotPage' }),
      },
    });

    const snapshot = getRegisteredModulePagesSnapshot('snapshot-module');

    expect(snapshot).toEqual([
      {
        moduleCode: 'snapshot-module',
        packageName: '@mango/snapshot-module',
        packageVersion: '1.2.3',
        pages: ['snapshot/index'],
      },
    ]);
    expect(Object.isFrozen(snapshot)).toBe(true);
    expect(Object.isFrozen(snapshot[0])).toBe(true);
    expect(Object.isFrozen(snapshot[0]?.pages)).toBe(true);
  });

  it('probes one registered Vue page without exposing the loader registry', async () => {
    registerModulePages({
      moduleCode: 'ready-module',
      packageName: '@mango/ready-module',
      packageVersion: '2.0.0',
      pages: {
        'ready/index': async () => ({ name: 'ReadyPage' }),
      },
    });

    await expect(probeRegisteredPage('ready-module', 'ready/index')).resolves.toMatchObject({
      status: 'PASS',
      reasonCode: 'PAGE_RUNTIME_READY',
      actualVersion: '2.0.0',
      stages: {
        registration: { status: 'PASS' },
        loader: { status: 'PASS' },
        component: { status: 'PASS' },
      },
    });
  });

  it('reports missing page, loader rejection, chunk failure and invalid component distinctly', async () => {
    registerModulePages({
      moduleCode: 'failure-module',
      pages: {
        'reject/index': async () => {
          throw new Error('application loader failed');
        },
        'chunk/index': async () => {
          throw new TypeError('Failed to fetch dynamically imported module');
        },
        'invalid/index': async () => ({ value: 'not-a-component' }),
      },
    });

    await expect(probeRegisteredPage('failure-module', 'missing/index')).resolves.toMatchObject({
      status: 'FAIL',
      reasonCode: 'PAGE_NOT_REGISTERED',
    });
    await expect(probeRegisteredPage('failure-module', 'reject/index')).resolves.toMatchObject({
      status: 'FAIL',
      reasonCode: 'LOADER_REJECTED',
    });
    await expect(probeRegisteredPage('failure-module', 'chunk/index')).resolves.toMatchObject({
      status: 'FAIL',
      reasonCode: 'CHUNK_LOAD_FAILED',
    });
    await expect(probeRegisteredPage('failure-module', 'invalid/index')).resolves.toMatchObject({
      status: 'FAIL',
      reasonCode: 'VUE_COMPONENT_INVALID',
    });
  });
});
