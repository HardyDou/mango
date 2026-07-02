import { List } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';
import MyTodoWidget from './MyTodoWidget.vue';

export { default as MyTodoWidget } from './MyTodoWidget.vue';
export type { MyTodoWidgetProps } from '../types';

export const workflowMyTodoWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'workflow.my-todo',
    title: '我的待办',
    description: '展示当前登录人的工作流待办统计，并跳转到我的待办页面',
    source: 'business',
    businessDomainCode: 'WORKFLOW',
    businessDomainName: '工作流',
    domainCode: 'WORKFLOW',
    domainName: '工作流',
    groupName: '工作流',
    moduleCode: 'workflow',
    order: 90,
    icon: List,
    component: MyTodoWidget,
    visibility: {
      mode: 'any',
      widgetPermissionCodes: ['workflow:task:list'],
    },
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 8 },
    showTitle: false,
    padding: false,
  },
];
