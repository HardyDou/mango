<template>
  <div class="cms-page">
    <section class="cms-toolbar">
      <div class="cms-toolbar-head">
        <div>
          <h2>{{ config.title }}</h2>
        </div>
        <el-button v-if="config.editor" type="primary" :icon="Plus" @click="openEditor()">新增</el-button>
      </div>

      <el-form :model="query" class="cms-search" inline @submit.prevent>
        <el-form-item label="关键字" class="cms-search-item">
          <el-input v-model="query.keyword" clearable placeholder="名称/编码" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item v-if="config.filterSite" label="站点" class="cms-search-item">
          <el-select v-model="query.siteId" clearable filterable placeholder="全部站点">
            <el-option v-for="site in siteOptions" :key="site.id" :label="site.siteName || site.siteCode" :value="site.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="config.statusField" label="状态" class="cms-search-item">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="cms-search-actions">
          <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="cms-panel">
      <el-alert v-if="errorMessage" class="cms-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" @click="loadRows">重试</el-button>
        </template>
      </el-alert>

      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        row-key="id"
        :default-expand-all="Boolean(config.defaultExpandAll)"
        :tree-props="{ children: config.treeChildrenProp || 'children' }"
        :empty-text="`暂无${config.title}`"
      >
        <el-table-column
          v-for="column in config.columns"
          :key="column.prop"
          :label="column.label"
          :min-width="column.minWidth"
          :width="column.width"
          :show-overflow-tooltip="false"
        >
          <template #default="{ row }">
            <div v-if="column.type === 'cover'" class="cms-cover-cell">
              <el-image
                v-if="fileImageUrl(formatValue(row, column.prop))"
                class="cms-cover-thumb"
                fit="cover"
                :src="fileImageUrl(formatValue(row, column.prop))"
                :preview-src-list="[fileImageUrl(formatValue(row, column.prop))]"
                preview-teleported
              >
                <template #error>
                  <div class="cms-cover-empty cms-cover-empty--in-image">无图片</div>
                </template>
              </el-image>
              <div v-else class="cms-cover-empty">无图片</div>
            </div>
            <span v-else-if="column.type === 'treeName'" class="cms-tree-node-label">
              {{ formatValue(row, column.prop) || '-' }}
            </span>
            <div v-else-if="column.type === 'name'" class="cms-name-cell">
              <div class="cms-name-text">
                <strong>{{ formatValue(row, column.prop) }}</strong>
                <span v-if="column.subProp">{{ formatValue(row, column.subProp) }}</span>
              </div>
            </div>
            <div v-else-if="column.type === 'meta'" class="cms-meta-cell">
              <strong>{{ column.optionKey ? optionLabel(resolveColumnOptions(column), formatValue(row, column.prop)) : formatValue(row, column.prop) || '-' }}</strong>
              <span>{{ column.subProp ? formatValue(row, column.subProp) || '-' : '-' }}</span>
            </div>
            <el-tag v-else-if="column.type === 'status'" :type="statusTag(formatValue(row, column.prop))" size="small">
              {{ optionLabel(statusOptions, formatValue(row, column.prop)) }}
            </el-tag>
            <el-tag v-else-if="column.type === 'contentStatus'" :type="contentStatusTag(formatValue(row, column.prop))" size="small">
              {{ optionLabel(contentStatusOptions, formatValue(row, column.prop)) }}
            </el-tag>
            <span v-else-if="column.type === 'site'">{{ siteName(formatValue(row, column.prop)) }}</span>
            <span v-else-if="column.optionKey">{{ optionLabels(resolveColumnOptions(column), formatValue(row, column.prop)) }}</span>
            <span v-else>{{ formatValue(row, column.prop) || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.updatedAt || row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="cms-actions">
              <el-button v-if="config.detail" link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
              <el-button v-if="config.editor" link type="primary" :icon="Edit" @click="openEditor(row)">编辑</el-button>
              <el-button v-if="config.canToggleStatus && rowStatus(row) === 'ENABLED'" link type="warning" @click="changeStatus(row, 'DISABLED')">禁用</el-button>
              <el-button v-if="config.canToggleStatus && rowStatus(row) === 'DISABLED'" link type="success" @click="changeStatus(row, 'ENABLED')">启用</el-button>
              <el-button v-if="config.key === 'contents' && row.status === 'DRAFT'" link type="primary" @click="submitContent(row)">提交</el-button>
              <el-button v-if="config.key === 'contents' && row.status === 'PENDING_REVIEW'" link type="success" @click="approveContent(row)">通过</el-button>
              <el-button v-if="config.key === 'contents' && row.status === 'PENDING_REVIEW'" link type="warning" @click="rejectContent(row)">驳回</el-button>
              <el-button v-if="config.key === 'contents' && row.status === 'PUBLISHED'" link type="warning" @click="offlineContent(row)">下线</el-button>
              <el-button v-if="config.key === 'contentPublishes' && row.publishStatus === 'PUBLISHED'" link type="warning" @click="offlinePublish(row)">下线</el-button>
              <el-button v-if="config.remove" link type="danger" :icon="Delete" @click="deleteRow(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!config.hidePagination" class="cms-pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @size-change="loadRows"
          @current-change="loadRows"
        />
      </div>
    </section>

    <el-dialog v-model="editorVisible" :title="form.id ? `编辑${config.title}` : `新增${config.title}`" width="820px" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="112px">
        <div class="cms-form-section-title">基础信息</div>
        <el-row :gutter="14">
          <el-col v-for="field in visibleEditorFields" :key="field.key || field.prop" :span="field.span || 12">
            <el-form-item :label="field.label" :prop="field.required ? field.prop : undefined">
              <Editor
                v-if="field.type === 'richText'"
                v-model="form[field.prop]"
                :data-testid="`cms-rich-text-${field.prop}`"
                :height="field.height || 360"
                mode="default"
                :placeholder="field.placeholder || field.label"
              />
              <el-input v-else-if="field.type === 'textarea'" v-model="form[field.prop]" type="textarea" :rows="field.rows || 3" :placeholder="field.placeholder || field.label" />
              <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]" :min="0" :max="999999" style="width: 100%" />
              <el-date-picker
                v-else-if="field.type === 'datetime'"
                v-model="form[field.prop]"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                :placeholder="field.placeholder || field.label"
                style="width: 100%"
              />
              <el-select v-else-if="field.type === 'select'" v-model="form[field.prop]" clearable filterable :placeholder="field.placeholder || field.label" style="width: 100%" @change="handleEditorFieldChange(field)">
                <el-option v-for="item in resolveOptions(field)" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-select v-else-if="field.type === 'multiSelect'" v-model="form[field.prop]" multiple clearable filterable collapse-tags collapse-tags-tooltip :placeholder="field.placeholder || field.label" style="width: 100%" @change="handleEditorFieldChange(field)">
                <el-option v-for="item in resolveOptions(field)" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-checkbox-group v-else-if="field.type === 'checkButtonGroup'" v-model="form[field.prop]" class="cms-check-button-group">
                <el-checkbox-button v-for="item in resolveOptions(field)" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-checkbox-button>
              </el-checkbox-group>
              <el-tree-select
                v-else-if="field.type === 'treeSelect'"
                v-model="form[field.prop]"
                :data="resolveTreeOptions(field)"
                :props="{ label: 'label', value: 'value', children: 'children', disabled: 'disabled' }"
                check-strictly
                clearable
                filterable
                :multiple="field.multiple"
                collapse-tags
                collapse-tags-tooltip
                :default-expand-all="Boolean(field.defaultExpandAll)"
                expand-on-click-node
                check-on-click-node
                node-key="value"
                :placeholder="field.placeholder || field.label"
                style="width: 100%"
                @change="handleEditorFieldChange(field)"
              />
              <MUpload
                v-else-if="field.type === 'upload'"
                v-model="form[field.prop]"
                value-type="id"
                :display="field.upload?.display || 'thumbnail'"
                :count="field.upload?.count || 1"
                :fmt="field.upload?.fmt"
                :biz-type="field.upload?.bizType"
                :purpose="field.upload?.purpose"
                :access-level="field.upload?.accessLevel"
                :button-text="field.upload?.buttonText || `上传${field.label}`"
              />
              <el-switch v-else-if="field.type === 'switch'" v-model="form[field.prop]" />
              <el-input v-else v-model="form[field.prop]" :placeholder="field.placeholder || field.label" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRow">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="config.key === 'contents' ? '文章详情' : `${config.title}详情`" :width="config.key === 'contents' ? '920px' : '860px'" destroy-on-close append-to-body>
      <article v-if="config.key === 'contents'" class="cms-article-detail">
        <h1>{{ formatValue(detailRow, 'title') || '-' }}</h1>
        <p v-if="formatValue(detailRow, 'subtitle')" class="cms-article-subtitle">{{ formatValue(detailRow, 'subtitle') }}</p>
        <div class="cms-article-meta">
          <el-tag :type="contentStatusTag(formatValue(detailRow, 'status'))" size="small">
            {{ optionLabel(contentStatusOptions, formatValue(detailRow, 'status')) }}
          </el-tag>
          <span>{{ optionLabel(typeOptions.contentType, formatValue(detailRow, 'contentType')) }}</span>
          <span>{{ formatValue(detailRow, 'categoryName') || '未分类' }}</span>
          <span>{{ formatValue(detailRow, 'author') || '未署名' }}</span>
          <span>{{ detailRow.updatedAt || detailRow.createdAt || '-' }}</span>
        </div>
        <div v-if="formatValue(detailRow, 'source') || formatValue(detailRow, 'externalUrl')" class="cms-article-source">
          <span v-if="formatValue(detailRow, 'source')">来源：{{ formatValue(detailRow, 'source') }}</span>
          <a v-if="safeExternalUrl(formatValue(detailRow, 'externalUrl'))" :href="safeExternalUrl(formatValue(detailRow, 'externalUrl'))" target="_blank" rel="noopener noreferrer">
            原文链接
          </a>
          <span v-else-if="formatValue(detailRow, 'externalUrl')">外链：{{ formatValue(detailRow, 'externalUrl') }}</span>
        </div>
        <p v-if="formatValue(detailRow, 'summary')" class="cms-article-summary">{{ formatValue(detailRow, 'summary') }}</p>
        <div
          v-if="hasRichTextValue(detailRow, 'body')"
          class="cms-detail-richtext cms-article-body"
          v-html="formatRichTextValue(detailRow, { prop: 'body', label: '正文', type: 'richText' })"
        />
        <el-empty v-else class="cms-article-empty" description="暂无正文内容" :image-size="72" />
      </article>
      <div v-else class="cms-detail">
        <section v-for="group in config.detail?.groups" :key="group.title" class="cms-detail-section">
          <div class="cms-detail-title">{{ group.title }}</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item v-for="field in group.fields" :key="field.prop" :label="field.label" :span="field.detailSpan || 1">
              <div
                v-if="field.type === 'richText'"
                class="cms-detail-richtext"
                v-html="formatRichTextValue(detailRow, field)"
              />
              <span v-else class="cms-detail-value" :class="{ 'cms-detail-value--long': field.type === 'textarea' }">
                {{ formatDetailValue(detailRow, field) }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
        </section>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue';
import { Editor } from '@mango/common';
import { MUpload, fileApi } from '@mango/file';
import { cmsApi, requestErrorMessage, type ApiId, type CmsPageQuery, type CmsSite } from '../api/cms';

type CmsRow = Record<string, unknown> & { id?: ApiId; status?: string; publishStatus?: string };

type OptionItem = {
  label: string;
  value: string;
};

type TreeOptionItem = OptionItem & {
  disabled?: boolean;
  children?: TreeOptionItem[];
};

type FieldConfig = {
  key?: string;
  prop: string;
  label: string;
  type?: 'input' | 'textarea' | 'richText' | 'number' | 'datetime' | 'select' | 'multiSelect' | 'checkButtonGroup' | 'treeSelect' | 'switch' | 'upload';
  optionKey?: string;
  required?: boolean;
  visibleWhen?: Record<string, unknown | unknown[]>;
  visibleWhenAny?: Record<string, unknown | unknown[]>[];
  span?: number;
  rows?: number;
  height?: number;
  placeholder?: string;
  detailSpan?: number;
  multiple?: boolean;
  defaultExpandAll?: boolean;
  upload?: {
    fmt?: string;
    count?: number;
    display?: 'list' | 'thumbnail' | 'table' | 'drag';
    purpose?: string;
    bizType?: string;
    accessLevel?: string;
    buttonText?: string;
  };
};

type ColumnConfig = {
  prop: string;
  subProp?: string;
  label: string;
  type?: 'name' | 'treeName' | 'cover' | 'meta' | 'status' | 'contentStatus' | 'site';
  optionKey?: string;
  minWidth?: number;
  width?: number;
};

type PageData = {
  list: CmsRow[];
  total: number;
  pageNum: number;
  pageSize: number;
};

type DetailGroupConfig = {
  title: string;
  fields: FieldConfig[];
};

export type CmsResourceConfig = {
  key: string;
  title: string;
  description: string;
  columns: ColumnConfig[];
  filterSite?: boolean;
  requireSiteForPage?: boolean;
  statusField?: boolean;
  statusProp?: string;
  canToggleStatus?: boolean;
  tree?: boolean;
  treeChildrenProp?: string;
  defaultExpandAll?: boolean;
  hidePagination?: boolean;
  page: (query: CmsPageQuery) => Promise<PageData>;
  create?: (data: CmsRow) => Promise<unknown>;
  update?: (data: CmsRow) => Promise<unknown>;
  updateStatus?: (id: ApiId, status: 'ENABLED' | 'DISABLED') => Promise<unknown>;
  remove?: (id: ApiId) => Promise<unknown>;
  normalizePayload?: (payload: CmsRow, form: CmsRow) => CmsRow;
  editor?: {
    defaults: CmsRow;
    fields: FieldConfig[];
  };
  detail?: {
    groups: DetailGroupConfig[];
  };
};

const props = defineProps<{
  config: CmsResourceConfig;
}>();

const statusOptions: OptionItem[] = [
  { label: '启用', value: 'ENABLED' },
  { label: '禁用', value: 'DISABLED' },
];

const contentStatusOptions: OptionItem[] = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已下线', value: 'OFFLINE' },
];

const typeOptions: Record<string, OptionItem[]> = {
  status: statusOptions,
  contentType: [
    { label: '文章', value: 'ARTICLE' },
    { label: '单页', value: 'PAGE' },
    { label: '图文', value: 'IMAGE_TEXT' },
    { label: '视频', value: 'VIDEO' },
    { label: '附件', value: 'ATTACHMENT' },
  ],
  categoryType: [
    { label: '列表', value: 'LIST' },
    { label: '详情', value: 'DETAIL' },
    { label: '外链', value: 'EXTERNAL' },
  ],
  accessType: [
    { label: '公开', value: 'PUBLIC' },
    { label: '登录可见', value: 'LOGIN' },
    { label: '角色可见', value: 'ROLE' },
  ],
  navType: [
    { label: '顶部导航', value: 'TOP' },
    { label: '底部导航', value: 'FOOTER' },
    { label: '快捷导航', value: 'QUICK' },
  ],
  jumpType: [
    { label: '栏目', value: 'CATEGORY' },
    { label: '内容', value: 'CONTENT' },
    { label: '链接', value: 'URL' },
  ],
  openTarget: [
    { label: '当前窗口', value: 'SELF' },
    { label: '新窗口', value: 'BLANK' },
  ],
  publishStatus: [
    { label: '待发布', value: 'PENDING' },
    { label: '已发布', value: 'PUBLISHED' },
    { label: '定时发布', value: 'SCHEDULED' },
    { label: '已下线', value: 'OFFLINE' },
  ],
  mediaType: [
    { label: '图片', value: 'IMAGE' },
    { label: '视频', value: 'VIDEO' },
  ],
  positionType: [
    { label: 'Banner', value: 'BANNER' },
    { label: '首页推荐', value: 'RECOMMEND' },
    { label: '侧边栏', value: 'SIDEBAR' },
    { label: '底部', value: 'FOOTER' },
    { label: '弹窗', value: 'POPUP' },
    { label: '自定义', value: 'CUSTOM' },
  ],
  materialType: [
    { label: '单图', value: 'SINGLE_IMAGE' },
    { label: '多图', value: 'MULTI_IMAGE' },
    { label: '视频', value: 'VIDEO' },
    { label: '文本', value: 'TEXT' },
    { label: '富文本', value: 'RICH_TEXT' },
    { label: 'HTML', value: 'HTML' },
  ],
};

const query = reactive<CmsPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  siteId: '',
});
const rows = ref<CmsRow[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const editorVisible = ref(false);
const detailVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<CmsRow>({});
const detailRow = reactive<CmsRow>({});
const siteOptions = ref<CmsSite[]>([]);
const contentOptions = ref<CmsRow[]>([]);
const contentCategoryOptions = ref<CmsRow[]>([]);
const siteCategoryOptions = ref<CmsRow[]>([]);
const advertisementOptions = ref<CmsRow[]>([]);

const visibleEditorFields = computed(() => (props.config.editor?.fields || []).filter(field => isFieldVisible(field)));

const rules = computed<FormRules>(() => {
  const nextRules: FormRules = {};
  visibleEditorFields.value
    .filter(field => field.required)
    .forEach((field) => {
      nextRules[field.prop] = [{ required: true, message: `${field.label}不能为空`, trigger: 'blur' }];
    });
  return nextRules;
});

onMounted(loadResource);

watch(() => props.config.key, () => {
  void loadResource();
});

async function loadResource() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.keyword = '';
  query.status = '';
  query.siteId = '';
  rows.value = [];
  total.value = 0;
  contentOptions.value = [];
  contentCategoryOptions.value = [];
  siteCategoryOptions.value = [];
  advertisementOptions.value = [];
  editorVisible.value = false;
  detailVisible.value = false;
  await loadSiteOptions();
  await loadEditorOptions();
  await loadRows();
}

async function loadSiteOptions() {
  if (!props.config.filterSite && !props.config.editor?.fields.some(field => field.optionKey === 'sites')) {
    return;
  }
  try {
    const page = await cmsApi.pageSites({ pageNum: 1, pageSize: 200 });
    siteOptions.value = page.list || [];
    if (props.config.requireSiteForPage && !query.siteId) {
      query.siteId = siteOptions.value.find(site => site.id)?.id || '';
    }
  } catch (error) {
    errorMessage.value = requestErrorMessage(error, '站点选项加载失败');
  }
}

async function loadEditorOptions() {
  const fields = props.config.editor?.fields || [];
  try {
    if (fields.some(field => field.optionKey === 'contents')) {
      const page = await cmsApi.pageContents({ pageNum: 1, pageSize: 200 });
      contentOptions.value = page.list || [];
    }
    if (fields.some(field => field.optionKey === 'contentCategories')) {
      contentCategoryOptions.value = await cmsApi.treeContentCategories({ status: 'ENABLED' }) as CmsRow[];
    }
    if (fields.some(field => field.optionKey === 'siteCategories')) {
      siteCategoryOptions.value = [];
      for (const site of siteOptions.value.filter(site => site.id)) {
        const tree = await cmsApi.treeSiteCategories({ siteId: site.id });
        siteCategoryOptions.value.push(...(tree as CmsRow[]));
      }
    }
    if (fields.some(field => field.optionKey === 'advertisements')) {
      const page = await cmsApi.pageAdvertisements({ pageNum: 1, pageSize: 500 });
      advertisementOptions.value = page.list || [];
    }
  } catch (error) {
    errorMessage.value = requestErrorMessage(error, '关联选项加载失败');
  }
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    ensureRequiredSiteQuery();
    const page = await props.config.page({ ...query });
    rows.value = page.list || [];
    total.value = Number(page.total || 0);
  } catch (error) {
    rows.value = [];
    total.value = 0;
    errorMessage.value = requestErrorMessage(error, `${props.config.title}加载失败`);
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.keyword = '';
  query.status = '';
  query.siteId = props.config.requireSiteForPage ? defaultSiteId() : '';
  void loadRows();
}

function ensureRequiredSiteQuery() {
  if (props.config.requireSiteForPage && !query.siteId) {
    query.siteId = defaultSiteId();
  }
}

function defaultSiteId() {
  return siteOptions.value.find(site => site.id)?.id || '';
}

function openEditor(row?: CmsRow) {
  Object.keys(form).forEach(key => delete form[key]);
  Object.assign(form, props.config.editor?.defaults || {}, row || {});
  props.config.editor?.fields.forEach((field) => {
    const value = form[field.prop];
    if ((field.type === 'multiSelect' || field.type === 'checkButtonGroup' || (field.type === 'upload' && Number(field.upload?.count || 1) > 1))
      && typeof value === 'string') {
      form[field.prop] = value.split(',').map(item => item.trim()).filter(Boolean);
    }
  });
  props.config.editor?.fields
    .filter(field => field.prop === 'parentId' && !form.parentId)
    .forEach(() => {
      form.parentId = '0';
    });
  editorVisible.value = true;
}

function openDetail(row: CmsRow) {
  Object.keys(detailRow).forEach(key => delete detailRow[key]);
  Object.assign(detailRow, row);
  detailVisible.value = true;
}

function handleEditorFieldChange(field: FieldConfig) {
  if (field.prop === 'siteId') {
    normalizeSiteScopedFields();
  }
  if (field.prop === 'adId') {
    normalizeAdMaterialType();
  }
}

function normalizeSiteScopedFields() {
  if (form.adId && !advertisementBelongsToSite(form.adId, form.siteId)) {
    form.adId = '';
  }
  if (Array.isArray(form.categoryIds)) {
    form.categoryIds = form.categoryIds.filter(id => siteCategoryBelongsToSite(id, form.siteId));
  }
  if (form.categoryId && !siteCategoryBelongsToSite(form.categoryId, form.siteId)) {
    form.categoryId = '';
  }
  normalizeAdMaterialType();
}

function normalizeAdMaterialType() {
  if (props.config.key !== 'adDeliveries' || !form.adId) {
    return;
  }
  const options = supportedMaterialOptions();
  if (options.length && !options.some(item => item.value === form.materialType)) {
    form.materialType = options[0].value;
  }
}

async function saveRow() {
  if (!props.config.editor || !props.config.create || !props.config.update) {
    return;
  }
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = editorPayload();
    if (form.id) {
      await props.config.update(payload);
    } else {
      await props.config.create(payload);
    }
    ElMessage.success('保存成功');
    editorVisible.value = false;
    await loadRows();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '保存失败'));
  } finally {
    saving.value = false;
  }
}

function editorPayload() {
  const payload: CmsRow = {};
  if (form.id) {
    payload.id = form.id;
  }
  visibleEditorFields.value.forEach((field) => {
    payload[field.prop] = form[field.prop];
  });
  return props.config.normalizePayload ? props.config.normalizePayload(payload, form) : payload;
}

function isFieldVisible(field: FieldConfig) {
  if (!field.visibleWhen && !field.visibleWhenAny) {
    return true;
  }
  if (field.visibleWhenAny) {
    return field.visibleWhenAny.some(condition => isConditionMatched(condition));
  }
  return isConditionMatched(field.visibleWhen || {});
}

function isConditionMatched(condition: Record<string, unknown | unknown[]>) {
  return Object.entries(condition).every(([prop, expected]) => {
    const current = form[prop];
    return Array.isArray(expected) ? expected.includes(current) : current === expected;
  });
}

async function changeStatus(row: CmsRow, status: 'ENABLED' | 'DISABLED') {
  if (!row.id || !props.config.updateStatus) {
    return;
  }
  await props.config.updateStatus(row.id, status);
  ElMessage.success('状态已更新');
  await loadRows();
}

async function deleteRow(row: CmsRow) {
  if (!row.id || !props.config.remove) {
    return;
  }
  await ElMessageBox.confirm('确认删除该记录？', '删除确认', { type: 'warning' });
  await props.config.remove(row.id);
  ElMessage.success('删除成功');
  await loadRows();
}

async function submitContent(row: CmsRow) {
  if (!row.id) {
    return;
  }
  await cmsApi.submitContent(row.id);
  ElMessage.success('已提交审核');
  await loadRows();
}

async function approveContent(row: CmsRow) {
  if (!row.id) {
    return;
  }
  await cmsApi.approveContent(row.id, '审核通过');
  ElMessage.success('内容已发布');
  await loadRows();
}

async function rejectContent(row: CmsRow) {
  if (!row.id) {
    return;
  }
  await cmsApi.rejectContent(row.id, '内容需要调整');
  ElMessage.success('内容已驳回');
  await loadRows();
}

async function offlineContent(row: CmsRow) {
  if (!row.id) {
    return;
  }
  await cmsApi.offlineContent(row.id);
  ElMessage.success('内容已下线');
  await loadRows();
}

async function offlinePublish(row: CmsRow) {
  if (!row.id) {
    return;
  }
  await cmsApi.offlinePublish(row.id);
  ElMessage.success('发布关系已下线');
  await loadRows();
}

function resolveOptions(field: FieldConfig) {
  if (field.optionKey === 'sites') {
    return siteOptions.value
      .filter(site => site.id)
      .map(site => ({ label: site.siteName || site.siteCode || String(site.id), value: String(site.id) }));
  }
  if (field.optionKey === 'contents') {
    return contentOptions.value
      .filter(item => item.id)
      .map(item => ({ label: String(item.title || item.id), value: String(item.id) }));
  }
  if (field.optionKey === 'contentCategories') {
    return contentCategoryOptions.value
      .filter(item => item.id)
      .map(item => ({ label: String(item.categoryName || item.categoryCode || item.id), value: String(item.id) }));
  }
  if (field.optionKey === 'siteCategories') {
    return siteCategoryOptions.value
      .filter(item => item.id)
      .map(item => ({ label: String(item.categoryName || item.categoryCode || item.id), value: String(item.id) }));
  }
  if (field.optionKey === 'advertisements') {
    return advertisementOptions.value
      .filter(item => !form.siteId || String(item.siteId || '') === String(form.siteId))
      .filter(item => item.id)
      .map(item => ({ label: `${String(item.adName || item.adCode || item.id)} · ${String(item.position || '-')}`, value: String(item.id) }));
  }
  if (field.optionKey === 'materialType' && props.config.key === 'adDeliveries') {
    return supportedMaterialOptions();
  }
  return typeOptions[field.optionKey || ''] || [];
}

function supportedMaterialOptions() {
  const selectedAd = advertisementOptions.value.find(item => String(item.id || '') === String(form.adId || ''));
  const supportedValues = String(selectedAd?.supportedMaterialTypes || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
  if (!supportedValues.length) {
    return typeOptions.materialType;
  }
  return typeOptions.materialType.filter(item => supportedValues.includes(item.value));
}

function advertisementBelongsToSite(adId: unknown, siteId: unknown) {
  const id = String(adId || '');
  const site = String(siteId || '');
  if (!id || !site) {
    return true;
  }
  const ad = advertisementOptions.value.find(item => String(item.id || '') === id);
  return !ad || String(ad.siteId || '') === site;
}

function siteCategoryBelongsToSite(categoryId: unknown, siteId: unknown) {
  const id = String(categoryId || '');
  const site = String(siteId || '');
  if (!id || !site) {
    return true;
  }
  const category = findTreeRecord(siteCategoryOptions.value, id);
  return !category || String(category.siteId || '') === site;
}

function findTreeRecord(records: CmsRow[], id: string): CmsRow | undefined {
  for (const record of records) {
    if (String(record.id || '') === id) {
      return record;
    }
    const matched = findTreeRecord((record.children || []) as CmsRow[], id);
    if (matched) {
      return matched;
    }
  }
  return undefined;
}

function resolveTreeOptions(field: FieldConfig) {
  const topLevelOption: TreeOptionItem[] = field.prop === 'parentId'
    ? [{ label: '顶级', value: '0' }]
    : [];
  if (field.optionKey === 'contentCategories') {
    return [
      ...topLevelOption,
      ...toTreeOptions(contentCategoryOptions.value, {
      nameKey: 'categoryName',
      codeKey: 'categoryCode',
      excludeId: props.config.key === 'contentCategories' && field.prop === 'parentId' ? form.id : undefined,
      }),
    ];
  }
  if (field.optionKey === 'siteCategories') {
    return [
      ...topLevelOption,
      ...toTreeOptions(siteCategoryOptions.value, {
      nameKey: 'categoryName',
      codeKey: 'categoryCode',
      excludeId: props.config.key === 'siteCategories' && field.prop === 'parentId' ? form.id : undefined,
      }),
    ];
  }
  return [];
}

function toTreeOptions(records: CmsRow[], config: { nameKey: string; codeKey: string; excludeId?: unknown }) {
  const excludedIds = collectDescendantIds(records, config.excludeId);
  const toOption = (record: CmsRow): TreeOptionItem | undefined => {
    const id = String(record.id || '');
    if (!id) {
      return undefined;
    }
    const children = ((record.children || []) as CmsRow[])
      .map(child => toOption(child))
      .filter((child): child is TreeOptionItem => Boolean(child));
    return {
      label: String(record[config.nameKey] || record[config.codeKey] || id),
      value: id,
      disabled: excludedIds.has(id),
      ...(children.length ? { children } : {}),
    };
  };
  const roots = records
    .map(record => toOption(record))
    .filter((record): record is TreeOptionItem => Boolean(record));
  trimEmptyChildren(roots);
  return roots;
}

function collectDescendantIds(records: CmsRow[], rootId: unknown) {
  const id = String(rootId || '');
  const ids = new Set<string>();
  if (!id) {
    return ids;
  }
  const visit = (items: CmsRow[], matched: boolean) => {
    items.forEach((item) => {
      const itemId = String(item.id || '');
      const nextMatched = matched || itemId === id;
      if (nextMatched && itemId) {
        ids.add(itemId);
      }
      visit((item.children || []) as CmsRow[], nextMatched);
    });
  };
  visit(records, false);
  return ids;
}

function trimEmptyChildren(nodes: TreeOptionItem[]) {
  nodes.forEach((node) => {
    if (node.children?.length) {
      trimEmptyChildren(node.children);
    } else {
      delete node.children;
    }
  });
}

function resolveColumnOptions(column: ColumnConfig) {
  return typeOptions[column.optionKey || ''] || [];
}

function optionLabel(options: OptionItem[], value: unknown) {
  const matched = options.find(item => item.value === value);
  return matched?.label || String(value || '-');
}

function optionLabels(options: OptionItem[], value: unknown) {
  const values = Array.isArray(value)
    ? value
    : String(value || '').split(',').map(item => item.trim()).filter(Boolean);
  if (!values.length) {
    return '-';
  }
  return values.map(item => optionLabel(options, item)).join('、');
}

function formatDetailValue(row: CmsRow, field: FieldConfig) {
  const value = row[field.prop];
  if (field.optionKey) {
    return optionLabel(resolveOptions(field), value);
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否';
  }
  if (Array.isArray(value)) {
    return value.length ? value.join('、') : '-';
  }
  return String(value || '-');
}

function formatRichTextValue(row: CmsRow, field: FieldConfig) {
  const html = String(row[field.prop] || '').trim();
  return html ? sanitizeRichText(html) : '<span class="cms-muted">-</span>';
}

function hasRichTextValue(row: CmsRow, prop: string) {
  return String(row[prop] || '').trim().length > 0;
}

function safeExternalUrl(value: unknown) {
  const url = String(value || '').trim();
  return /^https?:\/\//i.test(url) ? url : '';
}

function sanitizeRichText(html: string) {
  const template = document.createElement('template');
  template.innerHTML = html;
  template.content.querySelectorAll('script, style, iframe, object, embed, link, meta').forEach(element => element.remove());
  template.content.querySelectorAll('*').forEach((element) => {
    Array.from(element.attributes).forEach((attribute) => {
      const name = attribute.name.toLowerCase();
      const value = attribute.value.trim();
      if (name.startsWith('on') || name === 'style') {
        element.removeAttribute(attribute.name);
        return;
      }
      if ((name === 'href' || name === 'src') && /^(javascript|data):/i.test(value)) {
        element.removeAttribute(attribute.name);
      }
    });
  });
  return template.innerHTML;
}

function statusTag(value: unknown) {
  return value === 'ENABLED' ? 'success' : 'info';
}

function rowStatus(row: CmsRow) {
  return row[props.config.statusProp || 'status'];
}

function contentStatusTag(value: unknown) {
  if (value === 'PUBLISHED') {
    return 'success';
  }
  if (value === 'PENDING_REVIEW') {
    return 'warning';
  }
  if (value === 'REJECTED' || value === 'OFFLINE') {
    return 'info';
  }
  return 'info';
}

function siteName(value: unknown) {
  const id = String(value || '');
  return siteOptions.value.find(site => String(site.id) === id)?.siteName || id || '-';
}

function fileImageUrl(value: unknown) {
  const id = String(value || '').trim();
  return id ? fileApi.downloadUrl(id) : '';
}

function formatValue(row: CmsRow, prop?: string) {
  if (!prop) {
    return '';
  }
  const value = row[prop];
  if (typeof value === 'boolean') {
    return value ? '是' : '否';
  }
  return value as string | number | undefined;
}

function flattenTree(records: CmsRow[]): CmsRow[] {
  return records.flatMap(record => [record, ...flattenTree((record.children || []) as CmsRow[])]);
}
</script>
