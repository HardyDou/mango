import './style.css';

export { createInformationApi } from './api/information';
export type * from './api/information';
export { useInformationApi } from './composables/useInformationApi';
export {
  FIELD_TOOLBOX_GROUPS,
  fieldTypeLabel,
  isDisplayOnly,
  isUnsupported,
  maskSensitiveValue,
  needsOptions,
  nextFieldCode,
  parseFields,
  stringifyFields,
} from './domain/fields';
export type { InformationFieldDefinition, InformationFieldOption, InformationFieldType } from './domain/fields';
export {
  formatCount,
  formatDate,
  formatDateTime,
  formStatusLabel,
  formStatusTone,
  submissionStatusLabel,
  submissionStatusTone,
} from './domain/status';
export { default as InformationFormSurface } from './components/InformationFormSurface.vue';
export { registerMangoInformationAdminPages } from './admin-pages';
