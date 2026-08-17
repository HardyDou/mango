import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';

const packageRoot = path.resolve(import.meta.dirname, '..', '..');
const componentSource = fs.readFileSync(path.join(packageRoot, 'src/components/LinkPage.vue'), 'utf8');
const typeSource = fs.readFileSync(path.join(packageRoot, 'src/types.ts'), 'utf8');
const readmeSource = fs.readFileSync(path.join(packageRoot, 'README.md'), 'utf8');
const packageJson = JSON.parse(fs.readFileSync(path.join(packageRoot, 'package.json'), 'utf8'));
const releaseVersions = JSON.parse(
  fs.readFileSync(path.resolve(packageRoot, '..', 'mango-cli', 'release-versions.json'), 'utf8'),
);

test('link-page exposes tenantId and forwards it only for anonymous public queries', () => {
  assert.match(typeSource, /tenantId\?: string \| number;/);
  assert.match(
    componentSource,
    /if \(props\.authenticated\)\s*\{[\s\S]*?return listVisibleLinks\(query, requestOptions\.value\);[\s\S]*?\}\s*return listPublicLinks\([\s\S]*?tenantId: props\.tenantId == null \? undefined : String\(props\.tenantId\),[\s\S]*?\.\.\.query/,
  );
  assert.match(readmeSource, /`tenantId`.*匿名公开导航查询使用的租户 ID/u);
  assert.equal(packageJson.version, releaseVersions.npm['@mango/link-page']);
});
