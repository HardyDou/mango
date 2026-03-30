<template>
  <div class="right-toolbar">
    <el-tooltip content="刷新" placement="top">
      <el-icon class="toolbar-icon" @click="handleRefresh">
        <Refresh />
      </el-icon>
    </el-tooltip>
    <el-tooltip content="列设置" placement="top">
      <el-icon class="toolbar-icon" @click="handleColumnSetting">
        <Setting />
      </el-icon>
    </el-tooltip>
    <el-tooltip content="密度" placement="top">
      <el-dropdown trigger="click" @command="handleCommand">
        <el-icon class="toolbar-icon">
          <ScaleToOriginal />
        </el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="large" :class="{ active: size === 'large' }">大</el-dropdown-item>
            <el-dropdown-item command="default" :class="{ active: size === 'default' }">中</el-dropdown-item>
            <el-dropdown-item command="small" :class="{ active: size === 'small' }">小</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts" name="RightToolbar">
import { computed } from 'vue';
import { Refresh, Setting, ScaleToOriginal } from '@element-plus/icons-vue';
import { usePreferencesStore } from '@/stores/preferences';

const preferencesStore = usePreferencesStore();

const size = computed(() => preferencesStore.size);

const emit = defineEmits(['refresh', 'columnSetting', 'sizeChange']);

const handleRefresh = () => {
  emit('refresh');
};

const handleColumnSetting = () => {
  emit('columnSetting');
};

const handleCommand = (command: string) => {
  preferencesStore.size = command as 'large' | 'default' | 'small';
  emit('sizeChange', command);
};
</script>

<style scoped lang="scss">
.right-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;

  .toolbar-icon {
    padding: 4px 8px;
    font-size: 16px;
    color: var(--mango-text-color-regular);
    cursor: pointer;
    border-radius: 4px;
    transition: all 0.2s;

    &:hover {
      color: var(--mango-color-primary);
      background: var(--mango-color-menu-hover);
    }
  }
}

:deep(.el-dropdown-menu__item.active) {
  color: var(--mango-color-primary);
}
</style>
