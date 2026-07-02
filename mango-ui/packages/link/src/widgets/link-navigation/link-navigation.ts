import { Search } from '@element-plus/icons-vue';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';
import LinkNavigationWidget from './LinkNavigationWidget.vue';

export { default as LinkNavigationWidget } from './LinkNavigationWidget.vue';
export type {
  LinkNavigationGroup,
  LinkNavigationItem,
  LinkNavigationItemLoader,
  LinkNavigationSearchEngineCode,
  LinkNavigationWidgetProps,
} from './types';

export const linkNavigationWidgets: MangoGridWidgetDefinition[] = [
  {
    type: 'system.link-navigation',
    title: '网址导航',
    description: '在工作台首页提供百度、谷歌搜索和我的收藏网址',
    order: 95,
    icon: Search,
    component: LinkNavigationWidget,
    defaultLayout: { w: 12, h: 20, minW: 6, minH: 18 },
    showTitle: false,
    padding: false,
  },
];
