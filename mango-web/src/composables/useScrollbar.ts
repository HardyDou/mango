import { nextTick, type Ref } from 'vue';

/**
 * 滚动条逻辑 composable
 * 消除 layout 文件中的重复代码
 */
export function useScrollbar(
  layoutMainRef: Ref<any>,
  layoutScrollbarRef?: Ref<any>
) {
  const updateScrollbar = () => {
    if (layoutScrollbarRef?.value) {
      layoutScrollbarRef.value.update();
    }
    layoutMainRef.value?.layoutMainScrollbarRef?.update();
  };

  const initScrollHeight = () => {
    nextTick(() => {
      setTimeout(() => {
        updateScrollbar();
        if (layoutScrollbarRef?.value?.wrapRef) {
          layoutScrollbarRef.value.wrapRef.scrollTop = 0;
        }
        if (layoutMainRef.value?.layoutMainScrollbarRef?.wrapRef) {
          layoutMainRef.value.layoutMainScrollbarRef.wrapRef.scrollTop = 0;
        }
      }, 500);
    });
  };

  return { updateScrollbar, initScrollHeight };
}
