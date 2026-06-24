import { List } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '../../types';
import MyTodoWidget from './MyTodoWidget.vue';

export { default as MyTodoWidget } from './MyTodoWidget.vue';
export type { MyTodoWidgetProps } from '../../types';

export const systemMyTodoWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.my-todo',
    title: '我的待办',
    description: '展示当前登录人的工作流待办统计，并跳转到我的待办页面',
    category: '系统组件',
    source: 'mango',
    moduleCode: 'workflow',
    order: 90,
    icon: List,
    component: MyTodoWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 8 },
    showTitle: false,
    padding: false,
  },
];
