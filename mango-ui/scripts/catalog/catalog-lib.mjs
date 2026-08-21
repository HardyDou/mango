import { createHash } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

export function compileCatalog({ repositoryRoot, architectureReport, mavenInventory }) {
  const catalogRoot = path.join(repositoryRoot, 'mango-catalog');
  const uiRoot = path.join(repositoryRoot, 'mango-ui');
  const errors = [];
  const moduleSchema = readJson(path.join(catalogRoot, 'schema/module.schema.json'));
  const releaseArtifactSchema = readJson(path.join(catalogRoot, 'schema/release-artifact.schema.json'));
  const packageIndex = readPackageIndex(uiRoot, errors);
  const moduleRecords = readJsonDirectory(path.join(catalogRoot, 'modules'), errors);
  const artifactRecords = readJsonDirectory(path.join(catalogRoot, 'release-artifacts'), errors);

  if (architectureReport?.errors?.length) {
    errors.push(...architectureReport.errors.map((error) => `architecture: ${error}`));
  }
  if (!architectureReport?.reportSha256) errors.push('architecture: reportSha256 is required');
  if (mavenInventory?.sourceModel !== 'maven-effective-pom') {
    errors.push('maven: inventory must come from maven-effective-pom');
  }

  for (const record of moduleRecords) {
    errors.push(...validateJsonSchema(record.value, moduleSchema).map((error) => `${record.relativePath}: ${error}`));
  }
  for (const record of artifactRecords) {
    errors.push(
      ...validateJsonSchema(record.value, releaseArtifactSchema).map((error) => `${record.relativePath}: ${error}`),
    );
  }

  validateModules({ repositoryRoot, moduleRecords, packageIndex, mavenInventory, errors });
  validateReleaseArtifacts({ repositoryRoot, artifactRecords, errors });
  validatePackageProjections({ repositoryRoot, packageIndex, errors });
  if (errors.length > 0) throw new CatalogValidationError(errors);

  const packages = [...packageIndex.values()]
    .filter((record) => record.publishable)
    .map(normalizePackage)
    .sort((left, right) => compareUtf8(left.name, right.name));
  const modules = moduleRecords
    .map((record) => normalizeModule(record.value))
    .sort((left, right) => compareUtf8(left.moduleId, right.moduleId));
  const releaseArtifacts = artifactRecords
    .map((record) => normalizeReleaseArtifact(record.value))
    .sort((left, right) => compareUtf8(left.artifactId, right.artifactId));
  const projections = buildModuleProjections({
    repositoryRoot,
    manifests: moduleRecords.map((record) => record.value),
    mavenInventory,
    packageIndex,
  });
  const unsigned = {
    schemaVersion: 1,
    schemas: {
      moduleSha256: sha256(canonicalJsonBytes(moduleSchema)),
      releaseArtifactSha256: sha256(canonicalJsonBytes(releaseArtifactSchema)),
    },
    architectureGraph: {
      schemaVersion: architectureReport.schemaVersion,
      reportSha256: architectureReport.reportSha256,
      workspaceCount: architectureReport.summary.workspaceCount,
    },
    packages,
    modules,
    maven: mavenInventory,
    releaseArtifacts,
    projections,
  };
  return { ...unsigned, catalogDigest: sha256(canonicalJsonBytes(unsigned)) };
}

export function buildAdminModulesProjection(manifests) {
  const groups = { defaultPackages: [], fullPackages: [] };
  for (const manifest of [...manifests].sort((left, right) => compareUtf8(left.moduleId, right.moduleId))) {
    const packageName = manifest.frontendPackages[0];
    if (!packageName) continue;
    const entry = {
      code: manifest.moduleId,
      packageName,
      style: `${packageName}/style.css`,
      ...(manifest.adminIntegration?.registrarExports?.length
        ? {
            registrars: manifest.adminIntegration.registrarExports.map((registrar) => ({
              name: registrar.symbol,
              import: `${registrar.package}${registrar.subpath.slice(1)}`,
            })),
          }
        : {}),
      cliVersionKey: toCliVersionKey(packageName),
      ...(manifest.presetMembership === 'default' && manifest.adminIntegration?.registrarExports?.length
        ? { cliOptional: false }
        : {}),
    };
    const target = manifest.presetMembership === 'default' ? groups.defaultPackages : groups.fullPackages;
    target.push(entry);
  }
  return {
    schemaVersion: 1,
    defaultPackages: groups.defaultPackages,
    fullPackages: groups.fullPackages,
  };
}

export function buildModuleProjections({ repositoryRoot, manifests, mavenInventory, packageIndex = new Map() }) {
  const mavenByArtifactId = new Map(
    (mavenInventory?.publishableCoordinates || []).map((coordinate) => [coordinate.artifactId, coordinate]),
  );
  const moduleByPackage = new Map(
    manifests.flatMap((manifest) => manifest.frontendPackages.map((packageName) => [packageName, manifest])),
  );
  const modules = [...manifests]
    .sort((left, right) => compareUtf8(left.moduleId, right.moduleId))
    .map((manifest) => ({
      moduleId: manifest.moduleId,
      presetMembership: manifest.presetMembership,
      frontend: buildAdminModuleEntry(manifest),
      dependsOn: manifest.backendStarters?.length
        ? []
        : [
            ...new Set(
              manifest.frontendPackages.flatMap((packageName) => {
                const packageManifest = packageIndex.get(packageName)?.manifest || {};
                return Object.keys(packageManifest.dependencies || {})
                  .map((dependency) => moduleByPackage.get(dependency))
                  .filter(
                    (dependencyModule) =>
                      dependencyModule &&
                      dependencyModule.moduleId !== manifest.moduleId &&
                      ['full', 'custom-selectable'].includes(dependencyModule.presetMembership) &&
                      dependencyModule.backendStarters?.length,
                  )
                  .map((dependencyModule) => dependencyModule.moduleId);
              }),
            ),
          ].sort(compareUtf8),
      backendStarters: (manifest.backendStarters || []).map((starter) => {
        const coordinate = mavenByArtifactId.get(starter.slice(1));
        return { groupId: coordinate.groupId, artifactId: coordinate.artifactId };
      }),
      configFragments: (manifest.configFragments || []).map((fragment) => ({
        targetPath: fragment.targetPath,
        content: fs.readFileSync(path.join(repositoryRoot, fragment.source), 'utf8'),
        sha256: sha256(fs.readFileSync(path.join(repositoryRoot, fragment.source))),
      })),
      resourceCopies: (manifest.resourceCopies || []).map((resource) => ({
        targetPath: resource.targetPath,
        sha256: sha256(fs.readFileSync(path.join(repositoryRoot, resource.source))),
      })),
    }));
  return {
    adminModules: buildAdminModulesProjection(manifests),
    cliModules: {
      schemaVersion: 1,
      modules,
    },
  };
}

function buildAdminModuleEntry(manifest) {
  const packageName = manifest.frontendPackages[0];
  if (!packageName) return null;
  return {
    code: manifest.moduleId,
    packageName,
    style: `${packageName}/style.css`,
    registrars: (manifest.adminIntegration?.registrarExports || []).map((registrar) => ({
      name: registrar.symbol,
      import: `${registrar.package}${registrar.subpath.slice(1)}`,
    })),
    cliVersionKey: toCliVersionKey(packageName),
  };
}

export class CatalogValidationError extends Error {
  constructor(errors) {
    const normalized = [...new Set(errors)].sort(compareUtf8);
    super(`Catalog validation failed with ${normalized.length} issue(s):\n${normalized.join('\n')}`);
    this.name = 'CatalogValidationError';
    this.errors = normalized;
  }
}

export function validateJsonSchema(value, schema, location = '$') {
  const errors = [];
  validate(value, schema, location, errors);
  return errors;
}

export function canonicalJsonBytes(value) {
  return Buffer.from(JSON.stringify(canonicalize(value), null, 2) + '\n', 'utf8');
}

export function assertCatalogProjection(catalogFile, generatedBytes) {
  if (!fs.existsSync(catalogFile)) throw new Error(`tracked Catalog projection is missing: ${catalogFile}`);
  if (!fs.readFileSync(catalogFile).equals(generatedBytes)) {
    throw new Error('tracked Catalog projection differs from compiler output; run pnpm catalog -- --write');
  }
}

export function assertFileCopyProjection(sourceFile, targetFile) {
  if (!fs.existsSync(targetFile)) throw new Error(`tracked resource projection is missing: ${targetFile}`);
  if (!fs.readFileSync(sourceFile).equals(fs.readFileSync(targetFile))) {
    throw new Error(`tracked resource projection differs from source: ${targetFile}`);
  }
}

function validate(value, schema, location, errors) {
  if (Object.hasOwn(schema, 'const') && !sameJson(value, schema.const)) {
    errors.push(`${location} must equal ${JSON.stringify(schema.const)}`);
    return;
  }
  if (schema.enum && !schema.enum.some((candidate) => sameJson(value, candidate))) {
    errors.push(`${location} must be one of ${schema.enum.map((entry) => JSON.stringify(entry)).join(', ')}`);
    return;
  }
  if (schema.type && !matchesType(value, schema.type)) {
    errors.push(`${location} must be ${schema.type}`);
    return;
  }
  if (schema.type === 'object') {
    for (const required of schema.required || []) {
      if (!Object.hasOwn(value, required)) errors.push(`${location}.${required} is required`);
    }
    for (const [key, child] of Object.entries(value)) {
      if (schema.properties?.[key]) validate(child, schema.properties[key], `${location}.${key}`, errors);
      else if (schema.additionalProperties === false) errors.push(`${location}.${key} is not allowed`);
    }
  }
  if (schema.type === 'array') {
    if (schema.minItems !== undefined && value.length < schema.minItems) {
      errors.push(`${location} must contain at least ${schema.minItems} item(s)`);
    }
    if (schema.uniqueItems) {
      const seen = new Set();
      for (const item of value) {
        const identity = canonicalJsonBytes(item).toString('utf8');
        if (seen.has(identity)) errors.push(`${location} must contain unique items`);
        seen.add(identity);
      }
    }
    if (schema.items) value.forEach((item, index) => validate(item, schema.items, `${location}[${index}]`, errors));
  }
  if (schema.type === 'string') {
    if (schema.minLength !== undefined && value.length < schema.minLength) {
      errors.push(`${location} must contain at least ${schema.minLength} character(s)`);
    }
    if (schema.pattern && !new RegExp(schema.pattern, 'u').test(value)) {
      errors.push(`${location} must match ${schema.pattern}`);
    }
  }
}

function validateModules({ repositoryRoot, moduleRecords, packageIndex, mavenInventory, errors }) {
  const moduleIds = new Map();
  const packageOwners = new Map();
  const starterOwners = new Map();
  const projectionTargets = new Map();
  const mavenArtifacts = new Set(
    (mavenInventory?.publishableCoordinates || []).map((coordinate) => `:${coordinate.artifactId}`),
  );

  for (const record of moduleRecords) {
    const manifest = record.value;
    if (typeof manifest?.moduleId !== 'string') continue;
    const expectedFile = `${manifest.moduleId}.json`;
    if (path.basename(record.file) !== expectedFile) {
      errors.push(`${record.relativePath}: filename must be ${expectedFile}`);
    }
    claim(moduleIds, manifest.moduleId, record.relativePath, 'moduleId', errors);
    for (const packageName of manifest.frontendPackages || []) {
      const packageRecord = packageIndex.get(packageName);
      if (!packageRecord?.publishable)
        errors.push(`${record.relativePath}: unknown publishable package ${packageName}`);
      claim(packageOwners, packageName, manifest.moduleId, 'frontend package owner', errors);
    }
    const integration = manifest.adminIntegration;
    if (integration) {
      if (!packageIndex.get(integration.aggregateOwner)?.publishable) {
        errors.push(`${record.relativePath}: unknown Admin aggregate owner ${integration.aggregateOwner}`);
      }
      if (!(integration.registrarExports?.length || integration.styleEntries?.length)) {
        errors.push(`${record.relativePath}: adminIntegration must declare a registrar or style entry`);
      }
      for (const registrar of integration.registrarExports || []) {
        validatePackageSubpath(record.relativePath, registrar, manifest, packageIndex, 'registrar', errors);
        claimProjectionTarget(
          projectionTargets,
          `${integration.aggregateOwner}:registrar:${registrar.symbol}`,
          manifest.moduleId,
          errors,
        );
      }
      for (const style of integration.styleEntries || []) {
        validatePackageSubpath(record.relativePath, style, manifest, packageIndex, 'style', errors);
        claimProjectionTarget(
          projectionTargets,
          `${integration.aggregateOwner}:style:${style.package}/${style.subpath}`,
          manifest.moduleId,
          errors,
        );
      }
    }
    for (const starter of manifest.backendStarters || []) {
      if (!mavenArtifacts.has(starter))
        errors.push(`${record.relativePath}: unknown publishable Maven starter ${starter}`);
      claim(starterOwners, starter, manifest.moduleId, 'backend starter owner', errors);
    }
    for (const projection of [...(manifest.configFragments || []), ...(manifest.resourceCopies || [])]) {
      validateSourcePath(repositoryRoot, projection.source, record.relativePath, errors);
      validateTargetOwner(projection.targetOwner, packageIndex, mavenArtifacts, record.relativePath, errors);
      if (!isSafeRelativePath(projection.targetPath)) {
        errors.push(`${record.relativePath}: unsafe target path ${projection.targetPath}`);
      }
      claimProjectionTarget(
        projectionTargets,
        `${projection.targetOwner}:file:${projection.targetPath}`,
        manifest.moduleId,
        errors,
      );
    }
  }
}

function validateReleaseArtifacts({ repositoryRoot, artifactRecords, errors }) {
  const artifactIds = new Map();
  for (const record of artifactRecords) {
    const manifest = record.value;
    if (typeof manifest?.artifactId !== 'string') continue;
    const expectedFile = `${manifest.artifactId}.json`;
    if (path.basename(record.file) !== expectedFile) {
      errors.push(`${record.relativePath}: filename must be ${expectedFile}`);
    }
    claim(artifactIds, manifest.artifactId, record.relativePath, 'release artifactId', errors);
    const roots = [];
    for (const sourceRoot of manifest.sourceRoots || []) {
      const resolved = validateSourcePath(repositoryRoot, sourceRoot, record.relativePath, errors, 'directory');
      if (resolved) roots.push(resolved);
    }
    for (const requiredFile of manifest.requiredFiles || []) {
      if (!isSafeRelativePath(requiredFile)) {
        errors.push(`${record.relativePath}: unsafe required file path ${requiredFile}`);
        continue;
      }
      const matches = roots.filter((root) => {
        const candidate = path.resolve(root, requiredFile);
        return isInside(root, candidate) && fs.existsSync(candidate) && fs.statSync(candidate).isFile();
      });
      if (matches.length === 0) errors.push(`${record.relativePath}: required file is missing: ${requiredFile}`);
      if (matches.length > 1)
        errors.push(`${record.relativePath}: required file has multiple source owners: ${requiredFile}`);
    }
  }
}

function validatePackageProjections({ repositoryRoot, packageIndex, errors }) {
  for (const [packageName, record] of packageIndex) {
    if (!record.publishable) continue;
    const architecture = record.manifest.mangoArchitecture || {};
    const packageRoot = path.resolve(repositoryRoot, record.directory);
    for (const [subpath, descriptor] of Object.entries(architecture.sourceExports || {})) {
      validatePackageProjectionSource(packageRoot, record.directory, packageName, subpath, descriptor, errors);
      if (!Object.hasOwn(record.manifest.exports || {}, subpath)) {
        errors.push(`${record.directory}: source export ${subpath} is missing from package.json exports`);
      }
    }
    for (const [subpath, descriptor] of Object.entries(architecture.nonCodeExports || {})) {
      validatePackageProjectionSource(packageRoot, record.directory, packageName, subpath, descriptor, errors);
      if (!Object.hasOwn(record.manifest.exports || {}, subpath)) {
        errors.push(`${record.directory}: non-code export ${subpath} is missing from package.json exports`);
      }
    }
  }
}

function validatePackageProjectionSource(packageRoot, recordPath, packageName, subpath, descriptor, errors) {
  if (!descriptor || (typeof descriptor.source !== 'string' && typeof descriptor.sourcePattern !== 'string')) {
    errors.push(`${recordPath}: ${packageName} export ${subpath} must declare a source path`);
    return;
  }
  if (typeof descriptor.sourcePattern === 'string') {
    validatePackageProjectionPattern(packageRoot, recordPath, packageName, subpath, descriptor.sourcePattern, errors);
    return;
  }
  if (!isSafePackagePath(descriptor.source)) {
    errors.push(`${recordPath}: unsafe source path ${descriptor.source} for ${packageName}${subpath}`);
    return;
  }
  const sourcePath = path.resolve(packageRoot, descriptor.source);
  if (!isInside(packageRoot, sourcePath)) {
    errors.push(`${recordPath}: source path escapes package root: ${descriptor.source}`);
    return;
  }
  if (!fs.existsSync(sourcePath)) {
    errors.push(`${recordPath}: source path does not exist: ${descriptor.source}`);
    return;
  }
  const realRoot = fs.realpathSync(packageRoot);
  const realSource = fs.realpathSync(sourcePath);
  if (!isInside(realRoot, realSource)) {
    errors.push(`${recordPath}: source path escapes package root through symlink: ${descriptor.source}`);
  }
}

function validatePackageProjectionPattern(packageRoot, recordPath, packageName, subpath, pattern, errors) {
  if (!isSafePackagePath(pattern) || !pattern.includes('*')) {
    errors.push(`${recordPath}: unsafe source pattern ${pattern} for ${packageName}${subpath}`);
    return;
  }
  const normalized = pattern.startsWith('./') ? pattern.slice(2) : pattern;
  const starIndex = normalized.indexOf('*');
  const directory = path.resolve(packageRoot, normalized.slice(0, starIndex));
  const suffix = normalized.slice(starIndex + 1);
  if (!isInside(packageRoot, directory) || !fs.existsSync(directory) || !fs.statSync(directory).isDirectory()) {
    errors.push(`${recordPath}: source pattern directory does not exist: ${pattern}`);
    return;
  }
  const matches = fs
    .readdirSync(directory, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith(suffix));
  if (matches.length === 0) errors.push(`${recordPath}: source pattern has no matches: ${pattern}`);
}

function validatePackageSubpath(recordPath, reference, manifest, packageIndex, kind, errors) {
  if (!(manifest.frontendPackages || []).includes(reference.package)) {
    errors.push(`${recordPath}: ${kind} package ${reference.package} is not owned by module ${manifest.moduleId}`);
  }
  const packageRecord = packageIndex.get(reference.package);
  if (!packageRecord?.publishable) {
    errors.push(`${recordPath}: unknown ${kind} package ${reference.package}`);
    return;
  }
  const architecture = packageRecord.manifest.mangoArchitecture || {};
  const declared =
    kind === 'style'
      ? Object.hasOwn(architecture.nonCodeExports || {}, reference.subpath)
      : Object.hasOwn(architecture.sourceExports || {}, reference.subpath);
  if (!declared || !Object.hasOwn(packageRecord.manifest.exports || {}, reference.subpath)) {
    errors.push(
      `${recordPath}: ${kind} subpath is not a public authored export: ${reference.package}/${reference.subpath}`,
    );
  }
}

function validateTargetOwner(owner, packageIndex, mavenArtifacts, recordPath, errors) {
  if (packageIndex.get(owner)?.publishable || mavenArtifacts.has(owner)) return;
  errors.push(`${recordPath}: unknown projection target owner ${owner}`);
}

function validateSourcePath(repositoryRoot, relativePath, recordPath, errors, expectedKind = null) {
  if (!isSafeRelativePath(relativePath)) {
    errors.push(`${recordPath}: unsafe source path ${relativePath}`);
    return null;
  }
  const resolved = path.resolve(repositoryRoot, relativePath);
  if (!fs.existsSync(resolved)) {
    errors.push(`${recordPath}: source path does not exist: ${relativePath}`);
    return null;
  }
  const realRoot = fs.realpathSync(repositoryRoot);
  const realSource = fs.realpathSync(resolved);
  if (!isInside(realRoot, realSource)) {
    errors.push(`${recordPath}: source path escapes repository: ${relativePath}`);
    return null;
  }
  const stats = fs.statSync(realSource);
  if (expectedKind === 'directory' && !stats.isDirectory()) {
    errors.push(`${recordPath}: source root must be a directory: ${relativePath}`);
    return null;
  }
  return realSource;
}

function readPackageIndex(uiRoot, errors) {
  const index = new Map();
  const packagesRoot = path.join(uiRoot, 'packages');
  for (const entry of fs
    .readdirSync(packagesRoot, { withFileTypes: true })
    .sort((a, b) => compareUtf8(a.name, b.name))) {
    if (!entry.isDirectory()) continue;
    const manifestFile = path.join(packagesRoot, entry.name, 'package.json');
    if (!fs.existsSync(manifestFile)) continue;
    try {
      const manifest = readJson(manifestFile);
      if (!manifest.name) {
        errors.push(`${toPosix(path.relative(uiRoot, manifestFile))}: package name is required`);
        continue;
      }
      if (index.has(manifest.name)) errors.push(`duplicate frontend package ${manifest.name}`);
      index.set(manifest.name, {
        manifest,
        directory: `mango-ui/packages/${entry.name}`,
        publishable: manifest.private !== true && manifest.name.startsWith('@mango/'),
      });
    } catch (error) {
      errors.push(`${toPosix(path.relative(uiRoot, manifestFile))}: ${error.message}`);
    }
  }
  return index;
}

function readJsonDirectory(directory, errors) {
  if (!fs.existsSync(directory)) return [];
  return fs
    .readdirSync(directory, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith('.json'))
    .sort((left, right) => compareUtf8(left.name, right.name))
    .flatMap((entry) => {
      const file = path.join(directory, entry.name);
      try {
        return [
          { file, relativePath: toPosix(path.relative(path.resolve(directory, '..'), file)), value: readJson(file) },
        ];
      } catch (error) {
        errors.push(`${file}: ${error.message}`);
        return [];
      }
    });
}

function normalizePackage(record) {
  const architecture = record.manifest.mangoArchitecture || {};
  return {
    name: record.manifest.name,
    version: record.manifest.version,
    owner: `package:${record.manifest.name}`,
    path: record.directory,
    npmProjection: {
      types: record.manifest.types || null,
      exports: canonicalize(record.manifest.exports || {}),
      files: [...(record.manifest.files || [])].sort(compareUtf8),
    },
    publicEntries: normalizeExportMap(architecture.sourceExports || {}),
    publicResources: normalizeExportMap(architecture.nonCodeExports || {}),
  };
}

function normalizeExportMap(exports) {
  return Object.entries(exports)
    .map(([subpath, descriptor]) => ({ subpath, ...canonicalize(descriptor) }))
    .sort((left, right) => compareUtf8(left.subpath, right.subpath));
}

function normalizeModule(manifest) {
  const normalized = canonicalize(manifest);
  normalized.owner = `module:${manifest.moduleId}`;
  normalized.frontendPackages = [...manifest.frontendPackages].sort(compareUtf8);
  if (normalized.backendStarters) normalized.backendStarters.sort(compareUtf8);
  if (normalized.adminIntegration?.registrarExports) {
    normalized.adminIntegration.registrarExports.sort((left, right) =>
      compareUtf8(
        `${left.package}:${left.subpath}:${left.symbol}`,
        `${right.package}:${right.subpath}:${right.symbol}`,
      ),
    );
  }
  if (normalized.adminIntegration?.styleEntries) {
    normalized.adminIntegration.styleEntries.sort((left, right) =>
      compareUtf8(`${left.package}:${left.subpath}`, `${right.package}:${right.subpath}`),
    );
  }
  return normalized;
}

function normalizeReleaseArtifact(manifest) {
  return {
    ...canonicalize(manifest),
    owner: `release-artifact:${manifest.artifactId}`,
    sourceRoots: [...manifest.sourceRoots].sort(compareUtf8),
    requiredFiles: [...manifest.requiredFiles].sort(compareUtf8),
  };
}

function claim(owners, identity, owner, label, errors) {
  const previous = owners.get(identity);
  if (previous && previous !== owner) errors.push(`duplicate ${label} ${identity}: ${previous} and ${owner}`);
  else owners.set(identity, owner);
}

function claimProjectionTarget(owners, identity, owner, errors) {
  if (owners.has(identity)) {
    errors.push(`duplicate projection target ${identity}: ${owners.get(identity)} and ${owner}`);
  } else {
    owners.set(identity, owner);
  }
}

function isSafeRelativePath(value) {
  if (typeof value !== 'string' || !value || value.includes('\\') || path.posix.isAbsolute(value)) return false;
  const segments = value.split('/');
  return !segments.includes('..') && !segments.includes('.') && segments.every(Boolean);
}

function isSafePackagePath(value) {
  if (typeof value !== 'string' || !value) return false;
  const normalized = value.startsWith('./') ? value.slice(2) : value;
  return isSafeRelativePath(normalized);
}

function isInside(root, target) {
  const relative = path.relative(root, target);
  return relative === '' || (!relative.startsWith(`..${path.sep}`) && relative !== '..' && !path.isAbsolute(relative));
}

function matchesType(value, type) {
  if (type === 'null') return value === null;
  if (type === 'array') return Array.isArray(value);
  if (type === 'object') return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
  if (type === 'integer') return Number.isInteger(value);
  return typeof value === type;
}

function sameJson(left, right) {
  return canonicalJsonBytes(left).equals(canonicalJsonBytes(right));
}

function canonicalize(value) {
  if (Array.isArray(value)) return value.map(canonicalize);
  if (!value || typeof value !== 'object') return value;
  return Object.fromEntries(
    Object.keys(value)
      .sort(compareUtf8)
      .map((key) => [key, canonicalize(value[key])]),
  );
}

function sha256(value) {
  return createHash('sha256').update(value).digest('hex');
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function toPosix(value) {
  return value.split(path.sep).join('/');
}

function compareUtf8(left, right) {
  return Buffer.compare(Buffer.from(left, 'utf8'), Buffer.from(right, 'utf8'));
}

function toCliVersionKey(packageName) {
  const packageCode = packageName.replace(/^@mango\//u, '');
  return `mango${packageCode
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')}`;
}
