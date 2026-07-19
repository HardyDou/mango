import { createApp, type Component, type App as VueApp } from 'vue';
import { createMemoryHistory, createRouter, type Router } from 'vue-router';
import { MANGO_HTTP_CLIENT_KEY, type MangoAppRuntime, type MangoRuntimeTheme } from './index';

type MangoVueRoot = Component | (() => Promise<Component | { default?: Component }>);

export interface MangoWujieVueAppOptions {
  standaloneRoot: MangoVueRoot;
  standaloneRouter: Router;
  runtimeRoot: Component;
  install: (app: VueApp, runtime?: MangoAppRuntime) => void;
  mountSelector?: string;
  onStandaloneReady?: (router: Router) => void | Promise<void>;
  onMicroReady?: (runtime?: MangoAppRuntime) => void | (() => void) | Promise<void | (() => void)>;
}

export function createMangoWujieVueApp(options: MangoWujieVueAppOptions) {
  let app: VueApp | undefined;
  let microDispose: (() => void) | undefined;
  const mountSelector = options.mountSelector || '#app';

  async function mountStandalone() {
    app = createApp(await resolveRoot(options.standaloneRoot));
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
    options.install(app, runtime);
    app.use(runtimeRouter);
    if (runtime) {
      app.provide('mangoRuntime', runtime);
      app.provide(MANGO_HTTP_CLIENT_KEY, runtime.httpClient);
      await runtimeRouter.push(runtime.menu?.path || '/');
    }
    const dispose = await options.onMicroReady?.(runtime);
    microDispose = typeof dispose === 'function' ? dispose : undefined;
    app.mount(mountSelector);
  }

  function unmountWujie() {
    microDispose?.();
    microDispose = undefined;
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

export function applyMangoRuntimeTheme(theme?: MangoRuntimeTheme) {
  if (typeof document === 'undefined' || !theme) {
    return;
  }
  const root = document.documentElement;
  root.setAttribute('data-theme', theme.isDark ? 'dark' : 'light');
  applyCssVar('--mango-color-primary', theme.primary);
  applyCssVar('--el-color-primary', theme.primary);
  applyCssVar('--mango-bg-top-bar', theme.topBar);
  applyCssVar('--mango-color-top-bar', theme.topBarColor);
  applyCssVar('--mango-bg-menu-bar', theme.menuBar);
  applyCssVar('--mango-color-menu-bar', theme.menuBarColor);
  applyCssVar('--mango-color-menu-active-bg', theme.menuBarActiveColor);
  applyCssVar('--mango-bg-columns-menu-bar', theme.columnsMenuBar);
  applyCssVar('--mango-color-columns-menu-bar', theme.columnsMenuBarColor);
  Object.entries(theme.tokens || {}).forEach(([name, value]) => {
    applyCssVar(name, value);
  });
}

export function bindMangoRuntimeTheme(runtime?: MangoAppRuntime) {
  applyMangoRuntimeTheme(runtime?.theme);
  return runtime?.eventBus.on('theme-change', (theme) => {
    applyMangoRuntimeTheme(theme as MangoRuntimeTheme);
  });
}

async function resolveRoot(root: MangoVueRoot): Promise<Component> {
  if (typeof root !== 'function') {
    return root;
  }
  const module = await root();
  return ('default' in Object(module) ? (module as { default?: Component }).default : module) as Component;
}

function getWujieRuntime() {
  return (
    window as Window & {
      $wujie?: {
        props?: {
          mangoRuntime?: MangoAppRuntime;
        };
      };
    }
  ).$wujie?.props?.mangoRuntime;
}

function applyCssVar(name: string, value?: string) {
  if (!value) {
    return;
  }
  document.documentElement.style.setProperty(name, value);
}
