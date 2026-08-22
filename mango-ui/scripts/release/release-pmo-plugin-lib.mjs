export const PMO_PLUGIN_MANIFEST_PATH = 'mango-pmo/plugin-src/.codex-plugin/plugin.json';

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
