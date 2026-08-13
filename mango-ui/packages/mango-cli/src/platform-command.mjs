import { accessSync, constants, existsSync } from 'node:fs';
import { posix, win32 } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';

const WINDOWS_COMMAND_SHIMS = new Set(['mango', 'mvn', 'npm', 'npx', 'pnpm', 'yarn']);
const DEFAULT_WINDOWS_PATHEXT = ['.COM', '.EXE', '.BAT', '.CMD'];

export function commandForPlatform(command, platform = process.platform) {
  const value = String(command || '');
  if (platform === 'win32' && WINDOWS_COMMAND_SHIMS.has(value.toLowerCase())) {
    return `${value}.cmd`;
  }
  return value;
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

export function commandPathCandidates(command, platform = process.platform, env = process.env) {
  const executable = commandForPlatform(command, platform);
  if (/[\\/]/u.test(executable)) {
    return [executable];
  }
  const pathApi = platform === 'win32' ? win32 : posix;
  const pathEntries = String(env.PATH || '').split(pathApi.delimiter);
  const extensions =
    platform === 'win32' && !/\.[^\\/.]+$/u.test(executable)
      ? String(env.PATHEXT || DEFAULT_WINDOWS_PATHEXT.join(';'))
          .split(';')
          .filter(Boolean)
      : [''];
  return pathEntries.flatMap((directory) =>
    extensions.map((extension) => pathApi.join(directory, `${executable}${extension}`)),
  );
}

export function isCommandAvailable(command, platform = process.platform, env = process.env) {
  return commandPathCandidates(command, platform, env).some((candidate) => {
    if (platform === 'win32') {
      return existsSync(candidate);
    }
    try {
      accessSync(candidate, constants.X_OK);
      return true;
    } catch {
      return false;
    }
  });
}

export function spawnCommand(command, args, options = {}) {
  const resolved = resolveCommandForPlatform(command);
  return spawn(resolved.command, args, {
    ...options,
    shell: resolved.shell || options.shell,
  });
}

export function spawnCommandSync(command, args, options = {}) {
  const resolved = resolveCommandForPlatform(command);
  return spawnSync(resolved.command, args, {
    ...options,
    shell: resolved.shell || options.shell,
  });
}
