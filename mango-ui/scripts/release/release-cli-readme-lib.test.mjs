import assert from 'node:assert/strict';
import test from 'node:test';
import { assertCliReadmeProjection, projectCliReadmeVersion } from './release-cli-readme-lib.mjs';

const source = `# @mango/cli

| 当前发布版本  | \`1.0.107\` |

\`\`\`bash
npm view @mango/cli@1.0.107 version --registry "$MANGO_NPM_REGISTRY"
npm install -g @mango/cli@1.0.107 --registry "$MANGO_NPM_REGISTRY"
\`\`\`

Historical release: @mango/cli@1.0.107.
`;

test('projects the CLI release version in exactly the three public version positions', () => {
  const projected = projectCliReadmeVersion(source, '1.0.107', '1.0.108');

  assert.match(projected, /\| 当前发布版本 {2}\| `1\.0\.108` \|/u);
  assert.match(projected, /npm view @mango\/cli@1\.0\.108 version --registry/u);
  assert.match(projected, /npm install -g @mango\/cli@1\.0\.108 --registry/u);
  assert.match(projected, /Historical release: @mango\/cli@1\.0\.107\./u);
});

test('rejects any CLI README drift beyond the deterministic version projection', () => {
  const projected = `${projectCliReadmeVersion(source, '1.0.107', '1.0.108')}extra release text\n`;

  assert.throws(
    () =>
      assertCliReadmeProjection({
        sourceContent: source,
        projectedContent: projected,
        sourceVersion: '1.0.107',
        targetVersion: '1.0.108',
      }),
    /differs from the deterministic release version projection/u,
  );
});

test('fails closed when a required public version position is missing', () => {
  assert.throws(
    () =>
      projectCliReadmeVersion(
        source.replace('npm view @mango/cli@1.0.107 version --registry', ''),
        '1.0.107',
        '1.0.108',
      ),
    /npm view command must contain exactly one source version/u,
  );
});
