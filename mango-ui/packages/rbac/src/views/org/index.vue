<template>
  <div class="org-container">
    <el-row :gutter="16">
      <el-col
        :xs="24"
        :lg="8"
      >
        <el-card class="org-panel">
          <template #header>
            <div class="card-header">
              <span>组织架构</span>
              <div class="header-actions">
                <el-button @click="loadTree">
                  刷新
                </el-button>
              </div>
            </div>
          </template>

          <el-form class="filter-form">
            <el-form-item label="组织类型">
              <el-select
                v-model="query.type"
                placeholder="全部类型"
                clearable
                @change="loadTree"
              >
                <el-option
                  v-for="item in orgTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <el-tree
            v-loading="treeLoading"
            class="org-tree"
            :data="treeData"
            node-key="id"
            default-expand-all
            highlight-current
            :props="{ label: 'orgName', children: 'children' }"
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span class="tree-name">{{ data.orgName }}</span>
                <el-tag
                  size="small"
                  effect="plain"
                >
                  {{ orgTypeLabel(data.orgType) }}
                </el-tag>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <el-col
        :xs="24"
        :lg="16"
      >
        <el-card class="org-panel">
          <template #header>
            <div class="card-header">
              <span>组织详情</span>
              <div
                v-if="currentOrg"
                class="header-actions"
              >
                <el-button
                  type="primary"
                  @click="handleAddChild(currentOrg)"
                >
                  新增下级
                </el-button>
                <el-button @click="handleEdit(currentOrg)">
                  编辑
                </el-button>
                <el-button
                  type="danger"
                  :disabled="isRootOrg(currentOrg)"
                  @click="handleDelete(currentOrg)"
                >
                  删除
                </el-button>
                <el-tag
                  :type="currentOrg.orgStatus === '1' ? 'success' : 'danger'"
                  effect="light"
                >
                  {{ currentOrg.orgStatus === '1' ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </div>
          </template>

          <el-empty
            v-if="!currentOrg"
            description="请选择左侧组织"
          />

          <template v-else>
            <el-descriptions
              :column="2"
              border
            >
              <el-descriptions-item label="组织名称">
                {{ currentOrg.orgName }}
              </el-descriptions-item>
              <el-descriptions-item label="组织编码">
                {{ currentOrg.orgCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="组织类型">
                {{ orgTypeLabel(currentOrg.orgType) }}
              </el-descriptions-item>
              <el-descriptions-item label="上级组织ID">
                {{ currentOrg.pid ?? 0 }}
              </el-descriptions-item>
              <el-descriptions-item label="排序">
                {{ currentOrg.orgSort ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="租户ID">
                {{ currentOrg.tenantId ?? '-' }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="section-header">
              <span>直属下级</span>
              <el-button
                link
                type="primary"
                @click="loadChildren(currentOrg.id)"
              >
                刷新下级
              </el-button>
            </div>

            <el-table
              v-loading="childrenLoading"
              :data="childrenData"
              stripe
              row-key="id"
            >
              <el-table-column
                prop="orgName"
                label="组织名称"
                min-width="160"
              />
              <el-table-column
                prop="orgCode"
                label="组织编码"
                min-width="140"
              />
              <el-table-column
                prop="orgType"
                label="类型"
                width="100"
              >
                <template #default="{ row }">
                  {{ orgTypeLabel(row.orgType) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="orgStatus"
                label="状态"
                width="100"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="row.orgStatus === '1' ? 'success' : 'danger'"
                    size="small"
                  >
                    {{ row.orgStatus === '1' ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                prop="orgSort"
                label="排序"
                width="90"
              />
              <el-table-column
                label="操作"
                width="180"
                fixed="right"
              >
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    size="small"
                    @click="handleAddChild(row)"
                  >
                    新增下级
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
                    :disabled="isRootOrg(row)"
                    @click="handleDelete(row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑组织' : '新增组织'"
      width="560px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="父级组织"
          prop="pid"
        >
          <el-select
            v-model="form.pid"
            placeholder="请选择父级组织"
            filterable
            :disabled="form.id ? isRootOrg(form as SysOrg) : false"
          >
            <el-option
              label="根节点"
              :value="0"
            />
            <el-option
              v-for="item in flatOrgOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
              :disabled="form.id === item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="组织名称"
          prop="orgName"
        >
          <el-input
            v-model="form.orgName"
            placeholder="请输入组织名称"
          />
        </el-form-item>
        <el-form-item
          label="组织编码"
          prop="orgCode"
        >
          <el-input
            v-model="form.orgCode"
            placeholder="请输入组织编码"
            :disabled="!!form.id"
          />
        </el-form-item>
        <el-form-item
          label="组织类型"
          prop="orgType"
        >
          <el-select
            v-model="form.orgType"
            placeholder="请选择组织类型"
          >
            <el-option
              v-for="item in orgTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="状态"
          prop="orgStatus"
        >
          <el-radio-group
            v-model="form.orgStatus"
            :disabled="form.id ? isRootOrg(form as SysOrg) : false"
          >
            <el-radio label="1">
              启用
            </el-radio>
            <el-radio label="0">
              禁用
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="排序"
          prop="orgSort"
        >
          <el-input-number
            v-model="form.orgSort"
            :min="0"
            :max="9999"
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

<script setup lang="ts" name="SystemOrg">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { orgApi, type SysOrg } from '../../api/org';

const orgTypeOptions = [
  { label: '集团', value: 1 },
  { label: '公司', value: 2 },
  { label: '部门', value: 3 },
  { label: '小组', value: 4 },
];

const query = reactive({
  type: undefined as number | undefined,
});

const treeLoading = ref(false);
const childrenLoading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const treeData = ref<SysOrg[]>([]);
const childrenData = ref<SysOrg[]>([]);
const currentOrg = ref<SysOrg>();
const formRef = ref<FormInstance>();

const form = reactive<Partial<SysOrg>>({
  id: undefined,
  pid: 0,
  orgName: '',
  orgCode: '',
  orgType: 3,
  orgSort: 0,
  orgStatus: '1',
});

const rules: FormRules = {
  pid: [{ required: true, message: '请选择父级组织', trigger: 'change' }],
  orgName: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  orgCode: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
  orgType: [{ required: true, message: '请选择组织类型', trigger: 'change' }],
  orgStatus: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

const flatOrgOptions = computed(() => flattenTree(treeData.value));

function orgTypeLabel(type?: number) {
  return orgTypeOptions.find(item => item.value === Number(type))?.label || '-';
}

async function loadTree() {
  treeLoading.value = true;
  try {
    treeData.value = await orgApi.tree({ parentId: 0, type: query.type, includeDisabled: true });
    if (currentOrg.value) {
      const latest = findInTree(treeData.value, currentOrg.value.id);
      if (latest) {
        await handleNodeClick(latest);
      } else {
        currentOrg.value = undefined;
        childrenData.value = [];
      }
    } else if (treeData.value.length > 0) {
      await handleNodeClick(treeData.value[0]);
    }
  } finally {
    treeLoading.value = false;
  }
}

async function loadChildren(parentId: number) {
  childrenLoading.value = true;
  try {
    childrenData.value = await orgApi.children(parentId);
  } finally {
    childrenLoading.value = false;
  }
}

async function handleNodeClick(row: SysOrg) {
  try {
    currentOrg.value = await orgApi.detail(row.id);
    await loadChildren(row.id);
  } catch (error) {
    ElMessage.error('组织详情加载失败');
  }
}

function resetForm() {
  form.id = undefined;
  form.pid = 0;
  form.orgName = '';
  form.orgCode = '';
  form.orgType = 3;
  form.orgSort = 0;
  form.orgStatus = '1';
}

function handleAddChild(row: SysOrg) {
  resetForm();
  form.pid = row.id;
  form.orgType = nextOrgType(row.orgType);
  dialogVisible.value = true;
}

function handleEdit(row: SysOrg) {
  Object.assign(form, row);
  form.orgStatus = row.orgStatus ?? '1';
  form.orgSort = row.orgSort ?? 0;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    if (form.id) {
      await orgApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await orgApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    await loadTree();
  } finally {
    submitLoading.value = false;
  }
}

function handleDelete(row: SysOrg) {
  if (!row.id || isRootOrg(row)) return;
  ElMessageBox.confirm(`确认删除组织「${row.orgName}」?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await orgApi.delete(row.id);
    ElMessage.success('删除成功');
    if (currentOrg.value?.id === row.id) {
      currentOrg.value = undefined;
      childrenData.value = [];
    }
    await loadTree();
  }).catch(() => {});
}

function isRootOrg(row: Partial<SysOrg>) {
  return Number(row.pid ?? 0) === 0;
}

function nextOrgType(type?: number) {
  return Math.min(Number(type || 1) + 1, 4);
}

function flattenTree(items: SysOrg[], level = 0): Array<SysOrg & { label: string }> {
  return items.flatMap((item) => {
    const label = `${'　'.repeat(level)}${item.orgName}`;
    return [
      { ...item, label },
      ...flattenTree(item.children || [], level + 1),
    ];
  });
}

function findInTree(items: SysOrg[], id: number): SysOrg | undefined {
  for (const item of items) {
    if (item.id === id) return item;
    const child = findInTree(item.children || [], id);
    if (child) return child;
  }
  return undefined;
}

onMounted(loadTree);
</script>

<style scoped lang="scss">
.org-container {
  padding: 0;
}

.org-panel {
  min-height: 520px;
}

.card-header,
.section-header,
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-form {
  margin-bottom: 12px;
}

.org-tree {
  min-height: 360px;
}

.tree-node {
  width: 100%;
  min-width: 0;
}

.tree-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-header {
  margin: 18px 0 10px;
  font-weight: 600;
}
</style>
