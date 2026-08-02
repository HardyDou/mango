#!/usr/bin/env node
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { spawnSync } from 'node:child_process';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const workspaceRoot = resolve(scriptDir, '..');
const repoRoot = resolve(workspaceRoot, '..');
const rootChangelogPath = join(repoRoot, 'CHANGELOG.md');
const cliRoot = join(workspaceRoot, 'packages/mango-cli');
const pmoRoot = join(workspaceRoot, 'packages/mango-pmo');
const pmoSourceChangelogPath = join(repoRoot, 'mango-pmo/CHANGELOG.md');
const pmoPackageJsonPath = join(pmoRoot, 'package.json');
const cliPackageJsonPath = join(cliRoot, 'package.json');
const cliReleaseVersionsPath = join(cliRoot, 'release-versions.json');
const generatedWorkflowPaths = [
  'mango-ui/packages/mango-cli/templates/full/.github/workflows/pmo-doc-check.yml',
  'mango-ui/packages/mango-cli/templates/full/.gitea/workflows/pmo-doc-check.yml',
];

export function runReleaseNotesCheck(argv = process.argv.slice(2)) {
  const args = parseArgs(argv);
  if (args.help) {
    usage();
    return 0;
  }

  const errors = [];
  if (!args.packageName) errors.push('--package is required.');
  if (!args.version) errors.push('--version is required.');

  let requiredPmoChecks = [];
  if (args.packageName === '@mango/cli' && args.version) {
    requiredPmoChecks = checkCliPmoReleaseNotes(args.version, errors);
  } else if (args.packageName === '@mango/pmo' && args.version) {
    requiredPmoChecks = checkPmoReleaseNotes(args.version, errors);
  }

  if (!existsSync(rootChangelogPath)) {
    errors.push('Root CHANGELOG.md is missing.');
  } else {
    const rootChangelog = readFileSync(rootChangelogPath, 'utf8');
    errors.push(
      ...validateRootReleaseNotes(rootChangelog, {
        packageName: args.packageName,
        version: args.version,
        releaseTag: args.releaseTag,
        requiredPmoChecks,
      }),
    );
  }

  if (args.checkGithubRelease) {
    checkRelease(args, requiredPmoChecks, errors);
  }

  if (errors.length > 0) {
    console.error(`Release notes check failed:\n${errors.map((error) => `- ${error}`).join('\n')}`);
    return 1;
  }

  console.log(`Release notes cover ${args.packageName}@${args.version}.`);
  return 0;
}

function parseArgs(argv) {
  const readArg = (name) => {
    const prefix = `${name}=`;
    return argv.find((arg) => arg.startsWith(prefix))?.slice(prefix.length) || '';
  };
  return {
    packageName: readArg('--package'),
    version: readArg('--version'),
    releaseTag: readArg('--tag'),
    checkGithubRelease: argv.includes('--check-github-release'),
    help: argv.includes('--help') || argv.includes('-h'),
  };
}

export function validateReleaseSection(section, { label, packageName, version }) {
  const errors = [];
  if (!section) {
    errors.push(`${label} is missing.`);
    return errors;
  }
  if (packageName && !section.includes(packageName)) {
    errors.push(`${label} does not mention ${packageName}.`);
  }
  if (version && !section.includes(version)) {
    errors.push(`${label} does not mention version ${version}.`);
  }
  for (const requiredHeading of ['Published Packages', 'Upgrade Notes', 'Verification']) {
    if (!section.includes(`### ${requiredHeading}`)) {
      errors.push(`${label} must contain "### ${requiredHeading}".`);
    }
  }
  return errors;
}

export function latestReleaseSection(changelog) {
  const releaseSections = changelog.match(/^##\s+.+$(?:\n(?!##\s).*)*/gm) || [];
  return releaseSections.find((section) => !/^##\s+Unreleased\s*$/m.test(section)) || '';
}

export function releaseSectionForVersion(changelog, version) {
  const escapedVersion = escapeRegExp(version);
  const versionPattern = new RegExp(`(^|[^0-9.])${escapedVersion}(?![0-9.])`, 'u');
  const releaseSections = changelog.match(/^##\s+.+$(?:\n(?!##\s).*)*/gm) || [];
  return releaseSections.find((section) => versionPattern.test(section.split('\n', 1)[0])) || '';
}

export function releaseSectionForTag(changelog, releaseTag) {
  if (!releaseTag) return '';
  const releaseSections = changelog.match(/^##\s+.+$(?:\n(?!##\s).*)*/gm) || [];
  return (
    releaseSections.find((section) => {
      const heading = section.split('\n', 1)[0];
      return new RegExp(`(?:^|\\s)${escapeRegExp(releaseTag)}(?:\\s|$)`, 'u').test(heading);
    }) || ''
  );
}

export function validateRootReleaseNotes(changelog, { packageName, version, releaseTag = '', requiredPmoChecks = [] }) {
  const label = `Root CHANGELOG.md ${packageName}@${version} release section`;
  const section = releaseTag
    ? releaseSectionForTag(changelog, releaseTag)
    : releaseSectionForVersion(changelog, version);
  return [
    ...validateReleaseSection(section, { label, packageName, version }),
    ...validateRequiredCheckCoverage(section, { label, checkers: requiredPmoChecks }),
  ];
}

export function extractWorkflowCheckers(content) {
  const checkers = new Set();
  const pattern = /business-pmo\/mango-baseline\/tools\/(check-[A-Za-z0-9._-]+\.mjs)/gu;
  for (const match of content.matchAll(pattern)) checkers.add(match[1]);
  return checkers;
}

export function addedWorkflowCheckers(previousContents, currentContents) {
  const previous = new Set(previousContents.flatMap((content) => [...extractWorkflowCheckers(content)]));
  const current = new Set(currentContents.flatMap((content) => [...extractWorkflowCheckers(content)]));
  return [...current].filter((checker) => !previous.has(checker)).sort(compareText);
}

export function validateRequiredCheckCoverage(section, { label, checkers }) {
  const errors = [];
  if (checkers.length === 0) return errors;
  if (!section.includes('### PMO Required Checks')) {
    errors.push(`${label} must contain "### PMO Required Checks" for newly added PMO required checks.`);
    return errors;
  }

  const lines = section.split(/\r?\n/u);
  for (const checker of checkers) {
    const itemPattern = new RegExp('^- `' + escapeRegExp(checker) + '`(?:\\s|$)', 'u');
    const start = lines.findIndex((line) => itemPattern.test(line));
    if (start < 0) {
      errors.push(`${label} does not document newly added PMO required check ${checker}.`);
      continue;
    }
    let end = lines.length;
    for (let index = start + 1; index < lines.length; index += 1) {
      if (/^(?:###\s|- `check-[^`]+\.mjs`)/u.test(lines[index])) {
        end = index;
        break;
      }
    }
    const block = lines.slice(start, end).join('\n');
    for (const field of ['Migration', 'Exception', 'Verify']) {
      if (!new RegExp(`^\\s+- ${field}:\\s+\\S`, 'mu').test(block)) {
        errors.push(`${label} ${checker} entry must contain a non-empty ${field} item.`);
      }
    }
  }
  return errors;
}

function checkCliPmoReleaseNotes(cliVersion, errors) {
  if (!existsSync(cliReleaseVersionsPath)) {
    errors.push('@mango/cli release-versions.json is missing.');
    return [];
  }
  if (!existsSync(pmoPackageJsonPath)) {
    errors.push('@mango/pmo package.json is missing.');
    return [];
  }

  const releaseVersions = readJson(cliReleaseVersionsPath, errors, '@mango/cli release-versions.json');
  const pmoPackage = readJson(pmoPackageJsonPath, errors, '@mango/pmo package.json');
  if (!releaseVersions || !pmoPackage) return [];
  const pmoVersion = releaseVersions.npm?.['@mango/pmo'];
  if (!pmoVersion) {
    errors.push('@mango/cli release-versions.json must lock @mango/pmo.');
    return [];
  }
  if (pmoPackage.version !== pmoVersion) {
    errors.push(`@mango/cli locks @mango/pmo@${pmoVersion}, but the local PMO package is ${pmoPackage.version}.`);
    return [];
  }

  const previousRelease = findPreviousCliRelease(cliVersion, errors);
  const requiredPmoChecks =
    previousRelease && previousRelease.pmoVersion !== pmoVersion
      ? collectAddedPmoRequiredChecks(previousRelease.tag, errors)
      : [];

  const cliChangelogPath = join(cliRoot, 'CHANGELOG.md');
  if (!existsSync(cliChangelogPath)) {
    errors.push('@mango/cli CHANGELOG.md is missing.');
  } else {
    const cliSection = releaseSectionForVersion(readFileSync(cliChangelogPath, 'utf8'), cliVersion);
    if (!cliSection) errors.push(`@mango/cli CHANGELOG.md is missing version ${cliVersion}.`);
    errors.push(
      ...validateRequiredCheckCoverage(cliSection, {
        label: `@mango/cli CHANGELOG.md ${cliVersion} section`,
        checkers: requiredPmoChecks,
      }),
    );
  }

  if (!existsSync(pmoSourceChangelogPath)) {
    errors.push('mango-pmo/CHANGELOG.md is missing.');
  } else {
    const pmoSection = releaseSectionForVersion(readFileSync(pmoSourceChangelogPath, 'utf8'), pmoVersion);
    if (!pmoSection) errors.push(`mango-pmo/CHANGELOG.md is missing version ${pmoVersion}.`);
    errors.push(
      ...validateRequiredCheckCoverage(pmoSection, {
        label: `mango-pmo/CHANGELOG.md ${pmoVersion} section`,
        checkers: requiredPmoChecks,
      }),
    );
  }

  return requiredPmoChecks;
}

function checkPmoReleaseNotes(pmoVersion, errors) {
  const pmoPackage = readJson(pmoPackageJsonPath, errors, '@mango/pmo package.json');
  const cliPackage = readJson(cliPackageJsonPath, errors, '@mango/cli package.json');
  if (!pmoPackage || !cliPackage) return [];
  if (pmoPackage.version !== pmoVersion) {
    errors.push(`Requested @mango/pmo@${pmoVersion}, but the local PMO package is ${pmoPackage.version}.`);
    return [];
  }
  return checkCliPmoReleaseNotes(cliPackage.version, errors);
}

function findPreviousCliRelease(currentVersion, errors) {
  const result = spawnSync('git', ['tag', '--merged', 'HEAD', '--sort=-creatordate'], {
    cwd: repoRoot,
    encoding: 'utf8',
    stdio: 'pipe',
  });
  if (result.status !== 0) {
    errors.push(`Cannot list prior CLI release tags: ${result.stderr.trim() || 'git tag failed'}.`);
    return null;
  }
  for (const tag of result.stdout.split(/\r?\n/u).filter(Boolean)) {
    const match = tag.match(/(?:^|-)cli-(\d+\.\d+\.\d+)(?:-|$)/u);
    if (!match || match[1] === currentVersion) continue;
    const previousLock = spawnSync('git', ['show', `${tag}:mango-ui/packages/mango-cli/release-versions.json`], {
      cwd: repoRoot,
      encoding: 'utf8',
      stdio: 'pipe',
    });
    if (previousLock.status !== 0) continue;
    try {
      const pmoVersion = JSON.parse(previousLock.stdout).npm?.['@mango/pmo'];
      if (pmoVersion) return { tag, pmoVersion };
    } catch {
      continue;
    }
  }
  errors.push(`Cannot find a prior CLI release with a PMO lock before ${currentVersion}.`);
  return null;
}

function collectAddedPmoRequiredChecks(previousTag, errors) {
  const previousContents = [];
  const currentContents = [];
  for (const workflowPath of generatedWorkflowPaths) {
    const previous = spawnSync('git', ['show', `${previousTag}:${workflowPath}`], {
      cwd: repoRoot,
      encoding: 'utf8',
      stdio: 'pipe',
    });
    if (previous.status !== 0) {
      errors.push(`Cannot read ${workflowPath} from prior PMO release ${previousTag}.`);
      continue;
    }
    const currentPath = join(repoRoot, workflowPath);
    if (!existsSync(currentPath)) {
      errors.push(`Current generated PMO workflow is missing: ${workflowPath}.`);
      continue;
    }
    previousContents.push(previous.stdout);
    currentContents.push(readFileSync(currentPath, 'utf8'));
  }
  if (previousContents.length !== generatedWorkflowPaths.length) return [];
  return addedWorkflowCheckers(previousContents, currentContents);
}

function checkRelease(args, requiredPmoChecks, errors) {
  if (!args.releaseTag) {
    errors.push('--check-github-release requires --tag=<tag>.');
    return;
  }
  const result = spawnSync('gh', ['release', 'view', args.releaseTag, '--json', 'tagName,body'], {
    encoding: 'utf8',
    stdio: 'pipe',
  });
  if (result.status !== 0) {
    errors.push(`GitHub Release ${args.releaseTag} does not exist.`);
    return;
  }
  const release = JSON.parse(result.stdout);
  const body = release.body || '';
  if (!body.includes(args.packageName) || !body.includes(args.version)) {
    errors.push(`GitHub Release ${args.releaseTag} does not mention ${args.packageName}@${args.version}.`);
  }
  for (const requiredHeading of ['Published Packages', 'Upgrade Notes', 'Verification']) {
    if (!body.includes(requiredHeading)) {
      errors.push(`GitHub Release ${args.releaseTag} must contain ${requiredHeading}.`);
    }
  }
  errors.push(
    ...validateRequiredCheckCoverage(body, {
      label: `GitHub Release ${args.releaseTag}`,
      checkers: requiredPmoChecks,
    }),
  );
}

function readJson(path, errors, label) {
  try {
    return JSON.parse(readFileSync(path, 'utf8'));
  } catch (error) {
    errors.push(`${label} is invalid JSON: ${error.message}`);
    return null;
  }
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&');
}

function compareText(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}

function usage() {
  console.log(`Usage: node scripts/check-release-notes.mjs --package=<name> --version=<version> [--tag=<tag> --check-github-release]

Checks that the platform and package changelogs cover the target release. CLI checks also compare
the locked PMO version with the previous PMO release and require migration, exception, and verification
notes for every newly added PMO required-check tool.`);
}

const isDirectExecution = process.argv[1] && pathToFileURL(resolve(process.argv[1])).href === import.meta.url;
if (isDirectExecution) process.exitCode = runReleaseNotesCheck();
