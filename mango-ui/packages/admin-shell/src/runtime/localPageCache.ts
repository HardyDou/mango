import {
  createApp,
  defineComponent,
  Fragment,
  h,
  KeepAlive,
  nextTick,
  provide,
  ref,
  shallowReadonly,
  shallowRef,
  computed,
  type App as VueApp,
  type Component,
  type PropType,
} from 'vue';
import { routeLocationKey, routerViewLocationKey, type RouteLocationNormalizedLoaded, type Router } from 'vue-router';
import type { MangoAppRuntime } from '@mango/app-runtime';
import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';
import { installShellApp } from '../appBootstrap';

export interface LocalPageEntry {
  tabKey: string;
  component: Component;
  runtime: MangoAppRuntime;
  cacheName?: string;
  route?: RouteLocationNormalizedLoaded;
}

const LocalPageHost = defineComponent({
  name: 'MangoRuntimeLocalPage',
  props: {
    entry: {
      type: Object as PropType<LocalPageEntry>,
      required: true,
    },
  },
  setup(props) {
    provide('mangoRuntime', props.entry.runtime);
    provide(MANGO_HTTP_CLIENT_KEY, props.entry.runtime.httpClient);
    if (props.entry.route) {
      // Cached pages must not observe another tab's global route changes while
      // deactivated. Otherwise business route watchers can reset draft state.
      provide(routeLocationKey, props.entry.route);
      provide(routerViewLocationKey, shallowRef(props.entry.route));
    }
    return () => h(props.entry.component);
  },
});

export function createLocalPageCacheHost(
  root: HTMLElement,
  router: Router,
  options: { installApp?: (app: VueApp) => void } = {},
) {
  const cachedPages = shallowRef(new Map<string, LocalPageEntry>());
  const activeTabKey = ref('');
  const transientPage = shallowRef<LocalPageEntry>();
  let cacheNameSequence = 0;
  const cachedComponentNames = computed(() =>
    Array.from(cachedPages.value.values())
      .map((entry) => entry.cacheName)
      .filter(Boolean),
  );

  function createCachedEntry(entry: LocalPageEntry): LocalPageEntry {
    if (entry.cacheName) {
      return entry;
    }
    const cacheName = `MangoRuntimeLocalPage_${++cacheNameSequence}`;
    return {
      ...entry,
      cacheName,
      component: defineComponent({
        name: cacheName,
        setup: () => () => h(LocalPageHost, { entry }),
      }),
    };
  }

  function withRouteContext(entry: LocalPageEntry): LocalPageEntry {
    if (entry.route) {
      return entry;
    }
    const route = router.currentRoute?.value;
    if (!route) {
      return entry;
    }
    return {
      ...entry,
      route: shallowReadonly({
        ...route,
        query: { ...route.query },
        params: { ...route.params },
        meta: { ...route.meta },
        matched: [...route.matched],
      }) as RouteLocationNormalizedLoaded,
    };
  }

  const app = createApp({
    render: () => {
      const activeEntry = cachedPages.value.get(activeTabKey.value);
      const cached = h(
        KeepAlive,
        { include: cachedComponentNames.value },
        {
          default: () => (activeEntry ? h(activeEntry.component, { key: activeEntry.tabKey }) : null),
        },
      );
      const transient = transientPage.value
        ? h(LocalPageHost, { key: transientPage.value.tabKey, entry: transientPage.value })
        : null;
      return h(Fragment, null, [cached, transient]);
    },
  });

  (options.installApp || installShellApp)(app);
  app.use(router);
  app.mount(root);

  function activate(entry: LocalPageEntry, keepAlive: boolean) {
    entry = withRouteContext(entry);
    if (keepAlive) {
      entry = createCachedEntry(entry);
      if (!cachedPages.value.has(entry.tabKey)) {
        cachedPages.value = new Map(cachedPages.value).set(entry.tabKey, entry);
      }
      activeTabKey.value = entry.tabKey;
      const previousTransient = transientPage.value;
      transientPage.value = undefined;
      if (previousTransient) {
        nextTick(() => previousTransient.runtime.dispose?.());
      }
      return;
    }
    const previousTransient = transientPage.value;
    activeTabKey.value = '';
    transientPage.value = entry;
    if (previousTransient && previousTransient.tabKey !== entry.tabKey) {
      nextTick(() => previousTransient.runtime.dispose?.());
    }
  }

  function deactivate(tabKey: string) {
    if (activeTabKey.value === tabKey) {
      activeTabKey.value = '';
    }
    if (transientPage.value?.tabKey === tabKey) {
      const entry = transientPage.value;
      transientPage.value = undefined;
      nextTick(() => entry.runtime.dispose?.());
    }
  }

  function remove(tabKey: string) {
    const entry = cachedPages.value.get(tabKey);
    if (!entry) {
      return;
    }
    const nextPages = new Map(cachedPages.value);
    nextPages.delete(tabKey);
    cachedPages.value = nextPages;
    deactivate(tabKey);
    nextTick(() => entry.runtime.dispose?.());
  }

  function dispose() {
    const pages = Array.from(cachedPages.value.values());
    const transient = transientPage.value;
    cachedPages.value = new Map();
    activeTabKey.value = '';
    transientPage.value = undefined;
    app.unmount();
    pages.forEach((entry) => entry.runtime.dispose?.());
    transient?.runtime.dispose?.();
  }

  return {
    app,
    activate,
    remove,
    dispose,
    deactivate,
    cachedPages,
    activeTabKey,
    transientPage,
  };
}
