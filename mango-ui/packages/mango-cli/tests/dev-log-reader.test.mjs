import assert from 'node:assert/strict';
import { closeSync, mkdtempSync, openSync, rmSync, writeSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { getLogFileSize, readLogSuffixSince, tailLogFile } from '../src/dev-log-reader.mjs';

test('log readers return empty results for a missing file', () => {
  const path = join(tmpdir(), `mango-missing-log-${process.pid}-${Date.now()}`);
  assert.equal(getLogFileSize(path), 0);
  assert.equal(readLogSuffixSince(path, 0), '');
  assert.equal(tailLogFile(path, 20), '');
});

test('lifecycle diagnostics find a marker after a large sparse prefix', () => {
  const directory = mkdtempSync(join(tmpdir(), 'mango-log-reader-'));
  const path = join(directory, 'backend.log');
  const sparsePrefixSize = 4 * 1024 * 1024 * 1024;
  const marker = 'BOOTSTRAP_FINGERPRINT_MISMATCH\n';
  const descriptor = openSync(path, 'w');
  try {
    writeSync(descriptor, Buffer.from(marker), 0, Buffer.byteLength(marker), sparsePrefixSize);
  } finally {
    closeSync(descriptor);
  }

  try {
    assert.equal(getLogFileSize(path), sparsePrefixSize + Buffer.byteLength(marker));
    assert.match(readLogSuffixSince(path, sparsePrefixSize), /BOOTSTRAP_FINGERPRINT_MISMATCH/u);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});

test('tail reads only a bounded suffix and returns the requested last lines', () => {
  const directory = mkdtempSync(join(tmpdir(), 'mango-log-reader-'));
  const path = join(directory, 'backend.log');
  const prefix = Buffer.from('ignored-prefix\n');
  const suffix = Buffer.from('line-one\nline-two\nline-three\n');
  const descriptor = openSync(path, 'w');
  try {
    writeSync(descriptor, prefix, 0, prefix.length, 2 * 1024 * 1024);
    writeSync(descriptor, suffix, 0, suffix.length, 2 * 1024 * 1024 + prefix.length);
  } finally {
    closeSync(descriptor);
  }

  try {
    assert.equal(tailLogFile(path, 3, 64), 'line-two\nline-three\n\n');
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});
