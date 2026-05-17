import { createApp, type Component, type App as VueApp } from 'vue';
import { createMemoryHistory, createRouter, type Router } from 'vue-router';
import type { MangoAppRuntime } from './index';

export interface MangoWujieVueAppOptions {
  standaloneRoot: Component;
  standaloneRouter: Router;
  runtimeRoot: Component;
  install: (app: VueApp) => void;
  mountSelector?: string;
  onStandaloneReady?: (router: Router) => void | Promise<void>;
  onMicroReady?: (runtime?: MangoAppRuntime) => void | Promise<void>;
}

export function createMangoWujieVueApp(options: MangoWujieVueAppOptions) {
  let app: VueApp | undefined;
  const mountSelector = options.mountSelector || '#app';

  async function mountStandalone() {
    app = createApp(options.standaloneRoot);
    options.install(app);
    app.use(options.standaloneRouter);
    await options.onStandaloneReady?.(options.standaloneRouter);
    app.mount(mountSelector);
  }

  async function mountWujie() {
    const runtime = getWujieRuntime();
    const runtimeRouter = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/:pathMatch(.*)*',
          component: { render: () => null },
        },
      ],
    });

    app = createApp(options.runtimeRoot);
    options.install(app);
    app.use(runtimeRouter);
    if (runtime) {
      app.provide('mangoRuntime', runtime);
      await runtimeRouter.push(runtime.menu?.path || '/');
    }
    await options.onMicroReady?.(runtime);
    app.mount(mountSelector);
  }

  function unmountWujie() {
    app?.unmount();
    app = undefined;
  }

  const runtimeWindow = window as Window & {
    __POWERED_BY_WUJIE__?: boolean;
    __WUJIE_MOUNT?: () => void | Promise<void>;
    __WUJIE_UNMOUNT?: () => void;
  };
  if (runtimeWindow.__POWERED_BY_WUJIE__) {
    runtimeWindow.__WUJIE_MOUNT = mountWujie;
    runtimeWindow.__WUJIE_UNMOUNT = unmountWujie;
  } else {
    void mountStandalone();
  }
}

function getWujieRuntime() {
  return (window as Window & {
    $wujie?: {
      props?: {
        mangoRuntime?: MangoAppRuntime;
      };
    };
  }).$wujie?.props?.mangoRuntime;
}
