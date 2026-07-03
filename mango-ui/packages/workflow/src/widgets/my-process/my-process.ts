import { Tickets } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';
import MyProcessWidget from './MyProcessWidget.vue';

export { default as MyProcessWidget } from './MyProcessWidget.vue';
export type { MyProcessWidgetProps } from '../types';

export const workflowMyProcessWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'workflow.my-process',
    title: '我的申请',
    description: '展示当前登录人发起的工作流申请统计，并跳转到我的申请页面',
    source: 'business',
    businessDomainCode: 'WORKFLOW',
    businessDomainName: '工作流',
    domainCode: 'WORKFLOW',
    domainName: '工作流',
    groupName: '工作流',
    moduleCode: 'workflow',
    order: 95,
    icon: Tickets,
    component: MyProcessWidget,
    visibility: {
      mode: 'any',
      widgetPermissionCodes: ['workflow:business-apply:list'],
    },
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 8 },
    showTitle: false,
    padding: false,
  },
];
