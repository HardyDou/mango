import { Calendar } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';
import CalendarWidget from './CalendarWidget.vue';

export { default as CalendarWidget } from './CalendarWidget.vue';
export type { CalendarWidgetProps } from '../types';

export const calendarWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'calendar.calendar',
    title: '日历',
    description: '展示今日日期、农历与本月最后一个工作日提醒',
    source: 'business',
    businessDomainCode: 'CALENDAR',
    businessDomainName: '日历',
    domainCode: 'CALENDAR',
    domainName: '日历',
    groupName: '日历',
    moduleCode: 'calendar',
    order: 90,
    icon: Calendar,
    component: CalendarWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 8 },
    showTitle: false,
    padding: false,
  },
];
