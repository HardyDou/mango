<template>
  <PaymentResourcePage
    title="支付方式"
    description="维护标准支付方式、收银台展示属性和支付物料；通道差异、商户配置和路由命中在签约通道与路由策略中维护。"
    :columns="columns"
    :api="paymentMethodApi"
    :defaults="defaults"
    :rules="rules"
    :to-save-payload="toSavePayload"
    :on-editor-opened="handleEditorOpened"
    :row-actions="buildRowActions"
    editor-width="860px"
  >
    <template #form="{ form }">
      <el-form-item label="方式编码" prop="methodCode">
        <el-input v-model="form.methodCode" placeholder="例如 PERSONAL_WECHAT_QR" />
      </el-form-item>
      <el-form-item label="方式名称" prop="methodName">
        <el-input v-model="form.methodName" placeholder="例如 微信扫码" />
      </el-form-item>
      <el-form-item label="三级分类" prop="categoryPath">
        <el-cascader
          v-model="form.categoryPath"
          :options="categoryOptions"
          :props="categoryCascaderProps"
          clearable
          filterable
          placeholder="选择 对私/对公 -> 支付工具 -> 交互形态"
          @change="applyCategoryPath(form)"
        />
      </el-form-item>
      <el-form-item label="终端范围" prop="terminalScopes">
        <el-checkbox-group v-model="form.terminalScopes">
          <el-checkbox-button label="WEB">Web</el-checkbox-button>
          <el-checkbox-button label="H5">H5</el-checkbox-button>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="支付物料" prop="paymentMaterialType">
        <el-select v-model="form.paymentMaterialType">
          <el-option label="二维码" value="QR" />
          <el-option label="跳转地址" value="REDIRECT_URL" />
          <el-option label="HTML 表单" value="HTML_FORM" />
          <el-option label="转账账户" value="TRANSFER_ACCOUNT" />
          <el-option label="H5 参数" value="H5_PARAM" />
        </el-select>
      </el-form-item>
      <el-form-item label="收银台分组" prop="cashierGroupCode">
        <el-select
          v-model="form.cashierGroupCode"
          filterable
          allow-create
          default-first-option
          placeholder="选择或输入展示分组编码"
          @change="applyCashierGroupName(form)"
        >
          <el-option label="微信支付" value="WECHAT_PAY" />
          <el-option label="支付宝" value="ALIPAY" />
          <el-option label="网银支付" value="EBANK" />
          <el-option label="线下转账" value="OFFLINE_TRANSFER" />
        </el-select>
      </el-form-item>
      <el-form-item label="分组名称" prop="cashierGroupName">
        <el-input v-model="form.cashierGroupName" placeholder="例如 网银支付" />
      </el-form-item>
      <el-form-item label="分组排序">
        <el-input-number v-model="form.cashierGroupSort" :min="0" :precision="0" />
      </el-form-item>
      <el-form-item label="方式图标">
        <MUpload
          v-model="form.iconFileId"
          value-type="id"
          display="thumbnail"
          :count="1"
          fmt="jpg,png,jpeg,webp,svg"
          biz-type="payment-method-icon"
          purpose="payment-method-icon"
          button-text="上传图标"
        />
      </el-form-item>
      <el-form-item label="需要银行列表">
        <el-switch v-model="form.requiresBankSelection" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="二维码可刷新">
        <el-switch v-model="form.requiresQrRefresh" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="产品说明">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="用于收银台展示该支付方式的产品说明" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" :precision="0" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </template>
  </PaymentResourcePage>
  <PaymentMethodRoutePanel ref="routePanelRef" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { CascaderProps, FormRules } from 'element-plus';
import { MUpload } from '@mango/file';
import PaymentResourcePage from '../../components/PaymentResourcePage.vue';
import type { PaymentRowAction } from '../../components/PaymentRowActions';
import PaymentMethodRoutePanel from './PaymentMethodRoutePanel.vue';
import {
  paymentMethodApi,
  type PaymentMethod,
  type PaymentMethodCategory,
  type PaymentTableColumn,
} from '../../api/payment';

interface CategoryOption {
  value: string;
  label: string;
  children?: CategoryOption[];
}

type MethodForm = Partial<PaymentMethod> & {
  categoryPath?: string[];
  terminalScopes?: string[];
};

const cashierGroupNames: Record<string, string> = {
  WECHAT_PAY: '微信支付',
  ALIPAY: '支付宝',
  EBANK: '网银支付',
  OFFLINE_TRANSFER: '线下转账',
};

const defaults: MethodForm = {
  status: 1,
  sort: 100,
  accountNature: 'PERSONAL',
  instrumentType: 'WECHAT',
  interactionType: 'QR_CODE',
  categoryPath: ['PERSONAL', 'WECHAT', 'QR_CODE'],
  terminalScope: 'WEB,H5',
  terminalScopes: ['WEB', 'H5'],
  paymentMaterialType: 'QR',
  cashierGroupCode: 'WECHAT_PAY',
  cashierGroupName: '微信支付',
  cashierGroupSort: 10,
  requiresBankSelection: 0,
  requiresQrRefresh: 0,
};

const categoryOptions = ref<CategoryOption[]>([]);
const categoryLabels = ref<Record<string, string>>({});
const routePanelRef = ref<InstanceType<typeof PaymentMethodRoutePanel>>();
const categoryCascaderProps: CascaderProps = {
  emitPath: true,
  checkStrictly: false,
};

const columns: PaymentTableColumn[] = [
  { prop: 'methodCode', label: '方式编码', minWidth: 170 },
  { prop: 'methodName', label: '方式名称', minWidth: 150 },
  { prop: 'accountNature', label: '一级', width: 100, formatter: (_row, value) => categoryLabel(String(value || '')) },
  { prop: 'instrumentType', label: '二级', width: 130, formatter: (_row, value) => categoryLabel(String(value || '')) },
  { prop: 'interactionType', label: '三级', width: 140, formatter: (_row, value) => categoryLabel(String(value || '')) },
  { prop: 'cashierGroupName', label: '收银台分组', width: 130 },
  { prop: 'terminalScope', label: '终端范围', width: 120 },
  { prop: 'paymentMaterialType', label: '支付物料', width: 130 },
  { prop: 'description', label: '产品说明', minWidth: 180 },
];

const rules: FormRules = {
  methodCode: [{ required: true, message: '请输入方式编码', trigger: 'blur' }],
  methodName: [{ required: true, message: '请输入方式名称', trigger: 'blur' }],
  categoryPath: [{ required: true, type: 'array', min: 3, message: '请选择完整三级分类', trigger: 'change' }],
  terminalScopes: [{ required: true, type: 'array', min: 1, message: '请选择终端范围', trigger: 'change' }],
  paymentMaterialType: [{ required: true, message: '请选择支付物料', trigger: 'change' }],
  cashierGroupCode: [{ required: true, message: '请选择或输入收银台分组编码', trigger: 'change' }],
  cashierGroupName: [{ required: true, message: '请输入收银台分组名称', trigger: 'blur' }],
};

onMounted(loadCategories);

async function loadCategories() {
  const categories = await paymentMethodApi.categories();
  categoryOptions.value = categories.map(toOption);
  const labels: Record<string, string> = {};
  collectLabels(categories, labels);
  categoryLabels.value = labels;
}

async function handleEditorOpened(form: MethodForm) {
  if (!categoryOptions.value.length) {
    await loadCategories();
  }
  const accountNature = form.accountNature || defaults.accountNature;
  const instrumentType = form.instrumentType || defaults.instrumentType;
  const interactionType = form.interactionType || defaults.interactionType;
  form.categoryPath = [accountNature, instrumentType, interactionType].filter(Boolean) as string[];
  form.terminalScopes = splitCsv(form.terminalScope || defaults.terminalScope);
  form.cashierGroupCode = form.cashierGroupCode || defaults.cashierGroupCode;
  form.cashierGroupName = form.cashierGroupName || cashierGroupNames[stringValue(form.cashierGroupCode)] || defaults.cashierGroupName;
  form.cashierGroupSort = form.cashierGroupSort ?? defaults.cashierGroupSort;
  form.requiresBankSelection = form.requiresBankSelection ?? 0;
  form.requiresQrRefresh = form.requiresQrRefresh ?? 0;
}

function applyCategoryPath(form: MethodForm) {
  const path = form.categoryPath || [];
  form.accountNature = path[0] || '';
  form.instrumentType = path[1] || '';
  form.interactionType = path[2] || '';
}

function applyCashierGroupName(form: MethodForm) {
  const groupName = cashierGroupNames[stringValue(form.cashierGroupCode)];
  if (groupName) {
    form.cashierGroupName = groupName;
  }
}

function toSavePayload(form: MethodForm) {
  applyCategoryPath(form);
  return {
    id: form.id,
    methodCode: stringValue(form.methodCode),
    methodName: stringValue(form.methodName),
    accountNature: stringValue(form.accountNature),
    instrumentType: stringValue(form.instrumentType),
    interactionType: stringValue(form.interactionType),
    terminalScope: (form.terminalScopes || []).join(','),
    paymentMaterialType: stringValue(form.paymentMaterialType),
    cashierGroupCode: stringValue(form.cashierGroupCode),
    cashierGroupName: stringValue(form.cashierGroupName),
    cashierGroupSort: optionalNumber(form.cashierGroupSort) ?? 0,
    iconFileId: form.iconFileId,
    requiresBankSelection: form.requiresBankSelection ?? 0,
    requiresQrRefresh: form.requiresQrRefresh ?? 0,
    description: optionalString(form.description),
    sort: optionalNumber(form.sort) ?? 0,
    status: optionalNumber(form.status) ?? 1,
  };
}

function toOption(category: PaymentMethodCategory): CategoryOption {
  const children = category.children?.map(toOption);
  return {
    value: category.categoryCode,
    label: category.categoryName,
    children: children && children.length ? children : undefined,
  };
}

function collectLabels(categories: PaymentMethodCategory[], labels: Record<string, string>) {
  for (const category of categories) {
    labels[category.categoryCode] = category.categoryName;
    if (category.children?.length) {
      collectLabels(category.children, labels);
    }
  }
}

function categoryLabel(value: string) {
  return categoryLabels.value[value] || value || '-';
}

function splitCsv(value?: string) {
  return String(value || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
}

function stringValue(value: unknown) {
  return String(value || '').trim();
}

function optionalString(value: unknown) {
  const text = stringValue(value);
  return text || undefined;
}

function optionalNumber(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return undefined;
  }
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}

function openRoutePanel(row: PaymentMethod) {
  routePanelRef.value?.open(row);
}

function buildRowActions(row: PaymentMethod): PaymentRowAction[] {
  return [{
    key: 'routes',
    label: '路由策略',
    type: 'primary',
    onClick: () => openRoutePanel(row),
  }];
}
</script>
