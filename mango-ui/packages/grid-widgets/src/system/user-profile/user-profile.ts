import { UserFilled } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '../../types';
import UserProfileWidget from './UserProfileWidget.vue';

export { default as UserProfileWidget } from './UserProfileWidget.vue';
export type { UserProfileWidgetProps } from '../../types';

export const systemUserProfileWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.user-profile',
    title: '用户信息',
    description: '展示当前登录人信息，并跳转到个人中心和修改密码',
    category: '系统组件',
    source: 'mango',
    moduleCode: 'user-profile',
    order: 90,
    icon: UserFilled,
    component: UserProfileWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 8 },
    // 组件内部展示完整内容，不依赖布局卡片标题，避免重复标题占用空间。
    showTitle: false,
    padding: false,
  },
];
