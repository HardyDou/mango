<template>
  <div class="permission-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>权限管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增权限
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
            placeholder="搜索权限名称/编码"
            clearable
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="query.permissionType"
            placeholder="请选择"
            clearable
          >
            <el-option
              label="菜单权限"
              :value="1"
            />
            <el-option
              label="按钮权限"
              :value="2"
            />
            <el-option
              label="API权限"
              :value="3"
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
          prop="permissionName"
          label="权限名称"
        />
        <el-table-column
          prop="permissionCode"
          label="权限编码"
        />
        <el-table-column
          prop="permissionType"
          label="类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.permissionType === 1 ? 'success' : row.permissionType === 2 ? 'warning' : 'info'"
              size="small"
            >
              {{ row.permissionType === 1 ? '菜单' : row.permissionType === 2 ? '按钮' : 'API' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="resource"
          label="资源路径"
          show-overflow-tooltip
        />
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
      :title="form.id ? '编辑权限' : '新增权限'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="权限名称"
          prop="permissionName"
        >
          <el-input
            v-model="form.permissionName"
            placeholder="请输入权限名称"
          />
        </el-form-item>
        <el-form-item
          label="权限编码"
          prop="permissionCode"
        >
          <el-input
            v-model="form.permissionCode"
            placeholder="请输入权限编码，如: system:user:list"
            :disabled="!!form.id"
          />
        </el-form-item>
        <el-form-item
          label="权限类型"
          prop="permissionType"
        >
          <el-select
            v-model="form.permissionType"
            placeholder="请选择"
          >
            <el-option
              label="菜单权限"
              :value="1"
            />
            <el-option
              label="按钮权限"
              :value="2"
            />
            <el-option
              label="API权限"
              :value="3"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="资源路径"
          prop="resource"
        >
          <el-input
            v-model="form.resource"
            placeholder="请输入资源路径，如: /api/system/user"
          />
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

<script setup lang="ts" name="SystemPermission">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import Pagination from '@/components/Pagination/index.vue';

// ==================== 类型定义 ====================
interface SysPermission {
  id?: number;
  permissionName: string;
  permissionCode: string;
  permissionType: number;
  resource?: string;
  description?: string;
  status?: number;
  createTime?: string;
}

interface PermissionQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  permissionType?: number;
}

// ==================== Mock 数据 ====================
const mockPermissions: SysPermission[] = [
  { id: 1, permissionName: '用户列表', permissionCode: 'system:user:list', permissionType: 2, resource: '/api/system/user/list', description: '查看用户列表', status: 1, createTime: '2024-01-01 10:00:00' },
  { id: 2, permissionName: '用户新增', permissionCode: 'system:user:add', permissionType: 2, resource: '/api/system/user', description: '新增用户', status: 1, createTime: '2024-01-01 10:01:00' },
  { id: 3, permissionName: '用户编辑', permissionCode: 'system:user:edit', permissionType: 2, resource: '/api/system/user', description: '编辑用户', status: 1, createTime: '2024-01-01 10:02:00' },
  { id: 4, permissionName: '用户删除', permissionCode: 'system:user:delete', permissionType: 2, resource: '/api/system/user/:id', description: '删除用户', status: 1, createTime: '2024-01-01 10:03:00' },
  { id: 5, permissionName: '角色列表', permissionCode: 'system:role:list', permissionType: 2, resource: '/api/system/role/list', description: '查看角色列表', status: 1, createTime: '2024-01-02 10:00:00' },
  { id: 6, permissionName: '角色新增', permissionCode: 'system:role:add', permissionType: 2, resource: '/api/system/role', description: '新增角色', status: 1, createTime: '2024-01-02 10:01:00' },
  { id: 7, permissionName: '角色编辑', permissionCode: 'system:role:edit', permissionType: 2, resource: '/api/system/role', description: '编辑角色', status: 1, createTime: '2024-01-02 10:02:00' },
  { id: 8, permissionName: '菜单查看', permissionCode: 'system:menu:list', permissionType: 1, resource: '/api/system/menu/list', description: '查看菜单', status: 1, createTime: '2024-01-03 10:00:00' },
  { id: 9, permissionName: '字典管理', permissionCode: 'system:dict:list', permissionType: 1, resource: '/api/system/dict/list', description: '查看字典', status: 1, createTime: '2024-01-04 10:00:00' },
  { id: 10, permissionName: '系统配置', permissionCode: 'system:config:edit', permissionType: 2, resource: '/api/system/config', description: '修改系统配置', status: 1, createTime: '2024-01-05 10:00:00' },
  { id: 11, permissionName: 'API权限', permissionCode: 'system:api:access', permissionType: 3, resource: '/api/**', description: 'API访问权限', status: 1, createTime: '2024-01-06 10:00:00' },
];

// ==================== 状态 ====================
const loading = ref(false);
const tableData = ref<SysPermission[]>([]);
const total = ref(0);
const query = reactive<PermissionQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  permissionType: undefined,
});

const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<SysPermission>({
  id: undefined,
  permissionName: '',
  permissionCode: '',
  permissionType: 1,
  resource: '',
  description: '',
  status: 1,
});

const rules: FormRules = {
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permissionCode: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
};

// ==================== 方法 ====================
function loadData() {
  loading.value = true;
  // 模拟分页
  let filtered = mockPermissions;
  if (query.keyword) {
    filtered = filtered.filter(
      p => p.permissionName.includes(query.keyword!) || p.permissionCode.includes(query.keyword!)
    );
  }
  if (query.permissionType) {
    filtered = filtered.filter(p => p.permissionType === query.permissionType);
  }
  total.value = filtered.length;
  const start = (query.pageNum - 1) * query.pageSize;
  const end = start + query.pageSize;
  tableData.value = filtered.slice(start, end);
  loading.value = false;
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.keyword = '';
  query.permissionType = undefined;
  query.pageNum = 1;
  loadData();
}

function handleAdd() {
  form.id = undefined;
  form.permissionName = '';
  form.permissionCode = '';
  form.permissionType = 1;
  form.resource = '';
  form.description = '';
  form.status = 1;
  dialogVisible.value = true;
}

function handleEdit(row: SysPermission) {
  Object.assign(form, row);
  dialogVisible.value = true;
}

function handleSubmit() {
  if (!formRef.value) return;
  formRef.value.validate((valid) => {
    if (valid) {
      ElMessage.success(form.id ? '修改成功' : '新增成功');
      dialogVisible.value = false;
      loadData();
    }
  });
}

function handleDelete(row: SysPermission) {
  ElMessageBox.confirm('确认删除该权限?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    ElMessage.success('删除成功');
    loadData();
  }).catch(() => {});
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.permission-container {
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
