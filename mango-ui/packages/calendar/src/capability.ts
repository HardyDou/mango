const calendarPages = {
  'data/calendar/index': () => import('./views/calendar/index.vue').then(m => m.default),
};

export const mangoCalendarPageRegistry = {
  moduleCode: 'mango-calendar',
  pages: calendarPages,
};

export const mangoCalendarCapability = {
  moduleCode: 'mango-calendar',
  packageName: '@mango/calendar',
  capabilityCode: 'calendar',
  capabilityName: '工作日历',
  requires: ['auth', 'rbac'],
  optional: [],
  backend: {
    moduleCode: 'mango-calendar',
    menuSource: 'backend',
    resourceManifest: 'META-INF/mango/resource-manifest.json',
    requiredApis: ['/api/calendar/admin/calendars/page'],
  },
  pages: [
    {
      component: 'data/calendar/index',
      loader: calendarPages['data/calendar/index'],
      menuCode: 'data:calendar',
      permissions: ['calendar:admin:list'],
    },
  ],
  menus: [
    {
      menuCode: 'data:calendar',
      moduleCode: 'mango-calendar',
      component: 'data/calendar/index',
      permissions: ['calendar:admin:list'],
      source: 'backend',
    },
  ],
  permissions: ['calendar:admin:list'],
  styles: [],
  runtime: {
    modes: ['local', 'micro', 'mixed'],
    defaultMode: 'local',
  },
  e2e: {
    smoke: ['calendar-management'],
    screenshots: ['data-calendar'],
    dataChecks: ['calendar-page', 'workday-count'],
  },
};
