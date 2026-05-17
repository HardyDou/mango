<template>
  <div class="upload-view">
    <header class="page-header">
      <h1>Upload 文件上传</h1>
      <p>统一使用文件中心 Upload 组件，覆盖通用附件、图片、办公文档和业务附件清单。</p>
    </header>

    <div class="doc-layout">
      <main class="examples">
      <section id="default" class="doc-section">
        <h2>通用/默认用法</h2>
        <p>默认不限制格式，选择文件后自动上传，v-model 返回 mango-file:id 标识；完整文件记录通过 change/success 事件获取。</p>
        <div class="demo-block" data-testid="mixed-upload-panel">
          <div class="demo-source">
            <FeatureTags
              :items="['自动上传', '不限格式', '返回 mango-file:id', '直连预览优先']"
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
            <ResultLine label="返回标识" :value="mixedTokens" />
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
              v-model="imageTokens"
              fmt="image"
              display="thumbnail"
              :count="4"
              size="10MB"
              biz-type="FILE_CENTER_DEMO"
              :biz-id="demoBizId"
              purpose="image"
              :biz-meta="{ scene: 'image', source: 'upload-demo' }"
              @change="handleChange('图片上传', $event)"
            />
            <ResultLine label="返回标识" :value="imageTokens" />
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
            <ResultLine label="文件记录" :value="officeRecords" />
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
            <ResultLine label="文件记录" :value="tableRecords" />
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
            <ResultLine label="返回标识" :value="manualTokens" />
          </div>
          <div class="op-btns" @click="toggleCode('manual')">
            <el-icon><component :is="codeVisible.manual ? ArrowUp : ArrowDown" /></el-icon>
            <span>{{ codeVisible.manual ? '隐藏代码' : '显示代码' }}</span>
          </div>
          <CodeBlock v-show="codeVisible.manual" :code="manualCode" />
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

      <section id="methods" class="doc-section api-section">
        <h2>支持方法 / 事件</h2>
        <el-table :data="eventsTable" size="small" border>
          <el-table-column prop="name" label="名称" width="150" />
          <el-table-column prop="description" label="说明" min-width="260" />
          <el-table-column prop="payload" label="参数" min-width="240" />
        </el-table>
      </section>

      <section id="result" class="doc-section result-section">
        <h2>最近上传结果</h2>
        <el-empty v-if="!lastEvent" description="暂无上传结果" :image-size="72" />
        <pre v-else>{{ lastEvent }}</pre>
      </section>
      </main>
      <aside class="article-toc">
        <button
          v-for="item in tocItems"
          :key="item.id"
          type="button"
          :class="{ active: activeToc === item.id }"
          @click="scrollToSection(item.id)"
        >
          {{ item.label }}
        </button>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineComponent, h, ref, type PropType } from 'vue';
import { ElInput } from 'element-plus';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { MUpload, type FileRecord, type UploadColumn } from '@mango/file';

const demoBizId = `DEMO-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}`;
const imageTokens = ref<string[]>([]);
const mixedTokens = ref<string[]>([]);
const manualTokens = ref<string[]>([]);
const officeRecords = ref<FileRecord[]>([]);
const tableRecords = ref<FileRecord[]>([]);
const lastEvent = ref('');
const codeVisible = ref<Record<string, boolean>>({
  mixed: false,
  image: false,
  office: false,
  table: false,
  manual: false,
});
const activeToc = ref('default');

const tocItems = [
  { id: 'default', label: '通用/默认用法' },
  { id: 'features', label: '展示方式' },
  { id: 'manual', label: '手动上传' },
  { id: 'props', label: '支持属性' },
  { id: 'methods', label: '支持方法 / 事件' },
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
  v-model="imageTokens"
  fmt="image"
  display="thumbnail"
  :count="4"
  size="10MB"
  biz-type="FILE_CENTER_DEMO"
  :biz-id="bizId"
/>`;

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
  { name: 'v-model', description: '绑定上传结果', type: 'string[] | FileRecord[] | string | FileRecord', defaultValue: '-' },
  { name: 'fmt', description: '允许格式；不传表示不限格式', type: 'string | string[]', defaultValue: '-' },
  { name: 'count', description: '文件数量限制', type: 'number', defaultValue: '1' },
  { name: 'size', description: '单文件通用大小限制，支持 50MB / 1GB', type: 'string | number', defaultValue: '-' },
  { name: 'sizes', description: '按类型设置大小限制', type: 'UploadSizeRules', defaultValue: '-' },
  { name: 'display', description: '展示方式', type: 'list | thumbnail | table | drag', defaultValue: 'list' },
  { name: 'auto', description: '是否选择后自动上传；false 时显示手动上传按钮，多文件提交走批量接口', type: 'boolean', defaultValue: 'true' },
  { name: 'columns', description: 'table 展示列，支持 uploadProgress 和 meta.xxx', type: 'UploadColumn[]', defaultValue: 'fileName,fileSize,uploadProgress,createdTime' },
  { name: 'valueType', description: '返回 token 或完整文件记录', type: 'token | record', defaultValue: 'token' },
  { name: 'bizType / bizId', description: '业务类型和业务 ID', type: 'string', defaultValue: '-' },
  { name: 'bizMeta', description: '业务自定义参数 JSON', type: 'Record<string, unknown> | string', defaultValue: '-' },
  { name: 'directoryId', description: '文件中心逻辑目录 ID', type: 'string | number', defaultValue: '0' },
];

const eventsTable = [
  { name: 'change', description: '上传列表变化后触发', payload: '(value, records) => void' },
  { name: 'success', description: '单个文件上传成功后触发', payload: '(record: FileRecord) => void' },
  { name: 'error', description: '上传失败后触发', payload: '(error: unknown) => void' },
  { name: 'preview', description: '组件内部点击文件时打开下载/预览地址', payload: '由组件内部处理' },
];

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
      directAccess: item.directAccess,
      createdTime: item.createdTime,
    })),
  }, null, 2);
}

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

function scrollToSection(id: string) {
  activeToc.value = id;
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

const ResultLine = defineComponent({
  name: 'ResultLine',
  props: {
    label: {
      type: String,
      required: true,
    },
    value: {
      type: [String, Array, Object],
      default: '',
    },
  },
  setup(props) {
    return () => h('div', { class: 'result-line' }, [
      h('span', props.label),
      h(ElInput, {
        modelValue: formatValue(props.value),
        readonly: true,
        size: 'small',
      }),
    ]);
  },
});

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

function formatValue(value: unknown) {
  if (!value) return '';
  if (Array.isArray(value) && value.length === 0) return '';
  return typeof value === 'string' ? value : JSON.stringify(value);
}
</script>

<style scoped lang="scss">
.upload-view {
  width: 100%;
  padding: 32px 40px 64px;
  box-sizing: border-box;
  color: #1f2f3d;
}

.page-header {
  margin-bottom: 32px;

  h1 {
    margin: 0;
    color: #1f2f3d;
    font-size: 28px;
    font-weight: 400;
    letter-spacing: 0;
  }

  p {
    margin: 14px 0 0;
    color: #5e6d82;
    font-size: 14px;
    line-height: 1.8;
  }
}

.examples {
  display: flex;
  flex-direction: column;
  gap: 36px;
  min-width: 0;
}

.doc-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 32px;
  align-items: start;
}

.article-toc {
  position: sticky;
  top: 72px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-left: 14px;
  border-left: 1px solid #eaeefb;

  button {
    padding: 0;
    border: 0;
    background: transparent;
    color: #5e6d82;
    font-size: 13px;
    font-family: inherit;
    line-height: 1.6;
    text-align: left;
    text-decoration: none;
    transition: color 0.2s;
    cursor: pointer;

    &:hover,
    &.active {
      color: #409eff;
    }
  }
}

.doc-section {
  background: transparent;

  h2 {
    margin: 0 0 16px;
    color: #1f2f3d;
    font-size: 22px;
    font-weight: 400;
    line-height: 1.4;
  }

  h3 {
    margin: 28px 0 14px;
    color: #1f2f3d;
    font-size: 18px;
    font-weight: 400;
    line-height: 1.4;
  }

  p {
    margin: 0 0 16px;
    color: #5e6d82;
    font-size: 14px;
    line-height: 1.8;
  }
}

.api-section {
  :deep(.el-table) {
    margin-top: 12px;
  }
}

.demo-block {
  width: 100%;
  overflow: hidden;
  border: 1px solid #ebebeb;
  border-radius: 3px;
  background: #fff;
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
    border: 1px solid #d9ecff;
    border-radius: 3px;
    background: #ecf5ff;
    color: #409eff;
    font-size: 12px;
    line-height: 1;
  }
}

.op-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 44px;
  border-top: 1px solid #eaeefb;
  color: #d3dce6;
  font-size: 13px;
  background: #fff;
  cursor: pointer;
  transition: 0.2s;

  &:hover {
    color: #409eff;
    background-color: #f9fafc;
  }
}

.demo-code {
  margin: 0;
  padding: 18px 24px;
  overflow: auto;
  border-top: 1px solid #eaeefb;
  background: #fafafa;
  color: #334155;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
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
    border: 1px solid #ebebeb;
    border-radius: 3px;
    background: #fafafa;
    color: #1f2937;
    font-size: 12px;
    line-height: 1.7;
  }
}

.result-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;

  span {
    flex: 0 0 auto;
    color: #5e6d82;
    font-size: 12px;
  }
}

@media (max-width: 760px) {
  .upload-view {
    padding: 18px 16px 36px;
  }

  .doc-layout {
    display: block;
  }

  .article-toc {
    display: none;
  }

  .demo-source,
  .demo-code {
    padding: 16px;
  }
}
</style>
