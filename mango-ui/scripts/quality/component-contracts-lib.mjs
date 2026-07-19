import fs from 'node:fs';
import path from 'node:path';

const LEVELS = new Set(['C3', 'C4']);
const HOST_PROFILES = new Set(['host-agnostic', 'mango-runtime', 'admin-shell']);
const ENVIRONMENT_PROFILES = new Set(['universal', 'browser-only']);
const DISTRIBUTIONS = new Set(['workspace', 'npm']);
const DEPLOYMENT_MODES = new Set(['monolith', 'microfrontend']);

function identity(packageName, item) {
  return `${packageName}:${item.exportKey}:${item.exportName}`;
}

function requireString(value, label, failures) {
  if (typeof value !== 'string' || value.trim() === '') failures.push(`${label} must be a non-empty string`);
}

function requireStringArray(value, label, allowed, failures, { allowEmpty = false } = {}) {
  if (!Array.isArray(value) || (!allowEmpty && value.length === 0)) {
    failures.push(`${label} must be ${allowEmpty ? 'an' : 'a non-empty'} array`);
    return;
  }
  const seen = new Set();
  for (const entry of value) {
    if (typeof entry !== 'string' || entry.trim() === '') {
      failures.push(`${label} must contain only non-empty strings`);
      continue;
    }
    if (seen.has(entry)) failures.push(`${label} contains duplicate value: ${entry}`);
    seen.add(entry);
    if (allowed && !allowed.has(entry)) failures.push(`${label} contains unsupported value: ${entry}`);
  }
}

function exportedVueIdentities(inventory) {
  return new Set(
    (inventory.publicVueExports || []).map((item) =>
      identity(item.workspace, { exportKey: item.exportKey, exportName: item.exportName }),
    ),
  );
}

function manifestExportKeys(manifest) {
  return new Set(Object.keys(manifest.exports || {}));
}

function architectureExportKeys(manifest, field) {
  return new Set(Object.keys(manifest.mangoArchitecture?.[field] || {}));
}

function validateComponent(registry, component, manifest, registryDirectory, failures) {
  const prefix = `${identity(registry.packageName, component)} component`;
  requireString(component.name, `${prefix}.name`, failures);
  requireString(component.exportKey, `${prefix}.exportKey`, failures);
  requireString(component.exportName, `${prefix}.exportName`, failures);
  if (!LEVELS.has(component.level)) failures.push(`${prefix}.level must be C3 or C4`);
  if (!HOST_PROFILES.has(component.hostProfile)) failures.push(`${prefix}.hostProfile is unsupported`);
  if (!ENVIRONMENT_PROFILES.has(component.environmentProfile))
    failures.push(`${prefix}.environmentProfile is unsupported`);
  requireStringArray(component.distribution, `${prefix}.distribution`, DISTRIBUTIONS, failures);
  requireStringArray(component.deploymentModes, `${prefix}.deploymentModes`, DEPLOYMENT_MODES, failures);
  requireString(component.stability, `${prefix}.stability`, failures);
  requireString(component.ownerRole, `${prefix}.ownerRole`, failures);
  requireStringArray(component.styleExportKeys, `${prefix}.styleExportKeys`, null, failures, { allowEmpty: true });

  const exports = manifestExportKeys(manifest);
  const sourceExports = architectureExportKeys(manifest, 'sourceExports');
  const nonCodeExports = architectureExportKeys(manifest, 'nonCodeExports');
  if (!exports.has(component.exportKey))
    failures.push(`${prefix} refers to missing package export ${component.exportKey}`);
  if (!sourceExports.has(component.exportKey)) {
    failures.push(`${prefix} must refer to a mangoArchitecture.sourceExports key: ${component.exportKey}`);
  }
  for (const styleKey of component.styleExportKeys || []) {
    if (!exports.has(styleKey)) failures.push(`${prefix} refers to missing style export ${styleKey}`);
    if (!nonCodeExports.has(styleKey)) {
      failures.push(`${prefix} must refer to a mangoArchitecture.nonCodeExports key: ${styleKey}`);
    }
  }
  if ((component.styleExportKeys || []).length === 0) {
    requireString(component.styleNotApplicableReason, `${prefix}.styleNotApplicableReason`, failures);
  } else if (component.styleNotApplicableReason !== undefined) {
    failures.push(`${prefix}.styleNotApplicableReason is only allowed when styleExportKeys is empty`);
  }

  if (component.level === 'C3') {
    if (manifest.private !== true) failures.push(`${prefix} C3 requires a private workspace package`);
    if (!(component.distribution || []).includes('workspace'))
      failures.push(`${prefix} C3 requires workspace distribution`);
    if ((component.distribution || []).includes('npm')) failures.push(`${prefix} C3 cannot use npm distribution`);
  }
  if (component.level === 'C4') {
    if (!(component.distribution || []).includes('npm')) failures.push(`${prefix} C4 requires npm distribution`);
    if (component.hostProfile === 'admin-shell') failures.push(`${prefix} C4 cannot use admin-shell host profile`);
  }

  if (!component.docs || typeof component.docs !== 'object') {
    failures.push(`${prefix}.docs must be an object`);
  } else {
    requireString(component.docs.path, `${prefix}.docs.path`, failures);
    requireStringArray(component.docs.requiredSections, `${prefix}.docs.requiredSections`, null, failures);
    if (component.docs.path && !fs.existsSync(path.join(registryDirectory, component.docs.path))) {
      failures.push(`${prefix}.docs.path does not exist: ${component.docs.path}`);
    }
  }
  requireStringArray(component.testEvidence, `${prefix}.testEvidence`, null, failures);
  for (const evidence of component.testEvidence || []) {
    if (!fs.existsSync(path.join(registryDirectory, evidence)))
      failures.push(`${prefix} test evidence does not exist: ${evidence}`);
  }
}

function validateLegacy(registry, item, failures) {
  const prefix = `${identity(registry.packageName, item)} legacy`;
  requireString(item.exportKey, `${prefix}.exportKey`, failures);
  requireString(item.exportName, `${prefix}.exportName`, failures);
  requireString(item.ownerRole, `${prefix}.ownerRole`, failures);
  requireString(item.targetPhase, `${prefix}.targetPhase`, failures);
  requireString(item.exitCriteria, `${prefix}.exitCriteria`, failures);
}

export function validateComponentContracts(inventory, registryRecords) {
  const failures = [];
  const inventoryIdentities = exportedVueIdentities(inventory);
  if (inventoryIdentities.size === 0) failures.push('public Vue export inventory is empty');
  if (registryRecords.length === 0) failures.push('component registry input is empty');

  const classified = new Map();
  for (const record of registryRecords) {
    const { registry, manifest, directory } = record;
    if (registry.schemaVersion !== 1)
      failures.push(`${registry.packageName || record.packageName} schemaVersion must be 1`);
    if (registry.packageName !== manifest.name) {
      failures.push(`${record.file} packageName must match manifest name ${manifest.name}`);
    }
    if (!Array.isArray(registry.components)) failures.push(`${manifest.name} components must be an array`);
    if (!Array.isArray(registry.legacyComponentExports)) {
      failures.push(`${manifest.name} legacyComponentExports must be an array`);
    }
    for (const component of registry.components || []) {
      validateComponent(registry, component, manifest, directory, failures);
      const key = identity(registry.packageName, component);
      if (classified.has(key)) failures.push(`component export is classified more than once: ${key}`);
      classified.set(key, 'component');
      if (!inventoryIdentities.has(key))
        failures.push(`component registry refers to a missing public Vue export: ${key}`);
    }
    for (const item of registry.legacyComponentExports || []) {
      validateLegacy(registry, item, failures);
      const key = identity(registry.packageName, item);
      if (classified.has(key)) failures.push(`component export is classified more than once: ${key}`);
      classified.set(key, 'legacy');
      if (!inventoryIdentities.has(key)) failures.push(`legacy registry refers to a missing public Vue export: ${key}`);
    }
  }

  for (const key of inventoryIdentities) {
    if (!classified.has(key)) failures.push(`public Vue export is not classified: ${key}`);
  }
  return failures;
}

export function compareLegacyComponentBaselines(currentRecords, baseRecords) {
  const baseLegacy = new Map();
  for (const { registry } of baseRecords) {
    for (const item of registry.legacyComponentExports || []) {
      baseLegacy.set(identity(registry.packageName, item), item);
    }
  }
  const failures = [];
  for (const { registry } of currentRecords) {
    for (const item of registry.legacyComponentExports || []) {
      const key = identity(registry.packageName, item);
      const base = baseLegacy.get(key);
      if (!base) {
        failures.push(`new legacy component export is not allowed: ${key}`);
        continue;
      }
      for (const field of ['ownerRole', 'targetPhase', 'exitCriteria']) {
        if (item[field] !== base[field]) failures.push(`legacy component ${field} changed: ${key}`);
      }
    }
  }
  return failures;
}

export function readComponentContractRecords(uiRoot) {
  const records = [];
  for (const kind of ['apps', 'packages']) {
    const parent = path.join(uiRoot, kind);
    if (!fs.existsSync(parent)) continue;
    for (const entry of fs.readdirSync(parent, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      const directory = path.join(parent, entry.name);
      const file = path.join(directory, 'component-contracts.json');
      const manifestFile = path.join(directory, 'package.json');
      if (!fs.existsSync(file)) continue;
      if (!fs.existsSync(manifestFile)) throw new Error(`Component registry has no package manifest: ${file}`);
      records.push({
        file,
        directory,
        packageName: entry.name,
        registry: JSON.parse(fs.readFileSync(file, 'utf8')),
        manifest: JSON.parse(fs.readFileSync(manifestFile, 'utf8')),
      });
    }
  }
  return records.sort((left, right) => left.registry.packageName.localeCompare(right.registry.packageName));
}
