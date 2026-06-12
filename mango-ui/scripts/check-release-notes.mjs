#!/usr/bin/env node
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const workspaceRoot = resolve(scriptDir, '..');
const repoRoot = resolve(workspaceRoot, '..');
const changelogPath = join(repoRoot, 'CHANGELOG.md');

const args = process.argv.slice(2);
const packageName = readArg('--package');
const version = readArg('--version');
const releaseTag = readArg('--tag');
const checkGithubRelease = args.includes('--check-github-release');

if (args.includes('--help') || args.includes('-h')) {
  usage();
  process.exit(0);
}

const errors = [];

if (!packageName) {
  errors.push('--package is required.');
}
if (!version) {
  errors.push('--version is required.');
}
if (!existsSync(changelogPath)) {
  errors.push('Root CHANGELOG.md is missing.');
} else {
  const changelog = readFileSync(changelogPath, 'utf8');
  checkChangelog(latestReleaseSection(changelog));
}

if (checkGithubRelease) {
  checkRelease();
}

if (errors.length > 0) {
  console.error(`Release notes check failed:\n${errors.map((error) => `- ${error}`).join('\n')}`);
  process.exit(1);
}

console.log(`Release notes cover ${packageName}@${version}.`);

function checkChangelog(changelog) {
  if (!changelog) {
    errors.push('Root CHANGELOG.md must contain at least one release section starting with "## ".');
    return;
  }
  if (!changelog.includes(packageName)) {
    errors.push(`Latest root CHANGELOG.md release section does not mention ${packageName}.`);
  }
  if (!changelog.includes(version)) {
    errors.push(`Latest root CHANGELOG.md release section does not mention version ${version}.`);
  }
  for (const requiredHeading of ['Published Packages', 'Upgrade Notes', 'Verification']) {
    if (!changelog.includes(`### ${requiredHeading}`)) {
      errors.push(`Latest root CHANGELOG.md release section must contain "### ${requiredHeading}".`);
    }
  }
}

function latestReleaseSection(changelog) {
  const match = changelog.match(/^##\s+.+$(?:\n(?!##\s).*)*/m);
  return match?.[0] || '';
}

function checkRelease() {
  if (!releaseTag) {
    errors.push('--check-github-release requires --tag=<tag>.');
    return;
  }
  const result = spawnSync('gh', ['release', 'view', releaseTag, '--json', 'tagName,body'], {
    encoding: 'utf8',
    stdio: 'pipe',
  });
  if (result.status !== 0) {
    errors.push(`GitHub Release ${releaseTag} does not exist.`);
    return;
  }
  const release = JSON.parse(result.stdout);
  const body = release.body || '';
  if (!body.includes(packageName) || !body.includes(version)) {
    errors.push(`GitHub Release ${releaseTag} does not mention ${packageName}@${version}.`);
  }
  for (const requiredHeading of ['Published Packages', 'Upgrade Notes', 'Verification']) {
    if (!body.includes(requiredHeading)) {
      errors.push(`GitHub Release ${releaseTag} must contain ${requiredHeading}.`);
    }
  }
}

function readArg(name) {
  const prefix = `${name}=`;
  return args.find((arg) => arg.startsWith(prefix))?.slice(prefix.length) || '';
}

function usage() {
  console.log(`Usage: node scripts/check-release-notes.mjs --package=<name> --version=<version> [--tag=<tag> --check-github-release]

Checks that the platform CHANGELOG.md and optional GitHub Release contain package version, upgrade notes and verification notes.`);
}
