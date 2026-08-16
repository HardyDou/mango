import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { describe, expect, it } from 'vitest';
import Pagination from '../index.vue';

const ElPaginationStub = defineComponent({
  name: 'ElPagination',
  props: {
    size: { type: String, default: undefined },
  },
  setup(props) {
    return () => h('div', { 'data-size': props.size });
  },
});

function mountPagination(small: boolean) {
  return mount(Pagination, {
    props: { small },
    global: { stubs: { ElPagination: ElPaginationStub } },
  });
}

describe('Pagination', () => {
  it('maps the compatible small input to the current Element Plus size prop', () => {
    expect(mountPagination(true).attributes('data-size')).toBe('small');
    expect(mountPagination(false).attributes('data-size')).toBe('default');
  });
});
