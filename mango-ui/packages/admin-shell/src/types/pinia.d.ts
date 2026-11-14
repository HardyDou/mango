import type { PiniaPluginContext, StateTree } from 'pinia';

interface MangoPersistOptions {
  enabled?: boolean;
  afterRestore?: (context: PiniaPluginContext) => void;
  strategies?: Array<{ key: string; storage: Storage }>;
}

declare module 'pinia' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface DefineStoreOptionsBase<S extends StateTree, Store> {
    persist?: boolean | MangoPersistOptions | MangoPersistOptions[];
  }
}

export {};
