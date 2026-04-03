<template>
  <div class="org-selector-view-container">
    <h1>组织架构选择器</h1>
    <p class="subtitle">
      基于 el-tree 的组织架构树形选择组件，支持单选、多选、搜索过滤等功能
    </p>

    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>组织架构选择器 (OrgSelector)</span>
        </div>
      </template>

      <el-form
        label-width="100px"
        style="max-width: 600px"
      >
        <el-form-item label="单选模式">
          <OrgSelector
            v-model="singleValue"
            placeholder="请选择组织"
            clearable
          />
        </el-form-item>

        <el-form-item label="多选模式">
          <OrgSelector
            v-model="multiValue"
            placeholder="请选择组织"
            clearable
          />
        </el-form-item>

        <el-form-item label="禁用状态">
          <OrgSelector
            v-model="disabledValue"
            :disabled="true"
            placeholder="禁用状态"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleSubmit"
          >
            提交
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <div
        v-if="singleValue !== undefined || multiValue.length > 0 || disabledValue.length > 0"
        class="result"
      >
        <el-divider>选中值</el-divider>
        <el-tag
          v-if="singleValue !== undefined"
          class="result-item"
        >
          单选: {{ singleValue }}
        </el-tag>
        <el-tag
          v-for="org in multiValue"
          :key="org"
          type="success"
          class="result-item"
        >
          {{ org }}
        </el-tag>
        <el-tag
          v-for="org in disabledValue"
          :key="org"
          type="warning"
          class="result-item"
        >
          {{ org }}
        </el-tag>
      </div>
    </el-card>

    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>组件属性</span>
      </template>
      <el-table :data="propsTableData">
        <el-table-column
          prop="name"
          label="属性名"
          width="150"
        />
        <el-table-column
          prop="type"
          label="类型"
          width="120"
        />
        <el-table-column
          prop="default"
          label="默认值"
          width="100"
        />
        <el-table-column
          prop="description"
          label="说明"
        />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="OrgSelectorView">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import OrgSelector from '@/components/OrgSelector/index.vue';

const singleValue = ref<number>();
const multiValue = ref<number[]>([]);
const disabledValue = ref<number[]>([1]);

const propsTableData = [
  { name: 'modelValue', type: 'string | string[]', default: '-', description: '选中值 (v-model)' },
  { name: 'multiple', type: 'boolean', default: 'false', description: '是否多选' },
  { name: 'showOrgName', type: 'boolean', default: 'false', description: '是否显示组织名称' },
  { name: 'placeholder', type: 'string', default: '-', description: '占位文本' },
  { name: 'clearable', type: 'boolean', default: 'false', description: '是否可清空' },
  { name: 'disabled', type: 'boolean', default: 'false', description: '是否禁用' },
];

function handleSubmit() {
  ElMessage.success({
    message: `单选: ${singleValue.value}\n多选: ${multiValue.value.join(',')}`,
    duration: 0,
    showClose: true,
  });
}

function handleReset() {
  singleValue.value = undefined;
  multiValue.value = [];
}
</script>

<style scoped lang="scss">
.org-selector-view-container {
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
      align-items: center;
      justify-content: space-between;
    }

    .result {
      margin-top: 20px;
      padding-top: 20px;
      border-top: 1px solid #ebeef5;
    }

    .result-item {
      margin-right: 10px;
      margin-bottom: 10px;
    }
  }
}
</style>
