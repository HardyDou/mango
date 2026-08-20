import fs from 'node:fs';
import path from 'node:path';

export const defaultProjectPmoChecks = Object.freeze({
  frontendPageBaseline: true,
});

export function resolveProjectPmoChecks(config) {
  if (config === null || typeof config !== 'object' || Array.isArray(config)) {
    throw new Error('mango.config.json must contain a JSON object');
  }
  const configured = config.pmoChecks;
  if (configured === undefined) return { ...defaultProjectPmoChecks };
  if (configured === null || typeof configured !== 'object' || Array.isArray(configured)) {
    throw new Error('mango.config.json pmoChecks must be an object');
  }
  const frontendPageBaseline = configured.frontendPageBaseline;
  if (frontendPageBaseline === undefined) return { ...defaultProjectPmoChecks };
  if (typeof frontendPageBaseline !== 'boolean') {
    throw new Error('mango.config.json pmoChecks.frontendPageBaseline must be a boolean');
  }
  return { frontendPageBaseline };
}

export function readProjectPmoChecks(repositoryRoot) {
  const configPath = path.join(repositoryRoot, 'mango.config.json');
  if (!fs.existsSync(configPath)) return { ...defaultProjectPmoChecks };
  let config;
  try {
    config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  } catch (error) {
    throw new Error(`Cannot read mango.config.json PMO checks: ${error.message}`);
  }
  return resolveProjectPmoChecks(config);
}
