<template>
  <div class="system-event-page">
    <el-card class="system-event-search">
      <el-form
        :model="query"
        :inline="true"
        class="system-event-search__form"
        @submit.prevent
      >
        <el-row :gutter="16">
          <el-col
            :xs="24"
            :sm="12"
            :lg="6"
          >
            <el-form-item label="关键词">
              <el-input
                v-model="query.keyword"
                placeholder="消息ID/类型/业务键"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
            :lg="6"
          >
            <el-form-item label="状态">
              <el-select
                v-model="query.status"
                placeholder="全部状态"
                clearable
              >
                <el-option
                  v-for="item in systemEventStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
            :lg="6"
          >
            <el-form-item label="事件类型">
              <el-input
                v-model="query.eventType"
                placeholder="eventType"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
            :lg="6"
          >
            <el-form-item label="业务类型">
              <el-input
                v-model="query.businessType"
                placeholder="businessType"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
            :lg="6"
          >
            <el-form-item label="业务键">
              <el-input
                v-model="query.businessKey"
                placeholder="businessKey"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
            :lg="6"
          >
            <el-form-item label="异常范围">
              <el-switch
                v-model="query.abnormalOnly"
                active-text="仅异常"
                inactive-text="全部"
              />
            </el-form-item>
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
            :lg="6"
          >
            <el-form-item>
              <el-button
                type="primary"
                @click="handleSearch"
              >
                查询
              </el-button>
              <el-button @click="handleReset">
                重置
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <div class="system-event-toolbar">
      <el-button
        plain
        :loading="loading"
        @click="loadData"
      >
        刷新
      </el-button>
    </div>

    <el-card class="system-event-table">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        :closable="false"
      >
        <template #default>
          <el-button
            link
            type="primary"
            @click="loadData"
          >
            重试
          </el-button>
        </template>
      </el-alert>
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        empty-text="暂无系统事件"
      >
        <el-table-column
          prop="status"
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="systemEventStatusTagType(row.status)"
              size="small"
            >
              {{ systemEventStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="eventType"
          label="事件类型"
          min-width="220"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.eventType || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="businessType"
          label="业务类型"
          min-width="160"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.businessType || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="businessKey"
          label="业务键"
          min-width="180"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.businessKey || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="attemptCount"
          label="次数"
          width="80"
        />
        <el-table-column
          prop="occurredAt"
          label="发生时间"
          width="180"
        >
          <template #default="{ row }">
            {{ row.occurredAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="nextAttemptAt"
          label="下次投递"
          width="180"
        >
          <template #default="{ row }">
            {{ row.nextAttemptAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="errorMessage"
          label="错误"
          min-width="220"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.errorMessage || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="handleDetail(row)"
            >
              详情
            </el-button>
            <el-button
              link
              type="primary"
              :disabled="row.status === 'SUCCESS'"
              @click="handleReconsume(row)"
            >
              重投
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadData"
      />
    </el-card>

    <el-dialog
      v-model="detailVisible"
      title="系统事件详情"
      width="820px"
      destroy-on-close
    >
      <el-skeleton
        v-if="detailLoading"
        :rows="8"
        animated
      />
      <template v-else>
        <el-descriptions
          :column="2"
          border
        >
          <el-descriptions-item label="状态">
            <el-tag
              :type="systemEventStatusTagType(currentEvent?.status)"
              size="small"
            >
              {{ systemEventStatusLabel(currentEvent?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="投递次数">
            {{ currentEvent?.attemptCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="事件类型">
            {{ currentEvent?.eventType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="业务类型">
            {{ currentEvent?.businessType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="业务键">
            {{ currentEvent?.businessKey || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="聚合ID">
            {{ currentEvent?.aggregateId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发生时间">
            {{ currentEvent?.occurredAt || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="下次投递">
            {{ currentEvent?.nextAttemptAt || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="锁定Worker">
            {{ currentEvent?.lockedBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="锁定时间">
            {{ currentEvent?.lockedAt || '-' }}
          </el-descriptions-item>
          <el-descriptions-item
            label="消息ID"
            :span="2"
          >
            {{ currentEvent?.messageId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item
            v-if="currentEvent?.errorMessage"
            label="错误信息"
            :span="2"
          >
            <el-text type="danger">
              {{ currentEvent.errorMessage }}
            </el-text>
          </el-descriptions-item>
        </el-descriptions>
        <div class="system-event-detail__block">
          <div class="system-event-detail__label">事件头</div>
          <pre>{{ formatJson(currentEvent?.headers) }}</pre>
        </div>
        <div class="system-event-detail__block">
          <div class="system-event-detail__label">事件载荷</div>
          <pre>{{ formatJson(currentEvent?.payload) }}</pre>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
        <el-button
          type="primary"
          :disabled="currentEvent?.status === 'SUCCESS'"
          :loading="reconsumeLoading"
          @click="currentEvent && handleReconsume(currentEvent)"
        >
          重新投递
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemEvent">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Pagination } from '@mango/common';
import {
  requestErrorMessage,
  systemEventApi,
  systemEventStatusLabel,
  systemEventStatusOptions,
  systemEventStatusTagType,
  type SystemEvent,
  type SystemEventQuery,
} from '../../api/event';

const loading = ref(false);
const detailLoading = ref(false);
const reconsumeLoading = ref(false);
const errorMessage = ref('');
const tableData = ref<SystemEvent[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const currentEvent = ref<SystemEvent | null>(null);

const query = reactive<SystemEventQuery>({
  pageNum: 1,
  pageSize: 20,
  status: '',
  eventType: '',
  businessType: '',
  businessKey: '',
  keyword: '',
  abnormalOnly: true,
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const page = await systemEventApi.page(query);
    tableData.value = page.list;
    total.value = page.total;
  } catch (error) {
    errorMessage.value = requestErrorMessage(error, '系统事件加载失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.pageNum = 1;
  query.pageSize = 20;
  query.status = '';
  query.eventType = '';
  query.businessType = '';
  query.businessKey = '';
  query.keyword = '';
  query.abnormalOnly = true;
  loadData();
}

async function handleDetail(row: SystemEvent) {
  detailVisible.value = true;
  detailLoading.value = true;
  currentEvent.value = row;
  try {
    currentEvent.value = await systemEventApi.detail(row.messageId);
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '系统事件详情加载失败'));
  } finally {
    detailLoading.value = false;
  }
}

async function handleReconsume(row: SystemEvent) {
  await ElMessageBox.confirm(
    `确认将事件 ${row.businessKey || row.messageId} 放回待投递队列？`,
    '重新投递系统事件',
    {
      confirmButtonText: '确认重投',
      cancelButtonText: '取消',
      type: 'warning',
    },
  );
  reconsumeLoading.value = true;
  try {
    await systemEventApi.reconsume(row.messageId);
    ElMessage.success('已放回待投递队列');
    await loadData();
    if (detailVisible.value) {
      currentEvent.value = await systemEventApi.detail(row.messageId);
    }
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '重新投递失败'));
  } finally {
    reconsumeLoading.value = false;
  }
}

function formatJson(value: unknown): string {
  if (!value) {
    return '{}';
  }
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

onMounted(loadData);
</script>

<style scoped lang="scss">
.system-event-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.system-event-search__form {
  :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
    margin-bottom: 12px;
  }

  :deep(.el-select),
  :deep(.el-input) {
    width: 100%;
  }
}

.system-event-toolbar {
  display: flex;
  justify-content: flex-end;
}

.system-event-table {
  :deep(.el-alert) {
    margin-bottom: 12px;
  }
}

.system-event-detail__block {
  margin-top: 16px;
}

.system-event-detail__label {
  margin-bottom: 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

pre {
  max-height: 260px;
  margin: 0;
  overflow: auto;
  padding: 12px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

</style>
