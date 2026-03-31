<template>
  <div class="role-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增角色
          </el-button>
        </div>
      </template>
      <el-table
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="roleName"
          label="角色名称"
        />
        <el-table-column
          prop="roleCode"
          label="角色编码"
        />
        <el-table-column
          prop="description"
          label="描述"
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

<script setup lang="ts" name="SystemRole">
import { ref } from 'vue';

const tableData = ref([
  {
    roleName: '超级管理员',
    roleCode: 'admin',
    description: '系统超级管理员，拥有所有权限',
    status: 1,
    createTime: '2024-01-01 10:00:00',
  },
  {
    roleName: '普通用户',
    roleCode: 'user',
    description: '普通用户，拥有基本访问权限',
    status: 1,
    createTime: '2024-01-01 10:00:00',
  },
]);

const handleAdd = () => {
  console.log('add role');
};
</script>

<style scoped lang="scss">
.role-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
