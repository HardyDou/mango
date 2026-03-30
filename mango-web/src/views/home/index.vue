<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">1,234</div>
            <div class="stat-label">用户总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon :size="32"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">5,678</div>
            <div class="stat-label">订单总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon :size="32"><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">98.5%</div>
            <div class="stat-label">系统效率</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>数据概览</span>
          </template>
          <div class="chart-container">
            <el-empty description="图表区域" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>快捷入口</span>
          </template>
          <div class="quick-entry">
            <div class="quick-item">
              <el-icon :size="24"><Setting /></el-icon>
              <span>系统设置</span>
            </div>
            <div class="quick-item">
              <el-icon :size="24"><User /></el-icon>
              <span>用户管理</span>
            </div>
            <div class="quick-item">
              <el-icon :size="24"><Document /></el-icon>
              <span>订单管理</span>
            </div>
            <div class="quick-item">
              <el-icon :size="24"><DataLine /></el-icon>
              <span>数据分析</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="Home">
import { User, Document, TrendCharts, Setting, DataLine } from '@element-plus/icons-vue';
</script>

<style scoped lang="scss">
.home-container {
  padding: 20px;
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
