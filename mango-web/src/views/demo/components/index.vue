<template>
  <div class="component-demo-container">
    <h1>前端组件库</h1>
    <p class="subtitle">
      已实现组件的演示和测试页面
    </p>

    <el-row :gutter="20">
      <!-- 富文本编辑器 -->
      <el-col :span="12">
        <el-card class="demo-card">
          <template #header>
            <span>富文本编辑器 (Editor)</span>
          </template>
          <div
            class="editor-demo"
            data-testid="editor-demo"
          >
            <Editor
              v-model="editorContent"
              height="300"
            />
          </div>
          <div class="demo-tip">
            <el-tag type="info">
              v-model 双向绑定
            </el-tag>
            <el-tag type="info">
              高度自适应
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 代码编辑器 -->
      <el-col :span="12">
        <el-card class="demo-card">
          <template #header>
            <span>代码编辑器 (CodeEditor)</span>
          </template>
          <div
            class="editor-demo"
            data-testid="code-editor-demo"
          >
            <CodeEditor
              v-model="codeContent"
              language="javascript"
              height="300px"
            />
          </div>
          <div class="demo-tip">
            <el-tag type="info">
              多语言支持
            </el-tag>
            <el-tag type="info">
              语法高亮
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row
      :gutter="20"
      style="margin-top: 20px"
    >
      <!-- 图片上传 -->
      <el-col :span="8">
        <el-card class="demo-card">
          <template #header>
            <span>图片上传 (ImageUpload)</span>
          </template>
          <ImageUpload v-model="imageUrl" />
          <div class="demo-tip">
            <el-tag type="info">
              图片预览
            </el-tag>
            <el-tag type="info">
              自动上传
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 文件上传 -->
      <el-col :span="8">
        <el-card class="demo-card">
          <template #header>
            <span>文件上传 (FileUpload)</span>
          </template>
          <FileUpload v-model="fileUrl" />
          <div class="demo-tip">
            <el-tag type="info">
              通用文件
            </el-tag>
            <el-tag type="info">
              进度显示
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- Excel上传 -->
      <el-col :span="8">
        <el-card class="demo-card">
          <template #header>
            <span>Excel上传 (ExcelUpload)</span>
          </template>
          <ExcelUpload
            v-model="excelData"
            @change="handleExcelChange"
          />
          <div
            v-if="excelData"
            class="excel-preview"
          >
            <p>已解析 {{ excelData.length }} 条数据</p>
          </div>
          <div class="demo-tip">
            <el-tag type="info">
              Excel解析
            </el-tag>
            <el-tag type="info">
              数据预览
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row
      :gutter="20"
      style="margin-top: 20px"
    >
      <!-- ECharts 图表 -->
      <el-col :span="12">
        <el-card class="demo-card">
          <template #header>
            <span>数据图表 (ECharts)</span>
          </template>
          <ECharts
            :options="chartOptions"
            height="300px"
          />
          <div class="demo-tip">
            <el-tag type="info">
              折线图
            </el-tag>
            <el-tag type="info">
              响应式
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 权限指令 -->
      <el-col :span="12">
        <el-card class="demo-card">
          <template #header>
            <span>权限指令 (v-auth)</span>
          </template>
          <div class="auth-demo">
            <el-button
              v-auth="'admin:user:add'"
              type="primary"
            >
              有 admin:user:add 权限可见
            </el-button>
            <el-button
              v-auth="'admin:user:delete'"
              type="danger"
            >
              有 admin:user:delete 权限可见
            </el-button>
            <el-button
              v-auth="'fake:permission'"
              type="warning"
            >
              这个不应该显示
            </el-button>
          </div>
          <div class="demo-tip">
            <el-tag type="info">
              v-auth 单权限
            </el-tag>
            <el-tag type="info">
              v-auths 多权限 OR
            </el-tag>
            <el-tag type="info">
              v-auth-all 多权限 AND
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="ComponentDemo">
import { ref } from 'vue';
import Editor from '@/components/Editor/index.vue';
import CodeEditor from '@/components/CodeEditor/index.vue';
import ImageUpload from '@/components/Upload/ImageUpload.vue';
import FileUpload from '@/components/Upload/FileUpload.vue';
import ExcelUpload from '@/components/Upload/ExcelUpload.vue';
import ECharts from '@/components/ECharts/index.vue';

const editorContent = ref('<p>这是一个<b>富文本编辑器</b>示例</p>');
const codeContent = ref(`function hello() {
  console.log('Hello, World!');
}`);
const imageUrl = ref('');
const fileUrl = ref('');
const excelData = ref<any[]>([]);

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
      smooth: true,
    },
  ],
});

function handleExcelChange(data: any[]) {
  excelData.value = data;
}
</script>

<style scoped lang="scss">
.component-demo-container {
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
    .editor-demo {
      margin-bottom: 10px;
    }

    .demo-tip {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .auth-demo {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .excel-preview {
      margin-top: 10px;
      padding: 10px;
      background: #f5f7fa;
      border-radius: 4px;
      font-size: 14px;
    }
  }
}
</style>
