<template>
  <div class="param-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统参数</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增参数
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索参数键/描述"
            clearable
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="query.paramType"
            placeholder="请选择"
            clearable
          >
            <el-option
              label="系统参数"
              :value="1"
            />
            <el-option
              label="应用参数"
              :value="2"
            />
          </el-select>
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

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="paramKey"
          label="参数键"
        />
        <el-table-column
          prop="paramValue"
          label="参数值"
          show-overflow-tooltip
        />
        <el-table-column
          prop="paramType"
          label="类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.paramType === 1 ? 'success' : 'warning'"
              size="small"
            >
              {{ row.paramType === 1 ? '系统' : '应用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="description"
          label="描述"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
        />
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

      <!-- 分页 -->
      <Pagination
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadData"
      />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑参数' : '新增参数'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="参数键"
          prop="paramKey"
        >
          <el-input
            v-model="form.paramKey"
            placeholder="请输入参数键"
            :disabled="!!form.id"
          />
        </el-form-item>
        <el-form-item
          label="参数值"
          prop="paramValue"
        >
          <el-input
            v-model="form.paramValue"
            placeholder="请输入参数值"
          />
        </el-form-item>
        <el-form-item
          label="参数类型"
          prop="paramType"
        >
          <el-radio-group v-model="form.paramType">
            <el-radio :label="1">
              系统参数
            </el-radio>
            <el-radio :label="2">
              应用参数
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="描述"
          prop="description"
        >
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="form.status">
            <el-radio :label="1">
              启用
            </el-radio>
            <el-radio :label="0">
              禁用
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemParam">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import Pagination from '@/components/Pagination/index.vue';
import { paramApi, type SysParam } from '@/api/admin/param';

const loading = ref(false);
const tableData = ref<SysParam[]>([]);
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  paramType: undefined as number | undefined,
});

const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<SysParam>({
  id: undefined,
  paramKey: '',
  paramValue: '',
  paramType: 1,
  description: '',
  status: 1,
});

const rules: FormRules = {
  paramKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
  paramValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }],
};

async function loadData() {
  loading.value = true;
  try {
    const data = await paramApi.list(query);
    tableData.value = data.list;
    total.value = data.total;
  } catch (error) {
    console.error('加载数据失败:', error);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.keyword = '';
  query.paramType = undefined;
  query.pageNum = 1;
  loadData();
}

function handleAdd() {
  form.id = undefined;
  form.paramKey = '';
  form.paramValue = '';
  form.paramType = 1;
  form.description = '';
  form.status = 1;
  dialogVisible.value = true;
}

function handleEdit(row: SysParam) {
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    if (form.id) {
      await paramApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await paramApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch (error) {
    console.error('提交失败:', error);
  }
}

function handleDelete(row: SysParam) {
  ElMessageBox.confirm('确认删除该参数?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await paramApi.delete(row.id!);
      ElMessage.success('删除成功');
      loadData();
    } catch (error) {
      console.error('删除失败:', error);
    }
  }).catch(() => {});
}

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.param-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 16px;
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
</style>
