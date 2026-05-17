<template>
  <div class="operation-log-container">
    <el-card>
      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索操作人/操作描述"
            clearable
          />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input
            v-model="query.username"
            placeholder="请输入"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="请选择"
            clearable
          >
            <el-option
              v-for="item in operationStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="Number(item.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
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
      </el-form>

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button
            type="primary"
            @click="handleExport"
            disabled
          >
            导出
          </el-button>
          <el-button
            type="danger"
            @click="handleClean"
          >
            清理
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="username"
          label="操作人"
          width="120"
        />
        <el-table-column
          prop="operation"
          label="操作描述"
        />
        <el-table-column
          prop="requestMethod"
          label="请求方法"
          width="100"
        >
          <template #default="{ row }">
            <el-tag size="small">
              {{ row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="requestUrl"
          label="请求URL"
          show-overflow-tooltip
        />
        <el-table-column
          prop="costTime"
          label="耗时(ms)"
          width="100"
        />
        <el-table-column
          prop="ip"
          label="IP地址"
          width="140"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="sys_operation_status"
              :value="row.status"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="operateTime"
          label="操作时间"
          width="180"
        />
        <el-table-column
          label="操作"
          width="100"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleDetail(row)"
            >
              详情
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

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="操作详情"
      width="700px"
    >
      <el-descriptions
        :column="2"
        border
      >
        <el-descriptions-item label="操作人">
          {{ currentRow?.username }}
        </el-descriptions-item>
        <el-descriptions-item label="操作描述">
          {{ currentRow?.operation }}
        </el-descriptions-item>
        <el-descriptions-item label="请求方法">
          {{ currentRow?.requestMethod }}
        </el-descriptions-item>
        <el-descriptions-item label="处理器方法">
          {{ currentRow?.handlerMethod || '-' }}
        </el-descriptions-item>
        <el-descriptions-item
          label="请求URL"
          :span="2"
        >
          {{ currentRow?.requestUrl }}
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">
          {{ currentRow?.ip }}
        </el-descriptions-item>
        <el-descriptions-item label="地理位置">
          {{ currentRow?.location }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ currentRow?.costTime }}ms
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <DictTag
            dict-code="sys_operation_status"
            :value="currentRow?.status"
            size="small"
          />
        </el-descriptions-item>
        <el-descriptions-item
          label="操作时间"
          :span="2"
        >
          {{ currentRow?.operateTime }}
        </el-descriptions-item>
        <el-descriptions-item
          label="请求参数"
          :span="2"
        >
          <pre style="max-height: 200px; overflow: auto">{{ formatJson(currentRow?.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item
          label="请求体"
          :span="2"
        >
          <pre style="max-height: 200px; overflow: auto">{{ formatJson(currentRow?.requestBody) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item
          label="响应结果"
          :span="2"
        >
          <pre style="max-height: 200px; overflow: auto">{{ formatJson(currentRow?.responseResult) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentRow?.errorMsg"
          label="错误信息"
          :span="2"
        >
          <span style="color: #f56c6c">{{ currentRow?.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemOperationLog">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import DictTag from '@mango/common/components/DictTag/index.vue';
import Pagination from '@mango/common/components/Pagination/index.vue';
import { useDict } from '@mango/common/hooks/useDict';
import { operationLogApi, type SysOperationLog } from '../../api/log';

const { options: operationStatusOptions } = useDict('sys_operation_status');

const loading = ref(false);
const tableData = ref<SysOperationLog[]>([]);
const total = ref(0);
const dateRange = ref<string[]>([]);
const detailVisible = ref(false);
const currentRow = ref<SysOperationLog | null>(null);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  username: '',
  status: undefined as number | undefined,
  startTime: '',
  endTime: '',
});

async function loadData() {
  loading.value = true;
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      query.startTime = dateRange.value[0];
      query.endTime = dateRange.value[1];
    }
    const data = await operationLogApi.list(query);
    tableData.value = data.list;
    total.value = data.total;
  } catch (error) {
    console.error('加载数据失败:', error);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.keyword = '';
  query.username = '';
  query.status = undefined;
  dateRange.value = [];
  query.startTime = '';
  query.endTime = '';
  query.pageNum = 1;
  loadData();
}

function handleDetail(row: SysOperationLog) {
  currentRow.value = row;
  detailVisible.value = true;
}

function handleExport() {
  ElMessage.info('后端暂未提供操作日志导出接口');
}

function handleClean() {
  ElMessageBox.prompt('请输入保留天数', '清理日志', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^\d+$/,
    inputErrorMessage: '请输入数字',
  }).then(async ({ value }) => {
    try {
      await operationLogApi.clean(parseInt(value));
      ElMessage.success('清理成功');
      loadData();
    } catch (error) {
      console.error('清理失败:', error);
    }
  }).catch(() => {});
}

function formatJson(str: string | undefined): string {
  if (!str) return '';
  try {
    return JSON.stringify(JSON.parse(str), null, 2);
  } catch {
    return str;
  }
}

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.operation-log-container {
  padding: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 16px;
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
pre {
  margin: 0;
  font-size: 12px;
  background: var(--el-fill-color-light);
  padding: 8px;
  border-radius: 4px;
}
</style>
