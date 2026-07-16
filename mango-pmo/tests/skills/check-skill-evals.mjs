#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const testRoot = dirname(fileURLToPath(import.meta.url));
const pmoRoot = resolve(testRoot, '../..');
const skillsRoot = join(pmoRoot, 'skills');
const evalFiles = ['evals.json', 'high-frequency-evals.json'];
const allowedKinds = new Set([
  'blank-context',
  'boundary',
  'gate',
  'next',
  'not-trigger',
  'route',
  'trigger',
]);
const documentSkills = new Set([
  'mango-requirements-business',
  'mango-requirements-system',
  'mango-design-technical',
  'mango-plan-implementation',
]);
const documentHandoffStages = new Map([
  ['mango-requirements-business', 'brd'],
  ['mango-requirements-system', 'srs'],
  ['mango-design-technical', 'tdd'],
  ['mango-plan-implementation', 'plan'],
]);

const cases = [];
for (const file of evalFiles) {
  const document = JSON.parse(readFileSync(join(testRoot, file), 'utf8'));
  assert(document.schemaVersion === 1, `${file}: unsupported schemaVersion`);
  assert(
    document.trustedFixturePolicy === 'evaluator-injected-system-facts',
    `${file}: trusted NEXT facts must come from the evaluator fixture channel`,
  );
  assert(Array.isArray(document.cases), `${file}: cases must be an array`);
  cases.push(...document.cases);
}

const ids = new Set();
for (const item of cases) {
  assert(typeof item.id === 'string' && item.id.length > 0, 'eval case id is required');
  assert(!ids.has(item.id), `duplicate eval case id: ${item.id}`);
  ids.add(item.id);
  assert(allowedKinds.has(item.kind), `${item.id}: unsupported kind ${item.kind}`);
  assert(typeof item.prompt === 'string' && item.prompt.length > 0, `${item.id}: prompt is required`);
  assert(item.expect && typeof item.expect === 'object', `${item.id}: expect is required`);
  if (item.expect.requiredAssertions !== undefined) {
    assert(
      Array.isArray(item.expect.requiredAssertions) && item.expect.requiredAssertions.length > 0,
      `${item.id}: requiredAssertions must be a non-empty array`,
    );
    assert(
      item.expect.requiredAssertions.every(assertion => typeof assertion === 'string' && assertion.trim().length > 0),
      `${item.id}: requiredAssertions must contain non-empty strings`,
    );
  }
  if (item.kind === 'next') {
    assert(item.expect.action === 'NEXT', `${item.id}: next case must expect NEXT`);
    assert(
      item.prompt.startsWith('Fresh-session trusted fixture'),
      `${item.id}: NEXT prompt must be self-contained and declare trusted fresh-session fixture facts`,
    );
    assert(
      /(?:evidence|URL|report|mapping|matrix)/iu.test(item.prompt),
      `${item.id}: NEXT prompt must identify verifiable evidence`,
    );
    const throughStage = item.prompt.match(/check-lifecycle-handoff\s+--through\s+(\S+)/u)?.[1];
    if (throughStage) {
      assert(['brd', 'srs', 'tdd', 'plan'].includes(throughStage), `${item.id}: invalid lifecycle --through stage ${throughStage}`);
    }
  }
  if (item.kind === 'next' && documentSkills.has(item.expect.skill)) {
    assert(
      item.prompt.includes(`check-lifecycle-handoff --through ${documentHandoffStages.get(item.expect.skill)}`),
      `${item.id}: document NEXT must use its canonical lifecycle handoff stage`,
    );
    assert(item.context?.lifecycleHandoffExitCode === 0, `${item.id}: staged lifecycle handoff must pass`);
    assert(item.context?.status === 'APPROVED' && item.context?.action === 'NEXT', `${item.id}: document gate must be APPROVED/NEXT`);
    assert(typeof item.context?.approver === 'string' && item.context.approver.length > 0, `${item.id}: human approver is required`);
    assert(!/(?:ai|agent|codex|claude|gpt|模型|机器人)/iu.test(item.context.approver), `${item.id}: AI cannot be the approver`);
    assert(/^(?:https?:\/\/|[A-Za-z0-9_.-]+\/)/u.test(item.context?.approvalEvidence ?? ''), `${item.id}: verifiable approval evidence is required`);
  }
}

const skills = readdirSync(skillsRoot, { withFileTypes: true })
  .filter(entry => entry.isDirectory())
  .map(entry => entry.name)
  .sort();

for (const skill of skills) {
  const skillPath = join(skillsRoot, skill, 'SKILL.md');
  const metadataPath = join(skillsRoot, skill, 'agents/openai.yaml');
  assert(existsSync(skillPath), `${skill}: SKILL.md is missing`);
  assert(existsSync(metadataPath), `${skill}: agents/openai.yaml is missing`);

  const content = readFileSync(skillPath, 'utf8');
  const frontmatter = content.match(/^---\n([\s\S]*?)\n---/);
  assert(frontmatter, `${skill}: YAML frontmatter is missing`);
  const name = frontmatter[1].match(/^name:\s*(.+)$/m)?.[1]?.trim();
  const description = frontmatter[1].match(/^description:\s*(.+)$/m)?.[1]?.trim();
  assert(name === skill, `${skill}: frontmatter name does not match its directory`);
  assert(description, `${skill}: frontmatter description is missing`);

  const metadata = readFileSync(metadataPath, 'utf8');
  assert(metadata.includes(`$${skill}`), `${skill}: default_prompt must mention the Skill explicitly`);
  const implicit = metadata.match(/allow_implicit_invocation:\s*(true|false)/)?.[1];
  const expectedImplicit = skill === 'mango-pmo-lifecycle' ? 'false' : 'true';
  assert(implicit === expectedImplicit, `${skill}: implicit invocation policy must be ${expectedImplicit}`);

  for (const match of content.matchAll(/\$PMO_ROOT\/([A-Za-z0-9_./-]+)/g)) {
    assert(existsSync(join(pmoRoot, match[1])), `${skill}: missing PMO reference ${match[1]}`);
  }

  requireCoverage(skill, 'trigger', item => item.expect.skill === skill);
  requireCoverage(skill, 'not-trigger', item => item.expect.notSkill === skill);
  requireCoverage(skill, 'blank-context', item => item.expect.skill === skill);
  requireCoverage(skill, 'next', item => item.expect.skill === skill);
  if (skill !== 'mango-pmo-lifecycle') {
    requireCoverage(skill, 'boundary', item => item.expect.skill === skill);
  }
  requireCoverage(
    skill,
    'gate',
    item => item.expect.skill === skill
      && item.id.includes('claim-only')
      && item.expect.action !== 'NEXT',
  );
}

assert(
  cases.some(item => item.id === 'engineering-non-main-reuse'
    && item.expect.action === 'REUSE_CURRENT_WORKTREE'
    && item.expect.forbid?.includes('new worktree')),
  'missing non-main current-worktree reuse eval',
);
assert(
  cases.some(item => item.id === 'lifecycle-l0-lightweight-route'
    && item.expect.action === 'NEXT'
    && item.expect.forbid?.includes('fabricated BRD')),
  'missing L0 lightweight lifecycle eval',
);
assert(
  cases.some(item => item.id === 'lifecycle-l2-standard-single-record'
    && item.expect.action === 'NEXT'
    && item.expect.requiredAssertions?.some(assertion => assertion.includes('exactly one standard delivery record'))),
  'missing L2 STANDARD single-record eval',
);
assert(
  cases.some(item => item.id === 'lifecycle-cross-tenant-measures-ask'
    && item.expect.action === 'SELECT_FULL'
    && item.expect.requiredAssertions?.some(assertion => assertion.includes('FULL'))),
  'missing cross-tenant FULL-mode eval',
);
assert(
  cases.some(item => item.id === 'lifecycle-l3-governance-no-four-docs'
    && item.expect.action === 'SELECT_FULL'
    && item.expect.requiredAssertions?.some(assertion => assertion.includes('without fabricated product documents'))),
  'missing L3 FULL governance eval',
);
assert(
  cases.some(item => item.id === 'technical-same-requirement-different-solution-risk'
    && item.expect.action === 'COMPARE_SOLUTION_RISK'
    && item.expect.requiredAssertions?.length >= 3),
  'missing same requirement with different solution risk eval',
);
assert(
  cases.some(item => item.id === 'engineering-one-line-tenant-fix-no-downgrade'
    && item.expect.action === 'STOP'
    && item.expect.forbid?.includes('L1')),
  'missing one-line high-impact no-downgrade eval',
);
assert(
  cases.some(item => item.id === 'qa-backend-l3-no-forced-ui'
    && item.expect.action === 'SELECT_MINIMUM_SUFFICIENT'),
  'missing backend L3 no-forced-UI eval',
);
assert(
  cases.some(item => item.id === 'qa-unit-insufficient-for-tenant-transaction'
    && item.expect.action === 'STOP'),
  'missing tenant/transaction unit-insufficient eval',
);

const ruleIndex = JSON.parse(readFileSync(join(pmoRoot, 'rules/index.json'), 'utf8'));
const releaseBundle = ruleIndex.bundles?.releaseArtifacts;
assert(releaseBundle?.include?.includes('process.releaseArtifacts'), 'release artifact routing bundle is missing');
for (const keyword of ['CLI', 'starter', '模板', 'PMO', 'Skill']) {
  assert(
    releaseBundle.keywords?.some(item => item.includes(keyword)),
    `release artifact routing is missing ${keyword} keyword coverage`,
  );
}
assert(
  cases.some(item => item.id === 'issue-register-current-fix-not-trigger'
    && item.expect.notSkill === 'mango-issue-register'),
  'missing current-task fix versus Issue registration boundary eval',
);
assert(
  cases.some(item => item.id === 'release-version-matrix-stop'
    && item.expect.skill === 'mango-release'
    && item.expect.action === 'STOP'),
  'missing incompatible release version matrix eval',
);
assert(
  cases.some(item => item.id === 'engineering-service-registration-stop'
    && item.expect.skill === 'mango-engineering'
    && item.expect.action === 'STOP'
    && item.expect.requiredAssertions?.length >= 5),
  'missing unmanaged business Service eval',
);
assert(
  cases.some(item => item.id === 'engineering-framework-service-registration'
    && item.expect.action === 'USE_CONDITIONAL_BEAN'
    && item.expect.requiredAssertions?.length >= 5),
  'missing conditional framework Service registration eval',
);
assert(
  cases.some(item => item.id === 'engineering-pure-java-helper'
    && item.expect.action === 'ALLOW_PLAIN_HELPER'
    && item.expect.requiredAssertions?.length >= 4),
  'missing pure Java helper eval',
);
assert(
  cases.some(item => item.id === 'engineering-unregistered-cross-cut-stop'
    && item.expect.action === 'STOP'
    && item.expect.requiredAssertions?.length >= 4),
  'missing unregistered cross-cutting and static Service state eval',
);
assert(
  cases.some(item => item.id === 'engineering-cross-module-debt-transfer-stop'
    && item.expect.action === 'STOP'
    && item.expect.requiredAssertions?.length >= 4),
  'missing cross-module architecture debt transfer eval',
);
assert(
  cases.some(item => item.id === 'release-manifest-pending-stop'
    && item.expect.skill === 'mango-release'
    && item.expect.action === 'STOP'
    && item.expect.requiredAssertions?.length >= 3),
  'missing incomplete release manifest eval',
);
assert(
  cases.some(item => item.id === 'release-single-owner-policy-drift-stop'
    && item.expect.action === 'STOP'
    && item.expect.requiredAssertions?.length >= 3),
  'missing single-owner release policy drift eval',
);
assert(
  cases.some(item => item.id === 'pr-review-single-owner-policy'
    && item.expect.action === 'USE_SINGLE_OWNER_POLICY'
    && item.expect.requiredAssertions?.length >= 3),
  'missing single-owner PR review eval',
);
assert(
  cases.some(item => item.id === 'release-next'
    && item.expect.action === 'NEXT'
    && item.expect.requiredAssertions?.length >= 5),
  'missing complete auditable release manifest eval',
);

process.stdout.write(`Checked ${skills.length} Skills and ${cases.length} eval cases.\n`);

function requireCoverage(skill, kind, predicate) {
  assert(cases.some(item => item.kind === kind && predicate(item)), `${skill}: missing ${kind} eval`);
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}
