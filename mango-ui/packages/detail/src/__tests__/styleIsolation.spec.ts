import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const packageRoot = resolve(process.cwd());

describe('@mango/detail 样式与依赖隔离', () => {
  it('只发布组件局部样式，不引入 Admin 或 Shell 全局样式', () => {
    const pageSource = readFileSync(resolve(packageRoot, 'src/MangoCollapseDetailPage.vue'), 'utf8');
    const richTextSource = readFileSync(resolve(packageRoot, 'src/MangoRichTextPreview.vue'), 'utf8');
    const packageJson = JSON.parse(readFileSync(resolve(packageRoot, 'package.json'), 'utf8')) as {
      dependencies?: Record<string, string>;
      peerDependencies?: Record<string, string>;
    };
    const combinedSource = `${pageSource}\n${richTextSource}`;

    expect(pageSource).toContain('<style scoped>');
    expect(combinedSource).not.toMatch(/@mango\/admin(?:-shell)?/);
    expect(combinedSource).not.toMatch(/style-full\.css|element-plus\/(?:dist|theme-chalk)/);
    expect(combinedSource).not.toMatch(/(?:^|\n)\s*(?:html|body|#app|\.mango-layout-[\w-]*)\s*(?:,|\{)/m);
    expect(combinedSource).not.toMatch(/:global\(\.el-/);
    expect(packageJson.dependencies).toBeUndefined();
    expect(packageJson.peerDependencies).toMatchObject({
      '@mango/common': 'workspace:2.0.4',
      '@mango/file': 'workspace:1.0.40',
    });
  });
});
