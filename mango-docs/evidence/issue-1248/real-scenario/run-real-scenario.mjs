#!/usr/bin/env node
/**
 * Issue #1248 真实场景验收脚本。
 *
 * 场景：保函风控审批（业务类型 BAOHAN_RC_PROJECT_REVIEW）。
 * 依赖：当前 worktree 已用 `mango dev start backend` 启动后端；数据库为 .mango/dev-workspace.env 指向的真实 MySQL/MariaDB。
 * 覆盖：Flyway V5 迁移后的表可用、创建申请写属性、任务分页按属性过滤（且/或/多值/缺属性/分页/申请时间）、
 *       非法属性名拒绝、提交后属性覆盖与清空、默认通知模板展开 applyAttribute_<key>。
 */
import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(HERE, '../../../..');
const ENV_FILE = resolve(ROOT, '.mango/dev-workspace.env');
const BASE = `http://127.0.0.1:${env('MANGO_BACKEND_PORT', '18003')}`;
const DB = env('MANGO_DB_NAME', 'mango_dev_mango_issue_1248_verify_003');
const DB_HOST = env('MANGO_DB_HOST', '127.0.0.1');
const DB_PORT = env('MANGO_DB_PORT', '3306');
const DB_USER = env('MANGO_DB_USERNAME', 'root');
const DB_PASSWORD = env('MANGO_DB_PASSWORD', '').replace(/^'|'$/g, '');

const BUSINESS_TYPE = 'BAOHAN_RC_PROJECT_REVIEW';
const DEFINITION_KEY = 'BAOHAN_RC_REVIEW_E2E_1248';
const DEFINITION_NAME = '保函风控审核（1248 真实场景验收）';
const KEYS = ['BIZ-E2E-1248-1', 'BIZ-E2E-1248-2', 'BIZ-E2E-1248-3'];
const checks = [];

function env(name, fallback) {
  try {
    const text = readFileSync(ENV_FILE, 'utf8');
    const line = text.split('\n').find((item) => item.startsWith(name + '='));
    return line ? line.slice(name.length + 1) : fallback;
  } catch {
    return fallback;
  }
}

function sql(statement) {
  const args = ['-h', DB_HOST, '-P', DB_PORT, '-u', DB_USER];
  if (DB_PASSWORD) {
    args.push('-p' + DB_PASSWORD);
  }
  args.push(DB, '-N', '-B', '-e', statement);
  return execFileSync('mysql', args, { encoding: 'utf8' }).trim();
}

async function api(method, path, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = 'Bearer ' + token;
  }
  const response = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const payload = await response.json();
  return { status: response.status, payload };
}

function check(name, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  checks.push({ name, ok, actual, expected });
  console.log(`${ok ? 'PASS' : 'FAIL'} ${name} actual=${JSON.stringify(actual)} expected=${JSON.stringify(expected)}`);
}

function keysOf(page) {
  return (page.list ?? []).map((row) => row.businessKey).sort();
}

function orderedKeysOf(page) {
  return (page.list ?? []).map((row) => row.businessKey);
}

async function login() {
  const { payload } = await api('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantId: '1', realm: 'INTERNAL',
  });
  if (!payload.success) {
    throw new Error('登录失败: ' + JSON.stringify(payload));
  }
  return payload.data.accessToken;
}

async function createDefinition(token) {
  const categoryId = sql(
    "select id from workflow_category where category_code='PAYMENT_BUILTIN' limit 1");
  const designer = {
    id: 'root',
    nodeType: 'ROOT',
    nodeName: '发起',
    childNode: {
      id: 'risk_review',
      nodeType: 'APPROVAL',
      nodeName: '风控审核',
      properties: { approvalConfig: { approvalMode: 'OR_SIGN', assigneeIds: ['1'] } },
    },
  };
  const { payload } = await api('POST', '/workflow/definitions', {
    definitionName: DEFINITION_NAME,
    definitionKey: DEFINITION_KEY,
    domainCode: 'PAYMENT',
    categoryId,
    orgId: 1,
    adminUsers: ['admin'],
    startEntryVisible: true,
    status: 'DRAFT',
    formJson: '{}',
    designerJson: JSON.stringify(designer),
  }, token);
  if (!payload.success) {
    throw new Error('创建流程定义失败: ' + JSON.stringify(payload));
  }
  const definitionId = payload.data;
  const deploy = await api('POST', `/workflow/definitions/deploy?id=${definitionId}`, {}, token);
  if (!deploy.payload.success) {
    throw new Error('发布流程定义失败: ' + JSON.stringify(deploy.payload));
  }
  return { definitionId, processDefinitionId: deploy.payload.data.processDefinitionId };
}

async function createApply(token, key, title, attributes) {
  const { payload } = await api('POST', '/workflow/business-applies', {
    businessType: BUSINESS_TYPE,
    businessKey: key,
    applyTitle: title,
    applySummary: `名称：保函风控审批，业务主键：${key}`,
    renderMode: 'DYNAMIC_FORM',
    filterAttributes: attributes,
  }, token);
  if (!payload.success) {
    throw new Error(`创建申请失败 ${key}: ` + JSON.stringify(payload));
  }
  return payload.data;
}

async function startApply(token, apply, key) {
  const { payload } = await api('POST', '/workflow/processes/start', {
    applyId: apply.id,
    definitionKey: DEFINITION_KEY,
    businessType: BUSINESS_TYPE,
    businessKey: key,
  }, token);
  if (!payload.success) {
    throw new Error(`发起流程失败 ${key}: ` + JSON.stringify(payload));
  }
}

async function queryTasks(token, extra = {}) {
  const { payload } = await api('POST', '/workflow/tasks/page-by-business-key', {
    businessType: BUSINESS_TYPE, scope: 'SELF', page: 1, size: 20, ...extra,
  }, token);
  if (!payload.success) {
    throw new Error('任务分页失败: ' + JSON.stringify(payload));
  }
  return payload.data;
}

function totalOf(page) {
  return Number(page.total);
}

async function waitForNotice(applyTitle, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs;
  let content = '';
  while (Date.now() < deadline) {
    content = sql(
      `select content from notice_site_message where content like '%${applyTitle}%' order by id desc limit 1`);
    if (content) {
      return content;
    }
    await new Promise((item) => setTimeout(item, 500));
  }
  return content;
}

async function main() {
  const token = await login();
  const definition = await createDefinition(token);

  // 真实业务配置：默认通知模板引用 applyAttribute_<key> 模板变量
  sql(`update notice_business_channel_template
       set title_template='待审核：{{processName}}（{{applyAttribute_guaranteeTypeCode}}）',
           content_template='业务：{{applyTitle}}；保函种类：{{applyAttribute_guaranteeTypeCode}}；受益人：{{applyAttribute_beneficiary}}；版本状态：{{applyAttribute_versionStatus}}'
       where biz_type='workflow.task.assigned' and channel_type='SITE' and version_status='ACTIVE'`);

  const p1 = await createApply(token, KEYS[0], '履约保函风控审批-1', [
    { key: 'guaranteeTypeCode', values: ['PERF'] },
    { key: 'beneficiary', values: ['中铁三局', '中铁四局', '中铁四局'] },
    { key: 'versionStatus', values: ['PENDING'] },
  ]);
  await new Promise((item) => setTimeout(item, 1100));
  const p2 = await createApply(token, KEYS[1], '投标保函风控审批-2', [
    { key: 'guaranteeTypeCode', values: ['BID'] },
    { key: 'beneficiary', values: ['中铁四局'] },
    { key: 'versionStatus', values: ['PENDING'] },
  ]);
  await new Promise((item) => setTimeout(item, 1100));
  const p3 = await createApply(token, KEYS[2], '履约保函风控审批-3', [
    { key: 'guaranteeTypeCode', values: ['PERF'] },
    { key: 'beneficiary', values: ['中建八局'] },
    { key: 'versionStatus', values: ['REVIEWED'] },
  ]);
  await startApply(token, p1, KEYS[0]);
  await startApply(token, p2, KEYS[1]);
  await startApply(token, p3, KEYS[2]);

  let page = await queryTasks(token);
  for (let index = 0; index < 20 && totalOf(page) < 3; index++) {
    await new Promise((item) => setTimeout(item, 300));
    page = await queryTasks(token);
  }
  check('基线：不传属性过滤返回 3 条任务', totalOf(page), 3);

  page = await queryTasks(token, { applyAttributeFilters: [{ key: 'guaranteeTypeCode', values: ['PERF'] }] });
  check('AC-1 单条件单值只返回履约保函任务', [totalOf(page), keysOf(page)], [2, [KEYS[0], KEYS[2]]]);

  page = await queryTasks(token, { applyAttributeFilters: [{ key: 'guaranteeTypeCode', values: ['PERF', 'BID'] }] });
  check('AC-3 同一条件多值之间为或', [totalOf(page), keysOf(page)], [3, KEYS]);

  page = await queryTasks(token, {
    applyAttributeFilters: [
      { key: 'guaranteeTypeCode', values: ['PERF'] },
      { key: 'versionStatus', values: ['PENDING'] },
    ],
  });
  check('AC-2 条件之间为且', [totalOf(page), keysOf(page)], [1, [KEYS[0]]]);

  page = await queryTasks(token, {
    applyAttributeFilters: [
      { key: 'guaranteeTypeCode', values: ['PERF'] },
      { key: 'beneficiary', values: ['中铁四局'] },
    ],
  });
  check('AC-5 多值字段任一值命中', [totalOf(page), keysOf(page)], [1, [KEYS[0]]]);

  page = await queryTasks(token, { applyAttributeFilters: [{ key: 'guaranteeTypeCode', values: ['UNKNOWN'] }] });
  check('AC-4 未登记的属性值不匹配', totalOf(page), 0);

  page = await queryTasks(token, {
    page: 2, size: 1, sortBy: 'applyTime', sortOrder: 'asc',
    applyAttributeFilters: [{ key: 'guaranteeTypeCode', values: ['PERF'] }],
  });
  check('AC-9 翻页 total 与逐页结果同源一致', [totalOf(page), keysOf(page)], [2, [KEYS[2]]]);

  page = await queryTasks(token, {
    sortBy: 'applyTime', sortOrder: 'desc',
    applyAttributeFilters: [{ key: 'guaranteeTypeCode', values: ['PERF'] }],
  });
  check('AC-8 sortBy=applyTime 按申请创建时间排序', orderedKeysOf(page), [KEYS[2], KEYS[0]]);

  page = await queryTasks(token, { applyTimeBegin: p2.createdAt, applyTimeEnd: p2.createdAt });
  check('AC-7 applyTimeBegin/End 闭区间过滤', keysOf(page), [KEYS[1]]);

  const invalid = await api('POST', '/workflow/tasks/page-by-business-key', {
    businessType: BUSINESS_TYPE,
    scope: 'SELF',
    applyAttributeFilters: [{ key: '1invalid', values: ['x'] }],
  }, token);
  check('AC-1 非法属性名被拒绝', [invalid.payload.success, invalid.payload.msg.includes('可筛属性名')], [false, true]);

  const update = await api('PUT', '/workflow/business-applies/filter-attributes', {
    applyId: p3.id,
    filterAttributes: [
      { key: 'guaranteeTypeCode', values: ['PERF'] },
      { key: 'beneficiary', values: ['中建八局'] },
      { key: 'versionStatus', values: ['PENDING'] },
    ],
  }, token);
  check('AC-12 提交后覆盖版本状态', update.payload.success, true);

  page = await queryTasks(token, { applyAttributeFilters: [{ key: 'versionStatus', values: ['PENDING'] }] });
  check('AC-12 覆盖后筛选结果随之变化', totalOf(page), 3);

  const clear = await api('PUT', '/workflow/business-applies/filter-attributes', {
    applyId: p2.id, filterAttributes: [],
  }, token);
  check('AC-12 空数组清空属性', clear.payload.success, true);
  page = await queryTasks(token, { applyAttributeFilters: [{ key: 'guaranteeTypeCode', values: ['BID'] }] });
  check('AC-4 清空属性后不再命中', totalOf(page), 0);

  const p1BeneficiaryRows = Number(sql(
    `select count(*) from workflow_business_apply_attribute where apply_id=${p1.id} and attr_key='beneficiary'`));
  check('AC-11 多值字段一行一个值且重复值去重', p1BeneficiaryRows, 2);

  const notice = await waitForNotice('履约保函风控审批-1');
  check('AC-13/14 事件载荷与通知变量展开（保函种类）', notice.includes('保函种类：PERF'), true);
  check('AC-13/14 事件载荷与通知变量展开（多值逗号拼接）', notice.includes('受益人：中铁三局,中铁四局'), true);
  check('AC-13/14 事件载荷与通知变量展开（版本状态）', notice.includes('版本状态：PENDING'), true);

  const report = {
    generatedAt: new Date().toISOString(),
    base: BASE,
    database: DB,
    definition,
    applyIds: { p1: p1.id, p2: p2.id, p3: p3.id },
    noticeContent: notice,
    passed: checks.filter((item) => item.ok).length,
    failed: checks.filter((item) => !item.ok).length,
    checks,
  };
  writeFileSync(resolve(HERE, 'run-output.json'), JSON.stringify(report, null, 2) + '\n');
  console.log('\nSUMMARY ' + JSON.stringify({ passed: report.passed, failed: report.failed }));
  return report.failed === 0 ? 0 : 1;
}

main().then((code) => process.exit(code)).catch((error) => {
  console.error(error);
  process.exit(2);
});
