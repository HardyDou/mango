import { defineAsyncComponent } from 'vue';
import { Clock } from '@element-plus/icons-vue';
import { systemQuickEntryWidgets } from './widgets/quick-entry';
import { systemUserProfileWidgets } from './widgets/user-profile';

const systemWidgets = [...systemUserProfileWidgets, ...systemQuickEntryWidgets];

export function registerMangoSystemAdminPages() {
  return {
    businessDomainCode: 'SYSTEM',
    businessDomainName: '系统管理',
    groupName: '系统管理',
    widgets: systemWidgets,
    profileSections: [
      {
        key: 'login-log',
        label: '登录日志',
        group: '安全设置',
        icon: Clock,
        component: defineAsyncComponent(() => import('./views/personal-login-log/index.vue')),
      },
    ],
  };
}
