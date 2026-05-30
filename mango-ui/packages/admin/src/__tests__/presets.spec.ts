import { describe, expect, it } from 'vitest';
import { mangoDefaultCapabilities } from '@mango/admin-pages/defaults';
import type { MangoCapabilityManifest } from '@mango/admin-pages/core';
import { resolveMangoAdminPreset } from '../presets';

describe('@mango/admin presets', () => {
  it('resolves full preset to all default Mango capabilities', () => {
    const resolution = resolveMangoAdminPreset({ preset: 'full' });

    expect(resolution.capabilities.map(capability => capability.capabilityCode)).toEqual(
      mangoDefaultCapabilities.map(capability => capability.capabilityCode),
    );
  });

  it('resolves standard preset to foundational admin capabilities', () => {
    const resolution = resolveMangoAdminPreset({ preset: 'standard' });

    expect(resolution.capabilities.map(capability => capability.capabilityCode)).toEqual([
      'auth',
      'rbac',
      'system',
      'file',
      'notice',
    ]);
  });

  it('keeps minimal preset explicit', () => {
    const capability = createCapability('business-order');
    const resolution = resolveMangoAdminPreset({
      preset: 'minimal',
      capabilities: [capability],
    });

    expect(resolution.capabilities).toEqual([capability]);
  });

  it('auto-completes custom preset required Mango capabilities from the default catalog', () => {
    const resolution = resolveMangoAdminPreset({
      preset: 'custom',
      capabilities: [createCapability('system', ['auth', 'rbac'])],
    });

    expect(resolution.capabilities.map(capability => capability.capabilityCode)).toEqual([
      'auth',
      'rbac',
      'system',
    ]);
    expect(resolution.dependencyReport.autoInstalledCodes).toEqual(['auth', 'rbac']);
    expect(resolution.dependencyReport.nodes.find(node => node.capabilityCode === 'system')?.origin).toBe('selected');
  });

  it('accepts custom preset when required capabilities are provided', () => {
    const resolution = resolveMangoAdminPreset({
      preset: 'custom',
      capabilities: [
        createCapability('auth'),
        createCapability('rbac', ['auth']),
        createCapability('system', ['auth', 'rbac']),
      ],
    });

    expect(resolution.capabilities.map(capability => capability.capabilityCode)).toEqual([
      'auth',
      'rbac',
      'system',
    ]);
    expect(resolution.dependencyReport.autoInstalledCodes).toEqual([]);
  });

  it('fails custom preset when required capabilities cannot be found in selected or default catalog', () => {
    expect(() => resolveMangoAdminPreset({
      preset: 'custom',
      capabilities: [createCapability('business-order', ['missing-foundation'])],
    })).toThrow(/business-order requires missing capability missing-foundation/);
  });

  it('fails custom preset on conflicting capabilities', () => {
    expect(() => resolveMangoAdminPreset({
      preset: 'custom',
      capabilities: [
        createCapability('business-alpha', [], ['business-beta']),
        createCapability('business-beta'),
      ],
    })).toThrow(/business-alpha conflicts with capability business-beta/);
  });

  it('fails custom preset on circular dependencies', () => {
    expect(() => resolveMangoAdminPreset({
      preset: 'custom',
      capabilities: [
        createCapability('business-alpha', ['business-beta']),
        createCapability('business-beta', ['business-alpha']),
      ],
    })).toThrow(/circular capability dependency detected: business-alpha -> business-beta -> business-alpha/);
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
