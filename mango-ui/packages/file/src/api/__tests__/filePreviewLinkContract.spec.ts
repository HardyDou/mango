import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const VO_RELATIVE_PATH =
  'mango/mango-platform/mango-file-preview/mango-file-preview-api/src/main/java/io/mango/file/preview/api/vo/FilePreviewLinkVO.java';

/** 单测环境的 import.meta.url 不是 file 协议，按工作目录向上找仓库根。 */
function locate(relativePath: string): string {
  const candidates = [
    resolve(process.cwd(), relativePath),
    resolve(process.cwd(), '..', relativePath),
    resolve(process.cwd(), '..', '..', relativePath),
    resolve(process.cwd(), '..', '..', '..', relativePath),
  ];
  return candidates.find((candidate) => existsSync(candidate)) || candidates[0];
}

const javaVoPath = locate(VO_RELATIVE_PATH);
const tsApiPath = resolve(process.cwd(), 'src/api/file.ts');

function javaVoFields(source: string): string[] {
  const body = source.slice(source.indexOf('class FilePreviewLinkVO'), source.lastIndexOf('}'));
  return Array.from(body.matchAll(/^\s*private\s+(?!static)[\w<>.,\s]+?\s+(\w+);/gm)).map((match) => match[1]);
}

function tsInterfaceFields(source: string): string[] {
  const start = source.indexOf('export interface FilePreviewLink {');
  const body = source.slice(start, source.indexOf('}', start));
  return Array.from(body.matchAll(/^\s*(\w+)\??:/gm)).map((match) => match[1]);
}

describe.skipIf(!existsSync(javaVoPath))('preview-link 契约', () => {
  it('后端 FilePreviewLinkVO 字段与前端 FilePreviewLink 类型逐字一致', () => {
    const javaFields = javaVoFields(readFileSync(javaVoPath, 'utf8'));
    const tsFields = tsInterfaceFields(readFileSync(tsApiPath, 'utf8'));

    expect(javaFields.length).toBeGreaterThan(0);
    expect(tsFields.slice().sort()).toEqual(javaFields.slice().sort());
  });
});
