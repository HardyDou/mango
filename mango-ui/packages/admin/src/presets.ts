import {
  mangoDefaultCapabilities,
  registerDefaultAdminPages,
  type RegisterDefaultAdminPagesOptions,
} from '@mango/admin-pages/defaults';
import {
  registerCapabilities,
  registerModulePages,
  registerShellPages,
  resolveMangoCapabilityDependencies,
  type MangoCapabilityDependencyReport,
  type MangoCapabilityManifest,
  type MangoPageRegistry,
  type MangoShellPageLoaders,
} from '@mango/admin-pages/core';

export type MangoAdminPreset = 'full' | 'standard' | 'minimal' | 'custom';

export interface MangoAdminPresetResolution {
  preset: MangoAdminPreset;
  capabilities: MangoCapabilityManifest[];
  registries: MangoPageRegistry[];
  diagnostics: string[];
  dependencyReport: MangoCapabilityDependencyReport;
}

export interface MangoAdminPresetOptions {
  preset?: MangoAdminPreset;
  capabilities?: MangoCapabilityManifest[];
  registries?: MangoPageRegistry[];
  shellPages?: MangoShellPageLoaders;
}

const standardCapabilityCodes = new Set(['auth', 'rbac', 'system', 'file', 'notice']);

export function resolveMangoAdminPreset(options: MangoAdminPresetOptions = {}): MangoAdminPresetResolution {
  const preset = options.preset || 'full';
  assertKnownPreset(preset);

  const explicitCapabilities = options.capabilities || [];
  const explicitRegistries = options.registries || [];
  const defaultCapabilities = mangoDefaultCapabilities as MangoCapabilityManifest[];

  if (preset === 'full') {
    const dependencyResolution = resolveMangoCapabilityDependencies({
      preset,
      selected: mergeCapabilities(defaultCapabilities, explicitCapabilities),
      catalog: defaultCapabilities,
      autoInstallRequired: false,
    });
    return {
      preset,
      capabilities: dependencyResolution.capabilities,
      registries: explicitRegistries,
      diagnostics: dependencyResolution.diagnostics,
      dependencyReport: dependencyResolution.report,
    };
  }

  if (preset === 'standard') {
    const standardCapabilities = defaultCapabilities.filter(capability =>
      standardCapabilityCodes.has(capability.capabilityCode),
    );
    const dependencyResolution = resolveMangoCapabilityDependencies({
      preset,
      selected: mergeCapabilities(standardCapabilities, explicitCapabilities),
      catalog: defaultCapabilities,
      autoInstallRequired: true,
    });
    return {
      preset,
      capabilities: dependencyResolution.capabilities,
      registries: explicitRegistries,
      diagnostics: dependencyResolution.diagnostics,
      dependencyReport: dependencyResolution.report,
    };
  }

  if (preset === 'minimal') {
    const dependencyResolution = resolveMangoCapabilityDependencies({
      preset,
      selected: explicitCapabilities,
      catalog: [],
      autoInstallRequired: false,
    });
    return {
      preset,
      capabilities: dependencyResolution.capabilities,
      registries: explicitRegistries,
      diagnostics: dependencyResolution.diagnostics,
      dependencyReport: dependencyResolution.report,
    };
  }

  const dependencyResolution = resolveMangoCapabilityDependencies({
    preset,
    selected: explicitCapabilities,
    catalog: defaultCapabilities,
    autoInstallRequired: true,
  });
  if (dependencyResolution.diagnostics.length > 0) {
    throw new Error([
      '@mango/admin custom preset dependency validation failed.',
      ...dependencyResolution.diagnostics.map((item: string) => `- ${item}`),
    ].join('\n'));
  }

  return {
    preset,
    capabilities: dependencyResolution.capabilities,
    registries: explicitRegistries,
    diagnostics: dependencyResolution.diagnostics,
    dependencyReport: dependencyResolution.report,
  };
}

export function registerMangoAdminPreset(options: MangoAdminPresetOptions = {}) {
  const resolution = resolveMangoAdminPreset(options);

  if (resolution.preset === 'full' || resolution.preset === 'standard') {
    const registerOptions: RegisterDefaultAdminPagesOptions = {
      shellPages: options.shellPages,
      capabilities: resolution.capabilities,
      registries: resolution.registries,
    };
    registerDefaultAdminPages(registerOptions);
    return resolution;
  }

  registerCapabilities(resolution.capabilities);
  for (const registry of resolution.registries) {
    registerModulePages(registry);
  }
  if (options.shellPages) {
    registerShellPages(options.shellPages);
  }
  return resolution;
}

function mergeCapabilities(
  baseCapabilities: MangoCapabilityManifest[],
  extensionCapabilities: MangoCapabilityManifest[],
) {
  const merged = new Map<string, MangoCapabilityManifest>();
  for (const capability of [...baseCapabilities, ...extensionCapabilities]) {
    merged.set(capability.capabilityCode, capability);
  }
  return [...merged.values()];
}

function assertKnownPreset(preset: string): asserts preset is MangoAdminPreset {
  if (!['full', 'standard', 'minimal', 'custom'].includes(preset)) {
    throw new Error(`Unknown Mango admin preset: ${preset}`);
  }
}
