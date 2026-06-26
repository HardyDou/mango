<template>
  <div class="numgen-page">
    <section v-if="historyMode" class="numgen-rule-panel history-panel">
      <div class="numgen-table-head">
        <div>
          <h3>历史版本</h3>
          <p>{{ historyGenerator?.genName }} / {{ historyGenerator?.genKey }}</p>
        </div>
        <div class="numgen-head-actions">
          <el-button @click="exitHistoryMode">返回</el-button>
        </div>
      </div>

      <el-table
        class="history-table"
        :data="historyVersionRows"
        v-loading="historyLoading"
        row-key="id"
        stripe
        highlight-current-row
      >
        <el-table-column label="版本" width="90">
          <template #default="{ row }">V{{ row.version }}</template>
        </el-table-column>
        <el-table-column label="版本状态" width="110">
          <template #default="{ row }">
            <el-tag :type="versionStateTagType(row)">
              {{ versionStateLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ruleName" label="规则名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="片段预览" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">{{ historySegmentPreview(row) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="versionState(row) !== 'ACTIVE'"
              link
              type="primary"
              :loading="switchingVersionId === String(row.id)"
              @click="switchToHistoryVersion(row)"
            >
              {{ versionState(row) === 'DRAFT' ? '发布' : '切换版本' }}
            </el-button>
            <span v-else class="muted-text">当前生效</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div v-else class="numgen-list-layout">
      <DomainSideTree
        v-model="generatorQuery.domainCode"
        title="业务域"
        subtitle="按业务域维护编号"
        all-label="全部规则"
        all-code="ALL"
        :all-count="generatorTotal"
        @change="handleDomainChange"
      />

      <section class="numgen-rule-panel">
        <div class="numgen-table-head">
          <div>
            <h3>编号规则</h3>
            <p>维护业务Key、当前生效版本和待发布规则，编辑后发布才会影响业务生成。</p>
          </div>
          <div class="numgen-head-actions">
            <el-button type="primary" :icon="Plus" @click="openGeneratorDrawer()">新增规则</el-button>
          </div>
        </div>

        <el-form :inline="true" :model="generatorQuery" class="search-form">
          <el-form-item label="关键字">
            <el-input
              v-model="generatorQuery.keyword"
              placeholder="业务Key / 名称"
              clearable
              @keyup.enter="loadGenerators"
              @clear="loadGenerators"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="generatorQuery.status" clearable placeholder="全部状态">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadGenerators">查询</el-button>
            <el-button :icon="Refresh" @click="resetGeneratorQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table
          class="generator-table"
          :data="generatorRows"
          v-loading="generatorLoading"
          row-key="id"
          stripe
          highlight-current-row
          @row-click="selectGenerator"
        >
        <el-table-column prop="genName" label="规则名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="genKey" label="业务Key" min-width="180" show-overflow-tooltip />
        <el-table-column prop="domainCode" label="业务域" width="120" show-overflow-tooltip />
        <el-table-column label="生效版本" width="118">
          <template #default="{ row }">
            <span v-if="row.currentRuleVersion">V{{ row.currentRuleVersion }}</span>
            <span v-else class="muted-text">未发布</span>
          </template>
        </el-table-column>
        <el-table-column label="发布变更" width="100">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.hasUnpublishedChanges"
              content="已保存但未发布，发布后才会成为业务生成使用的规则"
              placement="top"
            >
              <el-tag type="warning">未同步</el-tag>
            </el-tooltip>
            <el-tag v-else effect="plain" type="success">已同步</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" :icon="Edit" @click.stop="openGeneratorDrawer(row)">编辑</el-button>
              <el-button link type="success" :icon="Finished" @click.stop="publishCurrentVersion(row)">发布</el-button>
              <el-button link type="primary" :icon="Clock" @click.stop="openHistoryPage(row)">历史版本</el-button>
              <el-button link type="danger" :icon="Delete" @click.stop="deleteGenerator(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
        </el-table>

        <div class="pagination-row">
          <Pagination
            v-model:current-page="generatorQuery.pageNum"
            v-model:page-size="generatorQuery.pageSize"
            :total="generatorTotal"
            @change="loadGenerators"
          />
        </div>
      </section>
    </div>

    <el-dialog v-model="generatorDrawerVisible" :title="generatorForm.id ? '编辑生成器' : '新增生成器'" width="860px" destroy-on-close append-to-body>
      <el-form ref="generatorFormRef" :model="generatorForm" :rules="generatorRules" label-width="96px" class="generator-editor-block">
        <div class="base-form-stack">
          <el-form-item label="业务Key" prop="genKey">
            <el-input v-model="generatorForm.genKey" :disabled="Boolean(generatorForm.id)" placeholder="例如 ORDER_NO" />
          </el-form-item>
          <el-form-item label="名称" prop="genName">
            <el-input v-model="generatorForm.genName" placeholder="例如 订单号" />
          </el-form-item>
          <el-form-item label="业务域" prop="domainCode">
            <el-input v-model="generatorForm.domainCode" placeholder="如 NUMGEN" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="generatorForm.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
      </el-form>

      <el-tabs v-model="editorActiveTab" class="rule-tabs generator-editor-block">
        <el-tab-pane label="定义参数" name="params">
          <div class="param-editor">
            <div class="param-editor-head">
              <el-button type="primary" :icon="Plus" @click="addBusinessParamRow">添加参数</el-button>
            </div>
            <el-table :data="businessParamOptions" border>
              <el-table-column label="参数Key" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.key" placeholder="例如 orgCode" />
                </template>
              </el-table-column>
              <el-table-column label="名称" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.label" placeholder="例如 组织编码" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90">
                <template #default="{ $index }">
                  <el-button link type="danger" :icon="Delete" @click="deleteBusinessParamAt($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane name="segments">
          <template #label>
            <span class="tab-label-with-help">
              规则配置
              <el-tooltip placement="top" content="查看字符串占位符语法">
                <button class="tab-help" type="button" @click.stop="showExpressionHelp">!</button>
              </el-tooltip>
            </span>
          </template>
          <div class="dialog-segment-editor" v-loading="loadingVersionSegments">
            <div class="format-preview">
              <div>
                <span>公式预览</span>
                <strong>{{ dialogFormatPreview || '-' }}</strong>
              </div>
              <div>
                <span>示例</span>
                <strong>{{ dialogExamplePreview || '-' }}</strong>
              </div>
            </div>

            <div class="segment-strip dialog-strip" @dragover.prevent>
              <div
                v-for="segment in dialogSegmentRows"
                :key="segmentKey(segment)"
                class="segment-chip"
                :class="{ active: selectedDialogSegmentKey === segmentKey(segment) }"
                draggable="true"
                role="button"
                tabindex="0"
                @click="openSegmentPropertyDialog(segment)"
                @dragstart="onSegmentDragStart(segment)"
                @drop="onSegmentDrop(segment)"
              >
                <strong>{{ segmentPreview(segment) }}</strong>
                <small>
                  {{ segmentTitle(segment) }}
                  <em v-if="segment.sequenceScope === 1">分组</em>
                </small>
              </div>
              <button class="segment-add" type="button" @click="openSegmentPropertyDialog()">
                <el-icon><Plus /></el-icon>
                添加片段
              </button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="generatorDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingGenerator" @click="saveGenerator">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="expressionHelpVisible" title="字符串占位符语法" width="560px" append-to-body>
      <div class="expression-help">
        <p>字符串片段可以直接输入固定内容，也可以使用 <strong>${参数Key}</strong> 引用参数值。</p>
        <div>
          <span>自定义参数</span>
          <code>${orgCode}-${bizType}</code>
        </div>
        <div>
          <span>混合文本</span>
          <code>ORG-${orgCode}-SALE</code>
        </div>
        <p>未找到的参数会按空字符串处理。需要固定字符时，直接写在字符串里。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="expressionHelpVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="segmentPropertyVisible" title="片段属性" width="640px" destroy-on-close append-to-body>
      <el-form :model="segmentForm" :rules="segmentRules" label-width="86px" class="segment-property-form">
        <el-form-item label="片段名称" prop="segmentName">
          <el-input v-model="segmentForm.segmentName" placeholder="例如 前缀" />
        </el-form-item>
        <el-form-item label="片段类型" prop="segmentType">
          <div class="segment-type-picker" role="radiogroup" aria-label="片段类型">
            <button
              v-for="type in segmentTypes"
              :key="type.value"
              class="segment-type-option"
              :class="{ active: segmentForm.segmentType === type.value }"
              type="button"
              role="radio"
              :aria-checked="segmentForm.segmentType === type.value"
              @click="chooseSegmentType(type.value)"
            >
              <span>{{ type.label }}</span>
            </button>
          </div>
        </el-form-item>
        <el-form-item v-if="segmentForm.segmentType === 'TEXT'" label="字符串" prop="literalValue">
          <el-input v-model="segmentForm.literalValue" placeholder="例如 SO" />
        </el-form-item>
        <el-form-item v-if="segmentForm.segmentType === 'EXPR'" label="表达式" prop="literalValue">
          <el-input v-model="segmentForm.literalValue" placeholder="例如 SO-${orgCode}-${bizType}" />
        </el-form-item>
        <el-form-item v-if="segmentForm.segmentType === 'DATE'" label="日期格式" prop="dateFormat">
          <el-select v-model="segmentForm.dateFormat" filterable allow-create default-first-option placeholder="选择或输入日期格式">
            <el-option label="yyyyMMdd" value="yyyyMMdd" />
            <el-option label="yyyyMM" value="yyyyMM" />
            <el-option label="yyyy" value="yyyy" />
            <el-option label="yyyyMMddHHmmss" value="yyyyMMddHHmmss" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="segmentForm.segmentType === 'PARAM'" label="参数Key" prop="variableKey">
          <el-select v-model="segmentForm.variableKey" filterable placeholder="选择参数" @change="handleSegmentParamChange">
            <el-option v-for="item in businessParamOptions" :key="item.key" :label="item.label" :value="item.key" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="segmentForm.segmentType !== 'SEQ'" label="流水分组">
          <div class="scope-toggle-row">
            <el-switch
              v-model="segmentForm.sequenceScope"
              :active-value="1"
              :inactive-value="0"
              active-text="参与"
              inactive-text="不参与"
              inline-prompt
            />
            <span>{{ sequenceScopeHint }}</span>
          </div>
        </el-form-item>
        <template v-if="segmentForm.segmentType === 'SEQ'">
          <el-form-item label="流水位数" prop="seqWidth">
            <el-input-number v-model="segmentForm.seqWidth" :min="1" :max="12" />
          </el-form-item>
          <el-form-item label="补齐字符" prop="padChar">
            <el-input v-model="segmentForm.padChar" maxlength="1" placeholder="0" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="segmentPropertyVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDialogSegment">保存片段</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="previewDrawerVisible" title="试生成" size="560px" destroy-on-close>
      <el-form label-width="86px">
        <el-form-item label="生成器">
          <el-input :model-value="selectedGenerator?.genKey || ''" readonly />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="previewForm.count" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="参数">
          <el-input v-model="previewParamsText" type="textarea" :rows="7" spellcheck="false" placeholder='{"orgCode":"A1"}' />
        </el-form-item>
      </el-form>
      <div class="drawer-result">
        <strong v-for="(value, index) in previewRows" :key="`${value}-${index}`">{{ value }}</strong>
        <el-empty v-if="!previewRows.length" description="点击试生成查看结果" />
      </div>
      <template #footer>
        <el-button @click="previewDrawerVisible = false">关闭</el-button>
        <el-button type="primary" :loading="previewLoading" @click="generatePreview">试生成</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="issueDrawerVisible" title="生成编号" size="560px" destroy-on-close>
      <el-form label-width="86px">
        <el-form-item label="生成器">
          <el-input :model-value="selectedGenerator?.genKey || ''" readonly />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="issueForm.count" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="参数">
          <el-input v-model="issueParamsText" type="textarea" :rows="7" spellcheck="false" placeholder='{"orgCode":"A1","bizKey":"ORDER-1"}' />
        </el-form-item>
      </el-form>
      <div class="drawer-result issued">
        <strong v-for="(value, index) in issuedRows" :key="`${value}-${index}`">{{ value }}</strong>
        <el-empty v-if="!issuedRows.length" description="确认后生成真实编号" />
      </div>
      <template #footer>
        <el-button @click="issueDrawerVisible = false">关闭</el-button>
        <el-button type="primary" :loading="issueLoading" @click="generateIssue">确认生成</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { Pagination } from '@mango/common';
import { computed, onMounted, reactive, ref, shallowRef, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Clock, Delete, Edit, Finished, Plus, Refresh, Search } from '@element-plus/icons-vue';
import { DomainSideTree } from '@mango/system';
import {
  numgenApi,
  type ApiId,
  type NumgenGenerator,
  type NumgenGeneratorQuery,
  type NumgenGeneratorSavePayload,
  type NumgenSegment,
  type NumgenSegmentType,
  type NumgenVersion,
  type NumgenVersionState,
} from '../../api/numgen';

interface SegmentTypeOption {
  value: SegmentEditorType;
  label: string;
}

type SegmentEditorType = NumgenSegmentType | 'EXPR';

interface EditableSegment extends Omit<NumgenSegment, 'segmentType'> {
  segmentType: SegmentEditorType;
  clientKey?: string;
}

interface ParamOption {
  key: string;
  label: string;
}

const generatorRows = ref<NumgenGenerator[]>([]);
const versionRows = ref<NumgenVersion[]>([]);
const historyVersionRows = ref<NumgenVersion[]>([]);
const selectedGenerator = ref<NumgenGenerator>();
const selectedVersion = ref<NumgenVersion>();
const historyGenerator = ref<NumgenGenerator>();
const dialogSegmentRows = ref<EditableSegment[]>([]);
const historySegmentsByRuleId = ref<Record<string, string>>({});
const selectedDialogSegmentKey = ref('');
const draggingSegmentKey = ref('');
const previewRows = ref<string[]>([]);
const issuedRows = ref<string[]>([]);
const editorActiveTab = ref<'params' | 'segments'>('params');

const generatorLoading = ref(false);
const historyLoading = ref(false);
const loadingVersionSegments = ref(false);
const savingGenerator = ref(false);
const historyMode = ref(false);
const switchingVersionId = ref('');
const previewLoading = ref(false);
const issueLoading = ref(false);
const generatorTotal = ref(0);

const generatorDrawerVisible = ref(false);
const previewDrawerVisible = ref(false);
const issueDrawerVisible = ref(false);
const segmentPropertyVisible = ref(false);
const expressionHelpVisible = ref(false);

const generatorFormRef = ref<FormInstance>();
const generatorQuery = reactive<NumgenGeneratorQuery>({ pageNum: 1, pageSize: 10, keyword: '', domainCode: '', status: '' });

const generatorForm = reactive<NumgenGenerator>({
  genKey: '',
  genName: '',
  domainCode: 'NUMGEN',
  status: 1,
});

const versionForm = reactive<NumgenVersion>({
  genKey: '',
  ruleName: '',
  version: 1,
  status: 1,
  publishStatus: 0,
});

const segmentForm = reactive<EditableSegment>({
  ruleId: undefined,
  sortOrder: 1,
  segmentType: 'TEXT',
  segmentName: '',
  literalValue: '',
  variableKey: '',
  dateFormat: 'yyyyMMdd',
  seqWidth: 6,
  padChar: '0',
  sequenceScope: 0,
});

const previewForm = reactive({ count: 1 });
const issueForm = reactive({ count: 1 });
const previewParamsText = ref('{"orgCode":"A1"}');
const issueParamsText = ref('{"orgCode":"A1","bizKey":"ORDER-1"}');

const segmentTypes = shallowRef<SegmentTypeOption[]>([
  { value: 'TEXT', label: '字符串' },
  { value: 'DATE', label: '时间' },
  { value: 'PARAM', label: '业务参数' },
  { value: 'SEQ', label: '自增流水' },
  { value: 'EXPR', label: '表达式' },
]);

const businessParamOptions = ref<ParamOption[]>([]);

const activeVersion = computed(() => versionRows.value.find(item => item.versionState === 'ACTIVE' || item.publishStatus === 1));
const dialogFormatPreview = computed(() => normalizedDialogSegments()
  .map(segment => segmentPreview(segment))
  .join(''));
const dialogExamplePreview = computed(() => normalizedDialogSegments()
  .map(segment => segmentExample(segment))
  .join(''));
const sequenceScopeHint = computed(() => {
  if (segmentForm.segmentType === 'DATE') return '按日期重置流水';
  if (segmentForm.segmentType === 'PARAM') return '按参数值隔离流水';
  return '按该片段值隔离流水';
});
const generatorRules: FormRules = {
  genKey: [{ required: true, message: '请输入业务Key', trigger: 'blur' }],
  genName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  domainCode: [{ required: true, message: '请输入业务域编码', trigger: 'blur' }],
};

const segmentRules: FormRules = {
  segmentType: [{ required: true, message: '请选择片段类型', trigger: 'change' }],
  segmentName: [{ required: true, message: '请输入片段名称', trigger: 'blur' }],
};

onMounted(async () => {
  await loadGenerators();
});

async function loadGenerators() {
  generatorLoading.value = true;
  try {
    const data = await numgenApi.pageGenerators(generatorQuery);
    generatorRows.value = data.list;
    generatorTotal.value = data.total;
    if (!selectedGenerator.value && data.list.length) {
      await selectGenerator(data.list[0]);
    }
  } finally {
    generatorLoading.value = false;
  }
}

function resetGeneratorQuery() {
  generatorQuery.keyword = '';
  generatorQuery.status = '';
  generatorQuery.pageNum = 1;
  loadGenerators();
}

function handleDomainChange() {
  generatorQuery.pageNum = 1;
  selectedGenerator.value = undefined;
  selectedVersion.value = undefined;
  versionRows.value = [];
  loadGenerators();
}

function showExpressionHelp() {
  expressionHelpVisible.value = true;
}

function latestVersion(rows = versionRows.value) {
  return rows
    .slice()
    .sort((a, b) => Number(b.version || 0) - Number(a.version || 0))[0];
}

function latestDraftVersion(rows = versionRows.value) {
  return rows
    .filter(item => item.versionState === 'DRAFT')
    .sort((a, b) => Number(b.version || 0) - Number(a.version || 0))[0];
}

function preferredEditableVersion() {
  return latestDraftVersion() || activeVersion.value || latestVersion();
}

async function selectGenerator(row: NumgenGenerator) {
  selectedGenerator.value = row;
  selectedVersion.value = undefined;
  previewRows.value = [];
  issuedRows.value = [];
  await loadVersions();
}

async function loadVersions() {
  if (!selectedGenerator.value) return;
  try {
    const data = await numgenApi.pageVersions({ pageNum: 1, pageSize: 20, genKey: selectedGenerator.value.genKey });
    versionRows.value = data.list;
    const next = selectedVersion.value
      ? versionRows.value.find(item => String(item.id) === String(selectedVersion.value?.id))
      : preferredEditableVersion();
    if (next) {
      await selectVersion(next);
    } else {
      selectedVersion.value = undefined;
    }
  } finally {}
}

async function selectVersion(row: NumgenVersion) {
  selectedVersion.value = row;
}

async function openGeneratorDrawer(row?: NumgenGenerator) {
  Object.assign(generatorForm, row || {
    id: undefined,
    genKey: '',
    genName: '',
    domainCode: generatorQuery.domainCode || 'NUMGEN',
    status: 1,
  });
  resetDialogSegmentEditor();
  editorActiveTab.value = 'params';
  generatorDrawerVisible.value = true;
  if (row) {
    await selectGenerator(row);
    prepareVersionForm(preferredEditableVersion());
  } else {
    prepareVersionForm();
  }
}

async function saveGenerator() {
  await generatorFormRef.value?.validate();
  savingGenerator.value = true;
  try {
    normalizeBusinessParams();
    syncParamDefinitionsToSegments();
    const generatorPayload = buildGeneratorSavePayload();
    let savedVersionId: ApiId | undefined;
    if (generatorForm.id) {
      await numgenApi.updateGenerator(generatorPayload);
      savedVersionId = await saveVersionFromGenerator();
      await syncDialogSegments(savedVersionId);
    } else {
      await numgenApi.createGenerator(generatorPayload);
      savedVersionId = await numgenApi.createVersion({ ...versionForm, genKey: generatorForm.genKey });
      versionForm.id = savedVersionId;
      await syncDialogSegments(savedVersionId);
    }
    ElMessage.success('生成器已保存');
    generatorDrawerVisible.value = false;
    await loadGenerators();
    const current = generatorRows.value.find(item => item.genKey === generatorForm.genKey);
    if (current) {
      selectedGenerator.value = current;
      selectedVersion.value = savedVersionId ? ({ id: savedVersionId } as NumgenVersion) : undefined;
      await loadVersions();
    }
  } finally {
    savingGenerator.value = false;
  }
}

function buildGeneratorSavePayload(): NumgenGeneratorSavePayload {
  return {
    id: generatorForm.id,
    genKey: generatorForm.genKey,
    genName: generatorForm.genName,
    domainCode: generatorForm.domainCode,
    status: generatorForm.status,
  };
}

async function deleteGenerator(row: NumgenGenerator) {
  const confirmed = await confirmAction(`确认删除生成器「${row.genName}」？`, '删除确认');
  if (!confirmed) return;
  await numgenApi.deleteGenerator(row.id!);
  ElMessage.success('生成器已删除');
  if (selectedGenerator.value?.id === row.id) {
    selectedGenerator.value = undefined;
    selectedVersion.value = undefined;
    versionRows.value = [];
  }
  await loadGenerators();
}

async function publishCurrentVersion(row: NumgenGenerator) {
  const confirmed = await confirmAction(`发布「${row.genName}」的最新规则后将作为当前生效版本，是否继续？`, '发布规则');
  if (!confirmed) return;
  await numgenApi.publishGenerator(row.genKey);
  ElMessage.success('规则已发布');
  await loadGenerators();
}

async function openHistoryPage(row: NumgenGenerator) {
  historyGenerator.value = row;
  historyMode.value = true;
  await loadHistoryVersions();
}

function exitHistoryMode() {
  historyMode.value = false;
  historyGenerator.value = undefined;
  historyVersionRows.value = [];
  historySegmentsByRuleId.value = {};
}

async function loadHistoryVersions() {
  if (!historyGenerator.value) return;
  historyLoading.value = true;
  try {
    const data = await numgenApi.pageVersions({ pageNum: 1, pageSize: 100, genKey: historyGenerator.value.genKey });
    historyVersionRows.value = data.list.sort((a, b) => Number(b.version || 0) - Number(a.version || 0));
    await loadHistorySegmentPreviews(historyVersionRows.value);
  } finally {
    historyLoading.value = false;
  }
}

async function loadHistorySegmentPreviews(rows: NumgenVersion[]) {
  const previews: Record<string, string> = {};
  await Promise.all(rows.map(async row => {
    if (!row.id) return;
    const data = await numgenApi.pageSegments({ ruleId: row.id, pageSize: 50 });
    previews[String(row.id)] = data.list
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map(segment => segmentPreview({ ...segment, segmentType: inferEditorSegmentType(segment) }))
      .join('');
  }));
  historySegmentsByRuleId.value = previews;
}

function historySegmentPreview(row: NumgenVersion) {
  return row.id ? historySegmentsByRuleId.value[String(row.id)] : '';
}

async function switchToHistoryVersion(row: NumgenVersion) {
  if (!row?.id) return;
  const state = versionState(row);
  const message = state === 'DRAFT'
    ? `发布 V${row.version} 后将作为当前生效版本，是否继续？`
    : `将使用 V${row.version} 的规则内容发布为新的生效版本，流水号不会回退，是否继续？`;
  const confirmed = await confirmAction(message, state === 'DRAFT' ? '发布版本' : '切换历史版本');
  if (!confirmed) return;
  switchingVersionId.value = String(row.id);
  try {
    await numgenApi.publishVersion({ versionId: row.id });
    ElMessage.success(state === 'DRAFT' ? '版本已发布' : '历史版本已切换');
    await loadGenerators();
    if (historyGenerator.value) {
      const current = generatorRows.value.find(item => item.genKey === historyGenerator.value?.genKey);
      historyGenerator.value = current || historyGenerator.value;
    }
    await loadHistoryVersions();
  } finally {
    switchingVersionId.value = '';
  }
}

async function prepareVersionForm(row?: NumgenVersion) {
  const source = row;
  const cloningPublishedVersion = source?.versionState === 'ACTIVE' || source?.publishStatus === 1;
  const versionNumber = cloningPublishedVersion || !source?.id ? nextVersionNumber() : source.version || nextVersionNumber();
  Object.assign(versionForm, source
    ? {
        ...source,
        id: cloningPublishedVersion ? undefined : source.id,
        version: versionNumber,
        publishStatus: 0,
      }
    : {
        id: undefined,
        genKey: generatorForm.genKey,
        ruleName: generatorForm.genName || '默认规则',
        version: 1,
        status: 1,
        publishStatus: 0,
      });
  selectedDialogSegmentKey.value = '';
  await loadDialogSegments(source, cloningPublishedVersion);
}

async function saveVersionFromGenerator() {
  const payload: NumgenVersion = { ...versionForm, genKey: generatorForm.genKey };
  if (versionForm.id) {
    await numgenApi.updateVersion(payload);
    return versionForm.id;
  }
  const id = await numgenApi.createVersion(payload);
  versionForm.id = id;
  return id;
}

async function publishVersion(row?: NumgenVersion) {
  if (!row?.id) return;
  const confirmed = await confirmAction(`发布「${row.ruleName}」后将作为当前生效版本，是否继续？`, '发布版本');
  if (!confirmed) return;
  await numgenApi.publishVersion({ versionId: row.id });
  ElMessage.success('版本已发布');
  await loadVersions();
  await loadGenerators();
}

function versionState(row: NumgenVersion): NumgenVersionState {
  if (row.versionState) return row.versionState;
  return row.publishStatus === 1 ? 'ACTIVE' : 'HISTORY';
}

function versionStateLabel(row: NumgenVersion) {
  const state = versionState(row);
  if (state === 'ACTIVE') return '生效中';
  if (state === 'DRAFT') return '草稿';
  return '历史';
}

function versionStateTagType(row: NumgenVersion) {
  const state = versionState(row);
  if (state === 'ACTIVE') return 'success';
  if (state === 'DRAFT') return 'warning';
  return 'info';
}

async function confirmAction(message: string, title: string) {
  try {
    await ElMessageBox.confirm(message, title, { type: 'warning' });
    return true;
  } catch {
    return false;
  }
}

function chooseSegmentType(type: SegmentEditorType) {
  segmentForm.segmentType = type;
  if (type === 'TEXT') {
    segmentForm.segmentName ||= '字符串';
    segmentForm.literalValue ||= '';
    segmentForm.variableKey = '';
    segmentForm.dateFormat = '';
    segmentForm.seqWidth = undefined;
  }
  if (type === 'EXPR') {
    segmentForm.segmentName ||= '表达式';
    segmentForm.literalValue ||= '';
    segmentForm.variableKey = '';
    segmentForm.dateFormat = '';
    segmentForm.seqWidth = undefined;
  }
  if (type === 'DATE') {
    segmentForm.segmentName ||= '时间';
    segmentForm.dateFormat ||= 'yyyyMMdd';
    segmentForm.literalValue = '';
    segmentForm.variableKey = '';
    segmentForm.seqWidth = undefined;
  }
  if (type === 'PARAM') {
    segmentForm.segmentName ||= '业务参数';
    segmentForm.variableKey ||= businessParamOptions.value[0]?.key || '';
    segmentForm.literalValue = '';
    segmentForm.dateFormat = '';
    segmentForm.seqWidth = undefined;
  }
  if (type === 'SEQ') {
    segmentForm.segmentName ||= '自增流水';
    segmentForm.seqWidth ||= 6;
    segmentForm.padChar ||= '0';
    segmentForm.literalValue = '';
    segmentForm.variableKey = '';
    segmentForm.dateFormat = '';
    segmentForm.sequenceScope = 0;
  }
}

async function loadDialogSegments(row?: NumgenVersion, clone = false) {
  if (!row?.id) {
    dialogSegmentRows.value = [];
    return;
  }
  loadingVersionSegments.value = true;
  try {
    const data = await numgenApi.pageSegments({ ruleId: row.id, pageSize: 50 });
    dialogSegmentRows.value = data.list
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map(item => ({
        ...item,
        id: clone ? undefined : item.id,
        ruleId: clone ? undefined : item.ruleId,
        segmentType: inferEditorSegmentType(item),
        sequenceScope: item.segmentType === 'SEQ' ? 0 : item.sequenceScope || 0,
        clientKey: clone ? `clone-${item.id}-${Date.now()}` : `saved-${item.id}`,
      }));
    inferBusinessParamsFromSegments();
    selectedDialogSegmentKey.value = dialogSegmentRows.value[0] ? segmentKey(dialogSegmentRows.value[0]) : '';
  } finally {
    loadingVersionSegments.value = false;
  }
}

function openSegmentPropertyDialog(row?: EditableSegment) {
  Object.assign(segmentForm, row || newSegmentDraft());
  if (segmentForm.segmentType === 'PARAM') {
    inferBusinessParam(segmentForm.variableKey);
  }
  selectedDialogSegmentKey.value = segmentKey(segmentForm);
  segmentPropertyVisible.value = true;
}

function saveDialogSegment() {
  if (!validateSegmentForm()) return;
  if (segmentForm.segmentType === 'PARAM') {
    inferBusinessParam(segmentForm.variableKey);
  }
  const draft: EditableSegment = {
    ...segmentForm,
    clientKey: segmentForm.clientKey || `new-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  };
  const key = segmentKey(draft);
  const index = dialogSegmentRows.value.findIndex(item => segmentKey(item) === key);
  if (index >= 0) {
    dialogSegmentRows.value.splice(index, 1, draft);
  } else {
    dialogSegmentRows.value.push(draft);
  }
  normalizeDialogSort();
  selectedDialogSegmentKey.value = segmentKey(draft);
  segmentPropertyVisible.value = false;
}

function validateSegmentForm() {
  if (segmentForm.segmentType === 'TEXT' && !segmentForm.literalValue) {
    ElMessage.error('字符串不能为空');
    return false;
  }
  if (segmentForm.segmentType === 'EXPR' && !segmentForm.literalValue) {
    ElMessage.error('表达式不能为空');
    return false;
  }
  if (segmentForm.segmentType === 'DATE' && !segmentForm.dateFormat) {
    ElMessage.error('时间格式不能为空');
    return false;
  }
  if (segmentForm.segmentType === 'PARAM' && !segmentForm.variableKey) {
    ElMessage.error('参数Key不能为空');
    return false;
  }
  if (segmentForm.segmentType === 'SEQ' && !segmentForm.seqWidth) {
    ElMessage.error('流水位数不能为空');
    return false;
  }
  return true;
}

function onSegmentDragStart(segment: EditableSegment) {
  draggingSegmentKey.value = segmentKey(segment);
}

function onSegmentDrop(target: EditableSegment) {
  const sourceKey = draggingSegmentKey.value;
  const targetKey = segmentKey(target);
  if (!sourceKey || sourceKey === targetKey) return;
  const sorted = normalizedDialogSegments();
  const sourceIndex = sorted.findIndex(item => segmentKey(item) === sourceKey);
  const targetIndex = sorted.findIndex(item => segmentKey(item) === targetKey);
  if (sourceIndex < 0 || targetIndex < 0) return;
  const [source] = sorted.splice(sourceIndex, 1);
  sorted.splice(targetIndex, 0, source);
  dialogSegmentRows.value = sorted.map((item, index) => ({ ...item, sortOrder: index + 1 }));
  selectedDialogSegmentKey.value = sourceKey;
  draggingSegmentKey.value = '';
}

function handleSegmentParamChange(key: string) {
  const option = businessParamOptions.value.find(item => item.key === key);
  if (!option) return;
  segmentForm.segmentName = option.label;
}

function inferBusinessParamsFromSegments() {
  for (const segment of dialogSegmentRows.value) {
    if (segment.segmentType === 'PARAM') {
      inferBusinessParam(segment.variableKey, segment.segmentName);
    }
  }
}

function inferBusinessParam(key?: string, label?: string) {
  const value = key?.trim();
  if (!value) return;
  const name = (label || value).trim();
  const existing = businessParamOptions.value.find(item => item.key === value);
  if (existing) {
    existing.label = existing.label === existing.key ? name : existing.label;
  } else {
    businessParamOptions.value.push({ key: value, label: name });
  }
}

function addBusinessParamRow() {
  businessParamOptions.value.push({ key: '', label: '' });
}

function deleteBusinessParamAt(index: number) {
  businessParamOptions.value.splice(index, 1);
}

function normalizeBusinessParams() {
  const seen = new Set<string>();
  businessParamOptions.value = businessParamOptions.value
    .map(item => ({
      key: item.key.trim(),
      label: (item.label || item.key).trim(),
    }))
    .filter(item => {
      if (!item.key || seen.has(item.key)) return false;
      seen.add(item.key);
      return true;
    });
}

function syncParamDefinitionsToSegments() {
  const labelByKey = new Map(businessParamOptions.value.map(item => [item.key, item.label || item.key]));
  dialogSegmentRows.value = dialogSegmentRows.value.map(segment => {
    if (segment.segmentType !== 'PARAM' || !segment.variableKey) return segment;
    const label = labelByKey.get(segment.variableKey);
    return label ? { ...segment, segmentName: label } : segment;
  });
}

async function syncDialogSegments(ruleId: ApiId) {
  const previous = versionForm.id
    ? (await numgenApi.pageSegments({ ruleId, pageSize: 50 })).list
    : [];
  const next = normalizedDialogSegments();
  const nextIds = new Set(next.filter(item => item.id).map(item => String(item.id)));
  for (const item of previous) {
    if (item.id && !nextIds.has(String(item.id))) {
      await numgenApi.deleteSegment(item.id);
    }
  }
  for (const item of next) {
    const payload = stripEditableSegment({ ...item, ruleId });
    if (item.id) {
      await numgenApi.updateSegment(payload);
    } else {
      await numgenApi.createSegment(payload);
    }
  }
}

function newSegmentDraft(): EditableSegment {
  return {
    clientKey: `new-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    ruleId: versionForm.id,
    sortOrder: dialogSegmentRows.value.length + 1,
    segmentType: 'TEXT',
    segmentName: '字符串',
    literalValue: '',
    variableKey: '',
    dateFormat: 'yyyyMMdd',
    seqWidth: 6,
    padChar: '0',
    sequenceScope: 0,
  };
}

function normalizedDialogSegments() {
  return dialogSegmentRows.value
    .slice()
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map((item, index) => ({ ...item, sortOrder: index + 1 }));
}

function normalizeDialogSort() {
  dialogSegmentRows.value = normalizedDialogSegments();
}

function segmentKey(segment: EditableSegment) {
  return segment.id ? String(segment.id) : segment.clientKey || '';
}

function stripEditableSegment(segment: EditableSegment): NumgenSegment {
  const { clientKey, ...payload } = segment;
  payload.sequenceScope = payload.segmentType === 'SEQ' ? 0 : payload.sequenceScope || 0;
  return payload;
}

function resetDialogSegmentEditor() {
  dialogSegmentRows.value = [];
  businessParamOptions.value = [];
  selectedDialogSegmentKey.value = '';
  segmentPropertyVisible.value = false;
}

function defaultVersionPayload(genKey: string, version: number): NumgenVersion {
  return {
    genKey,
    ruleName: `V${version}`,
    version,
    status: 1,
    publishStatus: 0,
  };
}

async function openPreviewDrawer(row?: NumgenGenerator) {
  if (row) await selectGenerator(row);
  if (!selectedGenerator.value) return;
  previewDrawerVisible.value = true;
}

async function generatePreview() {
  if (!selectedGenerator.value) return;
  previewLoading.value = true;
  try {
    const response = await numgenApi.previewVersion({
      genKey: selectedGenerator.value.genKey,
      count: previewForm.count,
      params: parseParamsText(previewParamsText.value),
    });
    previewRows.value = response.values || [];
    ElMessage.success('预览已生成');
  } finally {
    previewLoading.value = false;
  }
}

async function openIssueDrawer(row?: NumgenGenerator) {
  if (row) await selectGenerator(row);
  if (!activeVersion.value) {
    ElMessage.warning('请先发布一个版本');
    return;
  }
  issueDrawerVisible.value = true;
}

async function generateIssue() {
  if (!selectedGenerator.value) return;
  issueLoading.value = true;
  try {
    const params = parseParamsText(issueParamsText.value);
    const values = issueForm.count === 1
      ? [await numgenApi.nextValue({ genKey: selectedGenerator.value.genKey, params })]
      : await numgenApi.batchValue({ genKey: selectedGenerator.value.genKey, count: issueForm.count, params });
    issuedRows.value = values;
    ElMessage.success('编号已生成');
  } finally {
    issueLoading.value = false;
  }
}

function parseParamsText(text: string): Record<string, unknown> {
  try {
    const parsed: unknown = JSON.parse(text || '{}');
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      ElMessage.error('参数必须是 JSON 对象');
      return {};
    }
    return parsed as Record<string, unknown>;
  } catch {
    ElMessage.error('参数不是合法 JSON');
    return {};
  }
}

function nextVersionNumber() {
  return Math.max(0, ...versionRows.value.map(item => Number(item.version || 0))) + 1;
}

function segmentTitle(segment: EditableSegment) {
  return segment.segmentName || segmentTypeLabel(segment.segmentType);
}

function segmentPreview(segment: EditableSegment) {
  if (segment.segmentType === 'TEXT') return segment.literalValue || '[字符串]';
  if (segment.segmentType === 'EXPR') return segment.literalValue || '[表达式]';
  if (segment.segmentType === 'DATE') return `{${segment.dateFormat || 'yyyyMMdd'}}`;
  if (segment.segmentType === 'PARAM') return `{${segment.variableKey || '参数'}}`;
  if (segment.segmentType === 'SEQ') return String('').padStart(segment.seqWidth || 6, segment.padChar || '0') || '{流水}';
  return '';
}

function segmentExample(segment: EditableSegment) {
  if (segment.segmentType === 'TEXT') return expressionExample(segment.literalValue);
  if (segment.segmentType === 'EXPR') return expressionExample(segment.literalValue);
  if (segment.segmentType === 'DATE') return dateExample(segment.dateFormat);
  if (segment.segmentType === 'PARAM') return paramExample(segment.variableKey);
  if (segment.segmentType === 'SEQ') return String(1).padStart(segment.seqWidth || 6, segment.padChar || '0');
  return '';
}

function dateExample(format?: string) {
  if (format === 'yyyy') return '2026';
  if (format === 'yyyyMM') return '202605';
  if (format === 'yyyyMMddHHmmss') return '20260523143059';
  return '20260523';
}

function paramExample(key?: string) {
  return key ? key.replace(/[^a-zA-Z0-9]/g, '').slice(0, 4).toUpperCase() || 'P1' : 'P1';
}

function expressionExample(expression?: string) {
  return (expression || '').replace(/\$\{([^}]+)}/g, (_, key: string) => paramExample(key.trim()));
}

function inferEditorSegmentType(segment: NumgenSegment): SegmentEditorType {
  return segment.segmentType;
}

function segmentTypeLabel(type: SegmentEditorType) {
  return segmentTypes.value.find(item => item.value === type)?.label || type;
}

</script>

<style scoped lang="scss">
.numgen-page {
  min-height: 100%;
  padding: 0;
  min-width: 0;
}

.numgen-list-layout {
  display: grid;
  grid-template-columns: minmax(220px, 260px) minmax(0, 1fr);
  gap: 12px;
  min-height: calc(100vh - 136px);
}

.numgen-rule-panel {
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  min-width: 0;
  overflow: hidden;
}

.numgen-table-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.numgen-table-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
  line-height: 1.35;
  color: var(--el-text-color-primary);
}

.numgen-table-head p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.numgen-head-actions,
.table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.table-actions {
  gap: 10px;
  white-space: nowrap;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
}

.search-form {
  margin-bottom: 2px;
}

.search-form :deep(.el-input),
.search-form :deep(.el-select) {
  width: 220px;
}

.numgen-rule-panel :deep(.el-table) {
  width: 100%;
}

.numgen-rule-panel :deep(.el-table__body-wrapper),
.numgen-rule-panel :deep(.el-table__header-wrapper) {
  min-width: 0;
}

.muted-text {
  color: var(--el-text-color-secondary);
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

.segment-strip {
  min-height: 128px;
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding: 12px;
  border: 1px dashed var(--el-border-color);
  background: var(--el-fill-color-light);
}

.segment-chip,
.segment-add {
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  color: inherit;
  cursor: pointer;
}

.segment-chip {
  flex: 0 0 auto;
  min-width: max-content;
  max-width: 240px;
  min-height: 58px;
  padding: 8px 10px;
  display: grid;
  gap: 8px;
  place-content: center;
  text-align: center;
  position: relative;
}

.segment-chip:hover,
.segment-chip.active {
  border-color: var(--el-color-primary);
}

.segment-chip span,
.segment-chip small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.segment-chip small em {
  margin-left: 5px;
  padding: 1px 5px;
  border-radius: 999px;
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning-dark-2);
  font-style: normal;
  font-size: 11px;
}

.segment-chip strong {
  font-size: 15px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.segment-add {
  flex: 0 0 132px;
  display: grid;
  place-items: center;
  gap: 6px;
  color: var(--el-color-primary);
  font-weight: 600;
}

.format-preview {
  margin-top: 12px;
  padding: 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.format-preview > div {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.format-preview span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.format-preview strong {
  font-size: 20px;
  line-height: 1.35;
  word-break: break-all;
}

.form-grid,
.segment-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.generator-editor-block {
  padding: 0 4px;
}

.base-form-stack {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.base-form-stack :deep(.el-form-item:nth-child(3)) {
  grid-column: 1;
}

.dialog-segment-editor {
  margin-top: 0;
}

.rule-tabs {
  margin-top: 8px;
}

.tab-label-with-help {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.tab-help {
  width: 16px;
  height: 16px;
  padding: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 50%;
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 14px;
  cursor: pointer;
}

.tab-help:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.param-editor-head {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.expression-help {
  display: grid;
  gap: 12px;
}

.expression-help p {
  margin: 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.expression-help div {
  display: grid;
  gap: 4px;
}

.expression-help span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.expression-help code {
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
  word-break: break-all;
}

.segment-type-picker {
  width: fit-content;
  max-width: 100%;
  display: inline-grid;
  grid-auto-flow: column;
  grid-auto-columns: max-content;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}

.segment-type-option {
  min-width: 0;
  height: 30px;
  padding: 0 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-right: 1px solid var(--el-border-color);
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-regular);
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}

.segment-type-option:last-child {
  border-right: 0;
}

.segment-type-option:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.segment-type-option:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
}

.segment-type-option.active {
  background: var(--el-color-primary);
  color: var(--el-color-white);
  font-weight: 600;
}

.scope-toggle-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.scope-toggle-row span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.dialog-strip {
  min-height: 74px;
  padding: 8px;
  gap: 6px;
}

.dialog-strip .segment-chip {
  flex-basis: auto;
  min-width: max-content;
  max-width: 220px;
  min-height: 52px;
  padding: 8px 6px;
  gap: 4px;
}

.dialog-strip .segment-chip strong {
  font-size: 13px;
}

.dialog-strip .segment-add {
  flex-basis: 104px;
  font-size: 13px;
}

.segment-form-grid {
  column-gap: 12px;
}

.segment-form-grid :deep(.el-form-item) {
  margin-bottom: 12px;
}

.dialog-segment-editor .format-preview {
  margin-top: 8px;
  padding: 10px 12px;
}

.dialog-segment-editor .format-preview strong {
  font-size: 15px;
}

.drawer-result {
  min-height: 160px;
  margin-top: 12px;
  padding: 12px;
  display: grid;
  align-content: start;
  gap: 8px;
  border: 1px dashed var(--el-border-color);
}

.drawer-result strong {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
  word-break: break-all;
}

.drawer-result.issued strong {
  border-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
}

@media (max-width: 960px) {
  .numgen-list-layout {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
  }

  .format-preview {
    grid-template-columns: 1fr;
  }
}
</style>
