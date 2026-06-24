import { Bell } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '../../types';
import MessageCenterWidget from './MessageCenterWidget.vue';

export { default as MessageCenterWidget } from './MessageCenterWidget.vue';
export type { MessageCenterCategory, MessageCenterWidgetProps } from '../../types';

export const systemMessageCenterWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.message-center',
    title: '消息中心',
    description: '展示当前登录人的未读消息、最新未读摘要和消息分类统计',
    category: '系统组件',
    source: 'mango',
    moduleCode: 'message-center',
    order: 110,
    icon: Bell,
    component: MessageCenterWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
    // 组件内部有完整标题与操作区，避免和布局卡片标题重复。
    showTitle: false,
    padding: false,
  },
];
