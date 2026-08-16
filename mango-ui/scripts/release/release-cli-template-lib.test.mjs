import assert from 'node:assert/strict';
import test from 'node:test';
import {
  assertCliFullFrontendTemplateProjection,
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
