<template>
  <div class="login-log-container">
    <el-card>
      <!-- 统计卡片 -->
      <el-row
        :gutter="20"
        class="stat-cards"
      >
        <el-col :span="6">
          <el-statistic
            title="总登录数"
            :value="statistics.totalCount"
          />
        </el-col>
        <el-col :span="6">
          <el-statistic
            title="成功"
            :value="statistics.successCount"
          />
        </el-col>
        <el-col :span="6">
          <el-statistic
            title="失败"
            :value="statistics.failCount"
          />
        </el-col>
        <el-col :span="6">
          <el-statistic
            title="今日登录"
            :value="statistics.todayCount"
          />
        </el-col>
      </el-row>

      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索用户名/IP"
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
              v-for="item in loginStatusOptions"
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
            type="danger"
            @click="handleClean"
          >
            清理日志
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
          label="用户名"
          width="120"
        />
        <el-table-column
          prop="ip"
          label="IP地址"
          width="140"
        />
        <el-table-column
          prop="location"
          label="登录地点"
        />
        <el-table-column
          prop="userAgent"
          label="浏览器"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="sys_login_status"
              :value="row.status"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="msg"
          label="消息"
          show-overflow-tooltip
        />
        <el-table-column
          prop="loginTime"
          label="登录时间"
          width="180"
        />
      </el-table>

      <Pagination
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts" name="SystemLoginLog">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import DictTag from '@mango/common/components/DictTag/index.vue';
import Pagination from '@mango/common/components/Pagination/index.vue';
import { useDict } from '@mango/common/hooks/useDict';
import { loginLogApi, type SysLoginLog, type LoginStatistics } from '../../api/log';

const { options: loginStatusOptions } = useDict('sys_login_status');

const loading = ref(false);
const tableData = ref<SysLoginLog[]>([]);
const total = ref(0);
const dateRange = ref<string[]>([]);
const statistics = reactive<LoginStatistics>({
  totalCount: 0,
  successCount: 0,
  failCount: 0,
  todayCount: 0,
});

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
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
    const data = await loginLogApi.list(query);
    tableData.value = data.list;
    total.value = data.total;
  } catch (error) {
    console.error('加载数据失败:', error);
  } finally {
    loading.value = false;
  }
}

async function loadStatistics() {
  try {
    const data = await loginLogApi.statistics({
      startTime: query.startTime,
      endTime: query.endTime,
    });
    Object.assign(statistics, data);
  } catch (error) {
    console.error('加载统计失败:', error);
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
  loadStatistics();
}

function handleReset() {
  query.keyword = '';
  query.status = undefined;
  dateRange.value = [];
  query.startTime = '';
  query.endTime = '';
  query.pageNum = 1;
  loadData();
  loadStatistics();
}

function handleClean() {
  ElMessageBox.prompt('请输入保留天数', '清理日志', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^\d+$/,
    inputErrorMessage: '请输入数字',
  }).then(async ({ value }) => {
    try {
      await loginLogApi.clean(parseInt(value));
      ElMessage.success('清理成功');
      loadData();
      loadStatistics();
    } catch (error) {
      console.error('清理失败:', error);
    }
  }).catch(() => {});
}

onMounted(() => {
  loadData();
  loadStatistics();
});
</script>

<style scoped lang="scss">
.login-log-container {
  padding: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-cards {
  margin-bottom: 20px;
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
.search-form {
  margin-bottom: 16px;
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
</style>
