import { useAuthStore } from 'src/stores/auth-store';

/** UI affordances only; the backend enforces every role check. */
export function usePermissions() {
  const role = useAuthStore((state) => state.user?.role);
  return { edit: !!role && role !== 'VIEWER', admin: role === 'ORG_ADMIN' };
}
