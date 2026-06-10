<template>
  <section class="payment-reconciliations">
    <section class="payment-reconciliations__header">
      <div>
        <h3>对账管理</h3>
        <p>管理通道账单导入、批次结果和支付成功金额核对。</p>
      </div>
      <div class="payment-reconciliations__header-actions">
        <el-button @click="openGenerateDialog">生成芒果支付账单</el-button>
        <el-button type="primary" @click="openImportDialog">导入账单</el-button>
      </div>
    </section>

    <section class="payment-reconciliations__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="批次号 / 通道 / 文件 / 导入人" @keyup.enter="loadPage" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.statusCode" clearable placeholder="全部状态">
            <el-option v-for="item in statuses" :key="item.statusCode" :label="item.statusName" :value="item.statusCode" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="payment-reconciliations__actions">
            <el-button type="primary" @click="loadPage">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </section>

    <el-table
      v-loading="loading"
      class="payment-reconciliations__table"
      :data="rows"
      row-key="id"
      stripe
      highlight-current-row
    >
      <el-table-column prop="reconciliationNo" label="对账批次号" min-width="180" />
      <el-table-column prop="channelCode" label="通道" width="120" />
      <el-table-column prop="billDate" label="账单日期" width="120" />
      <el-table-column prop="totalCount" label="笔数" width="90" align="right" />
      <el-table-column label="账单金额（元）" width="130" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="手续费（元）" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalFee) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.matchStatus)">{{ row.matchStatusName || row.matchStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="billFileName" label="文件名" min-width="170" />
      <el-table-column prop="fileDigest" label="文件摘要" min-width="180" show-overflow-tooltip />
      <el-table-column prop="importerName" label="导入人" width="120" />
      <el-table-column prop="importTime" label="导入时间" min-width="170" />
      <el-table-column label="操作" width="104" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          </div>
        </template>
      </el-table-column>
      <template #empty>
        <div class="payment-reconciliations__empty">未查询到匹配的对账批次</div>
      </template>
    </el-table>

    <div class="payment-reconciliations__pagination">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadPage"
        @current-change="loadPage"
      />
    </div>

    <el-dialog v-model="importDialogVisible" title="导入通道账单" width="960px" destroy-on-close>
      <el-form ref="importFormRef" :model="importForm" :rules="importRules" label-width="108px" class="payment-dialog-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="通道编码" prop="channelCode">
              <el-input v-model="importForm.channelCode" placeholder="例如 MANGO_PAY / ALLINPAY" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="账单日期" prop="billDate">
              <el-date-picker v-model="importForm.billDate" type="date" value-format="YYYY-MM-DD" placeholder="选择账单日期" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="账单文件 ID">
              <el-input v-model="importForm.billFileId" placeholder="文件中心 ID，可为空" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="文件名" prop="billFileName">
              <el-input v-model="importForm.billFileName" placeholder="通道账单文件名" />
            </el-form-item>
          </el-col>
          <el-col :xs="24">
            <el-form-item label="文件摘要" prop="fileDigest">
              <el-input v-model="importForm.fileDigest" placeholder="账单文件 SHA256/MD5 摘要" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="payment-reconciliations__line-header">
          <span>账单明细</span>
          <el-button size="small" @click="addBillItem">新增明细</el-button>
        </div>
        <el-table :data="importForm.items" row-key="rowKey" class="payment-reconciliations__items">
          <el-table-column label="通道交易号" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.channelTradeNo" placeholder="通道交易号" />
            </template>
          </el-table-column>
          <el-table-column label="交易类型" width="130">
            <template #default="{ row }">
              <el-select v-model="row.tradeType">
                <el-option label="支付" value="PAYMENT" />
                <el-option label="退款" value="REFUND" />
                <el-option label="手续费" value="FEE" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="金额（元）" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.amountYuan" :min="0" :precision="2" :step="1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="手续费（元）" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.feeYuan" :min="0" :precision="2" :step="0.1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="交易时间" min-width="190">
            <template #default="{ row }">
              <el-date-picker v-model="row.tradeTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择交易时间" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button type="danger" link :disabled="importForm.items.length === 1" @click="removeBillItem($index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="generateDialogVisible" title="生成芒果支付账单" width="520px" destroy-on-close>
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="118px" class="payment-dialog-form">
        <el-form-item label="通道编码" prop="channelCode">
          <el-input v-model="generateForm.channelCode" disabled />
        </el-form-item>
        <el-form-item label="签约配置 ID">
          <el-input v-model="generateForm.contractId" clearable placeholder="用于指定签约场景控制，可为空" />
        </el-form-item>
        <el-form-item label="账单日期" prop="billDate">
          <el-date-picker v-model="generateForm.billDate" type="date" value-format="YYYY-MM-DD" placeholder="选择账单日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="submitGenerate">生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="对账批次详情" size="720px">
      <div v-if="currentDetail" class="payment-reconciliations__detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="批次号">{{ currentDetail.reconciliationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentDetail.matchStatus)">{{ currentDetail.matchStatusName || currentDetail.matchStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="通道">{{ currentDetail.channelCode }}</el-descriptions-item>
          <el-descriptions-item label="账单日期">{{ currentDetail.billDate }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ currentDetail.billFileName }}</el-descriptions-item>
          <el-descriptions-item label="文件摘要">{{ currentDetail.fileDigest }}</el-descriptions-item>
          <el-descriptions-item label="导入人">{{ currentDetail.importerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="导入时间">{{ currentDetail.importTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="账单金额（元）">{{ formatMoney(currentDetail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="手续费（元）">{{ formatMoney(currentDetail.totalFee) }}</el-descriptions-item>
          <el-descriptions-item label="对账结果" :span="2">{{ currentDetail.reconcileResult || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h3>账单明细</h3>
        <el-table :data="currentDetail.details || []" row-key="id" stripe>
          <el-table-column prop="channelTradeNo" label="通道交易号" min-width="180" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <span>{{ row.tradeTypeName || row.tradeType }}</span>
            </template>
          </el-table-column>
          <el-table-column label="金额（元）" width="110" align="right">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="手续费（元）" width="110" align="right">
            <template #default="{ row }">{{ formatMoney(row.fee) }}</template>
          </el-table-column>
          <el-table-column label="匹配状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.matchStatus)">{{ row.matchStatusName || row.matchStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="matchedOrderNo" label="匹配订单" min-width="150" />
          <el-table-column prop="matchMessage" label="匹配说明" min-width="190" show-overflow-tooltip />
        </el-table>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import {
  paymentReconciliationApi,
  type GenerateMangoPayVirtualBillCommand,
  type ImportPaymentReconciliationCommand,
  type PaymentPageQuery,
  type PaymentReconciliation,
  type PaymentReconciliationStatus,
} from '../../api/payment';

type BillItemForm = Omit<ImportPaymentReconciliationCommand['items'][number], 'amount' | 'fee'> & {
  rowKey: string;
  amountYuan: number;
  feeYuan: number;
};

type ImportForm = Omit<ImportPaymentReconciliationCommand, 'items'> & {
  items: BillItemForm[];
};

type GenerateForm = GenerateMangoPayVirtualBillCommand;

const loading = ref(false);
const importing = ref(false);
const generating = ref(false);
const importDialogVisible = ref(false);
const generateDialogVisible = ref(false);
const detailVisible = ref(false);
const total = ref(0);
const rows = ref<PaymentReconciliation[]>([]);
const statuses = ref<PaymentReconciliationStatus[]>([]);
const currentDetail = ref<PaymentReconciliation>();
const importFormRef = ref<FormInstance>();
const generateFormRef = ref<FormInstance>();

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});

const importForm = reactive<ImportForm>({
  channelCode: '',
  billDate: '',
  billFileId: '',
  billFileName: '',
  fileDigest: '',
  items: [createBillItem()],
});

const generateForm = reactive<GenerateForm>({
  channelCode: 'MANGO_PAY',
  contractId: '',
  billDate: '',
});

const importRules: FormRules<ImportForm> = {
  channelCode: [{ required: true, message: '请输入通道编码', trigger: 'blur' }],
  billDate: [{ required: true, message: '请选择账单日期', trigger: 'change' }],
  billFileName: [{ required: true, message: '请输入账单文件名', trigger: 'blur' }],
  fileDigest: [{ required: true, message: '请输入账单文件摘要', trigger: 'blur' }],
};

const generateRules: FormRules<GenerateForm> = {
  channelCode: [{ required: true, message: '请输入通道编码', trigger: 'blur' }],
  billDate: [{ required: true, message: '请选择账单日期', trigger: 'change' }],
};

onMounted(async () => {
  await Promise.all([loadStatuses(), loadPage()]);
});

async function loadStatuses() {
  statuses.value = await paymentReconciliationApi.statuses();
}

async function loadPage() {
  loading.value = true;
  try {
    const page = await paymentReconciliationApi.page(query);
    rows.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.keyword = '';
  query.statusCode = '';
  query.pageNum = 1;
  loadPage();
}

function openImportDialog() {
  resetImportForm();
  importDialogVisible.value = true;
}

function openGenerateDialog() {
  resetGenerateForm();
  generateDialogVisible.value = true;
}

function resetImportForm() {
  importForm.channelCode = '';
  importForm.billDate = '';
  importForm.billFileId = '';
  importForm.billFileName = '';
  importForm.fileDigest = '';
  importForm.items = [createBillItem()];
}

function resetGenerateForm() {
  generateForm.channelCode = 'MANGO_PAY';
  generateForm.contractId = '';
  generateForm.billDate = new Date().toISOString().slice(0, 10);
}

function addBillItem() {
  importForm.items.push(createBillItem());
}

function removeBillItem(index: number) {
  importForm.items.splice(index, 1);
}

async function submitImport() {
  const valid = await importFormRef.value?.validate();
  if (!valid) return;
  const items = importForm.items.map((item) => ({
    channelTradeNo: item.channelTradeNo.trim(),
    tradeType: item.tradeType,
    amount: yuanToCents(item.amountYuan),
    fee: yuanToCents(item.feeYuan),
    tradeTime: item.tradeTime,
  }));
  if (items.some(item => !item.channelTradeNo || !item.tradeType || !item.tradeTime)) {
    ElMessage.error('请完整填写账单明细');
    return;
  }
  importing.value = true;
  try {
    await paymentReconciliationApi.importBill({
      channelCode: importForm.channelCode.trim(),
      billDate: importForm.billDate,
      billFileId: importForm.billFileId || undefined,
      billFileName: importForm.billFileName.trim(),
      fileDigest: importForm.fileDigest.trim(),
      items,
    });
    ElMessage.success('账单已导入');
    importDialogVisible.value = false;
    query.pageNum = 1;
    await loadPage();
  } finally {
    importing.value = false;
  }
}

async function submitGenerate() {
  const valid = await generateFormRef.value?.validate();
  if (!valid) return;
  generating.value = true;
  try {
    await paymentReconciliationApi.generateMangoPayVirtualBill({
      channelCode: generateForm.channelCode.trim(),
      contractId: generateForm.contractId || undefined,
      billDate: generateForm.billDate,
    });
    ElMessage.success('芒果支付账单已生成');
    generateDialogVisible.value = false;
    query.pageNum = 1;
    await loadPage();
  } finally {
    generating.value = false;
  }
}

async function openDetail(row: PaymentReconciliation) {
  if (!row.id) return;
  currentDetail.value = await paymentReconciliationApi.detail(row.id);
  detailVisible.value = true;
}

function createBillItem(): BillItemForm {
  return {
    rowKey: `${Date.now()}-${Math.random()}`,
    channelTradeNo: '',
    tradeType: 'PAYMENT',
    amountYuan: 0,
    feeYuan: 0,
    tradeTime: '',
  };
}

function yuanToCents(value?: number | null) {
  return Math.round(Number(value || 0) * 100);
}

function formatMoney(value?: number | string | null) {
  if (value === undefined || value === null || value === '') {
    return '¥0.00';
  }
  const raw = typeof value === 'number' ? Math.trunc(value).toString() : value.trim();
  if (!/^-?\d+$/.test(raw)) {
    return '¥0.00';
  }
  const negative = raw.startsWith('-');
  const cents = BigInt(negative ? raw.slice(1) : raw);
  const yuan = cents / 100n;
  const fen = (cents % 100n).toString().padStart(2, '0');
  return `${negative ? '-' : ''}¥${yuan}.${fen}`;
}

function statusTagType(status?: string) {
  if (status === 'MATCHED') return 'success';
  if (status === 'DIFFERENCE') return 'danger';
  return 'info';
}
</script>

<style scoped>
.payment-reconciliations__actions {
  display: flex;
  gap: 8px;
}

.payment-reconciliations__empty {
  padding: 24px 0;
  color: var(--el-text-color-secondary);
}

.payment-reconciliations__line-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
  font-weight: 600;
}

.payment-reconciliations__items :deep(.el-input-number) {
  width: 100%;
}

.payment-reconciliations__detail h3 {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 600;
}
</style>
