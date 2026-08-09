#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { existsSync, lstatSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentFile = fileURLToPath(import.meta.url);
const cliRoot = resolve(dirname(currentFile), '..');
const repoRoot = resolve(cliRoot, '../../..');
const defaultSourceRoot = join(repoRoot, 'mango-business-starter/backend/modules/{{moduleKebab}}');
const defaultProjectionRoot = join(cliRoot, 'templates/business-module/backend/modules/{{moduleKebab}}');
const defaultProjectionPairs = [
  {
    name: 'backend',
    sourceRoot: defaultSourceRoot,
    projectionRoot: defaultProjectionRoot,
  },
  {
    name: 'frontend-ui-source',
    sourceRoot: join(repoRoot, 'mango-business-starter/frontend/packages/{{moduleKebab}}/src'),
    projectionRoot: join(cliRoot, 'templates/business-module/frontend/packages/{{moduleKebab}}/src'),
  },
  {
    name: 'frontend-api-source',
    sourceRoot: join(repoRoot, 'mango-business-starter/frontend/packages/{{moduleKebab}}-api/src'),
    projectionRoot: join(cliRoot, 'templates/business-module/frontend/packages/{{moduleKebab}}-api/src'),
  },
];

export function describeProjection(root) {
  const resolvedRoot = resolve(root);
  if (!existsSync(resolvedRoot) || !lstatSync(resolvedRoot).isDirectory()) {
    throw new Error(`template projection root is missing: ${resolvedRoot}`);
  }
  return walkFiles(resolvedRoot)
    .map((file) => {
      const content = readFileSync(file);
      const stat = lstatSync(file);
      return {
        path: toPosix(relative(resolvedRoot, file)),
        size: content.length,
        sha256: sha256(content),
        mode: stat.mode & 0o111 ? '0755' : '0644',
      };
    })
    .sort((left, right) => compareText(left.path, right.path));
}

export function compareProjectionTrees(sourceRoot, projectionRoot) {
  const sourceFiles = describeProjection(sourceRoot);
  const projectionFiles = describeProjection(projectionRoot);
  const sourceByPath = new Map(sourceFiles.map((file) => [file.path, file]));
  const projectionByPath = new Map(projectionFiles.map((file) => [file.path, file]));
  const missing = sourceFiles.filter((file) => !projectionByPath.has(file.path)).map((file) => file.path);
  const extra = projectionFiles.filter((file) => !sourceByPath.has(file.path)).map((file) => file.path);
  const changed = sourceFiles
    .filter((sourceFile) => {
      const projectedFile = projectionByPath.get(sourceFile.path);
      return (
        projectedFile &&
        (sourceFile.sha256 !== projectedFile.sha256 ||
          sourceFile.size !== projectedFile.size ||
          sourceFile.mode !== projectedFile.mode)
      );
    })
    .map((file) => file.path);
  const sourceSha256 = projectionSha256(sourceFiles);
  const projectionSha256Value = projectionSha256(projectionFiles);
  return {
    equal: missing.length === 0 && extra.length === 0 && changed.length === 0 && sourceSha256 === projectionSha256Value,
    sourceRoot: resolve(sourceRoot),
    projectionRoot: resolve(projectionRoot),
    sourceSha256,
    projectionSha256: projectionSha256Value,
    fileCount: sourceFiles.length,
    projectionFileCount: projectionFiles.length,
    missing,
    extra,
    changed,
  };
}

export function projectionSha256(files) {
  return sha256(Buffer.from(JSON.stringify(files), 'utf8'));
}

function walkFiles(root) {
  const files = [];
  const entries = readdirSync(root, { withFileTypes: true }).sort((left, right) => compareText(left.name, right.name));
  for (const entry of entries) {
    const path = join(root, entry.name);
    if (entry.isSymbolicLink()) {
      throw new Error(`template projection must not contain symbolic links: ${path}`);
    }
    if (entry.isDirectory()) {
      files.push(...walkFiles(path));
      continue;
    }
    if (!entry.isFile()) {
      throw new Error(`template projection contains an unsupported entry: ${path}`);
    }
    files.push(path);
  }
  return files;
}

function parseArgs(argv) {
  const result = {
    sourceRoot: defaultSourceRoot,
    projectionRoot: defaultProjectionRoot,
    json: false,
    customRoots: false,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--json') {
      result.json = true;
      continue;
    }
    for (const [option, field] of [
      ['--source', 'sourceRoot'],
      ['--projection', 'projectionRoot'],
    ]) {
      if (arg === option) {
        const value = argv[index + 1];
        if (!value || value.startsWith('--')) {
          throw new Error(`missing value for ${option}`);
        }
        result[field] = resolve(value);
        result.customRoots = true;
        index += 1;
        break;
      }
      if (arg.startsWith(`${option}=`)) {
        const value = arg.slice(option.length + 1);
        if (!value) {
          throw new Error(`missing value for ${option}`);
        }
        result[field] = resolve(value);
        result.customRoots = true;
        break;
      }
    }
    if (
      arg.startsWith('--') &&
      arg !== '--source' &&
      arg !== '--projection' &&
      !arg.startsWith('--source=') &&
      !arg.startsWith('--projection=')
    ) {
      throw new Error(`unknown option: ${arg}`);
    }
  }
  return result;
}

function printComparisons(comparisons, json) {
  if (json) {
    process.stdout.write(`${JSON.stringify(comparisons, null, 2)}\n`);
    return;
  }
  for (const comparison of comparisons) {
    if (comparison.equal) {
      process.stdout.write(
        `Business module template ${comparison.name} projection PASS: ${comparison.fileCount} files, ` +
          `sha256=${comparison.sourceSha256}.\n`,
      );
      continue;
    }
    process.stderr.write(`Business module template ${comparison.name} projection FAIL:\n`);
    process.stderr.write(`- source sha256: ${comparison.sourceSha256}\n`);
    process.stderr.write(`- projection sha256: ${comparison.projectionSha256}\n`);
    process.stderr.write(`- missing: ${comparison.missing.join(', ') || '-'}\n`);
    process.stderr.write(`- extra: ${comparison.extra.join(', ') || '-'}\n`);
    process.stderr.write(`- changed: ${comparison.changed.join(', ') || '-'}\n`);
  }
}

function sha256(content) {
  return createHash('sha256').update(content).digest('hex');
}

function compareText(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}

function toPosix(path) {
  return path.split('\\').join('/');
}

if (process.argv[1] && resolve(process.argv[1]) === currentFile) {
  try {
    const args = parseArgs(process.argv.slice(2));
    const pairs = args.customRoots
      ? [{ name: 'custom', sourceRoot: args.sourceRoot, projectionRoot: args.projectionRoot }]
      : defaultProjectionPairs;
    const comparisons = pairs.map((pair) => ({
      name: pair.name,
      ...compareProjectionTrees(pair.sourceRoot, pair.projectionRoot),
    }));
    printComparisons(comparisons, args.json);
    process.exitCode = comparisons.every((comparison) => comparison.equal) ? 0 : 1;
  } catch (error) {
    process.stderr.write(`Business module template projection FAIL: ${error.message}\n`);
    process.exitCode = 1;
  }
}
