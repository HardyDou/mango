export const PMO_PLUGIN_MANIFEST_PATH = 'mango-pmo/plugin-src/.codex-plugin/plugin.json';
export const PMO_CONTRACT_PATHS = [
  'mango-pmo/contracts/business-requirements.json',
  'mango-pmo/contracts/implementation-plan.json',
  'mango-pmo/contracts/system-requirements.json',
  'mango-pmo/contracts/technical-design.json',
];
export const PMO_TEXT_VERSION_PATHS = [
  'mango-pmo/README.md',
  'mango-pmo/rules/index.json',
  'mango-pmo/rules/product/05-document-lifecycle.md',
  'mango-pmo/tests/document-contract/fixtures/valid/business-requirements.md',
  'mango-pmo/tests/document-contract/fixtures/valid/implementation-plan.md',
  'mango-pmo/tests/document-contract/fixtures/valid/system-requirements.md',
  'mango-pmo/tests/document-contract/fixtures/valid/technical-design.md',
];
export const PMO_VERSION_PROJECTION_PATHS = [
  PMO_PLUGIN_MANIFEST_PATH,
  ...PMO_CONTRACT_PATHS,
  ...PMO_TEXT_VERSION_PATHS,
];

export function projectPmoPluginVersion(content, sourceVersion, targetVersion) {
  if (!sourceVersion || !targetVersion) throw new Error('PMO plugin projection requires source and target versions');
  const manifest = JSON.parse(content);
  if (manifest.version !== sourceVersion) {
    throw new Error(`PMO plugin source version ${manifest.version ?? '<missing>'} != ${sourceVersion}`);
  }
  manifest.version = targetVersion;
  return `${JSON.stringify(manifest, null, 2)}\n`;
}

export function assertPmoPluginProjection({ sourceContent, projectedContent, sourceVersion, targetVersion }) {
  const expected = projectPmoPluginVersion(sourceContent, sourceVersion, targetVersion);
  if (projectedContent !== expected) {
    throw new Error('PMO plugin manifest differs from the deterministic release version projection');
  }
}

export function projectPmoVersionedFile(path, content, sourceVersion, targetVersion) {
  if (path === PMO_PLUGIN_MANIFEST_PATH) return projectPmoPluginVersion(content, sourceVersion, targetVersion);
  if (PMO_CONTRACT_PATHS.includes(path)) {
    const contract = JSON.parse(content);
    if (contract.metadata?.fixed?.pmoVersion !== sourceVersion) {
      throw new Error(`${path}: fixed PMO source version must equal ${sourceVersion}`);
    }
    contract.metadata.fixed.pmoVersion = targetVersion;
    const historical = contract.metadata.historicalPmoVersions;
    if (!Array.isArray(historical)) throw new Error(`${path}: historicalPmoVersions is missing`);
    if (!historical.includes(sourceVersion)) historical.push(sourceVersion);
    return `${JSON.stringify(contract, null, 2)}\n`;
  }
  if (!PMO_TEXT_VERSION_PATHS.includes(path)) throw new Error(`unsupported PMO version projection path: ${path}`);
  const matches = content.split(sourceVersion).length - 1;
  if (matches === 0) throw new Error(`${path}: PMO source version ${sourceVersion} is missing`);
  let projected = content.split(sourceVersion).join(targetVersion);
  if (path === 'mango-pmo/rules/product/05-document-lifecycle.md') {
    const historicalRange = /(`1\.3\.6` 至 `)(\d+\.\d+\.\d+)(` 为历史版本)/u;
    if (!historicalRange.test(projected)) throw new Error(`${path}: historical PMO version range is missing`);
    projected = projected.replace(historicalRange, `$1${sourceVersion}$3`);
  }
  return projected;
}

export function assertPmoVersionedFileProjection({ path, sourceContent, projectedContent, sourceVersion, targetVersion }) {
  const expected = projectPmoVersionedFile(path, sourceContent, sourceVersion, targetVersion);
  if (projectedContent !== expected) throw new Error(`${path}: PMO release version projection differs`);
}
