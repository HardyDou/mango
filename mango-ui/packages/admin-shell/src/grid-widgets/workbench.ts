import { defineComponent, h } from 'vue';
import { Calendar, Files, Menu, Operation, Setting, User } from '@element-plus/icons-vue';
import { ElButton, ElEmpty, ElIcon, ElProgress, ElTag } from 'element-plus';
import type { GridWidgetDefinition } from '@mango/grid-layout';

const MetricWidget = defineComponent({
  name: 'MetricWidget',
  props: {
    value: { type: String, required: true },
    label: { type: String, required: true },
    tone: { type: String, default: 'primary' },
  },
  setup(props) {
    return () => h('div', { class: ['home-widget-metric', `is-${props.tone}`] }, [
      h('div', { class: 'home-widget-metric__value' }, props.value),
      h('div', { class: 'home-widget-metric__label' }, props.label),
    ]);
  },
});

const QuickEntryWidget = defineComponent({
  name: 'QuickEntryWidget',
  setup() {
    const entries = [
      { title: '系统设置', icon: Setting },
      { title: '菜单管理', icon: Menu },
      { title: '文件中心', icon: Files },
      { title: '工作日历', icon: Calendar },
    ];
    return () => h('div', { class: 'home-widget-quick' }, entries.map(item => h('button', {
      class: 'home-widget-quick__item',
      type: 'button',
    }, [
      h(ElIcon, { size: 22 }, () => h(item.icon)),
      h('span', item.title),
    ])));
  },
});

const PlatformStatusWidget = defineComponent({
  name: 'PlatformStatusWidget',
  setup() {
    const items = [
      { label: '权限与组织', value: 88 },
      { label: '流程协同', value: 72 },
      { label: '通用能力', value: 96 },
    ];
    return () => h('div', { class: 'home-widget-status' }, items.map(item => h('div', { class: 'home-widget-status__item' }, [
      h('div', { class: 'home-widget-status__label' }, item.label),
      h(ElProgress, { percentage: item.value, strokeWidth: 8 }),
    ])));
  },
});

const TodoWidget = defineComponent({
  name: 'TodoWidget',
  setup() {
    const todos = ['待处理审批流程', '待确认系统通知', '待维护平台参数'];
    return () => h('div', { class: 'home-widget-list' }, todos.map((todo, index) => h('div', { class: 'home-widget-list__item' }, [
      h(ElTag, { type: index === 0 ? 'warning' : 'info', effect: 'light' }, () => index === 0 ? '待办' : '提醒'),
      h('span', todo),
      h(ElButton, { text: true, type: 'primary' }, () => '查看'),
    ])));
  },
});

const EmptyWidget = defineComponent({
  name: 'EmptyWidget',
  props: {
    text: { type: String, default: '暂无数据' },
  },
  setup(props) {
    return () => h(ElEmpty, { description: props.text, imageSize: 72 });
  },
});

export const workbenchWidgets: GridWidgetDefinition[] = [
  {
    type: 'platform-permission',
    title: '权限与组织',
    description: '用户、角色、菜单、租户基础能力',
    category: '平台',
    icon: User,
    component: MetricWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
    defaultProps: { value: '4 项', label: '核心组织权限能力', tone: 'primary' },
  },
  {
    type: 'platform-workflow',
    title: '流程与协同',
    description: '审批、任务、通知等流程能力',
    category: '平台',
    icon: Operation,
    component: MetricWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
    defaultProps: { value: '3 项', label: '流程协同能力', tone: 'success' },
  },
  {
    type: 'platform-common',
    title: '平台基础能力',
    description: '文件、模板、编号、日历等通用能力',
    category: '平台',
    icon: Calendar,
    component: MetricWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
    defaultProps: { value: '6 项', label: '可复用基础能力', tone: 'warning' },
  },
  {
    type: 'platform-status',
    title: '平台状态',
    description: '查看平台能力配置情况',
    category: '业务',
    icon: Setting,
    component: PlatformStatusWidget,
    defaultLayout: { w: 6, h: 10, minW: 3, minH: 10 },
  },
  {
    type: 'quick-entry',
    title: '常用能力',
    description: '常用后台能力入口',
    category: '业务',
    icon: Menu,
    component: QuickEntryWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
  },
  {
    type: 'todo',
    title: '待办提醒',
    description: '展示当前用户待办提醒',
    category: '业务',
    icon: Operation,
    component: TodoWidget,
    defaultLayout: { w: 6, h: 10, minW: 3, minH: 10 },
  },
  {
    type: 'notice',
    title: '通知公告',
    description: '预留通知公告小组件',
    category: '业务',
    icon: Files,
    component: EmptyWidget,
    defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
    defaultProps: { text: '暂无通知公告' },
  },
];
