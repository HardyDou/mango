import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

describe('vertical nav menu indentation', () => {
  it('keeps left menu depth independent from top menu and increments nested levels', () => {
    const verticalSource = readSource('layout/navMenu/vertical.vue');
    const subItemSource = readSource('layout/navMenu/subItem.vue');

    expect(verticalSource).toContain(':level="2"');
    expect(subItemSource).toContain(':level="level + 1"');
    expect(subItemSource).toContain('ROOT_MENU_INDENT + (level - 1) * MENU_INDENT_STEP');
    expect(verticalSource).toContain('padding-left: var(--mango-nav-menu-indent, 20px) !important;');
    expect(verticalSource).not.toContain('padding-left: 44px !important;');
  });
});

function readSource(relativePath: string) {
  return readFileSync(join(packageRoot, 'src', relativePath), 'utf-8');
}
