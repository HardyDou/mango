import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { defineComponent, h } from 'vue';
import MangoDetailPage from '../../MangoDetailPage/index.vue';
import MangoFormPage from '../../MangoFormPage/index.vue';
import MangoListPage from '../index.vue';
import MangoListPanel from '../../MangoListPanel/index.vue';
import MangoPageSection from '../../MangoPageSection/index.vue';
import MangoSearchPanel from '../../MangoSearchPanel/index.vue';

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: {
    type: String,
    icon: [Object, Function],
  },
  emits: ['click'],
  setup(_props, { emit, slots }) {
    return () => h('button', { class: 'el-button', type: 'button', onClick: event => emit('click', event) }, slots.default?.());
  },
});

const ElFormStub = defineComponent({
  name: 'ElForm',
  props: {
    model: Object,
    labelWidth: [String, Number],
    labelSuffix: String,
    labelPosition: String,
    size: String,
  },
  setup(props, { slots }) {
    return () => h('form', {
      class: 'el-form',
      'data-label-width': props.labelWidth,
      'data-label-suffix': props.labelSuffix,
      'data-label-position': props.labelPosition,
      'data-size': props.size,
    }, slots.default?.());
  },
});

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  props: {
    label: String,
  },
  setup(props, { slots }) {
    return () => h('label', { class: 'el-form-item' }, [
      props.label ? h('span', { class: 'el-form-item__label' }, props.label) : null,
      h('span', { class: 'el-form-item__content' }, slots.default?.()),
    ]);
  },
});

const SearchStub = defineComponent({
  name: 'Search',
  setup() {
    return () => h('span', { class: 'mock-search-icon' });
  },
});

const RefreshStub = defineComponent({
  name: 'Refresh',
  setup() {
    return () => h('span', { class: 'mock-refresh-icon' });
  },
});

const ArrowDownStub = defineComponent({
  name: 'ArrowDown',
  setup() {
    return () => h('span', { class: 'mock-arrow-down-icon' });
  },
});

const ArrowUpStub = defineComponent({
  name: 'ArrowUp',
  setup() {
    return () => h('span', { class: 'mock-arrow-up-icon' });
  },
});

const ArrowDownBoldStub = defineComponent({
  name: 'ArrowDownBold',
  setup() {
    return () => h('span', { class: 'mock-arrow-down-bold-icon' });
  },
});

const ArrowUpBoldStub = defineComponent({
  name: 'ArrowUpBold',
  setup() {
    return () => h('span', { class: 'mock-arrow-up-bold-icon' });
  },
});

const ElIconStub = defineComponent({
  name: 'ElIcon',
  setup(_props, { slots }) {
    return () => h('i', { class: 'el-icon' }, slots.default?.());
  },
});

const BackStub = defineComponent({
  name: 'Back',
  setup() {
    return () => h('span', { class: 'mock-back-icon' });
  },
});

const global = {
  stubs: {
    ElButton: ElButtonStub,
    ElForm: ElFormStub,
    ElFormItem: ElFormItemStub,
    Search: SearchStub,
    Refresh: RefreshStub,
    ArrowDown: ArrowDownStub,
    ArrowUp: ArrowUpStub,
    ArrowDownBold: ArrowDownBoldStub,
    ArrowUpBold: ArrowUpBoldStub,
    ElIcon: ElIconStub,
    Back: BackStub,
  },
};

describe('Mango admin page layout components', () => {
  it('renders list page search and list sections without a page title', () => {
    const wrapper = mount(MangoListPage, {
      props: { dataPage: 'demo.orders' },
      slots: {
        search: '<section class="search-slot">Search</section>',
        default: '<section class="list-slot">List</section>',
      },
      global,
    });

    expect(wrapper.attributes('data-page')).toBe('demo.orders');
    expect(wrapper.find('.search-slot').exists()).toBe(true);
    expect(wrapper.find('.list-slot').exists()).toBe(true);
    expect(wrapper.find('h1,h2').exists()).toBe(false);
  });

  it('keeps search actions stable and emits search/reset events', async () => {
    const wrapper = mount(MangoSearchPanel, {
      props: { model: { keyword: '' } },
      slots: {
        default: '<el-form-item label="关键字"><input /></el-form-item>',
      },
      global,
    });

    const buttons = wrapper.findAll('button');
    const buttonComponents = wrapper.findAllComponents(ElButtonStub);
    expect(wrapper.find('.mango-search-panel__fields').text()).toContain('关键字');
    expect(buttons.map(button => button.text())).toEqual(['查询', '重置']);

    buttonComponents[0].vm.$emit('click');
    buttonComponents[1].vm.$emit('click');

    expect(wrapper.emitted('search')).toHaveLength(1);
    expect(wrapper.emitted('reset')).toHaveLength(1);
  });

  it('passes default and custom form display options to Element Plus form', () => {
    const defaultWrapper = mount(MangoSearchPanel, {
      props: { model: { keyword: '' } },
      slots: {
        default: '<el-form-item label="关键字"><input /></el-form-item>',
      },
      global,
    });

    const defaultForm = defaultWrapper.find('form');
    expect(defaultForm.attributes('data-label-suffix')).toBe('：');
    expect(defaultForm.attributes('data-label-position')).toBe('right');
    expect(defaultForm.attributes('data-size')).toBe('default');
    expect(defaultWrapper.find('.mango-search-panel__fields--fixed').exists()).toBe(true);

    const customWrapper = mount(MangoSearchPanel, {
      props: {
        model: { keyword: '' },
        labelSuffix: '',
        labelPosition: 'left',
        size: 'small',
      },
      slots: {
        default: '<el-form-item label="关键字"><input /></el-form-item>',
      },
      global,
    });

    const customForm = customWrapper.find('form');
    expect(customForm.attributes('data-label-suffix')).toBe('');
    expect(customForm.attributes('data-label-position')).toBe('left');
    expect(customForm.attributes('data-size')).toBe('small');
  });

  it('collapses search fields to common items and expands all fields', async () => {
    const wrapper = mount(MangoSearchPanel, {
      props: {
        model: { keyword: '' },
        collapsible: true,
        collapsedCount: 2,
      },
      slots: {
        default: `
          <el-form-item label="关键字"><input /></el-form-item>
          <el-form-item label="状态"><input /></el-form-item>
          <el-form-item label="类型"><input /></el-form-item>
          <el-form-item label="日期"><input /></el-form-item>
        `,
      },
      global,
    });

    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const items = wrapper.findAll('.mango-search-panel__fields > .el-form-item');
    expect(items).toHaveLength(4);
    expect(items[0].attributes('data-mango-search-hidden')).toBeUndefined();
    expect(items[1].attributes('data-mango-search-hidden')).toBeUndefined();
    expect(items[2].attributes('data-mango-search-hidden')).toBe('true');
    expect(items[3].attributes('data-mango-search-hidden')).toBe('true');
    expect(wrapper.findAll('button').map(button => button.text())).toEqual(['查询', '重置', '']);
    expect(wrapper.find('.mango-search-panel__actions').text()).not.toContain('展开');
    expect(wrapper.find('.mango-search-panel__more-button').attributes('aria-label')).toBe('展开');

    await wrapper.find('.mango-search-panel__more-button').trigger('click');
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted('expandChange')).toEqual([[true]]);
    wrapper.findAll('.mango-search-panel__fields > .el-form-item').forEach((item) => {
      expect(item.attributes('data-mango-search-hidden')).toBeUndefined();
    });
    expect(wrapper.findAll('button').map(button => button.text())).toEqual(['查询', '重置', '']);
    expect(wrapper.find('.mango-search-panel__more-button').attributes('aria-label')).toBe('收起');
  });

  it('supports fixed columns and bottom expand action for dense search panels', async () => {
    const wrapper = mount(MangoSearchPanel, {
      props: {
        model: { keyword: '' },
        collapsible: true,
        columns: 4,
        collapsedRows: 2,
        morePlacement: 'bottom',
        searchText: 'Search',
        resetText: 'Reset',
        expandText: 'More',
        collapseText: 'Less',
      },
      slots: {
        default: Array.from({ length: 9 }, (_, index) => `<el-form-item label="Field ${index + 1}"><input /></el-form-item>`).join(''),
      },
      global,
    });

    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const items = wrapper.findAll('.mango-search-panel__fields > .el-form-item');
    expect(items).toHaveLength(9);
    expect(wrapper.find('.mango-search-panel__fields--fixed').exists()).toBe(true);
    expect(wrapper.find('.mango-search-panel__more').exists()).toBe(true);
    expect(items[7].attributes('data-mango-search-hidden')).toBeUndefined();
    expect(items[8].attributes('data-mango-search-hidden')).toBe('true');
    expect(wrapper.find('.mango-search-panel__actions').text()).not.toContain('More');
    expect(wrapper.find('.mango-search-panel__more-button').attributes('aria-label')).toBe('More');

    await wrapper.find('.mango-search-panel__more button').trigger('click');
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted('expandChange')).toEqual([[true]]);
    wrapper.findAll('.mango-search-panel__fields > .el-form-item').forEach((item) => {
      expect(item.attributes('data-mango-search-hidden')).toBeUndefined();
    });
    expect(wrapper.find('.mango-search-panel__more-button').attributes('aria-label')).toBe('Less');
  });

  it('keeps the bottom expand row placeholder when fields do not overflow', async () => {
    const wrapper = mount(MangoSearchPanel, {
      props: {
        model: { keyword: '' },
        collapsible: true,
        columns: 4,
        collapsedRows: 2,
        morePlacement: 'bottom',
        searchText: 'Search',
        resetText: 'Reset',
      },
      slots: {
        default: `
          <el-form-item label="Keyword"><input /></el-form-item>
          <el-form-item label="Status"><input /></el-form-item>
        `,
      },
      global,
    });

    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const items = wrapper.findAll('.mango-search-panel__fields > .el-form-item');
    expect(items).toHaveLength(2);
    expect(wrapper.find('.mango-search-panel__more').exists()).toBe(true);
    expect(wrapper.find('.mango-search-panel__more-button').exists()).toBe(false);
    expect(wrapper.findAll('button').map(button => button.text())).toEqual(['Search', 'Reset']);
    items.forEach((item) => {
      expect(item.attributes('data-mango-search-hidden')).toBeUndefined();
    });
  });

  it('renders list toolbar, table content and pagination slots', () => {
    const wrapper = mount(MangoListPanel, {
      slots: {
        actions: '<button class="create-action">新增</button>',
        default: '<table class="data-table"><tbody><tr><td>记录</td></tr></tbody></table>',
        pagination: '<nav class="pagination">分页</nav>',
      },
      global,
    });

    expect(wrapper.find('.create-action').exists()).toBe(true);
    expect(wrapper.find('.data-table').text()).toContain('记录');
    expect(wrapper.find('.pagination').exists()).toBe(true);
  });

  it('renders detail and form back bars and emits back events', async () => {
    const detail = mount(MangoDetailPage, {
      props: { title: '订单详情' },
      slots: { default: '<div>详情内容</div>' },
      global,
    });
    const form = mount(MangoFormPage, {
      props: { title: '编辑订单' },
      slots: { default: '<div>表单内容</div>' },
      global,
    });

    expect(detail.text()).toContain('订单详情');
    expect(form.text()).toContain('编辑订单');

    detail.findComponent(ElButtonStub).vm.$emit('click');
    form.findComponent(ElButtonStub).vm.$emit('click');

    expect(detail.emitted('back')).toHaveLength(1);
    expect(form.emitted('back')).toHaveLength(1);
  });

  it('renders page section title, extra slot and body', () => {
    const wrapper = mount(MangoPageSection, {
      props: { title: '基本信息' },
      slots: {
        extra: '<button>更多</button>',
        default: '<p>字段内容</p>',
      },
      global,
    });

    expect(wrapper.find('.mango-page-section__title').text()).toBe('基本信息');
    expect(wrapper.find('.mango-page-section__extra').text()).toBe('更多');
    expect(wrapper.find('.mango-page-section__body').text()).toBe('字段内容');
  });
});
