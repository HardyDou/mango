import {
  chmodSync,
  cpSync,
  existsSync,
  lstatSync,
  mkdirSync,
  mkdtempSync,
  readdirSync,
  readFileSync,
  realpathSync,
  rmSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { tmpdir } from 'node:os';
import { join, relative, resolve } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const packageRoot = resolve(fileURLToPath(new URL('..', import.meta.url)));
const cli = join(packageRoot, 'src/index.mjs');
const pmoPackageRoot = resolve(packageRoot, '../mango-pmo');
const cliPackage = JSON.parse(readFileSync(join(packageRoot, 'package.json'), 'utf8'));
const releaseVersions = JSON.parse(readFileSync(join(packageRoot, 'release-versions.json'), 'utf8'));
const packagedAdminModules = JSON.parse(readFileSync(join(packageRoot, 'admin-modules.json'), 'utf8'));
const sourceAdminModules = JSON.parse(readFileSync(join(packageRoot, '../admin/admin-modules.json'), 'utf8'));
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-cli-'));
process.env.MANGO_WORKSPACE_REGISTRY = join(tempRoot, 'workspaces.json');
const fullProjectName = 'mango-full-acceptance';
const customProjectName = 'mango-custom-acceptance';
const customNoneProjectName = 'mango-custom-none-acceptance';
const expectedMavenRepository = 'https://nexus.inner.yunxinbaokeji.com/repository/maven-public/';

function shouldUseShellForCommand(command) {
  return process.platform === 'win32' && /\.cmd$/iu.test(command);
}

function hasCommand(command, args = ['--version']) {
  const result = spawnSync(command, args, {
    stdio: 'ignore',
    shell: shouldUseShellForCommand(command),
  });
  return !result.error && result.status === 0;
}

try {
  assertPmoPackageBuilt();
  assertPackedCliPullRequestTemplate(tempRoot);
  assertNoBundledTemplatePmoBaseline();
  assertPublishedPnpmPmoResolution(tempRoot);
  assertNoWorkspacePackageJsonInTemplates();
  assertQualityConfigsMatchPluginSources();
  assertPackagedAdminModules();

  const result = spawnSync(
    process.execPath,
    [
      cli,
      'init',
      fullProjectName,
      '--preset',
      'full',
      '--topology',
      'monolith',
      '--package',
      'com.example.acceptance',
      '--group-id',
      'com.example',
    ],
    {
      cwd: tempRoot,
      encoding: 'utf8',
    },
  );

  if (result.status !== 0) {
    throw new Error(`CLI failed:\n${result.stdout}\n${result.stderr}`);
  }

  const projectRoot = join(tempRoot, fullProjectName);
  const requiredFiles = [
    'mango.config.json',
    'mango.dev.json',
    'AGENTS.md',
    '.gitignore',
    'frontend/package.json',
    'frontend/eslint.config.mjs',
    'frontend/prettier.config.mjs',
    'frontend/stylelint.config.mjs',
    'frontend/pnpm-workspace.yaml',
    'frontend/scripts/build-with-report.mjs',
    'frontend/src/environment.ts',
    'frontend/src/environment.test.ts',
    'frontend/src/main.ts',
    'frontend/src/mango-admin-modular.d.ts',
    'frontend/tsconfig.app.json',
    'frontend/public/runtime-config.json',
    'scripts/dev-workspace.sh',
    'scripts/backend-dev.sh',
    'backend/pom.xml',
    'backend/app/pom.xml',
    'backend/architecture-verification/pom.xml',
    'backend/config/quality/checkstyle.xml',
    'backend/config/quality/pmd-p3c.xml',
    'backend/app/src/main/java/com/example/acceptance/MangoFullAcceptanceApplication.java',
    'backend/app/src/main/resources/application.yml',
    '.github/CODEOWNERS',
    '.github/pull_request_template.md',
    '.github/workflows/pmo-doc-check.yml',
    '.gitea/workflows/pmo-doc-check.yml',
    'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
    'business-pmo/mango-baseline/tools/check-document-set.mjs',
    'business-pmo/mango-baseline/baseline.json',
    'business-pmo/architecture-debt-budget.json',
    'business-pmo/global-entity-exceptions.json',
    'business-pmo/pmo-lock.json',
    '.agents/skills/.mango-pmo.json',
    '.agents/skills/mango-pmo-lifecycle/SKILL.md',
    'business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs',
    'business-pmo/mango-baseline/templates/acceptance-evidence.md',
    'topologies/monolith/README.md',
    'topologies/microservice/README.md',
  ];

  for (const file of requiredFiles) {
    if (!existsSync(join(projectRoot, file))) {
      throw new Error(`missing generated file: ${file}`);
    }
  }
  assertGeneratedFrontendFormatting(projectRoot);

  const globalEntityManifest = JSON.parse(
    readFileSync(join(projectRoot, 'business-pmo/global-entity-exceptions.json'), 'utf8'),
  );
  assertEqual(globalEntityManifest.contractId, 'global-entity-exceptions', 'global Entity contract id');
  assertEqual(globalEntityManifest.schemaRevision, 1, 'global Entity schema revision');
  assertEqual(globalEntityManifest.version, 1, 'global Entity manifest version');
  if (!Array.isArray(globalEntityManifest.exceptions) || globalEntityManifest.exceptions.length !== 0) {
    throw new Error('generated global Entity manifest must start with an empty exceptions array');
  }
  const architectureDebtBudget = JSON.parse(
    readFileSync(join(projectRoot, 'business-pmo/architecture-debt-budget.json'), 'utf8'),
  );
  assertEqual(architectureDebtBudget.schemaVersion, 4, 'architecture debt budget schema');
  assertEqual(architectureDebtBudget.totalIssueCount, 0, 'initial architecture debt total');
  if (
    Object.keys(architectureDebtBudget.modules || {}).length !== 0 ||
    Object.keys(architectureDebtBudget.moduleOnboardings || {}).length !== 0
  ) {
    throw new Error('generated architecture debt budget must start empty and auditable');
  }

  const config = JSON.parse(readFileSync(join(projectRoot, 'mango.config.json'), 'utf8'));
  assertEqual(config.preset, 'full', 'preset');
  assertEqual(config.topology, 'monolith', 'topology');
  assertEqual(config.basePackage, 'com.example.acceptance', 'basePackage');
  assertIncludes(config.modules.optional, 'workflow', 'full optional modules');
  assertIncludes(config.modules.optional, 'template', 'full optional modules');
  assertIncludes(config.modules.optional, 'notice', 'full optional modules');
  assertIncludes(config.modules.optional, 'payment', 'full optional modules');
  assertEqual(config.mangoBackendVersion, releaseVersions.maven.mangoBackend, 'Mango backend Maven version lock');
  assertEqual(config.mavenRepository, expectedMavenRepository, 'HTTPS Maven repository');
  assertEqual(config.paths.backend, 'backend', 'backend path');
  assertEqual(config.paths.frontend, 'frontend', 'frontend path');
  assertEqual(config.paths.businessDocs, 'business-docs', 'business docs path');

  const mainTs = readFileSync(join(projectRoot, 'frontend/src/main.ts'), 'utf8');
  if (!mainTs.includes("from '@mango/admin/full'") || !mainTs.includes("import '@mango/admin/style-full.css'")) {
    throw new Error('frontend entry does not consume @mango/admin/full');
  }
  if (
    !mainTs.includes('MangoAdminBootstrapHooks,') ||
    !mainTs.includes('MangoAdminFeatureRegistrar,') ||
    !mainTs.includes('MangoAdminShellOptions') ||
    !mainTs.includes("import { bootstrapMangoAdminApp } from '@mango/admin';") ||
    !mainTs.includes('const mangoBusinessFeatureRegistrars: MangoAdminFeatureRegistrar[] =') ||
    !mainTs.includes('const mangoAllFeatureRegistrars: MangoAdminFeatureRegistrar[] = [') ||
    !mainTs.includes('featureRegistrars: mangoAllFeatureRegistrars') ||
    !mainTs.includes("enabled: import.meta.env.VITE_MANGO_MODULE_DIAGNOSTICS_ENABLED === 'true'") ||
    !mainTs.includes('bootstrapMangoAdminApp(mangoAdminOptions, mangoAdminBootstrapHooks);')
  ) {
    throw new Error('frontend entry does not provide business feature registrar aggregation');
  }
  const frontendHtml = readFileSync(join(projectRoot, 'frontend/index.html'), 'utf8');
  if (!frontendHtml.includes('http-equiv="Referrer-Policy" content="no-referrer"')) {
    throw new Error('frontend entry does not enforce the module diagnostic referrer boundary');
  }
  if (frontendHtml.includes('http-equiv="Content-Security-Policy"')) {
    throw new Error('frontend entry hard-codes a CSP that can block remote runtime entries');
  }
  if (mainTs.includes('{{')) {
    throw new Error('frontend entry contains unrendered placeholders');
  }

  const frontendPackage = JSON.parse(readFileSync(join(projectRoot, 'frontend/package.json'), 'utf8'));
  assertEqual(frontendPackage.devDependencies['@mango/cli'], cliPackage.version, '@mango/cli');
  for (const script of ['format:check', 'lint', 'stylelint', 'typecheck', 'test:unit', 'build', 'check']) {
    if (!frontendPackage.scripts[script]) {
      throw new Error(`frontend package missing quality script: ${script}`);
    }
  }
  for (const dependency of ['eslint', 'prettier', 'stylelint', 'typescript-eslint', 'vitest']) {
    if (!frontendPackage.devDependencies[dependency]) {
      throw new Error(`frontend package missing quality dependency: ${dependency}`);
    }
  }
  assertGeneratedPnpmWorkspace(join(projectRoot, 'frontend/pnpm-workspace.yaml'));
  for (const dependency of [
    '@mango/admin',
    '@mango/grid-widgets',
    '@mango/file',
    '@mango/workflow',
    '@mango/template',
    '@mango/notice',
    '@mango/payment',
  ]) {
    if (!frontendPackage.dependencies[dependency]) {
      throw new Error(`frontend package missing dependency: ${dependency}`);
    }
  }
  const expectedVersions = {
    '@mango/admin': readReleasedPackageVersion('@mango/admin'),
    '@mango/admin-pages': readReleasedPackageVersion('@mango/admin-pages'),
    '@mango/admin-shell': readReleasedPackageVersion('@mango/admin-shell'),
    '@mango/app-runtime': readReleasedPackageVersion('@mango/app-runtime'),
    '@mango/auth': readReleasedPackageVersion('@mango/auth'),
    '@mango/calendar': readReleasedPackageVersion('@mango/calendar'),
    '@mango/common': readReleasedPackageVersion('@mango/common'),
    '@mango/http-client': readReleasedPackageVersion('@mango/http-client'),
    '@mango/grid-layout': readReleasedPackageVersion('@mango/grid-layout'),
    '@mango/grid-widgets': readReleasedPackageVersion('@mango/grid-widgets'),
    '@mango/file': readReleasedPackageVersion('@mango/file'),
    '@mango/job': readReleasedPackageVersion('@mango/job'),
    '@mango/notice': readReleasedPackageVersion('@mango/notice'),
    '@mango/numgen': readReleasedPackageVersion('@mango/numgen'),
    '@mango/payment': readReleasedPackageVersion('@mango/payment'),
    '@mango/rbac': readReleasedPackageVersion('@mango/rbac'),
    '@mango/system': readReleasedPackageVersion('@mango/system'),
    '@mango/template': readReleasedPackageVersion('@mango/template'),
    '@mango/workflow': readReleasedPackageVersion('@mango/workflow'),
    '@mango/workflow-business-example': readReleasedPackageVersion('@mango/workflow-business-example'),
  };
  for (const [dependency, expectedVersion] of Object.entries(expectedVersions)) {
    assertEqual(frontendPackage.dependencies[dependency], expectedVersion, dependency);
  }
  const frontendBuildScript = readFileSync(join(projectRoot, 'frontend/scripts/build-with-report.mjs'), 'utf8');
  if (
    !frontendPackage.scripts.build.includes('build-with-report.mjs') ||
    !frontendBuildScript.includes('frontend-build-warnings.log') ||
    !frontendBuildScript.includes('warningCount') ||
    !frontendBuildScript.includes("process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm'") ||
    !frontendBuildScript.includes('shouldUseShellForCommand(packageManagerCommand)') ||
    !frontendBuildScript.includes("args: ['exec', 'vite', 'build']")
  ) {
    throw new Error('generated frontend build must use pnpm production build and capture warnings');
  }
  const frontendViteConfig = readFileSync(join(projectRoot, 'frontend/vite.config.ts'), 'utf8');
  if (
    !frontendViteConfig.includes('manualChunks: mangoManualChunks') ||
    !frontendViteConfig.includes('mango-workflow') ||
    !frontendViteConfig.includes('element-plus') ||
    !frontendViteConfig.includes('vue-vendor')
  ) {
    throw new Error('generated frontend Vite config must split full preset bundles with manualChunks');
  }
  const devManifestFrontend = JSON.parse(readFileSync(join(projectRoot, 'mango.dev.json'), 'utf8')).apps[
    `${fullProjectName}-admin`
  ];
  assertEqual(devManifestFrontend.packageManager, 'pnpm', 'generated frontend package manager');

  const pom = readFileSync(join(projectRoot, 'backend/pom.xml'), 'utf8');
  const appPom = readFileSync(join(projectRoot, 'backend/app/pom.xml'), 'utf8');
  const architecturePom = readFileSync(join(projectRoot, 'backend/architecture-verification/pom.xml'), 'utf8');
  const pmoWorkflow = readFileSync(join(projectRoot, '.github/workflows/pmo-doc-check.yml'), 'utf8');
  const giteaPmoWorkflow = readFileSync(join(projectRoot, '.gitea/workflows/pmo-doc-check.yml'), 'utf8');
  const pullRequestTemplate = readFileSync(join(projectRoot, '.github/pull_request_template.md'), 'utf8');
  const canonicalPullRequestTemplate = readFileSync(
    join(projectRoot, 'business-pmo/mango-baseline/templates/business-pull-request-template.md'),
    'utf8',
  );
  const codeowners = readFileSync(join(projectRoot, '.github/CODEOWNERS'), 'utf8');
  const devWorkspaceScript = readFileSync(join(projectRoot, 'scripts/dev-workspace.sh'), 'utf8');
  const backendDevScript = readFileSync(join(projectRoot, 'scripts/backend-dev.sh'), 'utf8');
  const devManifest = JSON.parse(readFileSync(join(projectRoot, 'mango.dev.json'), 'utf8'));
  if (!appPom.includes('<artifactId>mango-admin-starter</artifactId>') || pom.includes('{{') || appPom.includes('{{')) {
    throw new Error('backend poms were not rendered as Mango full backend');
  }
  if (!pom.includes(`<mango.version>${releaseVersions.maven.mangoBackend}</mango.version>`)) {
    throw new Error('generated backend parent pom must use the CLI-owned fixed Mango Maven version lock');
  }
  if (
    !pom.includes('<version>${revision}</version>') ||
    !pom.includes('<revision>1.0.0-SNAPSHOT</revision>') ||
    !appPom.includes('<version>${revision}</version>') ||
    !architecturePom.includes('<version>${revision}</version>')
  ) {
    throw new Error('generated backend reactor must use the CI-friendly revision property');
  }
  if (
    !pom.includes('<flyway.version>11.20.3</flyway.version>') ||
    !pom.includes('<artifactId>flyway-core</artifactId>') ||
    !pom.includes('<artifactId>flyway-mysql</artifactId>')
  ) {
    throw new Error('generated backend parent pom must explicitly manage the MySQL 8.4 compatible Flyway baseline');
  }
  if (!pom.includes(`<url>${expectedMavenRepository}</url>`)) {
    throw new Error('generated backend parent pom must use the HTTPS Mango Maven repository');
  }
  if (
    !pom.includes('<mango.architecture.mode>full</mango.architecture.mode>') ||
    !pom.includes('<mango.architecture.skip>false</mango.architecture.skip>') ||
    !pom.includes('<mango.architecture.requireFullReactor>true</mango.architecture.requireFullReactor>') ||
    !pom.includes('<mango.check.rule>all</mango.check.rule>') ||
    !pom.includes('<mango.check.baseDir>${maven.multiModuleProjectDirectory}</mango.check.baseDir>') ||
    !pom.includes('<mango.check.gate>all</mango.check.gate>') ||
    !pom.includes('<mango.check.changedOnly>false</mango.check.changedOnly>') ||
    !pom.includes('<mango.check.requireFullScope>true</mango.check.requireFullScope>') ||
    !pom.includes('<mango.check.staticFailurePolicy>block</mango.check.staticFailurePolicy>') ||
    !pom.includes('<module>architecture-verification</module>') ||
    pom.lastIndexOf('<module>architecture-verification</module>') <
      pom.lastIndexOf('<!-- mango-cli:business-modules:end -->')
  ) {
    throw new Error('generated backend parent pom must keep the fail-closed architecture verifier last');
  }
  for (const expected of [
    '<artifactId>mango-maven-plugin</artifactId>',
    '<version>${mango.version}</version>',
    '<goal>architecture</goal>',
    '<mode>${mango.architecture.mode}</mode>',
    '<skip>${mango.architecture.skip}</skip>',
    '<requireFullReactor>${mango.architecture.requireFullReactor}</requireFullReactor>',
    '<lockFullReactor>false</lockFullReactor>',
    '<lockFullMode>false</lockFullMode>',
    '<globalEntityManifest>${maven.multiModuleProjectDirectory}/../business-pmo/global-entity-exceptions.json</globalEntityManifest>',
    '<businessGroupPrefix>com.example</businessGroupPrefix>',
    '<property>mango.architecture.skip</property>',
    '<property>mango.architecture.mode</property>',
    '<property>mango.architecture.requireFullReactor</property>',
    '<goal>check</goal>',
    '<rule>${mango.check.rule}</rule>',
    '<baseDir>${mango.check.baseDir}</baseDir>',
    '<gate>${mango.check.gate}</gate>',
    '<changedOnly>${mango.check.changedOnly}</changedOnly>',
    '<baseRef>${mango.check.baseRef}</baseRef>',
    '<staticFailurePolicy>${mango.check.staticFailurePolicy}</staticFailurePolicy>',
    '<requireExecutionRoot>true</requireExecutionRoot>',
    '<requiredRule>all</requiredRule>',
    '<requiredGate>${mango.check.gate}</requiredGate>',
    '<requireBlockingStaticFailures>true</requireBlockingStaticFailures>',
    '<requireFullScope>${mango.check.requireFullScope}</requireFullScope>',
    '<property>mango.check.gate</property>',
    '<property>mango.check.changedOnly</property>',
    '<property>mango.check.requireFullScope</property>',
    '<property>mango.check.staticFailurePolicy</property>',
  ]) {
    if (!architecturePom.includes(expected)) {
      throw new Error(`generated architecture verification pom missing contract: ${expected}`);
    }
  }
  if (architecturePom.includes('<excludedModules>')) {
    throw new Error('generated architecture verification pom must not exclude reactor modules');
  }
  for (const expected of [
    '<artifactId>maven-pmd-plugin</artifactId>',
    '<version>3.16.0</version>',
    '<ruleset>${maven.multiModuleProjectDirectory}/config/quality/pmd-p3c.xml</ruleset>',
    '<artifactId>p3c-pmd</artifactId>',
    '<artifactId>maven-checkstyle-plugin</artifactId>',
    '<configLocation>${maven.multiModuleProjectDirectory}/config/quality/checkstyle.xml</configLocation>',
    '<violationSeverity>warning</violationSeverity>',
    '<artifactId>spotbugs-maven-plugin</artifactId>',
    '<threshold>Medium</threshold>',
    '<xmlOutput>true</xmlOutput>',
    '<spotbugsXmlOutputFilename>spotbugsXml.xml</spotbugsXmlOutputFilename>',
  ]) {
    if (!pom.includes(expected)) {
      throw new Error(`generated backend parent pom missing Java quality contract: ${expected}`);
    }
  }
  for (const expected of [
    'name: PMO Documentation Checks',
    'pmo-doc-check:',
    'actions/setup-node@v4',
    'classify-pmo-check-scope.mjs',
    'risk-verification.mjs',
    'node business-pmo/mango-baseline/tools/check-document-set.mjs',
    '--root "$BUSINESS_DOCS_ROOT"',
    'mvn -B -ntp -f "$BACKEND_POM"',
    '-pl architecture-verification',
    'help:effective-pom',
    '-pl "$MAVEN_PROJECTS"',
    '-Dmango.architecture.mode=changed',
    '-Dmango.architecture.requireFullReactor=false',
    'Generate trusted full-Reactor architecture inventory for governance',
    '-Dmango.architecture.requireFullReactor=true',
    '-Dmango.architecture.inventoryOnly=true',
    '-Dmango.architecture.skip=false',
    '-Dmango.check.rule=all',
    '-Dmango.check.gate=no-new-violations',
    '-Dmango.check.changedOnly=true',
    '-Dmango.check.baseRef="$BASE_SHA"',
    '-Dmango.check.requireFullScope=false',
    '-Dmango.check.requireFullScope=true',
    '-Dmango.check.staticFailurePolicy=block',
    '-Denforcer.skip=false',
    'verify',
    'Enforce project architecture debt budget policy',
    'business-pmo/mango-baseline/tools/check-architecture-debt-budget.mjs',
    '--report "$BACKEND_ROOT/target/mango-architecture-report.json"',
    '--baseline business-pmo/architecture-debt-budget.json',
    '--baseline-only',
    '--base-ref "$BASE_SHA"',
  ]) {
    for (const workflow of [pmoWorkflow, giteaPmoWorkflow]) {
      if (!workflow.includes(expected)) {
        throw new Error(`generated PMO required-check workflow missing contract: ${expected}`);
      }
    }
  }
  for (const expected of [
    'concurrency:',
    "github.event.action == 'edited' && 'contract' || 'code'",
    'cancel-in-progress: true',
    'preflight_scope:',
    'maven_dependency_projects: ${{ steps.scope.outputs.maven_dependency_projects }}',
    'backend_root: ${{ steps.scope.outputs.backend_root }}',
    'pmo:\n    name: PMO contract gates\n    needs: preflight_scope',
    'docs:\n    name: PMO document gates\n    needs: preflight_scope',
    'backend:\n    name: Affected backend gates\n    needs: preflight_scope',
    "needs.preflight_scope.outputs.backend_mode == 'governance'",
    "needs.preflight_scope.outputs.backend_mode == 'partial'",
    'pmo-doc-check:\n    name: pmo-doc-check',
    'needs: [preflight_scope, pmo, docs, backend]',
    'if: ${{ always() }}',
    'success|skipped) ;;',
  ]) {
    if (!pmoWorkflow.includes(expected)) {
      throw new Error(`generated GitHub PMO workflow missing parallel contract: ${expected}`);
    }
  }
  for (const expected of [
    'pmo-doc-check:\n    runs-on: ubuntu-latest',
    "steps.scope.outputs.backend_mode == 'governance'",
    "steps.scope.outputs.backend_mode == 'partial'",
    'steps.scope.outputs.maven_dependency_projects',
    'steps.scope.outputs.backend_root',
  ]) {
    if (!giteaPmoWorkflow.includes(expected)) {
      throw new Error(`generated Gitea PMO workflow missing compatible scope contract: ${expected}`);
    }
  }
  if (!pmoWorkflow.includes('${{ github.event.pull_request.base.sha')) {
    throw new Error('generated PMO workflow must preserve GitHub Actions expressions');
  }
  if (!giteaPmoWorkflow.includes('${{ gitea.event.pull_request.base.sha')) {
    throw new Error('generated PMO workflow must preserve Gitea Actions expressions');
  }
  for (const workflow of [pmoWorkflow, giteaPmoWorkflow]) {
    if (/^\s+paths:/mu.test(workflow)) {
      throw new Error(
        'generated PMO required-check workflow must run for every PR; paths filters can leave the required check pending',
      );
    }
    if (workflow.includes("backend_mode == 'full'") || workflow.includes('-Dmango.architecture.mode=full')) {
      throw new Error('generated PR workflow must reserve full-Reactor inventory for scheduled or manual execution');
    }
    const dependencyBuild = extractNamedWorkflowStep(workflow, 'Build affected-module upstream dependencies');
    if (
      !/^\s+-am\s*$/mu.test(dependencyBuild) ||
      !dependencyBuild.includes('-DskipTests') ||
      !dependencyBuild.includes('install')
    ) {
      throw new Error('generated dependency build must install affected upstream modules with an isolated -am build');
    }
    const qualityGate = extractNamedWorkflowStep(workflow, 'Verify affected backend modules and Mango architecture');
    if (/^\s+-(?:am|amd)\s*$/mu.test(qualityGate)) {
      throw new Error('generated partial quality gate must not expand Maven scope with -am or -amd');
    }
    const governanceInventory = extractNamedWorkflowStep(
      workflow,
      'Generate trusted full-Reactor architecture inventory for governance',
    );
    if (
      !governanceInventory.includes('-Dmango.check.changedOnly=false') ||
      /-Dmango\.check\.changedOnly=true/u.test(governanceInventory)
    ) {
      throw new Error('generated governance inventory must use full-scope changedOnly=false');
    }
    if (!qualityGate.includes('-Dmango.check.changedOnly=true')) {
      throw new Error('generated partial quality gate must keep changedOnly=true');
    }
  }
  for (const expected of [
    '## Risk / Verification',
    'Requirement impact:',
    'Solution risk:',
    'Final risk:',
    'Delivery mode:',
    'Workspace decision:',
    'Non-downgradable facts:',
    'Assurance baseline:',
    'Assurance selections:',
    'Assurance reasoning:',
    'Assurance evidence:',
    'Residual risks:',
  ]) {
    if (!pullRequestTemplate.includes(expected)) {
      throw new Error(`generated pull request template missing risk contract: ${expected}`);
    }
  }
  for (const legacy of ['Selected verification:', 'Why sufficient:', 'Skipped verification:']) {
    if (pullRequestTemplate.includes(legacy)) {
      throw new Error(`generated pull request template contains legacy risk contract: ${legacy}`);
    }
  }
  assertEqual(
    pullRequestTemplate,
    canonicalPullRequestTemplate,
    'generated pull request template compared with the PMO canonical template',
  );
  for (const expected of [
    '/backend/pom.xml @backend-team @tech-lead',
    '/backend/architecture-verification/ @backend-team @tech-lead',
    '/backend/config/quality/ @platform-team @tech-lead',
    '/backend/.mvn/ @platform-team @tech-lead',
    '/.mvn/ @platform-team @tech-lead',
    '/.github/CODEOWNERS @platform-team @tech-lead @pmo',
    '/.github/pull_request_template.md @platform-team @tech-lead @pmo',
    '/.github/workflows/ @platform-team @tech-lead @pmo',
    '/business-docs/ @pm @tech-lead @qa',
  ]) {
    if (!codeowners.includes(expected)) {
      throw new Error(`generated CODEOWNERS does not protect the architecture gate: ${expected}`);
    }
  }
  assertManagedDependency(pom, 'io.mango.platform.file', 'mango-file-api');
  assertManagedDependency(pom, 'io.mango.platform.file.preview', 'mango-file-preview-api');
  assertManagedDependency(pom, 'io.mango.platform.notice', 'mango-notice-api');
  assertManagedDependency(pom, 'io.mango.platform.workflow', 'mango-workflow-api');
  assertNoDirectDependency(appPom, 'mango-file-api', 'full backend app pom');
  assertNoDirectDependency(appPom, 'mango-file-preview-api', 'full backend app pom');
  if (
    appPom.includes('<artifactId>mango-seed-starter</artifactId>') ||
    pom.includes('<artifactId>mango-seed-starter</artifactId>')
  ) {
    throw new Error('full backend must not require removed mango-seed-starter');
  }
  const applicationYml = readFileSync(join(projectRoot, 'backend/app/src/main/resources/application.yml'), 'utf8');
  if (
    applicationYml.includes('MANGO_SEED_') ||
    applicationYml.includes('mango.seed') ||
    applicationYml.includes('\n  seed:\n')
  ) {
    throw new Error('full backend application.yml must not include removed Mango seed config');
  }
  if (
    !applicationYml.includes('    sm4:\n      secret-key: ${MANGO_CRYPTO_SM4_SECRET_KEY:}') ||
    applicationYml.includes('sm4-key') ||
    applicationYml.includes('sm4-iv') ||
    applicationYml.includes('00112233445566778899aabbccddeeff')
  ) {
    throw new Error(
      'full backend application.yml must use current mango.crypto.sm4.secret-key env config without a public default',
    );
  }
  assertYamlFlywayModuleEnabled(applicationYml, 'domain');
  assertYamlFlywayModuleEnabled(applicationYml, 'workflow');
  assertYamlFlywayModuleEnabled(applicationYml, 'resource');
  assertYamlFlywayModuleEnabled(applicationYml, 'home');
  assertYamlFlywayModuleEnabled(applicationYml, 'mango-job');
  assertYamlFlywayModuleEnabled(applicationYml, 'payment');
  assertYamlFlywayModuleEnabled(applicationYml, 'link');
  assertYamlFlywayModuleEnabled(applicationYml, 'grid-layout');
  assertYamlFlywayModuleEnabled(applicationYml, 'mango-cms');
  assertYamlFlywayModuleOutOfOrder(applicationYml, 'domain');
  assertYamlFlywayModuleOutOfOrder(applicationYml, 'mango-job');
  assertYamlFlywayModuleOutOfOrder(applicationYml, 'payment');
  assertYamlFlywayModuleStrictOrdering(applicationYml, 'workflow');
  if (
    pom.includes('<password>') ||
    pom.includes('_authToken') ||
    appPom.includes('<password>') ||
    appPom.includes('_authToken')
  ) {
    throw new Error('generated backend contains repository credentials');
  }
  if (
    !devManifest.apps['mango-full-acceptance-service'] ||
    !devManifest.apps['mango-full-acceptance-admin'] ||
    devManifest.apps['mango-full-acceptance-service'].goal !==
      'org.springframework.boot:spring-boot-maven-plugin:3.5.14:run' ||
    devManifest.apps['mango-full-acceptance-service'].env?.MANGO_CRYPTO_SM4_SECRET_KEY !==
      '${env.MANGO_CRYPTO_SM4_SECRET_KEY}' ||
    devManifest.apps['mango-full-acceptance-admin'].dependsOn[0] !== 'mango-full-acceptance-service'
  ) {
    throw new Error(
      'generated mango.dev.json must describe backend/frontend startup with explicit Spring Boot plugin and SM4 env propagation',
    );
  }
  if (
    !devWorkspaceScript.includes('scripts/dev-workspace.sh is deprecated') ||
    !devWorkspaceScript.includes('workspace init') ||
    !devWorkspaceScript.includes('dev start') ||
    !devWorkspaceScript.includes('command -v mango') ||
    !devWorkspaceScript.includes('exec pnpm exec mango "$@"') ||
    !devWorkspaceScript.includes('cd frontend && pnpm install') ||
    !devWorkspaceScript.includes('npm install -g @mango/cli@') ||
    !devWorkspaceScript.includes('FRONTEND_ROOT}/node_modules/.bin/mango') ||
    devWorkspaceScript.includes('init|init-dev') ||
    devWorkspaceScript.includes('run_mango workspace init') ||
    devWorkspaceScript.includes('npx --yes') ||
    devWorkspaceScript.includes('mango-ui/packages/mango-cli/src/index.mjs') ||
    devWorkspaceScript.includes('spring-boot:run') ||
    devWorkspaceScript.includes('diagnose_backend_failure')
  ) {
    throw new Error('generated dev-workspace script must be a thin mango CLI shim');
  }
  if (process.platform !== 'win32') {
    assertGeneratedDevWorkspaceUsesCliFallback(projectRoot);
    assertGeneratedDevWorkspaceRejectsInitShim(projectRoot);
    assertGeneratedDevWorkspaceCreatesLocalSecretKey(projectRoot);
    assertGeneratedDevWorkspaceBackfillsLocalSecretKey(projectRoot);
    assertDevWorkspaceRejectsNonCiFriendlyMavenPom(projectRoot);
    assertDevWorkspaceAutoCreatesDatabase(projectRoot);
    assertDevWorkspaceStreamsLargeInstallOutput(projectRoot);
    assertCommandDevWorkspaceAutoCreatesDatabase(projectRoot);
    assertDevWorkspaceReportsMissingMysql(projectRoot);
    assertDevWorkspaceRestartUsesStopThenStart(projectRoot);
  }
  if (
    !backendDevScript.includes('mango dev start backend') ||
    !backendDevScript.includes('exec "${ROOT_DIR}/scripts/dev-workspace.sh" backend')
  ) {
    throw new Error('generated backend-dev script must delegate to dev-workspace backend entry');
  }
  assertCommandOk([cli, 'validate'], projectRoot, 'generated mango validate');
  const planResult = assertCommandOk([cli, 'plan'], projectRoot, 'generated mango plan');
  if (
    !planResult.stdout.includes('mango-full-acceptance-service') ||
    !/-Drevision=1\.0\.0-mango-[0-9]{3}-SNAPSHOT/u.test(planResult.stdout) ||
    !planResult.stdout.includes('org.springframework.boot:spring-boot-maven-plugin:3.5.14:run') ||
    !planResult.stdout.includes('mango-full-acceptance-admin')
  ) {
    throw new Error(`generated mango plan did not include resolved backend/frontend apps:\n${planResult.stdout}`);
  }
  if (process.platform !== 'win32') {
    if ((statSync(join(projectRoot, 'scripts/dev-workspace.sh')).mode & 0o111) === 0) {
      throw new Error('generated dev-workspace script must be executable');
    }
    if ((statSync(join(projectRoot, 'scripts/backend-dev.sh')).mode & 0o111) === 0) {
      throw new Error('generated backend dev script must be executable');
    }
  }

  const npmrc = readFileSync(join(projectRoot, 'frontend/.npmrc'), 'utf8');
  if (!npmrc.includes('registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/')) {
    throw new Error('generated .npmrc does not contain the configured Mango npm registry');
  }
  if (npmrc.includes('_authToken') || npmrc.includes('password') || npmrc.includes('username')) {
    throw new Error('generated .npmrc contains credentials');
  }
  assertNoUnrenderedPlaceholders(projectRoot);
  const businessAgents = readFileSync(join(projectRoot, 'AGENTS.md'), 'utf8');
  if (!businessAgents.includes('mango-cli init --preset full')) {
    throw new Error('generated full AGENTS.md should record full preset');
  }
  for (const expected of [
    'mango pmo check --project-dir .',
    'git worktree list',
    'mango workspace status',
    'business-docs',
  ]) {
    if (!businessAgents.includes(expected)) {
      throw new Error(`generated AGENTS.md should mention governance workflow: ${expected}`);
    }
  }
  const generatedBaselineManifest = JSON.parse(
    readFileSync(join(projectRoot, 'business-pmo/mango-baseline/baseline.json'), 'utf8'),
  );
  assertEqual(generatedBaselineManifest.packageName, '@mango/pmo', 'generated PMO baseline package');
  const baselineReadme = readFileSync(join(projectRoot, 'business-pmo/mango-baseline/README.md'), 'utf8');
  if (
    !baselineReadme.includes('mango pmo check') ||
    baselineReadme.includes('7bca6b8f') ||
    baselineReadme.includes('{{mangoBaselineCommit}}')
  ) {
    throw new Error('generated baseline README contains stale, unrendered, or unversioned source info');
  }

  const fullAddResult = spawnSync(process.execPath, [cli, 'add', 'notice', '--project-dir', projectRoot], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (
    fullAddResult.status === 0 ||
    !fullAddResult.stderr.includes('full preset already includes all optional modules')
  ) {
    throw new Error(`add command should reject full preset:\n${fullAddResult.stdout}\n${fullAddResult.stderr}`);
  }

  const baselinePreflight = spawnSync(
    process.execPath,
    [
      'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
      '--role',
      'dev',
      '--phase',
      'develop',
      '--task',
      '基于 Mango full preset 开发业务项目',
      '--paths',
      'frontend,backend',
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  if (baselinePreflight.status !== 0) {
    throw new Error(`generated PMO preflight failed:\n${baselinePreflight.stdout}\n${baselinePreflight.stderr}`);
  }
  if (!baselinePreflight.stdout.includes('rules/frontend/01-vue-code.md')) {
    throw new Error(`generated PMO preflight did not include frontend rules:\n${baselinePreflight.stdout}`);
  }
  if (!baselinePreflight.stdout.includes('rules/frontend/04-test.md')) {
    throw new Error(`generated PMO preflight did not include frontend test rules:\n${baselinePreflight.stdout}`);
  }
  assertGeneratedBaselineLoadsDeliveryContractForPr(projectRoot);
  assertBusinessAcceptanceBaseline(projectRoot);
  assertPmoCommands(projectRoot);
  assertPmoSyncCommand(tempRoot);
  if (hasCommand('jar')) {
    assertDocsBundleCommands(projectRoot, tempRoot);
  }

  const customResult = spawnSync(
    process.execPath,
    [
      cli,
      'init',
      customProjectName,
      '--preset',
      'custom',
      '--modules',
      'workflow-example,template',
      '--topology',
      'monolith',
      '--package',
      'com.example.custom',
      '--group-id',
      'com.example',
    ],
    {
      cwd: tempRoot,
      encoding: 'utf8',
    },
  );
  if (customResult.status !== 0) {
    throw new Error(`custom CLI failed:\n${customResult.stdout}\n${customResult.stderr}`);
  }

  const customRoot = join(tempRoot, customProjectName);
  const customConfig = JSON.parse(readFileSync(join(customRoot, 'mango.config.json'), 'utf8'));
  assertEqual(customConfig.preset, 'custom', 'custom preset');
  assertEqual(customConfig.modules.optional.join(','), 'workflow,workflow-example,template', 'custom modules');
  const customAgents = readFileSync(join(customRoot, 'AGENTS.md'), 'utf8');
  if (
    !customAgents.includes('mango-cli init --preset custom') ||
    customAgents.includes('mango-cli init --preset full')
  ) {
    throw new Error('generated custom AGENTS.md should record custom preset');
  }

  const customPackage = JSON.parse(readFileSync(join(customRoot, 'frontend/package.json'), 'utf8'));
  assertIncludes(Object.keys(customPackage.dependencies), '@mango/grid-widgets', 'custom default admin dependencies');
  assertIncludes(Object.keys(customPackage.dependencies), '@mango/workflow', 'custom dependencies');
  assertIncludes(Object.keys(customPackage.dependencies), '@mango/workflow-business-example', 'custom dependencies');
  assertIncludes(Object.keys(customPackage.dependencies), '@mango/template', 'custom dependencies');
  for (const dependency of ['@mango/calendar', '@mango/file', '@mango/notice', '@mango/numgen', '@mango/payment']) {
    assertIncludes(Object.keys(customPackage.dependencies), dependency, 'admin optional peer dependencies');
    assertEqual(customPackage.dependencies[dependency], readReleasedPackageVersion(dependency), dependency);
  }

  const customMain = readFileSync(join(customRoot, 'frontend/src/main.ts'), 'utf8');
  if (!customMain.includes("from '@mango/admin'") || customMain.includes("from '@mango/admin/full'")) {
    throw new Error('custom frontend entry should consume modular @mango/admin entry');
  }
  if (!customMain.includes("const mangoFeatures = ['workflow', 'template'] as const;")) {
    throw new Error('custom frontend entry should preserve literal feature types');
  }
  for (const expected of [
    'registerMangoJobAdminPages',
    'registerMangoCmsAdminPages',
    'registerMangoLinkAdminPages',
    'registerMangoWorkflowAdminPages',
    'registerMangoWorkflowBusinessExampleAdminPages',
    'registerMangoTemplateAdminPages',
    "import '@mango/workflow/style.css';",
    "import '@mango/workflow-business-example/style.css';",
    "import '@mango/template/style.css';",
  ]) {
    if (!customMain.includes(expected)) {
      throw new Error(`custom frontend entry missing registrar: ${expected}`);
    }
  }
  if (customMain.includes('registerMangoNoticeAdminPages')) {
    throw new Error('custom frontend entry registered unselected notice module');
  }

  const customPom = readFileSync(join(customRoot, 'backend/pom.xml'), 'utf8');
  const customAppPom = readFileSync(join(customRoot, 'backend/app/pom.xml'), 'utf8');
  if (customAppPom.includes('<artifactId>mango-admin-starter</artifactId>')) {
    throw new Error('custom backend should not depend on full mango-admin-starter');
  }
  for (const expected of [
    'mango-system-starter',
    'mango-notice-starter',
    'mango-workflow-starter',
    'mango-template-starter',
  ]) {
    if (!customAppPom.includes(`<artifactId>${expected}</artifactId>`)) {
      throw new Error(`custom backend missing dependency: ${expected}`);
    }
  }
  assertManagedDependency(customPom, 'io.mango.platform.file', 'mango-file-api');
  assertManagedDependency(customPom, 'io.mango.platform.file.preview', 'mango-file-preview-api');
  assertManagedDependency(customPom, 'io.mango.platform.template', 'mango-template-api');
  assertManagedDependency(customPom, 'io.mango.platform.workflow', 'mango-workflow-api');
  assertNoDirectDependency(customAppPom, 'mango-file-api', 'custom backend app pom');
  assertNoDirectDependency(customAppPom, 'mango-template-api', 'custom backend app pom');

  const customNoneResult = spawnSync(
    process.execPath,
    [
      cli,
      'init',
      customNoneProjectName,
      '--preset',
      'custom',
      '--modules',
      'none',
      '--topology',
      'monolith',
      '--package',
      'com.example.customnone',
      '--group-id',
      'com.example',
    ],
    {
      cwd: tempRoot,
      encoding: 'utf8',
    },
  );
  if (customNoneResult.status !== 0) {
    throw new Error(`custom none CLI failed:\n${customNoneResult.stdout}\n${customNoneResult.stderr}`);
  }
  const customNoneRoot = join(tempRoot, customNoneProjectName);
  const customNoneMain = readFileSync(join(customNoneRoot, 'frontend/src/main.ts'), 'utf8');
  if (customNoneMain.includes('registerMangoNoticeAdminPages')) {
    throw new Error('custom none frontend registered notice admin pages');
  }
  const customNoneAppPom = readFileSync(join(customNoneRoot, 'backend/app/pom.xml'), 'utf8');
  if (
    !customNoneAppPom.includes('<artifactId>mango-auth-starter</artifactId>') ||
    !customNoneAppPom.includes('<artifactId>mango-notice-starter</artifactId>')
  ) {
    throw new Error('custom none backend should include auth and notice starters for a bootable baseline');
  }
  const businessReadmePath = join(customRoot, 'README.md');
  const businessReadmeBeforeAdd = '# business-owned readme\n';
  writeFileSync(businessReadmePath, businessReadmeBeforeAdd);
  const customPackageBeforeAdd = JSON.parse(readFileSync(join(customRoot, 'frontend/package.json'), 'utf8'));
  customPackageBeforeAdd.dependencies['business-owned-package'] = '1.2.3';
  writeFileSync(join(customRoot, 'frontend/package.json'), `${JSON.stringify(customPackageBeforeAdd, null, 2)}\n`);
  writeFileSync(
    join(customRoot, 'frontend/src/main.ts'),
    `${customMain}\nconsole.info('business-owned bootstrap hook');\n`,
  );
  writeFileSync(
    join(customRoot, 'backend/app/pom.xml'),
    customAppPom.replace(
      '        <dependency>\n            <groupId>org.springframework.boot</groupId>\n            <artifactId>spring-boot-starter-actuator</artifactId>\n        </dependency>',
      [
        '        <dependency>',
        '            <groupId>com.example</groupId>',
        '            <artifactId>business-owned-starter</artifactId>',
        '            <version>1.2.3</version>',
        '        </dependency>',
        '        <dependency>',
        '            <groupId>org.springframework.boot</groupId>',
        '            <artifactId>spring-boot-starter-actuator</artifactId>',
        '        </dependency>',
      ].join('\n'),
    ),
  );

  const addResult = spawnSync(process.execPath, [cli, 'add', 'notice', '--project-dir', customRoot], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (addResult.status !== 0) {
    throw new Error(`add command failed:\n${addResult.stdout}\n${addResult.stderr}`);
  }
  const addedConfig = JSON.parse(readFileSync(join(customRoot, 'mango.config.json'), 'utf8'));
  assertEqual(addedConfig.modules.optional.join(','), 'workflow,workflow-example,template,notice', 'modules after add');
  const addedPackage = JSON.parse(readFileSync(join(customRoot, 'frontend/package.json'), 'utf8'));
  assertIncludes(Object.keys(addedPackage.dependencies), '@mango/notice', 'dependencies after add');
  assertEqual(
    addedPackage.dependencies['@mango/file'],
    readReleasedPackageVersion('@mango/file'),
    'file peer dependency after add',
  );
  assertEqual(addedPackage.dependencies['business-owned-package'], '1.2.3', 'business dependency after add');
  const addedMain = readFileSync(join(customRoot, 'frontend/src/main.ts'), 'utf8');
  if (!addedMain.includes('registerMangoNoticeAdminPages') || !addedMain.includes('registerMangoNoticeAdminShell')) {
    throw new Error('add command did not update notice frontend registrars');
  }
  if (!addedMain.includes("import '@mango/notice/style.css';")) {
    throw new Error('add command did not update notice frontend style import');
  }
  if (!addedMain.includes("console.info('business-owned bootstrap hook');")) {
    throw new Error('add command overwrote business-owned frontend entry content');
  }
  const addedPom = readFileSync(join(customRoot, 'backend/app/pom.xml'), 'utf8');
  if (!addedPom.includes('<artifactId>mango-notice-starter</artifactId>')) {
    throw new Error('add command did not update notice backend dependency');
  }
  if (!addedPom.includes('<artifactId>business-owned-starter</artifactId>')) {
    throw new Error('add command overwrote business-owned backend dependency');
  }
  assertEqual(readFileSync(businessReadmePath, 'utf8'), businessReadmeBeforeAdd, 'business-owned file after add');
  assertNoUnrenderedPlaceholders(customRoot);

  const moduleAddResult = spawnSync(
    process.execPath,
    [
      cli,
      'module',
      'add',
      'contract',
      '--aggregate',
      'seal',
      '--aggregate-name',
      '合同印章',
      '--module-name',
      '合同管理',
      '--project-dir',
      customRoot,
    ],
    {
      cwd: tempRoot,
      encoding: 'utf8',
    },
  );
  if (moduleAddResult.status !== 0) {
    throw new Error(`module add command failed:\n${moduleAddResult.stdout}\n${moduleAddResult.stderr}`);
  }
  for (const file of [
    'backend/modules/contract/README.md',
    'backend/modules/contract/contract-api/src/main/java/com/example/custom/contract/api/ContractApi.java',
    'backend/modules/contract/contract-api/src/main/java/com/example/custom/contract/api/command/UpdateSealCommand.java',
    'backend/modules/contract/contract-api/src/main/java/com/example/custom/contract/api/enums/SealCode.java',
    'backend/modules/contract/contract-core/src/main/java/com/example/custom/contract/core/entity/SealEntity.java',
    'backend/modules/contract/contract-core/src/main/java/com/example/custom/contract/core/mapper/SealMapper.java',
    'backend/modules/contract/contract-core/src/main/java/com/example/custom/contract/core/service/ISealService.java',
    'backend/modules/contract/contract-core/src/main/java/com/example/custom/contract/core/service/impl/SealService.java',
    'backend/modules/contract/contract-starter/src/main/java/com/example/custom/contract/starter/controller/ContractController.java',
    'backend/modules/contract/contract-starter/src/main/resources/META-INF/mango/resource-manifest.json',
    'backend/modules/contract/contract-starter-remote/src/main/java/com/example/custom/contract/starter/remote/ContractFeignClient.java',
    'frontend/packages/contract-api/src/api.ts',
    'frontend/packages/contract/style.css',
    'frontend/packages/contract/src/index.ts',
    'frontend/packages/contract/src/views/contract/seal/index.vue',
  ]) {
    if (!existsSync(join(customRoot, file))) {
      throw new Error(`module add missing generated file: ${file}`);
    }
  }
  assertGeneratedFrontendFormatting(customRoot);
  const modulePom = readFileSync(join(customRoot, 'backend/pom.xml'), 'utf8');
  const moduleAppPom = readFileSync(join(customRoot, 'backend/app/pom.xml'), 'utf8');
  const moduleApplicationYml = readFileSync(join(customRoot, 'backend/app/src/main/resources/application.yml'), 'utf8');
  if (!modulePom.includes('<module>modules/contract</module>')) {
    throw new Error('module add did not register backend module');
  }
  const moduleReadme = readFileSync(join(customRoot, 'backend/modules/contract/README.md'), 'utf8');
  for (const expected of [
    'Mango 能力入口',
    'Persistence 持久化',
    'Authorization 授权资源',
    'Admin Pages 页面注册',
    'mango-docs/capabilities/README.md',
    'mango/mango-infra/mango-infra-persistence/README.md',
    'business-pmo/mango-baseline/rules/backend/07-persistence.md',
  ]) {
    if (!moduleReadme.includes(expected)) {
      throw new Error(`module add README missing capability documentation entry: ${expected}`);
    }
  }
  if (
    !modulePom.includes('<artifactId>mango-infra-persistence-starter</artifactId>') ||
    !modulePom.includes('<artifactId>mango-infra-feign-starter</artifactId>') ||
    !modulePom.includes('<artifactId>swagger-annotations</artifactId>')
  ) {
    throw new Error('module add did not provide business backend dependency management');
  }
  const moduleApiPom = readFileSync(join(customRoot, 'backend/modules/contract/contract-api/pom.xml'), 'utf8');
  for (const expected of [
    '<artifactId>mango-common</artifactId>',
    '<artifactId>swagger-annotations</artifactId>',
    '<artifactId>mango-infra-persistence-api</artifactId>',
    '<artifactId>jakarta.validation-api</artifactId>',
  ]) {
    if (!moduleApiPom.includes(expected)) {
      throw new Error(`module add API POM missing contract dependency: ${expected}`);
    }
  }
  for (const forbidden of [
    '<artifactId>mango-infra-persistence-starter</artifactId>',
    '<artifactId>spring-web</artifactId>',
    '<artifactId>spring-boot-starter-web</artifactId>',
  ]) {
    if (moduleApiPom.includes(forbidden)) {
      throw new Error(`module add API POM contains runtime dependency: ${forbidden}`);
    }
  }
  const moduleCorePom = readFileSync(join(customRoot, 'backend/modules/contract/contract-core/pom.xml'), 'utf8');
  for (const expected of [
    '<artifactId>contract-api</artifactId>',
    '<artifactId>mango-infra-persistence-api</artifactId>',
    '<artifactId>spring-context</artifactId>',
  ]) {
    if (!moduleCorePom.includes(expected)) {
      throw new Error(`module add core POM missing boundary dependency: ${expected}`);
    }
  }
  if (moduleCorePom.includes('<artifactId>mango-infra-persistence-starter</artifactId>')) {
    throw new Error('module add core POM must not depend on the persistence starter');
  }
  if (!moduleAppPom.includes('<artifactId>contract-starter</artifactId>')) {
    throw new Error('module add did not register app dependency');
  }
  if (!moduleApplicationYml.includes('        contract:\n          enabled: true')) {
    throw new Error('module add did not enable business Flyway migration');
  }
  const moduleServiceInterface = readFileSync(
    join(
      customRoot,
      'backend/modules/contract/contract-core/src/main/java/com/example/custom/contract/core/service/ISealService.java',
    ),
    'utf8',
  );
  for (const expected of [
    'extends MangoTypedCrudService<',
    'SealEntity,',
    'CreateSealCommand,',
    'UpdateSealCommand,',
    'SealPageQuery,',
    'SealVO,',
    'Long>',
  ]) {
    if (!moduleServiceInterface.includes(expected)) {
      throw new Error(`module add typed CRUD service interface missing: ${expected}`);
    }
  }
  if (moduleServiceInterface.includes('extends MangoCrudService<SealEntity>')) {
    throw new Error('module add did not generate typed Mango CRUD service interface');
  }
  const moduleService = readFileSync(
    join(
      customRoot,
      'backend/modules/contract/contract-core/src/main/java/com/example/custom/contract/core/service/impl/SealService.java',
    ),
    'utf8',
  );
  if (
    !moduleService.includes('extends MangoCrudServiceImpl<SealMapper, SealEntity>') ||
    !moduleService.includes('Require.notNull(command, SealCode.VALIDATION_ERROR)') ||
    !moduleService.includes('Require.notNull(getById(command.getId()), SealCode.NOT_FOUND)') ||
    !moduleService.includes('@Transactional(rollbackFor = Exception.class)') ||
    !moduleService.includes('public Long create(CreateSealCommand command)') ||
    !moduleService.includes('public boolean delete(DeleteCommand command)') ||
    !moduleService.includes('public PersistencePageResult<SealVO> page(SealPageQuery query)') ||
    !moduleService.includes('public SealVO detail(Long id)') ||
    moduleService.includes('com.baomidou.mybatisplus.extension.service.impl.ServiceImpl') ||
    moduleService.includes('extends ServiceImpl<') ||
    moduleService.includes('selectPage') ||
    moduleService.includes('new Page<') ||
    moduleService.includes('setTenantId')
  ) {
    throw new Error('module add did not generate Mango persistence baseline service implementation');
  }
  const moduleController = readFileSync(
    join(
      customRoot,
      'backend/modules/contract/contract-starter/src/main/java/com/example/custom/contract/starter/controller/ContractController.java',
    ),
    'utf8',
  );
  for (const expected of [
    'public class ContractController implements ContractApi',
    '@Validated',
    '@RequestMapping("/contract/seals")',
    '@PostMapping("/create")',
    'public R<Long> create(',
    '@PostMapping("/update")',
    '@PostMapping("/delete")',
    '@GetMapping("/page")',
    '@GetMapping("/detail")',
    '@RequestParam("id")',
    'return R.ok(sealService.',
    '创建合同印章',
    '修改合同印章',
    '删除合同印章',
    '分页查询合同印章',
    '查询合同印章详情',
  ]) {
    if (!moduleController.includes(expected)) {
      throw new Error(`module add controller missing API adapter contract: ${expected}`);
    }
  }
  if (
    moduleController.includes('extends BaseCrudController') ||
    moduleController.includes('PathVariable') ||
    moduleController.includes('/{')
  ) {
    throw new Error('module add generated a legacy or path-variable controller');
  }
  const moduleFeign = readFileSync(
    join(
      customRoot,
      'backend/modules/contract/contract-starter-remote/src/main/java/com/example/custom/contract/starter/remote/ContractFeignClient.java',
    ),
    'utf8',
  );
  for (const expected of [
    'public interface ContractFeignClient extends ContractApi',
    '@FeignClient(',
    'contextId = "contractFeignClient"',
    'path = "/contract/seals"',
    '@PostMapping("/create")',
    '@PostMapping("/update")',
    '@PostMapping("/delete")',
    '@GetMapping("/page")',
    '@SpringQueryMap',
    '@GetMapping("/detail")',
    '@RequestParam("id")',
  ]) {
    if (!moduleFeign.includes(expected)) {
      throw new Error(`module add Feign adapter missing contract: ${expected}`);
    }
  }
  if (moduleFeign.includes('PathVariable') || moduleFeign.includes('/{')) {
    throw new Error('module add generated a path-variable Feign contract');
  }
  const moduleMigration = readFileSync(
    join(
      customRoot,
      'backend/modules/contract/contract-core/src/main/resources/db/migration/contract/V1__init_contract.sql',
    ),
    'utf8',
  );
  for (const expected of ['tenant_id', 'org_id', 'created_by', 'created_at', 'updated_by', 'updated_at']) {
    if (!moduleMigration.includes(expected)) {
      throw new Error(`module add migration missing persistence baseline field: ${expected}`);
    }
  }
  const moduleApi = readFileSync(join(customRoot, 'frontend/packages/contract-api/src/api.ts'), 'utf8');
  if (moduleApi.includes('@mango/common') || moduleApi.includes('axios')) {
    throw new Error('module add generated frontend API must not depend on a transport implementation');
  }
  const generatedFrontendPackage = JSON.parse(readFileSync(join(customRoot, 'frontend/package.json'), 'utf8'));
  for (const dependency of ['@mango-custom-acceptance/contract', '@mango-custom-acceptance/contract-api']) {
    if (generatedFrontendPackage.dependencies?.[dependency] !== 'workspace:1.0.0-SNAPSHOT') {
      throw new Error(`module add must pin local business package ${dependency} with an exact workspace version`);
    }
  }
  for (const expected of [
    "import type { HttpClient } from '@mango/api-schema';",
    'export function createSealApi(client: HttpClient)',
    "method: 'GET'",
    "method: 'POST'",
    'signal,',
  ]) {
    if (!moduleApi.includes(expected)) {
      throw new Error(`module add generated frontend API missing client contract: ${expected}`);
    }
  }
  const generatedTsConfig = readFileSync(join(customRoot, 'frontend/tsconfig.json'), 'utf8');
  if (generatedTsConfig.includes('mango-common.d.ts')) {
    throw new Error('generated frontend tsconfig must use the published @mango/common type surface');
  }
  if (
    !moduleApi.includes('`${basePath}/create`') ||
    !moduleApi.includes('`${basePath}/update`') ||
    !moduleApi.includes('`${basePath}/delete`') ||
    !moduleApi.includes('`${basePath}/detail`') ||
    !moduleApi.includes('`${basePath}/page`')
  ) {
    throw new Error('module add did not generate standard CRUD frontend API');
  }
  const modulePage = readFileSync(
    join(customRoot, 'frontend/packages/contract/src/views/contract/seal/index.vue'),
    'utf8',
  );
  for (const expected of [
    "defineOptions({ name: 'SealListPage' })",
    'MangoListPage',
    'MangoSearchPanel',
    'MangoListPanel',
    'Pagination',
    'openCreateDialog',
    'openEditDialog',
    'openDetail',
    'handleDelete',
    'el-dialog',
    'el-drawer',
    'useSealApi',
    'sealApi.update',
    'sealApi.delete',
    'sealApi.detail',
    'pageAbortController.signal',
    'onBeforeUnmount',
    '合同印章名称',
    '新增合同印章',
    '编辑合同印章',
    '合同印章详情',
  ]) {
    if (!modulePage.includes(expected)) {
      throw new Error(`module add generated frontend page missing CRUD capability: ${expected}`);
    }
  }
  const moduleManifest = JSON.parse(
    readFileSync(
      join(
        customRoot,
        'backend/modules/contract/contract-starter/src/main/resources/META-INF/mango/resource-manifest.json',
      ),
      'utf8',
    ),
  );
  const moduleRootMenu = moduleManifest.menus?.[0];
  const moduleChildMenu = moduleRootMenu?.children?.[0];
  if (moduleRootMenu?.menuType !== 1 || moduleRootMenu?.redirect !== '/contract/seals') {
    throw new Error('module add did not configure directory menu redirect to first runnable child menu');
  }
  if (moduleChildMenu?.path !== '/contract/seals' || moduleChildMenu?.component !== 'contract/seal/index') {
    throw new Error('module add did not generate the expected child menu route/component');
  }
  if (moduleChildMenu?.menuName !== '合同印章管理') {
    throw new Error('module add did not render aggregate display name in menu metadata');
  }
  const permissionCodes = new Set(moduleChildMenu?.permissions || []);
  for (const permissionCode of [
    'contract:seal:create',
    'contract:seal:view',
    'contract:seal:update',
    'contract:seal:delete',
  ]) {
    if (!permissionCodes.has(permissionCode)) {
      throw new Error(`module add did not generate expected permission: ${permissionCode}`);
    }
  }
  const permissionNames = new Set((moduleChildMenu?.permissionItems || []).map((item) => item.permissionName));
  for (const permissionName of ['新增合同印章', '查看合同印章', '编辑合同印章', '删除合同印章']) {
    if (!permissionNames.has(permissionName)) {
      throw new Error(`module add did not generate expected permission name: ${permissionName}`);
    }
  }
  const moduleApiJava = readFileSync(
    join(
      customRoot,
      'backend/modules/contract/contract-api/src/main/java/com/example/custom/contract/api/ContractApi.java',
    ),
    'utf8',
  );
  for (const expected of [
    'public interface ContractApi',
    'R<Long> create(@Valid CreateSealCommand command)',
    'R<Boolean> update(@Valid UpdateSealCommand command)',
    'R<Boolean> delete(@Valid DeleteCommand command)',
    'R<PersistencePageResult<SealVO>> page(@Valid SealPageQuery query)',
    'R<SealVO> detail(@NotNull Long id)',
  ]) {
    if (!moduleApiJava.includes(expected)) {
      throw new Error(`module add API contract missing typed method: ${expected}`);
    }
  }
  for (const forbidden of [
    'org.springframework.web.bind.annotation',
    '@RequestMapping',
    '@GetMapping',
    '@PostMapping',
    '@RequestBody',
    '@RequestParam',
    'PathVariable',
    '/{',
  ]) {
    if (moduleApiJava.includes(forbidden)) {
      throw new Error(`module add API contract contains transport concern: ${forbidden}`);
    }
  }
  const moduleBizCode = readFileSync(
    join(
      customRoot,
      'backend/modules/contract/contract-api/src/main/java/com/example/custom/contract/api/enums/SealCode.java',
    ),
    'utf8',
  );
  for (const expected of [
    'public enum SealCode implements BizCode',
    'NOT_FOUND(404, "合同印章不存在")',
    'VALIDATION_ERROR(400, "合同印章参数校验失败")',
  ]) {
    if (!moduleBizCode.includes(expected)) {
      throw new Error(`module add BizCode contract missing: ${expected}`);
    }
  }
  const generatedBackendJava = walkFiles(join(customRoot, 'backend/modules/contract'))
    .filter((file) => file.endsWith('.java'))
    .map((file) => readFileSync(file, 'utf8'))
    .join('\n');
  if (generatedBackendJava.includes('PathVariable') || generatedBackendJava.includes('/{')) {
    throw new Error('module add backend template must not generate path-variable endpoints');
  }
  const moduleMain = readFileSync(join(customRoot, 'frontend/src/main.ts'), 'utf8');
  if (
    !moduleMain.includes("import { registerContractPages } from '@mango-custom-acceptance/contract';") ||
    !moduleMain.includes("import '@mango-custom-acceptance/contract/style.css';") ||
    !moduleMain.includes("import { createMangoHttpClient } from '@mango/http-client';") ||
    !moduleMain.includes("import { Session } from '@mango/common';") ||
    !moduleMain.includes("getTenantId: () => Session.get('userInfo')?.tenantId ?? Session.get('tenantId')") ||
    !moduleMain.includes('MangoAdminBootstrapHooks,') ||
    !moduleMain.includes('MangoAdminFeatureRegistrar,') ||
    !moduleMain.includes('MangoAdminShellOptions') ||
    !moduleMain.includes('const mangoBusinessHttpClient = createMangoHttpClient({') ||
    !moduleMain.includes('const mangoBusinessFeatureRegistrars: MangoAdminFeatureRegistrar[] = [') ||
    !moduleMain.includes('const mangoAllFeatureRegistrars: MangoAdminFeatureRegistrar[] = [') ||
    !moduleMain.includes(
      'const mangoBusinessFeatureRegistrars: MangoAdminFeatureRegistrar[] = [() => registerContractPages()];',
    ) ||
    !moduleMain.includes("import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';") ||
    !moduleMain.includes('const mangoAdminBootstrapHooks: MangoAdminBootstrapHooks = {') ||
    !moduleMain.includes('beforeMount(mangoAdminApp) {') ||
    !moduleMain.includes('mangoAdminApp.app.provide(MANGO_HTTP_CLIENT_KEY, mangoBusinessHttpClient);') ||
    !moduleMain.includes('bootstrapMangoAdminApp(mangoAdminOptions, mangoAdminBootstrapHooks);') ||
    moduleMain.includes('createMangoAdminApp({') ||
    moduleMain.includes('registerContractPages(mangoBusinessHttpClient)') ||
    !moduleMain.includes('featureRegistrars: mangoAllFeatureRegistrars')
  ) {
    throw new Error('module add did not register frontend feature registrar and style entry');
  }
  const moduleConfig = JSON.parse(readFileSync(join(customRoot, 'mango.config.json'), 'utf8'));
  if (!Array.isArray(moduleConfig.businessModules) || moduleConfig.businessModules[0]?.module !== 'contract') {
    throw new Error('module add did not update mango.config.json businessModules');
  }
  if (moduleConfig.businessModules[0]?.aggregateDisplayName !== '合同印章') {
    throw new Error('module add did not persist aggregate display name');
  }
  const modulePackageJson = JSON.parse(readFileSync(join(customRoot, 'frontend/package.json'), 'utf8'));
  if (!Array.isArray(modulePackageJson.workspaces) || !modulePackageJson.workspaces.includes('packages/*')) {
    throw new Error('module add did not configure frontend workspaces');
  }
  const businessUiPackageJson = JSON.parse(
    readFileSync(join(customRoot, 'frontend/packages/contract/package.json'), 'utf8'),
  );
  if (
    businessUiPackageJson.style !== './style.css' ||
    businessUiPackageJson.exports?.['./style.css'] !== './style.css' ||
    businessUiPackageJson.dependencies?.['@mango/app-runtime'] !== releaseVersions.npm['@mango/app-runtime'] ||
    businessUiPackageJson.mangoAdmin?.businessDomainCode !== 'CONTRACT' ||
    businessUiPackageJson.mangoAdmin?.businessDomainName !== '合同管理' ||
    businessUiPackageJson.mangoAdmin?.registrars?.[0]?.name !== 'registerContractPages' ||
    businessUiPackageJson.mangoAdmin?.registrars?.[0]?.import !== '@mango-custom-acceptance/contract'
  ) {
    throw new Error('module add did not generate business UI package admin manifest');
  }
  if (!existsSync(join(customRoot, 'frontend/packages/contract/style.css'))) {
    throw new Error('module add did not generate business UI package style entry');
  }
  const businessUiIndex = readFileSync(join(customRoot, 'frontend/packages/contract/src/index.ts'), 'utf8');
  for (const expected of ["businessDomainCode: 'CONTRACT'", "businessDomainName: '合同管理'", 'widgets: []']) {
    if (!businessUiIndex.includes(expected)) {
      throw new Error(`module add did not generate feature registration metadata: ${expected}`);
    }
  }
  const businessApiContext = readFileSync(join(customRoot, 'frontend/packages/contract/src/api-context.ts'), 'utf8');
  for (const expected of [
    "import { inject } from 'vue';",
    "import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';",
    'const apiByClient = new WeakMap<HttpClient, SealApi>();',
    'export function useSealApi(): SealApi',
    'inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined)',
  ]) {
    if (!businessApiContext.includes(expected)) {
      throw new Error(`module add API context missing instance isolation contract: ${expected}`);
    }
  }
  for (const forbidden of ['let sealApi', 'configureSealApi(', 'getSealApi()']) {
    if (businessApiContext.includes(forbidden)) {
      throw new Error(`module add API context contains module singleton contract: ${forbidden}`);
    }
  }
  const businessApiContextTest = readFileSync(
    join(customRoot, 'frontend/packages/contract/src/__tests__/api-context.spec.ts'),
    'utf8',
  );
  for (const expected of [
    'keeps HttpClient instances isolated between Vue apps',
    'firstApp.provide(MANGO_HTTP_CLIENT_KEY',
    'secondApp.provide(MANGO_HTTP_CLIENT_KEY',
    'fails closed when the current Vue app has no HttpClient provider',
  ]) {
    if (!businessApiContextTest.includes(expected)) {
      throw new Error(`module add API context test missing isolation assertion: ${expected}`);
    }
  }
  assertNoUnrenderedPlaceholders(customRoot);
  if (process.platform !== 'win32') {
    assertDevWorkspaceRunnerScenarios(tempRoot);
  }

  console.log('mango-cli full/custom/add/module/pmo sync checks passed.');
} finally {
  if (process.env.MANGO_CLI_KEEP_TEMP === 'true') {
    console.log(`mango-cli diagnostic project retained at ${tempRoot}`);
  } else {
    rmSync(tempRoot, { recursive: true, force: true, maxRetries: 10, retryDelay: 200 });
  }
}

function assertEqual(actual, expected, field) {
  if (actual !== expected) {
    throw new Error(`${field} expected ${expected}, got ${actual}`);
  }
}

function assertGeneratedFrontendFormatting(projectRoot) {
  const localPrettierCommand = join(
    packageRoot,
    '..',
    '..',
    'node_modules',
    '.bin',
    process.platform === 'win32' ? 'prettier.cmd' : 'prettier',
  );
  const frontendRoot = join(projectRoot, 'frontend');
  const frontendPackage = JSON.parse(readFileSync(join(frontendRoot, 'package.json'), 'utf8'));
  const prettierVersion = frontendPackage.devDependencies?.prettier;
  if (!/^\d+\.\d+\.\d+$/u.test(prettierVersion ?? '')) {
    throw new Error(`generated frontend must lock an exact Prettier version, got ${prettierVersion ?? 'missing'}`);
  }

  const useLocalPrettier = existsSync(localPrettierCommand);
  const prettierCommand = useLocalPrettier ? localPrettierCommand : process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
  const fallbackRegistry = process.env.NPM_CONFIG_REGISTRY?.trim() || 'https://registry.npmjs.org/';
  const prettierArgs = (args) =>
    useLocalPrettier ? args : [`--config.registry=${fallbackRegistry}`, 'dlx', `prettier@${prettierVersion}`, ...args];
  const runPrettier = (args, options = {}) =>
    spawnSync(prettierCommand, prettierArgs(args), {
      cwd: frontendRoot,
      encoding: 'utf8',
      shell: shouldUseShellForCommand(prettierCommand),
      ...options,
    });

  const result = runPrettier(['--check', '.']);
  if (result.error) {
    throw new Error(`generated frontend formatting could not start Prettier: ${result.error.message}`);
  }
  if (result.status !== 0) {
    const stdout = result.stdout ?? '';
    const stderr = result.stderr ?? '';
    const reportedFile = stdout.match(/^\[warn\] (.+)$/mu)?.[1] ?? 'src/main.ts';
    const reportedPath = join(frontendRoot, reportedFile);
    const actual = readFileSync(reportedPath, 'utf8');
    const formatted = runPrettier(['--stdin-filepath', reportedFile], { input: actual });
    if (formatted.error || formatted.status !== 0 || typeof formatted.stdout !== 'string') {
      const diagnostic = formatted.error?.message ?? `${formatted.stdout ?? ''}\n${formatted.stderr ?? ''}`;
      throw new Error(
        `generated frontend formatting failed and Prettier diagnostics failed:\n${stdout}\n${stderr}\n${diagnostic}`,
      );
    }
    const actualLines = actual.split(/\r?\n/u);
    const expectedLines = formatted.stdout.split(/\r?\n/u);
    const differenceIndex = actualLines.findIndex((line, index) => line !== expectedLines[index]);
    const difference =
      differenceIndex < 0
        ? 'unable to locate line difference'
        : `line ${differenceIndex + 1}: actual=${JSON.stringify(actualLines[differenceIndex])}, expected=${JSON.stringify(expectedLines[differenceIndex])}`;
    throw new Error(`generated frontend formatting failed:\n${stdout}\n${stderr}\nfirst difference: ${difference}`);
  }
}

function assertGeneratedPnpmWorkspace(workspacePath) {
  const workspace = readFileSync(workspacePath, 'utf8');
  const requiredLines = [
    "  - 'packages/*'",
    'allowBuilds:',
    "  '@swc/core': true",
    '  core-js-pure: true',
    '  es5-ext: true',
    '  esbuild: true',
    '  msw: true',
    '  vue-demi: true',
  ];
  for (const line of requiredLines) {
    if (!workspace.split(/\r?\n/u).includes(line)) {
      throw new Error(`generated pnpm workspace missing required install policy: ${line}`);
    }
  }
}

function extractNamedWorkflowStep(workflow, name) {
  const marker = `      - name: ${name}`;
  const start = workflow.indexOf(marker);
  if (start < 0) {
    throw new Error(`generated PMO workflow missing step: ${name}`);
  }
  const remainder = workflow.slice(start + marker.length);
  const boundary = remainder.search(/\n(?:      - name:|  [A-Za-z0-9_-]+:)/u);
  const end = boundary >= 0 ? start + marker.length + boundary : workflow.length;
  return workflow.slice(start, end);
}

function assertIncludes(values, expected, field) {
  if (!values.includes(expected)) {
    throw new Error(`${field} expected to include ${expected}`);
  }
}

function assertPackagedAdminModules() {
  if (JSON.stringify(packagedAdminModules) !== JSON.stringify(sourceAdminModules)) {
    throw new Error('packaged @mango/cli admin-modules.json must match @mango/admin admin-modules.json');
  }
  if (!cliPackage.files.includes('admin-modules.json')) {
    throw new Error('@mango/cli package files must include admin-modules.json');
  }
}

function assertNoTrailingBlankLinesAtEof(root, label) {
  for (const file of walkFiles(root)) {
    const content = readFileSync(file, 'utf8');
    if (content.endsWith('\n\n')) {
      throw new Error(`${label} file has trailing blank line at EOF: ${file}`);
    }
  }
}

function assertNoBundledTemplatePmoBaseline() {
  const templateBaselineRoot = join(packageRoot, 'templates/full/business-pmo/mango-baseline');
  if (existsSync(templateBaselineRoot)) {
    throw new Error(
      'CLI templates must not bundle business-pmo/mango-baseline; use @mango/pmo as the single baseline source',
    );
  }
}

function assertPmoPackageBuilt() {
  const build = spawnSync(process.execPath, ['scripts/build-package.mjs'], {
    cwd: pmoPackageRoot,
    encoding: 'utf8',
  });
  if (build.status !== 0) {
    throw new Error(`@mango/pmo build failed:\n${build.stdout}\n${build.stderr}`);
  }
  const check = spawnSync(process.execPath, ['scripts/check-package.mjs'], {
    cwd: pmoPackageRoot,
    encoding: 'utf8',
  });
  if (check.status !== 0) {
    throw new Error(`@mango/pmo check failed:\n${check.stdout}\n${check.stderr}`);
  }
  if (!existsSync(join(pmoPackageRoot, 'dist/baseline.json'))) {
    throw new Error('@mango/pmo build did not create dist/baseline.json');
  }
}

function assertPackedCliPullRequestTemplate(tempRoot) {
  const packRoot = join(tempRoot, 'packed-cli-pmo-contract');
  const cliPackRoot = join(packRoot, 'cli');
  const pmoPackRoot = join(packRoot, 'pmo');
  const cliExtractRoot = join(cliPackRoot, 'extract');
  const pmoExtractRoot = join(pmoPackRoot, 'extract');
  mkdirSync(cliExtractRoot, { recursive: true });
  mkdirSync(pmoExtractRoot, { recursive: true });

  const cliPack = spawnSync('pnpm', ['pack', '--pack-destination', cliPackRoot], {
    cwd: packageRoot,
    encoding: 'utf8',
  });
  if (cliPack.status !== 0) {
    throw new Error(`@mango/cli pack failed:\n${cliPack.stdout}\n${cliPack.stderr}`);
  }
  const pmoPack = spawnSync('pnpm', ['pack', '--pack-destination', pmoPackRoot], {
    cwd: pmoPackageRoot,
    encoding: 'utf8',
  });
  if (pmoPack.status !== 0) {
    throw new Error(`@mango/pmo pack failed:\n${pmoPack.stdout}\n${pmoPack.stderr}`);
  }

  const cliTarballs = readdirSync(cliPackRoot).filter((file) => file.endsWith('.tgz'));
  const pmoTarballs = readdirSync(pmoPackRoot).filter((file) => file.endsWith('.tgz'));
  if (cliTarballs.length !== 1 || pmoTarballs.length !== 1) {
    throw new Error(
      `expected one CLI and one PMO tarball; CLI=${cliTarballs.join(', ') || 'none'} PMO=${pmoTarballs.join(', ') || 'none'}`,
    );
  }
  const cliExtract = spawnSync('tar', ['-xzf', join(cliPackRoot, cliTarballs[0]), '-C', cliExtractRoot], {
    encoding: 'utf8',
  });
  if (cliExtract.status !== 0) {
    throw new Error(`@mango/cli tarball extraction failed:\n${cliExtract.stdout}\n${cliExtract.stderr}`);
  }
  const pmoExtract = spawnSync('tar', ['-xzf', join(pmoPackRoot, pmoTarballs[0]), '-C', pmoExtractRoot], {
    encoding: 'utf8',
  });
  if (pmoExtract.status !== 0) {
    throw new Error(`@mango/pmo tarball extraction failed:\n${pmoExtract.stdout}\n${pmoExtract.stderr}`);
  }

  const packedTemplate = join(cliExtractRoot, 'package/templates/full/.github/pull_request_template.md');
  const packedPmoBaseline = join(pmoExtractRoot, 'package/dist/baseline');
  const canonicalTemplate = join(packedPmoBaseline, 'templates/business-pull-request-template.md');
  const packagedChecker = join(packedPmoBaseline, 'tools/risk-verification.mjs');
  if (!existsSync(packedTemplate)) {
    throw new Error('@mango/cli tarball is missing templates/full/.github/pull_request_template.md');
  }
  if (!readFileSync(packedTemplate).equals(readFileSync(canonicalTemplate))) {
    throw new Error('@mango/cli tarball PR template must match the canonical @mango/pmo template');
  }

  const check = spawnSync(process.execPath, [packagedChecker, '--template', '--body', packedTemplate], {
    encoding: 'utf8',
  });
  if (check.status !== 0) {
    throw new Error(
      `packed @mango/cli PR template failed @mango/pmo contract validation:\n${check.stdout}\n${check.stderr}`,
    );
  }
}

function assertPublishedPnpmPmoResolution(tempRoot) {
  const publishedRoot = join(tempRoot, 'published-pnpm-layout');
  const packageStoreRoot = join(
    publishedRoot,
    `node_modules/.pnpm/@mango+cli@${cliPackage.version}/node_modules/@mango`,
  );
  const publishedCliRoot = join(packageStoreRoot, 'cli');
  const publishedPmoRoot = join(packageStoreRoot, 'pmo');
  mkdirSync(join(publishedCliRoot, 'src'), { recursive: true });
  mkdirSync(publishedPmoRoot, { recursive: true });
  for (const file of ['package.json', 'release-versions.json', 'admin-modules.json']) {
    cpSync(join(packageRoot, file), join(publishedCliRoot, file));
  }
  cpSync(join(packageRoot, 'src'), join(publishedCliRoot, 'src'), { recursive: true });
  cpSync(join(packageRoot, 'templates'), join(publishedCliRoot, 'templates'), { recursive: true });
  cpSync(join(pmoPackageRoot, 'package.json'), join(publishedPmoRoot, 'package.json'));
  cpSync(join(pmoPackageRoot, 'dist'), join(publishedPmoRoot, 'dist'), { recursive: true });

  const projectRoot = join(tempRoot, 'published-pnpm-business');
  mkdirSync(projectRoot, { recursive: true });

  const publishedCli = join(publishedCliRoot, 'src/index.mjs');
  assertCommandOk(
    [publishedCli, 'pmo', 'upgrade', '--project-dir', projectRoot],
    projectRoot,
    'published pnpm mango pmo upgrade',
  );
  const status = assertCommandOk(
    [publishedCli, 'pmo', 'status', '--project-dir', projectRoot],
    projectRoot,
    'published pnpm mango pmo status',
  );
  if (!status.stdout.includes('Baseline: @mango/pmo@') || status.stdout.includes('@mango/cli-template')) {
    throw new Error(`published pnpm CLI should consume @mango/pmo baseline:\n${status.stdout}`);
  }
  assertCommandOk(
    [publishedCli, 'pmo', 'check', '--project-dir', projectRoot],
    projectRoot,
    'published pnpm mango pmo check',
  );
}

function assertDocsBundleCommands(projectRoot, tempRoot) {
  const config = JSON.parse(readFileSync(join(projectRoot, 'mango.config.json'), 'utf8'));
  const version = config.mangoBackendVersion;
  const docsRepoRoot = join(tempRoot, 'docs-maven-repo');
  const expectedFiles = createFakeDocsBundle(docsRepoRoot, version);
  const repositoryUrl = pathToFileURL(`${docsRepoRoot}/`).toString();

  const statusBefore = assertCommandOk(
    [cli, 'docs', 'status', '--project-dir', projectRoot],
    projectRoot,
    'mango docs status before pull',
  );
  if (!statusBefore.stdout.includes(`mangoVersion: ${version}`) || !statusBefore.stdout.includes('installed: no')) {
    throw new Error(`mango docs status should report the project docs version before pull:\n${statusBefore.stdout}`);
  }

  const pull = assertCommandOk(
    [cli, 'docs', 'pull', '--project-dir', projectRoot, '--maven-repository', repositoryUrl],
    projectRoot,
    'mango docs pull',
  );
  if (!pull.stdout.includes(`Pulled Mango docs ${version}`)) {
    throw new Error(`mango docs pull did not report the pulled version:\n${pull.stdout}`);
  }

  const pathResult = assertCommandOk(
    [cli, 'docs', 'path', '--project-dir', projectRoot],
    projectRoot,
    'mango docs path',
  );
  const docsPath = pathResult.stdout.trim();
  if (!docsPath.endsWith('META-INF/mango-docs') || !existsSync(docsPath)) {
    throw new Error(`mango docs path should point at extracted META-INF/mango-docs:\n${pathResult.stdout}`);
  }
  for (const [relativePath, expectedContent] of Object.entries(expectedFiles)) {
    assertEqual(
      readFileSync(join(docsPath, relativePath), 'utf8'),
      expectedContent,
      `docs bundle file ${relativePath}`,
    );
  }

  const current = JSON.parse(readFileSync(join(projectRoot, '.mango/docs/current.json'), 'utf8'));
  assertEqual(current.version, version, 'docs current version');
  assertEqual(current.artifact, `io.mango:mango-docs-bundle:${version}`, 'docs current artifact');
  if (!current.sourceUrl.includes(`/io/mango/mango-docs-bundle/${version}/mango-docs-bundle-${version}.jar`)) {
    throw new Error(`docs current sourceUrl should point at Maven artifact:\n${current.sourceUrl}`);
  }

  const statusAfter = assertCommandOk(
    [cli, 'docs', 'status', '--project-dir', projectRoot],
    projectRoot,
    'mango docs status after pull',
  );
  if (!statusAfter.stdout.includes('installed: yes') || !statusAfter.stdout.includes(`currentVersion: ${version}`)) {
    throw new Error(`mango docs status should report installed docs after pull:\n${statusAfter.stdout}`);
  }
}

function createFakeDocsBundle(repoRoot, version) {
  const stagingRoot = join(repoRoot, 'staging');
  const docsRoot = join(stagingRoot, 'META-INF/mango-docs');
  const artifactDir = join(repoRoot, 'io/mango/mango-docs-bundle', version);
  const jarPath = join(artifactDir, `mango-docs-bundle-${version}.jar`);
  const files = {
    'README.md': `# Mango Docs ${version}\n\nBusiness project docs entry.\n`,
    'capabilities/README.md': '# Capability Index\n\nUse local versioned capability docs first.\n',
    'agents/context.md': 'AI agents must prefer this local docs bundle over stale conversation context.\n',
    'rules/dev-flow.md': 'Development flow facts come from the versioned docs bundle.\n',
    'examples/workflow.md': 'Workflow initialization examples are packaged with the matching Mango version.\n',
  };
  rmSync(stagingRoot, { recursive: true, force: true });
  mkdirSync(docsRoot, { recursive: true });
  for (const [relativePath, content] of Object.entries(files)) {
    const target = join(docsRoot, relativePath);
    mkdirSync(resolve(target, '..'), { recursive: true });
    writeFileSync(target, content);
  }
  mkdirSync(artifactDir, { recursive: true });
  const jar = spawnSync('jar', ['cf', jarPath, '-C', stagingRoot, '.'], {
    encoding: 'utf8',
  });
  if (jar.status !== 0) {
    throw new Error(`failed to create fake docs bundle jar:\n${jar.stdout}\n${jar.stderr}`);
  }
  return files;
}

function assertCommandOk(args, cwd, label) {
  const result = spawnSync(process.execPath, args, {
    cwd,
    encoding: 'utf8',
  });
  if (result.status !== 0) {
    throw new Error(`${label} failed:\n${result.stdout}\n${result.stderr}`);
  }
  return result;
}

function assertGeneratedDevWorkspaceUsesCliFallback(projectRoot) {
  if (process.platform === 'win32') {
    return;
  }
  const fakeLocalBinDir = join(projectRoot, '.runtime/fake-local-bin');
  mkdirSync(fakeLocalBinDir, { recursive: true });
  const frontendBinDir = join(projectRoot, 'frontend/node_modules/.bin');
  mkdirSync(frontendBinDir, { recursive: true });
  const fakeLocalMangoPath = join(frontendBinDir, 'mango');
  writeFileSync(fakeLocalMangoPath, '#!/usr/bin/env sh\necho local-mango-bin \"$@\"\n');
  chmodExecutable(fakeLocalMangoPath);
  const fakePnpmPath = join(fakeLocalBinDir, 'pnpm');
  writeFileSync(fakePnpmPath, '#!/usr/bin/env sh\necho local-pnpm-runner \"cwd=$(pwd)\" \"$@\"\n');
  chmodExecutable(fakePnpmPath);
  const result = spawnSync(
    'env',
    [`PATH=${fakeLocalBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`, 'scripts/dev-workspace.sh', 'validate'],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  if (
    result.status !== 0 ||
    !result.stdout.includes('local-pnpm-runner cwd=') ||
    !result.stdout.includes('/frontend exec mango workspace doctor')
  ) {
    throw new Error(
      `generated dev-workspace should use project-local pnpm exec mango first:\n${result.stdout}\n${result.stderr}`,
    );
  }

  rmSync(fakeLocalMangoPath, { force: true });
  const fakeGlobalBinDir = join(projectRoot, '.runtime/fake-global-bin');
  mkdirSync(fakeGlobalBinDir, { recursive: true });
  const fakeMangoPath = join(fakeGlobalBinDir, 'mango');
  writeFileSync(fakeMangoPath, '#!/usr/bin/env sh\necho global-mango-runner \"$@\"\n');
  chmodExecutable(fakeMangoPath);
  const globalResult = spawnSync(
    'env',
    [
      `PATH=${fakeGlobalBinDir}:${fakeLocalBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`,
      'scripts/dev-workspace.sh',
      'validate',
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  if (globalResult.status !== 0 || !globalResult.stdout.includes('global-mango-runner workspace doctor')) {
    throw new Error(
      `generated dev-workspace should fallback to global mango CLI:\n${globalResult.stdout}\n${globalResult.stderr}`,
    );
  }

  rmSync(fakePnpmPath, { force: true });
  const missingResult = spawnSync(
    'env',
    [`PATH=${fakeLocalBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`, 'scripts/dev-workspace.sh', 'validate'],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  const output = `${missingResult.stdout}\n${missingResult.stderr}`;
  if (
    missingResult.status === 0 ||
    !output.includes('mango CLI not found in project frontend dependencies or globally') ||
    !output.includes('cd frontend && pnpm install') ||
    !output.includes('npm install -g @mango/cli@') ||
    !output.includes('--registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/')
  ) {
    throw new Error(`generated dev-workspace should fail clearly without global or local mango:\n${output}`);
  }
}

function assertGeneratedDevWorkspaceRejectsInitShim(projectRoot) {
  if (process.platform === 'win32') {
    return;
  }
  const result = spawnSync('scripts/dev-workspace.sh', ['init'], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  const output = `${result.stdout}\n${result.stderr}`;
  if (
    result.status === 0 ||
    !output.includes('Usage: scripts/dev-workspace.sh <command>') ||
    output.includes('run_mango workspace init')
  ) {
    throw new Error(`generated dev-workspace init shim should be removed:\n${output}`);
  }
}

function assertGeneratedDevWorkspaceCreatesLocalSecretKey(projectRoot) {
  if (process.platform === 'win32') {
    return;
  }
  const fakeBinDir = join(projectRoot, '.runtime/init-fake-bin');
  mkdirSync(fakeBinDir, { recursive: true });
  const fakeLocalMangoDir = join(projectRoot, 'frontend/node_modules/.bin');
  mkdirSync(fakeLocalMangoDir, { recursive: true });
  const fakeMangoPath = join(fakeLocalMangoDir, 'mango');
  writeFileSync(fakeMangoPath, `#!/usr/bin/env sh\nexec "${process.execPath}" "${cli}" "$@"\n`);
  chmodExecutable(fakeMangoPath);
  const fakePnpmPath = join(fakeBinDir, 'pnpm');
  writeFileSync(
    fakePnpmPath,
    '#!/usr/bin/env sh\nif [ "$1" = "exec" ]; then shift; exec "./node_modules/.bin/$@"; fi\necho "unsupported fake pnpm command: $*" >&2\nexit 1\n',
  );
  chmodExecutable(fakePnpmPath);
  rmSync(join(projectRoot, '.mango'), { recursive: true, force: true });
  const result = spawnSync(
    'env',
    [
      `MANGO_WORKSPACE_REGISTRY=${join(projectRoot, '.runtime/init-workspaces.json')}`,
      `PATH=${fakeBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`,
      process.execPath,
      cli,
      'workspace',
      'init',
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  if (result.status !== 0) {
    throw new Error(`mango workspace init should succeed for generated project:\n${result.stdout}\n${result.stderr}`);
  }
  const envFile = readFileSync(join(projectRoot, '.mango/dev-workspace.env'), 'utf8');
  const match = envFile.match(/^MANGO_CRYPTO_SM4_SECRET_KEY=([0-9a-f]{32})$/m);
  if (!match) {
    throw new Error(`generated dev-workspace env must contain a random 16-byte SM4 key:\n${envFile}`);
  }
  if (match[1] === '00112233445566778899aabbccddeeff') {
    throw new Error('generated dev-workspace env must not use the public fixed SM4 key');
  }
  for (const expected of [
    /^MANGO_WORKSPACE_ID=mango_[0-9]{3}$/m,
    /^MANGO_MAVEN_REVISION_QUALIFIER=mango-[0-9]{3}$/m,
    /^MANGO_BACKEND_PORT=[0-9]+$/m,
    /^MANGO_FRONTEND_PORT=[0-9]+$/m,
    /^MANGO_DB_NAME=mango_dev_mango_full_acceptance_[0-9]{3}$/m,
  ]) {
    if (!expected.test(envFile)) {
      throw new Error(
        `generated dev-workspace env must contain allocated workspace values matching ${expected}:\n${envFile}`,
      );
    }
  }
  const backendPort = Number(envFile.match(/^MANGO_BACKEND_PORT=([0-9]+)$/m)?.[1] || 0);
  if (backendPort < 18001 || backendPort > 18200) {
    throw new Error(`generated backend port must be in allocated range 18001-18200:\n${envFile}`);
  }
  const frontendPort = Number(envFile.match(/^MANGO_FRONTEND_PORT=([0-9]+)$/m)?.[1] || 0);
  if (frontendPort < 30001 || frontendPort > 30200) {
    throw new Error(`generated frontend port must be in allocated range 30001-30200:\n${envFile}`);
  }
  const workspaceConfig = JSON.parse(readFileSync(join(projectRoot, '.mango/workspace.json'), 'utf8'));
  if (
    workspaceConfig.backendPort !== backendPort ||
    workspaceConfig.frontendPort !== frontendPort ||
    workspaceConfig.dbName !== envFile.match(/^MANGO_DB_NAME=(.+)$/m)?.[1] ||
    workspaceConfig.mavenRevisionQualifier !== envFile.match(/^MANGO_MAVEN_REVISION_QUALIFIER=(.+)$/m)?.[1]
  ) {
    throw new Error(
      `generated workspace.json must match dev-workspace.env:\n${JSON.stringify(workspaceConfig, null, 2)}\n${envFile}`,
    );
  }
  if (
    workspaceConfig.backendPort !== 18000 + workspaceConfig.slot ||
    workspaceConfig.frontendPort !== 30000 + workspaceConfig.slot ||
    workspaceConfig.frontendApps.MANGO_ADMIN_SHELL_PORT !== 31000 + workspaceConfig.slot ||
    workspaceConfig.frontendApps.MANGO_ADMIN_RBAC_APP_PORT !== 32000 + workspaceConfig.slot
  ) {
    throw new Error(
      `generated workspace ports must share the workspace number:\n${JSON.stringify(workspaceConfig, null, 2)}`,
    );
  }
}

function assertGeneratedDevWorkspaceBackfillsLocalSecretKey(projectRoot) {
  if (process.platform === 'win32') {
    return;
  }
  const fakeBinDir = join(projectRoot, '.runtime/backfill-fake-bin');
  mkdirSync(fakeBinDir, { recursive: true });
  const fakeMangoPath = join(fakeBinDir, 'mango');
  writeFileSync(fakeMangoPath, `#!/usr/bin/env sh\nexec "${process.execPath}" "${cli}" "$@"\n`);
  chmodExecutable(fakeMangoPath);
  rmSync(join(projectRoot, '.mango'), { recursive: true, force: true });
  mkdirSync(join(projectRoot, '.mango'), { recursive: true });
  writeFileSync(
    join(projectRoot, '.mango/dev-workspace.env'),
    ['MANGO_BACKEND_PORT=5555', 'MANGO_FRONTEND_PORT=5176', 'MANGO_DB_NAME=mango_full_acceptance', ''].join('\n'),
  );
  const result = spawnSync(
    'env',
    [
      `MANGO_WORKSPACE_REGISTRY=${join(projectRoot, '.runtime/backfill-workspaces.json')}`,
      `PATH=${fakeBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`,
      process.execPath,
      cli,
      'workspace',
      'init',
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  if (result.status !== 0) {
    throw new Error(
      `mango workspace init should backfill SM4 key for existing env:\n${result.stdout}\n${result.stderr}`,
    );
  }
  const envFile = readFileSync(join(projectRoot, '.mango/dev-workspace.env'), 'utf8');
  if (!/^MANGO_CRYPTO_SM4_SECRET_KEY=[0-9a-f]{32}$/m.test(envFile)) {
    throw new Error(`generated dev-workspace env must backfill a random 16-byte SM4 key:\n${envFile}`);
  }
  if (
    envFile.includes('MANGO_BACKEND_PORT=5555') ||
    envFile.includes('MANGO_FRONTEND_PORT=5176') ||
    envFile.includes('MANGO_DB_NAME=mango_full_acceptance')
  ) {
    throw new Error(`generated dev-workspace env must synchronize stale workspace ownership values:\n${envFile}`);
  }
  if (
    !/^MANGO_BACKEND_PORT=180[0-9]{2}$/m.test(envFile) ||
    !/^MANGO_FRONTEND_PORT=30[0-9]{3}$/m.test(envFile) ||
    !/^MANGO_MAVEN_REVISION_QUALIFIER=mango-[0-9]{3}$/m.test(envFile) ||
    !/^MANGO_DB_NAME=mango_dev_mango_full_acceptance_[0-9]{3}$/m.test(envFile)
  ) {
    throw new Error(`generated dev-workspace env must contain current workspace ownership values:\n${envFile}`);
  }
}

function assertDevWorkspaceRejectsNonCiFriendlyMavenPom(projectRoot) {
  const pomPath = join(projectRoot, 'backend/pom.xml');
  const originalPom = readFileSync(pomPath, 'utf8');
  const incompatiblePom = originalPom
    .replace('<version>${revision}</version>', '<version>1.0.0-SNAPSHOT</version>')
    .replace(/\s*<revision>1\.0\.0-SNAPSHOT<\/revision>/u, '');
  writeFileSync(pomPath, incompatiblePom);
  try {
    const result = spawnSync(process.execPath, [cli, 'dev', 'plan', 'backend'], {
      cwd: projectRoot,
      encoding: 'utf8',
    });
    const output = `${result.stdout}\n${result.stderr}`;
    if (result.status === 0 || !output.includes('does not use CI-friendly ${revision}')) {
      throw new Error(`mango dev plan must reject a non-CI-friendly Maven reactor:\n${output}`);
    }
  } finally {
    writeFileSync(pomPath, originalPom);
  }
}

function assertDevWorkspaceAutoCreatesDatabase(projectRoot) {
  const fakeBinDir = join(projectRoot, '.runtime/db-auto-create-bin');
  const callLog = join(projectRoot, '.runtime/db-auto-create-calls.log');
  mkdirSync(fakeBinDir, { recursive: true });
  writeFileSync(
    join(fakeBinDir, 'mysql'),
    ['#!/usr/bin/env sh', `echo "mysql:$*" >> "${callLog}"`, 'exit 0', ''].join('\n'),
  );
  writeFileSync(
    join(fakeBinDir, 'mvn'),
    ['#!/usr/bin/env sh', `echo "mvn:$*" >> "${callLog}"`, 'exit 17', ''].join('\n'),
  );
  chmodExecutable(join(fakeBinDir, 'mysql'));
  chmodExecutable(join(fakeBinDir, 'mvn'));
  rmSync(callLog, { force: true });
  rmSync(join(projectRoot, '.mango'), { recursive: true, force: true });
  const result = spawnSync(
    'env',
    [
      `MANGO_WORKSPACE_REGISTRY=${join(projectRoot, '.runtime/db-auto-create-workspaces.json')}`,
      `PATH=${fakeBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`,
      process.execPath,
      cli,
      'dev',
      'start',
      'mango-full-acceptance-service',
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  const output = `${result.stdout}\n${result.stderr}`;
  if (result.status === 0 || !output.includes('install command failed')) {
    throw new Error(`database auto-create scenario should stop at fake Maven install:\n${output}`);
  }
  const calls = waitForCallLogLines(callLog, 3);
  const createCall = calls.find((line) =>
    line.includes('CREATE DATABASE IF NOT EXISTS `mango_dev_mango_full_acceptance_'),
  );
  const mavenCall = calls.find((line) =>
    /mvn:-Drevision=1\.0\.0-mango-[0-9]{3}-SNAPSHOT -f pom\.xml -DskipTests install/u.test(line),
  );
  if (
    !calls.some((line) => line.includes('SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA')) ||
    !createCall?.includes('mysql:--protocol=TCP') ||
    !createCall.includes('-h 127.0.0.1') ||
    !createCall.includes('-P 3306') ||
    !createCall.includes('-u root') ||
    !mavenCall
  ) {
    throw new Error(`mango dev start must create workspace database before Maven install:\n${calls.join('\n')}`);
  }
}

function assertDevWorkspaceStreamsLargeInstallOutput(projectRoot) {
  const fakeBinDir = join(projectRoot, '.runtime/large-install-bin');
  const callLog = join(projectRoot, '.runtime/large-install-calls.log');
  mkdirSync(fakeBinDir, { recursive: true });
  writeFileSync(
    join(fakeBinDir, 'mysql'),
    ['#!/usr/bin/env sh', `echo "mysql:$*" >> "${callLog}"`, 'exit 0', ''].join('\n'),
  );
  writeFileSync(
    join(fakeBinDir, 'mvn'),
    [
      '#!/usr/bin/env sh',
      `echo "mvn:$*" >> "${callLog}"`,
      'case "$*" in',
      '  *"-DskipTests install"*)',
      "    dd if=/dev/zero bs=1024 count=1280 2>/dev/null | tr '\\000' x",
      '    exit 0',
      '    ;;',
      'esac',
      'exit 17',
      '',
    ].join('\n'),
  );
  chmodExecutable(join(fakeBinDir, 'mysql'));
  chmodExecutable(join(fakeBinDir, 'mvn'));
  rmSync(callLog, { force: true });
  rmSync(join(projectRoot, '.mango'), { recursive: true, force: true });
  const result = spawnSync(
    'env',
    [
      `MANGO_WORKSPACE_REGISTRY=${join(projectRoot, '.runtime/large-install-workspaces.json')}`,
      `PATH=${fakeBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`,
      process.execPath,
      cli,
      'dev',
      'start',
      'mango-full-acceptance-service',
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  const output = `${result.stdout}\n${result.stderr}`;
  if (
    result.status === 0 ||
    output.includes('install command failed') ||
    !output.includes('exited before becoming healthy')
  ) {
    throw new Error(`large install output must stream to the app log before backend startup:\n${output}`);
  }
  const calls = waitForCallLogLines(callLog, 4);
  const mavenCalls = calls.filter((line) => line.startsWith('mvn:'));
  if (
    !mavenCalls.some((line) => /-Drevision=1\.0\.0-mango-[0-9]{3}-SNAPSHOT .*?-DskipTests install/u.test(line)) ||
    !mavenCalls.some((line) => /-Drevision=1\.0\.0-mango-[0-9]{3}-SNAPSHOT .*?spring-boot-maven-plugin/u.test(line))
  ) {
    throw new Error(`large install output scenario must reach backend startup:\n${calls.join('\n')}`);
  }
}

function assertCommandDevWorkspaceAutoCreatesDatabase(projectRoot) {
  const manifestPath = join(projectRoot, 'mango.dev.json');
  const originalManifest = readFileSync(manifestPath, 'utf8');
  const manifest = JSON.parse(originalManifest);
  manifest.groups.commandDb = ['command-db-backend'];
  manifest.apps['command-db-backend'] = {
    type: 'command',
    cwd: '.',
    command: 'run-backend',
    portEnv: 'MANGO_BACKEND_PORT',
    port: 5555,
    health: '/actuator/health',
    args: [
      '--spring.datasource.url=jdbc:mysql://${env.MANGO_DB_HOST}:${env.MANGO_DB_PORT}/${env.MANGO_DB_NAME}',
      '--server.port=${port}',
    ],
  };
  writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
  try {
    const fakeBinDir = join(projectRoot, '.runtime/db-auto-create-command-bin');
    const callLog = join(projectRoot, '.runtime/db-auto-create-command-calls.log');
    mkdirSync(fakeBinDir, { recursive: true });
    writeFileSync(
      join(fakeBinDir, 'mysql'),
      ['#!/usr/bin/env sh', `echo "mysql:$*" >> "${callLog}"`, 'exit 0', ''].join('\n'),
    );
    writeFileSync(
      join(fakeBinDir, 'run-backend'),
      ['#!/usr/bin/env sh', `echo "run-backend:$*" >> "${callLog}"`, 'exit 17', ''].join('\n'),
    );
    chmodExecutable(join(fakeBinDir, 'mysql'));
    chmodExecutable(join(fakeBinDir, 'run-backend'));
    rmSync(callLog, { force: true });
    rmSync(join(projectRoot, '.mango'), { recursive: true, force: true });
    const result = spawnSync(
      'env',
      [
        `MANGO_WORKSPACE_REGISTRY=${join(projectRoot, '.runtime/db-auto-create-command-workspaces.json')}`,
        `PATH=${fakeBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`,
        process.execPath,
        cli,
        'dev',
        'start',
        'command-db-backend',
      ],
      {
        cwd: projectRoot,
        encoding: 'utf8',
      },
    );
    const output = `${result.stdout}\n${result.stderr}`;
    if (result.status === 0 || !output.includes('exited before becoming healthy')) {
      throw new Error(`command backend scenario should stop after fake backend exits:\n${output}`);
    }
    const calls = waitForCallLogLines(callLog, 3);
    const createCall = calls.find((line) =>
      line.includes('CREATE DATABASE IF NOT EXISTS `mango_dev_mango_full_acceptance_'),
    );
    const runCall = calls.find((line) =>
      line.includes('run-backend:--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/mango_dev_mango_full_acceptance_'),
    );
    if (
      !calls.some((line) => line.includes('SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA')) ||
      !createCall?.includes('mysql:--protocol=TCP') ||
      !runCall
    ) {
      throw new Error(`command backend must create workspace database before starting:\n${calls.join('\n')}`);
    }
  } finally {
    writeFileSync(manifestPath, originalManifest);
  }
}

function assertDevWorkspaceReportsMissingMysql(projectRoot) {
  const fakeBinDir = join(projectRoot, '.runtime/db-missing-mysql-bin');
  mkdirSync(fakeBinDir, { recursive: true });
  rmSync(join(projectRoot, '.mango'), { recursive: true, force: true });
  const result = spawnSync(
    'env',
    [
      `MANGO_WORKSPACE_REGISTRY=${join(projectRoot, '.runtime/db-missing-mysql-workspaces.json')}`,
      `PATH=${fakeBinDir}`,
      process.execPath,
      cli,
      'dev',
      'start',
      'mango-full-acceptance-service',
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  const output = `${result.stdout}\n${result.stderr}`;
  if (
    result.status === 0 ||
    !output.includes('failed to auto-create database') ||
    !output.includes('spawnSync mysql ENOENT')
  ) {
    throw new Error(`missing mysql should report the spawn failure reason:\n${output}`);
  }
}

function assertDevWorkspaceRestartUsesStopThenStart(projectRoot) {
  const manifestPath = join(projectRoot, 'mango.dev.json');
  const originalManifest = readFileSync(manifestPath, 'utf8');
  const noPsBinDir = join(projectRoot, '.runtime/no-ps-bin');
  mkdirSync(noPsBinDir, { recursive: true });
  const noPsEnv = { ...process.env, PATH: noPsBinDir };
  const manifest = JSON.parse(originalManifest);
  manifest.groups.restart = ['restart-worker'];
  manifest.apps['restart-worker'] = {
    type: 'command',
    cwd: '.',
    command: process.execPath,
    args: ['-e', 'setInterval(() => {}, 1000)'],
  };
  writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
  try {
    rmSync(join(projectRoot, '.mango/run/pids/restart-worker.json'), { force: true });
    const start = spawnSync(process.execPath, [cli, 'dev', 'start', 'restart'], {
      cwd: projectRoot,
      encoding: 'utf8',
      env: noPsEnv,
    });
    if (start.status !== 0 || !start.stdout.includes('restart-worker: started pid=')) {
      throw new Error(`restart fixture start should create a running process:\n${start.stdout}\n${start.stderr}`);
    }
    const firstPid = JSON.parse(readFileSync(join(projectRoot, '.mango/run/pids/restart-worker.json'), 'utf8')).pid;
    const restart = spawnSync(process.execPath, [cli, 'dev', 'restart', 'restart'], {
      cwd: projectRoot,
      encoding: 'utf8',
      env: noPsEnv,
    });
    if (
      restart.status !== 0 ||
      !restart.stdout.includes(`restart-worker: stopped pid=${firstPid}`) ||
      !restart.stdout.includes('restart-worker: started pid=')
    ) {
      throw new Error(
        `mango dev restart should stop then start selected targets:\n${restart.stdout}\n${restart.stderr}`,
      );
    }
    const secondPid = JSON.parse(readFileSync(join(projectRoot, '.mango/run/pids/restart-worker.json'), 'utf8')).pid;
    if (secondPid === firstPid) {
      throw new Error(`mango dev restart should replace the running pid: ${firstPid}`);
    }
    const stop = spawnSync(process.execPath, [cli, 'dev', 'stop', 'restart'], {
      cwd: projectRoot,
      encoding: 'utf8',
      env: noPsEnv,
    });
    if (stop.status !== 0 || !stop.stdout.includes(`restart-worker: stopped pid=${secondPid}`)) {
      throw new Error(`restart fixture cleanup should stop the restarted process:\n${stop.stdout}\n${stop.stderr}`);
    }
  } finally {
    if (existsSync(join(projectRoot, '.mango/run/pids/restart-worker.json'))) {
      spawnSync(process.execPath, [cli, 'dev', 'stop', 'restart'], {
        cwd: projectRoot,
        encoding: 'utf8',
        env: noPsEnv,
      });
    }
    writeFileSync(manifestPath, originalManifest);
    rmSync(join(projectRoot, '.mango/run/pids/restart-worker.json'), { force: true });
  }
}

function assertGeneratedBaselineLoadsDeliveryContractForPr(projectRoot) {
  const result = spawnSync(
    process.execPath,
    [
      'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
      '--role',
      'dev',
      '--phase',
      'develop',
      '--task',
      '评审 PR #151 并提交 PR 修复',
      '--paths',
      'frontend',
      '--json',
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  if (result.status !== 0) {
    throw new Error(`generated PMO PR preflight failed:\n${result.stdout}\n${result.stderr}`);
  }
  const output = JSON.parse(result.stdout);
  const mustRead = output.mustRead || [];
  if (!mustRead.some((entry) => entry.path === 'rules/01-delivery-contract.md')) {
    throw new Error(`generated PMO PR preflight must load delivery contract:\n${result.stdout}`);
  }
}

function chmodExecutable(path) {
  chmodSync(path, 0o755);
  if ((statSync(path).mode & 0o111) === 0) {
    throw new Error(`failed to make executable: ${path}`);
  }
}

function assertCommandFails(args, cwd, label, expectedText) {
  const result = spawnSync(process.execPath, args, {
    cwd,
    encoding: 'utf8',
  });
  if (result.status === 0) {
    throw new Error(`${label} should fail`);
  }
  const output = `${result.stdout}\n${result.stderr}`;
  if (!output.includes(expectedText)) {
    throw new Error(`${label} did not report expected text "${expectedText}":\n${output}`);
  }
  return result;
}

function assertDevWorkspaceRunnerScenarios(tempRoot) {
  const businessRoot = join(tempRoot, 'business-custom-app-path');
  mkdirSync(join(businessRoot, 'services/guarantee-service'), { recursive: true });
  mkdirSync(join(businessRoot, 'web/admin-console'), { recursive: true });
  mkdirSync(join(businessRoot, 'web/cashier-console'), { recursive: true });
  mkdirSync(join(businessRoot, '.mango'), { recursive: true });
  writeFileSync(
    join(businessRoot, 'mango.dev.json'),
    `${JSON.stringify(
      {
        version: 1,
        groups: {
          default: ['guarantee-service', 'admin-console'],
          cashier: ['guarantee-service', 'cashier-console'],
        },
        apps: {
          'guarantee-service': {
            type: 'command',
            cwd: 'backend/default-service',
            command: 'node',
            args: ['-e', "setInterval(() => console.log('guarantee-service alive'), 1000)"],
          },
          'admin-console': {
            type: 'command',
            cwd: 'frontend/default-admin',
            dependsOn: ['guarantee-service'],
            command: 'node',
            args: ['-e', "setInterval(() => console.log('admin-console alive'), 1000)"],
          },
          'cashier-console': {
            type: 'command',
            cwd: 'frontend/default-cashier',
            dependsOn: ['guarantee-service'],
            command: 'node',
            args: ['-e', "setInterval(() => console.log('cashier-console alive'), 1000)"],
          },
        },
      },
      null,
      2,
    )}\n`,
  );
  writeFileSync(
    join(businessRoot, '.mango/dev-workspace.local.json'),
    `${JSON.stringify(
      {
        apps: {
          'guarantee-service': { cwd: 'services/guarantee-service' },
          'admin-console': { cwd: 'web/admin-console' },
          'cashier-console': { cwd: 'web/cashier-console' },
        },
      },
      null,
      2,
    )}\n`,
  );

  const nestedCwd = join(businessRoot, 'web/admin-console');
  assertCommandOk([cli, 'validate'], nestedCwd, 'nested business validate');
  const plan = assertCommandOk([cli, 'plan', 'cashier'], nestedCwd, 'custom cashier plan');
  const normalizedPlan = plan.stdout.replace(/\\/gu, '/');
  if (
    !normalizedPlan.includes('services/guarantee-service') ||
    !normalizedPlan.includes('web/cashier-console') ||
    normalizedPlan.includes('web/admin-console')
  ) {
    throw new Error(`custom app/path plan did not resolve expected local overrides:\n${plan.stdout}`);
  }
  assertCommandOk([cli, 'start', 'cashier'], nestedCwd, 'custom cashier start');
  const statusRunning = assertCommandOk([cli, 'status'], nestedCwd, 'custom cashier status running');
  if (
    !statusRunning.stdout.includes('running  guarantee-service') ||
    !statusRunning.stdout.includes('running  cashier-console') ||
    !statusRunning.stdout.includes('stopped  admin-console')
  ) {
    throw new Error(`custom cashier status mismatch:\n${statusRunning.stdout}`);
  }
  const logs = assertCommandOk([cli, 'logs', 'cashier-console'], nestedCwd, 'custom cashier logs');
  if (!logs.stdout.includes('cashier-console')) {
    throw new Error(`custom cashier logs missing app output:\n${logs.stdout}`);
  }
  assertCommandOk([cli, 'stop', 'cashier'], nestedCwd, 'custom cashier stop');
  const statusStopped = assertCommandOk([cli, 'status'], nestedCwd, 'custom cashier status stopped');
  if (statusStopped.stdout.includes('running')) {
    throw new Error(`custom cashier apps should be stopped:\n${statusStopped.stdout}`);
  }

  const renamedRoot = join(tempRoot, 'business-renamed-committed-paths');
  mkdirSync(join(renamedRoot, 'apps/api-server'), { recursive: true });
  mkdirSync(join(renamedRoot, 'apps/backoffice-web'), { recursive: true });
  mkdirSync(join(renamedRoot, 'apps/portal-web'), { recursive: true });
  writeFileSync(
    join(renamedRoot, 'mango.dev.json'),
    `${JSON.stringify(
      {
        version: 1,
        groups: {
          default: ['api-server', 'backoffice-web'],
          portal: ['api-server', 'portal-web'],
        },
        apps: {
          'api-server': {
            type: 'command',
            cwd: 'apps/api-server',
            command: 'node',
            args: ['--version'],
          },
          'backoffice-web': {
            type: 'command',
            cwd: 'apps/backoffice-web',
            dependsOn: ['api-server'],
            command: 'node',
            args: ['--version'],
          },
          'portal-web': {
            type: 'command',
            cwd: 'apps/portal-web',
            dependsOn: ['api-server'],
            command: 'node',
            args: ['--version'],
          },
        },
      },
      null,
      2,
    )}\n`,
  );
  const renamedPlan = assertCommandOk(
    [cli, 'plan', 'portal'],
    join(renamedRoot, 'apps/portal-web'),
    'renamed committed path plan',
  );
  if (
    !renamedPlan.stdout.includes('apps/api-server') ||
    !renamedPlan.stdout.includes('apps/portal-web') ||
    renamedPlan.stdout.includes('apps/backoffice-web')
  ) {
    throw new Error(`renamed committed paths did not resolve expected group:\n${renamedPlan.stdout}`);
  }

  const negativeRoot = join(tempRoot, 'dev-workspace-negative');
  mkdirSync(join(negativeRoot, 'bad-path'), { recursive: true });
  mkdirSync(join(negativeRoot, 'cycle'), { recursive: true });
  mkdirSync(join(negativeRoot, 'spring-prefix/app'), { recursive: true });
  writeFileSync(
    join(negativeRoot, 'bad-path/mango.dev.json'),
    '{"version":1,"groups":{"default":["bad"]},"apps":{"bad":{"type":"command","cwd":"missing-dir","command":"node","args":["--version"]}}}\n',
  );
  writeFileSync(
    join(negativeRoot, 'cycle/mango.dev.json'),
    '{"version":1,"groups":{"default":["a"]},"apps":{"a":{"type":"command","cwd":".","dependsOn":["b"],"command":"node","args":["--version"]},"b":{"type":"command","cwd":".","dependsOn":["a"],"command":"node","args":["--version"]}}}\n',
  );
  writeFileSync(
    join(negativeRoot, 'spring-prefix/mango.dev.json'),
    '{"version":1,"groups":{"default":["app"]},"apps":{"app":{"type":"spring-boot-maven","cwd":"app","goal":"spring-boot:run","port":5555}}}\n',
  );
  writeFileSync(join(negativeRoot, 'spring-prefix/app/pom.xml'), '<project></project>\n');
  mkdirSync(join(negativeRoot, 'missing-pom/app'), { recursive: true });
  writeFileSync(
    join(negativeRoot, 'missing-pom/mango.dev.json'),
    '{"version":1,"groups":{"default":["app"]},"apps":{"app":{"type":"spring-boot-maven","cwd":"app","pom":"missing/pom.xml","goal":"org.springframework.boot:spring-boot-maven-plugin:3.5.14:run"}}}\n',
  );
  assertCommandFails([cli, 'validate'], join(negativeRoot, 'bad-path'), 'bad path validation', 'cwd not found');
  assertCommandFails([cli, 'validate'], join(negativeRoot, 'cycle'), 'cycle validation', 'cyclic app dependency');
  assertCommandFails(
    [cli, 'validate'],
    join(negativeRoot, 'spring-prefix'),
    'spring prefix validation',
    'explicit Spring Boot Maven plugin coordinate',
  );
  assertCommandFails([cli, 'validate'], join(negativeRoot, 'missing-pom'), 'missing pom validation', 'pom not found');

  const legacyEnvRoot = join(tempRoot, 'dev-workspace-legacy-env');
  mkdirSync(join(legacyEnvRoot, 'app'), { recursive: true });
  mkdirSync(join(legacyEnvRoot, '.mango'), { recursive: true });
  writeFileSync(
    join(legacyEnvRoot, 'mango.dev.json'),
    `${JSON.stringify(
      {
        version: 1,
        groups: { default: ['legacy-env-app'] },
        apps: {
          'legacy-env-app': {
            type: 'command',
            cwd: 'app',
            command: 'node',
            args: ['--version'],
          },
        },
      },
      null,
      2,
    )}\n`,
  );
  writeFileSync(
    join(legacyEnvRoot, '.mango/dev-workspace.env'),
    ['MANGO_BACKEND_PORT=5555', 'MANGO_FRONTEND_PORT=5176', 'MANGO_DB_NAME=legacy_env', ''].join('\n'),
  );
  assertCommandOk([cli, 'start'], legacyEnvRoot, 'legacy env start backfills SM4 key');
  const legacyEnvFile = readFileSync(join(legacyEnvRoot, '.mango/dev-workspace.env'), 'utf8');
  if (!/^MANGO_CRYPTO_SM4_SECRET_KEY=[0-9a-f]{32}$/m.test(legacyEnvFile)) {
    throw new Error(`CLI start must backfill SM4 key for legacy workspace env:\n${legacyEnvFile}`);
  }
  if (
    legacyEnvFile.includes('MANGO_BACKEND_PORT=5555') ||
    legacyEnvFile.includes('MANGO_FRONTEND_PORT=5176') ||
    legacyEnvFile.includes('MANGO_DB_NAME=legacy_env')
  ) {
    throw new Error(`CLI start must synchronize stale legacy workspace env values:\n${legacyEnvFile}`);
  }
  if (
    !/^MANGO_BACKEND_PORT=180[0-9]{2}$/m.test(legacyEnvFile) ||
    !/^MANGO_FRONTEND_PORT=30[0-9]{3}$/m.test(legacyEnvFile) ||
    !/^MANGO_DB_NAME=mango_dev_dev_workspace_legacy_env_[0-9]{3}$/m.test(legacyEnvFile)
  ) {
    throw new Error(`CLI start must write current workspace env values:\n${legacyEnvFile}`);
  }
  assertCommandOk([cli, 'stop'], legacyEnvRoot, 'legacy env stop');

  const occupiedRoot = join(tempRoot, 'dev-workspace-occupied-port');
  mkdirSync(join(occupiedRoot, 'app'), { recursive: true });
  const occupiedPort = 45671;
  writeFileSync(
    join(occupiedRoot, 'mango.dev.json'),
    `${JSON.stringify(
      {
        version: 1,
        groups: { default: ['legacy-owned'] },
        apps: {
          'legacy-owned': {
            type: 'command',
            cwd: 'app',
            port: occupiedPort,
            command: 'node',
            args: ['--version'],
          },
        },
      },
      null,
      2,
    )}\n`,
  );
  const legacyProcess = spawn(
    process.execPath,
    ['-e', `require('node:net').createServer().listen(${occupiedPort}, '127.0.0.1'); setInterval(() => {}, 1000);`],
    {
      stdio: 'ignore',
      detached: true,
    },
  );
  legacyProcess.unref();
  try {
    waitForPort(occupiedPort);
    const occupiedStatus = assertCommandOk([cli, 'status'], occupiedRoot, 'occupied status');
    if (
      !occupiedStatus.stdout.includes('occupied legacy-owned') ||
      !occupiedStatus.stdout.includes(`pid=${legacyProcess.pid}`) ||
      !occupiedStatus.stdout.includes('command=node')
    ) {
      throw new Error(`status should report unmanaged port occupant:\n${occupiedStatus.stdout}`);
    }
    const occupiedDoctor = assertCommandOk([cli, 'doctor'], occupiedRoot, 'occupied doctor');
    if (
      !occupiedDoctor.stdout.includes(`warn    legacy-owned port ${occupiedPort} is already in use`) ||
      !occupiedDoctor.stdout.includes(`pid=${legacyProcess.pid}`) ||
      !occupiedDoctor.stdout.includes('command=node')
    ) {
      throw new Error(`doctor should report unmanaged port occupant:\n${occupiedDoctor.stdout}`);
    }
    const occupiedStart = assertCommandFails([cli, 'start'], occupiedRoot, 'occupied start', 'already in use');
    const occupiedStartOutput = `${occupiedStart.stdout}\n${occupiedStart.stderr}`;
    if (
      !occupiedStartOutput.includes(`legacy-owned port ${occupiedPort} is already in use`) ||
      !occupiedStartOutput.includes(`pid=${legacyProcess.pid}`) ||
      !occupiedStartOutput.includes('command=node')
    ) {
      throw new Error(`start should fail with unmanaged port occupant detail:\n${occupiedStartOutput}`);
    }
  } finally {
    try {
      process.kill(legacyProcess.pid, 'SIGTERM');
    } catch {
      // Already exited.
    }
  }
}

function assertYamlFlywayModuleEnabled(applicationYml, moduleName) {
  const escapedModuleName = escapeRegExp(moduleName);
  const pattern = new RegExp(`\\n\\s{8}${escapedModuleName}:\\n\\s{10}enabled: true\\n`);
  if (!pattern.test(applicationYml)) {
    throw new Error(`full backend application.yml must enable ${moduleName} Flyway module`);
  }
}

function assertYamlFlywayModuleOutOfOrder(applicationYml, moduleName) {
  const block = findYamlFlywayModuleBlock(applicationYml, moduleName);
  if (!block.includes('out-of-order: true')) {
    throw new Error(`full backend application.yml must mark ${moduleName} Flyway module as out-of-order compatible`);
  }
}

function assertYamlFlywayModuleStrictOrdering(applicationYml, moduleName) {
  const block = findYamlFlywayModuleBlock(applicationYml, moduleName);
  if (block.includes('out-of-order: true')) {
    throw new Error(`full backend application.yml must keep ${moduleName} Flyway module strict by default`);
  }
}

function findYamlFlywayModuleBlock(applicationYml, moduleName) {
  const escapedModuleName = escapeRegExp(moduleName);
  const pattern = new RegExp(`\\n\\s{8}${escapedModuleName}:\\n(?<body>(?:\\s{10}[^\\n]+\\n)+)`);
  const match = applicationYml.match(pattern);
  if (!match?.groups?.body) {
    throw new Error(`full backend application.yml is missing ${moduleName} Flyway module block`);
  }
  return match.groups.body;
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function assertNotIncludes(values, unexpected, field) {
  if (values.includes(unexpected)) {
    throw new Error(`${field} should not include ${unexpected}`);
  }
}

function readReleasedPackageVersion(packageName) {
  const version = releaseVersions.npm?.[packageName];
  if (!version) {
    throw new Error(`release-versions.json missing npm version for ${packageName}`);
  }
  return version;
}

function assertBusinessAcceptanceBaseline(projectRoot) {
  const baselineRoot = join(projectRoot, 'business-pmo/mango-baseline');
  const issueRunbook = readFileSync(join(baselineRoot, 'rules/07-mango-issue-runbook.md'), 'utf8');
  for (const expected of ['https://github.com/HardyDou/mango/issues', 'gh issue create', 'P0|P1|P2', '业务任务记录']) {
    if (!issueRunbook.includes(expected)) {
      throw new Error(`generated Mango issue runbook missing requirement: ${expected}`);
    }
  }
  const frontendTestRule = readFileSync(join(baselineRoot, 'rules/frontend/04-test.md'), 'utf8');
  for (const expected of [
    '不能只验证接口 200',
    '业务结果断言',
    'UI 细节断言',
    'console error',
    '截图或 trace/video 路径',
    'acceptance-evidence-check.mjs',
  ]) {
    if (!frontendTestRule.includes(expected)) {
      throw new Error(`generated frontend test rule missing acceptance requirement: ${expected}`);
    }
  }

  const qaAgent = readFileSync(join(baselineRoot, 'agents/04-qa-agent.md'), 'utf8');
  for (const expected of ['frontend/04-test.md', 'frontend/02-element-plus-ui.md', '不能只写接口 200']) {
    if (!qaAgent.includes(expected)) {
      throw new Error(`generated QA agent missing acceptance requirement: ${expected}`);
    }
  }

  const deliveryTemplate = readFileSync(join(baselineRoot, 'templates/delivery-contract.md'), 'utf8');
  const evidenceTemplate = readFileSync(join(baselineRoot, 'templates/acceptance-evidence.md'), 'utf8');
  for (const expected of ['功能点', '关键断言', 'UI/交互检查', 'console/network 结果', '截图/trace/日志']) {
    if (!deliveryTemplate.includes(expected) || !evidenceTemplate.includes(expected)) {
      throw new Error(`generated acceptance templates missing evidence column: ${expected}`);
    }
  }

  const validEvidencePath = join(projectRoot, 'acceptance-valid.md');
  writeFileSync(
    validEvidencePath,
    [
      '# acceptance',
      '',
      '| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |',
      '|---|---|---|---|---|---|---|---|---|',
      '| TASK-001 | /demo | 新增业务记录 | name=acceptance | 列表新增一行且详情字段回显正确 | 主按钮位置正确，表单分组和必填校验可见 | 无 console error，无资源 404，无接口 4xx/5xx | artifacts/demo.png | DONE |',
      '',
    ].join('\n'),
  );
  const evidenceCheck = spawnSync(
    process.execPath,
    ['business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs', '--evidence', validEvidencePath],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  if (evidenceCheck.status !== 0) {
    throw new Error(`generated acceptance evidence check failed:\n${evidenceCheck.stdout}\n${evidenceCheck.stderr}`);
  }

  const weakEvidencePath = join(projectRoot, 'acceptance-weak.md');
  writeFileSync(
    weakEvidencePath,
    [
      '# weak acceptance',
      '',
      '| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |',
      '|---|---|---|---|---|---|---|---|---|',
      '| TASK-001 | /demo | 新增 | - | 接口 200 | 页面无异常 | 无报错 | 截图正常 | DONE |',
      '',
    ].join('\n'),
  );
  const weakEvidenceCheck = spawnSync(
    process.execPath,
    ['business-pmo/mango-baseline/tools/acceptance-evidence-check.mjs', '--evidence', weakEvidencePath],
    {
      cwd: projectRoot,
      encoding: 'utf8',
    },
  );
  if (weakEvidenceCheck.status === 0 || !weakEvidenceCheck.stdout.includes('weak acceptance wording')) {
    throw new Error(
      `generated acceptance evidence check should reject weak evidence:\n${weakEvidenceCheck.stdout}\n${weakEvidenceCheck.stderr}`,
    );
  }
}

function waitForPort(port) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 5000) {
    const result = spawnSync('sh', ['-c', `lsof -nP -iTCP:${Number(port)} -sTCP:LISTEN >/dev/null 2>&1`], {
      stdio: 'ignore',
    });
    if (result.status === 0) {
      return;
    }
    spawnSync('sleep', ['0.1']);
  }
  throw new Error(`port did not become occupied: ${port}`);
}

function waitForCallLogLines(callLog, expectedCount) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 5000) {
    if (existsSync(callLog)) {
      const lines = readFileSync(callLog, 'utf8').trim().split(/\r?\n/).filter(Boolean);
      if (lines.length >= expectedCount) {
        return lines;
      }
    }
    spawnSync('sleep', ['0.1']);
  }
  if (!existsSync(callLog)) {
    return [];
  }
  return readFileSync(callLog, 'utf8').trim().split(/\r?\n/).filter(Boolean);
}

function assertPmoCommands(projectRoot) {
  const lock = JSON.parse(readFileSync(join(projectRoot, 'business-pmo/pmo-lock.json'), 'utf8'));
  const installedManifest = JSON.parse(
    readFileSync(join(projectRoot, 'business-pmo/mango-baseline/baseline.json'), 'utf8'),
  );
  const skillState = JSON.parse(readFileSync(join(projectRoot, '.agents/skills/.mango-pmo.json'), 'utf8'));
  assertEqual(lock.packageVersion, installedManifest.packageVersion, 'PMO lock package version');
  assertEqual(lock.bundleSha256, installedManifest.bundleSha256, 'PMO lock bundle hash');
  assertIncludes(skillState.roots, 'mango-pmo-lifecycle', 'project PMO skill roots');
  const architectureBudgetPath = join(projectRoot, 'business-pmo/architecture-debt-budget.json');
  const projectOwnedBudget = JSON.parse(readFileSync(architectureBudgetPath, 'utf8'));
  projectOwnedBudget.generatedAt = '2026-07-22T12:34:56.000Z';
  writeFileSync(architectureBudgetPath, `${JSON.stringify(projectOwnedBudget, null, 2)}\n`);
  const projectOwnedBudgetText = readFileSync(architectureBudgetPath, 'utf8');
  const status = assertCommandOk(
    [cli, 'pmo', 'status', '--project-dir', projectRoot],
    projectRoot,
    'generated mango pmo status',
  );
  if (!status.stdout.includes('Baseline: @mango/pmo@') || !status.stdout.includes('PMO baseline is current.')) {
    throw new Error(`pmo status should report current @mango/pmo baseline:\n${status.stdout}`);
  }
  assertCommandOk([cli, 'pmo', 'check', '--project-dir', projectRoot], projectRoot, 'generated mango pmo check');
  assertCommandOk(
    [cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'],
    projectRoot,
    'generated locked mango pmo check',
  );
  const projectTemplatePath = join(projectRoot, '.github/pull_request_template.md');
  const canonicalProjectTemplate = readFileSync(
    join(projectRoot, 'business-pmo/mango-baseline/templates/business-pull-request-template.md'),
    'utf8',
  );
  rmSync(projectTemplatePath);
  const missingTemplateCheck = spawnSync(
    process.execPath,
    [cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'],
    { cwd: projectRoot, encoding: 'utf8' },
  );
  if (
    missingTemplateCheck.status === 0 ||
    !missingTemplateCheck.stdout.includes('.github/pull_request_template.md') ||
    !missingTemplateCheck.stdout.includes('mango pmo sync --project-dir .')
  ) {
    throw new Error(
      `pmo check --locked should report a missing project PR template and repair command:\n${missingTemplateCheck.stdout}\n${missingTemplateCheck.stderr}`,
    );
  }
  assertCommandOk(
    [cli, 'pmo', 'sync', '--project-dir', projectRoot],
    projectRoot,
    'generated mango pmo missing PR template repair',
  );
  assertEqual(readFileSync(projectTemplatePath, 'utf8'), canonicalProjectTemplate, 'restored project PR template');

  const legacyProjectTemplate = `## Summary

- business-owned summary

## Risk / Verification

- Requirement impact: L0-L3 - facts
- Solution risk: L0-L3 - facts
- Final risk: L0-L3
- Selected verification: STATIC, UNIT, API, UI
- Why sufficient:
- Skipped verification: None

## Business Deployment

- preserve this rollout section
`;
  writeFileSync(projectTemplatePath, legacyProjectTemplate);
  const legacyTemplateCheck = spawnSync(
    process.execPath,
    [cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'],
    { cwd: projectRoot, encoding: 'utf8' },
  );
  if (
    legacyTemplateCheck.status === 0 ||
    !legacyTemplateCheck.stdout.includes('differs from the locked PMO contract')
  ) {
    throw new Error(
      `pmo check --locked should reject a legacy project PR template:\n${legacyTemplateCheck.stdout}\n${legacyTemplateCheck.stderr}`,
    );
  }
  assertCommandOk(
    [cli, 'pmo', 'sync', '--project-dir', projectRoot],
    projectRoot,
    'generated mango pmo legacy PR template repair',
  );
  const repairedProjectTemplate = readFileSync(projectTemplatePath, 'utf8');
  if (
    !repairedProjectTemplate.includes('business-owned summary') ||
    !repairedProjectTemplate.includes('preserve this rollout section') ||
    !repairedProjectTemplate.includes('Assurance selections:') ||
    repairedProjectTemplate.includes('Selected verification:')
  ) {
    throw new Error(`pmo sync must replace only the managed Risk / Verification section:\n${repairedProjectTemplate}`);
  }
  const duplicateProjectTemplate = `${repairedProjectTemplate}\n## Risk / Verification\n\n- duplicate\n`;
  writeFileSync(projectTemplatePath, duplicateProjectTemplate);
  const duplicateTemplateSync = spawnSync(process.execPath, [cli, 'pmo', 'sync', '--project-dir', projectRoot], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (
    duplicateTemplateSync.status === 0 ||
    !`${duplicateTemplateSync.stdout}\n${duplicateTemplateSync.stderr}`.includes('exactly one')
  ) {
    throw new Error(
      `pmo sync should fail closed for duplicate managed PR template sections:\n${duplicateTemplateSync.stdout}\n${duplicateTemplateSync.stderr}`,
    );
  }
  assertEqual(
    readFileSync(projectTemplatePath, 'utf8'),
    duplicateProjectTemplate,
    'duplicate project PR template after rejected sync',
  );
  writeFileSync(projectTemplatePath, repairedProjectTemplate);
  const dryRun = assertCommandOk(
    [cli, 'pmo', 'upgrade', '--project-dir', projectRoot, '--dry-run'],
    projectRoot,
    'generated mango pmo upgrade dry-run',
  );
  if (!dryRun.stdout.includes('PMO baseline dry-run plan')) {
    throw new Error(`pmo upgrade --dry-run should print plan:\n${dryRun.stdout}`);
  }
  const tamperedPath = join(projectRoot, 'business-pmo/mango-baseline/rules/00-dev-flow.md');
  const original = readFileSync(tamperedPath, 'utf8');
  writeFileSync(tamperedPath, `${original}\n# tampered by CLI test\n`);
  const checkResult = spawnSync(process.execPath, [cli, 'pmo', 'check', '--project-dir', projectRoot], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (checkResult.status === 0 || !checkResult.stdout.includes('changed')) {
    throw new Error(
      `pmo check should fail when baseline file is tampered:\n${checkResult.stdout}\n${checkResult.stderr}`,
    );
  }
  const syncResult = assertCommandOk(
    [cli, 'pmo', 'sync', '--project-dir', projectRoot],
    projectRoot,
    'generated mango pmo sync repair',
  );
  if (!syncResult.stdout.includes('PMO baseline sync complete')) {
    throw new Error(`pmo sync should repair tampered baseline:\n${syncResult.stdout}`);
  }
  assertEqual(readFileSync(tamperedPath, 'utf8'), original, 'tampered baseline after pmo sync');

  const stalePath = join(projectRoot, 'business-pmo/mango-baseline/rules/stale-rule.md');
  writeFileSync(stalePath, '# stale PMO rule\n');
  const staleCheck = spawnSync(process.execPath, [cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (staleCheck.status === 0 || !staleCheck.stdout.includes('stale baseline files')) {
    throw new Error(`pmo check --locked should reject stale bundle files:\n${staleCheck.stdout}\n${staleCheck.stderr}`);
  }
  assertCommandOk([cli, 'pmo', 'sync', '--project-dir', projectRoot], projectRoot, 'generated mango pmo stale cleanup');
  if (existsSync(stalePath)) {
    throw new Error('pmo sync must remove stale files that are absent from the locked manifest');
  }

  const skillPath = join(projectRoot, '.agents/skills/mango-pmo-lifecycle/SKILL.md');
  const originalSkill = readFileSync(skillPath, 'utf8');
  writeFileSync(skillPath, `${originalSkill}\ncorrupt\n`);
  const skillCheck = spawnSync(process.execPath, [cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'], {
    cwd: projectRoot,
    encoding: 'utf8',
  });
  if (skillCheck.status === 0 || !skillCheck.stdout.includes('project PMO skill files differ')) {
    throw new Error(
      `pmo check --locked should reject changed project skills:\n${skillCheck.stdout}\n${skillCheck.stderr}`,
    );
  }
  const skillRepair = assertCommandOk(
    [cli, 'pmo', 'sync', '--project-dir', projectRoot],
    projectRoot,
    'generated mango pmo skill repair',
  );
  if (!skillRepair.stdout.includes('no user-level Codex plugin installation was performed')) {
    throw new Error(
      `pmo sync must distinguish project skill sync from global plugin installation:\n${skillRepair.stdout}`,
    );
  }
  assertEqual(readFileSync(skillPath, 'utf8'), originalSkill, 'tampered project skill after pmo sync');
  const projectTemplateBeforeRollback = readFileSync(projectTemplatePath, 'utf8');
  const githubWorkflowPath = join(projectRoot, '.github/workflows/pmo-doc-check.yml');
  const giteaWorkflowPath = join(projectRoot, '.gitea/workflows/pmo-doc-check.yml');
  const githubWorkflowBeforeRollback = readFileSync(githubWorkflowPath, 'utf8');
  const giteaWorkflowBeforeRollback = readFileSync(giteaWorkflowPath, 'utf8');
  writeFileSync(
    projectTemplatePath,
    projectTemplateBeforeRollback.replace('business-owned summary', 'post-backup business summary'),
  );
  writeFileSync(githubWorkflowPath, `${githubWorkflowBeforeRollback}\n# post-backup drift\n`);
  writeFileSync(giteaWorkflowPath, `${giteaWorkflowBeforeRollback}\n# post-backup drift\n`);

  const unavailableUpgrade = spawnSync(
    process.execPath,
    [cli, 'pmo', 'upgrade', '--project-dir', projectRoot, '--to', '0.0.0-test'],
    { cwd: projectRoot, encoding: 'utf8' },
  );
  if (unavailableUpgrade.status === 0 || !unavailableUpgrade.stderr.includes('is not available to this CLI')) {
    throw new Error(
      `pmo upgrade --to should reject unavailable exact versions:\n${unavailableUpgrade.stdout}\n${unavailableUpgrade.stderr}`,
    );
  }
  const rollback = assertCommandOk(
    [cli, 'pmo', 'rollback', '--project-dir', projectRoot],
    projectRoot,
    'generated mango pmo rollback',
  );
  if (!rollback.stdout.includes('PMO rollback complete')) {
    throw new Error(`pmo rollback should restore a verified backup:\n${rollback.stdout}`);
  }
  assertEqual(
    readFileSync(projectTemplatePath, 'utf8'),
    projectTemplateBeforeRollback,
    'project PR template restored from PMO rollback backup',
  );
  assertEqual(readFileSync(githubWorkflowPath, 'utf8'), githubWorkflowBeforeRollback, 'GitHub workflow restored');
  assertEqual(readFileSync(giteaWorkflowPath, 'utf8'), giteaWorkflowBeforeRollback, 'Gitea workflow restored');
  assertEqual(
    readFileSync(architectureBudgetPath, 'utf8'),
    projectOwnedBudgetText,
    'project budget after PMO rollback',
  );
  assertCommandOk(
    [cli, 'pmo', 'check', '--project-dir', projectRoot, '--locked'],
    projectRoot,
    'rolled back mango pmo check',
  );
}

function assertPmoSyncCommand(tempRoot) {
  const dryRunRoot = join(tempRoot, 'existing-business-dry-run');
  mkdirSync(dryRunRoot, { recursive: true });
  const dryRunResult = spawnSync(process.execPath, [cli, 'pmo', 'sync', '--project-dir', dryRunRoot, '--dry-run'], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (dryRunResult.status !== 0) {
    throw new Error(`pmo sync dry-run failed:\n${dryRunResult.stdout}\n${dryRunResult.stderr}`);
  }
  if (
    !dryRunResult.stdout.includes('PMO baseline dry-run plan') ||
    !dryRunResult.stdout.includes('add') ||
    existsSync(join(dryRunRoot, 'business-pmo'))
  ) {
    throw new Error(`pmo sync dry-run should print a plan without writing files:\n${dryRunResult.stdout}`);
  }

  const customWorkflowRoot = join(tempRoot, 'existing-business-custom-workflow');
  const customWorkflowPath = join(customWorkflowRoot, '.github/workflows/pmo-doc-check.yml');
  mkdirSync(join(customWorkflowRoot, '.github/workflows'), { recursive: true });
  const customWorkflow = 'name: Business-owned checks\n\non: [push]\n';
  writeFileSync(customWorkflowPath, customWorkflow);
  const rejectedCustomWorkflow = spawnSync(
    process.execPath,
    [cli, 'pmo', 'upgrade', '--project-dir', customWorkflowRoot],
    { cwd: tempRoot, encoding: 'utf8' },
  );
  if (
    rejectedCustomWorkflow.status === 0 ||
    !`${rejectedCustomWorkflow.stdout}\n${rejectedCustomWorkflow.stderr}`.includes('will not be overwritten') ||
    existsSync(join(customWorkflowRoot, 'business-pmo/mango-baseline'))
  ) {
    throw new Error(
      `pmo upgrade must fail before writes for an unrecognized custom workflow:\n${rejectedCustomWorkflow.stdout}\n${rejectedCustomWorkflow.stderr}`,
    );
  }
  assertEqual(readFileSync(customWorkflowPath, 'utf8'), customWorkflow, 'custom workflow after rejected upgrade');
  assertCommandOk(
    [cli, 'pmo', 'upgrade', '--project-dir', customWorkflowRoot, '--adopt-governance'],
    customWorkflowRoot,
    'explicit custom governance workflow adoption',
  );
  const adoptedWorkflow = readFileSync(customWorkflowPath, 'utf8');
  if (!adoptedWorkflow.startsWith('# Managed by Mango CLI.')) {
    throw new Error('pmo upgrade --adopt-governance did not establish explicit workflow ownership');
  }
  if (existsSync(join(customWorkflowRoot, 'business-pmo/architecture-debt-budget.json'))) {
    throw new Error('pmo upgrade must never synthesize a legacy project architecture debt budget');
  }
  const idempotentGovernanceSync = assertCommandOk(
    [cli, 'pmo', 'sync', '--project-dir', customWorkflowRoot, '--dry-run'],
    customWorkflowRoot,
    'idempotent governance workflow sync',
  );
  for (const workflowPath of ['.github/workflows/pmo-doc-check.yml', '.gitea/workflows/pmo-doc-check.yml']) {
    if (!idempotentGovernanceSync.stdout.includes(`skip   ${workflowPath}`)) {
      throw new Error(`second pmo sync should leave managed workflow unchanged: ${workflowPath}`);
    }
  }
  const managedHistoricalWorkflow = `${readFileSync(customWorkflowPath, 'utf8')}\n# managed historical revision\n`;
  writeFileSync(customWorkflowPath, managedHistoricalWorkflow);
  assertCommandOk(
    [cli, 'pmo', 'sync', '--project-dir', customWorkflowRoot],
    customWorkflowRoot,
    'managed governance workflow refresh before rollback',
  );
  assertCommandOk(
    [cli, 'pmo', 'rollback', '--project-dir', customWorkflowRoot],
    customWorkflowRoot,
    'historical governance workflow rollback',
  );
  assertEqual(
    readFileSync(customWorkflowPath, 'utf8'),
    managedHistoricalWorkflow,
    'byte-exact historical workflow rollback',
  );
  assertCommandOk(
    [cli, 'pmo', 'sync', '--project-dir', customWorkflowRoot],
    customWorkflowRoot,
    'managed governance workflow repair after rollback',
  );

  const legacyWorkflowRoot = join(tempRoot, 'existing-business-legacy-workflow');
  for (const workflowPath of ['.github/workflows/pmo-doc-check.yml', '.gitea/workflows/pmo-doc-check.yml']) {
    const historical = spawnSync('git', ['show', `HEAD:mango-ui/packages/mango-cli/templates/full/${workflowPath}`], {
      cwd: packageRoot,
      encoding: 'utf8',
    });
    if (historical.status !== 0) {
      throw new Error(`cannot load historical canonical workflow ${workflowPath}: ${historical.stderr}`);
    }
    const target = join(legacyWorkflowRoot, workflowPath);
    mkdirSync(join(target, '..'), { recursive: true });
    writeFileSync(
      target,
      workflowPath.startsWith('.gitea/') ? historical.stdout.replaceAll('\n', '\r\n') : historical.stdout,
    );
  }
  assertCommandOk(
    [cli, 'pmo', 'upgrade', '--project-dir', legacyWorkflowRoot],
    legacyWorkflowRoot,
    'verified legacy governance workflow upgrade',
  );
  for (const workflowPath of ['.github/workflows/pmo-doc-check.yml', '.gitea/workflows/pmo-doc-check.yml']) {
    if (!readFileSync(join(legacyWorkflowRoot, workflowPath), 'utf8').startsWith('# Managed by Mango CLI.')) {
      throw new Error(`legacy canonical workflow was not upgraded: ${workflowPath}`);
    }
  }

  const syncRoot = join(tempRoot, 'existing-business-sync');
  mkdirSync(join(syncRoot, 'business-pmo/rules/domain'), { recursive: true });
  mkdirSync(join(syncRoot, 'business-docs/plans'), { recursive: true });
  const businessRulePath = join(syncRoot, 'business-pmo/rules/domain/01-owned.md');
  const businessDocPath = join(syncRoot, 'business-docs/plans/example-ledger.md');
  const ownedBusinessRule = '# owned business rule\n';
  const ownedBusinessDoc = '# owned business plan\n';
  writeFileSync(businessRulePath, ownedBusinessRule);
  writeFileSync(businessDocPath, ownedBusinessDoc);
  writeFileSync(
    join(syncRoot, 'AGENTS.md'),
    [
      '# Legacy business entry',
      '',
      'Run `/Users/hardy/Work/mango/mango-pmo/tools/pmo-preflight.mjs` before delivery.',
      '',
    ].join('\n'),
  );

  const warnOnlyResult = spawnSync(process.execPath, [cli, 'pmo', 'sync', '--project-dir', syncRoot], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (warnOnlyResult.status !== 0) {
    throw new Error(`pmo sync warn-only failed:\n${warnOnlyResult.stdout}\n${warnOnlyResult.stderr}`);
  }
  if (
    !warnOnlyResult.stdout.includes('warn   AGENTS.md') ||
    !readFileSync(join(syncRoot, 'AGENTS.md'), 'utf8').includes('/Users/hardy/Work/mango/mango-pmo')
  ) {
    throw new Error(
      `pmo sync should only warn about external AGENTS.md without --write-agents:\n${warnOnlyResult.stdout}`,
    );
  }

  const syncResult = spawnSync(process.execPath, [cli, 'pmo', 'sync', '--project-dir', syncRoot, '--write-agents'], {
    cwd: tempRoot,
    encoding: 'utf8',
  });
  if (syncResult.status !== 0) {
    throw new Error(`pmo sync failed:\n${syncResult.stdout}\n${syncResult.stderr}`);
  }
  for (const file of [
    '.github/pull_request_template.md',
    'business-pmo/README.md',
    'business-pmo/mango-baseline/README.md',
    'business-pmo/mango-baseline/baseline.json',
    'business-pmo/pmo-lock.json',
    '.agents/skills/.mango-pmo.json',
    '.agents/skills/mango-pmo-lifecycle/SKILL.md',
    'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
    'business-pmo/mango-baseline/rules/backend/07-persistence.md',
    'business-docs/plans/example-contract.md',
  ]) {
    if (!existsSync(join(syncRoot, file))) {
      throw new Error(`pmo sync missing generated file: ${file}`);
    }
  }
  assertEqual(readFileSync(businessRulePath, 'utf8'), ownedBusinessRule, 'business-owned PMO rule after sync');
  assertEqual(readFileSync(businessDocPath, 'utf8'), ownedBusinessDoc, 'business-owned plan after sync');
  const syncedAgents = readFileSync(join(syncRoot, 'AGENTS.md'), 'utf8');
  if (
    !syncedAgents.includes('business-pmo/mango-baseline/tools/pmo-preflight.mjs') ||
    syncedAgents.includes('/Users/hardy/Work/mango/mango-pmo')
  ) {
    throw new Error('pmo sync --write-agents did not migrate AGENTS.md to project-local baseline');
  }
  const syncedReadme = readFileSync(join(syncRoot, 'business-pmo/mango-baseline/README.md'), 'utf8');
  if (!syncedReadme.includes('mango pmo check') || syncedReadme.includes('{{')) {
    throw new Error('pmo sync baseline README missing versioned package guidance');
  }
  const syncedBaselineManifest = JSON.parse(
    readFileSync(join(syncRoot, 'business-pmo/mango-baseline/baseline.json'), 'utf8'),
  );
  assertEqual(syncedBaselineManifest.packageName, '@mango/pmo', 'synced PMO baseline package');
  assertCommandOk([cli, 'pmo', 'check', '--project-dir', syncRoot], syncRoot, 'synced mango pmo check');
  const persistenceRule = readFileSync(
    join(syncRoot, 'business-pmo/mango-baseline/rules/backend/07-persistence.md'),
    'utf8',
  );
  for (const expected of [
    '数据库命名规则',
    'Mango 模块独立数据库统一使用 `mango_{module}`',
    '禁止新增 `job`、`system`、`file`',
  ]) {
    if (!persistenceRule.includes(expected)) {
      throw new Error(`pmo sync baseline persistence rule missing latest DB naming rule: ${expected}`);
    }
  }
  const baselinePreflight = spawnSync(
    process.execPath,
    [
      'business-pmo/mango-baseline/tools/pmo-preflight.mjs',
      '--role',
      'dev',
      '--phase',
      'develop',
      '--task',
      '验证 baseline 同步',
      '--paths',
      'backend/**,frontend/**',
    ],
    {
      cwd: syncRoot,
      encoding: 'utf8',
    },
  );
  if (baselinePreflight.status !== 0) {
    throw new Error(`synced PMO preflight failed:\n${baselinePreflight.stdout}\n${baselinePreflight.stderr}`);
  }
  if (
    !baselinePreflight.stdout.includes('rules/backend/10-dev-flow.md') ||
    !baselinePreflight.stdout.includes('rules/frontend/04-test.md')
  ) {
    throw new Error(`synced PMO preflight did not include expected rules:\n${baselinePreflight.stdout}`);
  }

  const shellSyncRoot = join(tempRoot, 'existing-business-shell-sync');
  mkdirSync(join(shellSyncRoot, 'backend/app'), { recursive: true });
  mkdirSync(join(shellSyncRoot, 'frontend'), { recursive: true });
  writeFileSync(join(shellSyncRoot, 'backend/pom.xml'), '<project></project>\n');
  writeFileSync(join(shellSyncRoot, 'backend/app/pom.xml'), '<project></project>\n');
  const shellSyncResult = spawnSync(
    process.execPath,
    [cli, 'pmo', 'sync', '--project-dir', shellSyncRoot, '--sync-shell'],
    {
      cwd: tempRoot,
      encoding: 'utf8',
    },
  );
  if (shellSyncResult.status !== 0) {
    throw new Error(`pmo sync --sync-shell failed:\n${shellSyncResult.stdout}\n${shellSyncResult.stderr}`);
  }
  if (
    !existsSync(join(shellSyncRoot, 'mango.dev.json')) ||
    !readFileSync(join(shellSyncRoot, 'scripts/dev-workspace.sh'), 'utf8').includes(
      'scripts/dev-workspace.sh is deprecated',
    )
  ) {
    throw new Error('pmo sync --sync-shell should install mango.dev.json and CLI shim scripts');
  }
  assertCommandOk([cli, 'validate'], shellSyncRoot, 'synced mango validate');

  const discoveredShellRoot = join(tempRoot, 'existing-business-discovered-shell-sync');
  mkdirSync(join(discoveredShellRoot, 'baohan-backend'), { recursive: true });
  mkdirSync(join(discoveredShellRoot, 'baohan-backend/apps/baohan-api'), { recursive: true });
  mkdirSync(join(discoveredShellRoot, 'baohan-ui/apps/admin-console'), { recursive: true });
  mkdirSync(join(discoveredShellRoot, 'baohan-ui/apps/portal-console'), { recursive: true });
  writeFileSync(
    join(discoveredShellRoot, 'baohan-backend/pom.xml'),
    [
      '<project>',
      '  <artifactId>baohan-backend</artifactId>',
      '  <version>${revision}</version>',
      '  <packaging>pom</packaging>',
      '  <properties><revision>1.0.0-SNAPSHOT</revision></properties>',
      '  <modules><module>apps/baohan-api</module></modules>',
      '</project>',
    ].join('\n'),
  );
  writeFileSync(
    join(discoveredShellRoot, 'baohan-backend/apps/baohan-api/pom.xml'),
    [
      '<project>',
      '  <dependencies>',
      '    <dependency>',
      '      <groupId>org.springframework.boot</groupId>',
      '      <artifactId>spring-boot-starter-web</artifactId>',
      '    </dependency>',
      '  </dependencies>',
      '  <build><plugins><plugin>',
      '    <groupId>org.springframework.boot</groupId>',
      '    <artifactId>spring-boot-maven-plugin</artifactId>',
      '  </plugin></plugins></build>',
      '</project>',
    ].join('\n'),
  );
  writeFileSync(join(discoveredShellRoot, 'baohan-ui/pnpm-lock.yaml'), 'lockfileVersion: 9.0\n');
  for (const appPath of ['admin-console', 'portal-console']) {
    writeFileSync(
      join(discoveredShellRoot, `baohan-ui/apps/${appPath}/package.json`),
      JSON.stringify(
        {
          scripts: { dev: 'vite --host 127.0.0.1' },
          dependencies: { vite: '^4.3.3' },
        },
        null,
        2,
      ),
    );
    writeFileSync(join(discoveredShellRoot, `baohan-ui/apps/${appPath}/vite.config.ts`), 'export default {};\n');
  }
  const discoveredDryRun = spawnSync(
    process.execPath,
    [cli, 'pmo', 'sync', '--project-dir', discoveredShellRoot, '--sync-shell', '--dry-run'],
    {
      cwd: tempRoot,
      encoding: 'utf8',
    },
  );
  if (
    discoveredDryRun.status !== 0 ||
    !discoveredDryRun.stdout.includes('skipped aggregator POM') ||
    !discoveredDryRun.stdout.includes('confirm groups before starting')
  ) {
    throw new Error(
      `pmo sync --sync-shell dry-run should report discovered layout confirmation items:\n${discoveredDryRun.stdout}\n${discoveredDryRun.stderr}`,
    );
  }
  const discoveredSync = spawnSync(
    process.execPath,
    [cli, 'pmo', 'sync', '--project-dir', discoveredShellRoot, '--sync-shell'],
    {
      cwd: tempRoot,
      encoding: 'utf8',
    },
  );
  if (discoveredSync.status !== 0) {
    throw new Error(
      `pmo sync --sync-shell discovered layout failed:\n${discoveredSync.stdout}\n${discoveredSync.stderr}`,
    );
  }
  const discoveredManifest = JSON.parse(readFileSync(join(discoveredShellRoot, 'mango.dev.json'), 'utf8'));
  if (
    !discoveredManifest.apps['baohan-api'] ||
    discoveredManifest.apps['baohan-api'].cwd !== 'baohan-backend/apps/baohan-api' ||
    discoveredManifest.apps['baohan-api'].pom !== 'pom.xml' ||
    discoveredManifest.apps['baohan-api'].pom === 'baohan-backend/pom.xml' ||
    !discoveredManifest.apps['admin-console'] ||
    discoveredManifest.apps['admin-console'].cwd !== 'baohan-ui/apps/admin-console' ||
    discoveredManifest.apps['admin-console'].packageManager !== 'pnpm' ||
    !discoveredManifest.apps['portal-console'] ||
    discoveredManifest.groups.default.join(',') !== 'baohan-api,admin-console' ||
    discoveredManifest.groups.frontend.join(',') !== 'admin-console,portal-console'
  ) {
    throw new Error(
      `discovered mango.dev.json did not match real business layout:\n${JSON.stringify(discoveredManifest, null, 2)}`,
    );
  }
  assertCommandOk([cli, 'validate'], discoveredShellRoot, 'discovered mango validate');
  const discoveredPlan = assertCommandOk([cli, 'plan', 'frontend'], discoveredShellRoot, 'discovered frontend plan');
  const normalizedDiscoveredPlan = discoveredPlan.stdout.replace(/\\/gu, '/');
  if (
    !normalizedDiscoveredPlan.includes('baohan-ui/apps/admin-console') ||
    !normalizedDiscoveredPlan.includes('baohan-ui/apps/portal-console') ||
    normalizedDiscoveredPlan.includes('-f baohan-backend/pom.xml')
  ) {
    throw new Error(
      `discovered frontend plan should use detected Vite apps and not aggregator POM:\n${discoveredPlan.stdout}`,
    );
  }

  const ownedManifestRoot = join(tempRoot, 'existing-business-owned-manifest');
  mkdirSync(ownedManifestRoot, { recursive: true });
  const ownedManifest =
    '{"version":1,"groups":{"default":["custom-app"]},"apps":{"custom-app":{"type":"command","cwd":".","command":"node","args":["--version"]}}}\n';
  writeFileSync(join(ownedManifestRoot, 'mango.dev.json'), ownedManifest);
  const ownedSyncResult = spawnSync(
    process.execPath,
    [cli, 'pmo', 'sync', '--project-dir', ownedManifestRoot, '--sync-shell'],
    {
      cwd: tempRoot,
      encoding: 'utf8',
    },
  );
  if (ownedSyncResult.status !== 0) {
    throw new Error(
      `pmo sync --sync-shell with owned manifest failed:\n${ownedSyncResult.stdout}\n${ownedSyncResult.stderr}`,
    );
  }
  assertEqual(
    readFileSync(join(ownedManifestRoot, 'mango.dev.json'), 'utf8'),
    ownedManifest,
    'business-owned mango.dev.json after sync',
  );
  if (process.platform !== 'win32') {
    assertDevWorkspaceRegistryAllocation(tempRoot);
  }
}

function assertDevWorkspaceRegistryAllocation(tempRoot) {
  const registryPath = join(tempRoot, 'workspaces.json');
  const testHome = join(tempRoot, 'workspace-home');
  mkdirSync(testHome, { recursive: true });
  process.env.HOME = testHome;
  process.env.USERPROFILE = testHome;
  const roots = [join(tempRoot, 'workspace-a'), join(tempRoot, 'workspace-b')];
  for (const root of roots) {
    mkdirSync(root, { recursive: true });
    writeFileSync(
      join(root, 'mango.dev.json'),
      `${JSON.stringify(
        {
          version: 1,
          groups: { default: ['backend'] },
          apps: {
            backend: {
              type: 'command',
              cwd: '.',
              command: 'node',
              args: ['--version'],
              portEnv: 'MANGO_BACKEND_PORT',
              port: 5555,
            },
          },
        },
        null,
        2,
      )}\n`,
    );
    const result = spawnSync(
      'env',
      [`MANGO_WORKSPACE_REGISTRY=${registryPath}`, process.execPath, cli, 'workspace', 'init'],
      {
        cwd: root,
        encoding: 'utf8',
      },
    );
    if (result.status !== 0) {
      throw new Error(`mango workspace init should allocate workspace env:\n${result.stdout}\n${result.stderr}`);
    }
  }
  const workspaces = roots.map((root) => JSON.parse(readFileSync(join(root, '.mango/workspace.json'), 'utf8')));
  const envs = roots.map((root) => parseSimpleEnv(readFileSync(join(root, '.mango/dev-workspace.env'), 'utf8')));
  const sharedMavenRepository = join(testHome, '.m2/repository');
  for (const root of roots) {
    const workspaceMavenRepository = join(root, '.mango/m2/repository');
    if (
      !lstatSync(workspaceMavenRepository).isSymbolicLink() ||
      realpathSync(workspaceMavenRepository) !== realpathSync(sharedMavenRepository)
    ) {
      throw new Error(
        `workspace init should link the worktree Maven repository to the shared user repository: ${workspaceMavenRepository}`,
      );
    }
  }
  if (
    workspaces[0].slot === workspaces[1].slot ||
    workspaces[0].backendPort === workspaces[1].backendPort ||
    workspaces[0].frontendPort === workspaces[1].frontendPort ||
    workspaces[0].dbName === workspaces[1].dbName ||
    workspaces[0].mavenRevisionQualifier === workspaces[1].mavenRevisionQualifier
  ) {
    throw new Error(
      `workspace allocation should isolate workspace.json values:\n${JSON.stringify(workspaces, null, 2)}`,
    );
  }
  if (
    envs[0].MANGO_BACKEND_PORT === envs[1].MANGO_BACKEND_PORT ||
    envs[0].MANGO_FRONTEND_PORT === envs[1].MANGO_FRONTEND_PORT ||
    envs[0].MANGO_DB_NAME === envs[1].MANGO_DB_NAME ||
    envs[0].MANGO_MAVEN_REVISION_QUALIFIER === envs[1].MANGO_MAVEN_REVISION_QUALIFIER
  ) {
    throw new Error(`workspace allocation should isolate ports and DBs:\n${JSON.stringify(envs, null, 2)}`);
  }
  if (
    !/^mango_[0-9]{3}$/.test(workspaces[0].workspaceId) ||
    !String(workspaces[0].backendPort).startsWith('180') ||
    !String(workspaces[0].frontendPort).startsWith('300')
  ) {
    throw new Error(`workspace.json should contain stable slot allocation:\n${JSON.stringify(workspaces[0], null, 2)}`);
  }
  for (const workspace of workspaces) {
    const slotText = String(workspace.slot).padStart(3, '0');
    if (
      workspace.backendPort !== 18000 + workspace.slot ||
      workspace.frontendPort !== 30000 + workspace.slot ||
      workspace.frontendApps.MANGO_ADMIN_SHELL_PORT !== 31000 + workspace.slot ||
      workspace.frontendApps.MANGO_ADMIN_RBAC_APP_PORT !== 32000 + workspace.slot ||
      workspace.mavenRevisionQualifier !== `mango-${slotText}` ||
      !workspace.dbName.endsWith(`_${slotText}`)
    ) {
      throw new Error(
        `workspace ports and DB suffix must share the same workspace number:\n${JSON.stringify(workspace, null, 2)}`,
      );
    }
  }
  assertWorkspaceDatabaseStatus(roots[0], registryPath, workspaces[0]);
  const before = readFileSync(join(roots[0], '.mango/dev-workspace.env'), 'utf8');
  const workspaceBefore = readFileSync(join(roots[0], '.mango/workspace.json'), 'utf8');
  const repeat = spawnSync(
    'env',
    [`MANGO_WORKSPACE_REGISTRY=${registryPath}`, process.execPath, cli, 'workspace', 'init'],
    {
      cwd: roots[0],
      encoding: 'utf8',
    },
  );
  if (repeat.status !== 0) {
    throw new Error(`mango workspace init repeat should preserve existing env:\n${repeat.stdout}\n${repeat.stderr}`);
  }
  assertEqual(
    readFileSync(join(roots[0], '.mango/dev-workspace.env'), 'utf8'),
    before,
    'existing dev-workspace.env after repeat init',
  );
  assertEqual(
    readFileSync(join(roots[0], '.mango/workspace.json'), 'utf8'),
    workspaceBefore,
    'existing workspace.json after repeat init',
  );
  if (realpathSync(join(roots[0], '.mango/m2/repository')) !== realpathSync(sharedMavenRepository)) {
    throw new Error('mango workspace init repeat should preserve the shared Maven repository link');
  }

  const isolatedMavenRoot = join(tempRoot, 'workspace-isolated-maven');
  const isolatedMavenRepository = join(isolatedMavenRoot, '.mango/m2/repository');
  mkdirSync(isolatedMavenRepository, { recursive: true });
  writeFileSync(join(isolatedMavenRoot, 'mango.dev.json'), '{"version":1,"groups":{"default":[]},"apps":{}}\n');
  writeFileSync(join(isolatedMavenRepository, '.keep'), 'isolated\n');
  const isolatedMavenInit = spawnSync(
    'env',
    [`MANGO_WORKSPACE_REGISTRY=${registryPath}`, process.execPath, cli, 'workspace', 'init'],
    {
      cwd: isolatedMavenRoot,
      encoding: 'utf8',
    },
  );
  if (
    isolatedMavenInit.status !== 0 ||
    !isolatedMavenInit.stdout.includes('preserving existing Maven repository') ||
    lstatSync(isolatedMavenRepository).isSymbolicLink() ||
    readFileSync(join(isolatedMavenRepository, '.keep'), 'utf8') !== 'isolated\n'
  ) {
    throw new Error(
      `workspace init should preserve an explicitly isolated Maven repository:\n${isolatedMavenInit.stdout}\n${isolatedMavenInit.stderr}`,
    );
  }

  writeFileSync(
    join(roots[0], '.mango/dev-workspace.env'),
    [
      'MANGO_BACKEND_PORT=5555',
      'MANGO_FRONTEND_PORT=5176',
      'MANGO_DB_NAME=old_business_db',
      'MANGO_DB_USERNAME=root',
      "MANGO_DB_PASSWORD=''",
      '',
    ].join('\n'),
  );
  const legacyWorkspace = JSON.parse(readFileSync(join(roots[0], '.mango/workspace.json'), 'utf8'));
  delete legacyWorkspace.mavenRevisionQualifier;
  writeFileSync(join(roots[0], '.mango/workspace.json'), `${JSON.stringify(legacyWorkspace, null, 2)}\n`);
  const syncRepeat = spawnSync(
    'env',
    [`MANGO_WORKSPACE_REGISTRY=${registryPath}`, process.execPath, cli, 'workspace', 'init'],
    {
      cwd: roots[0],
      encoding: 'utf8',
    },
  );
  if (syncRepeat.status !== 0) {
    throw new Error(
      `mango workspace init repeat should synchronize stale env:\n${syncRepeat.stdout}\n${syncRepeat.stderr}`,
    );
  }
  const syncedEnv = parseSimpleEnv(readFileSync(join(roots[0], '.mango/dev-workspace.env'), 'utf8'));
  if (
    syncedEnv.MANGO_BACKEND_PORT !== String(workspaces[0].backendPort) ||
    syncedEnv.MANGO_FRONTEND_PORT !== String(workspaces[0].frontendPort) ||
    syncedEnv.MANGO_DB_NAME !== workspaces[0].dbName ||
    syncedEnv.MANGO_MAVEN_REVISION_QUALIFIER !== workspaces[0].mavenRevisionQualifier ||
    syncedEnv.MANGO_DB_USERNAME !== 'root'
  ) {
    throw new Error(
      `mango workspace init repeat should sync ownership fields but preserve DB connection fields:\n${JSON.stringify(syncedEnv, null, 2)}`,
    );
  }
  const migratedWorkspace = JSON.parse(readFileSync(join(roots[0], '.mango/workspace.json'), 'utf8'));
  if (migratedWorkspace.mavenRevisionQualifier !== workspaces[0].mavenRevisionQualifier) {
    throw new Error(
      `mango workspace init must migrate a legacy workspace qualifier:\n${JSON.stringify(migratedWorkspace, null, 2)}`,
    );
  }

  const fakeBinDir = join(tempRoot, 'release-fake-bin');
  const releaseCallLog = join(tempRoot, 'release-db-calls.log');
  mkdirSync(fakeBinDir, { recursive: true });
  writeFileSync(
    join(fakeBinDir, 'mysql'),
    ['#!/usr/bin/env sh', `echo "mysql:$*" >> "${releaseCallLog}"`, 'exit 0', ''].join('\n'),
  );
  chmodExecutable(join(fakeBinDir, 'mysql'));
  const release = spawnSync(
    'env',
    [
      `MANGO_WORKSPACE_REGISTRY=${registryPath}`,
      `PATH=${fakeBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`,
      process.execPath,
      cli,
      'workspace',
      'release',
      '--workspace',
      roots[1],
    ],
    {
      cwd: roots[1],
      encoding: 'utf8',
    },
  );
  if (release.status !== 0) {
    throw new Error(
      `mango workspace release should drop workspace DB by default:\n${release.stdout}\n${release.stderr}`,
    );
  }
  const releaseCalls = waitForCallLogLines(releaseCallLog, 1);
  if (!releaseCalls.some((line) => line.includes(`DROP DATABASE IF EXISTS \`${workspaces[1].dbName}\``))) {
    throw new Error(`mango workspace release should drop the owned workspace database:\n${releaseCalls.join('\n')}`);
  }
  const registryAfterRelease = JSON.parse(readFileSync(registryPath, 'utf8'));
  if (registryAfterRelease.some((entry) => entry.root === roots[1])) {
    throw new Error(
      `mango workspace release should remove the workspace registry entry:\n${JSON.stringify(registryAfterRelease, null, 2)}`,
    );
  }

  const existingDbRoot = join(tempRoot, 'db-existing-root');
  const existingDbRegistryPath = join(tempRoot, 'db-existing-workspaces.json');
  const existingDbFakeBinDir = join(tempRoot, 'db-existing-fake-bin');
  const existingDbCallLog = join(tempRoot, 'db-existing-calls.log');
  mkdirSync(existingDbRoot, { recursive: true });
  mkdirSync(existingDbFakeBinDir, { recursive: true });
  writeFileSync(
    join(existingDbRoot, 'mango.dev.json'),
    `${JSON.stringify(
      {
        version: 1,
        groups: { default: ['backend'] },
        apps: {
          backend: {
            type: 'command',
            cwd: '.',
            command: 'node',
            args: ['--version'],
            portEnv: 'MANGO_BACKEND_PORT',
          },
        },
      },
      null,
      2,
    )}\n`,
  );
  writeFileSync(
    join(existingDbFakeBinDir, 'mysql'),
    [
      '#!/usr/bin/env sh',
      `echo "mysql:$*" >> "${existingDbCallLog}"`,
      `case "$*" in *mango_dev_db_existing_root_001*) echo "mango_dev_db_existing_root_001";; esac`,
      'exit 0',
      '',
    ].join('\n'),
  );
  chmodExecutable(join(existingDbFakeBinDir, 'mysql'));
  writeFileSync(join(existingDbFakeBinDir, 'lsof'), ['#!/usr/bin/env sh', 'exit 1', ''].join('\n'));
  chmodExecutable(join(existingDbFakeBinDir, 'lsof'));
  const existingDbInit = spawnSync(
    'env',
    [
      `MANGO_WORKSPACE_REGISTRY=${existingDbRegistryPath}`,
      `PATH=${existingDbFakeBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`,
      process.execPath,
      cli,
      'workspace',
      'init',
    ],
    {
      cwd: existingDbRoot,
      encoding: 'utf8',
    },
  );
  if (existingDbInit.status !== 0) {
    throw new Error(
      `mango workspace init should skip existing local MySQL DB names:\n${existingDbInit.stdout}\n${existingDbInit.stderr}`,
    );
  }
  const existingDbWorkspace = JSON.parse(readFileSync(join(existingDbRoot, '.mango/workspace.json'), 'utf8'));
  if (existingDbWorkspace.slot <= 1 || existingDbWorkspace.dbName === 'mango_dev_db_existing_root_001') {
    throw new Error(
      `mango workspace init should skip workspace number 001 when DB 001 already exists:\n${JSON.stringify(existingDbWorkspace, null, 2)}`,
    );
  }
  const existingDbCalls = waitForCallLogLines(existingDbCallLog, 2);
  if (
    !existingDbCalls.some((line) => line.includes('mango_dev_db_existing_root_001')) ||
    !existingDbCalls.some((line) => line.includes(existingDbWorkspace.dbName))
  ) {
    throw new Error(`mango workspace init should probe DB names before allocation:\n${existingDbCalls.join('\n')}`);
  }

  const legacyRegistryPath = join(tempRoot, 'legacy-workspaces.tsv');
  writeFileSync(
    legacyRegistryPath,
    `${join(tempRoot, 'legacy-root')}\tmango_123\t18123\t30123\tmango_dev_legacy_root_123\n`,
  );
  const legacyList = spawnSync(
    'env',
    [`MANGO_WORKSPACE_REGISTRY=${legacyRegistryPath}`, process.execPath, cli, 'workspace', 'list'],
    {
      cwd: tempRoot,
      encoding: 'utf8',
    },
  );
  if (
    legacyList.status !== 0 ||
    !legacyList.stdout.includes('slot=123') ||
    !legacyList.stdout.includes('backend=18123') ||
    !legacyList.stdout.includes('frontend=30123') ||
    !legacyList.stdout.includes('db=mango_dev_legacy_root_123')
  ) {
    throw new Error(
      `legacy workspace TSV registry should be readable during migration:\n${legacyList.stdout}\n${legacyList.stderr}`,
    );
  }
}

function assertWorkspaceDatabaseStatus(workspaceRoot, registryPath, workspace) {
  const statusBinDir = join(workspaceRoot, '.runtime/status-bin');
  mkdirSync(statusBinDir, { recursive: true });
  writeFileSync(
    join(statusBinDir, 'mysql'),
    ['#!/usr/bin/env sh', `echo "${workspace.dbName}"`, 'exit 0', ''].join('\n'),
  );
  chmodExecutable(join(statusBinDir, 'mysql'));
  const baseEnv = [`MANGO_WORKSPACE_REGISTRY=${registryPath}`, `PATH=${statusBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`];
  const workspaceStatus = spawnSync('env', [...baseEnv, process.execPath, cli, 'workspace', 'status'], {
    cwd: workspaceRoot,
    encoding: 'utf8',
  });
  if (
    workspaceStatus.status !== 0 ||
    !workspaceStatus.stdout.includes(`Database:  ${workspace.dbName}`) ||
    !workspaceStatus.stdout.includes('DB exists: YES') ||
    !workspaceStatus.stdout.includes('Init source: mango workspace init ->') ||
    !workspaceStatus.stdout.includes('DB env match: PASS') ||
    !workspaceStatus.stdout.includes('DB auto-create: enabled (mango dev start)')
  ) {
    throw new Error(
      `workspace status should show effective database and init provenance:\n${workspaceStatus.stdout}\n${workspaceStatus.stderr}`,
    );
  }

  const missingMysqlBinDir = join(workspaceRoot, '.runtime/status-missing-mysql-bin');
  mkdirSync(missingMysqlBinDir, { recursive: true });
  const unknownStatus = spawnSync(
    'env',
    [
      `MANGO_WORKSPACE_REGISTRY=${registryPath}`,
      `PATH=${missingMysqlBinDir}:/usr/bin:/bin:/usr/sbin:/sbin`,
      process.execPath,
      cli,
      'workspace',
      'status',
    ],
    {
      cwd: workspaceRoot,
      encoding: 'utf8',
    },
  );
  if (unknownStatus.status !== 0 || !unknownStatus.stdout.includes('DB exists: UNKNOWN')) {
    throw new Error(
      `workspace status should keep MySQL probe failures distinct from a missing database:\n${unknownStatus.stdout}\n${unknownStatus.stderr}`,
    );
  }

  const pidDir = join(workspaceRoot, '.mango/run/pids');
  mkdirSync(pidDir, { recursive: true });
  writeFileSync(
    join(pidDir, 'backend.json'),
    `${JSON.stringify(
      {
        pid: process.pid,
        pgid: process.pid,
        startedAt: new Date().toISOString(),
        cwd: workspaceRoot,
        command: 'node',
        args: [
          '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/mango_dev_wrong_999?useUnicode=true',
          '--spring.datasource.password=top-secret-status-test',
        ],
        port: workspace.backendPort,
        url: `http://127.0.0.1:${workspace.backendPort}`,
        workspaceId: workspace.workspaceId,
        workspaceRoot,
      },
      null,
      2,
    )}\n`,
  );
  const mismatchStatus = spawnSync('env', [...baseEnv, process.execPath, cli, 'dev', 'status'], {
    cwd: workspaceRoot,
    encoding: 'utf8',
  });
  if (
    mismatchStatus.status === 0 ||
    !mismatchStatus.stdout.includes('datasourceDb=mango_dev_wrong_999 dbMatch=FAIL') ||
    !mismatchStatus.stderr.includes('running backend datasource does not match workspace database') ||
    `${mismatchStatus.stdout}\n${mismatchStatus.stderr}`.includes('top-secret-status-test')
  ) {
    throw new Error(
      `dev status should fail a datasource mismatch without exposing credentials:\n${mismatchStatus.stdout}\n${mismatchStatus.stderr}`,
    );
  }
}

function parseSimpleEnv(content) {
  const result = {};
  for (const line of content.split(/\r?\n/)) {
    const index = line.indexOf('=');
    if (index > 0) {
      result[line.slice(0, index)] = line.slice(index + 1);
    }
  }
  return result;
}

function assertManagedDependency(pom, groupId, artifactId) {
  const expected = [
    '<dependency>',
    `            <groupId>${groupId}</groupId>`,
    `            <artifactId>${artifactId}</artifactId>`,
    '            <version>${mango.version}</version>',
    '</dependency>',
  ];
  for (const line of expected) {
    if (!pom.includes(line)) {
      throw new Error(`generated backend parent pom missing managed dependency ${groupId}:${artifactId}`);
    }
  }
}

function assertNoDirectDependency(pom, artifactId, label) {
  if (pom.includes(`<artifactId>${artifactId}</artifactId>`)) {
    throw new Error(`${label} should not directly depend on managed API artifact: ${artifactId}`);
  }
}

function assertNoUnrenderedPlaceholders(projectRoot) {
  const placeholderFiles = walkFiles(projectRoot)
    .map((file) => ({
      file,
      relativePath: relative(projectRoot, file).split('\\').join('/'),
    }))
    .filter(({ relativePath }) => !relativePath.startsWith('business-pmo/mango-baseline/'))
    .filter(({ file }) => hasUnrenderedMangoPlaceholder(readFileSync(file)))
    .map(({ relativePath }) => relativePath)
    .sort();
  if (placeholderFiles.length > 0) {
    throw new Error(`generated project contains unrendered placeholders:\n${placeholderFiles.join('\n')}`);
  }
}

function hasUnrenderedMangoPlaceholder(content) {
  return /(^|[^$])\{\{[^{}\n]+}}/m.test(content);
}

function assertNoWorkspacePackageJsonInTemplates() {
  const templateRoot = join(packageRoot, 'templates');
  const packageJsonFiles = walkFiles(templateRoot).filter((file) => file.endsWith('/package.json'));
  if (packageJsonFiles.length > 0) {
    throw new Error(
      `template package.json files are parsed by workspace tooling; use package.json.template instead:\n${packageJsonFiles.join('\n')}`,
    );
  }
}

function assertQualityConfigsMatchPluginSources() {
  const repoRoot = resolve(packageRoot, '../../..');
  const pairs = [
    ['checkstyle.xml', 'checkstyle.xml'],
    ['pmd-p3c.xml', 'pmd-p3c.xml'],
  ];
  for (const [templateName, sourceName] of pairs) {
    const template = readFileSync(join(packageRoot, 'templates/full/backend/config/quality', templateName));
    const source = readFileSync(
      join(repoRoot, 'mango/mango-tools/mango-maven-plugin/src/main/resources/rulesets/java', sourceName),
    );
    if (!template.equals(source)) {
      throw new Error(`generated Java quality config differs from mango-maven-plugin source: ${templateName}`);
    }
  }
}

function walkFiles(root) {
  const result = [];
  for (const entry of readdirSync(root)) {
    const fullPath = join(root, entry);
    const fileStat = lstatSync(fullPath);
    if (fileStat.isSymbolicLink()) {
      continue;
    }
    if (fileStat.isDirectory()) {
      result.push(...walkFiles(fullPath));
    } else {
      result.push(fullPath);
    }
  }
  return result;
}
