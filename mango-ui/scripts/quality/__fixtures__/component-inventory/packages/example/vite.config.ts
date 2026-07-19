export default {
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'admin-pages': 'src/admin-pages.ts',
        'widgets/generated': 'src/widgets/generated.ts',
      },
      formats: ['es'],
    },
  },
};
