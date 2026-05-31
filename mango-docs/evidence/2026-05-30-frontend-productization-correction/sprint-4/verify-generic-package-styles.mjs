import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { tmpdir } from 'node:os';

const repoRoot = process.cwd();
const scriptPath = join(repoRoot, 'mango-ui/scripts/generate-package-styles.mjs');
const evidenceDir = join(repoRoot, 'mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-4');
const reportPath = join(evidenceDir, 'generic-package-style-generator-report.json');
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-package-styles-'));

const errors = [];
const details = [];

function check(condition, message, detail) {
  details.push({ ok: Boolean(condition), message, detail });
  if (!condition) {
    errors.push(message);
  }
}

function writeJson(path, value) {
  writeFileSync(path, JSON.stringify(value, null, 2));
}

function runNode(args, cwd) {
  return execFileSync(process.execPath, args, {
    cwd,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  });
}

try {
  const admin2Dir = join(tempRoot, 'admin2');
  const packageDir = join(admin2Dir, 'node_modules/@company/order-admin');
  mkdirSync(packageDir, { recursive: true });

  writeJson(join(admin2Dir, 'package.json'), {
    name: '@company/admin2',
    version: '1.0.0',
    type: 'module',
    dependencies: {
      '@company/order-admin': '1.2.3',
    },
  });
  writeJson(join(admin2Dir, 'admin-packages.json'), {
    schemaVersion: 1,
    packages: [
      {
        name: '@company/order-admin',
        style: '@company/order-admin/style.css',
      },
    ],
  });
  writeJson(join(packageDir, 'package.json'), {
    name: '@company/order-admin',
    version: '1.2.3',
    type: 'module',
    exports: {
      '.': './dist/index.js',
      './style.css': './dist/style.css',
    },
  });
  mkdirSync(join(packageDir, 'dist'), { recursive: true });
  writeFileSync(join(packageDir, 'dist/style.css'), '.order-admin-page { display: block; }\n');

  const outputPath = join(admin2Dir, 'generated-package-styles.css');
  const generateOutput = runNode(
    [
      scriptPath,
      '--manifest',
      './admin-packages.json',
      '--package',
      './package.json',
      '--out',
      './generated-package-styles.css',
    ],
    admin2Dir,
  );
  const checkOutput = runNode(
    [
      scriptPath,
      '--manifest',
      './admin-packages.json',
      '--package',
      './package.json',
      '--out',
      './generated-package-styles.css',
      '--check',
    ],
    admin2Dir,
  );
  const generatedContent = readFileSync(outputPath, 'utf8');

  check(existsSync(outputPath), 'generic generator writes style aggregate for admin2-like consumer');
  check(
    generatedContent.includes("@import '@company/order-admin/style.css'"),
    'generic generator imports published package public style export',
    { generatedContent },
  );
  check(checkOutput.includes('checked 1 package style exports'), 'generic generator validates published package style export', {
    checkOutput,
    generateOutput,
  });

  const missingDependencyDir = join(tempRoot, 'missing-dependency-admin');
  mkdirSync(join(missingDependencyDir, 'node_modules/@company/order-admin'), { recursive: true });
  writeJson(join(missingDependencyDir, 'package.json'), {
    name: '@company/missing-dependency-admin',
    version: '1.0.0',
    type: 'module',
    dependencies: {},
  });
  writeJson(join(missingDependencyDir, 'admin-packages.json'), {
    schemaVersion: 1,
    packages: [
      {
        name: '@company/order-admin',
        style: '@company/order-admin/style.css',
      },
    ],
  });

  let missingDependencyFailed = false;
  try {
    runNode(
      [
        scriptPath,
        '--manifest',
        './admin-packages.json',
        '--package',
        './package.json',
        '--out',
        './generated-package-styles.css',
        '--check',
      ],
      missingDependencyDir,
    );
  } catch (error) {
    missingDependencyFailed = error.stderr.includes('missing dependency declaration');
  }
  check(missingDependencyFailed, 'generic generator rejects package not declared by consumer package.json');
} finally {
  rmSync(tempRoot, { recursive: true, force: true });
}

const report = {
  ok: errors.length === 0,
  checkedAt: new Date().toISOString(),
  scriptPath: resolve(scriptPath),
  errors,
  details,
};

writeFileSync(reportPath, JSON.stringify(report, null, 2));

if (errors.length > 0) {
  console.error(`Generic package style generator contract failed. Report: ${reportPath}`);
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

console.log(`Generic package style generator contract passed. Report: ${reportPath}`);
