import { getMangoAdminShellOptions } from '../config';
import type { MangoAdminFeatureRegistration } from '../config';
import { registerMangoAdminHomeWidgets, resetMangoAdminHomeWidgetsForTest } from './homeWidgets';

let featureRegistrarsPromise: Promise<void> | undefined;

export function ensureFeatureRegistrars() {
  if (!featureRegistrarsPromise) {
    const options = getMangoAdminShellOptions();
    registerMangoAdminHomeWidgets(options.widgets || []);
    const results = (options.featureRegistrars || [])
      .map(async (registrar) => {
        applyFeatureRegistration(await registrar());
      });
    featureRegistrarsPromise = Promise.all(results).then(() => undefined);
  }
  return featureRegistrarsPromise;
}

export function resetFeatureRegistrarsForTest() {
  featureRegistrarsPromise = undefined;
  resetMangoAdminHomeWidgetsForTest();
}

function applyFeatureRegistration(registration: void | MangoAdminFeatureRegistration): void {
  if (!registration) {
    return;
  }
  registerMangoAdminHomeWidgets(registration.widgets || [], {
    businessDomainCode: registration.businessDomainCode,
    businessDomainName: registration.businessDomainName,
    groupName: registration.groupName,
    moduleCode: registration.moduleCode,
    moduleName: registration.moduleName,
  });
}
