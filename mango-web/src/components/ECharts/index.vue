<template>
  <div
    ref="chartRef"
    class="echarts-container"
    :style="{ height: height, width: width }"
  />
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick, PropType } from 'vue';
import * as echarts from 'echarts';
import { useThemeStore } from '@/stores/theme';
import { storeToRefs } from 'pinia';

const props = withDefaults(
  defineProps<{
    options?: echarts.EChartsOption;
    height?: string;
    width?: string;
    autoresize?: boolean;
    loading?: boolean;
    loadingOptions?: echarts.LoadingOptions;
  }>(),
  {
    options: () => ({}),
    height: '300px',
    width: '100%',
    autoresize: true,
    loading: false,
  }
);

const emit = defineEmits<{
  (e: 'click', params: echarts.ECElementEvent): void;
  (e: 'ready', chart: echarts.ECharts): void;
}>();

const chartRef = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;
const themeStore = useThemeStore();
const { isDark } = storeToRefs(themeStore);

// Initialize chart
const initChart = () => {
  if (!chartRef.value) return;

  chartInstance = echarts.init(chartRef.value);
  chartInstance.setOption(props.options);

  // Register click event
  chartInstance.on('click', (params) => {
    emit('click', params as echarts.ECElementEvent);
  });

  // Emit ready event
  emit('ready', chartInstance);

  // Show loading if needed
  if (props.loading) {
    chartInstance.showLoading(props.loadingOptions);
  }
};

// Set option
const setOption = (options: echarts.EChartsOption) => {
  if (chartInstance) {
    chartInstance.setOption(options);
  }
};

// Resize
const resize = () => {
  if (chartInstance) {
    chartInstance.resize();
  }
};

// Clear
const clear = () => {
  if (chartInstance) {
    chartInstance.clear();
  }
};

// Dispose
const dispose = () => {
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
};

// Show loading
const showLoading = (options?: echarts.LoadingOptions) => {
  if (chartInstance) {
    chartInstance.showLoading(options);
  }
};

// Hide loading
const hideLoading = () => {
  if (chartInstance) {
    chartInstance.hideLoading();
  }
};

// Get instance
const getInstance = () => chartInstance;

// Handle resize
const handleResize = () => {
  if (props.autoresize) {
    resize();
  }
};

// Watch for options changes
watch(
  () => props.options,
  (newOptions) => {
    if (chartInstance) {
      chartInstance.setOption(newOptions);
    }
  },
  { deep: true }
);

// Watch for loading state
watch(
  () => props.loading,
  (loading) => {
    if (loading) {
      showLoading(props.loadingOptions);
    } else {
      hideLoading();
    }
  }
);

// Watch for theme changes
watch(isDark, () => {
  nextTick(() => {
    if (chartInstance) {
      dispose();
      initChart();
    }
  });
});

onMounted(() => {
  nextTick(() => {
    initChart();
    if (props.autoresize) {
      window.addEventListener('resize', handleResize);
    }
  });
});

onBeforeUnmount(() => {
  if (props.autoresize) {
    window.removeEventListener('resize', handleResize);
  }
  dispose();
});

// Expose methods
defineExpose({
  setOption,
  resize,
  clear,
  dispose,
  showLoading,
  hideLoading,
  getInstance,
});
</script>

<style scoped lang="scss">
.echarts-container {
  width: 100%;
  height: 100%;
}
</style>
