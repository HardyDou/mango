#!/usr/bin/env node
/**
 * 检查同一迁移模块内是否重复创建同名表。
 *
 * 背景：`notice` 模块的 `V2` 里重复写了一遍 `V1` 已创建的 `notice_channel_route_tag`，
 * 因为 `CREATE TABLE IF NOT EXISTS` 不报错，只有 MySQL 返回 "already exists" 的 WARN，
 * 每次全新安装都会刷一条噪音，且 Flyway checksum 约束让历史迁移不能被修改。
 *
 * 规则：
 * - 扫描 `*\/src/main/resources/db/migration/<模块>/*.sql`，按模块分组；
 * - 同一模块内，同一张表只能在**一个**迁移文件里被 CREATE；
 * - 已存在的历史重复登记在 `mango-pmo/baselines/migration-create-table-duplicates.txt`，
 *   格式 `<模块>:<表名>`，每行可跟 ` -- 说明`；
 * - 新增重复一律失败。
 */
import { readdirSync, readFileSync, existsSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const DEFAULT_REPO_ROOT = resolve(SCRIPT_DIR, '..', '..');

function readArgument(name) {
  const index = process.argv.indexOf(name);
  return index >= 0 && index + 1 < process.argv.length ? process.argv[index + 1] : null;
}

const REPO_ROOT = readArgument('--root') ? resolve(readArgument('--root')) : DEFAULT_REPO_ROOT;
const BASELINE_FILE = readArgument('--baseline')
  ? resolve(readArgument('--baseline'))
  : join(REPO_ROOT, 'mango-pmo', 'baselines', 'migration-create-table-duplicates.txt');
const MIGRATION_DIR_SEGMENT = join('src', 'main', 'resources', 'db', 'migration');
const SKIP_DIRECTORIES = new Set(['node_modules', '.git', 'target', 'dist', 'build', '.runtime']);
const CREATE_TABLE_PATTERN = /create\s+table\s+(?:if\s+not\s+exists\s+)?`?([a-z0-9_$]+)`?/gi;
const SQL_LINE_COMMENT_PATTERN = /--[^\n]*/g;

function collectMigrationDirectories(directory, found = []) {
  let entries;
  try {
    entries = readdirSync(directory, { withFileTypes: true });
  } catch {
    return found;
  }
  for (const entry of entries) {
    if (!entry.isDirectory()) {
      continue;
    }
    if (SKIP_DIRECTORIES.has(entry.name)) {
      continue;
    }
    const child = join(directory, entry.name);
    if (child.endsWith(MIGRATION_DIR_SEGMENT)) {
      found.push(child);
    }
    collectMigrationDirectories(child, found);
  }
  return found;
}

function readBaseline() {
  const allowed = new Map();
  if (!existsSync(BASELINE_FILE)) {
    return allowed;
  }
  for (const rawLine of readFileSync(BASELINE_FILE, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim();
    if (line === '' || line.startsWith('#')) {
      continue;
    }
    const [entry, reason] = line.split('--');
    const key = entry.trim();
    if (key === '') {
      continue;
    }
    allowed.set(key.toLowerCase(), (reason || '').trim());
  }
  return allowed;
}

function collectDuplicates() {
  const duplicates = new Map();
  let groupCount = 0;
  for (const migrationRoot of collectMigrationDirectories(REPO_ROOT)) {
    for (const group of readdirSync(migrationRoot, { withFileTypes: true })) {
      if (!group.isDirectory()) {
        continue;
      }
      groupCount += 1;
      const groupDirectory = join(migrationRoot, group.name);
      const createdIn = new Map();
      for (const file of readdirSync(groupDirectory).sort()) {
        if (!file.toLowerCase().endsWith('.sql')) {
          continue;
        }
        const content = readFileSync(join(groupDirectory, file), 'utf8').replace(SQL_LINE_COMMENT_PATTERN, '');
        for (const match of content.matchAll(CREATE_TABLE_PATTERN)) {
          const table = match[1].toLowerCase();
          createdIn.set(table, [...(createdIn.get(table) || []), file]);
        }
      }
      for (const [table, files] of createdIn) {
        if (files.length > 1) {
          duplicates.set(`${group.name}:${table}`, { group: group.name, table, files });
        }
      }
    }
  }
  return { duplicates, groupCount };
}

const allowed = readBaseline();
const { duplicates, groupCount } = collectDuplicates();
const failures = [];

for (const [key, duplicate] of duplicates) {
  if (allowed.has(key)) {
    continue;
  }
  failures.push(
    `${duplicate.group}/${duplicate.table} 在多个迁移文件中被创建: ${duplicate.files.join(', ')}`,
  );
}

const allowedButNotDuplicate = [...allowed.keys()].filter((key) => !duplicates.has(key));

if (failures.length > 0) {
  console.error('Migration duplicate CREATE TABLE check failed:');
  for (const failure of failures) {
    console.error(`  - ${failure}`);
  }
  console.error(
    '  历史迁移受 Flyway checksum 约束不能直接修改：请在新迁移里前向处理，' +
      '或在 mango-pmo/baselines/migration-create-table-duplicates.txt 登记并写明原因。',
  );
  process.exit(1);
}

for (const key of allowedButNotDuplicate) {
  console.warn(`Migration duplicate baseline entry no longer applies: ${key}（可以删除该登记）`);
}

console.log(
  `Migration duplicate CREATE TABLE check passed: groups=${groupCount}, duplicates=${duplicates.size}, allowlisted=${allowed.size}`,
);
