<template>
  <div class="captcha-selector">
    <component
      :is="currentComponent"
      v-if="fixedType"
      :ref="setFixedRef"
      v-bind="currentComponentProps"
      @success="onSuccess"
      @refresh="emit('refresh')"
      @input-change="onInputChange"
    />
    <el-tabs
      v-else
      v-model="currentType"
      @tab-change="handleTabChange"
    >
      <el-tab-pane label="算术" name="ARITHMETIC">
        <ArithmeticCaptcha
          ref="arithmeticRef"
          @success="onSuccess"
          @refresh="emit('refresh')"
          @input-change="onInputChange"
        />
      </el-tab-pane>
      <el-tab-pane label="图片滑块" name="BLOCK_PUZZLE">
        <BlockPuzzleCaptcha
          ref="blockPuzzleRef"
          :mode="mode"
          @success="onSuccess"
          @refresh="emit('refresh')"
        />
      </el-tab-pane>
      <el-tab-pane label="点选文字" name="CLICK_WORD">
        <ClickWordCaptcha
          ref="clickWordRef"
          @success="onSuccess"
          @refresh="emit('refresh')"
        />
      </el-tab-pane>
      <el-tab-pane label="无感行为" name="BEHAVIOR">
        <BehaviorCaptcha
          ref="behaviorRef"
          @success="onSuccess"
          @refresh="emit('refresh')"
        />
      </el-tab-pane>
      <el-tab-pane label="Canvas滑块" name="CANVAS_SLIDER">
        <CanvasSliderCaptcha
          ref="canvasSliderRef"
          :mode="mode"
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
import { computed, ref, watch } from 'vue';
import { CaptchaType } from '../../api/captcha';
import ArithmeticCaptcha from './ArithmeticCaptcha.vue';
import BlockPuzzleCaptcha from './BlockPuzzleCaptcha.vue';
import ClickWordCaptcha from './ClickWordCaptcha.vue';
import BehaviorCaptcha from './BehaviorCaptcha.vue';
import CanvasSliderCaptcha from './CanvasSliderCaptcha.vue';
import SmsCaptcha from './SmsCaptcha.vue';
import EmailCaptcha from './EmailCaptcha.vue';

type CaptchaDisplayMode = 'embedded' | 'trigger' | 'popup';

const props = withDefaults(defineProps<{
  type?: CaptchaType;
  mode?: CaptchaDisplayMode;
}>(), {
  mode: 'embedded',
});

const emit = defineEmits<{
  success: [key: string, code?: string, type?: CaptchaType];
  refresh: [];
  inputChange: [value: string, type?: CaptchaType];
}>();

const fixedType = computed(() => props.type);
const mode = computed(() => props.mode);
const currentType = ref<CaptchaType>(props.type ?? CaptchaType.CANVAS_SLIDER);
const arithmeticRef = ref<InstanceType<typeof ArithmeticCaptcha> | null>(null);
const blockPuzzleRef = ref<InstanceType<typeof BlockPuzzleCaptcha> | null>(null);
const clickWordRef = ref<InstanceType<typeof ClickWordCaptcha> | null>(null);
const behaviorRef = ref<InstanceType<typeof BehaviorCaptcha> | null>(null);
const canvasSliderRef = ref<InstanceType<typeof CanvasSliderCaptcha> | null>(null);
const smsRef = ref<InstanceType<typeof SmsCaptcha> | null>(null);
const emailRef = ref<InstanceType<typeof EmailCaptcha> | null>(null);

const componentMap = {
  [CaptchaType.ARITHMETIC]: ArithmeticCaptcha,
  [CaptchaType.BLOCK_PUZZLE]: BlockPuzzleCaptcha,
  [CaptchaType.CLICK_WORD]: ClickWordCaptcha,
  [CaptchaType.BEHAVIOR]: BehaviorCaptcha,
  [CaptchaType.CANVAS_SLIDER]: CanvasSliderCaptcha,
  [CaptchaType.SMS]: SmsCaptcha,
  [CaptchaType.EMAIL]: EmailCaptcha,
};

const currentComponent = computed(() => componentMap[currentType.value]);
const currentComponentProps = computed(() => {
  if ([CaptchaType.BLOCK_PUZZLE, CaptchaType.CANVAS_SLIDER].includes(currentType.value)) {
    return { mode: mode.value };
  }
  return {};
});

watch(() => props.type, (type) => {
  if (type) {
    currentType.value = type;
  }
}, { immediate: true });

function onSuccess(key: string, code?: string) {
  emit('success', key, code, currentType.value);
}

function handleTabChange(type: string | number) {
  currentType.value = type as CaptchaType;
}

function setFixedRef(instance: unknown) {
  const refreshable = instance as { refresh?: () => void; verify?: () => Promise<boolean> } | null;
  if (currentType.value === CaptchaType.ARITHMETIC) {
    arithmeticRef.value = refreshable as InstanceType<typeof ArithmeticCaptcha> | null;
  }
  if (currentType.value === CaptchaType.BLOCK_PUZZLE) {
    blockPuzzleRef.value = refreshable as InstanceType<typeof BlockPuzzleCaptcha> | null;
  }
  if (currentType.value === CaptchaType.CLICK_WORD) {
    clickWordRef.value = refreshable as InstanceType<typeof ClickWordCaptcha> | null;
  }
  if (currentType.value === CaptchaType.BEHAVIOR) {
    behaviorRef.value = refreshable as InstanceType<typeof BehaviorCaptcha> | null;
  }
  if (currentType.value === CaptchaType.CANVAS_SLIDER) {
    canvasSliderRef.value = refreshable as InstanceType<typeof CanvasSliderCaptcha> | null;
  }
  if (currentType.value === CaptchaType.SMS) {
    smsRef.value = refreshable as InstanceType<typeof SmsCaptcha> | null;
  }
  if (currentType.value === CaptchaType.EMAIL) {
    emailRef.value = refreshable as InstanceType<typeof EmailCaptcha> | null;
  }
}

function refresh() {
  const refreshers: Partial<Record<CaptchaType, { refresh?: () => void } | null>> = {
    [CaptchaType.ARITHMETIC]: arithmeticRef.value,
    [CaptchaType.BLOCK_PUZZLE]: blockPuzzleRef.value,
    [CaptchaType.CLICK_WORD]: clickWordRef.value,
    [CaptchaType.BEHAVIOR]: behaviorRef.value,
    [CaptchaType.CANVAS_SLIDER]: canvasSliderRef.value,
    [CaptchaType.SMS]: smsRef.value,
    [CaptchaType.EMAIL]: emailRef.value,
  };
  refreshers[currentType.value]?.refresh?.();
}

function verify() {
  const verifiers: Partial<Record<CaptchaType, { verify?: () => Promise<boolean> } | null>> = {
    [CaptchaType.ARITHMETIC]: arithmeticRef.value,
    [CaptchaType.BLOCK_PUZZLE]: blockPuzzleRef.value,
    [CaptchaType.BEHAVIOR]: behaviorRef.value,
    [CaptchaType.CANVAS_SLIDER]: canvasSliderRef.value,
  };
  return verifiers[currentType.value]?.verify?.() ?? Promise.resolve(false);
}

function onInputChange(value: string) {
  emit('inputChange', value, currentType.value);
}

defineExpose({ refresh, verify });
</script>
