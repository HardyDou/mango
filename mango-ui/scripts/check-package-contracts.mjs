#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptFile = fileURLToPath(import.meta.url);
const repoRoot = resolve(scriptFile, '../..');
const packagesRoot = join(repoRoot, 'packages');
const errors = [];
const builtInCapabilityNames = ['auth', 'calendar', 'file', 'notice', 'numgen', 'rbac', 'system', 'template', 'workflow'];

const packageDirs = readdirSync(packagesRoot)
  .map((entry) => join(packagesRoot, entry))
  .filter((packageDir) => existsSync(join(packageDir, 'package.json')));

for (const packageDir of packageDirs) {
  const packageJson = JSON.parse(readFileSync(join(packageDir, 'package.json'), 'utf8'));
  if (packageJson.name === 'create-mango-app') {
    continue;
  }
  checkPackage(packageDir, packageJson);
  checkMaterialProductization(packageDir, packageJson);
}

checkBuiltInApiAdminPairs();
checkAdminPagesDefaultCapabilities();
checkCapabilityDependencyGraph();
scanForForbiddenSourcePaths(packagesRoot);

if (errors.length > 0) {
  console.error('Package contract check failed:');
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

console.log(`Package contract check passed: ${packageDirs.length - 1} packages checked.`);

function checkPackage(packageDir, packageJson) {
  const label = packageJson.name || relative(repoRoot, packageDir);
  assert(packageJson.type === 'module', `${label} must declare "type": "module"`);
  assert(packageJson.main === './dist/index.js', `${label} main must be ./dist/index.js`);
  assert(packageJson.module === './dist/index.js', `${label} module must be ./dist/index.js`);
  assert(packageJson.types === './dist/index.d.ts', `${label} types must be ./dist/index.d.ts`);
  assert(packageJson.exports && typeof packageJson.exports === 'object', `${label} must declare exports`);
  assert(Array.isArray(packageJson.files), `${label} must declare publish files`);
  assert(packageJson.files.length === 1 && packageJson.files[0] === 'dist', `${label} publish files must be ["dist"]`);
  assert(!JSON.stringify(packageJson).includes('workspace:*'), `${label} must not publish workspace:* dependency ranges`);
  assert(!JSON.stringify(packageJson).includes('"src/'), `${label} package.json must not expose src entries`);

  const rootExport = packageJson.exports['.'];
  assert(rootExport?.import === './dist/index.js', `${label} root export import must be ./dist/index.js`);
  assert(rootExport?.types === './dist/index.d.ts', `${label} root export types must be ./dist/index.d.ts`);
  for (const [subpath, target] of Object.entries(packageJson.exports)) {
    if (subpath === '.') {
      continue;
    }
    if (target?.style) {
      assert(target.style.startsWith('./dist/'), `${label} ${subpath} export style must point to dist`);
      assert(/\.(css|scss)$/.test(target.style), `${label} ${subpath} export style must point to css or scss`);
      assert(target.import === target.style, `${label} ${subpath} export import must match style entry`);
      continue;
    }
    assert(target?.import?.startsWith('./dist/'), `${label} ${subpath} export import must point to dist`);
    assert(target?.import?.endsWith('.js'), `${label} ${subpath} export import must point to js`);
    assert(target?.types?.startsWith('./dist/'), `${label} ${subpath} export types must point to dist`);
    assert(target?.types?.endsWith('.d.ts'), `${label} ${subpath} export types must point to d.ts`);
  }
}

function checkMaterialProductization(packageDir, packageJson) {
  const packageName = packageJson.name || '';
  const label = packageName || relative(repoRoot, packageDir);
  if (!packageName.startsWith('@mango/')) {
    return;
  }

  const shortName = packageName.replace('@mango/', '');
  const sourceDir = join(packageDir, 'src');
  const hasCapability = existsSync(join(sourceDir, 'capability.ts'));
  const isApiPackage = shortName.endsWith('-api');
  const isAdminPackage = shortName === 'admin' || shortName.endsWith('-admin');
  const isRuntimePackage = ['admin-shell', 'admin-pages', 'app-runtime'].includes(shortName);
  const isUtilityPackage = ['common', 'api-schema'].includes(shortName);
  const isExampleExtensionPackage = ['workflow-business-example'].includes(shortName);

  if (isApiPackage) {
    assert(!hasCapability, `${label} API package must not export admin capability`);
    assert(!existsSync(join(sourceDir, 'views')), `${label} API package must not contain admin views`);
    assert(!dependsOn(packageJson, '@mango/admin'), `${label} API package must not depend on @mango/admin`);
    assert(!dependsOn(packageJson, '@mango/admin-shell'), `${label} API package must not depend on @mango/admin-shell`);
    assert(!dependsOn(packageJson, '@mango/admin-pages'), `${label} API package must not depend on @mango/admin-pages`);
    assert(!dependsOn(packageJson, '@mango/app-runtime'), `${label} API package must not depend on @mango/app-runtime`);
    assert(!dependsOn(packageJson, 'element-plus'), `${label} API package must not depend on element-plus`);
    assert(!dependsOn(packageJson, '@element-plus/icons-vue'), `${label} API package must not depend on @element-plus/icons-vue`);
    assert(!JSON.stringify(packageJson.exports || {}).includes('capability'), `${label} API package must not export capability`);
    assertNoForbiddenApiSourceImports(packageDir, label);
  }

  if (isAdminPackage) {
    assert(hasCapability || shortName === 'admin', `${label} admin package must provide src/capability.ts or be the admin runtime`);
    if (shortName.endsWith('-admin')) {
      const apiPeer = `@mango/${shortName.replace(/-admin$/, '-api')}`;
      assert(dependsOn(packageJson, apiPeer), `${label} admin package must depend on ${apiPeer}`);
    }
  }

  if (hasCapability) {
    assert(
      packageJson.exports?.['./capability']?.import === './dist/capability.js',
      `${label} with src/capability.ts must export ./capability import as ./dist/capability.js`,
    );
    assert(
      packageJson.exports?.['./capability']?.types === './dist/capability.d.ts',
      `${label} with src/capability.ts must export ./capability types as ./dist/capability.d.ts`,
    );
    checkCapabilityManifest(packageDir, packageJson, { allowWrapper: shortName.endsWith('-admin') });
  }

  if (!isApiPackage && !isAdminPackage && !isRuntimePackage && !isUtilityPackage && !isExampleExtensionPackage && existsSync(join(sourceDir, 'views'))) {
    assert(
      hasCapability,
      `${label} contains admin views and must provide src/capability.ts during the mixed-package transition`,
    );
  }
}

function checkBuiltInApiAdminPairs() {
  for (const capabilityName of builtInCapabilityNames) {
    const apiDir = join(packagesRoot, `${capabilityName}-api`);
    const adminDir = join(packagesRoot, `${capabilityName}-admin`);
    assert(existsSync(join(apiDir, 'package.json')), `Missing built-in API package @mango/${capabilityName}-api`);
    assert(existsSync(join(adminDir, 'package.json')), `Missing built-in admin package @mango/${capabilityName}-admin`);
  }
}

function checkAdminPagesDefaultCapabilities() {
  const defaultsPath = join(packagesRoot, 'admin-pages/src/defaults.ts');
  if (!existsSync(defaultsPath)) {
    return;
  }
  const text = readFileSync(defaultsPath, 'utf8');
  for (const capabilityName of builtInCapabilityNames) {
    assert(
      text.includes(`@mango/${capabilityName}-admin/capability`),
      `@mango/admin-pages defaults must import @mango/${capabilityName}-admin/capability`,
    );
    assert(
      !text.includes(`@mango/${capabilityName}/capability`),
      `@mango/admin-pages defaults must not import legacy @mango/${capabilityName}/capability`,
    );
  }
}

function checkCapabilityDependencyGraph() {
  const capabilitySources = new Map();
  for (const capabilityName of builtInCapabilityNames) {
    const capabilityPath = join(packagesRoot, `${capabilityName}/src/capability.ts`);
    if (!existsSync(capabilityPath)) {
      continue;
    }
    const text = readFileSync(capabilityPath, 'utf8');
    const capabilityCode = readStringProperty(text, 'capabilityCode');
    const requires = extractStringArrayProperty(text, 'requires');
    const optional = extractStringArrayProperty(text, 'optional');
    const conflicts = extractStringArrayProperty(text, 'conflicts');
    assert(capabilityCode === capabilityName, `${relative(repoRoot, capabilityPath)} capabilityCode must be ${capabilityName}`);
    capabilitySources.set(capabilityCode, { path: capabilityPath, requires, optional, conflicts });
  }

  for (const [capabilityCode, manifest] of capabilitySources.entries()) {
    for (const dependencyCode of [...manifest.requires, ...manifest.optional]) {
      assert(
        capabilitySources.has(dependencyCode),
        `${relative(repoRoot, manifest.path)} ${capabilityCode} references unknown dependency ${dependencyCode}`,
      );
    }
    for (const conflictCode of manifest.conflicts) {
      assert(
        capabilitySources.has(conflictCode),
        `${relative(repoRoot, manifest.path)} ${capabilityCode} references unknown conflict ${conflictCode}`,
      );
    }
  }

  const visiting = [];
  const visited = new Set();
  const visit = (capabilityCode) => {
    if (visiting.includes(capabilityCode)) {
      errors.push(`Built-in capability dependency graph contains cycle: ${[...visiting.slice(visiting.indexOf(capabilityCode)), capabilityCode].join(' -> ')}`);
      return;
    }
    if (visited.has(capabilityCode)) {
      return;
    }
    visiting.push(capabilityCode);
    const manifest = capabilitySources.get(capabilityCode);
    for (const dependencyCode of manifest?.requires || []) {
      if (capabilitySources.has(dependencyCode)) {
        visit(dependencyCode);
      }
    }
    visiting.pop();
    visited.add(capabilityCode);
  };
  for (const capabilityCode of capabilitySources.keys()) {
    visit(capabilityCode);
  }
}

function assertNoForbiddenApiSourceImports(packageDir, label) {
  const sourceDir = join(packageDir, 'src');
  if (!existsSync(sourceDir)) {
    return;
  }
  for (const filePath of walk(sourceDir)) {
    if (!/\.(ts|vue)$/.test(filePath)) {
      continue;
    }
    const source = readFileSync(filePath, 'utf8');
    const display = relative(repoRoot, filePath);
    assert(!source.includes('@mango/admin'), `${label} API source ${display} must not import @mango/admin runtime packages`);
    assert(!source.includes('@mango/admin-shell'), `${label} API source ${display} must not import @mango/admin-shell`);
    assert(!source.includes('@mango/admin-pages'), `${label} API source ${display} must not import @mango/admin-pages`);
    assert(!source.includes('@mango/app-runtime'), `${label} API source ${display} must not import @mango/app-runtime`);
    assert(!source.includes('element-plus'), `${label} API source ${display} must not import element-plus`);
  }
}

function checkCapabilityManifest(packageDir, packageJson, options = {}) {
  const label = packageJson.name || relative(repoRoot, packageDir);
  const capabilityPath = join(packageDir, 'src/capability.ts');
  const text = readFileSync(capabilityPath, 'utf8');
  const display = relative(repoRoot, capabilityPath);

  const isWrapper = options.allowWrapper && text.includes('base') && text.includes('...base');

  assert(text.includes('packageName'), `${display} capability must declare packageName`);
  assert(text.includes(`packageName: '${packageJson.name}'`) || text.includes(`packageName: "${packageJson.name}"`), `${display} packageName must match ${label}`);

  if (isWrapper) {
    assert(text.includes('MangoCapabilityManifest'), `${display} wrapper capability must declare MangoCapabilityManifest type`);
    assert(text.includes('pages:') && text.includes('menus:') && text.includes('permissions:') && text.includes('styles:'), `${display} wrapper capability must clone pages, menus, permissions and styles`);
    return;
  }

  assert(text.includes('moduleCode'), `${display} capability must declare moduleCode`);
  assert(text.includes('capabilityCode'), `${display} capability must declare capabilityCode`);
  assert(text.includes('capabilityName'), `${display} capability must declare capabilityName`);
  assert(text.includes('requires'), `${display} capability must declare requires`);
  assert(text.includes('optional'), `${display} capability must declare optional`);
  assert(text.includes('backend'), `${display} capability must declare backend`);
  assert(text.includes('pages'), `${display} capability must declare pages`);
  assert(text.includes('menus'), `${display} capability must declare menus`);
  assert(text.includes('permissions'), `${display} capability must declare permissions`);
  assert(text.includes('styles'), `${display} capability must declare styles`);
  assert(text.includes('runtime'), `${display} capability must declare runtime`);
  assert(text.includes('e2e'), `${display} capability must declare e2e`);
  assert(/component\s*:/.test(text), `${display} capability pages must declare component`);
  assert(/loader\s*:/.test(text), `${display} capability pages must declare loader`);
  assert(/requiredApis\s*:/.test(text), `${display} capability backend must declare requiredApis`);
  assert(/resourceManifest\s*:/.test(text), `${display} capability backend must declare resourceManifest`);
  assert(/modes\s*:/.test(text), `${display} capability runtime must declare modes`);
  assert(/defaultMode\s*:/.test(text), `${display} capability runtime must declare defaultMode`);
  assert(/smoke\s*:/.test(text), `${display} capability e2e must declare smoke`);
  assert(/screenshots\s*:/.test(text), `${display} capability e2e must declare screenshots`);
  assert(/dataChecks\s*:/.test(text), `${display} capability e2e must declare dataChecks`);

  const pageEntries = extractCapabilityPageEntries(text);
  const menuEntriesFromManifest = extractTopLevelArrayObjectEntries(text, 'menus');
  const declaredPermissionList = extractLastStringArrayProperty(text, 'permissions');
  const declaredMenus = new Map();
  for (const entry of menuEntriesFromManifest) {
    const menuCode = readStringProperty(entry, 'menuCode');
    const component = readStringProperty(entry, 'component');
    const moduleCode = readStringProperty(entry, 'moduleCode');
    const source = readStringProperty(entry, 'source');
    const menuPermissions = extractStringArrayProperty(entry, 'permissions');
    assert(menuCode, `${display} menu entry must declare menuCode`);
    assert(moduleCode, `${display} menu "${menuCode || '(unknown)'}" must declare moduleCode`);
    assert(['backend', 'capability', 'business'].includes(source), `${display} menu "${menuCode || '(unknown)'}" source must be backend, capability, or business`);
    assert(menuPermissions.length > 0, `${display} menu "${menuCode || '(unknown)'}" must declare permissions`);
    for (const permission of menuPermissions) {
      assert(declaredPermissionList.includes(permission), `${display} menu "${menuCode}" permission "${permission}" must be listed in top-level permissions`);
    }
    if (menuCode) {
      declaredMenus.set(menuCode, { component, permissions: menuPermissions });
    }
  }

  const menuEntries = pageEntries.filter(entry => /menuCode\s*:/.test(entry));
  for (const entry of menuEntries) {
    const component = readStringProperty(entry, 'component');
    const menuCode = readStringProperty(entry, 'menuCode');
    assert(component, `${display} menu page entry must declare component`);
    if (component && !isExemptPermissionComponent(component)) {
      assert(/permissions\s*:/.test(entry), `${display} menu page "${component}" must declare permissions`);
    }
    assert(declaredMenus.has(menuCode), `${display} page "${component}" menuCode "${menuCode}" must be declared in menus`);
    const pagePermissions = extractStringArrayProperty(entry, 'permissions');
    const menuPermissions = declaredMenus.get(menuCode)?.permissions || [];
    for (const permission of pagePermissions) {
      assert(declaredPermissionList.includes(permission), `${display} page "${component}" permission "${permission}" must be listed in top-level permissions`);
      assert(menuPermissions.includes(permission), `${display} page "${component}" permission "${permission}" must be declared on menu "${menuCode}"`);
    }
  }
}

function extractCapabilityPageEntries(text) {
  const pagesIndex = text.indexOf('pages');
  if (pagesIndex < 0) {
    return [];
  }
  const openIndex = text.indexOf('[', pagesIndex);
  if (openIndex < 0) {
    return [];
  }
  const closeIndex = findMatchingBracket(text, openIndex, '[', ']');
  if (closeIndex < 0) {
    return [];
  }
  const body = text.slice(openIndex + 1, closeIndex);
  const entries = [];
  let depth = 0;
  let start = -1;
  for (let index = 0; index < body.length; index += 1) {
    const char = body[index];
    if (char === '{') {
      if (depth === 0) {
        start = index;
      }
      depth += 1;
    } else if (char === '}') {
      depth -= 1;
      if (depth === 0 && start >= 0) {
        entries.push(body.slice(start, index + 1));
        start = -1;
      }
    }
  }
  return entries;
}

function findMatchingBracket(text, openIndex, openChar, closeChar) {
  let depth = 0;
  let quote = '';
  let escaped = false;
  for (let index = openIndex; index < text.length; index += 1) {
    const char = text[index];
    if (quote) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === quote) {
        quote = '';
      }
      continue;
    }
    if (char === '\'' || char === '"' || char === '`') {
      quote = char;
      continue;
    }
    if (char === openChar) {
      depth += 1;
    } else if (char === closeChar) {
      depth -= 1;
      if (depth === 0) {
        return index;
      }
    }
  }
  return -1;
}

function readStringProperty(text, propertyName) {
  const match = text.match(new RegExp(`${propertyName}\\s*:\\s*['"]([^'"]+)['"]`));
  return match?.[1] || '';
}

function extractTopLevelArrayObjectEntries(text, propertyName) {
  const propertyIndex = findObjectPropertyIndex(text, propertyName);
  if (propertyIndex < 0) {
    return [];
  }
  const openIndex = text.indexOf('[', propertyIndex);
  if (openIndex < 0) {
    return [];
  }
  const closeIndex = findMatchingBracket(text, openIndex, '[', ']');
  if (closeIndex < 0) {
    return [];
  }
  const body = text.slice(openIndex + 1, closeIndex);
  const entries = [];
  let depth = 0;
  let start = -1;
  for (let index = 0; index < body.length; index += 1) {
    const char = body[index];
    if (char === '{') {
      if (depth === 0) {
        start = index;
      }
      depth += 1;
    } else if (char === '}') {
      depth -= 1;
      if (depth === 0 && start >= 0) {
        entries.push(body.slice(start, index + 1));
        start = -1;
      }
    }
  }
  return entries;
}

function extractStringArrayProperty(text, propertyName) {
  const propertyIndex = findObjectPropertyIndex(text, propertyName);
  if (propertyIndex < 0) {
    return [];
  }
  const openIndex = text.indexOf('[', propertyIndex);
  if (openIndex < 0) {
    return [];
  }
  const closeIndex = findMatchingBracket(text, openIndex, '[', ']');
  if (closeIndex < 0) {
    return [];
  }
  const body = text.slice(openIndex + 1, closeIndex);
  return [...body.matchAll(/['"]([^'"]+)['"]/g)].map(match => match[1]);
}

function extractLastStringArrayProperty(text, propertyName) {
  const propertyIndex = findLastObjectPropertyIndex(text, propertyName);
  if (propertyIndex < 0) {
    return [];
  }
  const openIndex = text.indexOf('[', propertyIndex);
  if (openIndex < 0) {
    return [];
  }
  const closeIndex = findMatchingBracket(text, openIndex, '[', ']');
  if (closeIndex < 0) {
    return [];
  }
  const body = text.slice(openIndex + 1, closeIndex);
  return [...body.matchAll(/['"]([^'"]+)['"]/g)].map(match => match[1]);
}

function findObjectPropertyIndex(text, propertyName) {
  const pattern = new RegExp(`(^|[^A-Za-z0-9_$])${propertyName}\\s*:`, 'g');
  const match = pattern.exec(text);
  return match ? match.index + match[1].length : -1;
}

function findLastObjectPropertyIndex(text, propertyName) {
  const pattern = new RegExp(`(^|[^A-Za-z0-9_$])${propertyName}\\s*:`, 'g');
  let lastIndex = -1;
  let match;
  while ((match = pattern.exec(text)) !== null) {
    lastIndex = match.index + match[1].length;
  }
  return lastIndex;
}

function isExemptPermissionComponent(component) {
  return component.startsWith('login/')
    || component.startsWith('profile/')
    || component.startsWith('password/')
    || component.startsWith('debug/');
}

function dependsOn(packageJson, dependencyName) {
  return Boolean(
    packageJson.dependencies?.[dependencyName]
      || packageJson.peerDependencies?.[dependencyName]
      || packageJson.optionalDependencies?.[dependencyName]
      || packageJson.devDependencies?.[dependencyName],
  );
}

function scanForForbiddenSourcePaths(targetDir) {
  for (const filePath of walk(targetDir)) {
    if (!/\.(json|ts|vue|mjs)$/.test(filePath)) {
      continue;
    }
    if (filePath.includes('/dist/') || filePath.includes('/node_modules/')) {
      continue;
    }
    const text = readFileSync(filePath, 'utf8');
    const display = relative(repoRoot, filePath);
    assert(!text.includes('../../../apps/mango-admin'), `${display} references mango-admin app private path`);
    assert(!text.includes('../../../apps/mango-admin-shell'), `${display} references mango-admin-shell app private path`);
    assert(!/@mango\/[^'"]+\/src\//.test(text), `${display} references another package src path`);
  }
}

function walk(dir) {
  const result = [];
  for (const entry of readdirSync(dir)) {
    const fullPath = join(dir, entry);
    const stat = statSync(fullPath);
    if (stat.isDirectory()) {
      if (entry === 'node_modules' || entry === 'dist' || entry === '.mango-build') {
        continue;
      }
      result.push(...walk(fullPath));
    } else {
      result.push(fullPath);
    }
  }
  return result;
}

function assert(condition, message) {
  if (!condition) {
    errors.push(message);
  }
}
