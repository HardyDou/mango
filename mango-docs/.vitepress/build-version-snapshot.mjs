import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const docsRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const stageRoot = resolve(docsRoot, '.vitepress/public-src');
const distRoot = resolve(stageRoot, '.vitepress/dist');
const versionsRoot = resolve(docsRoot, 'versions');
const manifestPath = resolve(versionsRoot, 'manifest.json');

const version = parseVersionArg(process.argv.slice(2));
const siteBase = normalizeBase(process.env.MANGO_DOCS_SITE_BASE || '/mango/');
const versionBase = `${siteBase}versions/${version}/`;
const versionDir = resolve(versionsRoot, version);

await mkdir(versionsRoot, { recursive: true });
await upsertVersionManifest(version);

run('node', ['.vitepress/stage-public-docs.mjs'], {
  VITEPRESS_BASE: versionBase,
  MANGO_DOCS_VERSION_LABEL: version
});
run(resolveBin('vitepress'), ['build', '.vitepress/public-src'], {
  VITEPRESS_BASE: versionBase,
  MANGO_DOCS_VERSION_LABEL: version
});

await rm(versionDir, { recursive: true, force: true });
await mkdir(versionDir, { recursive: true });
await cp(distRoot, versionDir, { recursive: true });

console.log(`Built Mango Docs snapshot ${version} at versions/${version}`);

function parseVersionArg(args) {
  const raw =
    args.find((arg) => arg.startsWith('--version='))?.slice('--version='.length) ||
    args.find((arg) => !arg.startsWith('-'));

  if (!raw) {
    throw new Error('Missing docs version. Usage: npm run docs:snapshot -- <release-tag>');
  }

  if (!/^[0-9A-Za-z._-]+$/.test(raw)) {
    throw new Error(`Invalid docs version "${raw}". Use only letters, numbers, dot, underscore and hyphen.`);
  }

  return raw;
}

async function upsertVersionManifest(nextVersion) {
  const manifest = await readManifest();
  const versions = Array.isArray(manifest.versions) ? manifest.versions : [];
  const nextEntry = {
    version: nextVersion,
    label: nextVersion,
    path: `/versions/${nextVersion}/`
  };
  const deduped = versions.filter((entry) => entry?.version !== nextVersion);

  await writeFile(
    manifestPath,
    `${JSON.stringify(
      {
        latest: manifest.latest || { label: 'Latest', path: '/' },
        versions: [nextEntry, ...deduped]
      },
      null,
      2
    )}\n`
  );
}

async function readManifest() {
  try {
    return JSON.parse(await readFile(manifestPath, 'utf8'));
  } catch (error) {
    if (error?.code === 'ENOENT') {
      return {};
    }
    throw error;
  }
}

function run(command, args, extraEnv) {
  const result = spawnSync(command, args, {
    cwd: docsRoot,
    env: { ...process.env, ...extraEnv },
    stdio: 'inherit'
  });

  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed with exit code ${result.status}`);
  }
}

function resolveBin(name) {
  return resolve(docsRoot, 'node_modules/.bin', process.platform === 'win32' ? `${name}.cmd` : name);
}

function normalizeBase(base) {
  const withLeadingSlash = base.startsWith('/') ? base : `/${base}`;
  return withLeadingSlash.endsWith('/') ? withLeadingSlash : `${withLeadingSlash}/`;
}
