import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

import {
  injectStableBootstrapIdentity,
  readStableBootstrapReceipt,
  validateStableBootstrapReceipt,
} from '../src/dev-bootstrap-receipt.mjs';

const fingerprint = 'a'.repeat(64);

function fixture() {
  const root = mkdtempSync(join(tmpdir(), 'mango-bootstrap-receipt-'));
  const directory = join(root, '.mango/bootstrap');
  mkdirSync(directory, { recursive: true });
  return { root, directory };
}

function receipt(overrides = {}) {
  return {
    schemaVersion: 1,
    environmentKey: 'mango_023',
    databaseName: 'mango_dev_acceptance_023',
    releaseId: 'release-1',
    buildRevision: '1.0.0-mango-023-SNAPSHOT',
    stableGeneration: 1,
    stableFingerprint: fingerprint,
    state: 'FINALIZED',
    ...overrides,
  };
}

function writeReceipt(directory, name, value) {
  writeFileSync(join(directory, name), `${JSON.stringify(value, null, 2)}\n`);
}

function readOptions(root, directory) {
  return {
    directories: [directory],
    workspaceRoot: root,
    workspaceId: 'mango_023',
    databaseName: 'mango_dev_acceptance_023',
    expectedRevision: '1.0.0-mango-023-SNAPSHOT',
  };
}

test('loads one finalized stable receipt and injects the complete runtime identity', () => {
  const { root, directory } = fixture();
  try {
    writeReceipt(directory, 'mango_023.json', receipt());
    const stable = readStableBootstrapReceipt(readOptions(root, directory));
    const args = injectStableBootstrapIdentity(
      ['-f', 'app/pom.xml', '-Dspring-boot.run.arguments=runtime --server.port=18023', 'spring-boot:run'],
      stable,
    );
    const springArguments = args.find((argument) => argument.startsWith('-Dspring-boot.run.arguments='));
    assert.match(springArguments, /--mango\.bootstrap\.environment-key=mango_023/u);
    assert.match(springArguments, /--mango\.release\.id=release-1/u);
    assert.match(springArguments, /--mango\.release\.revision=1\.0\.0-mango-023-SNAPSHOT/u);
    assert.match(springArguments, /--mango\.release\.generation=1/u);
    assert.match(springArguments, new RegExp(`--mango\\.release\\.fingerprint=${fingerprint}`, 'u'));
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('fails closed for missing and ambiguous receipts', () => {
  const { root, directory } = fixture();
  try {
    assert.throws(
      () => readStableBootstrapReceipt(readOptions(root, directory)),
      (error) => error.code === 'BOOTSTRAP_RUNTIME_RECEIPT_MISSING',
    );
    writeReceipt(directory, 'first.json', receipt());
    writeReceipt(directory, 'second.json', receipt());
    assert.throws(
      () => readStableBootstrapReceipt(readOptions(root, directory)),
      (error) => error.code === 'BOOTSTRAP_RUNTIME_RECEIPT_AMBIGUOUS',
    );
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('rejects zero generation, non-finalized state and stale revision', () => {
  assert.match(validateStableBootstrapReceipt(receipt({ stableGeneration: 0 })), /positive safe integer/u);
  assert.match(validateStableBootstrapReceipt(receipt({ state: 'EXPANDED' })), /FINALIZED/u);
  const { root, directory } = fixture();
  try {
    writeReceipt(directory, 'stale.json', receipt({ buildRevision: '1.0.0-mango-022-SNAPSHOT' }));
    assert.throws(
      () => readStableBootstrapReceipt(readOptions(root, directory)),
      (error) => error.code === 'BOOTSTRAP_RUNTIME_RECEIPT_STALE_REVISION',
    );
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('rejects database/environment drift and manually supplied identity arguments', () => {
  const first = fixture();
  try {
    writeReceipt(first.directory, 'database-drift.json', receipt({ databaseName: 'mango_dev_other_023' }));
    assert.throws(
      () => readStableBootstrapReceipt(readOptions(first.root, first.directory)),
      (error) => error.code === 'BOOTSTRAP_RUNTIME_RECEIPT_DATABASE_MISMATCH',
    );
  } finally {
    rmSync(first.root, { recursive: true, force: true });
  }

  const second = fixture();
  try {
    writeReceipt(second.directory, 'environment-drift.json', receipt({ environmentKey: 'mango_022' }));
    assert.throws(
      () => readStableBootstrapReceipt(readOptions(second.root, second.directory)),
      (error) => error.code === 'BOOTSTRAP_RUNTIME_RECEIPT_ENVIRONMENT_MISMATCH',
    );
  } finally {
    rmSync(second.root, { recursive: true, force: true });
  }

  assert.throws(
    () =>
      injectStableBootstrapIdentity(['-Dspring-boot.run.arguments=runtime --mango.release.generation=1'], receipt()),
    (error) => error.code === 'BOOTSTRAP_RUNTIME_IDENTITY_CONFLICT',
  );
});
