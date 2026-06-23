import { systemQuickEntryWidgets } from './quick-entry';
import { systemUserProfileWidgets } from './user-profile';

export * from './quick-entry';
export * from './user-profile';

export const systemGridWidgets = [
  ...systemUserProfileWidgets,
  ...systemQuickEntryWidgets,
];
