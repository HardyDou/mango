import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const workflow = fs.readFileSync(path.join(repositoryRoot, '.github/workflows/frontend-quality.yml'), 'utf8');

function job(id) {
  const match = workflow.match(new RegExp(`\\n  ${id}:\\n[\\s\\S]*?(?=\\n  [a-z][a-z0-9-]*:\\n|$)`, 'u'));
  assert.ok(match, `missing workflow job ${id}`);
  return match[0];
}

test('pull requests do not run a blocking frontend quality job', () => {
  assert.doesNotMatch(workflow, /frontend-pr-quality/u);
  assert.doesNotMatch(workflow, /^  pull_request:/mu);
});

test('deep frontend quality and P0 browser acceptance do not run for pull requests', () => {
  assert.match(job('frontend-quality'), /if: github\.event_name != 'pull_request'/u);
  assert.match(job('frontend-e2e-p0'), /if: github\.event_name != 'pull_request'/u);
});

test('P0 browser acceptance installs backend Maven prerequisites before startup', () => {
  const e2eJob = job('frontend-e2e-p0');
  const prerequisites = e2eJob.indexOf('- name: Install Mango backend prerequisites');
  const startup = e2eJob.indexOf('- name: Start real Mango backend');
  assert.ok(prerequisites >= 0, 'missing backend prerequisite install step');
  assert.ok(startup > prerequisites, 'backend startup must follow prerequisite installation');
  assert.match(e2eJob, /-pl :mango-bom,:mango-parent,:mango-common,:mango-tools[\s\S]*?-DskipTests[\s\S]*?install/u);
});
