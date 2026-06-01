<template>
  <DemoDocLayout
    class="directive-view"
    title="功能指令"
    subtitle="权限指令用于根据当前用户权限控制元素显示，适合按钮、操作列、下拉菜单等前端可见性控制。"
    content-box
    :toc-items="tocItems"
  >
    <section id="simulator" class="doc-section">
      <h2>权限模拟器</h2>
      <p>勾选权限后，下方示例会按相同规则显示或隐藏按钮。</p>
      <div class="demo-block">
        <div class="demo-source">
          <el-checkbox-group v-model="currentPermissions" class="permission-options">
            <el-checkbox v-for="item in permissionOptions" :key="item" :label="item">{{ item }}</el-checkbox>
          </el-checkbox-group>
          <div class="demo-actions">
            <el-button type="primary" @click="resetPermissions">重置权限</el-button>
            <el-button @click="currentPermissions = []">清空权限</el-button>
          </div>
          <div class="permission-status">
            <el-tag v-for="perm in currentPermissions" :key="perm" type="success">{{ perm }}</el-tag>
            <el-tag v-if="currentPermissions.length === 0" type="info">无任何权限</el-tag>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('simulator')">
          <el-icon><component :is="codeVisible.simulator ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.simulator ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.simulator" :code="simulatorCode" />
      </div>
    </section>

    <section id="single" class="doc-section">
      <h2>v-auth 单权限</h2>
      <p>传入单个权限码，用户拥有该权限时显示元素。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="auth-demo">
            <el-button v-if="hasPermission('admin:user:add')" type="primary">
              <el-icon><Plus /></el-icon>
              新增用户
            </el-button>
            <el-button v-if="hasPermission('admin:user:delete')" type="danger">
              <el-icon><Delete /></el-icon>
              删除用户
            </el-button>
            <el-button v-if="!hasPermission('admin:user:delete')" disabled>缺少删除权限</el-button>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('single')">
          <el-icon><component :is="codeVisible.single ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.single ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.single" :code="singleCode" />
      </div>
    </section>

    <section id="any" class="doc-section">
      <h2>v-auths 任一权限</h2>
      <p>传入权限数组，用户拥有其中任一权限时显示元素，适合合并入口。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="auth-demo">
            <el-button v-if="hasAnyPermission(['admin:user:add', 'admin:user:edit'])" type="primary">
              <el-icon><Edit /></el-icon>
              新增或编辑
            </el-button>
            <el-button v-if="hasAnyPermission(['admin:user:delete', 'admin:user:export'])" type="warning">
              <el-icon><Download /></el-icon>
              删除或导出
            </el-button>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('any')">
          <el-icon><component :is="codeVisible.any ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.any ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.any" :code="anyCode" />
      </div>
    </section>

    <section id="all" class="doc-section">
      <h2>v-auth-all 全部权限</h2>
      <p>传入权限数组，用户同时拥有所有权限时显示元素，适合高风险操作。</p>
      <div class="demo-block">
        <div class="demo-source">
          <div class="auth-demo">
            <el-button v-if="hasAllPermissions(['admin:user:add', 'admin:user:edit', 'admin:user:delete'])" type="danger">
              <el-icon><Setting /></el-icon>
              超级管理
            </el-button>
            <el-button v-if="hasAllPermissions(['admin:user:query', 'admin:user:export'])" type="success">
              <el-icon><DataLine /></el-icon>
              查询导出
            </el-button>
          </div>
        </div>
        <div class="op-btns" @click="toggleCode('all')">
          <el-icon><component :is="codeVisible.all ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.all ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.all" :code="allCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="指令" width="150" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="绑定值" min-width="220" />
        <el-table-column prop="defaultValue" label="默认值" width="120" />
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
import { ArrowDown, ArrowUp, DataLine, Delete, Download, Edit, Plus, Setting } from '@element-plus/icons-vue';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

const tocItems = [
  { id: 'simulator', label: '权限模拟器' },
  { id: 'single', label: 'v-auth' },
  { id: 'any', label: 'v-auths' },
  { id: 'all', label: 'v-auth-all' },
  { id: 'props', label: '支持属性' },
  { id: 'slots', label: '支持插槽' },
  { id: 'events', label: '支持方法 / 事件' },
  { id: 'value', label: '返回字段' },
];

const permissionOptions = ['admin:user:add', 'admin:user:edit', 'admin:user:delete', 'admin:user:query', 'admin:user:export'];
const currentPermissions = ref<string[]>(['admin:user:add', 'admin:user:edit', 'admin:user:query']);
const codeVisible = ref<Record<string, boolean>>({
  simulator: false,
  single: false,
  any: false,
  all: false,
});

const simulatorCode = `const permissions = ref([
  'admin:user:add',
  'admin:user:edit',
  'admin:user:query',
]);`;
const singleCode = `<el-button v-auth="'admin:user:add'" type="primary">
  新增用户
</el-button>`;
const anyCode = `<el-button
  v-auths="['admin:user:add', 'admin:user:edit']"
  type="primary"
>
  新增或编辑
</el-button>`;
const allCode = `<el-button
  v-auth-all="['admin:user:add', 'admin:user:edit', 'admin:user:delete']"
  type="danger"
>
  超级管理
</el-button>`;

const propsTable = [
  { name: 'v-auth', description: '单权限判断，拥有指定权限时显示元素', type: 'string | string[]', defaultValue: '-' },
  { name: 'v-auths', description: '任一权限判断，拥有数组中任一权限时显示元素', type: 'string[]', defaultValue: '-' },
  { name: 'v-auth-all', description: '全部权限判断，必须拥有数组中所有权限才显示元素', type: 'string[]', defaultValue: '-' },
];

const slotsTable = [
  { name: '-', description: '指令作用于宿主元素，不提供插槽', scope: '-' },
];

const eventsTable = [
  { name: 'registerAuthDirectives', description: '全局注册权限指令，应用启动时执行', payload: '(app: App) => void' },
  { name: 'auth', description: '权限判断工具函数，供 v-auth 使用', payload: '(permission: string | string[]) => boolean' },
  { name: 'auths', description: '任一权限判断工具函数，供 v-auths 使用', payload: '(permissions: string[]) => boolean' },
  { name: 'authAll', description: '全部权限判断工具函数，供 v-auth-all 使用', payload: '(permissions: string[]) => boolean' },
];

const valueTable = [
  { field: '绑定值', type: 'string | string[]', description: '权限码或权限码数组，例如 admin:user:add' },
  { field: '判断结果', type: 'boolean', description: '内部权限函数返回 true 时保留元素，false 时移除或隐藏元素' },
  { field: '安全边界', type: '说明', description: '该指令只控制前端显示，接口权限仍必须由后端校验' },
];

function hasPermission(permission: string): boolean {
  return currentPermissions.value.includes(permission);
}

function hasAnyPermission(permissions: string[]): boolean {
  return permissions.some((permission) => currentPermissions.value.includes(permission));
}

function hasAllPermissions(permissions: string[]): boolean {
  return permissions.every((permission) => currentPermissions.value.includes(permission));
}

function toggleCode(key: string) {
  codeVisible.value[key] = !codeVisible.value[key];
}

function resetPermissions() {
  currentPermissions.value = ['admin:user:add', 'admin:user:edit', 'admin:user:query'];
  ElMessage.success('权限已重置');
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.permission-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px 14px;
}

.demo-actions,
.auth-demo,
.permission-status {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}
</style>
