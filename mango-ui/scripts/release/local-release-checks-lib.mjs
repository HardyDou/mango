export function releaseScopeCheckCommands({ releaseOnly, base, head }) {
  if (releaseOnly) {
    return [{ executable: 'pnpm', args: ['release:pr-check'] }];
  }
  return [
    {
      executable: 'pnpm',
      args: ['release:change-check', '--', `--base=${base}`, `--head=${head}`],
    },
    { executable: 'pnpm', args: ['release:plan:check'] },
  ];
}
