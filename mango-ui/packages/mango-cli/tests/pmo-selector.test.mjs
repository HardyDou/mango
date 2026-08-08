import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const cli = fileURLToPath(new URL('../src/index.mjs', import.meta.url));

function run(args, cwd) {
  return spawnSync(process.execPath, [cli, ...args], { cwd, encoding: 'utf8', timeout: 10000 });
}

test('CLI 帮助列出 pmo 中文批量选择入口', () => {
  const result = run(['--help'], process.cwd());
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /mango pmo 选择 \[--项目目录 <目录>\]/u);
  const selectorHelp = run(['pmo', '选择', '--帮助'], process.cwd());
  assert.equal(selectorHelp.status, 0, selectorHelp.stderr);
  assert.match(selectorHelp.stdout, /--等级 <L0-L5>/u);
});

test('pmo select 可直接确认推荐并输出稳定机器结果', () => {
  const project = mkdtempSync(join(tmpdir(), 'mango-pmo-selector-'));
  try {
    const result = run(
      [
        'pmo',
        '选择',
        '--项目目录',
        project,
        '--等级',
        'L3',
        '--措施',
        'M01=REUSE,M03=ENABLE,M04=ENABLE,M09=ENABLE',
        '--采用推荐',
        '--机器结果',
      ],
      project,
    );
    assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
    const parsed = JSON.parse(result.stdout);
    assert.equal(parsed.documentVersion.label, '标准版');
    assert.deepEqual(
      parsed.selectedMeasures.map((item) => item.id),
      ['M01', 'M03', 'M04', 'M09'],
    );
    assert.equal(parsed.machineSelections.M01, 'REUSE');
    assert.equal(parsed.machineSelections.M16, 'DISABLE');
  } finally {
    rmSync(project, { recursive: true, force: true });
  }
});

test('pmo select 非 TTY 参数一次提交文档版本和措施', () => {
  const project = mkdtempSync(join(tmpdir(), 'mango-pmo-selector-'));
  try {
    const result = run(
      [
        'pmo',
        'select',
        `--project-dir=${project}`,
        '--level',
        'L2',
        '--measures',
        'M01=CREATE,M03=ENABLE',
        '--answer',
        '5;1,3,4,5,6,9,14',
        '--json',
      ],
      project,
    );
    assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
    const parsed = JSON.parse(result.stdout);
    assert.equal(parsed.documentVersion.label, '四文档');
    assert.deepEqual(
      parsed.selectedMeasures.map((item) => item.id),
      ['M01', 'M03', 'M04', 'M05', 'M06', 'M09', 'M14'],
    );
  } finally {
    rmSync(project, { recursive: true, force: true });
  }
});

test('pmo select 固定 L5 时拒绝批量降级', () => {
  const project = mkdtempSync(join(tmpdir(), 'mango-pmo-selector-'));
  try {
    const result = run(
      ['pmo', 'select', '--project-dir', project, '--level', 'L5', '--fixed-level', 'L5', '--answer', '3;1,3,4'],
      project,
    );
    assert.equal(result.status, 1);
    assert.match(result.stderr, /不可降级/u);
  } finally {
    rmSync(project, { recursive: true, force: true });
  }
});

test('pmo 选择在任务事实不足时拒绝猜测默认文档版本', () => {
  const result = run(['pmo', '选择', '--采用推荐'], process.cwd());
  assert.equal(result.status, 1);
  assert.match(result.stderr, /请先让交付保障 Skill 明确目标、边界和文档版本/u);
});
