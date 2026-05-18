<template>
  <DemoDocLayout
    class="china-area-view-container"
    title="省市区选择器"
    subtitle="基于级联选择器的行政区划组件，支持省市区三级、省市区街道四级和路径显示控制。"
    content-box
    :toc-items="tocItems"
  >
    <section id="level3" class="doc-section">
      <h2>省市区三级</h2>
      <p>level=3 时返回省、市、区县 ID 数组，适合地址表单里的行政区字段。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="96px" class="demo-panel-medium">
            <el-form-item label="所在地区">
              <ChinaArea v-model="areaValue" :level="3" placeholder="请选择省市区" clearable />
            </el-form-item>
          </el-form>
          <div class="result-tags">
            <el-tag v-if="areaValue.length">省市区：{{ areaValue.join(',') }}</el-tag>
            <el-tag v-else type="info">暂未选择</el-tag>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('level3')">
          <el-icon><component :is="codeVisible.level3 ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.level3 ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.level3" :code="level3Code" />
      </div>
    </section>

    <section id="level4" class="doc-section">
      <h2>省市区街道四级</h2>
      <p>level=4 时继续加载街道节点，v-model 返回四级路径 ID 数组。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="96px" class="demo-panel-medium">
            <el-form-item label="四级联动">
              <ChinaArea v-model="areaValue4" :level="4" placeholder="请选择省市区街道" clearable />
            </el-form-item>
          </el-form>
          <div class="result-tags">
            <el-tag v-if="areaValue4.length" type="success">四级：{{ areaValue4.join(',') }}</el-tag>
            <el-tag v-else type="info">暂未选择</el-tag>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('level4')">
          <el-icon><component :is="codeVisible.level4 ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.level4 ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.level4" :code="level4Code" />
      </div>
    </section>

    <section id="display" class="doc-section">
      <h2>仅显示末级</h2>
      <p>show-all-levels=false 时选择框只显示末级名称，但 v-model 仍返回完整路径 ID 数组。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="96px" class="demo-panel-medium">
            <el-form-item label="街道">
              <ChinaArea v-model="streetOnly" :level="4" :show-all-levels="false" placeholder="请选择街道" clearable />
            </el-form-item>
          </el-form>
          <div class="result-tags">
            <el-tag v-if="streetOnly.length" type="warning">街道路径：{{ streetOnly.join(',') }}</el-tag>
            <el-tag v-else type="info">暂未选择</el-tag>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('display')">
          <el-icon><component :is="codeVisible.display ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.display ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.display" :code="displayCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="属性名" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="类型" min-width="180" />
        <el-table-column prop="defaultValue" label="默认值" width="120" />
      </el-table>
    </section>

    <section id="events" class="doc-section api-section">
      <h2>支持方法 / 事件</h2>
      <el-table :data="eventsTable" size="small" border>
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="payload" label="参数" min-width="240" />
      </el-table>
    </section>

    <section id="value" class="doc-section api-section">
      <h2>返回字段</h2>
      <el-table :data="valueTable" size="small" border>
        <el-table-column prop="name" label="字段" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="example" label="示例" min-width="180" />
      </el-table>
    </section>
  </DemoDocLayout>
</template>

<script setup lang="ts" name="ChinaAreaView">
import { ref } from 'vue';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { ChinaArea } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'level3', label: '省市区三级' },
  { id: 'level4', label: '省市区街道四级' },
  { id: 'display', label: '仅显示末级' },
  { id: 'props', label: '支持属性' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'value', label: '返回字段' },
];

const areaValue = ref<number[]>([]);
const areaValue4 = ref<number[]>([]);
const streetOnly = ref<number[]>([]);
const codeVisible = ref<Record<string, boolean>>({
  level3: false,
  level4: false,
  display: false,
});

const level3Code = `<ChinaArea
  v-model="areaIds"
  :level="3"
  placeholder="请选择省市区"
  clearable
/>`;

const level4Code = `<ChinaArea
  v-model="areaIds"
  :level="4"
  placeholder="请选择省市区街道"
  clearable
/>`;

const displayCode = `<ChinaArea
  v-model="areaIds"
  :level="4"
  :show-all-levels="false"
  placeholder="请选择街道"
  clearable
/>`;

const propsTable = [
  { name: 'v-model', description: '选中行政区划 ID 路径', type: 'number[]', defaultValue: '[]' },
  { name: 'level', description: '联动级别；3=省市区，4=省市区街道', type: 'number', defaultValue: '3' },
  { name: 'showAllLevels', description: '是否在输入框展示完整路径', type: 'boolean', defaultValue: 'true' },
  { name: 'showHot', description: '是否优先展示热门城市配置', type: 'boolean', defaultValue: 'true' },
  { name: 'placeholder', description: '占位文本，支持普通文本或 i18n key', type: 'string', defaultValue: 'chinaArea.placeholder' },
  { name: 'disabled', description: '是否禁用', type: 'boolean', defaultValue: 'false' },
  { name: 'clearable', description: '是否可清空', type: 'boolean', defaultValue: 'true' },
  { name: 'filterable', description: '是否支持搜索过滤', type: 'boolean', defaultValue: 'true' },
  { name: 'collapseTags', description: '多选标签是否折叠；当前组件按级联单路径使用', type: 'boolean', defaultValue: 'false' },
  { name: 'separator', description: '路径展示分隔符', type: 'string', defaultValue: '/' },
];

const eventsTable = [
  { name: 'update:modelValue', description: '选择结果变化时触发', payload: 'number[]' },
  { name: 'change', description: '选择结果变化时触发', payload: 'number[]' },
  { name: 'getValue', description: '组件暴露方法，获取当前选中路径 ID', payload: '() => number[]' },
  { name: 'clear', description: '组件暴露方法，清空当前选择并清理缓存', payload: '() => void' },
  { name: 'clearNodeCache', description: '组件暴露方法，清理指定父节点缓存，强制重新加载', payload: '(parentId: number) => void' },
];

const valueTable = [
  { name: 'modelValue', description: '行政区划 ID 路径数组，按省、市、区县、街道顺序返回', example: '[440000, 440100, 440106]' },
  { name: 'level=4', description: '四级联动会在路径末尾追加街道 ID', example: '[440000, 440100, 440106, 440106001]' },
];

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
</style>
