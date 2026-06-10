<template>
  <div class="channel-capability-editor">
    <div class="channel-capability-editor__toolbar">
      <el-button :icon="Plus" @click="addRow">新增能力</el-button>
    </div>

    <el-table
      :data="rows"
      size="small"
      empty-text="尚未声明通道能力"
      class="channel-capability-editor__table"
    >
      <el-table-column label="支付方式" min-width="180">
        <template #default="{ row }">
          <PaymentEntityValueSelect
            v-model="row.methodCode"
            :api="paymentMethodApi"
            value-field="methodCode"
            label-field="methodName"
            description-field="methodCode"
            single
            placeholder="选择标准支付方式"
          />
        </template>
      </el-table-column>
      <el-table-column label="终端" width="110">
        <template #default="{ row }">
          <el-select v-model="row.terminalType" placeholder="终端">
            <el-option label="Web/PC" value="WEB" />
            <el-option label="H5" value="H5" />
            <el-option label="App" value="APP" />
            <el-option label="小程序" value="MP" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="接入场景" width="128">
        <template #default="{ row }">
          <el-select v-model="row.environment" placeholder="接入场景">
            <el-option label="芒果支付" value="MANGO_PAY" />
            <el-option label="生产" value="PROD" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="退款" width="76" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.supportsRefund !== 0"
            @update:model-value="value => row.supportsRefund = value ? 1 : 0"
          />
        </template>
      </el-table-column>
      <el-table-column label="查单" width="76" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.supportsQuery !== 0"
            @update:model-value="value => row.supportsQuery = value ? 1 : 0"
          />
        </template>
      </el-table-column>
      <el-table-column label="关单" width="76" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.supportsClose !== 0"
            @update:model-value="value => row.supportsClose = value ? 1 : 0"
          />
        </template>
      </el-table-column>
      <el-table-column label="账单" width="76" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.supportsBill !== 0"
            @update:model-value="value => row.supportsBill = value ? 1 : 0"
          />
        </template>
      </el-table-column>
      <el-table-column label="对账" width="76" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.supportsReconcile !== 0"
            @update:model-value="value => row.supportsReconcile = value ? 1 : 0"
          />
        </template>
      </el-table-column>
      <el-table-column label="最小金额（元）" width="140">
        <template #default="{ row }">
          <el-input-number
            :model-value="centsToYuan(row.minAmount)"
            :min="0"
            :precision="2"
            :step="1"
            controls-position="right"
            @update:model-value="value => row.minAmount = value === undefined || value === null ? undefined : yuanToCents(value)"
          />
        </template>
      </el-table-column>
      <el-table-column label="最大金额（元）" width="140">
        <template #default="{ row }">
          <el-input-number
            :model-value="centsToYuan(row.maxAmount)"
            :min="0"
            :precision="2"
            :step="1"
            controls-position="right"
            @update:model-value="value => row.maxAmount = value === undefined || value === null ? undefined : yuanToCents(value)"
          />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status !== 0"
            active-text="启用"
            inactive-text="停用"
            inline-prompt
            @update:model-value="value => row.status = value ? 1 : 0"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="76" fixed="right">
        <template #default="{ $index }">
          <el-button :icon="Delete" link type="danger" @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Delete, Plus } from '@element-plus/icons-vue';
import PaymentEntityValueSelect from './PaymentEntityValueSelect.vue';
import { paymentMethodApi, type PaymentChannelCapability } from '../api/payment';

const props = defineProps<{
  modelValue?: PaymentChannelCapability[];
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: PaymentChannelCapability[]): void;
}>();

const rows = computed({
  get: () => props.modelValue || [],
  set: value => emit('update:modelValue', value),
});

function addRow() {
  rows.value = [
    ...rows.value,
    {
      methodCode: '',
      terminalType: 'WEB',
      environment: 'MANGO_PAY',
      supportsRefund: 1,
      supportsQuery: 1,
      supportsClose: 1,
      supportsBill: 1,
      supportsReconcile: 1,
      status: 1,
    },
  ];
}

function removeRow(index: number) {
  rows.value = rows.value.filter((_item, currentIndex) => currentIndex !== index);
}

function centsToYuan(value?: number | null) {
  if (value === undefined || value === null) return undefined;
  return Number((Number(value) / 100).toFixed(2));
}

function yuanToCents(value: number) {
  return Math.round(Number(value || 0) * 100);
}
</script>

<style scoped>
.channel-capability-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.channel-capability-editor__toolbar {
  display: flex;
  justify-content: flex-start;
}

.channel-capability-editor__table {
  width: 100%;
}

.channel-capability-editor :deep(.el-input-number),
.channel-capability-editor :deep(.el-select) {
  width: 100%;
}
</style>
