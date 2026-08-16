export const CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH =
  'mango-ui/packages/mango-cli/templates/full/frontend/package.json.template';

export function projectCliFullFrontendTemplateVersion(content, sourceVersion, targetVersion) {
  if (!sourceVersion || !targetVersion) {
    throw new Error('CLI full frontend template projection requires source and target versions');
  }
  const pattern = new RegExp(`("@mango/cli"\\s*:\\s*")${escapeRegExp(sourceVersion)}(")`, 'gu');
  const matches = [...content.matchAll(pattern)];
  if (matches.length !== 1) {
    throw new Error(`CLI full frontend template must contain exactly one source version ${sourceVersion}`);
  }
  return content.replace(pattern, `$1${targetVersion}$2`);
}

export function assertCliFullFrontendTemplateProjection({
  sourceContent,
  projectedContent,
  sourceVersion,
  targetVersion,
}) {
  const expected = projectCliFullFrontendTemplateVersion(sourceContent, sourceVersion, targetVersion);
  if (projectedContent !== expected) {
    throw new Error('CLI full frontend template differs from the deterministic release version projection');
  }
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&');
}
