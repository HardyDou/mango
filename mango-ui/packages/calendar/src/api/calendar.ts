import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export type CalendarDayType =
  | 'WORKDAY'
  | 'RESTDAY'
  | 'LEGAL_HOLIDAY'
  | 'ADJUSTED_WORKDAY'
  | 'TEMP_CLOSED_DAY'
  | 'TEMP_OPEN_DAY';

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface CalendarVO {
  id?: ApiId;
  calendarCode: string;
  calendarName: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface CalendarOptionVO {
  calendarCode: string;
  calendarName: string;
  status: number;
}

export interface CalendarYearSummaryVO {
  calendarCode: string;
  calendarName: string;
  year: number;
  totalDays: number;
  workdays: number;
  restdays: number;
  legalHolidays: number;
  adjustedWorkdays: number;
  tempClosedDays: number;
  tempOpenDays: number;
  enabled: number;
}

export interface CalendarDayVO {
  id?: ApiId;
  calendarCode: string;
  calendarName?: string;
  calendarYear: number;
  date: string;
  dayOfWeek: number;
  dayType: CalendarDayType;
  workday: boolean;
  dayName?: string;
  lunarYear?: number;
  lunarMonth?: number;
  lunarDay?: number;
  lunarLeapMonth?: boolean;
  lunarText?: string;
  ganzhiYear?: string;
  zodiac?: string;
  solarTerm?: string;
  source?: string;
  remark?: string;
  enabled?: number;
  updateTime?: string;
}

export interface CalendarPageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number | '';
}

export interface CalendarYearPageQuery {
  pageNum?: number;
  pageSize?: number;
  calendarCode?: string;
  year?: number | '';
  enabled?: number | '';
}

export interface CalendarDayPageQuery {
  pageNum?: number;
  pageSize?: number;
  calendarCode?: string;
  year?: number | '';
  startDate?: string;
  endDate?: string;
  dayType?: CalendarDayType | '';
  workday?: boolean | '';
  enabled?: number | '';
  keyword?: string;
}

export interface InitCalendarYearCommand {
  calendarCode: string;
  year: number;
  overwrite?: boolean;
  sourceYear?: number;
}

export interface RefreshCalendarYearLunarCommand {
  calendarCode: string;
  year: number;
}

export interface UpdateCalendarDayCommand {
  id: ApiId;
  dayType: CalendarDayType;
  dayName?: string;
  source?: string;
  remark?: string;
}

export interface BatchUpdateCalendarDaysCommand {
  ids: ApiId[];
  dayType: CalendarDayType;
  dayName?: string;
  source?: string;
  remark?: string;
}

export interface ImportCalendarDaysCommand {
  calendarCode: string;
  year: number;
  items: Array<{
    date: string;
    dayType: CalendarDayType;
    dayName?: string;
    source?: string;
    remark?: string;
  }>;
}

export interface AddWorkdaysQuery {
  calendarCode: string;
  sourceDate: string;
  amount: number;
  includeSource?: boolean;
}

export interface CountWorkdaysQuery {
  calendarCode: string;
  startDate: string;
  endDate: string;
  includeStart?: boolean;
  includeEnd?: boolean;
}

export interface MonthQuery {
  calendarCode: string;
  year: number;
  month: number;
}

export interface NthWorkdayOfMonthQuery extends MonthQuery {
  nth: number;
}

export interface MonthWorkdaySummaryVO {
  calendarCode: string;
  year: number;
  month: number;
  totalDays: number;
  workdays: number;
  restdays: number;
  firstWorkday?: string;
  lastWorkday?: string;
}

export interface LunarDayInfoVO {
  solarDate: string;
  lunarYear: number;
  lunarMonth: number;
  lunarDay: number;
  lunarLeapMonth: boolean;
  lunarText: string;
  ganzhiYear: string;
  zodiac: string;
  solarTerm?: string;
}

export interface LunarDateQuery {
  lunarYear: number;
  lunarMonth: number;
  lunarDay: number;
  leapMonth?: boolean;
}

export interface SolarTermVO {
  name: string;
  date: string;
}

export const calendarApi = {
  pageCalendars: (params?: CalendarPageQuery) => get<any>('/calendar/admin/calendars/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, fromBackendCalendar, params)),
  listCalendarOptions: (params?: { keyword?: string; includeDisabled?: boolean }) =>
    get<CalendarOptionVO[]>('/calendar/admin/calendars/options', { params }),
  createCalendar: (data: CalendarVO) => post<ApiId>('/calendar/admin/calendars', {
    calendarCode: data.calendarCode,
    calendarName: data.calendarName,
  }),
  updateCalendar: (data: CalendarVO) => put<boolean>('/calendar/admin/calendars', {
    id: normalizeOptionalId(data.id),
    calendarCode: data.calendarCode,
    calendarName: data.calendarName,
  }),
  updateCalendarStatus: (id: ApiId, status: number) => put<boolean>('/calendar/admin/calendars/status', {
    id: normalizeOptionalId(id),
    status,
  }),
  deleteCalendar: (id: ApiId) => del<boolean>('/calendar/admin/calendars', { params: { id: normalizeOptionalId(id) } }),
  pageCalendarYears: (params?: CalendarYearPageQuery) =>
    get<any>('/calendar/admin/years/page', { params: toBackendPageParams(params) })
      .then(data => fromBackendPageResult(data, item => item as CalendarYearSummaryVO, params)),
  initCalendarYear: (data: InitCalendarYearCommand) => post<boolean>('/calendar/admin/years/init', data),
  refreshCalendarYearLunar: (data: RefreshCalendarYearLunarCommand) => put<boolean>('/calendar/admin/years/lunar', data),
  updateCalendarYearEnabled: (calendarCode: string, year: number, enabled: number) =>
    put<boolean>('/calendar/admin/years/enabled', { calendarCode, year, enabled }),
  deleteCalendarYear: (calendarCode: string, year: number) =>
    del<boolean>('/calendar/admin/years', { params: { calendarCode, year } }),
  yearSummary: (calendarCode: string, year: number) =>
    get<CalendarYearSummaryVO>('/calendar/admin/years/summary', { params: { calendarCode, year } }),
  pageCalendarDays: (params?: CalendarDayPageQuery) =>
    get<any>('/calendar/admin/days/page', { params: toBackendPageParams(params) })
      .then(data => fromBackendPageResult(data, fromBackendDay, params)),
  updateCalendarDay: (data: UpdateCalendarDayCommand) => put<boolean>('/calendar/admin/days', toBackendDayCommand(data)),
  deleteCalendarDay: (id: ApiId) => del<boolean>('/calendar/admin/days', { params: { id: normalizeOptionalId(id) } }),
  batchUpdateCalendarDays: (data: BatchUpdateCalendarDaysCommand) =>
    put<boolean>('/calendar/admin/days/batch', { ...data, ids: data.ids.map(normalizeOptionalId) }),
  importCalendarDays: (data: ImportCalendarDaysCommand) => post<boolean>('/calendar/admin/days/import', data),
  getDay: (params: { calendarCode: string; date: string }) => get<CalendarDayVO>('/calendar/workdays/day', { params }).then(fromBackendDay),
  isWorkday: (params: { calendarCode: string; date: string }) => get<boolean>('/calendar/workdays/check', { params }),
  nextWorkday: (params: { calendarCode: string; date: string }) => get<string>('/calendar/workdays/next', { params }),
  previousWorkday: (params: { calendarCode: string; date: string }) => get<string>('/calendar/workdays/previous', { params }),
  addWorkdays: (params: AddWorkdaysQuery) => get<string>('/calendar/workdays/add', { params }),
  countWorkdays: (params: CountWorkdaysQuery) => get<number>('/calendar/workdays/count', { params }),
  monthSummary: (params: MonthQuery) => get<MonthWorkdaySummaryVO>('/calendar/workdays/month/summary', { params }),
  nthWorkdayOfMonth: (params: NthWorkdayOfMonthQuery) => get<string>('/calendar/workdays/month/nth', { params }),
  lunarDay: (params: { date: string }) => get<LunarDayInfoVO>('/calendar/lunar/day', { params }).then(fromBackendLunarDay),
  lunarToSolar: (params: LunarDateQuery) => get<string>('/calendar/lunar/to-solar', { params }),
  solarTerms: (params: { year: number }) => get<SolarTermVO[]>('/calendar/lunar/solar-terms', { params })
    .then(list => list.map(item => ({ ...item, date: normalizeDate(item.date) }))),
};

export const dayTypeOptions: Array<{ label: string; value: CalendarDayType; workday: boolean }> = [
  { label: '工作日', value: 'WORKDAY', workday: true },
  { label: '休息日', value: 'RESTDAY', workday: false },
  { label: '法定节假日', value: 'LEGAL_HOLIDAY', workday: false },
  { label: '调休补班', value: 'ADJUSTED_WORKDAY', workday: true },
  { label: '临时停工日', value: 'TEMP_CLOSED_DAY', workday: false },
  { label: '临时营业日', value: 'TEMP_OPEN_DAY', workday: true },
];

export const weekDayOptions: Array<{ label: string; value: number }> = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
];

export function dayTypeLabel(value?: string) {
  return dayTypeOptions.find(item => item.value === value)?.label || value || '-';
}

export function weekDayLabel(value?: number) {
  return weekDayOptions.find(item => item.value === value)?.label || value || '-';
}

function fromBackendCalendar(item: any): CalendarVO {
  return {
    ...item,
    id: normalizeId(item.id),
  };
}

function fromBackendDay(item: any): CalendarDayVO {
  return {
    ...item,
    id: normalizeId(item.id),
    date: normalizeDate(item.date),
  };
}

function fromBackendLunarDay(item: LunarDayInfoVO): LunarDayInfoVO {
  return {
    ...item,
    solarDate: normalizeDate(item.solarDate),
  };
}

function toBackendDayCommand(data: UpdateCalendarDayCommand) {
  return {
    ...data,
    id: normalizeOptionalId(data.id),
  };
}

function toBackendPageParams(params?: any) {
  if (!params) return params;
  const { pageNum, pageSize, status, year, dayType, enabled, workday, ...rest } = params;
  return {
    ...rest,
    status: status === '' ? undefined : status,
    year: year === '' ? undefined : year,
    dayType: dayType === '' ? undefined : dayType,
    enabled: enabled === '' ? undefined : enabled,
    workday: workday === '' ? undefined : workday,
    page: pageNum,
    size: pageSize,
  };
}

function fromBackendPageResult<T>(data: any, mapper: (item: any) => T, params?: { pageNum?: number; pageSize?: number }): PageResult<T> {
  const list = Array.isArray(data?.list) ? data.list.map(mapper) : [];
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.page ?? params?.pageNum ?? 1),
    pageSize: Number(data?.size ?? params?.pageSize ?? 10),
  };
}

function normalizeId(value: any): string {
  return value === undefined || value === null ? '' : String(value);
}

function normalizeOptionalId(value: any): string | undefined {
  return value === undefined || value === null || value === '' ? undefined : String(value);
}

function normalizeDate(value: any): string {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day] = value;
    return `${year}-${pad(month)}-${pad(day)}`;
  }
  return String(value).slice(0, 10);
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}
