<template>
  <div class="payment-notification-records">
    <section class="payment-notification-records__header">
      <div>
        <h3>通知记录</h3>
        <p>查看业务回调通知结果、失败重试和人工补偿推送记录。</p>
      </div>
    </section>

    <section class="payment-notification-records__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="通知单号 / 关联订单号 / 类型 / 状态"
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
              v-for="status in notifyStatuses"
              :key="status.statusCode"
              :label="status.statusName"
              :value="status.statusCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button
            type="primary"
            plain
            :icon="RefreshRight"
            :loading="deliverDueSubmitting"
            @click="deliverDueNotifications"
          >
            投递到期通知
          </el-button>
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
      class="payment-notification-records__table"
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column prop="notificationNo" label="通知单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="relatedOrderNo" label="关联订单号" min-width="190" show-overflow-tooltip />
      <el-table-column label="通知类型" width="150">
        <template #default="{ row }">
          <span>{{ row.notificationTypeName || row.notificationType || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="通知状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.notifyStatus)" effect="light">{{ row.notifyStatusName || row.notifyStatus || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetUrl" label="目标地址" min-width="280" show-overflow-tooltip />
      <el-table-column prop="retryTimes" label="重试次数" width="100" />
      <el-table-column prop="nextRetryTime" label="下次重试时间" width="170" show-overflow-tooltip />
      <el-table-column prop="responseCode" label="响应码" width="120" show-overflow-tooltip />
      <el-table-column prop="responseMessage" label="响应信息" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="156" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions">
            <el-button link type="primary" :icon="Tickets" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="canRetry(row)"
              link
              type="primary"
              :icon="RefreshRight"
              @click="openRetry(row)"
            >
              重推
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="payment-notification-records__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      title="通知记录详情"
      size="760px"
      destroy-on-close
      append-to-body
      class="payment-notification-records__drawer"
    >
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <template v-else-if="detail">
        <el-descriptions title="通知信息" :column="2" border>
          <el-descriptions-item label="通知单号">{{ valueText(detail.notificationNo) }}</el-descriptions-item>
          <el-descriptions-item label="关联订单号">{{ valueText(detail.relatedOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="通知类型">
            {{ detail.notificationTypeName || detail.notificationType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="通知状态">
            <el-tag :type="statusTagType(detail.notifyStatus)" effect="light">{{ detail.notifyStatusName || detail.notifyStatus || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="目标地址" :span="2">{{ valueText(detail.targetUrl) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="响应与重试" :column="2" border class="payment-notification-records__detail-block">
          <el-descriptions-item label="重试次数">{{ valueText(detail.retryTimes) }}</el-descriptions-item>
          <el-descriptions-item label="下次重试时间">{{ valueText(detail.nextRetryTime) }}</el-descriptions-item>
          <el-descriptions-item label="响应码">{{ valueText(detail.responseCode) }}</el-descriptions-item>
          <el-descriptions-item label="响应信息">{{ valueText(detail.responseMessage) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="人工补偿" :column="2" border class="payment-notification-records__detail-block">
          <el-descriptions-item label="重推人">{{ valueText(detail.lastManualRetryOperatorName) }}</el-descriptions-item>
          <el-descriptions-item label="重推时间">{{ valueText(detail.lastManualRetryTime) }}</el-descriptions-item>
          <el-descriptions-item label="重推原因" :span="2">{{ valueText(detail.lastManualRetryReason) }}</el-descriptions-item>
          <el-descriptions-item label="重推结果" :span="2">{{ valueText(detail.lastManualRetryResult) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间信息" :column="2" border class="payment-notification-records__detail-block">
          <el-descriptions-item label="创建时间">{{ valueText(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ valueText(detail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="未查询到通知记录详情" />
    </el-drawer>

    <el-dialog
      v-model="retryVisible"
      title="人工重推通知"
      width="600px"
      destroy-on-close
      append-to-body
    >
      <el-form ref="retryFormRef" :model="retryForm" :rules="retryRules" label-width="96px" class="payment-dialog-form">
        <el-form-item label="通知单号">
          <el-input :model-value="currentRow?.notificationNo || '-'" disabled />
        </el-form-item>
        <el-form-item label="关联订单">
          <el-input :model-value="currentRow?.relatedOrderNo || '-'" disabled />
        </el-form-item>
        <el-form-item label="重推原因" prop="retryReason">
          <el-input
            v-model="retryForm.retryReason"
            type="textarea"
            :rows="4"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="retryVisible = false">取消</el-button>
        <el-button type="primary" :loading="retrySubmitting" @click="submitRetry">确认重推</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Refresh, RefreshRight, Search, Tickets } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import Pagination from '@mango/common/components/Pagination/index.vue';
import {
  paymentNotificationRecordApi,
  type PaymentNotificationRecord,
  type PaymentNotificationStatus,
  type PaymentPageQuery,
  type RetryPaymentNotificationRecordCommand,
} from '../../api/payment';

type TagType = '' | 'success' | 'warning' | 'info' | 'primary' | 'danger';

const notifyStatuses = ref<PaymentNotificationStatus[]>([]);
const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});
const rows = ref<PaymentNotificationRecord[]>([]);
const total = ref(0);
const loading = ref(false);
const errorMessage = ref('');
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentNotificationRecord>();
const retryVisible = ref(false);
const retrySubmitting = ref(false);
const deliverDueSubmitting = ref(false);
const currentRow = ref<PaymentNotificationRecord>();
const retryFormRef = ref<FormInstance>();
const retryForm = reactive<RetryPaymentNotificationRecordCommand>({
  id: '',
  retryReason: '',
});

const retryRules: FormRules<RetryPaymentNotificationRecordCommand> = {
  retryReason: [{ required: true, message: '请输入重推原因', trigger: 'blur' }],
};

const emptyDescription = computed(() => {
  if (errorMessage.value) return '通知记录加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的通知记录' : '暂无通知记录';
});

onMounted(() => {
  void loadOptions();
  void loadRows();
});

async function loadOptions() {
  try {
    notifyStatuses.value = await paymentNotificationRecordApi.statuses();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '通知状态加载失败');
  }
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentNotificationRecordApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '通知记录加载失败';
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

async function openDetail(row: PaymentNotificationRecord) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentNotificationRecordApi.detail(row.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '通知记录详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

function openRetry(row: PaymentNotificationRecord) {
  if (!row.id) return;
  currentRow.value = row;
  retryForm.id = row.id;
  retryForm.retryReason = '';
  retryVisible.value = true;
}

async function submitRetry() {
  if (!retryFormRef.value) return;
  const valid = await retryFormRef.value.validate().catch(() => false);
  if (!valid) return;
  retrySubmitting.value = true;
  try {
    await paymentNotificationRecordApi.retry(retryForm);
    ElMessage.success('通知重推已登记');
    retryVisible.value = false;
    await loadRows();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '通知重推失败');
  } finally {
    retrySubmitting.value = false;
  }
}

async function deliverDueNotifications() {
  try {
    await ElMessageBox.confirm(
      '系统将投递当前租户已到计划时间的支付或退款通知记录，不会修改资金状态。',
      '投递到期通知',
      {
        confirmButtonText: '确认投递',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
  } catch {
    return;
  }
  deliverDueSubmitting.value = true;
  try {
    const delivered = await paymentNotificationRecordApi.deliverDue({ limit: 20 });
    ElMessage.success(delivered > 0 ? `已投递 ${delivered} 条到期通知` : '暂无到期通知需要投递');
    await loadRows();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '到期通知投递失败');
  } finally {
    deliverDueSubmitting.value = false;
  }
}

function canRetry(row: PaymentNotificationRecord) {
  return row.notifyStatus === 'FAILED' || row.notifyStatus === 'RETRYING' || row.notifyStatus === 'PENDING';
}

function statusTagType(status?: string): TagType {
  if (status === 'SUCCESS') return 'success';
  if (status === 'RETRYING' || status === 'PENDING') return 'warning';
  if (status === 'FAILED') return 'danger';
  return '';
}

function valueText(value: unknown) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}
</script>

<style scoped>
.payment-notification-records__detail-block {
  margin-top: 18px;
}

</style>
