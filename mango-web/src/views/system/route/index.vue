<template>
  <div class="route-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>路由管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增路由
          </el-button>
        </div>
      </template>

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
              label="菜单"
              :value="1"
            />
            <el-option
              label="按钮"
              :value="2"
            />
            <el-option
              label="API"
              :value="3"
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
              label="启用"
              :value="1"
            />
            <el-option
              label="禁用"
              :value="0"
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

      <!-- 路由树表格 -->
      <el-table
        v-loading="loading"
        :data="routeTree"
        row-key="id"
        default-expand-all
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
          prop="component"
          label="组件路径"
          show-overflow-tooltip
        />
        <el-table-column
          prop="routeType"
          label="类型"
          width="80"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.routeType === 1"
              size="small"
            >
              菜单
            </el-tag>
            <el-tag
              v-else-if="row.routeType === 2"
              type="warning"
              size="small"
            >
              按钮
            </el-tag>
            <el-tag
              v-else
              type="info"
              size="small"
            >
              API
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="icon"
          label="图标"
          width="100"
        />
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
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="permission"
          label="权限标识"
        />
        <el-table-column
          label="操作"
          width="200"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleAddChild(row)"
            >
              新增子级
            </el-button>
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
        <el-form-item label="父级路由">
          <el-tree-select
            v-model="form.parentId"
            :data="routeTreeSelect"
            :props="{ label: 'routeName', value: 'id', children: 'children' }"
            placeholder="请选择父级(不选则为顶级)"
            clearable
            check-strictly
          />
        </el-form-item>
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
            <el-radio :label="1">
              菜单
            </el-radio>
            <el-radio :label="2">
              按钮
            </el-radio>
            <el-radio :label="3">
              API
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="组件路径"
          prop="component"
        >
          <el-input
            v-model="form.component"
            placeholder="前端组件路径，如：/views/system/user/index.vue"
          />
        </el-form-item>
        <el-form-item label="图标">
          <el-input
            v-model="form.icon"
            placeholder="Element Plus 图标名"
          />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input
            v-model="form.permission"
            placeholder="如：system:user:list"
          />
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
            <el-radio :label="1">
              启用
            </el-radio>
            <el-radio :label="0">
              禁用
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
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { routeApi, type SysRoute } from '@/api/admin/route';

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
  parentId: 0,
  routeName: '',
  routePath: '',
  routeType: 1,
  component: '',
  redirect: '',
  icon: '',
  permission: '',
  sort: 0,
  status: 1,
  description: '',
});

const rules: FormRules = {
  routeName: [{ required: true, message: '请输入路由名称', trigger: 'blur' }],
  routePath: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  routeType: [{ required: true, message: '请选择路由类型', trigger: 'change' }],
};

const routeTree = computed(() => {
  return buildTree(tableData.value);
});

const routeTreeSelect = computed(() => {
  return [{ id: 0, routeName: '顶级', children: buildTree(tableData.value) }];
});

function buildTree(list: SysRoute[]): SysRoute[] {
  const map: Record<number, SysRoute> = {};
  const result: SysRoute[] = [];

  list.forEach((item) => {
    map[item.id!] = { ...item, children: [] };
  });

  list.forEach((item) => {
    const node = map[item.id!];
    if (item.parentId === 0 || !map[item.parentId!]) {
      result.push(node);
    } else {
      map[item.parentId!].children!.push(node);
    }
  });

  return result;
}

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
  form.parentId = 0;
  form.routeName = '';
  form.routePath = '';
  form.routeType = 1;
  form.component = '';
  form.redirect = '';
  form.icon = '';
  form.permission = '';
  form.sort = 0;
  form.status = 1;
  form.description = '';
  dialogVisible.value = true;
}

function handleAddChild(row: SysRoute) {
  form.id = undefined;
  form.parentId = row.id!;
  form.routeName = '';
  form.routePath = '';
  form.routeType = row.routeType === 3 ? 3 : 2; // 如果父级是API，子级也是API；否则默认按钮
  form.component = '';
  form.redirect = '';
  form.icon = '';
  form.permission = '';
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
