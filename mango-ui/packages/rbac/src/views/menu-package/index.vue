<template>
  <div class="menu-package-container">
    <el-card>
      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索套餐名称/编码" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="query.status"
            dict-type="sys_normal_disable"
            placeholder="状态"
            show-any-option
            any-option-label="不限"
            number-value
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="handleAdd">新增套餐</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="packageName" label="套餐名称" min-width="180" />
        <el-table-column prop="packageCode" label="套餐编码" min-width="160" />
        <el-table-column prop="appCode" label="应用" width="140" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <DictTag dict-code="sys_normal_disable" :value="row.status" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="授权菜单数" width="120">
          <template #default="{ row }">{{ row.menuIds?.length || 0 }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handlePreview(row)">预览</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.packageId ? '编辑套餐' : '新增套餐'" width="860px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="套餐名称" prop="packageName">
              <el-input v-model="form.packageName" placeholder="请输入套餐名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="套餐编码" prop="packageCode">
              <el-input v-model="form.packageCode" placeholder="请输入套餐编码" :disabled="!!form.packageId" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="应用编码" prop="appCode">
              <el-input v-model="form.appCode" placeholder="internal-admin" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="说明套餐适用机构和授权边界" />
        </el-form-item>
        <el-form-item label="授权菜单" prop="menuIds">
          <el-tree
            ref="treeRef"
            class="package-tree"
            node-key="menuId"
            show-checkbox
            default-expand-all
            :data="menuTree"
            :props="treeProps"
            @check="handleTreeCheck"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="previewVisible" title="套餐菜单预览" size="420px">
      <el-tree
        ref="previewTreeRef"
        node-key="menuId"
        show-checkbox
        default-expand-all
        :data="menuTree"
        :props="treeProps"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="RbacMenuPackage">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { DictSelect, DictTag, useDict } from '@mango/common';
import { menuApi, type SysMenuVO } from '../../api/menu';
import { menuPackageApi, type MenuPackageVO } from '../../api/menuPackage';

const { options: statusOptions } = useDict('sys_normal_disable');

const treeProps = {
  label: 'menuName',
  children: 'children',
};

const loading = ref(false);
const dialogVisible = ref(false);
const previewVisible = ref(false);
const formRef = ref<FormInstance>();
const treeRef = ref();
const previewTreeRef = ref();
const tableData = ref<MenuPackageVO[]>([]);
const menuTree = ref<SysMenuVO[]>([]);

const query = reactive({
  keyword: '',
  status: undefined as number | undefined,
});

const form = reactive<MenuPackageVO>({
  packageId: undefined,
  packageName: '',
  packageCode: '',
  appCode: 'internal-admin',
  status: 1,
  sort: 0,
  remark: '',
  menuIds: [],
});

const rules: FormRules = {
  packageName: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  packageCode: [{ required: true, message: '请输入套餐编码', trigger: 'blur' }],
  appCode: [{ required: true, message: '请输入应用编码', trigger: 'blur' }],
  menuIds: [{ required: true, type: 'array', min: 1, message: '请选择授权菜单', trigger: 'change' }],
};

async function loadMenuTree() {
  menuTree.value = await menuApi.getMenuTree({ appCode: 'internal-admin', fmt: 'tree' });
}

async function loadData() {
  loading.value = true;
  try {
    tableData.value = await menuPackageApi.list({ ...query, appCode: 'internal-admin' });
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  query.keyword = '';
  query.status = undefined;
  loadData();
}

function resetForm() {
  Object.assign(form, {
    packageId: undefined,
    packageName: '',
    packageCode: '',
    appCode: 'internal-admin',
    status: 1,
    sort: 0,
    remark: '',
    menuIds: [],
  });
}

async function applyCheckedMenuIds(menuIds: number[]) {
  await nextTick();
  treeRef.value?.setCheckedKeys(menuIds, false);
  previewTreeRef.value?.setCheckedKeys(menuIds, false);
}

function collectCheckedKeys(tree: any) {
  return Array.from(new Set([...(tree?.getCheckedKeys?.() || []), ...(tree?.getHalfCheckedKeys?.() || [])])) as number[];
}

function handleTreeCheck() {
  form.menuIds = collectCheckedKeys(treeRef.value);
}

async function handleAdd() {
  resetForm();
  dialogVisible.value = true;
  await applyCheckedMenuIds([]);
}

async function handleEdit(row: MenuPackageVO) {
  const detail = await menuPackageApi.detail(row.packageId!);
  Object.assign(form, detail);
  dialogVisible.value = true;
  await applyCheckedMenuIds(detail.menuIds || []);
}

async function handlePreview(row: MenuPackageVO) {
  const detail = await menuPackageApi.detail(row.packageId!);
  previewVisible.value = true;
  await nextTick();
  previewTreeRef.value?.setCheckedKeys(detail.menuIds || [], false);
}

async function handleSubmit() {
  await formRef.value?.validate();
  form.menuIds = collectCheckedKeys(treeRef.value);
  if (form.packageId) {
    await menuPackageApi.update(form);
    ElMessage.success('修改成功');
  } else {
    await menuPackageApi.create(form);
    ElMessage.success('新增成功');
  }
  dialogVisible.value = false;
  await loadData();
}

async function handleDelete(row: MenuPackageVO) {
  await ElMessageBox.confirm(`确认删除套餐“${row.packageName}”？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  });
  await menuPackageApi.delete(row.packageId!);
  ElMessage.success('删除成功');
  await loadData();
}

onMounted(async () => {
  await loadMenuTree();
  await loadData();
});
</script>

<style scoped lang="scss">
.menu-package-container {
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

.package-tree {
  width: 100%;
  max-height: 420px;
  overflow: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
}
</style>
