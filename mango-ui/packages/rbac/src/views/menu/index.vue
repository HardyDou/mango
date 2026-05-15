<template>
  <div class="menu-container">
    <el-card>
      <!-- 菜单分组 Tab -->
      <div class="group-tabs">
        <div class="tabs-wrapper">
          <el-tabs v-model="activeGroup" @tab-change="handleGroupChange">
            <el-tab-pane
              v-for="group in menuGroups"
              :key="group.code"
              :label="group.name"
              :name="group.code"
            />
          </el-tabs>
        </div>
        <el-button
          type="primary"
          circle
          plain
          size="small"
          class="add-group-btn"
          @click="handleAddGroup"
        >
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索菜单名称/路径"
            clearable
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="query.menuType"
            placeholder="请选择"
            clearable
          >
            <el-option
              v-for="item in menuTypeOptions"
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
            新增菜单
          </el-button>
        </div>
      </div>

      <!-- 菜单树表格 -->
      <el-table
        v-loading="loading"
        :data="menuTree"
        row-key="menuId"
        default-expand-all
        stripe
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <el-table-column
          prop="menuName"
          label="菜单名称"
          width="200"
        />
        <el-table-column
          prop="path"
          label="路由路径"
        />
        <el-table-column
          prop="component"
          label="组件路径"
          show-overflow-tooltip
        />
        <el-table-column
          prop="menuType"
          label="类型"
          width="80"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="authorization_menu_type"
              :value="row.menuType"
              :type="getMenuTypeTagType(row.menuType)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="icon"
          label="图标"
          width="100"
        >
          <template #default="{ row }">
            <el-icon
              v-if="row.icon"
              size="16"
            >
              <component :is="row.icon" />
            </el-icon>
            <span v-else>-</span>
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
          label="权限标识"
          min-width="200"
        >
          <template #default="{ row }">
            <el-tag
              v-for="perm in (row.permissions || '').split(',').filter(Boolean)"
              :key="perm"
              size="small"
              class="permission-tag"
            >
              {{ perm }}
            </el-tag>
            <span v-if="!row.permissions">-</span>
          </template>
        </el-table-column>
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

    <!-- 新增分组弹窗 -->
    <el-dialog
      v-model="groupDialogVisible"
      title="新增菜单分组"
      width="400px"
    >
      <el-form label-width="80px">
        <el-form-item label="分组名称">
          <el-input
            v-model="newGroupName"
            placeholder="请输入分组名称"
            @keyup.enter="handleSubmitGroup"
          />
        </el-form-item>
        <el-form-item label="唯一标识">
          <el-input
            v-model="newGroupCode"
            placeholder="请输入唯一标识，如：system"
            @keyup.enter="handleSubmitGroup"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmitGroup"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.menuId ? '编辑菜单' : '新增菜单'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="菜单分组"
          prop="groupCode"
        >
          <el-select
            v-model="form.groupCode"
            placeholder="请选择菜单分组"
          >
            <el-option
              v-for="group in menuGroups"
              :key="group.code"
              :label="group.name"
              :value="group.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="父级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="menuTreeSelect"
            :props="{ label: 'menuName', value: 'menuId', children: 'children' }"
            placeholder="请选择父级(不选则为顶级)"
            clearable
            check-strictly
          />
        </el-form-item>
        <el-form-item
          label="菜单类型"
          prop="menuType"
        >
          <el-radio-group v-model="form.menuType">
            <el-radio
              v-for="item in menuTypeOptions"
              :key="item.value"
              :label="Number(item.value)"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="菜单名称"
          prop="menuName"
        >
          <el-input
            v-model="form.menuName"
            placeholder="请输入菜单名称"
          />
        </el-form-item>
        <el-form-item
          label="菜单编码"
          prop="menuCode"
        >
          <el-input
            v-model="form.menuCode"
            placeholder="请输入唯一菜单编码，如：system:user"
          />
        </el-form-item>
        <el-form-item
          label="路由路径"
          prop="path"
        >
          <el-input
            v-model="form.path"
            placeholder="请输入路由路径"
          />
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
          <div class="permissions-tags">
            <el-tag
              v-for="perm in permissionList"
              :key="perm"
              closable
              size="small"
              @close="handleRemovePermission(perm)"
            >
              {{ perm }}
            </el-tag>
            <el-input
              v-if="inputPermissionVisible"
              ref="permissionInputRef"
              v-model="inputPermissionValue"
              size="small"
              class="permission-input"
              placeholder="输入权限码"
              @keyup.enter="handleInputPermission"
              @blur="handleInputPermission"
            />
            <el-button
              v-else
              size="small"
              class="add-permission-btn"
              @click="showPermissionInput"
            >
              + 添加权限
            </el-button>
          </div>
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

<script setup lang="ts" name="SystemMenu">
import { ref, reactive, onMounted, computed, nextTick } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { DictTag, useDict } from '@mango/common';
import { Plus } from '@element-plus/icons-vue';
import { menuApi, type SysMenuVO } from '../../api/menu';

const { options: menuTypeOptions } = useDict('authorization_menu_type');
const { options: statusOptions } = useDict('sys_normal_disable');

function getMenuTypeTagType(type?: number) {
  if (type === 2) return 'success';
  if (type === 3) return 'warning';
  return '';
}

// ==================== 权限标识 Tag 相关 ====================
const permissionInputRef = ref<HTMLInputElement>();
const inputPermissionVisible = ref(false);
const inputPermissionValue = ref('');

// 权限标识字符串转数组
const permissionList = computed({
  get: () => {
    if (!form.permissions) return [];
    return form.permissions.split(',').filter(Boolean);
  },
  set: (val: string[]) => {
    form.permissions = val.join(',');
  },
});

function handleRemovePermission(perm: string) {
  permissionList.value = permissionList.value.filter(p => p !== perm);
}

function showPermissionInput() {
  inputPermissionVisible.value = true;
  nextTick(() => {
    permissionInputRef.value?.focus();
  });
}

function handleInputPermission() {
  const value = inputPermissionValue.value.trim();
  if (value && !permissionList.value.includes(value)) {
    permissionList.value = [...permissionList.value, value];
  }
  inputPermissionVisible.value = false;
  inputPermissionValue.value = '';
}

/**
 * 菜单分组定义
 * - name: 分组显示名称
 * - code: 唯一标识，用于 API 调用加载分组下的菜单
 */
interface MenuGroup {
  code: string;
  name: string;
}

/**
 * 菜单分组列表
 */
const menuGroups = ref<MenuGroup[]>([
  { code: 'internal-admin', name: '管理系统' },
]);

const groupDialogVisible = ref(false);
const newGroupName = ref('');
const newGroupCode = ref('');

function handleAddGroup() {
  newGroupName.value = '';
  newGroupCode.value = '';
  groupDialogVisible.value = true;
}

function handleSubmitGroup() {
  if (!newGroupName.value.trim()) {
    ElMessage.warning('请输入分组名称');
    return;
  }
  if (!newGroupCode.value.trim()) {
    ElMessage.warning('请输入唯一标识');
    return;
  }
  const code = newGroupCode.value.trim();
  if (menuGroups.value.some(g => g.code === code)) {
    ElMessage.warning('唯一标识已存在');
    return;
  }
  menuGroups.value.push({ code, name: newGroupName.value.trim() });
  groupDialogVisible.value = false;
  ElMessage.success('分组创建成功');
}

const activeGroup = ref('internal-admin');

const loading = ref(false);
const tableData = ref<SysMenuVO[]>([]);
const query = reactive({
  keyword: '',
  menuType: undefined as number | undefined,
  status: undefined as number | undefined,
  groupCode: 'internal-admin',
});

const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<SysMenuVO & { groupCode?: string }>({
  menuId: undefined,
  parentId: 0,
  menuType: 2,
  menuName: '',
  menuCode: '',
  path: '',
  component: '',
  icon: '',
  sort: 0,
  status: 1,
  visible: 1,
  permissions: '',
  groupCode: 'internal-admin',
});

const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuCode: [{ required: true, message: '请输入菜单编码', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
};

const menuTree = computed(() => {
  return buildTree(tableData.value);
});

const menuTreeSelect = computed(() => {
  return [{ menuId: 0, menuName: '顶级', children: buildTree(tableData.value) }];
});

function normalizeMenuId(id: string | number | undefined | null) {
  if (id === undefined || id === null || id === '') {
    return '0';
  }
  return String(id);
}

function buildTree(list: SysMenuVO[]): SysMenuVO[] {
  const map: Record<string, SysMenuVO> = {};
  const result: SysMenuVO[] = [];

  list.forEach((item) => {
    map[normalizeMenuId(item.menuId)] = { ...item, children: [] };
  });

  list.forEach((item) => {
    const node = map[normalizeMenuId(item.menuId)];
    const parentId = normalizeMenuId(item.parentId);
    if (parentId === '0' || !map[parentId]) {
      result.push(node);
    } else {
      map[parentId].children!.push(node);
    }
  });

  return result;
}

async function loadData() {
  loading.value = true;
  try {
    const data = await menuApi.getMenus({
      appCode: query.groupCode,
      menuName: query.keyword || undefined,
      type: query.menuType,
      status: query.status,
    });
    tableData.value = data || [];
  } catch (error) {
    console.error('加载数据失败:', error);
  } finally {
    loading.value = false;
  }
}

function handleGroupChange(groupCode: string) {
  query.groupCode = groupCode;
  loadData();
}

function handleSearch() {
  loadData();
}

function handleReset() {
  query.keyword = '';
  query.menuType = undefined;
  query.status = undefined;
  loadData();
}

function handleAdd() {
  form.menuId = undefined;
  form.parentId = 0;
  form.menuType = 2;
  form.menuName = '';
  form.menuCode = '';
  form.path = '';
  form.component = '';
  form.icon = '';
  form.sort = 0;
  form.status = 1;
  form.visible = 1;
  form.permissions = '';
  form.groupCode = activeGroup.value;
  dialogVisible.value = true;
}

function handleAddChild(row: SysMenuVO) {
  form.menuId = undefined;
  form.parentId = row.menuId;
  form.menuType = row.menuType === 1 ? 2 : 3;
  form.menuName = '';
  form.menuCode = '';
  form.path = '';
  form.component = '';
  form.icon = '';
  form.sort = 0;
  form.status = 1;
  form.visible = 1;
  form.permissions = '';
  form.groupCode = activeGroup.value;
  dialogVisible.value = true;
}

function handleEdit(row: SysMenuVO) {
  Object.assign(form, {
    menuId: row.menuId,
    parentId: row.parentId,
    menuType: row.menuType,
    menuName: row.menuName,
    menuCode: row.menuCode,
    path: row.path,
    component: row.component,
    icon: row.icon,
    sort: row.sort,
    status: row.status,
    visible: row.visible,
    permissions: row.permissions,
    groupCode: activeGroup.value,
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    const payload = {
      ...form,
      appCode: form.groupCode || activeGroup.value,
    };
    if (form.menuId) {
      await menuApi.updateMenu(payload);
    } else {
      await menuApi.createMenu(payload);
    }
    ElMessage.success(form.menuId ? '修改成功' : '新增成功');
    dialogVisible.value = false;
    loadData();
  } catch (error) {
    console.error('提交失败:', error);
  }
}

function handleDelete(row: SysMenuVO) {
  ElMessageBox.confirm('确认删除该菜单?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await menuApi.deleteMenu(row.menuId);
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
.menu-container {
  padding: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.group-tabs {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  .tabs-wrapper {
    flex: 1;
    overflow: hidden;
    :deep(.el-tabs__header) {
      margin-bottom: 0;
    }
  }
  .add-group-btn {
    flex-shrink: 0;
    margin-left: 8px;
  }
}
.search-form {
  margin-bottom: 16px;
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
.permissions-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  .el-tag {
    max-width: 150px;
  }
  .permission-input {
    width: 140px;
  }
  .add-permission-btn {
    height: 24px;
    padding: 0 8px;
    font-size: 12px;
  }
}
.permission-tag {
  margin-right: 4px;
  margin-bottom: 2px;
}
</style>
