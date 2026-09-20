import { defineComponent, h } from 'vue';

const noopRequest = async () => undefined;

export const MangoDialog = defineComponent({
  name: 'MangoDialog',
  props: { modelValue: Boolean, title: { type: String, default: '' } },
  emits: ['update:modelValue', 'closed'],
  setup(props, { slots }) {
    return () =>
      props.modelValue
        ? h('section', { class: 'mango-dialog-stub', 'data-title': props.title }, [
            h('div', slots.default?.()),
            slots.footer ? h('footer', slots.footer()) : null,
          ])
        : null;
  },
});

export const get = noopRequest;
export const post = noopRequest;
export const put = noopRequest;
export const del = noopRequest;

export const request = {
  get: noopRequest,
  post: noopRequest,
  put: noopRequest,
  delete: noopRequest,
};

export type RequestConfig = Record<string, unknown>;
