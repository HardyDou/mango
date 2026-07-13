import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const RISK_LEVELS = ['L0', 'L1', 'L2', 'L3'];
export const VERIFICATION_TYPES = ['STATIC', 'UNIT', 'API', 'UI'];

const PLACEHOLDERS = new Set([
  '',
  '-',
  'n/a',
  'na',
  'none / list each skipped type and reason',
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

function parseSelectedTypes(value) {
  if (isPlaceholder(value)) return [];
  return value.split(/[,，]/u).map(item => item.trim().toUpperCase()).filter(Boolean);
}

function parseSkippedTypes(value) {
  if (/^none$/iu.test(value.trim())) return new Map();
  const entries = new Map();
  for (const item of value.split(/[;；]/u).map(entry => entry.trim()).filter(Boolean)) {
    const match = /^(STATIC|UNIT|API|UI)\s*[-:：]\s*(.+)$/iu.exec(item);
    if (!match || isPlaceholder(match[2])) return null;
    entries.set(match[1].toUpperCase(), match[2].trim());
  }
  return entries;
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
  const selected = parseSelectedTypes(fieldValue(section, 'Selected verification'));
  const sufficientReason = fieldValue(section, 'Why sufficient');
  const skipped = parseSkippedTypes(fieldValue(section, 'Skipped verification'));

  if (!requirement) failures.push('"Requirement impact" must use "L0-L3 - concrete impact facts"');
  if (!solution) failures.push('"Solution risk" must use "L0-L3 - concrete implementation and recovery facts"');
  if (!RISK_LEVELS.includes(finalRisk)) failures.push('"Final risk" must be one of L0, L1, L2, L3');

  if (requirement && solution && RISK_LEVELS.includes(finalRisk)) {
    const expected = RISK_LEVELS[Math.max(
      RISK_LEVELS.indexOf(requirement.level),
      RISK_LEVELS.indexOf(solution.level),
    )];
    if (finalRisk !== expected) {
      failures.push(`"Final risk" must equal max(requirement impact, solution risk): expected ${expected}, got ${finalRisk}`);
    }
  }

  const uniqueSelected = new Set(selected);
  if (selected.length === 0) failures.push('"Selected verification" must select at least one of STATIC, UNIT, API, UI');
  if (uniqueSelected.size !== selected.length) failures.push('"Selected verification" must not contain duplicate types');
  for (const type of uniqueSelected) {
    if (!VERIFICATION_TYPES.includes(type)) failures.push(`unsupported verification type: ${type}`);
  }
  if (isPlaceholder(sufficientReason)) failures.push('"Why sufficient" must explain how the selected checks prove the acceptance outcome');
  if (skipped === null) {
    failures.push('"Skipped verification" must use "TYPE - concrete reason" entries separated by semicolons, or "None"');
  } else {
    const expectedSkipped = VERIFICATION_TYPES.filter(type => !uniqueSelected.has(type));
    const actualSkipped = [...skipped.keys()];
    for (const type of expectedSkipped) {
      if (!skipped.has(type)) failures.push(`"Skipped verification" must explain why ${type} is not needed`);
    }
    for (const type of actualSkipped) {
      if (uniqueSelected.has(type)) failures.push(`${type} cannot be both selected and skipped`);
      if (!VERIFICATION_TYPES.includes(type)) failures.push(`unsupported skipped verification type: ${type}`);
    }
    if (expectedSkipped.length === 0 && fieldValue(section, 'Skipped verification').trim().toLowerCase() !== 'none') {
      failures.push('"Skipped verification" must be "None" when all verification types are selected');
    }
  }

  return {
    failures,
    assessment: requirement && solution && RISK_LEVELS.includes(finalRisk)
      ? {
          requirementImpact: requirement,
          solutionRisk: solution,
          finalRisk,
          selectedVerification: [...uniqueSelected],
          skippedVerification: skipped ? Object.fromEntries(skipped) : {},
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
