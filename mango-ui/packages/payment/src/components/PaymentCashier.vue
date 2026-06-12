<template>
  <div class="cashier-page" :class="{ 'is-embedded': props.embedded }">
    <section v-loading="loading" class="cashier-shell">
      <header class="cashier-header">
        <div class="cashier-brand">
          <img v-if="logoUrl" class="cashier-brand__logo" :src="logoUrl" alt="" />
          <div v-else class="cashier-brand__mark">{{ brandInitial }}</div>
          <div class="cashier-brand__text">
            <strong>{{ session?.display?.title || session?.cashierName || '收银台' }}</strong>
            <span>{{ session?.display?.subtitle || '请确认订单金额后选择支付方式' }}</span>
          </div>
        </div>
        <div class="cashier-order-no">
          <span>业务订单号</span>
          <strong>{{ displayOrder.bizOrderNo }}</strong>
        </div>
      </header>

      <section class="cashier-order">
        <div class="cashier-order__info">
          <div>
            <span>收款方</span>
            <strong>{{ displaySubjectName }}</strong>
          </div>
          <div>
            <span>商品名称</span>
            <strong>{{ displayOrder.orderTitle }}</strong>
          </div>
        </div>
        <div class="cashier-order__amount">
          <span>应付金额</span>
          <strong>{{ amountText }}</strong>
          <small>{{ expireText }}</small>
        </div>
      </section>

      <main class="cashier-main">
        <section v-if="!methodGroups.length || showMethodGroupTabs" class="cashier-methods">
          <el-empty v-if="!methodGroups.length" description="当前收银台暂无可用支付方式" />

          <el-tabs
            v-else-if="showMethodGroupTabs"
            :model-value="selectedGroupCode"
            class="payment-method-tabs"
            @update:model-value="selectMethodGroupByCode"
          >
            <el-tab-pane
              v-for="group in methodGroups"
              :key="group.code"
              :disabled="paying"
              :name="group.code"
            >
              <template #label>
                <span class="method-tab-label">
                  <span class="method-tab-label__icon">
                    <img v-if="methodLogoUrl(primaryMethod(group))" :src="methodLogoUrl(primaryMethod(group))" alt="" />
                    <span v-else>{{ methodIcon(primaryMethod(group)) }}</span>
                  </span>
                  <span>{{ displayGroupName(group) }}</span>
                </span>
              </template>
            </el-tab-pane>
          </el-tabs>
        </section>

        <section class="cashier-workspace">
          <el-alert
            v-if="session?.order?.businessOrderId && !payableOrder"
            :title="orderNotPayableTitle"
            type="warning"
            show-icon
            :closable="false"
            class="cashier-alert"
          />

          <div v-loading="paying" class="material-panel">
            <el-empty v-if="!selectedMethod && selectedGroupCode !== 'EBANK'" description="请选择支付方式" />

            <template v-else-if="terminalResultVisible">
              <el-result :icon="resultIcon" :title="resultTitle" :sub-title="payResult.payOrderNo">
                <template #extra>
                  <el-tag :type="resultTagType" effect="light">{{ payResult.status }}</el-tag>
                </template>
              </el-result>
            </template>

            <template v-else-if="bankWorkspaceVisible">
              <el-form
                ref="bankPaymentFormRef"
                :model="bankPaymentForm"
                :rules="bankPaymentRules"
                label-width="108px"
                class="bank-form"
              >
                <div v-if="showBankMethodTabs" class="ebank-type-tabs">
                  <button
                    v-for="method in bankMethods"
                    :key="method.methodCode"
                    type="button"
                    class="ebank-type-tab"
                    :class="{ active: selectedMethodCode === method.methodCode }"
                    @click="selectBankMethod(method.methodCode)"
                  >
                    {{ method.methodName }}
                  </button>
                </div>

                <el-form-item v-if="selectedMethod" label="选择银行" prop="bankCode">
                  <div class="bank-banner-grid">
                    <button
                      v-for="bank in bankBannerOptions"
                      :key="bank.code"
                      type="button"
                      class="bank-banner"
                      :class="{ active: bankPaymentForm.bankCode === bank.code }"
                      @click="selectBankBanner(bank)"
                    >
                      <strong>{{ bank.shortName }}</strong>
                      <span>{{ bank.name }}</span>
                    </button>
                  </div>
                </el-form-item>
                <template v-if="selectedMethod && bankPaymentForm.bankCode">
                  <el-form-item :label="bankAccountLabel" prop="accountNo">
                    <el-input v-model="bankPaymentForm.accountNo" placeholder="请输入账号或卡号" clearable />
                  </el-form-item>
                  <el-form-item label="付款户名" prop="payerName">
                    <el-input v-model="bankPaymentForm.payerName" placeholder="请输入付款户名" clearable />
                  </el-form-item>
                  <el-form-item v-if="selectedMethod?.accountNature === 'CORPORATE'" label="企业授权">
                    <el-checkbox v-model="bankPaymentForm.enterpriseAuthorized">
                      已完成企业网银授权准备
                    </el-checkbox>
                  </el-form-item>
                  <el-form-item>
                    <el-button
                      type="primary"
                      size="large"
                      :loading="paying"
                      :disabled="!payableOrder"
                      @click="submitBankPayment"
                    >
                      进入{{ selectedMethod?.methodName || '网银' }}支付
                    </el-button>
                  </el-form-item>
                </template>
              </el-form>
            </template>

            <template v-else-if="previewMaterialVisible">
              <template v-if="previewMaterialType === 'TRANSFER_ACCOUNT'">
                <div class="transfer-material">
                  <div class="transfer-check is-preview">
                    <div class="transfer-check__header">
                      <div class="transfer-check__title">
                        <span>转账通知单</span>
                        <small>{{ transferDeadlineText }}</small>
                      </div>
                      <div class="transfer-check__amount">
                        <span>转账金额</span>
                        <strong>{{ resultAmountText }}</strong>
                      </div>
                    </div>

                    <p class="transfer-check__intro">
                      请按本通知单信息完成线下转账，转账金额、目标账户和转账备注需与下列内容一致。
                    </p>

                    <div class="transfer-check__body">
                      <div class="transfer-check__payee">
                        <span>目标户主</span>
                        <strong>{{ previewTransferMaterial.accountName }}</strong>
                      </div>
                      <div class="transfer-check__payee">
                        <span>目标账号</span>
                        <strong>{{ previewTransferMaterial.accountNo }}</strong>
                      </div>
                      <div class="transfer-check__payee">
                        <span>目标开户行</span>
                        <strong>{{ previewTransferMaterial.bankName }}</strong>
                      </div>
                    </div>

                    <div class="transfer-check__remark">
                      <span>转账备注</span>
                      <strong>{{ previewTransferMaterial.transferRemark }}</strong>
                    </div>

                    <div class="transfer-check__footer">
                      请完成转账后回传转账凭证。
                    </div>
                  </div>
                </div>
              </template>

              <template v-else-if="previewMaterialType === 'QR'">
                <div class="qr-material is-preview" :class="qrReceiptClass">
                  <div class="qr-receipt__brand">
                    <span class="qr-receipt__brand-icon">{{ qrReceiptIcon }}</span>
                    <strong>{{ qrReceiptBrandName }}</strong>
                  </div>

                  <div class="qr-receipt__amount">
                    <span>应付金额</span>
                    <strong>{{ resultAmountText }}</strong>
                  </div>

                  <div class="qr-receipt__scan-frame">
                    <span class="qr-corner is-left-top" />
                    <span class="qr-corner is-right-top" />
                    <span class="qr-corner is-left-bottom" />
                    <span class="qr-corner is-right-bottom" />
                    <div class="qr-code-box">
                      <img v-if="qrImageUrl" class="qr-image" :src="qrImageUrl" alt="支付二维码" />
                      <el-empty v-else description="二维码生成中" />
                    </div>
                  </div>

                  <strong class="qr-receipt__tip">扫描二维码付款</strong>
                  <span class="qr-receipt__welcome">{{ qrReceiptWelcomeText }}</span>
                  <div class="qr-receipt__order">预览模式不生成支付订单</div>
                </div>
              </template>

              <template v-else>
                <div class="redirect-material">
                  <strong>{{ selectedMethod?.methodName || '支付方式' }}</strong>
                  <span>预览模式仅展示收银台布局，真实支付需从业务订单发起。</span>
                </div>
              </template>
            </template>

            <template v-else-if="payResult">
              <template v-if="selectedMethodIsOffline && payMaterial?.materialType !== 'TRANSFER_ACCOUNT'">
                <el-alert
                  title="线下转账通道未返回收款账户信息"
                  description="当前支付请求没有拿到线下转账所需的收款户名、账号和开户行，请检查支付方法路由是否绑定到线下收款通道。"
                  type="error"
                  show-icon
                  :closable="false"
                  class="cashier-alert"
                />
              </template>

              <template v-else-if="payMaterial?.materialType === 'TRANSFER_ACCOUNT'">
                <div class="transfer-material">
                  <div class="transfer-check">
                    <div class="transfer-check__header">
                      <div class="transfer-check__title">
                        <span>转账通知单</span>
                        <small>{{ transferDeadlineText }}</small>
                      </div>
                      <div class="transfer-check__amount">
                        <span>转账金额</span>
                        <strong>{{ resultAmountText }}</strong>
                      </div>
                    </div>

                    <p class="transfer-check__intro">
                      请按本通知单信息完成线下转账，转账金额、目标账户和转账备注需与下列内容一致。
                    </p>

                    <div class="transfer-check__body">
                      <div class="transfer-check__payee">
                        <span>目标户主</span>
                        <strong>{{ payMaterial.accountName || '-' }}</strong>
                        <el-button link type="primary" :disabled="!payMaterial.accountName" @click="copyText(payMaterial.accountName, '目标户主')">复制</el-button>
                      </div>
                      <div class="transfer-check__payee">
                        <span>目标账号</span>
                        <strong>{{ payMaterial.accountNo || payMaterial.accountNoMask || '-' }}</strong>
                        <el-button link type="primary" :disabled="!payMaterial.accountNo" @click="copyText(payMaterial.accountNo, '目标账号')">复制</el-button>
                      </div>
                      <div class="transfer-check__payee">
                        <span>目标开户行</span>
                        <strong>{{ payMaterial.bankName || '-' }}</strong>
                        <el-button link type="primary" :disabled="!payMaterial.bankName" @click="copyText(payMaterial.bankName, '目标开户行')">复制</el-button>
                      </div>
                    </div>

                    <div class="transfer-check__remark">
                      <span>转账备注</span>
                      <strong>{{ payMaterial.transferRemark || '-' }}</strong>
                      <el-button link type="primary" :disabled="!payMaterial.transferRemark" @click="copyText(payMaterial.transferRemark, '转账备注')">复制</el-button>
                    </div>

                    <div class="transfer-check__footer">
                      请完成转账后回传转账凭证。
                    </div>
                  </div>
                  <p v-if="payMaterial.transferInstruction" class="material-note">{{ payMaterial.transferInstruction }}</p>

                  <div v-if="offlineVoucherVisible && !offlineVoucherFormVisible" class="offline-action">
                    <el-button type="primary" size="large" @click="showOfflineVoucherForm">
                      已完成转账，回传转账凭证
                    </el-button>
                  </div>

                  <el-button
                    v-if="mangoPayActionVisible"
                    type="primary"
                    size="large"
                    class="material-primary-action"
                    :loading="mangoPaying"
                    @click="submitMangoPayVirtualPayment"
                  >
                    立即支付
                  </el-button>
                </div>
              </template>

              <template v-else-if="payMaterial?.materialType === 'QR'">
                <div class="qr-material" :class="qrReceiptClass">
                  <div class="qr-receipt__brand">
                    <span class="qr-receipt__brand-icon">{{ qrReceiptIcon }}</span>
                    <strong>{{ qrReceiptBrandName }}</strong>
                  </div>

                  <div class="qr-receipt__amount">
                    <span>应付金额</span>
                    <strong>{{ resultAmountText }}</strong>
                  </div>

                  <div class="qr-receipt__scan-frame">
                    <span class="qr-corner is-left-top" />
                    <span class="qr-corner is-right-top" />
                    <span class="qr-corner is-left-bottom" />
                    <span class="qr-corner is-right-bottom" />
                    <div class="qr-code-box">
                      <img v-if="qrImageUrl" class="qr-image" :src="qrImageUrl" alt="支付二维码" />
                      <el-empty v-else description="二维码生成中" />
                    </div>
                  </div>

                  <strong class="qr-receipt__tip">扫描二维码付款</strong>
                  <span class="qr-receipt__welcome">{{ qrReceiptWelcomeText }}</span>

                  <div v-if="!previewMode" class="qr-receipt__actions">
                    <el-button
                      type="primary"
                      :loading="qrManualChecking"
                      :disabled="qrManualLocked"
                      @click="confirmQrPaymentCompleted"
                    >
                      {{ qrManualButtonText }}
                    </el-button>
                  </div>

                  <div class="qr-receipt__order">
                    订单号：{{ payResult.payOrderNo }}
                  </div>
                </div>
              </template>

              <template v-else-if="payMaterial?.materialType === 'HTML_FORM'">
                <div class="redirect-material">
                  <strong>网银支付请求已生成</strong>
                  <span>请进入银行页面完成授权付款，完成后回到本页等待结果确认。</span>
                  <div class="redirect-material__actions">
                    <el-button type="primary" size="large" :disabled="!payMaterial.htmlForm" @click="submitHtmlForm">
                      打开网银支付
                    </el-button>
                    <el-button link type="primary" @click="refreshCurrentPayResult">查询支付结果</el-button>
                  </div>
                  <el-button
                    v-if="mangoPayActionVisible"
                    type="primary"
                    size="large"
                    :loading="mangoPaying"
                    @click="submitMangoPayVirtualPayment"
                  >
                    立即支付
                  </el-button>
                </div>
              </template>

              <template v-else-if="payMaterial?.materialType === 'H5_PARAM'">
                <div class="redirect-material">
                  <strong>账户支付请求已生成</strong>
                  <span>请跳转到支付页面完成付款，完成后回到本页等待结果确认。</span>
                  <div class="redirect-material__actions">
                    <el-button type="primary" size="large" :disabled="!payMaterial.redirectUrl" @click="openRedirectUrl">
                      打开支付页面
                    </el-button>
                    <el-button link type="primary" @click="refreshCurrentPayResult">查询支付结果</el-button>
                  </div>
                  <el-button
                    v-if="mangoPayActionVisible"
                    type="primary"
                    size="large"
                    :loading="mangoPaying"
                    @click="submitMangoPayVirtualPayment"
                  >
                    立即支付
                  </el-button>
                </div>
              </template>

              <template v-else>
                <div class="redirect-material">
                  <strong>支付请求已提交</strong>
                  <span>支付结果可能延迟返回，请在本页等待系统确认。</span>
                  <el-button link type="primary" @click="refreshCurrentPayResult">查询支付结果</el-button>
                  <el-button
                    v-if="mangoPayActionVisible"
                    type="primary"
                    size="large"
                    :loading="mangoPaying"
                    @click="submitMangoPayVirtualPayment"
                  >
                    立即支付
                  </el-button>
                </div>
              </template>
            </template>
          </div>
        </section>
      </main>

      <footer v-if="session?.display?.helpText" class="cashier-help">{{ session.display.helpText }}</footer>
    </section>

    <el-dialog
      v-model="offlineVoucherFormVisible"
      title="提交转账凭证"
      width="520px"
      append-to-body
      destroy-on-close
      class="offline-voucher-dialog"
    >
      <el-form
        ref="offlineVoucherFormRef"
        :model="offlineVoucherForm"
        :rules="offlineVoucherRules"
        label-width="112px"
        class="offline-voucher-form"
      >
        <el-form-item label="实际转账金额（元）" prop="transferAmount">
          <el-input-number
            v-model="offlineVoucherForm.transferAmount"
            :min="0.01"
            :precision="2"
            :step="1"
            :controls="false"
            class="offline-voucher-form__amount"
          />
        </el-form-item>
        <el-form-item label="转账凭证" prop="voucherFileIds">
          <MUpload
            v-model="offlineVoucherForm.voucherFileIds"
            value-type="id"
            display="list"
            :count="6"
            purpose="payment-offline-transfer-voucher"
            biz-type="payment-offline-collection"
            button-text="上传凭证"
          />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input
            v-model="offlineVoucherForm.submitRemark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="填写转账流水号、付款户名或补充说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offlineVoucherFormVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submittingOfflineVoucher"
          @click="submitOfflineVoucher"
        >
          提交
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="qrResultDialogVisible"
      title="支付结果"
      width="420px"
      append-to-body
      destroy-on-close
      class="qr-result-dialog"
    >
      <el-result :icon="resultIcon" :title="qrManualResultTitle" :sub-title="qrManualResultDescription">
        <template #extra>
          <el-tag :type="resultTagType" effect="light">{{ payResult?.status || '-' }}</el-tag>
        </template>
      </el-result>
      <template #footer>
        <el-button type="primary" @click="qrResultDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { fileApi, MUpload } from '@mango/file';
import type { ApiId } from '@mango/api-schema';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import QRCode from 'qrcode';
import {
  paymentCashierApi,
  type PaymentCashierMethod,
  type PaymentCashierPayResult,
  type PaymentCashierSession,
} from '../api/payment';

interface MethodGroup {
  code: string;
  name: string;
  sort: number;
  methods: PaymentCashierMethod[];
}

interface BankPaymentForm {
  bankCode: string;
  bankName: string;
  accountNo: string;
  payerName: string;
  enterpriseAuthorized: boolean;
}

interface BankBannerOption {
  code: string;
  name: string;
  shortName: string;
}

const props = withDefaults(defineProps<{
  cashierConfigId?: ApiId | string;
  businessOrderId?: ApiId | string;
  embedded?: boolean;
}>(), {
  cashierConfigId: '',
  businessOrderId: '',
  embedded: false,
});

const emit = defineEmits<{
  success: [result: PaymentCashierPayResult];
  close: [];
}>();

const loading = ref(false);
const paying = ref(false);
const mangoPaying = ref(false);
const qrManualChecking = ref(false);
const qrResultDialogVisible = ref(false);
const submittingOfflineVoucher = ref(false);
const polling = ref(false);
const session = ref<PaymentCashierSession>();
const selectedGroupCode = ref('');
const selectedMethodCode = ref('');
const payResult = ref<PaymentCashierPayResult>();
const qrImageUrl = ref('');
const pollVersion = ref(0);
const resultRefreshCountdown = ref(5);
const qrManualLockSeconds = ref(0);
const completionHandled = ref(false);
const expireNow = ref(Date.now());
const serverTimeDelta = ref(0);
const offlineVoucherFormVisible = ref(false);
const offlineVoucherFormRef = ref<FormInstance>();
const bankPaymentFormRef = ref<FormInstance>();
const offlineVoucherForm = ref<{
  transferAmount: number;
  voucherFileIds: string[];
  submitRemark: string;
}>({
  transferAmount: 0,
  voucherFileIds: [],
  submitRemark: '',
});
const bankPaymentForm = ref<BankPaymentForm>({
  bankCode: '',
  bankName: '',
  accountNo: '',
  payerName: '',
  enterpriseAuthorized: false,
});

const bankBannerOptions: BankBannerOption[] = [
  { code: 'ICBC', name: '中国工商银行', shortName: '工行' },
  { code: 'ABC', name: '中国农业银行', shortName: '农行' },
  { code: 'BOC', name: '中国银行', shortName: '中行' },
  { code: 'CCB', name: '中国建设银行', shortName: '建行' },
  { code: 'CMB', name: '招商银行', shortName: '招行' },
  { code: 'BOCOM', name: '交通银行', shortName: '交行' },
  { code: 'CIB', name: '兴业银行', shortName: '兴业' },
  { code: 'SPDB', name: '浦发银行', shortName: '浦发' },
];

let expireTimer: number | undefined;

const resolvedCashierConfigId = computed(() => String(props.cashierConfigId || ''));
const resolvedBusinessOrderId = computed(() => String(props.businessOrderId || ''));
const logoUrl = computed(() => session.value?.display?.logoFileId ? fileApi.downloadUrl(session.value.display.logoFileId) : '');
const brandInitial = computed(() => (session.value?.display?.title || session.value?.cashierName || '收').slice(0, 1));
const amountText = computed(() => formatMoney(displayOrder.value.amount));
const resultAmountText = computed(() => previewMode.value
  ? formatMoney(displayOrder.value.amount)
  : formatMoney(payResult.value?.amount ?? session.value?.order?.amount ?? displayOrder.value.amount));
const mangoPayOrderTitle = computed(() => session.value?.order?.orderTitle || session.value?.order?.bizOrderNo || '订单支付');
const serverNow = computed(() => expireNow.value + serverTimeDelta.value);
const orderExpired = computed(() => {
  const expireTime = session.value?.order?.expireTime;
  return Boolean(expireTime && new Date(expireTime).getTime() <= serverNow.value);
});
const expireText = computed(() => {
  if (previewMode.value) return '预览模式';
  const expireTime = session.value?.order?.expireTime;
  if (!expireTime) return '等待业务订单';
  if (orderExpired.value) return '订单已超时';
  return `剩余支付时间 ${formatExpire(expireTime, serverNow.value)}`;
});
const methodGroups = computed<MethodGroup[]>(() => {
  const groups: MethodGroup[] = [];
  const groupIndex = new Map<string, MethodGroup>();
  for (const method of session.value?.methods || []) {
    const code = method.categoryCode || 'DEFAULT';
    let group = groupIndex.get(code);
    if (!group) {
      group = {
        code,
        name: method.categoryName || '其他支付',
        sort: method.categorySort ?? 0,
        methods: [],
      };
      groupIndex.set(code, group);
      groups.push(group);
    }
    group.methods.push(method);
  }
  return groups.sort((current, next) => current.sort - next.sort);
});
const selectedMethod = computed(() => (session.value?.methods || []).find(method => method.methodCode === selectedMethodCode.value));
const selectedMethodGroup = computed(() => methodGroups.value.find(group => group.code === selectedGroupCode.value)
  || methodGroups.value.find(group => group.methods.some(method => method.methodCode === selectedMethodCode.value)));
const bankGroup = computed(() => methodGroups.value.find(group => group.code === 'EBANK'));
const bankMethods = computed(() => bankGroup.value?.methods || []);
const showMethodGroupTabs = computed(() => methodGroups.value.length > 1);
const showBankMethodTabs = computed(() => bankMethods.value.length > 1);
const payMaterial = computed(() => payResult.value?.material);
const payableOrder = computed(() => (session.value?.order?.status === 'TO_PAY' || session.value?.order?.status === 'PAYING') && !orderExpired.value);
const terminalPayResult = computed(() => payResult.value?.status === 'SUCCESS' || payResult.value?.status === 'FAILED' || payResult.value?.status === 'CLOSED');
const terminalResultVisible = computed(() => Boolean(payResult.value && terminalPayResult.value));
const previewMode = computed(() => Boolean(session.value && !resolvedBusinessOrderId.value));
const mangoPayActionVisible = computed(() => payResult.value?.status === 'PAYING' && payResult.value.channelCode === 'MANGO_PAY');
const offlineVoucherVisible = computed(() => payResult.value?.status === 'PAYING' && payResult.value.channelCode === 'OFFLINE_COLLECTION');
const qrReceiptProvider = computed(() => {
  if (selectedMethod.value?.instrumentType === 'WECHAT') return 'wechat';
  if (selectedMethod.value?.instrumentType === 'ALIPAY') return 'alipay';
  return 'default';
});
const qrReceiptClass = computed(() => `is-${qrReceiptProvider.value}`);
const qrReceiptBrandName = computed(() => {
  if (qrReceiptProvider.value === 'wechat') return '微信支付';
  if (qrReceiptProvider.value === 'alipay') return '支付宝支付';
  return selectedMethod.value?.methodName || '扫码支付';
});
const qrReceiptIcon = computed(() => {
  if (qrReceiptProvider.value === 'wechat') return '微';
  if (qrReceiptProvider.value === 'alipay') return '支';
  return '扫';
});
const qrReceiptWelcomeText = computed(() => {
  if (qrReceiptProvider.value === 'wechat') return '欢迎使用微信付款';
  if (qrReceiptProvider.value === 'alipay') return '欢迎使用支付宝付款';
  return '欢迎使用扫码付款';
});
const qrManualResultTitle = computed(() => {
  if (payResult.value?.status === 'SUCCESS') return '支付成功';
  if (payResult.value?.status === 'FAILED') return '支付失败';
  if (payResult.value?.status === 'CLOSED') return '订单已关闭';
  return '支付处理中';
});
const qrManualResultDescription = computed(() => {
  if (payResult.value?.status === 'SUCCESS') return '已查询到支付成功结果。';
  if (payResult.value?.status === 'FAILED') return '支付失败，请重新选择支付方式或联系业务人员。';
  if (payResult.value?.status === 'CLOSED') return '当前支付订单已关闭。';
  return '暂未查询到成功结果，请确认付款完成后稍后再试。';
});
const qrManualLocked = computed(() => qrManualChecking.value || qrManualLockSeconds.value > 0);
const qrManualButtonText = computed(() => qrManualLockSeconds.value > 0 ? `请稍候 ${qrManualLockSeconds.value}s` : '我已完成支付');
const selectedMethodIsBank = computed(() => isBankMethod(selectedMethod.value));
const selectedMethodIsOffline = computed(() => isOfflineMethod(selectedMethod.value));
const selectedMethodIsQr = computed(() => isQrMethod(selectedMethod.value));
const bankMethodAwaitingInput = computed(() => selectedMethodIsBank.value && !payResult.value && Boolean(session.value?.order?.businessOrderId) && payableOrder.value);
const bankWorkspaceVisible = computed(() => selectedGroupCode.value === 'EBANK' && !payResult.value && (previewMode.value || (Boolean(session.value?.order?.businessOrderId) && payableOrder.value)));
const previewMaterialVisible = computed(() => previewMode.value && Boolean(selectedMethod.value) && !payResult.value && !bankWorkspaceVisible.value);
const previewMaterialType = computed(() => {
  if (selectedMethodIsOffline.value) return 'TRANSFER_ACCOUNT';
  if (selectedMethodIsQr.value) return 'QR';
  return selectedMethod.value?.paymentMaterialType || 'INFO';
});
const previewQrContent = computed(() => {
  if (!previewMode.value || previewMaterialType.value !== 'QR') {
    return '';
  }
  return [
    'MANGO_PAYMENT_CASHIER_PREVIEW',
    session.value?.cashierConfigId || '',
    selectedMethod.value?.methodCode || '',
  ].join(':');
});
const activeQrContent = computed(() => payMaterial.value?.qrContent || previewQrContent.value);
const orderNotPayableTitle = computed(() => orderExpired.value ? '当前订单已超时，不允许发起支付。' : '当前订单状态不可发起支付，可查看订单和支付方式信息。');
const bankAccountLabel = computed(() => selectedMethod.value?.accountNature === 'CORPORATE' ? '企业账号' : '银行卡号');
const transferDeadlineText = computed(() => {
  if (previewMode.value) {
    return `请你于 ${formatChineseDateTime(previewTransferDeadline.value)} 之前完成转账`;
  }
  const deadline = transferDeadlineBeforeOrderExpire(session.value?.order?.expireTime);
  return deadline ? `请你于 ${formatChineseDateTime(deadline)} 之前完成转账` : '请按页面金额和备注完成转账';
});
const previewTransferDeadline = computed(() => {
  const date = new Date(serverNow.value + 20 * 60 * 1000);
  return formatDateTimeValue(date);
});
const displayOrder = computed(() => {
  const order = session.value?.order;
  if (previewMode.value) {
    return {
      bizOrderNo: 'PREVIEW-ORDER',
      orderTitle: '收银台预览订单',
      amount: 128800,
    };
  }
  if (!order?.businessOrderId) {
    return {
      bizOrderNo: '-',
      orderTitle: '-',
      amount: undefined,
    };
  }
  return {
    bizOrderNo: order.bizOrderNo || '-',
    orderTitle: order.orderTitle || '-',
    amount: order.amount,
  };
});
const displaySubjectName = computed(() => previewMode.value ? '演示收款方' : (session.value?.subject?.subjectName || '签约主体'));
const previewTransferMaterial = computed(() => ({
  accountName: '演示收款方',
  accountNo: '6222 0000 0000 0000',
  bankName: '演示银行营业部',
  transferRemark: 'A8K3P2',
}));
const offlineVoucherRules: FormRules = {
  transferAmount: [{ required: true, type: 'number', min: 0.01, message: '请输入实际转账金额', trigger: 'blur' }],
  voucherFileIds: [{ required: true, type: 'array', min: 1, message: '请上传转账凭证', trigger: 'change' }],
};
const bankPaymentRules: FormRules = {
  bankCode: [{ required: true, message: '请选择银行', trigger: 'change' }],
  accountNo: [{ required: true, message: '请输入账号或卡号', trigger: 'blur' }],
  payerName: [{ required: true, message: '请输入付款户名', trigger: 'blur' }],
};
const resultIcon = computed(() => {
  if (payResult.value?.status === 'SUCCESS') return 'success';
  if (payResult.value?.status === 'FAILED' || payResult.value?.status === 'CLOSED') return 'error';
  return 'info';
});
const resultTitle = computed(() => {
  if (payResult.value?.status === 'SUCCESS') return '支付成功';
  if (payResult.value?.status === 'FAILED') return '支付失败';
  if (payResult.value?.status === 'CLOSED') return '订单已关闭';
  if (offlineVoucherVisible.value && offlineVoucherFormVisible.value) return '等待财务确认到账';
  return polling.value ? '支付处理中，等待结果返回' : '支付处理中';
});
const resultTagType = computed(() => {
  if (payResult.value?.status === 'SUCCESS') return 'success';
  if (payResult.value?.status === 'FAILED' || payResult.value?.status === 'CLOSED') return 'danger';
  if (payResult.value?.status === 'PAYING') return 'warning';
  return 'info';
});

onMounted(() => {
  expireTimer = window.setInterval(() => {
    expireNow.value = Date.now();
  }, 1000);
  void loadSession();
});

onBeforeUnmount(() => {
  pollVersion.value += 1;
  if (expireTimer) {
    window.clearInterval(expireTimer);
  }
});

watch([resolvedCashierConfigId, resolvedBusinessOrderId], () => {
  void loadSession();
});

watch(activeQrContent, value => {
  void renderQrImage(value || '');
});

async function loadSession() {
  const cashierConfigId = resolvedCashierConfigId.value;
  if (!cashierConfigId) {
    ElMessage.error('缺少收银台配置参数');
    return;
  }
  loading.value = true;
  resetPayInteraction();
  try {
    session.value = await paymentCashierApi.session(cashierConfigId, resolvedBusinessOrderId.value);
    serverTimeDelta.value = session.value.serverTime ? new Date(session.value.serverTime).getTime() - Date.now() : 0;
    await activateInitialPaymentMethod();
  } finally {
    loading.value = false;
  }
}

async function activateInitialPaymentMethod() {
  const method = initialPaymentMethod();
  if (!method) {
    selectedGroupCode.value = '';
    selectedMethodCode.value = '';
    return;
  }
  selectedMethodCode.value = method.methodCode;
  selectedGroupCode.value = methodGroups.value.find(group => group.methods.some(item => item.methodCode === method.methodCode))?.code || '';
  if (isBankMethod(method)) {
    resetBankPaymentFormWithDefaultBank();
    return;
  }
  if (!previewMode.value && session.value?.order?.businessOrderId && payableOrder.value) {
    await submitPayment();
  }
}

function selectMethod(methodCode: string) {
  selectedMethodCode.value = methodCode;
  selectedGroupCode.value = selectedMethodGroup.value?.code || selectedGroupCode.value;
  resetPayInteraction();
  const method = selectedMethod.value;
  if (!method || previewMode.value || !session.value?.order?.businessOrderId || !payableOrder.value) {
    return;
  }
  if (isBankMethod(method)) {
    resetBankPaymentFormWithDefaultBank();
    return;
  }
  void submitPayment();
}

function selectMethodGroup(group: MethodGroup) {
  selectedGroupCode.value = group.code;
  resetPayInteraction();
  if (group.code === 'EBANK') {
    selectedMethodCode.value = group.methods[0]?.methodCode || '';
    resetBankPaymentFormWithDefaultBank();
    return;
  }
  const method = preferredGroupMethod(group);
  if (method) {
    selectedMethodCode.value = method.methodCode;
    if (previewMode.value || !session.value?.order?.businessOrderId || !payableOrder.value) {
      return;
    }
    void submitPayment();
  }
}

function selectMethodGroupByCode(value: string | number) {
  const group = methodGroups.value.find(item => item.code === String(value));
  if (group) {
    selectMethodGroup(group);
  }
}

function selectBankMethod(methodCode: string) {
  selectedMethodCode.value = methodCode;
  selectedGroupCode.value = 'EBANK';
  resetPayInteraction();
  resetBankPaymentFormWithDefaultBank();
}

function selectBankBanner(bank: BankBannerOption) {
  bankPaymentForm.value.bankCode = bank.code;
  bankPaymentForm.value.bankName = bank.name;
  bankPaymentFormRef.value?.clearValidate('bankCode');
}

async function submitBankPayment() {
  if (!selectedMethodIsBank.value) {
    return;
  }
  await bankPaymentFormRef.value?.validate();
  await submitPayment();
}

async function submitPayment() {
  if (!session.value?.order?.businessOrderId || !selectedMethod.value || !payableOrder.value) {
    return;
  }
  paying.value = true;
  try {
    payResult.value = await paymentCashierApi.pay({
      cashierConfigId: session.value.cashierConfigId,
      businessOrderId: session.value.order.businessOrderId,
      methodCode: selectedMethod.value.methodCode,
      bankCode: selectedMethodIsBank.value ? bankPaymentForm.value.bankCode : undefined,
      bankName: selectedMethodIsBank.value ? bankPaymentForm.value.bankName : undefined,
      payerAccountNo: selectedMethodIsBank.value ? bankPaymentForm.value.accountNo : undefined,
      payerName: selectedMethodIsBank.value ? bankPaymentForm.value.payerName : undefined,
    });
    resetOfflineVoucherForm();
    if (payResult.value.status === 'SUCCESS') {
      await refreshPayResult(payResult.value.payOrderNo);
      handlePaymentSuccess();
    } else {
      startResultPolling(payResult.value.payOrderNo);
    }
  } finally {
    paying.value = false;
  }
}

function showOfflineVoucherForm() {
  offlineVoucherFormVisible.value = true;
  void nextTick(() => {
    offlineVoucherFormRef.value?.clearValidate();
  });
}

async function submitOfflineVoucher() {
  if (!payResult.value?.payOrderNo) {
    return;
  }
  await offlineVoucherFormRef.value?.validate();
  const voucherFileIds = offlineVoucherForm.value.voucherFileIds.filter(Boolean).join(',');
  if (!voucherFileIds) {
    ElMessage.warning('请上传转账凭证');
    return;
  }
  submittingOfflineVoucher.value = true;
  try {
    await paymentCashierApi.submitOfflineTransferVoucher({
      payOrderNo: payResult.value.payOrderNo,
      transferAmount: yuanToCents(offlineVoucherForm.value.transferAmount),
      voucherFileIds,
      submitRemark: offlineVoucherForm.value.submitRemark || undefined,
    });
    ElMessage.success('转账凭证已提交，等待财务确认到账');
    await refreshPayResult(payResult.value.payOrderNo);
    startResultPolling(payResult.value.payOrderNo);
  } finally {
    submittingOfflineVoucher.value = false;
  }
}

function resetPayInteraction() {
  payResult.value = undefined;
  qrImageUrl.value = '';
  polling.value = false;
  qrManualChecking.value = false;
  qrManualLockSeconds.value = 0;
  qrResultDialogVisible.value = false;
  completionHandled.value = false;
  resultRefreshCountdown.value = 5;
  offlineVoucherFormVisible.value = false;
  pollVersion.value += 1;
  resetOfflineVoucherForm();
  resetBankPaymentForm();
}

function resetOfflineVoucherForm() {
  offlineVoucherForm.value = {
    transferAmount: centsToYuan(session.value?.order?.amount || payResult.value?.amount || 0),
    voucherFileIds: [],
    submitRemark: '',
  };
  offlineVoucherFormRef.value?.clearValidate();
}

function resetBankPaymentForm() {
  bankPaymentForm.value = {
    bankCode: '',
    bankName: '',
    accountNo: '',
    payerName: '',
    enterpriseAuthorized: false,
  };
  bankPaymentFormRef.value?.clearValidate();
}

function resetBankPaymentFormWithDefaultBank() {
  resetBankPaymentForm();
  const firstBank = bankBannerOptions[0];
  if (firstBank) {
    bankPaymentForm.value.bankCode = firstBank.code;
    bankPaymentForm.value.bankName = firstBank.name;
  }
}

function startResultPolling(payOrderNo: string) {
  const version = pollVersion.value + 1;
  pollVersion.value = version;
  polling.value = true;
  void pollPayResult(payOrderNo, 20, version);
}

async function pollPayResult(payOrderNo: string, remaining: number, version: number) {
  if (version !== pollVersion.value) {
    return;
  }
  if (remaining <= 0) {
    polling.value = false;
    return;
  }
  for (let seconds = 5; seconds > 0; seconds -= 1) {
    if (version !== pollVersion.value) {
      return;
    }
    resultRefreshCountdown.value = seconds;
    await delay(1000);
  }
  if (version !== pollVersion.value) {
    return;
  }
  await refreshPayResult(payOrderNo);
  if (payResult.value?.status === 'SUCCESS') {
    polling.value = false;
    handlePaymentSuccess();
    return;
  }
  if (payResult.value?.status === 'FAILED' || payResult.value?.status === 'CLOSED') {
    polling.value = false;
    return;
  }
  void pollPayResult(payOrderNo, remaining - 1, version);
}

async function refreshPayResult(payOrderNo: string) {
  payResult.value = await paymentCashierApi.payResult(payOrderNo);
}

async function submitMangoPayVirtualPayment() {
  if (!session.value?.order?.amount || !payResult.value?.payOrderNo || payResult.value.channelCode !== 'MANGO_PAY') {
    return;
  }
  mangoPaying.value = true;
  try {
    await paymentCashierApi.mangoPayVirtualPay({
      cashierConfigId: session.value.cashierConfigId,
      payOrderNo: payResult.value.payOrderNo,
      title: mangoPayOrderTitle.value,
      amount: Number(session.value.order.amount),
      paymentMethodCode: payResult.value.methodCode,
    });
    await refreshPayResult(payResult.value.payOrderNo);
    if (payResult.value?.status === 'SUCCESS') {
      polling.value = false;
      pollVersion.value += 1;
      handlePaymentSuccess();
    }
  } finally {
    mangoPaying.value = false;
  }
}

async function refreshCurrentPayResult() {
  if (!payResult.value?.payOrderNo) {
    return;
  }
  await refreshPayResult(payResult.value.payOrderNo);
  if (terminalPayResult.value) {
    polling.value = false;
    pollVersion.value += 1;
    if (payResult.value?.status === 'SUCCESS') {
      handlePaymentSuccess();
    }
  } else {
    startResultPolling(payResult.value.payOrderNo);
  }
}

async function confirmQrPaymentCompleted() {
  if (!payResult.value?.payOrderNo || qrManualLocked.value || completionHandled.value) {
    return;
  }
  const payOrderNo = payResult.value.payOrderNo;
  qrManualChecking.value = true;
  qrManualLockSeconds.value = 5;
  try {
    payResult.value = await paymentCashierApi.syncPayResult(payOrderNo);
    if (payResult.value?.status === 'SUCCESS') {
      polling.value = false;
      pollVersion.value += 1;
      handlePaymentSuccess();
    } else if (terminalPayResult.value) {
      polling.value = false;
      pollVersion.value += 1;
      qrResultDialogVisible.value = true;
    } else {
      startResultPolling(payOrderNo);
    }
    while (qrManualLockSeconds.value > 0) {
      await delay(1000);
      qrManualLockSeconds.value -= 1;
      if (!payResult.value?.payOrderNo) {
        continue;
      }
      await refreshPayResult(payResult.value.payOrderNo);
      if (payResult.value?.status === 'SUCCESS') {
        handlePaymentSuccess();
        break;
      }
    }
  } finally {
    qrManualChecking.value = false;
    qrManualLockSeconds.value = 0;
  }
}

function handlePaymentSuccess() {
  if (!payResult.value || completionHandled.value) {
    return;
  }
  completionHandled.value = true;
  polling.value = false;
  pollVersion.value += 1;
  qrResultDialogVisible.value = false;
  ElMessage.success('支付成功');
  emit('success', payResult.value);
  const returnUrl = successReturnUrl();
  window.setTimeout(() => {
    if (returnUrl) {
      window.location.href = returnUrl;
      return;
    }
    if (props.embedded) {
      emit('close');
    }
  }, 800);
}

function successReturnUrl() {
  return session.value?.order?.returnUrl || session.value?.returnUrl || '';
}

function openRedirectUrl() {
  const redirectUrl = payMaterial.value?.redirectUrl;
  if (!redirectUrl) {
    ElMessage.warning('当前通道未返回支付跳转地址');
    return;
  }
  window.open(redirectUrl, '_blank', 'noopener,noreferrer');
}

function submitHtmlForm() {
  const htmlForm = payMaterial.value?.htmlForm;
  if (!htmlForm) {
    ElMessage.warning('当前通道未返回网银表单');
    return;
  }
  const container = document.createElement('div');
  container.style.display = 'none';
  container.innerHTML = htmlForm;
  document.body.appendChild(container);
  const form = container.querySelector('form');
  if (!form) {
    document.body.removeChild(container);
    ElMessage.error('网银表单格式不正确');
    return;
  }
  form.setAttribute('target', '_blank');
  form.submit();
  window.setTimeout(() => {
    if (container.parentNode) {
      container.parentNode.removeChild(container);
    }
  }, 1000);
}

async function copyText(value: string | undefined, label: string) {
  if (!value) {
    return;
  }
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(value);
  } else {
    const input = document.createElement('textarea');
    input.value = value;
    input.style.position = 'fixed';
    input.style.opacity = '0';
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
  }
  ElMessage.success(`${label}已复制`);
}

function delay(ms: number) {
  return new Promise(resolve => window.setTimeout(resolve, ms));
}

function centsToYuan(value: number | string) {
  return Number((Number(value || 0) / 100).toFixed(2));
}

function yuanToCents(value: number) {
  return Math.round(Number(value || 0) * 100);
}

function formatMoney(value: number | string | undefined) {
  const amount = Number(value);
  return Number.isFinite(amount) ? `￥${(amount / 100).toFixed(2)}` : '-';
}

function transferDeadlineBeforeOrderExpire(value: string | undefined) {
  if (!value) {
    return undefined;
  }
  const date = parseLocalDateTime(value);
  if (!date) {
    return value;
  }
  date.setMinutes(date.getMinutes() - 10);
  return formatDateTimeValue(date);
}

function formatChineseDateTime(value: string) {
  const date = parseLocalDateTime(value);
  if (!date) {
    return value;
  }
  return `${date.getFullYear()} 年 ${padDatePart(date.getMonth() + 1)} 月 ${padDatePart(date.getDate())} 日 ${padDatePart(date.getHours())}:${padDatePart(date.getMinutes())}`;
}

function parseLocalDateTime(value: string) {
  const date = new Date(value.replace(/-/g, '/').replace('T', ' '));
  return Number.isNaN(date.getTime()) ? undefined : date;
}

function formatDateTimeValue(date: Date) {
  return `${date.getFullYear()}-${padDatePart(date.getMonth() + 1)}-${padDatePart(date.getDate())} ${padDatePart(date.getHours())}:${padDatePart(date.getMinutes())}:${padDatePart(date.getSeconds())}`;
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}

function methodIcon(method: PaymentCashierMethod | undefined) {
  if (!method) return '付';
  if (method.instrumentType === 'WECHAT') return '微';
  if (method.instrumentType === 'ALIPAY') return '支';
  if (method.instrumentType === 'UNIONPAY') return '银';
  if (method.instrumentType === 'EBANK') return '网';
  if (method.instrumentType === 'OFFLINE_TRANSFER') return '转';
  if (method.instrumentType === 'BANK_CARD' && method.interactionType === 'CREDIT_QUICK') return '信';
  if (method.instrumentType === 'BANK_CARD' && method.interactionType === 'DEBIT_QUICK') return '储';
  if (method.instrumentType === 'WALLET') return '包';
  return '付';
}

function methodLogoUrl(method: PaymentCashierMethod | undefined) {
  if (!method) return '';
  return method.iconFileId ? fileApi.downloadUrl(method.iconFileId) : '';
}

function primaryMethod(group: MethodGroup) {
  return group.methods[0];
}

function preferredGroupMethod(group: MethodGroup) {
  if (group.code === 'WECHAT_PAY') {
    return group.methods.find(method => method.paymentMaterialType === 'QR') || primaryMethod(group);
  }
  if (group.code === 'ALIPAY') {
    return group.methods.find(method => method.paymentMaterialType === 'QR') || primaryMethod(group);
  }
  if (group.code === 'OFFLINE_TRANSFER') {
    return group.methods.find(method => method.paymentMaterialType === 'TRANSFER_ACCOUNT') || primaryMethod(group);
  }
  return primaryMethod(group);
}

function initialPaymentMethod() {
  const methods = session.value?.methods || [];
  if (!methods.length) return undefined;
  const defaultMethod = methods.find(method => method.methodCode === session.value?.defaultMethodCode);
  if (defaultMethod) return defaultMethod;
  const firstGroup = methodGroups.value[0];
  return firstGroup ? preferredGroupMethod(firstGroup) : methods[0];
}

function displayGroupName(group: MethodGroup) {
  const names: Record<string, string> = {
    WECHAT_PAY: '微信支付',
    ALIPAY: '支付宝支付',
    EBANK: '网银支付',
    OFFLINE_TRANSFER: '线下转账',
  };
  return names[group.code] || group.name;
}

function isBankMethod(method: PaymentCashierMethod | undefined) {
  if (!method) return false;
  return method.instrumentType === 'EBANK' || method.interactionType === 'EBANK' || method.methodName.includes('网银');
}

function isOfflineMethod(method: PaymentCashierMethod | undefined) {
  if (!method) return false;
  return method.instrumentType === 'OFFLINE_TRANSFER'
    || method.interactionType === 'OFFLINE_TRANSFER'
    || method.methodCode === 'CORPORATE_OFFLINE_ACCOUNT';
}

function isQrMethod(method: PaymentCashierMethod | undefined) {
  if (!method) return false;
  return method.paymentMaterialType === 'QR'
    || method.instrumentType === 'WECHAT'
    || method.instrumentType === 'ALIPAY'
    || method.interactionType === 'SCAN_QR';
}

async function renderQrImage(content: string) {
  if (!content) {
    qrImageUrl.value = '';
    return;
  }
  try {
    qrImageUrl.value = await QRCode.toDataURL(content, {
      errorCorrectionLevel: 'M',
      margin: 2,
      scale: 6,
      width: 220,
    });
  } catch (error) {
    qrImageUrl.value = '';
    ElMessage.error('二维码生成失败');
    throw error;
  }
}

function formatExpire(value: string, now: number) {
  const expire = new Date(value).getTime();
  const diff = Math.max(0, expire - now);
  const minutes = Math.floor(diff / 60000);
  const seconds = Math.floor((diff % 60000) / 1000);
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

</script>

<style scoped>
.cashier-page {
  min-height: calc(100vh - 112px);
  padding: 16px;
  background: var(--el-bg-color-page);
}

:global(.payment-cashier-dialog .el-dialog__body) {
  max-height: calc(86vh - 56px);
  overflow: hidden;
  background: var(--el-bg-color-page);
  padding: 0;
}

.cashier-page.is-embedded {
  max-height: calc(86vh - 56px);
  min-height: 0;
  overflow: auto;
  padding: 0;
  background: var(--el-bg-color);
}

.cashier-page.is-embedded .cashier-shell {
  max-height: calc(86vh - 56px);
}

.cashier-shell {
  max-width: 920px;
  margin: 0 auto;
  color: var(--el-text-color-primary);
}

.cashier-header,
.cashier-order,
.cashier-main,
.cashier-help {
  background: var(--el-bg-color);
}

.cashier-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.cashier-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.cashier-brand__logo,
.cashier-brand__mark {
  flex: 0 0 auto;
  width: 46px;
  height: 46px;
  border-radius: 6px;
}

.cashier-brand__logo {
  object-fit: contain;
  background: var(--el-bg-color);
}

.cashier-brand__mark {
  display: grid;
  place-items: center;
  background: var(--el-color-primary);
  color: var(--el-color-white);
  font-size: 20px;
  font-weight: 700;
}

.cashier-brand__text {
  min-width: 0;
}

.cashier-brand__text strong {
  display: block;
  overflow: hidden;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cashier-brand__text span,
.cashier-order-no span,
.cashier-order span,
.cashier-order__amount small,
.material-note,
.redirect-material span,
.cashier-help {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.cashier-brand__text span {
  display: block;
  margin-top: 4px;
}

.cashier-order-no {
  flex: 0 1 260px;
  min-width: 0;
  text-align: right;
}

.cashier-order-no strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cashier-order {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(160px, 220px);
  gap: 14px;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.cashier-order__info {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.cashier-order__info > div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  align-items: baseline;
  min-width: 0;
}

.cashier-order__amount {
  min-width: 0;
}

.cashier-order strong {
  display: block;
  overflow: hidden;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cashier-order__amount {
  text-align: right;
}

.cashier-order__amount strong {
  display: block;
  margin-top: 3px;
  color: var(--el-color-danger);
  font-size: 21px;
  line-height: 1.2;
  letter-spacing: 0;
}

.cashier-order__amount small {
  display: block;
  margin-top: 4px;
}

.cashier-main {
  display: grid;
  gap: 12px;
  padding: 14px 20px 18px;
}

.payment-method-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.payment-method-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--el-border-color-lighter);
}

.payment-method-tabs :deep(.el-tabs__item) {
  height: 42px;
  padding: 0 18px;
}

.method-tab-label {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.method-tab-label__icon {
  display: grid;
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.method-tab-label__icon img {
  display: block;
  width: 18px;
  height: 18px;
  object-fit: contain;
}

.cashier-workspace {
  min-width: 0;
}

.cashier-alert {
  margin-bottom: 12px;
}

.material-panel {
  min-height: 0;
  padding: 14px;
  background: var(--el-fill-color-extra-light);
  border-radius: 6px;
}

.bank-form__control,
.offline-voucher-form__amount {
  width: 100%;
}

.ebank-type-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.ebank-type-tab {
  min-width: 96px;
  height: 34px;
  padding: 0 14px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  cursor: pointer;
}

.ebank-type-tab.active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.bank-banner-grid {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.bank-banner {
  display: grid;
  gap: 2px;
  min-height: 54px;
  padding: 8px 10px;
  text-align: left;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  cursor: pointer;
}

.bank-banner:hover,
.bank-banner.active {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.bank-banner strong {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.bank-banner span {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-material {
  display: grid;
  gap: 10px;
}

.transfer-check {
  position: relative;
  overflow: hidden;
  padding: 16px 18px 14px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--el-color-primary) 4%, transparent), transparent 34%),
    var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  box-shadow: 0 8px 20px color-mix(in srgb, var(--el-text-color-primary) 5%, transparent);
}

.transfer-check::before,
.transfer-check::after {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 8px;
  background: repeating-linear-gradient(
    180deg,
    var(--el-border-color-lighter) 0,
    var(--el-border-color-lighter) 8px,
    transparent 8px,
    transparent 16px
  );
  content: "";
}

.transfer-check::before {
  left: 0;
}

.transfer-check::after {
  right: 0;
}

.transfer-check__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 0 6px 12px;
  border-bottom: 1px dashed var(--el-border-color);
}

.transfer-check__title {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.transfer-check__title span {
  color: var(--el-text-color-primary);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0;
}

.transfer-check__title small {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.45;
}

.transfer-check__amount {
  display: grid;
  justify-items: end;
  min-width: 150px;
}

.transfer-check__amount span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.transfer-check__amount strong {
  color: var(--el-color-danger);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0;
}

.transfer-check__intro {
  margin: 12px 6px 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.55;
}

.transfer-check__body {
  display: grid;
  gap: 0;
  padding: 12px 6px 8px;
}

.transfer-check__payee {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 34px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.transfer-check__payee span,
.transfer-check__remark span {
  color: var(--el-text-color-secondary);
}

.transfer-check__payee strong,
.transfer-check__remark strong {
  overflow-wrap: anywhere;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.transfer-check__remark {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin: 0 6px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border: 1px dashed var(--el-color-primary-light-5);
  border-radius: 4px;
}

.transfer-check__remark strong {
  color: var(--el-color-primary);
  font-size: 20px;
  letter-spacing: 1px;
}

.transfer-check__footer {
  margin: 10px 6px 0;
  padding-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.5;
  border-top: 1px dashed var(--el-border-color-lighter);
}

.material-note {
  margin: 0;
  line-height: 1.5;
}

.offline-action,
.material-primary-action {
  margin-top: 2px;
}

.offline-action {
  display: flex;
  justify-content: flex-end;
}

.offline-voucher-form {
  padding-top: 4px;
}

.qr-material {
  --qr-provider-color: var(--el-color-primary);
  --qr-provider-color-soft: var(--el-color-primary-light-9);
  display: grid;
  width: min(100%, 286px);
  margin: 0 auto;
  gap: 8px;
  justify-items: center;
  padding: 12px 14px;
  background: linear-gradient(180deg, var(--qr-provider-color-soft), var(--el-bg-color) 42%);
  border-radius: 6px;
}

.qr-material.is-wechat {
  --qr-provider-color: #07c160;
  --qr-provider-color-soft: #e9f8ef;
}

.qr-material.is-alipay {
  --qr-provider-color: #1677ff;
  --qr-provider-color-soft: #eaf3ff;
}

.qr-receipt__brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  color: var(--qr-provider-color);
}

.qr-receipt__brand-icon {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--qr-provider-color);
  font-size: 16px;
  font-weight: 700;
}

.qr-receipt__brand strong {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0;
}

.qr-receipt__amount {
  display: grid;
  gap: 3px;
  justify-items: center;
  width: 100%;
}

.qr-receipt__amount span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.qr-receipt__amount strong {
  color: var(--el-color-danger);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.15;
}

.qr-receipt__scan-frame {
  position: relative;
  display: grid;
  width: 198px;
  height: 198px;
  place-items: center;
}

.qr-corner {
  position: absolute;
  width: 38px;
  height: 38px;
  border-color: var(--qr-provider-color);
  border-style: solid;
}

.qr-corner.is-left-top {
  top: 0;
  left: 0;
  border-width: 3px 0 0 3px;
}

.qr-corner.is-right-top {
  top: 0;
  right: 0;
  border-width: 3px 3px 0 0;
}

.qr-corner.is-left-bottom {
  bottom: 0;
  left: 0;
  border-width: 0 0 3px 3px;
}

.qr-corner.is-right-bottom {
  right: 0;
  bottom: 0;
  border-width: 0 3px 3px 0;
}

.qr-code-box {
  display: grid;
  width: 168px;
  height: 168px;
  place-items: center;
  background: var(--el-bg-color);
  border-radius: 4px;
}

.qr-image {
  display: block;
  width: 154px;
  height: 154px;
}

.qr-receipt__tip {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}

.qr-receipt__welcome {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 16px;
  border-radius: 999px;
  background: var(--el-bg-color);
  color: var(--qr-provider-color);
  font-size: 12px;
}

.qr-receipt__actions {
  display: flex;
  justify-content: center;
  width: 100%;
}

.qr-receipt__order {
  overflow: hidden;
  width: 100%;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.redirect-material {
  display: grid;
  gap: 10px;
  min-height: 180px;
  align-content: center;
  justify-items: center;
  text-align: center;
}

.redirect-material strong {
  font-size: 16px;
}

.redirect-material__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.cashier-help {
  padding: 0 20px 18px;
  line-height: 1.5;
}

@media (max-width: 1180px) {
  .qr-code-box {
    width: 168px;
  }
}

@media (max-width: 760px) {
  .cashier-order {
    grid-template-columns: 1fr;
  }

  .cashier-order__amount {
    text-align: left;
  }

  .payment-method-tabs :deep(.el-tabs__nav) {
    width: 100%;
  }

  .payment-method-tabs :deep(.el-tabs__item) {
    flex: 1 0 auto;
    padding: 0 10px;
  }

  .bank-banner-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .cashier-page {
    padding: 12px;
  }

  .cashier-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .cashier-order-no {
    width: 100%;
    text-align: left;
  }

  .cashier-main,
  .cashier-order,
  .cashier-header {
    padding-right: 14px;
    padding-left: 14px;
  }

  .material-panel {
    padding: 16px;
  }

  .transfer-check__payee,
  .transfer-check__remark {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .transfer-check__header {
    display: grid;
  }

  .transfer-check__amount {
    justify-items: start;
  }
}
</style>
