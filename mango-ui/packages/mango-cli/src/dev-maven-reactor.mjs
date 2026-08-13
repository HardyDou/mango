import { existsSync, readFileSync } from 'node:fs';
import { basename, dirname, isAbsolute, join, relative, resolve } from 'node:path';

const SPRING_BOOT_PLUGIN_ARTIFACT_ID = 'spring-boot-maven-plugin';

export function resolveSpringBootMavenReactor({ workspaceRoot, appPomPath }) {
  const normalizedWorkspaceRoot = resolve(workspaceRoot);
  const normalizedAppPom = resolve(appPomPath);
  if (!isPathInside(normalizedAppPom, normalizedWorkspaceRoot) || !existsSync(normalizedAppPom)) {
    throw new Error(`Spring Boot app POM must exist inside the workspace: ${normalizedAppPom}`);
  }

  const rootPom = findCiFriendlyReactorPom(normalizedWorkspaceRoot, normalizedAppPom);
  if (!rootPom) {
    throw new Error(
      'Maven project does not use CI-friendly ${revision}: reactor root must declare <version>${revision}</version> with a concrete <revision> value',
    );
  }

  const rootPomText = readFileSync(rootPom, 'utf8');
  const appPomText = readFileSync(normalizedAppPom, 'utf8');
  const artifactId = readProjectArtifactId(appPomText);
  if (!artifactId || artifactId.includes('${')) {
    throw new Error(`Spring Boot app POM must declare a concrete artifactId: ${normalizedAppPom}`);
  }

  const reactorProjects = collectReactorProjects(rootPom);
  const matchingProjects = reactorProjects.filter((project) => project.artifactId === artifactId);
  if (!reactorProjects.some((project) => project.pomPath === normalizedAppPom)) {
    throw new Error(`Spring Boot app POM is not a module of reactor ${rootPom}: ${normalizedAppPom}`);
  }
  if (matchingProjects.length !== 1) {
    throw new Error(
      `Spring Boot app artifactId must be unique in reactor ${rootPom}: ${artifactId} matched ${matchingProjects.length} projects`,
    );
  }

  assertSpringBootSkipContract({ rootPom, rootPomText, appPomPath: normalizedAppPom, appPomText });
  return {
    rootPom,
    cwd: dirname(rootPom),
    appPom: normalizedAppPom,
    artifactId,
    selector: `:${artifactId}`,
  };
}

export function buildSpringBootReactorArgs({ rootPom, selector, revision, springArgs, goal }) {
  return [
    '-f',
    basename(rootPom),
    '-pl',
    selector,
    '-am',
    '-DskipTests',
    `-Drevision=${revision}`,
    `-Dspring-boot.run.arguments=${springArgs.join(' ')}`,
    'compile',
    goal,
  ];
}

export function readProjectArtifactId(pomText) {
  const projectModel = String(pomText || '').replace(/<parent\b[^>]*>[\s\S]*?<\/parent>/u, '');
  const coordinates = projectModel.split(
    /<(?:properties|modules|dependencies|dependencyManagement|build|profiles|repositories|pluginRepositories|distributionManagement)\b/u,
    1,
  )[0];
  return coordinates.match(/<artifactId>\s*([^<]+?)\s*<\/artifactId>/u)?.[1]?.trim() || '';
}

export function readMavenModules(pomText) {
  const modules = String(pomText || '').match(/<modules\b[^>]*>([\s\S]*?)<\/modules>/u)?.[1] || '';
  return [...modules.matchAll(/<module>\s*([^<]+?)\s*<\/module>/gu)].map((match) => match[1].trim());
}

function findCiFriendlyReactorPom(workspaceRoot, appPomPath) {
  let directory = dirname(appPomPath);
  while (isPathInside(directory, workspaceRoot)) {
    const pomPath = join(directory, 'pom.xml');
    if (existsSync(pomPath) && readCiFriendlyRevision(readFileSync(pomPath, 'utf8'))) {
      return resolve(pomPath);
    }
    if (directory === workspaceRoot || dirname(directory) === directory) {
      break;
    }
    directory = dirname(directory);
  }
  return '';
}

function collectReactorProjects(rootPom) {
  const projects = [];
  const visited = new Set();
  const visit = (pomPath) => {
    const normalizedPom = resolve(pomPath);
    if (visited.has(normalizedPom) || !existsSync(normalizedPom)) {
      return;
    }
    visited.add(normalizedPom);
    const pomText = readFileSync(normalizedPom, 'utf8');
    projects.push({ pomPath: normalizedPom, artifactId: readProjectArtifactId(pomText) });
    for (const moduleName of readMavenModules(pomText)) {
      if (moduleName.includes('${')) {
        throw new Error(`Maven reactor module path must be concrete: ${moduleName} in ${normalizedPom}`);
      }
      const modulePath = resolve(dirname(normalizedPom), moduleName);
      visit(modulePath.endsWith('.xml') ? modulePath : join(modulePath, 'pom.xml'));
    }
  };
  visit(rootPom);
  return projects;
}

function assertSpringBootSkipContract({ rootPom, rootPomText, appPomPath, appPomText }) {
  const rootProperties = readPomProperties(rootPomText);
  const appProperties = { ...rootProperties, ...readPomProperties(appPomText) };
  const inheritedSkip = readSpringBootPluginSkip(rootPomText);
  const appSkip = readSpringBootPluginSkip(appPomText) || inheritedSkip;
  if (resolvePomValue(inheritedSkip, rootProperties) !== 'true') {
    throw new Error(
      `Reactor root must configure ${SPRING_BOOT_PLUGIN_ARTIFACT_ID} with skip=true so upstream modules do not run: ${rootPom}`,
    );
  }
  if (resolvePomValue(appSkip, appProperties) !== 'false') {
    throw new Error(`Target app must override ${SPRING_BOOT_PLUGIN_ARTIFACT_ID} with skip=false: ${appPomPath}`);
  }
}

function readSpringBootPluginSkip(pomText) {
  const plugins = [...String(pomText || '').matchAll(/<plugin\b[^>]*>([\s\S]*?)<\/plugin>/gu)];
  for (const plugin of plugins) {
    if (!new RegExp(`<artifactId>\\s*${SPRING_BOOT_PLUGIN_ARTIFACT_ID}\\s*</artifactId>`, 'u').test(plugin[1])) {
      continue;
    }
    return plugin[1].match(/<skip>\s*([^<]+?)\s*<\/skip>/u)?.[1]?.trim() || '';
  }
  return '';
}

function readPomProperties(pomText) {
  const body = String(pomText || '').match(/<properties\b[^>]*>([\s\S]*?)<\/properties>/u)?.[1] || '';
  return Object.fromEntries(
    [...body.matchAll(/<([A-Za-z0-9_.-]+)>\s*([^<]*?)\s*<\/\1>/gu)].map((match) => [match[1], match[2].trim()]),
  );
}

function resolvePomValue(value, properties) {
  const normalized = String(value || '').trim();
  const propertyName = normalized.match(/^\$\{([^}]+)\}$/u)?.[1];
  return String(propertyName ? properties[propertyName] || '' : normalized)
    .trim()
    .toLowerCase();
}

function readCiFriendlyRevision(pomText) {
  const text = String(pomText || '');
  const projectModel = text.replace(/<parent\b[^>]*>[\s\S]*?<\/parent>/u, '');
  if (!/<artifactId>\s*[^<]+\s*<\/artifactId>\s*<version>\s*\$\{revision\}\s*<\/version>/u.test(projectModel)) {
    return '';
  }
  return text.match(/<revision>\s*([^<]+?)\s*<\/revision>/u)?.[1]?.trim() || '';
}

function isPathInside(path, root) {
  const relativePath = relative(resolve(root), resolve(path));
  return relativePath === '' || (!relativePath.startsWith('..') && !isAbsolute(relativePath));
}
