import type { MangoRuntimeConfig } from '@mango/app-runtime';

export const starterRuntimeConfig: MangoRuntimeConfig = {
  profile: (import.meta.env.VITE_MANGO_RUNTIME_PROFILE || '{{runtimeProfile}}') as MangoRuntimeConfig['profile'],
  modules: {
{{mangoRuntimeModulesTs}},
  },
};
