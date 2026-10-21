import { nextTick, ref } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useScrollbar, type LayoutMainScrollbarHost } from '../composables/useScrollbar';

describe('admin shell content scrollbar', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('resets the content scroll through the LayoutMain host after the route update delay', async () => {
    vi.useFakeTimers();
    const host: LayoutMainScrollbarHost = {
      updateScrollbar: vi.fn(() => true),
      resetContentScroll: vi.fn(() => true),
    };
    const { initScrollHeight } = useScrollbar(ref(host));

    initScrollHeight();
    await nextTick();
    vi.advanceTimersByTime(500);

    expect(host.updateScrollbar).toHaveBeenCalledOnce();
    expect(host.resetContentScroll).toHaveBeenCalledOnce();
  });

  it('reports a missing LayoutMain host instead of silently skipping the reset', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const { resetContentScroll } = useScrollbar(ref<LayoutMainScrollbarHost>());

    expect(resetContentScroll()).toBe(false);
    expect(error).toHaveBeenCalledWith('[mango-shell] failed to reset content scroll: LayoutMain is not ready');
  });
});
