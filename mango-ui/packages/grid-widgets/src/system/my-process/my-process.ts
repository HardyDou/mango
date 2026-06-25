import { Tickets } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '../../types';
import MyProcessWidget from './MyProcessWidget.vue';

export { default as MyProcessWidget } from './MyProcessWidget.vue';
export type { MyProcessWidgetProps } from '../../types';

export const systemMyProcessWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.my-process',
    title: '我的申请',
    description: '展示当前登录人发起的工作流申请统计，并跳转到我的申请页面',
    category: '系统组件',
    source: 'mango',
    moduleCode: 'workflow',
    order: 95,
    icon: Tickets,
    component: MyProcessWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 8 },
    showTitle: false,
    padding: false,
  },
];
