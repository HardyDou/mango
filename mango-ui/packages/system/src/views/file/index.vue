<template>
  <div class="file-container">
    <el-card>
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
          <el-select
            v-model="query.accessLevel"
            placeholder="请选择"
            clearable
          >
            <el-option label="机构私有" value="PRIVATE" />
            <el-option label="公开读取" value="PUBLIC_READ" />
            <el-option label="内部文件" value="INTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.includeArchived">
            包含归档
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-upload
            :show-file-list="false"
            :http-request="handleUpload"
          >
            <el-button type="primary">
              上传文件
            </el-button>
          </el-upload>
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
          prop="accessLevel"
          label="访问级别"
          width="110"
        >
          <template #default="{ row }">
            <el-tag size="small" :type="accessLevelType(row.accessLevel)">
              {{ accessLevelLabel(row.accessLevel) }}
            </el-tag>
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
            <el-button link type="primary" size="small" @click="handlePreview(row)">
              详情
            </el-button>
            <el-button link type="primary" size="small" @click="handleDownload(row)">
              下载
            </el-button>
            <el-button
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
      title="文件详情"
      width="520px"
    >
      <el-descriptions
        v-if="preview"
        :column="1"
        border
      >
        <el-descriptions-item label="文件名">
          {{ preview.fileName }}
        </el-descriptions-item>
        <el-descriptions-item label="文件大小">
          {{ formatSize(preview.fileSize) }}
        </el-descriptions-item>
        <el-descriptions-item label="内容类型">
          {{ preview.contentType || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="扩展名">
          {{ preview.fileExt || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="是否可预览">
          {{ preview.previewable ? '是' : '否' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus';
import Pagination from '@mango/common/components/Pagination/index.vue';
import { downloadFileRecord, fileApi, type FilePreview, type FileQuery, type FileRecord } from '../../api/file';

const loading = ref(false);
const tableData = ref<FileRecord[]>([]);
const total = ref(0);
const previewVisible = ref(false);
const preview = ref<FilePreview | null>(null);

const query = reactive<FileQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  purpose: '',
  accessLevel: '',
  includeArchived: false,
});

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
  loadData();
}

async function handleUpload(options: UploadRequestOptions) {
  try {
    await fileApi.upload(options.file as File, {
      purpose: 'attachment',
      accessLevel: 'PRIVATE',
    });
    ElMessage.success('上传成功');
    options.onSuccess?.({});
    loadData();
  } catch (error) {
    options.onError?.(error as Error);
  }
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

function accessLevelLabel(value?: string) {
  if (value === 'PUBLIC_READ') return '公开读取';
  if (value === 'INTERNAL') return '内部文件';
  return '机构私有';
}

function accessLevelType(value?: string) {
  if (value === 'PUBLIC_READ') return 'success';
  if (value === 'INTERNAL') return 'warning';
  return 'info';
}

function formatSize(size?: number) {
  const value = Number(size || 0);
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(2)} MB`;
  if (value >= 1024) return `${(value / 1024).toFixed(2)} KB`;
  return `${value} B`;
}

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.file-container {
  padding: 0;
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
</style>
