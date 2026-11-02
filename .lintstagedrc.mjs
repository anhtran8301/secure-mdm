// Root-level files only; be/, fe/ and simulator/ have their own configs.
export default {
  '*.{json,md,yml,yaml,mjs}': ['prettier --write'],
  '.github/**/*.{yml,yaml}': ['prettier --write'],
};
