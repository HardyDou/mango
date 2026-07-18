export default {
  extends: ['stylelint-config-standard', 'stylelint-config-standard-scss', 'stylelint-config-recommended-vue/scss'],
  ignoreFiles: ['**/node_modules/**', '**/dist/**', '**/coverage/**', '**/build-reports/**'],
  overrides: [
    {
      files: ['**/*.vue'],
      customSyntax: 'postcss-html',
    },
  ],
  rules: {
    'custom-property-pattern': null,
    'keyframes-name-pattern': null,
    'selector-class-pattern': null,
    'selector-id-pattern': null,
  },
};
