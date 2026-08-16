import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const source = readFileSync(resolve(dirname(fileURLToPath(import.meta.url)), 'index.vue'), 'utf8');

describe('file settings view', () => {
  it('uses the Element Plus 2.14 radio value API', () => {
    expect(source).toContain('<el-radio-button value="PROXY">');
    expect(source).toContain('<el-radio-button value="DIRECT">');
    expect(source).not.toMatch(/<el-radio-button\s+label=/);
  });
});
