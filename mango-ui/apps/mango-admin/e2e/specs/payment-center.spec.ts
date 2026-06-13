import { Buffer } from 'node:buffer';
import { execFileSync } from 'node:child_process';
import { createHash, createHmac } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { createServer, type Server } from 'node:http';
import { resolve } from 'node:path';
import { expect, test, type APIResponse, type Locator, type Page } from '@playwright/test';
import * as XLSX from 'xlsx';

const PAY_ORDER_NO_PATTERN = /^PO\d+$/;
const EXCEPTION_ORDER_NO_PATTERN = /^EX\d+$/;
const OFFLINE_REFUND_NO_PATTERN = /^OF\d+$/;
let e2eRefundOrderSequence = 0;

function nextE2eRefundOrderNo() {
  e2eRefundOrderSequence += 1;
  const datePart = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  const sequence = (Number(Date.now().toString().slice(-8)) + e2eRefundOrderSequence) % 100000000;
  return `RO${datePart}${String(sequence).padStart(8, '0')}`;
}

type ApiBody<T> = {
  code?: number;
  success?: boolean;
  data?: T;
  msg?: string;
};

type PageData = {
  list?: Array<Record<string, unknown>>;
  total?: string | number;
};

type PaymentApplication = {
  id?: string;
  appId?: string;
  appName?: string;
  appSecret?: string;
  secretConfigured?: number;
  secretVersion?: number;
  secretLastResetTime?: string;
  signAlgorithm?: string;
  ipWhitelistEnabled?: number;
  ipWhitelist?: string;
  payloadEncryptEnabled?: number;
  notifyRetryPolicy?: string;
  demoApp?: number;
  status?: number;
};

type PaymentApplicationSaveResult = {
  id?: string;
  appId?: string;
  appSecret?: string;
  secretGenerated?: number;
};

type PaymentCashierConfig = {
  id?: string;
  applicationId?: string;
  applicationName?: string;
  cashierName?: string;
  defaultCashier?: number;
  enterpriseSubjectIds?: string;
  enterpriseSubjectNames?: string;
  methodCodes?: string;
  methodNames?: string;
  defaultMethodCode?: string;
  defaultMethodName?: string;
  methodDisplayOrder?: string;
  displayConfig?: string;
  resultReturnUrl?: string;
  status?: number;
};

type PaymentEnterpriseSubject = {
  id?: string;
  subjectName?: string;
  creditCode?: string;
  creditCodeMask?: string;
  bankAccountNo?: string;
  bankAccountNoMask?: string;
  bankName?: string;
  licenseFileId?: string;
  status?: number;
};

type PaymentChannel = {
  id?: string;
  channelCode?: string;
  channelName?: string;
  channelType?: string;
  adapterType?: string;
  fieldTemplateJson?: string;
  capabilitySummary?: string;
  capabilities?: PaymentChannelCapability[];
  status?: number;
};

type PaymentChannelCapability = {
  id?: string;
  channelId?: string;
  methodCode?: string;
  methodName?: string;
  terminalType?: string;
  environment?: string;
  supportsRefund?: number;
  supportsQuery?: number;
  supportsClose?: number;
  supportsBill?: number;
  supportsReconcile?: number;
  minAmount?: number;
  maxAmount?: number;
  status?: number;
};

type PaymentChannelContractCapability = {
  id?: string;
  channelCapabilityId?: string;
  methodCode?: string;
  terminalType?: string;
  feeRate?: number;
  minAmount?: number;
  maxAmount?: number;
  priority?: number;
  certificateExpireTime?: string;
  status?: number;
};

type PaymentChannelContract = {
  id?: string;
  contractCode?: string;
  contractName?: string;
  subjectId?: string;
  channelId?: string;
  environment?: string;
  merchantNo?: string;
  appId?: string;
  configValuesJson?: string;
  enabledMethodCodes?: string;
  capabilities?: PaymentChannelContractCapability[];
  status?: number;
};

type PaymentMethod = {
  id?: string;
  methodCode?: string;
  methodName?: string;
  accountNature?: string;
  instrumentType?: string;
  interactionType?: string;
  terminalScope?: string;
  paymentMaterialType?: string;
  iconFileId?: string;
  requiresBankSelection?: number;
  requiresQrRefresh?: number;
  minAmount?: number;
  maxAmount?: number;
  status?: number;
};

type PaymentMethodRouteRuleItem = {
  id?: string;
  ruleId?: string;
  contractCapabilityId?: string;
  contractName?: string;
  channelName?: string;
  methodCode?: string;
  terminalType?: string;
  priority?: number;
  weight?: number;
  minAmount?: number;
  maxAmount?: number;
  status?: number;
};

type PaymentMethodRouteRule = {
  id?: string;
  ruleCode?: string;
  ruleName?: string;
  appId?: string;
  subjectId?: string;
  methodCode?: string;
  terminalType?: string;
  environment?: string;
  routeMode?: string;
  fallbackEnabled?: number;
  status?: number;
  items?: PaymentMethodRouteRuleItem[];
};

type PaymentMethodRouteTrialResult = {
  matched?: boolean;
  matchedRule?: PaymentMethodRouteRule;
  matchedItem?: PaymentMethodRouteRuleItem;
  filterReasons?: string[];
};

type PaymentMethodCategory = {
  id?: string;
  categoryCode?: string;
  categoryName?: string;
  level?: number;
  parentId?: string;
  children?: PaymentMethodCategory[];
};

type MangoPayScenarioType = 'PAYMENT' | 'PAYMENT_QUERY' | 'REFUND' | 'REFUND_QUERY' | 'BILL' | 'CALLBACK_DELAY';

type PaymentBusinessOrder = {
  id?: string;
  bizOrderNo?: string;
  appId?: string;
  appName?: string;
  title?: string;
  subjectName?: string;
  cashierConfigId?: string;
  cashierName?: string;
  amount?: number;
  paidAmount?: number;
  refundedAmount?: number;
  status?: string;
  statusName?: string;
  notifyUrl?: string;
  returnUrl?: string;
  extendInfo?: string;
  paymentOrderCount?: number;
  refundOrderCount?: number;
};

type PaymentOpenCashier = {
  cashierConfigId?: string;
  businessOrderId?: string;
  bizOrderNo?: string;
  cashierUrl?: string;
  expireTime?: string;
};

type PaymentOpenPaymentOrder = {
  id?: string;
  payOrderNo?: string;
  businessOrderId?: string;
  bizOrderNo?: string;
  appId?: string;
  title?: string;
  amount?: number | string;
  currency?: string;
  status?: string;
  methodCode?: string;
  methodName?: string;
  channelCode?: string;
  channelName?: string;
  channelMerchantNo?: string;
  contractId?: string;
  contractCapabilityId?: string;
  routeRuleId?: string;
  channelTradeNo?: string;
  successFlag?: number;
  payTime?: string;
  expireTime?: string;
  flowNo?: string;
  material?: {
    materialType?: string;
    qrContent?: string;
    redirectUrl?: string;
    htmlForm?: string;
    accountName?: string;
    accountNo?: string;
    bankName?: string;
    transferRemark?: string;
    transferInstruction?: string;
  };
};

type MangoPayVirtualPaymentResult = {
  virtualPaymentNo?: string;
  payOrderNo?: string;
  status?: string;
  title?: string;
  amount?: number | string;
  paidTime?: string;
};

type PaymentOpenRefundOrder = {
  id?: string;
  refundOrderNo?: string;
  bizRefundNo?: string;
  paymentOrderId?: string;
  payOrderNo?: string;
  bizOrderNo?: string;
  appId?: string;
  refundAmount?: number | string;
  currency?: string;
  reason?: string;
  status?: string;
  methodCode?: string;
  channelCode?: string;
  channelTradeNo?: string;
  channelRefundNo?: string;
  refundTime?: string;
  flowNo?: string;
};

type PaymentOpenReceipt = {
  receiptNo?: string;
  bizOrderNo?: string;
  payOrderNo?: string;
  appId?: string;
  title?: string;
  amount?: number | string;
  currency?: string;
  status?: string;
  methodCode?: string;
  methodName?: string;
  channelCode?: string;
  channelName?: string;
  channelMerchantNo?: string;
  channelTradeNo?: string;
  flowNo?: string;
  payTime?: string;
  createTime?: string;
  issuedTime?: string;
};

type PaymentOpenNotification = {
  notifyNo?: string;
  notificationType?: string;
  tenantId?: number | string;
  appId?: string;
  bizOrderNo?: string;
  payOrderNo?: string;
  bizRefundNo?: string;
  refundOrderNo?: string;
  amount?: number | string;
  refundAmount?: number | string;
  currency?: string;
  status?: string;
  methodCode?: string;
  channelCode?: string;
  channelTradeNo?: string;
  channelRefundNo?: string;
  flowNo?: string;
  eventTime?: string;
  notifyTime?: string;
  signAlgorithm?: string;
  signature?: string;
};

type PaymentBusinessOrderStatus = {
  statusCode?: string;
  statusName?: string;
};

type PaymentOrder = {
  id?: string;
  payOrderNo?: string;
  bizOrderNo?: string;
  title?: string;
  appId?: string;
  subjectName?: string;
  cashierName?: string;
  methodCode?: string;
  methodName?: string;
  channelCode?: string;
  channelName?: string;
  channelMerchantNo?: string;
  contractName?: string;
  contractCapabilityId?: string;
  routeRuleId?: string;
  amount?: number;
  refundedAmount?: number;
  occupyingRefundAmount?: number;
  refundableAmount?: number;
  currency?: string;
  status?: string;
  statusName?: string;
  channelTradeNo?: string;
  successFlag?: number;
  payTime?: string;
  expireTime?: string;
  flowNo?: string;
  statusFlows?: Array<{
    statusCode?: string;
    statusName?: string;
    source?: string;
    remark?: string;
  }>;
};

type PaymentOrderStatus = {
  statusCode?: string;
  statusName?: string;
};

type PaymentRefundOrder = {
  id?: string;
  refundOrderNo?: string;
  bizRefundNo?: string;
  payOrderNo?: string;
  bizOrderNo?: string;
  title?: string;
  appId?: string;
  methodCode?: string;
  methodName?: string;
  channelCode?: string;
  channelName?: string;
  channelMerchantNo?: string;
  channelTradeNo?: string;
  channelRefundNo?: string;
  refundAmount?: number;
  currency?: string;
  reason?: string;
  status?: string;
  statusName?: string;
  refundTime?: string;
  flowNo?: string;
  statusFlows?: Array<{
    statusCode?: string;
    statusName?: string;
    source?: string;
    remark?: string;
  }>;
};

type PaymentRefundOrderStatus = {
  statusCode?: string;
  statusName?: string;
};

type PaymentRefundApproval = {
  id?: string;
  approvalNo?: string;
  appId?: string;
  bizOrderNo?: string;
  bizRefundNo?: string;
  paymentOrderId?: string;
  payOrderNo?: string;
  refundOrderId?: string;
  refundOrderNo?: string;
  refundAmount?: number;
  reason?: string;
  remark?: string;
  status?: string;
  statusName?: string;
  applicantName?: string;
  reviewerName?: string;
  reviewReason?: string;
  workflowApplyStatus?: string;
  workflowApplyStatusName?: string;
  workflowCurrentTaskNames?: string;
  workflowCurrentAssigneeNames?: string;
};

type PaymentRefundApprovalStatus = {
  statusCode?: string;
  statusName?: string;
};

type PaymentTransactionFlow = {
  id?: string;
  flowNo?: string;
  bizOrderNo?: string;
  payOrderNo?: string;
  refundOrderNo?: string;
  flowType?: string;
  flowTypeName?: string;
  amount?: number;
  currency?: string;
};

type PaymentChannelQueryRecord = {
  queryNo?: string;
  payOrderNo?: string;
  beforeStatus?: string;
  channelStatus?: string;
  resultStatus?: string;
  processResult?: string;
};

type PaymentRefundQueryRecord = {
  queryNo?: string;
  refundOrderNo?: string;
  beforeStatus?: string;
  channelStatus?: string;
  resultStatus?: string;
  processResult?: string;
};

type PaymentExceptionOrder = {
  id?: string;
  exceptionNo?: string;
  relatedOrderNo?: string;
  exceptionType?: string;
  exceptionTypeName?: string;
  severity?: string;
  severityName?: string;
  handleStatus?: string;
  handleStatusName?: string;
  reason?: string;
  handleAction?: string;
  handleReason?: string;
  handleResult?: string;
  handleEvidence?: string;
  handlerName?: string;
  handleTime?: string;
};

type PaymentOfflineCollection = {
  id?: string;
  offlineCollectionNo?: string;
  payOrderNo?: string;
  bizOrderNo?: string;
  channelCode?: string;
  channelName?: string;
  accountName?: string;
  accountNoMask?: string;
  bankName?: string;
  amount?: number;
  transferAmount?: number;
  voucherFileIds?: string;
  voucherCount?: number;
  confirmedAmount?: number;
  reconciliationCode?: string;
  transferRemark?: string;
  collectionStatus?: string;
  collectionStatusName?: string;
};

type PaymentOfflineBankStatementItem = {
  id?: string;
  rowNo?: number;
  bankStatementNo?: string;
  amount?: number;
  reconciliationCode?: string;
  matchedOfflineCollectionNo?: string;
  matchedPayOrderNo?: string;
  matchStatus?: string;
  matchStatusName?: string;
};

type PaymentOfflineBankStatementBatch = {
  id?: string;
  batchNo?: string;
  statementFileName?: string;
  totalCount?: number;
  matchedCount?: number;
  confirmedCount?: number;
  differenceCount?: number;
  batchStatus?: string;
  batchStatusName?: string;
  items?: PaymentOfflineBankStatementItem[];
};

type PaymentOfflineRefund = {
  id?: string;
  offlineRefundNo?: string;
  offlineCollectionNo?: string;
  refundOrderNo?: string;
  payOrderNo?: string;
  bizOrderNo?: string;
  channelCode?: string;
  refundAmount?: number;
  refundAccountName?: string;
  refundAccountNoMask?: string;
  refundBankName?: string;
  refundVoucherFileIds?: string;
  refundVoucherCount?: number;
  refundStatus?: string;
  refundStatusName?: string;
};

type FileUploadResult = {
  id?: string | number;
  fileName?: string;
};

type PaymentExceptionOrderStatus = {
  statusCode?: string;
  statusName?: string;
};

type PaymentExceptionOrderAction = {
  actionCode?: string;
  actionName?: string;
};

type PaymentNotificationRecord = {
  id?: string;
  notificationNo?: string;
  relatedOrderNo?: string;
  notificationType?: string;
  notificationTypeName?: string;
  targetUrl?: string;
  notifyStatus?: string;
  notifyStatusName?: string;
  retryTimes?: number;
  nextRetryTime?: string;
  responseCode?: string;
  responseMessage?: string;
  lastManualRetryReason?: string;
  lastManualRetryResult?: string;
  lastManualRetryOperatorName?: string;
};

type PaymentNotificationStatus = {
  statusCode?: string;
  statusName?: string;
};

type PaymentChannelBillDetail = {
  channelTradeNo?: string;
  tradeType?: string;
  tradeTypeName?: string;
  amount?: number | string;
  fee?: number | string;
  matchStatus?: string;
  matchStatusName?: string;
  matchedOrderNo?: string;
  matchMessage?: string;
};

type PaymentReconciliation = {
  id?: string;
  reconciliationNo?: string;
  channelCode?: string;
  billDate?: string;
  totalCount?: number;
  totalAmount?: number | string;
  totalFee?: number | string;
  matchStatus?: string;
  matchStatusName?: string;
  billFileName?: string;
  fileDigest?: string;
  importerName?: string;
  importTime?: string;
  reconcileResult?: string;
  details?: PaymentChannelBillDetail[];
};

type PaymentReconciliationStatus = {
  statusCode?: string;
  statusName?: string;
};

type PaymentChannelBillSource = {
  id?: string;
  channelCode?: string;
  fetchMode?: string;
  fetchModeName?: string;
  endpoint?: string;
  enabled?: number;
};

type PaymentChannelBillFetchBatch = {
  id?: string;
  batchNo?: string;
  reconciliationNo?: string;
  channelCode?: string;
  fetchMode?: string;
  fetchModeName?: string;
  billDate?: string;
  totalCount?: number;
  fetchStatus?: string;
  fetchStatusName?: string;
  fetchResult?: string;
};

type PaymentDifference = {
  id?: string;
  differenceNo?: string;
  reconciliationNo?: string;
  channelCode?: string;
  billDate?: string;
  relatedOrderNo?: string;
  differenceType?: string;
  differenceTypeName?: string;
  differenceAmount?: number | string;
  processStatus?: string;
  processStatusName?: string;
  processAction?: string;
  processReason?: string;
  processResult?: string;
  processEvidence?: string;
  processorName?: string;
  processTime?: string;
};

type PaymentDifferenceStatus = {
  statusCode?: string;
  statusName?: string;
};

type PaymentDifferenceAction = {
  actionCode?: string;
  actionName?: string;
};

type PaymentSettlementSummary = {
  id?: string;
  settlementDate?: string;
  appCode?: string;
  subjectName?: string;
  channelCode?: string;
  tradeAmount?: number | string;
  refundAmount?: number | string;
  feeAmount?: number | string;
  netAmount?: number | string;
  tradeCount?: number;
  refundCount?: number;
  unresolvedDifferenceCount?: number;
  unresolvedDifferenceAmount?: number | string;
  status?: string;
  statusName?: string;
  voidReason?: string;
};

function expectMoneyCents(value: number | string | undefined, expected: number) {
  expect(String(value ?? '')).toBe(String(expected));
}

type FileRecord = {
  id?: string;
  bizType?: string;
  purpose?: string;
  storageType?: string;
};

type PaymentOperationAudit = {
  id?: string;
  operatorName?: string;
  operationAction?: string;
  resourceType?: string;
  resourceId?: string;
  operationResult?: string;
  operationTime?: string;
  createTime?: string;
};

type CashierSession = {
  cashierConfigId?: string;
  display?: {
    title?: string;
  };
  order?: {
    businessOrderId?: string;
    bizOrderNo?: string;
    status?: string;
  };
  methods?: Array<{
    methodCode?: string;
    methodName?: string;
    categoryName?: string;
  }>;
};

type CashierPayResult = {
  payOrderNo?: string;
  flowNo?: string;
  channelCode?: string;
  status?: string;
  material?: {
    materialType?: string;
    htmlForm?: string;
    redirectUrl?: string;
    accountName?: string;
    accountNo?: string;
    bankName?: string;
    transferRemark?: string;
  };
};

type PaymentTaskDispatchResult = {
  scannedCount?: number;
  successCount?: number;
  skippedCount?: number;
  failedCount?: number;
};

type MenuNode = {
  menuName?: string;
  children?: MenuNode[];
};

const paymentEndpoints = [
  'applications',
  'enterprise-subjects',
  'channel-contracts',
  'channels',
  'methods',
  'cashier-configs',
  'business-orders',
  'payment-orders',
  'refund-orders',
  'transaction-flows',
  'exception-orders',
  'notification-records',
  'reconciliations',
  'differences',
  'settlement-summaries',
  'operation-audits',
];

test.setTimeout(90 * 1000);

async function login(page: Page) {
  await page.goto('/#/login');
  const loginData = await page.evaluate(async () => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'admin',
        password: 'admin123',
        tenantId: '1',
        tenantCode: 'default',
        realm: 'INTERNAL',
        actorType: 'INTERNAL_USER',
        partyType: 'INTERNAL_ORG',
        appCode: 'internal-admin',
      }),
    });
    const body = await response.json();
    if (!response.ok || !(body.success || body.code === 200) || !body.data?.accessToken) {
      throw new Error(`登录失败：${JSON.stringify(body)}`);
    }
    return body.data;
  });
  await page.evaluate((data) => {
    const userInfo = {
      ...data,
      tenantId: data.tenantId || '1',
      tenantCode: data.tenantCode || 'default',
      tenantName: data.tenantName || '芒果集团',
      realm: data.realm || 'INTERNAL',
      actorType: data.actorType || 'INTERNAL_USER',
      partyType: data.partyType || 'INTERNAL_ORG',
      partyId: data.partyId || '1',
      appCode: data.appCode || 'internal-admin',
    };
    sessionStorage.setItem('MANGO_TOKEN', data.accessToken);
    sessionStorage.setItem('MANGO_REFRESH_TOKEN', data.refreshToken || '');
    sessionStorage.setItem('MANGO_TOKEN_EXPIRES_AT', String(Date.now() + Number(data.expiresIn || 7200) * 1000));
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
    sessionStorage.setItem('tenantId', String(userInfo.tenantId));
    document.cookie = `MANGO_TOKEN=${encodeURIComponent(data.accessToken)}; path=/; SameSite=Lax`;
  }, loginData);
  await page.goto('/#/home');
  await expect(page).toHaveURL(/#\/home$/, { timeout: 15000 });
}

async function apiHeaders(page: Page) {
  return page.evaluate(() => {
    const token = sessionStorage.getItem('MANGO_TOKEN') || '';
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
    const tenantId = String(userInfo?.tenantId || '1');
    return {
      Authorization: token ? `Bearer ${token}` : '',
      'TENANT-ID': tenantId,
      'X-Mango-Tenant-Id': tenantId,
    };
  });
}

async function expectBusinessOk<T>(response: APIResponse) {
  const body = await response.json() as ApiBody<T>;
  expect(response.status(), body.msg || JSON.stringify(body)).toBe(200);
  expect(body.success || body.code === 200, body.msg || '业务响应失败').toBeTruthy();
  return body;
}

async function expectBusinessError<T>(response: APIResponse) {
  expect(response.status()).toBe(200);
  const body = await response.json() as ApiBody<T>;
  expect(body.success || body.code === 200, '业务响应不应成功').toBeFalsy();
  return body;
}

async function createMangoPayScenarioControl(
  page: Page,
  headers: Record<string, string>,
  options: {
    scenarioType: MangoPayScenarioType;
    scenarioCode?: string;
    contractId?: number;
    effectiveCount?: number;
    remark?: string;
  },
) {
  const response = await page.request.post('/api/payment/mango-pay/virtual/scenario-controls', {
    headers,
    data: {
      channelCode: 'MANGO_PAY',
      contractId: options.contractId ?? 331001,
      scenarioType: options.scenarioType,
      scenarioCode: options.scenarioCode,
      effectiveCount: options.effectiveCount ?? 1,
      remark: options.remark,
    },
  });
  return expectBusinessOk<string | number>(response);
}

async function openCashierPage(page: Page, cashierConfigId: string | number, businessOrderId: string | number) {
  const sessionPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/session'));
  await page.goto(`/#/payment/cashier-configs/${cashierConfigId}/cashier?businessOrderId=${businessOrderId}`);
  await expect(page).toHaveURL(new RegExp(`#\\/payment\\/cashier-configs\\/${cashierConfigId}\\/cashier`), { timeout: 15000 });
  await expect(page.locator('.cashier-page')).toBeVisible({ timeout: 15000 });
  return expectBusinessOk<CashierSession>(await sessionPromise);
}

function dialog(page: Page) {
  return page.getByRole('dialog').last();
}

function formItem(page: Page, label: string) {
  return dialog(page).locator('.el-form-item').filter({ has: page.locator('.el-form-item__label', { hasText: label }) }).first();
}

async function fillInput(page: Page, label: string, value: string) {
  await formItem(page, label).locator('input').first().fill(value);
}

async function fillTextarea(page: Page, label: string, value: string) {
  await formItem(page, label).locator('textarea').first().fill(value);
}

async function fillNumber(page: Page, label: string, value: string) {
  const input = formItem(page, label).locator('input').first();
  await input.fill(value);
  await input.press('Tab');
}

async function chooseSelect(page: Page, label: string, optionText: string) {
  const dropdown = await openSelect(page, label);
  await clickVisibleOption(dropdown, optionText);
  await closeSelectDropdown(page);
}

async function searchAndChooseSelect(page: Page, label: string, keyword: string, optionText: string) {
  const item = formItem(page, label);
  const input = item.locator('input').first();
  await input.fill(keyword);
  const dropdown = page.locator('.el-select-dropdown:visible').last();
  await expect(dropdown).toBeVisible({ timeout: 10000 });
  await clickVisibleOption(dropdown, optionText);
  await closeSelectDropdown(page);
}

async function chooseMultiSelect(page: Page, label: string, optionText: string) {
  const dropdown = await openSelect(page, label);
  await clickVisibleOption(dropdown, optionText);
  await closeSelectDropdown(page);
}

async function chooseMultiSelectOptions(page: Page, label: string, optionTexts: string[]) {
  const dropdown = await openSelect(page, label);
  for (const optionText of optionTexts) {
    await clickVisibleOption(dropdown, optionText);
    await expect(formItem(page, label)).toContainText(optionText, { timeout: 10000 });
  }
  await closeSelectDropdown(page);
}

async function openSelect(page: Page, label: string): Promise<Locator> {
  const item = formItem(page, label);
  const select = item.locator('.el-select').first();
  await expect(select).toBeVisible({ timeout: 10000 });
  const listboxId = await select.evaluate((element: Element) => {
    const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
    (target as HTMLElement).click();
    return target.getAttribute('aria-controls') || '';
  });
  const dropdown = page.locator(`[id="${listboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
  await expect(dropdown).toBeVisible({ timeout: 10000 });
  return dropdown;
}

async function clickVisibleOption(dropdown: Locator, optionText: string) {
  const option = dropdown.locator('.el-select-dropdown__item').filter({ hasText: optionText }).first();
  await expect(option).toBeVisible({ timeout: 10000 });
  await option.evaluate((element: Element) => (element as HTMLElement).click());
}

async function closeSelectDropdown(page: Page) {
  await dialog(page).locator('.el-dialog__header').click({ force: true });
}

async function chooseRadio(page: Page, label: string, optionText: string) {
  await formItem(page, label).getByText(optionText, { exact: true }).click();
}

async function setSwitch(page: Page, label: string, enabled: boolean) {
  const switchInput = formItem(page, label).locator('.el-switch').first();
  const checked = await switchInput.evaluate(element => element.classList.contains('is-checked'));
  if (checked !== enabled) {
    await switchInput.click();
  }
}

async function openPaymentPage(page: Page, path: string, heading: string) {
  await page.goto('/#/home');
  await page.getByRole('button', { name: '支付中心' }).click();
  await expect(page.getByText('应用接入')).toBeVisible({ timeout: 10000 });
  await page.goto(path);
  await expect(page.getByRole('heading', { name: heading })).toBeVisible({ timeout: 10000 });
}

async function waitForPaymentTableIdle(page: Page) {
  const table = page.locator('.payment-table').first();
  await expect(table).toBeVisible({ timeout: 10000 });
  await expect(table.locator('.el-loading-mask')).toHaveCount(0, { timeout: 10000 });
}

async function paymentTableRow(page: Page, text: string) {
  await waitForPaymentTableIdle(page);
  const row = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: text }).first();
  await expect(row).toBeVisible({ timeout: 10000 });
  return row;
}

async function clickPaymentTableRowButton(page: Page, rowText: string, buttonName: string) {
  const row = await paymentTableRow(page, rowText);
  const button = row.getByRole('button', { name: buttonName });
  await expect(button).toBeVisible({ timeout: 10000 });
  await button.click();
}

async function createApplicationByUi(page: Page, data: {
  name: string;
  status: '启用' | '停用';
  demo: boolean;
  payloadEncrypt: boolean;
}): Promise<PaymentApplicationSaveResult> {
  await openPaymentPage(page, '/#/payment/applications', '应用管理');
  await page.getByRole('button', { name: '新增' }).click();
  await expect(dialog(page).getByText('新增应用管理')).toBeVisible();

  await fillInput(page, '应用名称', data.name);
  await setSwitch(page, '示例应用', data.demo);
  await chooseRadio(page, '状态', data.status);

  await setSwitch(page, '报文加密', data.payloadEncrypt);
  await setSwitch(page, 'IP 白名单', true);
  await fillTextarea(page, '允许来源', '127.0.0.1\n10.10.0.0/16');

  await fillInput(page, '通知重试策略', '1m,5m,15m,1h');

  const [createResponse] = await Promise.all([
    page.waitForResponse(response => response.url().includes('/api/payment/applications') && response.request().method() === 'POST'),
    dialog(page).getByRole('button', { name: '保存' }).click(),
  ]);
  const createBody = await expectBusinessOk<PaymentApplicationSaveResult>(createResponse);
  await expect(page.locator('.el-message').filter({ hasText: '已新增' }).last()).toBeVisible({ timeout: 10000 });
  if (data.payloadEncrypt) {
    await expect(dialog(page).getByRole('heading', { name: '应用密钥' })).toBeVisible({ timeout: 10000 });
    await expect(dialog(page).getByText('应用密钥仅展示一次，请交给业务系统后妥善保存。')).toBeVisible();
    expect(createBody.data?.secretGenerated).toBe(1);
    expect(createBody.data?.appId).toMatch(/^app_/);
    expect(createBody.data?.appSecret).toBeTruthy();
    await dialog(page).getByRole('button', { name: '我已保存' }).click();
  } else {
    expect(createBody.data?.secretGenerated).toBe(0);
    expect(createBody.data?.appSecret).toBeFalsy();
  }
  await expect(dialog(page)).toBeHidden({ timeout: 10000 });
  return createBody.data || {};
}

async function createCashierByUi(page: Page, data: {
  appName: string;
  cashierName: string;
  subjectNames: string[];
  methodNames: string[];
  defaultMethodName: string;
  resultReturnUrl: string;
}) {
  await openPaymentPage(page, '/#/payment/cashier-configs', '收银台');
  await page.getByRole('button', { name: '新增' }).click();
  await expect(dialog(page).getByText('新增收银台')).toBeVisible();

  await chooseSelect(page, '应用', data.appName);
  await fillInput(page, '收银台名称', data.cashierName);
  await chooseMultiSelectOptions(page, '企业主体', data.subjectNames);

  await chooseMultiSelectOptions(page, '可见方式', data.methodNames);
  await chooseSelect(page, '默认方式', data.defaultMethodName);
  await fillInput(page, '结果跳转', data.resultReturnUrl);

  const [createResponse] = await Promise.all([
    page.waitForResponse(response =>
      response.url().includes('/api/payment/cashier-configs')
      && !response.url().includes('/api/payment/cashier-configs/page')
      && response.request().method() === 'POST'
    ),
    dialog(page).getByRole('button', { name: '保存' }).click(),
  ]);
  await expectBusinessOk(createResponse);
  await expect(page.locator('.el-message').filter({ hasText: '已新增' }).last()).toBeVisible({ timeout: 10000 });
  await expect(page.getByRole('dialog', { name: '新增收银台' })).toBeHidden({ timeout: 10000 });

  await clickPaymentTableRowButton(page, data.cashierName, '装修');
  await expect(dialog(page).getByRole('heading', { name: '收银台装修' })).toBeVisible({ timeout: 10000 });
  const logoFileId = await uploadCashierLogo(page);
  await fillInput(page, '辅助说明', '请确认订单金额后选择支付方式');
  await fillTextarea(page, '帮助文案', '支付遇到问题请联系业务客服');

  const [decorationResponse] = await Promise.all([
    page.waitForResponse(response =>
      response.url().includes('/api/payment/cashier-configs')
      && !response.url().includes('/api/payment/cashier-configs/page')
      && response.request().method() === 'PUT'
    ),
    dialog(page).getByRole('button', { name: '保存装修' }).click(),
  ]);
  await expectBusinessOk(decorationResponse);
  await expect(page.locator('.el-message').filter({ hasText: '收银台装修已保存' }).last()).toBeVisible({ timeout: 10000 });
  await expect(page.getByRole('dialog', { name: '收银台装修' })).toBeHidden({ timeout: 10000 });
  return { logoFileId };
}

async function uploadCashierLogo(page: Page) {
  const logoUpload = formItem(page, 'Logo 文件').locator('input[type="file"]').first();
  const [uploadResponse] = await Promise.all([
    page.waitForResponse(response =>
      response.url().includes('/api/file/files')
      && !response.url().includes('/api/file/files/')
      && response.request().method() === 'POST'
    ),
    logoUpload.setInputFiles({
      name: `cashier-logo-${Date.now()}.png`,
      mimeType: 'image/png',
      buffer: Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAFgwJ/lcQ30QAAAABJRU5ErkJggg==',
        'base64',
      ),
    }),
  ]);
  const body = await expectBusinessOk<FileRecord>(uploadResponse);
  expect(body.data?.id).toBeTruthy();
  expect(body.data?.bizType).toBe('payment-cashier-logo');
  expect(body.data?.purpose).toBe('payment-cashier-logo');
  await expect(formItem(page, 'Logo 文件').locator('.el-upload-list__item')).toHaveCount(1, { timeout: 10000 });
  return String(body.data?.id || '');
}

async function uploadContractFile(page: Page, headers: Record<string, string>) {
  const response = await page.request.post('/api/file/files', {
    headers,
    multipart: {
      file: {
        name: `channel-cert-${Date.now()}.txt`,
        mimeType: 'text/plain',
        buffer: Buffer.from('payment-channel-contract-certificate'),
      },
      purpose: 'payment-channel-contract',
      accessLevel: 'PRIVATE',
      bizType: 'payment-channel-contract',
    },
  });
  const body = await expectBusinessOk<FileRecord>(response);
  expect(body.data?.id).toBeTruthy();
  expect(body.data?.bizType).toBe('payment-channel-contract');
  expect(body.data?.purpose).toBe('payment-channel-contract');
  return String(body.data?.id || '');
}

async function findApplicationByName(page: Page, headers: Record<string, string>, appName: string) {
  const response = await page.request.get('/api/payment/applications/page', {
    headers,
    params: { page: '1', size: '10', keyword: appName },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.appName === appName) as PaymentApplication | undefined;
}

async function findLatestAudit(page: Page, headers: Record<string, string>, appId: string, operationResult?: string) {
  const response = await page.request.get('/api/payment/operation-audits/page', {
    headers,
    params: { page: '1', size: '50', keyword: 'DELETE_APPLICATION' },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item =>
    item.resourceId === appId && (!operationResult || item.operationResult === operationResult)
  ) as PaymentOperationAudit | undefined;
}

async function findLatestPaymentAudit(page: Page, headers: Record<string, string>, options: {
  action: string;
  resourceType: string;
  resourceId: string;
  operationResult?: string;
}) {
  const response = await page.request.get('/api/payment/operation-audits/page', {
    headers,
    params: { page: '1', size: '50', keyword: options.action },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item =>
    item.operationAction === options.action
    && item.resourceType === options.resourceType
    && item.resourceId === options.resourceId
    && (!options.operationResult || item.operationResult === options.operationResult)
  ) as PaymentOperationAudit | undefined;
}

async function findExceptionOrderByRelatedNo(
  page: Page,
  headers: Record<string, string>,
  relatedOrderNo: string,
  statusCode = 'PENDING',
) {
  const response = await page.request.get('/api/payment/exception-orders/page', {
    headers,
    params: { page: '1', size: '10', keyword: relatedOrderNo, statusCode },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item =>
    (item as PaymentExceptionOrder).relatedOrderNo === relatedOrderNo
  ) as PaymentExceptionOrder | undefined;
}

async function findPaymentOrderByNo(page: Page, headers: Record<string, string>, payOrderNo: string) {
  const response = await page.request.get('/api/payment/payment-orders/page', {
    headers,
    params: { page: '1', size: '10', keyword: payOrderNo },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.payOrderNo === payOrderNo) as PaymentOrder | undefined;
}

function movePaymentSuccessTime(payOrderNo: string, billDate: string, time = '10:30:00') {
  mysqlExec(`
    UPDATE payment_order
    SET pay_time = '${billDate} ${time}',
        updated_at = NOW()
    WHERE tenant_id = 1
      AND pay_order_no = '${sqlValue(payOrderNo)}'
      AND status = 'SUCCESS';
  `);
}

function moveHistoricalReconciliationE2eOrdersOutOfBillDate(bizOrderPrefix: string, billDate: string, currentPayOrderNo: string) {
  mysqlExec(`
    UPDATE payment_order po
    JOIN payment_business_order bo
      ON bo.id = po.business_order_id
     AND bo.tenant_id = po.tenant_id
    SET po.pay_time = DATE_ADD('2026-04-01 00:00:00', INTERVAL MOD(po.id, 20) DAY),
        po.updated_at = NOW()
    WHERE po.tenant_id = 1
      AND po.channel_code = 'MANGO_PAY'
      AND po.status = 'SUCCESS'
      AND po.success_flag = 1
      AND po.channel_trade_no IS NOT NULL
      AND bo.biz_order_no LIKE '${sqlValue(bizOrderPrefix)}%'
      AND po.pay_order_no <> '${sqlValue(currentPayOrderNo)}'
      AND po.pay_time >= '${billDate}'
      AND po.pay_time < DATE_ADD('${billDate}', INTERVAL 1 DAY);
  `);
}

function moveHistoricalSettlementE2eRecordsOutOfBillDate(
  billDate: string,
  currentPayOrderNo: string,
  currentReconciliationNo: string,
) {
  moveHistoricalReconciliationE2eOrdersOutOfBillDate('SETTLE-BO-', billDate, currentPayOrderNo);
  mysqlExec(`
    UPDATE payment_difference d
    JOIN payment_reconciliation r
      ON r.id = d.reconciliation_id
     AND r.tenant_id = d.tenant_id
    SET d.del_flag = 1,
        d.updated_at = NOW()
    WHERE d.tenant_id = 1
      AND r.bill_date = '${billDate}'
      AND r.channel_code = 'MANGO_PAY'
      AND r.reconciliation_no LIKE 'SETTLE-RC-%'
      AND r.reconciliation_no <> '${sqlValue(currentReconciliationNo)}';

    UPDATE payment_channel_bill_detail bd
    LEFT JOIN payment_reconciliation r
      ON r.id = bd.reconciliation_id
     AND r.tenant_id = bd.tenant_id
    SET bd.del_flag = 1,
        bd.updated_at = NOW()
    WHERE bd.tenant_id = 1
      AND bd.bill_date = '${billDate}'
      AND bd.channel_code = 'MANGO_PAY'
      AND (
        bd.batch_no LIKE 'SETTLE-RC-%'
        OR bd.channel_trade_no LIKE 'SETTLE-CT-%'
        OR bd.channel_trade_no LIKE 'SETTLE-CR-%'
        OR r.reconciliation_no LIKE 'SETTLE-RC-%'
      )
      AND COALESCE(r.reconciliation_no, bd.batch_no, '') <> '${sqlValue(currentReconciliationNo)}';

    UPDATE payment_reconciliation
    SET del_flag = 1,
        updated_at = NOW()
    WHERE tenant_id = 1
      AND bill_date = '${billDate}'
      AND channel_code = 'MANGO_PAY'
      AND reconciliation_no LIKE 'SETTLE-RC-%'
      AND reconciliation_no <> '${sqlValue(currentReconciliationNo)}';
  `);
}

function disableHistoricalPaymentRouteE2eRules(currentRuleCode: string) {
  mysqlExec(`
    UPDATE payment_method_route_rule_item rri
    JOIN payment_method_route_rule rr
      ON rr.id = rri.rule_id
     AND rr.tenant_id = rri.tenant_id
    SET rri.del_flag = 1,
        rri.status = 0,
        rri.updated_at = NOW()
    WHERE rri.tenant_id = 1
      AND rr.rule_code LIKE 'E2E_WECHAT_ROUTE_%'
      AND rr.rule_code <> '${sqlValue(currentRuleCode)}';

    UPDATE payment_method_route_rule
    SET del_flag = 1,
        status = 0,
        updated_at = NOW()
    WHERE tenant_id = 1
      AND rule_code LIKE 'E2E_WECHAT_ROUTE_%'
      AND rule_code <> '${sqlValue(currentRuleCode)}';
  `);
}

async function findReconciliationByNo(page: Page, headers: Record<string, string>, reconciliationNo: string) {
  const response = await page.request.get('/api/payment/reconciliations/page', {
    headers,
    params: { page: '1', size: '10', keyword: reconciliationNo },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.reconciliationNo === reconciliationNo) as PaymentReconciliation | undefined;
}

async function findBusinessOrderByNo(page: Page, headers: Record<string, string>, bizOrderNo: string) {
  const response = await page.request.get('/api/payment/business-orders/page', {
    headers,
    params: { page: '1', size: '10', keyword: bizOrderNo },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.bizOrderNo === bizOrderNo) as PaymentBusinessOrder | undefined;
}

async function findTransactionFlowByPayOrderNo(page: Page, headers: Record<string, string>, payOrderNo: string) {
  const response = await page.request.get('/api/payment/transaction-flows/page', {
    headers,
    params: { page: '1', size: '10', keyword: payOrderNo },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.payOrderNo === payOrderNo) as PaymentTransactionFlow | undefined;
}

async function findRefundSuccessFlow(page: Page, headers: Record<string, string>, refundOrderNo: string) {
  const response = await page.request.get('/api/payment/transaction-flows/page', {
    headers,
    params: { page: '1', size: '10', keyword: refundOrderNo },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item =>
    item.refundOrderNo === refundOrderNo && item.flowType === 'REFUND_SUCCESS'
  ) as PaymentTransactionFlow | undefined;
}

async function findOfflineCollectionByPayOrderNo(page: Page, headers: Record<string, string>, payOrderNo: string) {
  const response = await page.request.get('/api/payment/offline-collections/page', {
    headers,
    params: { page: '1', size: '10', keyword: payOrderNo },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item =>
    (item as PaymentOfflineCollection).payOrderNo === payOrderNo
  ) as PaymentOfflineCollection | undefined;
}

async function findOfflineRefundByNo(page: Page, headers: Record<string, string>, offlineRefundNo: string) {
  const response = await page.request.get('/api/payment/offline-refunds/page', {
    headers,
    params: { page: '1', size: '10', keyword: offlineRefundNo },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item =>
    (item as PaymentOfflineRefund).offlineRefundNo === offlineRefundNo
  ) as PaymentOfflineRefund | undefined;
}

async function uploadPaymentEvidenceFile(
  page: Page,
  headers: Record<string, string>,
  options: { name: string; content: string; purpose: string; bizType: string; bizId: string },
) {
  const response = await page.request.post('/api/file/files', {
    headers,
    multipart: {
      file: {
        name: options.name,
        mimeType: 'text/plain',
        buffer: Buffer.from(options.content, 'utf8'),
      },
      purpose: options.purpose,
      bizType: options.bizType,
      bizId: options.bizId,
    },
  });
  const body = await expectBusinessOk<FileUploadResult>(response);
  expect(body.data?.id).toBeTruthy();
  return String(body.data?.id);
}

function bankStatementWorkbookBuffer(row: {
  bankStatementNo: string;
  tradeTime: string;
  amountYuan: number;
  bankAccountNo: string;
  bankName: string;
  counterpartyName: string;
  counterpartyAccountNo: string;
  summary: string;
  remark: string;
}) {
  const workbook = XLSX.utils.book_new();
  const sheet = XLSX.utils.aoa_to_sheet([
    ['银行流水号', '交易时间', '收入金额', '收款账号', '开户行', '对方户名', '对方账号', '摘要', '备注'],
    [
      row.bankStatementNo,
      row.tradeTime,
      row.amountYuan,
      row.bankAccountNo,
      row.bankName,
      row.counterpartyName,
      row.counterpartyAccountNo,
      row.summary,
      row.remark,
    ],
  ]);
  XLSX.utils.book_append_sheet(workbook, sheet, '银行流水');
  return XLSX.write(workbook, { bookType: 'xlsx', type: 'buffer' }) as Buffer;
}

function findLatestChannelQueryRecord(payOrderNo: string) {
  const rows = mysqlQueryRows(`
    SELECT query_no, pay_order_no, before_status, channel_status, result_status, process_result
    FROM payment_channel_query_record
    WHERE tenant_id = 1
      AND pay_order_no = '${sqlValue(payOrderNo)}'
      AND del_flag = 0
    ORDER BY query_time DESC, id DESC
    LIMIT 1
  `);
  if (rows.length === 0) {
    return undefined;
  }
  const [queryNo, recordPayOrderNo, beforeStatus, channelStatus, resultStatus, processResult] = rows[0].split('\t');
  return {
    queryNo,
    payOrderNo: recordPayOrderNo,
    beforeStatus,
    channelStatus,
    resultStatus,
    processResult,
  } as PaymentChannelQueryRecord;
}

function findLatestRefundQueryRecord(refundOrderNo: string) {
  const rows = mysqlQueryRows(`
    SELECT query_no, refund_order_no, before_status, channel_status, result_status, process_result
    FROM payment_refund_query_record
    WHERE tenant_id = 1
      AND refund_order_no = '${sqlValue(refundOrderNo)}'
      AND del_flag = 0
    ORDER BY query_time DESC, id DESC
    LIMIT 1
  `);
  if (rows.length === 0) {
    return undefined;
  }
  const [queryNo, recordRefundOrderNo, beforeStatus, channelStatus, resultStatus, processResult] = rows[0].split('\t');
  return {
    queryNo,
    refundOrderNo: recordRefundOrderNo,
    beforeStatus,
    channelStatus,
    resultStatus,
    processResult,
  } as PaymentRefundQueryRecord;
}

async function findAvailableCashierConfig(page: Page, headers: Record<string, string>) {
  const response = await page.request.get('/api/payment/cashier-configs/page', {
    headers,
    params: { page: '1', size: '20', keyword: '' },
  });
  const body = await expectBusinessOk<PageData>(response);
  const configs = (body.data?.list || []) as PaymentCashierConfig[];
  const config = configs.find(item => item.id && item.cashierName && item.methodNames) || configs.find(item => item.id && item.cashierName);
  expect(config?.id, '应存在可预览的收银台').toBeTruthy();
  return config as PaymentCashierConfig;
}

async function findEnterpriseSubjectByName(page: Page, headers: Record<string, string>, subjectName: string) {
  const response = await page.request.get('/api/payment/enterprise-subjects/page', {
    headers,
    params: { page: '1', size: '10', keyword: subjectName },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.subjectName === subjectName) as PaymentEnterpriseSubject | undefined;
}

async function findChannelByName(page: Page, headers: Record<string, string>, channelName: string) {
  const response = await page.request.get('/api/payment/channels/page', {
    headers,
    params: { page: '1', size: '20', keyword: channelName },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.channelName === channelName || item.channelCode === 'MANGO_PAY') as PaymentChannel | undefined;
}

async function findChannelByCode(page: Page, headers: Record<string, string>, channelCode: string) {
  const response = await page.request.get('/api/payment/channels/page', {
    headers,
    params: { page: '1', size: '20', keyword: channelCode },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.channelCode === channelCode) as PaymentChannel | undefined;
}

async function unusedChannelCode(page: Page, headers: Record<string, string>) {
  for (const channelCode of ['LIANLIAN_PAY', 'WECHAT_PAY', 'ALIPAY']) {
    const channel = await findChannelByCode(page, headers, channelCode);
    if (!channel) {
      return channelCode;
    }
  }
  throw new Error('没有可用于支付通道 E2E 的未占用通道编码');
}

function channelCodeOptionText(channelCode: string) {
  if (channelCode === 'LIANLIAN_PAY') return '连连支付';
  if (channelCode === 'WECHAT_PAY') return '微信支付';
  if (channelCode === 'ALIPAY') return '支付宝';
  return channelCode;
}

async function findChannelCapability(page: Page, headers: Record<string, string>, channelId: string, methodCode: string) {
  const response = await page.request.get('/api/payment/channel-capabilities/page', {
    headers,
    params: { page: '1', size: '50', channelId, keyword: methodCode },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.methodCode === methodCode) as PaymentChannelCapability | undefined;
}

async function findChannelContractByCode(page: Page, headers: Record<string, string>, contractCode: string) {
  const response = await page.request.get('/api/payment/channel-contracts/page', {
    headers,
    params: { page: '1', size: '10', keyword: contractCode },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.contractCode === contractCode) as PaymentChannelContract | undefined;
}

async function findPaymentMethodByCode(page: Page, headers: Record<string, string>, methodCode: string) {
  const response = await page.request.get('/api/payment/methods/page', {
    headers,
    params: { page: '1', size: '10', keyword: methodCode },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.methodCode === methodCode) as PaymentMethod | undefined;
}

async function findRouteRuleByCode(page: Page, headers: Record<string, string>, ruleCode: string) {
  const response = await page.request.get('/api/payment/method-routes/page', {
    headers,
    params: { page: '1', size: '10', keyword: ruleCode },
  });
  const body = await expectBusinessOk<PageData>(response);
  return (body.data?.list || []).find(item => item.ruleCode === ruleCode) as PaymentMethodRouteRule | undefined;
}

function cashierDisplayTitle(config: PaymentCashierConfig) {
  return config.cashierName || '';
}

function workspaceEnv() {
  const envPath = resolve(__dirname, '../../../../../.mango/dev-workspace.env');
  const raw = readFileSync(envPath, 'utf8');
  const values: Record<string, string> = {};
  for (const line of raw.split(/\r?\n/)) {
    const match = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (!match) continue;
    values[match[1]] = match[2].replace(/^'(.*)'$/, '$1');
  }
  return values;
}

function mysqlExec(sql: string) {
  const env = workspaceEnv();
  const args = [
    `-h${env.MANGO_DB_HOST || '127.0.0.1'}`,
    `-P${env.MANGO_DB_PORT || '3306'}`,
    `-u${env.MANGO_DB_USERNAME || 'root'}`,
    env.MANGO_DB_NAME || 'mango',
    '-e',
    sql,
  ];
  if (env.MANGO_DB_PASSWORD) {
    args.splice(3, 0, `-p${env.MANGO_DB_PASSWORD}`);
  }
  execFileSync('mysql', args, { stdio: 'pipe' });
}

function mysqlQueryRows(sql: string) {
  const env = workspaceEnv();
  const args = [
    `-h${env.MANGO_DB_HOST || '127.0.0.1'}`,
    `-P${env.MANGO_DB_PORT || '3306'}`,
    `-u${env.MANGO_DB_USERNAME || 'root'}`,
    '--batch',
    '--raw',
    '--skip-column-names',
    env.MANGO_DB_NAME || 'mango',
    '-e',
    sql,
  ];
  if (env.MANGO_DB_PASSWORD) {
    args.splice(3, 0, `-p${env.MANGO_DB_PASSWORD}`);
  }
  return execFileSync('mysql', args, { stdio: 'pipe', encoding: 'utf8' })
    .split(/\r?\n/)
    .filter(line => line.length > 0);
}

function sqlValue(value: string) {
  return value.replace(/\\/g, '\\\\').replace(/'/g, "''");
}

async function startPaymentNotifyReceiver(ackBody = 'SUCCESS') {
  const notifications: PaymentOpenNotification[] = [];
  const server = createServer((request, response) => {
    const chunks: Buffer[] = [];
    request.on('data', chunk => chunks.push(Buffer.from(chunk)));
    request.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8');
      if (raw) {
        notifications.push(JSON.parse(raw) as PaymentOpenNotification);
      }
      response.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' });
      response.end(ackBody);
    });
  });
  await new Promise<void>((resolveServer, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      server.off('error', reject);
      resolveServer();
    });
  });
  const address = server.address();
  if (!address || typeof address === 'string') {
    throw new Error('通知接收服务启动失败');
  }
  return {
    url: `http://127.0.0.1:${address.port}/payment/notify`,
    notifications,
    waitFor: async (
      type: string,
      timeout = 10_000,
      matcher: (notification: PaymentOpenNotification) => boolean = () => true,
    ) => {
      const startedAt = Date.now();
      while (Date.now() - startedAt < timeout) {
        const notification = notifications.find(item => item.notificationType === type && matcher(item));
        if (notification) {
          return notification;
        }
        await new Promise(resolveTimer => setTimeout(resolveTimer, 100));
      }
      throw new Error(`未收到 ${type} 通知`);
    },
    close: async () => closeServer(server),
  };
}

async function startPaymentBillHttpSource(getItems: () => Array<Record<string, unknown>>) {
  const requests: string[] = [];
  const server = createServer((request, response) => {
    requests.push(request.url || '');
    response.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    response.end(JSON.stringify({ items: getItems() }));
  });
  await new Promise<void>((resolveServer, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      server.off('error', reject);
      resolveServer();
    });
  });
  const address = server.address();
  if (!address || typeof address === 'string') {
    throw new Error('账单 HTTP 服务启动失败');
  }
  return {
    url: `http://127.0.0.1:${address.port}/channel-bill`,
    requests,
    close: async () => closeServer(server),
  };
}

async function closeServer(server: Server) {
  if (!server.listening) {
    return;
  }
  await new Promise<void>((resolveServer, reject) => {
    server.close(error => (error ? reject(error) : resolveServer()));
  });
}

function prepareOpenApiPaymentAccess(appId: string, appSecret: string, cashierConfigId: number) {
  const routeRuleId = cashierConfigId + 1000;
  const routeRuleItemId = cashierConfigId + 1001;
  mysqlExec(`
    DELETE FROM payment_openapi_nonce
    WHERE tenant_id = 1 AND app_id = '${appId}';

    DELETE tf FROM payment_transaction_flow tf
    LEFT JOIN payment_refund_order ro
      ON ro.id = tf.refund_order_id AND ro.tenant_id = tf.tenant_id
    LEFT JOIN payment_order po
      ON po.id = COALESCE(tf.payment_order_id, ro.payment_order_id) AND po.tenant_id = tf.tenant_id
    LEFT JOIN payment_business_order bo
      ON bo.id = po.business_order_id AND bo.tenant_id = po.tenant_id
    WHERE tf.tenant_id = 1 AND bo.app_code = '${appId}';

    DELETE ro FROM payment_refund_order ro
    JOIN payment_order po
      ON po.id = ro.payment_order_id AND po.tenant_id = ro.tenant_id
    JOIN payment_business_order bo
      ON bo.id = po.business_order_id AND bo.tenant_id = po.tenant_id
    WHERE ro.tenant_id = 1 AND bo.app_code = '${appId}';

    DELETE po FROM payment_order po
    JOIN payment_business_order bo
      ON bo.id = po.business_order_id AND bo.tenant_id = po.tenant_id
    WHERE po.tenant_id = 1 AND bo.app_code = '${appId}';

    DELETE nr FROM payment_notification_record nr
    JOIN payment_order po
      ON po.pay_order_no = nr.related_order_no AND po.tenant_id = nr.tenant_id
    JOIN payment_business_order bo
      ON bo.id = po.business_order_id AND bo.tenant_id = po.tenant_id
    WHERE nr.tenant_id = 1 AND bo.app_code = '${appId}';

    DELETE nr FROM payment_notification_record nr
    JOIN payment_refund_order ro
      ON ro.refund_order_no = nr.related_order_no AND ro.tenant_id = nr.tenant_id
    JOIN payment_order po
      ON po.id = ro.payment_order_id AND po.tenant_id = ro.tenant_id
    JOIN payment_business_order bo
      ON bo.id = po.business_order_id AND bo.tenant_id = po.tenant_id
    WHERE nr.tenant_id = 1 AND bo.app_code = '${appId}';

    DELETE FROM payment_business_order
    WHERE tenant_id = 1 AND app_code = '${appId}';

    INSERT INTO payment_application
      (id, app_id, app_name, app_secret, secret_configured, secret_version, secret_last_reset_time, sign_algorithm, ip_whitelist_enabled, ip_whitelist, payload_encrypt_enabled, notify_retry_policy, demo_app, status, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (319901, '${appId}', 'OpenAPI E2E 支付应用', '${appSecret}', 1, 1, NOW(), 'HMAC_SHA256', 0, NULL, 1, '1m,5m,15m', 0, 1, 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      app_name = VALUES(app_name),
      app_secret = VALUES(app_secret),
      secret_configured = VALUES(secret_configured),
      secret_version = VALUES(secret_version),
      secret_last_reset_time = VALUES(secret_last_reset_time),
      sign_algorithm = VALUES(sign_algorithm),
      ip_whitelist_enabled = VALUES(ip_whitelist_enabled),
      ip_whitelist = VALUES(ip_whitelist),
      payload_encrypt_enabled = VALUES(payload_encrypt_enabled),
      notify_retry_policy = VALUES(notify_retry_policy),
      demo_app = VALUES(demo_app),
      status = VALUES(status),
      updated_by = VALUES(updated_by),
      updated_at = NOW(),
      del_flag = 0;

    UPDATE payment_cashier_config
    SET default_cashier = 0,
        updated_at = NOW()
    WHERE tenant_id = 1 AND application_id = 319901 AND id <> ${cashierConfigId};

    INSERT INTO payment_cashier_config
      (id, cashier_name, application_id, default_cashier, enterprise_subject_ids, method_codes, default_method_code, method_display_order, result_return_url, display_config, status, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (${cashierConfigId}, 'OpenAPI E2E 默认收银台', 319901, 1, '320001,320002', 'PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT', 'PERSONAL_WECHAT_QR', 'PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT', 'https://business.example.test/payment/result', JSON_OBJECT('subtitle', '请确认订单金额后选择支付方式', 'helpText', '支付遇到问题请联系业务客服。'), 1, 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      cashier_name = VALUES(cashier_name),
      application_id = VALUES(application_id),
      default_cashier = VALUES(default_cashier),
      enterprise_subject_ids = VALUES(enterprise_subject_ids),
      method_codes = VALUES(method_codes),
      default_method_code = VALUES(default_method_code),
      method_display_order = VALUES(method_display_order),
      result_return_url = VALUES(result_return_url),
      display_config = VALUES(display_config),
      status = VALUES(status),
      updated_by = VALUES(updated_by),
      updated_at = NOW(),
      del_flag = 0;

    INSERT INTO payment_method_route_rule
      (id, rule_code, rule_name, app_id, subject_id, method_code, terminal_type, environment, route_mode, fallback_enabled, status, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (${routeRuleId}, 'OPENAPI_E2E_WECHAT_QR', 'OpenAPI E2E 微信扫码芒果支付路由', 319901, 320001, 'PERSONAL_WECHAT_QR', 'WEB', 'MANGO_PAY', 'PRIORITY', 1, 1, 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      rule_name = VALUES(rule_name),
      app_id = VALUES(app_id),
      subject_id = VALUES(subject_id),
      method_code = VALUES(method_code),
      terminal_type = VALUES(terminal_type),
      environment = VALUES(environment),
      route_mode = VALUES(route_mode),
      fallback_enabled = VALUES(fallback_enabled),
      status = VALUES(status),
      updated_by = VALUES(updated_by),
      updated_at = NOW(),
      del_flag = 0;

    INSERT INTO payment_method_route_rule_item
      (id, rule_id, contract_capability_id, priority, weight, min_amount, max_amount, status, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (${routeRuleItemId}, ${routeRuleId}, 333001, 10, 100, 1, 5000000, 1, 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      rule_id = VALUES(rule_id),
      contract_capability_id = VALUES(contract_capability_id),
      priority = VALUES(priority),
      weight = VALUES(weight),
      min_amount = VALUES(min_amount),
      max_amount = VALUES(max_amount),
      status = VALUES(status),
      updated_by = VALUES(updated_by),
      updated_at = NOW(),
      del_flag = 0;
  `);
}

function prepareCashierConfig(options: {
  id: number;
  cashierName: string;
  methodCodes: string;
  defaultMethodCode: string;
}) {
  mysqlExec(`
    INSERT INTO payment_cashier_config
      (id, cashier_name, application_id, default_cashier, enterprise_subject_ids, method_codes, default_method_code, method_display_order, result_return_url, display_config, status, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (${options.id}, '${sqlValue(options.cashierName)}', 310001, 0, '320001', '${options.methodCodes}', '${options.defaultMethodCode}', '${options.methodCodes}', 'https://business.example.test/payment/result', JSON_OBJECT('subtitle', '请确认订单金额后选择支付方式'), 1, 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      cashier_name = VALUES(cashier_name),
      application_id = VALUES(application_id),
      default_cashier = VALUES(default_cashier),
      enterprise_subject_ids = VALUES(enterprise_subject_ids),
      method_codes = VALUES(method_codes),
      default_method_code = VALUES(default_method_code),
      method_display_order = VALUES(method_display_order),
      result_return_url = VALUES(result_return_url),
      display_config = VALUES(display_config),
      status = VALUES(status),
      updated_by = VALUES(updated_by),
      updated_at = NOW(),
      del_flag = 0
  `);
}

function signPaymentOpenApi(method: string, path: string, body: string, appSecret: string, timestamp: string, nonce: string) {
  const bodyDigest = createHash('sha256').update(body).digest('hex');
  const canonical = [method.toUpperCase(), path, bodyDigest, timestamp, nonce].join('\n');
  return createHmac('sha256', appSecret).update(canonical).digest('base64');
}

function openApiHeaders(options: {
  appId: string;
  appSecret: string;
  method: string;
  path: string;
  body?: string;
  nonce: string;
  timestamp?: string;
}) {
  const timestamp = options.timestamp || Math.floor(Date.now() / 1000).toString();
  return {
    AppId: options.appId,
    tenantId: '1',
    timestamp,
    nonce: options.nonce,
    signature: signPaymentOpenApi(
      options.method,
      options.path,
      options.body || '',
      options.appSecret,
      timestamp,
      options.nonce,
    ),
    'Content-Type': 'application/json',
  };
}

function preparePayingBusinessOrder(bizOrderNo: string, notifyUrl = 'https://business.example.test/payment/notify') {
  const orderId = Number(`91${Date.now().toString().slice(-10)}`);
  const escapedNotifyUrl = sqlValue(notifyUrl);
  mysqlExec(`
    INSERT INTO payment_business_order
      (id, biz_order_no, app_code, title, subject_id, amount, paid_amount, refunded_amount, currency, status, expire_time, notify_url, return_url, extend_info, tenant_id, created_at, updated_at, del_flag)
    VALUES
      (${orderId}, '${bizOrderNo}', 'app_order_center', 'E2E 业务订单 ${bizOrderNo}', 320001, 128800, 0, 0, 'CNY', 'PAYING', DATE_ADD(NOW(), INTERVAL 30 MINUTE), '${escapedNotifyUrl}', 'https://business.example.test/payment/result', JSON_OBJECT('businessRefNo', '${bizOrderNo}', 'displayName', 'E2E 业务订单'), 1, NOW(), NOW(), 0)
    ON DUPLICATE KEY UPDATE
      status = 'PAYING',
      title = VALUES(title),
      amount = VALUES(amount),
      paid_amount = 0,
      refunded_amount = 0,
      expire_time = VALUES(expire_time),
      notify_url = VALUES(notify_url),
      return_url = VALUES(return_url),
      extend_info = VALUES(extend_info),
      updated_at = NOW(),
      del_flag = 0
  `);
  return orderId;
}

function expireBusinessOrder(bizOrderNo: string) {
  mysqlExec(`
    UPDATE payment_business_order
    SET expire_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE),
        updated_at = NOW()
    WHERE tenant_id = 1
      AND biz_order_no = '${bizOrderNo}'
      AND status = 'PAYING'
      AND del_flag = 0
  `);
}

function prioritizeProcessingPaymentOrder(payOrderNo: string) {
  mysqlExec(`
    UPDATE payment_order
    SET updated_at = DATE_SUB(NOW(), INTERVAL 10 YEAR),
        created_at = DATE_SUB(NOW(), INTERVAL 10 YEAR)
    WHERE tenant_id = 1
      AND pay_order_no = '${sqlValue(payOrderNo)}'
      AND status = 'PAYING'
  `);
}

function configureOrderCenterNotificationSecret() {
  mysqlExec(`
    UPDATE payment_application
    SET app_secret = 'order-center-e2e-secret',
        secret_configured = 1,
        secret_version = COALESCE(secret_version, 0) + 1,
        secret_last_reset_time = NOW(),
        sign_algorithm = 'HMAC_SHA256',
        payload_encrypt_enabled = 1,
        notify_retry_policy = '1m,5m,15m',
        updated_by = 1001,
        updated_at = NOW()
    WHERE tenant_id = 1
      AND app_id = 'app_order_center'
      AND del_flag = 0
  `);
}

function setMangoPayScenario(scenario: 'SUCCESS' | 'PAYING' | 'FAILED') {
  mysqlExec(`
    UPDATE payment_channel_contract
    SET config_values_json = JSON_SET(COALESCE(NULLIF(config_values_json, ''), '{}'), '$.mangoPayScenario', '${scenario}'),
        updated_at = NOW()
    WHERE id = 331001 AND tenant_id = 1
  `);
}

function setMangoPayRefundScenario(scenario: 'SUCCESS' | 'PROCESSING' | 'FAILED') {
  mysqlExec(`
    UPDATE payment_channel_contract
    SET config_values_json = JSON_SET(COALESCE(NULLIF(config_values_json, ''), '{}'), '$.mangoPayRefundScenario', '${scenario}'),
        updated_at = NOW()
    WHERE id = 331001 AND tenant_id = 1
  `);
}

function clearActiveMangoPayBillScenarioControls() {
  mysqlExec(`
    UPDATE payment_mango_pay_scenario_control
    SET status = 'CONSUMED',
        consumed_count = effective_count,
        consumed_at = NOW(),
        updated_by = 1001,
        updated_at = NOW()
    WHERE tenant_id = 1
      AND channel_code = 'MANGO_PAY'
      AND scenario_type = 'BILL'
      AND status = 'ACTIVE'
      AND del_flag = 0
  `);
}

function finishProcessingPayment(payOrderNo: string) {
  mysqlExec(`
    INSERT INTO payment_order_status_flow
      (id, order_type, order_id, order_no, from_status, to_status, trigger_source, trigger_no, operator_id, operator_name, happen_time, remark, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    SELECT
      CAST(CONCAT('94', RIGHT(REPLACE(UNIX_TIMESTAMP(NOW(3)), '.', ''), 10)) AS UNSIGNED),
      'PAYMENT_ORDER',
      po.id,
      po.pay_order_no,
      'PAYING',
      'SUCCESS',
      'CHANNEL_CALLBACK',
      po.pay_order_no,
      1001,
      'admin',
      NOW(),
      'E2E 通道回调推进支付成功',
      po.tenant_id,
      1001,
      NOW(),
      1001,
      NOW(),
      0
    FROM payment_order po
    WHERE po.tenant_id = 1 AND po.pay_order_no = '${payOrderNo}' AND po.status = 'PAYING'
      AND NOT EXISTS (
        SELECT 1
        FROM payment_order_status_flow f
        WHERE f.tenant_id = po.tenant_id
          AND f.order_type = 'PAYMENT_ORDER'
          AND f.order_id = po.id
          AND f.from_status = 'PAYING'
          AND f.to_status = 'SUCCESS'
      )
    LIMIT 1;

    INSERT INTO payment_transaction_flow
      (id, flow_no, business_order_id, payment_order_id, refund_order_id, flow_type, amount, tenant_id, created_at, updated_at)
    SELECT
      CAST(CONCAT('92', RIGHT(REPLACE(UNIX_TIMESTAMP(NOW(3)), '.', ''), 10)) AS UNSIGNED),
      CONCAT('FLOW', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), LPAD(FLOOR(RAND() * 900000) + 100000, 6, '0')),
      po.business_order_id,
      po.id,
      NULL,
      'PAY_SUCCESS',
      po.amount,
      po.tenant_id,
      NOW(),
      NOW()
    FROM payment_order po
    WHERE po.tenant_id = 1 AND po.pay_order_no = '${payOrderNo}' AND po.status = 'PAYING'
    LIMIT 1;

    UPDATE payment_business_order bo
    JOIN payment_order po ON po.business_order_id = bo.id AND po.tenant_id = bo.tenant_id
    SET bo.status = 'SUCCESS',
        bo.paid_amount = po.amount,
        bo.updated_at = NOW()
    WHERE po.tenant_id = 1 AND po.pay_order_no = '${payOrderNo}';

    UPDATE payment_order
    SET status = 'SUCCESS',
        success_flag = 1,
        pay_time = COALESCE(pay_time, NOW()),
        updated_at = NOW()
    WHERE tenant_id = 1 AND pay_order_no = '${payOrderNo}';
  `);
}

function createRefundOrderForPayment(payOrderNo: string, refundOrderNo: string, bizRefundNo: string) {
  const refundOrderId = Number(`93${Date.now().toString().slice(-10)}`);
  const flowId = refundOrderId + 1;
  mysqlExec(`
    INSERT INTO payment_refund_order
      (id, refund_order_no, biz_refund_no, payment_order_id, channel_refund_no, refund_amount, reason, status, refund_time, tenant_id, created_at, updated_at)
    SELECT
      ${refundOrderId},
      '${refundOrderNo}',
      '${bizRefundNo}',
      po.id,
      CONCAT('CRF-', '${refundOrderNo}'),
      38800,
      'E2E 退款申请',
      'REFUNDING',
      NULL,
      po.tenant_id,
      NOW(),
      NOW()
    FROM payment_order po
    WHERE po.tenant_id = 1 AND po.pay_order_no = '${payOrderNo}'
    ON DUPLICATE KEY UPDATE
      payment_order_id = VALUES(payment_order_id),
      channel_refund_no = VALUES(channel_refund_no),
      refund_amount = VALUES(refund_amount),
      reason = VALUES(reason),
      status = VALUES(status),
      updated_at = NOW();

    INSERT INTO payment_transaction_flow
      (id, flow_no, business_order_id, payment_order_id, refund_order_id, flow_type, amount, tenant_id, created_at, updated_at)
    SELECT
      ${flowId},
      CONCAT('RFLOW', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), LPAD(FLOOR(RAND() * 900000) + 100000, 6, '0')),
      po.business_order_id,
      po.id,
      ro.id,
      'ADJUST_NOTE',
      0,
      po.tenant_id,
      NOW(),
      NOW()
    FROM payment_order po
    JOIN payment_refund_order ro ON ro.payment_order_id = po.id AND ro.tenant_id = po.tenant_id
    WHERE po.tenant_id = 1 AND po.pay_order_no = '${payOrderNo}' AND ro.refund_order_no = '${refundOrderNo}'
    ON DUPLICATE KEY UPDATE
      amount = VALUES(amount);

    INSERT INTO payment_order_status_flow
      (id, order_type, order_id, order_no, from_status, to_status, trigger_source, trigger_no, operator_id, operator_name, happen_time, remark, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    SELECT
      ${refundOrderId + 2},
      'REFUND_ORDER',
      ro.id,
      ro.refund_order_no,
      NULL,
      'CREATED',
      'OPENAPI_REFUND',
      ro.biz_refund_no,
      1001,
      'admin',
      NOW(),
      'E2E 创建退款订单',
      ro.tenant_id,
      1001,
      NOW(),
      1001,
      NOW(),
      0
    FROM payment_refund_order ro
    WHERE ro.tenant_id = 1 AND ro.refund_order_no = '${refundOrderNo}'
      AND NOT EXISTS (
        SELECT 1
        FROM payment_order_status_flow f
        WHERE f.tenant_id = ro.tenant_id
          AND f.order_type = 'REFUND_ORDER'
          AND f.order_id = ro.id
          AND f.to_status = 'CREATED'
      )
    LIMIT 1;

    INSERT INTO payment_order_status_flow
      (id, order_type, order_id, order_no, from_status, to_status, trigger_source, trigger_no, operator_id, operator_name, happen_time, remark, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    SELECT
      ${refundOrderId + 3},
      'REFUND_ORDER',
      ro.id,
      ro.refund_order_no,
      'CREATED',
      'REFUNDING',
      'OPENAPI_REFUND',
      ro.biz_refund_no,
      1001,
      'admin',
      NOW(),
      'E2E 退款请求已提交，等待通道回调、主动退款查询或对账补偿推进',
      ro.tenant_id,
      1001,
      NOW(),
      1001,
      NOW(),
      0
    FROM payment_refund_order ro
    WHERE ro.tenant_id = 1 AND ro.refund_order_no = '${refundOrderNo}'
      AND NOT EXISTS (
        SELECT 1
        FROM payment_order_status_flow f
        WHERE f.tenant_id = ro.tenant_id
          AND f.order_type = 'REFUND_ORDER'
          AND f.order_id = ro.id
          AND f.from_status = 'CREATED'
          AND f.to_status = 'REFUNDING'
      )
    LIMIT 1;
  `);
  return refundOrderId;
}

function prepareNotificationRecord(notificationNo: string, relatedOrderNo: string, status = 'FAILED') {
  const notificationRecordId = Number(`95${Date.now().toString().slice(-10)}`);
  const payload = {
    notifyNo: notificationNo,
    notificationType: 'PAYMENT_FAILED',
    tenantId: 1,
    appId: 'E2E_NOTIFY_APP',
    payOrderNo: relatedOrderNo,
    status: 'FAILED',
    notifyTime: new Date().toISOString(),
  };
  mysqlExec(`
    INSERT INTO payment_notification_record
      (id, notification_no, related_order_no, notification_type, target_url, notify_status, retry_times, next_retry_time, payload_json, response_code, response_message, tenant_id, created_at, updated_at, del_flag)
    VALUES
      (${notificationRecordId}, '${notificationNo}', '${relatedOrderNo}', 'PAYMENT_FAILED', 'https://merchant.example.com/payment/notify', '${status}', 2, DATE_ADD(NOW(), INTERVAL 5 MINUTE), '${sqlValue(JSON.stringify(payload))}', '500', 'E2E 业务系统临时不可用', 1, NOW(), NOW(), 0)
    ON DUPLICATE KEY UPDATE
      related_order_no = VALUES(related_order_no),
      notification_type = VALUES(notification_type),
      target_url = VALUES(target_url),
      notify_status = VALUES(notify_status),
      retry_times = VALUES(retry_times),
      next_retry_time = VALUES(next_retry_time),
      payload_json = VALUES(payload_json),
      response_code = VALUES(response_code),
      response_message = VALUES(response_message),
      last_manual_retry_time = NULL,
      last_manual_retry_reason = NULL,
      last_manual_retry_result = NULL,
      last_manual_retry_operator_id = NULL,
      last_manual_retry_operator_name = NULL,
      updated_at = NOW(),
      del_flag = 0
  `);
  return notificationRecordId;
}

function prepareNotificationRetryApplication(appId: string, retryPolicy = '1m,5m,15m') {
  mysqlExec(`
    INSERT INTO payment_application
      (id, app_id, app_name, app_secret, secret_configured, secret_version, secret_last_reset_time, sign_algorithm, ip_whitelist_enabled, ip_whitelist, payload_encrypt_enabled, notify_retry_policy, demo_app, status, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (${Number(`92${Date.now().toString().slice(-10)}`)}, '${sqlValue(appId)}', '通知重试 E2E 应用', 'notify-retry-e2e-secret', 1, 1, NOW(), 'HMAC_SHA256', 0, NULL, 0, '${sqlValue(retryPolicy)}', 0, 1, 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      app_name = VALUES(app_name),
      app_secret = VALUES(app_secret),
      secret_configured = VALUES(secret_configured),
      secret_version = VALUES(secret_version),
      secret_last_reset_time = VALUES(secret_last_reset_time),
      sign_algorithm = VALUES(sign_algorithm),
      notify_retry_policy = VALUES(notify_retry_policy),
      status = VALUES(status),
      updated_by = 1001,
      updated_at = NOW(),
      del_flag = 0
  `);
}

function prepareDueNotificationRecord(
  notificationNo: string,
  relatedOrderNo: string,
  targetUrl: string,
  options: {
    appId?: string;
    notifyStatus?: string;
    retryTimes?: number;
    responseCode?: string;
    responseMessage?: string;
  } = {},
) {
  const notificationRecordId = Number(`95${Date.now().toString().slice(-10)}`);
  const appId = options.appId || 'E2E_AUTO_NOTIFY_APP';
  const notifyStatus = options.notifyStatus || 'PENDING';
  const retryTimes = options.retryTimes ?? 0;
  const responseCode = options.responseCode == null ? 'NULL' : `'${sqlValue(options.responseCode)}'`;
  const responseMessage = options.responseMessage == null ? 'NULL' : `'${sqlValue(options.responseMessage)}'`;
  const payload: PaymentOpenNotification = {
    notifyNo: notificationNo,
    notificationType: 'PAYMENT_SUCCESS',
    tenantId: 1,
    appId,
    bizOrderNo: `BO-${relatedOrderNo}`,
    payOrderNo: relatedOrderNo,
    amount: 8800,
    currency: 'CNY',
    status: 'SUCCESS',
    methodCode: 'PERSONAL_WECHAT_QR',
    channelCode: 'MANGO_PAY',
    channelTradeNo: `MANGO_PAY-${relatedOrderNo}`,
    flowNo: `FLOW-${relatedOrderNo}`,
    eventTime: new Date().toISOString(),
    notifyTime: new Date().toISOString(),
    signAlgorithm: 'HMAC_SHA256',
    signature: 'E2E_AUTO_NOTIFY_SIGNATURE',
  };
  mysqlExec(`
    INSERT INTO payment_notification_record
      (id, notification_no, related_order_no, notification_type, target_url, notify_status, retry_times, scheduled_notify_time, next_retry_time, payload_json, response_code, response_message, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (${notificationRecordId}, '${sqlValue(notificationNo)}', '${sqlValue(relatedOrderNo)}', 'PAYMENT_SUCCESS', '${sqlValue(targetUrl)}', '${sqlValue(notifyStatus)}', ${retryTimes}, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), '${sqlValue(JSON.stringify(payload))}', ${responseCode}, ${responseMessage}, 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      related_order_no = VALUES(related_order_no),
      notification_type = VALUES(notification_type),
      target_url = VALUES(target_url),
      notify_status = VALUES(notify_status),
      retry_times = VALUES(retry_times),
      scheduled_notify_time = VALUES(scheduled_notify_time),
      next_retry_time = VALUES(next_retry_time),
      payload_json = VALUES(payload_json),
      response_code = NULL,
      response_message = NULL,
      updated_by = 1001,
      updated_at = NOW(),
      del_flag = 0
  `);
  return notificationRecordId;
}

function prepareSettlementScenario(suffix: string, billDate: string) {
  const base = Number(`96${Date.now().toString().slice(-9)}`);
  const businessOrderId = base;
  const paymentOrderId = base + 1;
  const refundOrderId = base + 2;
  const reconciliationId = base + 3;
  const paymentBillDetailId = base + 4;
  const refundBillDetailId = base + 5;
  const differenceId = base + 6;
  const bizOrderNo = `SETTLE-BO-${suffix}`;
  const payOrderNo = `SETTLE-PO-${suffix}`;
  const refundOrderNo = `SETTLE-RO-${suffix}`;
  const channelTradeNo = `SETTLE-CT-${suffix}`;
  const channelRefundNo = `SETTLE-CR-${suffix}`;
  const reconciliationNo = `SETTLE-RC-${suffix}`;
  const differenceNo = `SETTLE-DF-${suffix}`;
  mysqlExec(`
    DELETE FROM payment_settlement_summary
    WHERE tenant_id = 1
      AND settlement_date = '${billDate}'
      AND app_code = 'ORDER_CENTER'
      AND enterprise_subject_id = 320001
      AND channel_code = 'MANGO_PAY';

    INSERT INTO payment_business_order
      (id, biz_order_no, app_code, title, subject_id, amount, paid_amount, refunded_amount, currency, status, expire_time, notify_url, return_url, extend_info, tenant_id, created_at, updated_at, del_flag)
    VALUES
      (${businessOrderId}, '${bizOrderNo}', 'ORDER_CENTER', 'E2E 结算汇总订单 ${suffix}', 320001, 128800, 128800, 38800, 'CNY', 'SUCCESS', DATE_ADD(NOW(), INTERVAL 30 MINUTE), 'https://business.example.test/payment/notify', 'https://business.example.test/payment/result', JSON_OBJECT('settlementE2E', '${suffix}'), 1, NOW(), NOW(), 0)
    ON DUPLICATE KEY UPDATE
      title = VALUES(title),
      amount = VALUES(amount),
      paid_amount = VALUES(paid_amount),
      refunded_amount = VALUES(refunded_amount),
      status = VALUES(status),
      updated_at = NOW(),
      del_flag = 0;

    INSERT INTO payment_order
      (id, pay_order_no, business_order_id, cashier_config_id, channel_id, channel_code, channel_merchant_no, contract_id, contract_capability_id, route_rule_id, method_id, amount, status, channel_trade_no, success_flag, pay_time, expire_time, tenant_id, created_at, updated_at)
    VALUES
      (${paymentOrderId}, '${payOrderNo}', ${businessOrderId}, 350001, 330001, 'MANGO_PAY', 'MANGO_PAY_MERCHANT_001', 331001, 333001, NULL, 340001, 128800, 'SUCCESS', '${channelTradeNo}', 1, '${billDate} 10:00:00', DATE_ADD(NOW(), INTERVAL 30 MINUTE), 1, NOW(), NOW())
    ON DUPLICATE KEY UPDATE
      business_order_id = VALUES(business_order_id),
      amount = VALUES(amount),
      status = VALUES(status),
      channel_trade_no = VALUES(channel_trade_no),
      success_flag = VALUES(success_flag),
      pay_time = VALUES(pay_time),
      updated_at = NOW();

    INSERT INTO payment_refund_order
      (id, refund_order_no, biz_refund_no, payment_order_id, channel_refund_no, refund_amount, reason, status, refund_time, tenant_id, created_at, updated_at)
    VALUES
      (${refundOrderId}, '${refundOrderNo}', 'SETTLE-BR-${suffix}', ${paymentOrderId}, '${channelRefundNo}', 38800, 'E2E 结算退款', 'SUCCESS', '${billDate} 11:00:00', 1, NOW(), NOW())
    ON DUPLICATE KEY UPDATE
      payment_order_id = VALUES(payment_order_id),
      channel_refund_no = VALUES(channel_refund_no),
      refund_amount = VALUES(refund_amount),
      status = VALUES(status),
      refund_time = VALUES(refund_time),
      updated_at = NOW();

    INSERT INTO payment_reconciliation
      (id, reconciliation_no, channel_code, bill_date, total_count, total_amount, total_fee, match_status, bill_file_id, bill_file_name, file_digest, importer_id, importer_name, import_time, reconcile_result, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (${reconciliationId}, '${reconciliationNo}', 'MANGO_PAY', '${billDate}', 2, 167600, 260, 'DIFFERENCE', NULL, '${reconciliationNo}.csv', 'digest-${suffix}', 1001, 'admin', NOW(), 'E2E 结算汇总对账存在待处理差异', 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      total_count = VALUES(total_count),
      total_amount = VALUES(total_amount),
      total_fee = VALUES(total_fee),
      match_status = VALUES(match_status),
      file_digest = VALUES(file_digest),
      reconcile_result = VALUES(reconcile_result),
      updated_at = NOW(),
      del_flag = 0;

    INSERT INTO payment_channel_bill_detail
      (id, reconciliation_id, batch_no, channel_code, bill_date, channel_trade_no, trade_type, amount, fee, trade_time, match_status, matched_order_no, match_message, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (${paymentBillDetailId}, ${reconciliationId}, '${reconciliationNo}', 'MANGO_PAY', '${billDate}', '${channelTradeNo}', 'PAYMENT', 128800, 160, '${billDate} 10:00:00', 'MATCHED', '${payOrderNo}', 'E2E 支付账单匹配成功', 1, 1001, NOW(), 1001, NOW(), 0),
      (${refundBillDetailId}, ${reconciliationId}, '${reconciliationNo}', 'MANGO_PAY', '${billDate}', '${channelRefundNo}', 'REFUND', 38800, 100, '${billDate} 11:00:00', 'MATCHED', '${refundOrderNo}', 'E2E 退款账单匹配成功', 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      amount = VALUES(amount),
      fee = VALUES(fee),
      match_status = VALUES(match_status),
      matched_order_no = VALUES(matched_order_no),
      match_message = VALUES(match_message),
      updated_at = NOW(),
      del_flag = 0;

    INSERT INTO payment_difference
      (id, difference_no, reconciliation_id, related_order_no, difference_type, difference_amount, process_status, process_result, tenant_id, created_by, created_at, updated_by, updated_at, del_flag)
    VALUES
      (${differenceId}, '${differenceNo}', ${reconciliationId}, '${payOrderNo}', 'AMOUNT_MISMATCH', 100, 'PENDING', 'E2E 结算确认前阻断差异', 1, 1001, NOW(), 1001, NOW(), 0)
    ON DUPLICATE KEY UPDATE
      process_status = 'PENDING',
      process_action = NULL,
      process_reason = NULL,
      process_result = VALUES(process_result),
      processor_id = NULL,
      processor_name = NULL,
      process_time = NULL,
      updated_at = NOW(),
      del_flag = 0;
  `);
  return {
    billDate,
    differenceId: String(differenceId),
    payOrderNo,
    reconciliationNo,
  };
}

function collectMenuNames(nodes: MenuNode[]): string[] {
  return nodes.flatMap((node) => [
    node.menuName,
    ...collectMenuNames(node.children || []),
  ]).filter((name): name is string => Boolean(name));
}

function collectRuntimeErrors(page: Page, apiPathPrefix = '/api/payment') {
  const runtimeErrors: string[] = [];
  page.on('console', (message) => {
    if (message.type() === 'error') {
      runtimeErrors.push(message.text());
    }
  });
  page.on('pageerror', error => runtimeErrors.push(error.message));
  page.on('requestfailed', request => {
    if (request.url().includes(apiPathPrefix)) {
      runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
    }
  });
  return runtimeErrors;
}

async function findFirstPageRecord<T>(
  page: Page,
  headers: Record<string, string>,
  endpoint: string,
  keyword = '',
) {
  const response = await page.request.get(`/api/payment/${endpoint}/page`, {
    headers,
    params: { page: '1', size: '10', keyword },
  });
  const body = await expectBusinessOk<PageData>(response);
  const row = (body.data?.list || [])[0] as unknown as T | undefined;
  expect(row, `${endpoint} 应存在可验收数据`).toBeTruthy();
  return row as T;
}

async function searchPaymentTable(page: Page, endpoint: string, keyword: string, tableSelector: string) {
  const table = page.locator(tableSelector).first();
  const toolbarInput = page.locator(`${tableSelector.replace('__table', '__toolbar')} input`).first();
  await toolbarInput.fill(keyword);
  await Promise.all([
    page.waitForResponse(response => response.url().includes(`/api/payment/${endpoint}/page`)),
    page.getByRole('button', { name: '查询' }).click(),
  ]);
  await expect(table).toBeVisible({ timeout: 10000 });
  await expect(table.locator('.el-loading-mask')).toHaveCount(0, { timeout: 10000 });
  const row = table.locator('.el-table__body-wrapper tbody tr').filter({ hasText: keyword }).first();
  await expect(row).toBeVisible({ timeout: 10000 });
  return row;
}

async function expectPaymentListLayout(page: Page, config: {
  path: string;
  heading: string;
  rootClass: string;
  screenshotName: string;
}) {
  await openPaymentPage(page, config.path, config.heading);
  const root = page.locator(`.${config.rootClass}`).first();
  await expect(root.locator(`.${config.rootClass}__header`)).toBeVisible({ timeout: 10000 });
  await expect(root.locator(`.${config.rootClass}__toolbar`)).toBeVisible({ timeout: 10000 });
  await expect(root.locator(`.${config.rootClass}__toolbar .el-form-item`).first()).toBeVisible({ timeout: 10000 });
  await expect(root.locator(`.${config.rootClass}__table`)).toBeVisible({ timeout: 10000 });
  await expect(root.locator(`.${config.rootClass}__table .el-loading-mask`)).toHaveCount(0, { timeout: 10000 });
  await expect(root.locator(`.${config.rootClass}__pagination`)).toBeVisible({ timeout: 10000 });
  await page.screenshot({ path: `test-results/${config.screenshotName}`, fullPage: true });
}

async function expectNoTagText(scope: Locator, text: string | undefined, message: string) {
  if (!text || text === '-') return;
  await expect(scope.locator('.el-tag').filter({ hasText: text }), message).toHaveCount(0);
}

async function expectOnlySemanticTags(scope: Locator, allowed: RegExp[], message: string) {
  const texts = (await scope.locator('.el-tag').allTextContents()).map(text => text.trim()).filter(Boolean);
  for (const text of texts) {
    expect(allowed.some(pattern => pattern.test(text)), `${message}：${text}`).toBeTruthy();
  }
}

test.describe('支付中心 E2E', () => {
  test.describe.configure({ mode: 'serial' });
  test.skip(({ browserName }) => browserName !== 'chromium', '支付中心 E2E 使用共享测试库数据，仅在 Chromium 串行执行');

  test('支付中心搜索区、列表区和分页区样式结构一致', async ({ page }) => {
    const runtimeErrors = collectRuntimeErrors(page);
    await login(page);

    const pages = [
      {
        path: '/#/payment/business-orders',
        heading: '业务订单',
        rootClass: 'payment-business-orders',
        screenshotName: 'payment-ui-business-orders.png',
      },
      {
        path: '/#/payment/payment-orders',
        heading: '支付订单',
        rootClass: 'payment-orders',
        screenshotName: 'payment-ui-payment-orders.png',
      },
      {
        path: '/#/payment/refund-orders',
        heading: '退款订单',
        rootClass: 'payment-refund-orders',
        screenshotName: 'payment-ui-refund-orders.png',
      },
      {
        path: '/#/payment/reconciliations',
        heading: '对账管理',
        rootClass: 'payment-reconciliations',
        screenshotName: 'payment-ui-reconciliations.png',
      },
      {
        path: '/#/payment/operation-audits',
        heading: '操作审计',
        rootClass: 'payment-operation-audits',
        screenshotName: 'payment-ui-operation-audits.png',
      },
      {
        path: '/#/payment/settlement-summaries',
        heading: '结算汇总',
        rootClass: 'payment-settlement-summaries',
        screenshotName: 'payment-ui-settlement-summaries.png',
      },
      {
        path: '/#/payment/offline/collections',
        heading: '线下收款',
        rootClass: 'payment-offline-collections',
        screenshotName: 'payment-ui-offline-collections.png',
      },
      {
        path: '/#/payment/offline/refunds',
        heading: '线下退款订单',
        rootClass: 'payment-offline-refunds',
        screenshotName: 'payment-ui-offline-refunds.png',
      },
    ];

    for (const item of pages) {
      await expectPaymentListLayout(page, item);
    }

    expect(runtimeErrors).toEqual([]);
  });

  test('开放接口签名、防重放、创建查询和收银台入口真实可用', async ({ page }) => {
    await login(page);
    const adminHeaders = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const appId = 'app_openapi_e2e';
    const appSecret = `openapi-e2e-secret-${suffix}`;
    const cashierConfigId = 359901;
    const bizOrderNo = `OPENAPI-BO-${suffix}`;
    const createPath = '/openapi/pay/orders';
    prepareOpenApiPaymentAccess(appId, appSecret, cashierConfigId);
    const notifyReceiver = await startPaymentNotifyReceiver();

    try {
      const createPayload = {
        tenantId: 1,
        appId,
        bizOrderNo,
        title: `OpenAPI E2E 订单 ${suffix}`,
        amount: 128800,
        currency: 'CNY',
        expireMinutes: 30,
        notifyUrl: notifyReceiver.url,
        returnUrl: 'https://business.example.test/payment/result',
        extendInfo: {
          scenario: 'openapi-e2e',
          suffix,
        },
      };
      const createBody = JSON.stringify(createPayload);
      const createResponse = await page.request.post('/api/openapi/pay/orders', {
        headers: openApiHeaders({
          appId,
          appSecret,
          method: 'POST',
          path: createPath,
          body: createBody,
          nonce: `create-${suffix}`,
        }),
        data: createBody,
      });
      const createResult = await expectBusinessOk<PaymentBusinessOrder>(createResponse);
      expect(createResult.data?.bizOrderNo).toBe(bizOrderNo);
      expect(createResult.data?.appId).toBe(appId);
      expectMoneyCents(createResult.data?.amount, 128800);
      expect(createResult.data?.status).toBe('TO_PAY');

      const repeatResponse = await page.request.post('/api/openapi/pay/orders', {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'POST',
        path: createPath,
        body: createBody,
        nonce: `repeat-${suffix}`,
      }),
      data: createBody,
    });
    const repeatResult = await expectBusinessOk<PaymentBusinessOrder>(repeatResponse);
    expect(String(repeatResult.data?.id || '')).toBe(String(createResult.data?.id || ''));

    const conflictBody = JSON.stringify({ ...createPayload, amount: 129900 });
    const conflictResponse = await page.request.post('/api/openapi/pay/orders', {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'POST',
        path: createPath,
        body: conflictBody,
        nonce: `conflict-${suffix}`,
      }),
      data: conflictBody,
    });
    const conflictResult = await expectBusinessError<PaymentBusinessOrder>(conflictResponse);
    expect(conflictResult.code).toBe(3794);

    const replayHeaders = openApiHeaders({
      appId,
      appSecret,
      method: 'POST',
      path: createPath,
      body: createBody,
      nonce: `replay-${suffix}`,
    });
    const replayFirstResponse = await page.request.post('/api/openapi/pay/orders', {
      headers: replayHeaders,
      data: createBody,
    });
    await expectBusinessOk<PaymentBusinessOrder>(replayFirstResponse);
    const replaySecondResponse = await page.request.post('/api/openapi/pay/orders', {
      headers: replayHeaders,
      data: createBody,
    });
    const replaySecondResult = await expectBusinessError<PaymentBusinessOrder>(replaySecondResponse);
    expect(replaySecondResult.code).toBe(3793);

    const detailPath = `/openapi/pay/orders/${bizOrderNo}`;
    const detailResponse = await page.request.get(`/api/openapi/pay/orders/${bizOrderNo}`, {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'GET',
        path: detailPath,
        nonce: `detail-${suffix}`,
      }),
    });
    const detailResult = await expectBusinessOk<PaymentBusinessOrder>(detailResponse);
    expect(String(detailResult.data?.id || '')).toBe(String(createResult.data?.id || ''));
    expect(detailResult.data?.bizOrderNo).toBe(bizOrderNo);
    expectMoneyCents(detailResult.data?.amount, 128800);

    const cashierPath = `/openapi/pay/orders/${bizOrderNo}/cashier`;
    const cashierResponse = await page.request.post(`/api/openapi/pay/orders/${bizOrderNo}/cashier`, {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'POST',
        path: cashierPath,
        nonce: `cashier-${suffix}`,
      }),
    });
    const cashierResult = await expectBusinessOk<PaymentOpenCashier>(cashierResponse);
    expect(String(cashierResult.data?.cashierConfigId || '')).toBe(String(cashierConfigId));
    expect(String(cashierResult.data?.businessOrderId || '')).toBe(String(createResult.data?.id || ''));
    expect(cashierResult.data?.bizOrderNo).toBe(bizOrderNo);
    expect(cashierResult.data?.cashierUrl).toBe(
      `/payment/cashier-configs/${cashierConfigId}/cashier?businessOrderId=${createResult.data?.id}`,
    );

    const payPath = `/openapi/pay/orders/${bizOrderNo}/pay`;
    const payBody = JSON.stringify({ methodCode: 'PERSONAL_WECHAT_QR' });
    const payResponse = await page.request.post(`/api/openapi/pay/orders/${bizOrderNo}/pay`, {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'POST',
        path: payPath,
        body: payBody,
        nonce: `pay-${suffix}`,
      }),
      data: payBody,
    });
    const payResult = await expectBusinessOk<PaymentOpenPaymentOrder>(payResponse);
    expect(payResult.data?.payOrderNo).toBeTruthy();
    expect(payResult.data?.bizOrderNo).toBe(bizOrderNo);
    expect(payResult.data?.appId).toBe(appId);
    expectMoneyCents(payResult.data?.amount, 128800);
    expect(payResult.data?.currency).toBe('CNY');
    expect(payResult.data?.status).toBe('PAYING');
    expect(payResult.data?.methodCode).toBe('PERSONAL_WECHAT_QR');
    expect(payResult.data?.channelCode).toBe('MANGO_PAY');
    expect(String(payResult.data?.contractCapabilityId || '')).toBe('333001');
    expect(String(payResult.data?.routeRuleId || '')).toBe(String(cashierConfigId + 1000));
    expect(payResult.data?.channelTradeNo || '').toBeTruthy();
    expect(payResult.data?.material?.materialType).toBe('QR');
    expect(payResult.data?.material?.qrContent).toContain(String(payResult.data?.payOrderNo || ''));
    expect(payResult.data?.material?.qrContent).toContain('PERSONAL_WECHAT_QR');

    const virtualPayResponse = await page.request.post('/api/payment/mango-pay/virtual/pay', {
      headers: adminHeaders,
      data: {
        cashierConfigId,
        payOrderNo: payResult.data?.payOrderNo,
        title: createPayload.title,
        amount: createPayload.amount,
        paymentMethodCode: 'PERSONAL_WECHAT_QR',
      },
    });
    const virtualPayResult = await expectBusinessOk<MangoPayVirtualPaymentResult>(virtualPayResponse);
    expect(virtualPayResult.data?.payOrderNo).toBe(payResult.data?.payOrderNo);
    expect(virtualPayResult.data?.status).toBe('SUCCESS');

    const paymentNotification = await notifyReceiver.waitFor('PAYMENT_SUCCESS');
    expect(paymentNotification.notifyNo).toBeTruthy();
    expect(paymentNotification.bizOrderNo).toBe(bizOrderNo);
    expect(paymentNotification.payOrderNo).toBe(payResult.data?.payOrderNo);
    expect(paymentNotification.appId).toBe(appId);
    expectMoneyCents(paymentNotification.amount, 128800);
    expect(paymentNotification.currency).toBe('CNY');
    expect(paymentNotification.status).toBe('SUCCESS');
    expect(paymentNotification.methodCode).toBe('PERSONAL_WECHAT_QR');
    expect(paymentNotification.channelCode).toBe('MANGO_PAY');
    expect(paymentNotification.channelTradeNo).toBeTruthy();
    expect(paymentNotification.signAlgorithm).toBe('HMAC_SHA256');
    expect(paymentNotification.signature).toBeTruthy();

    const paymentOrderPath = `/openapi/pay/payment-orders/${payResult.data?.payOrderNo}`;
    const paymentOrderResponse = await page.request.get(`/api/openapi/pay/payment-orders/${payResult.data?.payOrderNo}`, {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'GET',
        path: paymentOrderPath,
        nonce: `payment-detail-${suffix}`,
      }),
    });
    const paymentOrderResult = await expectBusinessOk<PaymentOpenPaymentOrder>(paymentOrderResponse);
    expect(paymentOrderResult.data?.payOrderNo).toBe(payResult.data?.payOrderNo);
    expect(paymentOrderResult.data?.bizOrderNo).toBe(bizOrderNo);
    expect(paymentOrderResult.data?.appId).toBe(appId);
    expectMoneyCents(paymentOrderResult.data?.amount, 128800);
    expect(paymentOrderResult.data?.status).toBe('SUCCESS');
    expect(paymentOrderResult.data?.methodCode).toBe('PERSONAL_WECHAT_QR');
    expect(paymentOrderResult.data?.channelCode).toBe('MANGO_PAY');
    expect(paymentOrderResult.data?.channelTradeNo).toBe(paymentNotification.channelTradeNo);
    expect(paymentOrderResult.data?.flowNo).toBeTruthy();

    const receiptPath = `/openapi/pay/receipts/${bizOrderNo}`;
    const receiptResponse = await page.request.get(`/api/openapi/pay/receipts/${bizOrderNo}`, {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'GET',
        path: receiptPath,
        nonce: `receipt-${suffix}`,
      }),
    });
    const receiptResult = await expectBusinessOk<PaymentOpenReceipt>(receiptResponse);
    expect(receiptResult.data?.receiptNo).toBe(`RCPT-${bizOrderNo}-${payResult.data?.payOrderNo}`);
    expect(receiptResult.data?.bizOrderNo).toBe(bizOrderNo);
    expect(receiptResult.data?.payOrderNo).toBe(payResult.data?.payOrderNo);
    expect(receiptResult.data?.appId).toBe(appId);
    expectMoneyCents(receiptResult.data?.amount, 128800);
    expect(receiptResult.data?.currency).toBe('CNY');
    expect(receiptResult.data?.status).toBe('SUCCESS');
    expect(receiptResult.data?.methodCode).toBe('PERSONAL_WECHAT_QR');
    expect(receiptResult.data?.channelCode).toBe('MANGO_PAY');
    expect(receiptResult.data?.channelTradeNo).toBe(paymentOrderResult.data?.channelTradeNo);
    expect(receiptResult.data?.flowNo).toBeTruthy();
    expect(receiptResult.data?.payTime).toBeTruthy();
    expect(receiptResult.data?.issuedTime).toBeTruthy();

    const bizRefundNo = `OPENAPI-RF-${suffix}`;
    const refundPath = '/openapi/pay/refunds';
    const refundPayload = {
      tenantId: 1,
      appId,
      bizOrderNo,
      bizRefundNo,
      refundAmount: 38800,
      reason: 'OpenAPI E2E 退款申请',
    };
    const refundBody = JSON.stringify(refundPayload);
    const refundResponse = await page.request.post('/api/openapi/pay/refunds', {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'POST',
        path: refundPath,
        body: refundBody,
        nonce: `refund-${suffix}`,
      }),
      data: refundBody,
    });
    const refundResult = await expectBusinessOk<PaymentOpenRefundOrder>(refundResponse);
    expect(refundResult.data?.refundOrderNo).toBeTruthy();
    expect(refundResult.data?.bizRefundNo).toBe(bizRefundNo);
    expect(refundResult.data?.bizOrderNo).toBe(bizOrderNo);
    expect(refundResult.data?.appId).toBe(appId);
    expect(refundResult.data?.payOrderNo).toBe(payResult.data?.payOrderNo);
    expectMoneyCents(refundResult.data?.refundAmount, 38800);
    expect(refundResult.data?.currency).toBe('CNY');
    expect(refundResult.data?.reason).toBe('OpenAPI E2E 退款申请');
    expect(refundResult.data?.status).toBe('REFUNDING');
    expect(refundResult.data?.methodCode).toBe('PERSONAL_WECHAT_QR');
    expect(refundResult.data?.channelCode).toBe('MANGO_PAY');
    expect(refundResult.data?.channelTradeNo).toBe(paymentOrderResult.data?.channelTradeNo);
    expect(refundResult.data?.channelRefundNo).toContain(String(refundResult.data?.refundOrderNo || ''));

    const repeatRefundResponse = await page.request.post('/api/openapi/pay/refunds', {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'POST',
        path: refundPath,
        body: refundBody,
        nonce: `refund-repeat-${suffix}`,
      }),
      data: refundBody,
    });
    const repeatRefundResult = await expectBusinessOk<PaymentOpenRefundOrder>(repeatRefundResponse);
    expect(String(repeatRefundResult.data?.id || '')).toBe(String(refundResult.data?.id || ''));
    expect(repeatRefundResult.data?.refundOrderNo).toBe(refundResult.data?.refundOrderNo);

    const conflictRefundBody = JSON.stringify({ ...refundPayload, refundAmount: 39900 });
    const conflictRefundResponse = await page.request.post('/api/openapi/pay/refunds', {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'POST',
        path: refundPath,
        body: conflictRefundBody,
        nonce: `refund-conflict-${suffix}`,
      }),
      data: conflictRefundBody,
    });
    const conflictRefundResult = await expectBusinessError<PaymentOpenRefundOrder>(conflictRefundResponse);
    expect(conflictRefundResult.code).toBe(3794);

    const exceededRefundBody = JSON.stringify({
      ...refundPayload,
      bizRefundNo: `OPENAPI-RF-EXCEEDED-${suffix}`,
      refundAmount: 200000,
    });
    const exceededRefundResponse = await page.request.post('/api/openapi/pay/refunds', {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'POST',
        path: refundPath,
        body: exceededRefundBody,
        nonce: `refund-exceeded-${suffix}`,
      }),
      data: exceededRefundBody,
    });
    const exceededRefundResult = await expectBusinessError<PaymentOpenRefundOrder>(exceededRefundResponse);
    expect(exceededRefundResult.code).toBe(3802);

    const refundDetailPath = `/openapi/pay/refunds/${bizRefundNo}`;
    const refundDetailResponse = await page.request.get(`/api/openapi/pay/refunds/${bizRefundNo}`, {
      headers: openApiHeaders({
        appId,
        appSecret,
        method: 'GET',
        path: refundDetailPath,
        nonce: `refund-detail-${suffix}`,
      }),
    });
    const refundDetailResult = await expectBusinessOk<PaymentOpenRefundOrder>(refundDetailResponse);
    expect(refundDetailResult.data?.refundOrderNo).toBe(refundResult.data?.refundOrderNo);
    expect(refundDetailResult.data?.bizRefundNo).toBe(bizRefundNo);
    expect(refundDetailResult.data?.status).toBe('REFUNDING');
    expect(refundDetailResult.data?.channelRefundNo).toBe(refundResult.data?.channelRefundNo);
    expect(refundDetailResult.data?.flowNo).toBe(refundResult.data?.flowNo);
    } finally {
      await notifyReceiver.close();
    }
  });

  test('支付方式三级分类、受控删除和审计真实可用', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const methodCode = `PERSONAL_WECHAT_QR_E2E_${suffix}`;

    const categoryResponse = await page.request.get('/api/payment/methods/categories', { headers });
    const categoryBody = await expectBusinessOk<PaymentMethodCategory[]>(categoryResponse);
    const personal = categoryBody.data?.find(item => item.categoryCode === 'PERSONAL');
    const corporate = categoryBody.data?.find(item => item.categoryCode === 'CORPORATE');
    expect(personal?.categoryName).toBe('对私');
    expect(corporate?.categoryName).toBe('对公');
    const wechat = personal?.children?.find(item => item.categoryCode === 'WECHAT');
    const alipay = personal?.children?.find(item => item.categoryCode === 'ALIPAY');
    const wallet = personal?.children?.find(item => item.categoryCode === 'WALLET');
    const ebank = corporate?.children?.find(item => item.categoryCode === 'EBANK');
    const offline = corporate?.children?.find(item => item.categoryCode === 'OFFLINE_TRANSFER');
    expect(wechat?.children?.map(item => item.categoryCode)).toEqual(expect.arrayContaining(['QR_CODE']));
    expect(alipay?.children?.map(item => item.categoryCode)).toEqual(expect.arrayContaining(['QR_CODE', 'H5_REDIRECT']));
    expect(ebank?.children?.map(item => item.categoryCode)).toEqual(expect.arrayContaining(['BANK_GATEWAY']));
    expect(offline?.children?.map(item => item.categoryCode)).toEqual(expect.arrayContaining(['ACCOUNT_TRANSFER']));
    expect(wallet?.children?.map(item => item.categoryCode)).toEqual(expect.arrayContaining(['WALLET_QUICK']));

    const ebankResponse = await page.request.get('/api/payment/methods/page', {
      headers,
      params: { page: '1', size: '10', keyword: 'PERSONAL_EBANK_REDIRECT' },
    });
    const ebankBody = await expectBusinessOk<PageData>(ebankResponse);
    const ebankMethod = (ebankBody.data?.list || []).find(item => item.methodCode === 'PERSONAL_EBANK_REDIRECT') as PaymentMethod | undefined;
    expect(ebankMethod).toMatchObject({
      accountNature: 'PERSONAL',
      instrumentType: 'EBANK',
      interactionType: 'BANK_GATEWAY',
      requiresBankSelection: 1,
      requiresQrRefresh: 0,
    });

    const createResponse = await page.request.post('/api/payment/methods', {
      headers,
      data: {
        methodCode,
        methodName: `微信扫码E2E${suffix}`,
        accountNature: 'PERSONAL',
        instrumentType: 'WECHAT',
        interactionType: 'QR_CODE',
        terminalScope: 'WEB,H5',
        paymentMaterialType: 'QR',
        cashierGroupCode: 'WECHAT_PAY',
        cashierGroupName: '微信支付',
        cashierGroupSort: 10,
        requiresBankSelection: 0,
        requiresQrRefresh: 1,
        description: '支付方式 E2E',
        sort: 199,
        status: 1,
      },
    });
    const createBody = await expectBusinessOk<string | number>(createResponse);
    const createdId = String(createBody.data || '');
    expect(createdId).toBeTruthy();

    const detailResponse = await page.request.get('/api/payment/methods/detail', {
      headers,
      params: { id: createdId },
    });
    const detailBody = await expectBusinessOk<PaymentMethod>(detailResponse);
    expect(detailBody.data).toMatchObject({
      methodCode,
      accountNature: 'PERSONAL',
      instrumentType: 'WECHAT',
      interactionType: 'QR_CODE',
      terminalScope: 'WEB,H5',
      paymentMaterialType: 'QR',
      requiresBankSelection: 0,
      requiresQrRefresh: 1,
      status: 1,
    });
    expect(detailBody.data).not.toHaveProperty('minAmount');
    expect(detailBody.data).not.toHaveProperty('maxAmount');
    expect(detailBody.data).not.toHaveProperty('channelId');

    const duplicateResponse = await page.request.post('/api/payment/methods', {
      headers,
      data: {
        methodCode,
        methodName: `微信扫码E2E重复${suffix}`,
        accountNature: 'PERSONAL',
        instrumentType: 'WECHAT',
        interactionType: 'QR_CODE',
        terminalScope: 'WEB',
        paymentMaterialType: 'QR',
        cashierGroupCode: 'WECHAT_PAY',
        cashierGroupName: '微信支付',
        cashierGroupSort: 10,
        status: 1,
      },
    });
    const duplicateBody = await expectBusinessError(duplicateResponse);
    expect(duplicateBody.code).toBe(3754);

    const referencedResponse = await page.request.get('/api/payment/methods/page', {
      headers,
      params: { page: '1', size: '10', keyword: 'PERSONAL_WECHAT_QR' },
    });
    const referencedBody = await expectBusinessOk<PageData>(referencedResponse);
    const referenced = (referencedBody.data?.list || []).find(item => item.methodCode === 'PERSONAL_WECHAT_QR') as PaymentMethod | undefined;
    expect(referenced?.id).toBeTruthy();
    const referencedDeleteResponse = await page.request.delete('/api/payment/methods', {
      headers,
      params: { id: String(referenced?.id) },
    });
    const referencedDeleteBody = await expectBusinessError(referencedDeleteResponse);
    expect(referencedDeleteBody.code).toBe(3759);
    const rejectedAudit = await findLatestPaymentAudit(page, headers, {
      action: 'DELETE_METHOD',
      resourceType: 'PAYMENT_METHOD',
      resourceId: 'PERSONAL_WECHAT_QR',
      operationResult: 'REJECTED',
    });
    expect(rejectedAudit).toMatchObject({
      operationAction: 'DELETE_METHOD',
      resourceType: 'PAYMENT_METHOD',
      resourceId: 'PERSONAL_WECHAT_QR',
      operationResult: 'REJECTED',
    });

    const deleteResponse = await page.request.delete('/api/payment/methods', {
      headers,
      params: { id: createdId },
    });
    await expectBusinessOk<boolean>(deleteResponse);
    const successAudit = await findLatestPaymentAudit(page, headers, {
      action: 'DELETE_METHOD',
      resourceType: 'PAYMENT_METHOD',
      resourceId: methodCode,
      operationResult: 'SUCCESS',
    });
    expect(successAudit).toMatchObject({
      operationAction: 'DELETE_METHOD',
      resourceType: 'PAYMENT_METHOD',
      resourceId: methodCode,
      operationResult: 'SUCCESS',
    });
  });

  test('支付方式路由策略、试算和页面交互真实可用', async ({ page }) => {
    test.setTimeout(150 * 1000);
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/method-routes')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });

    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const ruleCode = `E2E_WECHAT_ROUTE_${suffix}`;
    let routeId = '';

    try {
      disableHistoricalPaymentRouteE2eRules(ruleCode);
      const method = await findPaymentMethodByCode(page, headers, 'PERSONAL_WECHAT_QR');
      const contract = await findChannelContractByCode(page, headers, 'MANGO_PAY_MANGO_TECH');
      const contractCapability = contract?.capabilities?.find(item => item.methodCode === 'PERSONAL_WECHAT_QR' && item.terminalType === 'WEB');
      expect(method?.id, '应存在微信扫码支付方式').toBeTruthy();
      expect(contract?.id, '应存在芒果支付签约').toBeTruthy();
      expect(contractCapability?.id, '签约能力应包含微信扫码 Web').toBeTruthy();

      const createResponse = await page.request.post('/api/payment/method-routes', {
        headers,
        data: {
          ruleCode,
          ruleName: `E2E 微信扫码芒果支付路由 ${suffix}`,
          appId: '310001',
          subjectId: '320001',
          methodCode: 'PERSONAL_WECHAT_QR',
          terminalType: 'WEB',
          routeMode: 'PRIORITY',
          fallbackEnabled: 1,
          status: 1,
          items: [{
            contractCapabilityId: contractCapability?.id,
            priority: 1,
            weight: 100,
            minAmount: 1,
            maxAmount: 5000000,
            status: 1,
          }],
        },
      });
      const createBody = await expectBusinessOk<string | number>(createResponse);
      routeId = String(createBody.data || '');
      expect(routeId).toBeTruthy();

      const trialResponse = await page.request.post('/api/payment/method-routes/trial', {
        headers,
        data: {
          applicationId: '310001',
          subjectId: '320001',
          methodCode: 'PERSONAL_WECHAT_QR',
          terminalType: 'WEB',
          amount: 9900,
        },
      });
      const trialBody = await expectBusinessOk<PaymentMethodRouteTrialResult>(trialResponse);
      expect(trialBody.data?.matched).toBe(true);
      expect(trialBody.data?.matchedRule?.ruleCode).toBe(ruleCode);
      expect(trialBody.data?.matchedItem?.contractCapabilityId).toBe(String(contractCapability?.id));

      await openPaymentPage(page, '/#/payment/methods', '支付方式');
      await page.getByPlaceholder('名称 / 编码').fill('PERSONAL_WECHAT_QR');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/methods/page')),
        page.getByRole('button', { name: '查询' }).click(),
      ]);
      const methodRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: 'PERSONAL_WECHAT_QR' }).first();
      await expect(methodRow).toBeVisible({ timeout: 10000 });
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/method-routes/page')),
        methodRow.getByRole('button', { name: '路由策略' }).click(),
      ]);
      const routeDialog = dialog(page);
      await expect(routeDialog.getByText('微信扫码 路由策略')).toBeVisible({ timeout: 10000 });
      await expect(routeDialog.getByText('接入场景')).toHaveCount(0);
      await expect(routeDialog.locator('.method-route-panel__table')).toBeVisible({ timeout: 10000 });
      await routeDialog.getByRole('button', { name: '路由试算' }).click();
      const trialDialog = page.getByRole('dialog').filter({ hasText: '路由试算' }).last();
      await expect(trialDialog).toBeVisible({ timeout: 10000 });
      await expect(trialDialog.getByText('接入场景')).toHaveCount(0);
      await searchAndChooseSelect(page, '应用', '订单中心示例应用', '订单中心示例应用');
      await searchAndChooseSelect(page, '企业主体', '芒果科技', '芒果科技有限公司');
      await fillNumber(page, '金额（元）', '99');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/method-routes/trial')),
        trialDialog.getByRole('button', { name: '试算' }).click(),
      ]);
      await expect(trialDialog.getByText('已命中路由')).toBeVisible({ timeout: 10000 });
      await expect(trialDialog.getByText(`E2E 微信扫码芒果支付路由 ${suffix}`)).toBeVisible();
      await page.screenshot({ path: 'test-results/payment-method-route.png', fullPage: true });

      await trialDialog.getByRole('button', { name: '关闭' }).click();
      await routeDialog.locator('.el-dialog__headerbtn').first().click();

      const deleteResponse = await page.request.delete('/api/payment/method-routes', {
        headers,
        params: { id: routeId },
      });
      await expectBusinessOk<boolean>(deleteResponse);
      routeId = '';
      const deleted = await findRouteRuleByCode(page, headers, ruleCode);
      expect(deleted).toBeUndefined();

      const createAudit = await findLatestPaymentAudit(page, headers, {
        action: 'CREATE_METHOD_ROUTE',
        resourceType: 'PAYMENT_METHOD_ROUTE',
        resourceId: ruleCode,
        operationResult: 'SUCCESS',
      });
      expect(createAudit).toMatchObject({
        operationAction: 'CREATE_METHOD_ROUTE',
        resourceType: 'PAYMENT_METHOD_ROUTE',
        resourceId: ruleCode,
        operationResult: 'SUCCESS',
      });
      const trialAudit = await findLatestPaymentAudit(page, headers, {
        action: 'TRIAL_METHOD_ROUTE',
        resourceType: 'PAYMENT_METHOD_ROUTE',
        resourceId: ruleCode,
        operationResult: 'SUCCESS',
      });
      expect(trialAudit).toMatchObject({
        operationAction: 'TRIAL_METHOD_ROUTE',
        resourceType: 'PAYMENT_METHOD_ROUTE',
        resourceId: ruleCode,
        operationResult: 'SUCCESS',
      });
      expect(runtimeErrors).toEqual([]);
    } finally {
      if (routeId) {
        const cleanupResponse = await page.request.delete('/api/payment/method-routes', {
          headers,
          params: { id: routeId },
        });
        await expectBusinessOk<boolean>(cleanupResponse);
      }
    }
  });

  test('数据库菜单分组、列表接口和收银台入口可用', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);

    const menuResponse = await page.request.get('/api/authorization/menus/user', {
      headers,
      params: { appCode: 'internal-admin', fmt: 'tree' },
    });
    const menuBody = await expectBusinessOk<MenuNode[]>(menuResponse);
    const menuNames = collectMenuNames(menuBody.data || []);
    expect(menuNames).toEqual(expect.arrayContaining([
      '支付中心',
      '应用接入',
      '支付通道',
      '交易订单',
      '对账结算',
      '应用管理',
      '企业主体',
      '签约通道',
      '支付通道',
      '支付方式',
      '收银台',
      '业务订单',
      '支付订单',
      '退款订单',
      '交易流水',
      '异常订单',
      '通知记录',
      '对账管理',
      '差异处理',
      '结算汇总',
      '操作审计',
    ]));
    expect(menuNames).not.toContain('租户收银台');
    expect(menuNames).not.toContain('App 收银台');
    expect(menuNames).not.toContain('小程序收银台');
    expect(menuNames).not.toContain('支付结果页');

    for (const endpoint of paymentEndpoints) {
      const response = await page.request.get(`/api/payment/${endpoint}/page`, {
        headers,
        params: { page: '1', size: '10', keyword: '' },
      });
      const body = await expectBusinessOk<PageData>(response);
      expect(Number(body.data?.total || 0), `${endpoint} 应返回支付测试数据`).toBeGreaterThan(0);
    }

    await openPaymentPage(page, '/#/payment/applications', '应用管理');
    await page.getByPlaceholder('应用名称 / AppId').fill('订单中心示例应用');
    await expect(page.getByText('支付通知')).toHaveCount(0);
    await expect(page.getByText('退款通知')).toHaveCount(0);
    await expect(page.getByText('通知白名单')).toHaveCount(0);
    await expect(page.getByText('返回域名白名单')).toHaveCount(0);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/applications/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    await expect(page.getByText('订单中心示例应用')).toBeVisible();
    const appRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: '订单中心示例应用' }).first();
    await expect(appRow.locator('.el-tag').filter({ hasText: /^app_/ })).toHaveCount(0);

    await openPaymentPage(page, '/#/payment/channels', '支付通道');
    const mangoPayChannelRow = await paymentTableRow(page, 'MANGO_PAY');
    await expect(mangoPayChannelRow.getByText('芒果支付', { exact: true })).toBeVisible();
    await expect(page.getByText(/自建.*支付通道/).filter({ hasText: '芒果支付' })).toHaveCount(0);
    await expect(page.getByText('通联支付通道')).toBeVisible();
    await expect(page.getByText('华夏银行通道')).toBeVisible();
    await page.getByRole('button', { name: '新增' }).click();
    await expect(dialog(page).getByText('字段模板')).toBeVisible();
    await expect(dialog(page).getByPlaceholder('字段名，如 merchantNo')).toHaveCount(0);
    await dialog(page).getByRole('button', { name: '新增字段' }).click();
    await expect(dialog(page).getByPlaceholder('字段名，如 merchantNo')).toBeVisible();
    await expect(dialog(page).getByPlaceholder('枚举选项，每行 label=value；非枚举字段可留空')).toBeVisible();
    await expect(dialog(page).getByPlaceholder(/JSON/)).toHaveCount(0);
    await dialog(page).getByRole('button', { name: '取消' }).click();

    await openPaymentPage(page, '/#/payment/channel-contracts', '签约通道');
    const builtInContractRow = await paymentTableRow(page, 'MANGO_PAY_MERCHANT_001');
    await expect(builtInContractRow.getByText('芒果科技有限公司')).toBeVisible();
    await expect(builtInContractRow.getByText('芒果支付')).toBeVisible();
    await page.getByRole('button', { name: '新增' }).click();
    await expect(dialog(page).getByText('配置值')).toBeVisible();
    await expect(formItem(page, '签约名称')).toHaveCount(0);
    await expect(formItem(page, '配置值').locator('textarea')).toHaveCount(0);
    await dialog(page).getByRole('button', { name: '取消' }).click();

    const cashierConfig = {
      id: '350001',
      cashierName: '订单中心 Web 收银台',
    } as PaymentCashierConfig;
    const expectedTitle = cashierDisplayTitle(cashierConfig);
    await openPaymentPage(page, '/#/payment/cashier-configs', '收银台');
    await page.getByPlaceholder('名称 / 编码').fill(cashierConfig.cashierName || '');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/cashier-configs/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const cashierConfigRow = await paymentTableRow(page, cashierConfig.cashierName || '');
    await expect(cashierConfigRow.getByText(cashierConfig.cashierName || '', { exact: true }).first()).toBeVisible();
    await expect(cashierConfigRow.locator('.el-tag').filter({ hasText: cashierConfig.cashierName || '' })).toHaveCount(0);
    if (cashierConfig.enterpriseSubjectNames) {
      await expect(page.getByText(cashierConfig.enterpriseSubjectNames.split(',')[0]).first()).toBeVisible();
    }
    if (cashierConfig.methodNames) {
      await expect(page.getByText(cashierConfig.methodNames.split(',')[0]).first()).toBeVisible();
    }

    const previewPayRequests: string[] = [];
    page.on('request', request => {
      if (request.url().includes('/api/payment/cashier/pay')) {
        previewPayRequests.push(request.url());
      }
    });
    const sessionPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/session'));
    await clickPaymentTableRowButton(page, cashierConfig.cashierName || '', '预览');
    const sessionResponse = await sessionPromise;
    const sessionBody = await expectBusinessOk<CashierSession>(sessionResponse);
    expect(sessionBody.data?.cashierConfigId).toBe('350001');
    expect([undefined, 'TO_PAY', 'PAYING']).toContain(sessionBody.data?.order?.status);
    expect(sessionBody.data?.methods?.map(item => item.methodCode)).toEqual(expect.arrayContaining([
      'PERSONAL_WECHAT_QR',
      'PERSONAL_ALIPAY_QR',
      'PERSONAL_ALIPAY_PC',
      'PERSONAL_EBANK_REDIRECT',
      'CORPORATE_EBANK_REDIRECT',
      'CORPORATE_OFFLINE_ACCOUNT',
    ]));
    const cashierDialog = dialog(page);
    await expect(cashierDialog.getByRole('heading', { name: '收银台预览' })).toBeVisible({ timeout: 10000 });
    await expect(cashierDialog.getByText(expectedTitle)).toBeVisible();
    await expect(cashierDialog.getByText('PREVIEW-ORDER')).toBeVisible();
    await expect(cashierDialog.getByText('收银台预览订单')).toBeVisible();
    await expect(cashierDialog.getByRole('tab', { name: /微信支付/ })).toBeVisible();
    await expect(cashierDialog.getByRole('tab', { name: /支付宝支付/ })).toBeVisible();
    await expect(cashierDialog.getByRole('tab', { name: /网银支付/ })).toBeVisible();
    await expect(cashierDialog.getByRole('tab', { name: /线下转账/ })).toBeVisible();
    await expect(cashierDialog.getByRole('tab', { name: /微信支付/ })).toHaveAttribute('aria-selected', 'true');
    await expect(cashierDialog.getByAltText('支付二维码')).toBeVisible({ timeout: 10000 });
    await expect(cashierDialog.getByText('预览模式不生成支付订单')).toBeVisible();
    await cashierDialog.getByRole('tab', { name: /支付宝支付/ }).click();
    await expect(cashierDialog.getByRole('tab', { name: /支付宝支付/ })).toHaveAttribute('aria-selected', 'true');
    await expect(cashierDialog.getByAltText('支付二维码')).toBeVisible({ timeout: 10000 });
    await cashierDialog.getByRole('tab', { name: /网银支付/ }).click();
    const personalEbankTab = cashierDialog.locator('.ebank-type-tab').filter({ hasText: '个人网银' });
    const corporateEbankTab = cashierDialog.locator('.ebank-type-tab').filter({ hasText: '企业网银' });
    await expect(personalEbankTab).toBeVisible();
    await expect(corporateEbankTab).toBeVisible();
    await expect(personalEbankTab).toHaveClass(/active/);
    const firstBankBanner = cashierDialog.locator('.bank-banner').filter({ hasText: '工行' });
    await expect(firstBankBanner).toBeVisible();
    await expect(firstBankBanner).toHaveClass(/active/);
    await cashierDialog.getByRole('tab', { name: /线下转账/ }).click();
    await expect(cashierDialog.getByText('转账通知单')).toBeVisible();
    await expect(cashierDialog.getByText('演示收款方').first()).toBeVisible();
    await expect(cashierDialog.getByText('6222 0000 0000 0000')).toBeVisible();
    await expect(cashierDialog.getByText('A8K3P2')).toBeVisible();
    await expect(cashierDialog.getByAltText('支付二维码')).toHaveCount(0);
    await expect(cashierDialog.getByText('线下转账通道未返回收款账户信息')).toHaveCount(0);
    expect(previewPayRequests, '收银台配置预览不应发起真实支付').toHaveLength(0);
    await cashierDialog.locator('.el-dialog__headerbtn').first().click();

    const referencedDeleteResponse = await page.request.delete('/api/payment/cashier-configs', {
      headers,
      params: { id: '350001' },
    });
    const referencedDeleteBody = await expectBusinessError<boolean>(referencedDeleteResponse);
    expect(referencedDeleteBody.code).toBe(3764);
    expect(referencedDeleteBody.msg).toContain('收银台配置存在关联数据');
    const rejectedAudit = await findLatestPaymentAudit(page, headers, {
      action: 'DELETE_CASHIER_CONFIG',
      resourceType: 'PAYMENT_CASHIER_CONFIG',
      resourceId: '350001',
      operationResult: 'REJECTED',
    });
    expect(rejectedAudit).toMatchObject({
      operationAction: 'DELETE_CASHIER_CONFIG',
      resourceType: 'PAYMENT_CASHIER_CONFIG',
      resourceId: '350001',
      operationResult: 'REJECTED',
    });
  });

  test('支付列表普通字段纯文本展示，无边框标签包裹', async ({ page }) => {
    const runtimeErrors = collectRuntimeErrors(page);
    await login(page);
    const headers = await apiHeaders(page);

    await openPaymentPage(page, '/#/payment/methods', '支付方式');
    await page.getByPlaceholder('名称 / 编码').fill('PERSONAL_WECHAT_QR');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/methods/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const methodRow = await paymentTableRow(page, 'PERSONAL_WECHAT_QR');
    await expectNoTagText(methodRow, '对私', '支付方式一级分类应为纯文本');
    await expectNoTagText(methodRow, '微信', '支付方式二级分类应为纯文本');
    await expectNoTagText(methodRow, '扫码', '支付方式三级分类应为纯文本');
    await expectOnlySemanticTags(methodRow, [/^(启用|停用)$/], '支付方式列表只允许状态标签');

    const route = await findFirstPageRecord<PaymentMethodRouteRule>(page, headers, 'method-routes');
    await page.getByPlaceholder('名称 / 编码').fill(route.methodCode);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/methods/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const routedMethodRow = await paymentTableRow(page, route.methodCode);
    await routedMethodRow.getByRole('button', { name: '路由策略' }).click();
    const routeDialog = dialog(page);
    await expect(routeDialog.getByText('路由策略')).toBeVisible({ timeout: 10000 });
    const routeRow = routeDialog.locator('.method-route-panel__table .el-table__body-wrapper tbody tr').filter({ hasText: route.ruleCode }).first();
    await expect(routeRow).toBeVisible({ timeout: 10000 });
    await expectOnlySemanticTags(routeRow, [/^(启用|停用)$/], '路由策略列表只允许状态标签');
    await routeDialog.locator('.el-dialog__headerbtn').first().click();

    await openPaymentPage(page, '/#/payment/channels', '支付通道');
    await page.getByPlaceholder('名称 / 编码').fill('MANGO_PAY');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channels/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const channelRow = await paymentTableRow(page, 'MANGO_PAY');
    await expectNoTagText(channelRow, '芒果支付', '通道类型应为纯文本');
    await expectOnlySemanticTags(channelRow, [/^(启用|停用)$/], '支付通道列表只允许状态标签');

    await openPaymentPage(page, '/#/payment/channel-contracts', '签约通道');
    await page.getByPlaceholder('名称 / 编码').fill('MANGO_PAY_MERCHANT_001');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channel-contracts/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const contractRow = await paymentTableRow(page, 'MANGO_PAY_MERCHANT_001');
    await expect(contractRow.getByText('接入场景')).toHaveCount(0);
    await expect(contractRow.locator('.el-tag').filter({ hasText: /微信扫码|支付宝|网银|线下/ }).first()).toBeVisible();

    const flow = await findFirstPageRecord<PaymentTransactionFlow>(page, headers, 'transaction-flows');
    await openPaymentPage(page, '/#/payment/transaction-flows', '交易流水');
    const flowRow = await searchPaymentTable(page, 'transaction-flows', flow.flowNo || '', '.payment-transaction-flows__table');
    await expectNoTagText(flowRow, flow.flowTypeName || flow.flowType, '交易流水类型应为纯文本');

    const exception = await findFirstPageRecord<PaymentExceptionOrder>(page, headers, 'exception-orders');
    await openPaymentPage(page, '/#/payment/exception-orders', '异常订单');
    const exceptionRow = await searchPaymentTable(page, 'exception-orders', exception.exceptionNo || '', '.payment-exception-orders__table');
    await expectNoTagText(exceptionRow, exception.exceptionTypeName || exception.exceptionType, '异常类型应为纯文本');

    const notification = await findFirstPageRecord<PaymentNotificationRecord>(page, headers, 'notification-records');
    await openPaymentPage(page, '/#/payment/notification-records', '通知记录');
    const notificationRow = await searchPaymentTable(page, 'notification-records', notification.notificationNo || '', '.payment-notification-records__table');
    await expectNoTagText(notificationRow, notification.notificationTypeName || notification.notificationType, '通知类型应为纯文本');

    const difference = await findFirstPageRecord<PaymentDifference>(page, headers, 'differences');
    await openPaymentPage(page, '/#/payment/differences', '差异处理');
    const differenceRow = await searchPaymentTable(page, 'differences', difference.differenceNo || '', '.payment-differences__table');
    await expectNoTagText(differenceRow, difference.differenceTypeName || difference.differenceType, '差异类型应为纯文本');

    const audit = await findFirstPageRecord<PaymentOperationAudit>(page, headers, 'operation-audits');
    await openPaymentPage(page, '/#/payment/operation-audits', '操作审计');
    const auditRow = await searchPaymentTable(page, 'operation-audits', audit.operationAction || '', '.payment-operation-audits__table');
    await expectNoTagText(auditRow, audit.operationAction, '操作动作应为纯文本');
    await expectNoTagText(auditRow, audit.resourceType, '资源类型应为纯文本');

    const reconciliation = await findFirstPageRecord<PaymentReconciliation>(page, headers, 'reconciliations');
    await openPaymentPage(page, '/#/payment/reconciliations', '对账管理');
    await page.locator('.payment-reconciliations__toolbar input').first().fill(reconciliation.reconciliationNo || '');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/reconciliations/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const reconciliationRow = page.locator('.payment-reconciliations__table .el-table__body-wrapper tbody tr')
      .filter({ hasText: reconciliation.reconciliationNo || '' })
      .first();
    await expect(reconciliationRow).toBeVisible({ timeout: 10000 });
    await reconciliationRow.getByRole('button', { name: '详情' }).click();
    const reconciliationDrawer = page.locator('.el-drawer').last();
    await expect(reconciliationDrawer.getByText('账单明细')).toBeVisible({ timeout: 10000 });
    const billDetail = (reconciliation.details || [])[0];
    if (billDetail) {
      const billDetailRow = reconciliationDrawer.locator('.el-table__body-wrapper tbody tr')
        .filter({ hasText: billDetail.channelTradeNo || '' })
        .first();
      await expect(billDetailRow).toBeVisible({ timeout: 10000 });
      await expectNoTagText(billDetailRow, billDetail.tradeTypeName || billDetail.tradeType, '对账明细交易类型应为纯文本');
    }

    await page.screenshot({ path: 'test-results/payment-list-ui-no-tag.png', fullPage: true });
    expect(runtimeErrors).toEqual([]);
  });

  test('支付方式编辑只维护产品展示字段，不暴露签约和路由差异配置', async ({ page }) => {
    const runtimeErrors = collectRuntimeErrors(page);
    await login(page);

    await openPaymentPage(page, '/#/payment/methods', '支付方式');
    await page.getByPlaceholder('名称 / 编码').fill('PERSONAL_WECHAT_QR');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/methods/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const methodRow = await paymentTableRow(page, 'PERSONAL_WECHAT_QR');
    await expect(page.getByText('通道差异、商户配置和路由命中在签约通道与路由策略中维护')).toBeVisible();

    await methodRow.getByRole('button', { name: '编辑' }).click();
    const methodDialog = dialog(page);
    await expect(methodDialog.getByText('编辑支付方式')).toBeVisible({ timeout: 10000 });
    await expect(methodDialog.getByText('产品说明')).toBeVisible();
    await expect(methodDialog.getByText('可见范围')).toHaveCount(0);
    await expect(methodDialog.getByText('最小金额（元）')).toHaveCount(0);
    await expect(methodDialog.getByText('最大金额（元）')).toHaveCount(0);
    await expect(methodDialog.locator('.el-form-item__label').filter({ hasText: /^路由策略$/ })).toHaveCount(0);
    await expect(methodDialog.getByText('商户号')).toHaveCount(0);
    await expect(methodDialog.getByText('开户行')).toHaveCount(0);
    await page.screenshot({ path: 'test-results/payment-method-product-form.png', fullPage: true });

    expect(runtimeErrors).toEqual([]);
  });

  test('线下收款独立菜单、确认到账操作和支付订单入口边界可用', async ({ page }) => {
    test.setTimeout(90 * 1000);
    const runtimeErrors = collectRuntimeErrors(page);
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const cashierConfigId = 350903;
    const bizOrderNo = `PAY-OFFLINE-MENU-${suffix}`;
    prepareCashierConfig({
      id: cashierConfigId,
      cashierName: 'E2E 线下收款后台菜单',
      methodCodes: 'CORPORATE_OFFLINE_ACCOUNT',
      defaultMethodCode: 'CORPORATE_OFFLINE_ACCOUNT',
    });
    const businessOrderId = preparePayingBusinessOrder(bizOrderNo);
    const payResponse = await page.request.post('/api/payment/cashier/pay', {
      headers,
      data: {
        cashierConfigId: String(cashierConfigId),
        businessOrderId: String(businessOrderId),
        methodCode: 'CORPORATE_OFFLINE_ACCOUNT',
      },
    });
    const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
    const payOrderNo = payBody.data?.payOrderNo || '';
    expect(payOrderNo).toMatch(PAY_ORDER_NO_PATTERN);
    expect(payBody.data?.channelCode).toBe('OFFLINE_COLLECTION');

    const menuResponse = await page.request.get('/api/authorization/menus/user', {
      headers,
      params: { appCode: 'internal-admin', fmt: 'tree' },
    });
    const menuBody = await expectBusinessOk<MenuNode[]>(menuResponse);
    const menuNames = collectMenuNames(menuBody.data || []);
    expect(menuNames).toEqual(expect.arrayContaining(['支付中心', '支付通道', '线下支付', '线下收款订单']));

    const statusesResponse = await page.request.get('/api/payment/offline-collections/statuses', { headers });
    const statusesBody = await expectBusinessOk<Array<{ statusCode?: string; statusName?: string }>>(statusesResponse);
    expect(statusesBody.data?.map(item => item.statusCode)).toEqual(expect.arrayContaining([
      'WAITING_TRANSFER',
      'PENDING_CONFIRM',
      'CONFIRMED',
      'RECONCILED',
      'EXPIRED',
      'CLOSED',
    ]));

    const pageResponse = await page.request.get('/api/payment/offline-collections/page', {
      headers,
      params: { page: '1', size: '10', keyword: '' },
    });
    const pageBody = await expectBusinessOk<PageData>(pageResponse);
    expect(Number(pageBody.data?.total || 0)).toBeGreaterThanOrEqual(0);
    expect(Array.isArray(pageBody.data?.list || [])).toBeTruthy();

    await openPaymentPage(page, '/#/payment/offline/collections', '线下收款');
    await expect(page.locator('.payment-offline-collections__toolbar')).toBeVisible();
    await expect(page.getByPlaceholder('收款单号 / 支付单号 / 业务单号 / 对账码 / 备注')).toBeVisible();
    await page.getByPlaceholder('收款单号 / 支付单号 / 业务单号 / 对账码 / 备注').fill(payOrderNo);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/offline-collections/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    await expect(page.locator('.payment-offline-collections__table')).toBeVisible();
    await page.locator('.payment-offline-collections__table .el-table__body-wrapper').waitFor({ state: 'visible' });
    const collectionRow = page.locator('.payment-offline-collections__table .el-table__body-wrapper tbody tr')
      .filter({ hasText: payOrderNo })
      .first();
    await expect(collectionRow).toBeVisible({ timeout: 10000 });
    await expect(collectionRow).toContainText('待转账');
    await expect(collectionRow.getByRole('button', { name: '确认到账' })).toBeEnabled();
    await expect(collectionRow.getByRole('button', { name: '退款' })).toBeDisabled();
    await expect(collectionRow.getByRole('button', { name: '详情' })).toBeVisible();

    await collectionRow.getByRole('button', { name: '确认到账' }).click();
    const confirmDialog = page.getByRole('dialog').filter({ hasText: '确认线下收款到账' }).last();
    await expect(confirmDialog).toBeVisible({ timeout: 10000 });
    await expect(confirmDialog.getByText('收款摘要')).toBeVisible();
    await expect(confirmDialog.getByText('到账确认')).toBeVisible();
    await fillNumber(page, '到账金额（元）', '1288');
    await fillTextarea(page, '确认说明', 'E2E 后台菜单确认线下收款到账');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/offline-collections/confirm')),
      confirmDialog.getByRole('button', { name: '确认到账' }).click(),
    ]);
    await expect(page.locator('.el-message').filter({ hasText: '已确认到账' }).last()).toBeVisible({ timeout: 10000 });
    await expect(confirmDialog).toBeHidden({ timeout: 10000 });

    const confirmedRow = page.locator('.payment-offline-collections__table .el-table__body-wrapper tbody tr')
      .filter({ hasText: payOrderNo })
      .first();
    await expect(confirmedRow).toContainText('已确认到账');
    await expect(confirmedRow.getByRole('button', { name: '确认到账' })).toBeDisabled();
    await expect(confirmedRow.getByRole('button', { name: '退款' })).toBeEnabled();

    const confirmedPaymentOrder = await findPaymentOrderByNo(page, headers, payOrderNo);
    expect(confirmedPaymentOrder).toMatchObject({
      payOrderNo,
      channelCode: 'OFFLINE_COLLECTION',
      status: 'SUCCESS',
      statusName: '支付成功',
    });

    await openPaymentPage(page, '/#/payment/payment-orders', '支付订单');
    await expect(page.locator('.payment-orders__table')).toBeVisible();
    await expect(page.getByRole('button', { name: '确认到账' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: '线下退款确认' })).toHaveCount(0);

    await page.screenshot({ path: 'test-results/payment-offline-collections.png', fullPage: true });
    expect(runtimeErrors).toEqual([]);
  });

  test('业务订单列表、状态筛选和详情真实可用', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/business-orders')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const bizOrderNo = `BO-LIST-E2E-${Date.now()}`;
    const businessOrderId = preparePayingBusinessOrder(bizOrderNo);
    const paidBizOrderNo = `BO-PAID-E2E-${Date.now()}`;
    const paidBusinessOrderId = preparePayingBusinessOrder(paidBizOrderNo);
    let paidPayOrderNo = '';
    setMangoPayScenario('PAYING');
    try {
      const paidPayResponse = await page.request.post('/api/payment/cashier/pay', {
        headers,
        data: {
          cashierConfigId: '350001',
          businessOrderId: String(paidBusinessOrderId),
          methodCode: 'PERSONAL_WECHAT_QR',
        },
      });
      const paidPayBody = await expectBusinessOk<CashierPayResult>(paidPayResponse);
      paidPayOrderNo = paidPayBody.data?.payOrderNo || '';
      expect(paidPayOrderNo).toBeTruthy();
      finishProcessingPayment(paidPayOrderNo);
    } finally {
      setMangoPayScenario('SUCCESS');
    }

    const statusesResponse = await page.request.get('/api/payment/business-orders/statuses', { headers });
    const statusesBody = await expectBusinessOk<PaymentBusinessOrderStatus[]>(statusesResponse);
    expect(statusesBody.data?.map(item => item.statusCode)).toEqual(expect.arrayContaining(['TO_PAY', 'PAYING', 'PAID', 'CLOSED', 'REFUNDED']));

    const pageResponse = await page.request.get('/api/payment/business-orders/page', {
      headers,
      params: { page: '1', size: '10', keyword: bizOrderNo, statusCode: 'PAYING' },
    });
    const pageBody = await expectBusinessOk<PageData>(pageResponse);
    expect(Number(pageBody.data?.total || 0)).toBeGreaterThan(0);
    const apiRow = (pageBody.data?.list || []).find(item => item.bizOrderNo === bizOrderNo) as PaymentBusinessOrder | undefined;
    expect(apiRow).toMatchObject({
      id: String(businessOrderId),
      bizOrderNo,
      appId: 'app_order_center',
      appName: '订单中心示例应用',
      title: `E2E 业务订单 ${bizOrderNo}`,
      status: 'PAYING',
      statusName: '支付中',
      cashierConfigId: '350001',
    });
    expect(apiRow?.cashierName).toBeTruthy();
    expect(Number(apiRow?.amount || 0)).toBe(128800);
    expect(Number(apiRow?.paidAmount || 0)).toBe(0);
    expect(Number(apiRow?.refundedAmount || 0)).toBe(0);

    const detailResponse = await page.request.get('/api/payment/business-orders/detail', {
      headers,
      params: { id: String(businessOrderId) },
    });
    const detailBody = await expectBusinessOk<PaymentBusinessOrder>(detailResponse);
    expect(detailBody.data).toMatchObject({
      id: String(businessOrderId),
      bizOrderNo,
      notifyUrl: 'https://business.example.test/payment/notify',
      returnUrl: 'https://business.example.test/payment/result',
      cashierConfigId: '350001',
    });
    expect(Number(detailBody.data?.paymentOrderCount || 0)).toBe(0);
    expect(Number(detailBody.data?.refundOrderCount || 0)).toBe(0);
    expect(detailBody.data?.extendInfo).toContain('businessRefNo');

    await openPaymentPage(page, '/#/payment/business-orders', '业务订单');
    const toolbar = page.locator('.payment-business-orders__toolbar');
    await toolbar.getByPlaceholder('业务单号 / 应用 / 标题 / 企业主体').fill(bizOrderNo);
    const statusSelect = toolbar.locator('.el-form-item').filter({ hasText: '订单状态' }).locator('.el-select').first();
    const openBusinessOrderStatusDropdown = async () => {
      const listboxId = await statusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const dropdown = page.locator(`[id="${listboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await expect(dropdown).toBeVisible({ timeout: 10000 });
      return dropdown;
    };
    const statusDropdown = await openBusinessOrderStatusDropdown();
    await expect(statusDropdown).toBeVisible();
    await expect(statusDropdown.getByText('支付中')).toBeVisible();
    await clickVisibleOption(statusDropdown, '支付中');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/business-orders/page') && response.url().includes('statusCode=PAYING')),
      toolbar.getByRole('button', { name: '查询' }).click(),
    ]);

    const row = page.locator('.payment-business-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: bizOrderNo }).first();
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row).toContainText(`E2E 业务订单 ${bizOrderNo}`);
    await expect(row).toContainText('订单中心示例应用');
    await expect(row).toContainText('￥1288.00');
    await expect(row).toContainText('支付中');
    await expect(row.locator('.el-tag').filter({ hasText: 'ORDER_CENTER' })).toHaveCount(0);
    const actionButtons = row.getByRole('button');
    await expect(actionButtons.nth(0)).toHaveText(/支付/);
    await expect(actionButtons.nth(1)).toHaveText(/详情/);

    const cashierSessionPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/session'));
    const cashierPayPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/pay'));
    await row.getByRole('button', { name: '支付' }).click();
    const [cashierSessionBody, cashierPayBody] = await Promise.all([
      expectBusinessOk<CashierSession>(await cashierSessionPromise),
      expectBusinessOk<CashierPayResult>(await cashierPayPromise),
    ]);
    expect(cashierSessionBody.data?.cashierConfigId).toBe('350001');
    expect(cashierSessionBody.data?.order?.bizOrderNo).toBe(bizOrderNo);
    expect(['PAYING', 'SUCCESS']).toContain(cashierPayBody.data?.status);
    expect(cashierPayBody.data?.payOrderNo).toBeTruthy();
    expect(cashierPayBody.data?.material?.materialType).toBe('QR');
    const cashierDialog = page.getByRole('dialog').filter({ hasText: `收银台 - ${bizOrderNo}` }).last();
    await expect(cashierDialog.getByText(bizOrderNo, { exact: true }).first()).toBeVisible();
    await expect(cashierDialog.getByRole('tab', { name: /微信支付/ })).toHaveAttribute('aria-selected', 'true');
    if (cashierPayBody.data?.status === 'SUCCESS') {
      await expect(cashierDialog.getByText('支付成功')).toBeVisible({ timeout: 10000 });
    } else {
      await expect(cashierDialog.getByAltText('支付二维码')).toBeVisible({ timeout: 10000 });
      await expect(cashierDialog.getByRole('button', { name: '我已完成支付' })).toBeVisible();
    }
    await cashierDialog.locator('.el-dialog__headerbtn').click();

    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/business-orders/detail')),
      row.getByRole('button', { name: '详情' }).click(),
    ]);
    const drawer = page.getByRole('dialog').filter({ hasText: '业务订单详情' }).last();
    await expect(drawer.getByText(bizOrderNo, { exact: true })).toBeVisible();
    await expect(drawer.getByText('订单中心示例应用')).toBeVisible();
    await expect(drawer.getByText('https://business.example.test/payment/notify')).toBeVisible();
    await expect(drawer.getByText('https://business.example.test/payment/result')).toBeVisible();
    await expect(drawer.getByText('businessRefNo')).toBeVisible();

    await drawer.locator('.el-drawer__close-btn').click();

    await toolbar.getByPlaceholder('业务单号 / 应用 / 标题 / 企业主体').fill(paidBizOrderNo);
    const paidStatusDropdown = await openBusinessOrderStatusDropdown();
    await clickVisibleOption(paidStatusDropdown, '已支付');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/business-orders/page') && response.url().includes('statusCode=PAID')),
      toolbar.getByRole('button', { name: '查询' }).click(),
    ]);
    const paidRow = page.locator('.payment-business-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: paidBizOrderNo }).first();
    await expect(paidRow).toBeVisible({ timeout: 10000 });
    await expect(paidRow).toContainText('已支付');
    await expect(paidRow.getByRole('button', { name: '支付' })).toBeDisabled();

    const emptyKeyword = `NO-BIZ-ORDER-${Date.now()}`;
    await toolbar.getByPlaceholder('业务单号 / 应用 / 标题 / 企业主体').fill(emptyKeyword);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/business-orders/page') && response.url().includes(encodeURIComponent(emptyKeyword))),
      toolbar.getByRole('button', { name: '查询' }).click(),
    ]);
    await expect(page.locator('.payment-business-orders__table')).toContainText('未查询到匹配的业务订单');
    await expect(page.locator('.payment-business-orders__table tbody tr')).toHaveCount(0);

    await page.screenshot({ path: 'test-results/payment-business-orders.png', fullPage: true });
    expect(runtimeErrors).toEqual([]);
  });

  test('支付订单列表、状态筛选和详情真实可用', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/payment-orders')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const bizOrderNo = `PO-LIST-E2E-${Date.now()}`;
    const businessOrderId = preparePayingBusinessOrder(bizOrderNo);
    setMangoPayScenario('PAYING');
    let payOrderNo = '';
    try {
      const payResponse = await page.request.post('/api/payment/cashier/pay', {
        headers,
        data: {
          cashierConfigId: '350001',
          businessOrderId: String(businessOrderId),
          methodCode: 'PERSONAL_WECHAT_QR',
        },
      });
      const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
      expect(payBody.data?.status).toBe('PAYING');
      expect(payBody.data?.payOrderNo).toBeTruthy();
      payOrderNo = payBody.data?.payOrderNo || '';

      const statusesResponse = await page.request.get('/api/payment/payment-orders/statuses', { headers });
      const statusesBody = await expectBusinessOk<PaymentOrderStatus[]>(statusesResponse);
      expect(statusesBody.data?.map(item => item.statusCode)).toEqual(expect.arrayContaining([
        'CREATED',
        'PAYING',
        'SUCCESS',
        'FAILED',
        'CLOSED',
        'DUPLICATE_REFUNDING',
        'DUPLICATE_REFUNDED',
      ]));

      const pageResponse = await page.request.get('/api/payment/payment-orders/page', {
        headers,
        params: { page: '1', size: '10', keyword: payOrderNo, statusCode: 'PAYING' },
      });
      const pageBody = await expectBusinessOk<PageData>(pageResponse);
      expect(Number(pageBody.data?.total || 0)).toBeGreaterThan(0);
      const apiRow = (pageBody.data?.list || []).find(item => item.payOrderNo === payOrderNo) as PaymentOrder | undefined;
      expect(apiRow).toMatchObject({
        payOrderNo,
        bizOrderNo,
        methodCode: 'PERSONAL_WECHAT_QR',
        methodName: '微信扫码',
        channelCode: 'MANGO_PAY',
        status: 'PAYING',
        statusName: '支付中',
      });
      expect(Number(apiRow?.amount || 0)).toBe(128800);
      expect(apiRow?.channelMerchantNo).toBeTruthy();
      expect(apiRow?.successFlag).toBe(0);

      const detailResponse = await page.request.get('/api/payment/payment-orders/detail', {
        headers,
        params: { id: String(apiRow?.id) },
      });
      const detailBody = await expectBusinessOk<PaymentOrder>(detailResponse);
      expect(detailBody.data).toMatchObject({
        payOrderNo,
        bizOrderNo,
        methodCode: 'PERSONAL_WECHAT_QR',
        channelCode: 'MANGO_PAY',
        status: 'PAYING',
        statusName: '支付中',
      });
      expect(detailBody.data?.statusFlows?.map(item => item.statusCode)).toEqual(expect.arrayContaining(['CREATED', 'PAYING']));
      expect(detailBody.data?.statusFlows?.some(item => item.remark?.includes('等待通道回调'))).toBeTruthy();

      await openPaymentPage(page, '/#/payment/payment-orders', '支付订单');
      const toolbar = page.locator('.payment-orders__toolbar');
      await toolbar.getByPlaceholder('支付单号 / 业务单号 / 通道单号 / 方式 / 通道 / 商户号').fill(payOrderNo);
      const statusSelect = toolbar.locator('.el-form-item').filter({ hasText: '支付状态' }).locator('.el-select').first();
      const statusListboxId = await statusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(statusDropdown, '支付中');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/payment-orders/page') && response.url().includes('statusCode=PAYING')),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);

      const row = page.locator('.payment-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: payOrderNo }).first();
      await expect(row).toBeVisible({ timeout: 10000 });
      await expect(row).toContainText(bizOrderNo);
      await expect(row).toContainText('微信扫码');
      await expect(row).toContainText('芒果支付');
      await expect(row).toContainText('￥1288.00');
      await expect(row).toContainText('支付中');
      await expect(row.locator('.el-tag').filter({ hasText: payOrderNo })).toHaveCount(0);
      await expect(row.locator('.el-tag').filter({ hasText: bizOrderNo })).toHaveCount(0);

      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/payment-orders/detail')),
        row.getByRole('button', { name: '详情' }).click(),
      ]);
      const drawer = page.getByRole('dialog').filter({ hasText: '支付订单详情' }).last();
      await expect(drawer.getByText(payOrderNo, { exact: true })).toBeVisible();
      await expect(drawer.getByText(bizOrderNo, { exact: true })).toBeVisible();
      const flowSection = drawer.locator('.payment-orders__flow');
      await expect(flowSection.getByText('状态流转')).toBeVisible();
      await expect(flowSection.getByText('已创建')).toBeVisible();
      await expect(flowSection.getByText('支付中')).toBeVisible();
      await expect(flowSection.getByText('等待通道回调、主动查单或对账补偿推进')).toBeVisible();
      await expect(drawer.getByText('签约能力 ID')).toBeVisible();
      await expect(drawer.getByText('路由规则 ID')).toBeVisible();

      await drawer.locator('.el-drawer__close-btn').click();
      const emptyKeyword = `NO-PAY-ORDER-${Date.now()}`;
      await toolbar.getByPlaceholder('支付单号 / 业务单号 / 通道单号 / 方式 / 通道 / 商户号').fill(emptyKeyword);
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/payment-orders/page') && response.url().includes(encodeURIComponent(emptyKeyword))),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      await expect(page.locator('.payment-orders__table')).toContainText('未查询到匹配的支付订单');
      await expect(page.locator('.payment-orders__table tbody tr')).toHaveCount(0);

      await page.screenshot({ path: 'test-results/payment-orders.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      setMangoPayScenario('SUCCESS');
    }
  });

  test('支付订单退款申请、退款审批和退款订单生成真实闭环', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (
        request.url().includes('/api/payment/payment-orders')
        || request.url().includes('/api/payment/refund-approvals')
        || request.url().includes('/api/payment/refund-orders')
      ) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const bizOrderNo = `REFUND-APPROVAL-E2E-${Date.now()}`;
    const businessOrderId = preparePayingBusinessOrder(bizOrderNo);
    setMangoPayScenario('PAYING');
    setMangoPayRefundScenario('PROCESSING');
    try {
      const payResponse = await page.request.post('/api/payment/cashier/pay', {
        headers,
        data: {
          cashierConfigId: '350001',
          businessOrderId: String(businessOrderId),
          methodCode: 'PERSONAL_WECHAT_QR',
        },
      });
      const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
      const payOrderNo = payBody.data?.payOrderNo || '';
      expect(payOrderNo).toBeTruthy();
      finishProcessingPayment(payOrderNo);

      await openPaymentPage(page, '/#/payment/payment-orders', '支付订单');
      const paymentToolbar = page.locator('.payment-orders__toolbar');
      await paymentToolbar.getByPlaceholder('支付单号 / 业务单号 / 通道单号 / 方式 / 通道 / 商户号').fill(payOrderNo);
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/payment-orders/page') && response.url().includes(encodeURIComponent(payOrderNo))),
        paymentToolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      const paymentRow = page.locator('.payment-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: payOrderNo }).first();
      await expect(paymentRow).toBeVisible({ timeout: 10000 });
      await expect(paymentRow.getByRole('button', { name: '退款' })).toBeEnabled();
      await paymentRow.getByRole('button', { name: '退款' }).click();
      const refundDialog = page.getByRole('dialog').filter({ hasText: '发起退款' }).last();
      await expect(refundDialog).toBeVisible();
      await expect(refundDialog.getByText(payOrderNo, { exact: true })).toBeVisible();
      await refundDialog.locator('.payment-orders__money-input input').fill('388');
      await refundDialog.getByLabel('退款原因').fill('E2E 后台部分退款申请');
      await refundDialog.getByLabel('备注').fill('E2E 覆盖退款审批闭环');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/refund-approvals') && response.request().method() === 'POST'),
        refundDialog.getByRole('button', { name: '提交审批' }).click(),
      ]);
      await expect(page.getByText('退款审批已创建')).toBeVisible({ timeout: 10000 });

      await expect.poll(async () => {
        const response = await page.request.get('/api/payment/refund-approvals/page', {
          headers,
          params: { page: '1', size: '10', keyword: payOrderNo },
        });
        const body = await expectBusinessOk<PageData>(response);
        const item = (body.data?.list || []).find(row => row.payOrderNo === payOrderNo) as PaymentRefundApproval | undefined;
        return item?.status || '';
      }, {
        intervals: [1000, 2000, 3000],
        timeout: 20000,
      }).toBe('APPROVED');
      const approvalPageResponse = await page.request.get('/api/payment/refund-approvals/page', {
        headers,
        params: { page: '1', size: '10', keyword: payOrderNo },
      });
      const approvalPageBody = await expectBusinessOk<PageData>(approvalPageResponse);
      const approval = (approvalPageBody.data?.list || []).find(item => item.payOrderNo === payOrderNo) as PaymentRefundApproval | undefined;
      expect(approval).toMatchObject({
        payOrderNo,
        bizOrderNo,
        status: 'APPROVED',
        statusName: '已通过',
      });
      expect(Number(approval?.refundAmount || 0)).toBe(38800);
      expect(approval?.approvalNo).toBeTruthy();
      expect(approval?.refundOrderId).toBeTruthy();
      expect(approval?.refundOrderNo).toBeTruthy();
      expect([approval?.workflowApplyStatus, approval?.workflowApplyStatusName]).toContain('APPROVED');

      await openPaymentPage(page, '/#/payment/refund-approvals', '退款审批');
      const approvalToolbar = page.locator('.payment-refund-approvals__toolbar');
      await approvalToolbar.getByPlaceholder('审批单号 / 支付单号 / 业务退款号 / 退款单号').fill(payOrderNo);
      const statusSelect = approvalToolbar.locator('.el-form-item').filter({ hasText: '审批状态' }).locator('.el-select').first();
      const statusListboxId = await statusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(statusDropdown, '已通过');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/refund-approvals/page') && response.url().includes('statusCode=APPROVED')),
        approvalToolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      const approvalRow = page.locator('.payment-refund-approvals__table .el-table__body-wrapper tbody tr').filter({ hasText: payOrderNo }).first();
      await expect(approvalRow).toBeVisible({ timeout: 10000 });
      await expect(approvalRow).toContainText('已通过');
      await expect(approvalRow).toContainText(approval?.refundOrderNo || '');
      await expect(approvalRow).toContainText('388.00');
      await expect(approvalRow.getByRole('button', { name: '通过' })).toHaveCount(0);
      await expect(approvalRow.getByRole('button', { name: '拒绝' })).toHaveCount(0);

      const paymentDetailResponse = await page.request.get('/api/payment/payment-orders/detail', {
        headers,
        params: { id: String(approval?.paymentOrderId) },
      });
      const paymentDetailBody = await expectBusinessOk<PaymentOrder>(paymentDetailResponse);
      expect(Number(paymentDetailBody.data?.occupyingRefundAmount || 0)).toBe(38800);
      expect(Number(paymentDetailBody.data?.refundableAmount || 0)).toBe(90000);

      const refundOrderResponse = await page.request.get('/api/payment/refund-orders/page', {
        headers,
        params: { page: '1', size: '10', keyword: approval?.refundOrderNo || '' },
      });
      const refundOrderBody = await expectBusinessOk<PageData>(refundOrderResponse);
      const refundOrder = (refundOrderBody.data?.list || []).find(item => item.payOrderNo === payOrderNo) as PaymentRefundOrder | undefined;
      expect(refundOrder).toMatchObject({
        refundOrderNo: approval?.refundOrderNo,
        payOrderNo,
        bizOrderNo,
        status: 'REFUNDING',
      });
      expect(Number(refundOrder?.refundAmount || 0)).toBe(38800);

      await page.screenshot({ path: 'test-results/payment-refund-approval-flow.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      setMangoPayScenario('SUCCESS');
      setMangoPayRefundScenario('SUCCESS');
    }
  });

  test('退款订单列表、状态筛选和详情真实可用', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/refund-orders')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const bizOrderNo = `RO-LIST-E2E-${Date.now()}`;
    const businessOrderId = preparePayingBusinessOrder(bizOrderNo);
    let payOrderNo = '';
    setMangoPayScenario('PAYING');
    try {
      const payResponse = await page.request.post('/api/payment/cashier/pay', {
        headers,
        data: {
          cashierConfigId: '350001',
          businessOrderId: String(businessOrderId),
          methodCode: 'PERSONAL_WECHAT_QR',
        },
      });
      const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
      payOrderNo = payBody.data?.payOrderNo || '';
      expect(payOrderNo).toBeTruthy();
      finishProcessingPayment(payOrderNo);

      const refundOrderNo = nextE2eRefundOrderNo();
      const bizRefundNo = `BR-E2E-${Date.now()}`;
      createRefundOrderForPayment(payOrderNo, refundOrderNo, bizRefundNo);

      const statusesResponse = await page.request.get('/api/payment/refund-orders/statuses', { headers });
      const statusesBody = await expectBusinessOk<PaymentRefundOrderStatus[]>(statusesResponse);
      expect(statusesBody.data?.map(item => item.statusCode)).toEqual(expect.arrayContaining([
        'CREATED',
        'REFUNDING',
        'SUCCESS',
        'FAILED',
        'CLOSED',
      ]));

      const pageResponse = await page.request.get('/api/payment/refund-orders/page', {
        headers,
        params: { page: '1', size: '10', keyword: refundOrderNo, statusCode: 'REFUNDING' },
      });
      const pageBody = await expectBusinessOk<PageData>(pageResponse);
      expect(Number(pageBody.data?.total || 0)).toBeGreaterThan(0);
      const apiRow = (pageBody.data?.list || []).find(item => item.refundOrderNo === refundOrderNo) as PaymentRefundOrder | undefined;
      expect(apiRow).toMatchObject({
        refundOrderNo,
        bizRefundNo,
        payOrderNo,
        bizOrderNo,
        methodCode: 'PERSONAL_WECHAT_QR',
        methodName: '微信扫码',
        channelCode: 'MANGO_PAY',
        status: 'REFUNDING',
        statusName: '退款中',
      });
      expect(Number(apiRow?.refundAmount || 0)).toBe(38800);

      const detailResponse = await page.request.get('/api/payment/refund-orders/detail', {
        headers,
        params: { id: String(apiRow?.id) },
      });
      const detailBody = await expectBusinessOk<PaymentRefundOrder>(detailResponse);
      expect(detailBody.data).toMatchObject({
        refundOrderNo,
        bizRefundNo,
        payOrderNo,
        bizOrderNo,
        status: 'REFUNDING',
        statusName: '退款中',
      });
      expect(detailBody.data?.statusFlows?.map(item => item.statusCode)).toEqual(expect.arrayContaining(['CREATED', 'REFUNDING']));
      expect(detailBody.data?.statusFlows?.some(item => item.remark?.includes('等待通道回调'))).toBeTruthy();

      await openPaymentPage(page, '/#/payment/refund-orders', '退款订单');
      const toolbar = page.locator('.payment-refund-orders__toolbar');
      await toolbar.getByPlaceholder('退款单号 / 业务退款号 / 支付单号 / 通道单号').fill(refundOrderNo);
      const statusSelect = toolbar.locator('.el-form-item').filter({ hasText: '退款状态' }).locator('.el-select').first();
      const statusListboxId = await statusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(statusDropdown, '退款中');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/refund-orders/page') && response.url().includes('statusCode=REFUNDING')),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);

      const row = page.locator('.payment-refund-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: refundOrderNo }).first();
      await expect(row).toBeVisible({ timeout: 10000 });
      await expect(row).toContainText(bizRefundNo);
      await expect(row).toContainText(payOrderNo);
      await expect(row).toContainText(bizOrderNo);
      await expect(row).toContainText('微信扫码');
      await expect(row).toContainText('芒果支付');
      await expect(row).toContainText('￥388.00');
      await expect(row).toContainText('退款中');
      await expect(row.locator('.el-tag').filter({ hasText: refundOrderNo })).toHaveCount(0);
      await expect(row.locator('.el-tag').filter({ hasText: payOrderNo })).toHaveCount(0);
      await expect(row.getByRole('button', { name: '主动查退款' })).toBeVisible();

      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/refund-orders/detail')),
        row.getByRole('button', { name: '详情' }).click(),
      ]);
      const drawer = page.getByRole('dialog').filter({ hasText: '退款订单详情' }).last();
      await expect(drawer.getByText(refundOrderNo, { exact: true })).toBeVisible();
      await expect(drawer.getByText(bizRefundNo, { exact: true })).toBeVisible();
      await expect(drawer.getByText(payOrderNo, { exact: true })).toBeVisible();
      const flowSection = drawer.locator('.payment-refund-orders__flow');
      await expect(flowSection.getByText('状态流转')).toBeVisible();
      await expect(flowSection.getByText('已创建')).toBeVisible();
      await expect(flowSection.getByText('退款中')).toBeVisible();
      await expect(flowSection.getByText('等待通道回调、主动退款查询或对账补偿推进')).toBeVisible();
      await expect(drawer.getByText('通道退款单号')).toBeVisible();

      await drawer.locator('.el-drawer__close-btn').click();

      setMangoPayRefundScenario('SUCCESS');
      const queryResponsePromise = page.waitForResponse(response =>
        response.url().includes('/api/payment/refund-orders/query-channel') && response.request().method() === 'POST'
      );
      await row.getByRole('button', { name: '主动查退款' }).click();
      const queryResponse = await queryResponsePromise;
      const queryBody = await expectBusinessOk<PaymentRefundOrder>(queryResponse);
      expect(queryBody.data).toMatchObject({
        refundOrderNo,
        bizRefundNo,
        payOrderNo,
        status: 'SUCCESS',
        statusName: '退款成功',
      });

      const queriedPageResponse = await page.request.get('/api/payment/refund-orders/page', {
        headers,
        params: { page: '1', size: '10', keyword: refundOrderNo, statusCode: 'SUCCESS' },
      });
      const queriedPageBody = await expectBusinessOk<PageData>(queriedPageResponse);
      const queriedRefundOrder = (queriedPageBody.data?.list || []).find(item => item.refundOrderNo === refundOrderNo) as PaymentRefundOrder | undefined;
      expect(queriedRefundOrder).toMatchObject({
        refundOrderNo,
        bizRefundNo,
        payOrderNo,
        bizOrderNo,
        status: 'SUCCESS',
        statusName: '退款成功',
      });
      const refundedBusinessOrder = await findBusinessOrderByNo(page, headers, bizOrderNo);
      expect(refundedBusinessOrder).toMatchObject({
        bizOrderNo,
        status: 'PARTIAL_REFUNDED',
      });
      expect(Number(refundedBusinessOrder?.refundedAmount || 0)).toBe(38800);
      const refundFlow = await findRefundSuccessFlow(page, headers, refundOrderNo);
      expect(refundFlow).toMatchObject({
        payOrderNo,
        bizOrderNo,
        refundOrderNo,
        flowType: 'REFUND_SUCCESS',
        flowTypeName: '退款成功支出',
      });
      expectMoneyCents(refundFlow?.amount, 38800);
      expect(findLatestRefundQueryRecord(refundOrderNo)).toMatchObject({
        refundOrderNo,
        beforeStatus: 'REFUNDING',
        channelStatus: 'SUCCESS',
        resultStatus: 'SUCCESS',
        processResult: 'UPDATED',
      });

      await toolbar.getByPlaceholder('退款单号 / 业务退款号 / 支付单号 / 通道单号').fill(refundOrderNo);
      const successStatusListboxId = await statusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const successStatusDropdown = page.locator(`[id="${successStatusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(successStatusDropdown, '退款成功');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/refund-orders/page') && response.url().includes('statusCode=SUCCESS')),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      const successRow = page.locator('.payment-refund-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: refundOrderNo }).first();
      await expect(successRow).toBeVisible({ timeout: 10000 });
      await expect(successRow).toContainText('退款成功');
      await expect(successRow.getByRole('button', { name: '主动查退款' })).toHaveCount(0);

      const emptyKeyword = `NO-REFUND-ORDER-${Date.now()}`;
      await toolbar.getByPlaceholder('退款单号 / 业务退款号 / 支付单号 / 通道单号').fill(emptyKeyword);
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/refund-orders/page') && response.url().includes(encodeURIComponent(emptyKeyword))),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      await expect(page.locator('.payment-refund-orders__table')).toContainText('未查询到匹配的退款订单');
      await expect(page.locator('.payment-refund-orders__table tbody tr')).toHaveCount(0);

      await page.screenshot({ path: 'test-results/payment-refund-orders.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      setMangoPayScenario('SUCCESS');
    }
  });

  test('交易流水列表、查询和详情真实可用', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/transaction-flows')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const bizOrderNo = `FLOW-LIST-E2E-${Date.now()}`;
    const businessOrderId = preparePayingBusinessOrder(bizOrderNo);
    let payOrderNo = '';
    setMangoPayScenario('PAYING');
    try {
      const payResponse = await page.request.post('/api/payment/cashier/pay', {
        headers,
        data: {
          cashierConfigId: '350001',
          businessOrderId: String(businessOrderId),
          methodCode: 'PERSONAL_WECHAT_QR',
        },
      });
      const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
      payOrderNo = payBody.data?.payOrderNo || '';
      expect(payOrderNo).toBeTruthy();
      finishProcessingPayment(payOrderNo);

      const pageResponse = await page.request.get('/api/payment/transaction-flows/page', {
        headers,
        params: { page: '1', size: '10', keyword: payOrderNo },
      });
      const pageBody = await expectBusinessOk<PageData>(pageResponse);
      expect(Number(pageBody.data?.total || 0)).toBeGreaterThan(0);
      const apiRow = (pageBody.data?.list || []).find(item => item.payOrderNo === payOrderNo) as PaymentTransactionFlow | undefined;
      expect(apiRow).toMatchObject({
        bizOrderNo,
        payOrderNo,
        flowType: 'PAY_SUCCESS',
        flowTypeName: '支付成功收入',
      });
      expect(apiRow?.flowNo).toBeTruthy();
      expect(Number(apiRow?.amount || 0)).toBe(128800);

      const detailResponse = await page.request.get('/api/payment/transaction-flows/detail', {
        headers,
        params: { id: String(apiRow?.id) },
      });
      const detailBody = await expectBusinessOk<PaymentTransactionFlow>(detailResponse);
      expect(detailBody.data).toMatchObject({
        flowNo: apiRow?.flowNo,
        bizOrderNo,
        payOrderNo,
        flowType: 'PAY_SUCCESS',
        flowTypeName: '支付成功收入',
      });

      const deleteResponse = await page.request.delete('/api/payment/transaction-flows', {
        headers,
        params: { id: String(apiRow?.id) },
      });
      expect([404, 405]).toContain(deleteResponse.status());
      const afterDeleteDetailResponse = await page.request.get('/api/payment/transaction-flows/detail', {
        headers,
        params: { id: String(apiRow?.id) },
      });
      const afterDeleteDetailBody = await expectBusinessOk<PaymentTransactionFlow>(afterDeleteDetailResponse);
      expect(afterDeleteDetailBody.data?.flowNo).toBe(apiRow?.flowNo);

      await openPaymentPage(page, '/#/payment/transaction-flows', '交易流水');
      const toolbar = page.locator('.payment-transaction-flows__toolbar');
      await toolbar.getByPlaceholder('流水号 / 业务单号 / 支付单号 / 退款单号 / 类型').fill(payOrderNo);
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/transaction-flows/page') && response.url().includes(encodeURIComponent(payOrderNo))),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);

      const row = page.locator('.payment-transaction-flows__table .el-table__body-wrapper tbody tr').filter({ hasText: payOrderNo }).first();
      await expect(row).toBeVisible({ timeout: 10000 });
      await expect(row).toContainText(bizOrderNo);
      await expect(row).toContainText('支付成功收入');
      await expect(row).toContainText('￥1288.00');
      await expect(row.locator('.el-tag').filter({ hasText: payOrderNo })).toHaveCount(0);
      await expect(row.locator('.el-tag').filter({ hasText: bizOrderNo })).toHaveCount(0);
      await expect(row.getByRole('button', { name: '删除' })).toHaveCount(0);

      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/transaction-flows/detail')),
        row.getByRole('button', { name: '详情' }).click(),
      ]);
      const drawer = page.getByRole('dialog').filter({ hasText: '交易流水详情' }).last();
      await expect(drawer.getByText(apiRow?.flowNo || '', { exact: true })).toBeVisible();
      await expect(drawer.getByText(bizOrderNo, { exact: true })).toBeVisible();
      await expect(drawer.getByText(payOrderNo, { exact: true })).toBeVisible();
      await expect(drawer.getByText('关联订单')).toBeVisible();

      await drawer.locator('.el-drawer__close-btn').click();
      const emptyKeyword = `NO-TRANSACTION-FLOW-${Date.now()}`;
      await toolbar.getByPlaceholder('流水号 / 业务单号 / 支付单号 / 退款单号 / 类型').fill(emptyKeyword);
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/transaction-flows/page') && response.url().includes(encodeURIComponent(emptyKeyword))),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      await expect(page.locator('.payment-transaction-flows__table')).toContainText('未查询到匹配的交易流水');
      await expect(page.locator('.payment-transaction-flows__table tbody tr')).toHaveCount(0);
      await page.locator('.payment-transaction-flows__table .el-scrollbar__wrap').first().evaluate((element: Element) => {
        element.scrollLeft = 0;
      });

      await page.screenshot({ path: 'test-results/payment-transaction-flows.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      setMangoPayScenario('SUCCESS');
    }
  });

  test('异常订单列表、详情和受控处理真实可用', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/exception-orders')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const apiBizOrderNo = `BO-EX-E2E-${Date.now()}`;
    const apiBusinessOrderId = preparePayingBusinessOrder(apiBizOrderNo);
    const uiBizOrderNo = `BO-EX-UI-${Date.now()}`;
    const uiBusinessOrderId = preparePayingBusinessOrder(uiBizOrderNo);

    setMangoPayScenario('PAYING');
    try {
      const createPayOrder = async (businessOrderId: number) => {
        const payResponse = await page.request.post('/api/payment/cashier/pay', {
          headers,
          data: {
            cashierConfigId: '350001',
            businessOrderId: String(businessOrderId),
            methodCode: 'PERSONAL_WECHAT_QR',
          },
        });
        const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
        const payOrderNo = payBody.data?.payOrderNo || '';
        expect(payOrderNo).toMatch(PAY_ORDER_NO_PATTERN);
        expect(payBody.data?.status).toBe('PAYING');
        return payOrderNo;
      };
      const apiPayOrderNo = await createPayOrder(apiBusinessOrderId);
      const uiPayOrderNo = await createPayOrder(uiBusinessOrderId);
      prioritizeProcessingPaymentOrder(apiPayOrderNo);
      prioritizeProcessingPaymentOrder(uiPayOrderNo);

      setMangoPayScenario('FAILED');
      const queryTaskResponse = await page.request.post('/api/payment/tasks/query-processing-orders?limit=100', { headers });
      const queryTaskBody = await expectBusinessOk<PaymentTaskDispatchResult>(queryTaskResponse);
      expect(Number(queryTaskBody.data?.scannedCount || 0)).toBeGreaterThan(0);
      expect(Number(queryTaskBody.data?.successCount || 0)).toBeGreaterThanOrEqual(2);

      const apiException = await findExceptionOrderByRelatedNo(page, headers, apiPayOrderNo);
      expect(apiException).toMatchObject({
        relatedOrderNo: apiPayOrderNo,
        exceptionType: 'CHANNEL_FAILED',
        exceptionTypeName: '通道失败',
        severity: 'HIGH',
        severityName: '高',
        handleStatus: 'PENDING',
        handleStatusName: '待处理',
      });
      const exceptionNo = apiException?.exceptionNo || '';
      const exceptionOrderId = apiException?.id || '';
      expect(exceptionNo).toMatch(EXCEPTION_ORDER_NO_PATTERN);
      expect(exceptionOrderId).toBeTruthy();

      const statusesResponse = await page.request.get('/api/payment/exception-orders/statuses', { headers });
      const statusesBody = await expectBusinessOk<PaymentExceptionOrderStatus[]>(statusesResponse);
      expect(statusesBody.data?.map(item => item.statusCode)).toEqual(expect.arrayContaining([
        'PENDING',
        'PROCESSING',
        'HANDLED',
        'IGNORED',
        'CLOSED',
      ]));

      const actionsResponse = await page.request.get('/api/payment/exception-orders/actions', { headers });
      const actionsBody = await expectBusinessOk<PaymentExceptionOrderAction[]>(actionsResponse);
      expect(actionsBody.data?.map(item => item.actionCode)).toEqual(expect.arrayContaining([
        'ACTIVE_QUERY',
        'CLOSE_PAYMENT_ORDER',
        'ADD_EVIDENCE',
        'MANUAL_CLOSE',
      ]));

      const pageResponse = await page.request.get('/api/payment/exception-orders/page', {
        headers,
        params: { page: '1', size: '10', keyword: apiPayOrderNo, statusCode: 'PENDING' },
      });
      const pageBody = await expectBusinessOk<PageData>(pageResponse);
      expect(Number(pageBody.data?.total || 0)).toBeGreaterThan(0);
      const apiRow = (pageBody.data?.list || []).find(item => item.exceptionNo === exceptionNo) as PaymentExceptionOrder | undefined;
      expect(apiRow).toMatchObject({
        id: exceptionOrderId,
        exceptionNo,
        relatedOrderNo: apiPayOrderNo,
        exceptionType: 'CHANNEL_FAILED',
        exceptionTypeName: '通道失败',
        severity: 'HIGH',
        severityName: '高',
        handleStatus: 'PENDING',
        handleStatusName: '待处理',
      });

      const detailResponse = await page.request.get('/api/payment/exception-orders/detail', {
        headers,
        params: { id: exceptionOrderId },
      });
      const detailBody = await expectBusinessOk<PaymentExceptionOrder>(detailResponse);
      expect(detailBody.data).toMatchObject({
        exceptionNo,
        relatedOrderNo: apiPayOrderNo,
        exceptionTypeName: '通道失败',
        severityName: '高',
        handleStatusName: '待处理',
      });
      expect(detailBody.data?.reason).toContain('主动查单发现通道支付失败');

      const handleResponse = await page.request.post('/api/payment/exception-orders/handle', {
        headers,
        data: {
          id: exceptionOrderId,
          handleAction: 'ADD_EVIDENCE',
          handleReason: 'E2E 记录通道失败核对结果',
          handleResult: '确认通道失败异常已进入人工处理闭环',
          handleEvidence: `channel-failed-${exceptionNo}`,
        },
      });
      const handleBody = await expectBusinessOk<PaymentExceptionOrder>(handleResponse);
      expect(handleBody.data).toMatchObject({
        exceptionNo,
        handleStatus: 'PROCESSING',
        handleStatusName: '处理中',
        handleAction: 'ADD_EVIDENCE',
        handleReason: 'E2E 记录通道失败核对结果',
        handleResult: '确认通道失败异常已进入人工处理闭环',
        handleEvidence: `channel-failed-${exceptionNo}`,
      });
      expect(handleBody.data?.handlerName).toBeTruthy();

      const invalidActionResponse = await page.request.post('/api/payment/exception-orders/handle', {
        headers,
        data: {
          id: exceptionOrderId,
          handleAction: 'UNSUPPORTED_ACTION',
          handleReason: '非法动作',
          handleResult: '非法动作应拒绝',
        },
      });
      const invalidActionBody = await expectBusinessError(invalidActionResponse);
      expect(invalidActionBody.code).toBe(3775);

      const audit = await findLatestPaymentAudit(page, headers, {
        action: 'HANDLE_EXCEPTION_ORDER',
        resourceType: 'PAYMENT_EXCEPTION_ORDER',
        resourceId: exceptionNo,
        operationResult: 'SUCCESS',
      });
      expect(audit).toMatchObject({
        operationAction: 'HANDLE_EXCEPTION_ORDER',
        resourceType: 'PAYMENT_EXCEPTION_ORDER',
        resourceId: exceptionNo,
        operationResult: 'SUCCESS',
      });

      const uiException = await findExceptionOrderByRelatedNo(page, headers, uiPayOrderNo);
      expect(uiException).toBeTruthy();
      const uiExceptionNo = uiException?.exceptionNo || '';
      const uiRelatedOrderNo = uiException?.relatedOrderNo || '';
      await openPaymentPage(page, '/#/payment/exception-orders', '异常订单');
      const toolbar = page.locator('.payment-exception-orders__toolbar');
      await toolbar.getByPlaceholder('异常单号 / 关联订单号 / 类型 / 状态').fill(uiExceptionNo);
      const statusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
      const statusListboxId = await statusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(statusDropdown, '待处理');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/page') && response.url().includes('statusCode=PENDING')),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);

      const row = page.locator('.payment-exception-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: uiExceptionNo }).first();
      await expect(row).toBeVisible({ timeout: 10000 });
      await expect(row).toContainText(uiRelatedOrderNo);
      await expect(row).toContainText('通道失败');
      await expect(row).toContainText('待处理');
      await expect(row.locator('.el-tag').filter({ hasText: uiExceptionNo })).toHaveCount(0);
      await expect(row.locator('.el-tag').filter({ hasText: uiRelatedOrderNo })).toHaveCount(0);
      await expect(row.getByRole('button', { name: '删除' })).toHaveCount(0);

      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/detail')),
        row.getByRole('button', { name: '详情' }).click(),
      ]);
      const drawer = page.getByRole('dialog').filter({ hasText: '异常订单详情' }).last();
      await expect(drawer.getByText(uiExceptionNo, { exact: true })).toBeVisible();
      await expect(drawer.getByText(uiRelatedOrderNo, { exact: true })).toBeVisible();
      await expect(drawer.getByText('异常信息')).toBeVisible();
      await expect(drawer.getByText('处理信息')).toBeVisible();
      await drawer.locator('.el-drawer__close-btn').click();

      await row.getByRole('button', { name: '处理' }).click();
      const handleDialog = page.getByRole('dialog').filter({ hasText: '处理异常订单' }).last();
      await expect(formItem(page, '异常单号').locator('input')).toHaveValue(uiExceptionNo);
      await expect(formItem(page, '关联订单').locator('input')).toHaveValue(uiRelatedOrderNo);
      await chooseSelect(page, '处理动作', '补充凭据');
      await fillTextarea(page, '处理原因', 'E2E 页面补充通道失败凭据');
      await fillTextarea(page, '处理结果', 'E2E 页面记录通道失败处理结果');
      await fillTextarea(page, '处理凭据', `ui-channel-failed-${uiExceptionNo}`);
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/handle') && response.request().method() === 'POST'),
        handleDialog.getByRole('button', { name: '保存处理' }).click(),
      ]);
      await expect(page.locator('.el-message').filter({ hasText: '异常订单已处理' }).last()).toBeVisible({ timeout: 10000 });
      await expect(handleDialog).toBeHidden({ timeout: 10000 });

      await toolbar.getByPlaceholder('异常单号 / 关联订单号 / 类型 / 状态').fill(uiExceptionNo);
      const handledStatusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
      const handledListboxId = await handledStatusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const handledDropdown = page.locator(`[id="${handledListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(handledDropdown, '处理中');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/page') && response.url().includes('statusCode=PROCESSING')),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      const handledRow = page.locator('.payment-exception-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: uiExceptionNo }).first();
      await expect(handledRow).toBeVisible({ timeout: 10000 });
      await expect(handledRow).toContainText('处理中');
      await expect(handledRow.getByRole('button', { name: '处理' })).toBeVisible();

      const emptyKeyword = `NO-EXCEPTION-ORDER-${Date.now()}`;
      await toolbar.getByPlaceholder('异常单号 / 关联订单号 / 类型 / 状态').fill(emptyKeyword);
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/page') && response.url().includes(encodeURIComponent(emptyKeyword))),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      await expect(page.locator('.payment-exception-orders__table')).toContainText('未查询到匹配的异常订单');
      await expect(page.locator('.payment-exception-orders__table tbody tr')).toHaveCount(0);
      await page.locator('.payment-exception-orders__table .el-scrollbar__wrap').first().evaluate((element: Element) => {
        element.scrollLeft = 0;
      });

      await page.screenshot({ path: 'test-results/payment-exception-orders.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      setMangoPayScenario('SUCCESS');
    }
  });

  test('异常订单页面可对真实支付订单执行主动查单和关闭支付订单', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/exception-orders')
        || request.url().includes('/api/payment/payment-orders')
        || request.url().includes('/api/payment/business-orders')
        || request.url().includes('/api/payment/refund-orders')
        || request.url().includes('/api/payment/transaction-flows')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);

    const activeBizOrderNo = `BO-EX-AQ-${Date.now()}`;
    const activeBusinessOrderId = preparePayingBusinessOrder(activeBizOrderNo);
    const closeBizOrderNo = `BO-EX-CL-${Date.now()}`;
    const closeBusinessOrderId = preparePayingBusinessOrder(closeBizOrderNo);
    const refundBizOrderNo = `BO-EX-RF-${Date.now()}`;
    const refundBusinessOrderId = preparePayingBusinessOrder(refundBizOrderNo);
    let activePayOrderNo = '';
    let closePayOrderNo = '';
    let refundPayOrderNo = '';
    let refundOrderNo = '';
    setMangoPayScenario('PAYING');
    setMangoPayRefundScenario('PROCESSING');
    try {
      const activePayResponse = await page.request.post('/api/payment/cashier/pay', {
        headers,
        data: {
          cashierConfigId: '350001',
          businessOrderId: String(activeBusinessOrderId),
          methodCode: 'PERSONAL_WECHAT_QR',
        },
      });
      const activePayBody = await expectBusinessOk<CashierPayResult>(activePayResponse);
      activePayOrderNo = activePayBody.data?.payOrderNo || '';
      expect(activePayOrderNo).toMatch(PAY_ORDER_NO_PATTERN);
      expect(activePayBody.data?.status).toBe('PAYING');
      prioritizeProcessingPaymentOrder(activePayOrderNo);

      await createMangoPayScenarioControl(page, headers, {
        scenarioType: 'PAYMENT_QUERY',
        scenarioCode: 'FAILED',
        effectiveCount: 1,
        remark: `E2E 主动查单失败 ${activePayOrderNo}`,
      });
      const queryTaskResponse = await page.request.post('/api/payment/tasks/query-processing-orders?limit=100', { headers });
      const queryTaskBody = await expectBusinessOk<PaymentTaskDispatchResult>(queryTaskResponse);
      expect(Number(queryTaskBody.data?.successCount || 0)).toBeGreaterThanOrEqual(1);
      const activeException = await findExceptionOrderByRelatedNo(page, headers, activePayOrderNo);
      expect(activeException).toMatchObject({
        relatedOrderNo: activePayOrderNo,
        exceptionType: 'CHANNEL_FAILED',
      });
      const activeExceptionNo = activeException?.exceptionNo || '';
      expect(activeExceptionNo).toMatch(EXCEPTION_ORDER_NO_PATTERN);

      setMangoPayScenario('PAYING');
      const closePayResponse = await page.request.post('/api/payment/cashier/pay', {
        headers,
        data: {
          cashierConfigId: '350001',
          businessOrderId: String(closeBusinessOrderId),
          methodCode: 'PERSONAL_WECHAT_QR',
        },
      });
      const closePayBody = await expectBusinessOk<CashierPayResult>(closePayResponse);
      closePayOrderNo = closePayBody.data?.payOrderNo || '';
      expect(closePayOrderNo).toMatch(PAY_ORDER_NO_PATTERN);
      expect(closePayBody.data?.status).toBe('PAYING');
      expireBusinessOrder(closeBizOrderNo);
      const expireTaskResponse = await page.request.post('/api/payment/tasks/expire-open-orders?limit=20', { headers });
      const expireTaskBody = await expectBusinessOk<PaymentTaskDispatchResult>(expireTaskResponse);
      expect(Number(expireTaskBody.data?.successCount || 0)).toBeGreaterThanOrEqual(1);
      const closeException = await findExceptionOrderByRelatedNo(page, headers, closePayOrderNo);
      expect(closeException).toMatchObject({
        relatedOrderNo: closePayOrderNo,
        exceptionType: 'PAY_TIMEOUT',
      });
      const closeExceptionNo = closeException?.exceptionNo || '';
      expect(closeExceptionNo).toMatch(EXCEPTION_ORDER_NO_PATTERN);

      setMangoPayScenario('PAYING');
      const refundPayResponse = await page.request.post('/api/payment/cashier/pay', {
        headers,
        data: {
          cashierConfigId: '350001',
          businessOrderId: String(refundBusinessOrderId),
          methodCode: 'PERSONAL_WECHAT_QR',
        },
      });
      const refundPayBody = await expectBusinessOk<CashierPayResult>(refundPayResponse);
      refundPayOrderNo = refundPayBody.data?.payOrderNo || '';
      expect(refundPayOrderNo).toMatch(PAY_ORDER_NO_PATTERN);
      finishProcessingPayment(refundPayOrderNo);
      refundOrderNo = nextE2eRefundOrderNo();
      const bizRefundNo = `BR${Date.now()}`;
      createRefundOrderForPayment(refundPayOrderNo, refundOrderNo, bizRefundNo);
      const refundPageResponse = await page.request.get('/api/payment/refund-orders/page', {
        headers,
        params: { page: '1', size: '10', keyword: refundOrderNo, statusCode: 'REFUNDING' },
      });
      const refundPageBody = await expectBusinessOk<PageData>(refundPageResponse);
      const refundOrder = (refundPageBody.data?.list || []).find(item => item.refundOrderNo === refundOrderNo) as PaymentRefundOrder | undefined;
      expect(refundOrder?.id).toBeTruthy();
      setMangoPayRefundScenario('FAILED');
      const failedRefundQueryResponse = await page.request.post('/api/payment/refund-orders/query-channel', {
        headers,
        data: { id: refundOrder?.id },
      });
      const failedRefundQueryBody = await expectBusinessOk<PaymentRefundOrder>(failedRefundQueryResponse);
      expect(failedRefundQueryBody.data).toMatchObject({
        refundOrderNo,
        status: 'FAILED',
      });
      const refundException = await findExceptionOrderByRelatedNo(page, headers, refundOrderNo);
      expect(refundException).toMatchObject({
        relatedOrderNo: refundOrderNo,
        exceptionType: 'REFUND_MISMATCH',
      });
      const refundExceptionNo = refundException?.exceptionNo || '';
      expect(refundExceptionNo).toMatch(EXCEPTION_ORDER_NO_PATTERN);

      await openPaymentPage(page, '/#/payment/exception-orders', '异常订单');
      const toolbar = page.locator('.payment-exception-orders__toolbar');
      const searchException = async (keyword: string) => {
        await toolbar.getByPlaceholder('异常单号 / 关联订单号 / 类型 / 状态').fill(keyword);
        const statusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
        const statusListboxId = await statusSelect.evaluate((element: Element) => {
          const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
          (target as HTMLElement).click();
          return target.getAttribute('aria-controls') || '';
        });
        const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
        await clickVisibleOption(statusDropdown, '待处理');
        await Promise.all([
          page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/page') && response.url().includes(encodeURIComponent(keyword))),
          toolbar.getByRole('button', { name: '查询' }).click(),
        ]);
      };
      const handleException = async (exceptionNo: string, payOrderNo: string, actionLabel: string, reason: string, result: string) => {
        await searchException(exceptionNo);
        const row = page.locator('.payment-exception-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: exceptionNo }).first();
        await expect(row).toBeVisible({ timeout: 10000 });
        await expect(row).toContainText(payOrderNo);
        await expect(row).toContainText('待处理');
        await row.getByRole('button', { name: '处理' }).click();
        const handleDialog = page.getByRole('dialog').filter({ hasText: '处理异常订单' }).last();
        await expect(handleDialog).toBeVisible({ timeout: 10000 });
        await expect(formItem(page, '异常单号').locator('input')).toHaveValue(exceptionNo);
        await expect(formItem(page, '关联订单').locator('input')).toHaveValue(payOrderNo);
        await chooseSelect(page, '处理动作', actionLabel);
        await fillTextarea(page, '处理原因', reason);
        await fillTextarea(page, '处理结果', result);
        await fillTextarea(page, '处理凭据', `payment-exception-${exceptionNo}`);
        const [handleResponse] = await Promise.all([
          page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/handle') && response.request().method() === 'POST'),
          handleDialog.getByRole('button', { name: '保存处理' }).click(),
        ]);
        await expectBusinessOk<PaymentExceptionOrder>(handleResponse);
        await expect(page.locator('.el-message').filter({ hasText: '异常订单已处理' }).last()).toBeVisible({ timeout: 10000 });
        await expect(handleDialog).toBeHidden({ timeout: 10000 });
      };

      await handleException(
        activeExceptionNo,
        activePayOrderNo,
        '主动查单',
        'E2E 页面触发失败终态订单主动查单',
        '页面操作后记录支付失败终态的查单结果',
      );

      const queriedPaymentOrder = await findPaymentOrderByNo(page, headers, activePayOrderNo);
      expect(queriedPaymentOrder).toMatchObject({
        payOrderNo: activePayOrderNo,
        bizOrderNo: activeBizOrderNo,
        status: 'FAILED',
        statusName: '支付失败',
        successFlag: 0,
      });
      const queriedBusinessOrder = await findBusinessOrderByNo(page, headers, activeBizOrderNo);
      expect(queriedBusinessOrder).toMatchObject({
        bizOrderNo: activeBizOrderNo,
        status: 'PAYING',
        statusName: '支付中',
      });
      expect(Number(queriedBusinessOrder?.paidAmount || 0)).toBe(0);
      expect(findLatestChannelQueryRecord(activePayOrderNo)).toMatchObject({
        payOrderNo: activePayOrderNo,
        beforeStatus: 'FAILED',
        channelStatus: 'FAILED',
        resultStatus: 'FAILED',
        processResult: 'NO_QUERY_TERMINAL',
      });
      const activeDetailResponse = await page.request.get('/api/payment/exception-orders/page', {
        headers,
        params: { page: '1', size: '10', keyword: activeExceptionNo, statusCode: 'HANDLED' },
      });
      const activeDetailBody = await expectBusinessOk<PageData>(activeDetailResponse);
      const activeHandledRow = (activeDetailBody.data?.list || []).find(item => item.exceptionNo === activeExceptionNo) as PaymentExceptionOrder | undefined;
      expect(activeHandledRow).toMatchObject({
        exceptionNo: activeExceptionNo,
        handleStatus: 'HANDLED',
        handleStatusName: '已处理',
        handleAction: 'ACTIVE_QUERY',
      });
      expect(activeHandledRow?.handleResult).toContain('查单结果：支付订单');
      const activeAudit = await findLatestPaymentAudit(page, headers, {
        action: 'HANDLE_EXCEPTION_ORDER',
        resourceType: 'PAYMENT_EXCEPTION_ORDER',
        resourceId: activeExceptionNo,
        operationResult: 'SUCCESS',
      });
      expect(activeAudit).toMatchObject({
        operationAction: 'HANDLE_EXCEPTION_ORDER',
        resourceType: 'PAYMENT_EXCEPTION_ORDER',
        resourceId: activeExceptionNo,
        operationResult: 'SUCCESS',
      });

      await handleException(
        closeExceptionNo,
        closePayOrderNo,
        '关闭支付订单',
        'E2E 页面复核超时关闭结果',
        '支付订单已由超时任务关闭，页面处理记录关单结果',
      );
      const closedPaymentOrder = await findPaymentOrderByNo(page, headers, closePayOrderNo);
      expect(closedPaymentOrder).toMatchObject({
        payOrderNo: closePayOrderNo,
        bizOrderNo: closeBizOrderNo,
        status: 'CLOSED',
        statusName: '已关闭',
        successFlag: 0,
      });
      const closedBusinessOrder = await findBusinessOrderByNo(page, headers, closeBizOrderNo);
      expect(closedBusinessOrder).toMatchObject({
        bizOrderNo: closeBizOrderNo,
        status: 'CLOSED',
        statusName: '已关闭',
      });
      expect(Number(closedBusinessOrder?.paidAmount || 0)).toBe(0);
      const closeDetailResponse = await page.request.get('/api/payment/exception-orders/page', {
        headers,
        params: { page: '1', size: '10', keyword: closeExceptionNo, statusCode: 'CLOSED' },
      });
      const closeDetailBody = await expectBusinessOk<PageData>(closeDetailResponse);
      const closeHandledRow = (closeDetailBody.data?.list || []).find(item => item.exceptionNo === closeExceptionNo) as PaymentExceptionOrder | undefined;
      expect(closeHandledRow).toMatchObject({
        exceptionNo: closeExceptionNo,
        handleStatus: 'CLOSED',
        handleStatusName: '已关闭',
        handleAction: 'CLOSE_PAYMENT_ORDER',
      });
      expect(closeHandledRow?.handleResult).toContain('关单结果：支付订单');
      const closeAudit = await findLatestPaymentAudit(page, headers, {
        action: 'CLOSE_PAYMENT_ORDER',
        resourceType: 'PAYMENT_ORDER',
        resourceId: closePayOrderNo,
        operationResult: 'SUCCESS',
      });
      expect(closeAudit).toMatchObject({
        operationAction: 'CLOSE_PAYMENT_ORDER',
        resourceType: 'PAYMENT_ORDER',
        resourceId: closePayOrderNo,
        operationResult: 'SUCCESS',
      });

      await searchException(closeExceptionNo);
      const closedRow = page.locator('.payment-exception-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: closeExceptionNo }).first();
      await expect(closedRow).toHaveCount(0);
      await toolbar.getByPlaceholder('异常单号 / 关联订单号 / 类型 / 状态').fill(closeExceptionNo);
      const handledStatusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
      const handledListboxId = await handledStatusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const handledDropdown = page.locator(`[id="${handledListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(handledDropdown, '已关闭');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/page') && response.url().includes('statusCode=CLOSED')),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      const handledCloseRow = page.locator('.payment-exception-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: closeExceptionNo }).first();
      await expect(handledCloseRow).toBeVisible({ timeout: 10000 });
      await expect(handledCloseRow).toContainText('已关闭');
      await expect(handledCloseRow.getByRole('button', { name: '处理' })).toHaveCount(0);

      await searchException(refundExceptionNo);
      const refundExceptionRow = page.locator('.payment-exception-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: refundExceptionNo }).first();
      await expect(refundExceptionRow).toBeVisible({ timeout: 10000 });
      await expect(refundExceptionRow).toContainText(refundOrderNo);
      await expect(refundExceptionRow).toContainText('退款异常');
      await refundExceptionRow.getByRole('button', { name: '处理' }).click();
      const refundHandleDialog = page.getByRole('dialog').filter({ hasText: '处理异常订单' }).last();
      await expect(refundHandleDialog).toBeVisible({ timeout: 10000 });
      await formItem(page, '处理动作').locator('.el-select').click();
      const refundActionDropdown = page.locator('.el-select-dropdown:visible').last();
      await expect(refundActionDropdown.getByText('主动查退款', { exact: true })).toBeVisible();
      await expect(refundActionDropdown.getByText('主动查单', { exact: true })).toHaveCount(0);
      await expect(refundActionDropdown.getByText('关闭支付订单', { exact: true })).toHaveCount(0);
      await refundActionDropdown.getByText('主动查退款', { exact: true }).click();
      await fillTextarea(page, '处理原因', 'E2E 页面触发退款异常主动查退款');
      await fillTextarea(page, '处理结果', '页面操作后记录退款失败终态的查退款结果');
      await fillTextarea(page, '处理凭据', `refund-exception-${refundExceptionNo}`);
      const [refundHandleResponse] = await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/handle') && response.request().method() === 'POST'),
        refundHandleDialog.getByRole('button', { name: '保存处理' }).click(),
      ]);
      const refundHandleBody = await expectBusinessOk<PaymentExceptionOrder>(refundHandleResponse);
      expect(refundHandleBody.data).toMatchObject({
        exceptionNo: refundExceptionNo,
        handleStatus: 'HANDLED',
        handleAction: 'ACTIVE_REFUND_QUERY',
      });
      expect(refundHandleBody.data?.handleResult).toContain('查退款结果：退款订单');
      await expect(refundHandleDialog).toBeHidden({ timeout: 10000 });
      const queriedRefundPageResponse = await page.request.get('/api/payment/refund-orders/page', {
        headers,
        params: { page: '1', size: '10', keyword: refundOrderNo, statusCode: 'FAILED' },
      });
      const queriedRefundPageBody = await expectBusinessOk<PageData>(queriedRefundPageResponse);
      const queriedRefundOrder = (queriedRefundPageBody.data?.list || []).find(item => item.refundOrderNo === refundOrderNo) as PaymentRefundOrder | undefined;
      expect(queriedRefundOrder).toMatchObject({
        refundOrderNo,
        status: 'FAILED',
        statusName: '退款失败',
      });

      await page.screenshot({ path: 'test-results/payment-exception-orders-real-actions.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      setMangoPayScenario('SUCCESS');
      setMangoPayRefundScenario('SUCCESS');
    }
  });

  test('通知记录列表、详情和人工重推真实可用', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/notification-records')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const notificationNo = `NT-E2E-${Date.now()}`;
    const relatedOrderNo = `PO-NT-E2E-${Date.now()}`;
    const notificationRecordId = prepareNotificationRecord(notificationNo, relatedOrderNo, 'FAILED');
    const successNotificationNo = `NT-SUCCESS-E2E-${Date.now()}`;
    const successNotificationRecordId = prepareNotificationRecord(successNotificationNo, `PO-NT-SUCCESS-${Date.now()}`, 'SUCCESS');

    const statusesResponse = await page.request.get('/api/payment/notification-records/statuses', { headers });
    const statusesBody = await expectBusinessOk<PaymentNotificationStatus[]>(statusesResponse);
    expect(statusesBody.data?.map(item => item.statusCode)).toEqual(expect.arrayContaining([
      'SUCCESS',
      'RETRYING',
      'FAILED',
      'PENDING',
    ]));

    const pageResponse = await page.request.get('/api/payment/notification-records/page', {
      headers,
      params: { page: '1', size: '10', keyword: notificationNo, statusCode: 'FAILED' },
    });
    const pageBody = await expectBusinessOk<PageData>(pageResponse);
    expect(Number(pageBody.data?.total || 0)).toBeGreaterThan(0);
    const apiRow = (pageBody.data?.list || []).find(item => item.notificationNo === notificationNo) as PaymentNotificationRecord | undefined;
    expect(apiRow).toMatchObject({
      id: String(notificationRecordId),
      notificationNo,
      relatedOrderNo,
      notificationType: 'PAYMENT_FAILED',
      notificationTypeName: '支付失败通知',
      notifyStatus: 'FAILED',
      notifyStatusName: '通知失败',
      responseCode: '500',
      responseMessage: 'E2E 业务系统临时不可用',
    });

    const detailResponse = await page.request.get('/api/payment/notification-records/detail', {
      headers,
      params: { id: String(notificationRecordId) },
    });
    const detailBody = await expectBusinessOk<PaymentNotificationRecord>(detailResponse);
    expect(detailBody.data).toMatchObject({
      notificationNo,
      relatedOrderNo,
      notificationTypeName: '支付失败通知',
      notifyStatusName: '通知失败',
      targetUrl: 'https://merchant.example.com/payment/notify',
    });

    const retryResponse = await page.request.post('/api/payment/notification-records/retry', {
      headers,
      data: {
        id: String(notificationRecordId),
        retryReason: 'E2E 业务系统恢复后人工补偿推送',
      },
    });
    const retryBody = await expectBusinessOk<PaymentNotificationRecord>(retryResponse);
    expect(retryBody.data).toMatchObject({
      notificationNo,
      notifyStatus: 'RETRYING',
      notifyStatusName: '重试中',
      responseCode: 'MANUAL_RETRY',
      responseMessage: '人工补偿重推已登记',
      lastManualRetryReason: 'E2E 业务系统恢复后人工补偿推送',
      lastManualRetryResult: '人工补偿重推已登记，等待通知任务执行 ACK',
    });
    expect(Number(retryBody.data?.retryTimes || 0)).toBe(3);
    expect(retryBody.data?.lastManualRetryOperatorName).toBeTruthy();

    const successRetryResponse = await page.request.post('/api/payment/notification-records/retry', {
      headers,
      data: {
        id: String(successNotificationRecordId),
        retryReason: '成功通知不允许重复重推',
      },
    });
    const successRetryBody = await expectBusinessError(successRetryResponse);
    expect(successRetryBody.code).toBe(3780);

    const audit = await findLatestPaymentAudit(page, headers, {
      action: 'RETRY_NOTIFICATION_RECORD',
      resourceType: 'PAYMENT_NOTIFICATION_RECORD',
      resourceId: notificationNo,
      operationResult: 'SUCCESS',
    });
    expect(audit).toMatchObject({
      operationAction: 'RETRY_NOTIFICATION_RECORD',
      resourceType: 'PAYMENT_NOTIFICATION_RECORD',
      resourceId: notificationNo,
      operationResult: 'SUCCESS',
    });

    const uiNotificationNo = `NT-UI-E2E-${Date.now()}`;
    const uiRelatedOrderNo = `PO-NT-UI-E2E-${Date.now()}`;
    prepareNotificationRecord(uiNotificationNo, uiRelatedOrderNo, 'FAILED');
    await openPaymentPage(page, '/#/payment/notification-records', '通知记录');
    const toolbar = page.locator('.payment-notification-records__toolbar');
    await toolbar.getByPlaceholder('通知单号 / 关联订单号 / 类型 / 状态').fill(uiNotificationNo);
    const statusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
    const statusListboxId = await statusSelect.evaluate((element: Element) => {
      const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
      (target as HTMLElement).click();
      return target.getAttribute('aria-controls') || '';
    });
    const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
    await clickVisibleOption(statusDropdown, '通知失败');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/notification-records/page') && response.url().includes('statusCode=FAILED')),
      toolbar.getByRole('button', { name: '查询' }).click(),
    ]);

    const row = page.locator('.payment-notification-records__table .el-table__body-wrapper tbody tr').filter({ hasText: uiNotificationNo }).first();
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row).toContainText(uiRelatedOrderNo);
    await expect(row).toContainText('支付失败通知');
    await expect(row).toContainText('通知失败');
    await expect(row.locator('.el-tag').filter({ hasText: uiNotificationNo })).toHaveCount(0);
    await expect(row.locator('.el-tag').filter({ hasText: uiRelatedOrderNo })).toHaveCount(0);
    await expect(row.getByRole('button', { name: '删除' })).toHaveCount(0);

    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/notification-records/detail')),
      row.getByRole('button', { name: '详情' }).click(),
    ]);
    const drawer = page.getByRole('dialog').filter({ hasText: '通知记录详情' }).last();
    await expect(drawer.getByText(uiNotificationNo, { exact: true })).toBeVisible();
    await expect(drawer.getByText(uiRelatedOrderNo, { exact: true })).toBeVisible();
    await expect(drawer.getByText('通知信息')).toBeVisible();
    await expect(drawer.getByText('响应与重试')).toBeVisible();
    await expect(drawer.getByText('人工补偿')).toBeVisible();
    await drawer.locator('.el-drawer__close-btn').click();

    await row.getByRole('button', { name: '重推' }).click();
    const retryDialog = page.getByRole('dialog').filter({ hasText: '人工重推通知' }).last();
    await expect(formItem(page, '通知单号').locator('input')).toHaveValue(uiNotificationNo);
    await expect(formItem(page, '关联订单').locator('input')).toHaveValue(uiRelatedOrderNo);
    await fillTextarea(page, '重推原因', 'E2E 页面确认业务系统恢复后人工补偿推送');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/notification-records/retry') && response.request().method() === 'POST'),
      retryDialog.getByRole('button', { name: '确认重推' }).click(),
    ]);
    await expect(page.locator('.el-message').filter({ hasText: '通知重推已登记' }).last()).toBeVisible({ timeout: 10000 });
    await expect(retryDialog).toBeHidden({ timeout: 10000 });

    await toolbar.getByPlaceholder('通知单号 / 关联订单号 / 类型 / 状态').fill(uiNotificationNo);
    const retryingStatusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
    const retryingListboxId = await retryingStatusSelect.evaluate((element: Element) => {
      const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
      (target as HTMLElement).click();
      return target.getAttribute('aria-controls') || '';
    });
    const retryingDropdown = page.locator(`[id="${retryingListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
    await clickVisibleOption(retryingDropdown, '重试中');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/notification-records/page') && response.url().includes('statusCode=RETRYING')),
      toolbar.getByRole('button', { name: '查询' }).click(),
    ]);
    const retryingRow = page.locator('.payment-notification-records__table .el-table__body-wrapper tbody tr').filter({ hasText: uiNotificationNo }).first();
    await expect(retryingRow).toBeVisible({ timeout: 10000 });
    await expect(retryingRow).toContainText('重试中');

    const deliverReceiver = await startPaymentNotifyReceiver();
    try {
      const dueNotificationNo = `NT-DUE-UI-E2E-${Date.now()}`;
      const dueRelatedOrderNo = `PO-DUE-UI-E2E-${Date.now()}`;
      prepareDueNotificationRecord(dueNotificationNo, dueRelatedOrderNo, deliverReceiver.url, {
        notifyStatus: 'FAILED',
        retryTimes: 0,
        responseCode: '500',
        responseMessage: 'E2E 等待页面投递到期通知',
      });
      await toolbar.getByRole('button', { name: '投递到期通知' }).click();
      const confirmDialog = page.getByRole('dialog').filter({ hasText: '投递到期通知' }).last();
      await expect(confirmDialog.getByText('不会修改资金状态')).toBeVisible();
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/notification-records/deliver-due') && response.request().method() === 'POST'),
        page.waitForResponse(response => response.url().includes('/api/payment/notification-records/page')),
        confirmDialog.getByRole('button', { name: '确认投递' }).click(),
      ]);
      await expect(page.locator('.el-message').filter({ hasText: /已投递|暂无到期通知/ }).last()).toBeVisible({ timeout: 10000 });
      const delivered = await deliverReceiver.waitFor('PAYMENT_SUCCESS');
      expect(delivered).toMatchObject({
        notifyNo: dueNotificationNo,
        notificationType: 'PAYMENT_SUCCESS',
        payOrderNo: dueRelatedOrderNo,
        status: 'SUCCESS',
        channelCode: 'MANGO_PAY',
      });
      const deliverAudit = await findLatestPaymentAudit(page, headers, {
        action: 'DELIVER_DUE_NOTIFICATION_RECORDS',
        resourceType: 'PAYMENT_NOTIFICATION_RECORD',
        resourceId: 'DUE_NOTIFICATION_RECORDS',
        operationResult: 'SUCCESS',
      });
      expect(deliverAudit).toMatchObject({
        operationAction: 'DELIVER_DUE_NOTIFICATION_RECORDS',
        resourceType: 'PAYMENT_NOTIFICATION_RECORD',
        resourceId: 'DUE_NOTIFICATION_RECORDS',
        operationResult: 'SUCCESS',
      });
      await toolbar.getByPlaceholder('通知单号 / 关联订单号 / 类型 / 状态').fill(dueNotificationNo);
      const successStatusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
      const successListboxId = await successStatusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const successDropdown = page.locator(`[id="${successListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(successDropdown, '通知成功');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/notification-records/page') && response.url().includes('statusCode=SUCCESS')),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      const deliveredRow = page.locator('.payment-notification-records__table .el-table__body-wrapper tbody tr')
        .filter({ hasText: dueNotificationNo })
        .first();
      await expect(deliveredRow).toBeVisible({ timeout: 10000 });
      await expect(deliveredRow).toContainText(dueRelatedOrderNo);
      await expect(deliveredRow).toContainText('通知成功');
    } finally {
      await deliverReceiver.close();
    }

    const emptyKeyword = `NO-NOTIFICATION-RECORD-${Date.now()}`;
    await toolbar.getByPlaceholder('通知单号 / 关联订单号 / 类型 / 状态').fill(emptyKeyword);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/notification-records/page') && response.url().includes(encodeURIComponent(emptyKeyword))),
      toolbar.getByRole('button', { name: '查询' }).click(),
    ]);
    await expect(page.locator('.payment-notification-records__table')).toContainText('未查询到匹配的通知记录');
    await expect(page.locator('.payment-notification-records__table tbody tr')).toHaveCount(0);
    await page.locator('.payment-notification-records__table .el-scrollbar__wrap').first().evaluate((element: Element) => {
      element.scrollLeft = 0;
    });

    await page.screenshot({ path: 'test-results/payment-notification-records.png', fullPage: true });
    expect(runtimeErrors).toEqual([]);
  });

  test('到期通知可由支付模块调度器自动投递并在页面回显成功', async ({ page }) => {
    test.setTimeout(120_000);
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/notification-records')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const notifyReceiver = await startPaymentNotifyReceiver();
    try {
      const notificationNo = `NT-AUTO-E2E-${Date.now()}`;
      const relatedOrderNo = `PO-AUTO-E2E-${Date.now()}`;
      mysqlExec(`
        DELETE FROM payment_notification_record
        WHERE tenant_id = 1
          AND notification_no LIKE 'NT-AUTO-E2E-%';
      `);
      const notificationRecordId = prepareDueNotificationRecord(notificationNo, relatedOrderNo, notifyReceiver.url);

      const delivered = await notifyReceiver.waitFor('PAYMENT_SUCCESS', 75_000);
      expect(delivered).toMatchObject({
        notifyNo: notificationNo,
        notificationType: 'PAYMENT_SUCCESS',
        payOrderNo: relatedOrderNo,
        status: 'SUCCESS',
        channelCode: 'MANGO_PAY',
      });

      await expect.poll(async () => {
        const detailResponse = await page.request.get('/api/payment/notification-records/detail', {
          headers,
          params: { id: String(notificationRecordId) },
        });
        const detailBody = await expectBusinessOk<PaymentNotificationRecord>(detailResponse);
        return detailBody.data?.notifyStatus;
      }, {
        timeout: 30_000,
        intervals: [500, 1000, 2000],
      }).toBe('SUCCESS');

      const detailResponse = await page.request.get('/api/payment/notification-records/detail', {
        headers,
        params: { id: String(notificationRecordId) },
      });
      const detailBody = await expectBusinessOk<PaymentNotificationRecord>(detailResponse);
      expect(detailBody.data).toMatchObject({
        notificationNo,
        relatedOrderNo,
        notificationTypeName: '支付成功通知',
        notifyStatus: 'SUCCESS',
        notifyStatusName: '通知成功',
        responseCode: '200',
      });
      expect(Number(detailBody.data?.retryTimes || 0)).toBe(1);

      await openPaymentPage(page, '/#/payment/notification-records', '通知记录');
      const toolbar = page.locator('.payment-notification-records__toolbar');
      await toolbar.getByPlaceholder('通知单号 / 关联订单号 / 类型 / 状态').fill(notificationNo);
      const statusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
      const statusListboxId = await statusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(statusDropdown, '通知成功');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/notification-records/page') && response.url().includes('statusCode=SUCCESS')),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);

      const row = page.locator('.payment-notification-records__table .el-table__body-wrapper tbody tr').filter({ hasText: notificationNo }).first();
      await expect(row).toBeVisible({ timeout: 10000 });
      await expect(row).toContainText(relatedOrderNo);
      await expect(row).toContainText('支付成功通知');
      await expect(row).toContainText('通知成功');
      await expect(row.getByRole('button', { name: '重推' })).toHaveCount(0);
      await page.screenshot({ path: 'test-results/payment-notification-auto-dispatch.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      await notifyReceiver.close();
    }
  });

  test('通知失败按应用重试策略推进并在耗尽后保留人工补偿入口', async ({ page }) => {
    test.setTimeout(90_000);
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/notification-records')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const retryReceiver = await startPaymentNotifyReceiver('WAIT');
    const exhaustedReceiver = await startPaymentNotifyReceiver('WAIT');
    try {
      const appId = `E2E_NOTIFY_RETRY_${Date.now()}`;
      prepareNotificationRetryApplication(appId, '1m,5m,15m');

      const retryNotificationNo = `NT-RETRY-E2E-${Date.now()}`;
      const retryRelatedOrderNo = `PO-RETRY-E2E-${Date.now()}`;
      const retryRecordId = prepareDueNotificationRecord(retryNotificationNo, retryRelatedOrderNo, retryReceiver.url, {
        appId,
        notifyStatus: 'FAILED',
        retryTimes: 0,
        responseCode: '500',
        responseMessage: 'E2E 首次通知失败',
      });
      const retryDispatchResponse = await page.request.post('/api/payment/notification-records/deliver-due', {
        headers,
        params: { limit: '20' },
      });
      await expectBusinessOk<number>(retryDispatchResponse);

      await expect.poll(async () => retryReceiver.notifications.length, {
        timeout: 10_000,
        intervals: [200, 500, 1000],
      }).toBe(1);
      const retryDetailResponse = await page.request.get('/api/payment/notification-records/detail', {
        headers,
        params: { id: String(retryRecordId) },
      });
      const retryDetailBody = await expectBusinessOk<PaymentNotificationRecord>(retryDetailResponse);
      expect(retryDetailBody.data).toMatchObject({
        notificationNo: retryNotificationNo,
        relatedOrderNo: retryRelatedOrderNo,
        notifyStatus: 'FAILED',
        notifyStatusName: '通知失败',
        responseCode: '200',
        responseMessage: 'WAIT',
      });
      expect(Number(retryDetailBody.data?.retryTimes || 0)).toBe(1);
      expect(retryDetailBody.data?.nextRetryTime).toBeTruthy();
      expect(new Date(retryDetailBody.data?.nextRetryTime || '').getTime()).toBeGreaterThan(Date.now() + 4 * 60 * 1000);
      expect(new Date(retryDetailBody.data?.nextRetryTime || '').getTime()).toBeLessThan(Date.now() + 6 * 60 * 1000);

      const exhaustedNotificationNo = `NT-EXHAUST-E2E-${Date.now()}`;
      const exhaustedRelatedOrderNo = `PO-EXHAUST-E2E-${Date.now()}`;
      const exhaustedRecordId = prepareDueNotificationRecord(exhaustedNotificationNo, exhaustedRelatedOrderNo, exhaustedReceiver.url, {
        appId,
        notifyStatus: 'FAILED',
        retryTimes: 2,
        responseCode: '500',
        responseMessage: 'E2E 第二次通知仍失败',
      });
      const exhaustedDispatchResponse = await page.request.post('/api/payment/notification-records/deliver-due', {
        headers,
        params: { limit: '20' },
      });
      await expectBusinessOk<number>(exhaustedDispatchResponse);

      await expect.poll(async () => exhaustedReceiver.notifications.length, {
        timeout: 10_000,
        intervals: [200, 500, 1000],
      }).toBe(1);
      const exhaustedDetailResponse = await page.request.get('/api/payment/notification-records/detail', {
        headers,
        params: { id: String(exhaustedRecordId) },
      });
      const exhaustedDetailBody = await expectBusinessOk<PaymentNotificationRecord>(exhaustedDetailResponse);
      expect(exhaustedDetailBody.data).toMatchObject({
        notificationNo: exhaustedNotificationNo,
        relatedOrderNo: exhaustedRelatedOrderNo,
        notifyStatus: 'FAILED',
        notifyStatusName: '通知失败',
        responseCode: '200',
      });
      expect(Number(exhaustedDetailBody.data?.retryTimes || 0)).toBe(3);
      expect(exhaustedDetailBody.data?.nextRetryTime || '').toBe('');
      expect(exhaustedDetailBody.data?.responseMessage || '').toContain('通知重试策略已耗尽，等待人工补偿重推');

      await openPaymentPage(page, '/#/payment/notification-records', '通知记录');
      const toolbar = page.locator('.payment-notification-records__toolbar');
      await toolbar.getByPlaceholder('通知单号 / 关联订单号 / 类型 / 状态').fill(exhaustedNotificationNo);
      const statusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
      const statusListboxId = await statusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(statusDropdown, '通知失败');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/notification-records/page') && response.url().includes('statusCode=FAILED')),
        toolbar.getByRole('button', { name: '查询' }).click(),
      ]);

      const exhaustedRow = page.locator('.payment-notification-records__table .el-table__body-wrapper tbody tr')
        .filter({ hasText: exhaustedNotificationNo })
        .first();
      await expect(exhaustedRow).toBeVisible({ timeout: 10000 });
      await expect(exhaustedRow).toContainText(exhaustedRelatedOrderNo);
      await expect(exhaustedRow).toContainText('通知失败');
      await expect(exhaustedRow.getByRole('button', { name: '重推' })).toBeVisible();
      await page.screenshot({ path: 'test-results/payment-notification-retry-policy.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      await retryReceiver.close();
      await exhaustedReceiver.close();
    }
  });

  test('支付失败关闭和退款失败通知可由真实状态流触发并在页面回显', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/exception-orders')
        || request.url().includes('/api/payment/refund-orders')
        || request.url().includes('/api/payment/notification-records')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const notifyReceiver = await startPaymentNotifyReceiver();
    configureOrderCenterNotificationSecret();
    setMangoPayScenario('PAYING');
    setMangoPayRefundScenario('PROCESSING');
    try {
      const failedBizOrderNo = `BO-NT-F-${Date.now()}`;
      const refundBizOrderNo = `BO-NT-R-${Date.now()}`;
      const failedBusinessOrderId = preparePayingBusinessOrder(failedBizOrderNo, notifyReceiver.url);
      const refundBusinessOrderId = preparePayingBusinessOrder(refundBizOrderNo, notifyReceiver.url);

      const createPayOrder = async (businessOrderId: number) => {
        const response = await page.request.post('/api/payment/cashier/pay', {
          headers,
          data: {
            cashierConfigId: '350001',
            businessOrderId: String(businessOrderId),
            methodCode: 'PERSONAL_WECHAT_QR',
          },
        });
        const body = await expectBusinessOk<CashierPayResult>(response);
        const payOrderNo = body.data?.payOrderNo || '';
        expect(payOrderNo).toMatch(PAY_ORDER_NO_PATTERN);
        expect(body.data?.status).toBe('PAYING');
        return payOrderNo;
      };

      const failedPayOrderNo = await createPayOrder(failedBusinessOrderId);
      prioritizeProcessingPaymentOrder(failedPayOrderNo);
      const refundPayOrderNo = await createPayOrder(refundBusinessOrderId);
      finishProcessingPayment(refundPayOrderNo);
      const refundOrderNo = nextE2eRefundOrderNo();
      const bizRefundNo = `RF-NT-F-${Date.now()}`;
      createRefundOrderForPayment(refundPayOrderNo, refundOrderNo, bizRefundNo);

      setMangoPayScenario('FAILED');
      const queryTaskResponse = await page.request.post('/api/payment/tasks/query-processing-orders?limit=100', { headers });
      const queryTaskBody = await expectBusinessOk<PaymentTaskDispatchResult>(queryTaskResponse);
      expect(Number(queryTaskBody.data?.successCount || 0)).toBeGreaterThanOrEqual(1);
      const failedException = await findExceptionOrderByRelatedNo(page, headers, failedPayOrderNo);
      expect(failedException).toMatchObject({
        relatedOrderNo: failedPayOrderNo,
        exceptionType: 'CHANNEL_FAILED',
      });
      const failedExceptionNo = failedException?.exceptionNo || '';
      expect(failedExceptionNo).toMatch(EXCEPTION_ORDER_NO_PATTERN);

      setMangoPayScenario('PAYING');
      const closedBizOrderNo = `BO-NT-C-${Date.now()}`;
      const closedBusinessOrderId = preparePayingBusinessOrder(closedBizOrderNo, notifyReceiver.url);
      const closedPayOrderNo = await createPayOrder(closedBusinessOrderId);
      expireBusinessOrder(closedBizOrderNo);
      const expireTaskResponse = await page.request.post('/api/payment/tasks/expire-open-orders?limit=100', { headers });
      const expireTaskBody = await expectBusinessOk<PaymentTaskDispatchResult>(expireTaskResponse);
      expect(Number(expireTaskBody.data?.successCount || 0)).toBeGreaterThanOrEqual(1);
      const closedException = await findExceptionOrderByRelatedNo(page, headers, closedPayOrderNo);
      expect(closedException).toMatchObject({
        relatedOrderNo: closedPayOrderNo,
        exceptionType: 'PAY_TIMEOUT',
      });
      const closedExceptionNo = closedException?.exceptionNo || '';
      expect(closedExceptionNo).toMatch(EXCEPTION_ORDER_NO_PATTERN);

      await openPaymentPage(page, '/#/payment/exception-orders', '异常订单');
      const exceptionToolbar = page.locator('.payment-exception-orders__toolbar');
      const searchPendingException = async (exceptionNo: string) => {
        await exceptionToolbar.getByPlaceholder('异常单号 / 关联订单号 / 类型 / 状态').fill(exceptionNo);
        const statusSelect = exceptionToolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
        const statusListboxId = await statusSelect.evaluate((element: Element) => {
          const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
          (target as HTMLElement).click();
          return target.getAttribute('aria-controls') || '';
        });
        const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
        await clickVisibleOption(statusDropdown, '待处理');
        await Promise.all([
          page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/page') && response.url().includes(encodeURIComponent(exceptionNo))),
          exceptionToolbar.getByRole('button', { name: '查询' }).click(),
        ]);
      };
      const handleException = async (exceptionNo: string, actionLabel: string) => {
        await searchPendingException(exceptionNo);
        const row = page.locator('.payment-exception-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: exceptionNo }).first();
        await expect(row).toBeVisible({ timeout: 10000 });
        await row.getByRole('button', { name: '处理' }).click();
        const handleDialog = page.getByRole('dialog').filter({ hasText: '处理异常订单' }).last();
        await expect(handleDialog).toBeVisible({ timeout: 10000 });
        await chooseSelect(page, '处理动作', actionLabel);
        await fillTextarea(page, '处理原因', `E2E 触发${actionLabel}业务通知`);
        await fillTextarea(page, '处理结果', `E2E 真实状态流生成${actionLabel}结果`);
        await fillTextarea(page, '处理凭据', `payment-notify-${exceptionNo}`);
        await Promise.all([
          page.waitForResponse(response => response.url().includes('/api/payment/exception-orders/handle') && response.request().method() === 'POST'),
          handleDialog.getByRole('button', { name: '保存处理' }).click(),
        ]);
        await expect(handleDialog).toBeHidden({ timeout: 10000 });
      };

      await handleException(failedExceptionNo, '主动查单');
      const failedNotification = await notifyReceiver.waitFor('PAYMENT_FAILED');
      expect(failedNotification).toMatchObject({
        notificationType: 'PAYMENT_FAILED',
        bizOrderNo: failedBizOrderNo,
        payOrderNo: failedPayOrderNo,
        status: 'FAILED',
      });

      await handleException(closedExceptionNo, '关闭支付订单');
      const closedNotification = await notifyReceiver.waitFor('PAYMENT_CLOSED');
      expect(closedNotification).toMatchObject({
        notificationType: 'PAYMENT_CLOSED',
        bizOrderNo: closedBizOrderNo,
        payOrderNo: closedPayOrderNo,
        status: 'CLOSED',
      });

      await openPaymentPage(page, '/#/payment/refund-orders', '退款订单');
      const refundToolbar = page.locator('.payment-refund-orders__toolbar');
      await refundToolbar.getByPlaceholder('退款单号 / 业务退款号 / 支付单号 / 通道单号').fill(refundOrderNo);
      const refundStatusSelect = refundToolbar.locator('.el-form-item').filter({ hasText: '退款状态' }).locator('.el-select').first();
      const refundStatusListboxId = await refundStatusSelect.evaluate((element: Element) => {
        const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
        (target as HTMLElement).click();
        return target.getAttribute('aria-controls') || '';
      });
      const refundStatusDropdown = page.locator(`[id="${refundStatusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
      await clickVisibleOption(refundStatusDropdown, '退款中');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/refund-orders/page') && response.url().includes('statusCode=REFUNDING')),
        refundToolbar.getByRole('button', { name: '查询' }).click(),
      ]);
      const refundRow = page.locator('.payment-refund-orders__table .el-table__body-wrapper tbody tr').filter({ hasText: refundOrderNo }).first();
      await expect(refundRow).toBeVisible({ timeout: 10000 });
      setMangoPayRefundScenario('FAILED');
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/refund-orders/query-channel') && response.request().method() === 'POST'),
        refundRow.getByRole('button', { name: '主动查退款' }).click(),
      ]);
      const refundFailedNotification = await notifyReceiver.waitFor(
        'REFUND_FAILED',
        10_000,
        notification => notification.refundOrderNo === refundOrderNo,
      );
      expect(refundFailedNotification).toMatchObject({
        notificationType: 'REFUND_FAILED',
        bizOrderNo: refundBizOrderNo,
        payOrderNo: refundPayOrderNo,
        refundOrderNo,
        bizRefundNo,
        status: 'FAILED',
      });

      await openPaymentPage(page, '/#/payment/notification-records', '通知记录');
      const notificationToolbar = page.locator('.payment-notification-records__toolbar');
      const assertNotificationRow = async (notification: PaymentOpenNotification, typeName: string, relatedOrderNo: string) => {
        await notificationToolbar.getByPlaceholder('通知单号 / 关联订单号 / 类型 / 状态').fill(notification.notifyNo || '');
        const statusSelect = notificationToolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
        const statusListboxId = await statusSelect.evaluate((element: Element) => {
          const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
          (target as HTMLElement).click();
          return target.getAttribute('aria-controls') || '';
        });
        const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
        await clickVisibleOption(statusDropdown, '通知成功');
        await Promise.all([
          page.waitForResponse(response => response.url().includes('/api/payment/notification-records/page') && response.url().includes('statusCode=SUCCESS')),
          notificationToolbar.getByRole('button', { name: '查询' }).click(),
        ]);
        const row = page.locator('.payment-notification-records__table .el-table__body-wrapper tbody tr').filter({ hasText: notification.notifyNo || '' }).first();
        await expect(row).toBeVisible({ timeout: 10000 });
        await expect(row).toContainText(relatedOrderNo);
        await expect(row).toContainText(typeName);
        await expect(row).toContainText('通知成功');
      };
      await assertNotificationRow(failedNotification, '支付失败通知', failedPayOrderNo);
      await assertNotificationRow(closedNotification, '支付关闭通知', closedPayOrderNo);
      await assertNotificationRow(refundFailedNotification, '退款失败通知', refundOrderNo);
      await page.screenshot({ path: 'test-results/payment-terminal-notifications.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      setMangoPayScenario('SUCCESS');
      setMangoPayRefundScenario('SUCCESS');
      await notifyReceiver.close();
    }
  });

  test('对账管理导入、详情、重复文件校验和差异生成真实可用', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/reconciliations')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const bizOrderNo = `RC-BIZ-E2E-${suffix}`;
    const businessOrderId = preparePayingBusinessOrder(bizOrderNo);
    const payResponse = await page.request.post('/api/payment/cashier/pay', {
      headers,
      data: {
        cashierConfigId: '350001',
        businessOrderId: String(businessOrderId),
        methodCode: 'PERSONAL_WECHAT_QR',
      },
    });
    const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
    const payOrderNo = payBody.data?.payOrderNo || '';
    expect(payOrderNo).toBeTruthy();
    const paymentOrder = await findPaymentOrderByNo(page, headers, payOrderNo);
    expect(paymentOrder?.channelTradeNo).toBeTruthy();
    const channelTradeNo = paymentOrder?.channelTradeNo || '';
    const billDate = `2026-05-${String(10 + Number(suffix.slice(-1))).padStart(2, '0')}`;
    moveHistoricalReconciliationE2eOrdersOutOfBillDate('RC-BIZ-E2E-', billDate, payOrderNo);
    movePaymentSuccessTime(payOrderNo, billDate, '10:30:00');
    const reconciliationChannelCode = 'MANGO_PAY';
    const digest = `sha256-special-recon-e2e-${suffix}`;

    const statusesResponse = await page.request.get('/api/payment/reconciliations/statuses', { headers });
    const statusesBody = await expectBusinessOk<PaymentReconciliationStatus[]>(statusesResponse);
    expect(statusesBody.data?.map(item => item.statusCode)).toEqual(expect.arrayContaining([
      'IMPORTED',
      'MATCHED',
      'DIFFERENCE',
    ]));

    const importResponse = await page.request.post('/api/payment/reconciliations/import', {
      headers,
      data: {
        channelCode: reconciliationChannelCode,
        billDate,
        billFileName: `mango-pay-recon-${suffix}.csv`,
        fileDigest: digest,
        items: [{
          channelTradeNo,
          tradeType: 'PAYMENT',
          amount: 128800,
          fee: 128,
          tradeTime: `${billDate} 10:30:00`,
        }],
      },
    });
    const importBody = await expectBusinessOk<PaymentReconciliation>(importResponse);
    expect(importBody.data).toMatchObject({
      channelCode: reconciliationChannelCode,
      billDate,
      totalCount: 1,
      matchStatus: 'MATCHED',
      matchStatusName: '已平账',
      billFileName: `mango-pay-recon-${suffix}.csv`,
      fileDigest: digest,
    });
    expectMoneyCents(importBody.data?.totalAmount, 128800);
    expectMoneyCents(importBody.data?.totalFee, 128);
    expect(importBody.data?.details?.[0]).toMatchObject({
      channelTradeNo,
      tradeType: 'PAYMENT',
      tradeTypeName: '支付',
      matchStatus: 'MATCHED',
      matchStatusName: '已平账',
      matchedOrderNo: payOrderNo,
    });
    expect(importBody.data?.details?.[0]?.matchMessage).toContain('支付');
    expectMoneyCents(importBody.data?.details?.[0]?.amount, 128800);
    expectMoneyCents(importBody.data?.details?.[0]?.fee, 128);

    const duplicateResponse = await page.request.post('/api/payment/reconciliations/import', {
      headers,
      data: {
        channelCode: reconciliationChannelCode,
        billDate,
        billFileName: `mango-pay-recon-${suffix}.csv`,
        fileDigest: digest,
        items: [{
          channelTradeNo,
          tradeType: 'PAYMENT',
          amount: 128800,
          fee: 128,
          tradeTime: `${billDate} 10:30:00`,
        }],
      },
    });
    const duplicateBody = await expectBusinessError(duplicateResponse);
    expect(duplicateBody.code).toBe(3783);

    const missingTradeNo = `CH-MISSING-${suffix}`;
    const diffDigest = `sha256-special-recon-diff-e2e-${suffix}`;
    const diffResponse = await page.request.post('/api/payment/reconciliations/import', {
      headers,
      data: {
        channelCode: reconciliationChannelCode,
        billDate,
        billFileName: `mango-pay-recon-diff-${suffix}.csv`,
        fileDigest: diffDigest,
        items: [{
          channelTradeNo: missingTradeNo,
          tradeType: 'PAYMENT',
          amount: 9900,
          fee: 9,
          tradeTime: `${billDate} 11:30:00`,
        }],
      },
    });
    const diffBody = await expectBusinessOk<PaymentReconciliation>(diffResponse);
    expect(diffBody.data).toMatchObject({
      matchStatus: 'DIFFERENCE',
      matchStatusName: '存在差异',
    });
    expectMoneyCents(diffBody.data?.totalAmount, 9900);
    expectMoneyCents(diffBody.data?.totalFee, 9);
    expect(diffBody.data?.details?.[0]).toMatchObject({
      channelTradeNo: missingTradeNo,
      matchStatus: 'DIFFERENCE',
      matchMessage: '通道成功但本地未找到支付订单',
    });

    const differenceResponse = await page.request.get('/api/payment/differences/page', {
      headers,
      params: { page: '1', size: '10', keyword: missingTradeNo },
    });
    const differenceBody = await expectBusinessOk<PageData>(differenceResponse);
    const difference = (differenceBody.data?.list || []).find(item => item.relatedOrderNo === missingTradeNo);
    expect(difference).toMatchObject({
      differenceType: 'CHANNEL_SUCCESS_LOCAL_MISSING',
      processStatus: 'PENDING',
    });
    expectMoneyCents(difference?.differenceAmount as number | string | undefined, 9900);

    const audit = await findLatestPaymentAudit(page, headers, {
      action: 'IMPORT_RECONCILIATION',
      resourceType: 'PAYMENT_RECONCILIATION',
      resourceId: importBody.data?.reconciliationNo || '',
      operationResult: 'SUCCESS',
    });
    expect(audit).toMatchObject({
      operationAction: 'IMPORT_RECONCILIATION',
      resourceType: 'PAYMENT_RECONCILIATION',
      resourceId: importBody.data?.reconciliationNo,
      operationResult: 'SUCCESS',
    });

    await openPaymentPage(page, '/#/payment/reconciliations', '对账管理');
    await page.getByPlaceholder('批次号 / 通道 / 文件 / 导入人').fill(importBody.data?.reconciliationNo || '');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/reconciliations/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const row = page.locator('.payment-reconciliations__table .el-table__body-wrapper tbody tr').filter({ hasText: importBody.data?.reconciliationNo || '' }).first();
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row).toContainText(reconciliationChannelCode);
    await expect(row).toContainText('已平账');
    await expect(row.getByRole('button', { name: '删除' })).toHaveCount(0);

    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/reconciliations/detail')),
      row.getByRole('button', { name: '详情' }).click(),
    ]);
    const drawer = page.getByRole('dialog').filter({ hasText: '对账批次详情' }).last();
    await expect(drawer.getByText(importBody.data?.reconciliationNo || '', { exact: true })).toBeVisible();
    await expect(drawer.getByText(channelTradeNo, { exact: true })).toBeVisible();
    await expect(drawer.getByText(payOrderNo, { exact: true })).toBeVisible();
    await drawer.locator('.el-drawer__close-btn').click();

    await page.getByRole('button', { name: '导入账单' }).click();
    const importDialog = page.getByRole('dialog').filter({ hasText: '导入通道账单' }).last();
    await fillInput(page, '通道编码', reconciliationChannelCode);
    await fillInput(page, '文件名', `special-ui-recon-${suffix}.csv`);
    await fillInput(page, '文件摘要', `sha256-special-ui-recon-${suffix}`);
    await formItem(page, '账单日期').locator('input').fill(billDate);
    await importDialog.locator('input[placeholder="通道交易号"]').first().fill(`CH-UI-${suffix}`);
    await importDialog.locator('.el-input-number input').nth(0).fill('18800');
    await importDialog.locator('.el-input-number input').nth(1).fill('18');
    await importDialog.locator('input[placeholder="选择交易时间"]').fill(`${billDate} 12:30:00`);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/reconciliations/import') && response.request().method() === 'POST'),
      importDialog.getByRole('button', { name: '导入' }).click(),
    ]);
    await expect(page.locator('.el-message').filter({ hasText: '账单已导入' }).last()).toBeVisible({ timeout: 10000 });
    await expect(importDialog).toBeHidden({ timeout: 10000 });

    const emptyKeyword = `NO-RECON-${Date.now()}`;
    await page.getByPlaceholder('批次号 / 通道 / 文件 / 导入人').fill(emptyKeyword);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/reconciliations/page') && response.url().includes(encodeURIComponent(emptyKeyword))),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    await expect(page.locator('.payment-reconciliations__table')).toContainText('未查询到匹配的对账批次');
    await page.locator('.payment-reconciliations__table .el-scrollbar__wrap').first().evaluate((element: Element) => {
      element.scrollLeft = 0;
    });

    await page.screenshot({ path: 'test-results/payment-reconciliations.png', fullPage: true });
    expect(runtimeErrors).toEqual([]);
  });

  test('对账管理可从页面生成芒果支付账单并回显真实明细', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/reconciliations')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const bizOrderNo = `RC-GEN-BIZ-E2E-${suffix}`;
    const businessOrderId = preparePayingBusinessOrder(bizOrderNo);
    setMangoPayScenario('SUCCESS');
    clearActiveMangoPayBillScenarioControls();
    const payResponse = await page.request.post('/api/payment/cashier/pay', {
      headers,
      data: {
        cashierConfigId: '350001',
        businessOrderId: String(businessOrderId),
        methodCode: 'PERSONAL_WECHAT_QR',
      },
    });
    const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
    const payOrderNo = payBody.data?.payOrderNo || '';
    expect(payOrderNo).toBeTruthy();
    finishProcessingPayment(payOrderNo);
    const paymentOrder = await findPaymentOrderByNo(page, headers, payOrderNo);
    expect(paymentOrder).toMatchObject({
      payOrderNo,
      channelCode: 'MANGO_PAY',
      status: 'SUCCESS',
    });
    const channelTradeNo = paymentOrder?.channelTradeNo || '';
    expect(channelTradeNo).toBeTruthy();
    const billDate = new Date().toISOString().slice(0, 10);
    moveHistoricalReconciliationE2eOrdersOutOfBillDate('RC-GEN-BIZ-E2E-', billDate, payOrderNo);
    movePaymentSuccessTime(payOrderNo, billDate, '10:30:00');

    await openPaymentPage(page, '/#/payment/reconciliations', '对账管理');
    const search = page.locator('.payment-reconciliations__toolbar');
    await expect(search.getByPlaceholder('批次号 / 通道 / 文件 / 导入人')).toBeVisible();
    await expect(search.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select')).toBeVisible();
    await expect(page.getByRole('button', { name: '生成芒果支付账单' })).toBeVisible();

    await page.getByRole('button', { name: '生成芒果支付账单' }).click();
    const generateDialog = page.getByRole('dialog').filter({ hasText: '生成芒果支付账单' }).last();
    await expect(generateDialog).toBeVisible();
    await expect(formItem(page, '通道编码').locator('input')).toHaveValue('MANGO_PAY');
    await expect(formItem(page, '账单日期').locator('input')).toHaveValue(billDate);

    const [generateResponse] = await Promise.all([
      page.waitForResponse(response =>
        response.url().includes('/api/payment/reconciliations/mango-pay/virtual/generate')
        && response.request().method() === 'POST'
      ),
      generateDialog.getByRole('button', { name: '生成' }).click(),
    ]);
    const generateBody = await expectBusinessOk<PaymentReconciliation>(generateResponse);
    expect(generateBody.data).toMatchObject({
      channelCode: 'MANGO_PAY',
      billDate,
      matchStatus: 'MATCHED',
      billFileName: `MANGO_PAY-${billDate}-generated.bill`,
    });
    expect(Number(generateBody.data?.totalCount || 0)).toBeGreaterThan(0);
    expect(generateBody.data?.details?.some(detail =>
      detail.channelTradeNo === channelTradeNo
      && detail.tradeType === 'PAYMENT'
      && detail.matchStatus === 'MATCHED'
      && detail.matchedOrderNo === payOrderNo
    )).toBeTruthy();
    await expect(page.locator('.el-message').filter({ hasText: '芒果支付账单已生成' }).last()).toBeVisible({ timeout: 10000 });
    await expect(generateDialog).toBeHidden({ timeout: 10000 });

    const reconciliationNo = generateBody.data?.reconciliationNo || '';
    expect(reconciliationNo).toBeTruthy();
    const apiRow = await findReconciliationByNo(page, headers, reconciliationNo);
    expect(apiRow).toMatchObject({
      reconciliationNo,
      channelCode: 'MANGO_PAY',
      billDate,
      matchStatus: 'MATCHED',
    });

    await page.getByPlaceholder('批次号 / 通道 / 文件 / 导入人').fill(reconciliationNo);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/reconciliations/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const row = page.locator('.payment-reconciliations__table .el-table__body-wrapper tbody tr').filter({ hasText: reconciliationNo }).first();
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row).toContainText('MANGO_PAY');
    await expect(row).toContainText('已平账');
    await expect(row.locator('.el-tag').filter({ hasText: 'MANGO_PAY' })).toHaveCount(0);
    await expect(row.getByRole('button', { name: '删除' })).toHaveCount(0);

    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/reconciliations/detail')),
      row.getByRole('button', { name: '详情' }).click(),
    ]);
    const drawer = page.getByRole('dialog').filter({ hasText: '对账批次详情' }).last();
    await expect(drawer.getByText(reconciliationNo, { exact: true })).toBeVisible();
    await expect(drawer.getByText(channelTradeNo, { exact: true })).toBeVisible();
    await expect(drawer.getByText(payOrderNo, { exact: true })).toBeVisible();
    await drawer.locator('.el-drawer__close-btn').click();

    const audit = await findLatestPaymentAudit(page, headers, {
      action: 'GENERATE_MANGO_PAY_CHANNEL_BILL',
      resourceType: 'PAYMENT_RECONCILIATION',
      resourceId: reconciliationNo,
      operationResult: 'SUCCESS',
    });
    expect(audit).toMatchObject({
      operationAction: 'GENERATE_MANGO_PAY_CHANNEL_BILL',
      resourceType: 'PAYMENT_RECONCILIATION',
      resourceId: reconciliationNo,
      operationResult: 'SUCCESS',
    });

    await page.locator('.payment-reconciliations__table .el-scrollbar__wrap').first().evaluate((element: Element) => {
      element.scrollLeft = 0;
    });
    await page.screenshot({ path: 'test-results/payment-reconciliations-special-bill.png', fullPage: true });
    expect(runtimeErrors).toEqual([]);
  });

  test('对账管理可配置 HTTP 获取源并发起真实账单获取', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/reconciliations')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const bizOrderNo = `RC-HTTP-BIZ-E2E-${suffix}`;
    const businessOrderId = preparePayingBusinessOrder(bizOrderNo);
    setMangoPayScenario('SUCCESS');
    const payResponse = await page.request.post('/api/payment/cashier/pay', {
      headers,
      data: {
        cashierConfigId: '350001',
        businessOrderId: String(businessOrderId),
        methodCode: 'PERSONAL_WECHAT_QR',
      },
    });
    const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
    const payOrderNo = payBody.data?.payOrderNo || '';
    const paymentOrder = await findPaymentOrderByNo(page, headers, payOrderNo);
    const channelTradeNo = paymentOrder?.channelTradeNo || '';
    expect(channelTradeNo).toBeTruthy();
    const billDate = new Date().toISOString().slice(0, 10);
    moveHistoricalReconciliationE2eOrdersOutOfBillDate('RC-HTTP-BIZ-E2E-', billDate, payOrderNo);
    movePaymentSuccessTime(payOrderNo, billDate, '10:30:00');

    const billSource = await startPaymentBillHttpSource(() => {
      const rows = mysqlQueryRows(`
        SELECT po.channel_trade_no, 'PAYMENT', po.amount, 0, DATE_FORMAT(po.pay_time, '%Y-%m-%dT%H:%i:%s')
        FROM payment_order po
        WHERE po.tenant_id = 1
          AND po.channel_code = 'MANGO_PAY'
          AND po.status IN ('PAYING', 'SUCCESS')
          AND po.channel_trade_no IS NOT NULL
          AND po.pay_time >= '${billDate} 00:00:00'
          AND po.pay_time < DATE_ADD('${billDate} 00:00:00', INTERVAL 1 DAY)
        UNION ALL
        SELECT ro.channel_refund_no, 'REFUND', ro.refund_amount, 0, DATE_FORMAT(ro.refund_time, '%Y-%m-%dT%H:%i:%s')
        FROM payment_refund_order ro
        JOIN payment_order po ON po.id = ro.payment_order_id AND po.tenant_id = ro.tenant_id
        WHERE ro.tenant_id = 1
          AND po.channel_code = 'MANGO_PAY'
          AND ro.status = 'SUCCESS'
          AND ro.channel_refund_no IS NOT NULL
          AND ro.refund_time >= '${billDate} 00:00:00'
          AND ro.refund_time < DATE_ADD('${billDate} 00:00:00', INTERVAL 1 DAY)
      `);
      const items = rows.map((line) => {
        const [tradeNo, tradeType, amount, fee, tradeTime] = line.split('\t');
        return {
          channelTradeNo: tradeNo,
          tradeType,
          amount: Number(amount),
          fee: Number(fee),
          tradeTime,
        };
      });
      if (!items.some(item => item.channelTradeNo === channelTradeNo)) {
        items.unshift({
          channelTradeNo,
          tradeType: 'PAYMENT',
          amount: 128800,
          fee: 0,
          tradeTime: `${billDate}T10:30:00`,
        });
      }
      items.push({
        channelTradeNo: `FEE-HTTP-E2E-${suffix}`,
        tradeType: 'FEE',
        amount: 0,
        fee: 0,
        tradeTime: `${billDate}T23:59:00`,
      });
      return items;
    });
    try {
      const existingSourcesResponse = await page.request.get('/api/payment/reconciliations/bill-sources/page', {
        headers,
        params: { page: '1', size: '50', keyword: 'MANGO_PAY' },
      });
      const existingSourcesBody = await expectBusinessOk<PageData>(existingSourcesResponse);
      const existingSource = (existingSourcesBody.data?.list || []).find(item =>
        item.channelCode === 'MANGO_PAY' && item.fetchMode === 'HTTP'
      ) as PaymentChannelBillSource | undefined;
      let sourceId = existingSource?.id;
      if (existingSource?.id) {
        await page.request.post('/api/payment/reconciliations/bill-sources', {
          headers,
          data: {
            id: existingSource.id,
            channelCode: 'MANGO_PAY',
            fetchMode: 'HTTP',
            endpoint: billSource.url,
            pageMode: 'PAGE',
            enabled: 1,
          },
        });
      } else {
        const saveSourceResponse = await page.request.post('/api/payment/reconciliations/bill-sources', {
          headers,
          data: {
            channelCode: 'MANGO_PAY',
            fetchMode: 'HTTP',
            endpoint: billSource.url,
            pageMode: 'PAGE',
            enabled: 1,
          },
        });
        const saveSourceBody = await expectBusinessOk<PaymentChannelBillSource>(saveSourceResponse);
        sourceId = saveSourceBody.data?.id;
      }
      expect(sourceId).toBeTruthy();

      await openPaymentPage(page, '/#/payment/reconciliations', '对账管理');
      const [modeResponse] = await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/reconciliations/bill-fetch-modes')),
        page.getByRole('button', { name: '配置获取方式' }).click(),
      ]);
      const modeBody = await expectBusinessOk<Array<{ fetchMode: string; fetchModeName: string }>>(modeResponse);
      expect(modeBody.data).toEqual(expect.arrayContaining([
        expect.objectContaining({ fetchMode: 'HTTP', fetchModeName: 'HTTP 接口' }),
        expect.objectContaining({ fetchMode: 'FTP', fetchModeName: 'FTP 拉取' }),
        expect.objectContaining({ fetchMode: 'FTPS', fetchModeName: 'FTPS 拉取' }),
        expect.objectContaining({ fetchMode: 'MANUAL', fetchModeName: '手动上传' }),
      ]));
      const sourceDialog = page.getByRole('dialog').filter({ hasText: '通道账单获取方式' }).last();
      await expect(sourceDialog).toBeVisible({ timeout: 10000 });
      await expect(sourceDialog.getByText('MANGO_PAY').first()).toBeVisible();
      await expect(sourceDialog.getByText('HTTP 接口').first()).toBeVisible();
      await sourceDialog.locator('.el-dialog__headerbtn').click({ timeout: 10000 });
      await expect(sourceDialog).toBeHidden({ timeout: 10000 });

      await page.getByRole('button', { name: '发起获取' }).click();
      const fetchDialog = page.getByRole('dialog').filter({ hasText: '发起通道账单获取' }).last();
      await expect(fetchDialog).toBeVisible({ timeout: 10000 });
      await chooseSelect(page, '获取方式', 'MANGO_PAY / HTTP 接口');
      await expect(formItem(page, '账单日期').locator('input')).toHaveValue(billDate);
      const [fetchResponse] = await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/payment/reconciliations/bill-fetch') && response.request().method() === 'POST'),
        fetchDialog.getByRole('button', { name: '发起获取' }).click(),
      ]);
      const fetchBody = await expectBusinessOk<PaymentReconciliation>(fetchResponse);
      expect(fetchBody.data).toMatchObject({
        channelCode: 'MANGO_PAY',
        billDate,
        matchStatus: 'MATCHED',
      });
      expect(fetchBody.data?.details?.some(detail =>
        detail.channelTradeNo === channelTradeNo && detail.matchedOrderNo === payOrderNo
      )).toBeTruthy();
      await expect(page.locator('.el-message').filter({ hasText: '账单获取并对账完成' }).last()).toBeVisible({ timeout: 10000 });
      expect(billSource.requests.some(url => url.includes('billDate=') && url.includes('page=1'))).toBeTruthy();

      await page.getByRole('button', { name: '配置获取方式' }).click();
      const batchDialog = page.getByRole('dialog').filter({ hasText: '通道账单获取方式' }).last();
      await expect(batchDialog.getByText('最近获取批次')).toBeVisible({ timeout: 10000 });
      await expect(batchDialog.getByText(fetchBody.data?.reconciliationNo || '').first()).toBeVisible({ timeout: 10000 });
      await expect(batchDialog.getByText('获取成功').first()).toBeVisible();
      await batchDialog.locator('.el-dialog__headerbtn').click();

      const fetchBatchResponse = await page.request.get('/api/payment/reconciliations/bill-fetch-batches/page', {
        headers,
        params: { page: '1', size: '10', keyword: fetchBody.data?.reconciliationNo || '' },
      });
      const fetchBatchBody = await expectBusinessOk<PageData>(fetchBatchResponse);
      const fetchBatch = (fetchBatchBody.data?.list || []).find(item =>
        item.reconciliationNo === fetchBody.data?.reconciliationNo
      ) as PaymentChannelBillFetchBatch | undefined;
      expect(fetchBatch).toMatchObject({
        channelCode: 'MANGO_PAY',
        fetchMode: 'HTTP',
        fetchStatus: 'SUCCESS',
      });

      await page.screenshot({ path: 'test-results/payment-reconciliations-http-fetch.png', fullPage: true });
      expect(runtimeErrors).toEqual([]);
    } finally {
      await billSource.close();
    }
  });

  test('差异处理列表、详情、受控处理和审计真实可用', async ({ page }) => {
    const runtimeErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        runtimeErrors.push(message.text());
      }
    });
    page.on('pageerror', error => runtimeErrors.push(error.message));
    page.on('requestfailed', request => {
      if (request.url().includes('/api/payment/differences')) {
        runtimeErrors.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || 'failed'}`);
      }
    });
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const billDate = new Date().toISOString().slice(0, 10);
    const channelCode = 'MANGO_PAY';
    const missingTradeNo = `CH-DIFF-E2E-${suffix}`;

    const statusesResponse = await page.request.get('/api/payment/differences/statuses', { headers });
    const statusesBody = await expectBusinessOk<PaymentDifferenceStatus[]>(statusesResponse);
    expect(statusesBody.data?.map(item => item.statusCode)).toEqual(expect.arrayContaining([
      'PENDING',
      'PROCESSING',
      'HANDLED',
      'IGNORED',
      'CLOSED',
    ]));

    const actionsResponse = await page.request.get('/api/payment/differences/actions', { headers });
    const actionsBody = await expectBusinessOk<PaymentDifferenceAction[]>(actionsResponse);
    expect(actionsBody.data?.map(item => item.actionCode)).toEqual(expect.arrayContaining([
      'ACTIVE_QUERY',
      'SUPPLEMENT_ORDER',
      'IGNORE',
      'CLOSE',
    ]));

    const importResponse = await page.request.post('/api/payment/reconciliations/import', {
      headers,
      data: {
        channelCode,
        billDate,
        billFileName: `mango-pay-diff-process-${suffix}.csv`,
        fileDigest: `sha256-special-diff-process-${suffix}`,
        items: [{
          channelTradeNo: missingTradeNo,
          tradeType: 'PAYMENT',
          amount: 7799,
          fee: 7,
          tradeTime: `${billDate} 13:30:00`,
        }],
      },
    });
    const importBody = await expectBusinessOk<PaymentReconciliation>(importResponse);
    expect(importBody.data?.matchStatus).toBe('DIFFERENCE');

    const pageResponse = await page.request.get('/api/payment/differences/page', {
      headers,
      params: { page: '1', size: '10', keyword: missingTradeNo, statusCode: 'PENDING' },
    });
    const pageBody = await expectBusinessOk<PageData>(pageResponse);
    const apiRow = (pageBody.data?.list || []).find(item => item.relatedOrderNo === missingTradeNo) as PaymentDifference | undefined;
    expect(apiRow).toMatchObject({
      relatedOrderNo: missingTradeNo,
      differenceType: 'CHANNEL_SUCCESS_LOCAL_MISSING',
      differenceTypeName: '通道成功我方无单',
      processStatus: 'PENDING',
      processStatusName: '待处理',
      reconciliationNo: importBody.data?.reconciliationNo,
      channelCode,
    });
    expect(apiRow?.id).toBeTruthy();
    expectMoneyCents(apiRow?.differenceAmount, 7799);

    const detailResponse = await page.request.get('/api/payment/differences/detail', {
      headers,
      params: { id: String(apiRow?.id) },
    });
    const detailBody = await expectBusinessOk<PaymentDifference>(detailResponse);
    expect(detailBody.data).toMatchObject({
      id: String(apiRow?.id),
      relatedOrderNo: missingTradeNo,
      processStatus: 'PENDING',
      processStatusName: '待处理',
    });

    await openPaymentPage(page, '/#/payment/differences', '差异处理');
    const toolbar = page.locator('.payment-differences__toolbar');
    await toolbar.getByPlaceholder('差异单号 / 订单号 / 批次号 / 通道').fill(missingTradeNo);
    const statusSelect = toolbar.locator('.el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
    const statusListboxId = await statusSelect.evaluate((element: Element) => {
      const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
      (target as HTMLElement).click();
      return target.getAttribute('aria-controls') || '';
    });
    const statusDropdown = page.locator(`[id="${statusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
    await clickVisibleOption(statusDropdown, '待处理');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/differences/page') && response.url().includes('statusCode=PENDING')),
      toolbar.getByRole('button', { name: '查询' }).click(),
    ]);

    const row = page.locator('.payment-differences__table .el-table__body-wrapper tbody tr').filter({ hasText: missingTradeNo }).first();
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row).toContainText(importBody.data?.reconciliationNo || '');
    await expect(row).toContainText('通道成功我方无单');
    await expect(row).toContainText('￥77.99');
    await expect(row).toContainText('待处理');
    await expect(row.getByRole('button', { name: '删除' })).toHaveCount(0);

    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/differences/detail')),
      row.getByRole('button', { name: '详情' }).click(),
    ]);
    const drawer = page.getByRole('dialog').filter({ hasText: '差异详情' }).last();
    await expect(drawer.getByText(missingTradeNo, { exact: true })).toBeVisible();
    await expect(drawer.getByText(importBody.data?.reconciliationNo || '', { exact: true })).toBeVisible();
    await expect(drawer.getByText('通道成功我方无单')).toBeVisible();
    await drawer.locator('.el-drawer__close-btn').click();

    await row.getByRole('button', { name: '处理' }).click();
    await expect(dialog(page).getByText('处理差异')).toBeVisible({ timeout: 10000 });
    await chooseSelect(page, '处理动作', '关闭差异');
    await fillTextarea(page, '处理原因', 'E2E 复核后确认该通道交易不属于当前租户订单');
    await fillTextarea(page, '处理结果', '关闭差异，不直接修改支付订单或退款订单状态');
    await fillInput(page, '处理凭据', `mango-file:diff-e2e-${suffix}`);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/differences/handle') && response.request().method() === 'POST'),
      dialog(page).getByRole('button', { name: '保存处理' }).click(),
    ]);
    await expect(page.locator('.el-message').filter({ hasText: '差异已处理' }).last()).toBeVisible({ timeout: 10000 });

    const handledResponse = await page.request.get('/api/payment/differences/detail', {
      headers,
      params: { id: String(apiRow?.id) },
    });
    const handledBody = await expectBusinessOk<PaymentDifference>(handledResponse);
    expect(handledBody.data).toMatchObject({
      processStatus: 'CLOSED',
      processStatusName: '已关闭',
      processAction: 'CLOSE',
      processReason: 'E2E 复核后确认该通道交易不属于当前租户订单',
      processResult: '关闭差异，不直接修改支付订单或退款订单状态',
      processEvidence: `mango-file:diff-e2e-${suffix}`,
      processorName: 'admin',
    });
    expect(handledBody.data?.processTime).toBeTruthy();

    const repeatHandleResponse = await page.request.post('/api/payment/differences/handle', {
      headers,
      data: {
        id: String(apiRow?.id),
        processAction: 'ACTIVE_QUERY',
        processReason: '重复处理',
        processResult: '不应允许重复处理',
      },
    });
    const repeatHandleBody = await expectBusinessError(repeatHandleResponse);
    expect(repeatHandleBody.code).toBe(3786);

    const audit = await findLatestPaymentAudit(page, headers, {
      action: 'HANDLE_DIFFERENCE',
      resourceType: 'PAYMENT_DIFFERENCE',
      resourceId: apiRow?.differenceNo || '',
      operationResult: 'SUCCESS',
    });
    expect(audit).toMatchObject({
      operationAction: 'HANDLE_DIFFERENCE',
      resourceType: 'PAYMENT_DIFFERENCE',
      resourceId: apiRow?.differenceNo,
      operationResult: 'SUCCESS',
    });

    await toolbar.getByPlaceholder('差异单号 / 订单号 / 批次号 / 通道').fill(missingTradeNo);
    const closedStatusListboxId = await statusSelect.evaluate((element: Element) => {
      const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
      (target as HTMLElement).click();
      return target.getAttribute('aria-controls') || '';
    });
    const closedStatusDropdown = page.locator(`[id="${closedStatusListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
    await clickVisibleOption(closedStatusDropdown, '已关闭');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/differences/page') && response.url().includes('statusCode=CLOSED')),
      toolbar.getByRole('button', { name: '查询' }).click(),
    ]);
    const closedRow = page.locator('.payment-differences__table .el-table__body-wrapper tbody tr').filter({ hasText: missingTradeNo }).first();
    await expect(closedRow).toBeVisible({ timeout: 10000 });
    await expect(closedRow).toContainText('已关闭');
    await expect(closedRow.getByRole('button', { name: '处理' })).toHaveCount(0);

    const emptyKeyword = `NO-DIFF-${Date.now()}`;
    await toolbar.getByPlaceholder('差异单号 / 订单号 / 批次号 / 通道').fill(emptyKeyword);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/differences/page') && response.url().includes(encodeURIComponent(emptyKeyword))),
      toolbar.getByRole('button', { name: '查询' }).click(),
    ]);
    await expect(page.locator('.payment-differences__table')).toContainText('未查询到匹配的差异单');
    await page.locator('.payment-differences__table .el-scrollbar__wrap').first().evaluate((element: Element) => {
      element.scrollLeft = 0;
    });

    await page.screenshot({ path: 'test-results/payment-differences.png', fullPage: true });
    expect(runtimeErrors).toEqual([]);
  });

  test('收银台支付结果延迟返回时轮询到终态成功', async ({ page }) => {
    await login(page);
    const e2eBizOrderNo = `PAY-DELAY-E2E-${Date.now()}`;
    const businessOrderId = preparePayingBusinessOrder(e2eBizOrderNo);
    setMangoPayScenario('PAYING');
    try {
      const sessionPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/session'));
      await page.goto(`/#/payment/cashier-configs/350001/cashier?businessOrderId=${businessOrderId}`);
      const sessionResponse = await sessionPromise;
      const sessionBody = await expectBusinessOk<CashierSession>(sessionResponse);
      expect(sessionBody.data?.order?.businessOrderId).toBe(String(businessOrderId));
      expect(sessionBody.data?.order?.status).toBe('PAYING');
      const cashierPage = page.locator('.cashier-page');

      await expect(cashierPage.getByText(e2eBizOrderNo).first()).toBeVisible({ timeout: 10000 });
      const payPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/pay'));
      const payResponse = await payPromise;
      const payBody = await expectBusinessOk<CashierPayResult>(payResponse);
      expect(payBody.data?.status).toBe('PAYING');
      expect(payBody.data?.payOrderNo).toBeTruthy();
      expect(payBody.data?.material?.materialType).toBe('QR');
      await expect(cashierPage.getByRole('tab', { name: /微信支付/ })).toHaveAttribute('aria-selected', 'true');
      await expect(cashierPage.getByAltText('支付二维码')).toBeVisible({ timeout: 10000 });
      await expect(cashierPage.getByRole('button', { name: '我已完成支付' })).toBeVisible();

      const firstResultPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/pay-result'));
      const firstResultResponse = await firstResultPromise;
      const firstResultBody = await expectBusinessOk<CashierPayResult>(firstResultResponse);
      expect(firstResultBody.data?.status).toBe('PAYING');

      finishProcessingPayment(payBody.data?.payOrderNo || '');
      const successResultPromise = page.waitForResponse(async (response) => {
        if (!response.url().includes('/api/payment/cashier/pay-result')) {
          return false;
        }
        const body = await response.json() as ApiBody<CashierPayResult>;
        return body.data?.status === 'SUCCESS';
      });
      await successResultPromise;
      await cashierPage.getByRole('button', { name: '我已完成支付' }).click();
      await expect(page.getByRole('dialog').filter({ hasText: '支付结果' }).getByText('支付成功', { exact: true })).toBeVisible({ timeout: 10000 });
      await page.getByRole('dialog').filter({ hasText: '支付结果' }).getByRole('button', { name: '确定' }).click();
      await expect(page.getByRole('dialog').filter({ hasText: '支付结果' })).toBeHidden({ timeout: 10000 });
    } finally {
      setMangoPayScenario('SUCCESS');
    }
  });

  test('Web 收银台网银和线下转账物料真实返回', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);

    const ebankBizOrderNo = `PAY-EBANK-E2E-${Date.now()}`;
    const ebankCashierConfigId = 350901;
    prepareCashierConfig({
      id: ebankCashierConfigId,
      cashierName: 'E2E 网银收银台',
      methodCodes: 'PERSONAL_EBANK_REDIRECT,CORPORATE_EBANK_REDIRECT',
      defaultMethodCode: 'PERSONAL_EBANK_REDIRECT',
    });
    const ebankBusinessOrderId = preparePayingBusinessOrder(ebankBizOrderNo);
    await openCashierPage(page, ebankCashierConfigId, ebankBusinessOrderId);
    const cashierPage = page.locator('.cashier-page');
    await expect(cashierPage.getByText(ebankBizOrderNo).first()).toBeVisible({ timeout: 10000 });
    await expect(cashierPage.locator('.payment-method-tabs')).toHaveCount(0);
    await expect(cashierPage.getByRole('tab', { name: /网银支付/ })).toHaveCount(0);
    await expect(cashierPage.getByRole('button', { name: '个人网银', exact: true })).toHaveClass(/active/);
    await expect(cashierPage.getByRole('button', { name: /中国工商银行/ })).toHaveClass(/active/);
    await expect(cashierPage.getByText('已选择银行')).toBeVisible();
    await expect(cashierPage.getByText('个人网银：以银行页面展示为准')).toBeVisible();
    await expect(cashierPage.getByText('企业网银：以企业网银授权限额为准')).toBeVisible();
    await expect(cashierPage.getByPlaceholder('请输入账号或卡号')).toHaveCount(0);
    await expect(cashierPage.getByPlaceholder('请输入付款户名')).toHaveCount(0);
    await expect(cashierPage.getByText('已完成企业网银授权准备')).toHaveCount(0);
    await cashierPage.getByRole('button', { name: '企业网银' }).click();
    await expect(cashierPage.getByRole('button', { name: '企业网银', exact: true })).toHaveClass(/active/);
    await expect(cashierPage.getByRole('button', { name: /中国工商银行/ })).toHaveClass(/active/);
    const ebankPayPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/pay'));
    await cashierPage.getByRole('button', { name: /进入企业网银支付/ }).click();
    const ebankPayBody = await expectBusinessOk<CashierPayResult>(await ebankPayPromise);
    expect(ebankPayBody.data?.material?.materialType).toBe('HTML_FORM');
    expect(ebankPayBody.data?.payOrderNo).toBeTruthy();
    expect(ebankPayBody.data?.material?.htmlForm).toContain(ebankPayBody.data?.payOrderNo || '');
    await expect(cashierPage.getByText('网银支付请求已生成')).toBeVisible({ timeout: 10000 });

    const transferBizOrderNo = `PAY-TRANSFER-E2E-${Date.now()}`;
    const transferCashierConfigId = 350902;
    prepareCashierConfig({
      id: transferCashierConfigId,
      cashierName: 'E2E 线下转账收银台',
      methodCodes: 'CORPORATE_OFFLINE_ACCOUNT',
      defaultMethodCode: 'CORPORATE_OFFLINE_ACCOUNT',
    });
    const transferBusinessOrderId = preparePayingBusinessOrder(transferBizOrderNo);
    const transferPayPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/pay'));
    await openCashierPage(page, transferCashierConfigId, transferBusinessOrderId);
    await expect(cashierPage.getByText(transferBizOrderNo).first()).toBeVisible({ timeout: 10000 });
    const transferPayBody = await expectBusinessOk<CashierPayResult>(await transferPayPromise);
    await expect(cashierPage.locator('.payment-method-tabs')).toHaveCount(0);
    await expect(cashierPage.getByRole('tab', { name: /线下转账/ })).toHaveCount(0);
    expect(transferPayBody.data?.channelCode).toBe('OFFLINE_COLLECTION');
    expect(transferPayBody.data?.material?.materialType).toBe('TRANSFER_ACCOUNT');
    expect(transferPayBody.data?.material?.accountName).toBeTruthy();
    expect(transferPayBody.data?.material?.accountNo).toBeTruthy();
    expect(transferPayBody.data?.material?.bankName).toBeTruthy();
    expect(transferPayBody.data?.material?.transferRemark).toMatch(/^[0-9A-Za-z]{4,6}$/);
    const transferPayOrderNo = transferPayBody.data?.payOrderNo || '';
    expect(transferPayOrderNo).toMatch(PAY_ORDER_NO_PATTERN);
    const transferCollection = await findOfflineCollectionByPayOrderNo(page, headers, transferPayOrderNo);
    expect(transferCollection).toMatchObject({
      payOrderNo: transferPayOrderNo,
      bizOrderNo: transferBizOrderNo,
      channelCode: 'OFFLINE_COLLECTION',
      collectionStatus: 'WAITING_TRANSFER',
      transferRemark: transferPayBody.data?.material?.transferRemark,
    });
    expect(Number(transferCollection?.amount || 0)).toBe(128800);
    await expect(cashierPage.getByText('目标户主')).toBeVisible();
    await expect(cashierPage.getByText('目标账号')).toBeVisible();
    await expect(cashierPage.getByText('目标开户行')).toBeVisible();
    await expect(cashierPage.getByText('转账备注', { exact: true })).toBeVisible();
    await expect(cashierPage.getByRole('button', { name: '已完成转账，回传转账凭证' })).toBeVisible();
    await cashierPage.getByRole('button', { name: '已完成转账，回传转账凭证' }).click();
    const voucherDialog = page.getByRole('dialog').filter({ hasText: '提交转账凭证' }).last();
    await expect(voucherDialog.getByLabel('实际转账金额')).toBeVisible();
    await expect(voucherDialog.getByRole('button', { name: '上传凭证' }).first()).toBeVisible();

    const transferVoucherFileId = await uploadPaymentEvidenceFile(page, headers, {
      name: `offline-transfer-voucher-${Date.now()}.txt`,
      content: `offline transfer voucher for ${transferPayOrderNo}`,
      purpose: 'payment-offline-transfer-voucher',
      bizType: 'payment-offline-collection',
      bizId: transferPayOrderNo,
    });
    const submitVoucherResponse = await page.request.post('/api/payment/cashier/offline-collections/transfer-voucher', {
      headers,
      data: {
        payOrderNo: transferPayOrderNo,
        transferAmount: 128800,
        voucherFileIds: transferVoucherFileId,
        submitRemark: 'E2E 用户已完成转账并回传凭证',
      },
    });
    const submitVoucherBody = await expectBusinessOk<PaymentOfflineCollection>(submitVoucherResponse);
    expect(submitVoucherBody.data).toMatchObject({
      payOrderNo: transferPayOrderNo,
      collectionStatus: 'PENDING_CONFIRM',
      collectionStatusName: '待确认到账',
      voucherFileIds: transferVoucherFileId,
      voucherCount: 1,
    });
    expect(Number(submitVoucherBody.data?.transferAmount || 0)).toBe(128800);

    const confirmResponse = await page.request.post('/api/payment/offline-collections/confirm', {
      headers,
      data: {
        id: submitVoucherBody.data?.id,
        confirmedAmount: 128800,
        confirmRemark: 'E2E 财务单笔确认到账',
      },
    });
    const confirmBody = await expectBusinessOk<PaymentOfflineCollection>(confirmResponse);
    expect(confirmBody.data).toMatchObject({
      payOrderNo: transferPayOrderNo,
      collectionStatus: 'CONFIRMED',
      collectionStatusName: '已确认到账',
    });
    expect(Number(confirmBody.data?.confirmedAmount || 0)).toBe(128800);

    const confirmedPaymentOrder = await findPaymentOrderByNo(page, headers, transferPayOrderNo);
    expect(confirmedPaymentOrder).toMatchObject({
      payOrderNo: transferPayOrderNo,
      bizOrderNo: transferBizOrderNo,
      channelCode: 'OFFLINE_COLLECTION',
      status: 'SUCCESS',
      statusName: '支付成功',
      successFlag: 1,
    });
    const confirmedBusinessOrder = await findBusinessOrderByNo(page, headers, transferBizOrderNo);
    expect(confirmedBusinessOrder).toMatchObject({
      bizOrderNo: transferBizOrderNo,
      status: 'PAID',
      statusName: '已支付',
    });
    expect(Number(confirmedBusinessOrder?.paidAmount || 0)).toBe(128800);
    const paySuccessFlow = await findTransactionFlowByPayOrderNo(page, headers, transferPayOrderNo);
    expect(paySuccessFlow).toMatchObject({
      payOrderNo: transferPayOrderNo,
      flowType: 'PAY_SUCCESS',
      flowTypeName: '支付成功收入',
    });

    const refundVoucherFileId = await uploadPaymentEvidenceFile(page, headers, {
      name: `offline-refund-voucher-${Date.now()}.txt`,
      content: `offline refund voucher for ${transferPayOrderNo}`,
      purpose: 'payment-offline-refund-voucher',
      bizType: 'payment-offline-refund',
      bizId: transferPayOrderNo,
    });
    const refundResponse = await page.request.post('/api/payment/offline-collections/refund', {
      headers,
      data: {
        offlineCollectionId: confirmBody.data?.id,
        refundAmount: 38800,
        refundAccountName: 'E2E 退款收款人',
        refundAccountNo: '622202060900009999',
        refundBankName: '中国工商银行北京分行',
        refundVoucherFileIds: refundVoucherFileId,
        reason: 'E2E 线下部分退款',
        remark: '财务已完成线下退款并回传凭证',
      },
    });
    const refundBody = await expectBusinessOk<PaymentOfflineRefund>(refundResponse);
    expect(refundBody.data).toMatchObject({
      offlineCollectionNo: confirmBody.data?.offlineCollectionNo,
      payOrderNo: transferPayOrderNo,
      bizOrderNo: transferBizOrderNo,
      channelCode: 'OFFLINE_COLLECTION',
      refundVoucherFileIds: refundVoucherFileId,
      refundVoucherCount: 1,
      refundStatus: 'REFUNDED',
      refundStatusName: '已退款',
    });
    expect(Number(refundBody.data?.refundAmount || 0)).toBe(38800);
    const offlineRefundNo = refundBody.data?.offlineRefundNo || '';
    expect(offlineRefundNo).toMatch(OFFLINE_REFUND_NO_PATTERN);
    const offlineRefundRow = await findOfflineRefundByNo(page, headers, offlineRefundNo);
    expect(offlineRefundRow).toMatchObject({
      offlineRefundNo,
      payOrderNo: transferPayOrderNo,
      refundStatus: 'REFUNDED',
      refundStatusName: '已退款',
    });
    const refundFlow = await findRefundSuccessFlow(page, headers, refundBody.data?.refundOrderNo || '');
    expect(refundFlow).toMatchObject({
      refundOrderNo: refundBody.data?.refundOrderNo,
      flowType: 'REFUND_SUCCESS',
      flowTypeName: '退款成功支出',
    });
    const refundedBusinessOrder = await findBusinessOrderByNo(page, headers, transferBizOrderNo);
    expect(Number(refundedBusinessOrder?.refundedAmount || 0)).toBe(38800);

    const statementBizOrderNo = `PAY-OFFLINE-BANK-${Date.now()}`;
    const statementBusinessOrderId = preparePayingBusinessOrder(statementBizOrderNo);
    const statementPayResponse = await page.request.post('/api/payment/cashier/pay', {
      headers,
      data: {
        cashierConfigId: String(transferCashierConfigId),
        businessOrderId: String(statementBusinessOrderId),
        methodCode: 'CORPORATE_OFFLINE_ACCOUNT',
      },
    });
    const statementPayBody = await expectBusinessOk<CashierPayResult>(statementPayResponse);
    const statementPayOrderNo = statementPayBody.data?.payOrderNo || '';
    const statementRemark = statementPayBody.data?.material?.transferRemark || '';
    expect(statementPayBody.data?.channelCode).toBe('OFFLINE_COLLECTION');
    expect(statementRemark).toMatch(/^[0-9A-Za-z]{4,6}$/);
    const statementCollection = await findOfflineCollectionByPayOrderNo(page, headers, statementPayOrderNo);
    expect(statementCollection?.collectionStatus).toBe('WAITING_TRANSFER');

    const statementNo = `BS${Date.now()}`;
    const bankStatementBuffer = bankStatementWorkbookBuffer({
      bankStatementNo: statementNo,
      tradeTime: '2026-06-10 12:30:00',
      amountYuan: 1288,
      bankAccountNo: '622202060900001234',
      bankName: statementPayBody.data?.material?.bankName || '中国工商银行',
      counterpartyName: 'E2E 付款企业',
      counterpartyAccountNo: '622202060900009876',
      summary: '线下转账到账',
      remark: `付款备注 ${statementRemark}`,
    });
    const bankStatementResponse = await page.request.post('/api/payment/offline-collections/bank-statements/import', {
      headers,
      multipart: {
        file: {
          name: `offline-bank-statement-${Date.now()}.xlsx`,
          mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          buffer: bankStatementBuffer,
        },
      },
    });
    const bankStatementBody = await expectBusinessOk<PaymentOfflineBankStatementBatch>(bankStatementResponse);
    expect(bankStatementBody.data).toMatchObject({
      totalCount: 1,
      matchedCount: 1,
      confirmedCount: 0,
      differenceCount: 0,
      batchStatus: 'MATCHED',
    });
    const matchedItem = bankStatementBody.data?.items?.[0];
    expect(matchedItem).toMatchObject({
      bankStatementNo: statementNo,
      reconciliationCode: statementRemark,
      matchedPayOrderNo: statementPayOrderNo,
      matchStatus: 'MATCHED_PENDING_CONFIRM',
    });
    expect(matchedItem?.id).toBeTruthy();
    const bankConfirmResponse = await page.request.post('/api/payment/offline-collections/bank-statements/confirm', {
      headers,
      data: {
        itemIds: [matchedItem?.id as string],
        confirmRemark: 'E2E 银行流水批量确认到账',
      },
    });
    const bankConfirmBody = await expectBusinessOk<PaymentOfflineBankStatementBatch>(bankConfirmResponse);
    expect(bankConfirmBody.data).toMatchObject({
      confirmedCount: 1,
      differenceCount: 0,
      batchStatus: 'CONFIRMED',
    });
    const reconciledCollection = await findOfflineCollectionByPayOrderNo(page, headers, statementPayOrderNo);
    expect(reconciledCollection).toMatchObject({
      payOrderNo: statementPayOrderNo,
      collectionStatus: 'RECONCILED',
      collectionStatusName: '已对账',
    });
    const reconciledPaymentOrder = await findPaymentOrderByNo(page, headers, statementPayOrderNo);
    expect(reconciledPaymentOrder).toMatchObject({
      payOrderNo: statementPayOrderNo,
      channelCode: 'OFFLINE_COLLECTION',
      status: 'SUCCESS',
      statusName: '支付成功',
    });
    const reconciledBusinessOrder = await findBusinessOrderByNo(page, headers, statementBizOrderNo);
    expect(reconciledBusinessOrder).toMatchObject({
      bizOrderNo: statementBizOrderNo,
      status: 'PAID',
      statusName: '已支付',
    });
  });

  test('应用管理配置接入安全，收银台支付规则', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const enabledAppName = `支付E2E启用应用${suffix}`;
    const disabledAppName = `支付E2E停用应用${suffix}`;

    await createApplicationByUi(page, {
      name: enabledAppName,
      status: '启用',
      demo: true,
      payloadEncrypt: true,
    });
    await createApplicationByUi(page, {
      name: disabledAppName,
      status: '停用',
      demo: false,
      payloadEncrypt: false,
    });

    await openPaymentPage(page, '/#/payment/applications', '应用管理');
    await page.getByPlaceholder('应用名称 / AppId').fill(enabledAppName);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/applications/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const enabledRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: enabledAppName }).first();
    await expect(enabledRow).toBeVisible();
    await expect(enabledRow.getByText(enabledAppName, { exact: true })).toBeVisible();
    await expect(enabledRow.getByText('已配置', { exact: true })).toBeVisible();
    await expect(enabledRow.getByText('开启', { exact: true })).toHaveCount(2);

    const appPageResponse = await page.request.get('/api/payment/applications/page', {
      headers,
      params: { page: '1', size: '10', keyword: enabledAppName },
    });
    const appPageBody = await expectBusinessOk<PageData>(appPageResponse);
    const createdApp = (appPageBody.data?.list || [])[0] as PaymentApplication | undefined;
    expect(createdApp?.id).toBeTruthy();
    expect(createdApp?.appId).toMatch(/^app_/);
    expect(createdApp?.appName).toBe(enabledAppName);
    expect(createdApp?.appSecret).toBeUndefined();
    expect(createdApp?.secretConfigured).toBe(1);
    expect(createdApp?.secretVersion).toBe(1);
    expect(createdApp?.signAlgorithm).toBe('HMAC_SHA256');
    expect(createdApp?.ipWhitelistEnabled).toBe(1);
    expect(createdApp?.ipWhitelist).toContain('10.10.0.0/16');
    expect(createdApp?.payloadEncryptEnabled).toBe(1);
    expect(createdApp).not.toHaveProperty('notifyUrl');
    expect(createdApp).not.toHaveProperty('refundNotifyUrl');
    expect(createdApp).not.toHaveProperty('notifyUrlWhitelist');
    expect(createdApp).not.toHaveProperty('returnDomainWhitelist');
    expect(createdApp?.notifyRetryPolicy).toBe('1m,5m,15m,1h');
    expect(createdApp?.demoApp).toBe(1);
    expect(createdApp?.status).toBe(1);

    const detailResponse = await page.request.get('/api/payment/applications/detail', {
      headers,
      params: { id: String(createdApp?.id) },
    });
    const detailBody = await expectBusinessOk<PaymentApplication>(detailResponse);
    expect(detailBody.data?.appSecret).toBeUndefined();

    await expect(page.getByRole('button', { name: '重置密钥' })).toHaveCount(0);

    const disabledResponse = await page.request.get('/api/payment/applications/page', {
      headers,
      params: { page: '1', size: '10', keyword: disabledAppName },
    });
    const disabledBody = await expectBusinessOk<PageData>(disabledResponse);
    const disabledApp = (disabledBody.data?.list || [])[0] as PaymentApplication | undefined;
    expect(disabledApp?.appId).toMatch(/^app_/);
    expect(disabledApp?.appName).toBe(disabledAppName);
    expect(disabledApp?.status).toBe(0);
    expect(disabledApp?.payloadEncryptEnabled).toBe(0);
    expect(disabledApp?.secretConfigured).toBe(0);
    expect(disabledApp?.secretVersion).toBe(0);
    expect(disabledApp?.secretLastResetTime).toBeFalsy();
    expect(disabledApp?.signAlgorithm).toBeFalsy();
    expect(disabledApp?.demoApp).toBe(0);

    const cashierCreateResult = await createCashierByUi(page, {
      appName: enabledAppName,
      cashierName: `支付E2E收银台${suffix}`,
      subjectNames: ['芒果科技有限公司', '芒果服务有限公司'],
      methodNames: ['微信扫码', '线下转账'],
      defaultMethodName: '微信扫码',
      resultReturnUrl: `https://pay-e2e.example.com/${suffix}/result`,
    });

    const cashierPageResponse = await page.request.get('/api/payment/cashier-configs/page', {
      headers,
      params: { page: '1', size: '10', keyword: `支付E2E收银台${suffix}` },
    });
    const cashierPageBody = await expectBusinessOk<PageData>(cashierPageResponse);
    const cashierConfig = (cashierPageBody.data?.list || [])[0] as PaymentCashierConfig | undefined;
    expect(cashierConfig?.applicationId).toBe(createdApp?.id);
    expect(cashierConfig?.enterpriseSubjectIds?.split(',')).toEqual(expect.arrayContaining(['320001', '320002']));
    expect(cashierConfig?.methodCodes).toBe('PERSONAL_WECHAT_QR,CORPORATE_OFFLINE_ACCOUNT');
    expect(cashierConfig?.defaultMethodCode).toBe('PERSONAL_WECHAT_QR');
    expect(cashierConfig?.displayConfig).toContain(`"logoFileId":"${cashierCreateResult.logoFileId}"`);
    expect(cashierConfig).not.toHaveProperty('refundEnabled');
    expect(cashierConfig).not.toHaveProperty('partialRefundEnabled');

    const cashierConfigId = String(cashierConfig?.id || '');
    expect(cashierConfigId).toBeTruthy();
    const cashierDeleteResponse = await page.request.delete('/api/payment/cashier-configs', {
      headers,
      params: { id: cashierConfigId },
    });
    await expectBusinessOk<boolean>(cashierDeleteResponse);
    const deletedCashierResponse = await page.request.get('/api/payment/cashier-configs/page', {
      headers,
      params: { page: '1', size: '10', keyword: `支付E2E收银台${suffix}` },
    });
    const deletedCashierBody = await expectBusinessOk<PageData>(deletedCashierResponse);
    expect(deletedCashierBody.data?.list || []).toHaveLength(0);
    const deleteAudit = await findLatestPaymentAudit(page, headers, {
      action: 'DELETE_CASHIER_CONFIG',
      resourceType: 'PAYMENT_CASHIER_CONFIG',
      resourceId: cashierConfigId,
      operationResult: 'SUCCESS',
    });
    expect(deleteAudit).toMatchObject({
      operationAction: 'DELETE_CASHIER_CONFIG',
      resourceType: 'PAYMENT_CASHIER_CONFIG',
      resourceId: cashierConfigId,
      operationResult: 'SUCCESS',
    });
  });

  test('签约通道按字段模板和签约能力真实保存、回显和删除审计可用', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const contractCode = `E2E_CONTRACT_${suffix}`;
    const merchantNo = `E2E_MERCHANT_${suffix}`;
    const rawSecret = `secret-${suffix}`;
    const certificateFileId = await uploadContractFile(page, headers);

    const channel = await findChannelByName(page, headers, '芒果支付');
    expect(channel?.id, '应存在芒果支付').toBeTruthy();
    const channelCapability = await findChannelCapability(page, headers, String(channel?.id), 'PERSONAL_WECHAT_QR');
    expect(channelCapability?.id, '芒果支付应存在微信扫码通道能力').toBeTruthy();

    const createResponse = await page.request.post('/api/payment/channel-contracts', {
      headers,
      data: {
        contractCode,
        contractName: `支付E2E签约配置${suffix}`,
        subjectId: '320002',
        channelId: channel?.id,
        merchantNo,
        appId: `e2e-app-${suffix}`,
        configValuesJson: JSON.stringify({
          merchantNo,
          apiSecret: rawSecret,
          certificateFileId,
          mangoPayScenario: 'SUCCESS',
        }),
        capabilities: [{
          channelCapabilityId: channelCapability?.id,
          feeRate: 0.006,
          minAmount: 10,
          maxAmount: 880000,
          priority: 11,
          certificateExpireTime: '2030-01-01 00:00:00',
          status: 1,
        }],
        status: 1,
      },
    });
    const createBody = await expectBusinessOk<string | number>(createResponse);
    const contractId = String(createBody.data || '');
    expect(contractId).toBeTruthy();

    const createdDetailResponse = await page.request.get('/api/payment/channel-contracts/detail', {
      headers,
      params: { id: contractId },
    });
    const createdDetailBody = await expectBusinessOk<PaymentChannelContract>(createdDetailResponse);
    expect(createdDetailBody.data?.contractCode).toMatch(/^MANGO_PAY_320002_/);
    expect(createdDetailBody.data?.merchantNo).toBe(merchantNo);
    expect(createdDetailBody.data?.capabilities?.[0]).toMatchObject({
      channelCapabilityId: String(channelCapability?.id),
      methodCode: 'PERSONAL_WECHAT_QR',
      terminalType: 'WEB',
      minAmount: '10',
      maxAmount: '880000',
      priority: 11,
      status: 1,
    });
    expect(createdDetailBody.data?.configValuesJson).toContain('"apiSecret":"******"');
    expect(createdDetailBody.data?.configValuesJson).toContain(`"certificateFileId":"${certificateFileId}"`);
    expect(createdDetailBody.data?.configValuesJson).not.toContain(rawSecret);

    await openPaymentPage(page, '/#/payment/channel-contracts', '签约通道');
    await page.getByPlaceholder('名称 / 编码').fill(merchantNo);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channel-contracts/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const createdRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: merchantNo }).first();
    await expect(createdRow).toBeVisible();
    await expect(createdRow.getByText(merchantNo, { exact: true })).toBeVisible();
    await expect(createdRow.locator('.el-tag').filter({ hasText: '微信扫码/电脑网页' })).toBeVisible();
    await expect(createdRow.locator('.el-tag').filter({ hasText: merchantNo })).toHaveCount(0);
    await expect(createdRow.locator('.el-tag').filter({ hasText: merchantNo })).toHaveCount(0);

    await createdRow.getByRole('button', { name: '编辑' }).click();
    await expect(dialog(page).getByText('编辑签约通道')).toBeVisible({ timeout: 10000 });
    await expect(dialog(page).getByText('接入场景')).toHaveCount(0);
    await expect.poll(async () => formItem(page, '配置值').locator('input').evaluateAll((inputs) =>
      inputs.map(input => (input as HTMLInputElement).value),
    )).toContain('******');
    await expect(formItem(page, '开通能力').getByText('微信扫码')).toBeVisible({ timeout: 10000 });
    await expect.poll(async () => formItem(page, '开通能力').locator('input').evaluateAll((inputs) =>
      inputs.map(input => (input as HTMLInputElement).value),
    )).toEqual(expect.arrayContaining([expect.stringMatching(/^0\.006(?:000)?$/)]));
    const appIdInput = formItem(page, 'AppId').locator('input').first();
    await appIdInput.fill(`e2e-app-edited-${suffix}`);
    const [updateResponse] = await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channel-contracts') && response.request().method() === 'PUT'),
      dialog(page).getByRole('button', { name: '保存' }).click(),
    ]);
    await expectBusinessOk<boolean>(updateResponse);
    await expect(page.locator('.el-message').filter({ hasText: '已保存' }).last()).toBeVisible({ timeout: 10000 });
    await expect(dialog(page)).toBeHidden({ timeout: 10000 });

    const updatedDetailResponse = await page.request.get('/api/payment/channel-contracts/detail', {
      headers,
      params: { id: contractId },
    });
    const updatedDetailBody = await expectBusinessOk<PaymentChannelContract>(updatedDetailResponse);
    expect(updatedDetailBody.data?.appId).toBe(`e2e-app-edited-${suffix}`);
    expect(updatedDetailBody.data?.configValuesJson).toContain('"apiSecret":"******"');

    const [deleteResponse] = await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channel-contracts') && response.request().method() === 'DELETE'),
      (async () => {
        await createdRow.getByRole('button', { name: '删除' }).click();
        await expect(page.getByText(/确认删除/)).toBeVisible({ timeout: 10000 });
        await page.getByRole('button', { name: '删除' }).last().click();
      })(),
    ]);
    await expectBusinessOk<boolean>(deleteResponse);
    await expect(page.locator('.el-message').filter({ hasText: '已删除' }).last()).toBeVisible({ timeout: 10000 });

    const removed = await findChannelContractByCode(page, headers, contractCode);
    expect(removed).toBeUndefined();
    const deleteAudit = await findLatestPaymentAudit(page, headers, {
      action: 'DELETE_CHANNEL_CONTRACT',
      resourceType: 'PAYMENT_CHANNEL_CONTRACT',
      resourceId: contractId,
      operationResult: 'SUCCESS',
    });
    expect(deleteAudit).toMatchObject({
      operationAction: 'DELETE_CHANNEL_CONTRACT',
      resourceType: 'PAYMENT_CHANNEL_CONTRACT',
      resourceId: contractId,
      operationResult: 'SUCCESS',
    });
  });

  test('支付通道真实维护字段模板、通道能力和删除审计', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const channelCode = await unusedChannelCode(page, headers);
    const channelName = `支付E2E通道${suffix}`;
    const editedChannelName = `支付E2E通道编辑${suffix}`;
    const adapterType = `E2E_ADAPTER_${suffix.slice(-6)}`;

    await openPaymentPage(page, '/#/payment/channels', '支付通道');
    await page.getByRole('button', { name: '新增' }).click();
    await expect(dialog(page).getByText('新增支付通道')).toBeVisible({ timeout: 10000 });
    await chooseSelect(page, '通道编码', channelCodeOptionText(channelCode));
    await fillInput(page, '通道名称', channelName);
    await chooseSelect(page, '通道类型', '聚合支付机构');
    await fillInput(page, '适配器类型', adapterType);
    await fillInput(page, '基础网关', `https://pay-e2e.example.com/${suffix}`);
    await fillTextarea(page, '能力摘要', '微信扫码、退款、查单、账单、对账');

    await dialog(page).getByRole('button', { name: '新增字段' }).click();
    const template = formItem(page, '字段模板');
    await template.getByPlaceholder('字段名，如 merchantNo').fill(`e2eMerchantNo${suffix.slice(-6)}`);
    await template.getByPlaceholder('显示名，如 商户号').fill('E2E商户号');
    await expect(template.getByPlaceholder('枚举选项，每行 label=value；非枚举字段可留空')).toBeVisible();

    const capabilities = formItem(page, '通道能力');
    await capabilities.getByRole('button', { name: '新增能力' }).click();
    const capabilityRow = capabilities.locator('.el-table__body-wrapper tbody tr').first();
    await expect(capabilityRow).toBeVisible();
    const methodSelect = capabilityRow.locator('.el-select').first();
    const methodListboxId = await methodSelect.evaluate((element: Element) => {
      const target = element.querySelector('[role="combobox"]') || element.querySelector('input') || element;
      (target as HTMLElement).click();
      return target.getAttribute('aria-controls') || '';
    });
    const methodDropdown = page.locator(`[id="${methodListboxId}"]`).locator('xpath=ancestor::*[contains(@class, "el-select-dropdown")]').first();
    await clickVisibleOption(methodDropdown, '微信扫码');
    await closeSelectDropdown(page);
    await capabilityRow.locator('.el-select').nth(1).click();
    await page.locator('.el-select-dropdown:visible').last().locator('.el-select-dropdown__item').filter({ hasText: 'Web/PC' }).first().click();
    await closeSelectDropdown(page);
    await expect(capabilityRow.getByText('接入场景')).toHaveCount(0);
    await capabilityRow.locator('input[type="number"]').nth(0).fill('10');
    await capabilityRow.locator('input[type="number"]').nth(1).fill('990000');
    await capabilityRow.locator('input[type="number"]').nth(1).press('Tab');

    const [createResponse] = await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channels') && response.request().method() === 'POST'),
      dialog(page).getByRole('button', { name: '保存' }).click(),
    ]);
    const createBody = await expectBusinessOk<string | number>(createResponse);
    const createdChannelId = String(createBody.data || '');
    expect(createdChannelId).toBeTruthy();
    await expect(page.locator('.el-message').filter({ hasText: '已新增' }).last()).toBeVisible({ timeout: 10000 });
    await expect(dialog(page)).toBeHidden({ timeout: 10000 });

    const detailResponse = await page.request.get('/api/payment/channels/detail', {
      headers,
      params: { id: createdChannelId },
    });
    const detailBody = await expectBusinessOk<PaymentChannel>(detailResponse);
    expect(detailBody.data).toMatchObject({
      id: createdChannelId,
      channelCode,
      channelName,
      channelType: 'AGGREGATOR',
      adapterType,
      status: 1,
    });
    expect(detailBody.data?.fieldTemplateJson).toContain(`e2eMerchantNo${suffix.slice(-6)}`);
    expect(detailBody.data?.fieldTemplateJson).toContain('"dataType":"string"');
    expect(detailBody.data).not.toHaveProperty('merchantNo');
    expect(detailBody.data).not.toHaveProperty('appId');
    const storedCapability = detailBody.data?.capabilities?.[0];
    expect(storedCapability).toMatchObject({
      channelId: createdChannelId,
      methodCode: 'PERSONAL_WECHAT_QR',
      terminalType: 'WEB',
      environment: 'PROD',
      supportsRefund: 1,
      supportsQuery: 1,
      supportsClose: 1,
      supportsBill: 1,
      supportsReconcile: 1,
      status: 1,
    });
    expect(String(storedCapability?.minAmount)).toBe('1000');
    expect(String(storedCapability?.maxAmount)).toBe('99000000');

    const readonlyCapability = await findChannelCapability(page, headers, createdChannelId, 'PERSONAL_WECHAT_QR');
    expect(readonlyCapability).toMatchObject({
      channelId: createdChannelId,
      methodCode: 'PERSONAL_WECHAT_QR',
      terminalType: 'WEB',
      environment: 'PROD',
      methodName: '微信扫码',
    });

    await page.getByPlaceholder('名称 / 编码').fill(channelName);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channels/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const createdRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: channelName }).first();
    await expect(createdRow).toBeVisible();
    await expect(createdRow.getByText(channelCode, { exact: true })).toBeVisible();
    await expect(createdRow.getByText(adapterType, { exact: true })).toBeVisible();
    await expect(createdRow.locator('.el-tag').filter({ hasText: channelName })).toHaveCount(0);
    await expect(createdRow.locator('.el-tag').filter({ hasText: adapterType })).toHaveCount(0);

    await createdRow.getByRole('button', { name: '编辑' }).click();
    await expect(dialog(page).getByText('编辑支付通道')).toBeVisible({ timeout: 10000 });
    await expect(formItem(page, '通道能力').getByText('微信扫码')).toBeVisible({ timeout: 10000 });
    await fillInput(page, '通道名称', editedChannelName);
    const [updateResponse] = await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channels') && response.request().method() === 'PUT'),
      dialog(page).getByRole('button', { name: '保存' }).click(),
    ]);
    await expectBusinessOk<boolean>(updateResponse);
    await expect(page.locator('.el-message').filter({ hasText: '已保存' }).last()).toBeVisible({ timeout: 10000 });
    await expect(dialog(page)).toBeHidden({ timeout: 10000 });

    const updateAudit = await findLatestPaymentAudit(page, headers, {
      action: 'UPDATE_CHANNEL',
      resourceType: 'PAYMENT_CHANNEL',
      resourceId: channelCode,
      operationResult: 'SUCCESS',
    });
    expect(updateAudit).toMatchObject({
      operationAction: 'UPDATE_CHANNEL',
      resourceType: 'PAYMENT_CHANNEL',
      resourceId: channelCode,
      operationResult: 'SUCCESS',
    });

    await page.getByPlaceholder('名称 / 编码').fill(editedChannelName);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channels/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const editedRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: editedChannelName }).first();
    await expect(editedRow).toBeVisible();
    const [deleteResponse] = await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/channels') && response.request().method() === 'DELETE'),
      (async () => {
        await editedRow.getByRole('button', { name: '删除' }).click();
        await expect(page.getByText(/确认删除/)).toBeVisible({ timeout: 10000 });
        await page.getByRole('button', { name: '删除' }).last().click();
      })(),
    ]);
    await expectBusinessOk<boolean>(deleteResponse);
    await expect(page.locator('.el-message').filter({ hasText: '已删除' }).last()).toBeVisible({ timeout: 10000 });
    const deletedChannel = await findChannelByCode(page, headers, channelCode);
    expect(deletedChannel).toBeUndefined();

    const deletedCapabilityResponse = await page.request.get('/api/payment/channel-capabilities/page', {
      headers,
      params: { page: '1', size: '50', channelId: createdChannelId, keyword: 'PERSONAL_WECHAT_QR' },
    });
    const deletedCapabilityBody = await expectBusinessOk<PageData>(deletedCapabilityResponse);
    expect(deletedCapabilityBody.data?.list || []).toHaveLength(0);

    const deleteAudit = await findLatestPaymentAudit(page, headers, {
      action: 'DELETE_CHANNEL',
      resourceType: 'PAYMENT_CHANNEL',
      resourceId: channelCode,
      operationResult: 'SUCCESS',
    });
    expect(deleteAudit).toMatchObject({
      operationAction: 'DELETE_CHANNEL',
      resourceType: 'PAYMENT_CHANNEL',
      resourceId: channelCode,
      operationResult: 'SUCCESS',
    });
  });

  test('应用管理删除受控校验和审计记录可用', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const blockedAppName = `支付E2E受控删除应用${suffix}`;
    const removableAppName = `支付E2E可删除应用${suffix}`;

    const blockedCreateResult = await createApplicationByUi(page, {
      name: blockedAppName,
      status: '启用',
      demo: false,
      payloadEncrypt: false,
    });
    const removableCreateResult = await createApplicationByUi(page, {
      name: removableAppName,
      status: '启用',
      demo: false,
      payloadEncrypt: false,
    });
    expect(blockedCreateResult.appId).toBeTruthy();
    expect(removableCreateResult.appId).toBeTruthy();

    await createCashierByUi(page, {
      appName: blockedAppName,
      cashierName: `支付E2E删除保护收银台${suffix}`,
      subjectNames: ['芒果科技有限公司'],
      methodNames: ['微信扫码'],
      defaultMethodName: '微信扫码',
      resultReturnUrl: `https://pay-e2e.example.com/${suffix}/blocked-result`,
    });
    const blockedCashierName = `支付E2E删除保护收银台${suffix}`;
    await openPaymentPage(page, '/#/payment/cashier-configs', '收银台');
    await page.getByPlaceholder('名称 / 编码').fill(blockedCashierName);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/cashier-configs/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const blockedCashierSessionPromise = page.waitForResponse(response => response.url().includes('/api/payment/cashier/session'));
    await clickPaymentTableRowButton(page, blockedCashierName, '预览');
    const blockedCashierSessionBody = await expectBusinessOk<CashierSession>(await blockedCashierSessionPromise);
    expect(blockedCashierSessionBody.data?.order?.businessOrderId).toBeFalsy();
    const blockedCashierDialog = dialog(page);
    await expect(blockedCashierDialog.getByRole('heading', { name: '收银台预览' })).toBeVisible({ timeout: 10000 });
    await expect(blockedCashierDialog.getByText(blockedCashierName)).toBeVisible();
    await expect(blockedCashierDialog.getByText('PREVIEW-ORDER')).toBeVisible();
    await expect(blockedCashierDialog.getByText('微信支付')).toBeVisible();
    await expect(blockedCashierDialog.getByAltText('支付二维码')).toBeVisible({ timeout: 10000 });
    await expect(blockedCashierDialog.getByText('预览模式不生成支付订单')).toBeVisible();
    await blockedCashierDialog.locator('.el-dialog__headerbtn').first().click();

    await openPaymentPage(page, '/#/payment/applications', '应用管理');
    await page.getByPlaceholder('应用名称 / AppId').fill(blockedAppName);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/applications/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const blockedRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: blockedAppName }).first();
    await expect(blockedRow).toBeVisible();
    await expect(blockedRow.getByRole('button', { name: '编辑' })).toBeVisible();
    await expect(blockedRow.getByRole('button', { name: '删除' })).toBeVisible();

    const [blockedDeleteResponse] = await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/applications') && response.request().method() === 'DELETE'),
      (async () => {
        await blockedRow.getByRole('button', { name: '删除' }).click();
        await expect(page.getByText(/存在收银台配置、业务订单、支付订单、退款、流水、通知、异常、对账差异等关联数据/)).toBeVisible({ timeout: 10000 });
        await page.getByRole('button', { name: '删除' }).last().click();
      })(),
    ]);
    const blockedDeleteBody = await expectBusinessError<boolean>(blockedDeleteResponse);
    expect(blockedDeleteBody.code).toBe(3709);
    expect(blockedDeleteBody.msg).toContain('支付应用存在关联数据');
    await expect(page.locator('.el-message').filter({ hasText: /支付应用存在关联数据/ }).last()).toBeVisible({ timeout: 10000 });

    const blockedAppAfterDelete = await findApplicationByName(page, headers, blockedAppName);
    expect(blockedAppAfterDelete?.appName).toBe(blockedAppName);
    const rejectedAudit = await findLatestAudit(page, headers, String(blockedCreateResult.appId), 'REJECTED');
    expect(rejectedAudit).toMatchObject({
      operationAction: 'DELETE_APPLICATION',
      resourceType: 'PAYMENT_APPLICATION',
      resourceId: blockedCreateResult.appId,
      operationResult: 'REJECTED',
    });

    await openPaymentPage(page, '/#/payment/applications', '应用管理');
    await page.getByPlaceholder('应用名称 / AppId').fill(removableAppName);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/applications/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const removableRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: removableAppName }).first();
    await expect(removableRow).toBeVisible();
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/applications') && response.request().method() === 'DELETE'),
      (async () => {
        await removableRow.getByRole('button', { name: '删除' }).click();
        await expect(page.getByText(/存在收银台配置、业务订单、支付订单、退款、流水、通知、异常、对账差异等关联数据/)).toBeVisible({ timeout: 10000 });
        await page.getByRole('button', { name: '删除' }).last().click();
      })(),
    ]);
    await expect(page.locator('.el-message').filter({ hasText: '已删除' }).last()).toBeVisible({ timeout: 10000 });
    await expect(removableRow).toHaveCount(0);

    const removedApp = await findApplicationByName(page, headers, removableAppName);
    expect(removedApp).toBeUndefined();
    const successAudit = await findLatestAudit(page, headers, String(removableCreateResult.appId), 'SUCCESS');
    expect(successAudit).toMatchObject({
      operationAction: 'DELETE_APPLICATION',
      resourceType: 'PAYMENT_APPLICATION',
      resourceId: removableCreateResult.appId,
      operationResult: 'SUCCESS',
    });

    await openPaymentPage(page, '/#/payment/operation-audits', '操作审计');
    await page.getByPlaceholder('操作人 / 资源 / 动作').fill('DELETE_APPLICATION');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/operation-audits/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const rejectedAuditRow = page.locator('.payment-operation-audits__table .el-table__body-wrapper tbody tr')
      .filter({ hasText: String(blockedCreateResult.appId) })
      .first();
    await expect(rejectedAuditRow).toBeVisible();
    await expect(rejectedAuditRow.getByText('DELETE_APPLICATION')).toBeVisible();
    await expect(rejectedAuditRow.getByText('已拒绝')).toBeVisible();
  });

  test('操作审计列表页按真实接口查询并符合列表布局规范', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const appName = `支付E2E操作审计应用${suffix}`;
    const createResult = await createApplicationByUi(page, {
      name: appName,
      status: '启用',
      demo: false,
      payloadEncrypt: false,
    });
    expect(createResult.appId).toBeTruthy();

    const audit = await findLatestPaymentAudit(page, headers, {
      action: 'CREATE_APPLICATION',
      resourceType: 'PAYMENT_APPLICATION',
      resourceId: createResult.appId,
      operationResult: 'SUCCESS',
    });
    expect(audit).toMatchObject({
      operationAction: 'CREATE_APPLICATION',
      resourceType: 'PAYMENT_APPLICATION',
      resourceId: createResult.appId,
      operationResult: 'SUCCESS',
    });
    expect(audit?.id).toBeTruthy();
    expect(audit?.operatorName).toBeTruthy();
    expect(audit?.operationTime).toBeTruthy();

    const filteredResponse = await page.request.get('/api/payment/operation-audits/page', {
      headers,
      params: { page: '1', size: '20', keyword: 'CREATE_APPLICATION', statusCode: 'SUCCESS' },
    });
    const filteredBody = await expectBusinessOk<PageData>(filteredResponse);
    expect((filteredBody.data?.list || []).every(item => item.operationResult === 'SUCCESS')).toBe(true);
    expect((filteredBody.data?.list || []).some(item =>
      item.operationAction === 'CREATE_APPLICATION'
      && item.resourceType === 'PAYMENT_APPLICATION'
      && item.resourceId === createResult.appId
    )).toBe(true);

    await openPaymentPage(page, '/#/payment/operation-audits', '操作审计');
    await page.getByPlaceholder('操作人 / 资源 / 动作').fill('CREATE_APPLICATION');
    await page.locator('.payment-operation-audits__toolbar .el-select').click();
    await page.getByRole('option', { name: '成功' }).click();
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/operation-audits/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);

    const auditRow = page.locator('.payment-operation-audits__table .el-table__body-wrapper tbody tr')
      .filter({ hasText: createResult.appId })
      .first();
    await expect(auditRow).toBeVisible({ timeout: 10000 });
    await expect(auditRow.getByText('CREATE_APPLICATION')).toBeVisible();
    await expect(auditRow.getByText('PAYMENT_APPLICATION')).toBeVisible();
    await expect(auditRow.getByText(createResult.appId, { exact: true })).toBeVisible();
    await expect(auditRow.locator('.el-tag').filter({ hasText: createResult.appId })).toHaveCount(0);
    await expect(auditRow.locator('.el-tag').filter({ hasText: '成功' })).toHaveCount(1);
    await expect(page.locator('.payment-operation-audits__pagination')).toBeVisible();
    await page.screenshot({ path: 'test-results/payment-operation-audits.png', fullPage: true });
  });

  test('企业主体新增、编辑、删除受控校验和审计记录可用', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const subjectName = `支付E2E企业主体${suffix}`;
    const editedSubjectName = `支付E2E企业主体编辑${suffix}`;
    const creditCode = `91310000E2E${suffix.slice(-8)}X`;
    const bankAccountNo = `622200${suffix.slice(-10)}8800`;
    const editedBankName = `招商银行上海E2E支行${suffix.slice(-4)}`;

    await openPaymentPage(page, '/#/payment/enterprise-subjects', '企业主体');
    await page.getByRole('button', { name: '新增' }).click();
    await expect(dialog(page).getByText('新增企业主体')).toBeVisible();
    await fillInput(page, '主体名称', subjectName);
    await fillInput(page, '统一社会信用代码', creditCode);
    await fillInput(page, '银行账户', bankAccountNo);
    await fillInput(page, '开户行', `招商银行上海支付测试支行${suffix.slice(-4)}`);
    await chooseRadio(page, '状态', '启用');

    const [createResponse] = await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/enterprise-subjects') && response.request().method() === 'POST'),
      dialog(page).getByRole('button', { name: '保存' }).click(),
    ]);
    const createBody = await expectBusinessOk<string | number>(createResponse);
    const createdSubjectId = String(createBody.data || '');
    expect(createdSubjectId).toBeTruthy();
    await expect(page.locator('.el-message').filter({ hasText: '已新增' }).last()).toBeVisible({ timeout: 10000 });
    await expect(dialog(page)).toBeHidden({ timeout: 10000 });

    await page.getByPlaceholder('名称 / 编码').fill(subjectName);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/enterprise-subjects/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const createdRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: subjectName }).first();
    await expect(createdRow).toBeVisible();
    await expect(createdRow.getByText(subjectName, { exact: true })).toBeVisible();
    const creditCodeMask = `${creditCode.slice(0, 4)}****${creditCode.slice(-4)}`;
    await expect(createdRow.getByText(creditCodeMask, { exact: true })).toBeVisible();
    await expect(createdRow.locator('.el-tag').filter({ hasText: subjectName })).toHaveCount(0);
    await expect(createdRow.locator('.el-tag').filter({ hasText: creditCodeMask })).toHaveCount(0);
    await expect(createdRow.locator('.el-tag').filter({ hasText: /^启用$/ })).toHaveCount(1);

    const createdSubject = await findEnterpriseSubjectByName(page, headers, subjectName);
    expect(createdSubject).toMatchObject({
      id: createdSubjectId,
      subjectName,
      creditCodeMask,
      status: 1,
    });
    expect(createdSubject?.creditCode).toBeUndefined();
    expect(createdSubject?.bankAccountNo).toBeUndefined();
    expect(createdSubject?.bankAccountNoMask).toBe(`${bankAccountNo.slice(0, 4)}****${bankAccountNo.slice(-4)}`);
    const createAudit = await findLatestPaymentAudit(page, headers, {
      action: 'CREATE_ENTERPRISE_SUBJECT',
      resourceType: 'PAYMENT_ENTERPRISE_SUBJECT',
      resourceId: createdSubjectId,
      operationResult: 'SUCCESS',
    });
    expect(createAudit).toMatchObject({
      operationAction: 'CREATE_ENTERPRISE_SUBJECT',
      resourceType: 'PAYMENT_ENTERPRISE_SUBJECT',
      resourceId: createdSubjectId,
      operationResult: 'SUCCESS',
    });

    await createdRow.getByRole('button', { name: '编辑' }).click();
    await expect(dialog(page).getByText('编辑企业主体')).toBeVisible();
    await fillInput(page, '主体名称', editedSubjectName);
    await fillInput(page, '统一社会信用代码', creditCode);
    await fillInput(page, '银行账户', bankAccountNo);
    await fillInput(page, '开户行', editedBankName);
    const [updateResponse] = await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/enterprise-subjects') && response.request().method() === 'PUT'),
      dialog(page).getByRole('button', { name: '保存' }).click(),
    ]);
    await expectBusinessOk<boolean>(updateResponse);
    await expect(page.locator('.el-message').filter({ hasText: '已保存' }).last()).toBeVisible({ timeout: 10000 });
    await expect(dialog(page)).toBeHidden({ timeout: 10000 });

    await page.getByPlaceholder('名称 / 编码').fill(editedSubjectName);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/enterprise-subjects/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const editedRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: editedSubjectName }).first();
    await expect(editedRow).toBeVisible();
    await expect(editedRow.getByText(editedBankName, { exact: true })).toBeVisible();
    const editedSubject = await findEnterpriseSubjectByName(page, headers, editedSubjectName);
    expect(editedSubject).toMatchObject({
      id: createdSubjectId,
      subjectName: editedSubjectName,
      bankName: editedBankName,
      status: 1,
    });
    const updateAudit = await findLatestPaymentAudit(page, headers, {
      action: 'UPDATE_ENTERPRISE_SUBJECT',
      resourceType: 'PAYMENT_ENTERPRISE_SUBJECT',
      resourceId: createdSubjectId,
      operationResult: 'SUCCESS',
    });
    expect(updateAudit).toMatchObject({
      operationAction: 'UPDATE_ENTERPRISE_SUBJECT',
      resourceType: 'PAYMENT_ENTERPRISE_SUBJECT',
      resourceId: createdSubjectId,
      operationResult: 'SUCCESS',
    });

    await page.getByPlaceholder('名称 / 编码').fill('芒果科技有限公司');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/enterprise-subjects/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const blockedRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: '芒果科技有限公司' }).first();
    await expect(blockedRow).toBeVisible();
    const [blockedDeleteResponse] = await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/enterprise-subjects') && response.request().method() === 'DELETE'),
      (async () => {
        await blockedRow.getByRole('button', { name: '删除' }).click();
        await expect(page.getByText(/确认删除/)).toBeVisible({ timeout: 10000 });
        await page.getByRole('button', { name: '删除' }).last().click();
      })(),
    ]);
    const blockedDeleteBody = await expectBusinessError<boolean>(blockedDeleteResponse);
    expect(blockedDeleteBody.code).toBe(3729);
    expect(blockedDeleteBody.msg).toContain('企业主体存在关联数据');
    await expect(page.locator('.el-message').filter({ hasText: /企业主体存在关联数据/ }).last()).toBeVisible({ timeout: 10000 });
    const blockedAudit = await findLatestPaymentAudit(page, headers, {
      action: 'DELETE_ENTERPRISE_SUBJECT',
      resourceType: 'PAYMENT_ENTERPRISE_SUBJECT',
      resourceId: '320001',
      operationResult: 'REJECTED',
    });
    expect(blockedAudit).toMatchObject({
      operationAction: 'DELETE_ENTERPRISE_SUBJECT',
      resourceType: 'PAYMENT_ENTERPRISE_SUBJECT',
      resourceId: '320001',
      operationResult: 'REJECTED',
    });

    await page.getByPlaceholder('名称 / 编码').fill(editedSubjectName);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/enterprise-subjects/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    const removableRow = page.locator('.payment-table .el-table__body-wrapper tbody tr').filter({ hasText: editedSubjectName }).first();
    await expect(removableRow).toBeVisible();
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/enterprise-subjects') && response.request().method() === 'DELETE'),
      (async () => {
        await removableRow.getByRole('button', { name: '删除' }).click();
        await expect(page.getByText(/确认删除/)).toBeVisible({ timeout: 10000 });
        await page.getByRole('button', { name: '删除' }).last().click();
      })(),
    ]);
    await expect(page.locator('.el-message').filter({ hasText: '已删除' }).last()).toBeVisible({ timeout: 10000 });
    await expect(removableRow).toHaveCount(0);
    const removedSubject = await findEnterpriseSubjectByName(page, headers, editedSubjectName);
    expect(removedSubject).toBeUndefined();
    const deleteAudit = await findLatestPaymentAudit(page, headers, {
      action: 'DELETE_ENTERPRISE_SUBJECT',
      resourceType: 'PAYMENT_ENTERPRISE_SUBJECT',
      resourceId: createdSubjectId,
      operationResult: 'SUCCESS',
    });
    expect(deleteAudit).toMatchObject({
      operationAction: 'DELETE_ENTERPRISE_SUBJECT',
      resourceType: 'PAYMENT_ENTERPRISE_SUBJECT',
      resourceId: createdSubjectId,
      operationResult: 'SUCCESS',
    });
  });

  test('结算汇总生成、差异阻断确认、作废和重新生成真实可用', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const suffix = `${Date.now()}`;
    const billDate = `2026-12-${String(10 + Number(suffix.slice(-1))).padStart(2, '0')}`;
    const scenario = prepareSettlementScenario(suffix, billDate);
    moveHistoricalSettlementE2eRecordsOutOfBillDate(billDate, scenario.payOrderNo, scenario.reconciliationNo);

    const generateResponse = await page.request.post('/api/payment/settlement-summaries/generate', {
      headers,
      data: {
        settlementDate: billDate,
        appCode: 'ORDER_CENTER',
        enterpriseSubjectId: '320001',
        channelCode: 'MANGO_PAY',
      },
    });
    const generateBody = await expectBusinessOk<PaymentSettlementSummary>(generateResponse);
    expect(generateBody.data?.id).toBeTruthy();
    expect(generateBody.data).toMatchObject({
      settlementDate: billDate,
      appCode: 'ORDER_CENTER',
      channelCode: 'MANGO_PAY',
      status: 'GENERATED',
      unresolvedDifferenceCount: 1,
    });
    expectMoneyCents(generateBody.data?.tradeAmount, 128800);
    expectMoneyCents(generateBody.data?.refundAmount, 38800);
    expectMoneyCents(generateBody.data?.feeAmount, 260);
    expectMoneyCents(generateBody.data?.netAmount, 89740);
    const summaryId = String(generateBody.data?.id || '');

    const duplicateGenerateResponse = await page.request.post('/api/payment/settlement-summaries/generate', {
      headers,
      data: {
        settlementDate: billDate,
        appCode: 'ORDER_CENTER',
        enterpriseSubjectId: '320001',
        channelCode: 'MANGO_PAY',
      },
    });
    const duplicateGenerateBody = await expectBusinessError(duplicateGenerateResponse);
    expect(duplicateGenerateBody.code).toBe(3789);

    const blockedConfirmResponse = await page.request.post('/api/payment/settlement-summaries/confirm', {
      headers,
      data: { id: summaryId },
    });
    const blockedConfirmBody = await expectBusinessError(blockedConfirmResponse);
    expect(blockedConfirmBody.code).toBe(3791);

    const handleDifferenceResponse = await page.request.post('/api/payment/differences/handle', {
      headers,
      data: {
        id: scenario.differenceId,
        processAction: 'CLOSE',
        processReason: 'E2E 结算汇总确认前人工复核',
        processResult: `确认 ${scenario.payOrderNo} 金额差异已人工复核`,
        processEvidence: `settlement-e2e-${suffix}`,
      },
    });
    await expectBusinessOk<PaymentDifference>(handleDifferenceResponse);

    const confirmResponse = await page.request.post('/api/payment/settlement-summaries/confirm', {
      headers,
      data: { id: summaryId },
    });
    const confirmBody = await expectBusinessOk<PaymentSettlementSummary>(confirmResponse);
    expect(confirmBody.data).toMatchObject({
      id: summaryId,
      status: 'CONFIRMED',
      unresolvedDifferenceCount: 0,
    });

    const voidResponse = await page.request.post('/api/payment/settlement-summaries/void', {
      headers,
      data: {
        id: summaryId,
        voidReason: 'E2E 账单修正后重新生成',
      },
    });
    const voidBody = await expectBusinessOk<PaymentSettlementSummary>(voidResponse);
    expect(voidBody.data).toMatchObject({
      id: summaryId,
      status: 'VOIDED',
      voidReason: 'E2E 账单修正后重新生成',
    });

    const rebuildResponse = await page.request.post('/api/payment/settlement-summaries/generate', {
      headers,
      data: {
        settlementDate: billDate,
        appCode: 'ORDER_CENTER',
        enterpriseSubjectId: '320001',
        channelCode: 'MANGO_PAY',
        rebuild: true,
      },
    });
    const rebuildBody = await expectBusinessOk<PaymentSettlementSummary>(rebuildResponse);
    expect(rebuildBody.data?.id).toBeTruthy();
    expect(rebuildBody.data?.id).not.toBe(summaryId);
    expect(rebuildBody.data).toMatchObject({
      settlementDate: billDate,
      status: 'GENERATED',
      unresolvedDifferenceCount: 0,
    });

    const generateAudit = await findLatestPaymentAudit(page, headers, {
      action: 'GENERATE_SETTLEMENT_SUMMARY',
      resourceType: 'PAYMENT_SETTLEMENT_SUMMARY',
      resourceId: summaryId,
      operationResult: 'SUCCESS',
    });
    expect(generateAudit).toMatchObject({
      operationAction: 'GENERATE_SETTLEMENT_SUMMARY',
      resourceType: 'PAYMENT_SETTLEMENT_SUMMARY',
      resourceId: summaryId,
      operationResult: 'SUCCESS',
    });
    const confirmAudit = await findLatestPaymentAudit(page, headers, {
      action: 'CONFIRM_SETTLEMENT_SUMMARY',
      resourceType: 'PAYMENT_SETTLEMENT_SUMMARY',
      resourceId: summaryId,
      operationResult: 'SUCCESS',
    });
    expect(confirmAudit).toMatchObject({
      operationAction: 'CONFIRM_SETTLEMENT_SUMMARY',
      resourceType: 'PAYMENT_SETTLEMENT_SUMMARY',
      resourceId: summaryId,
      operationResult: 'SUCCESS',
    });

    await openPaymentPage(page, '/#/payment/settlement-summaries', '结算汇总');
    await page.getByPlaceholder('应用 / 主体 / 通道').fill('ORDER_CENTER');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/payment/settlement-summaries/page')),
      page.getByRole('button', { name: '查询' }).click(),
    ]);
    await expect(page.getByText(billDate).first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('芒果科技有限公司').first()).toBeVisible();
    await expect(page.getByText('已生成').first()).toBeVisible();
    await page.screenshot({
      path: 'test-results/payment-settlement-summaries.png',
      fullPage: true,
    });
  });
});
