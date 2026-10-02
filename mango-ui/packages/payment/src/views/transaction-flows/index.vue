<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <MangoListPage class="payment-transaction-flows" data-page="payment.transaction-flows">
    <template #search>
      <MangoSearchPanel :model="query" :columns="3" @search="loadRows" @reset="resetQuery">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="流水号 / 业务单号 / 支付单号 / 退款单号 / 类型"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false">
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
        class="payment-transaction-flows__table"
      >
        <template #empty>
          <el-empty :description="emptyDescription" />
        </template>
        <el-table-column prop="flowNo" label="流水号" min-width="190" show-overflow-tooltip />
        <el-table-column prop="bizOrderNo" label="业务订单号" min-width="190" show-overflow-tooltip />
        <el-table-column prop="payOrderNo" label="支付订单号" min-width="190" show-overflow-tooltip />
        <el-table-column prop="refundOrderNo" label="退款订单号" min-width="190" show-overflow-tooltip />
        <el-table-column label="流水类型" width="150">
          <template #default="{ row }">
            <span>{{ row.flowTypeName || row.flowType || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额（元）" width="130" align="right">
          <template #default="{ row }">
            <span>{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="104" fixed="right" align="left" class-name="payment-table__operation-cell">
          <template #default="{ row }">
            <div class="payment-table__actions">
              <el-button link type="primary" :icon="Tickets" @click="openDetail(row)">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadRows" />
      </template>
    </MangoListPanel>

    <MangoSideDrawerShell
      v-model="detailVisible"
      title="交易流水详情"
      drawer-size="680px"
      :destroy-on-close="true"
      :show-trigger="false"
      data-surface="payment.transaction-flow.detail"
      class="payment-transaction-flows__drawer"
    >
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="detail">
        <el-descriptions title="流水信息" :column="2" border>
          <el-descriptions-item label="流水号">{{ valueText(detail.flowNo) }}</el-descriptions-item>
          <el-descriptions-item label="流水类型">
            {{ detail.flowTypeName || detail.flowType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="金额（元）">{{ formatMoney(detail.amount) }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ valueText(detail.currency) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="关联订单" :column="1" border class="payment-transaction-flows__detail-block">
          <el-descriptions-item label="业务订单号">{{ valueText(detail.bizOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="支付订单号">{{ valueText(detail.payOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="退款订单号">{{ valueText(detail.refundOrderNo) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间信息" :column="2" border class="payment-transaction-flows__detail-block">
          <el-descriptions-item label="创建时间">{{ valueText(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ valueText(detail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="未查询到交易流水详情" />
    </MangoSideDrawerShell>
  </MangoListPage>
</template>

<script setup lang="ts">
import { MangoListPage, MangoListPanel, MangoSearchPanel, MangoSideDrawerShell, Pagination } from '@mango/common';
import { computed, onMounted, reactive, ref } from 'vue';
import { Refresh, Tickets } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { paymentTransactionFlowApi, type PaymentPageQuery, type PaymentTransactionFlow } from '../../api/payment';

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
});
const rows = ref<PaymentTransactionFlow[]>([]);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentTransactionFlow>();
const errorMessage = ref('');

const emptyDescription = computed(() => {
  if (errorMessage.value) return '交易流水加载失败';
  return query.keyword ? '未查询到匹配的交易流水' : '暂无交易流水';
});

onMounted(loadRows);

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentTransactionFlowApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '交易流水加载失败';
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.pageNum = 1;
  query.keyword = '';
  void loadRows();
}

async function openDetail(row: PaymentTransactionFlow) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentTransactionFlowApi.detail(row.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '交易流水详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function formatMoney(value: unknown) {
  const amount = Number(value || 0) / 100;
  return amount ? `￥${amount.toFixed(2)}` : '-';
}

function valueText(value: unknown) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}
</script>

<style scoped>
.payment-transaction-flows__detail-block {
  margin-top: 18px;
}
</style>
