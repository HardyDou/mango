import { createHash } from 'node:crypto';
import { existsSync, lstatSync, readFileSync, readdirSync, realpathSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';

const TEXT_BOUNDARY_FILES = new Set([
  '.npmrc',
  'package.json',
  'pnpm-lock.yaml',
  'pnpm-workspace.yaml',
  'tsconfig.json',
  'tsconfig.app.json',
  'vite.config.ts',
]);

export function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

export function writeJson(path, value) {
  writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`);
}

export function sha256File(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

export function applyTarballMappings(frontendRoot, tarballsByName, registry) {
  const packagePath = join(frontendRoot, 'package.json');
  const packageJson = readJson(packagePath);
  const relativeMappings = new Map();

  for (const [packageName, tarballPath] of [...tarballsByName].sort(([left], [right]) => left.localeCompare(right))) {
    const relativeTarball = relative(frontendRoot, tarballPath).split('\\').join('/');
    relativeMappings.set(packageName, `file:${relativeTarball}`);
  }

  for (const section of ['dependencies', 'devDependencies', 'peerDependencies']) {
    for (const dependency of Object.keys(packageJson[section] || {})) {
      if (dependency.startsWith('@mango/')) {
        const mapping = relativeMappings.get(dependency);
        if (!mapping) {
          throw new Error(`generated frontend dependency has no local tarball: ${dependency}`);
        }
        packageJson[section][dependency] = mapping;
      }
    }
  }
  delete packageJson.pnpm;
  writeJson(packagePath, packageJson);

  const overrideLines = [...relativeMappings].map(([packageName, tarball]) => `  '${packageName}': '${tarball}'`);
  writeFileSync(
    join(frontendRoot, 'pnpm-workspace.yaml'),
    [
      'packages:',
      "  - 'packages/*'",
      'trustLockfile: true',
      'allowBuilds:',
      "  '@parcel/watcher': true",
      "  '@swc/core': true",
      '  core-js-pure: true',
      '  es5-ext: true',
      '  esbuild: true',
      '  msw: true',
      '  vue-demi: true',
      'overrides:',
      ...overrideLines,
      '',
    ].join('\n'),
  );
  writeFileSync(join(frontendRoot, '.npmrc'), `registry=${registry}\n`);
  return relativeMappings;
}

export function assertGeneratedProjectBoundary(projectRoot, forbiddenAbsoluteRoots = []) {
  const issues = [];
  const workspacePackages = collectWorkspacePackages(projectRoot);
  for (const file of walkFiles(projectRoot, { skipNodeModules: true })) {
    const isTextFile = TEXT_BOUNDARY_FILES.has(file.name) || /\.(?:mjs|ts|json|ya?ml)$/u.test(file.name);
    if (!isTextFile) {
      continue;
    }
    const source = readFileSync(file.path, 'utf8');
    if (TEXT_BOUNDARY_FILES.has(file.name)) {
      for (const marker of ['portal:', 'mango-ui/packages/']) {
        if (source.includes(marker)) {
          issues.push(`${relative(projectRoot, file.path)} contains forbidden source reference ${marker}`);
        }
      }
      if (file.name === 'package.json') {
        assertWorkspaceManifest(file.path, workspacePackages, issues, projectRoot);
      } else if (source.includes('workspace:')) {
        for (const specifier of source.match(/workspace:[^\s'"\]}]+/gu) || []) {
          const version = specifier.slice('workspace:'.length);
          if (![...workspacePackages.values()].some((entry) => entry.version === version)) {
            issues.push(`${relative(projectRoot, file.path)} contains non-exact workspace reference ${specifier}`);
          }
        }
      }
      for (const match of source.matchAll(/link:([^\s'"\]}]+)/gu)) {
        const workspaceEntries = [...workspacePackages.values()];
        const possibleTargets = [dirname(file.path), ...workspaceEntries.map((entry) => entry.root)].map((root) =>
          resolve(root, match[1]),
        );
        if (!workspaceEntries.some((entry) => possibleTargets.includes(entry.root))) {
          issues.push(`${relative(projectRoot, file.path)} contains non-project link reference link:${match[1]}`);
        }
      }
    }
    for (const root of forbiddenAbsoluteRoots.filter(Boolean)) {
      const absoluteDirectoryPrefix = `${resolve(root).replace(/[\\/]+$/u, '')}/`;
      if (source.includes(absoluteDirectoryPrefix)) {
        issues.push(`${relative(projectRoot, file.path)} contains host repository path`);
      }
    }
    if (file.name === '.npmrc' && /(?:_authToken|_password|username)\s*=/iu.test(source)) {
      issues.push(`${relative(projectRoot, file.path)} contains registry credentials`);
    }
  }
  if (issues.length > 0) {
    throw new Error(`generated project boundary failed:\n${issues.join('\n')}`);
  }
}

export function assertInstalledSymlinkBoundary(nodeModulesRoot, allowedWorkspaceRoots = []) {
  const allowedRoot = `${realpathSync(nodeModulesRoot)}/`;
  const allowedWorkspacePrefixes = allowedWorkspaceRoots.map(
    (root) => `${realpathSync(root).replace(/[\\/]+$/u, '')}/`,
  );
  const issues = [];
  for (const file of walkFiles(nodeModulesRoot, { includeDirectories: true, followSymlinks: false })) {
    if (!file.isSymbolicLink) {
      continue;
    }
    let target;
    try {
      target = realpathSync(file.path);
    } catch (error) {
      issues.push(`${relative(nodeModulesRoot, file.path)} is broken: ${error.message}`);
      continue;
    }
    const allowedWorkspaceTarget = allowedWorkspacePrefixes.some(
      (root) => target === root.slice(0, -1) || target.startsWith(root),
    );
    if (target !== allowedRoot.slice(0, -1) && !target.startsWith(allowedRoot) && !allowedWorkspaceTarget) {
      issues.push(`${relative(nodeModulesRoot, file.path)} escapes node_modules to ${target}`);
    }
  }
  if (issues.length > 0) {
    throw new Error(`installed dependency boundary failed:\n${issues.join('\n')}`);
  }
}

function collectWorkspacePackages(projectRoot) {
  const packages = new Map();
  for (const file of walkFiles(projectRoot, { skipNodeModules: true })) {
    if (file.name !== 'package.json') continue;
    const packageJson = readJson(file.path);
    if (typeof packageJson.name !== 'string' || typeof packageJson.version !== 'string') continue;
    packages.set(packageJson.name, { version: packageJson.version, root: dirname(file.path) });
  }
  return packages;
}

function assertWorkspaceManifest(packagePath, workspacePackages, issues, projectRoot) {
  const packageJson = readJson(packagePath);
  for (const section of ['dependencies', 'devDependencies', 'optionalDependencies', 'peerDependencies']) {
    for (const [dependency, specifier] of Object.entries(packageJson[section] || {})) {
      if (typeof specifier !== 'string' || !specifier.startsWith('workspace:')) continue;
      const workspacePackage = workspacePackages.get(dependency);
      if (!workspacePackage || specifier !== `workspace:${workspacePackage.version}`) {
        issues.push(
          `${relative(projectRoot, packagePath)} contains invalid local workspace dependency ` +
            `${section}.${dependency}=${specifier}`,
        );
      }
    }
  }
}

function walkFiles(root, options = {}) {
  if (!existsSync(root)) {
    return [];
  }
  const result = [];
  const visit = (path) => {
    const stat = lstatSync(path);
    const name = path.split(/[\\/]/u).pop() || '';
    if (stat.isSymbolicLink()) {
      result.push({ path, name, isSymbolicLink: true });
      return;
    }
    if (stat.isDirectory()) {
      if (options.skipNodeModules && name === 'node_modules') {
        return;
      }
      if (options.includeDirectories && path !== root) {
        result.push({ path, name, isSymbolicLink: false });
      }
      for (const entry of readdirSync(path).sort()) {
        visit(join(path, entry));
      }
      return;
    }
    if (stat.isFile() && statSync(path).size <= 5 * 1024 * 1024) {
      result.push({ path, name, isSymbolicLink: false });
    }
  };
  visit(root);
  return result;
}
