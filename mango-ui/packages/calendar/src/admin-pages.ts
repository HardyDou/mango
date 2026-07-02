import { registerModulePages } from '@mango/admin-pages/core';
import { calendarWidgets } from './widgets/calendar';

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

  return {
    businessDomainCode: 'CALENDAR',
    businessDomainName: '日历',
    groupName: '日历',
    widgets: calendarWidgets,
  };
}
