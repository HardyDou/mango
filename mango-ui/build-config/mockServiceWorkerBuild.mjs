import { existsSync, rmSync } from 'node:fs';
import { isAbsolute, resolve } from 'node:path';

export const MOCK_SERVICE_WORKER_FILE = 'mockServiceWorker.js';

export function removeMockServiceWorkerFromBuild(outDir) {
  const workerPath = resolve(outDir, MOCK_SERVICE_WORKER_FILE);
  if (!existsSync(workerPath)) {
    return false;
  }
  rmSync(workerPath);
  return true;
}

export function excludeMockServiceWorkerFromProductionBuild() {
  let outDir;
  return {
    name: 'mango-exclude-mock-service-worker-from-production',
    apply: 'build',
    configResolved(config) {
      outDir = isAbsolute(config.build.outDir) ? config.build.outDir : resolve(config.root, config.build.outDir);
    },
    closeBundle() {
      if (outDir) {
        removeMockServiceWorkerFromBuild(outDir);
      }
    },
  };
}
