<template>
  <div class="app-container">
    <el-card>
      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增应用
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="appName"
          label="应用名称"
          min-width="160"
        />
        <el-table-column
          prop="appCode"
          label="应用编码"
          min-width="160"
        />
        <el-table-column
          label="登录上下文"
          min-width="260"
        >
          <template #default="{ row }">
            <div class="context-tags">
              <el-tag
                v-for="context in row.loginContexts || []"
                :key="`${context.realm}:${context.actorType}`"
                size="small"
                :type="context.defaultFlag === 1 ? 'success' : 'info'"
              >
                <DictTag
                  dict-code="auth_realm"
                  :value="context.realm"
                />
                /
                <DictTag
                  dict-code="auth_actor_type"
                  :value="context.actorType"
                />
              </el-tag>
              <span v-if="!row.loginContexts?.length">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="icon"
          label="图标"
          width="90"
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
          width="80"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="90"
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
              :disabled="row.appCode === 'internal-admin'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.appId ? '编辑应用' : '新增应用'"
      width="min(640px, 92vw)"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="应用名称"
          prop="appName"
        >
          <el-input v-model="form.appName" />
        </el-form-item>
        <el-form-item
          label="应用编码"
          prop="appCode"
        >
          <el-input
            v-model="form.appCode"
            :disabled="!!form.appId"
          />
        </el-form-item>
        <el-form-item
          label="登录上下文"
          prop="loginContexts"
        >
          <div class="context-editor">
            <div
              v-for="(context, index) in form.loginContexts"
              :key="index"
              class="context-row"
            >
              <div class="context-fields">
                <DictSelect
                  v-model="context.realm"
                  dict-type="auth_realm"
                  placeholder="登录域"
                  filterable
                  @change="handleRealmChange(context)"
                />
                <DictSelect
                  v-model="context.actorType"
                  dict-type="auth_actor_type"
                  placeholder="操作者类型"
                  filterable
                />
              </div>
              <div class="context-actions">
                <el-checkbox
                  :model-value="context.defaultFlag === 1"
                  @change="setDefaultContext(index)"
                >
                  默认
                </el-checkbox>
                <div class="status-switch">
                  <span>启用</span>
                  <el-switch
                    v-model="context.status"
                    :active-value="1"
                    :inactive-value="0"
                  />
                </div>
                <el-button
                  link
                  type="danger"
                  :disabled="form.loginContexts.length <= 1"
                  @click="removeContext(index)"
                >
                  删除
                </el-button>
              </div>
            </div>
            <el-button
              class="context-add-button"
              @click="addContext"
            >
              新增登录上下文
            </el-button>
          </div>
        </el-form-item>
        <el-form-item
          label="图标"
          prop="icon"
        >
          <div class="icon-picker-field">
            <div class="icon-preview">
              <el-icon
                v-if="form.icon"
                size="18"
              >
                <component :is="form.icon" />
              </el-icon>
              <span v-else>-</span>
            </div>
            <el-input
              v-model="form.icon"
              readonly
              placeholder="请选择应用图标"
            />
            <el-button @click="openIconSelector">
              选择
            </el-button>
            <el-button
              v-if="form.icon"
              @click="form.icon = ''"
            >
              清空
            </el-button>
          </div>
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
          label="备注"
          prop="remark"
        >
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
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

    <IconSelector
      ref="iconSelectorRef"
      v-model="iconValue"
    />
  </div>
</template>

<script setup lang="ts" name="SystemApp">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { DictSelect, DictTag, IconSelector, useDict } from '@mango/common';
import { appApi, type AppLoginContext, type AuthorizationApp } from '../../api/app';

const { options: statusOptions } = useDict('sys_normal_disable');

const actorTypeByRealm: Record<string, string> = {
  INTERNAL: 'INTERNAL_USER',
  TENANT: 'TENANT_USER',
  CUSTOMER: 'CUSTOMER_USER',
  PARTNER: 'PARTNER_USER',
  FINANCIAL: 'FINANCIAL_USER',
};

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const tableData = ref<AuthorizationApp[]>([]);
const formRef = ref<FormInstance>();
const iconSelectorRef = ref<{ open: () => void }>();

const form = reactive<AuthorizationApp>({
  appId: undefined,
  appCode: '',
  appName: '',
  loginContexts: [createContext('INTERNAL')],
  icon: 'Setting',
  sort: 0,
  status: 1,
  remark: '',
});

const rules: FormRules = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  appCode: [{ required: true, message: '请输入应用编码', trigger: 'blur' }],
  loginContexts: [{ required: true, validator: validateLoginContexts, trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

const iconValue = computed({
  get: () => form.icon || '',
  set: (value: string) => {
    form.icon = value;
  },
});

async function loadData() {
  loading.value = true;
  try {
    tableData.value = await appApi.list();
  } catch (error) {
    console.error('加载应用失败:', error);
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.appId = undefined;
  form.appCode = '';
  form.appName = '';
  form.loginContexts = [createContext('INTERNAL')];
  form.icon = 'Setting';
  form.sort = 0;
  form.status = 1;
  form.remark = '';
}

function handleAdd() {
  resetForm();
  dialogVisible.value = true;
}

function handleEdit(row: AuthorizationApp) {
  Object.assign(form, {
    ...row,
    loginContexts: row.loginContexts?.length
      ? row.loginContexts.map((item, index) => normalizeContext(item, index))
      : [createContext('INTERNAL')],
  });
  dialogVisible.value = true;
}

function createContext(realm = ''): AppLoginContext {
  return {
    realm,
    actorType: realm ? actorTypeByRealm[realm] || '' : '',
    defaultFlag: 1,
    status: 1,
    sort: 0,
  };
}

function normalizeContext(context: AppLoginContext, index: number): AppLoginContext {
  return {
    ...context,
    actorType: context.actorType || actorTypeByRealm[context.realm] || '',
    defaultFlag: context.defaultFlag === 1 ? 1 : 0,
    status: context.status ?? 1,
    sort: context.sort ?? index,
  };
}

function handleRealmChange(context: AppLoginContext) {
  if (context.realm && actorTypeByRealm[context.realm]) {
    context.actorType = actorTypeByRealm[context.realm];
  }
}

function addContext() {
  form.loginContexts = [
    ...(form.loginContexts || []),
    {
      realm: '',
      actorType: '',
      defaultFlag: 0,
      status: 1,
      sort: form.loginContexts?.length || 0,
    },
  ];
}

function removeContext(index: number) {
  form.loginContexts.splice(index, 1);
  if (!form.loginContexts.some((item) => item.defaultFlag === 1) && form.loginContexts[0]) {
    form.loginContexts[0].defaultFlag = 1;
  }
}

function setDefaultContext(index: number) {
  form.loginContexts.forEach((item, currentIndex) => {
    item.defaultFlag = currentIndex === index ? 1 : 0;
  });
}

function validateLoginContexts(_rule: unknown, value: AppLoginContext[] | undefined, callback: (error?: Error) => void) {
  if (!value?.length) {
    callback(new Error('请至少配置一个登录上下文'));
    return;
  }
  const invalid = value.some((item) => !item.realm || !item.actorType);
  if (invalid) {
    callback(new Error('请选择登录域和操作者类型'));
    return;
  }
  callback();
}

function openIconSelector() {
  iconSelectorRef.value?.open();
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    const hasDefault = form.loginContexts.some((item) => item.defaultFlag === 1);
    form.loginContexts = form.loginContexts.map((item, index) => ({
      ...item,
      defaultFlag: item.defaultFlag === 1 || (!hasDefault && index === 0) ? 1 : 0,
      status: item.status ?? 1,
      sort: index,
    }));
    if (form.appId) {
      await appApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await appApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    await loadData();
  } finally {
    submitLoading.value = false;
  }
}

function handleDelete(row: AuthorizationApp) {
  if (!row.appId) return;
  ElMessageBox.confirm(`确认删除应用「${row.appName}」?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await appApi.delete(row.appId!);
    ElMessage.success('删除成功');
    await loadData();
  }).catch(() => {});
}

function formatTime(value?: string | number[]) {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`;
  }
  return value;
}

function pad(value: number) {
  return String(value).padStart(2, '0');
}

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.app-container {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .icon-picker-field {
    display: grid;
    grid-template-columns: 40px minmax(0, 1fr) auto auto;
    gap: 8px;
    width: 100%;

    .icon-preview {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 32px;
      color: var(--mango-text-color-regular);
      border: 1px solid var(--mango-border-color);
      border-radius: 4px;
      background: var(--mango-fill-color-light, #f5f7fa);
    }
  }

  .context-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .context-editor {
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 100%;
  }

  .context-row {
    display: flex;
    flex-direction: column;
    gap: 8px;
    min-width: 0;
    padding: 10px;
    border: 1px solid var(--mango-border-color-lighter, #ebeef5);
    border-radius: 6px;
    background: var(--mango-fill-color-blank, #fff);
  }

  .context-fields {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 8px;
    min-width: 0;
  }

  .context-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 8px 12px;
    min-height: 28px;
  }

  .status-switch {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--mango-text-color-regular);
    font-size: 13px;
    white-space: nowrap;
  }

  .context-add-button {
    align-self: flex-start;
  }

  @media (max-width: 640px) {
    .context-fields {
      grid-template-columns: minmax(0, 1fr);
    }
  }
}
</style>
