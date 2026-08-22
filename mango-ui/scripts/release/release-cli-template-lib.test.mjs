import assert from 'node:assert/strict';
import test from 'node:test';
import {
  assertCliFullReadmeProjection,
  assertCliFullFrontendTemplateProjection,
  CLI_FULL_README_TEMPLATE_PATH,
  projectCliFullReadmeTuple,
  projectCliFullFrontendTemplateVersion,
} from './release-cli-template-lib.mjs';

const source = `{
  "name": "{{projectKebab}}-frontend",
  "dependencies": {
{{frontendPackageDependencies}}
    "vue": "{{vueVersion}}"
  },
  "devDependencies": {
    "@mango/cli": "1.0.107",
    "typescript": "{{typescriptVersion}}"
  }
}
`;

test('projects the full frontend template CLI devDependency without parsing template placeholders', () => {
  const projected = projectCliFullFrontendTemplateVersion(source, '1.0.107', '1.0.108');

  assert.match(projected, /"@mango\/cli": "1\.0\.108"/u);
  assert.match(projected, /\{\{frontendPackageDependencies\}\}/u);
});

test('rejects any full frontend template drift beyond the deterministic CLI version projection', () => {
  const projected = projectCliFullFrontendTemplateVersion(source, '1.0.107', '1.0.108').replace(
    '"typescript": "{{typescriptVersion}}"',
    '"typescript": "5.9.3"',
  );

  assert.throws(
    () =>
      assertCliFullFrontendTemplateProjection({
        sourceContent: source,
        projectedContent: projected,
        sourceVersion: '1.0.107',
        targetVersion: '1.0.108',
      }),
    /differs from the deterministic release version projection/u,
  );
});

test('fails closed when the full frontend template CLI source version is missing', () => {
  assert.throws(
    () => projectCliFullFrontendTemplateVersion(source.replace('"@mango/cli": "1.0.107",', ''), '1.0.107', '1.0.108'),
    /must contain exactly one source version/u,
  );
});

test('projects the generated project README release tuple', () => {
  const sourceReadme = `### Issue #690 升级合同

本模板随 Maven \`1.0.37\`、\`@mango/pmo@1.3.16\`、\`@mango/cli@1.0.109\` 完整 tuple。

升级顺序：安装 \`@mango/cli@1.0.109\`，执行 \`mango pmo upgrade --project-dir . --to 1.3.16 --dry-run\`，并把 \`mango-bom\` 保持为 \`1.0.37\`。

生成业务模块时，继续执行正式流程。
`;
  const versions = { mavenVersion: '1.0.39', pmoVersion: '1.4.0', cliVersion: '1.1.0' };
  const projected = projectCliFullReadmeTuple(sourceReadme, versions);
  assert.match(projected, /Maven `1\.0\.39`/u);
  assert.match(projected, /@mango\/pmo@1\.4\.0/u);
  assert.equal(projected.match(/@mango\/cli@1\.1\.0/gu)?.length, 2);
  assert.match(projected, /--to 1\.4\.0 --dry-run/u);
  assert.match(projected, /`mango-bom` 保持为 `1\.0\.39`/u);
  assert.doesNotThrow(() =>
    assertCliFullReadmeProjection({ sourceContent: sourceReadme, projectedContent: projected, versions }),
  );
  assert.equal(CLI_FULL_README_TEMPLATE_PATH, 'mango-ui/packages/mango-cli/templates/full/README.md');
});
