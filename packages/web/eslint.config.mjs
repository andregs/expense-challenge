import { defineConfig, globalIgnores } from 'eslint/config';
import nextVitals from 'eslint-config-next/core-web-vitals';
import nextTs from 'eslint-config-next/typescript';
import tseslint from 'typescript-eslint';
import importX from 'eslint-plugin-import-x';
import unicorn from 'eslint-plugin-unicorn';
import promise from 'eslint-plugin-promise';
import vitest from 'eslint-plugin-vitest';
import playwright from 'eslint-plugin-playwright';
import prettier from 'eslint-config-prettier';

const config = defineConfig([
  globalIgnores([
    '.next/**',
    'out/**',
    'build/**',
    'coverage/**',
    'next-env.d.ts',
    'public/mockServiceWorker.js',
  ]),

  ...nextVitals,
  ...nextTs,
  ...tseslint.configs.strictTypeChecked,
  ...tseslint.configs.stylisticTypeChecked,

  {
    languageOptions: {
      parserOptions: {
        projectService: {
          allowDefaultProject: ['eslint.config.mjs'],
        },
      },
    },
    plugins: {
      'import-x': importX,
      unicorn,
      promise,
    },
    rules: {
      'unicorn/filename-case': 'off',
      'unicorn/prevent-abbreviations': 'off',
      'unicorn/no-null': 'off',
    },
  },

  {
    files: ['**/*.test.{ts,tsx}', '**/__tests__/**/*.{ts,tsx}'],
    plugins: { vitest },
    rules: vitest.configs.recommended.rules,
  },

  {
    files: ['e2e/**/*.{ts,tsx}'],
    plugins: { playwright },
    rules: playwright.configs['flat/recommended'].rules,
  },

  prettier,
]);

export default config;
