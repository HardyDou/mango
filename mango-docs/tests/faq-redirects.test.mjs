import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const docsRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const dist = path.join(docsRoot, '.vitepress/public-src/.vitepress/dist');
// Pin the published URLs independently of the staging implementation.
const legacySlugs = [
  'permission-button-troubleshooting',
  'rbac-menu-page-troubleshooting',
  'tenant-dict-config-empty',
];

for (const slug of legacySlugs) {
  const oldPath = `mango-docs/guides/business-integration/${slug}`;
  const newPath = `mango-docs/guides/faq/${slug}.html`;

  test(`${slug}: built old URL contains a no-JavaScript entry to the built FAQ`, () => {
    const html = readFileSync(path.join(dist, `${oldPath}.html`), 'utf8');
    const refresh = html.match(/<meta http-equiv="refresh" content="0;url=([^"]+)">/u)?.[1];
    const manualLink = html.match(/<a href="([^"]+)">/u)?.[1];
    assert.ok(refresh, 'old URL must offer an automatic redirect even without JavaScript');
    assert.equal(manualLink, refresh);
    const base = 'https://docs.example.test/mango/';
    const target = new URL(refresh, `${base}${oldPath}.html`);
    assert.equal(target.href, `${base}${newPath}`);
    const targetHtml = readFileSync(path.join(dist, newPath), 'utf8');
    assert.match(targetHtml, /<main[\s>]/u, 'redirect must resolve to the actual VitePress FAQ page');
  });

  test(`${slug}: redirect script preserves query/hash and works under different base paths`, () => {
    const html = readFileSync(path.join(dist, `${oldPath}.html`), 'utf8');
    const script = html.match(/<script>([\s\S]*?)<\/script>/u)?.[1];
    assert.ok(script, 'compatibility page must carry the query/hash-preserving redirect');
    for (const base of ['/', '/mango/', '/mango/versions/1.0.52/']) {
      for (const suffix of ['', '.html']) {
        const search = '?from=bookmark&lang=zh';
        const hash = '#5-%E5%B8%B8%E8%A7%81%E5%A4%B1%E8%B4%A5';
        const location = new URL(`https://docs.example.test${base}${oldPath}${suffix}${search}${hash}`);
        const navigations = [];
        vm.runInNewContext(script, {
          URL,
          window: {
            location: {
              href: location.href,
              search: location.search,
              hash: location.hash,
              replace: value => navigations.push(String(value)),
            },
          },
        });
        assert.deepEqual(navigations, [`https://docs.example.test${base}${newPath}${search}${hash}`]);
      }
    }
  });
}
