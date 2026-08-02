import { existsSync, lstatSync, readdirSync, readFileSync } from 'node:fs';
import { isAbsolute, join, relative, resolve } from 'node:path';

class BootstrapReceiptError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'BootstrapReceiptError';
    this.code = code;
  }
}

export function readStableBootstrapReceipt({
  directories,
  workspaceRoot,
  workspaceId,
  databaseName,
  expectedRevision,
}) {
  const parsed = [];
  for (const directory of directories) {
    if (!existsSync(directory)) {
      continue;
    }
    for (const name of readdirSync(directory).filter((entry) => entry.endsWith('.json')).sort()) {
      const path = join(directory, name);
      const stats = lstatSync(path);
      if (!stats.isFile()) {
        if (stats.isSymbolicLink()) {
          throw receiptError('BOOTSTRAP_RUNTIME_RECEIPT_INVALID', workspaceRoot, path, 'symbolic links are forbidden');
        }
        continue;
      }
      try {
        parsed.push({ ...JSON.parse(readFileSync(path, 'utf8')), path });
      } catch (error) {
        throw receiptError('BOOTSTRAP_RUNTIME_RECEIPT_INVALID', workspaceRoot, path, error.message);
      }
    }
  }

  const environmentReceipts = parsed.filter((receipt) => receipt.environmentKey === workspaceId);
  const databaseReceipts = environmentReceipts.filter((receipt) => receipt.databaseName === databaseName);
  if (databaseReceipts.length === 0) {
    if (environmentReceipts.length > 0) {
      throw new BootstrapReceiptError(
        'BOOTSTRAP_RUNTIME_RECEIPT_DATABASE_MISMATCH',
        `expected=${databaseName} actual=${environmentReceipts
          .map((receipt) => receipt.databaseName || '<missing>')
          .join(',')}`,
      );
    }
    const sameDatabase = parsed.filter((receipt) => receipt.databaseName === databaseName);
    if (sameDatabase.length > 0) {
      throw new BootstrapReceiptError(
        'BOOTSTRAP_RUNTIME_RECEIPT_ENVIRONMENT_MISMATCH',
        `expected=${workspaceId} actual=${sameDatabase
          .map((receipt) => receipt.environmentKey || '<missing>')
          .join(',')}`,
      );
    }
    throw new BootstrapReceiptError(
      'BOOTSTRAP_RUNTIME_RECEIPT_MISSING',
      `environment=${workspaceId} database=${databaseName}; run bootstrap apply/finalize before mango dev start`,
    );
  }
  if (databaseReceipts.length > 1) {
    throw new BootstrapReceiptError(
      'BOOTSTRAP_RUNTIME_RECEIPT_AMBIGUOUS',
      `paths=${databaseReceipts.map((receipt) => relativeOrAbsolute(workspaceRoot, receipt.path)).join(',')}`,
    );
  }

  const receipt = databaseReceipts[0];
  const invalidReason = validateStableBootstrapReceipt(receipt);
  if (invalidReason) {
    throw receiptError('BOOTSTRAP_RUNTIME_RECEIPT_INVALID', workspaceRoot, receipt.path, invalidReason);
  }
  if (receipt.buildRevision !== expectedRevision) {
    throw receiptError(
      'BOOTSTRAP_RUNTIME_RECEIPT_STALE_REVISION',
      workspaceRoot,
      receipt.path,
      `expected=${expectedRevision} actual=${receipt.buildRevision}`,
    );
  }
  return receipt;
}

export function injectStableBootstrapIdentity(args, receipt) {
  const result = [...args];
  const propertyIndexes = result
    .map((argument, index) => (String(argument).startsWith('-Dspring-boot.run.arguments=') ? index : -1))
    .filter((index) => index >= 0);
  if (propertyIndexes.length !== 1) {
    throw new BootstrapReceiptError(
      'BOOTSTRAP_RUNTIME_COMMAND_UNSUPPORTED',
      'expected exactly one spring-boot.run.arguments property',
    );
  }
  const propertyIndex = propertyIndexes[0];
  const springArguments = String(result[propertyIndex]);
  if (/--mango\.(?:bootstrap\.environment-key|release\.(?:id|revision|generation|fingerprint))=/u.test(springArguments)) {
    throw new BootstrapReceiptError(
      'BOOTSTRAP_RUNTIME_IDENTITY_CONFLICT',
      'remove manually configured release identity arguments',
    );
  }
  const identityArguments = [
    `--mango.bootstrap.environment-key=${receipt.environmentKey}`,
    `--mango.release.id=${receipt.releaseId}`,
    `--mango.release.revision=${receipt.buildRevision}`,
    `--mango.release.generation=${receipt.stableGeneration}`,
    `--mango.release.fingerprint=${receipt.stableFingerprint}`,
  ];
  result[propertyIndex] = `${springArguments} ${identityArguments.join(' ')}`;
  return result;
}

export function validateStableBootstrapReceipt(receipt) {
  if (receipt.schemaVersion !== 1) {
    return 'schemaVersion must be 1';
  }
  for (const field of [
    'environmentKey',
    'databaseName',
    'releaseId',
    'buildRevision',
    'stableFingerprint',
    'state',
  ]) {
    if (typeof receipt[field] !== 'string' || !receipt[field].trim()) {
      return `${field} must be a non-empty string`;
    }
  }
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/u.test(receipt.environmentKey)) {
    return 'environmentKey contains unsupported characters';
  }
  if (!Number.isSafeInteger(receipt.stableGeneration) || receipt.stableGeneration <= 0) {
    return 'stableGeneration must be a positive safe integer';
  }
  if (!/^[0-9a-f]{64}$/u.test(receipt.stableFingerprint)) {
    return 'stableFingerprint must be a lowercase SHA-256 value';
  }
  if (receipt.state !== 'FINALIZED') {
    return 'state must be FINALIZED';
  }
  for (const [field, value] of [
    ['releaseId', receipt.releaseId],
    ['buildRevision', receipt.buildRevision],
  ]) {
    if (!/^[A-Za-z0-9][A-Za-z0-9._:+-]{0,127}$/u.test(value)) {
      return `${field} contains unsupported command characters`;
    }
  }
  return '';
}

function receiptError(code, workspaceRoot, path, reason) {
  return new BootstrapReceiptError(code, `path=${relativeOrAbsolute(workspaceRoot, path)} reason=${reason}`);
}

function relativeOrAbsolute(root, path) {
  const relativePath = relative(resolve(root), resolve(path));
  return relativePath === '' || (!relativePath.startsWith('..') && !isAbsolute(relativePath)) ? relativePath || '.' : path;
}
