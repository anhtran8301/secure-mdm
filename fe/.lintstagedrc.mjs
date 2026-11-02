// Runs from fe/ so ESLint and Prettier pick up this package's configs.
export default {
  '{src,e2e}/**/*.{ts,tsx}': ['eslint --fix --no-warn-ignored', 'prettier --write'],
  '*.{json,css,html}': ['prettier --write'],
};
