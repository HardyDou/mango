import type { Plugin } from 'vite';

export declare const MOCK_SERVICE_WORKER_FILE: 'mockServiceWorker.js';
export declare function removeMockServiceWorkerFromBuild(outDir: string): boolean;
export declare function excludeMockServiceWorkerFromProductionBuild(): Plugin;
