import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const RISK_LEVELS = ['L0', 'L1', 'L2', 'L3'];
export const DELIVERY_MODES = ['SIMPLE', 'STANDARD', 'FULL'];
const RISK_TO_MODE = new Map([['L0', 'SIMPLE'], ['L1', 'SIMPLE'], ['L2', 'STANDARD'], ['L3', 'FULL']]);
const DELIVERY_ASSURANCE_CONTRACT = JSON.parse(fs.readFileSync(
  path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../contracts/delivery-assurance.json'),
  'utf8',
));
const ASSURANCE_VALUES = new Map(DELIVERY_ASSURANCE_CONTRACT.measures.map(measure => [measure.id, measure.allowedValues]));
const DOWNWARD_POLICY = DELIVERY_ASSURANCE_CONTRACT.downwardModeOverride;
const NON_DOWNGRADABLE_FACTS = new Map(DOWNWARD_POLICY.nonDowngradableFacts.map(fact => [fact.id, fact.keywords]));

const PLACEHOLDERS = new Set([
  '',
  '-',
  'n/a',
  'na',
  'none / list each skipped type and reason',
  'confirmed / not applicable - evidence',
  'm01=create; m09=enable',
  'm01 - evidence; m09 - evidence',
  'tbd',
  'todo',
]);

function sectionText(markdown, heading) {
  const pattern = new RegExp(`^##[ \\t]+${escapeRegExp(heading)}[ \\t]*$`, 'm');
  const match = pattern.exec(markdown);
  if (!match) return '';
  const rest = markdown.slice(match.index + match[0].length);
  const next = rest.search(/^##[ \t]+/m);
  return (next >= 0 ? rest.slice(0, next) : rest).trim();
}

function fieldValue(section, label) {
  const pattern = new RegExp(`^-[ \\t]+${escapeRegExp(label)}:[ \\t]*(.*)$`, 'm');
  return pattern.exec(section)?.[1]?.trim() ?? '';
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function isPlaceholder(value) {
  return PLACEHOLDERS.has(value.trim().toLowerCase());
}

function parseAssessedLevel(value) {
  const match = /^(L[0-3])\s*[-:：]\s*(.+)$/u.exec(value);
  if (!match || isPlaceholder(match[2])) return null;
  return { level: match[1], evidence: match[2].trim() };
}

function parseMeasureEntries(value, separatorPattern, entryPattern) {
  if (isPlaceholder(value) || /^none$/iu.test(value.trim())) return new Map();
  const entries = new Map();
  for (const item of value.split(separatorPattern).map(entry => entry.trim()).filter(Boolean)) {
    const match = entryPattern.exec(item);
    if (!match || isPlaceholder(match[2])) return null;
    const id = match[1].toUpperCase();
    if (entries.has(id)) return null;
    entries.set(id, match[2].trim());
  }
  return entries;
}

function parseSelections(value) {
  const entries = parseMeasureEntries(value, /[;；,，]/u, /^(M(?:0[1-9]|1[0-6]))\s*=\s*([A-Z_]+)$/u);
  if (!entries) return null;
  for (const [id, selectedValue] of entries) {
    if (!ASSURANCE_VALUES.get(id)?.includes(selectedValue)) return null;
  }
  return entries;
}

function parseEvidence(value) {
  return parseMeasureEntries(value, /[;；]/u, /^(M(?:0[1-9]|1[0-6]))\s*[-:：]\s*(.+)$/u);
}

function parseNonDowngradableFacts(value) {
  if (/^none$/iu.test(value.trim())) return [];
  const facts = value.split(/[;；,，]/u).map(item => item.trim().toUpperCase()).filter(Boolean);
  if (facts.length === 0 || facts.some(fact => !NON_DOWNGRADABLE_FACTS.has(fact))) return null;
  return [...new Set(facts)];
}

function detectNonDowngradableFacts(...evidenceParts) {
  const evidence = evidenceParts.filter(Boolean).join(' ').toLowerCase();
  return [...NON_DOWNGRADABLE_FACTS.entries()]
    .filter(([, keywords]) => keywords.some(keyword => evidence.includes(keyword.toLowerCase())))
    .map(([id]) => id);
}

function namesAiAsConfirmer(value) {
  return /(?:\bai\b|agent|codex|claude|gpt|模型|机器人)/iu.test(value);
}

function hasHumanActor(value) {
  return /(?:owner|maintainer|human|user|负责人|维护者|用户)/iu.test(value);
}

export function validateRiskVerification(markdown) {
  const failures = [];
  const section = sectionText(markdown, 'Risk / Verification');
  if (!section) {
    return { failures: ['PR body is missing section: ## Risk / Verification'] };
  }

  const requirement = parseAssessedLevel(fieldValue(section, 'Requirement impact'));
  const solution = parseAssessedLevel(fieldValue(section, 'Solution risk'));
  const finalRisk = fieldValue(section, 'Final risk').toUpperCase();
  const deliveryMode = fieldValue(section, 'Delivery mode').toUpperCase();
  const workspaceDecision = fieldValue(section, 'Workspace decision').toUpperCase();
  const declaredNonDowngradableFacts = parseNonDowngradableFacts(fieldValue(section, 'Non-downgradable facts'));
  const baseline = fieldValue(section, 'Assurance baseline');
  const selections = parseSelections(fieldValue(section, 'Assurance selections'));
  const assuranceReasoning = fieldValue(section, 'Assurance reasoning');
  const evidence = parseEvidence(fieldValue(section, 'Assurance evidence'));
  const residualRisks = fieldValue(section, 'Residual risks');
  const releaseOnly = /^NOT_APPLICABLE\s*[-:：]\s*.+release/iu.test(baseline);

  if (!requirement) failures.push('"Requirement impact" must use "L0-L3 - concrete impact facts"');
  if (!solution) failures.push('"Solution risk" must use "L0-L3 - concrete implementation and recovery facts"');
  if (!RISK_LEVELS.includes(finalRisk)) failures.push('"Final risk" must be one of L0, L1, L2, L3');
  if (!releaseOnly && !DELIVERY_MODES.includes(deliveryMode)) failures.push('"Delivery mode" must be one of SIMPLE, STANDARD, FULL');
  if (releaseOnly && deliveryMode !== 'NOT_APPLICABLE') failures.push('release-only PR must set "Delivery mode" to NOT_APPLICABLE');
  if (!['CREATE', 'REUSE', 'MAIN_EXCEPTION'].includes(workspaceDecision)) failures.push('"Workspace decision" must be CREATE, REUSE, or MAIN_EXCEPTION');
  if (!declaredNonDowngradableFacts) failures.push('"Non-downgradable facts" must be None or a comma-separated contract fact list');
  if (workspaceDecision === 'MAIN_EXCEPTION'
    && (!baseline.includes(DOWNWARD_POLICY.mainExceptionToken) || namesAiAsConfirmer(baseline))) {
    failures.push(`MAIN_EXCEPTION requires human evidence containing ${DOWNWARD_POLICY.mainExceptionToken}`);
  }

  if (requirement && solution && RISK_LEVELS.includes(finalRisk)) {
    const expected = RISK_LEVELS[Math.max(
      RISK_LEVELS.indexOf(requirement.level),
      RISK_LEVELS.indexOf(solution.level),
    )];
    if (finalRisk !== expected) {
      failures.push(`"Final risk" must equal max(requirement impact, solution risk): expected ${expected}, got ${finalRisk}`);
    }
    const expectedMode = RISK_TO_MODE.get(finalRisk);
    const isDownward = DELIVERY_MODES.includes(deliveryMode)
      && DELIVERY_MODES.indexOf(deliveryMode) < DELIVERY_MODES.indexOf(expectedMode);
    const detectedFacts = detectNonDowngradableFacts(requirement.evidence, solution.evidence);
    const undeclaredFacts = detectedFacts.filter(fact => !declaredNonDowngradableFacts?.includes(fact));
    if (!releaseOnly && undeclaredFacts.length > 0) {
      failures.push(`"Non-downgradable facts" omits detected facts: ${undeclaredFacts.join(', ')}`);
    }
    const blockingFacts = [...new Set([...(declaredNonDowngradableFacts || []), ...detectedFacts])];
    const hasDownwardException = baseline.includes(DOWNWARD_POLICY.confirmationToken)
      && !namesAiAsConfirmer(baseline)
      && /(?:delivery mode|交付模式)/iu.test(residualRisks);
    if (!releaseOnly && isDownward && blockingFacts.length > 0) {
      failures.push(`"Delivery mode" cannot be downgraded because of: ${blockingFacts.join(', ')}`);
    } else if (!releaseOnly && isDownward && !hasDownwardException) {
      failures.push(`"Delivery mode" must not be lower than final risk ${finalRisk} without human ${DOWNWARD_POLICY.confirmationToken}: minimum ${expectedMode}, got ${deliveryMode}`);
    }
  }

  if (!releaseOnly) {
    const resolvedBaseline = /^RESOLVED\s*[-:：]\s*.+/u.test(baseline);
    const legacyConfirmedBaseline = DELIVERY_ASSURANCE_CONTRACT.legacyBaselinePrefixes.some(prefix =>
      new RegExp(`^${escapeRegExp(prefix)}\\s*[-:：]\\s*.+`, 'u').test(baseline),
    ) && hasHumanActor(baseline) && !namesAiAsConfirmer(baseline);
    if (!resolvedBaseline && !legacyConfirmedBaseline) {
      failures.push('"Assurance baseline" must use "RESOLVED - policy facts and exception evidence" or a human-confirmed legacy baseline');
    }
    if (!selections || selections.size === 0) {
      failures.push('"Assurance selections" must list exact resolved M01-M16 values, for example M01=CREATE; M09=ENABLE');
    }
    if (isPlaceholder(assuranceReasoning)) failures.push('"Assurance reasoning" must explain why the confirmed set protects the task goal');
    if (!evidence) failures.push('"Assurance evidence" must use "Mxx - concrete evidence" entries separated by semicolons');
    if (isPlaceholder(residualRisks)) failures.push('"Residual risks" must be concrete or None');

    if (selections && evidence) {
      for (const [id, selectedValue] of selections) {
        const enabled = ['CREATE', 'REUSE', 'REBUILD', 'ENABLE'].includes(selectedValue);
        if (enabled && !evidence.has(id)) failures.push(`"Assurance evidence" must provide evidence for enabled ${id}`);
        if (!enabled && selectedValue !== 'DO_NOT_CREATE' && !new RegExp(`\\b${id}\\b`, 'u').test(residualRisks)) {
          failures.push(`"Residual risks" must explain the confirmed non-enabled value for ${id}`);
        }
      }
      for (const id of evidence.keys()) {
        if (!selections.has(id)) failures.push(`Assurance evidence references unconfirmed measure ${id}`);
      }
    }
  } else {
    if (selections && selections.size > 0) failures.push('release-only PR must not include delivery-assurance selections');
    if (fieldValue(section, 'Assurance selections').trim().toLowerCase() !== 'none') failures.push('release-only PR must set "Assurance selections" to None');
  }

  return {
    failures,
    assessment: requirement && solution && RISK_LEVELS.includes(finalRisk)
      ? {
          requirementImpact: requirement,
          solutionRisk: solution,
          finalRisk,
          deliveryMode,
          workspaceDecision,
          nonDowngradableFacts: declaredNonDowngradableFacts || [],
          assuranceBaseline: baseline,
          assuranceSelections: selections ? Object.fromEntries(selections) : {},
          assuranceEvidence: evidence ? Object.fromEntries(evidence) : {},
          residualRisks,
        }
      : null,
  };
}

export function runRiskVerificationCli(argv = process.argv.slice(2)) {
  const bodyIndex = argv.indexOf('--body');
  const bodyPath = bodyIndex >= 0
    ? argv[bodyIndex + 1]
    : process.env.PR_BODY_FILE || '.pr-body.md';
  if (!bodyPath) {
    process.stderr.write('Missing PR body path. Use --body <file> or PR_BODY_FILE.\n');
    return 1;
  }
  const resolved = path.resolve(bodyPath);
  if (!fs.existsSync(resolved)) {
    process.stderr.write(`PR body file does not exist: ${bodyPath}\n`);
    return 1;
  }
  const result = validateRiskVerification(fs.readFileSync(resolved, 'utf8'));
  if (result.failures.length === 0) {
    process.stdout.write(`Risk verification contract passed: ${result.assessment.finalRisk}.\n`);
    return 0;
  }
  for (const failure of result.failures) process.stderr.write(`[FAIL] ${failure}\n`);
  return 1;
}

const invokedPath = process.argv[1] ? path.resolve(process.argv[1]) : '';
if (invokedPath === fileURLToPath(import.meta.url)) {
  process.exitCode = runRiskVerificationCli();
}
