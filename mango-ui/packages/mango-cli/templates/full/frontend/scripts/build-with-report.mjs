import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const reportDir = resolve(process.cwd(), 'build-reports');
const commands = [
  { name: 'typecheck', args: ['run', 'typecheck'] },
  { name: 'vite-build', args: ['exec', 'vite', '--', 'build'] },
];

mkdirSync(reportDir, { recursive: true });

const outputs = [];
let exitCode = 0;

for (const command of commands) {
  const result = spawnSync('npm', command.args, {
    cwd: process.cwd(),
    encoding: 'utf8',
    env: { ...process.env, FORCE_COLOR: '0' },
    maxBuffer: 32 * 1024 * 1024,
  });
  const stdout = result.stdout || '';
  const stderr = result.stderr || '';
  outputs.push({ command: command.name, stdout, stderr, status: result.status ?? 1 });
  if (stdout) {
    process.stdout.write(stdout);
  }
  if (stderr) {
    process.stderr.write(stderr);
  }
  if (result.status !== 0) {
    exitCode = result.status ?? 1;
    break;
  }
}

const combinedOutput = outputs
  .flatMap((entry) => [entry.stdout, entry.stderr])
  .join('\n');
const warningLines = combinedOutput
  .split(/\r?\n/)
  .filter((line) => /warning|deprecated|chunks are larger|manualChunks/i.test(line));

writeFileSync(resolve(reportDir, 'frontend-build.log'), combinedOutput);
writeFileSync(resolve(reportDir, 'frontend-build-warnings.log'), `${warningLines.join('\n')}${warningLines.length ? '\n' : ''}`);
writeFileSync(resolve(reportDir, 'frontend-build-report.json'), `${JSON.stringify({
  generatedAt: new Date().toISOString(),
  status: exitCode,
  warningCount: warningLines.length,
  warnings: warningLines,
  commands: outputs.map(({ command, status }) => ({ command, status })),
}, null, 2)}\n`);

process.exit(exitCode);
