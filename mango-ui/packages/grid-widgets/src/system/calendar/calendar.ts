import { Calendar } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '../../types';
import CalendarWidget from './CalendarWidget.vue';

export { default as CalendarWidget } from './CalendarWidget.vue';
export type { CalendarWidgetProps } from '../../types';

export const systemCalendarWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.calendar',
    title: '日历',
    description: '展示今日日期、农历与本月最后一个工作日提醒',
    category: '系统组件',
    source: 'mango',
    moduleCode: 'calendar',
    order: 90,
    icon: Calendar,
    component: CalendarWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 8 },
    showTitle: false,
    padding: false,
  },
];
