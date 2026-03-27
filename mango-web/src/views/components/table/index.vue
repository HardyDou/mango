<template>
  <div class="table-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>表格示例</span>
          <el-button type="primary" @click="handleRefresh">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe border>
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="ComponentsTable">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const loading = ref(false);
const tableData = ref<any[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const getTableData = () => {
  loading.value = true;
  setTimeout(() => {
    tableData.value = Array.from({ length: pageSize.value }, (_, i) => ({
      id: (currentPage.value - 1) * pageSize.value + i + 1,
      name: ['张三', '李四', '王五', '赵六', '钱七'][i % 5],
      age: 20 + (i % 30),
      email: `user${i}@example.com`,
      status: i % 3 === 0 ? 1 : 0,
      createTime: '2024-01-01 10:00:00',
    }));
    total.value = 100;
    loading.value = false;
  }, 500);
};

const handleRefresh = () => {
  getTableData();
  ElMessage.success('刷新成功');
};

const handleEdit = (row: any) => {
  console.log('edit', row);
  ElMessage.info(`编辑: ${row.name}`);
};

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除用户 ${row.name}？`, '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    ElMessage.success('删除成功');
    getTableData();
  });
};

const handleSizeChange = () => {
  currentPage.value = 1;
  getTableData();
};

const handleCurrentChange = () => {
  getTableData();
};

onMounted(() => {
  getTableData();
});
</script>

<style scoped lang="scss">
.table-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
