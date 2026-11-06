import { createApp, h, ref } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import Dialog from '@dialog';

const App = {
  setup() {
    const short = ref(false);
    const tall = ref(false);
    const drag = ref(false);

    function openShort() {
      short.value = true;
    }
    function openTall() {
      tall.value = true;
    }
    function openDrag() {
      drag.value = true;
    }

    return () =>
      h('div', { style: 'padding:16px' }, [
        h('button', { id: 'open-short', onClick: openShort }, 'short'),
        h('button', { id: 'open-tall', onClick: openTall }, 'tall'),
        h('button', { id: 'open-drag', onClick: openDrag }, 'drag'),
        h(
          Dialog,
          {
            modelValue: short.value,
            'onUpdate:modelValue': (v: boolean) => (short.value = v),
            title: '短内容弹框',
            width: '600px',
          },
          {
            default: () => h('div', { style: 'height:120px' }, 'short body'),
            footer: () => h('button', '确定'),
          },
        ),
        h(
          Dialog,
          {
            modelValue: tall.value,
            'onUpdate:modelValue': (v: boolean) => (tall.value = v),
            title: '长内容弹框',
            width: '600px',
          },
          {
            default: () => h('div', { style: 'height:2400px' }, 'tall body'),
            footer: () => h('button', '确定'),
          },
        ),
        h(
          Dialog,
          {
            modelValue: drag.value,
            'onUpdate:modelValue': (v: boolean) => (drag.value = v),
            title: '可拖拽弹框',
            width: '600px',
            draggable: true,
            resizable: true,
          },
          {
            default: () => h('div', { style: 'height:200px' }, 'drag body'),
          },
        ),
      ]);
  },
};

createApp(App).use(ElementPlus).mount('#app');
