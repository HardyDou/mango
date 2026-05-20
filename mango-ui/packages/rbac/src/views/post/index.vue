<template>
  <div class="post-container">
    <el-card>
      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="岗位名称">
          <el-input
            v-model="query.postName"
            placeholder="请输入岗位名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="岗位编码">
          <el-input
            v-model="query.postCode"
            placeholder="请输入岗位编码"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="query.postStatus"
            dict-type="sys_normal_disable"
            placeholder="状态"
            show-any-option
            any-option-label="不限"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增岗位
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="postName"
          label="岗位名称"
          min-width="160"
        />
        <el-table-column
          prop="postCode"
          label="岗位编码"
          min-width="160"
        />
        <el-table-column
          prop="postSort"
          label="排序"
          width="90"
        />
        <el-table-column
          prop="postStatus"
          label="状态"
          width="90"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="sys_normal_disable"
              :value="row.postStatus"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          label="备注"
          show-overflow-tooltip
        />
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
        >
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
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
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadData"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑岗位' : '新增岗位'"
      width="560px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="岗位名称"
          prop="postName"
        >
          <el-input
            v-model="form.postName"
            placeholder="请输入岗位名称"
          />
        </el-form-item>
        <el-form-item
          label="岗位编码"
          prop="postCode"
        >
          <el-input
            v-model="form.postCode"
            placeholder="请输入岗位编码"
            :disabled="!!form.id"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="postStatus"
        >
          <el-radio-group v-model="form.postStatus">
            <el-radio
              v-for="item in statusOptions"
              :key="item.value"
              :label="String(item.value)"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="排序"
          prop="postSort"
        >
          <el-input-number
            v-model="form.postSort"
            :min="0"
            :max="9999"
          />
        </el-form-item>
        <el-form-item
          label="备注"
          prop="remark"
        >
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemPost">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import DictSelect from '@mango/common/components/DictSelect/index.vue';
import DictTag from '@mango/common/components/DictTag/index.vue';
import Pagination from '@mango/common/components/Pagination/index.vue';
import { useDict } from '@mango/common/hooks/useDict';
import { postApi, type PostVO } from '../../api/post';

const { options: statusOptions } = useDict('sys_normal_disable');

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const tableData = ref<PostVO[]>([]);
const total = ref(0);
const formRef = ref<FormInstance>();

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  postName: '',
  postCode: '',
  postStatus: undefined as string | undefined,
});

const form = reactive<PostVO>({
  id: undefined,
  postName: '',
  postCode: '',
  postSort: 0,
  postStatus: '1',
  remark: '',
  tenantId: 1,
});

const rules: FormRules = {
  postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  postStatus: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

async function loadData() {
  loading.value = true;
  try {
    const data = await postApi.page(query);
    tableData.value = data.list;
    total.value = data.total;
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
  query.postName = '';
  query.postCode = '';
  query.postStatus = undefined;
  loadData();
}

function resetForm() {
  form.id = undefined;
  form.postName = '';
  form.postCode = '';
  form.postSort = 0;
  form.postStatus = '1';
  form.remark = '';
  form.tenantId = 1;
}

function handleAdd() {
  resetForm();
  dialogVisible.value = true;
}

function handleEdit(row: PostVO) {
  Object.assign(form, row);
  form.postStatus = row.postStatus ?? '1';
  form.postSort = row.postSort ?? 0;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    if (form.id) {
      await postApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await postApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    await loadData();
  } finally {
    submitLoading.value = false;
  }
}

function handleDelete(row: PostVO) {
  if (!row.id) return;
  ElMessageBox.confirm(`确认删除岗位「${row.postName}」?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await postApi.delete(row.id!);
    ElMessage.success('删除成功');
    await loadData();
  }).catch(() => {});
}

function formatTime(value?: string) {
  if (!value) return '';
  return value;
}

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.post-container {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-form {
  margin-bottom: 16px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
</style>
