import type { Component } from 'vue';
import type { MangoWidgetRuntimeContext } from '@mango/grid-widgets';

export type LinkNavigationSearchEngineCode = 'baidu' | 'google';

export interface LinkNavigationItem {
  id: string;
  title: string;
  path?: string;
  url?: string;
  redirectUrl?: string;
  iconUrl?: string;
  icon?: Component;
  iconName?: string;
  groupKey?: string;
  groupTitle?: string;
  categoryName?: string;
  summary?: string;
  tags?: string[];
  favoriteTime?: string;
  favorited?: boolean;
  moduleCode?: string;
  appCode?: string;
  pageType?: string;
  source?: 'PUBLIC' | 'COMPANY' | 'FAVORITE' | 'PERSONAL' | string;
  raw?: unknown;
}

export interface LinkNavigationGroup {
  key: string;
  title: string;
  categoryId?: string;
  owned?: boolean;
  items?: LinkNavigationItem[];
}

export type LinkNavigationItemLoader = (
  keyword?: string,
  runtime?: MangoWidgetRuntimeContext,
) => LinkNavigationItem[] | Promise<LinkNavigationItem[]>;

export interface LinkNavigationWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  items?: LinkNavigationItem[];
  groups?: LinkNavigationGroup[];
  loadItems?: LinkNavigationItemLoader;
  maxGroups?: number;
  maxItemsPerGroup?: number;
  placeholder?: string;
  defaultSearchEngine?: LinkNavigationSearchEngineCode;
  navigate?: (item: LinkNavigationItem) => void | Promise<void>;
}
