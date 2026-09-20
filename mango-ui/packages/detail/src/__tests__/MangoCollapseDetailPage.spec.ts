/* eslint-disable vue/one-component-per-file */
import { mount } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import type { MangoCollapseDetailPageExpose, MangoCollapsePanel } from '../MangoCollapseDetailPage.types';
import MangoCollapseDetailPage from '../MangoCollapseDetailPage.vue';

vi.mock('@mango/common', async () => {
  const { defineComponent, h } = await import('vue');
  const passthrough = (name: string, props: string[] = []) =>
    defineComponent({
      name,
      inheritAttrs: false,
      props,
      setup(componentProps, { attrs, slots }) {
        return () => h('div', { ...attrs, [`data-${name}`]: '' }, slots.default?.());
      },
    });
  const sideDrawer = defineComponent({
    name: 'MangoSideDrawerShell',
    props: {
      modelValue: Boolean,
      showTrigger: Boolean,
      title: { type: String, default: '' },
    },
    emits: ['update:modelValue'],
    setup(_props, { emit, expose, slots }) {
      const open = () => emit('update:modelValue', true);
      const close = () => emit('update:modelValue', false);
      const toggle = () => emit('update:modelValue', !_props.modelValue);
      expose({ open, close, toggle });
      return () => h('div', { 'data-MangoSideDrawerShell': '' }, [slots.main?.(), slots.default?.()]);
    },
  });
  return {
    MangoPageBackBar: passthrough('MangoPageBackBar', ['title', 'showRefresh', 'refreshLoading']),
    MangoSideDrawerShell: sideDrawer,
    MangoDetailSummary: passthrough('MangoDetailSummary'),
    MangoDescriptionList: passthrough('MangoDescriptionList', ['items', 'groups']),
    MangoDataTable: passthrough('MangoDataTable', ['card', 'mode', 'showModeSwitch']),
    RichTextViewer: passthrough('RichTextViewer', ['content']),
  };
});

vi.mock('@mango/file', async () => {
  const { defineComponent, h } = await import('vue');
  const component = (name: string, props: string[]) =>
    defineComponent({
      name,
      props,
      setup(_props, { slots }) {
        return () => h('div', { [`data-${name}`]: '' }, slots.default?.());
      },
    });
  return {
    MangoFileList: component('MangoFileList', ['rows', 'columns']),
    MangoFilePreviewDialog: component('MangoFilePreviewDialog', ['modelValue']),
  };
});

const passthroughStub = (name: string, props: string[] = []) =>
  defineComponent({
    name,
    props,
    setup(_props, { attrs, slots }) {
      return () => h('div', attrs, [slots.title?.(), slots.default?.(), slots.extra?.()]);
    },
  });

const global = {
  stubs: {
    ElTabs: passthroughStub('ElTabs', ['modelValue']),
    ElTabPane: passthroughStub('ElTabPane', ['name', 'label']),
    ElCollapse: passthroughStub('ElCollapse', ['modelValue']),
    ElCollapseItem: passthroughStub('ElCollapseItem', ['name']),
    ElRow: passthroughStub('ElRow'),
    ElCol: passthroughStub('ElCol'),
    ElResult: passthroughStub('ElResult', ['title', 'subTitle']),
    ElTooltip: passthroughStub('ElTooltip'),
    ElButton: defineComponent({
      name: 'ElButton',
      setup(_props, { attrs, slots }) {
        return () => h('button', attrs, slots.default?.());
      },
    }),
    ElEmpty: passthroughStub('ElEmpty', ['description']),
  },
  directives: { loading: () => undefined },
};

const panels: MangoCollapsePanel[] = [
  { name: 'base', title: '基本信息', content: { componentType: 'description-list', data: { items: [] } } },
  { name: 'files', title: '资料文件', content: { componentType: 'file-list', data: { rows: [], columns: [] } } },
];

function mountPage(props: Record<string, unknown> = {}) {
  return mount(MangoCollapseDetailPage, {
    props: { title: '保函详情', panels, ...props } as never,
    global,
  });
}

describe('MangoCollapseDetailPage', () => {
  it('默认展开全部面板，并在关闭自动展开后应用指定 Key', () => {
    const automatic = mountPage();
    expect(automatic.findComponent({ name: 'ElCollapse' }).props('modelValue')).toEqual(['base', 'files']);

    const configured = mountPage({ autoExpandPanels: false, defaultActivePanelNames: ['files'] });
    expect(configured.findComponent({ name: 'ElCollapse' }).props('modelValue')).toEqual(['files']);
  });

  it('拒绝冲突、重复和不存在的默认展开配置', () => {
    expect(() => mountPage({ defaultActivePanelNames: ['base'] })).toThrow('autoExpandPanels 开启时不能配置');
    expect(() => mountPage({ autoExpandPanels: false, defaultActivePanelNames: ['base', 'base'] })).toThrow(
      '不能包含重复 Key',
    );
    expect(() => mountPage({ autoExpandPanels: false, defaultActivePanelNames: ['missing'] })).toThrow(
      '包含不存在的面板 Key',
    );
  });

  it('加载态优先于错误态，错误态提供重试事件', async () => {
    const loading = mountPage({ loading: true, errorText: '接口异常' });
    expect(loading.find('[data-state="loading"]').exists()).toBe(true);
    expect(loading.find('[data-action="detail.retry"]').exists()).toBe(false);

    const error = mountPage({ errorText: '接口异常' });
    expect(error.find('[data-state="error"]').exists()).toBe(true);
    await error.find('[data-action="detail.retry"]').trigger('click');
    expect(error.emitted('retry')).toHaveLength(1);
  });

  it('通过公开方法打开和关闭工作流抽屉', async () => {
    const wrapper = mountPage({ showWorkflow: true });
    const exposed = wrapper.vm as unknown as MangoCollapseDetailPageExpose;

    exposed.openWorkflowDrawer();
    await nextTick();
    expect(wrapper.findComponent({ name: 'MangoSideDrawerShell' }).props('modelValue')).toBe(true);

    exposed.closeWorkflowDrawer();
    await nextTick();
    expect(wrapper.findComponent({ name: 'MangoSideDrawerShell' }).props('modelValue')).toBe(false);
  });

  it('内置描述列表、文件列表和无卡片表格内容', () => {
    const wrapper = mountPage({
      panels: [
        ...panels,
        {
          name: 'records',
          title: '记录',
          content: {
            componentType: 'list-table',
            data: { rows: [], columns: [] },
          },
        },
      ],
    });

    expect(wrapper.findComponent({ name: 'MangoDescriptionList' }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: 'MangoFileList' }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: 'MangoDataTable' }).props()).toMatchObject({
      card: false,
      mode: 'flat',
      showModeSwitch: false,
    });
  });
});
