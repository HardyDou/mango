<template>
  <div class="public-path-container">
    <!-- Search Bar -->
    <el-card class="search-card">
      <el-form inline>
        <el-form-item label="路径">
          <el-input v-model="searchForm.path" placeholder="请输入路径" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.pathType" placeholder="请选择类型" clearable style="width: 150px">
            <el-option
              v-for="item in PATH_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">新增</el-button>
      </div>

      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="path" label="路径" min-width="200">
          <template #default="{ row }">
            <code>{{ row.path }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="pathType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.pathType)">
              {{ row.pathTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" />
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="handleDialogClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="路径" prop="path">
          <el-input v-model="formData.path" placeholder="如: /public/** 或 /api/test" />
          <div class="form-tip">支持通配符: /** 匹配子路径, /* 匹配单层</div>
        </el-form-item>
        <el-form-item label="类型" prop="pathType">
          <el-select v-model="formData.pathType" placeholder="请选择类型" style="width: 100%">
            <el-option
              v-for="item in PATH_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            >
              <span>{{ item.label }}</span>
              <span class="option-desc">{{ item.description }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="formData.priority" :min="0" :max="999" />
          <div class="form-tip">数字越大，匹配时优先级越高</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="formData.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import {
  getPublicPathList,
  addPublicPath,
  updatePublicPath,
  deletePublicPath,
  PATH_TYPE_OPTIONS,
  type PublicPath,
} from '../../api/publicPath';

const searchForm = reactive({
  path: '',
  pathType: undefined as number | undefined,
  status: undefined as number | undefined,
});

const tableData = ref<PublicPath[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增');
const formRef = ref<FormInstance>();
const isEdit = ref(false);

const formData = reactive<Partial<PublicPath>>({
  path: '',
  pathType: 1,
  description: '',
  priority: 0,
  status: 1,
});

const formRules: FormRules = {
  path: [
    { required: true, message: '请输入路径', trigger: 'blur' },
    { pattern: /^\//, message: '路径必须以 / 开头', trigger: 'blur' },
  ],
  pathType: [{ required: true, message: '请选择类型', trigger: 'change' }],
};

function getTypeTagType(type: number) {
  switch (type) {
    case 1:
      return 'success';
    case 2:
      return 'warning';
    case 3:
      return 'info';
    default:
      return '';
  }
}

async function fetchData() {
  try {
    const res = await getPublicPathList();
    tableData.value = res.data || [];
  } catch (error) {
    console.error('Failed to fetch public paths:', error);
  }
}

function handleSearch() {
  // For now, just filter locally - can be changed to server-side filtering
  fetchData();
}

function handleReset() {
  searchForm.path = '';
  searchForm.pathType = undefined;
  searchForm.status = undefined;
  fetchData();
}

function handleAdd() {
  dialogTitle.value = '新增';
  isEdit.value = false;
  Object.assign(formData, {
    path: '',
    pathType: 1,
    description: '',
    priority: 0,
    status: 1,
  });
  dialogVisible.value = true;
}

function handleEdit(row: PublicPath) {
  dialogTitle.value = '编辑';
  isEdit.value = true;
  Object.assign(formData, {
    id: row.id,
    path: row.path,
    pathType: row.pathType,
    description: row.description,
    priority: row.priority,
    status: row.status,
  });
  dialogVisible.value = true;
}

async function handleStatusChange(row: PublicPath) {
  try {
    await updatePublicPath(row.id!, { status: row.status });
    ElMessage.success('状态更新成功');
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1; // Revert
    ElMessage.error('状态更新失败');
  }
}

async function handleDelete(row: PublicPath) {
  try {
    await ElMessageBox.confirm('确定要删除该配置吗？', '提示', {
      type: 'warning',
    });
    await deletePublicPath(row.id!);
    ElMessage.success('删除成功');
    fetchData();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
  }
}

async function handleSubmit() {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();

    if (isEdit.value) {
      await updatePublicPath(formData.id!, formData);
      ElMessage.success('更新成功');
    } else {
      await addPublicPath(formData);
      ElMessage.success('添加成功');
    }

    dialogVisible.value = false;
    fetchData();
  } catch (error) {
    console.error('Submit failed:', error);
  }
}

function handleDialogClose() {
  formRef.value?.resetFields();
}

// Initialize
fetchData();
</script>

<style scoped lang="scss">
.public-path-container {
  padding: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.table-toolbar {
  margin-bottom: 16px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.option-desc {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}
</style>
