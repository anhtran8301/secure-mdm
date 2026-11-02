# Frontend (React 19, Vite, MUI 7 / Minimal template, TanStack Query, Yarn 1)

```
src/
  lib/          axios.ts (bearer client, 401 -> one shared refresh + retry), auth-client.ts (cookie
                endpoints), session.ts (login/refresh/logout, Web Locks, BroadcastChannel),
                api-error.ts, query-client.ts, page.ts, format.ts
  stores/       Zustand stores for client state (auth-store: user + in-memory access token)
  features/<domain>/
    api.ts        axios calls returning typed data
    queries.ts    `<domain>Queries` = { key: {...}, list(), detail() } built with queryOptions
    mutations.ts  useXxx() hooks: invalidate exact keys, toast success
    schemas.ts    Zod schemas + inferred form types + form <-> body converters
    components/, views/   views are lazy-loaded routes; components are reused across features
  components/   console/ (Heading, DataTable, Pager, Status, QueryState, ErrorMessage),
                hook-form/ (Form, RHFTextField, RHFSelect, RHFCheckbox), snackbar/ (Sonner),
                iconify/ (Minimal icons, offline sets in icon-sets.ts)
  routes/       paths.ts, guards/ (AuthGuard, GuestGuard), sections.tsx
  layouts/      console-layout.tsx, nav-config.tsx
  types/api.ts  backend DTO types
```

## Conventions

- Server state only in TanStack Query; Zustand only for client/UI state; never persist tokens.
- Query keys come from the feature's factory; mutations invalidate the narrowest keys. Polling
  (`LIVE_REFETCH_MS`) only for live device/command/dashboard data.
- Forms: React Hook Form + `zodResolver`; render fields with `components/hook-form`; show API
  errors inline with `ErrorMessage`, success with `toast` from `components/snackbar`.
- Dates: Day.js through Minimal's `src/utils/format-time.ts` and `src/lib/format.ts`
  (`formatTimestamp`, `formatRelative`); no date-fns. Utilities (`debounce`, `throttle`,
  `cloneDeep`, ...): `es-toolkit`, imported by name (tree-shakeable, Lodash-compatible); no lodash.
- Status text: `Status` (Minimal `Label`, soft variant); never hand-roll chips. Colours come from
  `statusColor`/`statusMainColor`, so labels and charts agree. Charts: Minimal `Chart` + `useChart`
  (ApexCharts loads lazily in its own chunk); give each chart `role="img"` and an `aria-label`.
- Lists: `usePagination()` plus `<Pager>` with `onSizeChange` for a rows-per-page selector (sizes
  5-100; the backend caps at 100). Omit `onSizeChange` for small helper lists with a fixed size.
- Sign-out lives in the avatar menu (`layouts/account-menu.tsx`); e2e opens it via the "Tài khoản" button.
- Icons: Minimal `Iconify` (register new icons in `icon-sets.ts`); no lucide-react. Toasts: Sonner.
- Labels and button names are E2E selectors (`e2e/workflow.spec.ts`); change them together.
- Lint/format: ESLint (perfectionist import sort) + Prettier, `corepack yarn fix:all`.
- Context7 ids for `query-docs`: React `/reactjs/react.dev`, MUI `/mui/material-ui`, React Router
  `/remix-run/react-router`, TanStack Query `/tanstack/query` (v5), Zod `/colinhacks/zod` (v4, e.g.
  top-level `z.email()`), React Hook Form `/react-hook-form/documentation`, Zustand `/pmndrs/zustand`,
  Axios `/axios/axios`, Vite `/vitejs/vite`, Vitest `/vitest-dev/vitest`, Playwright
  `/microsoft/playwright`, Day.js `/iamkun/dayjs`, es-toolkit `/toss/es-toolkit`, Sonner
  `/emilkowalski/sonner`. Verify against `package.json` versions before applying an example.
- TanStack Query docs for the installed version: `npx @tanstack/intent@latest load @tanstack/react-query#core`.
- Skills: `vercel-react-best-practices` (React/perf), `vercel-composition-patterns` (component
  APIs), `web-design-guidelines` (a11y/UX review). Next.js-only rules do not apply (Vite).
