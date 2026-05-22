<template>
  <div class="calendar-admin">
    <el-card class="calendar-shell" shadow="never">
      <div class="calendar-layout">
        <aside class="calendar-side">
          <div class="section-head">
            <div>
              <h3>日历</h3>
              <span>当前租户工作日历</span>
            </div>
            <el-button v-auth="'calendar:admin:create'" type="primary" @click="openCalendarDialog()">新增</el-button>
          </div>

          <el-form :model="calendarQuery" class="side-filter" @submit.prevent>
            <el-input v-model="calendarQuery.keyword" clearable placeholder="编码/名称" @keyup.enter="loadCalendars" />
            <el-select v-model="calendarQuery.status" clearable placeholder="状态">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
            <el-button v-auth="'calendar:admin:list'" @click="loadCalendars">查询</el-button>
          </el-form>

          <div class="calendar-list-area">
            <el-table
              v-loading="calendarLoading"
              :data="calendarRows"
              height="100%"
              highlight-current-row
              class="compact-table"
              @current-change="selectCalendar"
            >
              <el-table-column label="日历" min-width="170">
                <template #default="{ row }">
                  <div class="name-cell">
                    <strong>{{ row.calendarName }}</strong>
                    <span>{{ row.calendarCode }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="72">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="104" fixed="right">
                <template #default="{ row }">
                  <div class="side-actions">
                    <el-tooltip content="编辑" placement="top">
                      <el-button
                        v-auth="'calendar:admin:edit'"
                        :icon="Edit"
                        link
                        type="primary"
                        size="small"
                        aria-label="编辑日历"
                        @click.stop="openCalendarDialog(row)"
                      />
                    </el-tooltip>
                    <el-tooltip :content="row.status === 1 ? '停用' : '启用'" placement="top">
                      <el-button
                        v-auth="'calendar:admin:status'"
                        :icon="row.status === 1 ? CircleClose : CircleCheck"
                        link
                        type="primary"
                        size="small"
                        :aria-label="row.status === 1 ? '停用日历' : '启用日历'"
                        @click.stop="toggleCalendarStatus(row)"
                      />
                    </el-tooltip>
                    <el-tooltip content="删除" placement="top">
                      <el-button
                        v-auth="'calendar:admin:delete'"
                        :icon="Delete"
                        link
                        type="danger"
                        size="small"
                        aria-label="删除日历"
                        @click.stop="deleteCalendar(row)"
                      />
                    </el-tooltip>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <Pagination
            v-model:current-page="calendarQuery.pageNum"
            v-model:page-size="calendarQuery.pageSize"
            :total="calendarTotal"
            class="side-pagination"
            layout="prev, pager, next"
            small
            @change="loadCalendars"
          />
        </aside>

        <main class="calendar-main">
          <div class="toolbar">
            <div>
              <h2>日历管理</h2>
              <p>{{ selectedCalendar ? `${selectedCalendar.calendarName} / ${selectedCalendar.calendarCode}` : '选择日历后维护年度' }}</p>
            </div>
            <div class="toolbar-actions">
              <el-button v-auth="'calendar:year:init'" :disabled="!selectedCalendar" @click="openYearDialog">初始化年度</el-button>
              <el-button v-auth="'calendar:day:batch'" :disabled="!selectedRows.length" @click="openBatchDialog">批量设置</el-button>
              <el-button
                v-auth="'calendar:calculate:query'"
                :icon="Tools"
                :disabled="!selectedCalendar"
                @click="toolDrawerVisible = true"
              >
                工具
              </el-button>
            </div>
          </div>

          <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="年度" name="years">
              <el-form :inline="true" :model="yearQuery" class="search-form">
                <el-form-item label="年度">
                  <el-date-picker
                    v-model="yearPickerValue"
                    type="year"
                    value-format="YYYY"
                    placeholder="选择年度"
                    style="width: 140px"
                  />
                </el-form-item>
                <el-form-item label="启用状态">
                  <el-select v-model="yearQuery.enabled" clearable placeholder="全部" style="width: 130px">
                    <el-option label="启用" :value="1" />
                    <el-option label="停用" :value="0" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button v-auth="'calendar:year:list'" type="primary" :disabled="!selectedCalendar" @click="searchYears">查询</el-button>
                  <el-button @click="resetYears">重置</el-button>
                </el-form-item>
              </el-form>

              <el-table v-loading="yearLoading" :data="yearRows" stripe @row-click="selectYear">
                <el-table-column prop="year" label="年度" width="110" />
                <el-table-column prop="calendarName" label="日历名称" min-width="160" show-overflow-tooltip />
                <el-table-column prop="totalDays" label="总天数" width="90" />
                <el-table-column prop="workdays" label="工作日" width="90" />
                <el-table-column prop="restdays" label="休息日" width="90" />
                <el-table-column prop="legalHolidays" label="法定节假日" width="110" />
                <el-table-column prop="adjustedWorkdays" label="调休补班" width="100" />
                <el-table-column label="状态" width="90">
                  <template #default="{ row }">
                    <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
                      {{ row.enabled === 1 ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="190" fixed="right">
                  <template #default="{ row }">
                    <el-button v-auth="'calendar:day:list'" link type="primary" size="small" @click.stop="selectYear(row)">查看日期</el-button>
                    <el-button v-auth="'calendar:year:enabled'" link type="primary" size="small" @click.stop="toggleYear(row)">
                      {{ row.enabled === 1 ? '停用' : '启用' }}
                    </el-button>
                    <el-button v-auth="'calendar:year:delete'" link type="danger" size="small" @click.stop="deleteYear(row)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <Pagination
                v-model:current-page="yearQuery.pageNum"
                v-model:page-size="yearQuery.pageSize"
                :total="yearTotal"
                @change="loadYears"
              />
            </el-tab-pane>

            <el-tab-pane label="日期明细" name="days">
              <el-form :inline="true" :model="dayQuery" class="search-form">
                <el-form-item label="年度">
                  <el-date-picker
                    v-model="dayYearPickerValue"
                    type="year"
                    value-format="YYYY"
                    placeholder="选择年度"
                    style="width: 140px"
                  />
                </el-form-item>
                <el-form-item label="日期">
                  <el-date-picker
                    v-model="dayRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    value-format="YYYY-MM-DD"
                  />
                </el-form-item>
                <el-form-item label="类型">
                  <el-select v-model="dayQuery.dayType" clearable placeholder="全部" style="width: 150px">
                    <el-option v-for="item in dayTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="工作日">
                  <el-select v-model="dayQuery.workday" clearable placeholder="全部" style="width: 120px">
                    <el-option label="是" :value="true" />
                    <el-option label="否" :value="false" />
                  </el-select>
                </el-form-item>
                <el-form-item label="关键词">
                  <el-input v-model="dayQuery.keyword" clearable placeholder="名称/来源/备注" />
                </el-form-item>
                <el-form-item>
                  <el-button v-auth="'calendar:day:list'" type="primary" :disabled="!selectedCalendar" @click="searchDays">查询</el-button>
                  <el-button @click="resetDays">重置</el-button>
                </el-form-item>
              </el-form>

              <el-table
                v-loading="dayLoading"
                :data="dayRows"
                stripe
                @selection-change="selectedRows = $event"
              >
                <el-table-column type="selection" width="48" />
                <el-table-column prop="date" label="日期" width="120" />
                <el-table-column label="星期" width="80">
                  <template #default="{ row }">{{ weekDayLabel(row.dayOfWeek) }}</template>
                </el-table-column>
                <el-table-column label="农历/节气" min-width="170" show-overflow-tooltip>
                  <template #default="{ row }">
                    <div class="lunar-cell">
                      <span>{{ lunarLabel(row) }}</span>
                      <el-tag v-if="row.solarTerm" type="warning" size="small">{{ row.solarTerm }}</el-tag>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="类型" width="120">
                  <template #default="{ row }">
                    <el-tag :type="row.workday ? 'success' : 'info'" size="small">{{ dayTypeLabel(row.dayType) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="工作日" width="90">
                  <template #default="{ row }">{{ row.workday ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column prop="dayName" label="名称" min-width="140" show-overflow-tooltip />
                <el-table-column prop="source" label="来源" width="120" show-overflow-tooltip />
                <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
                <el-table-column prop="updateTime" label="更新时间" width="180" />
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="{ row }">
                    <el-button v-auth="'calendar:day:edit'" link type="primary" size="small" @click="openDayDialog(row)">编辑</el-button>
                    <el-button v-auth="'calendar:day:delete'" link type="danger" size="small" @click="deleteDay(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <Pagination
                v-model:current-page="dayQuery.pageNum"
                v-model:page-size="dayQuery.pageSize"
                :total="dayTotal"
                @change="loadDays"
              />
            </el-tab-pane>
          </el-tabs>
        </main>
      </div>
    </el-card>

    <el-dialog v-model="calendarDialogVisible" :title="calendarForm.id ? '编辑日历' : '新增日历'" width="520px" destroy-on-close>
      <el-form ref="calendarFormRef" :model="calendarForm" :rules="calendarRules" label-width="92px">
        <el-form-item label="日历编码" prop="calendarCode">
          <el-input v-model="calendarForm.calendarCode" placeholder="如 CN_STANDARD" />
        </el-form-item>
        <el-form-item label="日历名称" prop="calendarName">
          <el-input v-model="calendarForm.calendarName" placeholder="如 中国标准工作日历" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="calendarDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="calendarSaving" @click="saveCalendar">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="yearDialogVisible" title="初始化年度" width="560px" destroy-on-close>
      <el-form ref="yearFormRef" :model="yearForm" :rules="yearRules" label-width="112px">
        <el-form-item label="日历">
          <el-input :model-value="selectedCalendarLabel" disabled />
        </el-form-item>
        <el-form-item label="年度" prop="year">
          <el-date-picker
            v-model="yearFormYearPicker"
            type="year"
            value-format="YYYY"
            placeholder="选择年度"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="复制来源年度">
          <el-date-picker
            v-model="yearFormSourceYearPicker"
            clearable
            type="year"
            value-format="YYYY"
            placeholder="不复制则留空"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="覆盖已有数据">
          <el-switch v-model="yearForm.overwrite" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="yearDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="yearSaving" @click="initYear">初始化</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dayDialogVisible" title="编辑日期" width="560px" destroy-on-close>
      <el-form ref="dayFormRef" :model="dayForm" :rules="dayRules" label-width="92px">
        <el-form-item label="日期">
          <el-input :model-value="`${dayForm.date || ''} ${weekDayLabel(dayForm.dayOfWeek)}`" disabled />
        </el-form-item>
        <el-form-item label="日期类型" prop="dayType">
          <el-select v-model="dayForm.dayType" style="width: 100%">
            <el-option v-for="item in dayTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="dayForm.dayName" placeholder="如 春节、调休补班" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="dayForm.source" placeholder="如 国务院公告、手工维护" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dayForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dayDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="daySaving" @click="saveDay">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量设置日期" width="560px" destroy-on-close>
      <el-alert :title="`已选择 ${selectedRows.length} 天`" type="info" :closable="false" show-icon />
      <el-form ref="batchFormRef" :model="batchForm" :rules="dayRules" label-width="92px" style="margin-top: 16px">
        <el-form-item label="日期类型" prop="dayType">
          <el-select v-model="batchForm.dayType" style="width: 100%">
            <el-option v-for="item in dayTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="batchForm.dayName" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="batchForm.source" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="saveBatch">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="toolDrawerVisible" title="日历工具" size="460px">
      <div class="tool-calendar">
        <span>日历</span>
        <strong>{{ selectedCalendarLabel || '-' }}</strong>
      </div>
      <div class="tool-grid">
        <button
          v-for="tool in calendarTools"
          :key="tool.key"
          class="tool-card"
          type="button"
          @click="openToolDialog(tool.key)"
        >
          <component :is="tool.icon" class="tool-card-icon" />
          <span>{{ tool.title }}</span>
        </button>
      </div>
    </el-drawer>

    <el-dialog v-model="toolDialogVisible" :title="activeTool?.title || '日历工具'" width="560px" destroy-on-close>
      <el-form :model="toolForm" label-width="112px">
        <el-form-item label="日历">
          <el-input :model-value="selectedCalendarLabel" disabled />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('date')" label="日期">
          <el-date-picker v-model="toolForm.date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('sourceDate')" label="起算日期">
          <el-date-picker v-model="toolForm.sourceDate" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('amount')" label="偏移工作日">
          <el-input-number v-model="toolForm.amount" :min="-366" :max="366" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('includeSource')" label="包含起算日">
          <el-switch v-model="toolForm.includeSource" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('range')" label="区间">
          <el-date-picker
            v-model="toolForm.range"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('includeBoundary')" label="包含边界">
          <el-checkbox v-model="toolForm.includeStart">开始日期</el-checkbox>
          <el-checkbox v-model="toolForm.includeEnd">结束日期</el-checkbox>
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('month')" label="月份">
          <el-date-picker v-model="toolForm.month" type="month" value-format="YYYY-MM" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('nth')" label="第 N 个">
          <el-input-number v-model="toolForm.nth" :min="1" :max="31" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('termYear')" label="年度">
          <el-date-picker v-model="toolForm.termYear" type="year" value-format="YYYY" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('lunarYear')" label="农历年">
          <el-date-picker v-model="toolForm.lunarYearText" type="year" value-format="YYYY" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('lunarMonth')" label="农历月">
          <el-input-number v-model="toolForm.lunarMonth" :min="1" :max="12" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('lunarDay')" label="农历日">
          <el-input-number v-model="toolForm.lunarDay" :min="1" :max="30" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="activeTool?.fields.includes('leapMonth')" label="闰月">
          <el-switch v-model="toolForm.leapMonth" />
        </el-form-item>
      </el-form>

      <el-descriptions v-if="toolResultItems.length" :column="1" border class="tool-result">
        <el-descriptions-item v-for="item in toolResultItems" :key="item.label" :label="item.label">
          {{ item.value }}
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="toolDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="toolLoading" @click="runTool">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  Calendar,
  CircleCheck,
  CircleClose,
  DataAnalysis,
  Delete,
  Edit,
  Operation,
  Position,
  Right,
  Search,
  Sunny,
  Tools,
} from '@element-plus/icons-vue';
import Pagination from '@mango/common/components/Pagination/index.vue';
import {
  calendarApi,
  dayTypeLabel,
  dayTypeOptions,
  weekDayLabel,
  type CalendarDayType,
  type CalendarDayVO,
  type CalendarVO,
  type CalendarYearSummaryVO,
  type SolarTermVO,
} from '../../api/calendar';

type ToolKey = 'day' | 'check' | 'next' | 'previous' | 'add' | 'count' | 'month' | 'nth' | 'lunarDay' | 'lunarToSolar' | 'solarTerms';
type ToolField = 'date' | 'sourceDate' | 'amount' | 'includeSource' | 'range' | 'includeBoundary' | 'month' | 'nth'
  | 'termYear' | 'lunarYear' | 'lunarMonth' | 'lunarDay' | 'leapMonth';

const activeTab = ref<'years' | 'days'>('years');
const calendarLoading = ref(false);
const yearLoading = ref(false);
const dayLoading = ref(false);
const calendarRows = ref<CalendarVO[]>([]);
const yearRows = ref<CalendarYearSummaryVO[]>([]);
const dayRows = ref<CalendarDayVO[]>([]);
const selectedRows = ref<CalendarDayVO[]>([]);
const selectedCalendar = ref<CalendarVO>();
const selectedYear = ref<number>();
const calendarTotal = ref(0);
const yearTotal = ref(0);
const dayTotal = ref(0);

const calendarQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' as number | '' });
const yearQuery = reactive({ pageNum: 1, pageSize: 10, calendarCode: '', year: '' as number | '', enabled: '' as number | '' });
const dayQuery = reactive({
  pageNum: 1,
  pageSize: 20,
  calendarCode: '',
  year: '' as number | '',
  startDate: '',
  endDate: '',
  dayType: '' as CalendarDayType | '',
  workday: '' as boolean | '',
  enabled: 1,
  keyword: '',
});
const dayRange = ref<[string, string] | null>(null);
const yearPickerValue = computed({
  get: () => (yearQuery.year === '' ? '' : String(yearQuery.year)),
  set: value => { yearQuery.year = parseYearPickerValue(value); },
});
const dayYearPickerValue = computed({
  get: () => (dayQuery.year === '' ? '' : String(dayQuery.year)),
  set: value => { dayQuery.year = parseYearPickerValue(value); },
});
const yearFormYearPicker = computed({
  get: () => String(yearForm.year || ''),
  set: value => { yearForm.year = parseYearPickerValue(value) || new Date().getFullYear(); },
});
const yearFormSourceYearPicker = computed({
  get: () => (yearForm.sourceYear ? String(yearForm.sourceYear) : ''),
  set: value => { yearForm.sourceYear = parseYearPickerValue(value) || undefined; },
});
const selectedCalendarLabel = computed(() =>
  selectedCalendar.value ? `${selectedCalendar.value.calendarName}（${selectedCalendar.value.calendarCode}）` : '',
);
const calendarDialogVisible = ref(false);
const yearDialogVisible = ref(false);
const dayDialogVisible = ref(false);
const batchDialogVisible = ref(false);
const toolDrawerVisible = ref(false);
const toolDialogVisible = ref(false);
const calendarSaving = ref(false);
const yearSaving = ref(false);
const daySaving = ref(false);
const batchSaving = ref(false);
const toolLoading = ref(false);
const calendarFormRef = ref<FormInstance>();
const yearFormRef = ref<FormInstance>();
const dayFormRef = ref<FormInstance>();
const batchFormRef = ref<FormInstance>();

const calendarForm = reactive<CalendarVO>({ calendarCode: '', calendarName: '', status: 1 });
const yearForm = reactive({ year: new Date().getFullYear(), sourceYear: undefined as number | undefined, overwrite: false });
const dayForm = reactive<Partial<CalendarDayVO>>({ dayType: 'WORKDAY' });
const batchForm = reactive({ dayType: 'WORKDAY' as CalendarDayType, dayName: '', source: '手工维护', remark: '' });
const activeToolKey = ref<ToolKey>('check');
const toolForm = reactive({
  date: today(),
  sourceDate: today(),
  amount: 1,
  includeSource: false,
  range: [today(), today()] as [string, string],
  includeStart: true,
  includeEnd: true,
  month: today().slice(0, 7),
  nth: 1,
  termYear: String(new Date().getFullYear()),
  lunarYearText: String(new Date().getFullYear()),
  lunarMonth: 1,
  lunarDay: 1,
  leapMonth: false,
});
const toolResult = ref<Record<string, unknown> | CalendarDayVO | SolarTermVO[]>();

const calendarTools = [
  { key: 'day' as const, title: '日期详情', fields: ['date'] as ToolField[], icon: markRaw(Search) },
  { key: 'check' as const, title: '是否工作日', fields: ['date'] as ToolField[], icon: markRaw(CircleCheck) },
  { key: 'next' as const, title: '下一个工作日', fields: ['date'] as ToolField[], icon: markRaw(Right) },
  { key: 'previous' as const, title: '上一个工作日', fields: ['date'] as ToolField[], icon: markRaw(Position) },
  { key: 'add' as const, title: '偏移工作日', fields: ['sourceDate', 'amount', 'includeSource'] as ToolField[], icon: markRaw(Operation) },
  { key: 'count' as const, title: '区间工作日数', fields: ['range', 'includeBoundary'] as ToolField[], icon: markRaw(DataAnalysis) },
  { key: 'month' as const, title: '月份汇总', fields: ['month'] as ToolField[], icon: markRaw(Calendar) },
  { key: 'nth' as const, title: '第 N 个工作日', fields: ['month', 'nth'] as ToolField[], icon: markRaw(Calendar) },
  { key: 'lunarDay' as const, title: '农历查询', fields: ['date'] as ToolField[], icon: markRaw(Sunny) },
  { key: 'lunarToSolar' as const, title: '农历转公历', fields: ['lunarYear', 'lunarMonth', 'lunarDay', 'leapMonth'] as ToolField[], icon: markRaw(Sunny) },
  { key: 'solarTerms' as const, title: '节气查询', fields: ['termYear'] as ToolField[], icon: markRaw(Sunny) },
];

const activeTool = computed(() => calendarTools.find(tool => tool.key === activeToolKey.value));
const toolResultItems = computed(() => formatToolResult(activeToolKey.value, toolResult.value));

const calendarRules: FormRules = {
  calendarCode: [{ required: true, message: '请输入日历编码', trigger: 'blur' }],
  calendarName: [{ required: true, message: '请输入日历名称', trigger: 'blur' }],
};
const yearRules: FormRules = {
  year: [{ required: true, message: '请输入年度', trigger: 'change' }],
};
const dayRules: FormRules = {
  dayType: [{ required: true, message: '请选择日期类型', trigger: 'change' }],
};

onMounted(async () => {
  await loadCalendars();
});

async function loadCalendars() {
  calendarLoading.value = true;
  try {
    const result = await calendarApi.pageCalendars(calendarQuery);
    calendarRows.value = result.list;
    calendarTotal.value = result.total;
    if (!selectedCalendar.value && result.list.length) {
      selectCalendar(result.list[0]);
    }
  } finally {
    calendarLoading.value = false;
  }
}

async function loadYears() {
  if (!selectedCalendar.value) return;
  yearLoading.value = true;
  try {
    yearQuery.calendarCode = selectedCalendar.value.calendarCode;
    const result = await calendarApi.pageCalendarYears(yearQuery);
    yearRows.value = result.list;
    yearTotal.value = result.total;
  } finally {
    yearLoading.value = false;
  }
}

async function loadDays() {
  if (!selectedCalendar.value) return;
  dayLoading.value = true;
  try {
    dayQuery.calendarCode = selectedCalendar.value.calendarCode;
    if (dayRange.value) {
      dayQuery.startDate = dayRange.value[0];
      dayQuery.endDate = dayRange.value[1];
    } else {
      dayQuery.startDate = '';
      dayQuery.endDate = '';
    }
    const result = await calendarApi.pageCalendarDays(dayQuery);
    dayRows.value = result.list;
    dayTotal.value = result.total;
  } finally {
    dayLoading.value = false;
  }
}

function selectCalendar(row?: CalendarVO) {
  if (!row) return;
  selectedCalendar.value = row;
  selectedYear.value = undefined;
  activeTab.value = 'years';
  yearRows.value = [];
  dayRows.value = [];
  yearQuery.pageNum = 1;
  dayQuery.pageNum = 1;
  loadYears();
}

function selectYear(row: CalendarYearSummaryVO) {
  selectedYear.value = row.year;
  dayQuery.year = row.year;
  activeTab.value = 'days';
  loadDays();
}

function handleTabChange() {
  if (activeTab.value === 'years') loadYears();
  if (activeTab.value === 'days') loadDays();
}

function searchYears() {
  yearQuery.pageNum = 1;
  loadYears();
}

function resetYears() {
  yearQuery.year = '';
  yearQuery.enabled = '';
  searchYears();
}

function searchDays() {
  dayQuery.pageNum = 1;
  loadDays();
}

function resetDays() {
  dayQuery.year = selectedYear.value || '';
  dayRange.value = null;
  dayQuery.dayType = '';
  dayQuery.workday = '';
  dayQuery.enabled = 1;
  dayQuery.keyword = '';
  searchDays();
}

function openCalendarDialog(row?: CalendarVO) {
  Object.assign(calendarForm, row || { id: undefined, calendarCode: '', calendarName: '', status: 1 });
  calendarDialogVisible.value = true;
}

async function saveCalendar() {
  await calendarFormRef.value?.validate();
  calendarSaving.value = true;
  try {
    if (calendarForm.id) {
      await calendarApi.updateCalendar(calendarForm);
    } else {
      await calendarApi.createCalendar(calendarForm);
    }
    ElMessage.success('保存成功');
    calendarDialogVisible.value = false;
    await loadCalendars();
  } finally {
    calendarSaving.value = false;
  }
}

async function toggleCalendarStatus(row: CalendarVO) {
  const next = row.status === 1 ? 0 : 1;
  await calendarApi.updateCalendarStatus(row.id!, next);
  ElMessage.success('状态已更新');
  await loadCalendars();
}

async function deleteCalendar(row: CalendarVO) {
  await ElMessageBox.confirm(
    `删除日历「${row.calendarName}」会同时删除它的全部年度和日期明细，是否继续？`,
    '删除日历',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  );
  await calendarApi.deleteCalendar(row.id!);
  ElMessage.success('删除成功');
  if (selectedCalendar.value?.id === row.id) {
    selectedCalendar.value = undefined;
    selectedYear.value = undefined;
    yearRows.value = [];
    dayRows.value = [];
    dayTotal.value = 0;
    yearTotal.value = 0;
  }
  await loadCalendars();
}

function openYearDialog() {
  if (!selectedCalendar.value) return;
  yearForm.year = selectedYear.value || new Date().getFullYear();
  yearForm.sourceYear = selectedYear.value;
  yearForm.overwrite = false;
  yearDialogVisible.value = true;
}

async function initYear() {
  if (!selectedCalendar.value) return;
  await yearFormRef.value?.validate();
  yearSaving.value = true;
  try {
    await calendarApi.initCalendarYear({
      calendarCode: selectedCalendar.value.calendarCode,
      year: yearForm.year,
      sourceYear: yearForm.sourceYear,
      overwrite: yearForm.overwrite,
    });
    ElMessage.success('年度已初始化');
    yearDialogVisible.value = false;
    await loadYears();
  } finally {
    yearSaving.value = false;
  }
}

async function toggleYear(row: CalendarYearSummaryVO) {
  await calendarApi.updateCalendarYearEnabled(row.calendarCode, row.year, row.enabled === 1 ? 0 : 1);
  ElMessage.success('年度状态已更新');
  await loadYears();
  await loadDays();
}

async function deleteYear(row: CalendarYearSummaryVO) {
  await ElMessageBox.confirm(
    `删除 ${row.year} 年度会删除该年度全部日期明细，是否继续？`,
    '删除年度',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  );
  await calendarApi.deleteCalendarYear(row.calendarCode, row.year);
  ElMessage.success('删除成功');
  if (selectedYear.value === row.year) {
    selectedYear.value = undefined;
    dayRows.value = [];
    dayTotal.value = 0;
    dayQuery.year = '';
    activeTab.value = 'years';
  }
  await loadYears();
}

function openDayDialog(row: CalendarDayVO) {
  Object.assign(dayForm, row);
  dayDialogVisible.value = true;
}

async function saveDay() {
  await dayFormRef.value?.validate();
  daySaving.value = true;
  try {
    await calendarApi.updateCalendarDay({
      id: dayForm.id!,
      dayType: dayForm.dayType as CalendarDayType,
      dayName: dayForm.dayName,
      source: dayForm.source,
      remark: dayForm.remark,
    });
    ElMessage.success('保存成功');
    dayDialogVisible.value = false;
    await loadDays();
    await refreshSummary();
  } finally {
    daySaving.value = false;
  }
}

async function deleteDay(row: CalendarDayVO) {
  await ElMessageBox.confirm(
    `删除日期 ${row.date} 后，该日期不会再参与工作日计算，是否继续？`,
    '删除日期',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  );
  await calendarApi.deleteCalendarDay(row.id!);
  ElMessage.success('删除成功');
  await loadDays();
  await refreshSummary();
}

function openBatchDialog() {
  Object.assign(batchForm, { dayType: 'WORKDAY', dayName: '', source: '手工维护', remark: '' });
  batchDialogVisible.value = true;
}

async function saveBatch() {
  await batchFormRef.value?.validate();
  batchSaving.value = true;
  try {
    await calendarApi.batchUpdateCalendarDays({
      ids: selectedRows.value.map(row => row.id!),
      ...batchForm,
    });
    ElMessage.success('批量设置成功');
    batchDialogVisible.value = false;
    await loadDays();
    await refreshSummary();
  } finally {
    batchSaving.value = false;
  }
}

async function refreshSummary() {
  if (!selectedCalendar.value || !selectedYear.value) return;
  await loadYears();
}

function openToolDialog(key: ToolKey) {
  if (!selectedCalendar.value) return;
  activeToolKey.value = key;
  toolResult.value = undefined;
  toolDialogVisible.value = true;
}

async function runTool() {
  if (!selectedCalendar.value) return;
  toolLoading.value = true;
  try {
    const calendarCode = selectedCalendar.value.calendarCode;
    if (activeToolKey.value === 'day') {
      toolResult.value = await calendarApi.getDay({ calendarCode, date: toolForm.date }) as unknown as Record<string, unknown>;
      return;
    }
    if (activeToolKey.value === 'lunarDay') {
      toolResult.value = await calendarApi.lunarDay({ date: toolForm.date }) as unknown as Record<string, unknown>;
      return;
    }
    if (activeToolKey.value === 'lunarToSolar') {
      toolResult.value = {
        date: await calendarApi.lunarToSolar({
          lunarYear: Number(toolForm.lunarYearText),
          lunarMonth: toolForm.lunarMonth,
          lunarDay: toolForm.lunarDay,
          leapMonth: toolForm.leapMonth,
        }),
      };
      return;
    }
    if (activeToolKey.value === 'solarTerms') {
      toolResult.value = await calendarApi.solarTerms({ year: Number(toolForm.termYear) }) as unknown as Record<string, unknown>;
      return;
    }
    if (activeToolKey.value === 'check') {
      toolResult.value = { workday: await calendarApi.isWorkday({ calendarCode, date: toolForm.date }) };
      return;
    }
    if (activeToolKey.value === 'next') {
      toolResult.value = { date: await calendarApi.nextWorkday({ calendarCode, date: toolForm.date }) };
      return;
    }
    if (activeToolKey.value === 'previous') {
      toolResult.value = { date: await calendarApi.previousWorkday({ calendarCode, date: toolForm.date }) };
      return;
    }
    if (activeToolKey.value === 'add') {
      toolResult.value = {
        date: await calendarApi.addWorkdays({
          calendarCode,
          sourceDate: toolForm.sourceDate,
          amount: toolForm.amount,
          includeSource: toolForm.includeSource,
        }),
      };
      return;
    }
    if (activeToolKey.value === 'count') {
      const [startDate, endDate] = toolForm.range;
      toolResult.value = {
        count: await calendarApi.countWorkdays({
          calendarCode,
          startDate,
          endDate,
          includeStart: toolForm.includeStart,
          includeEnd: toolForm.includeEnd,
        }),
      };
      return;
    }
    const [year, month] = toolForm.month.split('-').map(Number);
    if (activeToolKey.value === 'month') {
      toolResult.value = await calendarApi.monthSummary({ calendarCode, year, month }) as unknown as Record<string, unknown>;
      return;
    }
    toolResult.value = {
      date: await calendarApi.nthWorkdayOfMonth({ calendarCode, year, month, nth: toolForm.nth }),
    };
  } finally {
    toolLoading.value = false;
  }
}

function formatToolResult(key: ToolKey, result?: Record<string, unknown> | CalendarDayVO | SolarTermVO[]) {
  if (!result) return [];
  if (key === 'day') {
    const day = result as unknown as CalendarDayVO;
    return [
      { label: '日期', value: day.date },
      { label: '星期', value: weekDayLabel(day.dayOfWeek) },
      { label: '类型', value: dayTypeLabel(day.dayType) },
      { label: '是否工作日', value: day.workday ? '是' : '否' },
      { label: '名称', value: day.dayName || '-' },
      { label: '农历', value: lunarLabel(day) },
      { label: '节气', value: day.solarTerm || '-' },
      { label: '来源', value: day.source || '-' },
    ];
  }
  if (key === 'lunarDay') {
    const lunar = result as Record<string, unknown>;
    return [
      { label: '公历日期', value: lunar.solarDate },
      { label: '农历', value: lunar.lunarText },
      { label: '干支纪年', value: lunar.ganzhiYear },
      { label: '生肖', value: lunar.zodiac },
      { label: '节气', value: lunar.solarTerm || '-' },
    ];
  }
  if (key === 'lunarToSolar') {
    return [{ label: '公历日期', value: (result as Record<string, unknown>).date }];
  }
  if (key === 'solarTerms') {
    const terms = result as SolarTermVO[];
    return terms.map(term => ({ label: term.name, value: term.date }));
  }
  if (key === 'check') {
    return [{ label: '是否工作日', value: result.workday ? '是' : '否' }];
  }
  if (key === 'count') {
    return [{ label: '工作日数量', value: result.count }];
  }
  if (key === 'month') {
    return [
      { label: '总天数', value: result.totalDays },
      { label: '工作日', value: result.workdays },
      { label: '休息日', value: result.restdays },
      { label: '首个工作日', value: result.firstWorkday || '-' },
      { label: '最后工作日', value: result.lastWorkday || '-' },
    ];
  }
  return [{ label: '结果日期', value: result.date }];
}

function lunarLabel(day: Partial<CalendarDayVO>) {
  const lunar = day.lunarText || '-';
  const ganzhi = day.ganzhiYear ? `${day.ganzhiYear}年` : '';
  const zodiac = day.zodiac ? `${day.zodiac}年` : '';
  return [lunar, ganzhi, zodiac].filter(Boolean).join(' / ');
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function parseYearPickerValue(value?: string | number) {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  const year = Number(value);
  return Number.isFinite(year) ? year : '';
}
</script>

<style scoped>
.calendar-admin {
  padding: 16px;
}

.calendar-shell {
  border-radius: 8px;
}

.calendar-shell :deep(.el-card__body) {
  height: calc(100vh - 128px);
  min-height: 560px;
}

.calendar-layout {
  display: grid;
  grid-template-columns: minmax(320px, 380px) minmax(0, 1fr);
  gap: 18px;
  height: 100%;
  min-height: 0;
}

.calendar-side {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color-lighter);
  padding-right: 18px;
}

.calendar-main {
  min-height: 0;
  overflow: auto;
}

.section-head,
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head h3,
.toolbar h2 {
  margin: 0;
  line-height: 1.2;
}

.section-head span,
.toolbar p {
  display: block;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.side-filter {
  display: grid;
  grid-template-columns: 1fr 112px 70px;
  gap: 8px;
  margin-bottom: 12px;
}

.compact-table :deep(.el-table__cell) {
  padding: 7px 0;
}

.side-actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  white-space: nowrap;
}

.side-actions :deep(.el-button) {
  margin-left: 0;
}

.calendar-list-area {
  min-height: 180px;
  flex: 1;
  overflow: hidden;
}

.side-pagination {
  flex: 0 0 auto;
}

.side-pagination :deep(.el-pagination) {
  justify-content: center;
  padding: 10px 0 0;
}

.side-pagination :deep(.el-pager li) {
  min-width: 28px;
}

.name-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.name-cell strong,
.name-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lunar-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.lunar-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.name-cell span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.search-form {
  margin-bottom: 12px;
}

.tool-calendar {
  display: grid;
  gap: 6px;
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px 14px;
  background: var(--el-fill-color-lighter);
}

.tool-calendar span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.tool-calendar strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.tool-card {
  display: flex;
  min-height: 72px;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 0 12px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: border-color 0.16s ease, background-color 0.16s ease, color 0.16s ease;
}

.tool-card:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.tool-card-icon {
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
}

.tool-result {
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .calendar-layout {
    grid-template-columns: 1fr;
  }

  .calendar-side {
    min-height: 360px;
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
    padding-right: 0;
    padding-bottom: 16px;
  }
}

@media (max-width: 760px) {
  .calendar-admin {
    padding: 10px;
  }

  .side-filter {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .tool-grid {
    grid-template-columns: 1fr;
  }
}
</style>
