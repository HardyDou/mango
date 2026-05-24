<template>
  <div class="payment-admin">
    <aside class="payment-nav">
      <div class="nav-head">
        <h2>支付管理</h2>
        <el-tag type="success" effect="plain">SANDBOX</el-tag>
      </div>

      <el-scrollbar class="nav-scroll">
        <button
          v-for="domain in paymentManageDomains"
          :key="domain.code"
          class="domain-button"
          :class="{ active: activeDomain === domain.code }"
          type="button"
          @click="activeDomain = domain.code"
        >
          <span class="domain-badge">{{ domain.badge }}</span>
          <span class="domain-copy">
            <strong>{{ domain.title }}</strong>
            <small>{{ domain.description }}</small>
          </span>
        </button>
      </el-scrollbar>
    </aside>

    <main class="payment-main">
      <section v-loading="managementLoading" class="summary-strip">
        <div class="summary-item">
          <span>租户收银台</span>
          <strong>{{ tenantCashiers.length }}</strong>
        </div>
        <div class="summary-item">
          <span>已启用配置</span>
          <strong>{{ enabledManageCount }}</strong>
        </div>
        <div class="summary-item">
          <span>沙箱通道</span>
          <strong>SANDBOX</strong>
        </div>
        <div class="summary-item">
          <span>管理域</span>
          <strong>{{ paymentManageDomains.length }}</strong>
        </div>
      </section>

      <section v-if="activeDomain === 'overview'" class="workspace-grid">
        <div class="panel wide-panel">
          <div class="panel-head">
            <div>
              <h3>平台能力总览</h3>
              <p>设计文档要求的后台管理域与当前沙箱闭环</p>
            </div>
            <el-button type="primary" @click="activeDomain = 'orders'">进入订单管理</el-button>
          </div>

          <div class="capability-grid">
            <div v-for="domain in paymentManageDomains.filter(item => item.code !== 'overview')" :key="domain.code" class="capability-cell">
              <span>{{ domain.badge }}</span>
              <strong>{{ domain.title }}</strong>
              <small>{{ domain.description }}</small>
            </div>
          </div>
        </div>

        <div class="panel cashier-panel">
          <div class="panel-head compact-head">
            <div>
              <h3>租户收银台</h3>
              <p>每个租户可进入独立沙箱支付流程</p>
            </div>
          </div>
          <el-table :data="tenantCashiers" height="300" class="compact-table">
            <el-table-column label="租户" min-width="150">
              <template #default="{ row }">
                <div class="name-cell">
                  <strong>{{ row.tenantName }}</strong>
                  <span>{{ row.appCode }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="cashierName" label="收银台" min-width="140" show-overflow-tooltip />
            <el-table-column label="方式" width="70">
              <template #default="{ row }">{{ row.enabledMethods.length }}</template>
            </el-table-column>
          </el-table>
        </div>

        <PaymentSandbox class="sandbox-panel" @created="loadBizOrders" @paid="handleSandboxPaid" />
      </section>

      <section v-else-if="isConfigDomain" class="panel">
        <div class="panel-head">
          <div>
            <h3>{{ currentDomain?.title }}</h3>
            <p>{{ currentDomain?.description }}</p>
          </div>
          <el-button :icon="Plus" type="primary">新增配置</el-button>
        </div>

        <el-table :data="domainItems" stripe>
          <el-table-column label="配置" min-width="220">
            <template #default="{ row }">
              <div class="name-cell">
                <strong>{{ row.name }}</strong>
                <span>{{ row.code }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="owner" label="归属" width="140" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="manageStatusType(row.status)" size="small">
                {{ manageStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="primaryText" label="关键配置" min-width="180" />
          <el-table-column prop="secondaryText" label="说明" min-width="220" show-overflow-tooltip />
          <el-table-column prop="updatedAt" label="更新时间" width="170" />
          <el-table-column label="操作" width="170" fixed="right">
            <template #default>
              <el-button link type="primary" size="small">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeDomain === 'orders'" class="panel">
        <div class="panel-head">
          <div>
            <h3>订单与流水</h3>
            <p>业务订单、支付订单、沙箱支付材料和状态刷新</p>
          </div>
          <el-button v-auth="'payment:biz-order:add'" type="primary" @click="openCreateBizDialog">创建业务支付单</el-button>
        </div>

        <el-tabs v-model="orderTab" @tab-change="handleOrderTabChange">
          <el-tab-pane label="业务支付单" name="biz">
            <el-form :inline="true" :model="bizQuery" class="search-form">
              <el-form-item label="应用编码">
                <el-input v-model="bizQuery.appCode" clearable />
              </el-form-item>
              <el-form-item label="商户单号">
                <el-input v-model="bizQuery.merchantOrderNo" clearable />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="bizQuery.status" clearable placeholder="全部" style="width: 140px">
                  <el-option v-for="item in payBizStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button v-auth="'payment:biz-order:list'" type="primary" @click="searchBizOrders">查询</el-button>
                <el-button @click="resetBizOrders">重置</el-button>
              </el-form-item>
            </el-form>

            <el-table v-loading="bizLoading" :data="bizRows" stripe>
              <el-table-column prop="id" label="业务单 ID" min-width="170" show-overflow-tooltip />
              <el-table-column prop="appCode" label="应用编码" width="130" show-overflow-tooltip />
              <el-table-column prop="merchantOrderNo" label="商户单号" min-width="180" show-overflow-tooltip />
              <el-table-column prop="subject" label="订单标题" min-width="180" show-overflow-tooltip />
              <el-table-column label="金额" width="120">
                <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
              </el-table-column>
              <el-table-column label="已退款" width="120">
                <template #default="{ row }">{{ formatAmount(row.refundedAmount || 0) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="bizStatusType(row.status)" size="small">
                    {{ statusLabel(row.status, payBizStatusOptions) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
              <el-table-column label="操作" width="230" fixed="right">
                <template #default="{ row }">
                  <el-button v-auth="'payment:biz-order:query'" link type="primary" size="small" @click="openBizDetail(row)">详情</el-button>
                  <el-button v-auth="'payment:payment:add'" link type="primary" size="small" @click="openPayDialog(row)">发起支付</el-button>
                  <el-button v-auth="'payment:biz-order:close'" link type="danger" size="small" @click="closeBizOrder(row)">关闭</el-button>
                </template>
              </el-table-column>
            </el-table>

            <Pagination
              v-model:current-page="bizQuery.pageNum"
              v-model:page-size="bizQuery.pageSize"
              :total="bizTotal"
              @change="loadBizOrders"
            />
          </el-tab-pane>

          <el-tab-pane label="支付单" name="payment">
            <el-form :inline="true" :model="paymentQuery" class="search-form">
              <el-form-item label="业务单 ID">
                <el-input v-model="paymentQuery.bizOrderId" clearable />
              </el-form-item>
              <el-form-item label="渠道">
                <el-input v-model="paymentQuery.channelCode" clearable />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="paymentQuery.status" clearable placeholder="全部" style="width: 140px">
                  <el-option v-for="item in paymentStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button v-auth="'payment:payment:list'" type="primary" @click="searchPayments">查询</el-button>
                <el-button @click="resetPayments">重置</el-button>
              </el-form-item>
            </el-form>

            <el-table v-loading="paymentLoading" :data="paymentRows" stripe>
              <el-table-column prop="id" label="支付单 ID" min-width="170" show-overflow-tooltip />
              <el-table-column prop="bizOrderId" label="业务单 ID" min-width="170" show-overflow-tooltip />
              <el-table-column prop="channelCode" label="渠道" width="130" />
              <el-table-column prop="payMethod" label="支付方式" width="150" />
              <el-table-column label="金额" width="120">
                <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="paymentStatusType(row.status)" size="small">
                    {{ statusLabel(row.status, paymentStatusOptions) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
              <el-table-column label="操作" width="190" fixed="right">
                <template #default="{ row }">
                  <el-button v-auth="'payment:payment:query'" link type="primary" size="small" @click="openPaymentDetail(row)">详情</el-button>
                  <el-button v-auth="'payment:payment:refresh'" link type="primary" size="small" @click="refreshPayment(row)">刷新</el-button>
                </template>
              </el-table-column>
            </el-table>

            <Pagination
              v-model:current-page="paymentQuery.pageNum"
              v-model:page-size="paymentQuery.pageSize"
              :total="paymentTotal"
              @change="loadPayments"
            />
          </el-tab-pane>
        </el-tabs>
      </section>

      <section v-else-if="activeDomain === 'refunds'" class="panel">
        <div class="panel-head">
          <div>
            <h3>退款管理</h3>
            <p>退款申请、幂等退款号、状态刷新和退款单追踪</p>
          </div>
          <el-button v-auth="'payment:refund:add'" type="primary" @click="openRefundDialog()">发起退款</el-button>
        </div>

        <el-form :inline="true" :model="refundQuery" class="search-form">
          <el-form-item label="业务单 ID">
            <el-input v-model="refundQuery.bizOrderId" clearable />
          </el-form-item>
          <el-form-item label="商户退款号">
            <el-input v-model="refundQuery.merchantRefundNo" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="refundQuery.status" clearable placeholder="全部" style="width: 140px">
              <el-option v-for="item in refundStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button v-auth="'payment:refund:list'" type="primary" @click="searchRefunds">查询</el-button>
            <el-button @click="resetRefunds">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="refundLoading" :data="refundRows" stripe>
          <el-table-column prop="id" label="退款单 ID" min-width="170" show-overflow-tooltip />
          <el-table-column prop="bizOrderId" label="业务单 ID" min-width="170" show-overflow-tooltip />
          <el-table-column prop="paymentOrderId" label="支付单 ID" min-width="170" show-overflow-tooltip />
          <el-table-column prop="merchantRefundNo" label="商户退款号" min-width="180" show-overflow-tooltip />
          <el-table-column label="退款金额" width="120">
            <template #default="{ row }">{{ formatAmount(row.refundAmount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="refundStatusType(row.status)" size="small">
                {{ statusLabel(row.status, refundStatusOptions) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button v-auth="'payment:refund:query'" link type="primary" size="small" @click="openRefundDetail(row)">详情</el-button>
              <el-button v-auth="'payment:refund:refresh'" link type="primary" size="small" @click="refreshRefund(row)">刷新</el-button>
            </template>
          </el-table-column>
        </el-table>

        <Pagination
          v-model:current-page="refundQuery.pageNum"
          v-model:page-size="refundQuery.pageSize"
          :total="refundTotal"
          @change="loadRefunds"
        />
      </section>

      <section v-else class="panel">
        <div class="panel-head">
          <div>
            <h3>{{ currentDomain?.title }}</h3>
            <p>{{ currentDomain?.description }}</p>
          </div>
        </div>
        <el-table :data="domainItems" stripe>
          <el-table-column label="记录" min-width="220">
            <template #default="{ row }">
              <div class="name-cell">
                <strong>{{ row.name }}</strong>
                <span>{{ row.code }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="owner" label="归属" width="140" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="manageStatusType(row.status)" size="small">
                {{ manageStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="primaryText" label="关键数据" min-width="180" />
          <el-table-column prop="secondaryText" label="说明" min-width="220" show-overflow-tooltip />
          <el-table-column prop="updatedAt" label="更新时间" width="170" />
        </el-table>
      </section>
    </main>

    <el-dialog v-model="createBizVisible" title="创建业务支付单" width="620px" destroy-on-close>
      <el-form ref="createBizFormRef" :model="createBizForm" :rules="createBizRules" label-width="112px">
        <el-form-item label="应用编码" prop="appCode">
          <el-input v-model="createBizForm.appCode" />
        </el-form-item>
        <el-form-item label="商户单号" prop="merchantOrderNo">
          <el-input v-model="createBizForm.merchantOrderNo" />
        </el-form-item>
        <el-form-item label="订单标题" prop="subject">
          <el-input v-model="createBizForm.subject" />
        </el-form-item>
        <el-form-item label="金额(分)" prop="amount">
          <el-input-number v-model="createBizForm.amount" :min="1" />
        </el-form-item>
        <el-form-item label="币种">
          <el-input v-model="createBizForm.currency" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createBizVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingBiz" @click="createBizOrder">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payVisible" title="发起沙箱支付" width="620px" destroy-on-close>
      <el-form ref="payFormRef" :model="payForm" :rules="payRules" label-width="112px">
        <el-form-item label="业务单 ID" prop="bizOrderId">
          <el-input v-model="payForm.bizOrderId" readonly />
        </el-form-item>
        <el-form-item label="支付方式" prop="payMethod">
          <el-select v-model="payForm.payMethod" style="width: 100%">
            <el-option v-for="item in sandboxPayMethods" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="幂等键" prop="idempotencyKey">
          <el-input v-model="payForm.idempotencyKey" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="payOrder">发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundVisible" title="发起退款" width="620px" destroy-on-close>
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="112px">
        <el-form-item label="业务单 ID" prop="bizOrderId">
          <el-input v-model="refundForm.bizOrderId" />
        </el-form-item>
        <el-form-item label="商户退款号" prop="merchantRefundNo">
          <el-input v-model="refundForm.merchantRefundNo" />
        </el-form-item>
        <el-form-item label="退款金额(分)" prop="refundAmount">
          <el-input-number v-model="refundForm.refundAmount" :min="1" />
        </el-form-item>
        <el-form-item label="幂等键" prop="idempotencyKey">
          <el-input v-model="refundForm.idempotencyKey" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="refunding" @click="createRefund">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="detailTitle" size="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item v-for="item in detailItems" :key="item.label" :label="item.label">
          {{ item.value || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-input
        v-if="detailMaterial"
        class="material-content"
        :model-value="detailMaterial"
        type="textarea"
        :rows="4"
        readonly
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import Pagination from '@mango/common/components/Pagination/index.vue';
import PaymentSandbox from '../../components/PaymentSandbox.vue';
import {
  paymentApi,
  paymentStatusOptions,
  payBizStatusOptions,
  refundStatusOptions,
  statusLabel,
  type PayBizOrder,
  type PayBizOrderStatus,
  type PaymentManageDomain,
  type PaymentManageDomainMeta,
  type PaymentManageItem,
  type PaymentMethodOption,
  type PaymentOrder,
  type PaymentOrderStatus,
  type PaymentTenantCashier,
  type RefundOrder,
  type RefundOrderStatus,
} from '../../api/payment';

type OrderTab = 'biz' | 'payment';
type ManageStatus = 'ENABLED' | 'DISABLED' | 'PENDING' | 'FAILED';

const configDomains: PaymentManageDomain[] = ['applications', 'subjects', 'channels', 'methods', 'cashiers'];
const activeDomain = ref<PaymentManageDomain>('overview');
const orderTab = ref<OrderTab>('biz');
const paymentManageDomains = ref<PaymentManageDomainMeta[]>([]);
const paymentManageItems = ref<PaymentManageItem[]>([]);
const tenantCashiers = ref<PaymentTenantCashier[]>([]);
const sandboxPayMethods = ref<PaymentMethodOption[]>([]);
const bizRows = ref<PayBizOrder[]>([]);
const paymentRows = ref<PaymentOrder[]>([]);
const refundRows = ref<RefundOrder[]>([]);
const bizTotal = ref(0);
const paymentTotal = ref(0);
const refundTotal = ref(0);
const bizLoading = ref(false);
const paymentLoading = ref(false);
const refundLoading = ref(false);
const managementLoading = ref(false);
const createBizVisible = ref(false);
const payVisible = ref(false);
const refundVisible = ref(false);
const detailVisible = ref(false);
const creatingBiz = ref(false);
const paying = ref(false);
const refunding = ref(false);
const createBizFormRef = ref<FormInstance>();
const payFormRef = ref<FormInstance>();
const refundFormRef = ref<FormInstance>();
const detailTitle = ref('');
const detailMaterial = ref('');
const detailItems = ref<Array<{ label: string; value: string }>>([]);

const bizQuery = reactive<{ pageNum: number; pageSize: number; appCode: string; merchantOrderNo: string; status: PayBizOrderStatus | '' }>({
  pageNum: 1,
  pageSize: 10,
  appCode: '',
  merchantOrderNo: '',
  status: '',
});
const paymentQuery = reactive<{ pageNum: number; pageSize: number; bizOrderId: string; channelCode: string; status: PaymentOrderStatus | '' }>({
  pageNum: 1,
  pageSize: 10,
  bizOrderId: '',
  channelCode: '',
  status: '',
});
const refundQuery = reactive<{ pageNum: number; pageSize: number; bizOrderId: string; merchantRefundNo: string; status: RefundOrderStatus | '' }>({
  pageNum: 1,
  pageSize: 10,
  bizOrderId: '',
  merchantRefundNo: '',
  status: '',
});

const createBizForm = reactive({
  appCode: 'mango-admin',
  merchantOrderNo: nextNo('SO'),
  subject: '后台管理支付单',
  amount: 100,
  currency: 'CNY',
});
const payForm = reactive({ bizOrderId: '', payMethod: 'SANDBOX_QR', idempotencyKey: nextNo('PAY') });
const refundForm = reactive({ bizOrderId: '', merchantRefundNo: nextNo('RF'), refundAmount: 100, idempotencyKey: nextNo('REF') });

const createBizRules: FormRules = {
  appCode: [{ required: true, message: '请输入应用编码', trigger: 'blur' }],
  merchantOrderNo: [{ required: true, message: '请输入商户单号', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入订单标题', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'change' }],
};
const payRules: FormRules = {
  bizOrderId: [{ required: true, message: '请输入业务单 ID', trigger: 'blur' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
  idempotencyKey: [{ required: true, message: '请输入幂等键', trigger: 'blur' }],
};
const refundRules: FormRules = {
  bizOrderId: [{ required: true, message: '请输入业务单 ID', trigger: 'blur' }],
  merchantRefundNo: [{ required: true, message: '请输入商户退款号', trigger: 'blur' }],
  refundAmount: [{ required: true, message: '请输入退款金额', trigger: 'change' }],
  idempotencyKey: [{ required: true, message: '请输入幂等键', trigger: 'blur' }],
};

const currentDomain = computed(() => paymentManageDomains.value.find(item => item.code === activeDomain.value));
const domainItems = computed(() => paymentManageItems.value.filter(item => item.domain === activeDomain.value));
const isConfigDomain = computed(() => configDomains.includes(activeDomain.value));
const enabledManageCount = computed(() => paymentManageItems.value.filter(item => item.status === 'ENABLED').length);

onMounted(() => {
  loadManagementData();
  loadBizOrders();
});

watch(activeDomain, domain => {
  if (domain === 'orders') {
    handleOrderTabChange();
  }
  if (domain === 'refunds') {
    loadRefunds();
  }
  if (!domainItems.value.length && domain !== 'overview') {
    loadManageItems(domain);
  }
});

async function loadManagementData() {
  managementLoading.value = true;
  try {
    const [domains, cashiers, methods] = await Promise.all([
      paymentApi.listManageDomains(),
      paymentApi.listTenantCashiers(),
      paymentApi.listSandboxMethods(),
    ]);
    paymentManageDomains.value = domains;
    tenantCashiers.value = cashiers;
    sandboxPayMethods.value = methods;
    await Promise.all(domains.filter(item => item.code !== 'overview').map(item => loadManageItems(item.code)));
  } finally {
    managementLoading.value = false;
  }
}

async function loadManageItems(domain: PaymentManageDomain) {
  if (domain === 'overview') {
    return;
  }
  const items = await paymentApi.listManageItems(domain);
  paymentManageItems.value = [
    ...paymentManageItems.value.filter(item => item.domain !== domain),
    ...items,
  ];
}

function handleOrderTabChange() {
  if (orderTab.value === 'biz') loadBizOrders();
  if (orderTab.value === 'payment') loadPayments();
}

function handleSandboxPaid() {
  paymentQuery.channelCode = 'SANDBOX';
  loadPayments();
}

function searchBizOrders() {
  bizQuery.pageNum = 1;
  loadBizOrders();
}

function resetBizOrders() {
  Object.assign(bizQuery, { pageNum: 1, pageSize: 10, appCode: '', merchantOrderNo: '', status: '' });
  loadBizOrders();
}

async function loadBizOrders() {
  bizLoading.value = true;
  try {
    const data = await paymentApi.pageBizOrders(bizQuery);
    bizRows.value = data.list;
    bizTotal.value = data.total;
  } finally {
    bizLoading.value = false;
  }
}

function openCreateBizDialog() {
  createBizForm.merchantOrderNo = nextNo('SO');
  createBizVisible.value = true;
}

async function createBizOrder() {
  await createBizFormRef.value?.validate();
  creatingBiz.value = true;
  try {
    await paymentApi.createBizOrder(createBizForm);
    ElMessage.success('业务支付单已创建');
    createBizVisible.value = false;
    loadBizOrders();
  } finally {
    creatingBiz.value = false;
  }
}

function openPayDialog(row: PayBizOrder) {
  payForm.bizOrderId = String(row.id);
  payForm.payMethod = 'SANDBOX_QR';
  payForm.idempotencyKey = nextNo('PAY');
  payVisible.value = true;
}

async function payOrder() {
  await payFormRef.value?.validate();
  paying.value = true;
  try {
    await paymentApi.pay(payForm);
    ElMessage.success('沙箱支付单已创建');
    payVisible.value = false;
    orderTab.value = 'payment';
    paymentQuery.bizOrderId = payForm.bizOrderId;
    activeDomain.value = 'orders';
    loadPayments();
  } finally {
    paying.value = false;
  }
}

async function closeBizOrder(row: PayBizOrder) {
  await ElMessageBox.confirm(`确认关闭业务支付单「${row.id}」？`, '关闭确认', { type: 'warning' });
  await paymentApi.closeBizOrder({ bizOrderId: row.id! });
  ElMessage.success('业务支付单已关闭');
  loadBizOrders();
}

async function openBizDetail(row: PayBizOrder) {
  const detail = await paymentApi.detailBizOrder(row.id!);
  detailTitle.value = '业务支付单详情';
  detailMaterial.value = '';
  detailItems.value = [
    { label: '业务单 ID', value: String(detail.id) },
    { label: '应用编码', value: detail.appCode },
    { label: '商户单号', value: detail.merchantOrderNo },
    { label: '订单标题', value: detail.subject },
    { label: '金额', value: formatAmount(detail.amount) },
    { label: '已退款', value: formatAmount(detail.refundedAmount || 0) },
    { label: '状态', value: statusLabel(detail.status, payBizStatusOptions) },
    { label: '创建时间', value: detail.createTime || '' },
  ];
  detailVisible.value = true;
}

function searchPayments() {
  paymentQuery.pageNum = 1;
  loadPayments();
}

function resetPayments() {
  Object.assign(paymentQuery, { pageNum: 1, pageSize: 10, bizOrderId: '', channelCode: '', status: '' });
  loadPayments();
}

async function loadPayments() {
  paymentLoading.value = true;
  try {
    const data = await paymentApi.pagePaymentOrders(paymentQuery);
    paymentRows.value = data.list;
    paymentTotal.value = data.total;
  } finally {
    paymentLoading.value = false;
  }
}

async function openPaymentDetail(row: PaymentOrder) {
  const detail = await paymentApi.detailPaymentOrder(row.id!);
  detailTitle.value = '支付单详情';
  detailMaterial.value = detail.materialContent || '';
  detailItems.value = [
    { label: '支付单 ID', value: String(detail.id) },
    { label: '业务单 ID', value: String(detail.bizOrderId) },
    { label: '渠道', value: detail.channelCode },
    { label: '渠道单号', value: detail.channelOrderNo || '' },
    { label: '支付方式', value: detail.payMethod },
    { label: '金额', value: formatAmount(detail.amount) },
    { label: '状态', value: statusLabel(detail.status, paymentStatusOptions) },
    { label: '材料类型', value: detail.materialType || '' },
    { label: '创建时间', value: detail.createTime || '' },
  ];
  detailVisible.value = true;
}

async function refreshPayment(row: PaymentOrder) {
  await paymentApi.refreshPaymentStatus({ paymentOrderId: row.id! });
  ElMessage.success('支付状态已刷新');
  loadPayments();
}

function searchRefunds() {
  refundQuery.pageNum = 1;
  loadRefunds();
}

function resetRefunds() {
  Object.assign(refundQuery, { pageNum: 1, pageSize: 10, bizOrderId: '', merchantRefundNo: '', status: '' });
  loadRefunds();
}

async function loadRefunds() {
  refundLoading.value = true;
  try {
    const data = await paymentApi.pageRefundOrders(refundQuery);
    refundRows.value = data.list;
    refundTotal.value = data.total;
  } finally {
    refundLoading.value = false;
  }
}

function openRefundDialog(row?: PayBizOrder) {
  refundForm.bizOrderId = row?.id ? String(row.id) : refundQuery.bizOrderId || '';
  refundForm.merchantRefundNo = nextNo('RF');
  refundForm.idempotencyKey = nextNo('REF');
  refundVisible.value = true;
}

async function createRefund() {
  await refundFormRef.value?.validate();
  refunding.value = true;
  try {
    await paymentApi.refund(refundForm);
    ElMessage.success('退款单已创建');
    refundVisible.value = false;
    activeDomain.value = 'refunds';
    refundQuery.bizOrderId = refundForm.bizOrderId;
    loadRefunds();
  } finally {
    refunding.value = false;
  }
}

async function openRefundDetail(row: RefundOrder) {
  const detail = await paymentApi.detailRefundOrder(row.id!);
  detailTitle.value = '退款单详情';
  detailMaterial.value = '';
  detailItems.value = [
    { label: '退款单 ID', value: String(detail.id) },
    { label: '业务单 ID', value: String(detail.bizOrderId) },
    { label: '支付单 ID', value: String(detail.paymentOrderId) },
    { label: '商户退款号', value: detail.merchantRefundNo },
    { label: '渠道退款号', value: detail.channelRefundNo || '' },
    { label: '退款金额', value: formatAmount(detail.refundAmount) },
    { label: '状态', value: statusLabel(detail.status, refundStatusOptions) },
    { label: '创建时间', value: detail.createTime || '' },
  ];
  detailVisible.value = true;
}

async function refreshRefund(row: RefundOrder) {
  await paymentApi.refreshRefundStatus({ refundOrderId: row.id! });
  ElMessage.success('退款状态已刷新');
  loadRefunds();
}

function formatAmount(value?: number) {
  return (Number(value || 0) / 100).toFixed(2);
}

function nextNo(prefix: string) {
  return `${prefix}${Date.now()}${Math.floor(Math.random() * 1000)}`;
}

function bizStatusType(status?: string) {
  if (status === 'PAID' || status === 'PARTIAL_REFUNDED' || status === 'REFUNDED') return 'success';
  if (status === 'FAILED' || status === 'CLOSED') return 'danger';
  if (status === 'PAYING') return 'warning';
  return 'info';
}

function paymentStatusType(status?: string) {
  if (status === 'SUCCESS') return 'success';
  if (status === 'FAILED' || status === 'CLOSED' || status === 'EXPIRED') return 'danger';
  if (status === 'PROCESSING') return 'warning';
  return 'info';
}

function refundStatusType(status?: string) {
  if (status === 'SUCCESS') return 'success';
  if (status === 'FAILED' || status === 'CLOSED') return 'danger';
  if (status === 'PROCESSING') return 'warning';
  return 'info';
}

function manageStatusType(status: ManageStatus) {
  if (status === 'ENABLED') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'PENDING') return 'warning';
  return 'info';
}

function manageStatusLabel(status: ManageStatus) {
  const labels: Record<ManageStatus, string> = {
    ENABLED: '启用',
    DISABLED: '停用',
    PENDING: '待处理',
    FAILED: '异常',
  };
  return labels[status];
}
</script>

<style scoped lang="scss">
.payment-admin {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  min-height: 100%;
  padding: 16px;
  background: var(--el-bg-color-page);
}

.payment-nav,
.panel,
.summary-strip {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.payment-nav {
  min-height: calc(100vh - 120px);
  overflow: hidden;
}

.nav-head,
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.nav-head h2,
.panel-head h3 {
  margin: 0;
  color: var(--el-text-color-primary);
}

.panel-head p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.compact-head {
  padding-bottom: 12px;
}

.nav-scroll {
  height: calc(100vh - 184px);
}

.domain-button {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 12px;
  width: calc(100% - 20px);
  min-height: 66px;
  margin: 10px;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.domain-button:hover,
.domain-button.active {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
}

.domain-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 34px;
  border-radius: 6px;
  background: var(--el-bg-color-page);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
}

.domain-copy {
  min-width: 0;
}

.domain-copy strong,
.name-cell strong {
  display: block;
  overflow: hidden;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.domain-copy small,
.name-cell span {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.payment-main {
  min-width: 0;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 16px;
}

.summary-item {
  padding: 14px 16px;
  border-right: 1px solid var(--el-border-color-lighter);
}

.summary-item:last-child {
  border-right: 0;
}

.summary-item span {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.summary-item strong {
  display: block;
  margin-top: 6px;
  color: var(--el-text-color-primary);
  font-size: 20px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(360px, 0.8fr);
  gap: 16px;
}

.wide-panel,
.sandbox-panel {
  grid-column: span 1;
}

.sandbox-panel {
  max-width: none;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 16px;
}

.capability-cell {
  min-height: 98px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color-page);
}

.capability-cell span {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
}

.capability-cell strong {
  display: block;
  margin-top: 8px;
  color: var(--el-text-color-primary);
}

.capability-cell small {
  display: block;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  line-height: 1.45;
}

.search-form {
  padding: 16px 16px 4px;
}

.material-content {
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .payment-admin {
    grid-template-columns: 1fr;
  }

  .payment-nav {
    min-height: auto;
  }

  .nav-scroll {
    height: auto;
  }

  .workspace-grid,
  .summary-strip {
    grid-template-columns: 1fr;
  }

  .summary-item {
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
}
</style>
