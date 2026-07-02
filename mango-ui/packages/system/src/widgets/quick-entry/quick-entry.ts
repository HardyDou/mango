import { Grid } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';
import QuickEntryWidget from './QuickEntryWidget.vue';

export { default as QuickEntryWidget } from './QuickEntryWidget.vue';
export type { QuickEntryMenuItem, QuickEntryMenuResolver, QuickEntryWidgetProps } from '../types';

export const systemQuickEntryWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.quick-entry',
    title: '快捷入口',
    description: '选择常用菜单，快速跳转到对应模块',
    source: 'business',
    businessDomainCode: 'SYSTEM',
    businessDomainName: '系统管理',
    domainCode: 'SYSTEM',
    domainName: '系统管理',
    groupName: '系统管理',
    moduleCode: 'quick-entry',
    order: 100,
    icon: Grid,
    component: QuickEntryWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
    // 快捷入口需要在小组件内部放置设置按钮，所以隐藏布局卡片自带标题。
    showTitle: false,
    padding: false,
  },
];
