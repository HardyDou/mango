import { expect, test, type Page, type Route } from '@playwright/test';

interface PayBizOrderRecord {
  bizOrderId: string;
  merchantOrderNo: string;
  amount: number;
  refundedAmount: number;
  currency: string;
  status: 'CREATED' | 'PAYING' | 'PAID';
  appCode: string;
  subject: string;
}

interface PaymentOrderRecord {
  paymentOrderId: string;
  bizOrderId: string;
  channelCode: string;
  amount: number;
  status: 'PROCESSING' | 'SUCCESS';
  materialType: 'SANDBOX_TOKEN';
  materialContent: string;
  payMethod: string;
  channelOrderNo: string;
}

function ok(data: unknown) {
  return {
    code: 200,
    success: true,
    message: 'success',
    data,
  };
}

async function fulfill(route: Route, data: unknown) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(ok(data)),
  });
}

async function setupPaymentRoutes(page: Page) {
  const bizOrders = new Map<string, PayBizOrderRecord>();
  const paymentOrders = new Map<string, PaymentOrderRecord>();
  let bizSeq = 1000;
  let paymentSeq = 9000;

  const manageDomains = [
    { code: 'overview', title: '总览', description: '支付平台能力、租户收银台和沙箱链路', badge: '总览' },
    { code: 'applications', title: '应用管理', description: '接入应用、密钥、回调地址和权限', badge: '接入' },
    { code: 'subjects', title: '企业主体', description: '收款主体、证照、银行账户和结算归属', badge: '主体' },
    { code: 'channels', title: '支付通道', description: '沙箱、微信、支付宝、通联、连连通道配置', badge: '通道' },
    { code: 'methods', title: '支付方式', description: '扫码、收银台、网银、线下方式和限额', badge: '方式' },
    { code: 'cashiers', title: '收银台配置', description: '租户收银台样式、可用方式和过期时间', badge: '收银台' },
    { code: 'orders', title: '订单与流水', description: '业务单、支付单、交易流水和异常订单', badge: '订单' },
    { code: 'refunds', title: '退款管理', description: '退款申请、退款单、刷新和状态追踪', badge: '退款' },
    { code: 'notifies', title: '通知管理', description: '渠道回调、业务通知、重试和补偿', badge: '通知' },
    { code: 'reconcile', title: '对账管理', description: '渠道账单、自动核对和差异处理', badge: '对账' },
    { code: 'settlement', title: '结算汇总', description: '按日、租户、主体、通道汇总资金', badge: '结算' },
    { code: 'audit', title: '审计管理', description: '配置变更、人工补偿和资金操作留痕', badge: '审计' },
  ];
  const manageItems = [
    { id: '2001', domain: 'applications', code: 'mango-admin', name: '管理后台接入应用', owner: '芒果集团', status: 'ENABLED', primaryText: '回调白名单 2 个', secondaryText: '支付、退款、查单已开通', updatedAt: '2026-05-24 09:00:00' },
    { id: '2004', domain: 'channels', code: 'SANDBOX', name: '沙箱支付通道', owner: '平台', status: 'ENABLED', primaryText: '无外部网络依赖', secondaryText: '支持支付、退款、回调验签', updatedAt: '2026-05-24 09:30:00' },
    { id: '2009', domain: 'methods', code: 'SANDBOX_QR', name: '沙箱扫码', owner: '全部租户', status: 'ENABLED', primaryText: '单笔 50,000.00', secondaryText: '默认展示在收银台首位', updatedAt: '2026-05-24 09:20:00' },
    { id: '2012', domain: 'cashiers', code: 'CASHIER_STANDARD', name: '标准收银台', owner: '芒果集团', status: 'ENABLED', primaryText: '3 个支付方式', secondaryText: '订单 30 分钟过期', updatedAt: '2026-05-24 09:25:00' },
    { id: '2015', domain: 'orders', code: 'PAY_ORDER_FLOW', name: '支付订单状态机', owner: '平台', status: 'ENABLED', primaryText: '一单多支付尝试', secondaryText: '同一业务单仅允许一笔成功支付', updatedAt: '2026-05-24 09:28:00' },
    { id: '2016', domain: 'refunds', code: 'REFUND_FLOW', name: '退款状态机', owner: '平台', status: 'ENABLED', primaryText: '商户退款号幂等', secondaryText: '累计退款金额不超过已支付金额', updatedAt: '2026-05-24 09:32:00' },
  ];
  const tenantCashiers = [
    { tenantId: '1', tenantName: '芒果集团', appCode: 'mango-admin', cashierCode: 'CASHIER_STANDARD', cashierName: '标准收银台', enabledMethods: ['SANDBOX_QR', 'SANDBOX_CASHIER', 'SANDBOX_BANK'], defaultMethod: 'SANDBOX_QR', expireMinutes: 30, dailyLimit: 5_000_000 },
    { tenantId: '2', tenantName: '华南事业部', appCode: 'mango-south', cashierCode: 'CASHIER_BRANCH', cashierName: '分支机构收银台', enabledMethods: ['SANDBOX_QR', 'SANDBOX_CASHIER'], defaultMethod: 'SANDBOX_CASHIER', expireMinutes: 20, dailyLimit: 2_000_000 },
  ];
  const sandboxMethods = [
    { code: 'SANDBOX_QR', label: '沙箱扫码', channelCode: 'SANDBOX', status: 'ENABLED', singleLimit: 5_000_000 },
    { code: 'SANDBOX_CASHIER', label: '沙箱收银台', channelCode: 'SANDBOX', status: 'ENABLED', singleLimit: 5_000_000 },
    { code: 'SANDBOX_BANK', label: '沙箱网银', channelCode: 'SANDBOX', status: 'ENABLED', singleLimit: 10_000_000 },
  ];

  await page.route('**/api/payment/management/domains', route => fulfill(route, manageDomains));
  await page.route('**/api/payment/management/items**', async (route) => {
    const url = new URL(route.request().url());
    const domain = url.searchParams.get('domain');
    await fulfill(route, manageItems.filter(item => item.domain === domain));
  });
  await page.route('**/api/payment/management/tenant-cashiers', route => fulfill(route, tenantCashiers));
  await page.route('**/api/payment/management/sandbox-methods', route => fulfill(route, sandboxMethods));

  await page.route('**/api/payment/biz-orders', async (route) => {
    const body = route.request().postDataJSON() as {
      appCode: string;
      merchantOrderNo: string;
      subject: string;
      amount: number;
      currency?: string;
    };
    const bizOrderId = String(++bizSeq);
    bizOrders.set(bizOrderId, {
      bizOrderId,
      merchantOrderNo: body.merchantOrderNo,
      amount: body.amount,
      refundedAmount: 0,
      currency: body.currency || 'CNY',
      status: 'CREATED',
      appCode: body.appCode,
      subject: body.subject,
    });
    await fulfill(route, bizOrderId);
  });

  await page.route('**/api/payment/payments', async (route) => {
    const body = route.request().postDataJSON() as { bizOrderId: string; payMethod: string };
    const bizOrder = bizOrders.get(body.bizOrderId);
    if (bizOrder) {
      bizOrder.status = 'PAYING';
    }
    const paymentOrderId = String(++paymentSeq);
    const paymentOrder: PaymentOrderRecord = {
      paymentOrderId,
      bizOrderId: body.bizOrderId,
      channelCode: 'SANDBOX',
      amount: bizOrder?.amount || 100,
      status: 'PROCESSING',
      materialType: 'SANDBOX_TOKEN',
      materialContent: `sandbox://pay/${paymentOrderId}`,
      payMethod: body.payMethod,
      channelOrderNo: `SANDBOX-PAY-${paymentOrderId}`,
    };
    paymentOrders.set(paymentOrderId, paymentOrder);
    await fulfill(route, paymentOrder);
  });

  await page.route('**/api/payment/sandbox/payment-notifies', async (route) => {
    const body = route.request().postDataJSON() as { paymentOrderId: string; sandboxEventId: string };
    const paymentOrder = paymentOrders.get(body.paymentOrderId);
    await fulfill(route, {
      channelCode: 'SANDBOX',
      paymentOrderId: body.paymentOrderId,
      channelOrderNo: paymentOrder?.channelOrderNo || `SANDBOX-PAY-${body.paymentOrderId}`,
      notifyEventId: body.sandboxEventId,
      signature: `SANDBOX:${body.paymentOrderId}`,
      notifyCommand: {
        paymentOrderId: body.paymentOrderId,
        channelOrderNo: paymentOrder?.channelOrderNo || `SANDBOX-PAY-${body.paymentOrderId}`,
        notifyEventId: body.sandboxEventId,
        signature: `SANDBOX:${body.paymentOrderId}`,
      },
    });
  });

  await page.route('**/api/payment/sandbox/payments/complete', async (route) => {
    const body = route.request().postDataJSON() as { paymentOrderId: string };
    const paymentOrder = paymentOrders.get(body.paymentOrderId);
    if (paymentOrder) {
      paymentOrder.status = 'SUCCESS';
      const bizOrder = bizOrders.get(paymentOrder.bizOrderId);
      if (bizOrder) {
        bizOrder.status = 'PAID';
      }
      await fulfill(route, paymentOrder);
      return;
    }
    await fulfill(route, {
      paymentOrderId: body.paymentOrderId,
      bizOrderId: '',
      channelCode: 'SANDBOX',
      amount: 100,
      status: 'SUCCESS',
      materialType: 'SANDBOX_TOKEN',
      materialContent: `sandbox://pay/${body.paymentOrderId}`,
    });
  });

  await page.route('**/api/payment/biz-orders/query', async (route) => {
    const body = route.request().postDataJSON() as { bizOrderId: string };
    await fulfill(route, bizOrders.get(body.bizOrderId));
  });

  await page.route('**/api/payment/biz-orders/page**', route => fulfill(route, {
    list: Array.from(bizOrders.values()).map(item => ({
      id: item.bizOrderId,
      ...item,
    })),
    total: bizOrders.size,
    page: 1,
    size: 10,
  }));
  await page.route('**/api/payment/orders/page**', route => fulfill(route, {
    list: Array.from(paymentOrders.values()).map(item => ({
      id: item.paymentOrderId,
      ...item,
    })),
    total: paymentOrders.size,
    page: 1,
    size: 10,
  }));
  await page.route('**/api/payment/refund-orders/page**', route => fulfill(route, {
    list: [],
    total: 0,
    page: 1,
    size: 10,
  }));
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  await page.getByPlaceholder('密码').blur();
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('支付中心 E2E', () => {
  test('支付中心菜单可进入管理页和租户收银台，并完成沙箱支付流程', async ({ page }) => {
    await setupPaymentRoutes(page);
    const userMenuResponse = page.waitForResponse(response =>
      response.url().includes('/api/authorization/menus/user') && response.ok()
    );
    await login(page);
    const menus = await userMenuResponse.then(response => response.json());
    expect(JSON.stringify(menus)).toContain('支付中心');

    await expect(page.getByText('支付中心', { exact: true })).toBeVisible({ timeout: 10000 });
    await page.getByText('支付中心', { exact: true }).click();
    await page.getByText('支付管理', { exact: true }).click();
    await expect(page.getByRole('heading', { name: '支付管理' })).toBeVisible();
    await expect(page.getByText('应用管理', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('收银台配置', { exact: true }).first()).toBeVisible();

    await page.getByRole('menubar').getByText('租户收银台', { exact: true }).click();
    await expect(page.getByRole('heading', { name: '租户收银台' })).toBeVisible();
    await expect(page.getByRole('heading', { name: '标准收银台' })).toBeVisible();

    const orderNo = `SOE2E${Date.now()}`;
    await page.getByLabel('商户单号').fill(orderNo);
    await page.getByLabel('订单标题').fill('E2E 沙箱订单');
    await page.getByRole('button', { name: '创建订单' }).click();
    await expect(page.locator('.el-message__content', { hasText: '收银台订单已创建' }).last()).toBeVisible({ timeout: 10000 });

    await page.getByRole('button', { name: '发起支付' }).click();
    await expect(page.locator('.el-message__content', { hasText: '沙箱支付已发起' }).last()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('SANDBOX', { exact: true }).first()).toBeVisible();
    await expect(page.getByText(/sandbox:\/\/pay\//).first()).toBeVisible();

    await page.getByRole('button', { name: '沙箱付款成功' }).click();
    await expect(page.locator('.el-message__content', { hasText: '沙箱支付流程已完成' }).last()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('支付完成')).toBeVisible();
    await expect(page.getByText('PAID')).toBeVisible();
  });
});
