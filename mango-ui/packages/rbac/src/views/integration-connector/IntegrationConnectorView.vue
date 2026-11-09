<template>
  <MangoListPage class="integration-connector-page" data-page="integration.definition">
    <template #search>
      <MangoSearchPanel :model="query" :columns="3" @search="loadData" @reset="handleReset">
        <el-form-item label="连接器">
          <el-input v-model="query.keyword" clearable placeholder="编码或名称" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="用途分类">
          <DictSelect
            v-model="query.category"
            dict-type="integration_connector_category"
            placeholder="全部"
            clearable
            class="full-width"
            @change="loadData"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" @change="loadData">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #actions>
        <el-button data-action="connector.cleanup" @click="cleanupVisible = true">清理日志</el-button>
        <el-button type="primary" plain data-action="connector.create" @click="handleAdd">新增连接器</el-button>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe empty-text="暂无连接器，点击“新增连接器”开始登记">
        <el-table-column label="连接器" min-width="200">
          <template #default="{ row }">
            <div class="name-text">{{ row.connectorName }}</div>
            <div class="code-text">{{ row.connectorCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用途分类" width="120">
          <template #default="{ row }">
            <DictTag
              v-if="row.category"
              dict-code="integration_connector_category"
              :value="row.category"
              size="small"
            />
            <span v-else class="muted">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="厂商" width="120">
          <template #default="{ row }">{{ row.provider || '-' }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="item in row.tags || []" :key="item" size="small" type="info" class="tag-item">
              {{ item }}
            </el-tag>
            <span v-if="!row.tags?.length" class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="能力" width="100">
          <template #default="{ row }">
            <el-tooltip
              v-if="parseCapabilities(row.capabilitiesJson).length"
              :content="
                parseCapabilities(row.capabilitiesJson)
                  .map((item) => `${item.name}（${item.code}）`)
                  .join('、')
              "
              placement="top"
            >
              <el-tag size="small" type="success">{{ parseCapabilities(row.capabilitiesJson).length }} 项</el-tag>
            </el-tooltip>
            <span v-else class="muted">未声明</span>
          </template>
        </el-table-column>
        <el-table-column label="被引用" width="90">
          <template #default="{ row }">{{ row.instanceCount || 0 }} 个</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag :type="row.source === 'DECLARED' ? 'warning' : 'info'" size="small">
              {{ row.source === 'DECLARED' ? '资源声明' : '页面维护' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" data-action="connector.view" @click="openDetail(row)"
              >查看</el-button
            >
            <el-tooltip v-if="row.source === 'DECLARED'" content="由资源声明维护，请在声明文件中修改" placement="top">
              <span><el-button link type="primary" size="small" disabled>编辑</el-button></span>
            </el-tooltip>
            <el-button v-else link type="primary" size="small" data-action="connector.edit" @click="handleEdit(row)"
              >编辑</el-button
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

    <MangoDialog v-model="cleanupVisible" title="清理历史日志" width="480px">
      <el-form label-width="110px">
        <el-form-item label="清理对象">
          <el-select v-model="cleanupResource" class="full-width">
            <el-option label="调用记录" value="CALL_LOG" />
            <el-option label="凭证读取记录" value="ACCESS_LOG" />
            <el-option label="凭证轮换记录" value="ROTATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="保留天数">
          <el-input-number v-model="cleanupDays" :min="1" :max="3650" />
        </el-form-item>
      </el-form>
      <div class="field-hint">早于保留天数的记录会被永久删除，操作不可恢复。</div>
      <template #footer>
        <el-button @click="cleanupVisible = false">取消</el-button>
        <el-button type="danger" :loading="cleanupLoading" @click="handleCleanup">确认清理</el-button>
      </template>
    </MangoDialog>

    <MangoDialog v-model="detailVisible" title="连接器定义" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="编码">{{ current?.connectorCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ current?.connectorName }}</el-descriptions-item>
        <el-descriptions-item label="用途分类">
          <DictTag
            v-if="current?.category"
            dict-code="integration_connector_category"
            :value="current.category"
            size="small"
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="厂商">{{ current?.provider || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ current?.status === 1 ? '启用' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="文档">
          <a v-if="current?.docsUrl" :href="current.docsUrl" target="_blank" rel="noreferrer">{{ current.docsUrl }}</a>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="section-head"><span class="section-title">配置规范</span></div>
      <el-table :data="parseConfig(current?.configSchema)" size="small" border empty-text="未声明配置">
        <el-table-column prop="label" label="名称" min-width="130" />
        <el-table-column prop="code" label="字段编码" min-width="140" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ componentLabel(row.component) }}</template>
        </el-table-column>
        <el-table-column label="必填" width="70">
          <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="默认值" min-width="120">
          <template #default="{ row }">{{ row.defaultValue ?? '-' }}</template>
        </el-table-column>
      </el-table>

      <div class="section-head"><span class="section-title">认证规范</span></div>
      <el-table :data="parseAuth(current?.authSchema).fields" size="small" border empty-text="未声明认证字段">
        <el-table-column prop="label" label="名称" min-width="130" />
        <el-table-column prop="code" label="字段编码" min-width="140" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ credentialTypeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="必填" width="70">
          <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
        </el-table-column>
      </el-table>

      <div class="section-head"><span class="section-title">能力</span></div>
      <el-table :data="parseCapabilities(current?.capabilitiesJson)" size="small" border empty-text="未声明能力">
        <el-table-column prop="name" label="名称" min-width="130" />
        <el-table-column prop="code" label="能力编码" min-width="150" />
        <el-table-column prop="description" label="介绍" min-width="180" />
      </el-table>

      <div class="section-head"><span class="section-title">引用该连接器的实例</span></div>
      <el-table v-loading="referenceLoading" :data="referenceInstances" size="small" border empty-text="暂无实例引用">
        <el-table-column prop="instanceName" label="实例名称" min-width="140" />
        <el-table-column prop="instanceCode" label="实例编码" min-width="150" />
        <el-table-column prop="environment" label="环境" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </MangoDialog>

    <MangoDialog
      v-model="dialogVisible"
      :title="editing ? '编辑连接器' : '新增连接器'"
      width="960px"
      class="connector-form-dialog"
      data-surface="connector.form"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="connector-form">
        <el-tabs v-model="formTab" class="connector-form__tabs">
          <el-tab-pane label="基础信息" name="basic">
            <div class="form-grid">
              <el-form-item label="连接器编码" prop="connectorCode">
                <el-input
                  v-model="form.connectorCode"
                  :disabled="editing"
                  data-field="connector.code"
                  placeholder="如 wecom-message"
                />
                <div class="field-hint">全局唯一，创建后不可修改。</div>
              </el-form-item>
              <el-form-item label="连接器名称" prop="connectorName">
                <el-input v-model="form.connectorName" data-field="connector.name" placeholder="展示给使用者的名称" />
              </el-form-item>
              <el-form-item label="用途分类">
                <DictSelect
                  v-model="form.category"
                  dict-type="integration_connector_category"
                  placeholder="如 支付、消息"
                  clearable
                  class="full-width"
                />
              </el-form-item>
              <el-form-item label="厂商">
                <el-input v-model="form.provider" placeholder="如 WECOM" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="form.status" class="full-width">
                  <el-option label="启用" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </el-form-item>
              <el-form-item label="文档地址">
                <el-input v-model="form.docsUrl" placeholder="https://" />
              </el-form-item>
              <el-form-item label="标签" class="span-2">
                <el-select
                  v-model="form.tags"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  class="full-width"
                  placeholder="输入后回车新增，例如 支付、微信"
                >
                  <el-option v-for="item in form.tags" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </div>
          </el-tab-pane>

          <el-tab-pane label="配置规范" name="config">
            <div class="tab-head">
              <span class="tab-desc">实例需要填写的非敏感配置，保存实例时按此校验。</span>
              <el-button type="primary" plain size="small" @click="addConfigRow">新增字段</el-button>
            </div>
            <el-table v-if="configRows.length" :data="configRows" size="small" border class="editor-table">
              <el-table-column label="名称" min-width="130">
                <template #default="{ row }"
                  ><el-input v-model="row.label" size="small" placeholder="如 接口地址"
                /></template>
              </el-table-column>
              <el-table-column label="字段编码" min-width="140">
                <template #default="{ row }"
                  ><el-input v-model="row.code" size="small" placeholder="如 apiBaseUrl"
                /></template>
              </el-table-column>
              <el-table-column label="类型" width="120">
                <template #default="{ row }">
                  <el-select v-model="row.component" size="small" @change="handleComponentChange(row)">
                    <el-option
                      v-for="item in componentOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="64">
                <template #default="{ row }"><el-switch v-model="row.required" size="small" /></template>
              </el-table-column>
              <el-table-column label="默认值" width="120">
                <template #default="{ row }"><el-input v-model="row.defaultValue" size="small" /></template>
              </el-table-column>
              <el-table-column label="字典/选项" width="150">
                <template #default="{ row }">
                  <el-input v-if="row.component === 'DICT'" v-model="row.options" size="small" placeholder="逗号分隔" />
                  <el-input
                    v-else-if="row.component === 'SYSTEM_DICT'"
                    v-model="row.dictCode"
                    size="small"
                    placeholder="字典编码"
                  />
                  <span v-else class="muted">-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row, $index }">
                  <el-button link type="primary" size="small" @click="openFieldAdvanced(row)">更多</el-button>
                  <el-button link type="danger" size="small" @click="configRows.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无配置字段，点击“新增字段”添加" :image-size="56" />
          </el-tab-pane>

          <el-tab-pane label="认证规范" name="auth">
            <div class="tab-head">
              <span class="tab-desc">凭证字段与认证方式；密钥类字段保存后只显示掩码。</span>
              <el-button type="primary" plain size="small" @click="addCredentialRow">新增字段</el-button>
            </div>
            <el-form label-width="96px">
              <div class="form-grid">
                <el-form-item label="认证方式">
                  <el-select v-model="authMode" class="full-width">
                    <el-option
                      v-for="item in authModeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </div>
            </el-form>
            <div class="tab-head">
              <span class="tab-desc">认证参数由平台工具类读取，如 tokenUrl、headerName、algorithm。</span>
              <el-button type="primary" plain size="small" @click="addParamRow">新增参数</el-button>
            </div>
            <el-table v-if="paramRows.length" :data="paramRows" size="small" border class="editor-table">
              <el-table-column label="参数名" min-width="200">
                <template #default="{ row }"><el-input v-model="row.key" size="small" /></template>
              </el-table-column>
              <el-table-column label="参数值" min-width="260">
                <template #default="{ row }"><el-input v-model="row.value" size="small" /></template>
              </el-table-column>
              <el-table-column label="操作" width="70">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="paramRows.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无认证参数" :image-size="48" />
            <el-table v-if="credentialRows.length" :data="credentialRows" size="small" border class="editor-table">
              <el-table-column label="名称" min-width="150">
                <template #default="{ row }"
                  ><el-input v-model="row.label" size="small" placeholder="如 企业 ID"
                /></template>
              </el-table-column>
              <el-table-column label="字段编码" min-width="160">
                <template #default="{ row }"
                  ><el-input v-model="row.code" size="small" placeholder="如 corpId"
                /></template>
              </el-table-column>
              <el-table-column label="类型" width="120">
                <template #default="{ row }">
                  <el-select v-model="row.type" size="small">
                    <el-option label="文本" value="STRING" />
                    <el-option label="密钥" value="SECRET" />
                    <el-option label="文件" value="FILE" />
                    <el-option label="证书" value="CERT" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="80">
                <template #default="{ row }"><el-switch v-model="row.required" size="small" /></template>
              </el-table-column>
              <el-table-column label="操作" width="70" fixed="right">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="credentialRows.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无认证字段，点击“新增字段”添加" :image-size="56" />
          </el-tab-pane>

          <el-tab-pane label="能力" name="capabilities">
            <div class="tab-head">
              <span class="tab-desc">声明该连接器能提供的能力，实例只能从这里勾选授权。</span>
              <el-button type="primary" plain size="small" @click="addCapabilityRow">新增能力</el-button>
            </div>
            <el-table v-if="capabilityRows.length" :data="capabilityRows" size="small" border class="editor-table">
              <el-table-column label="能力名称" min-width="150">
                <template #default="{ row }">
                  <el-input v-model="row.name" size="small" placeholder="如 发送应用消息" />
                </template>
              </el-table-column>
              <el-table-column label="能力编码" min-width="160">
                <template #default="{ row }">
                  <el-input v-model="row.code" size="small" placeholder="如 message.send" />
                </template>
              </el-table-column>
              <el-table-column label="介绍" min-width="220">
                <template #default="{ row }">
                  <el-input v-model="row.description" size="small" placeholder="说明该能力的用途" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70" fixed="right">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="capabilityRows.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无能力，点击“新增能力”添加" :image-size="56" />
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" data-action="connector.submit" @click="handleSubmit"
          >保存</el-button
        >
      </template>
    </MangoDialog>

    <MangoDialog v-model="advancedVisible" title="字段高级设置" width="520px">
      <el-form label-width="110px">
        <el-form-item label="字段">
          <span class="code-text">{{ advancedRow?.label }}（{{ advancedRow?.code }}）</span>
        </el-form-item>
        <el-form-item label="校验正则">
          <el-input v-model="advancedRow!.pattern" placeholder="如 ^https://.*" />
        </el-form-item>
        <el-form-item label="最小/最大值">
          <div class="range-inputs">
            <el-input v-model="advancedRow!.minValue" placeholder="最小" />
            <el-input v-model="advancedRow!.maxValue" placeholder="最大" />
          </div>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="advancedRow!.description" type="textarea" :rows="2" placeholder="展示给填写者" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="advancedVisible = false">完成</el-button>
      </template>
    </MangoDialog>
  </MangoListPage>
</template>

<script setup lang="ts" name="SystemIntegrationConnector">
import { DictSelect, DictTag, MangoDialog, MangoListPage, MangoListPanel, MangoSearchPanel } from '@mango/common';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import {
  integrationConnectorApi,
  integrationInstanceApi,
  integrationMaintenanceApi,
  type IntegrationConnector,
} from '../../api/integrationModel';

interface ConfigRow {
  code: string;
  label: string;
  component: string;
  required: boolean;
  defaultValue: string;
  description: string;
  options: string;
  dictCode: string;
  pattern: string;
  minValue: string;
  maxValue: string;
}
interface CredentialRow {
  code: string;
  label: string;
  type: string;
  required: boolean;
}
interface ParamRow {
  key: string;
  value: string;
}
interface CapabilityRow {
  code: string;
  name: string;
  description: string;
}

const componentOptions = [
  { label: '单行文本', value: 'INPUT' },
  { label: '多行文本', value: 'TEXTAREA' },
  { label: '数字', value: 'NUMBER' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: '字典', value: 'DICT' },
  { label: '系统字典', value: 'SYSTEM_DICT' },
  { label: 'JSON 字符串', value: 'JSON' },
  { label: '文件', value: 'FILE' },
];

const authModeOptions = [
  { label: '密钥', value: 'SECRET' },
  { label: 'API Key', value: 'API_KEY' },
  { label: 'OAuth2', value: 'OAUTH2' },
  { label: 'JWT', value: 'JWT' },
  { label: '双向 TLS', value: 'MTLS' },
  { label: '回调签名', value: 'CALLBACK_SIGNATURE' },
  { label: '免认证', value: 'NONE' },
];

const loading = ref(false);
const submitLoading = ref(false);
const detailVisible = ref(false);
const dialogVisible = ref(false);
const advancedVisible = ref(false);
const editing = ref(false);
const formTab = ref('basic');
const formRef = ref<FormInstance>();
const tableData = ref<IntegrationConnector[]>([]);
const current = ref<IntegrationConnector>();
const total = ref(0);

const configRows = ref<ConfigRow[]>([]);
const credentialRows = ref<CredentialRow[]>([]);
const paramRows = ref<ParamRow[]>([]);
const capabilityRows = ref<CapabilityRow[]>([]);
const authMode = ref('SECRET');
const advancedRow = ref<ConfigRow>();
const cleanupVisible = ref(false);
const cleanupLoading = ref(false);
const cleanupResource = ref<'CALL_LOG' | 'ACCESS_LOG' | 'ROTATION'>('CALL_LOG');
const cleanupDays = ref(90);
const referenceLoading = ref(false);
const referenceInstances = ref<Array<Record<string, unknown>>>([]);

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  category: undefined as string | undefined,
  status: undefined as number | undefined,
});

const form = reactive({
  connectorCode: '',
  connectorName: '',
  category: '',
  provider: '',
  status: 1,
  tags: [] as string[],
  docsUrl: '',
});

const rules: FormRules = {
  connectorCode: [
    { required: true, message: '请输入连接器编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9-]{1,63}$/, message: '只能包含小写字母、数字和中划线', trigger: 'blur' },
  ],
  connectorName: [{ required: true, message: '请输入连接器名称', trigger: 'blur' }],
};

function componentLabel(value?: string) {
  return componentOptions.find((item) => item.value === value)?.label || value || '单行文本';
}

function credentialTypeLabel(value?: string) {
  if (value === 'SECRET') return '密钥';
  if (value === 'FILE') return '文件';
  if (value === 'CERT') return '证书';
  return '文本';
}

function parseCapabilities(json?: string): CapabilityRow[] {
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

function parseConfig(json?: string): Array<Record<string, unknown>> {
  if (!json) {
    return [];
  }
  try {
    const schema = JSON.parse(json);
    const required: string[] = Array.isArray(schema.required) ? schema.required : [];
    return Object.entries(schema.properties || {}).map(([code, raw]) => {
      const value = raw as Record<string, unknown>;
      const hasEnum = Array.isArray(value.enum) && (value.enum as unknown[]).length > 0;
      return {
        code,
        label: value.title || code,
        component: String(
          value.component ||
            (hasEnum
              ? 'DICT'
              : value.type === 'integer' || value.type === 'number'
                ? 'NUMBER'
                : value.type === 'boolean'
                  ? 'BOOLEAN'
                  : 'INPUT'),
        ),
        required: required.includes(code),
        defaultValue: value.default,
        enum: value.enum,
        dictCode: value.dictCode,
        pattern: value.pattern,
        minimum: value.minimum,
        maximum: value.maximum,
        minLength: value.minLength,
        maxLength: value.maxLength,
        description: value.description,
      };
    });
  } catch {
    return [];
  }
}

function parseAuth(json?: string): {
  authMode?: string;
  params?: Record<string, unknown>;
  fields: Array<Record<string, unknown>>;
} {
  if (!json) {
    return { fields: [] };
  }
  try {
    const schema = JSON.parse(json);
    const fields = Array.isArray(schema.fields) ? schema.fields : [];
    return {
      authMode: schema.authMode,
      params: schema.params && typeof schema.params === 'object' ? schema.params : {},
      fields: fields.map((raw: Record<string, unknown>) => ({
        code: raw.fieldCode,
        label: raw.label,
        type: raw.type,
        required: Boolean(raw.required),
      })),
    };
  } catch {
    return { fields: [] };
  }
}

function handleReset() {
  query.keyword = '';
  query.category = undefined;
  query.status = undefined;
  query.page = 1;
  loadData();
}

async function loadData() {
  loading.value = true;
  try {
    const result = await integrationConnectorApi.page(query);
    tableData.value = result.list || [];
    total.value = result.total || 0;
  } finally {
    loading.value = false;
  }
}

async function openDetail(row: IntegrationConnector) {
  current.value = row;
  detailVisible.value = true;
  await loadReferenceInstances(row.connectorCode);
}

async function handleCleanup() {
  cleanupLoading.value = true;
  try {
    const deleted = await integrationMaintenanceApi.cleanup(cleanupResource.value, cleanupDays.value);
    ElMessage.success(`已清理 ${deleted} 条记录`);
    cleanupVisible.value = false;
  } finally {
    cleanupLoading.value = false;
  }
}

async function loadReferenceInstances(connectorCode: string) {
  referenceLoading.value = true;
  try {
    const result = await integrationInstanceApi.page({ page: 1, size: 50, connectorCode });
    referenceInstances.value = result.list || [];
  } finally {
    referenceLoading.value = false;
  }
}

function resetEditor() {
  configRows.value = [];
  credentialRows.value = [];
  paramRows.value = [];
  capabilityRows.value = [];
  authMode.value = 'SECRET';
  formTab.value = 'basic';
}

function handleAdd() {
  editing.value = false;
  resetEditor();
  Object.assign(form, {
    connectorCode: '',
    connectorName: '',
    category: '',
    provider: '',
    status: 1,
    tags: [],
    docsUrl: '',
  });
  dialogVisible.value = true;
}

function handleEdit(row: IntegrationConnector) {
  editing.value = true;
  resetEditor();
  Object.assign(form, {
    connectorCode: row.connectorCode,
    connectorName: row.connectorName,
    category: row.category || '',
    provider: row.provider || '',
    status: row.status ?? 1,
    tags: [...(row.tags || [])],
    docsUrl: row.docsUrl || '',
  });
  configRows.value = parseConfig(row.configSchema).map((item) => ({
    code: String(item.code),
    label: String(item.label),
    component: String(item.component || 'INPUT'),
    required: Boolean(item.required),
    defaultValue: item.defaultValue === undefined ? '' : String(item.defaultValue),
    description: item.description ? String(item.description) : '',
    options: Array.isArray(item.enum) ? (item.enum as unknown[]).join(',') : '',
    dictCode: item.dictCode ? String(item.dictCode) : '',
    pattern: item.pattern ? String(item.pattern) : '',
    minValue:
      item.minimum !== undefined ? String(item.minimum) : item.minLength !== undefined ? String(item.minLength) : '',
    maxValue:
      item.maximum !== undefined ? String(item.maximum) : item.maxLength !== undefined ? String(item.maxLength) : '',
  }));
  const auth = parseAuth(row.authSchema);
  authMode.value = auth.authMode || 'SECRET';
  paramRows.value = Object.entries(auth.params || {}).map(([key, value]) => ({ key, value: String(value) }));
  credentialRows.value = auth.fields.map((item) => ({
    code: String(item.code),
    label: String(item.label || item.code),
    type: String(item.type || 'STRING'),
    required: Boolean(item.required),
  }));
  capabilityRows.value = parseCapabilities(row.capabilitiesJson);
  dialogVisible.value = true;
}

function addConfigRow() {
  configRows.value.push({
    code: '',
    label: '',
    component: 'INPUT',
    required: false,
    defaultValue: '',
    description: '',
    options: '',
    dictCode: '',
    pattern: '',
    minValue: '',
    maxValue: '',
  });
}

function addCredentialRow() {
  credentialRows.value.push({ code: '', label: '', type: 'STRING', required: false });
}

function addParamRow() {
  paramRows.value.push({ key: '', value: '' });
}

function addCapabilityRow() {
  capabilityRows.value.push({ code: '', name: '', description: '' });
}

function openFieldAdvanced(row: ConfigRow) {
  advancedRow.value = row;
  advancedVisible.value = true;
}

function handleComponentChange(row: ConfigRow) {
  if (row.component !== 'DICT') {
    row.options = '';
  }
  if (row.component !== 'SYSTEM_DICT') {
    row.dictCode = '';
  }
}

function schemaTypeOf(component: string) {
  if (component === 'NUMBER') {
    return 'number';
  }
  if (component === 'BOOLEAN') {
    return 'boolean';
  }
  if (component === 'JSON') {
    return 'object';
  }
  return 'string';
}

function buildConfigSchema() {
  const properties: Record<string, unknown> = {};
  const required: string[] = [];
  configRows.value
    .filter((row) => row.code)
    .forEach((row) => {
      const property: Record<string, unknown> = {
        type: schemaTypeOf(row.component),
        title: row.label || row.code,
        component: row.component,
      };
      if (row.component === 'DICT') {
        property.enum = row.options
          .split(',')
          .map((item) => item.trim())
          .filter(Boolean);
      }
      if (row.component === 'SYSTEM_DICT') {
        property.dictCode = row.dictCode;
      }
      if (row.pattern) {
        property.pattern = row.pattern;
      }
      if (row.description) {
        property.description = row.description;
      }
      if (row.minValue !== '') {
        if (row.component === 'NUMBER') {
          property.minimum = Number(row.minValue);
        } else {
          property.minLength = Number(row.minValue);
        }
      }
      if (row.maxValue !== '') {
        if (row.component === 'NUMBER') {
          property.maximum = Number(row.maxValue);
        } else {
          property.maxLength = Number(row.maxValue);
        }
      }
      if (row.defaultValue !== '') {
        property.default =
          row.component === 'BOOLEAN'
            ? row.defaultValue === 'true'
            : row.component === 'NUMBER'
              ? Number(row.defaultValue)
              : row.defaultValue;
      }
      properties[row.code] = property;
      if (row.required) {
        required.push(row.code);
      }
    });
  return JSON.stringify({ type: 'object', required, properties });
}

function buildAuthSchema() {
  const params: Record<string, string> = {};
  paramRows.value
    .filter((row) => row.key)
    .forEach((row) => {
      params[row.key] = row.value;
    });
  return JSON.stringify({
    authMode: authMode.value,
    params,
    fields: credentialRows.value
      .filter((row) => row.code)
      .map((row) => ({ fieldCode: row.code, label: row.label || row.code, type: row.type, required: row.required })),
  });
}

function buildCapabilities() {
  return JSON.stringify(
    capabilityRows.value
      .filter((row) => row.code)
      .map((row) => ({ code: row.code, name: row.name || row.code, description: row.description || undefined })),
  );
}

function validateRows(): { tab: string; message: string } | null {
  const configCodes = configRows.value.map((row) => row.code).filter(Boolean);
  if (configRows.value.some((row) => !row.code)) {
    return { tab: 'config', message: '配置规范存在未填写字段编码的行' };
  }
  if (new Set(configCodes).size !== configCodes.length) {
    return { tab: 'config', message: '配置规范字段编码不能重复' };
  }
  const credentialCodes = credentialRows.value.map((row) => row.code).filter(Boolean);
  if (credentialRows.value.some((row) => !row.code)) {
    return { tab: 'auth', message: '认证规范存在未填写字段编码的行' };
  }
  if (new Set(credentialCodes).size !== credentialCodes.length) {
    return { tab: 'auth', message: '认证规范字段编码不能重复' };
  }
  if (paramRows.value.some((row) => !row.key && row.value)) {
    return { tab: 'auth', message: '认证参数存在未填写参数名的行' };
  }
  const capabilityCodes = capabilityRows.value.map((row) => row.code).filter(Boolean);
  if (capabilityRows.value.some((row) => !row.code || !row.name)) {
    return { tab: 'capabilities', message: '能力需要填写名称与编码' };
  }
  if (new Set(capabilityCodes).size !== capabilityCodes.length) {
    return { tab: 'capabilities', message: '能力编码不能重复' };
  }
  return null;
}

async function handleSubmit() {
  await formRef.value?.validate();
  const rowError = validateRows();
  if (rowError) {
    formTab.value = rowError.tab;
    ElMessage.error(rowError.message);
    return;
  }
  if (editing.value && form.status === 0) {
    const usage = await integrationInstanceApi.page({ page: 1, size: 1, connectorCode: form.connectorCode, status: 1 });
    const enabledCount = usage.total || 0;
    if (enabledCount > 0) {
      try {
        await ElMessageBox.confirm(
          `该连接器正被 ${enabledCount} 个启用中的实例使用，停用后这些实例将无法继续使用。确定停用吗？`,
          '确认停用连接器',
          { type: 'warning', confirmButtonText: '确定停用', cancelButtonText: '取消' },
        );
      } catch {
        return;
      }
    }
  }
  submitLoading.value = true;
  try {
    await integrationConnectorApi.save({
      connectorCode: form.connectorCode,
      connectorName: form.connectorName,
      category: form.category || undefined,
      provider: form.provider || undefined,
      status: form.status,
      configSchema: buildConfigSchema(),
      authSchema: buildAuthSchema(),
      capabilitiesJson: buildCapabilities(),
      tags: form.tags,
      docsUrl: form.docsUrl || undefined,
    });
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await loadData();
  } finally {
    submitLoading.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped lang="scss">
:global(.connector-form-dialog .mango-dialog__body) {
  height: min(560px, 62vh);
  overflow: hidden auto;
}

.integration-connector-page {
  min-width: 0;

  .name-text {
    font-weight: 500;
  }

  .code-text {
    font-family: 'SFMono-Regular', Consolas, monospace;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .tag-item {
    margin: 0 4px 4px 0;
  }

  .muted {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .section-head {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin: 16px 0 8px;
    padding-bottom: 6px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .connector-form__tabs :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }

  .tab-head {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;
  }

  .tab-desc {
    flex: 1;
    min-width: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 12px;
  }

  .span-2 {
    grid-column: span 2;
  }

  .editor-table {
    margin-bottom: 8px;
  }

  .field-hint {
    width: 100%;
    margin-top: 2px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .full-width {
    width: 100%;
  }

  .range-inputs {
    display: flex;
    gap: 8px;
  }

  @media (width <= 720px) {
    .form-grid {
      grid-template-columns: minmax(0, 1fr);
    }

    .span-2 {
      grid-column: span 1;
    }
  }
}
</style>
