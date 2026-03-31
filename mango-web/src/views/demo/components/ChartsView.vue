<template>
  <div class="charts-view-container">
    <h1>数据图表</h1>
    <p class="subtitle">
      基于 ECharts 的可视化图表组件，支持折线图、柱状图、饼图等多种图表类型
    </p>

    <!-- 图表类型选择 -->
    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>图表示例</span>
          <el-radio-group
            v-model="currentChartType"
            size="small"
          >
            <el-radio-button label="line">
              折线图
            </el-radio-button>
            <el-radio-button label="bar">
              柱状图
            </el-radio-button>
            <el-radio-button label="pie">
              饼图
            </el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <ECharts
        :options="currentOptions"
        height="350px"
      />

      <div class="chart-toolbar">
        <el-button
          type="primary"
          size="small"
          @click="refreshChart"
        >
          刷新数据
        </el-button>
        <el-button
          type="info"
          size="small"
          @click="toggleDark"
        >
          {{ isDark ? '亮色主题' : '暗色主题' }}
        </el-button>
      </div>
    </el-card>

    <!-- 功能特性 -->
    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>功能特性</span>
      </template>
      <div class="feature-list">
        <el-tag type="success">
          响应式
        </el-tag>
        <el-tag type="success">
          动态数据
        </el-tag>
        <el-tag type="success">
          主题切换
        </el-tag>
        <el-tag type="success">
          多种图表
        </el-tag>
        <el-tag type="info">
          自动 resize
        </el-tag>
        <el-tag type="info">
          Loading 状态
        </el-tag>
        <el-tag type="info">
          点击事件
        </el-tag>
      </div>
    </el-card>

    <!-- 图表类型说明 -->
    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>支持的图表类型</span>
      </template>
      <el-row :gutter="16">
        <el-col :span="8">
          <div class="chart-type-item">
            <el-tag type="primary">
              折线图
            </el-tag>
            <p>展示数据趋势变化，适合时间序列数据</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="chart-type-item">
            <el-tag type="success">
              柱状图
            </el-tag>
            <p>展示数据大小对比，适合分类数据比较</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="chart-type-item">
            <el-tag type="warning">
              饼图
            </el-tag>
            <p>展示数据占比，适合比例数据展示</p>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 使用方法 -->
    <el-card
      class="demo-card usage-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>使用方法</span>
      </template>
      <el-tabs>
        <el-tab-pane label="基础用法">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;ECharts :options="chartOptions" /&gt;
&lt;/template&gt;

&lt;script setup&gt;
import { ref } from 'vue';
import ECharts from '@/components/ECharts/index.vue';

const chartOptions = ref({
  xAxis: {
    type: 'category',
    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      data: [120, 200, 150, 80, 70, 110, 130],
      type: 'line',
    },
  ],
});
&lt;/script&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="柱状图">
          <div class="code-block">
            <pre><code>const chartOptions = ref({
  xAxis: {
    type: 'category',
    data: ['苹果', '香蕉', '橙子', '葡萄', '草莓'],
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      data: [120, 200, 150, 80, 70],
      type: 'bar',
    },
  ],
});</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="饼图">
          <div class="code-block">
            <pre><code>const chartOptions = ref({
  series: [
    {
      type: 'pie',
      radius: '50%',
      data: [
        { value: 1048, name: '搜索引擎' },
        { value: 735, name: '直接访问' },
        { value: 580, name: '邮件营销' },
        { value: 484, name: '联盟广告' },
      ],
    },
  ],
});</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="自定义尺寸">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;ECharts
    :options="chartOptions"
    height="500px"
    width="100%"
  /&gt;
&lt;/template&gt;

// props 说明:
// options: ECharts 配置对象
// height: 图表高度，默认 300px
// width: 图表宽度，默认 100%
// autoresize: 是否自动响应窗口大小变化，默认 true
// loading: 是否显示 loading 状态，默认 false</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="监听事件">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;ECharts
    :options="chartOptions"
    @click="handleClick"
    @ready="handleReady"
  /&gt;
&lt;/template&gt;

&lt;script setup&gt;
const handleClick = (params) => {
  console.log('点击了:', params);
};

const handleReady = (chartInstance) => {
  console.log('图表实例:', chartInstance);
};
&lt;/script&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="Loading 状态">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;ECharts
    :options="chartOptions"
    :loading="isLoading"
  /&gt;
&lt;/template&gt;</code></pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import ECharts from '@/components/ECharts/index.vue';

const currentChartType = ref('line');
const isDark = ref(false);

// 折线图数据
const lineOptions = ref({
  xAxis: {
    type: 'category',
    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      data: [120, 200, 150, 80, 70, 110, 130],
      type: 'line',
      smooth: true,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' },
          ],
        },
      },
    },
  ],
});

// 柱状图数据
const barOptions = ref({
  xAxis: {
    type: 'category',
    data: ['苹果', '香蕉', '橙子', '葡萄', '草莓', '西瓜'],
  },
  yAxis: {
    type: 'value',
  },
  series: [
    {
      data: [120, 200, 150, 80, 70, 110],
      type: 'bar',
      itemStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#79bbff' },
          ],
        },
        borderRadius: [4, 4, 0, 0],
      },
    },
  ],
});

// 饼图数据
const pieOptions = ref({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)',
  },
  legend: {
    orient: 'vertical',
    left: 'left',
  },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2,
      },
      label: {
        show: true,
        formatter: '{b}: {d}%',
      },
      data: [
        { value: 1048, name: '搜索引擎' },
        { value: 735, name: '直接访问' },
        { value: 580, name: '邮件营销' },
        { value: 484, name: '联盟广告' },
        { value: 300, name: '视频广告' },
      ],
    },
  ],
});

const currentOptions = computed(() => {
  switch (currentChartType.value) {
    case 'bar':
      return barOptions.value;
    case 'pie':
      return pieOptions.value;
    default:
      return lineOptions.value;
  }
});

function refreshChart() {
  // 模拟刷新数据
  const randomData = () => Math.floor(Math.random() * 200) + 50;

  if (currentChartType.value === 'line') {
    lineOptions.value = {
      ...lineOptions.value,
      series: [
        {
          ...lineOptions.value.series[0],
          data: [randomData(), randomData(), randomData(), randomData(), randomData(), randomData(), randomData()],
        },
      ],
    };
  } else if (currentChartType.value === 'bar') {
    barOptions.value = {
      ...barOptions.value,
      series: [
        {
          ...barOptions.value.series[0],
          data: [randomData(), randomData(), randomData(), randomData(), randomData(), randomData()],
        },
      ],
    };
  } else {
    pieOptions.value = {
      ...pieOptions.value,
      series: [
        {
          ...pieOptions.value.series[0],
          data: pieOptions.value.series[0].data.map(() => ({
            name: ['搜索引擎', '直接访问', '邮件营销', '联盟广告', '视频广告'][Math.floor(Math.random() * 5)],
            value: randomData(),
          })),
        },
      ],
    };
  }
}

function toggleDark() {
  isDark.value = !isDark.value;
}
</script>

<style scoped lang="scss">
.charts-view-container {
  padding: 20px;

  h1 {
    margin-bottom: 8px;
    font-size: 24px;
    font-weight: 600;
  }

  .subtitle {
    margin-bottom: 20px;
    color: #909399;
  }

  .demo-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .chart-toolbar {
      display: flex;
      gap: 12px;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #eee;
    }

    .feature-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .chart-type-item {
      padding: 12px;
      border: 1px solid #eee;
      border-radius: 4px;

      p {
        margin: 8px 0 0 0;
        font-size: 13px;
        color: #606266;
      }
    }
  }

  .usage-card {
    :deep(.el-tabs__content) {
      max-height: 400px;
      overflow-y: auto;
    }
  }

  .code-block {
    background: #1e1e1e;
    border-radius: 4px;
    padding: 16px;
    overflow-x: auto;

    pre {
      margin: 0;
    }

    code {
      color: #d4d4d4;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 13px;
      line-height: 1.5;
    }
  }
}
</style>
