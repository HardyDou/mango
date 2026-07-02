import { Bell } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';
import MessageCenterWidget from './MessageCenterWidget.vue';

export { default as MessageCenterWidget } from './MessageCenterWidget.vue';
export type { MessageCenterCategory, MessageCenterWidgetProps } from '../types';

export const noticeMessageCenterWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'notice.message-center',
    title: '我的消息',
    description: '展示当前登录人的未读消息、最新未读摘要和消息分类统计',
    source: 'business',
    businessDomainCode: 'NOTICE',
    businessDomainName: '通知中心',
    domainCode: 'NOTICE',
    domainName: '通知中心',
    groupName: '通知中心',
    moduleCode: 'notice',
    order: 110,
    icon: Bell,
    component: MessageCenterWidget,
    visibility: {
      mode: 'any',
      widgetPermissionCodes: ['notice:site:view'],
    },
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
    // 组件内部有完整标题与操作区，避免和布局卡片标题重复。
    showTitle: false,
    padding: false,
  },
];
