<template>
  <div class="icon-container">
    <el-card>
      <template #header>
        <span>图标示例</span>
      </template>
      <div class="icon-grid">
        <div v-for="icon in icons" :key="icon" class="icon-item" @click="handleCopy(icon)">
          <el-icon :size="32">
            <component :is="icon" />
          </el-icon>
          <span class="icon-name">{{ icon }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="ComponentsIcon">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  User,
  Lock,
  Setting,
  Document,
  List,
  Grid,
  Menu,
  TrendCharts,
  DataLine,
  Refresh,
  Plus,
  Delete,
  Edit,
  Search,
  View,
  More,
} from '@element-plus/icons-vue';

const icons = ref([
  'User',
  'Lock',
  'Setting',
  'Document',
  'List',
  'Grid',
  'Menu',
  'TrendCharts',
  'DataLine',
  'Refresh',
  'Plus',
  'Delete',
  'Edit',
  'Search',
  'View',
  'More',
]);

const handleCopy = (icon: string) => {
  navigator.clipboard.writeText(`<el-icon><${icon} /></el-icon>`);
  ElMessage.success(`已复制: ${icon}`);
};
</script>

<style scoped lang="scss">
.icon-container {
  padding: 20px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border: 1px solid var(--mango-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--mango-color-primary);
    background: var(--mango-color-menu-hover);
  }

  .icon-name {
    margin-top: 8px;
    font-size: 12px;
    color: var(--mango-text-color-regular);
  }
}
</style>
