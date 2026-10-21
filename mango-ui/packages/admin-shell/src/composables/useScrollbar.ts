import { nextTick, type Ref } from 'vue';

export interface LayoutMainScrollbarHost {
  updateScrollbar: () => boolean;
  resetContentScroll: () => boolean;
}

/**
 * 滚动条逻辑 composable
 * 消除 layout 文件中的重复代码
 */
export function useScrollbar(layoutMainRef: Ref<LayoutMainScrollbarHost | undefined>) {
  const updateScrollbar = () => {
    const layoutMain = layoutMainRef.value;
    if (!layoutMain) {
      console.error('[mango-shell] failed to update content scrollbar: LayoutMain is not ready');
      return false;
    }
    return layoutMain.updateScrollbar();
  };

  const resetContentScroll = () => {
    const layoutMain = layoutMainRef.value;
    if (!layoutMain) {
      console.error('[mango-shell] failed to reset content scroll: LayoutMain is not ready');
      return false;
    }
    return layoutMain.resetContentScroll();
  };

  const initScrollHeight = () => {
    nextTick(() => {
      setTimeout(() => {
        updateScrollbar();
        resetContentScroll();
      }, 500);
    });
  };

  return { updateScrollbar, resetContentScroll, initScrollHeight };
}
