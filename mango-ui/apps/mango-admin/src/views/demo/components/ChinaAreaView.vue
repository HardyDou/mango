<template>
  <div class="china-area-view-container">
    <h1>省市区选择器</h1>
    <p class="subtitle">
      基于 el-cascader 的省市区街道四级联动选择组件，支持 v-model 双向绑定
    </p>

    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>省市区选择器 (ChinaArea)</span>
        </div>
      </template>

      <el-form
        label-width="100px"
        style="max-width: 500px"
      >
        <el-form-item label="所在地区">
          <ChinaArea
            v-model="areaValue"
            :level="3"
            placeholder="请选择省市区"
            clearable
          />
        </el-form-item>

        <el-form-item label="四级联动">
          <ChinaArea
            v-model="areaValue4"
            :level="4"
            placeholder="请选择省市区街道"
            clearable
          />
        </el-form-item>

        <el-form-item label="只显示街道">
          <ChinaArea
            v-model="streetOnly"
            :level="4"
            :show-all-levels="false"
            placeholder="请选择街道"
            clearable
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
        v-if="areaValue || areaValue4 || streetOnly"
        class="result"
      >
        <el-divider>选中值</el-divider>
        <el-tag
          v-if="areaValue"
          class="result-item"
        >
          省市区: {{ areaValue.join(',') }}
        </el-tag>
        <el-tag
          v-if="areaValue4"
          type="success"
          class="result-item"
        >
          四级: {{ areaValue4.join(',') }}
        </el-tag>
        <el-tag
          v-if="streetOnly"
          type="warning"
          class="result-item"
        >
          街道: {{ streetOnly.join(',') }}
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

<script setup lang="ts" name="ChinaAreaView">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ChinaArea } from '@mango/common';

const areaValue = ref<string[]>([]);
const areaValue4 = ref<string[]>([]);
const streetOnly = ref<string[]>([]);

const propsTableData = [
  { name: 'modelValue', type: 'string[]', default: '-', description: '选中值 (v-model)' },
  { name: 'level', type: 'number', default: '3', description: '联动级别 (3=省市县, 4=省市县乡)' },
  { name: 'showAllLevels', type: 'boolean', default: 'true', description: '是否显示完整路径' },
  { name: 'placeholder', type: 'string', default: '-', description: '占位文本' },
  { name: 'clearable', type: 'boolean', default: 'false', description: '是否可清空' },
  { name: 'disabled', type: 'boolean', default: 'false', description: '是否禁用' },
];

function handleSubmit() {
  ElMessage.success({
    message: `省市区: ${areaValue.value.join('/')}\n四级: ${areaValue4.value.join('/')}\n街道: ${streetOnly.value.join('/')}`,
    duration: 0,
    showClose: true,
  });
}

function handleReset() {
  areaValue.value = [];
  areaValue4.value = [];
  streetOnly.value = [];
}
</script>

<style scoped lang="scss">
.china-area-view-container {
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
