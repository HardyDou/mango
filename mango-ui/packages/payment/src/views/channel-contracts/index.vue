<template>
  <PaymentResourcePage
    title="签约通道"
    description="登记企业主体在具体支付通道下的商户号、AppId、证书密钥配置和已开通支付能力。"
    :columns="columns"
    :api="paymentChannelContractApi"
    :defaults="{ status: 1 }"
    :rules="rules"
    :to-save-payload="toSavePayload"
    :row-actions="contractRowActions"
    :on-editor-opened="onEditorOpened"
  >
    <template #form="{ form }">
      <el-form-item label="企业主体" prop="subjectId">
        <PaymentEntitySelect
          v-model="form.subjectId"
          :api="paymentEnterpriseSubjectApi"
          label-field="subjectName"
          description-field="creditCodeMask"
          placeholder="请选择企业主体"
          @loaded="subjectLabels.setOptions"
        />
      </el-form-item>
      <el-form-item label="支付通道" prop="channelId">
        <PaymentEntitySelect
          v-model="form.channelId"
          :api="paymentChannelApi"
          label-field="channelName"
          description-field="adapterType"
          placeholder="请选择支付通道"
          @loaded="channelLabels.setOptions"
          @update:model-value="value => onChannelChange(form, value)"
        />
      </el-form-item>
      <el-form-item label="商户号" prop="merchantNo">
        <el-input v-model="form.merchantNo" />
      </el-form-item>
      <el-form-item label="AppId">
        <el-input v-model="form.appId" />
      </el-form-item>
      <el-form-item label="配置值">
        <PaymentChannelConfigValuesForm
          v-model="form.configValuesJson"
          :field-template-json="selectedChannelTemplate"
          :channel-id="form.channelId"
          :channel-api="paymentChannelApi"
        />
      </el-form-item>
      <el-form-item label="开通能力">
        <div class="contract-capability-editor">
          <el-alert
            v-if="!form.channelId"
            type="info"
            :closable="false"
            title="请选择支付通道后维护签约能力"
          />
          <template v-else>
            <el-table
              :data="contractCapabilities(form)"
              border
              size="small"
              empty-text="当前支付通道暂无可开通能力"
              class="contract-capability-editor__table"
            >
              <el-table-column label="开通" width="70" align="center">
                <template #default="{ row }">
                  <el-checkbox
                    :model-value="isCapabilityEnabled(form, row.id)"
                    @update:model-value="checked => toggleCapability(form, row, Boolean(checked))"
                  />
                </template>
              </el-table-column>
              <el-table-column label="支付方式" min-width="160">
                <template #default="{ row }">
                  <span>{{ row.methodName || row.methodCode }}</span>
                </template>
              </el-table-column>
              <el-table-column label="终端" width="100">
                <template #default="{ row }">{{ terminalText(row.terminalType) }}</template>
              </el-table-column>
              <el-table-column label="费率" width="130">
                <template #default="{ row }">
                  <el-input-number
                    :model-value="capabilityForm(form, row.id)?.feeRate"
                    :disabled="!isCapabilityEnabled(form, row.id)"
                    :min="0"
                    :max="1"
                    :step="0.0001"
                    :precision="6"
                    controls-position="right"
                    @update:model-value="value => updateCapabilityField(form, row.id, 'feeRate', value ?? undefined)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="最小金额（元）" width="150">
                <template #default="{ row }">
                  <el-input-number
                    :model-value="centsToYuan(capabilityForm(form, row.id)?.minAmount)"
                    :disabled="!isCapabilityEnabled(form, row.id)"
                    :min="0"
                    :precision="2"
                    :step="1"
                    controls-position="right"
                    @update:model-value="value => updateCapabilityField(form, row.id, 'minAmount', value === undefined || value === null ? undefined : yuanToCents(value))"
                  />
                </template>
              </el-table-column>
              <el-table-column label="最大金额（元）" width="150">
                <template #default="{ row }">
                  <el-input-number
                    :model-value="centsToYuan(capabilityForm(form, row.id)?.maxAmount)"
                    :disabled="!isCapabilityEnabled(form, row.id)"
                    :min="0"
                    :precision="2"
                    :step="1"
                    controls-position="right"
                    @update:model-value="value => updateCapabilityField(form, row.id, 'maxAmount', value === undefined || value === null ? undefined : yuanToCents(value))"
                  />
                </template>
              </el-table-column>
              <el-table-column label="优先级" width="120">
                <template #default="{ row }">
                  <el-input-number
                    :model-value="capabilityForm(form, row.id)?.priority"
                    :disabled="!isCapabilityEnabled(form, row.id)"
                    :min="1"
                    :precision="0"
                    controls-position="right"
                    @update:model-value="value => updateCapabilityField(form, row.id, 'priority', value ?? undefined)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="证书有效期" width="190">
                <template #default="{ row }">
                  <el-date-picker
                    :model-value="capabilityForm(form, row.id)?.certificateExpireTime"
                    :disabled="!isCapabilityEnabled(form, row.id)"
                    type="datetime"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    format="YYYY-MM-DD HH:mm:ss"
                    @update:model-value="value => updateCapabilityField(form, row.id, 'certificateExpireTime', value || undefined)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-switch
                    :model-value="capabilityForm(form, row.id)?.status !== 0"
                    :disabled="!isCapabilityEnabled(form, row.id)"
                    active-text="启用"
                    inactive-text="停用"
                    inline-prompt
                    @update:model-value="value => updateCapabilityField(form, row.id, 'status', value ? 1 : 0)"
                  />
                </template>
              </el-table-column>
            </el-table>
          </template>
        </div>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </template>
  </PaymentResourcePage>

  <el-dialog
    v-model="billSourceDialogVisible"
    title="账单获取配置"
    width="880px"
    destroy-on-close
    append-to-body
  >
    <section class="bill-source-config">
      <div class="bill-source-config__summary">
        <span>{{ currentContract?.subjectName || subjectLabels.labelOf(currentContract?.subjectId) }}</span>
        <span>{{ currentContract?.channelName || channelLabels.labelOf(currentContract?.channelId) }}</span>
        <span>{{ currentContract?.merchantNo || '-' }}</span>
      </div>
      <el-alert
        v-if="!fetchModeOptions.length"
        type="warning"
        :closable="false"
        show-icon
        title="当前支付通道尚未声明账单获取方式，请先在支付通道中配置。"
      />
      <el-form ref="billSourceFormRef" :model="billSourceForm" :rules="billSourceRules" label-width="112px" class="payment-dialog-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="获取方式" prop="fetchMode">
              <el-select v-model="billSourceForm.fetchMode" placeholder="请选择获取方式">
                <el-option
                  v-for="item in fetchModeOptions"
                  :key="item.fetchMode"
                  :label="item.fetchModeName"
                  :value="item.fetchMode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="启用状态" prop="enabled">
              <el-switch
                v-model="billSourceEnabled"
                active-text="启用"
                inactive-text="停用"
                inline-prompt
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="接口/服务器" prop="endpoint">
              <el-input v-model="billSourceForm.endpoint" clearable placeholder="HTTP 地址或 FTP/FTPS 服务器" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="远端路径">
              <el-input v-model="billSourceForm.remotePath" clearable placeholder="FTP/FTPS 远端文件路径" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="认证引用">
              <el-input v-model="billSourceForm.credentialRef" clearable placeholder="认证配置引用，不填写明文密钥" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="分页模式">
              <el-select v-model="billSourceForm.pageMode" clearable placeholder="HTTP 获取时可选">
                <el-option label="分页" value="PAGE" />
                <el-option label="游标" value="CURSOR" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div class="bill-source-config__actions">
        <el-button @click="resetBillSourceForm">清空</el-button>
        <el-button type="primary" :loading="savingBillSource" @click="submitBillSource">保存配置</el-button>
      </div>
      <el-table :data="billSourceRows" row-key="id" stripe class="bill-source-config__table">
        <el-table-column label="获取方式" width="120">
          <template #default="{ row }">{{ row.fetchModeName || row.fetchMode }}</template>
        </el-table-column>
        <el-table-column prop="endpoint" label="接口/服务器" min-width="180" show-overflow-tooltip />
        <el-table-column prop="remotePath" label="远端路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="credentialRef" label="认证引用" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="editBillSource(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import type { ApiId } from '@mango/api-schema';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import PaymentResourcePage from '../../components/PaymentResourcePage.vue';
import PaymentEntitySelect from '../../components/PaymentEntitySelect.vue';
import PaymentChannelConfigValuesForm from '../../components/PaymentChannelConfigValuesForm.vue';
import { usePaymentEntityLabels } from '../../components/PaymentEntityLabels';
import type { PaymentRowAction } from '../../components/PaymentRowActions';
import {
  paymentChannelApi,
  paymentChannelCapabilityApi,
  paymentChannelContractApi,
  paymentReconciliationApi,
  paymentEnterpriseSubjectApi,
  type PaymentChannelBillFetchMode,
  type PaymentChannelBillSource,
  type PaymentChannel,
  type PaymentChannelCapability,
  type PaymentChannelContractCapability,
  type PaymentChannelContract,
  type PaymentRecord,
  type SavePaymentChannelBillSourceCommand,
  type PaymentTableColumn,
} from '../../api/payment';

const subjectLabels = usePaymentEntityLabels();
const channelLabels = usePaymentEntityLabels();
const selectedChannelTemplate = ref<string>();
const channelCapabilities = ref<PaymentChannelCapability[]>([]);
const billSourceDialogVisible = ref(false);
const savingBillSource = ref(false);
const currentContract = ref<PaymentChannelContract>();
const billSourceRows = ref<PaymentChannelBillSource[]>([]);
const fetchModeOptions = ref<PaymentChannelBillFetchMode[]>([]);
const currentChannel = ref<PaymentChannel>();
const billSourceFormRef = ref<FormInstance>();

type PaymentChannelContractSavePayload = Omit<PaymentChannelContract, 'contractCode' | 'environment'> & PaymentRecord;
type PaymentChannelCapabilityOption = Omit<PaymentChannelCapability, 'environment'> & {
  environment?: string;
};
type BillSourceForm = SavePaymentChannelBillSourceCommand;

const billSourceForm = reactive<BillSourceForm>({
  id: undefined,
  contractId: '',
  fetchMode: 'MANUAL',
  endpoint: '',
  remotePath: '',
  credentialRef: '',
  pageMode: '',
  enabled: 1,
});

const billSourceEnabled = computed({
  get: () => billSourceForm.enabled === 1,
  set: value => { billSourceForm.enabled = value ? 1 : 0; },
});

const columns: PaymentTableColumn[] = [
  { prop: 'subjectName', label: '企业主体', minWidth: 190, formatter: (row, value) => String(value || subjectLabels.labelOf(row.subjectId)) },
  { prop: 'channelName', label: '支付通道', minWidth: 180, formatter: (row, value) => String(value || channelLabels.labelOf(row.channelId)) },
  { prop: 'merchantNo', label: '商户号', minWidth: 180 },
  { prop: 'capabilities', label: '开通能力', minWidth: 260, variant: 'tags', formatter: row => formatCapabilities(row.capabilities) },
];

const rules: FormRules = {
  subjectId: [{ required: true, message: '请选择企业主体', trigger: 'change' }],
  channelId: [{ required: true, message: '请选择支付通道', trigger: 'change' }],
  merchantNo: [{ required: true, message: '请输入商户号', trigger: 'blur' }],
};

const billSourceRules: FormRules<BillSourceForm> = {
  fetchMode: [{ required: true, message: '请选择获取方式', trigger: 'change' }],
  enabled: [{ required: true, message: '请选择启用状态', trigger: 'change' }],
};

function contractRowActions(row: PaymentRecord): PaymentRowAction[] {
  return [
    {
      key: 'bill-source',
      label: '账单配置',
      type: 'primary',
      onClick: () => openBillSourceDialog(row as PaymentChannelContract),
    },
  ];
}

async function openBillSourceDialog(row: PaymentChannelContract) {
  currentContract.value = row;
  currentChannel.value = row.channelId ? await paymentChannelApi.detail(row.channelId) : undefined;
  billSourceDialogVisible.value = true;
  await Promise.all([loadFetchModes(), loadBillSources(row.id)]);
  resetBillSourceForm();
}

async function loadFetchModes() {
  const options = await paymentReconciliationApi.billFetchModes();
  const supportedModes = currentChannel.value?.billFetchModes || [];
  fetchModeOptions.value = options.filter(item => supportedModes.includes(item.fetchMode));
}

async function loadBillSources(contractId?: ApiId) {
  if (!contractId) {
    billSourceRows.value = [];
    return;
  }
  const page = await paymentChannelContractApi.billSources({ pageNum: 1, pageSize: 20, contractId });
  billSourceRows.value = page.list;
}

function resetBillSourceForm() {
  billSourceForm.id = undefined;
  billSourceForm.contractId = currentContract.value?.id || '';
  billSourceForm.fetchMode = fetchModeOptions.value[0]?.fetchMode || '';
  billSourceForm.endpoint = '';
  billSourceForm.remotePath = '';
  billSourceForm.credentialRef = '';
  billSourceForm.pageMode = '';
  billSourceForm.enabled = 1;
}

function editBillSource(row: PaymentChannelBillSource) {
  billSourceForm.id = row.id;
  billSourceForm.contractId = row.contractId || currentContract.value?.id || '';
  billSourceForm.fetchMode = row.fetchMode || 'MANUAL';
  billSourceForm.endpoint = row.endpoint || '';
  billSourceForm.remotePath = row.remotePath || '';
  billSourceForm.credentialRef = row.credentialRef || '';
  billSourceForm.pageMode = row.pageMode || '';
  billSourceForm.enabled = row.enabled === 0 ? 0 : 1;
}

async function submitBillSource() {
  await billSourceFormRef.value?.validate();
  savingBillSource.value = true;
  try {
    await paymentChannelContractApi.saveBillSource({
      id: billSourceForm.id,
      contractId: billSourceForm.contractId,
      fetchMode: billSourceForm.fetchMode,
      endpoint: optionalString(billSourceForm.endpoint),
      remotePath: optionalString(billSourceForm.remotePath),
      credentialRef: optionalString(billSourceForm.credentialRef),
      pageMode: optionalString(billSourceForm.pageMode),
      enabled: billSourceForm.enabled,
    });
    ElMessage.success('账单获取配置已保存');
    await loadBillSources(currentContract.value?.id);
    resetBillSourceForm();
  } finally {
    savingBillSource.value = false;
  }
}

async function onChannelChange(form: PaymentRecord, value: ApiId | ApiId[] | string) {
  const channelId = Array.isArray(value) ? value[0] : value;
  form.capabilities = [];
  await loadChannelContext(channelId);
}

async function onEditorOpened(form: PaymentRecord) {
  await loadChannelContext(form.channelId as ApiId | string | undefined);
}

async function loadChannelContext(channelId: ApiId | string | undefined) {
  selectedChannelTemplate.value = channelId ? (await paymentChannelApi.detail(channelId)).fieldTemplateJson : undefined;
  await loadChannelCapabilities(channelId);
}

async function loadChannelCapabilities(channelId: ApiId | string | undefined) {
  if (!channelId) {
    channelCapabilities.value = [];
    return;
  }
  const result = await paymentChannelCapabilityApi.page({
    pageNum: 1,
    pageSize: 200,
    channelId,
    status: 1,
  });
  channelCapabilities.value = result.list;
}

function contractCapabilities(form: PaymentRecord) {
  return [
    ...channelCapabilities.value,
    ...toCapabilityForms(form)
      .filter(item => !channelCapabilities.value.some(capability => String(capability.id) === String(item.channelCapabilityId)))
      .map(item => ({
        id: item.channelCapabilityId,
        channelId: form.channelId,
        methodCode: item.methodCode || String(item.channelCapabilityId),
        methodName: item.methodName,
        terminalType: item.terminalType || '-',
        status: item.status,
      } as PaymentChannelCapabilityOption)),
  ].filter(item => item.id);
}

function toCapabilityForms(form: PaymentRecord): PaymentChannelContractCapability[] {
  return Array.isArray(form.capabilities) ? form.capabilities : [];
}

function isCapabilityEnabled(form: PaymentRecord, capabilityId?: ApiId) {
  return Boolean(capabilityId && toCapabilityForms(form).some(item => String(item.channelCapabilityId) === String(capabilityId)));
}

function capabilityForm(form: PaymentRecord, capabilityId?: ApiId) {
  return toCapabilityForms(form).find(item => String(item.channelCapabilityId) === String(capabilityId));
}

function toggleCapability(form: PaymentRecord, capability: PaymentChannelCapabilityOption, checked: boolean) {
  const rows = toCapabilityForms(form).filter(item => String(item.channelCapabilityId) !== String(capability.id));
  if (checked && capability.id) {
    rows.push({
      channelCapabilityId: capability.id,
      methodCode: capability.methodCode,
      methodName: capability.methodName,
      terminalType: capability.terminalType,
      minAmount: capability.minAmount,
      maxAmount: capability.maxAmount,
      priority: 100,
      status: 1,
    });
  }
  form.capabilities = rows;
}

function updateCapabilityField(form: PaymentRecord, capabilityId: ApiId | undefined, field: keyof PaymentChannelContractCapability, value: unknown) {
  const row = capabilityForm(form, capabilityId);
  if (!row) return;
  (row as Record<string, unknown>)[field] = value;
}

function formatCapabilities(value: unknown) {
  const rows = Array.isArray(value) ? value as PaymentChannelContractCapability[] : [];
  if (!rows.length) return '-';
  return rows.map(item => `${item.methodName || item.methodCode || item.channelCapabilityId}/${terminalText(item.terminalType)}`).join(',');
}

function terminalText(value: unknown) {
  const text = optionalString(value);
  if (text === 'WEB') return '电脑网页';
  if (text === 'H5') return '手机网页';
  if (text === 'MINI_PROGRAM') return '小程序';
  if (text === 'APP') return 'App';
  if (text === 'POS') return 'POS';
  if (text === 'OFFLINE') return '线下';
  if (text === 'ALL') return '全端';
  return text || '-';
}

function toSavePayload(form: PaymentRecord): PaymentChannelContractSavePayload {
  return {
    id: form.id as PaymentChannelContract['id'],
    contractName: buildContractName(form),
    subjectId: form.subjectId as ApiId,
    channelId: form.channelId as ApiId,
    merchantNo: String(form.merchantNo || ''),
    appId: optionalString(form.appId),
    configValuesJson: optionalString(form.configValuesJson),
    enabledMethodCodes: enabledMethodCodes(form),
    capabilities: toCapabilityForms(form).map(item => ({
      id: item.id,
      channelCapabilityId: item.channelCapabilityId,
      feeRate: item.feeRate,
      minAmount: item.minAmount,
      maxAmount: item.maxAmount,
      priority: item.priority,
      certificateExpireTime: item.certificateExpireTime,
      status: item.status ?? 1,
    })),
    status: Number(form.status ?? 1),
  };
}

function buildContractName(form: PaymentRecord) {
  const subjectName = subjectLabels.labelOf(form.subjectId);
  const channelName = channelLabels.labelOf(form.channelId);
  const merchantNo = optionalString(form.merchantNo) || '未填写商户号';
  return [subjectName, channelName, merchantNo].filter(Boolean).join(' / ');
}

function optionalString(value: unknown) {
  return value === undefined || value === null || value === '' ? undefined : String(value);
}

function enabledMethodCodes(form: PaymentRecord) {
  const methodCodes = toCapabilityForms(form)
    .map(item => item.methodCode)
    .filter((value): value is string => Boolean(value));
  return methodCodes.length ? Array.from(new Set(methodCodes)).join(',') : undefined;
}

function centsToYuan(value?: number | null) {
  if (value === undefined || value === null) return undefined;
  return Number((Number(value) / 100).toFixed(2));
}

function yuanToCents(value: number) {
  return Math.round(Number(value || 0) * 100);
}
</script>

<style scoped>
.contract-capability-editor {
  width: 100%;
}

.contract-capability-editor__table {
  width: 100%;
}

.contract-capability-editor :deep(.el-input-number),
.contract-capability-editor :deep(.el-date-editor) {
  width: 100%;
}

.bill-source-config {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bill-source-config__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  padding: 10px 12px;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-light);
  border-radius: 6px;
}

.bill-source-config__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.bill-source-config__table {
  width: 100%;
}
</style>
