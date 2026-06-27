#!/usr/bin/env node
import {
  findPackage,
  GROUP_REGISTRY,
  normalizePackageName,
  readReleaseContracts,
  verifyPublishedPackage,
} from './release-guard-utils.mjs';

function usage() {
  console.log(`Usage: pnpm release:verify-npm <package|short-name> [--version=<version>] [--registry=<registry-url>]

Examples:
  pnpm release:verify-npm grid-widgets --version=1.0.5
  pnpm release:verify-npm @mango/system --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
`);
}

const args = process.argv.slice(2);
const packageArg = args.find((arg) => !arg.startsWith('--'));
const versionArg = args.find((arg) => arg.startsWith('--version='));
const registryArg = args.find((arg) => arg.startsWith('--registry='));
const registry = registryArg?.slice('--registry='.length) || GROUP_REGISTRY;

if (args.includes('--help') || args.includes('-h')) {
  usage();
  process.exit(0);
}

if (!packageArg) {
  usage();
  process.exit(1);
}

const packageName = normalizePackageName(packageArg);
const found = findPackage(packageName);

if (!found) {
  console.error(`Package not found in packages/*: ${packageName}`);
  process.exit(1);
}

const version = versionArg?.slice('--version='.length) || found.packageJson.version;
const contracts = readReleaseContracts();

try {
  verifyPublishedPackage(packageName, version, found, {
    registry,
    contract: contracts[packageName],
  });
  console.log(`Verified ${packageName}@${version} from ${registry}`);
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
