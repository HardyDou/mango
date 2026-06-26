<template>
  <section class="payment-settlement-summaries">
    <div class="payment-settlement-summaries__header">
      <div>
        <h3>结算汇总</h3>
        <p>按日、应用、企业主体和通道汇总支付、退款、手续费和净收款，用于财务核对。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openGenerate(false)">生成汇总</el-button>
    </div>

    <div class="payment-settlement-summaries__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="应用 / 主体 / 通道"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.statusCode"
            placeholder="全部状态"
            clearable
            @change="loadRows"
            @clear="loadRows"
          >
            <el-option
              v-for="status in statuses"
              :key="status.statusCode"
              :label="status.statusName"
              :value="status.statusCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="payment-settlement-summaries__actions">
            <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button link type="primary" :icon="Refresh" @click="loadRows">重新加载</el-button>
      </template>
    </el-alert>

    <el-table
      v-loading="loading"
      class="payment-settlement-summaries__table"
      :data="rows"
      row-key="id"
      stripe
      highlight-current-row
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column prop="settlementDate" label="结算日期" width="120" />
      <el-table-column prop="appCode" label="应用" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ row.appName ? `${row.appName} / ${row.appCode}` : valueText(row.appCode) }}</template>
      </el-table-column>
      <el-table-column prop="subjectName" label="企业主体" min-width="180" show-overflow-tooltip />
      <el-table-column prop="channelCode" label="通道" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.channelName ? `${row.channelName} / ${row.channelCode}` : valueText(row.channelCode) }}</template>
      </el-table-column>
      <el-table-column label="支付金额（元）" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.tradeAmount) }}</template>
      </el-table-column>
      <el-table-column label="退款金额（元）" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.refundAmount) }}</template>
      </el-table-column>
      <el-table-column label="手续费（元）" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.feeAmount) }}</template>
      </el-table-column>
      <el-table-column label="净收款（元）" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.netAmount) }}</template>
      </el-table-column>
      <el-table-column label="未处理差异（笔/元）" width="140" align="right">
        <template #default="{ row }">
          {{ row.unresolvedDifferenceCount || 0 }} / {{ formatMoney(row.unresolvedDifferenceAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" effect="light">{{ row.statusName || row.status || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="generatedAt" label="生成时间" width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="292" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions">
            <el-button link type="primary" :icon="Tickets" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'GENERATED'" link type="primary" :icon="Check" @click="confirmRow(row)">确认</el-button>
            <el-button v-if="row.status === 'CONFIRMED'" link type="danger" :icon="Close" @click="openVoid(row)">作废</el-button>
            <el-button v-if="row.status === 'VOIDED'" link type="primary" :icon="Refresh" @click="openGenerate(true, row)">重新生成</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="payment-settlement-summaries__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>

    <el-dialog
      v-model="generateVisible"
      :title="generateForm.rebuild ? '重新生成结算汇总' : '生成结算汇总'"
      width="640px"
      destroy-on-close
      append-to-body
    >
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="112px" class="payment-dialog-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="结算日期" prop="settlementDate">
              <el-date-picker
                v-model="generateForm.settlementDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择结算日期"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="通道编码" prop="channelCode">
              <el-input v-model="generateForm.channelCode" placeholder="例如 MANGO_PAY" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="应用编码" prop="appCode">
              <el-input v-model="generateForm.appCode" placeholder="业务应用编码" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="企业主体 ID" prop="enterpriseSubjectId">
              <el-input v-model="generateForm.enterpriseSubjectId" placeholder="企业主体 ID" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitGenerate">
          {{ generateForm.rebuild ? '重新生成' : '生成' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="voidVisible"
      title="作废结算汇总"
      width="560px"
      destroy-on-close
      append-to-body
    >
      <el-form ref="voidFormRef" :model="voidForm" :rules="voidRules" label-width="96px" class="payment-dialog-form">
        <el-form-item label="汇总范围">
          <el-input :model-value="currentRow ? `${currentRow.settlementDate} / ${currentRow.appCode} / ${currentRow.channelCode}` : '-'" disabled />
        </el-form-item>
        <el-form-item label="作废原因" prop="voidReason">
          <el-input
            v-model="voidForm.voidReason"
            type="textarea"
            :rows="4"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="voidVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitVoid">确认作废</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      title="结算汇总详情"
      size="760px"
      destroy-on-close
      append-to-body
    >
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <template v-else-if="detail">
        <el-descriptions title="汇总范围" :column="2" border>
          <el-descriptions-item label="结算日期">{{ valueText(detail.settlementDate) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail.status)" effect="light">{{ detail.statusName || detail.status || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="应用">{{ detail.appName ? `${detail.appName} / ${detail.appCode}` : valueText(detail.appCode) }}</el-descriptions-item>
          <el-descriptions-item label="企业主体">{{ valueText(detail.subjectName) }}</el-descriptions-item>
          <el-descriptions-item label="通道">{{ detail.channelName ? `${detail.channelName} / ${detail.channelCode}` : valueText(detail.channelCode) }}</el-descriptions-item>
          <el-descriptions-item label="主体 ID">{{ valueText(detail.enterpriseSubjectId) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="金额与笔数" :column="2" border class="payment-settlement-summaries__detail-block">
          <el-descriptions-item label="支付成功金额（元）">{{ formatMoney(detail.tradeAmount) }}</el-descriptions-item>
          <el-descriptions-item label="支付成功笔数">{{ detail.tradeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="退款成功金额（元）">{{ formatMoney(detail.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="退款成功笔数">{{ detail.refundCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="通道手续费（元）">{{ formatMoney(detail.feeAmount) }}</el-descriptions-item>
          <el-descriptions-item label="净收款（元）">{{ formatMoney(detail.netAmount) }}</el-descriptions-item>
          <el-descriptions-item label="未处理差异笔数">{{ detail.unresolvedDifferenceCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="未处理差异金额（元）">{{ formatMoney(detail.unresolvedDifferenceAmount) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="操作信息" :column="2" border class="payment-settlement-summaries__detail-block">
          <el-descriptions-item label="生成人">{{ valueText(detail.generatedByName) }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ valueText(detail.generatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="确认人">{{ valueText(detail.confirmedByName) }}</el-descriptions-item>
          <el-descriptions-item label="确认时间">{{ valueText(detail.confirmedAt) }}</el-descriptions-item>
          <el-descriptions-item label="作废人">{{ valueText(detail.voidedByName) }}</el-descriptions-item>
          <el-descriptions-item label="作废时间">{{ valueText(detail.voidedAt) }}</el-descriptions-item>
          <el-descriptions-item label="作废原因" :span="2">{{ valueText(detail.voidReason) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="未查询到结算汇总详情" />
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { Pagination } from '@mango/common';
import { computed, onMounted, reactive, ref } from 'vue';
import { Check, Close, Plus, Refresh, Search, Tickets } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  paymentSettlementSummaryApi,
  type GeneratePaymentSettlementSummaryCommand,
  type PaymentPageQuery,
  type PaymentSettlementSummary,
  type PaymentSettlementSummaryStatus,
  type VoidPaymentSettlementSummaryCommand,
} from '../../api/payment';

type TagType = '' | 'success' | 'warning' | 'info' | 'primary' | 'danger';

const statuses = ref<PaymentSettlementSummaryStatus[]>([]);
const rows = ref<PaymentSettlementSummary[]>([]);
const total = ref(0);
const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref('');
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentSettlementSummary>();
const generateVisible = ref(false);
const voidVisible = ref(false);
const currentRow = ref<PaymentSettlementSummary>();
const generateFormRef = ref<FormInstance>();
const voidFormRef = ref<FormInstance>();

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});

const generateForm = reactive<GeneratePaymentSettlementSummaryCommand>({
  settlementDate: '',
  appCode: '',
  enterpriseSubjectId: '',
  channelCode: '',
  rebuild: false,
});

const voidForm = reactive<VoidPaymentSettlementSummaryCommand>({
  id: '',
  voidReason: '',
});

const generateRules: FormRules<GeneratePaymentSettlementSummaryCommand> = {
  settlementDate: [{ required: true, message: '请选择结算日期', trigger: 'change' }],
  appCode: [{ required: true, message: '请输入应用编码', trigger: 'blur' }],
  enterpriseSubjectId: [{ required: true, message: '请输入企业主体 ID', trigger: 'blur' }],
  channelCode: [{ required: true, message: '请输入通道编码', trigger: 'blur' }],
};

const voidRules: FormRules<VoidPaymentSettlementSummaryCommand> = {
  voidReason: [{ required: true, message: '请输入作废原因', trigger: 'blur' }],
};

const emptyDescription = computed(() => {
  if (errorMessage.value) return '结算汇总加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的结算汇总' : '暂无结算汇总';
});

onMounted(() => {
  void loadOptions();
  void loadRows();
});

async function loadOptions() {
  try {
    statuses.value = await paymentSettlementSummaryApi.statuses();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '结算汇总状态加载失败');
  }
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentSettlementSummaryApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '结算汇总加载失败';
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.pageNum = 1;
  query.keyword = '';
  query.statusCode = '';
  void loadRows();
}

function openGenerate(rebuild: boolean, row?: PaymentSettlementSummary) {
  currentRow.value = row;
  generateForm.settlementDate = row?.settlementDate || '';
  generateForm.appCode = row?.appCode || '';
  generateForm.enterpriseSubjectId = row?.enterpriseSubjectId || '';
  generateForm.channelCode = row?.channelCode || '';
  generateForm.rebuild = rebuild;
  generateVisible.value = true;
}

async function submitGenerate() {
  if (!generateFormRef.value) return;
  const valid = await generateFormRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    await paymentSettlementSummaryApi.generate({
      ...generateForm,
      appCode: generateForm.appCode.trim().toUpperCase(),
      channelCode: generateForm.channelCode.trim().toUpperCase(),
    });
    ElMessage.success(generateForm.rebuild ? '结算汇总已重新生成' : '结算汇总已生成');
    generateVisible.value = false;
    await loadRows();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '结算汇总生成失败');
  } finally {
    submitting.value = false;
  }
}

async function confirmRow(row: PaymentSettlementSummary) {
  if (!row.id) return;
  try {
    await ElMessageBox.confirm('确认后该结算汇总不能被覆盖，修正需作废后重新生成。', '确认结算汇总', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    });
    submitting.value = true;
    await paymentSettlementSummaryApi.confirm({ id: row.id });
    ElMessage.success('结算汇总已确认');
    await loadRows();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '结算汇总确认失败');
    }
  } finally {
    submitting.value = false;
  }
}

function openVoid(row: PaymentSettlementSummary) {
  if (!row.id) return;
  currentRow.value = row;
  voidForm.id = row.id;
  voidForm.voidReason = '';
  voidVisible.value = true;
}

async function submitVoid() {
  if (!voidFormRef.value) return;
  const valid = await voidFormRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    await paymentSettlementSummaryApi.void({
      id: voidForm.id,
      voidReason: voidForm.voidReason.trim(),
    });
    ElMessage.success('结算汇总已作废');
    voidVisible.value = false;
    await loadRows();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '结算汇总作废失败');
  } finally {
    submitting.value = false;
  }
}

async function openDetail(row: PaymentSettlementSummary) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentSettlementSummaryApi.detail(row.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '结算汇总详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function statusTagType(status?: string): TagType {
  if (status === 'CONFIRMED') return 'success';
  if (status === 'VOIDED') return 'info';
  if (status === 'GENERATED') return 'warning';
  return '';
}

function formatMoney(value: number | string | undefined) {
  if (value === undefined || value === null || value === '') {
    return '￥0.00';
  }
  const raw = typeof value === 'number' ? Math.trunc(value).toString() : value.trim();
  if (!/^-?\d+$/.test(raw)) {
    return '￥0.00';
  }
  const negative = raw.startsWith('-');
  const cents = BigInt(negative ? raw.slice(1) : raw);
  const yuan = cents / 100n;
  const fen = (cents % 100n).toString().padStart(2, '0');
  return `${negative ? '-' : ''}￥${yuan}.${fen}`;
}

function valueText(value: unknown) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}
</script>

<style scoped>
.payment-settlement-summaries__actions {
  display: flex;
  gap: 8px;
}

.payment-settlement-summaries__detail-block {
  margin-top: 18px;
}

</style>
