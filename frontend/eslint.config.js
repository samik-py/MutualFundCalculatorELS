import js from '@eslint/js'
import jsxA11y from 'eslint-plugin-jsx-a11y'

export default [
  js.configs.recommended,
  {
    files: ['src/**/*.{js,jsx}'],
    plugins: { 'jsx-a11y': jsxA11y },
    rules: jsxA11y.configs.recommended.rules,
    languageOptions: {
      parserOptions: { ecmaFeatures: { jsx: true } },
      globals: {
        window: 'readonly',
        document: 'readonly',
        console: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        requestAnimationFrame: 'readonly',
        cancelAnimationFrame: 'readonly',
        performance: 'readonly',
        Math: 'readonly',
        Intl: 'readonly',
      },
    },
  },
]
