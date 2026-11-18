import assert from 'node:assert/strict';
import test from 'node:test';

import {
  buildFingerprintModel,
  describeChange,
  extractCompatibleAdditions,
  extractFieldName,
  firstReleaseSection,
  formatReleaseNotesBlock,
  parseFingerprints,
  verifyReleaseNotesSection,
} from '../tools/workflow-contract-fingerprint.mjs';

const API_VALUE = '053297d7a998ef45b6c40093a2d4e1fb25d04d0323159773424b5c1fe148a8a1';
const WEB_VALUE = '277ab3b80baff855b0e4fdedc094ec7e73f67415e5951ca15835c273449da417';
const OLD_API_VALUE = '649612f32fe8455489b39ac2610a5d1322f66c9559c7868b24b17d7de0bc1f16';
const OLD_WEB_VALUE = '32e55a001ad78d3b65ec64e55d63cd70bf6e202e33843438d84b7d2012bd9851';

test('parseFingerprints 读常量形态', () => {
  const source = [
    '    private static final String API_FINGERPRINT =',
    `            "${API_VALUE}";`,
    '    private static final String HTTP_FINGERPRINT =',
    `            "${WEB_VALUE}";`,
  ].join('\n');

  assert.deepEqual(parseFingerprints(source), { api: API_VALUE, web: WEB_VALUE });
});

test('parseFingerprints 兜底读内联断言形态', () => {
  const source = [
    '    void a() {',
    `        assertThat(apiFingerprint()).isEqualTo("${OLD_API_VALUE}");`,
    `        assertThat(httpFingerprint()).isEqualTo("${OLD_WEB_VALUE}");`,
    '    }',
  ].join('\n');

  assert.deepEqual(parseFingerprints(source), { api: OLD_API_VALUE, web: OLD_WEB_VALUE });
});

test('describeChange 覆盖四种组合', () => {
  const base = { api: 'a', web: 'b' };
  assert.equal(describeChange(base, { api: 'a', web: 'b' }), 'none');
  assert.equal(describeChange(base, { api: 'x', web: 'b' }), 'api');
  assert.equal(describeChange(base, { api: 'a', web: 'y' }), 'web');
  assert.equal(describeChange(base, { api: 'x', web: 'y' }), 'both');
});

test('extractFieldName 跳过注解与泛型取变量名', () => {
  assert.equal(
    extractFieldName(
      ' @Schema(description = "业务类型集合") private List<@NotBlank(message = "不能为空") @Size(max = 128) String> businessTypes;'
    ),
    'businessTypes'
  );
  assert.equal(extractFieldName(' private final String processDefinitionKeys;'), 'processDefinitionKeys');
  assert.equal(extractFieldName(' private String[] values = new String[0];'), 'values');
  assert.equal(extractFieldName(' businessTypes++;'), null);
});

test('extractCompatibleAdditions 抽出跨行字段与枚举值并忽略范围外文件', () => {
  const diff = [
    'diff --git a/x b/x',
    '--- a/query/WorkflowTaskBusinessPageQuery.java',
    '+++ b/mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/query/WorkflowTaskBusinessPageQuery.java',
    '@@ -27,0 +28,4 @@',
    '+ * 说明段',
    '@@ -37,0 +42,6 @@',
    '+    @Schema(description = "业务类型集合，可选")',
    '+    @Size(max = 20, message = "业务类型数量不能超过20")',
    '+    private List<@NotBlank(message = "业务类型不能为空")',
    '+                 @Size(max = 128, message = "业务类型长度不能超过128") String> businessTypes;',
    '+',
    'diff --git a/y b/y',
    '--- a/enums/WorkflowTaskClaimStatus.java',
    '+++ b/mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/enums/WorkflowTaskClaimStatus.java',
    '@@ -10,0 +11,2 @@',
    '+    NONE("未认领且无候选人"),',
    '+    UNCLAIMED("未认领"),',
    'diff --git a/z b/z',
    '--- a/service/WorkflowTaskRuntimeService.java',
    '+++ b/mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/service/impl/WorkflowTaskRuntimeService.java',
    '@@ -1,0 +2,1 @@',
    '+    private String ignoredField;',
  ].join('\n');

  assert.deepEqual(extractCompatibleAdditions(diff), [
    { className: 'WorkflowTaskBusinessPageQuery', names: ['businessTypes'] },
    { className: 'WorkflowTaskClaimStatus', names: ['NONE', 'UNCLAIMED'] }
  ]);
});

test('extractCompatibleAdditions 不把删除的字段当成新增', () => {
  const diff = [
    '--- a/vo/WorkflowTaskVO.java',
    '+++ b/mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/vo/WorkflowTaskVO.java',
    '@@ -10,1 +10,0 @@',
    '-    private String legacyField;',
  ].join('\n');

  assert.deepEqual(extractCompatibleAdditions(diff), []);
});

test('formatReleaseNotesBlock 输出可直接粘进发布说明的片段', () => {
  const block = formatReleaseNotesBlock({
    api: API_VALUE,
    web: WEB_VALUE,
    changed: 'none',
    since: `${API_VALUE} / ${WEB_VALUE}`,
    additions: [{ className: 'WorkflowTaskBusinessPageQuery', names: ['businessTypes', 'processDefinitionKeys'] }]
  });

  assert.equal(
    block,
    [
      'workflow-contract-fingerprint:',
      `  api:    ${API_VALUE}`,
      `  web:    ${WEB_VALUE}`,
      '  changed: none',
      `  since:  ${API_VALUE} / ${WEB_VALUE}`,
      '',
      'workflow-compatible-additions:',
      '  - WorkflowTaskBusinessPageQuery: + businessTypes, processDefinitionKeys',
    ].join('\n')
  );
});

test('formatReleaseNotesBlock 在没有兼容新增时明确输出空列表', () => {
  const block = formatReleaseNotesBlock({
    api: API_VALUE,
    web: WEB_VALUE,
    changed: 'none',
    since: 'n/a',
    additions: [],
  });

  assert.ok(block.endsWith('workflow-compatible-additions: []'));
});

test('buildFingerprintModel 在缺少上一批次基线时给出 changed=none 与 since=n/a', () => {
  const headSource = `private static final String API_FINGERPRINT = "${API_VALUE}";\n`
    + `private static final String HTTP_FINGERPRINT = "${WEB_VALUE}";`;

  const model = buildFingerprintModel({ headSource, baseSource: null, base: '', additions: [] });

  assert.equal(model.changed, 'none');
  assert.equal(model.since, 'n/a');
  assert.equal(model.api, API_VALUE);
});

test('buildFingerprintModel 在基线是可解析的旧值时报出变化方向', () => {
  const headSource = `private static final String API_FINGERPRINT = "${API_VALUE}";\n`
    + `private static final String HTTP_FINGERPRINT = "${WEB_VALUE}";`;
  const baseSource = `assertThat(apiFingerprint()).isEqualTo("${OLD_API_VALUE}");\n`
    + `assertThat(httpFingerprint()).isEqualTo("${OLD_WEB_VALUE}");`;

  const model = buildFingerprintModel({
    headSource,
    baseSource,
    base: 'v2026.11.16-maven-1.12.0-task-release-facts-release',
    additions: [],
  });

  assert.equal(model.changed, 'both');
  assert.equal(model.since, `${OLD_API_VALUE} / ${OLD_WEB_VALUE}`);
});

test('firstReleaseSection 只取第一个发布段落', () => {
  const changelog = [
    '## Unreleased - x',
    'body A',
    '',
    '## v2026.11.18-maven-1.14.0-release - y',
    'body B',
  ].join('\n');

  assert.equal(firstReleaseSection(changelog), 'body A\n');
});

test('verifyReleaseNotesSection 接受与代码一致的块', () => {
  const model = {
    api: API_VALUE,
    web: WEB_VALUE,
    changed: 'none',
    since: `${API_VALUE} / ${WEB_VALUE}`,
    additions: [],
    unverifiedContractChanges: [],
  };
  const section = formatReleaseNotesBlock(model);

  assert.deepEqual(verifyReleaseNotesSection(section, model), []);
});

test('verifyReleaseNotesSection 报出缺失、数值不一致与未验证改动', () => {
  const model = {
    api: API_VALUE,
    web: WEB_VALUE,
    changed: 'api',
    since: `${OLD_API_VALUE} / ${OLD_WEB_VALUE}`,
    additions: [{ className: 'X', names: ['y'] }],
    unverifiedContractChanges: ['.../WorkflowCategoryApi.java'],
  };

  const problems = verifyReleaseNotesSection('## heading\n没有字段', model);

  assert.ok(problems.some((problem) => problem.includes('没有 workflow-contract-fingerprint 块')));
  assert.ok(problems.some((problem) => problem.includes('没有 workflow-compatible-additions 块')));
  assert.ok(problems.some((problem) => problem.includes('契约源码有改动但指纹常量未更新')));
});
