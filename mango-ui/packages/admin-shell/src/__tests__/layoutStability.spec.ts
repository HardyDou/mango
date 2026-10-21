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

  it('exposes the content scroll operations to the layout host', () => {
    const mainSource = readSource('src/layout/component/main.vue');

    expect(mainSource).toContain('const resetContentScroll = (): boolean =>');
    expect(mainSource).toContain('defineExpose({ updateScrollbar, resetContentScroll });');
    expect(mainSource).toContain('[mango-shell] failed to reset content scroll: scrollbar wrap is not ready');
  });

  it('keeps the tab cache host mounted when a non-cached page is active', () => {
    const parentSource = readSource('src/layout/routerView/parent.vue');

    expect(parentSource).toContain('<keep-alive>');
    expect(parentSource).not.toContain('<keep-alive v-if="route.meta.keepAlive">');
    expect(parentSource).toContain(':is="Component" v-if="route.meta.keepAlive"');
    expect(parentSource).toContain(':is="Component" v-if="!route.meta.keepAlive"');
  });
});
