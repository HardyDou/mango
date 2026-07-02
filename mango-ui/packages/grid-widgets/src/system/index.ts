import { systemCalendarWidgets } from './calendar';
import { systemMyTaskWidgets } from './my-task';
import { systemMyTodoWidgets } from './my-todo';
import { systemMyProcessWidgets } from './my-process';
import { systemQuickEntryWidgets } from './quick-entry';
import { systemMessageCenterWidgets } from './message-center';
import { systemUserProfileWidgets } from './user-profile';
export * from './calendar';
export * from './message-center';
export * from './link-navigation';
export * from './my-process';
export * from './my-task';
export * from './my-todo';
export * from './quick-entry';
export * from './user-profile';

export const systemGridWidgets = [
  ...systemCalendarWidgets,
  ...systemUserProfileWidgets,
  ...systemMyTaskWidgets,
  ...systemMyTodoWidgets,
  ...systemMyProcessWidgets,
  ...systemQuickEntryWidgets,
  ...systemMessageCenterWidgets,
];
