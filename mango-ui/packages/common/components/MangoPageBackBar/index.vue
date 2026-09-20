<template>
  <section class="mango-page-back-bar">
    <div class="mango-page-back-bar__main">
      <el-button :icon="ArrowLeft" circle :aria-label="backLabel" @click="handleBack" />
      <span class="mango-page-back-bar__title">{{ title }}</span>
    </div>
    <div v-if="$slots.extra || showRefresh" class="mango-page-back-bar__extra">
      <slot name="extra" />
      <el-button
        v-if="showRefresh"
        plain
        :icon="Refresh"
        :loading="refreshLoading"
        :disabled="refreshLoading"
        data-action="detail.refresh"
        aria-label="刷新"
        @click="emit('refresh')"
      >
        刷新
      </el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { ArrowLeft, Refresh } from '@element-plus/icons-vue';
import { routerKey } from 'vue-router';
import type { MangoPageBackBarEmits, MangoPageBackBarProps } from './types';

defineOptions({ name: 'MangoPageBackBar' });

const props = withDefaults(defineProps<MangoPageBackBarProps>(), {
  backLabel: '返回',
  backTo: undefined,
  navigateOnBack: false,
  showRefresh: true,
  refreshLoading: false,
});
const emit = defineEmits<MangoPageBackBarEmits>();
const router = inject(routerKey, undefined);

async function handleBack() {
  emit('back');
  if (!props.navigateOnBack) return;
  if (props.backTo === undefined) {
    throw new Error('MangoPageBackBar: navigateOnBack 开启时必须提供 backTo');
  }
  if (!router) {
    throw new Error('MangoPageBackBar: navigateOnBack 开启时必须在应用中安装 Vue Router');
  }
  await router.push(props.backTo);
}
</script>

<style scoped>
.mango-page-back-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  padding: 15px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
  box-shadow: var(--mango-shadow-light);
}

.mango-page-back-bar__main,
.mango-page-back-bar__extra {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mango-page-back-bar__main {
  min-width: 0;
  min-height: 32px;
}

.mango-page-back-bar__extra {
  justify-content: flex-end;
  flex-wrap: wrap;
}

.mango-page-back-bar__title {
  min-width: 0;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
  overflow-wrap: anywhere;
}

.mango-page-back-bar :deep(.el-button.is-circle) {
  width: 26px;
  height: 26px;
  padding: 0;
  border: 0;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color);
}

@media (width <= 760px) {
  .mango-page-back-bar {
    align-items: flex-start;
  }

  .mango-page-back-bar__extra {
    align-items: flex-end;
    flex-direction: column;
  }
}
</style>
