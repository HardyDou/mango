import { readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

function readSource(relativePath: string) {
  return readFileSync(join(packageRoot, relativePath), 'utf-8');
}

describe('responsive layout stability', () => {
  it('keeps one LayoutMain host while the responsive layout chrome changes', () => {
    const layoutSource = readSource('src/layout/index.vue');

    expect(layoutSource).not.toContain('<component :is="layouts[layoutStore.layout]"');
    expect(layoutSource.match(/<LayoutMain\b/gu)).toHaveLength(1);
    expect(layoutSource).toContain('key="layout-main"');
    expect(layoutSource).toContain("window.addEventListener('resize', onLayoutResize)");
  });

  it('keeps one router outlet subtree when fixed-shell layout styling changes', () => {
    const mainSource = readSource('src/layout/component/main.vue');

    expect(mainSource.match(/<ShellRuntimeOutlet\b/gu)).toHaveLength(1);
    expect(mainSource.match(/<LayoutParentView\b/gu)).toHaveLength(1);
    expect(mainSource).not.toContain('<template v-if="!enableFixedShell">');
    expect(mainSource).not.toContain('v-else\n      class="layout-main-body');
  });
});
