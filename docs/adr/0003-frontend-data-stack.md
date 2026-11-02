# Frontend data stack: TanStack Query, axios, Zustand, RHF + Zod, Sonner

Server state lives only in TanStack Query with per-feature `queryOptions` key factories; axios
centralizes bearer auth and the single-flight refresh on 401; Zustand holds client state (the
in-memory session, UI filters) and never persists tokens; forms use React Hook Form with Zod.
The Minimal template's Iconify is used for icons (no lucide-react) and Sonner for toasts, since
the free template has no snackbar.
