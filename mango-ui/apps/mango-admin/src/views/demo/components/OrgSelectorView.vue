<template>
  <DemoDocLayout
    class="org-selector-view-container"
    title="组织架构选择器"
    subtitle="基于组织树的选择组件，支持单选、多选、禁用、最大选择数量和名称标签回显。"
    content-box
    :toc-items="tocItems"
  >
    <section id="single" class="doc-section">
      <h2>单选组织</h2>
      <p>默认单选，v-model 返回当前选中的组织 ID；适合表单里选择所属组织、归属部门等字段。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="96px" class="demo-panel-medium">
            <el-form-item label="所属组织">
              <OrgSelector v-model="singleValue" placeholder="请选择组织" />
            </el-form-item>
          </el-form>
          <div class="result-tags">
            <el-tag v-if="singleValue !== undefined">当前值：{{ singleValue }}</el-tag>
            <el-tag v-else type="info">暂未选择</el-tag>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('single')">
          <el-icon><component :is="codeVisible.single ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.single ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.single" :code="singleCode" />
      </div>
    </section>

    <section id="multiple" class="doc-section">
      <h2>多选组织</h2>
      <p>设置 multiple 后，v-model 返回组织 ID 数组；max 可限制最多选择数量。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="96px" class="demo-panel-medium">
            <el-form-item label="参与组织">
              <OrgSelector v-model="multiValue" multiple :max="3" placeholder="请选择组织" />
            </el-form-item>
          </el-form>
          <div class="result-tags">
            <el-tag v-for="org in multiValue" :key="org" type="success">{{ org }}</el-tag>
            <el-tag v-if="multiValue.length === 0" type="info">暂未选择</el-tag>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('multiple')">
          <el-icon><component :is="codeVisible.multiple ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.multiple ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.multiple" :code="multipleCode" />
      </div>
    </section>

    <section id="disabled" class="doc-section">
      <h2>禁用状态</h2>
      <p>disabled 用于详情页或审批只读场景，保留当前值但禁止重新选择。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="96px" class="demo-panel-medium">
            <el-form-item label="只读组织">
              <OrgSelector v-model="disabledValue" multiple disabled placeholder="禁用状态" />
            </el-form-item>
          </el-form>
        </div>
        <div class="op-btns" @click="toggleCode('disabled')">
          <el-icon><component :is="codeVisible.disabled ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.disabled ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.disabled" :code="disabledCode" />
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
        <el-table-column prop="mode" label="模式" width="120" />
        <el-table-column prop="value" label="v-model 返回" min-width="220" />
        <el-table-column prop="scene" label="说明" min-width="260" />
      </el-table>
    </section>
  </DemoDocLayout>
</template>

<script setup lang="ts" name="OrgSelectorView">
import { ref } from 'vue';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { OrgSelector } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'single', label: '单选组织' },
  { id: 'multiple', label: '多选组织' },
  { id: 'disabled', label: '禁用状态' },
  { id: 'props', label: '支持属性' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'value', label: '返回字段' },
];

const singleValue = ref<number>();
const multiValue = ref<number[]>([]);
const disabledValue = ref<number[]>([1]);
const codeVisible = ref<Record<string, boolean>>({
  single: false,
  multiple: false,
  disabled: false,
});

const singleCode = `<OrgSelector
  v-model="orgId"
  placeholder="请选择组织"
/>`;

const multipleCode = `<OrgSelector
  v-model="orgIds"
  multiple
  :max="3"
  placeholder="请选择组织"
/>`;

const disabledCode = `<OrgSelector
  v-model="orgIds"
  multiple
  disabled
  placeholder="禁用状态"
/>`;

const propsTable = [
  { name: 'v-model', description: '选中组织 ID；multiple=false 返回单个 ID，multiple=true 返回 ID 数组', type: 'number | number[]', defaultValue: '[]' },
  { name: 'multiple', description: '是否多选', type: 'boolean', defaultValue: 'false' },
  { name: 'placeholder', description: '选择框占位文本，支持普通文本或 i18n key', type: 'string', defaultValue: 'orgSelector.placeholder' },
  { name: 'title', description: '组织选择弹窗标题', type: 'string', defaultValue: 'orgSelector.title' },
  { name: 'showTagNames', description: '是否在选择框下方展示已选组织名称标签', type: 'boolean', defaultValue: 'true' },
  { name: 'max', description: '最多可选数量；0 表示不限制', type: 'number', defaultValue: '0' },
  { name: 'disabled', description: '是否禁用', type: 'boolean', defaultValue: 'false' },
  { name: 'width', description: '选择弹窗宽度', type: 'string | number', defaultValue: '500px' },
];

const eventsTable = [
  { name: 'update:modelValue', description: '选择结果变化时触发', payload: 'number | number[] | undefined' },
  { name: 'change', description: '确认选择后触发', payload: 'number | number[] | undefined' },
  { name: 'open', description: '组件暴露方法，打开选择弹窗', payload: '() => void' },
  { name: 'close', description: '组件暴露方法，关闭选择弹窗', payload: '() => void' },
  { name: 'getValue', description: '组件暴露方法，获取当前选中 ID 数组', payload: '() => number[]' },
  { name: 'clear', description: '组件暴露方法，清空选择', payload: '() => void' },
];

const valueTable = [
  { mode: '单选', value: 'number | undefined', scene: '选择一个组织时返回组织 ID，未选择时返回 undefined' },
  { mode: '多选', value: 'number[]', scene: '返回组织 ID 数组，顺序由树组件选中结果决定' },
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
