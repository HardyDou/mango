<template>
  <div class="directive-view-container">
    <h1>功能指令</h1>
    <p class="subtitle">
      Vue 自定义权限指令，用于根据用户权限动态控制元素显示
    </p>

    <!-- 权限模拟器 -->
    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>权限模拟器</span>
          <el-button
            type="primary"
            size="small"
            @click="resetPermissions"
          >
            重置权限
          </el-button>
        </div>
      </template>
      <div class="permission-simulator">
        <p class="simulator-tip">
          勾选以下权限，查看页面中对应按钮的显示/隐藏变化：
        </p>
        <el-checkbox-group v-model="currentPermissions">
          <el-checkbox label="admin:user:add">
            admin:user:add
          </el-checkbox>
          <el-checkbox label="admin:user:edit">
            admin:user:edit
          </el-checkbox>
          <el-checkbox label="admin:user:delete">
            admin:user:delete
          </el-checkbox>
          <el-checkbox label="admin:user:query">
            admin:user:query
          </el-checkbox>
          <el-checkbox label="admin:user:export">
            admin:user:export
          </el-checkbox>
        </el-checkbox-group>
      </div>
    </el-card>

    <!-- 指令演示 -->
    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>指令演示</span>
      </template>

      <el-divider content-position="left">
        v-auth 单权限判断
      </el-divider>
      <div class="auth-demo">
        <!-- 使用 hasPermission 模拟 v-auth 指令行为 -->
        <el-button
          v-if="hasPermission('admin:user:add')"
          type="primary"
        >
          <el-icon><Plus /></el-icon>
          新增用户 (需要 admin:user:add)
        </el-button>
        <el-button
          v-if="hasPermission('admin:user:edit')"
          type="success"
        >
          <el-icon><Edit /></el-icon>
          编辑用户 (需要 admin:user:edit)
        </el-button>
        <el-button
          v-if="hasPermission('admin:user:delete')"
          type="danger"
        >
          <el-icon><Delete /></el-icon>
          删除用户 (需要 admin:user:delete)
        </el-button>
        <el-button
          v-if="!hasPermission('fake:permission')"
          type="info"
        >
          <el-icon><Warning /></el-icon>
          fake:permission (无此权限，不显示)
        </el-button>
      </div>

      <el-divider content-position="left">
        v-auths 多权限 OR 判断
      </el-divider>
      <div class="auth-demo">
        <el-button
          v-if="hasAnyPermission(['admin:user:add', 'admin:user:edit'])"
          type="primary"
        >
          <el-icon><Document /></el-icon>
          新增/编辑 (需要 admin:user:add 或 admin:user:edit)
        </el-button>
        <el-button
          v-if="hasAnyPermission(['admin:user:delete', 'admin:user:export'])"
          type="warning"
        >
          <el-icon><Download /></el-icon>
          删除/导出 (需要 admin:user:delete 或 admin:user:export)
        </el-button>
      </div>

      <el-divider content-position="left">
        v-auth-all 多权限 AND 判断
      </el-divider>
      <div class="auth-demo">
        <el-button
          v-if="hasAllPermissions(['admin:user:add', 'admin:user:edit', 'admin:user:delete'])"
          type="danger"
        >
          <el-icon><Setting /></el-icon>
          超级管理 (需要同时拥有 add + edit + delete)
        </el-button>
        <el-button
          v-if="hasAllPermissions(['admin:user:query', 'admin:user:export'])"
          type="success"
        >
          <el-icon><DataLine /></el-icon>
          查询导出 (需要同时拥有 query + export)
        </el-button>
      </div>
    </el-card>

    <!-- 功能特性 -->
    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>功能特性</span>
      </template>
      <div class="feature-list">
        <el-tag type="success">
          v-auth
        </el-tag>
        <el-tag>单权限判断</el-tag>
        <el-tag type="success">
          v-auths
        </el-tag>
        <el-tag>多权限 OR</el-tag>
        <el-tag type="success">
          v-auth-all
        </el-tag>
        <el-tag>多权限 AND</el-tag>
      </div>
      <el-divider />
      <div class="feature-desc">
        <el-alert
          type="info"
          :closable="false"
        >
          <template #title>
            <b>指令说明：</b>
            <ul>
              <li><code>v-auth="'权限码'"</code> - 判断是否拥有指定权限</li>
              <li><code>v-auths="['权限1', '权限2']"</code> - 判断是否拥有任一权限（OR）</li>
              <li><code>v-auth-all="['权限1', '权限2']"</code> - 判断是否同时拥有所有权限（AND）</li>
            </ul>
          </template>
        </el-alert>
      </div>
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
        <el-tab-pane label="v-auth 单权限">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;el-button v-auth="'admin:user:add'" type="primary"&gt;
    新增
  &lt;/el-button&gt;
&lt;/template&gt;

// 权限码格式: {model}:{module}:{action}
// 例如: admin:user:add 表示 admin 模块 user 资源 add 操作</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="v-auths 多权限 OR">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;el-button v-auths="['admin:user:add', 'admin:user:edit']" type="primary"&gt;
    新增/编辑
  &lt;/el-button&gt;
&lt;/template&gt;

// 只要拥有列表中的任一权限，按钮就会显示</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="v-auth-all 多权限 AND">
          <div class="code-block">
            <pre><code>&lt;template&gt;
  &lt;el-button
    v-auth-all="['admin:user:add', 'admin:user:edit', 'admin:user:delete']"
    type="danger"
  &gt;
    超级管理
  &lt;/el-button&gt;
&lt;/template&gt;

// 必须同时拥有列表中的所有权限，按钮才会显示</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="全局注册">
          <div class="code-block">
            <pre><code>// 指令在 main.ts 中全局注册
// 使用时直接在模板中添加指令即可

// src/components/auth/ 目录下包含：
// - auth.vue        (v-auth)
// - auths.vue       (v-auths)
// - authAll.vue     (v-auth-all)</code></pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="权限数据来源">
          <div class="code-block">
            <pre><code>// 权限数据通常来自用户登录后的路由或菜单信息
// 可以在 stores/user.ts 或 stores/auth.ts 中获取

// 示例：从 userStore 获取权限列表
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const permissions = userStore.permissions; // ['admin:user:add', ...]

// 指令内部会调用权限判断函数进行匹配</code></pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 当前权限状态 -->
    <el-card
      class="demo-card"
      style="margin-top: 20px"
    >
      <template #header>
        <span>当前权限状态</span>
      </template>
      <div class="permission-status">
        <el-tag
          v-for="perm in currentPermissions"
          :key="perm"
          type="success"
          style="margin: 4px"
        >
          {{ perm }}
        </el-tag>
        <el-tag
          v-if="currentPermissions.length === 0"
          type="info"
        >
          无任何权限
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Plus,
  Edit,
  Delete,
  Warning,
  Document,
  Download,
  Setting,
  DataLine,
} from '@element-plus/icons-vue';

// 模拟权限列表 - 初始有一些权限
const currentPermissions = ref<string[]>(['admin:user:add', 'admin:user:edit', 'admin:user:query']);

// 模拟权限检查函数
function hasPermission(permission: string): boolean {
  return currentPermissions.value.includes(permission);
}

function hasAnyPermission(permissions: string[]): boolean {
  return permissions.some((p) => currentPermissions.value.includes(p));
}

function hasAllPermissions(permissions: string[]): boolean {
  return permissions.every((p) => currentPermissions.value.includes(p));
}

function resetPermissions() {
  currentPermissions.value = ['admin:user:add', 'admin:user:edit', 'admin:user:query'];
  ElMessage({
    message: '权限已重置',
    type: 'success',
  });
}
</script>

<style scoped lang="scss">
.directive-view-container {
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

    .permission-simulator {
      .simulator-tip {
        margin-bottom: 12px;
        color: #606266;
      }

      .el-checkbox-group {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
    }

    .auth-demo {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 16px 0;
    }

    .feature-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .feature-desc {
      ul {
        margin: 8px 0;
        padding-left: 20px;

        li {
          margin: 4px 0;
          line-height: 1.6;
        }

        code {
          background: #f5f7fa;
          padding: 2px 6px;
          border-radius: 3px;
          font-size: 13px;
        }
      }
    }
  }

  .permission-status {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .usage-card {
    :deep(.el-tabs__content) {
      max-height: 400px;
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
