<template>
  <DemoDocLayout
    class="dict-component-demo"
    data-page="develop-dict-component"
    title="字典组件"
    subtitle="统一使用 DictSelect 组件，通过 shape=FLAT 或 shape=TREE 控制普通字典和 TREE 字典的选择、回显和返回值处理。"
    content-box
    :toc-items="tocItems"
  >
    <section id="flat" class="doc-section">
      <h2>基础用法</h2>
      <p>组件内部自动调用 FLAT 字典接口。业务页面只传 dictType 和 v-model，不需要手工请求选项。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="flat-select-row">
            <div class="flat-select-item">
              <div class="flat-select-label">单选</div>
              <DictSelect
                v-model="flatSingleValue"
                dict-type="sys_normal_disable"
                placeholder="请选择状态"
                clearable
                data-action="flat-single-select"
              />
            </div>
            <div class="flat-select-item flat-select-item--wide">
              <div class="flat-select-label">多选（显示数量）</div>
              <DictSelect
                v-model="flatMultipleValues"
                dict-type="sys_normal_disable"
                placeholder="请选择状态"
                multiple
                collapse-tags
                :collapse-tags-tooltip="true"
                clearable
                data-action="flat-multiple-select"
              />
            </div>
            <div class="flat-select-item flat-select-item--wide">
              <div class="flat-select-label">多选（显示所有标签）</div>
              <DictSelect
                v-model="flatMultipleValues"
                dict-type="sys_normal_disable"
                placeholder="请选择状态"
                multiple
                :collapse-tags="false"
                :collapse-tags-tooltip="false"
                clearable
                data-action="flat-multiple-all-select"
              />
            </div>
          </div>
        </div>
        <div class="op-btns" data-action="toggle-flat-code" @click="toggleCode('flat')">
          <el-icon><component :is="codeVisible.flat ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.flat ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.flat" :code="flatCode" />
      </div>
    </section>

    <section id="tree-single" class="doc-section">
      <h2>树形字典</h2>
      <p>内置示例字典为 system_demo_tree。以下使用同一个「物品分类」字典，展示单选、多选和路径回显。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="tree-single-row">
            <div class="tree-use-case">
              <div class="tree-use-case__title">默认用法</div>
              <div class="tree-use-case__label">物品分类</div>
              <DictSelect
                v-model="treeSingleValue"
                shape="TREE"
                dict-type="system_demo_tree"
                leaf-only
                clearable
                filterable
                data-action="tree-single-select"
              />
            </div>
            <div class="tree-use-case">
              <div class="tree-use-case__title">路径回显</div>
              <div class="tree-use-case__label">物品分类</div>
              <DictSelect
                v-model="treeFormatValue"
                shape="TREE"
                dict-type="system_demo_tree"
                leaf-only
                clearable
                filterable
                display-format="path"
                display-separator=" / "
                data-action="tree-display-select"
              />
            </div>
            <div class="tree-use-case">
              <div class="tree-use-case__title">多选用法（显示数量）</div>
              <div class="tree-use-case__label">物品分类</div>
              <DictSelect
                v-model="treeSimpleMultipleValues"
                shape="TREE"
                dict-type="system_demo_tree"
                :leaf-only="false"
                multiple
                collapse-tags
                clearable
                filterable
                display-format="path"
                display-separator=" / "
                data-action="tree-simple-multiple-select"
              />
            </div>
            <div class="tree-use-case">
              <div class="tree-use-case__title">多选用法（显示全部标签）</div>
              <div class="tree-use-case__label">物品分类</div>
              <DictSelect
                v-model="treeSimpleMultipleAllValues"
                shape="TREE"
                dict-type="system_demo_tree"
                :leaf-only="false"
                multiple
                :collapse-tags="false"
                clearable
                filterable
                display-format="path"
                display-separator=" / "
                data-action="tree-simple-multiple-all-select"
              />
            </div>
          </div>
        </div>
        <div class="op-btns" data-action="toggle-tree-single-code" @click="toggleCode('treeSingle')">
          <el-icon><component :is="codeVisible.treeSingle ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.treeSingle ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.treeSingle" :code="treeSingleCode" />
      </div>
    </section>

    <section id="tree-multiple" class="doc-section">
      <h2>树形字典-选择策略</h2>
      <p>
        多选使用树形复选框。leaf-only=false 时父节点可选；默认父子联动，只勾选部分子节点时父节点显示半选中状态，change 返回已选节点，check-change 返回 indeterminate；设置 check-strictly 后父子不联动，可直接提交父节点本身。
      </p>
      <div class="demo-block">
        <div class="demo-source">
          <el-form label-width="112px" class="demo-panel-medium">
            <el-form-item label="多选分类">
              <DictSelect
                ref="treeMultipleRef"
                v-model="treeMultipleValues"
                shape="TREE"
                dict-type="system_demo_tree"
                :leaf-only="treeMultipleLeafOnly"
                multiple
                clearable
                filterable
                data-action="tree-multiple-select"
              />
            </el-form-item>
            <el-form-item label="选择策略">
              <el-switch
                v-model="treeMultipleLeafOnly"
                active-text="仅叶子节点可选"
                inactive-text="父节点也可选"
                data-action="toggle-tree-leaf-only"
                @change="clearTreeMultiple"
              />
            </el-form-item>
          </el-form>
        </div>
        <div class="op-btns" data-action="toggle-tree-multiple-code" @click="toggleCode('treeMultiple')">
          <el-icon><component :is="codeVisible.treeMultiple ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.treeMultiple ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.treeMultiple" :code="treeMultipleCode" />
      </div>
    </section>

    <section id="display" class="doc-section">
      <h2>回显模式：DictDisplay</h2>
      <p>
        列表、详情等只读场景使用 DictDisplay 把 value 转成 label。它与 DictSelect 共用字典编码和缓存，单值/多值自动识别，默认纯文本、多值用「、」连接，树形字典可切换为完整路径。
      </p>
      <div class="demo-block">
        <div class="demo-source">
          <el-descriptions :column="1" border class="display-panel">
            <el-descriptions-item label="普通字典 · 单值">
              <DictDisplay dict-type="sys_normal_disable" :value="'1'" data-action="display-flat-single" />
            </el-descriptions-item>
            <el-descriptions-item label="普通字典 · 多值">
              <DictDisplay dict-type="sys_normal_disable" :value="['1', '0']" data-action="display-flat-multi" />
            </el-descriptions-item>
            <el-descriptions-item label="普通字典 · 标签样式">
              <DictDisplay
                dict-type="sys_normal_disable"
                :value="['1', '0']"
                mode="tag"
                tag-type="success"
                data-action="display-flat-tag"
              />
            </el-descriptions-item>
            <el-descriptions-item label="普通字典 · 空值">
              <DictDisplay dict-type="sys_normal_disable" :value="undefined" data-action="display-flat-empty" />
            </el-descriptions-item>
            <el-descriptions-item label="树形字典 · 末级 label">
              <DictDisplay dict-type="system_demo_tree" shape="TREE" :value="'paper'" data-action="display-tree-label" />
            </el-descriptions-item>
            <el-descriptions-item label="树形字典 · 多值路径">
              <DictDisplay
                dict-type="system_demo_tree"
                shape="TREE"
                :value="['paper', 'computer']"
                display-format="path"
                data-action="display-tree-path"
              />
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="op-btns" data-action="toggle-display-code" @click="toggleCode('display')">
          <el-icon><component :is="codeVisible.display ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.display ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.display" :code="displayCode" />
      </div>
    </section>

    <section id="group" class="doc-section">
      <h2>分组过滤：group / include / exclude</h2>
      <p>
        同一个字典可以给每个值打一个或多个分组标记（如 ORDER、AFTER_SALE）。组件不传 group 时保持全量；传 group 时只取该组内的值；再叠加 include 白名单与 exclude 黑名单做点对点微调。内置示例字典 system_demo_group：值1 属于 ORDER，值2、3 同属两组，值4 属于 AFTER_SALE。
      </p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="tree-single-row">
            <div class="tree-use-case">
              <div class="tree-use-case__title">A 处：ORDER 组</div>
              <div class="tree-use-case__label">可选值</div>
              <DictSelect
                v-model="groupOrderValue"
                dict-type="system_demo_group"
                group="ORDER"
                placeholder="只显示 ORDER 组"
                clearable
                data-action="group-order-select"
              />
              <div class="tree-use-case__label group-display-label">回显</div>
              <DictDisplay
                dict-type="system_demo_group"
                group="ORDER"
                :value="groupOrderValue"
                data-action="group-order-display"
              />
            </div>
            <div class="tree-use-case">
              <div class="tree-use-case__title">B 处：AFTER_SALE 组</div>
              <div class="tree-use-case__label">可选值</div>
              <DictSelect
                v-model="groupAfterSaleValue"
                dict-type="system_demo_group"
                group="AFTER_SALE"
                placeholder="只显示 AFTER_SALE 组"
                clearable
                data-action="group-after-sale-select"
              />
              <div class="tree-use-case__label group-display-label">回显</div>
              <DictDisplay
                dict-type="system_demo_group"
                group="AFTER_SALE"
                :value="groupAfterSaleValue"
                data-action="group-after-sale-display"
              />
            </div>
            <div class="tree-use-case">
              <div class="tree-use-case__title">组内再收窄</div>
              <div class="tree-use-case__label">include=[2,3] 且 exclude=[3]</div>
              <DictSelect
                v-model="groupNarrowValue"
                dict-type="system_demo_group"
                group="ORDER"
                :include-values="['2', '3']"
                :exclude-values="['3']"
                placeholder="只剩值2"
                clearable
                data-action="group-narrow-select"
              />
              <div class="tree-use-case__label group-display-label">回显</div>
              <DictDisplay
                dict-type="system_demo_group"
                group="ORDER"
                :include-values="['2', '3']"
                :exclude-values="['3']"
                :value="groupNarrowValue"
                data-action="group-narrow-display"
              />
            </div>
            <div class="tree-use-case">
              <div class="tree-use-case__title">不传 group（全量）</div>
              <div class="tree-use-case__label">可选值</div>
              <DictSelect
                v-model="groupAllValue"
                dict-type="system_demo_group"
                placeholder="显示全部值"
                clearable
                data-action="group-all-select"
              />
              <div class="tree-use-case__label group-display-label">回显</div>
              <DictDisplay dict-type="system_demo_group" :value="groupAllValue" data-action="group-all-display" />
            </div>
          </div>
        </div>
        <div class="op-btns" data-action="toggle-group-code" @click="toggleCode('group')">
          <el-icon><component :is="codeVisible.group ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.group ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.group" :code="groupCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>组件参数</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="component" label="组件" width="150" />
        <el-table-column prop="name" label="参数" width="180" />
        <el-table-column prop="description" label="说明" min-width="320" />
        <el-table-column prop="type" label="类型" min-width="170" />
        <el-table-column prop="defaultValue" label="默认值" width="140" />
      </el-table>
    </section>

    <section id="slots" class="doc-section">
      <h2>插槽</h2>
      <p>
        组件提供 <code>selected</code> 插槽，用于自定义选中项在选择框内部的显示内容。不传插槽时，组件按
        <code>display-format</code> 和 <code>display-separator</code> 自动输出 label 或完整路径。
      </p>
      <p>
        插槽参数为 <code>value</code>（选中值）、<code>label</code>（节点名称）和 <code>text</code>（组件已格式化好的显示文本）。
        多选时每个选中项分别渲染一次，适合在标签前补充前缀、图标或业务标识。
      </p>
      <p>
        插槽只影响展示，不改变提交值和回显值；数据仍以 <code>v-model</code> 绑定的 value 为准。
      </p>
      <div class="demo-block">
        <div class="demo-source">
          <DictSelect
            v-model="slotDemoValue"
            shape="TREE"
            dict-type="system_demo_tree"
            leaf-only
            display-format="path"
            display-separator=" / "
            clearable
            filterable
            data-action="tree-slot-select"
          >
            <template #selected="{ text }">
              <span class="slot-selected-text">分类：{{ text }}</span>
            </template>
          </DictSelect>
        </div>
        <div class="op-btns" data-action="toggle-slots-code" @click="toggleCode('slots')">
          <el-icon><component :is="codeVisible.slots ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.slots ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.slots" :code="slotCode" />
      </div>
    </section>

    <section id="events" class="doc-section api-section">
      <h2>事件和方法</h2>
      <el-table :data="eventsTable" size="small" border>
        <el-table-column prop="component" label="组件" width="150" />
        <el-table-column prop="name" label="名称" width="210" />
        <el-table-column prop="description" label="说明" min-width="320" />
        <el-table-column prop="payload" label="参数 / 返回值" min-width="260" />
      </el-table>
      <h3 class="api-subtitle">change 返回字段</h3>
      <el-table :data="eventFieldsTable" size="small" border>
        <el-table-column prop="scene" label="场景" width="110" />
        <el-table-column prop="field" label="字段" width="120" />
        <el-table-column prop="type" label="类型" width="240" />
        <el-table-column prop="description" label="说明" min-width="360" />
      </el-table>
    </section>

    <section id="api" class="doc-section api-section">
      <h2>数据来源和内置测试字典</h2>
      <p>业务页面优先使用组件；底层 API 仅用于扩展场景。示例不要求用户手工创建字典，也不要求猜测字典编码。</p>
      <el-table :data="apiTable" size="small" border>
        <el-table-column prop="name" label="名称" width="260" />
        <el-table-column prop="description" label="说明" min-width="360" />
        <el-table-column prop="example" label="示例" min-width="260" />
      </el-table>
    </section>
  </DemoDocLayout>
</template>

<script setup lang="ts" name="DictComponentDemo">
import { ref } from 'vue';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { DictDisplay, DictSelect } from '@mango/common';
import DemoCodeBlock from '@mango/admin-shell/dev-code-block';
import DemoDocLayout from '@mango/admin-shell/dev-demo-layout';

const tocItems = [
  { id: 'flat', label: '基础用法' },
  { id: 'tree-single', label: '树形字典' },
  { id: 'tree-multiple', label: '树形字典-选择策略' },
  { id: 'display', label: '回显模式：DictDisplay' },
  { id: 'group', label: '分组过滤：group / include / exclude' },
  { id: 'props', label: '组件参数' },
  { id: 'slots', label: '插槽' },
  { id: 'events', label: '事件和方法' },
  { id: 'api', label: '数据来源和内置字典' },
];

const flatSingleValue = ref<string>('1');
const flatMultipleValues = ref<string[]>(['1', '0']);
const treeSingleValue = ref<string>('paper');
const treeFormatValue = ref<string>('paper');
const treeSimpleMultipleValues = ref<string[]>(['paper']);
const treeSimpleMultipleAllValues = ref<string[]>(['paper', 'pen', 'computer']);
const slotDemoValue = ref<string>('paper');
const treeMultipleValues = ref<string[]>(['paper']);
const groupOrderValue = ref<string>('1');
const groupAfterSaleValue = ref<string>('4');
const groupNarrowValue = ref<string>('2');
const groupAllValue = ref<string>('1');

const treeMultipleLeafOnly = ref(false);
const treeMultipleRef = ref<InstanceType<typeof DictSelect>>();
const codeVisible = ref<Record<string, boolean>>({
  flat: false,
  treeSingle: false,
  treeMultiple: false,
  slots: false,
});

const flatCode = `<div class="form-row">
  <DictSelect
    v-model="status"
    dict-type="sys_normal_disable"
    clearable
  />

  <!-- 多选折叠标签并显示 +N 数量，悬浮可查看全部标签 -->
  <DictSelect
    v-model="statuses"
    dict-type="sys_normal_disable"
    multiple
    collapse-tags
    collapse-tags-tooltip
    clearable
  />

  <!-- 多选直接显示所有标签 -->
  <DictSelect
    v-model="statuses"
    dict-type="sys_normal_disable"
    multiple
    :collapse-tags="false"
    clearable
  />
</div>`;

const treeSingleCode = `<div class="form-row">
  <!-- 默认单选：仅允许选择叶子节点 -->
  <DictSelect
    v-model="categoryValue"
    dict-type="system_demo_tree"
    shape="TREE"
    leaf-only
    clearable
    filterable
  />

  <!-- 路径回显：直接显示在选择框中 -->
  <DictSelect
    v-model="categoryValue"
    dict-type="system_demo_tree"
    shape="TREE"
    leaf-only
    display-format="path"
    display-separator=" / "
    clearable
    filterable
  />

  <!-- 多选路径回显：允许选择父节点，并显示数量 -->
  <DictSelect
    v-model="categoryValues"
    dict-type="system_demo_tree"
    shape="TREE"
    multiple
    :leaf-only="false"
    display-format="path"
    display-separator=" / "
    collapse-tags
    clearable
    filterable
  />

  <!-- 多选直接显示全部路径，不显示 +N 数量 -->
  <DictSelect
    v-model="categoryValues"
    dict-type="system_demo_tree"
    shape="TREE"
    multiple
    :leaf-only="false"
    display-format="path"
    display-separator=" / "
    :collapse-tags="false"
    clearable
    filterable
  />
</div>`;

const groupCode = `<!-- 值可属于多个分组：值2、3 同时属于 ORDER 与 AFTER_SALE -->
<!-- A 处只看 ORDER 组：值1、值2、值3 -->
<DictSelect dict-type="system_demo_group" group="ORDER" v-model="form.status" />
<DictDisplay dict-type="system_demo_group" group="ORDER" :value="row.status" />

<!-- B 处只看 AFTER_SALE 组：值2、值3、值4 -->
<DictDisplay dict-type="system_demo_group" group="AFTER_SALE" :value="row.status" />

<!-- 组内再收窄：先取 ORDER 组，再限定 include 并剔除 exclude -->
<DictSelect
  dict-type="system_demo_group"
  group="ORDER"
  :include-values="['2', '3']"
  :exclude-values="['3']"
  v-model="form.status"
/>

<!-- 不传 group 时保持全量，向后兼容 -->
<DictSelect dict-type="system_demo_group" v-model="form.status" />`;

const displayCode = `<!-- 列表 / 详情：value 转 label，单值多值自动识别 -->
<DictDisplay dict-type="sys_normal_disable" :value="row.status" />

<!-- 多值默认用顿号连接 -->
<DictDisplay dict-type="sys_normal_disable" :value="row.statuses" />

<!-- 状态列：标签样式 -->
<DictDisplay dict-type="sys_normal_disable" :value="row.statuses" mode="tag" tag-type="success" />

<!-- 树形字典：默认末级 label，需要上下文时展示完整路径 -->
<DictDisplay dict-type="system_demo_tree" shape="TREE" :value="row.category" />
<DictDisplay
  dict-type="system_demo_tree"
  shape="TREE"
  :value="row.categories"
  display-format="path"
  display-separator=" / "
/>`;

const slotCode = `<DictSelect
  v-model="categoryValue"
  dict-type="system_demo_tree"
  shape="TREE"
  leaf-only
  display-format="path"
  display-separator=" / "
>
  <template #selected="{ text }">
    分类：{{ text }}
  </template>
</DictSelect>`;

const treeMultipleCode = `<DictSelect
  v-model="categoryValues"
  dict-type="system_demo_tree"
  shape="TREE"
  multiple
  :leaf-only="false"
/>`;

const propsTable = [
  { component: 'DictSelect', name: 'shape', description: '选择字典形态；FLAT 使用下拉选择，TREE 使用树形选择。', type: "'FLAT' | 'TREE'", defaultValue: 'FLAT' },
  { component: 'DictSelect', name: 'dictType', description: '字典编码；shape=FLAT 调用 options，shape=TREE 调用 tree。', type: 'string', defaultValue: '必填' },
  { component: 'DictSelect', name: 'modelValue', description: '单选 value 或多选 value[]；已有值会自动回显。', type: 'string | string[]', defaultValue: 'undefined' },
  { component: 'DictSelect', name: 'multiple', description: '是否启用多选；TREE 多选显示复选框和半选中状态。', type: 'boolean', defaultValue: 'false' },
  { component: 'DictSelect', name: 'leafOnly', description: 'TREE 专用；true 禁用父节点，只能选择叶子；false 允许父节点并保留半选中状态。', type: 'boolean', defaultValue: 'true' },
  { component: 'DictSelect', name: 'checkStrictly', description: 'TREE 专用；false 为父子联动，true 为父子不联动（单选选父节点需开启）。', type: 'boolean', defaultValue: 'false' },
  { component: 'DictSelect', name: 'displayFormat / displaySeparator', description: 'TREE 回显使用 label 或完整路径，并配置路径分隔符。', type: "'label' | 'path' / string", defaultValue: 'label /  / ' },
];

const eventsTable = [
  { component: 'DictSelect', name: 'update:modelValue', description: 'FLAT/TREE 选择值变化。', payload: 'string | string[] | undefined' },
  { component: 'DictSelect', name: 'change', description: '返回 value、已选节点 nodes 和完整路径 paths。', payload: '(value, nodes, paths) => void' },
  { component: 'DictSelect', name: 'check-change', description: 'TREE 复选框变化；indeterminate=true 表示父节点半选中。', payload: '(node, checked, indeterminate) => void' },
  { component: 'DictSelect', name: 'reload', description: '暴露方法，重新加载当前字典。', payload: '() => Promise<DictOption[] | DictTreeOption[]>' },
  { component: 'DictSelect', name: 'getSelectedNodes / getSelectedPaths', description: '暴露方法，读取选中节点和完整路径。', payload: '() => DictTreeOption[] / DictTreeOption[][]' },
  { component: 'DictSelect', name: 'getHalfCheckedNodes', description: '暴露方法，读取半选中的父节点，便于提交父级 ID。', payload: '() => DictTreeOption[]' },
  { component: 'DictSelect', name: 'clear / focus / blur', description: '暴露方法，清空、聚焦和失焦。', payload: '() => void' },
];

const eventFieldsTable = [
  { scene: '单选', field: 'value', type: 'string | number | undefined', description: '当前选中的节点 value；未选择时为 undefined。' },
  { scene: '单选', field: 'nodes', type: 'DictTreeOption[]', description: '当前选中的节点对象，单选通常包含 0 或 1 个节点。' },
  { scene: '单选', field: 'paths', type: 'DictTreeOption[][]', description: '当前节点从根节点到自身的完整路径。' },
  { scene: '多选', field: 'value', type: 'Array<string | number>', description: '当前选中的多个节点 value。' },
  { scene: '多选', field: 'nodes', type: 'DictTreeOption[]', description: '当前选中的多个节点对象。' },
  { scene: '多选', field: 'paths', type: 'DictTreeOption[][]', description: '每个选中节点对应一条从根节点到自身的完整路径。' },
];

const apiTable = [
  { name: 'system_demo_tree', description: '系统内置 TREE 示例字典，不需要用户创建。', example: '办公用品→纸张、笔；数码设备→电脑、显示器' },
  { name: 'DictSelect 内部 API', description: '调用 /system/dict/data/options。业务页面通常直接使用 DictSelect。', example: 'FLAT label/value' },
  { name: 'DictSelect shape="TREE" 内部 API', description: '调用 /system/dict/data/tree。业务页面通常直接使用 DictSelect。', example: 'TREE children/value' },
];

function clearTreeMultiple() {
  treeMultipleValues.value = [];
  treeMultipleRef.value?.clear();
}

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}
</script>

<style scoped lang="scss">
.flat-select-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 20px;
}

.flat-select-item {
  min-width: 180px;
}

.flat-select-item--wide {
  min-width: 240px;
}

.flat-select-label {
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.tree-single-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
  gap: 24px;
}

.tree-use-case {
  min-width: 0;
}

.tree-use-case .dict-select {
  width: 100%;
}

.tree-use-case .dict-select__control {
  width: 100%;
}

.tree-use-case__title {
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.tree-use-case__label {
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.display-panel {
  max-width: 720px;
}

.group-display-label {
  margin-top: 10px;
}

.api-subtitle {
  margin: 20px 0 12px;
  font-size: 16px;
}

.slot-selected-text {
  color: var(--el-color-primary);
}

pre {
  margin: 0;
  white-space: pre-wrap;
}
</style>
