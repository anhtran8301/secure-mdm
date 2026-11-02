/**
 * Conventional Commits: <type>(<scope>): <subject>
 * e.g. feat(be): add device search filter
 */
export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'scope-enum': [2, 'always', ['be', 'fe', 'sim', 'e2e', 'ci', 'docs', 'ai', 'deps', 'repo']],
    'subject-case': [0],
    'body-max-line-length': [1, 'always', 100],
  },
};
