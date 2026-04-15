<template>
  <div class="right-toolbar">
    <el-tooltip
      v-if="props.showRefresh"
      content="刷新"
      placement="top"
    >
      <el-icon
        class="toolbar-icon"
        @click="handleRefresh"
      >
        <Refresh />
      </el-icon>
    </el-tooltip>
    <el-tooltip
      v-if="props.showColumnSetting"
      content="列设置"
      placement="top"
    >
      <el-icon
        class="toolbar-icon"
        @click="handleColumnSetting"
      >
        <Setting />
      </el-icon>
    </el-tooltip>
    <el-tooltip
      v-if="props.showDensity"
      content="密度"
      placement="top"
    >
      <el-dropdown
        trigger="click"
        @command="handleCommand"
      >
        <el-icon class="toolbar-icon">
          <ScaleToOriginal />
        </el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              command="large"
              :class="{ active: props.size === 'large' }"
            >
              大
            </el-dropdown-item>
            <el-dropdown-item
              command="default"
              :class="{ active: props.size === 'default' }"
            >
              中
            </el-dropdown-item>
            <el-dropdown-item
              command="small"
              :class="{ active: props.size === 'small' }"
            >
              小
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts" name="RightToolbar">
import { Refresh, Setting, ScaleToOriginal } from '@element-plus/icons-vue';
type ToolbarSize = 'large' | 'default' | 'small';

const props = withDefaults(
  defineProps<{
    size?: ToolbarSize;
    showRefresh?: boolean;
    showColumnSetting?: boolean;
    showDensity?: boolean;
  }>(),
  {
    size: 'default',
    showRefresh: true,
    showColumnSetting: true,
    showDensity: true,
  }
);

const emit = defineEmits<{
  (e: 'refresh'): void;
  (e: 'columnSetting'): void;
  (e: 'sizeChange', value: ToolbarSize): void;
  (e: 'update:size', value: ToolbarSize): void;
}>();

const handleRefresh = () => {
  emit('refresh');
};

const handleColumnSetting = () => {
  emit('columnSetting');
};

const handleCommand = (command: string) => {
  const nextSize = command as ToolbarSize;
  emit('update:size', nextSize);
  emit('sizeChange', nextSize);
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
