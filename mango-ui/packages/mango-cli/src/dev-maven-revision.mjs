import { basename } from 'node:path';

const MAVEN_COMMAND_NAMES = new Set(['mvn', 'mvn.cmd', 'mvnw', 'mvnw.cmd']);

export function buildWorkspaceMavenRevisionQualifier(workspaceId) {
  const qualifier = String(workspaceId || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  if (!qualifier) {
    throw new Error('workspace id is required to isolate Maven SNAPSHOT coordinates');
  }
  return qualifier;
}

export function qualifyWorkspaceMavenRevision(baseRevision, qualifier) {
  const revision = String(baseRevision || '').trim();
  const normalizedQualifier = String(qualifier || '').trim();
  if (!revision || !normalizedQualifier) {
    throw new Error('Maven base revision and workspace qualifier are required');
  }
  if (revision.includes('${') || revision.includes('}')) {
    throw new Error(`Maven revision must be a concrete value, received: ${revision}`);
  }
  const suffix = `-${normalizedQualifier}-SNAPSHOT`;
  if (revision.endsWith(suffix)) {
    return revision;
  }
  if (revision.endsWith('-SNAPSHOT')) {
    return `${revision.slice(0, -'-SNAPSHOT'.length)}${suffix}`;
  }
  return `${revision}${suffix}`;
}

export function readCiFriendlyMavenRevision(pomText) {
  const text = String(pomText || '');
  const projectModel = text.replace(/<parent\b[^>]*>[\s\S]*?<\/parent>/u, '');
  if (!/<artifactId>\s*[^<]+\s*<\/artifactId>\s*<version>\s*\$\{revision\}\s*<\/version>/u.test(projectModel)) {
    return '';
  }
  return text.match(/<revision>\s*([^<]+?)\s*<\/revision>/u)?.[1]?.trim() || '';
}

export function isMavenCommand(command) {
  return MAVEN_COMMAND_NAMES.has(basename(String(command || '')).toLowerCase());
}

export function injectMavenRevisionArgs(args, revision) {
  const property = `-Drevision=${revision}`;
  let replaced = false;
  const nextArgs = (args || []).map((arg) => {
    if (String(arg).startsWith('-Drevision=')) {
      replaced = true;
      return property;
    }
    return arg;
  });
  return replaced ? nextArgs : [property, ...nextArgs];
}
