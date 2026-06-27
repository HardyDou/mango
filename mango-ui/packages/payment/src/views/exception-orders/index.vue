<template>
  <div class="payment-exception-orders">
    <section class="payment-exception-orders__header">
      <div>
        <h3>异常订单</h3>
        <p>处理重复支付、超时未回调、金额不一致、状态不一致等支付异常。</p>
      </div>
      <el-button link type="primary" :icon="QuestionFilled" @click="helpVisible = true">帮助</el-button>
    </section>

    <section class="payment-exception-orders__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="异常单号 / 关联订单号 / 类型 / 状态"
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
              v-for="status in handleStatuses"
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
      class="payment-exception-orders__table"
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column prop="exceptionNo" label="异常单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="relatedOrderNo" label="关联订单号" min-width="190" show-overflow-tooltip />
      <el-table-column label="异常类型" width="150">
        <template #default="{ row }">
          <span>{{ row.exceptionTypeName || row.exceptionType || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="severityTagType(row.severity)" effect="light">{{ row.severityName || row.severity || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.handleStatus)" effect="light">{{ row.handleStatusName || row.handleStatus || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="异常原因" min-width="220" show-overflow-tooltip />
      <el-table-column prop="handleResult" label="处理结果" min-width="220" show-overflow-tooltip />
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

    <div class="payment-exception-orders__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      title="异常订单详情"
      size="720px"
      destroy-on-close
      append-to-body
      class="payment-exception-orders__drawer"
    >
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <template v-else-if="detail">
        <el-descriptions title="异常信息" :column="2" border>
          <el-descriptions-item label="异常单号">{{ valueText(detail.exceptionNo) }}</el-descriptions-item>
          <el-descriptions-item label="关联订单号">{{ valueText(detail.relatedOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="异常类型">
            {{ detail.exceptionTypeName || detail.exceptionType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag :type="severityTagType(detail.severity)" effect="light">{{ detail.severityName || detail.severity || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="异常原因" :span="2">{{ valueText(detail.reason) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="处理信息" :column="2" border class="payment-exception-orders__detail-block">
          <el-descriptions-item label="处理状态">
            <el-tag :type="statusTagType(detail.handleStatus)" effect="light">{{ detail.handleStatusName || detail.handleStatus || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理动作">{{ valueText(detail.handleAction) }}</el-descriptions-item>
          <el-descriptions-item label="处理原因" :span="2">{{ valueText(detail.handleReason) }}</el-descriptions-item>
          <el-descriptions-item label="处理结果" :span="2">{{ valueText(detail.handleResult) }}</el-descriptions-item>
          <el-descriptions-item label="处理凭据" :span="2">{{ valueText(detail.handleEvidence) }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ valueText(detail.handlerName) }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ valueText(detail.handleTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间信息" :column="2" border class="payment-exception-orders__detail-block">
          <el-descriptions-item label="创建时间">{{ valueText(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ valueText(detail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="未查询到异常订单详情" />
    </el-drawer>

    <el-drawer
      v-model="helpVisible"
      title="异常订单使用说明"
      size="520px"
      append-to-body
      class="payment-exception-orders__help-drawer"
    >
      <el-descriptions title="页面用途" :column="1" border>
        <el-descriptions-item label="处理范围">
          用于处理支付和退款流程中由系统真实状态流生成的异常单，不在本页面直接伪造订单结果。
        </el-descriptions-item>
        <el-descriptions-item label="处理原则">
          先核对关联订单和异常原因，再选择系统允许的处理动作。不同异常类型只展示当前可执行动作。
        </el-descriptions-item>
      </el-descriptions>
      <el-descriptions title="处理动作" :column="1" border class="payment-exception-orders__detail-block">
        <el-descriptions-item label="主动查单">
          仅支付类异常可用。系统会按关联支付订单向通道查单，并按通道结果推进支付订单状态。
        </el-descriptions-item>
        <el-descriptions-item label="主动查退款">
          仅退款类异常可用。系统会按关联退款订单向通道查退款，并按通道结果推进退款订单状态。
        </el-descriptions-item>
        <el-descriptions-item label="关闭支付订单">
          仅支付类异常可用。系统只允许关闭未支付或支付中的支付订单，并同步关闭对应业务订单。
        </el-descriptions-item>
        <el-descriptions-item label="补充凭据">
          支付类和退款类异常均可用。只记录人工核对材料，异常单进入处理中，不直接修改订单状态。
        </el-descriptions-item>
        <el-descriptions-item label="人工复核关闭">
          支付类和退款类异常均可用。人工确认无需系统动作后关闭异常单，不直接修改支付或退款状态。
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog
      v-model="handleVisible"
      title="处理异常订单"
      width="640px"
      destroy-on-close
      append-to-body
    >
      <el-form
        ref="handleFormRef"
        :model="handleForm"
        :rules="handleRules"
        label-width="96px"
        class="payment-dialog-form"
      >
        <el-form-item label="异常单号">
          <el-input :model-value="currentRow?.exceptionNo || '-'" disabled />
        </el-form-item>
        <el-form-item label="关联订单">
          <el-input :model-value="currentRow?.relatedOrderNo || '-'" disabled />
        </el-form-item>
        <el-form-item label="处理动作" prop="handleAction">
          <el-select v-model="handleForm.handleAction" placeholder="选择处理动作">
            <el-option
              v-for="action in allowedHandleActions"
              :key="action.actionCode"
              :label="action.actionName"
              :value="action.actionCode"
            />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="selectedHandleActionDescription"
          class="payment-exception-orders__action-help"
          :title="selectedHandleActionDescription"
          type="info"
          show-icon
          :closable="false"
        />
        <el-form-item label="处理原因" prop="handleReason">
          <el-input
            v-model="handleForm.handleReason"
            type="textarea"
            :rows="3"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="处理结果" prop="handleResult">
          <el-input
            v-model="handleForm.handleResult"
            type="textarea"
            :rows="3"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="处理凭据">
          <el-input
            v-model="handleForm.handleEvidence"
            type="textarea"
            :rows="2"
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
import { Pagination } from '@mango/common';
import { computed, onMounted, reactive, ref } from 'vue';
import { Operation, QuestionFilled, Refresh, Search, Tickets } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  paymentExceptionOrderApi,
  type HandlePaymentExceptionOrderCommand,
  type PaymentExceptionOrderAction,
  type PaymentExceptionOrderStatus,
  type PaymentExceptionOrder,
  type PaymentPageQuery,
} from '../../api/payment';

type TagType = '' | 'success' | 'warning' | 'info' | 'primary' | 'danger';

const handleStatuses = ref<PaymentExceptionOrderStatus[]>([]);
const handleActions = ref<PaymentExceptionOrderAction[]>([]);

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});
const rows = ref<PaymentExceptionOrder[]>([]);
const total = ref(0);
const loading = ref(false);
const errorMessage = ref('');
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentExceptionOrder>();
const helpVisible = ref(false);
const handleVisible = ref(false);
const handleSubmitting = ref(false);
const currentRow = ref<PaymentExceptionOrder>();
const handleFormRef = ref<FormInstance>();
const handleForm = reactive<HandlePaymentExceptionOrderCommand>({
  id: '',
  handleAction: '',
  handleReason: '',
  handleResult: '',
  handleEvidence: '',
});

const handleRules: FormRules<HandlePaymentExceptionOrderCommand> = {
  handleAction: [{ required: true, message: '请选择处理动作', trigger: 'change' }],
  handleReason: [{ required: true, message: '请输入处理原因', trigger: 'blur' }],
  handleResult: [{ required: true, message: '请输入处理结果', trigger: 'blur' }],
};

const emptyDescription = computed(() => {
  if (errorMessage.value) return '异常订单加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的异常订单' : '暂无异常订单';
});
const allowedHandleActions = computed(() => {
  const exceptionType = currentRow.value?.exceptionType;
  if (!exceptionType) return [];
  return handleActions.value.filter((action) => {
    if (!action.allowedExceptionTypes?.length) return true;
    return action.allowedExceptionTypes.includes(exceptionType);
  });
});
const selectedHandleActionDescription = computed(() => {
  const action = allowedHandleActions.value.find(item => item.actionCode === handleForm.handleAction);
  return action?.description || '';
});

onMounted(() => {
  void loadOptions();
  void loadRows();
});

async function loadOptions() {
  try {
    const [statuses, actions] = await Promise.all([
      paymentExceptionOrderApi.statuses(),
      paymentExceptionOrderApi.actions(),
    ]);
    handleStatuses.value = statuses;
    handleActions.value = actions;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '异常订单选项加载失败');
  }
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentExceptionOrderApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '异常订单加载失败';
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

async function openDetail(row: PaymentExceptionOrder) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentExceptionOrderApi.detail(row.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '异常订单详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function openHandle(row: PaymentExceptionOrder) {
  if (!row.id) return;
  currentRow.value = row;
  handleForm.id = row.id;
  handleForm.handleAction = '';
  handleForm.handleReason = '';
  handleForm.handleResult = '';
  handleForm.handleEvidence = '';
  handleVisible.value = true;
  if (allowedHandleActions.value.length === 1) {
    handleForm.handleAction = allowedHandleActions.value[0]?.actionCode || '';
  }
}

async function submitHandle() {
  if (!handleFormRef.value) return;
  const valid = await handleFormRef.value.validate().catch(() => false);
  if (!valid) return;
  handleSubmitting.value = true;
  try {
    await paymentExceptionOrderApi.handle({
      ...handleForm,
      handleEvidence: handleForm.handleEvidence || undefined,
    });
    ElMessage.success('异常订单已处理');
    handleVisible.value = false;
    await loadRows();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '异常订单处理失败');
  } finally {
    handleSubmitting.value = false;
  }
}

function canHandle(row: PaymentExceptionOrder) {
  return row.handleStatus === 'PENDING' || row.handleStatus === 'PROCESSING';
}

function severityTagType(severity?: string): TagType {
  if (severity === 'CRITICAL' || severity === 'HIGH') return 'danger';
  if (severity === 'MEDIUM') return 'warning';
  if (severity === 'LOW') return 'info';
  return '';
}

function statusTagType(status?: string): TagType {
  if (status === 'HANDLED' || status === 'CLOSED') return 'success';
  if (status === 'PROCESSING') return 'warning';
  if (status === 'IGNORED') return 'info';
  return '';
}

function valueText(value: unknown) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}
</script>

<style scoped>
.payment-exception-orders__detail-block {
  margin-top: 18px;
}

</style>
