import { UserFilled } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';
import UserProfileWidget from './UserProfileWidget.vue';

export { default as UserProfileWidget } from './UserProfileWidget.vue';
export type { UserProfileWidgetProps } from '../types';

export const systemUserProfileWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.user-profile',
    title: '用户信息',
    description: '展示当前登录用户信息，并跳转到个人中心和修改密码。',
    source: 'business',
    businessDomainCode: 'SYSTEM',
    businessDomainName: '系统管理',
    domainCode: 'SYSTEM',
    domainName: '系统管理',
    groupName: '系统管理',
    moduleCode: 'user-profile',
    order: 90,
    icon: UserFilled,
    component: UserProfileWidget,
    defaultLayout: { w: 3, h: 22, minW: 3, minH: 16 },
    // 组件内部自带完整抬头与操作区，不再依赖布局卡片标题，避免重复占位。
    showTitle: false,
    padding: false,
  },
];
