import { readFileSync } from 'node:fs';
import { basename, resolve } from 'node:path';
import { compileScript, compileTemplate, parse } from '@vue/compiler-sfc';
import { describe, expect, it } from 'vitest';

const pageFiles = [
  resolve(__dirname, '../definition/index.vue'),
  resolve(__dirname, '../instance/index.vue'),
  resolve(__dirname, '../worker/index.vue'),
  resolve(__dirname, '../alarm/index.vue'),
];

describe('job list pagination component', () => {
  it.each(pageFiles)('resolves Pagination from the setup binding in %s', (filename) => {
    const source = readFileSync(filename, 'utf8');
    const { descriptor, errors } = parse(source, { filename });

    expect(errors).toEqual([]);
    expect(descriptor.template).toBeTruthy();

    const script = compileScript(descriptor, { id: basename(filename) });
    const template = compileTemplate({
      id: basename(filename),
      filename,
      source: descriptor.template!.content,
      compilerOptions: {
        bindingMetadata: script.bindings,
      },
    });

    expect(template.errors).toEqual([]);
    expect(script.bindings?.Pagination).toBe('setup-maybe-ref');
    expect(template.code).not.toContain('resolveComponent("Pagination")');
    expect(source).toContain('v-model:page="query.pageNum"');
    expect(source).toContain('v-model:limit="query.pageSize"');
    expect(source).toContain('@pagination="loadRows"');
  });
});
