<template>
  <div class="captcha-selector">
    <el-tabs
      v-model="currentType"
      @tab-change="handleTabChange"
    >
      <el-tab-pane label="算术" name="ARITHMETIC">
        <ArithmeticCaptcha
          ref="arithmeticRef"
          @success="onSuccess"
          @refresh="emit('refresh')"
        />
      </el-tab-pane>
      <el-tab-pane label="图片滑块" name="BLOCK_PUZZLE">
        <BlockPuzzleCaptcha
          ref="blockPuzzleRef"
          @success="onSuccess"
          @refresh="emit('refresh')"
        />
      </el-tab-pane>
      <el-tab-pane label="Canvas滑块" name="CANVAS_SLIDER">
        <CanvasSliderCaptcha
          ref="canvasSliderRef"
          @success="onSuccess"
          @refresh="emit('refresh')"
        />
      </el-tab-pane>
      <el-tab-pane label="短信" name="SMS">
        <SmsCaptcha ref="smsRef" @success="onSuccess" />
      </el-tab-pane>
      <el-tab-pane label="邮件" name="EMAIL">
        <EmailCaptcha ref="emailRef" @success="onSuccess" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { CaptchaType } from '../../api/captcha';
import ArithmeticCaptcha from './ArithmeticCaptcha.vue';
import BlockPuzzleCaptcha from './BlockPuzzleCaptcha.vue';
import CanvasSliderCaptcha from './CanvasSliderCaptcha.vue';
import SmsCaptcha from './SmsCaptcha.vue';
import EmailCaptcha from './EmailCaptcha.vue';

const emit = defineEmits<{
  success: [key: string, code?: string, type?: CaptchaType];
  refresh: [];
}>();

const currentType = ref<CaptchaType>(CaptchaType.CANVAS_SLIDER);
const arithmeticRef = ref<InstanceType<typeof ArithmeticCaptcha> | null>(null);
const blockPuzzleRef = ref<InstanceType<typeof BlockPuzzleCaptcha> | null>(null);
const canvasSliderRef = ref<InstanceType<typeof CanvasSliderCaptcha> | null>(null);
const smsRef = ref<InstanceType<typeof SmsCaptcha> | null>(null);
const emailRef = ref<InstanceType<typeof EmailCaptcha> | null>(null);

function onSuccess(key: string, code?: string) {
  emit('success', key, code, currentType.value);
}

function handleTabChange(type: string | number) {
  currentType.value = type as CaptchaType;
}

function refresh() {
  const refreshers: Partial<Record<CaptchaType, { refresh?: () => void } | null>> = {
    [CaptchaType.ARITHMETIC]: arithmeticRef.value,
    [CaptchaType.BLOCK_PUZZLE]: blockPuzzleRef.value,
    [CaptchaType.CANVAS_SLIDER]: canvasSliderRef.value,
    [CaptchaType.SMS]: smsRef.value,
    [CaptchaType.EMAIL]: emailRef.value,
  };
  refreshers[currentType.value]?.refresh?.();
}

defineExpose({ refresh });
</script>
