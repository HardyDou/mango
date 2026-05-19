<template>
  <div v-if="isPopupMode" class="captcha-selector is-popup">
    <el-dialog
      v-model="popupVisible"
      class="captcha-selector-dialog"
      :class="`is-${currentType.toLowerCase().replace('_', '-')}`"
      title="请完成安全验证"
      :width="popupWidth"
      append-to-body
      @closed="handlePopupClosed"
    >
      <component
        :is="currentComponent"
        :ref="setFixedRef"
        v-bind="currentComponentProps"
        @success="onSuccess"
        @refresh="handleRefresh"
        @input-change="onInputChange"
      />
      <template v-if="showPopupFooter" #footer>
        <el-button @click="closePopup(false)">取消</el-button>
        <el-button
          type="primary"
          @click="confirmPopup"
        >
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
  <div v-else class="captcha-selector">
    <component
      :is="currentComponent"
      v-if="fixedType"
      :ref="setFixedRef"
      v-bind="currentComponentProps"
      @success="onSuccess"
      @refresh="handleRefresh"
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
          @refresh="handleRefresh"
          @input-change="onInputChange"
        />
      </el-tab-pane>
      <el-tab-pane label="图片滑块" name="BLOCK_PUZZLE">
        <BlockPuzzleCaptcha
          ref="blockPuzzleRef"
          :mode="mode"
          @success="onSuccess"
          @refresh="handleRefresh"
        />
      </el-tab-pane>
      <el-tab-pane label="点选文字" name="CLICK_WORD">
        <ClickWordCaptcha
          ref="clickWordRef"
          @success="onSuccess"
          @refresh="handleRefresh"
        />
      </el-tab-pane>
      <el-tab-pane label="无感行为" name="BEHAVIOR">
        <BehaviorCaptcha
          ref="behaviorRef"
          @success="onSuccess"
          @refresh="handleRefresh"
        />
      </el-tab-pane>
      <el-tab-pane label="Canvas滑块" name="CANVAS_SLIDER">
        <CanvasSliderCaptcha
          ref="canvasSliderRef"
          :mode="mode"
          @success="onSuccess"
          @refresh="handleRefresh"
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
import { computed, nextTick, ref, watch } from 'vue';
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
const isPopupMode = computed(() => mode.value === 'popup');
const currentType = ref<CaptchaType>(props.type ?? CaptchaType.CANVAS_SLIDER);
const arithmeticRef = ref<InstanceType<typeof ArithmeticCaptcha> | null>(null);
const blockPuzzleRef = ref<InstanceType<typeof BlockPuzzleCaptcha> | null>(null);
const clickWordRef = ref<InstanceType<typeof ClickWordCaptcha> | null>(null);
const behaviorRef = ref<InstanceType<typeof BehaviorCaptcha> | null>(null);
const canvasSliderRef = ref<InstanceType<typeof CanvasSliderCaptcha> | null>(null);
const smsRef = ref<InstanceType<typeof SmsCaptcha> | null>(null);
const emailRef = ref<InstanceType<typeof EmailCaptcha> | null>(null);
const popupVisible = ref(false);
const popupVerified = ref(false);
let popupResolver: ((passed: boolean) => void) | null = null;
let popupPromise: Promise<boolean> | null = null;

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

const currentRef = computed(() => {
  const refs: Partial<Record<CaptchaType, { refresh?: () => void; verify?: () => Promise<boolean> } | null>> = {
    [CaptchaType.ARITHMETIC]: arithmeticRef.value,
    [CaptchaType.BLOCK_PUZZLE]: blockPuzzleRef.value,
    [CaptchaType.CLICK_WORD]: clickWordRef.value,
    [CaptchaType.BEHAVIOR]: behaviorRef.value,
    [CaptchaType.CANVAS_SLIDER]: canvasSliderRef.value,
    [CaptchaType.SMS]: smsRef.value,
    [CaptchaType.EMAIL]: emailRef.value,
  };
  return refs[currentType.value] ?? null;
});

const currentVerifier = computed(() => currentRef.value?.verify);
const showPopupFooter = computed(() => [
  CaptchaType.ARITHMETIC,
  CaptchaType.BEHAVIOR,
  CaptchaType.SMS,
  CaptchaType.EMAIL,
].includes(currentType.value));
const popupWidth = computed(() => {
  if ([CaptchaType.BLOCK_PUZZLE, CaptchaType.CANVAS_SLIDER].includes(currentType.value)) {
    return '340px';
  }
  if (currentType.value === CaptchaType.CLICK_WORD) {
    return '380px';
  }
  return '420px';
});

watch(() => props.type, (type) => {
  if (type) {
    currentType.value = type;
  }
}, { immediate: true });

function onSuccess(key: string, code?: string) {
  popupVerified.value = true;
  emit('success', key, code, currentType.value);
  if (isPopupMode.value) {
    closePopup(true);
  }
}

function handleTabChange(type: string | number) {
  currentType.value = type as CaptchaType;
  popupVerified.value = false;
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
  popupVerified.value = false;
  currentRef.value?.refresh?.();
}

function verify() {
  if (isPopupMode.value) {
    return openPopup();
  }
  return currentVerifier.value?.() ?? Promise.resolve(false);
}

function onInputChange(value: string) {
  emit('inputChange', value, currentType.value);
}

function handleRefresh() {
  popupVerified.value = false;
  emit('refresh');
}

async function openPopup() {
  if (popupVerified.value) return true;
  if (popupPromise) return popupPromise;
  popupVisible.value = true;
  await nextTick();
  popupPromise = new Promise<boolean>((resolve) => {
    popupResolver = resolve;
  });
  return popupPromise;
}

async function confirmPopup() {
  const passed = await currentVerifier.value?.();
  if (passed) {
    popupVerified.value = true;
    closePopup(true);
  }
}

function closePopup(passed: boolean) {
  popupVisible.value = false;
  popupResolver?.(passed);
  popupResolver = null;
  popupPromise = null;
}

function handlePopupClosed() {
  if (!popupVerified.value) {
    closePopup(false);
  }
}

defineExpose({ refresh, verify });
</script>

<style scoped lang="scss">
.captcha-selector.is-popup {
  display: contents;
}

:global(.captcha-selector-dialog) {
  --el-dialog-padding-primary: 24px;
}

:global(.captcha-selector-dialog .el-dialog__header) {
  margin-right: 0;
  padding-bottom: 16px;
}

:global(.captcha-selector-dialog .el-dialog__title) {
  color: var(--el-text-color-primary);
  font-size: 22px;
  line-height: 1.35;
}

:global(.captcha-selector-dialog .el-dialog__body) {
  padding-top: 0;
}
</style>
