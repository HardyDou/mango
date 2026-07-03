import { systemQuickEntryWidgets } from './widgets/quick-entry';
import { systemUserProfileWidgets } from './widgets/user-profile';

const systemWidgets = [
  ...systemUserProfileWidgets,
  ...systemQuickEntryWidgets,
];

export function registerMangoSystemAdminPages() {
  return {
    businessDomainCode: 'SYSTEM',
    businessDomainName: '系统管理',
    groupName: '系统管理',
    widgets: systemWidgets,
  };
}
