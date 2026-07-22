const PR_COMMON_COMMANDS = [
  ['pnpm', ['quality:inventory']],
  ['pnpm', ['admin:styles:check']],
  ['pnpm', ['quality:versions']],
  ['pnpm', ['check:boundaries']],
  ['pnpm', ['component-contracts:check']],
  ['pnpm', ['e2e-selectors:check']],
  ['pnpm', ['runtime-config:check']],
  ['pnpm', ['typecheck']],
  ['pnpm', ['quality:gate:test']],
];

function filters(names) {
  return names.flatMap((name) => ['--filter', name]);
}

function selectedTargets(records, selection, script) {
  const selected = new Set(selection.selected);
  return records
    .filter((item) => selected.has(item.name) && typeof item.scripts[script] === 'string')
    .map((item) => item.name);
}

function appendWorkspaceCommands(commands, records, selection) {
  const buildTargets = selectedTargets(records, selection, 'build');
  const testTargets = selectedTargets(records, selection, 'test');
  if (buildTargets.length > 0) commands.push(['pnpm', [...filters(buildTargets), '-r', 'build']]);
  if (testTargets.length > 0) commands.push(['pnpm', [...filters(testTargets), '-r', 'test']]);
}

function deepPlan(records, selection) {
  if (selection.mode === 'full') return [['pnpm', ['run', 'check:full']]];
  if (selection.mode === 'none') return [['pnpm', ['quality:versions']]];

  const commands = [
    ['pnpm', ['quality:inventory']],
    ['pnpm', ['admin:styles:check']],
    ['pnpm', ['check:static']],
    ['pnpm', ['quality:gate:test']],
  ];
  appendWorkspaceCommands(commands, records, selection);
  if (selection.publishableChanged.length > 0) {
    commands.push(['pnpm', ['package-exports:check']]);
    commands.push(['pnpm', ['package-consumer:typecheck']]);
  }
  return commands;
}

function pullRequestPlan(records, selection) {
  if (selection.mode === 'none') return [['pnpm', ['quality:versions']]];

  const commands = PR_COMMON_COMMANDS.map(([command, arguments_]) => [command, [...arguments_]]);
  appendWorkspaceCommands(commands, records, selection);
  commands.push(['pnpm', ['package-exports:check']]);
  if (selection.mode === 'affected' && selection.publishableChanged.length > 0) {
    commands.push(['pnpm', ['package-consumer:typecheck']]);
  }
  return commands;
}

export function createQualityCommandPlan(records, selection, profile = 'deep') {
  if (profile === 'deep') return deepPlan(records, selection);
  if (profile === 'pr') return pullRequestPlan(records, selection);
  throw new Error(`unsupported frontend quality profile: ${profile}`);
}
