<template>
  <div class="config-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统配置</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 系统参数 Tab -->
        <el-tab-pane label="系统参数" name="param">
          <div class="tab-toolbar">
            <el-button
              type="primary"
              @click="handleAddParam"
            >
              新增参数
            </el-button>
          </div>

          <el-form
            :inline="true"
            class="search-form"
          >
            <el-form-item label="关键词">
              <el-input
                v-model="paramQuery.keyword"
                placeholder="搜索参数键/描述"
                clearable
              />
            </el-form-item>
            <el-form-item label="类型">
              <el-select
                v-model="paramQuery.paramType"
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
                @click="handleSearchParam"
              >
                查询
              </el-button>
              <el-button @click="handleResetParam">
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="paramLoading"
            :data="paramTableData"
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
                  :type="row.paramType === 1 ? 'primary' : 'success'"
                  size="small"
                >
                  {{ row.paramType === 1 ? '系统参数' : '应用参数' }}
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
                  @click="handleEditParam(row)"
                >
                  编辑
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="handleDeleteParam(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 系统配置 Tab -->
        <el-tab-pane label="系统配置" name="config">
          <div class="tab-toolbar">
            <el-button
              type="primary"
              @click="handleAddConfig"
            >
              新增配置
            </el-button>
          </div>

          <el-form
            :inline="true"
            class="search-form"
          >
            <el-form-item label="关键词">
              <el-input
                v-model="configQuery.keyword"
                placeholder="搜索配置键/描述"
                clearable
              />
            </el-form-item>
            <el-form-item label="分组">
              <el-select
                v-model="configQuery.configGroup"
                placeholder="请选择"
                clearable
              >
                <el-option
                  label="系统"
                  value="system"
                />
                <el-option
                  label="安全"
                  value="security"
                />
                <el-option
                  label="上传"
                  value="upload"
                />
                <el-option
                  label="邮件"
                  value="email"
                />
                <el-option
                  label="短信"
                  value="sms"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="handleSearchConfig"
              >
                查询
              </el-button>
              <el-button @click="handleResetConfig">
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="configLoading"
            :data="configTableData"
            stripe
          >
            <el-table-column
              prop="configKey"
              label="配置键"
            />
            <el-table-column
              prop="configValue"
              label="配置值"
              show-overflow-tooltip
            />
            <el-table-column
              prop="configGroup"
              label="分组"
              width="100"
            >
              <template #default="{ row }">
                <el-tag size="small">
                  {{ row.configGroup }}
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
                  @click="handleEditConfig(row)"
                >
                  编辑
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="handleDeleteConfig(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 系统参数编辑弹窗 -->
    <el-dialog
      v-model="paramDialogVisible"
      :title="paramForm.id ? '编辑参数' : '新增参数'"
      width="500px"
    >
      <el-form
        ref="paramFormRef"
        :model="paramForm"
        :rules="paramRules"
        label-width="100px"
      >
        <el-form-item
          label="参数键"
          prop="paramKey"
        >
          <el-input
            v-model="paramForm.paramKey"
            placeholder="如：sys.login.maxRetryCount"
            :disabled="!!paramForm.id"
          />
        </el-form-item>
        <el-form-item
          label="参数值"
          prop="paramValue"
        >
          <el-input
            v-model="paramForm.paramValue"
            type="textarea"
            placeholder="请输入参数值"
          />
        </el-form-item>
        <el-form-item
          label="参数类型"
          prop="paramType"
        >
          <el-radio-group v-model="paramForm.paramType">
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
            v-model="paramForm.description"
            type="textarea"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="paramForm.status">
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
        <el-button @click="paramDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmitParam"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 系统配置编辑弹窗 -->
    <el-dialog
      v-model="configDialogVisible"
      :title="configForm.id ? '编辑配置' : '新增配置'"
      width="500px"
    >
      <el-form
        ref="configFormRef"
        :model="configForm"
        :rules="configRules"
        label-width="100px"
      >
        <el-form-item
          label="配置键"
          prop="configKey"
        >
          <el-input
            v-model="configForm.configKey"
            placeholder="请输入配置键"
            :disabled="!!configForm.id"
          />
        </el-form-item>
        <el-form-item
          label="配置值"
          prop="configValue"
        >
          <el-input
            v-model="configForm.configValue"
            type="textarea"
            placeholder="请输入配置值"
          />
        </el-form-item>
        <el-form-item
          label="配置分组"
          prop="configGroup"
        >
          <el-select
            v-model="configForm.configGroup"
            placeholder="请选择"
          >
            <el-option
              label="系统"
              value="system"
            />
            <el-option
              label="安全"
              value="security"
            />
            <el-option
              label="上传"
              value="upload"
            />
            <el-option
              label="邮件"
              value="email"
            />
            <el-option
              label="短信"
              value="sms"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="描述"
          prop="description"
        >
          <el-input
            v-model="configForm.description"
            type="textarea"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="configForm.status">
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
        <el-button @click="configDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmitConfig"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemConfig">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { paramApi, type SysParam } from '@/api/admin/param';
import { configApi, type SysConfig } from '@/api/admin/config';

const activeTab = ref('param');

// ==================== 系统参数 ====================
const paramLoading = ref(false);
const paramTableData = ref<SysParam[]>([]);
const paramQuery = reactive({
  keyword: '',
  paramType: undefined as number | undefined,
});
const paramDialogVisible = ref(false);
const paramFormRef = ref<FormInstance>();
const paramForm = reactive<SysParam>({
  id: undefined,
  paramKey: '',
  paramValue: '',
  paramType: 1,
  description: '',
  status: 1,
});
const paramRules: FormRules = {
  paramKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
  paramValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }],
  paramType: [{ required: true, message: '请选择参数类型', trigger: 'change' }],
};

async function loadParamData() {
  paramLoading.value = true;
  try {
    const data = await paramApi.list(paramQuery);
    paramTableData.value = data.list || [];
  } catch (error) {
    console.error('加载参数数据失败:', error);
  } finally {
    paramLoading.value = false;
  }
}

function handleSearchParam() {
  loadParamData();
}

function handleResetParam() {
  paramQuery.keyword = '';
  paramQuery.paramType = undefined;
  loadParamData();
}

function handleAddParam() {
  paramForm.id = undefined;
  paramForm.paramKey = '';
  paramForm.paramValue = '';
  paramForm.paramType = 1;
  paramForm.description = '';
  paramForm.status = 1;
  paramDialogVisible.value = true;
}

function handleEditParam(row: SysParam) {
  Object.assign(paramForm, row);
  paramDialogVisible.value = true;
}

async function handleSubmitParam() {
  if (!paramFormRef.value) return;
  try {
    await paramFormRef.value.validate();
    ElMessage.success(paramForm.id ? '修改成功' : '新增成功');
    paramDialogVisible.value = false;
    loadParamData();
  } catch (error) {
    console.error('提交失败:', error);
  }
}

function handleDeleteParam(row: SysParam) {
  ElMessageBox.confirm('确认删除该参数?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    ElMessage.success('删除成功');
    loadParamData();
  }).catch(() => {});
}

// ==================== 系统配置 ====================
const configLoading = ref(false);
const configTableData = ref<SysConfig[]>([]);
const configQuery = reactive({
  keyword: '',
  configGroup: '',
});
const configDialogVisible = ref(false);
const configFormRef = ref<FormInstance>();
const configForm = reactive<SysConfig>({
  id: undefined,
  configKey: '',
  configValue: '',
  configGroup: 'system',
  description: '',
  status: 1,
});
const configRules: FormRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  configGroup: [{ required: true, message: '请选择配置分组', trigger: 'change' }],
};

async function loadConfigData() {
  configLoading.value = true;
  try {
    const data = await configApi.list(configQuery);
    configTableData.value = data.list || [];
  } catch (error) {
    console.error('加载配置数据失败:', error);
  } finally {
    configLoading.value = false;
  }
}

function handleSearchConfig() {
  loadConfigData();
}

function handleResetConfig() {
  configQuery.keyword = '';
  configQuery.configGroup = '';
  loadConfigData();
}

function handleAddConfig() {
  configForm.id = undefined;
  configForm.configKey = '';
  configForm.configValue = '';
  configForm.configGroup = 'system';
  configForm.description = '';
  configForm.status = 1;
  configDialogVisible.value = true;
}

function handleEditConfig(row: SysConfig) {
  Object.assign(configForm, row);
  configDialogVisible.value = true;
}

async function handleSubmitConfig() {
  if (!configFormRef.value) return;
  try {
    await configFormRef.value.validate();
    ElMessage.success(configForm.id ? '修改成功' : '新增成功');
    configDialogVisible.value = false;
    loadConfigData();
  } catch (error) {
    console.error('提交失败:', error);
  }
}

function handleDeleteConfig(row: SysConfig) {
  ElMessageBox.confirm('确认删除该配置?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    ElMessage.success('删除成功');
    loadConfigData();
  }).catch(() => {});
}

onMounted(() => {
  loadParamData();
});
</script>

<style scoped lang="scss">
.config-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tab-toolbar {
  margin-bottom: 16px;
}
.search-form {
  margin-bottom: 16px;
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
</style>
