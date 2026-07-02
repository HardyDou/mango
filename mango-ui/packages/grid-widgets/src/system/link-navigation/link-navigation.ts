import { Search } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '../../types';
import LinkNavigationWidget from './LinkNavigationWidget.vue';

export { default as LinkNavigationWidget } from './LinkNavigationWidget.vue';
export type {
  LinkNavigationGroup,
  LinkNavigationItem,
  LinkNavigationSearchEngineCode,
  LinkNavigationWidgetProps,
} from '../../types';

export const systemLinkNavigationWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.link-navigation',
    title: '网址导航',
    description: '在工作台首页提供百度、谷歌搜索和我的收藏网址',
    category: '系统组件',
    source: 'mango',
    moduleCode: 'link-navigation',
    order: 95,
    icon: Search,
    component: LinkNavigationWidget,
    defaultLayout: { w: 12, h: 20, minW: 6, minH: 18 },
    showTitle: false,
    padding: false,
  },
];
