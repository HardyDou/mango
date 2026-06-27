<template>
  <div class="file-storage-container">
    <el-card>
      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索名称/桶/地址"
            clearable
          />
        </el-form-item>
        <el-form-item label="存储类型">
          <el-select
            v-model="query.storageType"
            placeholder="请选择"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in storageTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="默认启用">
          <el-select
            v-model="query.active"
            placeholder="请选择"
            clearable
            style="width: 120px"
          >
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="请选择"
            clearable
            style="width: 120px"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-auth="'file:storage-configs:list'" type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button v-auth="'file:storage-configs:list'" @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button v-auth="'file:storage-configs:add'" type="primary" @click="handleAdd">
            新增配置
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="configName"
          label="配置名称"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="storageType"
          label="存储类型"
          width="130"
        >
          <template #default="{ row }">
            {{ storageTypeLabel(row.storageType) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="bucketName"
          label="存储桶"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="storagePath"
          label="存储路径"
          min-width="160"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.storagePath || '/' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="endpoint"
          label="接入地址"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          prop="region"
          label="区域"
          width="120"
          show-overflow-tooltip
        />
        <el-table-column
          label="默认"
          width="90"
        >
          <template #default="{ row }">
            <el-tag v-if="row.active" type="success" size="small">
              默认
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          width="90"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="updatedTime"
          label="更新时间"
          width="180"
        />
        <el-table-column
          label="操作"
          width="260"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button v-auth="'file:storage-configs:test'" link type="primary" size="small" @click="handleTestSaved(row)">
              测试
            </el-button>
            <el-button
              v-auth="'file:storage-configs:active'"
              v-if="!row.active && row.status === 1"
              link
              type="primary"
              size="small"
              @click="handleActivate(row)"
            >
              设为默认
            </el-button>
            <el-button v-auth="'file:storage-configs:edit'" link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-auth="'file:storage-configs:delete'"
              v-if="!row.active"
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
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
      v-model="dialogVisible"
      :title="dialogTitle"
      width="720px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="配置名称" prop="configName">
              <el-input v-model="form.configName" placeholder="如 MinIO 本地联调" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存储类型" prop="storageType">
              <el-select v-model="form.storageType" placeholder="请选择" style="width: 100%" @change="handleStorageTypeChange">
                <el-option
                  v-for="item in storageTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="存储桶" prop="bucketName">
              <el-input v-model="form.bucketName" placeholder="bucket 名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="区域" prop="region">
              <el-input v-model="form.region" :placeholder="regionPlaceholder" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="存储路径">
          <el-input v-model="form.storagePath" placeholder="可选，如 prod/files；对象会落到该路径前缀下" />
        </el-form-item>

        <el-form-item label="接入地址" prop="endpoint">
          <el-input v-model="form.endpoint" :placeholder="endpointPlaceholder" />
        </el-form-item>

        <el-form-item label="浏览器访问地址">
          <el-input v-model="form.publicEndpoint" placeholder="如 http://file.mango.io:9000；用于直连预览/下载签名" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="AccessKey" prop="accessKey">
              <el-input v-model="form.accessKey" placeholder="本地存储可为空" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SecretKey" prop="secretKey">
              <el-input
                v-model="form.secretKey"
                type="password"
                show-password
                :placeholder="form.id && form.secretConfigured ? '留空表示不修改' : '请输入 SecretKey'"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="Path Style">
              <el-switch v-model="form.pathStyleAccess" active-text="开启" inactive-text="关闭" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="HTTPS">
              <el-switch v-model="form.sslEnabled" active-text="开启" inactive-text="关闭" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="设为默认">
              <el-switch v-model="form.active" active-text="是" inactive-text="否" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">
              启用
            </el-radio>
            <el-radio :value="0">
              停用
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="配置用途或注意事项"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button v-auth="'file:storage-configs:test'" @click="handleTestEditing">
          测试连接
        </el-button>
        <el-button
          v-auth="form.id ? 'file:storage-configs:edit' : 'file:storage-configs:add'"
          type="primary"
          :loading="saving"
          @click="handleSubmit"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Pagination } from '@mango/common';
import {
  fileStorageApi,
  storageTypeLabel,
  storageTypeOptions,
  type FileStorageConfig,
  type FileStorageConfigQuery,
  type FileStorageType,
} from '../../api/fileStorage';

const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('新增存储配置');
const tableData = ref<FileStorageConfig[]>([]);
const total = ref(0);
const formRef = ref<FormInstance>();

const query = reactive<FileStorageConfigQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  storageType: '',
  active: '',
  status: undefined,
});

const form = reactive<FileStorageConfig>({
  configName: '',
  storageType: 'LOCAL',
  endpoint: '',
  publicEndpoint: '',
  region: '',
  bucketName: 'local',
  storagePath: '',
  accessKey: '',
  secretKey: '',
  secretConfigured: false,
  pathStyleAccess: false,
  sslEnabled: false,
  active: false,
  status: 1,
  remark: '',
});

const rules: FormRules = {
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  storageType: [{ required: true, message: '请选择存储类型', trigger: 'change' }],
  bucketName: [{ required: true, message: '请输入存储桶', trigger: 'blur' }],
  accessKey: [{
    validator: (_rule, value, callback) => {
      if (form.storageType !== 'LOCAL' && !value) callback(new Error('请输入 AccessKey'));
      else callback();
    },
    trigger: 'blur',
  }],
  secretKey: [{
    validator: (_rule, value, callback) => {
      if (form.storageType !== 'LOCAL' && !form.id && !value) callback(new Error('请输入 SecretKey'));
      else callback();
    },
    trigger: 'blur',
  }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

const endpointPlaceholder = computed(() => {
  const map: Partial<Record<FileStorageType, string>> = {
    LOCAL: '本地存储可为空',
    MINIO: 'http://127.0.0.1:9000',
    S3: 'https://s3.example.com',
    AWS_S3: 'AWS S3 可为空或填写自定义 endpoint',
    ALIYUN_OSS: 'https://oss-cn-hangzhou.aliyuncs.com',
    TENCENT_COS: '腾讯云 COS 通常无需填写 endpoint',
    QINIU_KODO: '七牛云通常配置公开地址即可',
  };
  return map[form.storageType] || '';
});

const regionPlaceholder = computed(() => {
  const map: Partial<Record<FileStorageType, string>> = {
    AWS_S3: 'us-east-1',
    ALIYUN_OSS: 'cn-hangzhou',
    TENCENT_COS: 'ap-guangzhou',
    QINIU_KODO: 'huadong / z0 / auto',
  };
  return map[form.storageType] || '可选';
});

async function loadData() {
  loading.value = true;
  try {
    const result = await fileStorageApi.page(query);
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
  query.storageType = '';
  query.active = '';
  query.status = undefined;
  loadData();
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    configName: '',
    storageType: 'LOCAL',
    endpoint: '',
    publicEndpoint: '',
    region: '',
    bucketName: 'local',
    storagePath: '',
    accessKey: '',
    secretKey: '',
    secretConfigured: false,
    pathStyleAccess: false,
    sslEnabled: false,
    active: false,
    status: 1,
    remark: '',
  });
}

function handleAdd() {
  resetForm();
  dialogTitle.value = '新增存储配置';
  dialogVisible.value = true;
}

async function handleEdit(row: FileStorageConfig) {
  resetForm();
  if (!row.id) return;
  const detail = await fileStorageApi.detail(row.id);
  Object.assign(form, detail, { secretKey: '' });
  dialogTitle.value = '编辑存储配置';
  dialogVisible.value = true;
}

function handleStorageTypeChange(value: FileStorageType) {
  if (value === 'LOCAL') {
    form.bucketName = form.bucketName || 'local';
    form.endpoint = '';
    form.accessKey = '';
    form.secretKey = '';
    form.pathStyleAccess = false;
  }
  if (value === 'MINIO') {
    form.bucketName = form.bucketName === 'local' ? 'mango-file' : form.bucketName;
    form.endpoint = form.endpoint || 'http://127.0.0.1:9000';
    form.publicEndpoint = form.publicEndpoint || 'http://file.mango.io:9000';
    form.pathStyleAccess = true;
  }
}

async function handleSubmit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (form.id) {
      await fileStorageApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await fileStorageApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

async function handleTestEditing() {
  await formRef.value?.validate();
  const result = await fileStorageApi.test({ config: form });
  ElMessage.success(result.message || '连接测试通过');
}

async function handleTestSaved(row: FileStorageConfig) {
  const result = await fileStorageApi.test({ id: row.id });
  ElMessage.success(result.message || '连接测试通过');
}

async function handleActivate(row: FileStorageConfig) {
  await ElMessageBox.confirm(`确认将“${row.configName}”设为默认文件存储？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  });
  if (!row.id) return;
  await fileStorageApi.activate(row.id);
  ElMessage.success('已设为默认存储');
  loadData();
}

async function handleDelete(row: FileStorageConfig) {
  await ElMessageBox.confirm(`确认删除存储配置“${row.configName}”？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  });
  if (!row.id) return;
  await fileStorageApi.delete(row.id);
  ElMessage.success('删除成功');
  loadData();
}

onMounted(loadData);
</script>

<style scoped>
.file-storage-container {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-form {
  margin-bottom: 16px;
}
</style>
