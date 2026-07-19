import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import ts from 'typescript';
import { fileURLToPath } from 'node:url';

const qualityRoot = path.dirname(fileURLToPath(import.meta.url));
const uiRoot = path.resolve(qualityRoot, '../..');
const contractSource = path.join(uiRoot, 'packages/api-schema/src/index.ts');

function compileFixture(name) {
  const file = path.join(qualityRoot, '__fixtures__/http-contract', name);
  const program = ts.createProgram([file], {
    module: ts.ModuleKind.NodeNext,
    moduleResolution: ts.ModuleResolutionKind.NodeNext,
    noEmit: true,
    skipLibCheck: true,
    strict: true,
    target: ts.ScriptTarget.ES2022,
  });
  return ts.getPreEmitDiagnostics(program);
}

test('accepts a vendor-neutral typed HttpClient consumer using AbortSignal', () => {
  assert.deepEqual(compileFixture('positive.ts'), []);
});

test('rejects numeric ApiId and a non-generic transport implementation', () => {
  const diagnostics = compileFixture('negative.ts');
  assert.ok(diagnostics.length >= 2);
  const messages = diagnostics.map((item) => ts.flattenDiagnosticMessageText(item.messageText, '\n')).join('\n');
  assert.match(messages, /number.*string|not assignable to type 'ApiId'/u);
  assert.match(messages, /not assignable to type.*Promise<TResponse>|not assignable to type.*HttpClient/su);
});

test('keeps the FE0 contract free of framework, adapter and host imports', () => {
  const source = fs.readFileSync(contractSource, 'utf8');
  assert.doesNotMatch(source, /\b(?:axios|element-plus|pinia|vue-router)\b|from\s+['"]vue['"]/iu);
  assert.doesNotMatch(source, /\b(?:window|document|import\.meta\.env|process\.env)\b/u);
});
