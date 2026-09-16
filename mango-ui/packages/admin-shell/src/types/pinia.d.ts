import type { PiniaPluginContext, StateTree } from 'pinia';

interface MangoPersistOptions {
  enabled?: boolean;
  afterRestore?: (context: PiniaPluginContext) => void;
  strategies?: Array<{ key: string; storage: Storage }>;
}

declare module 'pinia' {
  interface DefineStoreOptionsBase<S extends StateTree, Store> {
    persist?: boolean | MangoPersistOptions | MangoPersistOptions[];
  }
}

export {};
