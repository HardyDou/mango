import { get, post } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export type PayBizOrderStatus = 'CREATED' | 'PAYING' | 'PAID' | 'CLOSED' | 'PARTIAL_REFUNDED' | 'REFUNDED' | 'FAILED';
export type PaymentOrderStatus = 'CREATED' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'CLOSED' | 'EXPIRED';
export type PaymentMaterialType = 'QRCODE' | 'REDIRECT_URL' | 'SANDBOX_TOKEN';
export type RefundOrderStatus = 'CREATED' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'CLOSED';
export type PaymentManageDomain =
  'overview'
  | 'applications'
  | 'subjects'
  | 'channels'
  | 'methods'
  | 'cashiers'
  | 'orders'
  | 'refunds'
  | 'notifies'
  | 'reconcile'
  | 'settlement'
  | 'audit';

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface CreatePayBizOrderCommand {
  appCode: string;
  merchantOrderNo: string;
  subject: string;
  amount: number;
  currency?: string;
}

export interface PayCommand {
  bizOrderId: ApiId;
  payMethod: string;
  idempotencyKey: string;
}

export interface QueryPayBizOrderCommand {
  bizOrderId: ApiId;
}

export interface QueryPaymentOrderCommand {
  paymentOrderId: ApiId;
}

export interface RefreshPaymentStatusCommand extends QueryPaymentOrderCommand {
}

export interface ClosePayBizOrderCommand extends QueryPayBizOrderCommand {
}

export interface RefundCommand {
  bizOrderId: ApiId;
  merchantRefundNo: string;
  refundAmount: number;
  idempotencyKey: string;
}

export interface QueryRefundOrderCommand {
  refundOrderId: ApiId;
}

export interface RefreshRefundStatusCommand extends QueryRefundOrderCommand {
}

export interface SandboxPaymentCommand {
  paymentOrderId: ApiId;
  sandboxEventId: string;
}

export interface PaymentNotifyCommand {
  paymentOrderId: ApiId;
  channelOrderNo: string;
  notifyEventId: string;
  signature: string;
}

export interface SandboxPaymentNotifyVO {
  channelCode: string;
  paymentOrderId: ApiId;
  channelOrderNo: string;
  notifyEventId: string;
  signature: string;
  notifyCommand: PaymentNotifyCommand;
}

export interface PayBizOrderVO {
  bizOrderId: ApiId;
  merchantOrderNo: string;
  amount: number;
  refundedAmount?: number;
  currency: string;
  status: PayBizOrderStatus;
}

export interface PaymentOrderVO {
  paymentOrderId: ApiId;
  bizOrderId: ApiId;
  channelCode: string;
  amount: number;
  status: PaymentOrderStatus;
  materialType?: PaymentMaterialType;
  materialContent?: string;
}

export interface RefundOrderVO {
  refundOrderId: ApiId;
  bizOrderId: ApiId;
  paymentOrderId: ApiId;
  merchantRefundNo: string;
  refundAmount: number;
  status: RefundOrderStatus;
}

export interface PayBizOrder {
  id?: ApiId;
  appCode: string;
  merchantOrderNo: string;
  subject: string;
  amount: number;
  refundedAmount?: number;
  currency: string;
  status: PayBizOrderStatus;
  createTime?: string;
  updateTime?: string;
}

export interface PaymentOrder {
  id?: ApiId;
  bizOrderId: ApiId;
  channelCode: string;
  channelOrderNo?: string;
  payMethod: string;
  idempotencyKey: string;
  amount: number;
  status: PaymentOrderStatus;
  materialType?: PaymentMaterialType;
  materialContent?: string;
  createTime?: string;
  updateTime?: string;
}

export interface RefundOrder {
  id?: ApiId;
  bizOrderId: ApiId;
  paymentOrderId: ApiId;
  merchantRefundNo: string;
  channelRefundNo?: string;
  idempotencyKey: string;
  refundAmount: number;
  status: RefundOrderStatus;
  createTime?: string;
  updateTime?: string;
}

export interface PayBizOrderQuery {
  pageNum?: number;
  pageSize?: number;
  appCode?: string;
  merchantOrderNo?: string;
  status?: PayBizOrderStatus | '';
}

export interface PaymentOrderQuery {
  pageNum?: number;
  pageSize?: number;
  bizOrderId?: ApiId | '';
  channelCode?: string;
  status?: PaymentOrderStatus | '';
}

export interface RefundOrderQuery {
  pageNum?: number;
  pageSize?: number;
  bizOrderId?: ApiId | '';
  merchantRefundNo?: string;
  status?: RefundOrderStatus | '';
}

export interface PaymentTenantCashier {
  tenantId: ApiId;
  tenantName: string;
  appCode: string;
  cashierCode: string;
  cashierName: string;
  enabledMethods: string[];
  defaultMethod: string;
  expireMinutes: number;
  dailyLimit: number;
}

export interface PaymentManageDomainMeta {
  code: PaymentManageDomain;
  title: string;
  description: string;
  badge: string;
  sortOrder?: number;
}

export interface PaymentManageItem {
  id: string;
  domain: PaymentManageDomain;
  code: string;
  name: string;
  owner: string;
  status: 'ENABLED' | 'DISABLED' | 'PENDING' | 'FAILED';
  primaryText: string;
  secondaryText: string;
  updatedAt: string;
}

export interface PaymentMethodOption {
  label: string;
  value: string;
  channelCode?: string;
  status?: string;
  singleLimit?: number;
}

export const paymentApi = {
  createBizOrder: (command: CreatePayBizOrderCommand) => post<ApiId>('/payment/biz-orders', command),
  pay: (command: PayCommand) => post<PaymentOrderVO>('/payment/payments', command),
  closeBizOrder: (command: ClosePayBizOrderCommand) => post<boolean>('/payment/biz-orders/close', command),
  queryBizOrder: (command: QueryPayBizOrderCommand) => post<PayBizOrderVO>('/payment/biz-orders/query', command),
  queryPaymentOrder: (command: QueryPaymentOrderCommand) => post<PaymentOrderVO>('/payment/payments/query', command),
  refreshPaymentStatus: (command: RefreshPaymentStatusCommand) => post<PaymentOrderVO>('/payment/payments/refresh', command),
  refund: (command: RefundCommand) => post<RefundOrderVO>('/payment/refunds', command),
  queryRefundOrder: (command: QueryRefundOrderCommand) => post<RefundOrderVO>('/payment/refunds/query', command),
  refreshRefundStatus: (command: RefreshRefundStatusCommand) => post<RefundOrderVO>('/payment/refunds/refresh', command),
  pageBizOrders: (params?: PayBizOrderQuery) => get<unknown>('/payment/biz-orders/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, fromBackendBizOrder, params)),
  detailBizOrder: (id: ApiId) => get<unknown>('/payment/biz-orders/detail', { params: { id } }).then(fromBackendBizOrder),
  pagePaymentOrders: (params?: PaymentOrderQuery) => get<unknown>('/payment/orders/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, fromBackendPaymentOrder, params)),
  detailPaymentOrder: (id: ApiId) => get<unknown>('/payment/orders/detail', { params: { id } }).then(fromBackendPaymentOrder),
  pageRefundOrders: (params?: RefundOrderQuery) => get<unknown>('/payment/refund-orders/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, fromBackendRefundOrder, params)),
  detailRefundOrder: (id: ApiId) => get<unknown>('/payment/refund-orders/detail', { params: { id } }).then(fromBackendRefundOrder),
  createSandboxPaymentNotify: (command: SandboxPaymentCommand) =>
    post<SandboxPaymentNotifyVO>('/payment/sandbox/payment-notifies', command).then(fromSandboxPaymentNotify),
  completeSandboxPayment: (command: SandboxPaymentCommand) =>
    post<PaymentOrderVO>('/payment/sandbox/payments/complete', command).then(fromPaymentOrderVO),
  listManageDomains: () => get<unknown[]>('/payment/management/domains').then(data => data.map(fromManageDomain)),
  listManageItems: (domain: PaymentManageDomain) =>
    get<unknown[]>('/payment/management/items', { params: { domain } }).then(data => data.map(fromManageItem)),
  listTenantCashiers: () =>
    get<unknown[]>('/payment/management/tenant-cashiers').then(data => data.map(fromTenantCashier)),
  listSandboxMethods: () =>
    get<unknown[]>('/payment/management/sandbox-methods').then(data => data.map(fromPaymentMethodOption)),
};

export const payBizStatusOptions: Array<{ label: string; value: PayBizOrderStatus }> = [
  { label: '已创建', value: 'CREATED' },
  { label: '支付中', value: 'PAYING' },
  { label: '已支付', value: 'PAID' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '部分退款', value: 'PARTIAL_REFUNDED' },
  { label: '已退款', value: 'REFUNDED' },
  { label: '失败', value: 'FAILED' },
];

export const paymentStatusOptions: Array<{ label: string; value: PaymentOrderStatus }> = [
  { label: '已创建', value: 'CREATED' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '已过期', value: 'EXPIRED' },
];

export const refundStatusOptions: Array<{ label: string; value: RefundOrderStatus }> = [
  { label: '已创建', value: 'CREATED' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已关闭', value: 'CLOSED' },
];

export function statusLabel(value: string | undefined, options: Array<{ label: string; value: string }>) {
  return options.find(item => item.value === value)?.label || value || '-';
}

function toBackendPageParams(params?: PayBizOrderQuery | PaymentOrderQuery | RefundOrderQuery) {
  if (!params) return params;
  const { pageNum, pageSize, status, bizOrderId, ...rest } = params;
  return {
    ...rest,
    status: status === '' ? undefined : status,
    bizOrderId: bizOrderId === '' ? undefined : bizOrderId,
    page: pageNum,
    size: pageSize,
  };
}

function fromBackendPageResult<T>(data: unknown, mapper: (item: unknown) => T, params?: { pageNum?: number; pageSize?: number }): PageResult<T> {
  const record = toRecord(data);
  const rawList = Array.isArray(record.list) ? record.list : Array.isArray(record.records) ? record.records : [];
  const list = rawList.map(mapper);
  return {
    list,
    total: toNumber(record.total, list.length),
    pageNum: toNumber(record.page ?? record.current, params?.pageNum ?? 1),
    pageSize: toNumber(record.size ?? record.pageSize, params?.pageSize ?? 10),
  };
}

function fromBackendBizOrder(item: unknown): PayBizOrder {
  const record = toRecord(item);
  return {
    id: normalizeId(record.id),
    appCode: toStringValue(record.appCode),
    merchantOrderNo: toStringValue(record.merchantOrderNo),
    subject: toStringValue(record.subject),
    amount: toNumber(record.amount, 0),
    refundedAmount: toNumber(record.refundedAmount, 0),
    currency: toStringValue(record.currency, 'CNY'),
    status: toPayBizOrderStatus(record.status),
    createTime: normalizeDateTime(record.createTime),
    updateTime: normalizeDateTime(record.updateTime),
  };
}

function fromBackendPaymentOrder(item: unknown): PaymentOrder {
  const record = toRecord(item);
  return {
    id: normalizeId(record.id),
    bizOrderId: normalizeId(record.bizOrderId),
    channelCode: toStringValue(record.channelCode),
    channelOrderNo: toOptionalString(record.channelOrderNo),
    payMethod: toStringValue(record.payMethod),
    idempotencyKey: toStringValue(record.idempotencyKey),
    amount: toNumber(record.amount, 0),
    status: toPaymentOrderStatus(record.status),
    materialType: toPaymentMaterialType(record.materialType),
    materialContent: toOptionalString(record.materialContent),
    createTime: normalizeDateTime(record.createTime),
    updateTime: normalizeDateTime(record.updateTime),
  };
}

function fromBackendRefundOrder(item: unknown): RefundOrder {
  const record = toRecord(item);
  return {
    id: normalizeId(record.id),
    bizOrderId: normalizeId(record.bizOrderId),
    paymentOrderId: normalizeId(record.paymentOrderId),
    merchantRefundNo: toStringValue(record.merchantRefundNo),
    channelRefundNo: toOptionalString(record.channelRefundNo),
    idempotencyKey: toStringValue(record.idempotencyKey),
    refundAmount: toNumber(record.refundAmount, 0),
    status: toRefundOrderStatus(record.status),
    createTime: normalizeDateTime(record.createTime),
    updateTime: normalizeDateTime(record.updateTime),
  };
}

function fromPaymentOrderVO(item: unknown): PaymentOrderVO {
  const record = toRecord(item);
  return {
    paymentOrderId: normalizeId(record.paymentOrderId),
    bizOrderId: normalizeId(record.bizOrderId),
    channelCode: toStringValue(record.channelCode),
    amount: toNumber(record.amount, 0),
    status: toPaymentOrderStatus(record.status),
    materialType: toPaymentMaterialType(record.materialType),
    materialContent: toOptionalString(record.materialContent),
  };
}

function fromSandboxPaymentNotify(item: unknown): SandboxPaymentNotifyVO {
  const record = toRecord(item);
  const notifyCommand = toRecord(record.notifyCommand);
  return {
    channelCode: toStringValue(record.channelCode),
    paymentOrderId: normalizeId(record.paymentOrderId),
    channelOrderNo: toStringValue(record.channelOrderNo),
    notifyEventId: toStringValue(record.notifyEventId),
    signature: toStringValue(record.signature),
    notifyCommand: {
      paymentOrderId: normalizeId(notifyCommand.paymentOrderId),
      channelOrderNo: toStringValue(notifyCommand.channelOrderNo),
      notifyEventId: toStringValue(notifyCommand.notifyEventId),
      signature: toStringValue(notifyCommand.signature),
    },
  };
}

function fromManageDomain(item: unknown): PaymentManageDomainMeta {
  const record = toRecord(item);
  return {
    code: toPaymentManageDomain(record.code),
    title: toStringValue(record.title),
    description: toStringValue(record.description),
    badge: toStringValue(record.badge),
    sortOrder: toNumber(record.sortOrder, 0),
  };
}

function fromManageItem(item: unknown): PaymentManageItem {
  const record = toRecord(item);
  return {
    id: normalizeId(record.id),
    domain: toPaymentManageDomain(record.domain),
    code: toStringValue(record.code),
    name: toStringValue(record.name),
    owner: toStringValue(record.owner),
    status: toManageStatus(record.status),
    primaryText: toStringValue(record.primaryText),
    secondaryText: toStringValue(record.secondaryText),
    updatedAt: normalizeDateTime(record.updatedAt),
  };
}

function fromTenantCashier(item: unknown): PaymentTenantCashier {
  const record = toRecord(item);
  return {
    tenantId: normalizeId(record.tenantId),
    tenantName: toStringValue(record.tenantName),
    appCode: toStringValue(record.appCode),
    cashierCode: toStringValue(record.cashierCode),
    cashierName: toStringValue(record.cashierName),
    enabledMethods: Array.isArray(record.enabledMethods) ? record.enabledMethods.map(String) : [],
    defaultMethod: toStringValue(record.defaultMethod),
    expireMinutes: toNumber(record.expireMinutes, 30),
    dailyLimit: toNumber(record.dailyLimit, 0),
  };
}

function fromPaymentMethodOption(item: unknown): PaymentMethodOption {
  const record = toRecord(item);
  return {
    label: toStringValue(record.label),
    value: toStringValue(record.code),
    channelCode: toOptionalString(record.channelCode),
    status: toOptionalString(record.status),
    singleLimit: toNumber(record.singleLimit, 0),
  };
}

function normalizeId(value: unknown): string {
  return value === undefined || value === null ? '' : String(value);
}

function normalizeDateTime(value: unknown): string {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`;
  }
  return String(value).replace('T', ' ');
}

function toRecord(value: unknown): Record<string, unknown> {
  return value !== null && typeof value === 'object' ? value as Record<string, unknown> : {};
}

function toStringValue(value: unknown, fallback = ''): string {
  return value === undefined || value === null || value === '' ? fallback : String(value);
}

function toOptionalString(value: unknown): string | undefined {
  return value === undefined || value === null || value === '' ? undefined : String(value);
}

function toNumber(value: unknown, fallback: number): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function toPayBizOrderStatus(value: unknown): PayBizOrderStatus {
  const status = String(value || '');
  return payBizStatusOptions.some(item => item.value === status) ? status as PayBizOrderStatus : 'CREATED';
}

function toPaymentOrderStatus(value: unknown): PaymentOrderStatus {
  const status = String(value || '');
  return paymentStatusOptions.some(item => item.value === status) ? status as PaymentOrderStatus : 'CREATED';
}

function toRefundOrderStatus(value: unknown): RefundOrderStatus {
  const status = String(value || '');
  return refundStatusOptions.some(item => item.value === status) ? status as RefundOrderStatus : 'CREATED';
}

function toPaymentManageDomain(value: unknown): PaymentManageDomain {
  const domains: PaymentManageDomain[] = [
    'overview',
    'applications',
    'subjects',
    'channels',
    'methods',
    'cashiers',
    'orders',
    'refunds',
    'notifies',
    'reconcile',
    'settlement',
    'audit',
  ];
  const domain = String(value || '');
  return domains.includes(domain as PaymentManageDomain) ? domain as PaymentManageDomain : 'overview';
}

function toManageStatus(value: unknown): PaymentManageItem['status'] {
  const status = String(value || '');
  if (status === 'ENABLED' || status === 'DISABLED' || status === 'PENDING' || status === 'FAILED') {
    return status;
  }
  return 'DISABLED';
}

function toPaymentMaterialType(value: unknown): PaymentMaterialType | undefined {
  if (value === 'QRCODE' || value === 'REDIRECT_URL' || value === 'SANDBOX_TOKEN') {
    return value;
  }
  return undefined;
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}
