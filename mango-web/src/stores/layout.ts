import { defineStore } from 'pinia';

export type LayoutType = 'defaults' | 'classic' | 'transverse' | 'columns';
export type ColumnsAsideStyle = 'columns-round' | 'columns-card';
export type ColumnsAsideLayout = 'columns-vertical' | 'columns-horizontal';

export interface LayoutState {
  layout: LayoutType;
  isCollapse: boolean;
  isFixedHeader: boolean;
  isFixedHeaderChange: boolean;
  isClassicSplitMenu: boolean;
  isMobileMenuOpen: boolean;
  isUniqueOpened: boolean;
  isShowLogo: boolean;
  isShowLogoChange: boolean;
  isBreadcrumb: boolean;
  isBreadcrumbIcon: boolean;
  isTagsview: boolean;
  isTagsviewIcon: boolean;
  isCacheTagsView: boolean;
  isSortableTagsView: boolean;
  isShareTagsView: boolean;
  isFooter: boolean;
  columnsAsideStyle: ColumnsAsideStyle;
  columnsAsideLayout: ColumnsAsideLayout;
  isColumnsMenuHoverPreload: boolean;
}

export const useLayoutStore = defineStore('layout', {
  state: (): LayoutState => ({
    layout: 'classic',
    isCollapse: false,
    isFixedHeader: false,
    isFixedHeaderChange: false,
    isClassicSplitMenu: true,
    isMobileMenuOpen: false,
    isUniqueOpened: true,
    isShowLogo: true,
    isShowLogoChange: false,
    isBreadcrumb: true,
    isBreadcrumbIcon: false,
    isTagsview: true,
    isTagsviewIcon: false,
    isCacheTagsView: true,
    isSortableTagsView: true,
    isShareTagsView: false,
    isFooter: true,
    columnsAsideStyle: 'columns-round',
    columnsAsideLayout: 'columns-vertical',
    isColumnsMenuHoverPreload: false,
  }),
  actions: {
    setLayout(layout: LayoutType) {
      this.layout = layout;
    },
    toggleCollapse() {
      this.isCollapse = !this.isCollapse;
    },
    toggleMobileMenu() {
      this.isMobileMenuOpen = !this.isMobileMenuOpen;
    },
    closeMobileMenu() {
      this.isMobileMenuOpen = false;
    },
  },
  persist: false,
});
