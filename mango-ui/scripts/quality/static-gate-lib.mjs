import { createHash } from 'node:crypto';
import path from 'node:path';

export const METRICS = {
  eslint: ['fatal', 'errors', 'warnings'],
  prettier: ['files'],
  stylelint: ['parseErrors', 'errors', 'warnings'],
  typecheck: ['failedWorkspaces', 'diagnostics'],
};

function normalizeText(value) {
  return String(value || '')
    .trim()
    .replace(/\s+/gu, ' ');
}

function normalizeDiagnosticText(value, uiRoot) {
  let normalized = normalizeText(value);
  const roots = new Set([uiRoot, uiRoot.split(path.sep).join('/'), uiRoot.split(path.sep).join('\\')]);
  for (const root of [...roots].filter(Boolean).sort((left, right) => right.length - left.length)) {
    normalized = normalized.split(root).join('<ui-root>');
  }
  return normalized;
}

function relativeFile(uiRoot, value) {
  const file = String(value || '');
  const relative = path.isAbsolute(file) ? path.relative(uiRoot, file) : file;
  return relative.split(path.sep).join('/');
}

function identity(parts) {
  return parts.map(normalizeText).join('|');
}

export function collectDiagnosticIdentities(tool, raw, uiRoot, readFile = () => '') {
  if (tool === 'eslint') {
    return raw
      .flatMap((file) =>
        (file.messages || []).map((message) =>
          identity([
            relativeFile(uiRoot, file.filePath),
            message.severity === 2 ? 'error' : 'warning',
            message.fatal ? 'fatal' : message.ruleId || 'parser',
            message.messageId || '',
            normalizeDiagnosticText(message.message, uiRoot),
          ]),
        ),
      )
      .sort();
  }
  if (tool === 'stylelint') {
    return raw
      .flatMap((file) => [
        ...(file.parseErrors || []).map((issue) =>
          identity([
            relativeFile(uiRoot, file.source),
            'parse-error',
            issue.rule || 'parser',
            normalizeDiagnosticText(issue.text || issue.message, uiRoot),
          ]),
        ),
        ...(file.warnings || []).map((issue) =>
          identity([
            relativeFile(uiRoot, file.source),
            issue.severity || 'warning',
            issue.rule || 'unknown',
            normalizeDiagnosticText(issue.text, uiRoot),
          ]),
        ),
      ])
      .sort();
  }
  if (tool === 'typecheck') {
    return (raw.results || [])
      .flatMap((result) =>
        (result.diagnostics || []).map((diagnostic) =>
          identity([
            diagnostic.workspace,
            relativeFile(uiRoot, diagnostic.file),
            diagnostic.severity,
            diagnostic.code,
            normalizeDiagnosticText(diagnostic.message, uiRoot),
          ]),
        ),
      )
      .sort();
  }
  if (tool === 'prettier') {
    return raw
      .map((file) => {
        const relative = relativeFile(uiRoot, file);
        const sha256 = createHash('sha256')
          .update(readFile(path.resolve(uiRoot, relative)))
          .digest('hex');
        return `${relative}|${sha256}`;
      })
      .sort();
  }
  throw new Error(`unsupported static identity tool: ${tool}`);
}

export function compareIdentityMultisets(current = [], baseline = [], strict = false) {
  const allowed = strict ? new Map() : countIdentities(baseline);
  const seen = new Map();
  const additions = [];
  for (const value of current) {
    const count = (seen.get(value) || 0) + 1;
    seen.set(value, count);
    if (count > (allowed.get(value) || 0)) additions.push(value);
  }
  return additions;
}

function countIdentities(values) {
  const counts = new Map();
  for (const value of values) counts.set(value, (counts.get(value) || 0) + 1);
  return counts;
}

export function compareMetrics(tool, current, baseline, strict = false) {
  const failures = [];
  for (const metric of METRICS[tool] || []) {
    const actual = Number(current[metric] || 0);
    const allowed = strict ? 0 : Number(baseline?.[metric] || 0);
    if (actual > allowed) failures.push({ metric, actual, allowed });
  }
  return failures;
}

export function assertToolExecution(result, allowedStatuses, name) {
  if (result.error) throw new Error(`${name} failed to start: ${result.error.message}`);
  if (result.signal) throw new Error(`${name} terminated by signal ${result.signal}`);
  if (!allowedStatuses.includes(result.status)) {
    throw new Error(
      `${name} exited with ${result.status ?? 'unknown'}: ${result.stderr || result.stdout || 'no output'}`,
    );
  }
}
