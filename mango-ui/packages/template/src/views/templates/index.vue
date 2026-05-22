<template>
  <div class="template-container">
    <el-card v-if="pageMode === 'list'" class="template-main">
      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索模板编码或名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryCode" placeholder="全部分类" clearable filterable style="width: 150px">
            <el-option
              v-for="item in categoryOptions"
              :key="item.categoryCode"
              :label="item.categoryName"
              :value="item.categoryCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="格式">
          <DictSelect
            v-model="query.sourceFormat"
            dict-type="template_source_format"
            placeholder="全部格式"
            clearable
            style="width: 130px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="handleCreate">新增模板</el-button>
          <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">批量删除</el-button>
          <el-button @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        class="data-table"
        stripe
        @selection-change="handleSelectionChange"
      >
        <template #empty>
          <el-empty description="暂无模板">
            <el-button type="primary" @click="handleCreate">新增模板</el-button>
          </el-empty>
        </template>
        <el-table-column type="selection" width="48" />
        <el-table-column label="模板" min-width="260" fixed="left">
          <template #default="{ row }">
            <div class="template-cell">
              <strong>{{ row.templateCode }}</strong>
              <span>{{ row.templateName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.categoryName || row.categoryCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sourceFormat" label="模板格式" width="120">
          <template #default="{ row }">
            <DictTag v-if="row.sourceFormat" dict-code="template_source_format" :value="row.sourceFormat" size="small" />
            <el-tag v-else size="small" type="info">未发布</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布变更" width="110">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.hasUnpublishedChanges"
              :content="`已保存但未发布：${(row.unpublishedChangeReasons || []).join('、') || '模板内容'}`"
              placement="top"
            >
              <el-tag type="warning">未同步</el-tag>
            </el-tooltip>
            <el-tag v-else effect="plain" type="success">已同步</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishedVersionNo" label="生效版本" width="110">
          <template #default="{ row }">
            <el-button v-if="row.publishedVersionNo" link type="primary" @click="handleVersions(row)">
              V{{ row.publishedVersionNo }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="updatedTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" :disabled="!row.publishedVersionNo" @click="handleVersions(row)">历史版本</el-button>
            <el-button link type="success" size="small" :disabled="!row.publishedVersionNo" @click="handlePreview(row)">预览</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadData"
      />
    </el-card>

    <el-card v-else-if="pageMode === 'preview'" class="template-main page-card preview-page">
      <div class="page-header">
        <div>
          <h1>模板预览</h1>
          <span>{{ currentTemplate?.templateCode || '-' }}</span>
        </div>
        <div class="page-actions">
          <el-button @click="backToList">返回</el-button>
          <el-button type="primary" :loading="previewLoading" @click="submitRender">生成预览</el-button>
        </div>
      </div>

      <div class="preview-layout">
        <aside class="preview-version-rail">
          <div class="section-title">历史版本</div>
          <div class="preview-version-list">
            <button
              v-for="item in detail?.versions || []"
              :key="item.id"
              class="preview-version-item"
              :class="{ active: previewingTemplateVersion?.versionNo === item.versionNo }"
              type="button"
              @click="switchPreviewVersion(item)"
            >
              <span class="preview-version-head">
                <strong>V{{ item.versionNo }}</strong>
                <el-tag v-if="item.currentPublished === 1" size="small" type="success">生效</el-tag>
                <el-tag v-else size="small" effect="plain" type="info">历史</el-tag>
              </span>
              <span class="preview-version-remark">{{ item.versionRemark || '无说明' }}</span>
            </button>
            <el-empty v-if="!detail?.versions?.length" :image-size="64" description="暂无版本" />
          </div>
        </aside>

        <aside class="preview-side">
          <section class="form-section">
            <div class="section-title">基础信息</div>
            <el-descriptions v-if="detail" :column="1" border>
              <el-descriptions-item label="模板编码">{{ detail.templateCode }}</el-descriptions-item>
              <el-descriptions-item label="模板名称">{{ detail.templateName }}</el-descriptions-item>
              <el-descriptions-item label="模板分类">{{ detail.categoryName || detail.categoryCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="预览版本">{{ previewingTemplateVersion ? `V${previewingTemplateVersion.versionNo}` : '-' }}</el-descriptions-item>
              <el-descriptions-item label="模板格式">
                <DictTag
                  v-if="previewingTemplateVersion?.sourceFormat || detail.sourceFormat"
                  dict-code="template_source_format"
                  :value="previewingTemplateVersion?.sourceFormat || detail.sourceFormat"
                  size="small"
                />
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="form-section">
          <div class="section-title">参数输入</div>
          <el-form label-width="88px">
            <el-form-item label="输出格式">
              <DictSelect v-model="renderForm.outputFormat" class="form-select" dict-type="template_output_format" />
            </el-form-item>
            <el-form-item label="变量参数">
                <el-tabs v-model="renderVariableMode" class="stable-tabs render-tabs">
                  <el-tab-pane label="表单填写" name="FORM">
                    <el-empty v-if="renderVariableRows.length === 0" description="生效版本未定义变量" />
                    <div v-else class="render-variable-form">
                      <div v-for="item in renderVariableRows" :key="item.id" class="render-variable-item">
                        <div class="render-variable-label">
                          <span>{{ item.label || item.name }}</span>
                          <em v-if="item.required !== false">*</em>
                          <small>{{ item.name }}</small>
                        </div>
                        <el-input-number
                          v-if="item.type === 'NUMBER'"
                          v-model="item.value"
                          class="render-variable-control"
                          controls-position="right"
                        />
                        <el-switch v-else-if="item.type === 'BOOLEAN'" v-model="item.value" />
                        <el-date-picker
                          v-else-if="item.type === 'DATE'"
                          v-model="item.value"
                          class="render-variable-control"
                          type="date"
                          value-format="YYYY-MM-DD"
                        />
                        <el-input
                          v-else-if="item.type === 'OBJECT' || item.type === 'ARRAY'"
                          v-model="item.value"
                          class="render-variable-control"
                          type="textarea"
                          :rows="3"
                          :placeholder="item.type === 'ARRAY' ? '[...]' : '{...}'"
                        />
                        <el-input
                          v-else
                          v-model="item.value"
                          class="render-variable-control"
                          :placeholder="item.example || ''"
                        />
                        <div v-if="item.description" class="render-variable-desc">{{ item.description }}</div>
                      </div>
                    </div>
                  </el-tab-pane>
                  <el-tab-pane label="JSON 输入" name="JSON">
                    <el-input v-model="renderVariablesText" type="textarea" :rows="14" />
                  </el-tab-pane>
                </el-tabs>
              </el-form-item>
            </el-form>
          </section>
        </aside>

        <section class="preview-main">
          <div class="section-title">{{ renderResultTitle }}</div>
          <div class="preview-result">
            <el-empty v-if="!renderResult" description="填写参数后点击生成预览" />
            <el-alert
              v-else-if="renderResult.status === 'FAILED'"
              :title="renderResult.errorMessage || '预览失败'"
              type="error"
              show-icon
              :closable="false"
            />
            <iframe
              v-else-if="renderForm.outputFormat === 'HTML' || previewingTemplateVersion?.sourceFormat === 'HTML'"
              class="html-preview preview-html"
              title="预览内容"
              :srcdoc="renderResult.content || ''"
              sandbox=""
            />
            <div v-else-if="isFileRenderResult" class="render-file-result">
              <div class="render-file-icon">{{ renderFileExt }}</div>
              <div class="render-file-meta">
                <strong>{{ renderResult.fileName || renderDefaultFileName }}</strong>
                <span>文件ID：{{ renderResult.fileId }}</span>
                <span>{{ renderFileDescription }}</span>
              </div>
              <div class="render-file-actions">
                <el-button type="primary" @click="downloadRenderFile">下载文件</el-button>
                <el-button @click="openRenderRecord">查看渲染记录</el-button>
              </div>
            </div>
            <pre v-else>{{ renderResult.content || (renderResult.fileId ? `文件ID：${renderResult.fileId}` : '') }}</pre>
          </div>
        </section>
      </div>
    </el-card>

    <el-card v-else-if="pageMode === 'versions'" class="template-main page-card">
      <div class="page-header">
        <div>
          <h1>历史版本</h1>
          <span>{{ detail?.templateCode || '-' }}</span>
        </div>
        <div class="page-actions">
          <el-button @click="backToList">返回</el-button>
          <el-button v-if="currentTemplate" type="primary" @click="handleEdit(currentTemplate)">编辑模板</el-button>
        </div>
      </div>

      <el-table :data="detail?.versions || []" stripe height="calc(100vh - 230px)" empty-text="暂无版本">
        <el-table-column prop="versionNo" label="版本" width="90">
          <template #default="{ row }">V{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column prop="sourceFormat" label="模板格式" width="120">
          <template #default="{ row }">
            <DictTag v-if="row.sourceFormat" dict-code="template_source_format" :value="row.sourceFormat" size="small" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentPublished" label="生效版本" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.currentPublished === 1" size="small" type="success">生效中</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="versionRemark" label="说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.versionRemark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="previewVersion(row)">预览</el-button>
            <el-button
              link
              type="success"
              size="small"
              :disabled="row.currentPublished === 1"
              @click="activateVersion(row)"
            >
              设为生效
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-else class="template-main page-card">
      <div class="page-header">
        <div>
          <h1>模板维护</h1>
          <span>{{ templateForm.id ? templateForm.templateCode : '新增模板' }}</span>
        </div>
        <div class="page-actions">
          <el-button @click="backToList">返回</el-button>
          <el-button @click="submitTemplate">保存</el-button>
          <el-button type="primary" @click="publishMaintenanceTemplate">发布</el-button>
        </div>
      </div>

      <el-form ref="templateFormRef" class="maintain-form" :model="templateForm" :rules="templateRules" label-width="96px">
          <section class="form-section">
            <div class="section-title">基础信息</div>
            <div class="form-grid">
              <el-form-item label="模板编码" prop="templateCode">
                <el-input v-model="templateForm.templateCode" :disabled="!!templateForm.id" placeholder="如 CONTRACT_NOTICE" />
              </el-form-item>
              <el-form-item label="模板名称" prop="templateName">
                <el-input v-model="templateForm.templateName" placeholder="如 合同到期提醒" />
              </el-form-item>
              <el-form-item label="模板分类">
                <el-select v-model="templateForm.categoryCode" class="form-select" placeholder="可选，用于列表分组" clearable filterable @change="handleCategoryChange">
                  <el-option
                    v-for="item in categoryOptions"
                    :key="item.categoryCode"
                    :label="item.categoryName"
                    :value="item.categoryCode"
                  />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item label="备注">
              <el-input v-model="templateForm.remark" type="textarea" :rows="3" placeholder="记录适用场景、调用方或维护说明" />
            </el-form-item>
          </section>

          <section class="form-section">
            <div class="section-title">
              <span>模板内容</span>
              <div class="section-actions">
                <el-button size="small" @click="resetVersionDraft">清空内容</el-button>
              </div>
            </div>
            <div class="maintain-editor-layout">
              <aside class="variable-pane">
                <div class="variable-toolbar">
                  <div class="variable-toolbar-meta">
                    <span>变量定义</span>
                    <el-tag size="small" effect="plain">{{ versionForm.variables.length }} 个</el-tag>
                  </div>
                  <div class="variable-toolbar-actions">
                    <el-button :icon="MagicStick" @click="extractVariables">提取变量</el-button>
                    <el-button type="primary" :icon="Plus" @click="addVariable()">新增变量</el-button>
                  </div>
                </div>
                <el-table
                  class="variable-tree-table"
                  :data="versionForm.variables"
                  row-key="id"
                  size="small"
                  height="420"
                  default-expand-all
                  :indent="16"
                  :tree-props="{ children: 'children' }"
                >
                  <el-table-column prop="name" label="字段" min-width="220">
                    <template #default="{ row, $index }">
                      <div class="field-name-cell">
                        <div class="field-inline-actions">
                          <el-tooltip v-if="isContainerVariable(row)" content="添加子字段" placement="top">
                            <el-button
                              link
                              type="primary"
                              :icon="Plus"
                              aria-label="添加子字段"
                              @click="addChildVariable(row)"
                            />
                          </el-tooltip>
                          <el-tooltip content="删除" placement="top">
                            <el-button
                              link
                              type="danger"
                              :icon="Delete"
                              aria-label="删除"
                              @click="removeVariable(row, $index)"
                            />
                          </el-tooltip>
                        </div>
                        <el-input v-model="row.name" placeholder="字段名" />
                        <el-popover placement="right" trigger="click" width="220">
                          <template #reference>
                            <el-button
                              class="field-label-button"
                              link
                              type="primary"
                              :icon="EditPen"
                              aria-label="编辑标签"
                            />
                          </template>
                          <div class="field-label-popover">
                            <span>字段标签</span>
                            <el-input v-model="row.label" placeholder="如 客户名称" clearable />
                          </div>
                        </el-popover>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="type" label="类型" width="100">
                    <template #default="{ row }">
                      <el-select v-model="row.type" @change="handleVariableTypeChange(row)">
                        <el-option
                          v-for="item in variableTypeOptions"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column prop="required" label="必填" width="70">
                    <template #default="{ row }"><el-switch v-model="row.required" /></template>
                  </el-table-column>
                </el-table>
              </aside>

              <section class="content-pane">
                <div class="content-format-bar">
                  <span>模板类型</span>
                  <el-radio-group v-model="versionForm.sourceFormat">
                    <el-radio-button
                      v-for="item in sourceFormatOptions"
                      :key="item.value"
                      :label="item.value"
                    >
                      {{ item.label }}
                    </el-radio-button>
                  </el-radio-group>
                </div>
                <div v-if="isTextTemplate" class="content-editor">
                  <CodeEditor
                    v-if="versionForm.sourceFormat === 'TEXT'"
                    v-model="versionForm.content"
                    language="markdown"
                    height="420px"
                  />
                  <Editor
                    v-else
                    v-model="versionForm.content"
                    height="420px"
                    mode="default"
                  />
                </div>
                <div v-else class="document-template-panel">
                  <div class="document-upload-area">
                    <MUpload
                      v-model="versionFileValue"
                      value-type="record"
                      :fmt="documentFormat"
                      button-text="上传模板文件"
                      biz-type="template-source"
                      display="drag"
                      @success="handleTemplateFileSuccess"
                      @change="handleTemplateFileChange"
                    />
                  </div>
                  <div v-if="versionForm.sourceFileId" class="document-preview-card">
                    <FilePreviewPanel :file-id="versionForm.sourceFileId" />
                  </div>
                  <el-empty v-else class="document-empty" description="上传 Word 或 Excel 模板文件后显示预览" />
                </div>
              </section>
            </div>
          </section>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Delete, EditPen, MagicStick, Plus } from '@element-plus/icons-vue';
import { CodeEditor, DictSelect, DictTag, Editor, Pagination, useDict } from '@mango/common';
import { fileApi, FilePreviewPanel, MUpload, type FileRecord } from '@mango/file';
import {
  templateApi,
  templateCategoryApi,
  type JsonObject,
  type JsonValue,
  type SaveTemplatePayload,
  type TemplateCategory,
  type TemplateDetail,
  type TemplateItem,
  type TemplateOutputFormat,
  type TemplateQuery,
  type TemplateRenderResult,
  type TemplateSourceFormat,
  type TemplateVariableDefinition,
  type TemplateVersion,
} from '../../api/template';

type VariableRow = TemplateVariableDefinition & { id: string };
type VariableValidationError = {
  path: string;
  message: string;
};
type RenderVariableRow = TemplateVariableDefinition & {
  id: string;
  value: JsonValue | undefined;
};

const loading = ref(false);
const tableData = ref<TemplateItem[]>([]);
const categoryOptions = ref<TemplateCategory[]>([]);
const selectedRows = ref<TemplateItem[]>([]);
const total = ref(0);
const pageMode = ref<'list' | 'maintain' | 'preview' | 'versions'>('list');
const detail = ref<TemplateDetail | null>(null);
const currentTemplate = ref<TemplateItem | null>(null);
const previewingTemplateVersion = ref<TemplateVersion | undefined>();
const templateFormRef = ref<FormInstance>();
const renderVariableMode = ref<'FORM' | 'JSON'>('FORM');
const versionFileValue = ref<FileRecord | null>(null);
const renderVariablesText = ref('{}');
const renderVariableRows = ref<RenderVariableRow[]>([]);
const renderResult = ref<TemplateRenderResult | null>(null);
const previewLoading = ref(false);
let variableSeed = 0;

const variableTypeOptions = [
  { label: '文本', value: 'STRING' },
  { label: '数字', value: 'NUMBER' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: '对象', value: 'OBJECT' },
  { label: '数组', value: 'ARRAY' },
  { label: '日期', value: 'DATE' },
] as const;
const query = reactive<TemplateQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryCode: '',
  sourceFormat: '',
});

const templateForm = reactive<SaveTemplatePayload>({
  templateCode: '',
  templateName: '',
  categoryCode: '',
  categoryName: '',
  sourceFormat: undefined,
  remark: '',
});

const versionForm = reactive<{
  sourceFormat: TemplateSourceFormat;
  content: string;
  sourceFileId?: number;
  versionRemark: string;
  variables: VariableRow[];
}>({
  sourceFormat: 'TEXT',
  content: '',
  sourceFileId: undefined,
  versionRemark: '',
  variables: [],
});

const renderForm = reactive<{
  outputFormat: TemplateOutputFormat;
  versionNo?: number;
  async: boolean;
}>({
  outputFormat: 'TEXT',
  versionNo: undefined,
  async: false,
});

const templateRules: FormRules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
};

const { options: sourceDictOptions } = useDict('template_source_format');
const sourceFormatOptions = computed(() => sourceDictOptions.value
  .filter((item) => ['TEXT', 'HTML', 'DOCX', 'XLSX'].includes(String(item.value)))
  .map((item) => ({ label: item.label, value: item.value as TemplateSourceFormat })));
const isTextTemplate = computed(() => versionForm.sourceFormat === 'TEXT' || versionForm.sourceFormat === 'HTML');
const documentFormat = computed(() => versionForm.sourceFormat.toLowerCase());
const isFileRenderResult = computed(() => Boolean(renderResult.value?.fileId) && !renderResult.value?.content);
const renderFileExt = computed(() => {
  const source = renderResult.value?.fileName || renderForm.outputFormat || '';
  const ext = source.includes('.') ? source.split('.').pop() : source;
  return (ext || 'FILE').toUpperCase();
});
const renderDefaultFileName = computed(() => `${currentTemplate.value?.templateCode || 'template-render'}.${renderFileExt.value.toLowerCase()}`);
const renderResultTitle = computed(() => (isFileRenderResult.value ? '生成文件结果' : '预览内容'));
const renderFileDescription = computed(() => `${renderForm.outputFormat} 文件已生成，可下载后查看完整内容。`);
onMounted(() => {
  loadCategories();
  loadData();
});

watch(renderVariableMode, (mode) => {
  if (mode === 'JSON') {
    syncRenderJsonFromForm();
  }
});

watch(() => versionForm.sourceFormat, (format, previous) => {
  if (!previous || format === previous) return;
  if (format === 'TEXT' || format === 'HTML') {
    versionForm.sourceFileId = undefined;
    versionFileValue.value = null;
    return;
  }
  versionForm.content = '';
});

async function loadCategories() {
  categoryOptions.value = await templateCategoryApi.list();
}

async function loadData() {
  loading.value = true;
  try {
    const result = await templateApi.page(query);
    tableData.value = result.list;
    total.value = result.total;
    selectedRows.value = [];
  } finally {
    loading.value = false;
  }
}

function handleSelectionChange(rows: TemplateItem[]) {
  selectedRows.value = rows;
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    categoryCode: '',
    sourceFormat: '',
  });
  loadData();
}

async function handleDelete(row: TemplateItem) {
  await ElMessageBox.confirm(`确认删除模板 ${row.templateCode}？删除后会同步清理版本和渲染记录。`, '删除模板', {
    type: 'warning',
  });
  await templateApi.delete(row.id);
  ElMessage.success('模板已删除');
  loadData();
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) return;
  await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 个模板？删除后会同步清理版本和渲染记录。`, '批量删除模板', {
    type: 'warning',
  });
  await Promise.all(selectedRows.value.map((item) => templateApi.delete(item.id)));
  ElMessage.success('模板已批量删除');
  loadData();
}

function handleCreate() {
  currentTemplate.value = null;
  detail.value = null;
  Object.assign(templateForm, {
    id: undefined,
    templateCode: '',
    templateName: '',
    categoryCode: '',
    categoryName: '',
    sourceFormat: undefined,
    remark: '',
  });
  resetVersionDraft();
  pageMode.value = 'maintain';
}

async function handleEdit(row: TemplateItem) {
  currentTemplate.value = row;
  Object.assign(templateForm, {
    id: row.id,
    templateCode: row.templateCode,
    templateName: row.templateName,
    categoryCode: row.categoryCode || '',
    categoryName: row.categoryName || '',
    sourceFormat: row.sourceFormat,
    remark: row.remark || '',
  });
  detail.value = await templateApi.detail(row.id);
  loadDraftOrVersionToForm(detail.value);
  pageMode.value = 'maintain';
}

async function handleVersions(row: TemplateItem) {
  currentTemplate.value = row;
  detail.value = await templateApi.detail(row.id);
  pageMode.value = 'versions';
}

function previewVersion(row: TemplateVersion) {
  if (!currentTemplate.value) return;
  handlePreview(currentTemplate.value, row);
}

function handleCategoryChange(value?: string | number | Array<string | number>) {
  const categoryCode = Array.isArray(value) ? value[0] : value;
  const category = categoryOptions.value.find((item) => item.categoryCode === categoryCode);
  templateForm.categoryName = category?.categoryName || '';
}

async function submitTemplate() {
  const creating = !templateForm.id;
  const saved = await saveTemplateBase();
  currentTemplate.value = saved;
  ElMessage.success(creating ? '模板已创建，发布后生效' : '保存成功，修改未同步');
  await loadData();
  pageMode.value = 'list';
}

function backToList() {
  pageMode.value = 'list';
}

async function saveTemplateBase(): Promise<TemplateItem> {
  await templateFormRef.value?.validate();
  if (!validateVariableSchema()) {
    throw new Error('变量定义不完整');
  }
  if (templateForm.id) {
    await templateApi.update(templateSavePayload());
    return {
      id: templateForm.id,
      templateCode: templateForm.templateCode,
      templateName: templateForm.templateName,
      categoryCode: templateForm.categoryCode,
      categoryName: templateForm.categoryName,
      sourceFormat: versionForm.sourceFormat,
      currentVersionNo: currentTemplate.value?.currentVersionNo || 0,
      publishedVersionNo: currentTemplate.value?.publishedVersionNo || currentTemplate.value?.currentVersionNo || 0,
      hasUnpublishedChanges: hasUnpublishedDraft(),
      unpublishedChangeReasons: hasUnpublishedDraft() ? ['模板内容'] : [],
      status: currentTemplate.value?.status || 1,
      remark: templateForm.remark,
    };
  }
  const id = await templateApi.create(templateSavePayload());
  templateForm.id = id;
  return {
    id,
    templateCode: templateForm.templateCode,
    templateName: templateForm.templateName,
    categoryCode: templateForm.categoryCode,
    categoryName: templateForm.categoryName,
    sourceFormat: versionForm.sourceFormat,
    currentVersionNo: 0,
    publishedVersionNo: 0,
    hasUnpublishedChanges: hasUnpublishedDraft(),
    unpublishedChangeReasons: hasUnpublishedDraft() ? ['模板内容'] : [],
    status: 1,
    remark: templateForm.remark,
  };
}

function currentVersion(value: TemplateDetail | null): TemplateVersion | undefined {
  return value?.versions?.find((item) => item.currentPublished === 1) || value?.versions?.[0];
}

function loadDraftOrVersionToForm(value: TemplateDetail | null) {
  if (value?.hasUnpublishedChanges || value?.draftContent || value?.draftSourceFileId || value?.draftVariables?.length) {
    Object.assign(versionForm, {
      sourceFormat: value.draftSourceFormat || value.sourceFormat || 'TEXT',
      content: value.draftContent || '',
      sourceFileId: value.draftSourceFileId,
      versionRemark: '',
      variables: normalizeVariables(value.draftVariables || []),
    });
    versionFileValue.value = value.draftSourceFileId ? {
      id: value.draftSourceFileId,
      name: `file-${value.draftSourceFileId}`,
      url: `mango-file:${value.draftSourceFileId}`,
      fileName: `file-${value.draftSourceFileId}`,
      fileSize: 0,
    } : null;
    return;
  }
  loadVersionToDraft(currentVersion(value), false);
}

function templateSavePayload(): SaveTemplatePayload {
  return {
    id: templateForm.id,
    templateCode: templateForm.templateCode,
    templateName: templateForm.templateName,
    categoryCode: templateForm.categoryCode,
    categoryName: templateForm.categoryName,
    sourceFormat: versionForm.sourceFormat,
    draftContent: versionForm.content,
    draftSourceFileId: versionForm.sourceFileId,
    draftVariables: cleanVariables(versionForm.variables),
    remark: templateForm.remark,
  };
}

function hasUnpublishedDraft() {
  return Boolean(
    versionForm.sourceFormat
      || versionForm.content?.trim()
      || versionForm.sourceFileId
      || cleanVariables(versionForm.variables).length,
  );
}

function resetVersionDraft() {
  Object.assign(versionForm, {
    sourceFormat: currentTemplate.value?.sourceFormat || 'TEXT',
    content: '',
    sourceFileId: undefined,
    versionRemark: '',
    variables: [],
  });
  versionFileValue.value = null;
  syncVariableJsonFromStruct();
}

function loadVersionToDraft(row?: TemplateVersion, notify = true) {
  Object.assign(versionForm, {
    sourceFormat: row?.sourceFormat || currentTemplate.value?.sourceFormat || 'TEXT',
    content: row?.content || '',
    sourceFileId: row?.sourceFileId,
    versionRemark: row ? `基于 V${row.versionNo} 调整` : '',
    variables: normalizeVariables(row?.variables || []),
  });
  versionFileValue.value = row?.sourceFileId ? {
    id: row.sourceFileId,
    name: `file-${row.sourceFileId}`,
    url: `mango-file:${row.sourceFileId}`,
    fileName: `file-${row.sourceFileId}`,
    fileSize: 0,
  } : null;
  syncVariableJsonFromStruct();
  if (notify && row) {
    ElMessage.success(`已载入 V${row.versionNo} 副本`);
  }
}

async function publishMaintenanceTemplate() {
  const saved = await saveTemplateBase();
  currentTemplate.value = saved;
  await publishVersion();
  pageMode.value = 'list';
}

async function publishVersion() {
  if (!currentTemplate.value) return;
  if (isTextTemplate.value && !versionForm.content?.trim()) {
    ElMessage.error('请填写模板内容');
    return;
  }
  if (!isTextTemplate.value && !versionForm.sourceFileId) {
    ElMessage.error('请上传模板文件');
    return;
  }
  await templateApi.publishVersion({
    templateId: currentTemplate.value.id,
    sourceFormat: versionForm.sourceFormat,
    content: versionForm.content,
    sourceFileId: versionForm.sourceFileId,
    versionRemark: versionForm.versionRemark,
    variables: cleanVariables(versionForm.variables),
  });
  ElMessage.success('发布成功');
  await refreshVersionDetail();
  loadData();
}

async function activateVersion(row: TemplateVersion) {
  if (!currentTemplate.value) return;
  await ElMessageBox.confirm(`确认将 V${row.versionNo} 设为生效版本？后续不指定版本的渲染都会使用该版本。`, '设置生效版本');
  await templateApi.activateVersion({ templateId: currentTemplate.value.id, versionNo: row.versionNo });
  ElMessage.success(`V${row.versionNo} 已设为生效版本`);
  await refreshVersionDetail();
  loadData();
}

async function refreshVersionDetail() {
  if (!currentTemplate.value) return;
  detail.value = await templateApi.detail(currentTemplate.value.id);
  const latestRow = tableData.value.find((item) => item.id === currentTemplate.value?.id);
  if (latestRow && detail.value) {
    Object.assign(latestRow, detail.value);
    currentTemplate.value = latestRow;
  }
}

async function extractVariables() {
  const names = await templateApi.extractVariables({
    sourceFormat: versionForm.sourceFormat,
    content: versionForm.content,
    sourceFileId: versionForm.sourceFileId,
  });
  const existing = new Set(versionForm.variables.map((item) => item.name));
  const additions = names.filter((name) => name && !existing.has(name));
  additions.forEach((name) => {
    versionForm.variables.push(createVariable({ name, label: name, required: false }));
  });
  if (names.length === 0) {
    ElMessage.warning('未从当前模板内容中识别到变量');
    return;
  }
  if (additions.length === 0) {
    ElMessage.info('识别到的变量已在列表中');
    return;
  }
  syncVariableJsonFromStruct();
  ElMessage.success(`已新增 ${additions.length} 个变量建议`);
}

function handleTemplateFileSuccess(file: FileRecord) {
  versionForm.sourceFileId = file.id;
  versionFileValue.value = file;
}

function handleTemplateFileChange(value: string | string[] | FileRecord | FileRecord[] | null | undefined) {
  const file = Array.isArray(value) ? value[0] : value;
  if (!file) {
    versionForm.sourceFileId = undefined;
    versionFileValue.value = null;
    return;
  }
  if (typeof file === 'string') {
    const id = Number(file.replace('mango-file:', ''));
    versionForm.sourceFileId = Number.isFinite(id) && id > 0 ? id : undefined;
    return;
  }
  versionForm.sourceFileId = file.id;
  versionFileValue.value = file;
}

async function handlePreview(row: TemplateItem, version?: TemplateVersion) {
  currentTemplate.value = row;
  detail.value = await templateApi.detail(row.id);
  const target = version || currentVersion(detail.value);
  applyPreviewVersion(target);
  pageMode.value = 'preview';
}

function switchPreviewVersion(row: TemplateVersion) {
  applyPreviewVersion(row);
}

function applyPreviewVersion(target?: TemplateVersion) {
  previewingTemplateVersion.value = target;
  renderResult.value = null;
  renderForm.outputFormat = target?.sourceFormat === 'HTML' ? 'HTML' : target?.sourceFormat === 'TEXT' ? 'TEXT' : (target?.sourceFormat || 'TEXT');
  renderForm.versionNo = target?.versionNo;
  renderForm.async = false;
  renderVariableMode.value = 'FORM';
  renderVariableRows.value = buildRenderVariableRows(target?.variables || []);
  syncRenderJsonFromForm();
}

async function submitRender() {
  if (!currentTemplate.value) return;
  let variables: JsonObject;
  if (renderVariableMode.value === 'FORM') {
    variables = buildVariablesFromRenderForm();
    if (!validateRequiredRenderVariables()) {
      return;
    }
    renderVariablesText.value = JSON.stringify(variables, null, 2);
  } else {
    try {
      const parsed = JSON.parse(renderVariablesText.value || '{}') as unknown;
      if (!isJsonObject(parsed)) {
        ElMessage.error('变量 JSON 必须是对象');
        return;
      }
      variables = parsed;
    } catch {
      ElMessage.error('变量 JSON 格式不正确');
      return;
    }
  }
  const payload = {
    templateCode: currentTemplate.value.templateCode,
    versionNo: renderForm.versionNo,
    outputFormat: renderForm.outputFormat,
    variables,
    async: renderForm.async,
  };
  previewLoading.value = true;
  try {
    renderResult.value = renderForm.async ? await templateApi.renderAsync(payload) : await templateApi.render(payload);
    ElMessage.success(renderForm.async ? '异步渲染已提交' : '渲染完成');
  } finally {
    previewLoading.value = false;
  }
}

function downloadRenderFile() {
  if (!renderResult.value?.fileId) return;
  window.open(fileApi.downloadUrl(renderResult.value.fileId), '_blank', 'noopener');
}

function openRenderRecord() {
  pageMode.value = 'list';
  window.location.hash = '/template/render-records';
}

function addVariable() {
  versionForm.variables.push(createVariable());
}

function removeVariable(row: VariableRow, index: number) {
  if (!removeVariableFromList(versionForm.variables, row)) {
    versionForm.variables.splice(index, 1);
  }
  syncVariableJsonFromStruct();
}

function addChildVariable(row: VariableRow) {
  if (!isContainerVariable(row)) return;
  row.children = normalizeVariables(row.children || []);
  row.children.push(createVariable());
}

function handleVariableTypeChange(row: VariableRow) {
  if (isContainerVariable(row)) {
    row.children = normalizeVariables(row.children || []);
    if (row.children.length === 0) {
      row.children.push(createVariable());
    }
    return;
  }
  row.children = [];
}

function isContainerVariable(row: TemplateVariableDefinition) {
  return row.type === 'OBJECT' || row.type === 'ARRAY';
}

function removeVariableFromList(items: VariableRow[], row: VariableRow): boolean {
  const index = items.indexOf(row);
  if (index >= 0) {
    items.splice(index, 1);
    return true;
  }
  return items.some((item) => removeVariableFromList((item.children || []) as VariableRow[], row));
}

function validateVariableSchema(): boolean {
  const errors = collectVariableErrors(versionForm.variables);
  if (errors.length === 0) return true;
  const first = errors[0];
  ElMessage.error(`${first.path}：${first.message}`);
  return false;
}

function collectVariableErrors(items: TemplateVariableDefinition[], parentPath = ''): VariableValidationError[] {
  const errors: VariableValidationError[] = [];
  const names = new Set<string>();
  cleanVariables(items).forEach((item) => {
    const name = item.name.trim();
    const path = parentPath ? `${parentPath}.${name}` : name;
    if (names.has(name)) {
      errors.push({ path, message: '同层变量名不能重复' });
    }
    names.add(name);
    if (isContainerVariable(item) && cleanVariables(item.children || []).length === 0) {
      errors.push({ path, message: `${item.type === 'ARRAY' ? '数组' : '对象'}必须添加下一层字段定义` });
    }
    errors.push(...collectVariableErrors(item.children || [], path));
  });
  return errors;
}

function syncVariableJsonFromStruct() {
  // Reserved for draft persistence callers. Variable editing is structure-first on this page.
}

function createVariable(partial: Partial<TemplateVariableDefinition> = {}): VariableRow {
  return {
    id: `var-${++variableSeed}`,
    name: '',
    label: '',
    type: 'STRING',
    required: true,
    example: '',
    description: '',
    children: [],
    ...partial,
  };
}

function normalizeVariables(items: TemplateVariableDefinition[]): VariableRow[] {
  return items.map((item) => createVariable({
    ...item,
    type: item.type || 'STRING',
    required: item.required !== false,
    children: normalizeVariables(item.children || []),
  }));
}

function cleanVariables(items: TemplateVariableDefinition[]): TemplateVariableDefinition[] {
  return items
    .filter((item) => item.name?.trim())
    .map((item) => ({
      name: item.name.trim(),
      label: item.label || item.name.trim(),
      type: item.type || 'STRING',
      required: item.required !== false,
      example: item.example,
      description: item.description,
      children: cleanVariables(item.children || []),
    }));
}

function sampleVariables(items: TemplateVariableDefinition[]): JsonObject {
  const result: JsonObject = {};
  cleanVariables(items).forEach((item) => {
    setJsonPathValue(result, item.name, sampleValue(item));
  });
  return result;
}

function buildRenderVariableRows(items: TemplateVariableDefinition[]): RenderVariableRow[] {
  return cleanVariables(items).map((item) => ({
    ...item,
    id: `render-${item.name}`,
    value: item.type === 'OBJECT' || item.type === 'ARRAY'
      ? JSON.stringify(sampleValue(item), null, 2)
      : sampleValue(item),
  }));
}

function syncRenderJsonFromForm() {
  renderVariablesText.value = JSON.stringify(buildVariablesFromRenderForm(), null, 2);
}

function buildVariablesFromRenderForm(): JsonObject {
  const result: JsonObject = {};
  renderVariableRows.value.forEach((item) => {
    if (!item.name?.trim()) return;
    setJsonPathValue(result, item.name.trim(), normalizeRenderValue(item));
  });
  return result;
}

function setJsonPathValue(target: JsonObject, path: string, value: JsonValue) {
  const parts = path.split('.').map((part) => part.trim()).filter(Boolean);
  if (parts.length === 0) return;
  let current: { [key: string]: JsonValue } = target;
  parts.forEach((part, index) => {
    if (index === parts.length - 1) {
      current[part] = value;
      return;
    }
    const next = current[part];
    if (!isJsonObject(next)) {
      current[part] = {};
    }
    current = current[part] as { [key: string]: JsonValue };
  });
}

function normalizeRenderValue(item: RenderVariableRow): JsonValue {
  if (item.type === 'NUMBER') {
    const value = Number(item.value ?? 0);
    return Number.isFinite(value) ? value : 0;
  }
  if (item.type === 'BOOLEAN') {
    return Boolean(item.value);
  }
  if (item.type === 'OBJECT' || item.type === 'ARRAY') {
    if (typeof item.value !== 'string') return item.value ?? (item.type === 'ARRAY' ? [] : {});
    try {
      const parsed = JSON.parse(item.value || (item.type === 'ARRAY' ? '[]' : '{}')) as JsonValue;
      return parsed;
    } catch {
      return item.value;
    }
  }
  return item.value ?? '';
}

function validateRequiredRenderVariables(): boolean {
  const missing = renderVariableRows.value.find((item) => {
    if (item.required === false) return false;
    if (item.value === undefined || item.value === null) return true;
    return typeof item.value === 'string' && !item.value.trim();
  });
  if (missing) {
    ElMessage.error(`请填写 ${missing.label || missing.name}`);
    return false;
  }
  return true;
}

function sampleValue(item: TemplateVariableDefinition): JsonValue {
  if (item.type === 'OBJECT') return sampleVariables(item.children || []);
  if (item.type === 'ARRAY') return [sampleVariables(item.children || [])];
  if (item.type === 'NUMBER') return Number(item.example || 0);
  if (item.type === 'BOOLEAN') return item.example === 'true';
  return item.example || (item.type === 'DATE' ? '2026-05-22' : item.label || item.name);
}

function isJsonObject(value: unknown): value is JsonObject {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
</script>

<style scoped>
.template-container {
  display: flex;
  min-height: calc(100vh - var(--mango-header-height) - var(--mango-tags-view-height) - 32px);
  padding: 0;
}

.template-main {
  display: flex;
  flex: 1;
  min-width: 0;
}

.template-main :deep(.el-card__body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 16px;
}

.page-card {
  min-height: calc(100vh - 136px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0 0 4px;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 650;
  line-height: 1.3;
}

.page-header span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.page-actions {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.maintain-form {
  min-width: 0;
}

.preview-layout {
  display: grid;
  grid-template-columns: 176px 390px minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
  min-height: calc(100vh - 220px);
}

.preview-version-rail,
.preview-side,
.preview-main {
  min-width: 0;
}

.preview-version-rail {
  overflow: hidden;
  border-right: 1px solid var(--el-border-color-lighter);
  padding-right: 12px;
}

.preview-version-list {
  display: grid;
  gap: 8px;
  max-height: calc(100vh - 280px);
  overflow: auto;
  padding-right: 2px;
}

.preview-version-item {
  width: 100%;
  min-height: 74px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  cursor: pointer;
  display: grid;
  gap: 7px;
  padding: 9px 10px;
  text-align: left;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.preview-version-item:hover,
.preview-version-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.preview-version-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.preview-version-head strong {
  font-size: 13px;
  font-weight: 650;
  line-height: 1.2;
}

.preview-version-remark {
  color: var(--el-text-color-secondary);
  display: -webkit-box;
  font-size: 12px;
  line-height: 1.35;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  word-break: break-word;
}

.preview-side {
  overflow: auto;
  padding-right: 4px;
}

.preview-main {
  display: flex;
  flex-direction: column;
}

.preview-result {
  flex: 1;
  min-height: 520px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
  overflow: hidden;
}

.search-form {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 8px;
}

.action-toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin-bottom: 12px;
}

.toolbar-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.data-table {
  flex: 1;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 420px;
}

.template-main :deep(.pagination-container) {
  flex-shrink: 0;
}

.muted-text {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.template-cell {
  display: grid;
  gap: 3px;
  line-height: 1.35;
}

.template-cell strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 650;
}

.template-cell span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 12px;
}

.form-select {
  width: 100%;
}

.form-section,
.detail-section {
  margin-bottom: 18px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
  font-weight: 650;
}

.section-actions {
  display: inline-flex;
  gap: 8px;
}

.variable-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  margin-bottom: 10px;
  padding: 0 2px;
}

.variable-toolbar-meta,
.variable-toolbar-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.variable-toolbar-meta {
  min-width: 0;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 650;
}

.variable-toolbar-actions {
  flex-shrink: 0;
}

.variable-pane :deep(.el-table__cell) {
  padding: 2px 0;
}

.variable-tree-table :deep(.cell) {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  padding: 0 4px;
  line-height: 26px;
  white-space: nowrap;
}

.variable-pane :deep(.el-table__placeholder) {
  flex: 0 0 12px;
  width: 12px;
}

.variable-pane :deep(.el-table__expand-icon) {
  flex: 0 0 18px;
  margin-right: 2px;
}

.variable-pane :deep(.el-input__wrapper),
.variable-pane :deep(.el-select__wrapper) {
  min-height: 24px;
  border-radius: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 0 7px;
  background: transparent;
  box-shadow: none;
}

.variable-pane :deep(.el-input__wrapper:hover),
.variable-pane :deep(.el-input__wrapper.is-focus),
.variable-pane :deep(.el-select__wrapper:hover),
.variable-pane :deep(.el-select__wrapper.is-focused) {
  border-bottom-color: var(--el-color-primary);
  background: transparent;
  box-shadow: none;
}

.variable-pane :deep(.el-switch) {
  height: 24px;
}

.variable-pane :deep(.el-button + .el-button) {
  margin-left: 2px;
}

.field-name-cell {
  display: flex;
  align-items: center;
  flex: 1;
  flex-wrap: nowrap;
  gap: 2px;
  min-width: 0;
  white-space: nowrap;
}

.field-name-cell .el-input {
  min-width: 0;
  flex: 1;
}

.field-label-button {
  flex: 0 0 18px;
  width: 18px;
  height: 20px;
  padding: 0;
}

.field-inline-actions {
  display: inline-flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
}

.field-inline-actions .el-button {
  width: 16px;
  height: 20px;
  padding: 0;
}

.field-inline-actions :deep(.el-icon) {
  font-size: 13px;
}

.field-label-popover {
  display: grid;
  gap: 8px;
}

.field-label-popover span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.maintain-editor-layout {
  display: grid;
  grid-template-columns: minmax(520px, 0.95fr) minmax(420px, 1.05fr);
  gap: 16px;
  align-items: stretch;
}

.variable-pane,
.content-pane {
  min-width: 0;
}

.content-pane {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content-format-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  padding: 0 2px;
}

.content-format-bar > span {
  flex-shrink: 0;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 650;
}

.content-editor {
  min-width: 0;
}

.document-template-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 420px;
}

.document-upload-area {
  min-width: 0;
}

.document-preview-card {
  min-height: 300px;
  max-height: 420px;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 12px;
  background: var(--el-bg-color);
}

.document-preview-card :deep(.preview-stage) {
  min-height: 180px;
}

.document-preview-card :deep(.preview-frame),
.document-preview-card :deep(.preview-image) {
  height: 260px;
}

.document-empty {
  min-height: 300px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}

.stable-tabs :deep(.el-tabs__content) {
  min-height: 380px;
}

.stable-tabs :deep(.el-tab-pane) {
  height: 380px;
  overflow: auto;
}

.render-tabs {
  width: 100%;
}

.render-tabs :deep(.el-tabs__content) {
  min-height: 360px;
}

.render-tabs :deep(.el-tab-pane) {
  height: 360px;
  overflow: auto;
}

.render-variable-form {
  display: grid;
  gap: 12px;
  padding-right: 4px;
}

.render-variable-item {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  column-gap: 12px;
  row-gap: 4px;
  align-items: start;
}

.render-variable-label {
  display: grid;
  gap: 2px;
  min-width: 0;
  padding-top: 6px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  line-height: 1.25;
}

.render-variable-label span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.render-variable-label em {
  display: inline;
  color: var(--el-color-danger);
  font-style: normal;
}

.render-variable-label small,
.render-variable-desc {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.render-variable-control {
  width: 100%;
}

.render-variable-desc {
  grid-column: 2;
}

.preview-result pre {
  overflow: auto;
  height: 100%;
  margin: 0;
  padding: 12px;
  background: transparent;
  color: var(--el-text-color-primary);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.html-preview {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 520px;
  border: 0;
  background: var(--el-bg-color);
}

@media (max-width: 980px) {
  .preview-layout,
  .maintain-editor-layout,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .render-variable-item {
    grid-template-columns: 1fr;
  }

  .render-variable-desc {
    grid-column: 1;
  }
}

@media (max-width: 760px) {
}
</style>
