<template>
  <div class="user-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增用户
          </el-button>
        </div>
      </template>
      <el-table
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="username"
          label="用户名"
        />
        <el-table-column
          prop="nickname"
          label="昵称"
        />
        <el-table-column
          prop="email"
          label="邮箱"
        />
        <el-table-column
          prop="phone"
          label="手机号"
        />
        <el-table-column
          prop="status"
          label="状态"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
        />
        <el-table-column
          label="操作"
          width="180"
        >
          <template #default>
            <el-button
              link
              type="primary"
              size="small"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="SystemUser">
import { ref } from 'vue';

const tableData = ref([
  {
    username: 'admin',
    nickname: '管理员',
    email: 'admin@example.com',
    phone: '13800138000',
    status: 1,
    createTime: '2024-01-01 10:00:00',
  },
]);

const handleAdd = () => {
  if (import.meta.env.DEV) console.log('add user');
};
</script>

<style scoped lang="scss">
.user-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
