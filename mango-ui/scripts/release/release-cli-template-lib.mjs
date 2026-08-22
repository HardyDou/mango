export const CLI_FULL_FRONTEND_PACKAGE_TEMPLATE_PATH =
  'mango-ui/packages/mango-cli/templates/full/frontend/package.json.template';
export const CLI_FULL_README_TEMPLATE_PATH = 'mango-ui/packages/mango-cli/templates/full/README.md';

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

export function projectCliFullReadmeTuple(content, { mavenVersion, pmoVersion, cliVersion }) {
  for (const [label, value] of Object.entries({ mavenVersion, pmoVersion, cliVersion })) {
    if (!/^\d+\.\d+\.\d+$/u.test(value ?? '')) throw new Error(`CLI full README ${label} must be semantic`);
  }
  const sectionPattern = /(### Issue #690 升级合同\n\n)([\s\S]*?)(\n\n生成业务模块时，)/u;
  const match = content.match(sectionPattern);
  if (!match) throw new Error('CLI full README Issue #690 upgrade contract is missing');
  let section = match[2];
  section = replaceCount(
    section,
    /本模板随 Maven `\d+\.\d+\.\d+`/gu,
    `本模板随 Maven \`${mavenVersion}\``,
    1,
  );
  section = replaceCount(section, /@mango\/pmo@\d+\.\d+\.\d+/gu, `@mango/pmo@${pmoVersion}`, 1);
  section = replaceCount(section, /@mango\/cli@\d+\.\d+\.\d+/gu, `@mango/cli@${cliVersion}`, 2);
  section = replaceCount(
    section,
    /--to \d+\.\d+\.\d+ --dry-run/gu,
    `--to ${pmoVersion} --dry-run`,
    1,
  );
  section = replaceCount(
    section,
    /(`mango-bom` 保持为 `)\d+\.\d+\.\d+(`)/gu,
    `$1${mavenVersion}$2`,
    1,
  );
  return content.replace(sectionPattern, `$1${section}$3`);
}

export function assertCliFullReadmeProjection({ sourceContent, projectedContent, versions }) {
  const expected = projectCliFullReadmeTuple(sourceContent, versions);
  if (projectedContent !== expected) {
    throw new Error('CLI full README differs from the deterministic release tuple projection');
  }
}

function replaceCount(content, pattern, replacement, expectedCount) {
  const matches = [...content.matchAll(pattern)];
  if (matches.length !== expectedCount) {
    throw new Error(`CLI full README projection expected ${expectedCount} match(es), found ${matches.length}`);
  }
  return content.replace(pattern, replacement);
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&');
}
