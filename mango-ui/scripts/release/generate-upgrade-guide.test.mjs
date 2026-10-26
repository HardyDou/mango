import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import {
  MANUAL_UPGRADE_GUIDE_MARKER,
  UPGRADE_GUIDE_MARKER,
  assertUpgradeGuide,
  generateUpgradeGuide,
  upgradeGuideRelativePath,
} from './generate-upgrade-guide.mjs';

function plan() {
  return {
    maven: { sourceVersion: '1.0.63', targetVersion: '1.1.0' },
    release: { tag: 'v2026.10.21-maven-1.1.0-test-release' },
    packages: [
      { name: '@mango/base', sourceVersion: '1.0.0', targetVersion: '1.0.1' },
      { name: '@mango/cli', sourceVersion: '1.3.3', targetVersion: '1.3.4' },
    ],
  };
}

const notes = `## v2026.10.21-maven-1.1.0-test-release

# Pull Requests

- [PR #1](https://github.com/HardyDou/mango/pull/1) Changed (breaking): converged sync modes.

# Fixed

- Fixed the bootstrap stall.

# Added

- Added ownership records.

# Changed

- Changed the release enum.

# Versions

- Maven 1.1.0.
`;

test('upgrade guide path follows the Maven source and target versions', () => {
  assert.equal(
    upgradeGuideRelativePath(plan()),
    'mango-docs/guides/operations/mango-1.0.63-to-1.1.0-upgrade.md',
  );
});

test('generateUpgradeGuide creates, updates and preserves files deterministically', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-upgrade-guide-'));
  try {
    const first = generateUpgradeGuide({ repoRoot: root, plan: plan(), notes });
    assert.equal(first.action, 'created');
    const written = readFileSync(join(root, first.relativePath), 'utf8');
    assert.ok(written.includes(UPGRADE_GUIDE_MARKER));
    assert.ok(written.includes('Mango 1.0.63 → 1.1.0 升级指南'));
    assert.ok(written.includes('<mango.version>1.1.0</mango.version>'));
    assert.ok(written.includes('`@mango/cli@1.3.4`'));
    assert.ok(written.includes('### Fixed'));
    assert.ok(written.includes('Fixed the bootstrap stall.'));
    assert.equal((written.match(/### Added/g) ?? []).length, 1);
    assert.equal((written.match(/### Changed/g) ?? []).length, 1);
    assert.ok(written.includes('## 8. Agent 执行清单'));
    assert.ok(written.includes('v2026.10.21-maven-1.1.0-test-release'));

    const second = generateUpgradeGuide({ repoRoot: root, plan: plan(), notes });
    assert.equal(second.action, 'updated');
    assert.equal(readFileSync(join(root, second.relativePath), 'utf8'), written);

    const manualPath = join(root, second.relativePath);
    writeFileSync(manualPath, '# hand written upgrade notes\n');
    const third = generateUpgradeGuide({ repoRoot: root, plan: plan(), notes });
    assert.equal(third.action, 'preserved');
    assert.equal(readFileSync(manualPath, 'utf8'), '# hand written upgrade notes\n');
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('assertUpgradeGuide accepts the sealed file and rejects drift', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-upgrade-guide-'));
  try {
    generateUpgradeGuide({ repoRoot: root, plan: plan(), notes });
    assert.equal(assertUpgradeGuide({ repoRoot: root, plan: plan(), notes }), upgradeGuideRelativePath(plan()));

    writeFileSync(join(root, upgradeGuideRelativePath(plan())), 'drifted\n');
    assert.throws(() => assertUpgradeGuide({ repoRoot: root, plan: plan(), notes }), /add the manual marker or regenerate/u);

    rmSync(join(root, upgradeGuideRelativePath(plan())));
    assert.throws(() => assertUpgradeGuide({ repoRoot: root, plan: plan(), notes }), /requires the generated upgrade guide/u);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('assertUpgradeGuide accepts a manual guide only when it is explicitly marked', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-upgrade-guide-'));
  try {
    const manualPath = join(root, upgradeGuideRelativePath(plan()));
    mkdirSync(dirname(manualPath), { recursive: true });
    writeFileSync(manualPath, '# manual\n');
    assert.throws(() => assertUpgradeGuide({ repoRoot: root, plan: plan(), notes }), /hand-written/u);

    writeFileSync(manualPath, `${MANUAL_UPGRADE_GUIDE_MARKER}\n# manual\n`);
    assert.equal(assertUpgradeGuide({ repoRoot: root, plan: plan(), notes }), upgradeGuideRelativePath(plan()));
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});
