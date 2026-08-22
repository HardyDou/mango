export const CLI_README_PATH = 'mango-ui/packages/mango-cli/README.md';

export function projectCliReadmeVersion(content, sourceVersion, targetVersion) {
  if (!sourceVersion || !targetVersion) throw new Error('CLI README projection requires source and target versions');

  const replacements = [
    {
      name: 'current release version',
      pattern: new RegExp(
        `(\\| 当前发布版本[ \\t]+\\|[ \\t]+\`)${escapeRegExp(sourceVersion)}(\`)([ \\t]+)(\\|)`,
        'gu',
      ),
      replacement: (_match, prefix, closingBacktick, padding, closingPipe) => {
        const paddingWidth = padding.length + sourceVersion.length - targetVersion.length;
        if (paddingWidth < 1) {
          throw new Error('CLI README current release version cannot preserve the Markdown table width');
        }
        return `${prefix}${targetVersion}${closingBacktick}${' '.repeat(paddingWidth)}${closingPipe}`;
      },
    },
    {
      name: 'npm view command',
      pattern: new RegExp(`npm view @mango/cli@${escapeRegExp(sourceVersion)} version --registry`, 'gu'),
      replacement: `npm view @mango/cli@${targetVersion} version --registry`,
    },
    {
      name: 'npm install command',
      pattern: new RegExp(`npm install -g @mango/cli@${escapeRegExp(sourceVersion)} --registry`, 'gu'),
      replacement: `npm install -g @mango/cli@${targetVersion} --registry`,
    },
  ];

  let projected = content;
  for (const { name, pattern, replacement } of replacements) {
    const matches = [...projected.matchAll(pattern)];
    if (matches.length !== 1) {
      throw new Error(`CLI README ${name} must contain exactly one source version ${sourceVersion}`);
    }
    projected = projected.replace(pattern, replacement);
  }
  return projected;
}

export function assertCliReadmeProjection({ sourceContent, projectedContent, sourceVersion, targetVersion }) {
  const expected = projectCliReadmeVersion(sourceContent, sourceVersion, targetVersion);
  if (projectedContent !== expected) {
    throw new Error('CLI README differs from the deterministic release version projection');
  }
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&');
}
