import { computed, inject } from 'vue';
import { getMangoAuthConfig, mangoAuthConfigKey, type MangoAuthConfig } from '../config';

export function useAuthConfig() {
  const provided = inject<MangoAuthConfig>(mangoAuthConfigKey, getMangoAuthConfig());
  return computed(() => provided || getMangoAuthConfig());
}
