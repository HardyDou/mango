<template>
  <div class="dict-container">
    <el-row :gutter="20">
      <!-- 左侧：字典类型列表 -->
      <el-col :span="8">
        <el-card class="type-card">
          <template #header>
            <div class="card-header">
              <span>字典类型</span>
              <el-button
                type="primary"
                size="small"
                @click="handleAddType"
              >
                新增类型
              </el-button>
            </div>
          </template>

          <!-- 搜索框 -->
          <el-input
            v-model="typeKeyword"
            placeholder="搜索类型名称/编码"
            clearable
            @input="handleTypeSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <!-- 类型列表 -->
          <div class="type-list">
            <div
              v-for="item in typeList"
              :key="item.id"
              class="type-item"
              :class="{ active: currentType?.id === item.id }"
              @click="handleSelectType(item)"
            >
              <div class="type-info">
                <span class="type-name">{{ item.name }}</span>
                <span class="type-code">{{ item.code }}</span>
              </div>
              <div class="type-actions">
                <el-tag
                  :type="item.status === 1 ? 'success' : 'danger'"
                  size="small"
                >
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </div>
            <el-empty
              v-if="typeList.length === 0"
              description="暂无数据"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：字典数据列表 -->
      <el-col :span="16">
        <el-card class="data-card">
          <template #header>
            <div class="card-header">
              <span>字典数据 {{ currentType ? `- ${currentType.name}` : '' }}</span>
              <el-button
                type="primary"
                :disabled="!currentType"
                @click="handleAddData"
              >
                新增数据
              </el-button>
            </div>
          </template>

          <!-- 搜索框 -->
          <el-form
            :inline="true"
            class="search-form"
          >
            <el-form-item label="关键词">
              <el-input
                v-model="dataKeyword"
                placeholder="搜索标签/值"
                clearable
                @keyup.enter="handleDataSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="handleDataSearch"
              >
                查询
              </el-button>
              <el-button @click="handleDataReset">
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 数据表格 -->
          <el-table
            v-loading="dataLoading"
            :data="dataList"
            stripe
          >
            <el-table-column
              prop="label"
              label="标签"
            />
            <el-table-column
              prop="value"
              label="值"
            />
            <el-table-column
              prop="sort"
              label="排序"
              width="80"
            />
            <el-table-column
              prop="status"
              label="状态"
              width="80"
            >
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 1 ? 'success' : 'danger'"
                  size="small"
                >
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="createTime"
              label="创建时间"
              width="180"
            />
            <el-table-column
              label="操作"
              width="150"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="handleEditData(row)"
                >
                  编辑
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="handleDeleteData(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <Pagination
            v-model:page="dataQuery.pageNum"
            v-model:limit="dataQuery.pageSize"
            :total="dataTotal"
            @pagination="loadDataList"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 字典类型编辑弹窗 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeForm.id ? '编辑类型' : '新增类型'"
      width="500px"
    >
      <el-form
        ref="typeFormRef"
        :model="typeForm"
        :rules="typeRules"
        label-width="100px"
      >
        <el-form-item
          label="类型名称"
          prop="name"
        >
          <el-input
            v-model="typeForm.name"
            placeholder="请输入类型名称"
          />
        </el-form-item>
        <el-form-item
          label="类型编码"
          prop="code"
        >
          <el-input
            v-model="typeForm.code"
            placeholder="请输入类型编码"
            :disabled="!!typeForm.id"
          />
        </el-form-item>
        <el-form-item
          label="描述"
          prop="description"
        >
          <el-input
            v-model="typeForm.description"
            type="textarea"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item
          label="排序"
          prop="sort"
        >
          <el-input-number
            v-model="typeForm.sort"
            :min="0"
            :max="9999"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="typeForm.status">
            <el-radio :label="1">
              启用
            </el-radio>
            <el-radio :label="0">
              禁用
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleTypeSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 字典数据编辑弹窗 -->
    <el-dialog
      v-model="dataDialogVisible"
      :title="dataForm.id ? '编辑数据' : '新增数据'"
      width="500px"
    >
      <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRules"
        label-width="100px"
      >
        <el-form-item label="所属类型">
          <el-input
            :value="currentType?.name"
            disabled
          />
        </el-form-item>
        <el-form-item
          label="标签"
          prop="label"
        >
          <el-input
            v-model="dataForm.label"
            placeholder="请输入显示标签"
          />
        </el-form-item>
        <el-form-item
          label="值"
          prop="value"
        >
          <el-input
            v-model="dataForm.value"
            placeholder="请输入值"
          />
        </el-form-item>
        <el-form-item
          label="排序"
          prop="sort"
        >
          <el-input-number
            v-model="dataForm.sort"
            :min="0"
            :max="9999"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="dataForm.status">
            <el-radio :label="1">
              启用
            </el-radio>
            <el-radio :label="0">
              禁用
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="扩展数据"
          prop="extra"
        >
          <el-input
            v-model="dataForm.extra"
            type="textarea"
            placeholder="请输入扩展数据(JSON)"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleDataSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemDict">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import Pagination from '@/components/Pagination/index.vue';
import { dictTypeApi, dictDataApi, type DictType, type DictData } from '@/api/admin/dict';

// ==================== 类型列表 ====================
const typeKeyword = ref('');
const typeList = ref<DictType[]>([]);
const currentType = ref<DictType | null>(null);
const typeDialogVisible = ref(false);
const typeFormRef = ref<FormInstance>();
const typeForm = reactive<DictType>({
  id: undefined,
  name: '',
  code: '',
  description: '',
  sort: 0,
  status: 1,
});

const typeRules: FormRules = {
  name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
};

/**
 * 加载字典类型列表
 */
async function loadTypeList() {
  try {
    const data = await dictTypeApi.list({ keyword: typeKeyword.value });
    typeList.value = data.list;
  } catch (error) {
    console.error('加载字典类型失败:', error);
  }
}

/**
 * 搜索类型
 */
function handleTypeSearch() {
  loadTypeList();
}

/**
 * 选择类型
 */
function handleSelectType(item: DictType) {
  currentType.value = item;
  dataQuery.typeId = item.id;
  loadDataList();
}

/**
 * 新增类型
 */
function handleAddType() {
  typeForm.id = undefined;
  typeForm.name = '';
  typeForm.code = '';
  typeForm.description = '';
  typeForm.sort = 0;
  typeForm.status = 1;
  typeDialogVisible.value = true;
}

/**
 * 提交类型表单
 */
async function handleTypeSubmit() {
  if (!typeFormRef.value) return;

  try {
    await typeFormRef.value.validate();
    if (typeForm.id) {
      await dictTypeApi.update(typeForm);
      ElMessage.success('修改成功');
    } else {
      await dictTypeApi.create(typeForm);
      ElMessage.success('新增成功');
    }
    typeDialogVisible.value = false;
    loadTypeList();
  } catch (error) {
    console.error('提交失败:', error);
  }
}

// ==================== 字典数据列表 ====================
const dataKeyword = ref('');
const dataLoading = ref(false);
const dataList = ref<DictData[]>([]);
const dataTotal = ref(0);
const dataQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  typeId: undefined as number | undefined,
});
const dataDialogVisible = ref(false);
const dataFormRef = ref<FormInstance>();
const dataForm = reactive<DictData>({
  id: undefined,
  typeId: 0,
  label: '',
  value: '',
  sort: 0,
  status: 1,
  extra: '',
});

const dataRules: FormRules = {
  label: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  value: [{ required: true, message: '请输入值', trigger: 'blur' }],
};

/**
 * 加载字典数据列表
 */
async function loadDataList() {
  if (!currentType.value) return;

  dataLoading.value = true;
  try {
    const data = await dictDataApi.list({
      ...dataQuery,
      typeId: currentType.value.id,
    });
    dataList.value = data.list;
    dataTotal.value = data.total;
  } catch (error) {
    console.error('加载字典数据失败:', error);
  } finally {
    dataLoading.value = false;
  }
}

/**
 * 搜索数据
 */
function handleDataSearch() {
  dataQuery.keyword = dataKeyword.value;
  dataQuery.pageNum = 1;
  loadDataList();
}

/**
 * 重置数据搜索
 */
function handleDataReset() {
  dataKeyword.value = '';
  dataQuery.keyword = '';
  dataQuery.pageNum = 1;
  loadDataList();
}

/**
 * 新增数据
 */
function handleAddData() {
  if (!currentType.value) return;
  dataForm.id = undefined;
  dataForm.typeId = currentType.value.id!;
  dataForm.label = '';
  dataForm.value = '';
  dataForm.sort = 0;
  dataForm.status = 1;
  dataForm.extra = '';
  dataDialogVisible.value = true;
}

/**
 * 编辑数据
 */
function handleEditData(row: DictData) {
  dataForm.id = row.id;
  dataForm.typeId = row.typeId;
  dataForm.label = row.label;
  dataForm.value = row.value;
  dataForm.sort = row.sort || 0;
  dataForm.status = row.status || 1;
  dataForm.extra = row.extra || '';
  dataDialogVisible.value = true;
}

/**
 * 提交数据表单
 */
async function handleDataSubmit() {
  if (!dataFormRef.value) return;

  try {
    await dataFormRef.value.validate();
    if (dataForm.id) {
      await dictDataApi.update(dataForm);
      ElMessage.success('修改成功');
    } else {
      await dictDataApi.create(dataForm);
      ElMessage.success('新增成功');
    }
    dataDialogVisible.value = false;
    loadDataList();
  } catch (error) {
    console.error('提交失败:', error);
  }
}

/**
 * 删除数据
 */
function handleDeleteData(row: DictData) {
  ElMessageBox.confirm('确认删除该字典数据?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await dictDataApi.delete(row.id!);
      ElMessage.success('删除成功');
      loadDataList();
    } catch (error) {
      console.error('删除失败:', error);
    }
  }).catch(() => {});
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadTypeList();
});
</script>

<style scoped lang="scss">
.dict-container {
  padding: 20px;
}

.type-card,
.data-card {
  height: calc(100vh - 140px);
  overflow: auto;

  :deep(.el-card__header) {
    padding: 12px 20px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.type-list {
  margin-top: 16px;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
}

.type-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: #409eff;
    background-color: #f5f7fa;
  }

  &.active {
    border-color: #409eff;
    background-color: #ecf5ff;
  }
}

.type-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.type-name {
  font-weight: 500;
  color: #303133;
}

.type-code {
  font-size: 12px;
  color: #909399;
}

.type-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-form {
  margin-bottom: 16px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
</style>
