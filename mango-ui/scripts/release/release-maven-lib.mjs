import { createHash } from 'node:crypto';
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

export function hasMavenReleaseImpact(files) {
  return files.some((file) => {
    const normalized = file.replaceAll('\\', '/').replace(/^\.\//u, '');
    if (!normalized.startsWith('mango/')) return false;
    return normalized === 'mango/pom.xml' || /\/pom\.xml$/u.test(normalized) || /\/src\/main\//u.test(normalized);
  });
}

export function inspectStagedMavenRepository(repositoryRoot, version) {
  if (!existsSync(repositoryRoot)) throw new Error(`staged Maven repository does not exist: ${repositoryRoot}`);
  const files = listFiles(repositoryRoot)
    .filter((path) => !/\.(?:sha1|sha256|sha512|md5|lastUpdated)$/u.test(path))
    .filter((path) => !/(?:^|\/)maven-metadata(?:-[^/]+)?\.xml$/u.test(path));
  const versionFiles = files.filter((path) => path.split('/').includes(version));
  const pomFiles = versionFiles.filter((path) => path.endsWith('.pom'));
  const coordinates = pomFiles.map((pomPath) => inspectCoordinate(repositoryRoot, pomPath, version, versionFiles));
  if (coordinates.length === 0) throw new Error(`staged Maven repository contains no ${version} coordinates`);
  const coordinateFiles = new Set(coordinates.flatMap((entry) => entry.files.map((file) => file.path)));
  const unowned = versionFiles.filter((path) => !coordinateFiles.has(path));
  if (unowned.length > 0) {
    throw new Error(`staged Maven repository contains unsupported release files:\n${unowned.join('\n')}`);
  }
  coordinates.sort((left, right) => left.coordinate.localeCompare(right.coordinate));
  const sealedFiles = coordinates
    .flatMap((entry) => entry.files)
    .sort((left, right) => left.path.localeCompare(right.path));
  return {
    version,
    coordinateCount: coordinates.length,
    fileCount: sealedFiles.length,
    repositorySha256: digestFileList(sealedFiles),
    coordinates,
  };
}

export function verifyStagedMavenRepository(repositoryRoot, descriptor) {
  const actual = inspectStagedMavenRepository(repositoryRoot, descriptor.version);
  if (actual.repositorySha256 !== descriptor.repositorySha256) {
    throw new Error('sealed Maven repository digest mismatch');
  }
  return actual;
}

export function decideMavenCoordinateAction({ publishFiles, consumeFiles, expectedFiles }) {
  const publish = summarizeRole(publishFiles, expectedFiles);
  const consume = summarizeRole(consumeFiles, expectedFiles);
  if (publish.state === 'unknown' || consume.state === 'unknown') {
    return { action: 'STOP', reason: 'Maven registry state is unknown' };
  }
  if (publish.state === 'partial' || consume.state === 'partial') {
    return { action: 'STOP', reason: 'Maven coordinate is only partially present' };
  }
  if (publish.state === 'mismatch' || consume.state === 'mismatch') {
    return { action: 'STOP', reason: 'Maven registry content differs from the sealed files' };
  }
  if (publish.state === 'absent' && consume.state === 'present') {
    return { action: 'STOP', reason: 'Maven consume registry contains a coordinate absent from publish registry' };
  }
  if (publish.state === 'present' && consume.state === 'present') {
    return { action: 'VERIFIED', reason: 'both Maven registry roles contain the sealed coordinate' };
  }
  if (publish.state === 'present') {
    return { action: 'VERIFY_PENDING', reason: 'Maven publish registry is complete; consume registry is pending' };
  }
  return { action: 'PUBLISH', reason: 'both Maven registry roles prove the coordinate is absent' };
}

function inspectCoordinate(repositoryRoot, pomPath, version, repositoryFiles) {
  const parts = pomPath.split('/');
  const versionIndex = parts.lastIndexOf(version);
  if (versionIndex < 2) throw new Error(`cannot derive Maven coordinate from ${pomPath}`);
  const artifactId = parts[versionIndex - 1];
  const groupId = parts.slice(0, versionIndex - 1).join('.');
  const prefix = `${parts.slice(0, versionIndex + 1).join('/')}/${artifactId}-${version}`;
  const candidates = repositoryFiles.filter(
    (path) => path === `${prefix}.pom` || path.startsWith(`${prefix}-`) || path === `${prefix}.jar`,
  );
  const unsupported = candidates.filter((path) => !path.endsWith('.pom') && !path.endsWith('.jar'));
  const classifiers = candidates.filter((path) => path !== `${prefix}.pom` && path !== `${prefix}.jar`);
  if (unsupported.length > 0 || classifiers.length > 0) {
    throw new Error(`unsupported classified Maven artifacts for ${groupId}:${artifactId}:${version}`);
  }
  const pom = `${prefix}.pom`;
  const jar = `${prefix}.jar`;
  if (!candidates.includes(pom))
    throw new Error(`staged Maven coordinate is missing POM: ${groupId}:${artifactId}:${version}`);
  const packaging = candidates.includes(jar) ? 'jar' : 'pom';
  const selected = packaging === 'jar' ? [pom, jar] : [pom];
  return {
    coordinate: `${groupId}:${artifactId}:${version}`,
    groupId,
    artifactId,
    version,
    packaging,
    files: selected.map((path) => describeFile(repositoryRoot, path)),
  };
}

function describeFile(root, path) {
  const absolute = join(root, path);
  return { path, sha256: sha256File(absolute), size: statSync(absolute).size };
}

function listFiles(root) {
  const files = [];
  visit(root);
  return files.sort();

  function visit(path) {
    for (const entry of readdirSync(path, { withFileTypes: true })) {
      const absolute = join(path, entry.name);
      if (entry.isDirectory()) visit(absolute);
      else if (entry.isFile()) files.push(relative(root, absolute).replaceAll('\\', '/'));
    }
  }
}

function summarizeRole(actualFiles, expectedFiles) {
  if (!Array.isArray(actualFiles) || actualFiles.some((entry) => entry.state === 'unknown'))
    return { state: 'unknown' };
  const present = actualFiles.filter((entry) => entry.state === 'present');
  if (present.length === 0) return { state: 'absent' };
  if (present.length !== expectedFiles.length) return { state: 'partial' };
  for (const expected of expectedFiles) {
    const actual = present.find((entry) => entry.path === expected.path);
    if (!actual || actual.sha256 !== expected.sha256) return { state: 'mismatch' };
  }
  return { state: 'present' };
}

function digestFileList(files) {
  const value = files.map((entry) => `${entry.path}\0${entry.sha256}\0${entry.size}`).join('\n');
  return createHash('sha256').update(value).digest('hex');
}

function sha256File(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}
