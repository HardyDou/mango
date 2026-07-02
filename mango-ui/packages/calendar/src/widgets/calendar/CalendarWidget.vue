<template>
  <section class="mango-grid-widget-calendar">
    <div
      class="mango-grid-widget-calendar__body"
      :class="{ 'is-loading': loading }"
    >
      <header class="mango-grid-widget-calendar__header">
        <div class="mango-grid-widget-calendar__date-row">
          <strong>{{ displayDate }}</strong>
          <div class="mango-grid-widget-calendar__weekday">{{ weekdayText }}</div>
        </div>

        <div class="mango-grid-widget-calendar__year">{{ yearText }}</div>
      </header>

      <div class="mango-grid-widget-calendar__tags">
        <span
          class="mango-grid-widget-calendar__tag is-workday"
        >
          {{ statusTag }}
        </span>
        <span
          v-if="lunarText"
          class="mango-grid-widget-calendar__tag is-lunar"
        >
          {{ lunarText }}
        </span>
        <span
          v-if="specialText"
          class="mango-grid-widget-calendar__tag is-special"
        >
          {{ specialText }}
        </span>
      </div>

      <p
        v-if="summaryText"
        class="mango-grid-widget-calendar__summary"
      >
        {{ summaryText }}
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { calendarApi, type CalendarDayVO, type LunarDayInfoVO, type MonthWorkdaySummaryVO } from '../../api/calendar';
import type { CalendarWidgetProps } from '../types';

defineOptions({
  name: 'MangoCalendarWidget',
});

const props = withDefaults(defineProps<CalendarWidgetProps>(), {
  calendarCode: 'CN_STANDARD',
});

const loading = ref(false);
const today = ref<CalendarDayVO | null>(null);
const lunarDay = ref<LunarDayInfoVO | null>(null);
const monthSummary = ref<MonthWorkdaySummaryVO | null>(null);

const displayDate = computed(() => {
  const date = today.value?.date;
  if (!date) return '--/--';
  return `${date.slice(5, 7)}/${date.slice(8, 10)}`;
});

const weekdayText = computed(() => {
  const dayOfWeek = today.value?.dayOfWeek;
  const map: Record<number, string> = {
    1: '星期一',
    2: '星期二',
    3: '星期三',
    4: '星期四',
    5: '星期五',
    6: '星期六',
    7: '星期日',
  };
  return map[dayOfWeek || 0] || '星期--';
});

const yearText = computed(() => {
  const date = today.value?.date;
  return date ? `${date.slice(0, 4)}年` : '--年';
});

const lunarText = computed(() => today.value?.lunarText || lunarDay.value?.lunarText || '');

const statusTag = computed(() => today.value?.workday ? '工作日' : '休息日');

const specialText = computed(() => {
  if (!today.value) {
    return '';
  }
  return today.value.dayName || today.value.solarTerm || lunarDay.value?.solarTerm || '无节假日';
});

const summaryText = computed(() => {
  if (!today.value || !monthSummary.value) {
    return '';
  }
  const { lastWorkday } = monthSummary.value;
  if (!lastWorkday) {
    return today.value.workday ? '今天为正常工作日。' : '今天为休息日。';
  }
  const days = diffDays(today.value.date, lastWorkday);
  if (days <= 0) {
    return today.value.workday ? '今天为本月最后一个工作日。' : '今天为休息日。';
  }
  return today.value.workday
    ? `今天为正常工作日，距离本月最后一个工作日还有 ${days} 天。`
    : `今天为休息日，距离本月最后一个工作日还有 ${days} 天。`;
});

onMounted(() => {
  loadCalendar();
});

async function loadCalendar(): Promise<void> {
  loading.value = true;
  try {
    // 小组件内部自行完成日历查询，消费页面只需要提供运行时上下文。
    const date = getToday();
    const [day, lunar, summary] = await Promise.all([
      calendarApi.getDay({
        calendarCode: props.calendarCode,
        date,
      }),
      calendarApi.lunarDay({ date }),
      calendarApi.monthSummary({
        calendarCode: props.calendarCode,
        year: Number(date.slice(0, 4)),
        month: Number(date.slice(5, 7)),
      }),
    ]);
    today.value = day;
    lunarDay.value = lunar;
    monthSummary.value = summary;
  } catch {
    ElMessage.error('日历加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

function getToday(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function diffDays(start: string, end: string): number {
  const startTime = new Date(`${start}T00:00:00`).getTime();
  const endTime = new Date(`${end}T00:00:00`).getTime();
  return Math.round((endTime - startTime) / (24 * 60 * 60 * 1000));
}
</script>
