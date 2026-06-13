<template>
  <PaymentResourcePage
    title="支付通道"
    description="维护芒果支付、通联支付、华夏银行等通道产品定义、适配器、签约字段模板和通道能力摘要。"
    :columns="columns"
    :api="paymentChannelApi"
    :defaults="{ status: 1, channelCode: 'MANGO_PAY', channelName: '芒果支付', channelType: 'BUILTIN_VIRTUAL', adapterType: 'MANGO_PAY', billFetchModes: ['MANUAL'] }"
    :rules="rules"
    editor-width="1180px"
    :to-save-payload="toSavePayload"
    :on-editor-opened="onEditorOpened"
  >
    <template #form="{ form }">
      <el-form-item label="通道编码" prop="channelCode">
        <el-select v-model="form.channelCode">
          <el-option label="芒果支付" value="MANGO_PAY" />
          <el-option label="线下收款" value="OFFLINE_COLLECTION" />
          <el-option label="通联支付" value="ALLINPAY" />
          <el-option label="富友支付" value="FUIOU_PAY" />
          <el-option label="华夏银行" value="HUAXIA_BANK" />
          <el-option label="微信支付" value="WECHAT_PAY" />
          <el-option label="支付宝" value="ALIPAY" />
          <el-option label="连连支付" value="LIANLIAN_PAY" />
        </el-select>
      </el-form-item>
      <el-form-item label="通道名称" prop="channelName">
        <el-input v-model="form.channelName" />
      </el-form-item>
      <el-form-item label="通道类型" prop="channelType">
        <el-select v-model="form.channelType">
          <el-option label="内置虚拟通道" value="BUILTIN_VIRTUAL" />
          <el-option label="内置线下通道" value="BUILTIN_OFFLINE" />
          <el-option label="聚合支付机构" value="AGGREGATOR" />
          <el-option label="银行通道" value="BANK" />
          <el-option label="直连通道" value="DIRECT" />
        </el-select>
      </el-form-item>
      <el-form-item label="适配器类型" prop="adapterType">
        <el-input
          v-model="form.adapterType"
          clearable
          placeholder="例如 MANGO_PAY、OFFLINE_COLLECTION、ALLINPAY"
        />
      </el-form-item>
      <el-form-item label="基础网关">
        <el-input v-model="form.gatewayBaseUrl" />
      </el-form-item>
      <el-form-item label="字段模板">
        <PaymentChannelFieldTemplateEditor v-model="form.fieldTemplateJson" />
      </el-form-item>
      <el-form-item label="能力摘要">
        <el-input v-model="form.capabilitySummary" type="textarea" :rows="3" placeholder="例如：微信扫码、支付宝 H5、退款、查单、账单、对账" />
      </el-form-item>
      <el-form-item label="账单获取">
        <el-checkbox-group v-model="form.billFetchModes">
          <el-checkbox value="MANUAL">手动上传</el-checkbox>
          <el-checkbox value="FTP">FTP 拉取</el-checkbox>
          <el-checkbox value="FTPS">FTPS 拉取</el-checkbox>
          <el-checkbox value="HTTP">HTTP 接口</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="通道能力">
        <PaymentChannelCapabilityEditor v-model="form.capabilities" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </template>
  </PaymentResourcePage>
</template>

<script setup lang="ts">
import type { FormRules } from 'element-plus';
import PaymentResourcePage from '../../components/PaymentResourcePage.vue';
import PaymentChannelFieldTemplateEditor from '../../components/PaymentChannelFieldTemplateEditor.vue';
import PaymentChannelCapabilityEditor from '../../components/PaymentChannelCapabilityEditor.vue';
import { paymentChannelApi, type PaymentChannel, type PaymentChannelCapability, type PaymentRecord, type PaymentTableColumn } from '../../api/payment';

const columns: PaymentTableColumn[] = [
  { prop: 'channelCode', label: '通道编码', minWidth: 140 },
  { prop: 'channelName', label: '通道名称', minWidth: 170 },
  { prop: 'channelType', label: '通道类型', width: 140, formatter: (_row, value) => channelTypeText(String(value || '')) },
  { prop: 'adapterType', label: '适配器', minWidth: 160, formatter: (_row, value) => adapterTypeText(String(value || '')) },
  { prop: 'billFetchModes', label: '账单获取', minWidth: 180, formatter: row => formatBillFetchModes(row.billFetchModes) },
  { prop: 'capabilities', label: '通道能力', minWidth: 260, formatter: row => formatCapabilities(row.capabilities, row.capabilitySummary) },
];

const rules: FormRules = {
  channelCode: [{ required: true, message: '请选择通道编码', trigger: 'change' }],
  channelName: [{ required: true, message: '请输入通道名称', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择通道类型', trigger: 'change' }],
  adapterType: [{ required: true, message: '请输入适配器类型', trigger: 'blur' }],
};

async function onEditorOpened(form: PaymentRecord, row?: PaymentRecord) {
  if (!row?.id) {
    form.capabilities = [];
    return;
  }
  const detail = await paymentChannelApi.detail(row.id);
  Object.assign(form, detail, {
    capabilities: Array.isArray(detail.capabilities) ? detail.capabilities : [],
    billFetchModes: Array.isArray(detail.billFetchModes) ? detail.billFetchModes : [],
  });
}

function toSavePayload(form: PaymentRecord): PaymentChannel {
  return {
    id: form.id as PaymentChannel['id'],
    channelCode: String(form.channelCode || ''),
    channelName: String(form.channelName || ''),
    channelType: String(form.channelType || ''),
    adapterType: String(form.adapterType || ''),
    gatewayBaseUrl: optionalString(form.gatewayBaseUrl),
    fieldTemplateJson: optionalString(form.fieldTemplateJson),
    capabilitySummary: optionalString(form.capabilitySummary),
    billFetchModes: toBillFetchModes(form.billFetchModes),
    capabilities: toCapabilities(form.capabilities),
    status: Number(form.status ?? 1),
  };
}

function toBillFetchModes(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
}

function toCapabilities(value: unknown): PaymentChannelCapability[] {
  if (!Array.isArray(value)) return [];
  return value.map(item => ({
    id: item.id,
    methodCode: item.methodCode,
    terminalType: item.terminalType,
    supportsRefund: item.supportsRefund,
    supportsQuery: item.supportsQuery,
    supportsClose: item.supportsClose,
    supportsBill: item.supportsBill,
    supportsReconcile: item.supportsReconcile,
    minAmount: item.minAmount,
    maxAmount: item.maxAmount,
    status: item.status ?? 1,
  }));
}

function optionalString(value: unknown) {
  return value === undefined || value === null || value === '' ? undefined : String(value);
}

function formatBillFetchModes(value: unknown) {
  const modes = Array.isArray(value) ? value.map(String) : [];
  if (!modes.length) return '-';
  return modes.map(fetchModeText).join('、');
}

function fetchModeText(value: string) {
  if (value === 'MANUAL') return '手动上传';
  if (value === 'FTP') return 'FTP 拉取';
  if (value === 'FTPS') return 'FTPS 拉取';
  if (value === 'HTTP') return 'HTTP 接口';
  return value || '-';
}

function formatCapabilities(value: unknown, fallback?: unknown) {
  const rows = Array.isArray(value) ? value as PaymentChannelCapability[] : [];
  if (!rows.length) return String(fallback || '-');
  return rows
    .map(item => `${item.methodName || item.methodCode}/${terminalText(item.terminalType)}`)
    .join('、');
}

function terminalText(value?: string) {
  if (value === 'WEB') return 'Web/PC';
  if (value === 'H5') return 'H5';
  if (value === 'APP') return 'App';
  if (value === 'MP') return '小程序';
  return value || '-';
}

function channelTypeText(value: string) {
  if (value === 'BUILTIN_VIRTUAL') return '内置虚拟通道';
  if (value === 'BUILTIN_OFFLINE') return '内置线下通道';
  if (value === 'AGGREGATOR') return '聚合支付机构';
  if (value === 'BANK') return '银行通道';
  if (value === 'DIRECT') return '直连通道';
  return value || '-';
}

function adapterTypeText(value: string) {
  if (value === 'MANGO_PAY') return '芒果支付适配器';
  if (value === 'OFFLINE_COLLECTION') return '线下收款适配器';
  if (value === 'ALLINPAY') return '通联支付适配器';
  if (value === 'FUIOU_PAY') return '富友支付适配器';
  if (value === 'HUAXIA_BANK') return '华夏银行适配器';
  if (value === 'WECHAT_PAY') return '微信支付适配器';
  if (value === 'ALIPAY') return '支付宝适配器';
  if (value === 'LIANLIAN_PAY') return '连连支付适配器';
  return value || '-';
}
</script>
