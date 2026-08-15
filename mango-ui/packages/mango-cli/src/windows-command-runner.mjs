#!/usr/bin/env node
import { spawnCommand } from './platform-command.mjs';

const command = process.argv[2];
let args;
try {
  args = JSON.parse(process.argv[3] || '[]');
} catch (error) {
  process.stderr.write(`Invalid command arguments: ${error.message}\n`);
  process.exit(1);
}

if (!command || !Array.isArray(args) || args.some((argument) => typeof argument !== 'string')) {
  process.stderr.write('Command and string argument array are required.\n');
  process.exit(1);
}

const child = spawnCommand(command, args, {
  env: process.env,
  stdio: 'inherit',
  windowsHide: true,
});
child.once('error', (error) => {
  process.stderr.write(`${error.message}\n`);
  process.exit(1);
});
child.once('exit', (code) => process.exit(code ?? 1));
