import { systemMyTodoWidgets } from './my-todo';
import { systemQuickEntryWidgets } from './quick-entry';
import { systemMessageCenterWidgets } from './message-center';
import { systemUserProfileWidgets } from './user-profile';

export * from './message-center';
export * from './my-todo';
export * from './quick-entry';
export * from './user-profile';

export const systemGridWidgets = [
  ...systemUserProfileWidgets,
  ...systemMyTodoWidgets,
  ...systemQuickEntryWidgets,
  ...systemMessageCenterWidgets,
];
