export type MangoAdminFeatureCode =
  | 'authorization'
  | 'system'
  | 'workflow'
  | 'file'
  | 'template'
  | 'notice'
  | 'numgen'
  | 'calendar'
  | 'job';

export type MangoAdminFeaturePreset = 'core' | 'full';

export type MangoAdminFeatures =
  | MangoAdminFeaturePreset
  | MangoAdminFeatureCode[]
  | Partial<Record<MangoAdminFeatureCode, boolean>>;

export const MANGO_ADMIN_CORE_FEATURES: MangoAdminFeatureCode[] = [
  'authorization',
  'system',
];

export const MANGO_ADMIN_OPTIONAL_FEATURES: MangoAdminFeatureCode[] = [
  'workflow',
  'file',
  'template',
  'notice',
  'numgen',
  'calendar',
  'job',
];

export const MANGO_ADMIN_FULL_FEATURES: MangoAdminFeatureCode[] = [
  ...MANGO_ADMIN_CORE_FEATURES,
  ...MANGO_ADMIN_OPTIONAL_FEATURES,
];

const MODULE_FEATURE_MAP: Record<string, MangoAdminFeatureCode> = {
  'mango-authorization': 'authorization',
  'mango-system': 'system',
  'mango-workflow': 'workflow',
  'mango-file': 'file',
  'mango-template': 'template',
  'mango-notice': 'notice',
  'mango-numgen': 'numgen',
  'mango-calendar': 'calendar',
  'mango-job': 'job',
};

export function resolveMangoAdminFeatures(features: MangoAdminFeatures = 'core') {
  if (features === 'full') {
    return new Set(MANGO_ADMIN_FULL_FEATURES);
  }
  if (features === 'core') {
    return new Set(MANGO_ADMIN_CORE_FEATURES);
  }
  if (Array.isArray(features)) {
    return new Set([
      ...MANGO_ADMIN_CORE_FEATURES,
      ...features,
    ]);
  }
  return new Set(
    MANGO_ADMIN_FULL_FEATURES.filter(feature =>
      MANGO_ADMIN_CORE_FEATURES.includes(feature) || features[feature] === true,
    ),
  );
}

export function resolveMangoAdminModuleFeature(moduleCode?: string) {
  return moduleCode ? MODULE_FEATURE_MAP[moduleCode] : undefined;
}

export function isMangoAdminFeatureEnabled(features: Set<MangoAdminFeatureCode>, feature?: MangoAdminFeatureCode) {
  return !feature || features.has(feature);
}
