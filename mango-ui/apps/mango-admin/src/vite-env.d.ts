/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const component: DefineComponent<object, object, any>;
  export default component;
}

interface ImportMetaEnv {
  readonly VITE_ADMIN_PROXY_PATH: string;
  readonly VITE_PORT: string;
  readonly VITE_OPEN: string;
  readonly VITE_SM4_KEY: string;
  readonly VITE_APP_TITLE: string;
  readonly VITE_PUBLIC_PATH: string;
  readonly VITE_FILE_PREVIEW_PROVIDER_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare global {
  interface Window {
    __MANGO_PINIA__: any;
    __MANGO_MITT_BUS__: any;
  }
}
