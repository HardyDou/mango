/**
 * useECharts Hook
 *
 * Provides ECharts instance management with automatic resize, theme switching, and responsive configuration.
 */

import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import * as echarts from 'echarts';

export interface EChartsOption {
  theme?: 'light' | 'dark';
  autoresize?: boolean;
  loading?: boolean;
  isDark?: boolean;
}

export function useECharts(
  elRef: HTMLElement | null,
  options: EChartsOption = {}
) {
  const { theme, autoresize = true, isDark } = options;

  const chartInstance = ref<echarts.ECharts | null>(null);
  const resolvedTheme = theme ?? (isDark ? 'dark' : 'light');

  // Initialize chart
  const initChart = () => {
    if (!elRef) return;

    chartInstance.value = echarts.init(elRef, resolvedTheme);

    // Apply autoresize
    if (autoresize) {
      window.addEventListener('resize', handleResize);
    }
  };

  // Handle resize
  const handleResize = () => {
    if (chartInstance.value) {
      chartInstance.value.resize();
    }
  };

  // Set option
  const setOption = (option: echarts.EChartsOption) => {
    if (chartInstance.value) {
      chartInstance.value.setOption(option);
    }
  };

  // Get instance
  const getInstance = () => chartInstance.value;

  // Resize
  const resize = () => {
    if (chartInstance.value) {
      chartInstance.value.resize();
    }
  };

  // Clear
  const clear = () => {
    if (chartInstance.value) {
      chartInstance.value.clear();
    }
  };

  // Dispose
  const dispose = () => {
    if (chartInstance.value) {
      chartInstance.value.dispose();
      chartInstance.value = null;
    }
  };

  // Show loading
  const showLoading = (loadingOptions?: echarts.LoadingOptions) => {
    if (chartInstance.value) {
      chartInstance.value.showLoading(loadingOptions);
    }
  };

  // Hide loading
  const hideLoading = () => {
    if (chartInstance.value) {
      chartInstance.value.hideLoading();
    }
  };

  // Watch for theme changes
  watch(
    () => [options.theme, options.isDark],
    () => {
      nextTick(() => {
        if (chartInstance.value) {
          dispose();
          initChart();
        }
      });
    }
  );

  onMounted(() => {
    nextTick(() => {
      initChart();
    });
  });

  onBeforeUnmount(() => {
    if (autoresize) {
      window.removeEventListener('resize', handleResize);
    }
    dispose();
  });

  return {
    chartInstance,
    setOption,
    getInstance,
    resize,
    clear,
    dispose,
    showLoading,
    hideLoading,
  };
}

// Chart types for type safety
export type ChartType = 'line' | 'bar' | 'pie' | 'scatter' | 'radar' | 'gauge' | 'funnel' | 'treemap';

// Common color palettes
export const CHART_COLORS = {
  light: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'],
  dark: ['#5470c6', '#73c0de', '#91cc75', '#fac858', '#ee6666', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'],
};

// Default chart options factory
export function createLineChartOptions(data: { xAxis: string[]; series: { name: string; data: number[] }[] }): echarts.EChartsOption {
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: data.series.map((s) => s.name) },
    xAxis: { type: 'category', data: data.xAxis },
    yAxis: { type: 'value' },
    series: data.series.map((s) => ({
      name: s.name,
      type: 'line',
      data: s.data,
    })),
  };
}

export function createBarChartOptions(data: { xAxis: string[]; series: { name: string; data: number[] }[] }): echarts.EChartsOption {
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: data.series.map((s) => s.name) },
    xAxis: { type: 'category', data: data.xAxis },
    yAxis: { type: 'value' },
    series: data.series.map((s) => ({
      name: s.name,
      type: 'bar',
      data: s.data,
    })),
  };
}

export function createPieChartOptions(data: { name: string; value: number }[]): echarts.EChartsOption {
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' },
        },
        labelLine: { show: false },
        data,
      },
    ],
  };
}
