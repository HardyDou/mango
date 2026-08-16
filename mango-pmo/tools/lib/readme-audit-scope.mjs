import fs from 'node:fs';
import path from 'node:path';

const DEFAULT_BUSINESS_PATHS = Object.freeze({
  backend: 'backend',
  frontend: 'frontend',
  businessDocs: 'business-docs'
});

export function resolveReadmeAuditScope({
  argv = process.argv.slice(2),
  cwd = process.cwd(),
  scriptPath
} = {}) {
  if (!scriptPath) {
    throw new Error('README audit scope resolution requires scriptPath');
  }

  const explicitRoot = readProjectRootArgument(argv);
  const root = explicitRoot
    ? canonicalDirectory(path.resolve(cwd, explicitRoot), '--project-root')
    : discoverProjectRoot(cwd, scriptPath);

  if (isMangoSourceRoot(root)) {
    return Object.freeze({
      kind: 'mango-source',
      root,
      paths: Object.freeze({
        backend: 'mango',
        frontend: 'mango-ui',
        businessDocs: 'mango-docs'
      })
    });
  }

  if (isBusinessConsumerRoot(root)) {
    return Object.freeze({
      kind: 'business-consumer',
      root,
      paths: Object.freeze(readBusinessPaths(root))
    });
  }

  throw new Error(
    `README audit project root is neither the Mango source repository nor a Mango business consumer: ${root}`
  );
}

export function isMangoSourceRoot(root) {
  return [
    'mango/pom.xml',
    'mango-pmo/rules/index.json',
    'mango-ui/packages/mango-pmo/package.json',
    'mango-business-starter/business-pmo'
  ].every((entry) => fs.existsSync(path.join(root, entry)));
}

export function isBusinessConsumerRoot(root) {
  return fs.existsSync(path.join(root, 'mango.config.json'))
    && fs.existsSync(path.join(root, 'business-pmo/mango-baseline/rules/08-capability-docs.md'));
}

export function businessCapabilityReadmes(scope) {
  if (scope.kind !== 'business-consumer') {
    throw new Error('businessCapabilityReadmes requires a business-consumer scope');
  }
  const capabilityMap = `${scope.paths.businessDocs}/capabilities/README.md`;
  const capabilityMapPath = path.join(scope.root, capabilityMap);
  if (!fs.existsSync(capabilityMapPath)) {
    throw new Error(`Business capability map is missing: ${capabilityMap}`);
  }
  const content = fs.readFileSync(capabilityMapPath, 'utf8');
  const moduleReadmes = new Set();
  for (const match of content.matchAll(/\[[^\]]+\]\(([^)]+)\)/g)) {
    const href = match[1].split('#')[0].split('?')[0];
    if (!href || /^[a-z][a-z0-9+.-]*:/i.test(href) || !href.endsWith('README.md')) {
      continue;
    }
    const target = path.resolve(path.dirname(capabilityMapPath), decodeURIComponent(href));
    const relative = repositoryRelativePath(scope.root, target);
    if (!relative || !isConfiguredModuleReadme(scope, relative)) {
      continue;
    }
    if (fs.existsSync(target) && !isCanonicalPathInsideRoot(scope.root, target)) {
      throw new Error(`Business capability map references a README outside the project root: ${relative}`);
    }
    if (!fs.existsSync(target) || !fs.statSync(target).isFile()) {
      throw new Error(`Business capability map references a missing module README: ${relative}`);
    }
    moduleReadmes.add(relative);
  }
  if (moduleReadmes.size === 0) {
    throw new Error(
      `Business capability map does not reference a README under ${scope.paths.backend} or ${scope.paths.frontend}`
    );
  }
  return Object.freeze({
    capabilityMap,
    moduleReadmes: Object.freeze([...moduleReadmes].sort())
  });
}

function readProjectRootArgument(argv) {
  let value = '';
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === '--project-root') {
      if (value || !argv[index + 1] || argv[index + 1].startsWith('--')) {
        throw new Error('--project-root requires exactly one directory argument');
      }
      value = argv[index + 1];
      index += 1;
      continue;
    }
    if (argument.startsWith('--project-root=')) {
      if (value || argument.slice('--project-root='.length).length === 0) {
        throw new Error('--project-root requires exactly one directory argument');
      }
      value = argument.slice('--project-root='.length);
    }
  }
  return value;
}

function discoverProjectRoot(cwd, scriptPath) {
  const seeds = [path.dirname(path.resolve(scriptPath)), path.resolve(cwd)];
  const visited = new Set();
  for (const seed of seeds) {
    const root = findAncestor(seed, (candidate) => (
      isMangoSourceRoot(candidate) || isBusinessConsumerRoot(candidate)
    ));
    if (root && !visited.has(root)) {
      return canonicalDirectory(root, 'discovered project root');
    }
    if (root) visited.add(root);
  }
  throw new Error(
    `Cannot resolve README audit project root from script ${path.resolve(scriptPath)} or cwd ${path.resolve(cwd)}`
  );
}

function findAncestor(start, predicate) {
  let current = path.resolve(start);
  while (true) {
    if (predicate(current)) return current;
    const parent = path.dirname(current);
    if (parent === current) return '';
    current = parent;
  }
}

function canonicalDirectory(candidate, label) {
  if (!fs.existsSync(candidate)) {
    throw new Error(`${label} does not exist: ${candidate}`);
  }
  const canonical = fs.realpathSync(candidate);
  if (!fs.statSync(canonical).isDirectory()) {
    throw new Error(`${label} is not a directory: ${candidate}`);
  }
  return canonical;
}

function readBusinessPaths(root) {
  let document;
  const configPath = path.join(root, 'mango.config.json');
  try {
    document = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  } catch (error) {
    throw new Error(`Invalid mango.config.json: ${error.message}`);
  }
  const configured = document?.paths;
  if (configured !== undefined && (!configured || typeof configured !== 'object' || Array.isArray(configured))) {
    throw new Error('mango.config.json paths must be an object');
  }
  return Object.fromEntries(Object.entries(DEFAULT_BUSINESS_PATHS).map(([name, fallback]) => {
    const value = configured?.[name] ?? fallback;
    return [name, normalizeRepositoryPath(root, value, `mango.config.json paths.${name}`)];
  }));
}

function normalizeRepositoryPath(root, value, label) {
  if (typeof value !== 'string' || value.trim().length === 0) {
    throw new Error(`${label} must be a non-empty relative path`);
  }
  const normalized = value.trim().split('\\').join('/').replace(/^\.\//, '').replace(/\/$/, '');
  if (path.posix.isAbsolute(normalized) || normalized === '..' || normalized.startsWith('../')) {
    throw new Error(`${label} must stay inside the project root: ${value}`);
  }
  const absolute = path.resolve(root, normalized);
  const relative = path.relative(root, absolute).split(path.sep).join('/');
  if (!relative || relative === '..' || relative.startsWith('../')) {
    throw new Error(`${label} must name a project subdirectory: ${value}`);
  }
  return relative;
}

function repositoryRelativePath(root, candidate) {
  const relative = path.relative(root, candidate).split(path.sep).join('/');
  if (!relative || relative === '..' || relative.startsWith('../')) {
    return '';
  }
  return relative;
}

function isCanonicalPathInsideRoot(root, candidate) {
  const canonicalRoot = fs.realpathSync(root);
  const canonicalCandidate = fs.realpathSync(candidate);
  const relative = path.relative(canonicalRoot, canonicalCandidate);
  return relative === '' || (relative !== '..' && !relative.startsWith(`..${path.sep}`) && !path.isAbsolute(relative));
}

function isConfiguredModuleReadme(scope, relative) {
  return relative.endsWith('/README.md') && (
    relative.startsWith(`${scope.paths.backend}/`)
      || relative.startsWith(`${scope.paths.frontend}/`)
  );
}
