#!/usr/bin/env node
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const testRoot = dirname(fileURLToPath(import.meta.url));
const path = join(testRoot, 'comprehension-evals.json');
const suite = JSON.parse(readFileSync(path, 'utf8'));

assert(suite.schemaVersion === 1, 'unsupported comprehension dataset schemaVersion');
assert(suite.suiteId === 'mango-understanding-first-pilot', 'unexpected comprehension suite id');
assert(suite.trustedFixturePolicy === 'evaluator-injected-current-repository-facts', 'trusted fixture policy is required');
assert(Array.isArray(suite.cases) && suite.cases.length === 8, 'the pilot must contain exactly eight comprehension cases');
assert(suite.scoring?.targetScore === 5, 'target score must remain 5/6');
assert(suite.scoring?.questions?.length === 6, 'the comprehension gate must contain six questions');

const ids = new Set();
for (const item of suite.cases) {
  assert(typeof item.id === 'string' && item.id.startsWith('COG-'), `${item.id || '<missing>'}: invalid id`);
  assert(!ids.has(item.id), `duplicate case id: ${item.id}`);
  ids.add(item.id);
  assert(typeof item.prompt === 'string' && item.prompt.trim(), `${item.id}: prompt is required`);
  assert(typeof item.recommendedFormat === 'string' && item.recommendedFormat.trim(), `${item.id}: recommended format is required`);
  assert(Array.isArray(item.expectedFacts) && item.expectedFacts.length >= 3, `${item.id}: expected facts are insufficient`);
  assert(Array.isArray(item.canonicalTerms) && item.canonicalTerms.length > 0, `${item.id}: canonical terms are required`);
  assert(Array.isArray(item.sourceRefs) && item.sourceRefs.length > 0, `${item.id}: current source references are required`);
  assert(Array.isArray(item.forbiddenAssumptions) && item.forbiddenAssumptions.length > 0, `${item.id}: forbidden assumptions are required`);
  assert(typeof item.nextSource === 'string' && item.nextSource.includes('/'), `${item.id}: next source is required`);
}

process.stdout.write(`Checked ${suite.cases.length} comprehension cases.\n`);

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}
