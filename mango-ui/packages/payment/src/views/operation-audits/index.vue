<template>
  <section class="payment-operation-audits">
    <div class="payment-operation-audits__header">
      <div>
        <h3>操作审计</h3>
        <p>查询支付域关键配置变更、资金相关人工操作和审批处理记录。</p>
      </div>
    </div>

    <div class="payment-operation-audits__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="操作人 / 资源 / 动作"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
        <el-form-item label="结果">
          <el-select
            v-model="query.statusCode"
            placeholder="全部结果"
            clearable
            @change="loadRows"
            @clear="loadRows"
          >
            <el-option label="成功" value="SUCCESS" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="payment-operation-audits__actions">
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
      class="payment-operation-audits__table"
      :data="rows"
      row-key="id"
      stripe
      highlight-current-row
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column prop="operatorName" label="操作人" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ valueText(row.operatorName) }}</template>
      </el-table-column>
      <el-table-column label="操作动作" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ valueText(row.operationAction) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="资源类型" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ valueText(row.resourceType) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="resourceId" label="资源标识" min-width="190" show-overflow-tooltip>
        <template #default="{ row }">{{ valueText(row.resourceId) }}</template>
      </el-table-column>
      <el-table-column label="结果" width="110">
        <template #default="{ row }">
          <el-tag :type="resultTagType(row.operationResult)" effect="light">
            {{ resultText(row.operationResult) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operationTime" label="操作时间" width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ valueText(row.operationTime) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="记录时间" width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ valueText(row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <div class="payment-operation-audits__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Refresh, Search } from '@element-plus/icons-vue';
import Pagination from '@mango/common/components/Pagination/index.vue';
import {
  paymentOperationAuditApi,
  type PaymentOperationAudit,
  type PaymentPageQuery,
} from '../../api/payment';

type TagType = '' | 'success' | 'warning' | 'info' | 'primary' | 'danger';

const rows = ref<PaymentOperationAudit[]>([]);
const total = ref(0);
const loading = ref(false);
const errorMessage = ref('');

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});

const emptyDescription = computed(() => {
  if (errorMessage.value) return '操作审计加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的操作审计' : '暂无操作审计';
});

onMounted(loadRows);

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentOperationAuditApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '操作审计加载失败';
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

function resultTagType(result?: string): TagType {
  if (result === 'SUCCESS') return 'success';
  if (result === 'REJECTED') return 'danger';
  return 'info';
}

function resultText(result?: string) {
  if (result === 'SUCCESS') return '成功';
  if (result === 'REJECTED') return '已拒绝';
  return valueText(result);
}

function valueText(value: unknown) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}
</script>

<style scoped>
.payment-operation-audits__actions {
  display: flex;
  gap: 8px;
}

</style>
