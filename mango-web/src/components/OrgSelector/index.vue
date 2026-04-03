<template>
  <div class="org-selector">
    <!-- Trigger Button -->
    <el-select
      :model-value="displayValue"
      :placeholder="placeholderText"
      :disabled="disabled"
      :clearable="true"
      readonly
      @click="openDialog"
      @clear="handleClear"
    >
      <template #empty>
        <div class="org-select-empty">
          {{ t('orgSelector.noData') }}
        </div>
      </template>
    </el-select>

    <!-- Selected Tags -->
    <div
      v-if="selectedNames.length > 0"
      class="org-selected-tags"
    >
      <el-tag
        v-for="name in selectedNames"
        :key="name"
        closable
        size="small"
        @close="handleRemoveTag(name)"
      >
        {{ name }}
      </el-tag>
    </div>

    <!-- Selection Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :width="width"
      append-to-body
      destroy-on-close
    >
      <div class="org-tree-container">
        <div
          v-if="loading"
          class="org-loading"
        >
          {{ t('orgSelector.loading') }}
        </div>

        <div
          v-else-if="error"
          class="org-error"
        >
          {{ error }}
        </div>

        <div
          v-else-if="orgOptions.length === 0"
          class="org-empty"
        >
          {{ t('orgSelector.noData') }}
        </div>

        <el-tree
          v-else
          ref="treeRef"
          :data="orgOptions"
          :props="treeProps"
          :node-key="'id'"
          :default-expand-all="true"
          :expand-on-click-node="false"
          :check-strictly="false"
          :show-checkbox="true"
          :default-checked-keys="modelValue"
          @check="handleTreeCheck"
        />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeDialog">
            {{ t('orgSelector.cancel') }}
          </el-button>
          <el-button
            type="primary"
            @click="confirmSelection"
          >
            {{ t('orgSelector.confirm') }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { getOrgTree } from '@/api/admin/org';
import type { OrgNode, OrgSelectorProps, OrgSelectorEmits, OrgSelectorExpose } from './types';
import type { ElTree } from 'element-plus';

const props = withDefaults(
  defineProps<OrgSelectorProps>(),
  {
    modelValue: () => [],
    placeholder: 'orgSelector.placeholder',
    title: 'orgSelector.title',
    showTagNames: true,
    max: 0,
    disabled: false,
    width: '500px',
  }
);

const emit = defineEmits<OrgSelectorEmits>();

const { t } = useI18n();

const dialogVisible = ref(false);
const loading = ref(false);
const error = ref<string | null>(null);
const orgOptions = ref<OrgNode[]>([]);
const tempSelectedIds = ref<number[]>([]);
const allNodes = ref<Map<number, OrgNode>>(new Map());

const treeRef = ref<InstanceType<typeof ElTree>>();

const placeholderText = computed(() => {
  const key = props.placeholder;
  return key.includes('.') ? t(key) : key;
});

const dialogTitle = computed(() => {
  const key = props.title;
  return key.includes('.') ? t(key) : key;
});

const displayValue = computed(() => {
  if (props.modelValue.length === 0) return '';
  return `${props.modelValue.length} ${t('orgSelector.placeholder')}`;
});

const selectedNames = computed(() => {
  if (!props.showTagNames || props.modelValue.length === 0) return [];
  return props.modelValue
    .map((id) => allNodes.value.get(id)?.name)
    .filter(Boolean) as string[];
});

const treeProps = {
  children: 'children',
  label: 'name',
};

/**
 * Flatten tree to get all nodes
 */
function flattenTree(nodes: OrgNode[], map: Map<number, OrgNode>) {
  nodes.forEach((node) => {
    map.set(node.id, node);
    if (node.children && node.children.length > 0) {
      flattenTree(node.children, map);
    }
  });
}

/**
 * Load org tree data
 */
async function loadOrgTree() {
  loading.value = true;
  error.value = null;

  try {
    const data = await getOrgTree({ parentId: 0 });
    orgOptions.value = data || [];
    allNodes.value.clear();
    flattenTree(orgOptions.value, allNodes.value);
  } catch (err) {
    console.error('Failed to load org tree:', err);
    error.value = '加载组织数据失败';
    orgOptions.value = [];
  } finally {
    loading.value = false;
  }
}

function openDialog() {
  if (props.disabled) return;
  tempSelectedIds.value = [...props.modelValue];
  dialogVisible.value = true;
  loadOrgTree();
}

function closeDialog() {
  dialogVisible.value = false;
}

function confirmSelection() {
  const checkedKeys = treeRef.value?.getCheckedKeys() || [];
  tempSelectedIds.value = checkedKeys as number[];

  let finalValue = tempSelectedIds.value;
  if (props.max > 0 && finalValue.length > props.max) {
    finalValue = finalValue.slice(0, props.max);
  }

  emit('update:modelValue', finalValue);
  emit('change', finalValue);
  closeDialog();
}

function handleTreeCheck(_node: OrgNode, checked: { checkedKeys: number[]; halfCheckedKeys: number[] }) {
  let checkedKeys = checked.checkedKeys;

  if (props.max > 0 && checkedKeys.length > props.max) {
    // Exceeded max selection - revert
    treeRef.value?.setCheckedKeys(tempSelectedIds.value);
    return;
  }

  tempSelectedIds.value = checkedKeys;
}

function handleClear() {
  emit('update:modelValue', []);
  emit('change', []);
}

function handleRemoveTag(name: string) {
  const id = [...allNodes.value.entries()]
    .find(([, node]) => node.name === name)?.[0];
  if (id !== undefined) {
    const newValue = props.modelValue.filter((v) => v !== id);
    emit('update:modelValue', newValue);
    emit('change', newValue);
  }
}

function getValue(): number[] {
  return props.modelValue;
}

function clear() {
  handleClear();
}

// Watch for external modelValue changes
watch(
  () => props.modelValue,
  (newVal) => {
    if (Array.isArray(newVal) && newVal.length === 0) {
      tempSelectedIds.value = [];
    }
  }
);

// Expose methods
defineExpose<OrgSelectorExpose>({
  open: openDialog,
  close: closeDialog,
  getValue,
  clear,
});
</script>

<style scoped lang="scss">
.org-selector {
  width: 100%;
}

.org-selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.org-tree-container {
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
}

.org-loading,
.org-error,
.org-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: #909399;
}

.org-error {
  color: #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
