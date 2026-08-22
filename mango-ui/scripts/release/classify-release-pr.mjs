#!/usr/bin/env node
import { appendFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { assertCliReadmeProjection, CLI_README_PATH } from './release-cli-readme-lib.mjs';
import {
  assertCliFullFrontendTemplateProjection,
  CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH,
} from './release-cli-template-lib.mjs';
import { assertPmoPluginProjection, PMO_PLUGIN_MANIFEST_PATH } from './release-pmo-plugin-lib.mjs';
import { readGitFile } from './release-repository-lib.mjs';

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const releasePlanPath = 'mango-ui/.changeset/release-plan.json';

export function classifyReleasePullRequest(files) {
  const normalized = [...new Set(files)].sort();
  const hasPlan = normalized.includes('mango-ui/.changeset/release-plan.json');
  const disallowed = normalized.filter((file) => !isReleaseOnlyFile(file));
  return {
    releaseOnly: hasPlan && disallowed.length === 0,
    hasPlan,
    files: normalized,
    disallowed,
    reason: !hasPlan
      ? 'release plan is not part of the pull request'
      : disallowed.length > 0
        ? `source or governance files require normal gates: ${disallowed.join(', ')}`
        : 'only machine-generated release projection files changed',
  };
}

export function isReleaseOnlyFile(file) {
  return (
    file === 'CHANGELOG.md' ||
    file === 'mango-pmo/CHANGELOG.md' ||
    file === PMO_PLUGIN_MANIFEST_PATH ||
    file === 'mango-ui/pnpm-lock.yaml' ||
    file === 'mango-ui/.changeset/release-plan.json' ||
    file === 'mango-ui/.changeset/release-notes.txt' ||
    (/^mango-ui\/\.changeset\/[a-z0-9-]+\.md$/u.test(file) &&
      file !== 'mango-ui/.changeset/release-notes-template.md') ||
    /^mango-ui\/packages\/[^/]+\/(?:package\.json|CHANGELOG\.md)$/u.test(file) ||
    file === 'mango-ui/packages/mango-cli/release-versions.json' ||
    file === 'mango-ui/packages/mango-cli/README.md' ||
    /^mango-ui\/packages\/mango-cli\/templates\/.+\/package\.json\.template$/u.test(file) ||
    /^mango-business-starter\/.+\/package\.json$/u.test(file)
  );
}

export function assertReleaseOnlyContent(headRef = 'HEAD') {
  const plan = JSON.parse(readGitFile(repoRoot, headRef, releasePlanPath));
  const pmo = plan.packages?.find((entry) => entry.name === '@mango/pmo');
  if (pmo) {
    assertPmoPluginProjection({
      sourceContent: readGitFile(repoRoot, plan.source?.commit, PMO_PLUGIN_MANIFEST_PATH),
      projectedContent: readGitFile(repoRoot, headRef, PMO_PLUGIN_MANIFEST_PATH),
      sourceVersion: pmo.sourceVersion,
      targetVersion: pmo.targetVersion,
    });
  }
  const cli = plan.packages?.find((entry) => entry.name === '@mango/cli');
  if (!cli) return;
  assertCliReadmeProjection({
    sourceContent: readGitFile(repoRoot, plan.source?.commit, CLI_README_PATH),
    projectedContent: readGitFile(repoRoot, headRef, CLI_README_PATH),
    sourceVersion: cli.sourceVersion,
    targetVersion: cli.targetVersion,
  });
  assertCliFullFrontendTemplateProjection({
    sourceContent: readGitFile(repoRoot, plan.source?.commit, CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH),
    projectedContent: readGitFile(repoRoot, headRef, CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH),
    sourceVersion: cli.sourceVersion,
    targetVersion: cli.targetVersion,
  });
}

if (process.argv[1] && import.meta.url === new URL(`file://${process.argv[1]}`).href) {
  const base = argument('--base');
  const head = argument('--head') || 'HEAD';
  if (!base) throw new Error('classify-release-pr requires --base');
  const result = spawnSync('git', ['diff', '--name-only', `${base}..${head}`], { encoding: 'utf8' });
  if (result.status !== 0) throw new Error(`cannot classify release PR: ${result.stderr}`);
  const classification = classifyReleasePullRequest(result.stdout.split(/\r?\n/u).filter(Boolean));
  if (classification.releaseOnly) assertReleaseOnlyContent(head);
  const output = process.env.GITHUB_OUTPUT;
  if (output) {
    appendFileSync(output, `release_only=${classification.releaseOnly}\n`);
    appendFileSync(output, `reason=${classification.reason}\n`);
  }
  console.log(JSON.stringify(classification, null, 2));
}

function argument(name) {
  const inline = process.argv.find((value) => value.startsWith(`${name}=`));
  if (inline) return inline.slice(name.length + 1);
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] || '' : '';
}
