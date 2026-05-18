<template>
  <DemoDocLayout
    class="upload-view"
    title="Upload 文件上传"
    subtitle="统一使用文件中心 Upload 组件，覆盖通用附件、图片、办公文档和业务附件清单。"
    content-box
    :toc-items="tocItems"
  >
      <section id="default" class="doc-section">
        <h2>通用/默认用法</h2>
        <p>默认不限制格式，选择文件后自动上传；v-model 用于表单提交，完整文件记录通过 change/success 事件获取。</p>
        <div class="demo-block" data-testid="mixed-upload-panel">
          <div class="demo-source">
            <FeatureTags
              :items="['自动上传', '不限格式', '表单绑定', '直连预览优先']"
            />
            <MUpload
              v-model="mixedTokens"
              display="list"
              :count="6"
              size="50MB"
              :sizes="sizeRules"
              biz-type="EXPENSE_REIMBURSEMENT"
              :biz-id="demoBizId"
              purpose="attachment"
              :biz-meta="bizMeta"
              @change="handleChange('通用/默认用法', $event)"
            />
          </div>
          <div class="op-btns" @click="toggleCode('mixed')">
            <el-icon><component :is="codeVisible.mixed ? ArrowUp : ArrowDown" /></el-icon>
            <span>{{ codeVisible.mixed ? '隐藏代码' : '显示代码' }}</span>
          </div>
          <CodeBlock v-show="codeVisible.mixed" :code="mixedCode" />
        </div>
      </section>

      <section id="features" class="doc-section">
        <h2>展示方式</h2>
        <p>display 支持 list、thumbnail、drag、table 四种展示形态；上传后点击文件优先打开文件中心返回的直连预览地址。</p>

        <h3>display="thumbnail" 图片缩略图</h3>
        <p>缩略图展示，限制图片格式和单文件大小。</p>
        <div class="demo-block" data-testid="image-upload-panel">
          <div class="demo-source">
            <FeatureTags
              :items="['缩略图', '仅图片', '最多 4 个', '单文件 10MB']"
            />
            <MUpload
              v-model="imageRecords"
              fmt="image"
              display="thumbnail"
              value-type="record"
              :count="4"
              size="10MB"
              biz-type="FILE_CENTER_DEMO"
              :biz-id="demoBizId"
              purpose="image"
              :biz-meta="{ scene: 'image', source: 'upload-demo' }"
              @change="handleRecordsChange"
            />
            <div class="model-value-note">
              <span>v-model="imageRecords"</span>
              <p>
                图片场景使用 value-type="record"，上传成功后 imageRecords 是 FileRecord[]，包含 id、fileName、fileSize、contentType、directPreviewUrl、directDownloadUrl、bizMeta 等完整信息，适合图片回显、预览和业务附件明细。
              </p>
            </div>
          </div>
          <div class="op-btns" @click="toggleCode('image')">
            <el-icon><component :is="codeVisible.image ? ArrowUp : ArrowDown" /></el-icon>
            <span>{{ codeVisible.image ? '隐藏代码' : '显示代码' }}</span>
          </div>
          <CodeBlock v-show="codeVisible.image" :code="imageCode" />
        </div>

        <h3>display="drag" 拖拽上传</h3>
        <p>限定 Word、Excel、PDF、ODF 等常见业务文档。</p>
        <div class="demo-block" data-testid="office-upload-panel">
          <div class="demo-source">
            <FeatureTags
              :items="['拖拽上传', 'Office/PDF/ODF', '返回完整记录', '文档业务场景']"
            />
            <MUpload
              v-model="officeRecords"
              :fmt="['office', 'pdf', 'odf']"
              display="drag"
              value-type="record"
              :count="3"
              size="80MB"
              biz-type="CONTRACT_REVIEW"
              :biz-id="demoBizId"
              purpose="document"
              :biz-meta="{ scene: 'contract', reviewer: 'legal' }"
              @change="handleRecordsChange"
            />
          </div>
          <div class="op-btns" @click="toggleCode('office')">
            <el-icon><component :is="codeVisible.office ? ArrowUp : ArrowDown" /></el-icon>
            <span>{{ codeVisible.office ? '隐藏代码' : '显示代码' }}</span>
          </div>
          <CodeBlock v-show="codeVisible.office" :code="officeCode" />
        </div>

        <h3>display="table" 业务附件清单</h3>
        <p>表格方式展示文件名、大小、上传进度、上传时间、业务 ID 和业务自定义参数。</p>
        <div class="demo-block" data-testid="table-upload-panel">
          <div class="demo-source">
            <FeatureTags
              :items="['表格清单', '自定义列', '业务 ID', 'meta 扩展字段']"
            />
            <MUpload
              v-model="tableRecords"
              display="table"
              value-type="record"
              :columns="tableColumns"
              :count="8"
              size="100MB"
              :sizes="sizeRules"
              biz-type="GUARANTEE_APPLICATION"
              :biz-id="demoBizId"
              purpose="biz-attachment"
              :biz-meta="{ scene: 'guarantee', applicant: '示例企业', projectCode: 'PRJ-20260517' }"
              @change="handleRecordsChange"
            />
          </div>
          <div class="op-btns" @click="toggleCode('table')">
            <el-icon><component :is="codeVisible.table ? ArrowUp : ArrowDown" /></el-icon>
            <span>{{ codeVisible.table ? '隐藏代码' : '显示代码' }}</span>
          </div>
          <CodeBlock v-show="codeVisible.table" :code="tableCode" />
        </div>
      </section>

      <section id="manual" class="doc-section">
        <h2>手动上传</h2>
        <p>auto=false 时先选择文件，再统一点击上传到服务器；一次选择多个文件时走文件中心批量上传接口。</p>
        <div class="demo-block" data-testid="manual-upload-panel">
          <div class="demo-source">
            <FeatureTags
              :items="['手动提交', '选取后不立即上传', '多文件批量接口', '提交后回填标识']"
            />
            <MUpload
              v-model="manualTokens"
              display="list"
              :auto="false"
              :count="6"
              button-text="选取文件"
              size="50MB"
              :sizes="sizeRules"
              biz-type="FILE_CENTER_DEMO"
              :biz-id="demoBizId"
              purpose="manual"
              :biz-meta="{ scene: 'manual', source: 'upload-demo' }"
              @change="handleChange('手动上传', $event)"
            />
          </div>
          <div class="op-btns" @click="toggleCode('manual')">
            <el-icon><component :is="codeVisible.manual ? ArrowUp : ArrowDown" /></el-icon>
            <span>{{ codeVisible.manual ? '隐藏代码' : '显示代码' }}</span>
          </div>
          <CodeBlock v-show="codeVisible.manual" :code="manualCode" />
        </div>
      </section>

      <section id="slots" class="doc-section">
        <h2>插槽用法</h2>
        <p>trigger 可替换默认上传按钮；thumbnail-trigger 和 drag-trigger 可分别覆盖缩略图、拖拽形态的触发区域。</p>
        <div class="demo-block" data-testid="slot-upload-panel">
          <div class="demo-source">
            <FeatureTags
              :items="['自定义 trigger', '保留校验', '保留上传流程', '适合业务按钮']"
            />
            <MUpload
              v-model="slotTokens"
              display="list"
              :count="3"
              size="20MB"
              biz-type="FILE_CENTER_DEMO"
              :biz-id="demoBizId"
              purpose="slot-demo"
              :biz-meta="{ scene: 'slot', source: 'upload-demo' }"
              @change="handleChange('插槽用法', $event)"
            >
              <template #trigger>
                <el-button type="primary" plain>
                  选择业务附件
                </el-button>
              </template>
            </MUpload>
          </div>
          <div class="op-btns" @click="toggleCode('slot')">
            <el-icon><component :is="codeVisible.slot ? ArrowUp : ArrowDown" /></el-icon>
            <span>{{ codeVisible.slot ? '隐藏代码' : '显示代码' }}</span>
          </div>
          <CodeBlock v-show="codeVisible.slot" :code="slotCode" />
        </div>
      </section>

      <section id="props" class="doc-section api-section">
        <h2>支持属性</h2>
        <el-table :data="propsTable" size="small" border>
          <el-table-column prop="name" label="属性名" width="150" />
          <el-table-column prop="description" label="说明" min-width="260" />
          <el-table-column prop="type" label="类型" min-width="180" />
          <el-table-column prop="defaultValue" label="默认值" width="120" />
        </el-table>
      </section>

      <section id="model-value" class="doc-section api-section">
        <h2>v-model 返回内容</h2>
        <p>通过 value-type 控制 v-model 的返回结构。只保存附件关系用 token 或 id；需要回显文件名、大小、预览地址、业务扩展字段时使用 record。</p>
        <el-table :data="modelValueTable" size="small" border>
          <el-table-column prop="valueType" label="value-type" width="120" />
          <el-table-column prop="modelValue" label="v-model 返回" min-width="240" />
          <el-table-column prop="scene" label="适用场景" min-width="260" />
        </el-table>
      </section>

      <section id="slot-api" class="doc-section api-section">
        <h2>支持插槽</h2>
        <el-table :data="slotsTable" size="small" border>
          <el-table-column prop="name" label="插槽名" width="180" />
          <el-table-column prop="description" label="说明" min-width="320" />
        </el-table>
      </section>

      <section id="methods" class="doc-section api-section">
        <h2>支持方法 / 事件</h2>
        <el-table :data="eventsTable" size="small" border>
          <el-table-column prop="name" label="名称" width="150" />
          <el-table-column prop="description" label="说明" min-width="260" />
          <el-table-column prop="payload" label="参数" min-width="240" />
        </el-table>
      </section>

      <section id="response" class="doc-section api-section">
        <h2>接口返回报文</h2>
        <p>上传接口返回 FileRecord。组件用于缩略图和预览的显示地址按 directPreviewUrl、previewUrl、directDownloadUrl、downloadUrl、url、下载接口兜底的顺序选择；上传返回缺少直连预览地址时会自动调用 preview 接口补齐。</p>
        <el-table :data="responseTable" size="small" border>
          <el-table-column prop="name" label="字段" width="180" />
          <el-table-column prop="description" label="说明" min-width="260" />
          <el-table-column prop="example" label="示例" min-width="220" />
        </el-table>
        <CodeBlock :code="responseCode" />
      </section>

      <section id="result" class="doc-section result-section">
        <h2>最近上传结果</h2>
        <el-empty v-if="!lastEvent" description="暂无上传结果" :image-size="72" />
        <pre v-else>{{ lastEvent }}</pre>
      </section>
  </DemoDocLayout>
</template>

<script setup lang="ts">
import { defineComponent, h, ref, type PropType } from 'vue';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { MUpload, type FileRecord, type UploadColumn } from '@mango/file';
import DemoDocLayout from './DemoDocLayout.vue';

const demoBizId = `DEMO-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}`;
const imageRecords = ref<FileRecord[]>([]);
const mixedTokens = ref<string[]>([]);
const manualTokens = ref<string[]>([]);
const slotTokens = ref<string[]>([]);
const officeRecords = ref<FileRecord[]>([]);
const tableRecords = ref<FileRecord[]>([]);
const lastEvent = ref('');
const codeVisible = ref<Record<string, boolean>>({
  mixed: false,
  image: false,
  office: false,
  table: false,
  manual: false,
  slot: false,
});
const tocItems = [
  { id: 'default', label: '通用/默认用法' },
  { id: 'features', label: '展示方式' },
  { id: 'manual', label: '手动上传' },
  { id: 'slots', label: '插槽用法' },
  { id: 'props', label: '支持属性' },
  { id: 'model-value', label: 'v-model 返回内容' },
  { id: 'slot-api', label: '支持插槽' },
  { id: 'methods', label: '支持方法 / 事件' },
  { id: 'response', label: '接口返回报文' },
  { id: 'result', label: '最近上传结果' },
];

const sizeRules = {
  image: '10MB',
  video: '200MB',
  document: '80MB',
  archive: '100MB',
  other: '50MB',
};

const bizMeta = {
  source: 'upload-demo',
  applicant: '张三',
  department: '财务部',
};

const tableColumns: UploadColumn[] = [
  { key: 'fileName', label: '文件名', minWidth: 220 },
  { key: 'fileSize', label: '大小', width: 120 },
  { key: 'uploadProgress', label: '上传进度', width: 150 },
  { key: 'createdTime', label: '上传时间', width: 180 },
  { key: 'createdBy', label: '上传账号', width: 120 },
  { key: 'bizId', label: '业务ID', width: 150 },
  { key: 'meta.projectCode', label: '项目编号', width: 140 },
];

const mixedCode = `<script setup lang="ts">
import { ref } from 'vue';
import { MUpload } from '@mango/file';

const files = ref<string[]>([]);
<\/script>

<template>
  <MUpload
    v-model="files"
    :count="6"
    size="50MB"
    :sizes="{ image: '10MB', video: '200MB', document: '80MB', other: '50MB' }"
    biz-type="EXPENSE_REIMBURSEMENT"
    :biz-id="bizId"
    purpose="attachment"
  />
</template>`;

const imageCode = `<MUpload
  v-model="imageRecords"
  fmt="image"
  display="thumbnail"
  value-type="record"
  :count="4"
  size="10MB"
  biz-type="FILE_CENTER_DEMO"
  :biz-id="bizId"
  @change="handleImageChange"
/>

// imageRecords: FileRecord[]
// 返回内容包含 id、fileName、fileSize、contentType、
// directPreviewUrl、directDownloadUrl、bizMeta 等字段。`;

const manualCode = `<MUpload
  v-model="files"
  display="list"
  :auto="false"
  :count="6"
  button-text="选取文件"
  size="50MB"
  biz-type="FILE_CENTER_DEMO"
  :biz-id="bizId"
/>`;

const slotCode = `<MUpload
  v-model="files"
  display="list"
  :count="3"
  size="20MB"
  biz-type="FILE_CENTER_DEMO"
  :biz-id="bizId"
>
  <template #trigger>
    <el-button type="primary" plain>
      选择业务附件
    </el-button>
  </template>
</MUpload>`;

const officeCode = `<MUpload
  v-model="records"
  :fmt="['office', 'pdf', 'odf']"
  display="drag"
  value-type="record"
  :count="3"
  size="80MB"
  biz-type="CONTRACT_REVIEW"
  :biz-id="bizId"
/>`;

const tableCode = `<MUpload
  v-model="records"
  display="table"
  value-type="record"
  :columns="[
    { key: 'fileName', label: '文件名' },
    { key: 'fileSize', label: '大小' },
    { key: 'uploadProgress', label: '上传进度' },
    { key: 'createdTime', label: '上传时间' },
    { key: 'meta.projectCode', label: '项目编号' }
  ]"
  :sizes="{ image: '10MB', document: '80MB', other: '50MB' }"
  biz-type="GUARANTEE_APPLICATION"
  :biz-id="bizId"
  :biz-meta="{ projectCode: 'PRJ-20260517' }"
/>`;

const propsTable = [
  { name: 'v-model', description: '绑定上传结果；实际结构由 valueType 和 count 决定', type: 'string[] | FileRecord[] | string | FileRecord', defaultValue: '-' },
  { name: 'fmt', description: '允许格式；不传表示不限格式', type: 'string | string[]', defaultValue: '-' },
  { name: 'count', description: '文件数量限制', type: 'number', defaultValue: '1' },
  { name: 'size', description: '单文件通用大小限制，支持 50MB / 1GB', type: 'string | number', defaultValue: '-' },
  { name: 'sizes', description: '按类型设置大小限制', type: 'UploadSizeRules', defaultValue: '-' },
  { name: 'display', description: '展示方式', type: 'list | thumbnail | table | drag', defaultValue: 'list' },
  { name: 'auto', description: '是否选择后自动上传；false 时显示手动上传按钮，多文件提交走批量接口', type: 'boolean', defaultValue: 'true' },
  { name: 'columns', description: 'table 展示列，支持 uploadProgress 和 meta.xxx', type: 'UploadColumn[]', defaultValue: 'fileName,fileSize,uploadProgress,createdTime' },
  { name: 'valueType', description: '控制 v-model 返回内容；需要文件名、大小、预览地址、业务元数据时使用 record', type: 'token | id | record', defaultValue: 'token' },
  { name: 'bizType / bizId', description: '业务类型和业务 ID', type: 'string', defaultValue: '-' },
  { name: 'bizMeta', description: '业务自定义参数 JSON', type: 'Record<string, unknown> | string', defaultValue: '-' },
  { name: 'directoryId', description: '文件中心逻辑目录 ID', type: 'string | number', defaultValue: '0' },
];

const modelValueTable = [
  { valueType: 'token', modelValue: '单文件 string；多文件 string[]，示例 mango-file:1935600000000000001', scene: '表单只需要保存附件标识，后续由后端按 token/id 查询文件详情' },
  { valueType: 'id', modelValue: '单文件 string；多文件 string[]，示例 1935600000000000001', scene: '后端接口明确只接收文件 ID，不需要 mango-file: 前缀' },
  { valueType: 'record', modelValue: '单文件 FileRecord；多文件 FileRecord[]', scene: '需要回显文件名、大小、MIME、预览地址、下载地址、bizMeta 等完整文件信息' },
];

const slotsTable = [
  { name: 'trigger', description: '替换 list/table 默认上传按钮，也可作为 thumbnail/drag 的通用兜底插槽' },
  { name: 'thumbnail-trigger', description: '替换 display="thumbnail" 的图片卡片触发区域' },
  { name: 'drag-trigger', description: '替换 display="drag" 的拖拽上传区域' },
];

const eventsTable = [
  { name: 'change', description: '上传列表变化后触发', payload: '(value, records) => void' },
  { name: 'success', description: '单个文件上传成功后触发', payload: '(record: FileRecord) => void' },
  { name: 'error', description: '上传失败后触发', payload: '(error: unknown) => void' },
  { name: 'preview', description: '组件内部点击文件时打开下载/预览地址', payload: '由组件内部处理' },
];

const responseTable = [
  { name: 'id', description: '文件中心主键，前端按字符串处理', example: '1935600000000000001' },
  { name: 'fileName', description: '原始文件名，用于列表、表格和下载文件名', example: '合同附件.pdf' },
  { name: 'fileSize', description: '文件大小，单位 byte', example: '204800' },
  { name: 'contentType', description: '文件 MIME 类型', example: 'application/pdf' },
  { name: 'url', description: '后端返回的可访问地址，组件会归一化为可展示地址', example: '/api/file/files/download?id=...' },
  { name: 'directPreviewUrl', description: '直连预览地址；存在时优先用于图片缩略图和预览', example: 'https://oss.example.com/preview/...' },
  { name: 'directDownloadUrl', description: '直连下载地址；预览地址不存在时用于展示/打开', example: 'https://oss.example.com/download/...' },
  { name: 'directAccess', description: '是否支持直连访问', example: 'true' },
  { name: 'bizMeta', description: '业务扩展参数；组件会尝试解析 JSON 字符串', example: '{"scene":"slot"}' },
];

const responseCode = `{
  "id": "1935600000000000001",
  "fileName": "合同附件.pdf",
  "fileSize": 204800,
  "contentType": "application/pdf",
  "url": "/api/file/files/download?id=1935600000000000001",
  "directAccess": true,
  "directPreviewUrl": "https://oss.example.com/preview/contract.pdf",
  "directDownloadUrl": "https://oss.example.com/download/contract.pdf",
  "directPreviewExpireSeconds": 3600,
  "directDownloadExpireSeconds": 3600,
  "bizMeta": {
    "scene": "slot",
    "source": "upload-demo"
  }
}`;

function handleChange(scene: string, value: unknown) {
  lastEvent.value = JSON.stringify({ scene, value }, null, 2);
}

function handleRecordsChange(value: unknown, records: FileRecord[]) {
  lastEvent.value = JSON.stringify({
    value,
    records: records.map(item => ({
      id: item.id,
      fileName: item.fileName,
      fileSize: item.fileSize,
      url: item.url,
      downloadUrl: item.downloadUrl,
      directPreviewUrl: item.directPreviewUrl,
      directDownloadUrl: item.directDownloadUrl,
      directAccess: item.directAccess,
      createdTime: item.createdTime,
    })),
  }, null, 2);
}

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

const FeatureTags = defineComponent({
  name: 'FeatureTags',
  props: {
    items: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
  },
  setup(props) {
    return () => h('div', { class: 'feature-tags' }, props.items.map(item => h('span', item)));
  },
});

const CodeBlock = defineComponent({
  name: 'CodeBlock',
  props: {
    code: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    return () => h('pre', { class: 'demo-code' }, [
      h('code', props.code),
    ]);
  },
});
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.api-section {
  :deep(.el-table) {
    margin-top: 12px;
  }
}

.demo-block {
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 3px;
  background: var(--el-bg-color);
  transition: box-shadow 0.2s ease;

  &:hover {
    box-shadow: 0 0 8px 0 rgb(232 237 250 / 60%), 0 2px 4px 0 rgb(232 237 250 / 50%);
  }
}

.demo-source {
  padding: 24px;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;

  span {
    display: inline-flex;
    align-items: center;
    height: 24px;
    padding: 0 9px;
    border: 1px solid var(--el-color-primary-light-8);
    border-radius: 3px;
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-size: 12px;
    line-height: 1;
  }
}

.model-value-note {
  margin-top: 16px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-fill-color-lighter);

  span {
    display: block;
    margin-bottom: 6px;
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: var(--el-text-color-regular);
    font-size: 14px;
    line-height: 1.7;
  }

  code {
    color: var(--el-color-primary);
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 13px;
  }
}

.op-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 44px;
  border-top: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-regular);
  font-size: 14px;
  font-weight: 500;
  background: var(--el-bg-color);
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;

  &:hover {
    color: var(--el-color-primary);
    background-color: var(--el-fill-color-light);
  }
}

.demo-code {
  margin: 0;
  padding: 18px 24px;
  overflow: auto;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre;
}

.result-section {
  padding-bottom: 24px;

  pre {
    max-height: 280px;
    margin: 0;
    padding: 16px;
    overflow: auto;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 3px;
    background: var(--el-fill-color-light);
    color: var(--el-text-color-primary);
    font-size: 12px;
    line-height: 1.7;
  }
}

@media (max-width: 760px) {
  .demo-source,
  .demo-code {
    padding: 16px;
  }
}
</style>
