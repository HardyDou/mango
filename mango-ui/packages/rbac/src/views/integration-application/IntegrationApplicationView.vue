<template>
  <MangoListPage class="integration-instance-page" data-page="integration.application">
    <template #search>
      <MangoSearchPanel :model="query" :columns="3" @search="handleSearch" @reset="handleReset">
        <el-form-item label="集成应用">
          <el-input
            v-model="query.keyword"
            clearable
            data-field="integration-instance.keyword"
            placeholder="实例名称或编码"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="连接器">
          <el-select v-model="query.connectorCode" clearable placeholder="全部" @change="handleSearch">
            <el-option
              v-for="item in enabledConnectors"
              :key="item.connectorCode"
              :label="`${item.connectorName}（${item.connectorCode}）`"
              :value="item.connectorCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #actions>
        <el-button type="primary" plain data-action="integration-instance.create" @click="handleAdd"
          >新增集成应用</el-button
        >
      </template>

      <el-table v-loading="loading" :data="tableData" stripe empty-text="暂无集成应用">
        <el-table-column label="集成应用" min-width="180">
          <template #default="{ row }">
            <div>{{ row.instanceName }}</div>
            <div class="code-text" data-field="integration-instance.code">{{ row.instanceCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="连接器" min-width="180">
          <template #default="{ row }">
            <div>
              {{ connectorName(row.connectorCode) }}
              <el-tag v-if="row.connectorEnabled === false" type="danger" size="small">连接器已停用</el-tag>
            </div>
            <div class="muted">{{ row.environment || 'PROD' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="凭证" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.credentialCode" type="success" size="small">已配置</el-tag>
            <el-tag v-else type="danger" size="small">未配置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="能力" width="90">
          <template #default="{ row }">
            <el-tooltip
              v-if="parseList(row.capabilitiesJson).length"
              :content="
                parseList(row.capabilitiesJson)
                  .map((code) => capabilityLabel(code))
                  .join('、')
              "
              placement="top"
            >
              <el-tag size="small" type="success">{{ parseList(row.capabilitiesJson).length }} 项</el-tag>
            </el-tooltip>
            <span v-else class="muted">未授权</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">{{
              row.status === 'ENABLED' ? '启用' : '停用'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" data-action="integration-instance.view" @click="handleView(row)"
              >查看</el-button
            >
            <el-button link type="primary" size="small" data-action="integration-instance.edit" @click="handleEdit(row)"
              >编辑</el-button
            >
            <el-button
              link
              type="warning"
              size="small"
              data-action="integration-instance.toggle"
              @click="handleToggle(row)"
              >{{ row.status === 'ENABLED' ? '停用' : '启用' }}</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </template>
    </MangoListPanel>

    <MangoDialog v-model="detailVisible" title="集成应用详情" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="实例编码">{{ detail?.instanceCode }}</el-descriptions-item>
        <el-descriptions-item label="实例名称">{{ detail?.instanceName }}</el-descriptions-item>
        <el-descriptions-item label="连接器">
          {{ connectorName(detail?.connectorCode) }}（{{ detail?.connectorCode }}）
        </el-descriptions-item>
        <el-descriptions-item label="环境">{{ detail?.environment || 'PROD' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail?.status === 'ENABLED' ? 'success' : 'info'" size="small">
            {{ detail?.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail?.owner || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回调路径" :span="2">
          <span class="code-text">{{ detail?.callbackPath }}</span>
          <el-button link type="primary" size="small" @click="copyCallbackPath">复制</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="detail?.connectorEnabled === false"
        type="warning"
        :closable="false"
        show-icon
        title="所属连接器已停用，该实例当前无法启用或调用。"
        style="margin-top: 12px"
      />

      <div v-if="detail?.credentialCode" class="detail-actions">
        <el-button
          size="small"
          type="primary"
          plain
          data-action="integration-instance.test-connection"
          :loading="testLoading"
          @click="handleTestConnection"
          >连接测试</el-button
        >
        <el-button size="small" plain data-action="integration-instance.call-logs" @click="openCallLogs"
          >调用记录</el-button
        >
        <el-button size="small" plain data-action="integration-instance.rotations" @click="openRotations"
          >轮换记录</el-button
        >
        <el-button size="small" type="primary" plain data-action="integration-instance.rotate" @click="openRotate"
          >轮换凭证</el-button
        >
      </div>

      <div class="section-head"><span class="section-title">配置值</span></div>
      <el-table :data="detailConfigRows" size="small" border empty-text="无配置">
        <el-table-column prop="label" label="配置项" min-width="140" />
        <el-table-column label="取值" min-width="220">
          <template #default="{ row }">
            <DictTag
              v-if="row.dictCode && row.value !== '未填写'"
              :dict-code="row.dictCode"
              :value="row.value"
              size="small"
            />
            <span v-else>{{ row.value }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="section-head"><span class="section-title">凭证</span></div>
      <el-descriptions v-if="detailCredential" :column="2" border class="credential-meta">
        <el-descriptions-item label="凭证编码">{{ detailCredential.credentialCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailCredential.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近轮换">{{ detailCredential.rotatedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到期时间">
          {{ detailCredential.expiresAt || '未设置' }}
          <el-tag v-if="expiryTag()" :type="expiryTag()!.type" size="small">{{ expiryTag()!.label }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-table :data="detailCredentialRows" size="small" border empty-text="未绑定凭证">
        <el-table-column prop="label" label="字段" min-width="140" />
        <el-table-column prop="value" label="状态" min-width="160" />
      </el-table>

      <div class="section-head"><span class="section-title">能力授权</span></div>
      <el-table :data="detailCapabilityRows" size="small" border empty-text="未授权能力">
        <el-table-column prop="name" label="能力名称" min-width="140" />
        <el-table-column prop="code" label="能力编码" min-width="150" />
        <el-table-column prop="description" label="介绍" min-width="180" />
      </el-table>

      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </MangoDialog>

    <MangoDialog v-model="callLogsVisible" title="调用记录" width="720px">
      <el-table v-loading="callLogsLoading" :data="callLogs" size="small" border empty-text="暂无调用记录">
        <el-table-column prop="createdAt" label="调用时间" min-width="170" />
        <el-table-column prop="httpMethod" label="方法" width="80" />
        <el-table-column prop="path" label="路径" min-width="180" />
        <el-table-column prop="statusCode" label="状态码" width="90" />
        <el-table-column prop="elapsedMs" label="耗时" width="90">
          <template #default="{ row }">{{ row.elapsedMs }}ms</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="callLogsVisible = false">关闭</el-button>
      </template>
    </MangoDialog>

    <MangoDialog v-model="rotationsVisible" title="凭证轮换记录" width="640px">
      <el-table v-loading="rotationsLoading" :data="rotations" size="small" border empty-text="暂无轮换记录">
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="fieldCodes" label="更新字段" min-width="180" />
        <el-table-column prop="rotatedAt" label="轮换时间" min-width="170" />
        <el-table-column prop="operatorId" label="操作人" width="110" />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="rotationsVisible = false">关闭</el-button>
      </template>
    </MangoDialog>

    <MangoDialog v-model="rotateVisible" title="轮换凭证" width="560px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="只更新填写的字段，未填写的字段保持原值；敏感字段保存后只显示掩码。"
      />
      <el-form label-width="120px" style="margin-top: 16px">
        <el-form-item v-for="field in rotateFields" :key="field.code" :label="field.label" :required="field.required">
          <el-input
            v-model="rotateValues[field.code]"
            :type="field.secret ? 'password' : 'text'"
            :show-password="field.secret"
            :placeholder="field.type === 'CERT' || field.type === 'FILE' ? '粘贴证书或文件文本' : '留空表示不修改'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rotateVisible = false">取消</el-button>
        <el-button type="primary" :loading="rotateLoading" @click="submitRotate">确定轮换</el-button>
      </template>
    </MangoDialog>

    <MangoDialog v-model="dialogVisible" :title="editing ? '编辑集成应用' : '新增集成应用'" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" data-surface="integration-instance.form">
        <div class="section-head"><span class="section-title">实例信息</span></div>
        <div class="form-grid">
          <el-form-item label="实例编码" prop="instanceCode">
            <el-input
              v-model="form.instanceCode"
              :disabled="editing"
              data-field="integration-instance.instance-code"
              placeholder="如 wecom-corp-main"
            />
          </el-form-item>
          <el-form-item label="实例名称" prop="instanceName">
            <el-input v-model="form.instanceName" data-field="integration-instance.instance-name" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="连接器" prop="connectorCode">
            <el-select
              v-model="form.connectorCode"
              :disabled="editing"
              filterable
              data-field="integration-instance.connector"
              @change="handleConnectorChange"
            >
              <el-option
                v-for="item in connectors"
                :key="item.connectorCode"
                :label="`${item.connectorName}（${item.connectorCode}）`"
                :value="item.connectorCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="环境">
            <el-select v-model="form.environment">
              <el-option label="生产" value="PROD" />
              <el-option label="测试" value="TEST" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="责任人">
            <el-input v-model="form.owner" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>

        <template v-if="currentConnector">
          <div class="section-head">
            <span class="section-title">连接器配置</span
            ><span class="section-desc">按连接器配置规范填写，必填项不可为空</span>
          </div>
          <el-form-item
            v-for="field in configFields"
            :key="`config-${field.code}`"
            :label="field.label"
            :required="field.required"
            :error="configErrors[field.code]"
          >
            <el-switch
              v-if="field.component === 'BOOLEAN' || field.type === 'boolean'"
              v-model="configValues[field.code]"
              :active-value="true"
              :inactive-value="false"
            />
            <DictSelect
              v-else-if="field.component === 'SYSTEM_DICT' && field.dictCode"
              v-model="configValues[field.code] as string"
              :dict-type="field.dictCode"
              :placeholder="field.description || '请选择'"
              class="full-width"
            />
            <el-select
              v-else-if="field.component === 'DICT' && field.options?.length"
              v-model="configValues[field.code]"
              :placeholder="field.description || '请选择'"
              class="full-width"
            >
              <el-option v-for="item in field.options" :key="item" :label="item" :value="item" />
            </el-select>
            <el-input-number
              v-else-if="field.component === 'NUMBER' || field.type === 'integer' || field.type === 'number'"
              v-model="configValues[field.code] as number"
              class="full-width"
            />
            <el-input
              v-else-if="field.component === 'TEXTAREA'"
              v-model="configValues[field.code] as string"
              type="textarea"
              :rows="3"
              :placeholder="field.description || ''"
            />
            <el-input
              v-else-if="field.component === 'JSON'"
              v-model="configValues[field.code] as string"
              type="textarea"
              :rows="4"
              placeholder="JSON 对象文本"
            />
            <el-input
              v-else-if="field.component === 'FILE'"
              v-model="configValues[field.code] as string"
              placeholder="文件标识（如 fileId）"
            />
            <el-input v-else v-model="configValues[field.code] as string" :placeholder="field.description || ''" />
          </el-form-item>

          <div class="section-head">
            <span class="section-title">凭证</span
            ><span class="section-desc">密钥类字段保存后只显示掩码，编辑时留空表示保持原值</span>
          </div>
          <el-form-item
            v-for="field in credentialFields"
            :key="`cred-${field.code}`"
            :label="field.label"
            :required="field.required && !editing"
          >
            <el-input
              v-if="field.type === 'CERT' || field.type === 'FILE'"
              v-model="credentialValues[field.code]"
              type="textarea"
              :rows="4"
              :placeholder="editing ? '留空表示保持原值；否则粘贴证书内容（PEM 等文本）' : '粘贴证书内容（PEM 等文本）'"
              :data-field="`integration-instance.credential.${field.code}`"
            />
            <el-input
              v-else
              v-model="credentialValues[field.code]"
              :type="field.secret ? 'password' : 'text'"
              :show-password="field.secret"
              :placeholder="field.secret && editing ? '留空表示保持原值' : field.description || ''"
              :data-field="`integration-instance.credential.${field.code}`"
            />
          </el-form-item>

          <div class="section-head">
            <span class="section-title">能力授权</span><span class="section-desc">只能授权连接器声明的能力</span>
          </div>
          <el-form-item label="已授权能力">
            <el-select
              v-model="selectedCapabilities"
              multiple
              style="width: 100%"
              placeholder="选择该实例允许使用的能力"
            >
              <el-option
                v-for="item in capabilityOptions"
                :key="item.code"
                :label="`${item.name}（${item.code}）`"
                :value="item.code"
              >
                <span>{{ item.name }}（{{ item.code }}）</span>
                <span v-if="item.description" class="option-desc">{{ item.description }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </template>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          data-action="integration-instance.submit"
          @click="handleSubmit"
          >确定</el-button
        >
      </template>
    </MangoDialog>
  </MangoListPage>
</template>

<script setup lang="ts" name="SystemIntegrationApplication">
import { DictSelect, DictTag, MangoDialog, MangoListPage, MangoListPanel, MangoSearchPanel } from '@mango/common';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import {
  integrationConnectorApi,
  integrationCredentialApi,
  integrationInstanceApi,
  type IntegrationConnector,
  type IntegrationInstance,
} from '../../api/integrationModel';

interface SchemaField {
  code: string;
  label: string;
  type: string;
  required: boolean;
  secret: boolean;
  description?: string;
  defaultValue?: unknown;
  options?: string[];
  component?: string;
  dictCode?: string;
}

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const detailVisible = ref(false);
const detail = ref<IntegrationInstance>();
const detailCredentialRows = ref<Array<{ label: string; value: string }>>([]);
const detailCredential = ref<Record<string, unknown> | null>(null);
const rotateVisible = ref(false);
const rotationsVisible = ref(false);
const callLogsVisible = ref(false);
const testLoading = ref(false);
const callLogsLoading = ref(false);
const callLogs = ref<Array<Record<string, unknown>>>([]);
const rotationsLoading = ref(false);
const rotations = ref<Array<Record<string, unknown>>>([]);
const rotateLoading = ref(false);
const rotateFields = ref<Array<{ code: string; label: string; type: string; secret: boolean; required: boolean }>>([]);
const rotateValues = reactive<Record<string, string>>({});
const editing = ref(false);
const formRef = ref<FormInstance>();
const tableData = ref<IntegrationInstance[]>([]);
const connectors = ref<IntegrationConnector[]>([]);
const currentConnector = ref<IntegrationConnector>();
const total = ref(0);
const configValues = reactive<Record<string, unknown>>({});
const credentialValues = reactive<Record<string, string>>({});
const selectedCapabilities = ref<string[]>([]);
const configErrors = reactive<Record<string, string>>({});
const detailConfigRows = computed(() => buildDetailConfigRows());
const detailCapabilityRows = computed(() => buildDetailCapabilityRows());

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  connectorCode: undefined as string | undefined,
  status: undefined as number | undefined,
});

const form = reactive({
  instanceCode: '',
  instanceName: '',
  connectorCode: '',
  environment: 'PROD',
  owner: '',
  status: 0,
  remark: '',
});

const rules: FormRules = {
  instanceCode: [
    { required: true, message: '请输入实例编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9-]{1,63}$/, message: '只能包含小写字母、数字和中划线', trigger: 'blur' },
  ],
  instanceName: [{ required: true, message: '请输入实例名称', trigger: 'blur' }],
  connectorCode: [{ required: true, message: '请选择连接器', trigger: 'change' }],
};

const configFields = computed<SchemaField[]>(() => parseSchema(currentConnector.value?.configSchema));
const credentialFields = computed<SchemaField[]>(() => parseAuthSchema(currentConnector.value?.authSchema));
const capabilityOptions = computed(() => parseCapabilities(currentConnector.value?.capabilitiesJson));

function parseSchema(json?: string): SchemaField[] {
  if (!json) {
    return [];
  }
  try {
    const schema = JSON.parse(json);
    const required: string[] = Array.isArray(schema.required) ? schema.required : [];
    return Object.entries(schema.properties || {}).map(([code, raw]) => {
      const value = raw as Record<string, unknown>;
      const type = String(value.type || 'string');
      return {
        code,
        label: String(value.title || code),
        type,
        required: required.includes(code),
        secret: false,
        description: value.description ? String(value.description) : undefined,
        defaultValue: value.default,
        options: Array.isArray(value.enum) ? (value.enum as unknown[]).map((item) => String(item)) : undefined,
        component: value.component ? String(value.component) : undefined,
        dictCode: value.dictCode ? String(value.dictCode) : undefined,
      };
    });
  } catch {
    return [];
  }
}

function parseAuthSchema(json?: string): SchemaField[] {
  if (!json) {
    return [];
  }
  try {
    const schema = JSON.parse(json);
    const fields = Array.isArray(schema.fields) ? schema.fields : [];
    return fields.map((raw: Record<string, unknown>) => ({
      code: String(raw.fieldCode),
      label: String(raw.label || raw.fieldCode),
      type: String(raw.type || 'STRING'),
      required: Boolean(raw.required),
      secret: String(raw.type || '').toUpperCase() === 'SECRET',
      description: raw.description ? String(raw.description) : undefined,
    }));
  } catch {
    return [];
  }
}

function parseCapabilities(json?: string): Array<{ code: string; name: string; description: string }> {
  if (!json) {
    return [];
  }
  try {
    const parsed = JSON.parse(json);
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed.map((item: unknown) => {
      if (typeof item === 'string') {
        return { code: item, name: item, description: '' };
      }
      const record = item as Record<string, unknown>;
      return {
        code: String(record.code || ''),
        name: String(record.name || record.code || ''),
        description: record.description ? String(record.description) : '',
      };
    });
  } catch {
    return [];
  }
}

function capabilityLabel(code?: string) {
  const option = capabilityOptions.value.find((item) => item.code === code);
  return option ? `${option.name}（${option.code}）` : code || '-';
}

function parseList(json?: string): string[] {
  if (!json) {
    return [];
  }
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) ? parsed.map((item) => String(item)) : [];
  } catch {
    return [];
  }
}

function connectorName(code?: string) {
  return connectors.value.find((item) => item.connectorCode === code)?.connectorName || code || '-';
}

function resetForm() {
  Object.assign(form, {
    instanceCode: '',
    instanceName: '',
    connectorCode: '',
    environment: 'PROD',
    owner: '',
    status: 0,
    remark: '',
  });
  Object.keys(configValues).forEach((key) => delete configValues[key]);
  Object.keys(credentialValues).forEach((key) => delete credentialValues[key]);
  selectedCapabilities.value = [];
  currentConnector.value = undefined;
}

function handleAdd() {
  resetForm();
  editing.value = false;
  dialogVisible.value = true;
}

async function handleView(row: IntegrationInstance) {
  detail.value = row;
  detailCredentialRows.value = [];
  detailCredential.value = null;
  detailVisible.value = true;
  if (!row.credentialCode) {
    return;
  }
  try {
    const credentials = await integrationCredentialApi.list(row.connectorCode);
    const matched = credentials.find((item) => item.credentialCode === row.credentialCode);
    detailCredential.value = matched ? { ...matched } : null;
    detailCredentialRows.value = parseCredentialMask(matched?.maskedFieldsJson);
  } catch {
    detailCredentialRows.value = [];
  }
}

function parseCredentialMask(masked?: string): Array<{ label: string; value: string }> {
  if (!masked) {
    return [];
  }
  return masked.split(',').map((item) => {
    const [label, value] = item.split('=');
    return { label, value };
  });
}

function buildDetailConfigRows(): Array<{
  label: string;
  value: string;
  dictCode?: string;
  component?: string;
  json?: string;
}> {
  if (!detail.value) {
    return [];
  }
  const connector = connectors.value.find((item) => item.connectorCode === detail.value?.connectorCode);
  const fields = parseSchema(connector?.configSchema);
  const values = parseJsonObject(detail.value.configValuesJson);
  return fields.map((field) => {
    const raw = values[field.code];
    const value = raw === undefined || raw === null || raw === '' ? '未填写' : String(raw);
    let jsonText = '';
    if (field.component === 'JSON' && raw !== undefined && raw !== null && raw !== '') {
      try {
        jsonText = JSON.stringify(typeof raw === 'string' ? JSON.parse(raw) : raw, null, 2);
      } catch {
        jsonText = String(raw);
      }
    }
    return {
      label: field.label,
      value: field.component === 'JSON' ? '已配置 JSON' : value,
      dictCode: field.component === 'SYSTEM_DICT' ? field.dictCode : undefined,
      component: field.component,
      json: jsonText,
    };
  });
}

function buildDetailCapabilityRows(): Array<{ name: string; code: string; description: string }> {
  if (!detail.value) {
    return [];
  }
  const codes = parseList(detail.value.capabilitiesJson);
  const connector = connectors.value.find((item) => item.connectorCode === detail.value?.connectorCode);
  const declared = parseCapabilities(connector?.capabilitiesJson);
  return codes.map((code) => {
    const matched = declared.find((item) => item.code === code);
    return { name: matched?.name || code, code, description: matched?.description || '' };
  });
}

async function copyCallbackPath() {
  const path = detail.value?.callbackPath;
  if (!path) {
    return;
  }
  try {
    await navigator.clipboard.writeText(path);
    ElMessage.success('回调路径已复制');
  } catch {
    ElMessage.warning('浏览器不允许自动复制，请手动选择复制');
  }
}

function expiryTag(): { label: string; type: 'success' | 'warning' | 'danger' } | null {
  const expiresAt = detailCredential.value?.expiresAt;
  if (!expiresAt) {
    return null;
  }
  const expireTime = new Date(String(expiresAt).replace(' ', 'T')).getTime();
  if (Number.isNaN(expireTime)) {
    return null;
  }
  const days = Math.ceil((expireTime - Date.now()) / 86400000);
  if (days < 0) {
    return { label: '已过期', type: 'danger' };
  }
  if (days <= 7) {
    return { label: `${days} 天后到期`, type: 'warning' };
  }
  return { label: '有效', type: 'success' };
}

async function handleTestConnection() {
  const instanceCode = detail.value?.instanceCode;
  if (!instanceCode) {
    return;
  }
  testLoading.value = true;
  try {
    const result = await integrationInstanceApi.testConnection(instanceCode, '/');
    if (result.success) {
      ElMessage.success(`连接成功（${result.statusCode}，${result.elapsedMs}ms）`);
    } else {
      ElMessage.warning(`连接失败：${result.message || '未知原因'}`);
    }
  } finally {
    testLoading.value = false;
  }
}

async function openCallLogs() {
  const instanceCode = detail.value?.instanceCode;
  if (!instanceCode) {
    return;
  }
  callLogsVisible.value = true;
  callLogsLoading.value = true;
  try {
    callLogs.value = (await integrationInstanceApi.callLogs(instanceCode, 50)) || [];
  } finally {
    callLogsLoading.value = false;
  }
}

async function openRotations() {
  const credentialCode = detail.value?.credentialCode;
  if (!credentialCode) {
    return;
  }
  rotationsVisible.value = true;
  rotationsLoading.value = true;
  try {
    rotations.value = (await integrationCredentialApi.rotations(credentialCode)) || [];
  } finally {
    rotationsLoading.value = false;
  }
}

function openRotate() {
  const connector = connectors.value.find((item) => item.connectorCode === detail.value?.connectorCode);
  rotateFields.value = parseAuthSchema(connector?.authSchema).map((field) => ({
    code: field.code,
    label: field.label,
    type: field.type,
    secret: field.secret,
    required: field.required,
  }));
  Object.keys(rotateValues).forEach((key) => delete rotateValues[key]);
  rotateVisible.value = true;
}

async function submitRotate() {
  const credentialCode = detail.value?.credentialCode;
  if (!credentialCode) {
    return;
  }
  const payload = Object.fromEntries(
    Object.entries(rotateValues).filter(([, value]) => value !== undefined && value !== ''),
  );
  if (!Object.keys(payload).length) {
    ElMessage.error('请至少填写一个字段');
    return;
  }
  rotateLoading.value = true;
  try {
    await integrationCredentialApi.rotate(credentialCode, JSON.stringify(payload));
    ElMessage.success('凭证已轮换');
    rotateVisible.value = false;
    await handleView(detail.value as IntegrationInstance);
  } finally {
    rotateLoading.value = false;
  }
}

async function handleEdit(row: IntegrationInstance) {
  resetForm();
  editing.value = true;
  Object.assign(form, {
    instanceCode: row.instanceCode,
    instanceName: row.instanceName,
    connectorCode: row.connectorCode,
    environment: row.environment || 'PROD',
    owner: row.owner || '',
    status: row.status === 'ENABLED' ? 1 : 0,
    remark: row.remark || '',
  });
  selectedCapabilities.value = parseList(row.capabilitiesJson);
  await loadConnector(row.connectorCode);
  Object.assign(configValues, parseJsonObject(row.configValuesJson));
  dialogVisible.value = true;
}

async function loadConnector(connectorCode: string) {
  if (!connectorCode) {
    currentConnector.value = undefined;
    return;
  }
  currentConnector.value = await integrationConnectorApi.detail(connectorCode);
}

async function handleConnectorChange(connectorCode: string) {
  Object.keys(configValues).forEach((key) => delete configValues[key]);
  Object.keys(credentialValues).forEach((key) => delete credentialValues[key]);
  selectedCapabilities.value = [];
  await loadConnector(connectorCode);
  configFields.value.forEach((field) => {
    if (field.defaultValue !== undefined) {
      configValues[field.code] = field.defaultValue;
    }
  });
  selectedCapabilities.value = parseCapabilities(currentConnector.value?.capabilitiesJson).map((item) => item.code);
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (!currentConnector.value) {
    ElMessage.error('请先选择连接器');
    return;
  }
  Object.keys(configErrors).forEach((key) => delete configErrors[key]);
  configFields.value.forEach((field) => {
    const value = configValues[field.code];
    const text = value === undefined || value === null ? '' : String(value).trim();
    if (field.required && text === '') {
      configErrors[field.code] = '必填';
      return;
    }
    if (field.component === 'JSON' && text !== '') {
      try {
        JSON.parse(text);
      } catch {
        configErrors[field.code] = '不是合法 JSON';
      }
    }
  });
  if (Object.keys(configErrors).length) {
    ElMessage.error('请修正连接器配置后再保存');
    return;
  }
  const missingCredential = credentialFields.value
    .filter((field) => field.required && !editing.value)
    .find((field) => !String(credentialValues[field.code] || '').trim());
  if (missingCredential) {
    ElMessage.error(`请填写必填凭证：${missingCredential.label}`);
    return;
  }
  submitLoading.value = true;
  try {
    const credentialCode = `${form.instanceCode}-credential`;
    const credentialPayload = Object.fromEntries(
      Object.entries(credentialValues).filter(([, value]) => value !== undefined && value !== ''),
    );
    if (Object.keys(credentialPayload).length) {
      await integrationCredentialApi.save({
        credentialCode,
        connectorCode: form.connectorCode,
        fieldsJson: JSON.stringify(credentialPayload),
        remark: `${form.instanceName} 凭证`,
      });
    }
    await integrationInstanceApi.save({
      instanceCode: form.instanceCode,
      instanceName: form.instanceName,
      connectorCode: form.connectorCode,
      environment: form.environment,
      configValuesJson: JSON.stringify(configValues),
      credentialCode: Object.keys(credentialPayload).length || editing.value ? credentialCode : undefined,
      capabilitiesJson: JSON.stringify(selectedCapabilities.value),
      status: form.status,
      owner: form.owner,
      remark: form.remark,
    });
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await loadData();
  } finally {
    submitLoading.value = false;
  }
}

async function handleToggle(row: IntegrationInstance) {
  await integrationInstanceApi.changeStatus(row.instanceCode, row.status === 'ENABLED' ? 0 : 1);
  ElMessage.success('状态更新成功');
  await loadData();
}

function parseJsonObject(json?: string): Record<string, unknown> {
  if (!json) {
    return {};
  }
  try {
    const parsed = JSON.parse(json);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function handleSearch() {
  query.page = 1;
  loadData();
}

function handleReset() {
  query.keyword = '';
  query.connectorCode = undefined;
  query.status = undefined;
  query.page = 1;
  loadData();
}

async function loadData() {
  loading.value = true;
  try {
    const result = await integrationInstanceApi.page(query);
    tableData.value = result.list || [];
    total.value = result.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadConnectors() {
  const result = await integrationConnectorApi.page({ page: 1, size: 200 });
  connectors.value = result.list || [];
}

const enabledConnectors = computed(() => connectors.value.filter((item) => item.status === 1));

onMounted(async () => {
  await Promise.all([loadConnectors(), loadData()]);
});
</script>

<style scoped lang="scss">
.integration-instance-page {
  min-width: 0;

  .code-text {
    font-family: 'SFMono-Regular', Consolas, monospace;
    font-size: 12px;
    color: var(--el-text-color-regular);
  }

  .ellipsis {
    overflow: hidden;
    max-width: 100%;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .muted {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .credential-meta {
    margin-bottom: 12px;
  }

  .json-preview {
    margin: 0;
    max-height: 240px;
    overflow: auto;
    font-family: 'SFMono-Regular', Consolas, monospace;
    font-size: 12px;
    line-height: 1.5;
  }

  .detail-actions {
    margin-top: 12px;
    text-align: right;
  }

  .option-desc {
    float: right;
    margin-left: 12px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 12px;
  }

  .section-head {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin: 16px 0 10px;
    padding-bottom: 6px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .section-desc {
    flex: 1;
    min-width: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  @media (width <= 720px) {
    .form-grid {
      grid-template-columns: minmax(0, 1fr);
    }
  }
}
</style>
