const WINDOWS_COMMANDS = new Set(['npm', 'pnpm', 'npx']);

export function commandForPlatform(command, platform = process.platform) {
  if (platform === 'win32' && WINDOWS_COMMANDS.has(command)) {
    return `${command}.cmd`;
  }
  return command;
}

export function shouldUseShellForCommand(command, platform = process.platform) {
  return platform === 'win32' && /\.cmd$/iu.test(command);
}

export function resolveCommandForPlatform(command, platform = process.platform) {
  const executable = commandForPlatform(command, platform);
  return {
    command: executable,
    shell: shouldUseShellForCommand(executable, platform),
  };
}
