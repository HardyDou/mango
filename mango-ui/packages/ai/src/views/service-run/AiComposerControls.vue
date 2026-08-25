<template>
  <div class="mango-ai-composer-controls" data-state="ai.service-run.next-turn-settings">
    <div class="mango-ai-composer-controls__model">
      <el-select
        :model-value="modelValue"
        class="mango-ai-composer-controls__model-select"
        size="small"
        placeholder="选择模型"
        aria-label="选择下一条消息使用的模型"
        data-field="ai.service-run.model"
        @update:model-value="modelChanged"
      >
        <el-option
          v-for="item in models"
          :key="item.modelId"
          :label="`${item.displayName} · ${item.providerDisplayName}`"
          :value="item.modelId"
        >
          <div class="mango-ai-workbench__model-option">
            <span>{{ item.displayName }}</span>
            <small>{{ item.providerDisplayName }} · 支持输入 {{ modalityLabels(item.inputModalities) }}</small>
          </div>
        </el-option>
      </el-select>
    </div>

    <el-tooltip :content="thinkingTooltip" placement="top">
      <div class="mango-ai-composer-controls__thinking">
        <el-switch
          :model-value="thinkingEnabled"
          size="small"
          aria-label="深度思考"
          data-field="ai.service-run.thinking"
          :disabled="!selectedModel?.thinkingConfigurable"
          @update:model-value="thinkingChanged"
        />
        <span><span class="mango-ai-composer-controls__thinking-prefix">深度</span>思考</span>
      </div>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import type { AiModality, AiServiceModelOption } from '@mango/ai-api';
import { computed } from 'vue';

defineOptions({ name: 'AiComposerControls' });
const props = defineProps<{
  models: AiServiceModelOption[];
  modelValue: string;
  thinkingEnabled: boolean;
  thinkingTooltip: string;
}>();
const emit = defineEmits<{
  'update:thinkingEnabled': [value: boolean];
  modelChange: [value: string];
}>();

const selectedModel = computed(() => props.models.find((item) => item.modelId === props.modelValue));

function modelChanged(value: string) {
  emit('modelChange', value);
}

function thinkingChanged(value: string | number | boolean) {
  emit('update:thinkingEnabled', Boolean(value));
}

function modalityLabels(values: AiModality[]) {
  const labels: Record<AiModality, string> = {
    TEXT: '文本',
    IMAGE: '图片',
    AUDIO: '音频',
    VIDEO: '视频',
    FILE: '文件',
    VECTOR: '向量',
  };
  return values.map((item) => labels[item]).join('、') || '无';
}
</script>
