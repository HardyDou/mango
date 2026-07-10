#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { analyzeArtifacts, computeFilesDigest, loadArtifacts } from './lib/quality-analyzer.mjs';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');

function parseArgs(argv) {
  const values = { base: 'origin/main', head: 'HEAD', out: '.runtime/pmo/quality-contract.json', capability: '', acceptance: [] };
  const allowed = new Set(['base', 'head', 'out', 'capability', 'acceptance']);
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (!arg.startsWith('--')) throw new Error(`Unexpected argument: ${arg}`);
    const key = arg.slice(2);
    if (!allowed.has(key)) throw new Error(`Unknown option: ${arg}`);
    const value = argv[index + 1];
    if (value === undefined || value.startsWith('--')) throw new Error(`Missing value: ${arg}`);
    if (key === 'acceptance') values.acceptance.push(value);
    else values[key] = value;
    index += 1;
  }
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(values.capability)) {
    throw new Error('--capability is required and must use kebab-case');
  }
  if (values.acceptance.length === 0 || values.acceptance.some((item) => item.trim().length < 8)) {
    throw new Error('At least one observable --acceptance (8+ characters) is required');
  }
  return values;
}

function git(...args) {
  return execFileSync('git', args, { cwd: repoRoot, encoding: 'utf8' }).trim();
}

function changedFiles(base, head) {
  const mergeBase = git('merge-base', base, head);
  const committed = git('diff', '--name-only', `${mergeBase}...${head}`, '--').split('\n');
  const working = head === 'HEAD' ? git('diff', '--name-only', 'HEAD', '--').split('\n') : [];
  const untracked = head === 'HEAD' ? git('ls-files', '--others', '--exclude-standard').split('\n') : [];
  return [...new Set([...committed, ...working, ...untracked].map((item) => item.trim()).filter(Boolean))]
    .filter((file) => fs.existsSync(path.join(repoRoot, file)));
}

function classify(artifacts) {
  let risk = 'R0';
  const reasons = new Set();
  for (const artifact of artifacts) {
    const file = artifact.path;
    const text = artifact.content;
    if (/mango-ui\/.*\/(?:views|pages)\/.*\.vue$|mango-ui\/.*\/e2e\/.*\.spec\./.test(file)) {
      risk = maxRisk(risk, 'R3');
      reasons.add('user-flow');
    }
    if (/(?:Controller|ServiceImpl|Mapper)\.java$|\/db\/migration\/|\/(?:security|authorization|event)\//.test(file)) {
      risk = maxRisk(risk, 'R2');
      reasons.add('application-or-persistence-boundary');
    }
    if (/\.(?:java|ts|tsx|vue|mjs)$/.test(file) && /\b(?:if|switch|catch|throw|transaction|save|update|delete|insert)\b|\?[^:]+:/.test(text)) {
      risk = maxRisk(risk, 'R1');
      reasons.add('branch-or-error-handling');
    }
    if (/pom\.xml$/.test(file)) {
      risk = maxRisk(risk, 'R2');
      reasons.add('module-dependency');
    }
  }
  if (reasons.size === 0) reasons.add('mechanical-change');
  return { risk, reasons: [...reasons] };
}

function maxRisk(left, right) {
  const order = ['R0', 'R1', 'R2', 'R3'];
  return order[Math.max(order.indexOf(left), order.indexOf(right))];
}

function obligations(risk) {
  if (risk === 'R0') return ['STATIC_REVIEW'];
  if (risk === 'R1') return ['STATIC_REVIEW', 'UNIT'];
  if (risk === 'R2') return ['STATIC_REVIEW', 'UNIT', 'API'];
  return ['STATIC_REVIEW', 'UNIT', 'API', 'UI'];
}

function proofPath(risk, artifacts) {
  if (risk === 'R0') return [];
  const names = artifacts
    .filter((artifact) => /\.(?:java|ts|tsx|vue|mjs)$/.test(artifact.path))
    .map((artifact) => path.basename(artifact.path).replace(/\.[^.]+$/, ''))
    .filter((name) => !/(?:Test|spec)$/.test(name));
  return [...new Set(['change-entry', ...names, 'observable-result'])];
}

try {
  const args = parseArgs(process.argv.slice(2));
  const output = path.resolve(repoRoot, args.out);
  const outputRelative = path.relative(repoRoot, output).replaceAll('\\', '/');
  const files = changedFiles(args.base, args.head).filter((file) =>
    file !== outputRelative && !file.startsWith('mango-docs/evidence/test-baseline/'));
  if (files.length === 0) throw new Error('No changed files found for quality contract');
  const artifacts = loadArtifacts(repoRoot, files);
  const classification = classify(artifacts);
  const contract = {
    schemaVersion: 1,
    change: {
      base: git('merge-base', args.base, args.head),
      head: git('rev-parse', args.head),
      filesDigest: computeFilesDigest(artifacts),
      files
    },
    capabilities: [{
      id: args.capability,
      acceptance: args.acceptance,
      risk: classification.risk,
      riskReasons: classification.reasons,
      obligations: obligations(classification.risk),
      protectedProofPath: proofPath(classification.risk, artifacts),
      allowedExternalDoubles: []
    }]
  };
  const issues = analyzeArtifacts({ artifacts, contract });
  const contractIssues = issues.filter((issue) => issue.file === 'quality-contract.json');
  if (contractIssues.length > 0) throw new Error(contractIssues.map((issue) => `${issue.rule}: ${issue.message}`).join('; '));
  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, `${JSON.stringify(contract, null, 2)}\n`);
  console.log(`Quality contract generated: ${path.relative(repoRoot, output)}`);
  console.log(`Risk: ${classification.risk}; obligations: ${obligations(classification.risk).join(', ')}`);
} catch (error) {
  console.error(`Quality contract generation failed: ${error.message}`);
  process.exit(1);
}
