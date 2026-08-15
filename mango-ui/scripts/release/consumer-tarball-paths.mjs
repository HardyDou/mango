import { realpathSync } from 'node:fs';
import { relative } from 'node:path';

export function toCanonicalRelativePath(fromRoot, targetPath) {
  return relative(realpathSync(fromRoot), realpathSync(targetPath)).split('\\').join('/');
}
