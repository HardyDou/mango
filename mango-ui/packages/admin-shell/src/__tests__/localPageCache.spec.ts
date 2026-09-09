// @vitest-environment happy-dom
import { defineComponent, h, nextTick, onActivated, onDeactivated, onMounted, onUnmounted, ref, watch } from 'vue';
import { createMemoryHistory, createRouter, useRoute } from 'vue-router';
import { describe, expect, it } from 'vitest';
import { createLocalPageCacheHost, type LocalPageEntry } from '../runtime/localPageCache';

describe('local page cache host', () => {
  it('isolates inactive cached pages from another tab route changes', async () => {
    const lifecycle = { mounted: 0, initialized: [] as string[] };
    const Page = defineComponent({
      setup() {
        const route = useRoute();
        const value = ref('');
        onMounted(() => lifecycle.mounted++);
        watch(
          () => route.query.taskId,
          (taskId) => {
            lifecycle.initialized.push(String(taskId || 'list'));
            value.value = taskId ? `loaded-${taskId}` : '';
          },
          { immediate: true },
        );
        return () =>
          h('input', {
            value: value.value,
            onInput: (event) => (value.value = (event.target as HTMLInputElement).value),
          });
      },
    });
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/risk/process', component: Page },
        { path: '/risk/list', component: Page },
        { path: '/risk/detail', component: Page },
      ],
    });
    await router.push({ path: '/risk/process', query: { taskId: 'task-a', processInstanceId: 'process-a' } });
    const root = document.createElement('div');
    document.body.appendChild(root);
    const host = createLocalPageCacheHost(root, router, { installApp: () => undefined });
    const runtime = { httpClient: {} } as any;
    const pageA: LocalPageEntry = {
      tabKey: '/risk/process::task-a',
      component: Page,
      runtime,
    };

    host.activate(pageA, true);
    await nextTick();
    const inputA = root.querySelector('input')!;
    inputA.value = 'unsaved-a';
    inputA.dispatchEvent(new Event('input'));
    await nextTick();

    await router.push('/risk/list');
    host.activate({ tabKey: '/risk/list', component: Page, runtime }, false);
    await nextTick();
    await router.push({ path: '/risk/detail', query: { taskId: 'task-b', processInstanceId: 'process-b' } });
    host.activate({ tabKey: '/risk/detail::task-b', component: Page, runtime }, true);
    await nextTick();
    host.activate(pageA, true);
    await nextTick();

    expect(root.querySelector('input')!.value).toBe('unsaved-a');
    expect(lifecycle.initialized.filter((taskId) => taskId === 'task-a')).toHaveLength(1);
    expect(lifecycle.mounted).toBe(3);

    host.dispose();
    root.remove();
  });

  it('keeps form state and the same component instance after window resize', async () => {
    const lifecycle = { mounted: 0, unmounted: 0 };
    const Page = defineComponent({
      setup() {
        const value = ref('');
        onMounted(() => lifecycle.mounted++);
        onUnmounted(() => lifecycle.unmounted++);
        return () =>
          h('input', {
            value: value.value,
            onInput: (event) => (value.value = (event.target as HTMLInputElement).value),
          });
      },
    });
    const root = document.createElement('div');
    document.body.appendChild(root);
    const host = createLocalPageCacheHost(root, { install: () => undefined } as any, { installApp: () => undefined });
    const page: LocalPageEntry = {
      tabKey: '/risk/process::id=1308',
      component: Page,
      runtime: { httpClient: {} } as any,
    };

    host.activate(page, true);
    await nextTick();
    const input = root.querySelector('input')!;
    input.value = 'unsaved-risk-form';
    input.dispatchEvent(new Event('input'));
    await nextTick();

    window.dispatchEvent(new Event('resize'));
    await nextTick();

    expect(root.querySelector('input')).toBe(input);
    expect(root.querySelector('input')!.value).toBe('unsaved-risk-form');
    expect(lifecycle).toEqual({ mounted: 1, unmounted: 0 });

    host.dispose();
    root.remove();
  });

  it('keeps two route identities as independent cached component instances', async () => {
    const lifecycle = { mounted: 0, activated: 0, deactivated: 0, unmounted: 0 };
    const Page = defineComponent({
      setup() {
        const value = ref('');
        onMounted(() => lifecycle.mounted++);
        onActivated(() => lifecycle.activated++);
        onDeactivated(() => lifecycle.deactivated++);
        onUnmounted(() => lifecycle.unmounted++);
        return () =>
          h('input', {
            value: value.value,
            onInput: (event) => (value.value = (event.target as HTMLInputElement).value),
          });
      },
    });
    const root = document.createElement('div');
    document.body.appendChild(root);
    const host = createLocalPageCacheHost(root, { install: () => undefined } as any, { installApp: () => undefined });
    const runtime = { httpClient: {} } as any;
    const pageA: LocalPageEntry = { tabKey: '/detail::id=a', component: Page, runtime };
    const pageB: LocalPageEntry = { tabKey: '/detail::id=b', component: Page, runtime };

    host.activate(pageA, true);
    await nextTick();
    const inputA = root.querySelector('input')!;
    inputA.value = 'draft-a';
    inputA.dispatchEvent(new Event('input'));
    await nextTick();

    host.activate(pageB, true);
    await nextTick();
    expect(root.querySelector('input')!.value).toBe('');
    host.activate(pageA, true);
    await nextTick();

    expect(root.querySelector('input')!.value).toBe('draft-a');
    expect(host.cachedPages.value.size).toBe(2);
    expect(lifecycle.mounted).toBe(2);
    expect(lifecycle.deactivated).toBeGreaterThanOrEqual(1);
    expect(lifecycle.activated).toBeGreaterThanOrEqual(2);

    host.remove(pageA.tabKey);
    await nextTick();
    expect(host.cachedPages.value.has(pageA.tabKey)).toBe(false);
    expect(lifecycle.unmounted).toBeGreaterThanOrEqual(1);
    host.dispose();
    root.remove();
  });

  it('does not cache routes without keepAlive and remounts them when revisited', async () => {
    const lifecycle = { mounted: 0, unmounted: 0 };
    const Page = defineComponent({
      setup() {
        onMounted(() => lifecycle.mounted++);
        onUnmounted(() => lifecycle.unmounted++);
        return () => h('div', 'page');
      },
    });
    const root = document.createElement('div');
    document.body.appendChild(root);
    const host = createLocalPageCacheHost(root, { install: () => undefined } as any, { installApp: () => undefined });
    const runtime = { httpClient: {} } as any;
    const page = (suffix: string): LocalPageEntry => ({ tabKey: `/plain::${suffix}`, component: Page, runtime });

    host.activate(page('a'), false);
    await nextTick();
    host.activate(page('b'), false);
    await nextTick();
    expect(host.cachedPages.value.size).toBe(0);
    expect(lifecycle.mounted).toBe(2);
    expect(lifecycle.unmounted).toBeGreaterThanOrEqual(1);
    host.dispose();
    root.remove();
  });
});
