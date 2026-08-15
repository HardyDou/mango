import { accessSync, constants, existsSync } from 'node:fs';
import { posix, win32 } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';

const WINDOWS_COMMAND_SHIMS = new Set(['mango', 'mvn', 'npm', 'npx', 'pnpm', 'yarn']);
const DEFAULT_WINDOWS_PATHEXT = ['.COM', '.EXE', '.BAT', '.CMD'];

function environmentValue(env, key) {
  const matchedKey = Object.keys(env).find((candidate) => candidate.toLowerCase() === key.toLowerCase());
  return matchedKey ? env[matchedKey] : '';
}

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

export function buildWindowsShellCommand(command, args = []) {
  return [command, ...args].map((value) => `"${String(value).replaceAll('"', '""')}"`).join(' ');
}

function resolveWindowsCommandShim(command, env) {
  return commandPathCandidates(command, 'win32', env).find((candidate) => existsSync(candidate)) || command;
}

export function commandPathCandidates(command, platform = process.platform, env = process.env) {
  const executable = commandForPlatform(command, platform);
  if (/[\\/]/u.test(executable)) {
    return [executable];
  }
  const pathApi = platform === 'win32' ? win32 : posix;
  const pathEntries = String(environmentValue(env, 'PATH')).split(pathApi.delimiter);
  const extensions =
    platform === 'win32' && !/\.[^\\/.]+$/u.test(executable)
      ? String(environmentValue(env, 'PATHEXT') || DEFAULT_WINDOWS_PATHEXT.join(';'))
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
  if (resolved.shell) {
    const executable = resolveWindowsCommandShim(resolved.command, options.env || process.env);
    return spawn(buildWindowsShellCommand(executable, args), {
      ...options,
      shell: true,
      windowsHide: options.windowsHide ?? true,
    });
  }
  return spawn(resolved.command, args, {
    ...options,
    shell: options.shell,
  });
}

export function spawnCommandSync(command, args, options = {}) {
  const resolved = resolveCommandForPlatform(command);
  if (resolved.shell) {
    const executable = resolveWindowsCommandShim(resolved.command, options.env || process.env);
    return spawnSync(buildWindowsShellCommand(executable, args), {
      ...options,
      shell: true,
      windowsHide: options.windowsHide ?? true,
    });
  }
  return spawnSync(resolved.command, args, {
    ...options,
    shell: options.shell,
  });
}
