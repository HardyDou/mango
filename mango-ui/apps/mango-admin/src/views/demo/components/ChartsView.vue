<template>
  <DemoDocLayout
    class="charts-view"
    title="数据图表"
    subtitle="基于 ECharts 的图表组件，统一封装图表初始化、响应式 resize、loading、主题和实例方法。"
    content-box
    :toc-items="tocItems"
  >
    <section id="line" class="doc-section">
      <h2>折线图</h2>
      <p>适合展示时间序列、趋势变化和连续指标。</p>
      <div class="demo-block">
        <div class="demo-source">
          <ECharts :options="lineOptions" height="320px" @click="handleChartClick" />
        </div>
        <div class="op-btns" @click="toggleCode('line')">
          <el-icon><component :is="codeVisible.line ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.line ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.line" :code="lineCode" />
      </div>
    </section>

    <section id="bar" class="doc-section">
      <h2>柱状图</h2>
      <p>适合展示分类对比、数量排行和区间统计。</p>
      <div class="demo-block">
        <div class="demo-source">
          <ECharts :options="barOptions" height="320px" />
        </div>
        <div class="op-btns" @click="toggleCode('bar')">
          <el-icon><component :is="codeVisible.bar ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.bar ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.bar" :code="barCode" />
      </div>
    </section>

    <section id="pie" class="doc-section">
      <h2>饼图</h2>
      <p>适合展示构成占比，数据项不宜过多。</p>
      <div class="demo-block">
        <div class="demo-source">
          <ECharts :options="pieOptions" height="320px" />
        </div>
        <div class="op-btns" @click="toggleCode('pie')">
          <el-icon><component :is="codeVisible.pie ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.pie ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.pie" :code="pieCode" />
      </div>
    </section>

    <section id="loading" class="doc-section">
      <h2>Loading 与主题</h2>
      <p>loading 控制图表加载态；theme 可指定 light 或 dark，适合跟随业务区域主题。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="demo-actions">
            <el-switch v-model="chartLoading" active-text="Loading" />
            <el-segmented v-model="chartTheme" :options="themeOptions" />
          </div>
          <ECharts :options="lineOptions" :loading="chartLoading" :theme="chartTheme" height="300px" />
        </div>
        <div class="op-btns" @click="toggleCode('loading')">
          <el-icon><component :is="codeVisible.loading ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.loading ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.loading" :code="loadingCode" />
      </div>
    </section>

    <section id="methods" class="doc-section">
      <h2>方法调用</h2>
      <p>通过 ref 可以刷新数据、手动 resize、显示 loading 或清空图表。</p>
      <div class="demo-block">
        <div class="demo-source">
          <ECharts ref="chartRef" :options="methodOptions" height="300px" @ready="handleReady" />
          <div class="demo-actions">
            <el-button type="primary" @click="refreshChart">刷新数据</el-button>
            <el-button @click="resizeChart">手动 resize</el-button>
            <el-button @click="showLoading">显示 loading</el-button>
            <el-button type="warning" @click="clearChart">清空</el-button>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('methods')">
          <el-icon><component :is="codeVisible.methods ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.methods ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.methods" :code="methodsCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="属性名" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="类型" min-width="220" />
        <el-table-column prop="defaultValue" label="默认值" width="130" />
      </el-table>
    </section>

    <section id="slots" class="doc-section api-section">
      <h2>支持插槽</h2>
      <el-table :data="slotsTable" size="small" border>
        <el-table-column prop="name" label="插槽名" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="scope" label="作用域参数" min-width="180" />
      </el-table>
    </section>

    <section id="events" class="doc-section api-section">
      <h2>支持方法 / 事件</h2>
      <el-table :data="eventsTable" size="small" border>
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="payload" label="参数 / 返回" min-width="260" />
      </el-table>
    </section>

    <section id="value" class="doc-section api-section">
      <h2>返回字段</h2>
      <el-table :data="valueTable" size="small" border>
        <el-table-column prop="field" label="字段" width="160" />
        <el-table-column prop="type" label="类型" min-width="200" />
        <el-table-column prop="description" label="说明" min-width="280" />
      </el-table>
    </section>
  </DemoDocLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { ECharts } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'line', label: '折线图' },
  { id: 'bar', label: '柱状图' },
  { id: 'pie', label: '饼图' },
  { id: 'loading', label: 'Loading 与主题' },
  { id: 'methods', label: '方法调用' },
  { id: 'props', label: '支持属性' },
  { id: 'slots', label: '支持插槽' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'value', label: '返回字段' },
];

const chartRef = ref<InstanceType<typeof ECharts>>();
const chartLoading = ref(false);
const chartTheme = ref<'light' | 'dark'>('light');
const themeOptions = [
  { label: '亮色', value: 'light' },
  { label: '暗色', value: 'dark' },
];
const codeVisible = ref<Record<string, boolean>>({
  line: false,
  bar: false,
  pie: false,
  loading: false,
  methods: false,
});

const lineOptions = ref({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
  yAxis: { type: 'value' },
  series: [{ name: '访问量', type: 'line', smooth: true, data: [120, 200, 150, 80, 70, 110, 130] }],
});

const barOptions = ref({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['华东', '华南', '华北', '西南', '西北'] },
  yAxis: { type: 'value' },
  series: [{ name: '订单数', type: 'bar', data: [320, 240, 180, 160, 120] }],
});

const pieOptions = ref({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{
    name: '来源',
    type: 'pie',
    radius: ['42%', '68%'],
    data: [
      { value: 1048, name: '搜索' },
      { value: 735, name: '直接访问' },
      { value: 580, name: '邮件' },
      { value: 484, name: '广告' },
    ],
  }],
});

const methodOptions = ref({ ...barOptions.value });

const lineCode = `<ECharts
  :options="lineOptions"
  height="320px"
  @click="handleChartClick"
/>`;
const barCode = `<ECharts
  :options="barOptions"
  height="320px"
/>`;
const pieCode = `<ECharts
  :options="pieOptions"
  height="320px"
/>`;
const loadingCode = `<ECharts
  :options="chartOptions"
  :loading="loading"
  theme="dark"
  height="300px"
/>`;
const methodsCode = `<template>
  <ECharts ref="chartRef" :options="chartOptions" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ECharts } from '@mango/common';

const chartRef = ref<InstanceType<typeof ECharts>>();

function resize() {
  chartRef.value?.resize();
}
<\/script>`;

const propsTable = [
  { name: 'options', description: 'ECharts 配置对象，变更后自动 setOption', type: 'EChartsOption', defaultValue: '{}' },
  { name: 'height', description: '图表容器高度', type: 'string', defaultValue: '300px' },
  { name: 'width', description: '图表容器宽度', type: 'string', defaultValue: '100%' },
  { name: 'autoresize', description: '是否监听窗口 resize 并自动调整图表', type: 'boolean', defaultValue: 'true' },
  { name: 'loading', description: '是否显示图表 loading 状态', type: 'boolean', defaultValue: 'false' },
  { name: 'loadingOptions', description: 'ECharts loading 配置', type: 'LoadingOptions', defaultValue: '-' },
  { name: 'theme', description: '指定图表主题；未传时跟随系统明暗偏好', type: "'light' | 'dark'", defaultValue: '-' },
];

const slotsTable = [
  { name: '-', description: '当前组件不提供业务插槽，图表内容通过 options 配置', scope: '-' },
];

const eventsTable = [
  { name: 'click', description: '点击图表元素时触发', payload: 'ECElementEvent' },
  { name: 'ready', description: '图表初始化完成时触发', payload: 'ECharts 实例' },
  { name: 'setOption', description: '暴露方法，更新图表配置', payload: '(options: EChartsOption) => void' },
  { name: 'resize', description: '暴露方法，手动调整图表尺寸', payload: '() => void' },
  { name: 'clear', description: '暴露方法，清空图表内容', payload: '() => void' },
  { name: 'dispose', description: '暴露方法，销毁图表实例', payload: '() => void' },
  { name: 'showLoading', description: '暴露方法，显示 loading', payload: '(options?: LoadingOptions) => void' },
  { name: 'hideLoading', description: '暴露方法，隐藏 loading', payload: '() => void' },
  { name: 'getInstance', description: '暴露方法，获取 ECharts 实例', payload: '() => ECharts | null' },
];

const valueTable = [
  { field: 'click', type: 'ECElementEvent', description: '返回被点击图形的数据、系列、坐标等事件参数' },
  { field: 'ready', type: 'ECharts', description: '返回初始化后的 ECharts 实例，可用于高级定制' },
  { field: 'getInstance()', type: 'ECharts | null', description: '返回当前图表实例；组件未初始化或已销毁时为 null' },
];

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

function handleChartClick() {
  ElMessage.success('已触发图表点击事件');
}

function handleReady() {
  ElMessage.info('图表已初始化');
}

function refreshChart() {
  const random = () => Math.floor(Math.random() * 260) + 40;
  methodOptions.value = {
    ...methodOptions.value,
    series: [{ name: '订单数', type: 'bar', data: [random(), random(), random(), random(), random()] }],
  };
}

function resizeChart() {
  chartRef.value?.resize();
  ElMessage.success('已执行 resize');
}

function showLoading() {
  chartRef.value?.showLoading();
  window.setTimeout(() => chartRef.value?.hideLoading(), 900);
}

function clearChart() {
  chartRef.value?.clear();
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.demo-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.demo-actions + .echarts-container {
  margin-top: 4px;
}
</style>
