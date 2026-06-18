import { mangoFullAdminFeatureRegistrars } from '@mango/admin/full';

export function registerFullMangoAdminFeaturePages() {
  for (const registerFeature of mangoFullAdminFeatureRegistrars) {
    registerFeature();
  }
}
