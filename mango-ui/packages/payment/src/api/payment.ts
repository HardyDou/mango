import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface PaymentPageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number | '';
  statusCode?: string;
  applicationId?: ApiId | '';
  enterpriseSubjectId?: ApiId | '';
  channelId?: ApiId | '';
  channelCode?: string;
}

export interface PaymentApplication {
  id?: ApiId;
  appId?: string;
  appName: string;
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
  createTime?: string;
  updateTime?: string;
}

export interface PaymentApplicationSaveResult {
  id: ApiId;
  appId: string;
  appSecret?: string;
  secretGenerated?: number;
}

export interface PaymentEnterpriseSubject {
  id?: ApiId;
  subjectName: string;
  creditCode?: string;
  creditCodeMask?: string;
  bankAccountNo?: string;
  bankAccountNoMask?: string;
  bankName: string;
  licenseFileId?: ApiId;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentChannel {
  id?: ApiId;
  channelCode: string;
  channelName: string;
  channelType: string;
  adapterType: string;
  gatewayBaseUrl?: string;
  fieldTemplateJson?: string;
  capabilitySummary?: string;
  capabilities?: PaymentChannelCapability[];
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentChannelContract {
  id?: ApiId;
  contractCode?: string;
  contractName: string;
  subjectId: ApiId;
  subjectName?: string;
  channelId: ApiId;
  channelName?: string;
  environment?: string;
  merchantNo: string;
  appId?: string;
  configValuesJson?: string;
  enabledMethodCodes?: string;
  capabilities?: PaymentChannelContractCapability[];
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentChannelCapability {
  id?: ApiId;
  channelId?: ApiId;
  channelName?: string;
  methodCode: string;
  methodName?: string;
  terminalType: string;
  environment: string;
  supportsRefund?: number;
  supportsQuery?: number;
  supportsClose?: number;
  supportsBill?: number;
  supportsReconcile?: number;
  minAmount?: number;
  maxAmount?: number;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentChannelContractCapability {
  id?: ApiId;
  channelCapabilityId: ApiId;
  methodCode?: string;
  methodName?: string;
  terminalType?: string;
  feeRate?: number | string;
  minAmount?: number;
  maxAmount?: number;
  priority?: number;
  certificateExpireTime?: string;
  status?: number;
}

export interface PaymentMethodRouteRule {
  id?: ApiId;
  ruleCode: string;
  ruleName: string;
  appId?: ApiId;
  appName?: string;
  subjectId?: ApiId;
  subjectName?: string;
  methodCode: string;
  methodName?: string;
  terminalType: string;
  environment: string;
  routeMode: string;
  fallbackEnabled?: number;
  status?: number;
  items?: PaymentMethodRouteRuleItem[];
  createTime?: string;
  updateTime?: string;
}

export interface PaymentMethodRouteRuleItem {
  id?: ApiId;
  ruleId?: ApiId;
  contractCapabilityId: ApiId;
  contractId?: ApiId;
  contractName?: string;
  channelId?: ApiId;
  channelName?: string;
  methodCode?: string;
  terminalType?: string;
  priority?: number;
  weight?: number;
  minAmount?: number;
  maxAmount?: number;
  status?: number;
}

export interface PaymentMethodRouteTrialCommand {
  applicationId: ApiId;
  subjectId: ApiId;
  methodCode: string;
  terminalType: string;
  environment: string;
  amount: number;
}

export interface PaymentMethodRouteTrialResult {
  matched?: boolean;
  matchedRule?: PaymentMethodRouteRule;
  matchedItem?: PaymentMethodRouteRuleItem;
  filterReasons?: string[];
}

export interface PaymentMethod {
  id?: ApiId;
  methodCode: string;
  methodName: string;
  accountNature: string;
  instrumentType: string;
  interactionType: string;
  terminalScope: string;
  paymentMaterialType: string;
  cashierGroupCode: string;
  cashierGroupName: string;
  cashierGroupSort?: number;
  iconFileId?: ApiId;
  requiresBankSelection?: number;
  requiresQrRefresh?: number;
  description?: string;
  visibleScope?: string;
  routeStrategy?: string;
  minAmount?: number;
  maxAmount?: number;
  sort?: number;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentMethodCategory {
  id: ApiId;
  categoryCode: string;
  categoryName: string;
  level: number;
  parentId: ApiId;
  sort?: number;
  children?: PaymentMethodCategory[];
}

export interface PaymentCashierConfig {
  id?: ApiId;
  applicationId: ApiId;
  cashierName: string;
  defaultCashier?: number;
  enterpriseSubjectIds: string;
  enterpriseSubjectNames?: string;
  methodCodes?: string;
  methodNames?: string;
  defaultMethodCode?: string;
  defaultMethodName?: string;
  methodDisplayOrder?: string;
  displayConfig?: string;
  resultReturnUrl?: string;
  cashierPath?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentBusinessOrder {
  id?: ApiId;
  bizOrderNo?: string;
  appId?: string;
  appName?: string;
  title?: string;
  subjectId?: ApiId;
  subjectName?: string;
  cashierConfigId?: ApiId;
  cashierName?: string;
  amount?: number;
  paidAmount?: number;
  refundedAmount?: number;
  currency?: string;
  status?: string;
  statusName?: string;
  payable?: boolean;
  payDisabledReason?: string;
  expireTime?: string;
  notifyUrl?: string;
  returnUrl?: string;
  extendInfo?: string;
  paymentOrderCount?: number;
  refundOrderCount?: number;
  createTime?: string;
  updateTime?: string;
  statusFlows?: PaymentOrderStatusFlow[];
}

export interface PaymentBusinessOrderStatus {
  statusCode: string;
  statusName: string;
}

export interface CreatePaymentBusinessOrderCommand {
  appId: string;
  subjectId: ApiId;
  bizOrderNo?: string;
  title: string;
  amount: number;
  currency?: string;
  expireTime?: string;
  notifyUrl?: string;
  returnUrl?: string;
  extendInfo?: string;
}

export interface PaymentOrderStatusFlow {
  fromStatus?: string;
  toStatus?: string;
  statusCode?: string;
  statusName?: string;
  triggerSource?: string;
  happenTime?: string;
  source?: string;
  triggerNo?: string;
  operatorId?: ApiId;
  operatorName?: string;
  remark?: string;
}

export interface PaymentOrder {
  id?: ApiId;
  payOrderNo?: string;
  businessOrderId?: ApiId;
  bizOrderNo?: string;
  title?: string;
  appId?: string;
  subjectId?: ApiId;
  subjectName?: string;
  cashierConfigId?: ApiId;
  cashierName?: string;
  methodId?: ApiId;
  methodCode?: string;
  methodName?: string;
  channelId?: ApiId;
  channelCode?: string;
  channelName?: string;
  channelMerchantNo?: string;
  contractId?: ApiId;
  contractName?: string;
  contractCapabilityId?: ApiId;
  routeRuleId?: ApiId;
  amount?: number;
  refundedAmount?: number;
  currency?: string;
  status?: string;
  statusName?: string;
  channelTradeNo?: string;
  successFlag?: number;
  payTime?: string;
  expireTime?: string;
  createTime?: string;
  updateTime?: string;
  flowNo?: string;
  statusFlows?: PaymentOrderStatusFlow[];
}

export interface PaymentOrderStatus {
  statusCode: string;
  statusName: string;
}

export interface PaymentOfflineCollection {
  id?: ApiId;
  offlineCollectionNo?: string;
  paymentOrderId?: ApiId;
  payOrderNo?: string;
  businessOrderId?: ApiId;
  bizOrderNo?: string;
  title?: string;
  appId?: string;
  channelId?: ApiId;
  channelCode?: string;
  channelName?: string;
  contractId?: ApiId;
  contractName?: string;
  contractCapabilityId?: ApiId;
  subjectId?: ApiId;
  subjectName?: string;
  bankAccountId?: ApiId;
  accountName?: string;
  accountNoMask?: string;
  bankName?: string;
  amount?: number;
  currency?: string;
  transferAmount?: number;
  voucherFileIds?: string;
  submittedTime?: string;
  submitRemark?: string;
  confirmedAmount?: number;
  confirmedBy?: ApiId;
  confirmedByName?: string;
  confirmRemark?: string;
  reconciliationCode?: string;
  transferRemark?: string;
  voucherCount?: number;
  collectionStatus?: string;
  collectionStatusName?: string;
  expireTime?: string;
  confirmedTime?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentOfflineCollectionStatus {
  statusCode: string;
  statusName: string;
  code?: string;
  label?: string;
}

export interface PaymentOfflineBankStatementItem {
  id?: ApiId;
  batchId?: ApiId;
  batchNo?: string;
  rowNo?: number;
  bankStatementNo?: string;
  bankAccountNoMask?: string;
  bankName?: string;
  tradeTime?: string;
  amount?: number;
  currency?: string;
  counterpartyName?: string;
  counterpartyAccountNoMask?: string;
  summary?: string;
  remark?: string;
  reconciliationCode?: string;
  matchedOfflineCollectionId?: ApiId;
  matchedOfflineCollectionNo?: string;
  matchedPayOrderNo?: string;
  matchStatus?: string;
  matchStatusName?: string;
  matchMessage?: string;
  confirmedTime?: string;
  confirmedByName?: string;
  confirmRemark?: string;
  createTime?: string;
}

export interface PaymentOfflineBankStatementBatch {
  id?: ApiId;
  batchNo?: string;
  bankAccountNoMask?: string;
  bankName?: string;
  statementFileId?: ApiId;
  statementFileName?: string;
  fileDigest?: string;
  totalCount?: number;
  matchedCount?: number;
  confirmedCount?: number;
  differenceCount?: number;
  batchStatus?: string;
  batchStatusName?: string;
  importerName?: string;
  importTime?: string;
  createTime?: string;
  updateTime?: string;
  items?: PaymentOfflineBankStatementItem[];
}

export interface PaymentOfflineBankStatementStatus {
  code?: string;
  label?: string;
  statusCode?: string;
  statusName?: string;
}

export interface ConfirmOfflineBankStatementMatchCommand {
  itemIds: ApiId[];
  confirmRemark?: string;
}

export interface SubmitOfflineTransferVoucherCommand {
  payOrderNo: string;
  transferAmount: number;
  voucherFileIds: string;
  submitRemark?: string;
}

export interface ConfirmOfflineCollectionCommand {
  id: ApiId;
  confirmedAmount: number;
  confirmRemark?: string;
}

export interface CreateOfflineRefundCommand {
  offlineCollectionId: ApiId;
  refundAmount: number;
  refundAccountName: string;
  refundAccountNo: string;
  refundBankName: string;
  refundVoucherFileIds: string;
  reason: string;
  remark?: string;
}

export interface PaymentOfflineRefund {
  id?: ApiId;
  offlineRefundNo?: string;
  offlineCollectionId?: ApiId;
  offlineCollectionNo?: string;
  paymentOrderId?: ApiId;
  payOrderNo?: string;
  businessOrderId?: ApiId;
  bizOrderNo?: string;
  title?: string;
  appId?: string;
  channelId?: ApiId;
  channelCode?: string;
  channelName?: string;
  refundAmount?: number;
  currency?: string;
  refundAccountName?: string;
  refundAccountNoMask?: string;
  refundBankName?: string;
  refundVoucherFileIds?: string;
  refundVoucherCount?: number;
  reason?: string;
  remark?: string;
  refundStatus?: string;
  refundStatusName?: string;
  refundedTime?: string;
  operatorId?: ApiId;
  operatorName?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentOfflineRefundStatus {
  code?: string;
  label?: string;
  statusCode?: string;
  statusName?: string;
}

export interface PaymentRefundOrder {
  id?: ApiId;
  refundOrderNo?: string;
  bizRefundNo?: string;
  paymentOrderId?: ApiId;
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
  createTime?: string;
  updateTime?: string;
  flowNo?: string;
  statusFlows?: PaymentOrderStatusFlow[];
}

export interface PaymentRefundOrderStatus {
  statusCode: string;
  statusName: string;
}

export interface PaymentRefundApproval {
  id?: ApiId;
  approvalNo?: string;
  businessOrderId?: ApiId;
  paymentOrderId?: ApiId;
  refundOrderId?: ApiId;
  bizOrderNo?: string;
  bizRefundNo?: string;
  appId?: string;
  refundAmount?: number;
  reason?: string;
  status?: string;
  statusName?: string;
  applicantId?: ApiId;
  applicantName?: string;
  reviewerId?: ApiId;
  reviewerName?: string;
  reviewReason?: string;
  applyTime?: string;
  reviewTime?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentRefundApprovalStatus {
  statusCode: string;
  statusName: string;
}

export interface CreatePaymentRefundApprovalCommand {
  paymentOrderId: ApiId;
  bizRefundNo?: string;
  refundAmount: number;
  reason: string;
  remark?: string;
}

export interface QueryPaymentRefundOrderCommand {
  id: ApiId;
}

export interface PaymentTransactionFlow {
  id?: ApiId;
  flowNo?: string;
  businessOrderId?: ApiId;
  bizOrderNo?: string;
  paymentOrderId?: ApiId;
  payOrderNo?: string;
  refundOrderId?: ApiId;
  refundOrderNo?: string;
  flowType?: string;
  flowTypeName?: string;
  amount?: number;
  currency?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentExceptionOrder {
  id?: ApiId;
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
  handlerId?: ApiId;
  handlerName?: string;
  handleTime?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentExceptionOrderStatus {
  statusCode: string;
  statusName: string;
}

export interface PaymentExceptionOrderAction {
  actionCode: string;
  actionName: string;
  allowedExceptionTypes?: string[];
  description?: string;
}

export interface HandlePaymentExceptionOrderCommand {
  id: ApiId;
  handleAction: string;
  handleReason: string;
  handleResult: string;
  handleEvidence?: string;
}

export interface PaymentNotificationRecord {
  id?: ApiId;
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
  lastManualRetryTime?: string;
  lastManualRetryReason?: string;
  lastManualRetryResult?: string;
  lastManualRetryOperatorId?: ApiId;
  lastManualRetryOperatorName?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentNotificationStatus {
  statusCode: string;
  statusName: string;
}

export interface RetryPaymentNotificationRecordCommand {
  id: ApiId;
  retryReason: string;
}

export interface DeliverDuePaymentNotificationRecordCommand {
  limit: number;
}

export interface PaymentDifference {
  id?: ApiId;
  differenceNo?: string;
  reconciliationId?: ApiId;
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
  processorId?: ApiId;
  processorName?: string;
  processTime?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentDifferenceStatus {
  statusCode: string;
  statusName: string;
}

export interface PaymentDifferenceAction {
  actionCode: string;
  actionName: string;
}

export interface HandlePaymentDifferenceCommand {
  id: ApiId;
  processAction: string;
  processReason: string;
  processResult: string;
  processEvidence?: string;
}

export interface PaymentChannelBillDetail {
  id?: ApiId;
  reconciliationId?: ApiId;
  batchNo?: string;
  channelCode?: string;
  billDate?: string;
  channelTradeNo?: string;
  tradeType?: string;
  tradeTypeName?: string;
  amount?: number | string;
  fee?: number | string;
  tradeTime?: string;
  matchStatus?: string;
  matchStatusName?: string;
  matchedOrderNo?: string;
  matchMessage?: string;
  createTime?: string;
}

export interface PaymentReconciliation {
  id?: ApiId;
  reconciliationNo?: string;
  channelCode?: string;
  billDate?: string;
  totalCount?: number;
  totalAmount?: number | string;
  totalFee?: number | string;
  matchStatus?: string;
  matchStatusName?: string;
  billFileId?: ApiId;
  billFileName?: string;
  fileDigest?: string;
  importerId?: ApiId;
  importerName?: string;
  importTime?: string;
  reconcileResult?: string;
  createTime?: string;
  updateTime?: string;
  details?: PaymentChannelBillDetail[];
}

export interface PaymentReconciliationStatus {
  statusCode: string;
  statusName: string;
}

export interface ImportPaymentReconciliationCommand {
  channelCode: string;
  billDate: string;
  billFileId?: ApiId | '';
  billFileName: string;
  fileDigest: string;
  items: Array<{
    channelTradeNo: string;
    tradeType: string;
    amount: number;
    fee: number;
    tradeTime: string;
  }>;
}

export interface GenerateMangoPayVirtualBillCommand {
  channelCode: string;
  contractId?: ApiId | '';
  billDate: string;
}

export interface PaymentSettlementSummary {
  id?: ApiId;
  settlementDate?: string;
  appCode?: string;
  appName?: string;
  enterpriseSubjectId?: ApiId;
  subjectName?: string;
  channelCode?: string;
  channelName?: string;
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
  generatedBy?: ApiId;
  generatedByName?: string;
  generatedAt?: string;
  confirmedBy?: ApiId;
  confirmedByName?: string;
  confirmedAt?: string;
  voidedBy?: ApiId;
  voidedByName?: string;
  voidedAt?: string;
  voidReason?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentSettlementSummaryStatus {
  statusCode: string;
  statusName: string;
}

export interface PaymentOperationAudit {
  id?: ApiId;
  operatorId?: ApiId;
  operatorName?: string;
  operationAction?: string;
  resourceType?: string;
  resourceId?: string;
  operationResult?: string;
  operationTime?: string;
  createTime?: string;
  updateTime?: string;
}

export interface GeneratePaymentSettlementSummaryCommand {
  settlementDate: string;
  appCode: string;
  enterpriseSubjectId: ApiId;
  channelCode: string;
  rebuild?: boolean;
}

export interface ConfirmPaymentSettlementSummaryCommand {
  id: ApiId;
}

export interface VoidPaymentSettlementSummaryCommand {
  id: ApiId;
  voidReason: string;
}

export interface PaymentTableColumn {
  prop: string;
  label: string;
  width?: number;
  minWidth?: number;
  variant?: 'text' | 'tag' | 'tags';
  money?: boolean;
  formatter?: (row: Record<string, unknown>, value: unknown) => string;
}

export type PaymentRecord = Record<string, unknown>;

export interface PaymentCashierDisplay {
  logoFileId?: ApiId;
  title?: string;
  subtitle?: string;
  helpText?: string;
}

export interface PaymentCashierApplication {
  id?: ApiId;
  appId?: string;
  appName?: string;
}

export interface PaymentCashierOrder {
  businessOrderId?: ApiId;
  bizOrderNo?: string;
  orderTitle?: string;
  amount?: number | string;
  currency?: string;
  status?: string;
  expireTime?: string;
}

export interface PaymentCashierSubject {
  id?: ApiId;
  subjectName?: string;
  creditCode?: string;
  bankAccountNo?: string;
  bankName?: string;
}

export interface PaymentCashierMethod {
  methodCode: string;
  methodName: string;
  categoryCode: string;
  categoryName: string;
  categorySort?: number;
  accountNature?: string;
  instrumentType?: string;
  interactionType?: string;
  paymentMaterialType?: string;
  iconFileId?: ApiId;
  description?: string;
  channelId?: ApiId;
  channelName?: string;
  contractId?: ApiId;
  contractName?: string;
  contractCapabilityId?: ApiId;
  routeRuleId?: ApiId;
  channelMerchantNo?: string;
  selected?: boolean;
}

export interface PaymentCashierSession {
  cashierConfigId: ApiId;
  cashierName: string;
  preview?: boolean;
  status?: string;
  defaultMethodCode?: string;
  serverTime?: string;
  display?: PaymentCashierDisplay;
  application?: PaymentCashierApplication;
  order?: PaymentCashierOrder;
  subject?: PaymentCashierSubject;
  methods: PaymentCashierMethod[];
}

export interface PaymentCashierPayMaterial {
  materialType?: string;
  qrContent?: string;
  redirectUrl?: string;
  htmlForm?: string;
  accountName?: string;
  accountNo?: string;
  accountNoMask?: string;
  bankName?: string;
  transferRemark?: string;
  transferInstruction?: string;
  reconciliationCode?: string;
  voucherRequired?: boolean;
  expireTime?: string;
}

export interface PaymentCashierPayResult {
  payOrderNo: string;
  flowNo?: string;
  status: string;
  channelCode?: string;
  channelName?: string;
  methodCode: string;
  methodName: string;
  amount?: number | string;
  paidTime?: string;
  material?: PaymentCashierPayMaterial;
}

export interface PaymentCashierPayCommand {
  cashierConfigId: ApiId;
  businessOrderId?: ApiId;
  methodCode: string;
  bankCode?: string;
  bankName?: string;
  payerAccountNo?: string;
  payerName?: string;
}

export interface MangoPayVirtualPaymentCommand {
  cashierConfigId: ApiId;
  payOrderNo: string;
  title: string;
  amount: number;
  paymentMethodCode?: string;
  payerName?: string;
}

export interface MangoPayVirtualPaymentResult {
  virtualPaymentNo: string;
  payOrderNo: string;
  status: string;
  title: string;
  amount: number;
  paidTime?: string;
}

export interface PaymentResourceApi<T extends object> {
  page(params?: PaymentPageQuery): Promise<PageResult<T>>;
  detail(id: ApiId): Promise<T>;
  create(data: Partial<T> | Record<string, unknown>): Promise<unknown>;
  update(data: Partial<T> | Record<string, unknown>): Promise<unknown>;
  remove(id: ApiId): Promise<boolean>;
}

export interface PaymentReadonlyResourceApi<T extends object> {
  page(params?: PaymentPageQuery): Promise<PageResult<T>>;
  detail(id: ApiId): Promise<T>;
}

export function createPaymentResourceApi<T extends object>(baseUrl: string): PaymentResourceApi<T> {
  return {
    page: (params?: PaymentPageQuery) => get<unknown>(`${baseUrl}/page`, { params: toBackendPageParams(params) }).then(data => fromBackendPageResult<T>(data, params)),
    detail: (id: ApiId) => get<T>(`${baseUrl}/detail`, { params: { id } }),
    create: (data: T) => post<ApiId>(baseUrl, data),
    update: (data: T) => put<boolean>(baseUrl, data),
    remove: (id: ApiId) => del<boolean>(baseUrl, { params: { id } }),
  };
}

export function createPaymentReadonlyResourceApi<T extends object>(baseUrl: string): PaymentReadonlyResourceApi<T> {
  return {
    page: (params?: PaymentPageQuery) => get<unknown>(`${baseUrl}/page`, { params: toBackendPageParams(params) }).then(data => fromBackendPageResult<T>(data, params)),
    detail: (id: ApiId) => get<T>(`${baseUrl}/detail`, { params: { id } }),
  };
}

export const paymentApplicationApi: PaymentResourceApi<PaymentApplication> & {
  create(data: PaymentApplication): Promise<PaymentApplicationSaveResult>;
  update(data: PaymentApplication): Promise<PaymentApplicationSaveResult>;
} = createPaymentResourceApi<PaymentApplication>('/payment/applications') as PaymentResourceApi<PaymentApplication> & {
  create(data: PaymentApplication): Promise<PaymentApplicationSaveResult>;
  update(data: PaymentApplication): Promise<PaymentApplicationSaveResult>;
};
export const paymentEnterpriseSubjectApi = createPaymentResourceApi<PaymentEnterpriseSubject>('/payment/enterprise-subjects');
export const paymentChannelApi = createPaymentResourceApi<PaymentChannel>('/payment/channels');
export const paymentChannelContractApi = createPaymentResourceApi<PaymentChannelContract>('/payment/channel-contracts');
export const paymentChannelCapabilityApi = createPaymentResourceApi<PaymentChannelCapability>('/payment/channel-capabilities');
export const paymentMethodApi = {
  ...createPaymentResourceApi<PaymentMethod>('/payment/methods'),
  categories: () => get<PaymentMethodCategory[]>('/payment/methods/categories'),
};
export const paymentMethodRouteApi = {
  ...createPaymentResourceApi<PaymentMethodRouteRule>('/payment/method-routes'),
  trial: (data: PaymentMethodRouteTrialCommand) => post<PaymentMethodRouteTrialResult>('/payment/method-routes/trial', data),
};
export const paymentCashierConfigApi = createPaymentResourceApi<PaymentCashierConfig>('/payment/cashier-configs');

export const paymentBusinessOrderApi = {
  ...createPaymentResourceApi<PaymentBusinessOrder>('/payment/business-orders'),
  create: (data: CreatePaymentBusinessOrderCommand) => post<PaymentBusinessOrder>('/payment/business-orders', data),
  statuses: () => get<PaymentBusinessOrderStatus[]>('/payment/business-orders/statuses'),
};
export const paymentOrderApi = {
  ...createPaymentResourceApi<PaymentOrder>('/payment/payment-orders'),
  statuses: () => get<PaymentOrderStatus[]>('/payment/payment-orders/statuses'),
};
export const paymentOfflineCollectionApi = {
  ...createPaymentReadonlyResourceApi<PaymentOfflineCollection>('/payment/offline-collections'),
  statuses: () => get<PaymentOfflineCollectionStatus[]>('/payment/offline-collections/statuses'),
  confirm: (data: ConfirmOfflineCollectionCommand) => post<PaymentOfflineCollection>('/payment/offline-collections/confirm', data),
  refund: (data: CreateOfflineRefundCommand) => post<PaymentOfflineRefund>('/payment/offline-collections/refund', data),
  bankStatements: (params?: PaymentPageQuery) => get<PageResult<PaymentOfflineBankStatementBatch>>('/payment/offline-collections/bank-statements/page', {
    params: toBackendPageParams(params),
  }),
  bankStatementDetail: (id: ApiId) => get<PaymentOfflineBankStatementBatch>('/payment/offline-collections/bank-statements/detail', { params: { id } }),
  bankStatementStatuses: () => get<PaymentOfflineBankStatementStatus[]>('/payment/offline-collections/bank-statements/statuses'),
  bankStatementMatchStatuses: () => get<PaymentOfflineBankStatementStatus[]>('/payment/offline-collections/bank-statements/match-statuses'),
  importBankStatement: (file: File, statementFileId?: ApiId) => {
    const formData = new FormData();
    formData.append('file', file);
    if (statementFileId) {
      formData.append('statementFileId', String(statementFileId));
    }
    return post<PaymentOfflineBankStatementBatch>('/payment/offline-collections/bank-statements/import', formData);
  },
  confirmBankStatement: (data: ConfirmOfflineBankStatementMatchCommand) => post<PaymentOfflineBankStatementBatch>(
    '/payment/offline-collections/bank-statements/confirm',
    data,
  ),
};
export const paymentOfflineRefundApi = {
  ...createPaymentReadonlyResourceApi<PaymentOfflineRefund>('/payment/offline-refunds'),
  statuses: () => get<PaymentOfflineRefundStatus[]>('/payment/offline-refunds/statuses'),
};
export const paymentRefundOrderApi = {
  ...createPaymentResourceApi<PaymentRefundOrder>('/payment/refund-orders'),
  statuses: () => get<PaymentRefundOrderStatus[]>('/payment/refund-orders/statuses'),
  queryChannel: (data: QueryPaymentRefundOrderCommand) => post<PaymentRefundOrder>('/payment/refund-orders/query-channel', data),
};
export const paymentRefundApprovalApi = {
  ...createPaymentReadonlyResourceApi<PaymentRefundApproval>('/payment/refund-approvals'),
  statuses: () => get<PaymentRefundApprovalStatus[]>('/payment/refund-approvals/statuses'),
  create: (data: CreatePaymentRefundApprovalCommand) => post<PaymentRefundApproval>('/payment/refund-approvals', data),
};
export const paymentTransactionFlowApi = createPaymentReadonlyResourceApi<PaymentTransactionFlow>('/payment/transaction-flows');
export const paymentExceptionOrderApi = {
  ...createPaymentResourceApi<PaymentExceptionOrder>('/payment/exception-orders'),
  statuses: () => get<PaymentExceptionOrderStatus[]>('/payment/exception-orders/statuses'),
  actions: () => get<PaymentExceptionOrderAction[]>('/payment/exception-orders/actions'),
  handle: (data: HandlePaymentExceptionOrderCommand) => post<PaymentExceptionOrder>('/payment/exception-orders/handle', data),
};
export const paymentNotificationRecordApi = {
  ...createPaymentResourceApi<PaymentNotificationRecord>('/payment/notification-records'),
  statuses: () => get<PaymentNotificationStatus[]>('/payment/notification-records/statuses'),
  retry: (data: RetryPaymentNotificationRecordCommand) => post<PaymentNotificationRecord>('/payment/notification-records/retry', data),
  deliverDue: (data: DeliverDuePaymentNotificationRecordCommand) =>
    post<number>('/payment/notification-records/deliver-due', undefined, { params: { limit: data.limit } }),
};
export const paymentReconciliationApi = {
  ...createPaymentResourceApi<PaymentReconciliation>('/payment/reconciliations'),
  statuses: () => get<PaymentReconciliationStatus[]>('/payment/reconciliations/statuses'),
  importBill: (data: ImportPaymentReconciliationCommand) => post<PaymentReconciliation>('/payment/reconciliations/import', data),
  generateMangoPayVirtualBill: (data: GenerateMangoPayVirtualBillCommand) => post<PaymentReconciliation>('/payment/reconciliations/mango-pay/virtual/generate', data),
};
export const paymentDifferenceApi = {
  ...createPaymentResourceApi<PaymentDifference>('/payment/differences'),
  statuses: () => get<PaymentDifferenceStatus[]>('/payment/differences/statuses'),
  actions: () => get<PaymentDifferenceAction[]>('/payment/differences/actions'),
  handle: (data: HandlePaymentDifferenceCommand) => post<PaymentDifference>('/payment/differences/handle', data),
};
export const paymentSettlementSummaryApi = {
  ...createPaymentResourceApi<PaymentSettlementSummary>('/payment/settlement-summaries'),
  statuses: () => get<PaymentSettlementSummaryStatus[]>('/payment/settlement-summaries/statuses'),
  generate: (data: GeneratePaymentSettlementSummaryCommand) => post<PaymentSettlementSummary>('/payment/settlement-summaries/generate', data),
  confirm: (data: ConfirmPaymentSettlementSummaryCommand) => post<PaymentSettlementSummary>('/payment/settlement-summaries/confirm', data),
  void: (data: VoidPaymentSettlementSummaryCommand) => post<PaymentSettlementSummary>('/payment/settlement-summaries/void', data),
};
export const paymentOperationAuditApi = {
  page: (params?: PaymentPageQuery) =>
    get<unknown>('/payment/operation-audits/page', { params: toBackendPageParams(params) })
      .then(data => fromBackendPageResult<PaymentOperationAudit>(data, params)),
};

export const paymentCashierApi = {
  session: (cashierConfigId: ApiId, businessOrderId?: ApiId | '') => get<PaymentCashierSession>('/payment/cashier/session', {
    params: {
      cashierConfigId,
      businessOrderId: businessOrderId || undefined,
    },
  }),
  pay: (data: PaymentCashierPayCommand) => post<PaymentCashierPayResult>('/payment/cashier/pay', data),
  payResult: (payOrderNo: string) => get<PaymentCashierPayResult>('/payment/cashier/pay-result', { params: { payOrderNo } }),
  submitOfflineTransferVoucher: (data: SubmitOfflineTransferVoucherCommand) =>
    post<PaymentOfflineCollection>('/payment/cashier/offline-collections/transfer-voucher', data),
  mangoPayVirtualPay: (data: MangoPayVirtualPaymentCommand) => post<MangoPayVirtualPaymentResult>('/payment/mango-pay/virtual/pay', data),
};

function toBackendPageParams(params?: PaymentPageQuery) {
  if (!params) return params;
  const { pageNum, pageSize, status, ...rest } = params;
  return {
    page: pageNum,
    size: pageSize,
    status: status === '' ? undefined : status,
    ...rest,
  };
}

function isPagePayload(data: unknown): data is {
  list?: unknown[];
  records?: unknown[];
  total?: unknown;
  pageNum?: unknown;
  page?: unknown;
  current?: unknown;
  pageSize?: unknown;
  size?: unknown;
} {
  return typeof data === 'object' && data !== null;
}

function fromBackendPageResult<T>(data: unknown, params?: PaymentPageQuery): PageResult<T> {
  const payload = isPagePayload(data) ? data : {};
  return {
    list: (payload.list || payload.records || []) as T[],
    total: Number(payload.total || 0),
    pageNum: Number(payload.pageNum || payload.page || payload.current || params?.pageNum || 1),
    pageSize: Number(payload.pageSize || payload.size || params?.pageSize || 10),
  };
}
