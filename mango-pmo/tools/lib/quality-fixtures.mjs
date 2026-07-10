import fs from 'node:fs';
import path from 'node:path';
import { computeFilesDigest } from './quality-analyzer.mjs';

export function loadFixtureCases(file) {
  const payload = JSON.parse(fs.readFileSync(file, 'utf8'));
  if (payload.version !== 1 || !Array.isArray(payload.cases)) {
    throw new Error(`Unsupported fixture file: ${file}`);
  }
  return payload.cases;
}

export function fixtureArtifacts(item) {
  return item.files.map((file) => ({ path: file.path, content: file.content }));
}

export function fixtureContract(item) {
  if (!item.contractCapabilities) return null;
  const artifacts = fixtureArtifacts(item);
  return {
    schemaVersion: 1,
    change: {
      base: 'aaaaaaaa',
      head: 'bbbbbbbb',
      filesDigest: computeFilesDigest(artifacts),
      files: artifacts.map((artifact) => artifact.path)
    },
    capabilities: item.contractCapabilities
  };
}

export function materializeFixture(root, item) {
  for (const file of item.files) {
    const target = path.join(root, file.path);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, file.content);
  }
  const contract = fixtureContract(item);
  if (!contract) return null;
  const contractPath = path.join(root, 'quality-contract.json');
  fs.writeFileSync(contractPath, `${JSON.stringify(contract, null, 2)}\n`);
  return contractPath;
}
