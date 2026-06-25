import { Histogram } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '../../types';
import MyTaskWidget from './MyTaskWidget.vue';

export { default as MyTaskWidget } from './MyTaskWidget.vue';
export type { MyTaskWidgetProps } from '../../types';

export const systemMyTaskWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.my-task',
    title: '我的任务',
    description: '展示当前登录人的任务执行统计，并跳转到对应任务列表',
    category: '系统组件',
    source: 'mango',
    moduleCode: 'workflow',
    order: 90,
    icon: Histogram,
    component: MyTaskWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
    showTitle: false,
    padding: false,
  },
];
