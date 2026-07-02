import { Histogram } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';
import MyTaskWidget from './MyTaskWidget.vue';

export { default as MyTaskWidget } from './MyTaskWidget.vue';
export type { MyTaskWidgetProps } from '../types';

export const workflowMyTaskWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'workflow.my-task',
    title: '我的任务',
    description: '展示当前登录人的任务执行统计，并跳转到对应任务列表',
    source: 'business',
    businessDomainCode: 'WORKFLOW',
    businessDomainName: '工作流',
    domainCode: 'WORKFLOW',
    domainName: '工作流',
    groupName: '工作流',
    moduleCode: 'workflow',
    order: 90,
    icon: Histogram,
    component: MyTaskWidget,
    visibility: {
      mode: 'any',
      widgetPermissionCodes: ['workflow:task:list'],
    },
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
    showTitle: false,
    padding: false,
  },
];
