<template>
  <div class="payment-table__actions payment-row-actions">
    <template v-for="action in directActions" :key="action.key">
      <el-tooltip
        v-if="action.tooltip"
        :content="action.tooltip"
        :disabled="action.tooltipDisabled ?? !action.disabled"
        placement="top"
      >
        <span class="payment-row-actions__item">
          <el-button
            link
            :type="action.type || 'primary'"
            :icon="action.icon"
            :disabled="action.disabled"
            :loading="action.loading"
            @click="runAction(action)"
          >
            {{ action.label }}
          </el-button>
        </span>
      </el-tooltip>
      <span v-else class="payment-row-actions__item">
        <el-button
          link
          :type="action.type || 'primary'"
          :icon="action.icon"
          :disabled="action.disabled"
          :loading="action.loading"
          @click="runAction(action)"
        >
          {{ action.label }}
        </el-button>
      </span>
    </template>

    <el-dropdown v-if="moreActions.length" trigger="click" @command="runMoreAction">
      <el-button link type="primary">更多</el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            v-for="action in moreActions"
            :key="action.key"
            :command="action.key"
            :disabled="action.disabled || action.loading"
          >
            <el-icon v-if="action.icon" class="payment-row-actions__more-icon">
              <component :is="action.icon" />
            </el-icon>
            <span>{{ action.label }}</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { PaymentRowAction } from './PaymentRowActions';

const props = withDefaults(defineProps<{
  actions: PaymentRowAction[];
  maxDirect?: number;
}>(), {
  maxDirect: 3,
});

const visibleActions = computed(() => props.actions.filter(action => action.visible !== false));
const directLimit = computed(() => (visibleActions.value.length > props.maxDirect ? Math.max(props.maxDirect - 1, 1) : props.maxDirect));
const directActions = computed(() => visibleActions.value.slice(0, directLimit.value));
const moreActions = computed(() => visibleActions.value.slice(directLimit.value));

function runAction(action: PaymentRowAction) {
  if (action.disabled || action.loading) {
    return;
  }
  void action.onClick();
}

function runMoreAction(key: string | number | object) {
  const action = moreActions.value.find(item => item.key === String(key));
  if (action) {
    runAction(action);
  }
}
</script>
