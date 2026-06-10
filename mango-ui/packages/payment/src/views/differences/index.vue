<template>
  <div class="payment-differences">
    <section class="payment-differences__header">
      <div>
        <h3>差异处理</h3>
        <p>对对账差异进行查单、补单、退款补偿、忽略和关闭等受控处理。</p>
      </div>
    </section>

    <section class="payment-differences__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="差异单号 / 订单号 / 批次号 / 通道"
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
              v-for="status in processStatuses"
              :key="status.statusCode"
              :label="status.statusName"
              :value="status.statusCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

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
      :data="rows"
      v-loading="loading"
      row-key="id"
      stripe
      highlight-current-row
      class="payment-differences__table"
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column prop="differenceNo" label="差异单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="reconciliationNo" label="对账批次" min-width="180" show-overflow-tooltip />
      <el-table-column prop="relatedOrderNo" label="关联订单号" min-width="190" show-overflow-tooltip />
      <el-table-column label="差异类型" min-width="170">
        <template #default="{ row }">
          <span>{{ row.differenceTypeName || row.differenceType || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="差异金额（元）" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.differenceAmount) }}</template>
      </el-table-column>
      <el-table-column label="处理状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.processStatus)" effect="light">{{ row.processStatusName || row.processStatus || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="processResult" label="处理结果" min-width="220" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="156" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions">
            <el-button link type="primary" :icon="Tickets" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="canHandle(row)"
              link
              type="primary"
              :icon="Operation"
              @click="openHandle(row)"
            >
              处理
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="payment-differences__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      title="差异详情"
      size="760px"
      destroy-on-close
      append-to-body
    >
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <template v-else-if="detail">
        <el-descriptions title="差异信息" :column="2" border>
          <el-descriptions-item label="差异单号">{{ valueText(detail.differenceNo) }}</el-descriptions-item>
          <el-descriptions-item label="对账批次">{{ valueText(detail.reconciliationNo) }}</el-descriptions-item>
          <el-descriptions-item label="通道编码">{{ valueText(detail.channelCode) }}</el-descriptions-item>
          <el-descriptions-item label="账单日期">{{ valueText(detail.billDate) }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ valueText(detail.relatedOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="差异金额（元）">{{ formatMoney(detail.differenceAmount) }}</el-descriptions-item>
          <el-descriptions-item label="差异类型" :span="2">
            {{ detail.differenceTypeName || detail.differenceType || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="处理信息" :column="2" border class="payment-differences__detail-block">
          <el-descriptions-item label="处理状态">
            <el-tag :type="statusTagType(detail.processStatus)" effect="light">{{ detail.processStatusName || detail.processStatus || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理动作">{{ actionName(detail.processAction) }}</el-descriptions-item>
          <el-descriptions-item label="处理原因" :span="2">{{ valueText(detail.processReason) }}</el-descriptions-item>
          <el-descriptions-item label="处理结果" :span="2">{{ valueText(detail.processResult) }}</el-descriptions-item>
          <el-descriptions-item label="处理凭据" :span="2">{{ valueText(detail.processEvidence) }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ valueText(detail.processorName) }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ valueText(detail.processTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间信息" :column="2" border class="payment-differences__detail-block">
          <el-descriptions-item label="创建时间">{{ valueText(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ valueText(detail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="未查询到差异详情" />
    </el-drawer>

    <el-dialog
      v-model="handleVisible"
      title="处理差异"
      width="660px"
      destroy-on-close
      append-to-body
    >
      <el-form
        ref="handleFormRef"
        :model="handleForm"
        :rules="handleRules"
        label-width="108px"
        class="payment-dialog-form"
      >
        <el-form-item label="差异单号">
          <el-input :model-value="currentRow?.differenceNo || '-'" disabled />
        </el-form-item>
        <el-form-item label="关联订单">
          <el-input :model-value="currentRow?.relatedOrderNo || '-'" disabled />
        </el-form-item>
        <el-form-item label="处理动作" prop="processAction">
          <el-select v-model="handleForm.processAction" placeholder="选择处理动作">
            <el-option
              v-for="action in processActions"
              :key="action.actionCode"
              :label="action.actionName"
              :value="action.actionCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处理原因" prop="processReason">
          <el-input
            v-model="handleForm.processReason"
            type="textarea"
            :rows="3"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="处理结果" prop="processResult">
          <el-input
            v-model="handleForm.processResult"
            type="textarea"
            :rows="3"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="处理凭据">
          <el-input
            v-model="handleForm.processEvidence"
            placeholder="文件中心 ID 或业务凭据 token"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleSubmitting" @click="submitHandle">保存处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Operation, Refresh, Search, Tickets } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import Pagination from '@mango/common/components/Pagination/index.vue';
import {
  paymentDifferenceApi,
  type HandlePaymentDifferenceCommand,
  type PaymentDifference,
  type PaymentDifferenceAction,
  type PaymentDifferenceStatus,
  type PaymentPageQuery,
} from '../../api/payment';

type TagType = '' | 'success' | 'warning' | 'info' | 'primary' | 'danger';

const processStatuses = ref<PaymentDifferenceStatus[]>([]);
const processActions = ref<PaymentDifferenceAction[]>([]);

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});
const rows = ref<PaymentDifference[]>([]);
const total = ref(0);
const loading = ref(false);
const errorMessage = ref('');
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentDifference>();
const handleVisible = ref(false);
const handleSubmitting = ref(false);
const currentRow = ref<PaymentDifference>();
const handleFormRef = ref<FormInstance>();
const handleForm = reactive<HandlePaymentDifferenceCommand>({
  id: '',
  processAction: '',
  processReason: '',
  processResult: '',
  processEvidence: '',
});

const handleRules: FormRules<HandlePaymentDifferenceCommand> = {
  processAction: [{ required: true, message: '请选择处理动作', trigger: 'change' }],
  processReason: [{ required: true, message: '请输入处理原因', trigger: 'blur' }],
  processResult: [{ required: true, message: '请输入处理结果', trigger: 'blur' }],
};

const emptyDescription = computed(() => {
  if (errorMessage.value) return '差异列表加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的差异单' : '暂无对账差异';
});

onMounted(() => {
  void loadOptions();
  void loadRows();
});

async function loadOptions() {
  try {
    const [statuses, actions] = await Promise.all([
      paymentDifferenceApi.statuses(),
      paymentDifferenceApi.actions(),
    ]);
    processStatuses.value = statuses;
    processActions.value = actions;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '差异处理选项加载失败');
  }
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentDifferenceApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '差异列表加载失败';
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

async function openDetail(row: PaymentDifference) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentDifferenceApi.detail(row.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '差异详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function openHandle(row: PaymentDifference) {
  if (!row.id) return;
  currentRow.value = row;
  handleForm.id = row.id;
  handleForm.processAction = '';
  handleForm.processReason = '';
  handleForm.processResult = '';
  handleForm.processEvidence = '';
  handleVisible.value = true;
}

async function submitHandle() {
  if (!handleFormRef.value) return;
  const valid = await handleFormRef.value.validate().catch(() => false);
  if (!valid) return;
  handleSubmitting.value = true;
  try {
    await paymentDifferenceApi.handle({
      ...handleForm,
      processEvidence: handleForm.processEvidence || undefined,
    });
    ElMessage.success('差异已处理');
    handleVisible.value = false;
    await loadRows();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '差异处理失败');
  } finally {
    handleSubmitting.value = false;
  }
}

function canHandle(row: PaymentDifference) {
  return row.processStatus === 'PENDING' || row.processStatus === 'PROCESSING';
}

function statusTagType(status?: string): TagType {
  if (status === 'HANDLED' || status === 'CLOSED') return 'success';
  if (status === 'PROCESSING') return 'warning';
  if (status === 'IGNORED') return 'info';
  return '';
}

function actionName(actionCode?: string) {
  if (!actionCode) return '-';
  return processActions.value.find(action => action.actionCode === actionCode)?.actionName || actionCode;
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
.payment-differences__detail-block {
  margin-top: 18px;
}

</style>
