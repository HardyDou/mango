import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerMangoCalendarAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-calendar',
    pages: {
      'data/calendar/index': () => import('./index').then(m => m.CalendarView),
    },
  });
}
