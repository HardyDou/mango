import { systemMyTaskWidgets } from './my-task';
import { systemMyTodoWidgets } from './my-todo';
import { systemMyProcessWidgets } from './my-process';
import { systemQuickEntryWidgets } from './quick-entry';
import { systemMessageCenterWidgets } from './message-center';
import { systemUserProfileWidgets } from './user-profile';

export * from './message-center';
export * from './my-process';
export * from './my-task';
export * from './my-todo';
export * from './quick-entry';
export * from './user-profile';

export const systemGridWidgets = [
  ...systemUserProfileWidgets,
  ...systemMyTaskWidgets,
  ...systemMyTodoWidgets,
  ...systemMyProcessWidgets,
  ...systemQuickEntryWidgets,
  ...systemMessageCenterWidgets,
];
