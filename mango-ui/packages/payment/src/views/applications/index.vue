<template>
  <PaymentResourcePage
    title="应用管理"
    description="维护业务系统接入支付平台的应用身份和接入安全配置。订单通知和返回地址由业务系统创建订单时提供。"
    keyword-placeholder="应用名称 / AppId"
    :columns="columns"
    :api="paymentApplicationApi"
    :defaults="defaults"
    :rules="rules"
    :delete-confirm-message="deleteConfirmMessage"
    :on-saved="handleApplicationSaved"
  >
    <template #form="{ form }">
      <section class="payment-application-form__section">
        <h4>基础信息</h4>
        <el-form-item v-if="form.appId" label="AppId">
          <el-input v-model="form.appId" disabled />
        </el-form-item>
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="form.appName" placeholder="例如 保函业务系统" />
        </el-form-item>
        <el-form-item label="示例应用">
          <el-switch v-model="form.demoApp" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </section>

      <section class="payment-application-form__section">
        <h4>请求安全</h4>
        <el-form-item label="报文加密">
          <el-switch v-model="form.payloadEncryptEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="form.payloadEncryptEnabled === 1" label="签名算法" prop="signAlgorithm">
          <el-select v-model="form.signAlgorithm">
            <el-option label="HMAC-SHA256" value="HMAC_SHA256" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP 白名单">
          <el-switch v-model="form.ipWhitelistEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="form.ipWhitelistEnabled === 1" label="允许来源" prop="ipWhitelist">
          <el-input v-model="form.ipWhitelist" type="textarea" :rows="3" placeholder="多个 IP 用逗号或换行分隔" />
        </el-form-item>
      </section>

      <section class="payment-application-form__section">
        <h4>通知策略</h4>
        <el-form-item label="通知重试策略">
          <el-input v-model="form.notifyRetryPolicy" placeholder="例如 1m,5m,15m,1h" />
        </el-form-item>
      </section>

    </template>
  </PaymentResourcePage>

  <el-dialog
    v-model="secretDialogVisible"
    title="应用密钥"
    width="620px"
    append-to-body
  >
    <el-alert
      type="warning"
      show-icon
      :closable="false"
      title="应用密钥仅展示一次，请交给业务系统后妥善保存。"
    />
    <el-descriptions :column="1" border class="payment-secret-dialog__content">
      <el-descriptions-item label="AppId">{{ savedSecret.appId }}</el-descriptions-item>
      <el-descriptions-item label="AppSecret">
        <el-input :model-value="savedSecret.appSecret" readonly />
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button type="primary" @click="secretDialogVisible = false">我已保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { FormRules } from 'element-plus';
import PaymentResourcePage from '../../components/PaymentResourcePage.vue';
import {
  paymentApplicationApi,
  type PaymentApplicationSaveResult,
  type PaymentTableColumn,
} from '../../api/payment';

const defaults = {
  status: 1,
  demoApp: 0,
  signAlgorithm: 'HMAC_SHA256',
  ipWhitelistEnabled: 0,
  payloadEncryptEnabled: 0,
};

const columns: PaymentTableColumn[] = [
  { prop: 'appId', label: 'AppId', minWidth: 180 },
  { prop: 'appName', label: '应用名称', minWidth: 180 },
  { prop: 'secretConfigured', label: '密钥', width: 90, formatter: (_row, value) => Number(value) === 1 ? '已配置' : '未配置' },
  { prop: 'payloadEncryptEnabled', label: '报文加密', width: 100, formatter: (_row, value) => Number(value) === 1 ? '开启' : '关闭' },
  { prop: 'ipWhitelistEnabled', label: 'IP 白名单', width: 110, formatter: (_row, value) => Number(value) === 1 ? '开启' : '关闭' },
  { prop: 'notifyRetryPolicy', label: '通知重试策略', minWidth: 160 },
  { prop: 'demoApp', label: '示例', width: 80, formatter: (_row, value) => Number(value) === 1 ? '是' : '否' },
];

const rules: FormRules = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  ipWhitelist: [{ required: true, message: '请输入允许来源', trigger: 'blur' }],
  signAlgorithm: [{ required: true, message: '请选择签名算法', trigger: 'change' }],
};

const secretDialogVisible = ref(false);
const savedSecret = reactive({
  appId: '',
  appSecret: '',
});

function handleApplicationSaved(result: unknown) {
  const saveResult = result as PaymentApplicationSaveResult | undefined;
  if (saveResult?.secretGenerated === 1 && saveResult.appSecret) {
    savedSecret.appId = saveResult.appId;
    savedSecret.appSecret = saveResult.appSecret;
    secretDialogVisible.value = true;
  }
}

function deleteConfirmMessage(row: Record<string, unknown>) {
  const name = String(row.appName || row.appId || row.id || '该应用');
  return `确认删除 ${name}？存在收银台配置、业务订单、支付订单、退款、流水、通知、异常、对账差异等关联数据时后端会拒绝删除，请先清理关联数据。`;
}

</script>

<style scoped>
.payment-application-form__section {
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 16px;
  padding-bottom: 10px;
}

.payment-application-form__section:last-child {
  border-bottom: 0;
}

.payment-application-form__section h4 {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 650;
  letter-spacing: 0;
  margin: 0 0 12px;
}

.payment-secret-dialog__content {
  margin-top: 16px;
}

</style>
