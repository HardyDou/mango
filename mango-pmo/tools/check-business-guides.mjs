#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const args = process.argv.slice(2);
const selfTest = args.includes('--self-test');

const guideChecks = [
  {
    file: 'mango-docs/guides/business-integration/file-upload-form.md',
    sections: ['阅读顺序', '接入检查点', '业务场景验收点', '最小闭环', '常见失败', '验证命令', '关联规则'],
    text: [
      'File 后端 README',
      'File Components README',
      'MUpload',
      'FilePreviewPanel',
      'attachmentIds',
      'fileId',
      'fileIds',
      '后端代码文件引用规则',
      '前端文件上传与回显规则',
      'pnpm -F @mango/file build'
    ],
    forbiddenText: ['投产检查项']
  },
  {
    file: 'mango-docs/guides/business-integration/workflow-business-approval.md',
    sections: ['阅读顺序', '接入检查点', '最小闭环', '常见失败', '验证命令', '关联规则'],
    text: [
      'Workflow 后端 README',
      '@mango/workflow README',
      'Workflow Example README',
      'businessKey',
      '审批通过',
      'pnpm -F @mango/workflow build'
    ]
  },
  {
    file: 'mango-docs/guides/business-integration/rbac-menu-page-troubleshooting.md',
    sections: ['阅读顺序', '接入检查点', '最小闭环', '常见失败', '验证命令', '关联规则'],
    text: [
      'Authorization 后端 README',
      '@mango/rbac README',
      '@mango/admin-shell README',
      '404',
      '401/403',
      'pnpm -F @mango/rbac build'
    ]
  },
  {
    file: 'mango-docs/guides/business-integration/permission-button-troubleshooting.md',
    sections: ['阅读顺序', '接入检查点', '最小闭环', '常见失败', '验证命令', '关联规则'],
    text: [
      'Access 后端 README',
      'Authorization 后端 README',
      '@mango/rbac README',
      '@mango/admin-shell README',
      '按钮',
      '权限码',
      '403'
    ]
  },
  {
    file: 'mango-docs/guides/business-integration/tenant-dict-config-empty.md',
    sections: ['阅读顺序', '接入检查点', '最小闭环', '常见失败', '验证命令', '关联规则'],
    text: [
      'Identity 后端 README',
      'Org 后端 README',
      'System 后端 README',
      'Resource 后端 README',
      '@mango/admin-shell README',
      'tenantId',
      '字典'
    ]
  }
];

const forbiddenRulePatterns = [
  /长期规则/,
  /长期规范/,
  /必须(?:永久|长期|统一|一律|始终)/,
  /禁止(?:永久|长期|统一|一律|始终)/,
  /所有业务必须/,
  /所有模块必须/,
  /不得作为长期/,
  /必须遵守(?:这个|该|本)/
];

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function hasSection(text, section) {
  return new RegExp(`^##\\s+(?:\\d+\\.\\s*)?${escapeRegExp(section)}\\s*$`, 'm').test(text);
}

function validateGuide({ file, sections, text, forbiddenText = [] }) {
  const failures = [];
  if (!fs.existsSync(path.join(root, file))) {
    return [`${file}: missing guide file`];
  }
  const body = read(file);
  for (const section of sections) {
    if (!hasSection(body, section)) {
      failures.push(`${file}: missing section "${section}"`);
    }
  }
  for (const expected of text) {
    if (!body.includes(expected)) {
      failures.push(`${file}: missing expected text "${expected}"`);
    }
  }
  for (const forbidden of forbiddenText) {
    if (body.includes(forbidden)) {
      failures.push(`${file}: should not contain "${forbidden}"`);
    }
  }
  for (const pattern of forbiddenRulePatterns) {
    if (pattern.test(body)) {
      failures.push(`${file}: contains long-term rule wording "${pattern.source}"`);
    }
  }
  return failures;
}

function validateTextForSelfTest(body) {
  return forbiddenRulePatterns
    .filter((pattern) => pattern.test(body))
    .map((pattern) => `contains long-term rule wording "${pattern.source}"`);
}

function runSelfTest() {
  const valid = validateGuide({
    file: 'mango-docs/guides/business-integration/file-upload-form.md',
    sections: ['阅读顺序'],
    text: ['MUpload'],
    forbiddenText: ['not-real-forbidden-marker']
  });
  const invalid = validateGuide({
    file: 'mango-docs/guides/business-integration/file-upload-form.md',
    sections: ['不存在章节'],
    text: ['not-real-business-guide-marker']
  });
  const forbidden = validateTextForSelfTest('所有业务必须永久遵守这个临时规则。');
  if (valid.length !== 0 || invalid.length !== 2 || forbidden.length !== 2) {
    console.error('Business guide checks self-test failed');
    process.exit(1);
  }
  console.log('Business guide checks self-test passed: 3 cases');
}

if (selfTest) {
  runSelfTest();
  process.exit(0);
}

const failures = guideChecks.flatMap(validateGuide);
if (failures.length > 0) {
  console.error(`Business guide checks failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
  process.exit(1);
}

console.log(`Business guide checks passed: ${guideChecks.length} guides inspected`);
