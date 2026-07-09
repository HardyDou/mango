<template>
  <DemoDocLayout
    class="search-panel-view-container"
    title="搜索面板"
    subtitle="MangoSearchPanel 用于统一后台列表页查询区域，业务页面只声明查询字段，由组件负责字段栅格、按钮位置和展开收起。"
    content-box
    :toc-items="tocItems"
  >
    <section id="basic" class="doc-section">
      <h2>基础查询</h2>
      <p>适合字段较少的列表页，查询和重置按钮固定在搜索卡片右下角。</p>
      <div class="demo-block">
        <div class="demo-source">
          <MangoSearchPanel :model="basicQuery" @search="handleSearch('基础查询')" @reset="resetBasicQuery">
            <el-form-item label="关键字">
              <el-input v-model="basicQuery.keyword" placeholder="请输入名称或编码" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="basicQuery.status" placeholder="请选择状态" clearable>
                <el-option label="启用" value="enabled" />
                <el-option label="停用" value="disabled" />
              </el-select>
            </el-form-item>
          </MangoSearchPanel>
        </div>
        <div class="op-btns" @click="toggleCode('basic')">
          <el-icon><component :is="codeVisible.basic ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.basic ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.basic" :code="basicCode" />
      </div>
    </section>

    <section id="collapsible" class="doc-section">
      <h2>可折叠查询</h2>
      <p>字段较多时启用 collapsible，收起态展示高频字段，展开态展示全部字段。</p>
      <div class="demo-block">
        <div class="demo-source">
          <MangoSearchPanel
            :model="advancedQuery"
            collapsible
            :collapsed-count="3"
            @search="handleSearch('可折叠查询')"
            @reset="resetAdvancedQuery"
          >
            <el-form-item label="用户名称">
              <el-input v-model="advancedQuery.userName" placeholder="请输入用户名称" clearable />
            </el-form-item>
            <el-form-item label="所属部门">
              <el-input v-model="advancedQuery.deptName" placeholder="请输入部门名称" clearable />
            </el-form-item>
            <el-form-item label="账号状态">
              <el-select v-model="advancedQuery.status" placeholder="请选择账号状态" clearable>
                <el-option label="正常" value="normal" />
                <el-option label="冻结" value="frozen" />
              </el-select>
            </el-form-item>
            <el-form-item label="创建时间">
              <el-date-picker
                v-model="advancedQuery.createdRange"
                type="daterange"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="advancedQuery.mobile" placeholder="请输入手机号" clearable />
            </el-form-item>
          </MangoSearchPanel>
        </div>
        <div class="op-btns" @click="toggleCode('collapsible')">
          <el-icon><component :is="codeVisible.collapsible ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.collapsible ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.collapsible" :code="collapsibleCode" />
      </div>
    </section>

    <section id="fixed-columns" class="doc-section">
      <h2>固定四列查询</h2>
      <p>适合字段较多但希望桌面端保持四列排版的业务页，收起态默认展示两行，更多按钮可放在搜索区底部居中。</p>
      <div class="demo-block">
        <div class="demo-source">
          <MangoSearchPanel
            :model="fixedQuery"
            collapsible
            :columns="4"
            :collapsed-rows="2"
            more-placement="bottom"
            @search="handleSearch('固定四列查询')"
            @reset="resetFixedQuery"
          >
            <el-form-item label="项目名称">
              <el-input v-model="fixedQuery.projectName" placeholder="请输入项目名称" clearable />
            </el-form-item>
            <el-form-item label="项目编号">
              <el-input v-model="fixedQuery.projectCode" placeholder="请输入项目编号" clearable />
            </el-form-item>
            <el-form-item label="客户名称">
              <el-input v-model="fixedQuery.customerName" placeholder="请输入客户名称" clearable />
            </el-form-item>
            <el-form-item label="所属机构">
              <el-input v-model="fixedQuery.orgName" placeholder="请输入所属机构" clearable />
            </el-form-item>
            <el-form-item label="业务类型">
              <el-select v-model="fixedQuery.businessType" placeholder="请选择业务类型" clearable>
                <el-option label="投标保函" value="bid" />
                <el-option label="履约保函" value="performance" />
              </el-select>
            </el-form-item>
            <el-form-item label="保函状态">
              <el-select v-model="fixedQuery.status" placeholder="请选择保函状态" clearable>
                <el-option label="待提交" value="draft" />
                <el-option label="审批中" value="approval" />
              </el-select>
            </el-form-item>
            <el-form-item label="申请日期">
              <el-date-picker v-model="fixedQuery.applyDate" type="date" placeholder="请选择申请日期" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="到期日期">
              <el-date-picker v-model="fixedQuery.expireDate" type="date" placeholder="请选择到期日期" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="经办人">
              <el-input v-model="fixedQuery.operatorName" placeholder="请输入经办人" clearable />
            </el-form-item>
          </MangoSearchPanel>
        </div>
        <div class="op-btns" @click="toggleCode('fixedColumns')">
          <el-icon><component :is="codeVisible.fixedColumns ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.fixedColumns ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.fixedColumns" :code="fixedColumnsCode" />
      </div>
    </section>

    <section id="list-page" class="doc-section">
      <h2>列表页组合</h2>
      <p>标准列表页建议和 MangoListPage、MangoListPanel、Pagination 一起使用。</p>
      <div class="demo-block">
        <div class="demo-source">
          <MangoListPage class="demo-list-page">
            <template #search>
              <MangoSearchPanel :model="listQuery" @search="handleSearch('列表页组合')" @reset="resetListQuery">
                <el-form-item label="菜单名称">
                  <el-input v-model="listQuery.menuName" placeholder="请输入菜单名称" clearable />
                </el-form-item>
                <el-form-item label="菜单类型">
                  <el-select v-model="listQuery.menuType" placeholder="请选择菜单类型" clearable>
                    <el-option label="目录" value="directory" />
                    <el-option label="页面" value="page" />
                    <el-option label="按钮" value="button" />
                  </el-select>
                </el-form-item>
              </MangoSearchPanel>
            </template>

            <MangoListPanel>
              <template #actions>
                <el-button type="primary" plain>新增菜单</el-button>
                <el-button plain>导出</el-button>
              </template>
              <el-table :data="tableData" border>
                <el-table-column prop="name" label="菜单名称" min-width="160" />
                <el-table-column prop="type" label="类型" width="100" />
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.status === '启用' ? 'success' : 'info'">{{ row.status }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
              </el-table>
              <template #pagination>
                <Pagination :total="36" :page="1" :limit="10" />
              </template>
            </MangoListPanel>
          </MangoListPage>
        </div>
        <div class="op-btns" @click="toggleCode('listPage')">
          <el-icon><component :is="codeVisible.listPage ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ codeVisible.listPage ? '隐藏代码' : '显示代码' }}</span>
        </div>
        <DemoCodeBlock v-show="codeVisible.listPage" :code="listPageCode" />
      </div>
    </section>

    <section id="props" class="doc-section api-section">
      <h2>支持属性</h2>
      <el-table :data="propsTable" size="small" border>
        <el-table-column prop="name" label="属性名" width="160" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="type" label="类型" min-width="180" />
        <el-table-column prop="defaultValue" label="默认值" width="130" />
      </el-table>
    </section>

    <section id="slots" class="doc-section api-section">
      <h2>支持插槽</h2>
      <el-table :data="slotsTable" size="small" border>
        <el-table-column prop="name" label="插槽名" width="160" />
        <el-table-column prop="description" label="说明" min-width="320" />
      </el-table>
    </section>

    <section id="events" class="doc-section api-section">
      <h2>支持事件</h2>
      <el-table :data="eventsTable" size="small" border>
        <el-table-column prop="name" label="事件名" width="160" />
        <el-table-column prop="description" label="说明" min-width="260" />
        <el-table-column prop="payload" label="参数" min-width="180" />
      </el-table>
    </section>
  </DemoDocLayout>
</template>

<script setup lang="ts" name="SearchPanelView">
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import DemoCodeBlock from './DemoCodeBlock.vue';
import DemoDocLayout from './DemoDocLayout.vue';

type CodeBlockKey = 'basic' | 'collapsible' | 'fixedColumns' | 'listPage';

const tocItems = [
  { id: 'basic', label: '基础查询' },
  { id: 'collapsible', label: '可折叠查询' },
  { id: 'fixed-columns', label: '固定四列查询' },
  { id: 'list-page', label: '列表页组合' },
  { id: 'props', label: '支持属性' },
  { id: 'slots', label: '支持插槽' },
  { id: 'events', label: '支持事件' },
];

const codeVisible = ref<Record<CodeBlockKey, boolean>>({
  basic: false,
  collapsible: false,
  fixedColumns: false,
  listPage: false,
});

const basicQuery = reactive({
  keyword: '',
  status: '',
});

const advancedQuery = reactive({
  userName: '',
  deptName: '',
  status: '',
  createdRange: [] as string[],
  mobile: '',
});

const fixedQuery = reactive({
  projectName: '',
  projectCode: '',
  customerName: '',
  orgName: '',
  businessType: '',
  status: '',
  applyDate: '',
  expireDate: '',
  operatorName: '',
});

const listQuery = reactive({
  menuName: '',
  menuType: '',
});

const tableData = [
  { name: '系统管理', type: '目录', status: '启用', updatedAt: '2026-07-08 09:30:00' },
  { name: '菜单管理', type: '页面', status: '启用', updatedAt: '2026-07-08 10:12:00' },
  { name: '删除菜单', type: '按钮', status: '停用', updatedAt: '2026-07-08 10:40:00' },
];

const basicCode = `<MangoSearchPanel :model="query" @search="handleSearch" @reset="handleReset">
  <el-form-item label="关键字">
    <el-input v-model="query.keyword" placeholder="请输入名称或编码" clearable />
  </el-form-item>
  <el-form-item label="状态">
    <el-select v-model="query.status" placeholder="请选择状态" clearable>
      <el-option label="启用" value="enabled" />
      <el-option label="停用" value="disabled" />
    </el-select>
  </el-form-item>
</MangoSearchPanel>`;

const collapsibleCode = `<MangoSearchPanel
  :model="query"
  collapsible
  :collapsed-count="3"
  @search="handleSearch"
  @reset="handleReset"
>
  <el-form-item label="用户名称">
    <el-input v-model="query.userName" clearable />
  </el-form-item>
  <el-form-item label="所属部门">
    <el-input v-model="query.deptName" clearable />
  </el-form-item>
  <el-form-item label="账号状态">
    <el-select v-model="query.status" clearable />
  </el-form-item>
  <el-form-item label="创建时间">
    <el-date-picker v-model="query.createdRange" type="daterange" />
  </el-form-item>
</MangoSearchPanel>`;

const fixedColumnsCode = `<MangoSearchPanel
  :model="query"
  collapsible
  :columns="4"
  :collapsed-rows="2"
  more-placement="bottom"
  @search="handleSearch"
  @reset="handleReset"
>
  <el-form-item label="项目名称">
    <el-input v-model="query.projectName" clearable />
  </el-form-item>
  <el-form-item label="项目编号">
    <el-input v-model="query.projectCode" clearable />
  </el-form-item>
  <el-form-item label="客户名称">
    <el-input v-model="query.customerName" clearable />
  </el-form-item>
  <el-form-item label="所属机构">
    <el-input v-model="query.orgName" clearable />
  </el-form-item>
  <el-form-item label="业务类型">
    <el-select v-model="query.businessType" clearable />
  </el-form-item>
  <el-form-item label="保函状态">
    <el-select v-model="query.status" clearable />
  </el-form-item>
  <el-form-item label="申请日期">
    <el-date-picker v-model="query.applyDate" type="date" />
  </el-form-item>
  <el-form-item label="到期日期">
    <el-date-picker v-model="query.expireDate" type="date" />
  </el-form-item>
  <el-form-item label="经办人">
    <el-input v-model="query.operatorName" clearable />
  </el-form-item>
</MangoSearchPanel>`;

const listPageCode = `<MangoListPage>
  <template #search>
    <MangoSearchPanel :model="query" @search="handleSearch" @reset="handleReset">
      <el-form-item label="菜单名称">
        <el-input v-model="query.menuName" clearable />
      </el-form-item>
    </MangoSearchPanel>
  </template>

  <MangoListPanel>
    <template #actions>
      <el-button type="primary" plain>新增菜单</el-button>
    </template>
    <el-table :data="tableData" border />
    <Pagination :total="total" :page="page" :limit="limit" />
  </MangoListPanel>
</MangoListPage>`;

const propsTable = [
  { name: 'model', description: '绑定查询对象，传给内部 ElForm 的 model', type: 'Record<string, unknown>', defaultValue: '-' },
  { name: 'labelWidth', description: '表单 label 宽度', type: 'string | number', defaultValue: '96px' },
  { name: 'searchText', description: '查询按钮文案', type: 'string', defaultValue: '查询' },
  { name: 'resetText', description: '重置按钮文案', type: 'string', defaultValue: '重置' },
  { name: 'showReset', description: '是否展示重置按钮', type: 'boolean', defaultValue: 'true' },
  { name: 'collapsible', description: '是否启用展开收起', type: 'boolean', defaultValue: 'false' },
  { name: 'collapsedRows', description: '收起态展示行数，未设置 collapsedCount 时生效', type: 'number', defaultValue: '1' },
  { name: 'collapsedCount', description: '收起态固定展示字段数量', type: 'number', defaultValue: '-' },
  { name: 'columns', description: '字段区列数；默认自适应，设置为数字时按固定列数排版', type: 'number | auto', defaultValue: 'auto' },
  { name: 'morePlacement', description: '展开收起按钮位置，可放在操作区或搜索区底部居中', type: 'actions | bottom', defaultValue: 'actions' },
  { name: 'fieldMinWidth', description: '自适应列模式下字段最小宽度', type: 'string', defaultValue: '280px' },
  { name: 'fieldMaxWidth', description: '自适应列模式下字段最大宽度', type: 'string', defaultValue: '320px' },
  { name: 'expandText', description: '展开按钮文案', type: 'string', defaultValue: '展开' },
  { name: 'collapseText', description: '收起按钮文案', type: 'string', defaultValue: '收起' },
];

const slotsTable = [
  { name: 'default', description: '查询字段区域，业务页面在这里声明 el-form-item' },
  { name: 'actions', description: '自定义操作区；不传时使用默认的查询、重置、展开或收起按钮' },
];

const eventsTable = [
  { name: 'search', description: '点击查询按钮时触发', payload: '-' },
  { name: 'reset', description: '点击重置按钮时触发，业务页面负责清空查询对象和重新查询', payload: '-' },
  { name: 'expandChange', description: '展开或收起状态变化时触发', payload: 'expanded: boolean' },
];

function resetBasicQuery() {
  basicQuery.keyword = '';
  basicQuery.status = '';
  ElMessage.info('已重置基础查询条件');
}

function resetAdvancedQuery() {
  advancedQuery.userName = '';
  advancedQuery.deptName = '';
  advancedQuery.status = '';
  advancedQuery.createdRange = [];
  advancedQuery.mobile = '';
  ElMessage.info('已重置可折叠查询条件');
}

function resetFixedQuery() {
  fixedQuery.projectName = '';
  fixedQuery.projectCode = '';
  fixedQuery.customerName = '';
  fixedQuery.orgName = '';
  fixedQuery.businessType = '';
  fixedQuery.status = '';
  fixedQuery.applyDate = '';
  fixedQuery.expireDate = '';
  fixedQuery.operatorName = '';
  ElMessage.info('已重置固定四列查询条件');
}

function resetListQuery() {
  listQuery.menuName = '';
  listQuery.menuType = '';
  ElMessage.info('已重置列表页查询条件');
}

function handleSearch(scene: string) {
  ElMessage.success(`${scene}已触发`);
}

function toggleCode(key: CodeBlockKey) {
  codeVisible.value[key] = !codeVisible.value[key];
}
</script>

<style scoped lang="scss">
@use './demo-page.scss';

.search-panel-view-container {
  :deep(.demo-source) {
    text-align: left;
  }
}

.demo-list-page {
  width: 100%;
}
</style>
