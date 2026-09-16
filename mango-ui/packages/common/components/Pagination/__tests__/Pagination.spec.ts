import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { describe, expect, it } from 'vitest';
import Pagination from '../index.vue';

const ElPaginationStub = defineComponent({
  name: 'ElPagination',
  props: {
    size: { type: String, default: undefined },
    currentPage: { type: Number, default: 1 },
    pageSize: { type: Number, default: 20 },
  },
  emits: ['update:current-page', 'update:page-size'],
  setup(props) {
    return () =>
      h('div', {
        'data-size': props.size,
        'data-page': props.currentPage,
        'data-limit': props.pageSize,
      });
  },
});

function mountPagination(small: boolean) {
  return mount(Pagination, {
    props: { small },
    global: { stubs: { ElPagination: ElPaginationStub } },
  });
}

describe('Pagination', () => {
  it('keeps the existing runtime component name', () => {
    expect(Pagination.name).toBe('Pagination');
  });

  it('maps the compatible small input to the current Element Plus size prop', () => {
    expect(mountPagination(true).get('[data-size]').attributes('data-size')).toBe('small');
    expect(mountPagination(false).get('[data-size]').attributes('data-size')).toBe('default');
  });

  it('aligns the pagination container without changing the page and limit API', () => {
    const wrapper = mount(Pagination, {
      props: { page: 2, limit: 30, align: 'center' },
      global: { stubs: { ElPagination: ElPaginationStub } },
    });

    expect(wrapper.classes()).toContain('mango-pagination--center');
    expect(wrapper.get('[data-size]').attributes()).toMatchObject({
      'data-page': '2',
      'data-limit': '30',
    });
  });

  it('coalesces linked page-size and current-page updates into one business event', async () => {
    const wrapper = mount(Pagination, {
      props: { page: 4, limit: 20 },
      global: { stubs: { ElPagination: ElPaginationStub } },
    });
    const pagination = wrapper.getComponent(ElPaginationStub);

    pagination.vm.$emit('update:page-size', 50);
    pagination.vm.$emit('update:current-page', 1);
    await Promise.resolve();

    expect(wrapper.emitted('update:limit')).toEqual([[50]]);
    expect(wrapper.emitted('update:page')).toEqual([[1]]);
    expect(wrapper.emitted('pagination')).toEqual([[{ page: 1, limit: 50 }]]);
  });
});
