export const METRICS = {
  eslint: ['fatal', 'errors', 'warnings'],
  prettier: ['files'],
  stylelint: ['parseErrors', 'errors', 'warnings'],
  typecheck: ['failedWorkspaces', 'diagnostics'],
};

export function compareMetrics(tool, current, baseline, strict = false) {
  const failures = [];
  for (const metric of METRICS[tool] || []) {
    const actual = Number(current[metric] || 0);
    const allowed = strict ? 0 : Number(baseline?.[metric] || 0);
    if (actual > allowed) failures.push({ metric, actual, allowed });
  }
  return failures;
}

