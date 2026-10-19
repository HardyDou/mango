import './style.css';

export { createInformationApi } from './api/information';
export { createInformationPublicFileClient } from './api/publicFileClient';
export type * from './api/information';
export { useInformationApi } from './composables/useInformationApi';
export { fieldTypeLabel, isDisplayOnly } from './domain/fields';
export { displayFieldValue, maskSensitiveValue, parseRules, rulesToFields } from './domain/rules';
export type { FormCreateRule, InformationFieldDefinition, InformationFieldOption } from './domain/rules';
export {
  formatCount,
  formatDate,
  formatDateTime,
  formStatusLabel,
  formStatusTone,
  submissionStatusLabel,
  submissionStatusTone,
} from './domain/status';
export { registerMangoInformationAdminPages } from './admin-pages';
