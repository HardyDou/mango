<template>
  <div class="captcha-selector">
    <el-tabs
      v-model="currentType"
      class="captcha-tabs"
      @tab-change="handleTabChange"
    >
      <el-tab-pane
        label="算术"
        name="ARITHMETIC"
      >
        <ArithmeticCaptcha
          ref="arithmeticRef"
          @success="onSuccess"
          @refresh="onRefresh"
        />
      </el-tab-pane>
      <el-tab-pane
        label="滑块"
        name="CANVAS_SLIDER"
      >
        <CanvasSliderCaptcha
          ref="canvasSliderRef"
          @success="onSuccess"
          @refresh="onRefresh"
        />
      </el-tab-pane>
      <el-tab-pane
        label="短信"
        name="SMS"
      >
        <SmsCaptcha
          ref="smsRef"
          @success="onSuccess"
        />
      </el-tab-pane>
      <el-tab-pane
        label="邮件"
        name="EMAIL"
      >
        <EmailCaptcha
          ref="emailRef"
          @success="onSuccess"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="CaptchaSelector">
import { ref } from 'vue';
import ArithmeticCaptcha from './ArithmeticCaptcha.vue';
import CanvasSliderCaptcha from './CanvasSliderCaptcha.vue';
import SmsCaptcha from './SmsCaptcha.vue';
import EmailCaptcha from './EmailCaptcha.vue';
import { CaptchaType } from '@/api/admin/captcha';

const emit = defineEmits<{
  success: [key: string, code?: string, type?: CaptchaType];
  refresh: [];
}>();

// 默认使用 Canvas 滑块（纯前端，不依赖后端图片）
const currentType = ref<CaptchaType>(CaptchaType.CANVAS_SLIDER);

const arithmeticRef = ref<InstanceType<typeof ArithmeticCaptcha> | null>(null);
const canvasSliderRef = ref<InstanceType<typeof CanvasSliderCaptcha> | null>(null);
const smsRef = ref<InstanceType<typeof SmsCaptcha> | null>(null);
const emailRef = ref<InstanceType<typeof EmailCaptcha> | null>(null);

const onSuccess = (key: string, code?: string) => {
  emit('success', key, code, currentType.value);
};

const onRefresh = () => {
  emit('refresh');
};

const handleTabChange = (type: string) => {
  currentType.value = type as CaptchaType;
};

const refresh = () => {
  if (currentType.value === CaptchaType.ARITHMETIC && arithmeticRef.value) {
    arithmeticRef.value.refresh();
  } else if (currentType.value === CaptchaType.CANVAS_SLIDER && canvasSliderRef.value) {
    canvasSliderRef.value.refresh();
  }
};

defineExpose({ refresh });
</script>

<style scoped lang="scss">
.captcha-selector {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }
}
</style>
