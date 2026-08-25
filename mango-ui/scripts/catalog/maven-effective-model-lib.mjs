import { createHash } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

const SUPPORTED_PACKAGING = new Map([
  ['jar', 'jar'],
  ['maven-plugin', 'jar'],
  ['pom', 'pom'],
]);

export function indexMavenModulePaths(mavenRoot) {
  const modules = new Map();
  for (const pomFile of listPomFiles(mavenRoot)) {
    const project = parseXmlDocument(fs.readFileSync(pomFile, 'utf8'));
    if (project.name !== 'project') throw new Error(`${pomFile}: root element must be project`);
    const artifactId = childText(project, 'artifactId');
    if (!artifactId) throw new Error(`${pomFile}: project artifactId is required`);
    if (modules.has(artifactId)) {
      throw new Error(`duplicate Maven reactor artifactId ${artifactId}: ${modules.get(artifactId)} and ${pomFile}`);
    }
    modules.set(artifactId, toPosix(path.relative(mavenRoot, path.dirname(pomFile))) || '.');
  }
  return modules;
}

export function exportMavenInventory(effectivePomXml, modulePathsByArtifactId) {
  const document = parseXmlDocument(effectivePomXml);
  const projects = document.name === 'projects' ? children(document, 'project') : [document];
  if (projects.length === 0 || projects.some((project) => project.name !== 'project')) {
    throw new Error('Maven Effective Model contains no projects');
  }
  const seenCoordinates = new Set();
  const parsedProjects = [];

  for (const project of projects) {
    const groupId = resolvedChildText(project, 'groupId', 'Maven project groupId');
    const artifactId = resolvedChildText(project, 'artifactId', 'Maven project artifactId');
    const version = resolvedChildText(project, 'version', `${groupId}:${artifactId} version`);
    const packaging = childText(project, 'packaging') || 'jar';
    const extension = SUPPORTED_PACKAGING.get(packaging);
    if (!extension) throw new Error(`${groupId}:${artifactId}:${version}: unsupported packaging ${packaging}`);
    const modulePath = modulePathsByArtifactId.get(artifactId);
    if (!modulePath) throw new Error(`${groupId}:${artifactId}:${version}: reactor module path is unknown`);
    const identity = `${groupId}:${artifactId}:${version}:${packaging}`;
    if (seenCoordinates.has(identity)) throw new Error(`duplicate Maven Effective Model coordinate ${identity}`);
    seenCoordinates.add(identity);

    parsedProjects.push({
      groupId,
      artifactId,
      version,
      packaging,
      extension,
      classifier: null,
      modulePath,
      dependencies: readDependencies(firstChild(project, 'dependencies')),
      dependencyManagement: readDependencies(firstChild(firstChild(project, 'dependencyManagement'), 'dependencies')),
    });
  }

  const reactorArtifacts = new Set(parsedProjects.map((record) => `${record.groupId}:${record.artifactId}`));
  const dependencyManagementByDigest = new Map();
  const publishableCoordinates = [];
  const excludedCoordinates = [];
  for (const parsed of parsedProjects) {
    const dependencies = parsed.dependencies.filter((dependency) =>
      reactorArtifacts.has(`${dependency.groupId}:${dependency.artifactId}`),
    );
    const dependencyManagement = parsed.dependencyManagement.filter((dependency) =>
      reactorArtifacts.has(`${dependency.groupId}:${dependency.artifactId}`),
    );
    const dependencyManagementDigest = digestDependencies(dependencyManagement);
    const record = {
      ...coordinateIdentity(parsed),
      modulePath: parsed.modulePath,
      dependencies,
      dependencyManagementDigest,
    };
    const exclusionReason = excludedReason(parsed.modulePath);
    if (exclusionReason) {
      excludedCoordinates.push({
        ...coordinateIdentity(record),
        modulePath: parsed.modulePath,
        reason: exclusionReason,
      });
    } else {
      dependencyManagementByDigest.set(dependencyManagementDigest, dependencyManagement);
      publishableCoordinates.push(record);
    }
  }

  publishableCoordinates.sort((left, right) => compareUtf8(coordinateKey(left), coordinateKey(right)));
  excludedCoordinates.sort((left, right) => compareUtf8(coordinateKey(left), coordinateKey(right)));
  return {
    schemaVersion: 1,
    sourceModel: 'maven-effective-pom',
    reactorModuleCount: projects.length,
    publishableCoordinateCount: publishableCoordinates.length,
    excludedCoordinateCount: excludedCoordinates.length,
    dependencyManagementSets: [...dependencyManagementByDigest]
      .map(([digest, dependencies]) => ({ digest, dependencies }))
      .sort((left, right) => compareUtf8(left.digest, right.digest)),
    publishableCoordinates,
    excludedCoordinates,
  };
}

function digestDependencies(dependencies) {
  return createHash('sha256').update(JSON.stringify(dependencies)).digest('hex');
}

function readDependencies(container) {
  if (!container) return [];
  const records = children(container, 'dependency').map((dependency) => ({
    groupId: resolvedChildText(dependency, 'groupId', 'dependency groupId'),
    artifactId: resolvedChildText(dependency, 'artifactId', 'dependency artifactId'),
    version: resolvedChildText(dependency, 'version', 'dependency version'),
    type: childText(dependency, 'type') || 'jar',
    classifier: childText(dependency, 'classifier') || null,
    scope: childText(dependency, 'scope') || 'compile',
    optional: childText(dependency, 'optional') === 'true',
  }));
  records.sort((left, right) => compareUtf8(dependencyKey(left), dependencyKey(right)));
  return records;
}

function excludedReason(modulePath) {
  if (modulePath === 'mango-app' || modulePath.startsWith('mango-app/')) return 'deployment-app';
  if (path.posix.basename(modulePath).endsWith('-test')) return 'internal-test-module';
  return null;
}

function coordinateIdentity(record) {
  return {
    groupId: record.groupId,
    artifactId: record.artifactId,
    version: record.version,
    packaging: record.packaging,
    extension: record.extension,
    classifier: null,
  };
}

function coordinateKey(record) {
  return `${record.groupId}:${record.artifactId}:${record.version}:${record.packaging}`;
}

function dependencyKey(record) {
  return [
    record.groupId,
    record.artifactId,
    record.version || '',
    record.type,
    record.classifier || '',
    record.scope,
  ].join(':');
}

function listPomFiles(root) {
  const files = [];
  visit(root);
  return files.sort(compareUtf8);

  function visit(current) {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      if (entry.name === 'target' || entry.name === 'src') continue;
      const absolute = path.join(current, entry.name);
      if (entry.isDirectory()) visit(absolute);
      else if (entry.isFile() && entry.name === 'pom.xml') files.push(absolute);
    }
  }
}

function parseXmlDocument(xml) {
  const source = String(xml);
  if (/<!DOCTYPE/iu.test(source)) throw new Error('DOCTYPE is not supported in Maven Effective Model input');
  const document = { name: '#document', children: [], text: '' };
  const stack = [document];
  const tokens = source.match(/<\?[\s\S]*?\?>|<!--[\s\S]*?-->|<!\[CDATA\[[\s\S]*?\]\]>|<\/?[^>]+>|[^<]+/gu) || [];
  for (const token of tokens) {
    if (token.startsWith('<?') || token.startsWith('<!--')) continue;
    if (token.startsWith('<![CDATA[')) {
      stack.at(-1).text += token.slice(9, -3);
      continue;
    }
    if (token.startsWith('</')) {
      const name = localName(token.slice(2, -1).trim());
      if (stack.length === 1 || stack.at(-1).name !== name) throw new Error(`mismatched XML closing tag ${name}`);
      stack.pop();
      continue;
    }
    if (token.startsWith('<')) {
      const selfClosing = /\/>$/u.test(token);
      const match = token.match(/^<\s*([^\s/>]+)/u);
      if (!match) throw new Error(`invalid XML tag ${token}`);
      const node = { name: localName(match[1]), children: [], text: '' };
      stack.at(-1).children.push(node);
      if (!selfClosing) stack.push(node);
      continue;
    }
    stack.at(-1).text += decodeXml(token);
  }
  if (stack.length !== 1) throw new Error(`unclosed XML tag ${stack.at(-1).name}`);
  if (document.children.length !== 1) throw new Error('XML input must contain exactly one root element');
  return document.children[0];
}

function firstChild(node, name) {
  return node?.children.find((child) => child.name === name) || null;
}

function children(node, name) {
  return node?.children.filter((child) => child.name === name) || [];
}

function childText(node, name) {
  const child = firstChild(node, name);
  return child ? child.text.trim() : '';
}

function requiredChildText(node, name, label) {
  const value = childText(node, name);
  if (!value) throw new Error(`${label} is required`);
  return value;
}

function resolvedChildText(node, name, label) {
  const value = requiredChildText(node, name, label);
  if (value.includes('${')) throw new Error(`${label} is unresolved: ${value}`);
  return value;
}

function localName(name) {
  return name.includes(':') ? name.slice(name.lastIndexOf(':') + 1) : name;
}

function decodeXml(value) {
  return value
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
    .replaceAll('&quot;', '"')
    .replaceAll('&apos;', "'")
    .replaceAll('&amp;', '&');
}

function toPosix(value) {
  return value.split(path.sep).join('/');
}

function compareUtf8(left, right) {
  return Buffer.compare(Buffer.from(left, 'utf8'), Buffer.from(right, 'utf8'));
}
