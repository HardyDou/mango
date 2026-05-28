#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const pmoRoot = path.resolve(__dirname, '..');
const indexPath = path.join(pmoRoot, 'rules', 'index.json');

function parseArgs(argv) {
  const args = {
    role: '',
    phase: '',
    task: '',
    paths: '',
    json: false
  };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '--json') {
      args.json = true;
    } else if (arg.startsWith('--')) {
      const key = arg.slice(2);
      args[key] = argv[i + 1] ?? '';
      i += 1;
    }
  }
  return args;
}

function readIndex() {
  if (!fs.existsSync(indexPath)) {
    throw new Error(`PMO index not found: ${indexPath}`);
  }
  return JSON.parse(fs.readFileSync(indexPath, 'utf8'));
}

function normalizeText(value) {
  return String(value || '').toLowerCase();
}

function splitPaths(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function pathMatches(inputPath, pattern) {
  const normalizedPath = inputPath.replaceAll('\\', '/');
  const normalizedPattern = pattern.replaceAll('\\', '/');
  if (normalizedPattern.endsWith('/**')) {
    const prefix = normalizedPattern.slice(0, -3);
    return normalizedPath === prefix || normalizedPath.startsWith(`${prefix}/`);
  }
  if (normalizedPattern.includes('*') || normalizedPattern.includes('?')) {
    const regex = globToRegExp(normalizedPattern);
    return regex.test(normalizedPath);
  }
  return normalizedPath === normalizedPattern || normalizedPath.startsWith(`${normalizedPattern}/`);
}

function globToRegExp(pattern) {
  let source = '^';
  for (let i = 0; i < pattern.length; i += 1) {
    const char = pattern[i];
    const next = pattern[i + 1];
    if (char === '*' && next === '*') {
      const after = pattern[i + 2];
      if (after === '/') {
        source += '(?:.*/)?';
        i += 2;
      } else {
        source += '.*';
        i += 1;
      }
    } else if (char === '*') {
      source += '[^/]*';
    } else if (char === '?') {
      source += '[^/]';
    } else if ('\\^$+?.()|{}[]'.includes(char)) {
      source += `\\${char}`;
    } else {
      source += char;
    }
  }
  source += '$';
  return new RegExp(source);
}

function bundleMatches(bundle, args) {
  const task = normalizeText(args.task);
  const inputPaths = splitPaths(args.paths);
  if (Array.isArray(bundle.roles) && bundle.roles.length > 0 && !bundle.roles.includes(args.role)) {
    return false;
  }
  if (Array.isArray(bundle.phases) && bundle.phases.length > 0 && !bundle.phases.includes(args.phase)) {
    return false;
  }
  const keywordHit = (bundle.keywords || []).some((keyword) => task.includes(normalizeText(keyword)));
  const pathHit = inputPaths.some((inputPath) => (bundle.paths || []).some((pattern) => pathMatches(inputPath, pattern)));
  return keywordHit || pathHit;
}

function addRule(result, index, key, source) {
  const rule = index.rules[key];
  if (!rule) {
    result.errors.push(`Unknown PMO rule key "${key}" from ${source}`);
    return;
  }
  if (!result.seen.has(rule.path)) {
    result.seen.add(rule.path);
    result.mustRead.push({
      key,
      path: rule.path,
      reason: rule.reason || source
    });
  }
}

function buildResult(index, args) {
  const result = {
    role: args.role || 'auto',
    phase: args.phase || 'auto',
    task: args.task || '',
    paths: splitPaths(args.paths),
    mustRead: [],
    errors: [],
    seen: new Set()
  };

  for (const entry of index.always || []) {
    if (!result.seen.has(entry.path)) {
      result.seen.add(entry.path);
      result.mustRead.push({
        key: 'always',
        path: entry.path,
        reason: entry.reason || '全局必读'
      });
    }
  }

  for (const key of index.roles?.[args.role] || []) {
    addRule(result, index, key, `role:${args.role}`);
  }

  for (const key of index.phases?.[args.phase] || []) {
    addRule(result, index, key, `phase:${args.phase}`);
  }

  for (const [bundleName, bundle] of Object.entries(index.bundles || {})) {
    if (bundleMatches(bundle, args)) {
      for (const key of bundle.include || []) {
        addRule(result, index, key, `bundle:${bundleName}`);
      }
    }
  }

  for (const item of result.mustRead) {
    const filePath = path.join(pmoRoot, item.path);
    if (!fs.existsSync(filePath)) {
      result.errors.push(`Missing PMO file: ${item.path}`);
    }
  }

  delete result.seen;
  return result;
}

function printText(result) {
  console.log('PMO Preflight');
  console.log(`Role: ${result.role}`);
  console.log(`Phase: ${result.phase}`);
  if (result.task) {
    console.log(`Task: ${result.task}`);
  }
  if (result.paths.length > 0) {
    console.log(`Paths: ${result.paths.join(', ')}`);
  }
  console.log('');
  console.log('Must read:');
  result.mustRead.forEach((item, index) => {
    console.log(`${index + 1}. ${item.path} - ${item.reason}`);
  });
  if (result.errors.length > 0) {
    console.log('');
    console.log('Errors:');
    result.errors.forEach((error) => console.log(`- ${error}`));
  }
}

try {
  const args = parseArgs(process.argv.slice(2));
  const index = readIndex();
  const result = buildResult(index, args);
  if (args.json) {
    console.log(JSON.stringify(result, null, 2));
  } else {
    printText(result);
  }
  process.exit(result.errors.length > 0 ? 1 : 0);
} catch (error) {
  console.error(`PMO preflight failed: ${error.message}`);
  process.exit(1);
}
