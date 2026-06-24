<template>
  <div class="workflow-task-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ title }}</span>
          <el-tag type="info">{{ description }}</el-tag>
        </div>
      </template>

      <el-tabs
        v-if="taskMode === 'todo'"
        v-model="todoTab"
        class="todo-tabs"
        @tab-change="handleTodoTabChange"
      >
        <el-tab-pane label="待处理" name="assigned" />
        <el-tab-pane label="待领取" name="claimable" />
      </el-tabs>

      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索流程/任务名称" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="taskName" label="任务名称" min-width="160" />
        <el-table-column prop="businessKey" label="业务单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="processName" label="流程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="processKey" label="流程编码" min-width="180" show-overflow-tooltip />
        <el-table-column prop="initiatorName" label="发起人" width="120" />
        <el-table-column prop="assigneeName" label="办理人" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="startTime" label="发起时间" width="180" />
        <el-table-column prop="endTime" label="完成时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="taskMode === 'todo'">
              <el-button type="primary" link @click="openTask(row)">
                {{ row.claimable ? '详情' : '处理' }}
              </el-button>
              <el-button v-if="row.claimable" type="primary" link @click="claimTask(row)">领取</el-button>
            </template>
            <el-button v-else-if="taskMode === 'copied' && row.status !== '已阅'" type="primary" link @click="readCopied(row)">已阅</el-button>
            <el-button v-else type="primary" link @click="openTask(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        class="pagination"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { workflowApi, type WorkflowPageQuery, type WorkflowTask } from '../../api/workflow';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const tableData = ref<WorkflowTask[]>([]);
const total = ref(0);
const todoTab = ref<'assigned' | 'claimable'>('assigned');
const query = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
});

const taskMode = computed(() => {
  if (route.path.includes('/initiated')) return 'initiated';
  if (route.path.includes('/done')) return 'done';
  if (route.path.includes('/copied')) return 'copied';
  return 'todo';
});

const title = computed(() => ({
  todo: '我的待办',
  initiated: '我的申请',
  done: '我的已办',
  copied: '抄送给我',
}[taskMode.value]));

const description = computed(() => ({
  todo: todoTab.value === 'claimable' ? '候选待领取任务，认领后进入待处理' : '已经分配给当前用户的流程任务',
  initiated: '当前用户发起的流程实例',
  done: '当前用户已经处理完成的流程任务',
  copied: '流程抄送通知与待阅事项',
}[taskMode.value]));

const todoType = computed(() => (todoTab.value === 'claimable' ? 'CLAIMABLE' : 'ASSIGNED'));

watch(
  () => route.fullPath,
  () => {
    query.value.pageNum = 1;
    syncTodoTabFromQuery(route.query.todoType);
    loadData();
  },
);

async function loadData() {
  loading.value = true;
  try {
    if (taskMode.value === 'initiated') {
      const result = await workflowApi.initiatedProcesses(query.value);
      tableData.value = result.list.map(item => ({
        ...item,
        id: item.processInstanceId,
        taskName: '流程实例',
        createTime: item.startTime,
      }));
      total.value = result.total;
      return;
    }
    const apiMap = {
      todo: workflowApi.todoTasks,
      done: workflowApi.doneTasks,
      copied: workflowApi.copiedTasks,
    };
    const params = buildTaskQueryParams();
    const result = await apiMap[taskMode.value as 'todo' | 'done' | 'copied'](params);
    tableData.value = result.list;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.value.keyword = '';
  query.value.pageNum = 1;
  loadData();
}

function handleTodoTabChange() {
  query.value.pageNum = 1;
  loadData();
}

function syncTodoTabFromQuery(todoType: unknown): void {
  if (taskMode.value !== 'todo') {
    return;
  }
  todoTab.value = todoType === 'CLAIMABLE' ? 'claimable' : 'assigned';
}

function buildTaskQueryParams(): WorkflowPageQuery {
  if (taskMode.value === 'todo') {
    return {
      ...query.value,
      todoType: todoType.value,
      overdue: route.query.overdue === 'true',
    };
  }
  if (taskMode.value === 'copied') {
    return { ...query.value, unread: route.query.unread === 'true' };
  }
  return query.value;
}

function openTask(row: WorkflowTask) {
  const queryParams: Record<string, string> = {
    from: taskMode.value,
  };
  if (taskMode.value === 'todo' && row.id) {
    queryParams.taskId = row.id;
  } else if (row.processInstanceId) {
    queryParams.processInstanceId = row.processInstanceId;
    queryParams.mode = 'view';
  }
  router.push({
    path: '/workflow/task/detail',
    query: queryParams,
  });
}

async function claimTask(row: WorkflowTask) {
  if (!row.id) {
    return;
  }
  await ElMessageBox.confirm('确认领取当前任务？', '领取任务', { type: 'warning' });
  await workflowApi.claimTask(row.id);
  ElMessage.success('领取成功');
  await loadData();
}

async function readCopied(row: WorkflowTask) {
  await workflowApi.readCopiedTask(row.id);
  ElMessage.success('已标记为已阅');
  await loadData();
}

onMounted(() => {
  syncTodoTabFromQuery(route.query.todoType);
  loadData();
});
</script>

<style scoped>
.workflow-task-page {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.search-form {
  margin-bottom: 16px;
}

.todo-tabs {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
