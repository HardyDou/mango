export { default as DictView } from './views/dict/index.vue';
export { default as OperationLogView } from './views/operation-log/index.vue';
export { default as LoginLogView } from './views/login-log/index.vue';
export { default as TenantView } from './views/tenant/index.vue';
export { default as ConfigView } from './views/config/index.vue';
export { default as PublicPathView } from './views/public-path/index.vue';
export { default as AreaView } from './views/area/index.vue';
export { default as DomainView } from './views/domain/index.vue';
export { default as ParticipantSelector } from './components/ParticipantSelector/index.vue';
export { default as DomainSelector } from './components/DomainSelector/index.vue';
export { default as DomainSideTree } from './components/DomainSideTree/index.vue';
export type {
  ParticipantOrgTreeOption,
  ParticipantSelectorLoading,
  ParticipantSelectorValue,
  ParticipantTargetOption,
  ParticipantType,
} from './components/ParticipantSelector/types';

export * from './api/area';
export * from './api/dict';
export * from './api/log';
export * from './api/tenant';
export * from './api/config';
export * from './api/publicPath';
export * from './api/param';
export * from './api/domain';
