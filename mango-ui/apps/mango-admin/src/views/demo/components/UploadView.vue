<template>
  <div class="upload-view-container">
    <h1>文件上传组件</h1>
    <p class="subtitle">
      基于 Element Plus Upload 封装，支持图片、文件、Excel等多种上传方式，自带预览、解析功能
    </p>

    <el-row :gutter="20">
      <!-- 图片上传 -->
      <el-col :span="24">
        <el-card class="demo-card">
          <template #header>
            <div class="card-header">
              <span>图片上传 (ImageUpload)</span>
              <el-switch
                v-model="multiImage"
                active-text="多图"
                inactive-text="单图"
              />
            </div>
          </template>
          <ImageUpload
            v-model="imageUrl"
            :multiple="multiImage"
          />
          <div class="upload-tip">
            <el-tag type="info">
              图片预览
            </el-tag>
            <el-tag type="info">
              自动上传
            </el-tag>
            <el-tag type="info">
              大小限制
            </el-tag>
          </div>
          <div
            v-if="imageUrl"
            class="upload-result"
          >
            <span>返回值：</span>
            <el-input
              v-model="imageUrl"
              readonly
              size="small"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row
      :gutter="20"
      style="margin-top: 20px"
    >
      <!-- 文件上传 -->
      <el-col :span="12">
        <el-card class="demo-card">
          <template #header>
            <span>文件上传 (FileUpload)</span>
          </template>
          <FileUpload v-model="fileUrl" />
          <div class="upload-tip">
            <el-tag type="info">
              通用文件
            </el-tag>
            <el-tag type="info">
              进度显示
            </el-tag>
            <el-tag type="info">
              文件类型
            </el-tag>
          </div>
          <div
            v-if="fileUrl"
            class="upload-result"
          >
            <span>返回值：</span>
            <el-input
              v-model="fileUrl"
              readonly
              size="small"
            />
          </div>
        </el-card>
      </el-col>

      <!-- Excel上传 -->
      <el-col :span="12">
        <el-card class="demo-card">
          <template #header>
            <span>Excel上传 (ExcelUpload)</span>
          </template>
          <ExcelUpload
            v-model="excelData"
            @change="handleExcelChange"
          />
          <div class="upload-tip">
            <el-tag type="info">
              Excel解析
            </el-tag>
            <el-tag type="info">
              数据预览
            </el-tag>
            <el-tag type="info">
              自动计算
            </el-tag>
          </div>
          <div
            v-if="excelData && excelData.length > 0"
            class="excel-preview"
          >
            <p>
              <el-tag type="success">
                已解析 {{ excelData.length }} 条数据
              </el-tag>
            </p>
            <el-table
              :data="excelData.slice(0, 3)"
              size="small"
              max-height="150"
            >
              <el-table-column
                v-for="(value, key) in excelData[0]"
                :key="key"
                :prop="String(key)"
                :label="String(key)"
              />
            </el-table>
            <p
              v-if="excelData.length > 3"
              class="more-hint"
            >
              仅显示前3条，更多数据请查看控制台
            </p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 功能特性 -->
    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>功能特性</span>
      </template>
      <el-row :gutter="16">
        <el-col :span="8">
          <h4>ImageUpload</h4>
          <div class="feature-list">
            <el-tag type="success">
              图片预览
            </el-tag>
            <el-tag type="success">
              大图弹窗
            </el-tag>
            <el-tag type="info">
              多图上传
            </el-tag>
            <el-tag type="info">
              大小限制
            </el-tag>
          </div>
        </el-col>
        <el-col :span="8">
          <h4>FileUpload</h4>
          <div class="feature-list">
            <el-tag type="success">
              通用文件
            </el-tag>
            <el-tag type="success">
              上传进度
            </el-tag>
            <el-tag type="info">
              文件类型
            </el-tag>
            <el-tag type="info">
              自动上传
            </el-tag>
          </div>
        </el-col>
        <el-col :span="8">
          <h4>ExcelUpload</h4>
          <div class="feature-list">
            <el-tag type="success">
              Excel解析
            </el-tag>
            <el-tag type="success">
              数据预览
            </el-tag>
            <el-tag type="info">
              自动计算
            </el-tag>
            <el-tag type="info">
              数据导出
            </el-tag>
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
        <el-tab-pane label="ImageUpload">
          <div class="code-block">
            <pre><code>// 基础用法
&lt;template&gt;
  &lt;ImageUpload v-model="imageUrl" /&gt;
&lt;/template&gt;

&lt;script setup&gt;
import { ref } from 'vue';
import { ImageUpload } from '@mango/common';

const imageUrl = ref('');
&lt;/script&gt;</code></pre>
          </div>
          <div
            class="code-block"
            style="margin-top: 12px"
          >
            <pre><code>// 多图上传
&lt;template&gt;
  &lt;ImageUpload v-model="imageList" :multiple="true" /&gt;
&lt;/template&gt;

&lt;script setup&gt;
import { ref } from 'vue';
import { ImageUpload } from '@mango/common';

// multiple=true 时，modelValue 为 string[] 类型
const imageList = ref([]);
&lt;/script&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="FileUpload">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;FileUpload v-model="fileUrl" /&gt;
&lt;/template&gt;

&lt;script setup&gt;
import { ref } from 'vue';
import { FileUpload } from '@mango/common';

const fileUrl = ref('');
&lt;/script&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="ExcelUpload">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;ExcelUpload
    v-model="excelData"
    @change="handleExcelChange"
  /&gt;
&lt;/template&gt;

&lt;script setup&gt;
import { ref } from 'vue';
import { ExcelUpload } from '@mango/common';

const excelData = ref([]);

function handleExcelChange(data) {
  console.log('解析出的数据:', data);
  // data 是解析后的数组，每项是一个对象
}
&lt;/script&gt;</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="自定义配置">
          <div class="code-block">
            <pre><code>// 自定义上传地址和请求头
&lt;template&gt;
  &lt;ImageUpload
    v-model="imageUrl"
    action="/custom/upload/path"
    :headers="{ Authorization: 'Bearer token' }"
    :max-size="10"
  /&gt;
&lt;/template&gt;

// props 说明:
// action: 上传地址，默认 /api/admin/upload/image
// headers: 请求头对象
// limit: 最大上传数量，默认 5
// multiple: 是否多选，默认 true
// disabled: 是否禁用，默认 false
// auto-upload: 是否自动上传，默认 true
// max-size: 文件大小限制(MB)，默认 5</code></pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ImageUpload, FileUpload, ExcelUpload } from '@mango/common';

const imageUrl = ref('');
const multiImage = ref(true);
const fileUrl = ref('');
const excelData = ref<any[]>([]);

function handleExcelChange(data: any[]) {
  excelData.value = data;
  console.log('Excel数据:', data);
}
</script>

<style scoped lang="scss">
.upload-view-container {
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

    .upload-tip {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-top: 12px;
    }

    .upload-result {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 12px;
      padding: 8px;
      background: #f5f7fa;
      border-radius: 4px;

      span {
        color: #606266;
        font-size: 14px;
        white-space: nowrap;
      }

      .el-input {
        flex: 1;
      }
    }

    .excel-preview {
      margin-top: 12px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 4px;

      .more-hint {
        margin-top: 8px;
        font-size: 12px;
        color: #909399;
      }
    }

    h4 {
      margin: 0 0 8px 0;
      font-size: 14px;
      font-weight: 600;
    }

    .feature-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
  }

  .usage-card {
    :deep(.el-tabs__content) {
      max-height: 500px;
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
