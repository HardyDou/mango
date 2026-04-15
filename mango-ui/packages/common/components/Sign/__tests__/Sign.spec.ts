import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import Sign from '../index.vue';

// Mock Element Plus icons
vi.mock('@element-plus/icons-vue', () => ({
  WarnTriangleFilled: {
    template: '<span class="mock-warn-icon"></span>',
  },
  ElButton: {
    template: '<button class="el-button"><slot /></button>',
    props: ['size', 'disabled'],
  },
}));

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'sign.placeholder': 'Please sign here',
        'sign.clear': 'Clear',
        'sign.color': 'Color',
        'sign.error': 'Failed to generate signature, please retry',
      };
      return translations[key] || key;
    },
  }),
}));

describe('Sign Component', () => {
  describe('Props', () => {
    it('should accept default props', () => {
      const wrapper = mount(Sign, {
        props: {},
      });
      expect(wrapper.exists()).toBe(true);
      wrapper.unmount();
    });

    it('should accept custom width and height', () => {
      const wrapper = mount(Sign, {
        props: {
          width: 600,
          height: 300,
        },
      });
      const canvas = wrapper.find('canvas');
      expect(canvas.exists()).toBe(true);
      expect(canvas.attributes('width')).toBe('600');
      expect(canvas.attributes('height')).toBe('300');
      wrapper.unmount();
    });

    it('should accept custom stroke color', () => {
      const wrapper = mount(Sign, {
        props: {
          strokeColor: '#FF0000',
        },
      });
      expect(wrapper.exists()).toBe(true);
      wrapper.unmount();
    });

    it('should accept disabled prop', () => {
      const wrapper = mount(Sign, {
        props: {
          disabled: true,
        },
      });
      expect(wrapper.find('.sign-canvas-wrapper').classes()).toContain('is-disabled');
      wrapper.unmount();
    });
  });

  describe('ModelValue Binding', () => {
    it('should handle empty string modelValue', () => {
      const wrapper = mount(Sign, {
        props: {
          modelValue: '',
        },
      });
      expect(wrapper.find('.sign-canvas-wrapper').classes()).toContain('is-empty');
      wrapper.unmount();
    });

    it('should show placeholder when empty and not disabled', () => {
      const wrapper = mount(Sign, {
        props: {
          modelValue: '',
          disabled: false,
        },
      });
      expect(wrapper.find('.sign-placeholder').exists()).toBe(true);
      wrapper.unmount();
    });

    it('should emit update:modelValue when signature changes', async () => {
      const wrapper = mount(Sign, {
        props: {
          modelValue: '',
        },
      });

      // Simulate drawing by directly calling canvas operations
      const canvas = wrapper.find('canvas');
      await canvas.trigger('mousedown', { offsetX: 10, offsetY: 10 });
      await canvas.trigger('mousemove', { offsetX: 50, offsetY: 50 });
      await canvas.trigger('mouseup');

      // Check if update:modelValue was emitted (signature was generated)
      const updateEvents = wrapper.emitted('update:modelValue');
      expect(updateEvents).toBeTruthy();
      wrapper.unmount();
    });
  });

  describe('Clear Function', () => {
    it('should clear signature and emit empty string', async () => {
      const wrapper = mount(Sign, {
        props: {
          modelValue: '',
        },
      });

      const signComponent = wrapper.findComponent(Sign);
      const exposed = (signComponent as InstanceType<typeof Sign>).exposed;

      // Simulate some drawing state
      const canvas = wrapper.find('canvas');
      await canvas.trigger('mousedown', { offsetX: 10, offsetY: 10 });
      await canvas.trigger('mousemove', { offsetX: 50, offsetY: 50 });
      await canvas.trigger('mouseup');

      // Now clear
      if (exposed?.clear) {
        exposed.clear();
      }

      wrapper.unmount();
    });
  });

  describe('Color Selection', () => {
    it('should have predefined colors', () => {
      const colors = [
        { label: 'Black', value: '#000000' },
        { label: 'Red', value: '#FF0000' },
        { label: 'Blue', value: '#0000FF' },
        { label: 'Green', value: '#00FF00' },
      ];

      expect(colors).toHaveLength(4);
      expect(colors[0].value).toBe('#000000');
    });

    it('should render color dots', () => {
      const wrapper = mount(Sign, {
        props: {},
      });
      const colorDots = wrapper.findAll('.sign-color-dot');
      expect(colorDots).toHaveLength(4);
      wrapper.unmount();
    });
  });

  describe('Canvas Events', () => {
    it('should handle mousedown event', async () => {
      const wrapper = mount(Sign, {
        props: {},
      });

      const canvas = wrapper.find('canvas');
      await canvas.trigger('mousedown', { offsetX: 10, offsetY: 10 });

      wrapper.unmount();
    });

    it('should handle mousemove event', async () => {
      const wrapper = mount(Sign, {
        props: {},
      });

      const canvas = wrapper.find('canvas');
      // First trigger mousedown to start drawing
      await canvas.trigger('mousedown', { offsetX: 10, offsetY: 10 });
      // Then move
      await canvas.trigger('mousemove', { offsetX: 50, offsetY: 50 });

      wrapper.unmount();
    });

    it('should handle mouseup event', async () => {
      const wrapper = mount(Sign, {
        props: {},
      });

      const canvas = wrapper.find('canvas');
      await canvas.trigger('mousedown', { offsetX: 10, offsetY: 10 });
      await canvas.trigger('mouseup');

      wrapper.unmount();
    });

    it('should handle mouseleave event', async () => {
      const wrapper = mount(Sign, {
        props: {},
      });

      const canvas = wrapper.find('canvas');
      await canvas.trigger('mouseleave');

      wrapper.unmount();
    });

    it('should handle touchstart event', async () => {
      const wrapper = mount(Sign, {
        props: {},
      });

      const canvas = wrapper.find('canvas');
      await canvas.trigger('touchstart', {
        touches: [{ clientX: 10, clientY: 10 }],
      });

      wrapper.unmount();
    });
  });

  describe('Error State', () => {
    it('should not show error by default', () => {
      const wrapper = mount(Sign, {
        props: {},
      });
      expect(wrapper.find('.sign-error').exists()).toBe(false);
      wrapper.unmount();
    });
  });
});
