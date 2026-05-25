<template>
  <div class="file-container">
    <aside class="directory-panel">
      <div class="directory-header">
        <span>目录</span>
        <div class="directory-actions">
          <el-button v-auth="'file:directories:add'" link type="primary" @click="handleAddDirectory">
            新建
          </el-button>
          <el-dropdown v-if="selectedDirectoryId !== '0'" trigger="click" @command="handleDirectoryCommand">
            <el-button link type="primary">
              更多
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-auth="'file:directories:edit'" command="edit">
                  重命名
                </el-dropdown-item>
                <el-dropdown-item v-auth="'file:directories:delete'" command="delete">
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <el-tree
        v-loading="directoryLoading"
        :data="directoryTree"
        node-key="id"
        :props="{ label: 'directoryName', children: 'children' }"
        :current-node-key="selectedDirectoryId"
        default-expand-all
        highlight-current
        @node-click="handleDirectoryClick"
      />
    </aside>

    <el-card class="file-main">
      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索文件名/业务信息"
            clearable
          />
        </el-form-item>
        <el-form-item label="用途">
          <el-input
            v-model="query.purpose"
            placeholder="如 attachment"
            clearable
          />
        </el-form-item>
        <el-form-item label="访问级别">
          <DictSelect
            v-model="query.accessLevel"
            dict-type="file_access_level"
            placeholder="全部级别"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.includeArchived">
            包含归档
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button v-auth="'file:files:list'" type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button v-auth="'file:files:list'" @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="action-toolbar">
        <div class="toolbar-left">
          <span class="current-directory">{{ currentDirectoryName }}</span>
          <MUpload
            v-auth="'file:files:upload'"
            :count="20"
            :size="uploadSize"
            :fmt="uploadFormats"
            :directory-id="selectedDirectoryId"
            :biz-meta="{ source: 'file-center' }"
            button-text="上传文件"
            @success="handleUploadSuccess"
          />
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="fileName"
          label="文件名"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          prop="purpose"
          label="用途"
          width="120"
        />
        <el-table-column
          prop="bizId"
          label="业务ID"
          min-width="130"
          show-overflow-tooltip
        />
        <el-table-column
          prop="accessLevel"
          label="访问级别"
          width="110"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="file_access_level"
              :value="row.accessLevel"
              :type="accessLevelType(row.accessLevel)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="fileSize"
          label="大小"
          width="110"
        >
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="contentType"
          label="类型"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="storageType"
          label="存储方式"
          width="110"
        >
          <template #default="{ row }">
            <el-tag size="small" type="info">
              {{ storageTypeLabel(row.storageType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdBy"
          label="上传账号"
          width="110"
        />
        <el-table-column
          label="存储位置"
          min-width="260"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <div class="storage-location">
              <span>{{ storageLocation(row) }}</span>
              <el-button
                link
                type="primary"
                size="small"
                @click.stop="copyStorageLocation(row)"
              >
                复制
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="90"
        >
          <template #default="{ row }">
            <el-tag size="small" :type="row.archived === 1 ? 'info' : 'success'">
              {{ row.archived === 1 ? '已归档' : '可用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdTime"
          label="创建时间"
          width="180"
        />
        <el-table-column
          label="操作"
          width="190"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button v-auth="'file:files:query'" link type="primary" size="small" @click="handlePreview(row)">
              预览
            </el-button>
            <el-button v-auth="'file:files:download'" link type="primary" size="small" @click="handleDownload(row)">
              下载
            </el-button>
            <el-button
              v-auth="'file:files:archive'"
              v-if="row.archived !== 1"
              link
              type="danger"
              size="small"
              @click="handleArchive(row)"
            >
              归档
            </el-button>
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

    <el-dialog
      v-model="previewVisible"
      title="文件预览"
      width="840px"
    >
      <FilePreviewPanel
        :file-id="preview?.id"
        :preview="preview"
        :preview-provider-url="settings.previewProviderUrl"
        :preview-external-extensions="settings.previewExternalExtensions"
      />
    </el-dialog>

    <el-dialog
      v-model="directoryDialogVisible"
      :title="directoryDialogTitle"
      width="420px"
      destroy-on-close
    >
      <el-form :model="directoryForm" label-width="90px">
        <el-form-item label="父目录">
          <el-input :model-value="currentDirectoryName" disabled />
        </el-form-item>
        <el-form-item label="目录名称">
          <el-input v-model="directoryForm.directoryName" placeholder="请输入目录名称" maxlength="128" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="directoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="directorySaving" @click="handleSaveDirectory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { DictSelect, DictTag, Pagination } from '@mango/common';
import { downloadFileRecord, fileApi, type FilePreview, type FileQuery, type FileRecord } from '../../api/file';
import { fileDirectoryApi, rootDirectory, type FileDirectory } from '../../api/fileDirectory';
import { defaultFileSettings, fileSettingsApi, formatBytes, type FileSettings } from '../../api/fileSettings';
import FilePreviewPanel from '../../components/FilePreviewPanel.vue';
import MUpload from '../../components/MUpload.vue';

const loading = ref(false);
const directoryLoading = ref(false);
const directorySaving = ref(false);
const tableData = ref<FileRecord[]>([]);
const total = ref(0);
const previewVisible = ref(false);
const directoryDialogVisible = ref(false);
const preview = ref<FilePreview | null>(null);
const settings = reactive<FileSettings>({ ...defaultFileSettings });
const directoryTree = ref<FileDirectory[]>([{ ...rootDirectory }]);
const selectedDirectoryId = ref('0');
const directoryForm = reactive({
  id: '',
  directoryName: '',
});
const directoryMode = ref<'create' | 'edit'>('create');

const query = reactive<FileQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  purpose: '',
  accessLevel: '',
  includeArchived: false,
  directoryId: '0',
});

const currentDirectoryName = computed(() => {
  const found = findDirectory(directoryTree.value, selectedDirectoryId.value);
  return found?.directoryName || '全部文件';
});
const uploadSize = computed(() => `${Math.floor(settings.maxSize / 1024 / 1024)}MB`);
const uploadFormats = computed(() => settings.allowedExtensions.length ? settings.allowedExtensions : undefined);

async function loadData() {
  loading.value = true;
  try {
    const result = await fileApi.page(query);
    tableData.value = result.list;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

async function loadSettings() {
  try {
    Object.assign(settings, await fileSettingsApi.get());
  } catch {
    Object.assign(settings, defaultFileSettings);
  }
}

async function loadDirectories() {
  directoryLoading.value = true;
  try {
    const children = await fileDirectoryApi.tree();
    directoryTree.value = [{ ...rootDirectory, children }];
  } finally {
    directoryLoading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.keyword = '';
  query.purpose = '';
  query.accessLevel = '';
  query.includeArchived = false;
  query.directoryId = selectedDirectoryId.value;
  loadData();
}

function handleUploadSuccess() {
  ElMessage.success('上传成功');
  loadData();
}

function handleDirectoryClick(data: FileDirectory) {
  selectedDirectoryId.value = String(data.id || '0');
  query.directoryId = selectedDirectoryId.value;
  handleSearch();
}

function handleAddDirectory() {
  directoryMode.value = 'create';
  directoryForm.id = '';
  directoryForm.directoryName = '';
  directoryDialogVisible.value = true;
}

const directoryDialogTitle = computed(() => directoryMode.value === 'edit' ? '重命名目录' : '新建目录');

function handleDirectoryCommand(command: 'edit' | 'delete') {
  if (command === 'edit') {
    handleEditDirectory();
  } else {
    handleDeleteDirectory();
  }
}

function handleEditDirectory() {
  const directory = findDirectory(directoryTree.value, selectedDirectoryId.value);
  if (!directory || selectedDirectoryId.value === '0') return;
  directoryMode.value = 'edit';
  directoryForm.id = String(directory.id);
  directoryForm.directoryName = directory.directoryName;
  directoryDialogVisible.value = true;
}

async function handleSaveDirectory() {
  const name = directoryForm.directoryName.trim();
  if (!name) {
    ElMessage.error('请输入目录名称');
    return;
  }
  directorySaving.value = true;
  try {
    if (directoryMode.value === 'edit') {
      await fileDirectoryApi.update({
        id: directoryForm.id,
        directoryName: name,
        status: 1,
        sort: 0,
      });
      ElMessage.success('目录修改成功');
    } else {
      await fileDirectoryApi.create({
        parentId: selectedDirectoryId.value,
        directoryName: name,
        status: 1,
        sort: 0,
      });
      ElMessage.success('目录创建成功');
    }
    directoryDialogVisible.value = false;
    await loadDirectories();
  } finally {
    directorySaving.value = false;
  }
}

async function handleDeleteDirectory() {
  const directory = findDirectory(directoryTree.value, selectedDirectoryId.value);
  if (!directory || selectedDirectoryId.value === '0') return;
  await ElMessageBox.confirm(`确认删除目录“${directory.directoryName}”？只能删除空目录。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  });
  await fileDirectoryApi.delete(directory.id);
  ElMessage.success('目录删除成功');
  selectedDirectoryId.value = '0';
  query.directoryId = '0';
  await loadDirectories();
  await loadData();
}

async function handlePreview(row: FileRecord) {
  preview.value = await fileApi.preview(row.id);
  previewVisible.value = true;
}

async function handleDownload(row: FileRecord) {
  await downloadFileRecord(row);
}

function handleArchive(row: FileRecord) {
  ElMessageBox.confirm(`确认归档文件“${row.fileName}”？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await fileApi.archive(row.id, '页面归档');
    ElMessage.success('归档成功');
    loadData();
  }).catch(() => {});
}

function accessLevelType(value?: string) {
  if (value === 'PUBLIC_READ') return 'success';
  if (value === 'INTERNAL') return 'warning';
  return 'info';
}

function storageTypeLabel(value?: string) {
  if (value === 'LOCAL') return '本地';
  if (value === 'MINIO') return 'MinIO';
  if (value === 'AWS_S3') return 'AWS S3';
  if (value === 'ALIYUN_OSS') return '阿里云 OSS';
  if (value === 'TENCENT_COS') return '腾讯云 COS';
  if (value === 'QINIU_KODO') return '七牛 Kodo';
  if (value === 'S3') return 'S3';
  return value || '-';
}

function storageLocation(row: FileRecord) {
  const bucket = row.bucketName || '-';
  const objectName = row.objectName || '-';
  return `${bucket}/${objectName}`;
}

function findDirectory(nodes: FileDirectory[], id: string): FileDirectory | undefined {
  for (const node of nodes) {
    if (String(node.id) === id) return node;
    const found = findDirectory(node.children || [], id);
    if (found) return found;
  }
  return undefined;
}

async function copyStorageLocation(row: FileRecord) {
  const value = storageLocation(row);
  await navigator.clipboard.writeText(value);
  ElMessage.success('存储位置已复制');
}

function formatSize(size?: number) {
  const value = Number(size || 0);
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(2)} MB`;
  if (value >= 1024) return `${(value / 1024).toFixed(2)} KB`;
  return `${value} B`;
}

onMounted(() => {
  query.directoryId = selectedDirectoryId.value;
  loadSettings();
  loadDirectories();
  loadData();
});
</script>

<style scoped>
.file-container {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 16px;
  padding: 0;
}

.directory-panel {
  min-height: 520px;
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.directory-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
}

.directory-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.file-main {
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.search-form {
  margin-bottom: 16px;
}

.current-directory {
  margin-right: 12px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.storage-location {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.storage-location span {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--el-font-family);
}

@media (max-width: 960px) {
  .file-container {
    grid-template-columns: 1fr;
  }

  .directory-panel {
    min-height: auto;
  }
}
</style>
