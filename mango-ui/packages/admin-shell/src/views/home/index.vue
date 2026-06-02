<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col
        v-for="item in platformCards"
        :key="item.title"
        :span="8"
      >
        <el-card class="stat-card">
          <div :class="['stat-icon', item.iconClass]">
            <el-icon :size="32">
              <component :is="item.icon" />
            </el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ item.title }}</div>
            <div class="stat-label">{{ item.description }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row
      :gutter="20"
      class="home-row"
    >
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>平台状态</span>
          </template>
          <div class="chart-container">
            <el-empty description="请选择左侧菜单进入具体能力" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>常用能力</span>
          </template>
          <div class="quick-entry">
            <div
              v-for="item in quickEntries"
              :key="item.title"
              class="quick-item"
            >
              <el-icon :size="24">
                <component :is="item.icon" />
              </el-icon>
              <span>{{ item.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="MangoShellHome">
import { Calendar, Files, Menu, Operation, Setting, User } from '@element-plus/icons-vue';

const platformCards = [
  {
    title: '权限与组织',
    description: '用户、角色、菜单、租户基础能力',
    icon: User,
    iconClass: 'stat-icon-user',
  },
  {
    title: '流程与协同',
    description: '审批、任务、通知等流程能力',
    icon: Operation,
    iconClass: 'stat-icon-document',
  },
  {
    title: '平台基础能力',
    description: '文件、模板、编号、日历等通用能力',
    icon: Calendar,
    iconClass: 'stat-icon-chart',
  },
];

const quickEntries = [
  { title: '系统设置', icon: Setting },
  { title: '菜单管理', icon: Menu },
  { title: '文件中心', icon: Files },
  { title: '工作日历', icon: Calendar },
];
</script>

<style scoped lang="scss">
.home-container {
  padding: 0;
}

.home-row {
  margin-top: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;

  .stat-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 60px;
    height: 60px;
    border-radius: 8px;
    color: #fff;
    margin-right: 20px;
  }

  .stat-icon-user {
    background: #409eff;
  }

  .stat-icon-document {
    background: #67c23a;
  }

  .stat-icon-chart {
    background: #e6a23c;
  }

  .stat-info {
    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: var(--mango-text-color);
    }

    .stat-label {
      margin-top: 4px;
      font-size: 14px;
      color: var(--mango-text-color-regular);
    }
  }
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-entry {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;

  .quick-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px;
    border: 1px solid var(--mango-border-color);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: var(--mango-color-primary);
      background: var(--mango-color-menu-hover);
    }

    span {
      margin-top: 8px;
      font-size: 14px;
      color: var(--mango-text-color);
    }
  }
}
</style>
