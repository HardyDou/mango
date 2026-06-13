<template>
  <PaymentResourcePage
    title="收银台"
    description="按应用维护收银台基础规则、允许企业主体、支付方式范围和结果跳转。"
    :columns="columns"
    :api="cashierConfigPageApi"
    :defaults="defaults"
    :rules="rules"
    :operation-width="260"
    :row-actions="cashierRowActions"
  >
    <template #form="{ form }">
      <section class="cashier-config-form__section">
        <h4>基础信息</h4>
        <el-form-item label="应用" prop="applicationId">
          <PaymentEntitySelect
            v-model="form.applicationId"
            :api="paymentApplicationApi"
            label-field="appName"
            description-field="appId"
            placeholder="请选择支付应用"
            @loaded="applicationLabels.setOptions"
          />
        </el-form-item>
        <el-form-item label="收银台名称" prop="cashierName">
          <el-input v-model="form.cashierName" placeholder="例如 订单中心 Web 收银台" />
        </el-form-item>
        <el-form-item label="默认收银台">
          <el-switch v-model="form.defaultCashier" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="企业主体" prop="enterpriseSubjectIds">
          <PaymentEntitySelect
            v-model="form.enterpriseSubjectIds"
            :api="paymentEnterpriseSubjectApi"
            label-field="subjectName"
            description-field="creditCodeMask"
            placeholder="请选择允许收款主体"
            multiple
            value-format="csv"
            @loaded="subjectLabels.setOptions"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </section>

      <section class="cashier-config-form__section">
        <h4>支付方式</h4>
        <el-form-item label="可见方式" prop="methodCodes">
          <PaymentEntityValueSelect
            v-model="form.methodCodes"
            :api="paymentMethodApi"
            value-field="methodCode"
            label-field="methodName"
            placeholder="请选择收银台可见支付方式"
          />
        </el-form-item>
        <el-form-item label="默认方式">
          <PaymentEntityValueSelect
            v-model="form.defaultMethodCode"
            :api="paymentMethodApi"
            value-field="methodCode"
            label-field="methodName"
            placeholder="请选择默认支付方式"
            single
          />
        </el-form-item>
        <el-form-item label="结果跳转">
          <el-input v-model="form.resultReturnUrl" placeholder="https://biz.example.com/pay/result" />
        </el-form-item>
      </section>
    </template>
  </PaymentResourcePage>

  <el-dialog
    v-model="cashierPreviewVisible"
    title="收银台预览"
    width="720px"
    class="payment-cashier-dialog"
    destroy-on-close
    append-to-body
  >
    <PaymentCashier
      v-if="cashierPreviewId"
      :cashier-config-id="cashierPreviewId"
      :business-order-id="cashierPreviewBusinessOrderId"
      embedded
    />
  </el-dialog>

  <el-dialog
    v-model="decorationVisible"
    title="收银台装修"
    width="640px"
    destroy-on-close
    append-to-body
  >
    <el-form :model="decorationForm" label-width="112px" class="payment-dialog-form cashier-decoration-form">
      <section class="cashier-config-form__section">
        <h4>展示主体</h4>
        <el-form-item label="收银台">
          <span class="payment-form-readonly">{{ decorationSource?.cashierName || '-' }}</span>
        </el-form-item>
        <el-form-item label="Logo 文件">
          <MUpload
            v-model="decorationForm.logoFileId"
            value-type="id"
            display="thumbnail"
            :count="1"
            fmt="jpg,png,jpeg,webp"
            biz-type="payment-cashier-logo"
            purpose="payment-cashier-logo"
            button-text="上传 Logo"
          />
        </el-form-item>
        <el-form-item label="辅助说明">
          <el-input v-model="decorationForm.displaySubtitle" maxlength="100" placeholder="例如 请确认订单金额后选择支付方式" />
        </el-form-item>
        <el-form-item label="帮助文案">
          <el-input v-model="decorationForm.helpText" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="例如 支付遇到问题请联系业务客服" />
        </el-form-item>
      </section>
    </el-form>
    <template #footer>
      <el-button @click="decorationVisible = false">取消</el-button>
      <el-button type="primary" :loading="decorationSaving" @click="saveDecoration">保存装修</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Brush, View } from '@element-plus/icons-vue';
import type { ApiId } from '@mango/api-schema';
import { MUpload } from '@mango/file';
import { ElMessage, type FormRules } from 'element-plus';
import PaymentResourcePage from '../../components/PaymentResourcePage.vue';
import PaymentEntitySelect from '../../components/PaymentEntitySelect.vue';
import PaymentEntityValueSelect from '../../components/PaymentEntityValueSelect.vue';
import { usePaymentEntityLabels } from '../../components/PaymentEntityLabels';
import PaymentCashier from '../../components/PaymentCashier.vue';
import type { PaymentRowAction } from '../../components/PaymentRowActions';
import {
  paymentApplicationApi,
  paymentCashierConfigApi,
  paymentEnterpriseSubjectApi,
  paymentMethodApi,
  type PageResult,
  type PaymentCashierConfig,
  type PaymentPageQuery,
  type PaymentTableColumn,
} from '../../api/payment';

interface DisplayConfig {
  logoFileId?: string;
  subtitle?: string;
  helpText?: string;
}

type CashierConfigForm = PaymentCashierConfig & {
  logoFileId?: string;
  displaySubtitle?: string;
  helpText?: string;
};

const applicationLabels = usePaymentEntityLabels();
const subjectLabels = usePaymentEntityLabels();
const cashierPreviewVisible = ref(false);
const cashierPreviewId = ref<ApiId>('');
const cashierPreviewBusinessOrderId = ref<ApiId>('');
const decorationVisible = ref(false);
const decorationSaving = ref(false);
const decorationSource = ref<CashierConfigForm>();
const decorationForm = ref<Pick<CashierConfigForm, 'logoFileId' | 'displaySubtitle' | 'helpText'>>({});

const defaults: CashierConfigForm = {
  applicationId: '',
  enterpriseSubjectIds: '',
  cashierName: '',
  defaultCashier: 0,
  status: 1,
};

const cashierConfigPageApi = {
  page: async (params?: PaymentPageQuery): Promise<PageResult<Record<string, unknown>>> => {
    const result = await paymentCashierConfigApi.page(params);
    return { ...result, list: result.list.map(toForm) };
  },
  detail: async (id: ApiId): Promise<Record<string, unknown>> => toForm(await paymentCashierConfigApi.detail(id)),
  create: (data: Record<string, unknown>): Promise<ApiId> => paymentCashierConfigApi.create(toPayload(data as CashierConfigForm)),
  update: (data: Record<string, unknown>): Promise<boolean> => paymentCashierConfigApi.update(toPayload(data as CashierConfigForm)),
  remove: (id: ApiId): Promise<boolean> => paymentCashierConfigApi.remove(id),
};

const columns: PaymentTableColumn[] = [
  { prop: 'cashierName', label: '收银台名称', minWidth: 180 },
  { prop: 'applicationName', label: '应用', minWidth: 180, formatter: (row, value) => String(value || applicationLabels.labelOf(row.applicationId)) },
  { prop: 'defaultCashier', label: '默认', width: 90, formatter: (_row, value) => Number(value) === 1 ? '是' : '否' },
  { prop: 'enterpriseSubjectNames', label: '企业主体', minWidth: 220, formatter: (row, value) => String(value || labelsOfCsv(row.enterpriseSubjectIds)) },
  { prop: 'methodNames', label: '支付方式', minWidth: 260, variant: 'tags', formatter: row => String(row.methodNames || row.methodCodes || '') },
];

const rules: FormRules = {
  applicationId: [{ required: true, message: '请选择支付应用', trigger: 'change' }],
  enterpriseSubjectIds: [{ required: true, message: '请选择企业主体', trigger: 'change' }],
  methodCodes: [{ required: true, message: '请选择可见支付方式', trigger: 'change' }],
  cashierName: [{ required: true, message: '请输入收银台名称', trigger: 'blur' }],
};

function openCashier(row: Record<string, unknown>) {
  const id = String(row.id || '');
  if (!id) {
    return;
  }
  cashierPreviewId.value = id;
  cashierPreviewBusinessOrderId.value = '';
  cashierPreviewVisible.value = true;
}

function cashierRowActions(row: Record<string, unknown>): PaymentRowAction[] {
  return [
    {
      key: 'decorate',
      label: '装修',
      type: 'success',
      icon: Brush,
      onClick: () => openDecoration(row),
    },
    {
      key: 'preview',
      label: '预览',
      type: 'primary',
      icon: View,
      onClick: () => openCashier(row),
    },
  ];
}

function openDecoration(row: Record<string, unknown>) {
  const source = toForm(row as PaymentCashierConfig);
  decorationSource.value = source;
  decorationForm.value = {
    logoFileId: source.logoFileId,
    displaySubtitle: source.displaySubtitle,
    helpText: source.helpText,
  };
  decorationVisible.value = true;
}

async function saveDecoration() {
  if (!decorationSource.value?.id) {
    return;
  }
  decorationSaving.value = true;
  try {
    await paymentCashierConfigApi.update(toPayload({
      ...decorationSource.value,
      ...decorationForm.value,
    }));
    ElMessage.success('收银台装修已保存');
    decorationVisible.value = false;
  } finally {
    decorationSaving.value = false;
  }
}

function toForm(config: PaymentCashierConfig): CashierConfigForm {
  const display = parseConfig<DisplayConfig>(config.displayConfig);
  return {
    ...config,
    logoFileId: display.logoFileId,
    displaySubtitle: display.subtitle,
    helpText: display.helpText,
  };
}

function toPayload(form: CashierConfigForm): PaymentCashierConfig {
  return {
    id: form.id,
    applicationId: form.applicationId,
    cashierName: form.cashierName,
    defaultCashier: Number(form.defaultCashier || 0),
    enterpriseSubjectIds: form.enterpriseSubjectIds,
    methodCodes: trimToUndefined(form.methodCodes),
    defaultMethodCode: trimToUndefined(form.defaultMethodCode),
    methodDisplayOrder: trimToUndefined(form.methodCodes),
    resultReturnUrl: trimToUndefined(form.resultReturnUrl),
    displayConfig: stringifyConfig<DisplayConfig>({
      logoFileId: trimToUndefined(form.logoFileId),
      subtitle: trimToUndefined(form.displaySubtitle),
      helpText: trimToUndefined(form.helpText),
    }),
    status: form.status,
  };
}

function parseConfig<T extends Record<string, unknown>>(value: string | undefined): Partial<T> {
  if (!value) {
    return {};
  }
  try {
    const parsed = JSON.parse(value);
    return typeof parsed === 'object' && parsed !== null ? parsed : {};
  } catch {
    return {};
  }
}

function stringifyConfig<T extends Record<string, unknown>>(value: T): string | undefined {
  const normalized = Object.fromEntries(Object.entries(value).filter(([, entry]) => entry !== undefined && entry !== ''));
  return Object.keys(normalized).length > 0 ? JSON.stringify(normalized) : undefined;
}

function trimToUndefined(value: unknown): string | undefined {
  const text = String(value || '').trim();
  return text || undefined;
}

function labelsOfCsv(value: unknown) {
  return String(value || '')
    .split(',')
    .map(item => subjectLabels.labelOf(item.trim()))
    .filter(label => label !== '-')
    .join(',') || '-';
}
</script>

<style scoped>
.cashier-config-form__section {
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 16px;
  padding-bottom: 10px;
}

.cashier-config-form__section:last-child {
  border-bottom: 0;
}

.cashier-config-form__section h4 {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 650;
  letter-spacing: 0;
  margin: 0 0 12px;
}

</style>
