<template>
  <div class="route-container">
    <el-card>
      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索路由名称/路径"
            clearable
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="query.routeType"
            placeholder="请选择"
            clearable
          >
            <el-option
              v-for="item in routeTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="Number(item.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="请选择"
            clearable
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="Number(item.value)"
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

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增路由
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="routeName"
          label="路由名称"
          width="200"
        />
        <el-table-column
          prop="routePath"
          label="路由路径"
        />
        <el-table-column
          prop="routeType"
          label="类型"
          width="80"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="system_route_type"
              :value="row.routeType"
              :type="getRouteTypeTagType(row.routeType)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="sort"
          label="排序"
          width="60"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="sys_normal_disable"
              :value="row.status"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="description"
          label="描述"
          show-overflow-tooltip
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
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑路由' : '新增路由'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="路由名称"
          prop="routeName"
        >
          <el-input
            v-model="form.routeName"
            placeholder="请输入路由名称"
          />
        </el-form-item>
        <el-form-item
          label="路由路径"
          prop="routePath"
        >
          <el-input
            v-model="form.routePath"
            placeholder="请输入路由路径"
          />
        </el-form-item>
        <el-form-item
          label="路由类型"
          prop="routeType"
        >
          <el-radio-group v-model="form.routeType">
            <el-radio
              v-for="item in routeTypeOptions"
              :key="item.value"
              :label="Number(item.value)"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="排序"
          prop="sort"
        >
          <el-input-number
            v-model="form.sort"
            :min="0"
            :max="9999"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="item in statusOptions"
              :key="item.value"
              :label="Number(item.value)"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入描述"
          />
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

<script setup lang="ts" name="SystemRoute">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { DictTag, useDict } from '@mango/common';
import { routeApi, type SysRoute } from '../../api/route';

const { options: routeTypeOptions } = useDict('system_route_type');
const { options: statusOptions } = useDict('sys_normal_disable');

function getRouteTypeTagType(type?: number) {
  if (type === 2) return 'success';
  if (type === 3) return 'warning';
  return '';
}

const loading = ref(false);
const tableData = ref<SysRoute[]>([]);
const query = reactive({
  pageNum: 1,
  pageSize: 100,
  keyword: '',
  routeType: undefined as number | undefined,
  status: undefined as number | undefined,
});

const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<SysRoute>({
  id: undefined,
  routeName: '',
  routePath: '',
  routeType: 1,
  sort: 0,
  status: 1,
  description: '',
});

const rules: FormRules = {
  routeName: [{ required: true, message: '请输入路由名称', trigger: 'blur' }],
  routePath: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  routeType: [{ required: true, message: '请选择路由类型', trigger: 'change' }],
};

async function loadData() {
  loading.value = true;
  try {
    const data = await routeApi.list(query);
    tableData.value = data.list;
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
  query.routeType = undefined;
  query.status = undefined;
  query.pageNum = 1;
  loadData();
}

function handleAdd() {
  form.id = undefined;
  form.routeName = '';
  form.routePath = '';
  form.routeType = 1;
  form.sort = 0;
  form.status = 1;
  form.description = '';
  dialogVisible.value = true;
}

function handleEdit(row: SysRoute) {
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    if (form.id) {
      await routeApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await routeApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch (error) {
    console.error('提交失败:', error);
  }
}

function handleDelete(row: SysRoute) {
  ElMessageBox.confirm('确认删除该路由?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await routeApi.delete(row.id!);
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
.route-container {
  padding: 0;
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
