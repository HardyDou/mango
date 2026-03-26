# UI/UX 规范 (ui-rules)

## 1. 设计系统

### 1.1 基础变量

```css
:root {
  /* 主色调 */
  --color-primary: #409EFF;
  --color-success: #67C23A;
  --color-warning: #E6A23C;
  --color-danger: #F56C6C;
  --color-info: #909399;

  /* 边框圆角 */
  --border-radius-base: 4px;
  --border-radius-small: 2px;
  --border-radius-large: 8px;

  /* 间距基准 */
  --spacing-base: 16px;
  --spacing-small: 8px;
  --spacing-large: 24px;

  /* 字体大小 */
  --font-size-base: 14px;
  --font-size-small: 12px;
  --font-size-large: 16px;
}
```

---

## 2. M* 组件库

### 2.1 组件列表

| 组件 | 底层 | 说明 |
|------|------|------|
| MButton | el-button | Props 白名单化 |
| MInput | el-input | 预配置校验 |
| MSelect | el-select | 支持远程搜索 |
| MTable | el-table | 预配置分页、排序 |
| MForm | el-form | 预配置布局规则 |
| MDialog | el-dialog | 预配置 footer |
| MDrawer | el-drawer | 预配置 footer |
| MTag | el-tag | Props 白名单化 |
| MCard | el-card | 标准化卡片 |

### 2.2 MButton 使用

```vue
<template>
  <!-- ✅ 正确：使用 MButton -->
  <MButton type="primary" @click="handleSubmit">
    提交
  </MButton>

  <!-- ❌ 错误：直接使用 el-button -->
  <el-button type="primary" @click="handleSubmit">
    提交
  </el-button>
</template>
```

### 2.3 MForm 使用

```vue
<template>
  <MForm :model="form" :rules="rules" ref="formRef">
    <MFormItem prop="username">
      <MInput v-model="form.username" placeholder="请输入用户名" />
    </MFormItem>
    <MFormItem prop="password">
      <MInput v-model="form.password" type="password" placeholder="请输入密码" />
    </MFormItem>
    <MFormItem>
      <MButton type="primary" @click="handleSubmit">提交</MButton>
    </MFormItem>
  </MForm>
</template>
```

---

## 3. ESLint 规则

### 3.1 组件使用检查

```json
{
  "rules": {
    "vue/no-div-as-component": "warn",
    "vue/require-use-component-styles": "error",
    "no-restricted-imports": ["error", {
      "patterns": [
        {
          "group": ["el-button", "el-input", "el-select", "el-table", "el-form"],
          "message": "请使用 M* 组件替代 el-* 组件"
        }
      ]
    }]
  }
}
```

### 3.2 检查命令

```bash
# 运行 ESLint
npm run lint

# 修复
npm run lint:fix
```

---

## 4. 样式规范

### 4.1 CSS 变量使用

```vue
<template>
  <div class="page-container">
    <el-button type="primary">按钮</el-button>
  </div>
</template>

<style scoped>
.page-container {
  /* ✅ 使用 CSS 变量 */
  padding: var(--spacing-base);
  border-radius: var(--border-radius-base);

  /* ❌ 不要硬编码 */
  /* padding: 16px; */
  /* border-radius: 4px; */
}
</style>
```

### 4.2 禁止 style 块

```vue
<template>
  <div class="page-container">
    <!-- ✅ 正确：使用 MButton -->
    <MButton type="primary">提交</MButton>
  </div>
</template>

<!-- ❌ 禁止：自定义样式 -->
<style>
.el-button {
  background-color: #409EFF !important;
}
</style>
```

---

## 5. 状态设计

### 5.1 加载状态

```vue
<template>
  <MButton type="primary" :loading="submitting" @click="handleSubmit">
    提交
  </MButton>
</template>
```

### 5.2 空状态

```vue
<template>
  <div v-if="list.length === 0" class="empty-state">
    <img src="/images/empty.png" alt="暂无数据" />
    <p>暂无数据</p>
  </div>
</template>
```

### 5.3 错误状态

```vue
<template>
  <MDialog v-model="visible" title="错误">
    <MTag type="danger">操作失败：{{ errorMessage }}</MTag>
    <template #footer>
      <MButton @click="visible = false">关闭</MButton>
    </template>
  </MDialog>
</template>
```

### 5.4 成功状态

```vue
<script setup>
import { ElMessage } from 'element-plus';

const handleSuccess = () => {
  ElMessage.success('操作成功');
};
</script>
```

---

## 6. 响应式规范

### 6.1 断点

| 断点 | 尺寸 |
|------|------|
| xs | < 768px |
| sm | 768px - 992px |
| md | 992px - 1200px |
| lg | 1200px - 1920px |
| xl | > 1920px |

### 6.2 布局

```vue
<template>
  <div class="page-container">
    <!-- 移动端隐藏 -->
    <div class="desktop-only">桌面端内容</div>
  </div>
</template>

<style scoped>
@media (max-width: 768px) {
  .desktop-only {
    display: none;
  }
}
</style>
```

---

## 7. 验收标准

### 7.1 截图验收

| 验收点 | 说明 |
|--------|------|
| 默认状态 | /golden/页面名-default.png |
| 加载状态 | /golden/页面名-loading.png |
| 空状态 | /golden/页面名-empty.png |
| 错误状态 | /golden/页面名-error.png |

### 7.2 人类验收清单

```markdown
## [页面名] 验收标准

### 视觉检查点
- [ ] 截图: `/golden/页面名-默认状态.png`
- [ ] 主色调: #409EFF
- [ ] 圆角: 4px

### 交互测试
- [ ] 点击主按钮 → 显示 loading
- [ ] 提交空表单 → 显示错误提示
- [ ] 操作成功 → Toast 轻提示

### 组件使用合规
- [ ] 使用 MButton（不是 el-button）
- [ ] 使用 MInput（不是 el-input）
- [ ] 无 <style> 块（除 M* 组件）
```
