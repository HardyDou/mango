<template>
  <div class="menu-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增菜单
          </el-button>
        </div>
      </template>
      <el-tree
        :data="treeData"
        :props="{ children: 'children', label: 'title' }"
        default-expand-all
      >
        <template #default="{ data }">
          <span class="tree-node">
            <span>{{ data.title }}</span>
            <span class="tree-actions">
              <el-button
                link
                type="primary"
                size="small"
              >编辑</el-button>
              <el-button
                link
                type="danger"
                size="small"
              >删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="SystemMenu">
import { ref } from 'vue';

const treeData = ref([
  {
    id: 1,
    title: '系统管理',
    children: [
      { id: 11, title: '用户管理' },
      { id: 12, title: '角色管理' },
      { id: 13, title: '菜单管理' },
    ],
  },
  {
    id: 2,
    title: '组件示例',
    children: [
      { id: 21, title: '图标' },
      { id: 22, title: '表格' },
    ],
  },
]);

const handleAdd = () => {
  console.log('add menu');
};
</script>

<style scoped lang="scss">
.menu-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 20px;
}

.tree-actions {
  display: flex;
  gap: 8px;
}
</style>
