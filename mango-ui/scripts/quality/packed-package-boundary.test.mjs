import assert from 'node:assert/strict';
import test from 'node:test';
import { assertPackedPackageBoundary, packedExportTargets } from './packed-package-boundary.mjs';

test('collects root fields and every conditional export target', () => {
  const targets = packedExportTargets({
    main: './dist/index.cjs',
    types: './dist/index.d.ts',
    exports: {
      '.': { types: './dist/index.d.ts', import: './dist/index.js' },
      './style.css': './style.css',
    },
  });
  assert.deepEqual(
    targets.map(({ target }) => target),
    ['./dist/index.cjs', './dist/index.d.ts', './dist/index.d.ts', './dist/index.js', './style.css'],
  );
});

test('rejects missing JavaScript and stylesheet targets in the packed tarball', () => {
  const packageJson = {
    name: '@mango/example',
    exports: { '.': { import: './dist/index.js' }, './style.css': './style.css' },
  };
  assert.throws(
    () => assertPackedPackageBoundary(packageJson, ['package/package.json', 'package/style.css']),
    /missing published export: exports\...import -> \.\/dist\/index\.js/u,
  );
  assert.throws(
    () => assertPackedPackageBoundary(packageJson, ['package/package.json', 'package/dist/index.js']),
    /missing published export: exports\.\.\/style\.css -> \.\/style\.css/u,
  );
});

test('accepts recursive wildcard targets and rejects source leakage', () => {
  const packageJson = {
    name: '@mango/example',
    exports: { './utils/*': { types: './dist/utils/*.d.ts', import: './dist/utils/*.js' } },
  };
  assert.doesNotThrow(() =>
    assertPackedPackageBoundary(packageJson, [
      'package/package.json',
      'package/dist/utils/realtime/socket.d.ts',
      'package/dist/utils/realtime/socket.js',
    ]),
  );
  assert.throws(
    () => assertPackedPackageBoundary(packageJson, ['package/package.json', 'package/src/index.ts']),
    /must not publish source file/u,
  );
  assert.throws(
    () =>
      assertPackedPackageBoundary(packageJson, [
        'package/package.json',
        'package/dist/utils/format.d.ts',
        'package/dist/utils/format.js',
        'package/dist/utils/realtime/socket.d.ts',
      ]),
    /wildcard export conditions do not publish the same subpaths/u,
  );
});

test('rejects workspace protocols from a packed consumer manifest', () => {
  assert.throws(
    () =>
      assertPackedPackageBoundary(
        {
          name: '@mango/example',
          dependencies: { '@mango/common': 'workspace:1.2.3' },
        },
        ['package/package.json'],
      ),
    /packed dependencies\.@mango\/common must not expose workspace protocol/u,
  );
});
