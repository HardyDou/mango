#!/usr/bin/env node
import { createRequire } from 'node:module';
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const scriptPath = fileURLToPath(import.meta.url);

function parseArgs(argv) {
  const options = {
    check: false,
    root: process.cwd(),
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--check') {
      options.check = true;
      continue;
    }

    if (['--manifest', '--package', '--out', '--root'].includes(arg)) {
      const value = argv[index + 1];
      if (!value || value.startsWith('--')) {
        fail(`${arg} requires a value.`);
      }
      options[arg.slice(2)] = value;
      index += 1;
      continue;
    }

    if (
      [
        '--admin-manifest-out',
        '--full-style-out',
        '--full-entry-out',
        '--full-types-out',
        '--build-deps-script-out',
      ].includes(arg)
    ) {
      const value = argv[index + 1];
      if (!value || value.startsWith('--')) {
        fail(`${arg} requires a value.`);
      }
      options[arg.slice(2)] = value;
      index += 1;
      continue;
    }

    fail(`Unknown argument: ${arg}`);
  }

  for (const required of ['manifest', 'package', 'out']) {
    if (!options[required]) {
      fail(`Missing required argument: --${required}`);
    }
  }

  options.root = resolvePath(process.cwd(), options.root);
  options.manifest = resolvePath(options.root, options.manifest);
  options.package = resolvePath(options.root, options.package);
  options.out = resolvePath(options.root, options.out);
  for (const optionalPath of [
    'admin-manifest-out',
    'full-style-out',
    'full-entry-out',
    'full-types-out',
    'build-deps-script-out',
  ]) {
    if (options[optionalPath]) {
      options[optionalPath] = resolvePath(options.root, options[optionalPath]);
    }
  }

  return options;
}

function resolvePath(baseDir, path) {
  return isAbsolute(path) ? path : resolve(baseDir, path);
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function fail(message) {
  console.error(`[package-styles] ${message}`);
  process.exit(1);
}

function hasDeclaredDependency(packageJson, dependencyName) {
  return Boolean(
    packageJson.dependencies?.[dependencyName] ||
      packageJson.devDependencies?.[dependencyName] ||
      packageJson.peerDependencies?.[dependencyName] ||
      packageJson.optionalDependencies?.[dependencyName],
  );
}

function findPackageJson(startPath, expectedName, rootDir) {
  let currentDir = dirname(startPath);
  while (currentDir !== dirname(currentDir)) {
    const packageJsonPath = join(currentDir, 'package.json');
    if (existsSync(packageJsonPath)) {
      const packageJson = readJson(packageJsonPath);
      if (packageJson.name === expectedName) {
        return { packageJsonPath, packageJson };
      }
    }
    currentDir = dirname(currentDir);
  }

  fail(`Could not locate package.json for ${expectedName} from ${relative(rootDir, startPath)}`);
}

function findWorkspacePackageJson(rootDir, expectedName) {
  const packagesDir = join(rootDir, 'packages');
  if (!existsSync(packagesDir)) {
    return null;
  }

  for (const entry of readdirSync(packagesDir, { withFileTypes: true })) {
    if (!entry.isDirectory()) {
      continue;
    }
    const packageJsonPath = join(packagesDir, entry.name, 'package.json');
    if (!existsSync(packageJsonPath)) {
      continue;
    }
    const packageJson = readJson(packageJsonPath);
    if (packageJson.name === expectedName) {
      return { packageJsonPath, packageJson };
    }
  }

  return null;
}

function readWorkspacePackageJson(rootDir, expectedName) {
  return findWorkspacePackageJson(rootDir, expectedName)?.packageJson || null;
}

function readStyleExportTarget(packageJson) {
  const styleExport = packageJson.exports?.['./style.css'];
  if (typeof styleExport === 'string') {
    return styleExport;
  }
  if (styleExport && typeof styleExport.import === 'string') {
    return styleExport.import;
  }
  if (styleExport && typeof styleExport.default === 'string') {
    return styleExport.default;
  }
  return '';
}

function validateStyleExport(item, consumerPackagePath, rootDir) {
  const workspacePackage = findWorkspacePackageJson(rootDir, item.name);
  if (workspacePackage) {
    const styleExportTarget = readStyleExportTarget(workspacePackage.packageJson);
    if (!styleExportTarget) {
      fail(`${item.name} must export ./style.css before it can be aggregated.`);
    }
    return {
      packageJsonPath: workspacePackage.packageJsonPath,
      resolvedStylePath: join(dirname(workspacePackage.packageJsonPath), styleExportTarget),
    };
  }

  const consumerRequire = createRequire(consumerPackagePath);
  let resolvedStylePath;

  try {
    resolvedStylePath = consumerRequire.resolve(item.style);
  } catch (error) {
    fail(`Cannot resolve ${item.style} from ${relative(rootDir, consumerPackagePath)}: ${error.message}`);
  }

  const { packageJsonPath, packageJson } = findPackageJson(resolvedStylePath, item.name, rootDir);
  if (!packageJson.exports?.['./style.css']) {
    fail(`${item.name} must export ./style.css before it can be aggregated.`);
  }

  return {
    packageJsonPath,
    resolvedStylePath,
  };
}

function normalizeManifest(manifest) {
  if (manifest.schemaVersion !== 1) {
    fail(`Unsupported schemaVersion: ${manifest.schemaVersion}`);
  }

  if (Array.isArray(manifest.packages)) {
    return {
      defaultPackages: manifest.packages,
      fullPackages: [],
    };
  }

  if (!Array.isArray(manifest.defaultPackages) || manifest.defaultPackages.length === 0) {
    fail('Admin module manifest must declare at least one default package.');
  }
  if (!Array.isArray(manifest.fullPackages)) {
    fail('Admin module manifest fullPackages must be an array.');
  }

  return {
    defaultPackages: manifest.defaultPackages,
    fullPackages: manifest.fullPackages,
  };
}

function validateStyleItems(items, consumerPackageJson, consumerPackagePath, rootDir) {
  const seen = new Set();
  return items.map((item) => {
    const name = item.name || item.packageName;
    if (!item || typeof name !== 'string' || typeof item.style !== 'string') {
      fail('Each package item must include string packageName/name and style fields.');
    }
    if (seen.has(name)) {
      fail(`Duplicate package declaration: ${name}`);
    }
    seen.add(name);

    if (item.style !== `${name}/style.css`) {
      fail(`${name} style must use its public style export: ${name}/style.css`);
    }
    if (!hasDeclaredDependency(consumerPackageJson, name)) {
      fail(`${relative(rootDir, consumerPackagePath)} is missing dependency declaration: ${name}`);
    }

    const resolution = validateStyleExport({ name, style: item.style }, consumerPackagePath, rootDir);
    return {
      ...item,
      name,
      packageName: name,
      style: item.style,
      packageJsonPath: relative(rootDir, resolution.packageJsonPath),
      resolvedStylePath: relative(rootDir, resolution.resolvedStylePath),
    };
  });
}

function validateManifest(manifest, consumerPackageJson, consumerPackagePath, rootDir) {
  const normalized = normalizeManifest(manifest);
  const allPackages = [...normalized.defaultPackages, ...normalized.fullPackages];
  const seen = new Set();
  for (const item of allPackages) {
    const name = item.name || item.packageName;
    if (seen.has(name)) {
      fail(`Duplicate package declaration across default/full packages: ${name}`);
    }
    seen.add(name);
  }

  return {
    defaultPackages: validateStyleItems(normalized.defaultPackages, consumerPackageJson, consumerPackagePath, rootDir),
    fullPackages: validateStyleItems(normalized.fullPackages, consumerPackageJson, consumerPackagePath, rootDir),
  };
}

function renderDefaultAdminManifest(packages) {
  return `${JSON.stringify(
    {
      schemaVersion: 1,
      packages: packages.map((item) => ({
        name: item.packageName,
        style: item.style,
      })),
    },
    null,
    2,
  )}\n`;
}

function renderStyles(packages, options) {
  const imports = packages.map((item) => `@import '${item.style}';`).join('\n');
  return [
    '/* This file is generated by scripts/generate-package-styles.mjs. */',
    `/* Edit ${relative(options.root, options.manifest)}, then run the package style generation command. */`,
    imports,
    '',
  ].join('\n');
}

function renderFullStyles(defaultPackages, fullPackages, options) {
  const imports = fullPackages.map((item) => `@import '${item.style}';`).join('\n');
  return [
    '/* This file is generated by scripts/generate-package-styles.mjs. */',
    `/* Edit ${relative(options.root, options.manifest)}, then run the package style generation command. */`,
    "@import './style.css';",
    imports,
    '',
  ].join('\n');
}

function getRegistrars(packages) {
  return packages.flatMap((item) => item.registrars || []);
}

function renderFullEntry(defaultPackages, fullPackages, options) {
  const registrars = getRegistrars([...defaultPackages, ...fullPackages]);
  const uniqueImports = [...new Map(registrars.map((registrar) => [registrar.name, registrar])).values()];
  const exportLines = uniqueImports.map((registrar) => `export { ${registrar.name} } from '${registrar.import}';`);
  const importLines = uniqueImports.map((registrar) => `import { ${registrar.name} } from '${registrar.import}';`);
  const registrarLines = uniqueImports.map((registrar) => `  ${registrar.name},`);

  return [
    '/* This file is generated by scripts/generate-package-styles.mjs. */',
    `/* Edit ${relative(options.root, options.manifest)}, then run the package style generation command. */`,
    "export { createMangoAdminApp } from '@mango/admin-shell';",
    "export type { MangoAdminShellOptions, MangoAdminAppInstance } from '@mango/admin-shell';",
    ...exportLines,
    '',
    "import type { MangoAdminFeatureRegistrar } from '@mango/admin-shell';",
    ...importLines,
    '',
    'export const mangoFullAdminFeatureRegistrars: MangoAdminFeatureRegistrar[] = [',
    ...registrarLines,
    '];',
    '',
  ].join('\n');
}

function renderFullTypes(defaultPackages, fullPackages, options) {
  const registrars = getRegistrars([...defaultPackages, ...fullPackages]);
  const uniqueImports = [...new Map(registrars.map((registrar) => [registrar.name, registrar])).values()];
  const exportLines = uniqueImports.map((registrar) => `export { ${registrar.name} } from '${registrar.import}';`);

  return [
    '/* This file is generated by scripts/generate-package-styles.mjs. */',
    `/* Edit ${relative(options.root, options.manifest)}, then run the package style generation command. */`,
    "export { createMangoAdminApp } from '@mango/admin-shell';",
    "export type { MangoAdminShellOptions, MangoAdminAppInstance } from '@mango/admin-shell';",
    ...exportLines,
    "import type { MangoAdminFeatureRegistrar } from '@mango/admin-shell';",
    'export declare const mangoFullAdminFeatureRegistrars: MangoAdminFeatureRegistrar[];',
    '',
  ].join('\n');
}

function renderBuildDepsScript(defaultPackages, fullPackages, consumerPackageJson, options) {
  const packageNames = [
    ...Object.keys(consumerPackageJson.dependencies || {}),
    ...defaultPackages.map((item) => item.packageName),
    ...fullPackages.map((item) => item.packageName),
  ].filter((packageName, index, packages) =>
    packageName.startsWith('@mango/') &&
    packageName !== '@mango/admin' &&
    packages.indexOf(packageName) === index &&
    Boolean(readWorkspacePackageJson(options.root, packageName)?.scripts?.build),
  );

  return [
    '#!/usr/bin/env node',
    "import { spawnSync } from 'node:child_process';",
    '',
    'const packages = [',
    ...packageNames.map((packageName) => `  '${packageName}',`),
    '];',
    '',
    'for (const packageName of packages) {',
    "  const result = spawnSync('pnpm', ['--dir', '../..', '-F', packageName, 'build'], {",
    "    stdio: 'inherit',",
    "    shell: process.platform === 'win32',",
    '  });',
    '',
    '  if (result.status !== 0) {',
    '    process.exit(result.status || 1);',
    '  }',
    '}',
    '',
    `// Generated from ${relative(options.root, options.manifest)}.`,
    '',
  ].join('\n');
}

function checkOrWrite(path, nextContent, options) {
  const currentContent = existsSync(path) ? readFileSync(path, 'utf8') : '';
  if (options.check) {
    if (currentContent !== nextContent) {
      fail(`${relative(options.root, path)} is out of date. Run the package style generation command.`);
    }
    return;
  }
  writeFileSync(path, nextContent);
}

export function main(argv = process.argv.slice(2)) {
  const options = parseArgs(argv);
  const manifest = readJson(options.manifest);
  const consumerPackageJson = readJson(options.package);
  const resolvedPackages = validateManifest(manifest, consumerPackageJson, options.package, options.root);

  checkOrWrite(options.out, renderStyles(resolvedPackages.defaultPackages, options), options);

  if (options['admin-manifest-out']) {
    checkOrWrite(
      options['admin-manifest-out'],
      renderDefaultAdminManifest(resolvedPackages.defaultPackages),
      options,
    );
  }
  if (options['full-style-out']) {
    checkOrWrite(
      options['full-style-out'],
      renderFullStyles(resolvedPackages.defaultPackages, resolvedPackages.fullPackages, options),
      options,
    );
  }
  if (options['full-entry-out']) {
    checkOrWrite(
      options['full-entry-out'],
      renderFullEntry(resolvedPackages.defaultPackages, resolvedPackages.fullPackages, options),
      options,
    );
  }
  if (options['full-types-out']) {
    checkOrWrite(
      options['full-types-out'],
      renderFullTypes(resolvedPackages.defaultPackages, resolvedPackages.fullPackages, options),
      options,
    );
  }
  if (options['build-deps-script-out']) {
    checkOrWrite(
      options['build-deps-script-out'],
      renderBuildDepsScript(
        resolvedPackages.defaultPackages,
        resolvedPackages.fullPackages,
        consumerPackageJson,
        options,
      ),
      options,
    );
  }

  const checkedCount = resolvedPackages.defaultPackages.length + resolvedPackages.fullPackages.length;
  if (options.check) {
    console.log(`[package-styles] ${relative(options.root, options.out)} is up to date.`);
    console.log(`[package-styles] checked ${checkedCount} package style exports.`);
    return;
  }

  console.log(`[package-styles] wrote ${relative(options.root, options.out)}`);
  console.log(`[package-styles] checked ${checkedCount} package style exports.`);
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
