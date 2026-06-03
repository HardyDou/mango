import { getMangoAdminShellOptions } from '../config';

let featureRegistrarsPromise: Promise<void> | undefined;

export function ensureFeatureRegistrars() {
  if (!featureRegistrarsPromise) {
    const results = (getMangoAdminShellOptions().featureRegistrars || [])
      .map(registrar => registrar());
    featureRegistrarsPromise = Promise.all(results).then(() => undefined);
  }
  return featureRegistrarsPromise;
}

export function resetFeatureRegistrarsForTest() {
  featureRegistrarsPromise = undefined;
}
